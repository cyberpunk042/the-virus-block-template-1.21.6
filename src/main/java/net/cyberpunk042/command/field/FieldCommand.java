package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Main field command registration.
 * 
 * <h2>Command Tree</h2>
 * <pre>
 * /field
 * ├── list          - List all field definitions
 * ├── types         - List field types
 * ├── reload        - Reload definitions
 * ├── theme/...     - Theme subcommands
 * ├── shield/...    - Shield subcommands
 * ├── personal/...  - Personal field subcommands
 * └── debug/...     - Debug toggles
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
}
