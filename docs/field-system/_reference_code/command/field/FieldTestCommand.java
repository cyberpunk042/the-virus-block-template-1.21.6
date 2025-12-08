package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.command.util.*;
import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.FieldProfileStore;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.FieldNetworking;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.SegmentPattern;
import net.cyberpunk042.visual.pattern.SectorPattern;
import net.cyberpunk042.visual.pattern.EdgePattern;
import net.cyberpunk042.visual.pattern.ShuffleGenerator;
import net.cyberpunk042.visual.pattern.DynamicQuadPattern;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug commands for testing field definitions with LIVE editing.
 * Uses CommandKnob, ListFormatter, ReportBuilder for beautiful output.
 *
 * <h2>Commands</h2>
 * <pre>
 * /fieldtest spawn <id>           - Spawn field at player position
 * /fieldtest list [filter]        - List all definitions (clickable)
 * /fieldtest info <id>            - Show detailed profile info
 * /fieldtest cycle                - Cycle to next profile
 * /fieldtest clear                - Clear all test fields
 * /fieldtest categories           - Show profile categories
 * /fieldtest status               - Show current edit state
 * 
 * Auto-Refresh (Watch Mode):
 * /fieldtest watch                - Show watch status
 * /fieldtest watch on             - Enable auto-refresh (detects JSON changes)
 * /fieldtest watch off            - Disable auto-refresh
 * /fieldtest reload               - Manual reload of all definitions
 * 
 * Profile Save/Load:
 * /fieldtest save <name>          - Save active profile to disk
 * /fieldtest load <name>          - Load saved profile
 * /fieldtest saved                - List saved profiles
 * /fieldtest delete <name>        - Delete saved profile
 * 
 * Triangle Patterns:
 * /fieldtest pattern <type>       - Set triangle pattern (default, meshed, facing, etc.)
 * 
 * Live Editing:
 * /fieldtest edit radius <value>
 * /fieldtest edit spin <value>
 * /fieldtest edit alpha <value>
 * /fieldtest edit glow <value>
 * /fieldtest edit scale <value>
 * /fieldtest edit lat <value>
 * /fieldtest edit lon <value>
 * /fieldtest edit layer <idx>
 * /fieldtest edit color <ref>
 * /fieldtest edit reset
 * 
 * Algorithm Selection:
 * /fieldtest algo lat_lon|type_a|type_e
 * </pre>
 * 
 * <h2>Watch Mode Usage</h2>
 * <ol>
 *   <li>Spawn a field: /fieldtest spawn alpha_antivirus</li>
 *   <li>Enable watch: /fieldtest watch on</li>
 *   <li>Edit the JSON file (e.g., alpha_antivirus.json)</li>
 *   <li>Changes appear automatically every second!</li>
 * </ol>
 */
public final class FieldTestCommand {

    // =========================================================================
    // State
    // =========================================================================
    
    private static Identifier currentProfile = null;
    private static long currentTestFieldId = -1;
    private static int currentLayerIndex = 0;
    
    // Live edit values
    private static float editRadius = 5.0f;
    private static float editSpin = 0.02f;
    private static float editAlpha = 0.6f;
    private static float editGlow = 0.3f;
    private static float editScale = 1.0f;
    private static int editLatSteps = 32;
    private static int editLonSteps = 64;
    private static String editAlgorithm = "lat_lon";
    private static String editColor = "@primary";
    
    // Auto-refresh state
    private static boolean autoRefreshEnabled = false;
    private static int autoRefreshInterval = 20; // ticks (1 second)
    private static int ticksSinceRefresh = 0;
    private static long lastDefinitionHash = 0;
    
    // Vertex pattern (supports all geometry types)
    private static VertexPattern editPattern = QuadPattern.DEFAULT;
    
    // Shuffle exploration state
    private static ShuffleType shuffleType = ShuffleType.QUAD;
    private static int shuffleIndex = 0;
    
    // Follow mode and prediction state
    private static FollowMode followMode = FollowMode.SNAP;
    private static boolean predictionEnabled = true;
    private static int predictionLeadTicks = 2;
    private static float predictionMaxDistance = 8.0f;
    private static float predictionLookAhead = 0.5f;
    private static float predictionVerticalBoost = 0.0f;
    
    // FollowMode is now in net.cyberpunk042.field.instance.FollowMode
    
    public enum ShuffleType {
        QUAD("quad", "Quad patterns (spheres/prisms)"),
        SEGMENT("segment", "Segment patterns (rings)"),
        SECTOR("sector", "Sector patterns (discs)"),
        EDGE("edge", "Edge patterns (wireframes)");
        
        private final String id;
        private final String description;
        
        ShuffleType(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public String id() { return id; }
        public String description() { return description; }
        
        public static ShuffleType fromId(String id) {
            for (ShuffleType t : values()) {
                if (t.id.equalsIgnoreCase(id)) return t;
            }
            return QUAD;
        }
        
        public int count() {
            return switch (this) {
                case QUAD -> ShuffleGenerator.quadCount();
                case SEGMENT -> ShuffleGenerator.segmentCount();
                case SECTOR -> ShuffleGenerator.sectorCount();
                case EDGE -> ShuffleGenerator.edgeCount();
            };
        }
    }
    
    // Profile cycling
    private static final List<Identifier> PROFILE_ORDER = new ArrayList<>();
    private static final AtomicInteger CURRENT_INDEX = new AtomicInteger(0);

    private FieldTestCommand() {}

    // =========================================================================
    // Suggestions (Full Autocomplete)
    // =========================================================================
    
    private static final SuggestionProvider<ServerCommandSource> DEFINITION_SUGGESTIONS = (ctx, builder) -> {
        String input = builder.getRemaining().toLowerCase();
        
        // Categorize definitions by geometry type
        java.util.Map<CellType, java.util.List<String>> byGeometry = new java.util.EnumMap<>(CellType.class);
        java.util.List<String> other = new java.util.ArrayList<>();
        
        for (FieldDefinition def : FieldRegistry.all()) {
            String path = def.id().getPath();
            if (!path.toLowerCase().contains(input)) continue;
            
            CellType geo = detectGeometry(def);
            if (geo != null) {
                byGeometry.computeIfAbsent(geo, k -> new java.util.ArrayList<>()).add(path);
            } else {
                other.add(path);
            }
        }
        
        // Add with headers
        if (!byGeometry.getOrDefault(CellType.QUAD, java.util.List.of()).isEmpty()) {
            builder.suggest("--spheres/prisms--", Text.literal("═══ Spheres/Prisms/Polyhedrons ═══").formatted(Formatting.GOLD));
            for (String path : byGeometry.get(CellType.QUAD)) {
                builder.suggest(path, Text.literal("[quad] " + path));
            }
        }
        if (!byGeometry.getOrDefault(CellType.SEGMENT, java.util.List.of()).isEmpty()) {
            builder.suggest("--rings--", Text.literal("═══ Rings ═══").formatted(Formatting.AQUA));
            for (String path : byGeometry.get(CellType.SEGMENT)) {
                builder.suggest(path, Text.literal("[ring] " + path));
            }
        }
        if (!byGeometry.getOrDefault(CellType.SECTOR, java.util.List.of()).isEmpty()) {
            builder.suggest("--discs--", Text.literal("═══ Discs ═══").formatted(Formatting.GREEN));
            for (String path : byGeometry.get(CellType.SECTOR)) {
                builder.suggest(path, Text.literal("[disc] " + path));
            }
        }
        if (!byGeometry.getOrDefault(CellType.EDGE, java.util.List.of()).isEmpty()) {
            builder.suggest("--cages--", Text.literal("═══ Cages/Wireframes ═══").formatted(Formatting.LIGHT_PURPLE));
            for (String path : byGeometry.get(CellType.EDGE)) {
                builder.suggest(path, Text.literal("[cage] " + path));
            }
        }
        if (!other.isEmpty()) {
            builder.suggest("--other--", Text.literal("═══ Other/Multi-Layer ═══").formatted(Formatting.GRAY));
            for (String path : other) {
                builder.suggest(path);
            }
        }
        
        return builder.buildFuture();
    };
    
    /**
     * Detects the primary geometry type for a field definition.
     */
    private static CellType detectGeometry(FieldDefinition def) {
        if (def == null || def.layers().isEmpty()) {
            return null;
        }
        
        for (var layer : def.layers()) {
            for (var primitive : layer.primitives()) {
                String type = primitive.type();
                return switch (type) {
                    case "sphere", "prism", "polyhedron", "stripes" -> CellType.QUAD;
                    case "ring", "rings" -> CellType.SEGMENT;
                    case "disc" -> CellType.SECTOR;
                    case "cage", "beam" -> CellType.EDGE;
                    default -> null;
                };
            }
        }
        return null;
    }
    
    private static final SuggestionProvider<ServerCommandSource> ALGORITHM_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("lat_lon", Text.literal("Lat/Lon tessellation (patterns, partial spheres)"));
        builder.suggest("type_a", Text.literal("Overlapping cubes (accurate, close-up)"));
        builder.suggest("type_e", Text.literal("Rotated rectangles (efficient, LOD)"));
        return builder.buildFuture();
    };
    
    private static final SuggestionProvider<ServerCommandSource> COLOR_SUGGESTIONS = (ctx, builder) -> {
        // Theme colors
        builder.suggest("@primary", Text.literal("Primary theme color"));
        builder.suggest("@secondary", Text.literal("Secondary theme color"));
        builder.suggest("@glow", Text.literal("Glow/emissive color"));
        builder.suggest("@accent", Text.literal("Accent color"));
        builder.suggest("@beam", Text.literal("Beam color"));
        builder.suggest("@wire", Text.literal("Wireframe color"));
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
    
    // Vertex pattern suggestions - filtered by current field's geometry type
    private static final SuggestionProvider<ServerCommandSource> VERTEX_SUGGESTIONS = (ctx, builder) -> {
        // Determine which geometry types are in the current field
        CellType activeGeometry = detectActiveGeometry();
        
        if (activeGeometry == null) {
            // No field spawned - show all with headers
            builder.suggest("--quads--", Text.literal("═══ Quad (Spheres/Prisms) ═══").formatted(Formatting.GOLD));
            for (String id : QuadPattern.ids()) {
                builder.suggest(id, Text.literal("[quad] " + id));
            }
            builder.suggest("--segments--", Text.literal("═══ Segment (Rings) ═══").formatted(Formatting.AQUA));
            for (String id : SegmentPattern.ids()) {
                builder.suggest(id, Text.literal("[segment] " + id));
            }
            builder.suggest("--sectors--", Text.literal("═══ Sector (Discs) ═══").formatted(Formatting.GREEN));
            for (String id : SectorPattern.ids()) {
                builder.suggest(id, Text.literal("[sector] " + id));
            }
            builder.suggest("--edges--", Text.literal("═══ Edge (Cages) ═══").formatted(Formatting.LIGHT_PURPLE));
            for (String id : EdgePattern.ids()) {
                builder.suggest(id, Text.literal("[edge] " + id));
            }
        } else {
            // Show only patterns matching current geometry
            switch (activeGeometry) {
                case QUAD -> {
                    for (String id : QuadPattern.ids()) {
                        builder.suggest(id);
                    }
                }
                case SEGMENT -> {
                    for (String id : SegmentPattern.ids()) {
                        builder.suggest(id);
                    }
                }
                case SECTOR -> {
                    for (String id : SectorPattern.ids()) {
                        builder.suggest(id);
                    }
                }
                case EDGE -> {
                    for (String id : EdgePattern.ids()) {
                        builder.suggest(id);
                    }
                }
                default -> {
                    // Show all
                    for (String id : QuadPattern.ids()) builder.suggest(id);
                    for (String id : SegmentPattern.ids()) builder.suggest(id);
                    for (String id : SectorPattern.ids()) builder.suggest(id);
                    for (String id : EdgePattern.ids()) builder.suggest(id);
                }
            }
        }
        return builder.buildFuture();
    };
    
    /**
     * Detects what geometry type the currently spawned field uses.
     * Returns null if no field is spawned.
     */
    private static CellType detectActiveGeometry() {
        if (currentProfile == null) {
            return null;
        }
        
        FieldDefinition def = FieldRegistry.get(currentProfile);
        if (def == null || def.layers().isEmpty()) {
            return null;
        }
        
        // Check the first primitive to determine geometry type
        for (var layer : def.layers()) {
            for (var primitive : layer.primitives()) {
                String type = primitive.type();
                return switch (type) {
                    case "sphere", "prism", "polyhedron", "stripes" -> CellType.QUAD;
                    case "ring", "rings" -> CellType.SEGMENT;
                    case "disc" -> CellType.SECTOR;
                    case "cage", "beam" -> CellType.EDGE;
                    default -> null;
                };
            }
        }
        return null;
    }
    
    private static final SuggestionProvider<ServerCommandSource> SAVED_PROFILE_SUGGESTIONS = (ctx, builder) -> {
        for (String name : FieldProfileStore.list()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    // =========================================================================
    // Registration
    // =========================================================================

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> cmd = CommandManager.literal("fieldtest")
            .requires(source -> source.hasPermissionLevel(2))
            
            // Core commands
            .then(CommandManager.literal("spawn")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .suggests(DEFINITION_SUGGESTIONS)
                    .executes(ctx -> handleSpawn(ctx, StringArgumentType.getString(ctx, "id")))))
            
            .then(CommandManager.literal("list")
                .executes(ctx -> handleList(ctx.getSource(), null))
                .then(CommandManager.argument("filter", StringArgumentType.word())
                    .executes(ctx -> handleList(ctx.getSource(), StringArgumentType.getString(ctx, "filter")))))
            
            .then(CommandManager.literal("info")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .suggests(DEFINITION_SUGGESTIONS)
                    .executes(ctx -> handleInfo(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            
            .then(CommandManager.literal("cycle")
                .executes(ctx -> handleCycle(ctx)))
            
            .then(CommandManager.literal("clear")
                .executes(ctx -> handleClear(ctx.getSource())))
            
            .then(CommandManager.literal("categories")
                .executes(ctx -> handleCategories(ctx.getSource())))
            
            .then(CommandManager.literal("status")
                .executes(ctx -> handleStatus(ctx.getSource())))
            
            // Auto-refresh (watch mode)
            .then(CommandManager.literal("watch")
                .executes(ctx -> handleWatch(ctx.getSource()))
                .then(CommandManager.literal("on")
                    .executes(ctx -> handleWatchToggle(ctx.getSource(), true)))
                .then(CommandManager.literal("off")
                    .executes(ctx -> handleWatchToggle(ctx.getSource(), false))))
            
            // Manual reload
            .then(CommandManager.literal("reload")
                .executes(ctx -> handleReload(ctx.getSource())))
            
            // Profile save/load
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> handleSave(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests(SAVED_PROFILE_SUGGESTIONS)
                    .executes(ctx -> handleLoad(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            
            .then(CommandManager.literal("saved")
                .executes(ctx -> handleListSaved(ctx.getSource())))
            
            .then(CommandManager.literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests(SAVED_PROFILE_SUGGESTIONS)
                    .executes(ctx -> handleDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            
            // Vertex pattern (all geometry types)
            .then(CommandManager.literal("vertex")
                .executes(ctx -> handleVertexList(ctx.getSource()))
                .then(CommandManager.argument("pattern", StringArgumentType.word())
                    .suggests(VERTEX_SUGGESTIONS)
                    .executes(ctx -> handleVertex(ctx.getSource(), StringArgumentType.getString(ctx, "pattern")))))
            
            // Legacy pattern alias
            .then(CommandManager.literal("pattern")
                .then(CommandManager.argument("pattern", StringArgumentType.word())
                    .suggests(VERTEX_SUGGESTIONS)
                    .executes(ctx -> handleVertex(ctx.getSource(), StringArgumentType.getString(ctx, "pattern")))))
            
            // Algorithm selection
            .then(CommandManager.literal("algo")
                .then(CommandManager.argument("algorithm", StringArgumentType.word())
                    .suggests(ALGORITHM_SUGGESTIONS)
                    .executes(ctx -> handleAlgorithm(ctx.getSource(), StringArgumentType.getString(ctx, "algorithm")))))
            
            // Shuffle exploration (cycles through ALL vertex arrangements)
            .then(CommandManager.literal("shuffle")
                .executes(ctx -> handleShuffleStatus(ctx.getSource()))
                .then(CommandManager.literal("next")
                    .executes(ctx -> handleShuffleNext(ctx.getSource())))
                .then(CommandManager.literal("prev")
                    .executes(ctx -> handleShufflePrev(ctx.getSource())))
                .then(CommandManager.literal("jump")
                    .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                        .executes(ctx -> handleShuffleJump(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))))
                .then(CommandManager.literal("type")
                    .then(CommandManager.literal("quad")
                        .executes(ctx -> handleShuffleType(ctx.getSource(), ShuffleType.QUAD)))
                    .then(CommandManager.literal("segment")
                        .executes(ctx -> handleShuffleType(ctx.getSource(), ShuffleType.SEGMENT)))
                    .then(CommandManager.literal("sector")
                        .executes(ctx -> handleShuffleType(ctx.getSource(), ShuffleType.SECTOR)))
                    .then(CommandManager.literal("edge")
                        .executes(ctx -> handleShuffleType(ctx.getSource(), ShuffleType.EDGE))))
                .then(CommandManager.literal("reset")
                    .executes(ctx -> handleShuffleReset(ctx.getSource()))))
            
            // Follow mode (snap/smooth/glide)
            .then(CommandManager.literal("follow")
                .executes(ctx -> handleFollowStatus(ctx.getSource()))
                .then(CommandManager.literal("snap")
                    .executes(ctx -> handleFollowMode(ctx.getSource(), FollowMode.SNAP)))
                .then(CommandManager.literal("smooth")
                    .executes(ctx -> handleFollowMode(ctx.getSource(), FollowMode.SMOOTH)))
                .then(CommandManager.literal("glide")
                    .executes(ctx -> handleFollowMode(ctx.getSource(), FollowMode.GLIDE))))
            
            // Prediction settings
            .then(CommandManager.literal("predict")
                .executes(ctx -> handlePredictionStatus(ctx.getSource()))
                .then(CommandManager.literal("on")
                    .executes(ctx -> handlePredictionToggle(ctx.getSource(), true)))
                .then(CommandManager.literal("off")
                    .executes(ctx -> handlePredictionToggle(ctx.getSource(), false)))
                .then(CommandManager.literal("lead")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0, 60))
                        .executes(ctx -> handlePredictionLead(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks")))))
                .then(CommandManager.literal("max")
                    .then(CommandManager.argument("blocks", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 64))
                        .executes(ctx -> handlePredictionMax(ctx.getSource(), com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "blocks")))))
                .then(CommandManager.literal("look")
                    .then(CommandManager.argument("offset", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-16, 16))
                        .executes(ctx -> handlePredictionLook(ctx.getSource(), com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "offset")))))
                .then(CommandManager.literal("vertical")
                    .then(CommandManager.argument("boost", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-16, 16))
                        .executes(ctx -> handlePredictionVertical(ctx.getSource(), com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "boost"))))))
            
            // Live editing
            .then(buildEditCommands());

        dispatcher.register(cmd);
        Logging.COMMANDS.info("Registered /fieldtest command with full autocomplete + live editing");
        
        rebuildProfileOrder();
    }

    // =========================================================================
    // Live Edit Commands (CommandKnob)
    // =========================================================================
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildEditCommands() {
        var edit = CommandManager.literal("edit");
        
        // Radius
        CommandKnob.floatValue("fieldtest.edit.radius", "Base radius")
            .range(0.5f, 64.0f)
            .unit("blocks")
            .defaultValue(5.0f)
            .handler((src, v) -> {
                editRadius = v;
                Logging.RENDER.topic("fieldtest").debug("Edit radius → {}", CommandFormatters.formatFloat(v));
                syncTestFieldEdit(src);
                return true;
            })
            .attach(edit);
        
        // Spin speed
        CommandKnob.floatValue("fieldtest.edit.spin", "Spin speed")
            .range(-1.0f, 1.0f)
            .unit("rad/tick")
            .defaultValue(0.02f)
            .handler((src, v) -> {
                editSpin = v;
                Logging.RENDER.topic("fieldtest").debug("Edit spin → {}", CommandFormatters.formatFloat(v));
                syncTestFieldEdit(src);
                return true;
            })
            .attach(edit);
        
        // Alpha
        CommandKnob.floatValue("fieldtest.edit.alpha", "Alpha/opacity")
            .range(0.0f, 1.0f)
            .defaultValue(0.6f)
            .handler((src, v) -> {
                editAlpha = v;
                Logging.RENDER.topic("fieldtest").debug("Edit alpha → {}", CommandFormatters.formatFloat(v));
                syncTestFieldEdit(src);
                return true;
            })
            .attach(edit);
        
        // Glow intensity
        CommandKnob.floatValue("fieldtest.edit.glow", "Glow intensity")
            .range(0.0f, 1.0f)
            .defaultValue(0.3f)
            .handler((src, v) -> {
                editGlow = v;
                Logging.RENDER.topic("fieldtest").debug("Edit glow → {}", CommandFormatters.formatFloat(v));
                syncTestFieldEdit(src);
                return true;
            })
            .attach(edit);
        
        // Visual scale
        CommandKnob.floatValue("fieldtest.edit.scale", "Visual scale")
            .range(0.1f, 10.0f)
            .defaultValue(1.0f)
            .handler((src, v) -> {
                editScale = v;
                Logging.RENDER.topic("fieldtest").debug("Edit scale → {}", CommandFormatters.formatFloat(v));
                syncTestFieldEdit(src);
                return true;
            })
            .attach(edit);
        
        // Latitude steps
        CommandKnob.value("fieldtest.edit.lat", "Latitude steps")
            .range(4, 128)
            .defaultValue(32)
            .handler((src, v) -> {
                editLatSteps = v;
                Logging.RENDER.topic("fieldtest").debug("Edit latSteps → {}", v);
                return true;
            })
            .attach(edit);
        
        // Longitude steps
        CommandKnob.value("fieldtest.edit.lon", "Longitude steps")
            .range(8, 256)
            .defaultValue(64)
            .handler((src, v) -> {
                editLonSteps = v;
                Logging.RENDER.topic("fieldtest").debug("Edit lonSteps → {}", v);
                return true;
            })
            .attach(edit);
        
        // Layer selection
        CommandKnob.value("fieldtest.edit.layer", "Active layer")
            .range(0, 10)
            .defaultValue(0)
            .handler((src, v) -> {
                currentLayerIndex = v;
                Logging.RENDER.topic("fieldtest").debug("Edit layer → {}", v);
                CommandFeedback.highlight(src, "Now editing layer " + v);
                return true;
            })
            .attach(edit);
        
        // Color
        edit.then(CommandManager.literal("color")
            .then(CommandManager.argument("value", StringArgumentType.greedyString())
                .suggests(COLOR_SUGGESTIONS)
                .executes(ctx -> {
                    String color = StringArgumentType.getString(ctx, "value");
                    editColor = color;
                    Logging.RENDER.topic("fieldtest").debug("Edit color → {}", color);
                    CommandFeedback.valueSet(ctx.getSource(), "Layer color", color);
                    return 1;
                })));
        
        // Reset
        CommandKnob.action("fieldtest.edit.reset", "Reset to defaults")
            .successMessage("Edit values reset to defaults")
            .handler(src -> {
                resetEditValues();
                Logging.RENDER.topic("fieldtest").info("Edit values reset");
                return true;
            })
            .attach(edit);
        
        return edit;
    }

    // =========================================================================
    // Handlers - Using ListFormatter & ReportBuilder
    // =========================================================================

    private static int handleSpawn(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource source = ctx.getSource();
        
        // Auto-despawn previous test field if exists
        if (currentTestFieldId != -1 && source.getEntity() instanceof ServerPlayerEntity player) {
            FieldManager.get((net.minecraft.server.world.ServerWorld) player.getWorld()).remove(currentTestFieldId);
            Logging.COMMANDS.topic("fieldtest").debug("Auto-despawned previous test field: {}", currentTestFieldId);
            currentTestFieldId = -1;
        }
        
        FieldDefinition def = findDefinition(id);
        if (def == null) {
            CommandFeedback.notFound(source, "Field definition", id);
            CommandFeedback.info(source, "Use /fieldtest list to see available definitions");
            return 0;
        }
        
        currentProfile = def.id();
        editRadius = def.baseRadius();
        if (def.modifiers() != null) {
            editScale = def.modifiers().visualScale();
        }
        
        Logging.COMMANDS.topic("fieldtest").info(
            "Spawning field '{}' at {}", def.id(), source.getPosition());
        
        // Beautiful report output
        ReportBuilder.create("Field Spawned: " + def.id().getPath())
            .kv("Type", def.type().id(), Formatting.AQUA)
            .kv("Base Radius", CommandFormatters.formatFloat(def.baseRadius()) + " blocks", Formatting.GREEN)
            .kv("Theme", def.themeId() != null ? def.themeId() : "default", Formatting.LIGHT_PURPLE)
            .kv("Layers", def.layers().size(), Formatting.YELLOW)
            .section("Edit Commands", s -> s
                .line("/fieldtest edit radius <value>")
                .line("/fieldtest edit spin <value>")
                .line("/fieldtest edit alpha <value>")
                .line("/fieldtest edit layer <idx>"))
            .send(source);
        
        // Spawn via FieldManager (automatically syncs to all players)
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d pos = player.getPos().add(0, 1.0, 0);
            var instance = FieldManager.get((net.minecraft.server.world.ServerWorld) player.getWorld()).spawnAt(
                def.id(), pos, editRadius * editScale, -1);
            if (instance != null) {
                currentTestFieldId = instance.id();
                Logging.COMMANDS.topic("fieldtest").info(
                    "Spawned test field via FieldManager: id={}, def={}, pos={}", 
                    instance.id(), def.id(), pos);
            }
        }
        
        CommandFeedback.highlight(source, "Field now rendering at your position!");
        return 1;
    }

    private static int handleList(ServerCommandSource source, String filter) {
        List<FieldDefinition> defs = new ArrayList<>();
        FieldRegistry.all().forEach(defs::add);
        
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            defs.removeIf(d -> !d.id().getPath().toLowerCase().contains(lowerFilter));
        }
        
        defs.sort((a, b) -> a.id().getPath().compareTo(b.id().getPath()));
        
        String header = filter != null 
            ? "Field definitions matching '" + filter + "'"
            : "Field definitions";
        
        return ListFormatter.<FieldDefinition>create(header)
            .emptyMessage("No definitions found" + (filter != null ? " matching '" + filter + "'" : ""))
            .showCount(true)
            .items(defs, def -> {
                String path = def.id().getPath();
                var entry = ListFormatter.entry(path)
                    .color(getColorForPrefix(path));
                
                // Add tags based on type
                entry.tag(def.type().id(), Formatting.DARK_AQUA);
                
                // Add layer count
                entry.tag(def.layers().size() + " layers", Formatting.GRAY);
                
                // Mark current
                if (def.id().equals(currentProfile)) {
                    entry.tag("ACTIVE", Formatting.GREEN);
                }
                
                return entry;
            })
            .send(source);
    }

    private static int handleInfo(ServerCommandSource source, String id) {
        FieldDefinition def = findDefinition(id);
        if (def == null) {
            CommandFeedback.notFound(source, "Field definition", id);
            return 0;
        }
        
        var report = ReportBuilder.create("Field: " + def.id().getPath())
            .kv("Type", def.type().id(), Formatting.AQUA)
            .kv("Base Radius", CommandFormatters.formatFloat(def.baseRadius()) + " blocks", Formatting.GREEN)
            .kv("Theme", def.themeId() != null ? def.themeId() : "default", Formatting.LIGHT_PURPLE);
        
        // Modifiers section
        if (def.modifiers() != null) {
            report.section("Modifiers", s -> {
                var m = def.modifiers();
                if (m.visualScale() != 1.0f) s.kv("visualScale", CommandFormatters.formatFloat(m.visualScale()));
                if (m.radiusMultiplier() != 1.0f) s.kv("radiusMultiplier", CommandFormatters.formatFloat(m.radiusMultiplier()));
                if (m.spinMultiplier() != 1.0f) s.kv("spinMultiplier", CommandFormatters.formatFloat(m.spinMultiplier()));
                if (m.pulsing()) s.kv("pulsing", "true", Formatting.YELLOW);
            });
        }
        
        // Layers section
        report.section("Layers (" + def.layers().size() + ")", s -> {
            int idx = 0;
            for (FieldLayer layer : def.layers()) {
                int i = idx++;
                String primitiveInfo = layer.primitives().size() + " primitives";
                if (!layer.primitives().isEmpty()) {
                    primitiveInfo = layer.primitives().get(0).type();
                    if (layer.primitives().size() > 1) {
                        primitiveInfo += " +" + (layer.primitives().size() - 1);
                    }
                }
                String layerDesc = "[" + i + "] " + layer.id() + " (" + primitiveInfo + ")";
                s.line(layerDesc, i == currentLayerIndex ? Formatting.GREEN : Formatting.GRAY);
            }
        });
        
        // Prediction
        if (def.prediction() != null && def.prediction().enabled()) {
            report.kv("Prediction", def.prediction().leadTicks() + " ticks", Formatting.AQUA);
        }
        
        report.send(source);
        
        // Hint for spawning
        CommandFeedback.highlight(source, "Spawn with: /fieldtest spawn " + def.id().getPath());
        
        return 1;
    }

    private static int handleStatus(ServerCommandSource source) {
        ReportBuilder.create("Field Test Status")
            .kv("Active Profile", currentProfile != null ? currentProfile.getPath() : "none",
                currentProfile != null ? Formatting.GREEN : Formatting.GRAY)
            .kv("Current Layer", currentLayerIndex, Formatting.YELLOW)
            .kv("Auto-Refresh", autoRefreshEnabled ? "WATCHING" : "off",
                autoRefreshEnabled ? Formatting.GREEN : Formatting.GRAY)
            .blank()
            .section("Edit Values", s -> s
                .kv("radius", CommandFormatters.formatFloat(editRadius) + " blocks")
                .kv("spin", CommandFormatters.formatFloat(editSpin) + " rad/tick")
                .kv("alpha", CommandFormatters.formatFloat(editAlpha))
                .kv("glow", CommandFormatters.formatFloat(editGlow))
                .kv("scale", CommandFormatters.formatFloat(editScale)))
            .section("Tessellation", s -> s
                .kv("algorithm", editAlgorithm, Formatting.LIGHT_PURPLE)
                .kv("vertexPattern", editPattern.id() + " (" + editPattern.getClass().getSimpleName().replace("Pattern", "").toLowerCase() + ")", Formatting.GOLD)
                .kv("latSteps", editLatSteps)
                .kv("lonSteps", editLonSteps))
            .kv("Color", editColor, Formatting.AQUA)
            .send(source);
        
        if (autoRefreshEnabled) {
            CommandFeedback.highlight(source, "Watching for JSON changes...");
        }
        
        return 1;
    }

    private static int handleCategories(ServerCommandSource source) {
        source.sendFeedback(() -> CommandFeedback.header("Profile Categories"), false);
        
        String[][] categories = {
            {"alpha_", "Alpha (converted)", "GOLD"},
            {"perf_", "Performance (TYPE_E)", "GREEN"},
            {"quality_", "Quality (TYPE_A)", "AQUA"},
            {"effect_", "Creative Effects", "LIGHT_PURPLE"}
        };
        
        for (String[] cat : categories) {
            String prefix = cat[0];
            String desc = cat[1];
            Formatting color = Formatting.byName(cat[2]);
            
            long count = FieldRegistry.all().stream()
                .filter(d -> d.id().getPath().startsWith(prefix))
                .count();
            
            source.sendFeedback(() -> Text.literal("  • " + desc + " ")
                .formatted(color)
                .append(Text.literal("(" + count + ")")
                    .formatted(Formatting.GRAY))
                .append(Text.literal(" → /fieldtest list " + prefix)
                    .formatted(Formatting.DARK_GRAY)), false);
        }
        
        return 1;
    }

    private static int handleCycle(CommandContext<ServerCommandSource> ctx) {
        if (PROFILE_ORDER.isEmpty()) {
            rebuildProfileOrder();
        }
        
        if (PROFILE_ORDER.isEmpty()) {
            CommandFeedback.error(ctx.getSource(), "No profiles available");
            return 0;
        }
        
        int idx = CURRENT_INDEX.getAndIncrement() % PROFILE_ORDER.size();
        Identifier next = PROFILE_ORDER.get(idx);
        
        CommandFeedback.highlight(ctx.getSource(), 
            "Cycling [" + (idx + 1) + "/" + PROFILE_ORDER.size() + "]: " + next.getPath());
        
        return handleSpawn(ctx, next.getPath());
    }

    private static int handleClear(ServerCommandSource source) {
        // Remove the test field via FieldManager
        if (currentTestFieldId != -1 && source.getEntity() instanceof ServerPlayerEntity player) {
            FieldManager.get((net.minecraft.server.world.ServerWorld) player.getWorld()).remove(currentTestFieldId);
            currentTestFieldId = -1;
        }
        
        currentProfile = null;
        resetEditValues();
        
        CommandFeedback.success(source, "Test field cleared, edit values reset");
        Logging.COMMANDS.topic("fieldtest").info("Cleared test field");
        return 1;
    }

    private static int handleAlgorithm(ServerCommandSource source, String algo) {
        if (!algo.equals("lat_lon") && !algo.equals("type_a") && !algo.equals("type_e")) {
            CommandFeedback.invalidValue(source, "algorithm", algo);
            CommandFeedback.info(source, "Valid: lat_lon, type_a, type_e");
            return 0;
        }
        editAlgorithm = algo;
        Logging.RENDER.topic("fieldtest").debug("Algorithm → {}", algo);
        CommandFeedback.valueSet(source, "Sphere algorithm", algo);
        return 1;
    }
    
    private static int handleWatch(ServerCommandSource source) {
        ReportBuilder.create("Auto-Refresh Status")
            .kv("Enabled", autoRefreshEnabled ? "ON" : "OFF", 
                autoRefreshEnabled ? Formatting.GREEN : Formatting.RED)
            .kv("Interval", autoRefreshInterval + " ticks (" + (autoRefreshInterval / 20.0f) + "s)", Formatting.AQUA)
            .blank()
            .line("Commands:", Formatting.YELLOW)
            .line("  /fieldtest watch on  - Enable auto-refresh")
            .line("  /fieldtest watch off - Disable auto-refresh")
            .line("  /fieldtest reload    - Manual reload")
            .send(source);
        return 1;
    }
    
    private static int handleWatchToggle(ServerCommandSource source, boolean enable) {
        autoRefreshEnabled = enable;
        ticksSinceRefresh = 0;
        
        if (enable) {
            // Calculate initial hash to detect changes
            if (currentProfile != null) {
                FieldDefinition def = FieldRegistry.get(currentProfile);
                if (def != null) {
                    lastDefinitionHash = computeDefinitionHash(def);
                }
            }
            CommandFeedback.success(source, "Auto-refresh ENABLED - watching for JSON changes");
            CommandFeedback.highlight(source, "Edit your JSON files, changes will appear automatically!");
            Logging.COMMANDS.topic("fieldtest").info("Auto-refresh enabled");
        } else {
            CommandFeedback.info(source, "Auto-refresh disabled");
            Logging.COMMANDS.topic("fieldtest").info("Auto-refresh disabled");
        }
        return 1;
    }
    
    private static int handleReload(ServerCommandSource source) {
        CommandFeedback.info(source, "Reloading field definitions...");
        Logging.COMMANDS.topic("fieldtest").info("Manual reload triggered");
        
        long startTime = System.currentTimeMillis();
        
        // Trigger hot-reload through config registry
        InfectionConfigRegistry.loadCommon();
        rebuildProfileOrder();
        
        long elapsed = System.currentTimeMillis() - startTime;
        int count = 0;
        for (FieldDefinition ignored : FieldRegistry.all()) {
            count++;
        }
        
        CommandFeedback.success(source, 
            "Reloaded " + count + " definitions in " + elapsed + "ms");
        
        // Re-sync current profile if active
        if (currentProfile != null) {
            FieldDefinition def = FieldRegistry.get(currentProfile);
            if (def != null) {
                lastDefinitionHash = computeDefinitionHash(def);
                CommandFeedback.highlight(source, "Active profile refreshed: " + currentProfile.getPath());
            }
        }
        
        return 1;
    }
    
    // =========================================================================
    // Profile Save/Load Handlers
    // =========================================================================
    
    private static int handleSave(ServerCommandSource source, String name) {
        if (currentProfile == null) {
            CommandFeedback.error(source, "No active profile to save");
            CommandFeedback.info(source, "First spawn a profile with /fieldtest spawn <id>");
            return 0;
        }
        
        FieldDefinition def = FieldRegistry.get(currentProfile);
        if (def == null) {
            CommandFeedback.error(source, "Active profile not found in registry");
            return 0;
        }
        
        if (FieldProfileStore.save(name, def)) {
            CommandFeedback.success(source, "Saved profile: " + name);
            CommandFeedback.highlight(source, "Location: " + FieldProfileStore.getProfileDirectory().resolve(name + ".json"));
            Logging.COMMANDS.topic("fieldtest").info("Saved profile {} from {}", name, currentProfile);
            return 1;
        } else {
            CommandFeedback.error(source, "Failed to save profile");
            return 0;
        }
    }
    
    private static int handleLoad(ServerCommandSource source, String name) {
        if (!FieldProfileStore.exists(name)) {
            CommandFeedback.notFound(source, "Saved profile", name);
            CommandFeedback.info(source, "Use /fieldtest saved to list available profiles");
            return 0;
        }
        
        if (FieldProfileStore.loadAndRegister(name)) {
            Identifier customId = Identifier.of("the-virus-block", "custom/" + FieldProfileStore.sanitizeName(name));
            currentProfile = customId;
            rebuildProfileOrder();
            
            CommandFeedback.success(source, "Loaded profile: " + name);
            CommandFeedback.highlight(source, "Profile is now active!");
            Logging.COMMANDS.topic("fieldtest").info("Loaded custom profile {}", name);
            return 1;
        } else {
            CommandFeedback.error(source, "Failed to load profile");
            return 0;
        }
    }
    
    private static int handleListSaved(ServerCommandSource source) {
        List<String> saved = FieldProfileStore.list();
        
        if (saved.isEmpty()) {
            CommandFeedback.info(source, "No saved profiles");
            CommandFeedback.highlight(source, "Save the active profile with: /fieldtest save <name>");
            return 1;
        }
        
        return ListFormatter.<String>create("Saved Profiles")
            .showCount(true)
            .items(saved, name -> ListFormatter.entry(name)
                .color(Formatting.GOLD)
                .tag("custom", Formatting.DARK_AQUA))
            .send(source);
    }
    
    private static int handleDelete(ServerCommandSource source, String name) {
        if (!FieldProfileStore.exists(name)) {
            CommandFeedback.notFound(source, "Saved profile", name);
            return 0;
        }
        
        if (FieldProfileStore.delete(name)) {
            CommandFeedback.success(source, "Deleted profile: " + name);
            Logging.COMMANDS.topic("fieldtest").info("Deleted profile {}", name);
            return 1;
        } else {
            CommandFeedback.error(source, "Failed to delete profile");
            return 0;
        }
    }
    
    private static int handleVertex(ServerCommandSource source, String patternName) {
        // Skip header suggestions
        if (patternName.startsWith("--")) {
            return handleVertexList(source);
        }
        
        VertexPattern pattern = VertexPattern.fromString(patternName);
        if (pattern == null || pattern == QuadPattern.DEFAULT && !patternName.equalsIgnoreCase("default")) {
            CommandFeedback.invalidValue(source, "vertex pattern", patternName);
            CommandFeedback.info(source, "Use /fieldtest vertex to see all available patterns");
            return 0;
        }
        
        editPattern = pattern;
        
        // For static patterns, we need to set the shuffle state to match
        // so the sync mechanism sends the pattern correctly
        if (pattern instanceof QuadPattern) {
            shuffleType = ShuffleType.QUAD;
            // Find the index for this static pattern (or use -1 for static)
            shuffleIndex = -1; // -1 indicates static pattern
        } else if (pattern instanceof SegmentPattern) {
            shuffleType = ShuffleType.SEGMENT;
            shuffleIndex = -1;
        } else if (pattern instanceof SectorPattern) {
            shuffleType = ShuffleType.SECTOR;
            shuffleIndex = -1;
        } else if (pattern instanceof EdgePattern) {
            shuffleType = ShuffleType.EDGE;
            shuffleIndex = -1;
        }
        
        String geometry = pattern.getClass().getSimpleName().replace("Pattern", "").toLowerCase();
        
        Logging.RENDER.topic("fieldtest").debug("VertexPattern → {} (geometry: {})", 
            pattern.id(), geometry);
        
        CommandFeedback.valueSet(source, "Vertex pattern", pattern.id());
        CommandFeedback.info(source, "Applies to: " + geometry + " geometry");
        
        // Sync to clients with the static pattern ID
        syncTestFieldEditWithPattern(source, pattern.id());
        return 1;
    }
    
    private static int handleVertexList(ServerCommandSource source) {
        ReportBuilder.create("Vertex Patterns")
            .section("Quad Patterns (Spheres/Prisms)", s -> {
                for (QuadPattern p : QuadPattern.values()) {
                    s.line("  " + p.id() + " (" + p.getVertexOrder().length + " triangles)", 
                        p == editPattern ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .section("Segment Patterns (Rings)", s -> {
                for (SegmentPattern p : SegmentPattern.values()) {
                    s.line("  " + p.id() + " (skip: " + 1 /* TODO: add skipInterval() to SegmentPattern */ + ")",
                        p == editPattern ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .section("Sector Patterns (Discs)", s -> {
                for (SectorPattern p : SectorPattern.values()) {
                    s.line("  " + p.id(),
                        p == editPattern ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .section("Edge Patterns (Wireframes)", s -> {
                for (EdgePattern p : EdgePattern.values()) {
                    s.line("  " + p.id() + (p.renderLatitude() && p.renderLongitude() ? " (full)" : ""),
                        p == editPattern ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .blank()
            .kv("Current", editPattern.id(), Formatting.GREEN)
            .kv("Geometry", editPattern.getClass().getSimpleName().replace("Pattern", "").toLowerCase(), Formatting.AQUA)
            .send(source);
        
        CommandFeedback.highlight(source, "Set pattern: /fieldtest vertex <pattern>");
        return 1;
    }

    // =========================================================================
    // Shuffle Handlers (Pattern Exploration)
    // =========================================================================
    
    private static int handleShuffleStatus(ServerCommandSource source) {
        int total = shuffleType.count();
        String current = getCurrentShuffleDescription();
        
        ReportBuilder.create("Shuffle Exploration")
            .kv("Type", shuffleType.id(), Formatting.AQUA)
            .kv("Index", (shuffleIndex + 1) + "/" + total, Formatting.GREEN)
            .kv("Current", current, Formatting.YELLOW)
            .blank()
            .section("Commands", s -> s
                .line("/fieldtest shuffle next  - Next arrangement")
                .line("/fieldtest shuffle prev  - Previous arrangement")
                .line("/fieldtest shuffle jump <n> - Jump to #n")
                .line("/fieldtest shuffle type <quad|segment|sector|edge>"))
            .section("Available Types", s -> {
                for (ShuffleType t : ShuffleType.values()) {
                    s.line("  " + t.id() + " (" + t.count() + ") - " + t.description(),
                        t == shuffleType ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .send(source);
        
        return 1;
    }
    
    private static int handleShuffleNext(ServerCommandSource source) {
        shuffleIndex = (shuffleIndex + 1) % shuffleType.count();
        return applyShuffleAndLog(source);
    }
    
    private static int handleShufflePrev(ServerCommandSource source) {
        shuffleIndex = (shuffleIndex - 1 + shuffleType.count()) % shuffleType.count();
        return applyShuffleAndLog(source);
    }
    
    private static int handleShuffleJump(ServerCommandSource source, int index) {
        if (index < 0 || index >= shuffleType.count()) {
            CommandFeedback.error(source, "Index out of range: 0-" + (shuffleType.count() - 1));
            return 0;
        }
        shuffleIndex = index;
        return applyShuffleAndLog(source);
    }
    
    private static int handleShuffleType(ServerCommandSource source, ShuffleType type) {
        shuffleType = type;
        shuffleIndex = 0;
        
        CommandFeedback.success(source, "Switched to " + type.id() + " patterns");
        CommandFeedback.info(source, type.count() + " arrangements available");
        Logging.RENDER.topic("shuffle").info("Switched to {} patterns ({} total)", 
            type.id(), type.count());
        
        return applyShuffleAndLog(source);
    }
    
    private static int handleShuffleReset(ServerCommandSource source) {
        shuffleIndex = 0;
        CommandFeedback.info(source, "Reset to first arrangement");
        return applyShuffleAndLog(source);
    }
    
    private static int applyShuffleAndLog(ServerCommandSource source) {
        int total = shuffleType.count();
        String desc;
        
        // Log detailed arrangement to console
        switch (shuffleType) {
            case QUAD -> {
                var arr = ShuffleGenerator.getQuad(shuffleIndex);
                desc = arr.describe();
                ShuffleGenerator.logQuad(shuffleIndex);
                
                // Apply as active pattern
                editPattern = DynamicQuadPattern.fromArrangement(arr);
            }
            case SEGMENT -> {
                var arr = ShuffleGenerator.getSegment(shuffleIndex);
                desc = arr.describe();
                ShuffleGenerator.logSegment(shuffleIndex);
                // TODO: Apply segment pattern
            }
            case SECTOR -> {
                var arr = ShuffleGenerator.getSector(shuffleIndex);
                desc = arr.describe();
                ShuffleGenerator.logSector(shuffleIndex);
                // TODO: Apply sector pattern
            }
            case EDGE -> {
                var arr = ShuffleGenerator.getEdge(shuffleIndex);
                desc = arr.describe();
                ShuffleGenerator.logEdge(shuffleIndex);
                // TODO: Apply edge pattern
            }
            default -> desc = "unknown";
        }
        
        // Show in chat
        CommandFeedback.highlight(source, 
            String.format("[%s #%d/%d] %s", 
                shuffleType.id().toUpperCase(), 
                shuffleIndex + 1, 
                total, 
                desc));
        
        // Sync to clients
        syncTestFieldEdit(source);
        
        return 1;
    }
    
    private static String getCurrentShuffleDescription() {
        return switch (shuffleType) {
            case QUAD -> ShuffleGenerator.getQuad(shuffleIndex).describe();
            case SEGMENT -> ShuffleGenerator.getSegment(shuffleIndex).describe();
            case SECTOR -> ShuffleGenerator.getSector(shuffleIndex).describe();
            case EDGE -> ShuffleGenerator.getEdge(shuffleIndex).describe();
        };
    }
    
    // =========================================================================
    // Follow Mode Handlers
    // =========================================================================
    
    private static int handleFollowStatus(ServerCommandSource source) {
        ReportBuilder.create("Follow Mode")
            .kv("Mode", followMode.id() + " (" + followMode.description() + ")")
            .kv("Lerp Factor", String.format("%.2f", followMode.lerpFactor()))
            .section("Available Modes", s -> {
                for (FollowMode m : FollowMode.values()) {
                    s.line("  " + m.id() + " - " + m.description(), 
                        m == followMode ? Formatting.GREEN : Formatting.GRAY);
                }
            })
            .send(source);
        return 1;
    }
    
    private static int handleFollowMode(ServerCommandSource source, FollowMode mode) {
        followMode = mode;
        CommandFeedback.valueSet(source, "Follow mode", mode.id());
        CommandFeedback.info(source, mode.description());
        
        Logging.COMMANDS.topic("fieldtest").debug("Follow mode → {} (lerp={})", mode.id(), mode.lerpFactor());
        
        // Sync to clients (if a field is active)
        if (currentTestFieldId != -1) {
            syncTestFieldEdit(source);
            CommandFeedback.info(source, "Applied to active field");
        } else {
            CommandFeedback.info(source, "No active field - will apply on next spawn");
        }
        return 1;
    }
    
    // =========================================================================
    // Prediction Handlers
    // =========================================================================
    
    private static int handlePredictionStatus(ServerCommandSource source) {
        ReportBuilder.create("Prediction Settings")
            .kv("Enabled", predictionEnabled ? "§aYES" : "§cNO")
            .kv("Lead Ticks", predictionLeadTicks + " ticks")
            .kv("Max Distance", String.format("%.1f blocks", predictionMaxDistance))
            .kv("Look Ahead", String.format("%.2f", predictionLookAhead))
            .kv("Vertical Boost", String.format("%.2f", predictionVerticalBoost))
            .section("Usage", s -> {
                s.line("/fieldtest predict on|off - Toggle prediction", Formatting.YELLOW);
                s.line("/fieldtest predict lead <ticks> - Lead time (0-60)", Formatting.YELLOW);
                s.line("/fieldtest predict max <blocks> - Max offset (0-64)", Formatting.YELLOW);
                s.line("/fieldtest predict look <offset> - Look direction offset", Formatting.YELLOW);
                s.line("/fieldtest predict vertical <boost> - Vertical offset", Formatting.YELLOW);
            })
            .send(source);
        return 1;
    }
    
    private static int handlePredictionToggle(ServerCommandSource source, boolean enabled) {
        predictionEnabled = enabled;
        CommandFeedback.toggle(source, "Prediction", enabled);
        
        Logging.COMMANDS.topic("fieldtest").debug("Prediction → {}", enabled);
        syncTestFieldEdit(source);
        return 1;
    }
    
    private static int handlePredictionLead(ServerCommandSource source, int ticks) {
        predictionLeadTicks = ticks;
        CommandFeedback.valueSet(source, "Prediction lead", ticks + " ticks");
        
        Logging.COMMANDS.topic("fieldtest").debug("Prediction lead → {} ticks", ticks);
        syncTestFieldEdit(source);
        return 1;
    }
    
    private static int handlePredictionMax(ServerCommandSource source, float blocks) {
        predictionMaxDistance = blocks;
        CommandFeedback.valueSet(source, "Prediction max distance", String.format("%.1f blocks", blocks));
        
        Logging.COMMANDS.topic("fieldtest").debug("Prediction max → {} blocks", blocks);
        syncTestFieldEdit(source);
        return 1;
    }
    
    private static int handlePredictionLook(ServerCommandSource source, float offset) {
        predictionLookAhead = offset;
        CommandFeedback.valueSet(source, "Prediction look ahead", String.format("%.2f", offset));
        
        Logging.COMMANDS.topic("fieldtest").debug("Prediction look → {}", offset);
        syncTestFieldEdit(source);
        return 1;
    }
    
    private static int handlePredictionVertical(ServerCommandSource source, float boost) {
        predictionVerticalBoost = boost;
        CommandFeedback.valueSet(source, "Prediction vertical boost", String.format("%.2f", boost));
        
        Logging.COMMANDS.topic("fieldtest").debug("Prediction vertical → {}", boost);
        syncTestFieldEdit(source);
        return 1;
    }
    
    /**
     * Applies prediction to a target position based on player movement.
     * Used for personal shields that follow the player.
     */
    public static Vec3d applyPrediction(net.minecraft.entity.player.PlayerEntity player, Vec3d base) {
        if (!predictionEnabled) {
            return base;
        }
        
        // Apply velocity prediction
        double lead = Math.max(0, Math.min(predictionLeadTicks, 60));
        Vec3d predicted = base.add(player.getVelocity().multiply(lead));
        
        // Apply look-ahead offset
        if (Math.abs(predictionLookAhead) > 0.001f) {
            Vec3d look = player.getRotationVector();
            predicted = predicted.add(look.multiply(predictionLookAhead));
        }
        
        // Apply vertical boost
        if (Math.abs(predictionVerticalBoost) > 0.001f) {
            predicted = predicted.add(0.0, predictionVerticalBoost, 0.0);
        }
        
        // Clamp to max distance
        if (predictionMaxDistance > 0) {
            Vec3d delta = predicted.subtract(base);
            double dist = delta.length();
            if (dist > predictionMaxDistance) {
                predicted = base.add(delta.normalize().multiply(predictionMaxDistance));
            }
        }
        
        return predicted;
    }
    
    /**
     * Gets the current follow mode for external use.
     */
    public static FollowMode getFollowMode() {
        return followMode;
    }
    
    /**
     * Gets whether prediction is enabled.
     */
    public static boolean isPredictionEnabled() {
        return predictionEnabled;
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private static FieldDefinition findDefinition(String id) {
        Identifier fullId = Identifier.of("the-virus-block", id);
        FieldDefinition def = FieldRegistry.get(fullId);
        
        if (def == null) {
            for (FieldDefinition d : FieldRegistry.all()) {
                if (d.id().getPath().equals(id)) {
                    return d;
                }
            }
        }
        return def;
    }

    private static void rebuildProfileOrder() {
        PROFILE_ORDER.clear();
        FieldRegistry.all().forEach(def -> PROFILE_ORDER.add(def.id()));
        PROFILE_ORDER.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        CURRENT_INDEX.set(0);
        Logging.COMMANDS.topic("fieldtest").debug("Built profile order: {} definitions", PROFILE_ORDER.size());
    }

    private static void resetEditValues() {
        editRadius = 5.0f;
        editSpin = 0.02f;
        editAlpha = 0.6f;
        editGlow = 0.3f;
        editScale = 1.0f;
        editLatSteps = 32;
        editLonSteps = 64;
        editAlgorithm = "lat_lon";
        editColor = "@primary";
        editPattern = QuadPattern.DEFAULT;
        currentLayerIndex = 0;
    }

    private static Formatting getColorForPrefix(String path) {
        if (path.startsWith("alpha_")) return Formatting.GOLD;
        if (path.startsWith("perf_")) return Formatting.GREEN;
        if (path.startsWith("quality_")) return Formatting.AQUA;
        if (path.startsWith("effect_")) return Formatting.LIGHT_PURPLE;
        return Formatting.WHITE;
    }

    // =========================================================================
    // Auto-Refresh Tick (call from client tick event)
    // =========================================================================
    
    /**
     * Called each client tick to handle auto-refresh.
     * If watch mode is enabled and the current profile's definition has changed,
     * this will trigger a reload.
     */
    public static void tick() {
        if (!autoRefreshEnabled || currentProfile == null) {
            return;
        }
        
        ticksSinceRefresh++;
        if (ticksSinceRefresh < autoRefreshInterval) {
            return;
        }
        ticksSinceRefresh = 0;
        
        // Trigger reload and check for changes
        InfectionConfigRegistry.loadCommon();
        
        FieldDefinition def = FieldRegistry.get(currentProfile);
        if (def == null) {
            return;
        }
        
        long newHash = computeDefinitionHash(def);
        if (newHash != lastDefinitionHash) {
            lastDefinitionHash = newHash;
            rebuildProfileOrder();
            Logging.RENDER.topic("fieldtest").info(
                "Auto-refreshed: {} (definition changed)", currentProfile.getPath());
        }
    }
    
    /**
     * Computes a simple hash of a field definition for change detection.
     */
    private static long computeDefinitionHash(FieldDefinition def) {
        if (def == null) return 0;
        
        long hash = 17;
        hash = 31 * hash + Float.floatToIntBits(def.baseRadius());
        hash = 31 * hash + def.layers().size();
        hash = 31 * hash + (def.themeId() != null ? def.themeId().hashCode() : 0);
        hash = 31 * hash + (def.hasBeam() ? 1 : 0);
        
        if (def.modifiers() != null) {
            hash = 31 * hash + Float.floatToIntBits(def.modifiers().visualScale());
            hash = 31 * hash + Float.floatToIntBits(def.modifiers().radiusMultiplier());
        }
        
        for (FieldLayer layer : def.layers()) {
            hash = 31 * hash + layer.id().hashCode();
            hash = 31 * hash + layer.primitives().size();
        }
        
        return hash;
    }

    
    // =========================================================================
    // Live Edit Sync
    // =========================================================================
    
    /**
     * Syncs current edit values to all clients via FieldManager update.
     * Called after any edit knob changes value.
     */
    private static void syncTestFieldEdit(ServerCommandSource source) {
        if (currentTestFieldId == -1) {
            return;
        }
        
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }
        
        var world = (net.minecraft.server.world.ServerWorld) player.getWorld();
        var manager = FieldManager.get(world);
        var instance = manager.getInstance(currentTestFieldId);
        
        if (instance == null) {
            Logging.COMMANDS.topic("fieldtest").warn("Test field {} not found in manager", currentTestFieldId);
            currentTestFieldId = -1;
            return;
        }
        
        // Update instance with new values
        instance.setScale(editScale);
        instance.setAlpha(editAlpha);
        
        // Broadcast update to all clients (including shuffle state, follow mode, prediction)
        FieldNetworking.sendUpdateFull(world, instance, 
            shuffleType.id(), shuffleIndex,
            followMode.id(), predictionEnabled,
            predictionLeadTicks, predictionMaxDistance,
            predictionLookAhead, predictionVerticalBoost);
        
        Logging.COMMANDS.topic("fieldtest").trace(
            "Synced edit values: shuffle={}:{}, follow={}, predict={}", 
            shuffleType.id(), shuffleIndex, followMode.id(), predictionEnabled);
    }
    
    /**
     * Syncs with a static pattern ID instead of shuffle index.
     */
    private static void syncTestFieldEditWithPattern(ServerCommandSource source, String patternId) {
        if (currentTestFieldId == -1) {
            return;
        }
        
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }
        
        var world = (net.minecraft.server.world.ServerWorld) player.getWorld();
        var manager = FieldManager.get(world);
        var instance = manager.getInstance(currentTestFieldId);
        
        if (instance == null) {
            Logging.COMMANDS.topic("fieldtest").warn("Test field {} not found in manager", currentTestFieldId);
            currentTestFieldId = -1;
            return;
        }
        
        // Update instance with new values
        instance.setScale(editScale);
        instance.setAlpha(editAlpha);
        
        // Broadcast update with static pattern ID (shuffleIndex = -1 means use pattern ID)
        FieldNetworking.sendUpdateWithPattern(world, instance, patternId,
            followMode.id(), predictionEnabled,
            predictionLeadTicks, predictionMaxDistance,
            predictionLookAhead, predictionVerticalBoost);
        
        Logging.COMMANDS.topic("fieldtest").trace(
            "Synced edit values: staticPattern={}, follow={}, predict={}", 
            patternId, followMode.id(), predictionEnabled);
    }

    // =========================================================================
    // Accessors for Renderer Integration
    // =========================================================================

    public static Identifier getCurrentProfile() { return currentProfile; }
    public static int getCurrentLayerIndex() { return currentLayerIndex; }
    public static float getEditRadius() { return editRadius; }
    public static float getEditSpin() { return editSpin; }
    public static float getEditAlpha() { return editAlpha; }
    public static float getEditGlow() { return editGlow; }
    public static float getEditScale() { return editScale; }
    public static int getEditLatSteps() { return editLatSteps; }
    public static int getEditLonSteps() { return editLonSteps; }
    public static String getEditAlgorithm() { return editAlgorithm; }
    public static String getEditColor() { return editColor; }
    public static VertexPattern getEditPattern() { return editPattern; }
    public static boolean hasActiveTestField() { return currentProfile != null; }
    public static boolean isAutoRefreshEnabled() { return autoRefreshEnabled; }
}
