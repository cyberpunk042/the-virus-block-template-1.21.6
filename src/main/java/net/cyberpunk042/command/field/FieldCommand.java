package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Main field command registration.
 * 
 * <h2>Command Tree</h2>
 * <pre>
 * /field
 * ├── list             - List all field definitions
 * ├── types            - List field types
 * ├── reload           - Reload definitions
 * ├── customize        - Open Field Customizer GUI
 * ├── customize <name> - Open GUI with profile
 * ├── theme/...        - Theme subcommands
 * ├── shield/...       - Shield subcommands
 * ├── personal/...     - Personal field subcommands
 * └── debug/...        - Debug toggles
 * </pre>
 */
public final class FieldCommand {
    
    // Debug state - accessible for rendering
    private static boolean debugBounds = false;
    private static boolean debugWireframe = false;
    private static boolean debugNormals = false;
    
    private FieldCommand() {}
    
    // Debug state accessors
    public static boolean isDebugBounds() { return debugBounds; }
    public static boolean isDebugWireframe() { return debugWireframe; }
    public static boolean isDebugNormals() { return debugNormals; }
    
    /**
     * Registers the /field command tree.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> cmd = CommandManager.literal("field")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("list")
                .executes(ctx -> handleList(ctx.getSource())))
            .then(CommandManager.literal("types")
                .executes(ctx -> handleTypes(ctx.getSource())))
            .then(CommandManager.literal("customize")
                .executes(ctx -> openCustomizer(ctx.getSource(), null))
                .then(CommandManager.argument("profile", StringArgumentType.word())
                    .executes(ctx -> openCustomizer(ctx.getSource(),
                        StringArgumentType.getString(ctx, "profile")))))
            .then(ThemeSubcommand.build())
            .then(ShieldSubcommand.build())
            .then(PersonalSubcommand.build())
            .then(buildDebug());
        
        // Reload action
        CommandKnob.action("field.reload", "Reload field definitions")
            .successMessage("Field definitions reloaded.")
            .handler(FieldCommand::handleReload)
            .attach(cmd);
        
        dispatcher.register(cmd);
        Logging.COMMANDS.info("Registered /field command tree");
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildDebug() {
        var cmd = CommandManager.literal("debug");
        
        CommandKnob.toggle("field.debug.bounds", "Debug bounds rendering")
            .defaultValue(false)
            .handler((src, v) -> {
                debugBounds = v;
                Logging.RENDER.info("Debug bounds: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.debug.wireframe", "Debug wireframe mode")
            .defaultValue(false)
            .handler((src, v) -> {
                debugWireframe = v;
                Logging.RENDER.info("Debug wireframe: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.debug.normals", "Debug normal visualization")
            .defaultValue(false)
            .handler((src, v) -> {
                debugNormals = v;
                Logging.RENDER.info("Debug normals: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static int handleList(ServerCommandSource source) {
        Logging.COMMANDS.topic("field").debug(
            "Player {} executed /field list", source.getName());
        int total = FieldRegistry.count();
        if (total == 0) {
            CommandFeedback.info(source, "No field definitions loaded.");
            return 0;
        }
        
        CommandFeedback.info(source, "Field definitions (" + total + "):");
        for (FieldType type : FieldType.values()) {
            int count = FieldRegistry.count(type);
            if (count > 0) {
                source.sendFeedback(() -> Text.literal("  " + type.id() + ": " + count), false);
            }
        }
        return total;
    }
    
    private static int handleTypes(ServerCommandSource source) {
        Logging.COMMANDS.topic("field").debug(
            "Player {} executed /field types", source.getName());
        CommandFeedback.info(source, "Field types:");
        for (FieldType type : FieldType.values()) {
            source.sendFeedback(() -> Text.literal("  " + type.id() + " - " + type.name()), false);
        }
        return FieldType.values().length;
    }
    
    private static boolean handleReload(ServerCommandSource source) {
        try {
            FieldRegistry.clear();
            FieldRegistry.registerDefaults();
            Logging.COMMANDS.info("Field definitions reloaded");
            return true;
        } catch (Exception e) {
            Logging.COMMANDS.error("Reload failed", e);
            CommandFeedback.error(source, "Reload failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Opens the Field Customizer GUI for a player.
     * G09: /field customize - Opens GUI with current profile
     * G10: /field customize <profile> - Opens GUI with specified profile
     * 
     * @param source Command source (must be a player)
     * @param profile Profile name to load, or null for current
     * @return 1 on success, 0 on failure
     */
    private static int openCustomizer(ServerCommandSource source, String profile) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        Logging.GUI.topic("command").info(
            "Player {} opening Field Customizer (profile: {})", 
            player.getName().getString(), 
            profile != null ? profile : "current");
        
        // TODO: G122-G129 - Send FieldGuiOpenS2C packet to client
        // For now, just acknowledge the command
        if (profile != null) {
            CommandFeedback.info(source, "Opening Field Customizer with profile: " + profile);
        } else {
            CommandFeedback.info(source, "Opening Field Customizer...");
        }
        
        // Packet will be implemented in Batch 13 (Network Packets)
        // ClientPlayNetworking.send(player, new FieldGuiOpenS2C(profile));
        
        return 1;
    }
}
