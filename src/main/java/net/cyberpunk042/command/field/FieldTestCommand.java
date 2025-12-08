package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.ListFormatter;
import net.cyberpunk042.command.util.ReportBuilder;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldProfileStore;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.FieldNetworking;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.pattern.*;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.visibility.MaskType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug/test commands for field definitions with LIVE editing.
 * 
 * <h2>Command Structure</h2>
 * <pre>
 * /fieldtest spawn <id>              - Spawn field definition
 * /fieldtest clear                   - Clear test field
 * /fieldtest list [filter]           - List definitions
 * /fieldtest info <id>               - Show definition details
 * /fieldtest cycle                   - Cycle through definitions
 * /fieldtest reload                  - Reload all definitions
 * 
 * /fieldtest edit shape.radius <v>   - Edit shape radius
 * /fieldtest edit shape.latSteps <v> - Edit latitude steps
 * /fieldtest edit shape.lonSteps <v> - Edit longitude steps  
 * /fieldtest edit shape.algorithm <v>- Edit tessellation algorithm
 * /fieldtest edit shape.$ref <ref>   - Load shape from $ref
 * 
 * /fieldtest edit transform.anchor <v>  - Edit anchor position
 * /fieldtest edit transform.scale <v>   - Edit scale
 * /fieldtest edit transform.$ref <ref>  - Load transform from $ref
 * 
 * /fieldtest edit fill.mode <v>      - Edit fill mode (solid/wireframe/cage)
 * /fieldtest edit fill.$ref <ref>    - Load fill from $ref
 * 
 * /fieldtest edit visibility.mask <v>- Edit visibility mask type
 * /fieldtest edit visibility.$ref <ref> - Load visibility from $ref
 * 
 * /fieldtest edit appearance.color <v>  - Edit color
 * /fieldtest edit appearance.alpha <v>  - Edit alpha
 * /fieldtest edit appearance.glow <v>   - Edit glow
 * /fieldtest edit appearance.$ref <ref> - Load appearance from $ref
 * 
 * /fieldtest edit animation.spin <v>    - Edit spin speed
 * /fieldtest edit animation.$ref <ref>  - Load animation from $ref
 * 
 * /fieldtest edit layer <idx>        - Select layer to edit
 * /fieldtest edit reset              - Reset all edit values
 * /fieldtest edit apply              - Apply changes
 * /fieldtest edit status             - Show current edit values
 * 
 * /fieldtest vertex <pattern>        - Set vertex pattern
 * /fieldtest shuffle next|prev|jump  - Explore shuffle patterns
 * /fieldtest follow snap|smooth|glide- Set follow mode
 * /fieldtest predict on|off|...      - Configure prediction
 * </pre>
 */
public final class FieldTestCommand {

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Current test field
    private static String currentProfile = null;
    private static long currentTestFieldId = -1;
    private static int currentLayerIndex = 0;
    
    // Shape parameters
    private static float shapeRadius = 5.0f;
    private static int shapeLatSteps = 32;
    private static int shapeLonSteps = 64;
    private static String shapeAlgorithm = "lat_lon";
    
    // Transform parameters
    private static Anchor transformAnchor = Anchor.CENTER;
    private static float transformScale = 1.0f;
    private static float transformOffsetX = 0.0f;
    private static float transformOffsetY = 0.0f;
    private static float transformOffsetZ = 0.0f;
    
    // Fill parameters
    private static FillMode fillMode = FillMode.SOLID;
    private static float fillWireThickness = 1.0f;
    private static boolean fillDoubleSided = false;
    
    // Visibility parameters
    private static MaskType visibilityMask = MaskType.FULL;
    private static int visibilityCount = 4;
    private static float visibilityThickness = 0.5f;
    
    // Appearance parameters
    private static String appearanceColor = "@primary";
    private static float appearanceAlpha = 0.6f;
    private static float appearanceGlow = 0.3f;
    
    // Animation parameters
    private static float animationSpin = 0.02f;
    private static float animationPhase = 0.0f;
    
    // Pattern state
    private static VertexPattern editPattern = QuadPattern.DEFAULT;
    private static ShuffleType shuffleType = ShuffleType.QUAD;
    private static int shuffleIndex = 0;
    
    // Follow mode and prediction
    private static FollowMode followMode = FollowMode.SNAP;
    private static boolean predictionEnabled = true;
    private static int predictionLeadTicks = 2;
    private static float predictionMaxDistance = 8.0f;
    private static float predictionLookAhead = 0.5f;
    private static float predictionVerticalBoost = 0.0f;
    
    // Profile cycling
    private static final List<String> PROFILE_ORDER = new ArrayList<>();
    private static int currentCycleIndex = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum ShuffleType {
        QUAD("quad", "Quad patterns (spheres/prisms)"),
        SEGMENT("segment", "Segment patterns (rings)"),
        SECTOR("sector", "Sector patterns (discs)"),
        EDGE("edge", "Edge patterns (wireframes)"),
        TRIANGLE("triangle", "Triangle patterns (polyhedra)");
        
        private final String id;
        private final String description;
        
        ShuffleType(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public String id() { return id; }
        public String description() { return description; }
        
        public int count() {
            return switch (this) {
                case QUAD -> ShuffleGenerator.quadCount();
                case SEGMENT -> ShuffleGenerator.segmentCount();
                case SECTOR -> ShuffleGenerator.sectorCount();
                case EDGE -> ShuffleGenerator.edgeCount();
                case TRIANGLE -> ShuffleGenerator.triangleCount();
            };
        }
        
        public static ShuffleType fromId(String id) {
            for (ShuffleType t : values()) {
                if (t.id.equalsIgnoreCase(id)) return t;
            }
            return QUAD;
        }
    }
    
    private FieldTestCommand() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUGGESTION PROVIDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Definition IDs
    private static final SuggestionProvider<ServerCommandSource> DEFINITION_SUGGESTIONS = (ctx, builder) -> {
        for (FieldDefinition def : FieldRegistry.all()) {
            String id = def.id();
            if (id.toLowerCase().contains(builder.getRemaining().toLowerCase())) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    };
    
    // Algorithms
    private static final SuggestionProvider<ServerCommandSource> ALGORITHM_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("lat_lon", Text.literal("Lat/Lon tessellation"));
        builder.suggest("type_a", Text.literal("Overlapping cubes"));
        builder.suggest("type_e", Text.literal("Rotated rectangles"));
        return builder.buildFuture();
    };
    
    // Anchors
    private static final SuggestionProvider<ServerCommandSource> ANCHOR_SUGGESTIONS = (ctx, builder) -> {
        for (Anchor a : Anchor.values()) {
            builder.suggest(a.name().toLowerCase(), Text.literal(a.name()));
        }
        return builder.buildFuture();
    };
    
    // Fill modes
    private static final SuggestionProvider<ServerCommandSource> FILL_MODE_SUGGESTIONS = (ctx, builder) -> {
        for (FillMode m : FillMode.values()) {
            builder.suggest(m.name().toLowerCase());
        }
        return builder.buildFuture();
    };
    
    // Mask types
    private static final SuggestionProvider<ServerCommandSource> MASK_TYPE_SUGGESTIONS = (ctx, builder) -> {
        for (MaskType m : MaskType.values()) {
            builder.suggest(m.name().toLowerCase());
        }
        return builder.buildFuture();
    };
    
    // Colors
    private static final SuggestionProvider<ServerCommandSource> COLOR_SUGGESTIONS = (ctx, builder) -> {
        // Theme references
        builder.suggest("@primary", Text.literal("Primary theme color"));
        builder.suggest("@secondary", Text.literal("Secondary theme color"));
        builder.suggest("@glow", Text.literal("Glow/emissive color"));
        builder.suggest("@accent", Text.literal("Accent color"));
        // Hex presets
        builder.suggest("#00CCFF", Text.literal("Cyan"));
        builder.suggest("#FF6600", Text.literal("Orange"));
        builder.suggest("#00FF88", Text.literal("Green"));
        builder.suggest("#FF0066", Text.literal("Pink"));
        builder.suggest("#FFCC00", Text.literal("Gold"));
        builder.suggest("#9900FF", Text.literal("Purple"));
        builder.suggest("#FFFFFF", Text.literal("White"));
        return builder.buildFuture();
    };
    
    // $ref suggestions for each category
    private static SuggestionProvider<ServerCommandSource> refSuggestions(String folder) {
        return (ctx, builder) -> {
            builder.suggest("$ref:" + folder + "/", Text.literal("Reference from " + folder));
            
            Path dataPath = Path.of("src/main/resources/data/the-virus-block/" + folder);
            if (Files.exists(dataPath)) {
                try (var stream = Files.list(dataPath)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            name = name.substring(0, name.length() - 5);
                            builder.suggest("$ref:" + folder + "/" + name);
                        });
                } catch (Exception ignored) {}
            }
            return builder.buildFuture();
        };
    }
    
    private static final SuggestionProvider<ServerCommandSource> SHAPE_REF = refSuggestions("field_shapes");
    private static final SuggestionProvider<ServerCommandSource> TRANSFORM_REF = refSuggestions("field_transforms");
    private static final SuggestionProvider<ServerCommandSource> FILL_REF = refSuggestions("field_fills");
    private static final SuggestionProvider<ServerCommandSource> VISIBILITY_REF = refSuggestions("field_masks");
    private static final SuggestionProvider<ServerCommandSource> APPEARANCE_REF = refSuggestions("field_appearances");
    private static final SuggestionProvider<ServerCommandSource> ANIMATION_REF = refSuggestions("field_animations");
    
    // Vertex patterns
    private static final SuggestionProvider<ServerCommandSource> VERTEX_SUGGESTIONS = (ctx, builder) -> {
        for (String id : QuadPattern.ids()) builder.suggest(id);
        for (String id : SegmentPattern.ids()) builder.suggest(id);
        for (String id : SectorPattern.ids()) builder.suggest(id);
        for (String id : EdgePattern.ids()) builder.suggest(id);
        return builder.buildFuture();
    };
    
    // Saved profiles
    private static final SuggestionProvider<ServerCommandSource> SAVED_SUGGESTIONS = (ctx, builder) -> {
        for (String name : FieldProfileStore.list()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var cmd = CommandManager.literal("fieldtest")
            .requires(source -> source.hasPermissionLevel(2));
        
        // Core commands
        cmd.then(buildSpawnCommand());
        cmd.then(buildClearCommand());
        cmd.then(buildListCommand());
        cmd.then(buildInfoCommand());
        cmd.then(buildCycleCommand());
        cmd.then(buildReloadCommand());
        cmd.then(buildStatusCommand());
        
        // Edit commands (dot-literal syntax)
        cmd.then(buildEditCommands());
        
        // Pattern commands
        cmd.then(buildVertexCommand());
        cmd.then(buildShuffleCommands());
        
        // Movement commands
        cmd.then(buildFollowCommands());
        cmd.then(buildPredictCommands());
        
        // Profile save/load
        cmd.then(buildSaveCommand());
        cmd.then(buildLoadCommand());
        cmd.then(buildSavedCommand());
        cmd.then(buildDeleteCommand());
        
        dispatcher.register(cmd);
        rebuildProfileOrder();
        Logging.COMMANDS.info("Registered /fieldtest with dot-literal edit syntax");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDIT COMMANDS - DOT-LITERAL SYNTAX
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildEditCommands() {
        var edit = CommandManager.literal("edit");
        
        // ─────────────────────────────────────────────────────────────────────────
        // SHAPE: /fieldtest edit shape.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var shape = CommandManager.literal("shape");
        
        shape.then(CommandManager.literal("radius")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 64.0f))
                .executes(ctx -> {
                    shapeRadius = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "shape.radius", shapeRadius);
                })));
        
        shape.then(CommandManager.literal("latSteps")
            .then(CommandManager.argument("value", IntegerArgumentType.integer(4, 512))
                .executes(ctx -> {
                    shapeLatSteps = IntegerArgumentType.getInteger(ctx, "value");
                    return editSuccess(ctx.getSource(), "shape.latSteps", shapeLatSteps);
                })));
        
        shape.then(CommandManager.literal("lonSteps")
            .then(CommandManager.argument("value", IntegerArgumentType.integer(8, 1024))
                .executes(ctx -> {
                    shapeLonSteps = IntegerArgumentType.getInteger(ctx, "value");
                    return editSuccess(ctx.getSource(), "shape.lonSteps", shapeLonSteps);
                })));
        
        shape.then(CommandManager.literal("algorithm")
            .then(CommandManager.argument("value", StringArgumentType.word())
                .suggests(ALGORITHM_SUGGESTIONS)
                .executes(ctx -> {
                    shapeAlgorithm = StringArgumentType.getString(ctx, "value");
                    return editSuccess(ctx.getSource(), "shape.algorithm", shapeAlgorithm);
                })));
        
        shape.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(SHAPE_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "shape", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(shape);
        
        // ─────────────────────────────────────────────────────────────────────────
        // TRANSFORM: /fieldtest edit transform.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var transform = CommandManager.literal("transform");
        
        transform.then(CommandManager.literal("anchor")
            .then(CommandManager.argument("value", StringArgumentType.word())
                .suggests(ANCHOR_SUGGESTIONS)
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "value");
                    try {
                        transformAnchor = Anchor.valueOf(name.toUpperCase());
                        return editSuccess(ctx.getSource(), "transform.anchor", transformAnchor.name().toLowerCase());
                    } catch (IllegalArgumentException e) {
                        CommandFeedback.error(ctx.getSource(), "Unknown anchor: " + name);
                        return 0;
                    }
                })));
        
        transform.then(CommandManager.literal("scale")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10.0f))
                .executes(ctx -> {
                    transformScale = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "transform.scale", transformScale);
                })));
        
        transform.then(CommandManager.literal("offset.x")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(-64f, 64f))
                .executes(ctx -> {
                    transformOffsetX = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "transform.offset.x", transformOffsetX);
                })));
        
        transform.then(CommandManager.literal("offset.y")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(-64f, 64f))
                .executes(ctx -> {
                    transformOffsetY = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "transform.offset.y", transformOffsetY);
                })));
        
        transform.then(CommandManager.literal("offset.z")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(-64f, 64f))
                .executes(ctx -> {
                    transformOffsetZ = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "transform.offset.z", transformOffsetZ);
                })));
        
        transform.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(TRANSFORM_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "transform", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(transform);
        
        // ─────────────────────────────────────────────────────────────────────────
        // FILL: /fieldtest edit fill.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var fill = CommandManager.literal("fill");
        
        fill.then(CommandManager.literal("mode")
            .then(CommandManager.argument("value", StringArgumentType.word())
                .suggests(FILL_MODE_SUGGESTIONS)
                .executes(ctx -> {
                    fillMode = FillMode.fromId(StringArgumentType.getString(ctx, "value"));
                    return editSuccess(ctx.getSource(), "fill.mode", fillMode.name().toLowerCase());
                })));
        
        fill.then(CommandManager.literal("wireThickness")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.5f, 10f))
                .executes(ctx -> {
                    fillWireThickness = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "fill.wireThickness", fillWireThickness);
                })));
        
        fill.then(CommandManager.literal("doubleSided")
            .then(CommandManager.literal("true").executes(ctx -> {
                fillDoubleSided = true;
                return editSuccess(ctx.getSource(), "fill.doubleSided", true);
            }))
            .then(CommandManager.literal("false").executes(ctx -> {
                fillDoubleSided = false;
                return editSuccess(ctx.getSource(), "fill.doubleSided", false);
            })));
        
        fill.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(FILL_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "fill", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(fill);
        
        // ─────────────────────────────────────────────────────────────────────────
        // VISIBILITY: /fieldtest edit visibility.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var visibility = CommandManager.literal("visibility");
        
        visibility.then(CommandManager.literal("mask")
            .then(CommandManager.argument("value", StringArgumentType.word())
                .suggests(MASK_TYPE_SUGGESTIONS)
                .executes(ctx -> {
                    visibilityMask = MaskType.fromId(StringArgumentType.getString(ctx, "value"));
                    return editSuccess(ctx.getSource(), "visibility.mask", visibilityMask.name().toLowerCase());
                })));
        
        visibility.then(CommandManager.literal("count")
            .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> {
                    visibilityCount = IntegerArgumentType.getInteger(ctx, "value");
                    return editSuccess(ctx.getSource(), "visibility.count", visibilityCount);
                })));
        
        visibility.then(CommandManager.literal("thickness")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                .executes(ctx -> {
                    visibilityThickness = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "visibility.thickness", visibilityThickness);
                })));
        
        visibility.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(VISIBILITY_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "visibility", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(visibility);
        
        // ─────────────────────────────────────────────────────────────────────────
        // APPEARANCE: /fieldtest edit appearance.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var appearance = CommandManager.literal("appearance");
        
        appearance.then(CommandManager.literal("color")
            .then(CommandManager.argument("value", StringArgumentType.greedyString())
                .suggests(COLOR_SUGGESTIONS)
                .executes(ctx -> {
                    appearanceColor = StringArgumentType.getString(ctx, "value");
                    return editSuccess(ctx.getSource(), "appearance.color", appearanceColor);
                })));
        
        appearance.then(CommandManager.literal("alpha")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                .executes(ctx -> {
                    appearanceAlpha = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "appearance.alpha", appearanceAlpha);
                })));
        
        appearance.then(CommandManager.literal("glow")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                .executes(ctx -> {
                    appearanceGlow = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "appearance.glow", appearanceGlow);
                })));
        
        appearance.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(APPEARANCE_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "appearance", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(appearance);
        
        // ─────────────────────────────────────────────────────────────────────────
        // ANIMATION: /fieldtest edit animation.<param>
        // ─────────────────────────────────────────────────────────────────────────
        var animation = CommandManager.literal("animation");
        
        animation.then(CommandManager.literal("spin")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(-1f, 1f))
                .executes(ctx -> {
                    animationSpin = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "animation.spin", animationSpin);
                })));
        
        animation.then(CommandManager.literal("phase")
            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                .executes(ctx -> {
                    animationPhase = FloatArgumentType.getFloat(ctx, "value");
                    return editSuccess(ctx.getSource(), "animation.phase", animationPhase);
                })));
        
        animation.then(CommandManager.literal("$ref")
            .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                .suggests(ANIMATION_REF)
                .executes(ctx -> loadRef(ctx.getSource(), "animation", StringArgumentType.getString(ctx, "ref")))));
        
        edit.then(animation);
        
        // ─────────────────────────────────────────────────────────────────────────
        // LAYER & ACTIONS
        // ─────────────────────────────────────────────────────────────────────────
        edit.then(CommandManager.literal("layer")
            .then(CommandManager.argument("index", IntegerArgumentType.integer(0, 10))
                .executes(ctx -> {
                    currentLayerIndex = IntegerArgumentType.getInteger(ctx, "index");
                    CommandFeedback.highlight(ctx.getSource(), "Now editing layer " + currentLayerIndex);
                    return 1;
                })));
        
        edit.then(CommandManager.literal("reset")
            .executes(ctx -> {
                resetEditValues();
                CommandFeedback.success(ctx.getSource(), "All edit values reset to defaults");
                return 1;
            }));
        
        edit.then(CommandManager.literal("apply")
            .executes(ctx -> {
                if (currentProfile == null) {
                    CommandFeedback.error(ctx.getSource(), "No test field spawned");
                    return 0;
                }
                syncToClients(ctx.getSource());
                CommandFeedback.success(ctx.getSource(), "Changes applied to test field");
                return 1;
            }));
        
        edit.then(CommandManager.literal("status")
            .executes(ctx -> showEditStatus(ctx.getSource())));
        
        return edit;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDIT HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int editSuccess(ServerCommandSource source, String param, Object value) {
        Logging.RENDER.topic("fieldtest").debug("Edit {} → {}", param, value);
        CommandFeedback.valueSet(source, param, String.valueOf(value));
        syncToClients(source);
        return 1;
    }
    
    private static int loadRef(ServerCommandSource source, String category, String ref) {
        Logging.RENDER.topic("fieldtest").info("Loading {} ref: {}", category, ref);
        CommandFeedback.info(source, category + " ref loaded: " + ref);
        // TODO: Parse and apply the ref
        syncToClients(source);
        return 1;
    }
    
    private static int showEditStatus(ServerCommandSource source) {
        ReportBuilder.create("Edit Parameters")
            .section("Shape", s -> s
                .kv("radius", shapeRadius + " blocks")
                .kv("latSteps", shapeLatSteps)
                .kv("lonSteps", shapeLonSteps)
                .kv("algorithm", shapeAlgorithm))
            .section("Transform", s -> s
                .kv("anchor", transformAnchor.name().toLowerCase())
                .kv("scale", transformScale)
                .kv("offset", String.format("(%.1f, %.1f, %.1f)", transformOffsetX, transformOffsetY, transformOffsetZ)))
            .section("Fill", s -> s
                .kv("mode", fillMode.name().toLowerCase())
                .kv("wireThickness", fillWireThickness)
                .kv("doubleSided", fillDoubleSided))
            .section("Visibility", s -> s
                .kv("mask", visibilityMask.name().toLowerCase())
                .kv("count", visibilityCount)
                .kv("thickness", visibilityThickness))
            .section("Appearance", s -> s
                .kv("color", appearanceColor)
                .kv("alpha", appearanceAlpha)
                .kv("glow", appearanceGlow))
            .section("Animation", s -> s
                .kv("spin", animationSpin + " rad/tick")
                .kv("phase", animationPhase))
            .section("Selection", s -> s
                .kv("layer", currentLayerIndex)
                .kv("profile", currentProfile != null ? currentProfile : "(none)"))
            .send(source);
        return 1;
    }
    
    private static void resetEditValues() {
        shapeRadius = 5.0f;
        shapeLatSteps = 32;
        shapeLonSteps = 64;
        shapeAlgorithm = "lat_lon";
        
        transformAnchor = Anchor.CENTER;
        transformScale = 1.0f;
        transformOffsetX = 0.0f;
        transformOffsetY = 0.0f;
        transformOffsetZ = 0.0f;
        
        fillMode = FillMode.SOLID;
        fillWireThickness = 1.0f;
        fillDoubleSided = false;
        
        visibilityMask = MaskType.FULL;
        visibilityCount = 4;
        visibilityThickness = 0.5f;
        
        appearanceColor = "@primary";
        appearanceAlpha = 0.6f;
        appearanceGlow = 0.3f;
        
        animationSpin = 0.02f;
        animationPhase = 0.0f;
        
        editPattern = QuadPattern.DEFAULT;
        currentLayerIndex = 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildSpawnCommand() {
        return CommandManager.literal("spawn")
            .then(CommandManager.argument("id", StringArgumentType.word())
                .suggests(DEFINITION_SUGGESTIONS)
                .executes(ctx -> handleSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "id"))));
    }
    
    private static int handleSpawn(ServerCommandSource source, String id) {
        // Clear previous
        if (currentTestFieldId != -1 && source.getEntity() instanceof ServerPlayerEntity player) {
            FieldManager.get((ServerWorld) player.getWorld()).remove(currentTestFieldId);
            currentTestFieldId = -1;
        }
        
        FieldDefinition def = FieldRegistry.get(Identifier.of("the-virus-block", id));
        if (def == null) {
            // Try without namespace
            for (FieldDefinition d : FieldRegistry.all()) {
                if (d.id().equals(id)) {
                    def = d;
                    break;
                }
            }
        }
        
        if (def == null) {
            CommandFeedback.notFound(source, "Field definition", id);
            return 0;
        }
        
        currentProfile = def.id();
        shapeRadius = def.baseRadius();
        if (def.modifiers() != null) {
            transformScale = def.modifiers().visualScale();
        }
        
        // Spawn
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d pos = player.getPos().add(0, 1.0, 0);
            var instance = FieldManager.get((ServerWorld) player.getWorld())
                .spawnAt(Identifier.of("the-virus-block", def.id()), pos, shapeRadius * transformScale, -1);
            if (instance != null) {
                currentTestFieldId = instance.id();
            }
        }
        
        CommandFeedback.success(source, "Spawned: " + def.id());
        return 1;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildClearCommand() {
        return CommandManager.literal("clear")
            .executes(ctx -> {
                if (currentTestFieldId != -1 && ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                    FieldManager.get((ServerWorld) player.getWorld()).remove(currentTestFieldId);
                    currentTestFieldId = -1;
                }
                currentProfile = null;
                resetEditValues();
                CommandFeedback.success(ctx.getSource(), "Test field cleared");
                return 1;
            });
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildListCommand() {
        return CommandManager.literal("list")
            .executes(ctx -> {
                List<FieldDefinition> defs = new ArrayList<>();
                FieldRegistry.all().forEach(defs::add);
                defs.sort((a, b) -> a.id().compareTo(b.id()));
                
                return ListFormatter.<FieldDefinition>create("Field Definitions")
                    .showCount(true)
                    .items(defs, def -> ListFormatter.entry(def.id())
                        .tag(def.type().id(), Formatting.DARK_AQUA)
                        .tag(def.layers().size() + " layers", Formatting.GRAY))
                    .send(ctx.getSource());
            })
            .then(CommandManager.argument("filter", StringArgumentType.word())
                .executes(ctx -> {
                    String filter = StringArgumentType.getString(ctx, "filter").toLowerCase();
                    List<FieldDefinition> defs = new ArrayList<>();
                    FieldRegistry.all().forEach(d -> {
                        if (d.id().toLowerCase().contains(filter)) defs.add(d);
                    });
                    defs.sort((a, b) -> a.id().compareTo(b.id()));
                    
                    return ListFormatter.<FieldDefinition>create("Definitions matching: " + filter)
                        .showCount(true)
                        .items(defs, def -> ListFormatter.entry(def.id()))
                        .send(ctx.getSource());
                }));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildInfoCommand() {
        return CommandManager.literal("info")
            .then(CommandManager.argument("id", StringArgumentType.word())
                .suggests(DEFINITION_SUGGESTIONS)
                .executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    FieldDefinition def = FieldRegistry.get(Identifier.of("the-virus-block", id));
                    if (def == null) {
                        CommandFeedback.notFound(ctx.getSource(), "Definition", id);
                        return 0;
                    }
                    
                    var report = ReportBuilder.create("Field: " + def.id())
                        .kv("Type", def.type().id())
                        .kv("Base Radius", def.baseRadius() + " blocks")
                        .kv("Theme", def.themeId() != null ? def.themeId() : "default")
                        .kv("Layers", def.layers().size());
                    
                    report.section("Layers", s -> {
                        int idx = 0;
                        for (FieldLayer layer : def.layers()) {
                            s.line("[" + idx++ + "] " + layer.id() + " (" + layer.primitives().size() + " primitives)");
                        }
                    });
                    
                    report.send(ctx.getSource());
                    return 1;
                }));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildCycleCommand() {
        return CommandManager.literal("cycle")
            .executes(ctx -> {
                if (PROFILE_ORDER.isEmpty()) rebuildProfileOrder();
                if (PROFILE_ORDER.isEmpty()) {
                    CommandFeedback.error(ctx.getSource(), "No profiles available");
                    return 0;
                }
                
                currentCycleIndex = (currentCycleIndex + 1) % PROFILE_ORDER.size();
                String next = PROFILE_ORDER.get(currentCycleIndex);
                CommandFeedback.highlight(ctx.getSource(), "[" + (currentCycleIndex + 1) + "/" + PROFILE_ORDER.size() + "] " + next);
                return handleSpawn(ctx.getSource(), next);
            });
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildReloadCommand() {
        return CommandManager.literal("reload")
            .executes(ctx -> {
                net.cyberpunk042.config.InfectionConfigRegistry.loadCommon();
                rebuildProfileOrder();
                java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
                FieldRegistry.all().forEach(def -> count.incrementAndGet());
                CommandFeedback.success(ctx.getSource(), "Reloaded " + count + " definitions");
                return 1;
            });
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildStatusCommand() {
        return CommandManager.literal("status")
            .executes(ctx -> {
                ReportBuilder.create("Field Test Status")
                    .kv("Active Profile", currentProfile != null ? currentProfile : "none")
                    .kv("Layer", currentLayerIndex)
                    .kv("Pattern", editPattern.id())
                    .send(ctx.getSource());
                return 1;
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VERTEX & SHUFFLE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildVertexCommand() {
        return CommandManager.literal("vertex")
            .executes(ctx -> {
                ReportBuilder.create("Vertex Patterns")
                    .section("Quad", s -> { for (String id : QuadPattern.ids()) s.line("  " + id); })
                    .section("Segment", s -> { for (String id : SegmentPattern.ids()) s.line("  " + id); })
                    .section("Sector", s -> { for (String id : SectorPattern.ids()) s.line("  " + id); })
                    .section("Edge", s -> { for (String id : EdgePattern.ids()) s.line("  " + id); })
                    .kv("Current", editPattern.id(), Formatting.GREEN)
                    .send(ctx.getSource());
                return 1;
            })
            .then(CommandManager.argument("pattern", StringArgumentType.word())
                .suggests(VERTEX_SUGGESTIONS)
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "pattern");
                    VertexPattern pattern = VertexPattern.fromString(name);
                    if (pattern == null) {
                        CommandFeedback.error(ctx.getSource(), "Unknown pattern: " + name);
                        return 0;
                    }
                    editPattern = pattern;
                    CommandFeedback.valueSet(ctx.getSource(), "Vertex pattern", pattern.id());
                    syncToClients(ctx.getSource());
                    return 1;
                }));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildShuffleCommands() {
        return CommandManager.literal("shuffle")
            .executes(ctx -> {
                CommandFeedback.info(ctx.getSource(), 
                    "[" + shuffleType.id() + " " + (shuffleIndex + 1) + "/" + shuffleType.count() + "]");
                return 1;
            })
            .then(CommandManager.literal("next").executes(ctx -> {
                shuffleIndex = (shuffleIndex + 1) % shuffleType.count();
                return applyShuffleAndShow(ctx.getSource());
            }))
            .then(CommandManager.literal("prev").executes(ctx -> {
                shuffleIndex = (shuffleIndex - 1 + shuffleType.count()) % shuffleType.count();
                return applyShuffleAndShow(ctx.getSource());
            }))
            .then(CommandManager.literal("jump")
                .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                    .executes(ctx -> {
                        int idx = IntegerArgumentType.getInteger(ctx, "index");
                        if (idx >= shuffleType.count()) {
                            CommandFeedback.error(ctx.getSource(), "Max index: " + (shuffleType.count() - 1));
                            return 0;
                        }
                        shuffleIndex = idx;
                        return applyShuffleAndShow(ctx.getSource());
                    })))
            .then(CommandManager.literal("type")
                .then(CommandManager.literal("quad").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.QUAD)))
                .then(CommandManager.literal("segment").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.SEGMENT)))
                .then(CommandManager.literal("sector").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.SECTOR)))
                .then(CommandManager.literal("edge").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.EDGE)))
                .then(CommandManager.literal("triangle").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.TRIANGLE))));
    }
    
    private static int setShuffle(ServerCommandSource source, ShuffleType type) {
        shuffleType = type;
        shuffleIndex = 0;
        CommandFeedback.success(source, "Switched to " + type.id() + " (" + type.count() + " patterns)");
        return applyShuffleAndShow(source);
    }
    
    private static int applyShuffleAndShow(ServerCommandSource source) {
        String desc = switch (shuffleType) {
            case QUAD -> {
                var arr = ShuffleGenerator.getQuad(shuffleIndex);
                editPattern = DynamicQuadPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case SEGMENT -> {
                var arr = ShuffleGenerator.getSegment(shuffleIndex);
                editPattern = DynamicSegmentPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case SECTOR -> {
                var arr = ShuffleGenerator.getSector(shuffleIndex);
                editPattern = DynamicSectorPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case EDGE -> {
                var arr = ShuffleGenerator.getEdge(shuffleIndex);
                editPattern = DynamicEdgePattern.fromArrangement(arr);
                yield arr.describe();
            }
            case TRIANGLE -> {
                var arr = ShuffleGenerator.getTriangle(shuffleIndex);
                editPattern = DynamicTrianglePattern.fromArrangement(arr);
                yield arr.describe();
            }
        };
        
        CommandFeedback.highlight(source, 
            String.format("[%s #%d/%d] %s", shuffleType.id().toUpperCase(), shuffleIndex + 1, shuffleType.count(), desc));
        syncToClients(source);
        return 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW & PREDICT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildFollowCommands() {
        return CommandManager.literal("follow")
            .executes(ctx -> {
                CommandFeedback.info(ctx.getSource(), "Follow mode: " + followMode.id());
                return 1;
            })
            .then(CommandManager.literal("snap").executes(ctx -> setFollow(ctx.getSource(), FollowMode.SNAP)))
            .then(CommandManager.literal("smooth").executes(ctx -> setFollow(ctx.getSource(), FollowMode.SMOOTH)))
            .then(CommandManager.literal("glide").executes(ctx -> setFollow(ctx.getSource(), FollowMode.GLIDE)));
    }
    
    private static int setFollow(ServerCommandSource source, FollowMode mode) {
        followMode = mode;
        CommandFeedback.valueSet(source, "Follow mode", mode.id());
        syncToClients(source);
        return 1;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildPredictCommands() {
        return CommandManager.literal("predict")
            .executes(ctx -> {
                ReportBuilder.create("Prediction")
                    .kv("Enabled", predictionEnabled)
                    .kv("Lead", predictionLeadTicks + " ticks")
                    .kv("Max", predictionMaxDistance + " blocks")
                    .kv("Look", predictionLookAhead)
                    .kv("Vertical", predictionVerticalBoost)
                    .send(ctx.getSource());
                return 1;
            })
            .then(CommandManager.literal("on").executes(ctx -> { 
                predictionEnabled = true; 
                CommandFeedback.toggle(ctx.getSource(), "Prediction", true);
                syncToClients(ctx.getSource());
                return 1;
            }))
            .then(CommandManager.literal("off").executes(ctx -> { 
                predictionEnabled = false; 
                CommandFeedback.toggle(ctx.getSource(), "Prediction", false);
                syncToClients(ctx.getSource());
                return 1;
            }))
            .then(CommandManager.literal("lead")
                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0, 60))
                    .executes(ctx -> {
                        predictionLeadTicks = IntegerArgumentType.getInteger(ctx, "ticks");
                        CommandFeedback.valueSet(ctx.getSource(), "Prediction lead", predictionLeadTicks + " ticks");
                        syncToClients(ctx.getSource());
                        return 1;
                    })))
            .then(CommandManager.literal("max")
                .then(CommandManager.argument("blocks", FloatArgumentType.floatArg(0, 64))
                    .executes(ctx -> {
                        predictionMaxDistance = FloatArgumentType.getFloat(ctx, "blocks");
                        CommandFeedback.valueSet(ctx.getSource(), "Prediction max", predictionMaxDistance + " blocks");
                        syncToClients(ctx.getSource());
                        return 1;
                    })));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE/LOAD COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildSaveCommand() {
        return CommandManager.literal("save")
            .then(CommandManager.argument("name", StringArgumentType.word())
                .executes(ctx -> {
                    if (currentProfile == null) {
                        CommandFeedback.error(ctx.getSource(), "No active profile");
                        return 0;
                    }
                    String name = StringArgumentType.getString(ctx, "name");
                    FieldDefinition def = FieldRegistry.get(currentProfile);
                    if (def != null && FieldProfileStore.save(name, def)) {
                        CommandFeedback.success(ctx.getSource(), "Saved: " + name);
                        return 1;
                    }
                    CommandFeedback.error(ctx.getSource(), "Failed to save");
                    return 0;
                }));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildLoadCommand() {
        return CommandManager.literal("load")
            .then(CommandManager.argument("name", StringArgumentType.word())
                .suggests(SAVED_SUGGESTIONS)
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    if (FieldProfileStore.loadAndRegister(name)) {
                        CommandFeedback.success(ctx.getSource(), "Loaded: " + name);
                        return 1;
                    }
                    CommandFeedback.notFound(ctx.getSource(), "Profile", name);
                    return 0;
                }));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildSavedCommand() {
        return CommandManager.literal("saved")
            .executes(ctx -> {
                List<String> saved = FieldProfileStore.list();
                if (saved.isEmpty()) {
                    CommandFeedback.info(ctx.getSource(), "No saved profiles");
                    return 1;
                }
                return ListFormatter.<String>create("Saved Profiles")
                    .showCount(true)
                    .items(saved, name -> ListFormatter.entry(name).tag("custom", Formatting.GOLD))
                    .send(ctx.getSource());
            });
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildDeleteCommand() {
        return CommandManager.literal("delete")
            .then(CommandManager.argument("name", StringArgumentType.word())
                .suggests(SAVED_SUGGESTIONS)
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    if (FieldProfileStore.delete(name)) {
                        CommandFeedback.success(ctx.getSource(), "Deleted: " + name);
                        return 1;
                    }
                    CommandFeedback.error(ctx.getSource(), "Failed to delete");
                    return 0;
                }));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC & UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void syncToClients(ServerCommandSource source) {
        if (currentTestFieldId == -1) return;
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return;
        
        var world = (ServerWorld) player.getWorld();
        var manager = FieldManager.get(world);
        var instance = manager.getInstance(currentTestFieldId);
        
        if (instance == null) {
            currentTestFieldId = -1;
            return;
        }
        
        instance.setScale(transformScale);
        instance.setAlpha(appearanceAlpha);
        
        FieldNetworking.sendUpdateFull(world, instance,
            shuffleType.id(), shuffleIndex,
            followMode.id(), predictionEnabled,
            predictionLeadTicks, predictionMaxDistance,
            predictionLookAhead, predictionVerticalBoost);
    }
    
    private static void rebuildProfileOrder() {
        PROFILE_ORDER.clear();
        FieldRegistry.all().forEach(def -> PROFILE_ORDER.add(def.id()));
        PROFILE_ORDER.sort(String::compareTo);
        currentCycleIndex = 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static String getCurrentProfile() { return currentProfile; }
    public static int getCurrentLayerIndex() { return currentLayerIndex; }
    public static float getShapeRadius() { return shapeRadius; }
    public static float getAnimationSpin() { return animationSpin; }
    public static float getAppearanceAlpha() { return appearanceAlpha; }
    public static float getAppearanceGlow() { return appearanceGlow; }
    public static float getTransformScale() { return transformScale; }
    public static int getShapeLatSteps() { return shapeLatSteps; }
    public static int getShapeLonSteps() { return shapeLonSteps; }
    public static String getShapeAlgorithm() { return shapeAlgorithm; }
    public static String getAppearanceColor() { return appearanceColor; }
    public static VertexPattern getEditPattern() { return editPattern; }
    public static boolean hasActiveTestField() { return currentProfile != null; }
    public static FollowMode getFollowMode() { return followMode; }
    public static boolean isPredictionEnabled() { return predictionEnabled; }
    
    // Compatibility aliases
    public static float getEditRadius() { return shapeRadius; }
    public static float getEditSpin() { return animationSpin; }
    public static float getEditAlpha() { return appearanceAlpha; }
    public static float getEditGlow() { return appearanceGlow; }
    public static float getEditScale() { return transformScale; }
    public static int getEditLatSteps() { return shapeLatSteps; }
    public static int getEditLonSteps() { return shapeLonSteps; }
    public static String getEditAlgorithm() { return shapeAlgorithm; }
    public static String getEditColor() { return appearanceColor; }
}
