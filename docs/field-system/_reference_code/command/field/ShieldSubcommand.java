package net.cyberpunk042.command.field;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * /field shield subcommands
 */
public final class ShieldSubcommand {
    
    private ShieldSubcommand() {}
    
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        var cmd = CommandManager.literal("shield")
            .then(CommandManager.literal("list")
                .executes(ctx -> handleList(ctx.getSource())))
            .then(buildSet());
        
        // Knob-based settings
        CommandKnob.floatValue("field.shield.radius", "Shield radius")
            .range(1.0f, 64.0f)
            .unit("blocks")
            .defaultValue(8.0f)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Shield radius set to {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.value("field.shield.segments", "Shield segments")
            .range(8, 64)
            .defaultValue(32)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Shield segments set to {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.shield.spin", "Shield auto-spin")
            .defaultValue(true)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Shield spin: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildSet() {
        return CommandManager.literal("set")
            .then(CommandManager.literal("lat")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(4, 64))
                    .executes(ctx -> {
                        int v = IntegerArgumentType.getInteger(ctx, "value");
                        CommandFeedback.valueSet(ctx.getSource(), "Latitude steps", String.valueOf(v));
                        return 1;
                    })))
            .then(CommandManager.literal("lon")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(4, 64))
                    .executes(ctx -> {
                        int v = IntegerArgumentType.getInteger(ctx, "value");
                        CommandFeedback.valueSet(ctx.getSource(), "Longitude steps", String.valueOf(v));
                        return 1;
                    })))
            .then(CommandManager.literal("scale")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                    .executes(ctx -> {
                        float v = FloatArgumentType.getFloat(ctx, "value");
                        CommandFeedback.valueSet(ctx.getSource(), "Scale", String.valueOf(v));
                        return 1;
                    })));
    }
    
    private static int handleList(ServerCommandSource source) {
        var shields = FieldRegistry.byType(FieldType.SHIELD);
        if (shields.isEmpty()) {
            CommandFeedback.info(source, "No shield definitions loaded.");
            return 0;
        }
        
        CommandFeedback.info(source, "Shield definitions (" + shields.size() + "):");
        for (var def : shields) {
            source.sendFeedback(() -> Text.literal("  " + def.id().getPath()), false);
        }
        return shields.size();
    }
}
