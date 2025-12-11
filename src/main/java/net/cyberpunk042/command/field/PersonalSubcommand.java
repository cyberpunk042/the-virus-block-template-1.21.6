package net.cyberpunk042.command.field;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * /field personal subcommands - Configure the player's personal field (shield).
 * 
 * <p>These settings control how the personal field behaves in-game:
 * <ul>
 *   <li>enabled - Toggle personal field visibility</li>
 *   <li>visual - Toggle visual rendering</li>
 *   <li>follow - Follow mode (SNAP, SMOOTH, GLIDE)</li>
 *   <li>prediction - Movement prediction settings</li>
 * </ul>
 * 
 * <p>Settings are persisted via CommandKnob to JSON config.</p>
 */
public final class PersonalSubcommand {
    
    private PersonalSubcommand() {}
    
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        var cmd = CommandManager.literal("personal")
            .then(buildPrediction());
        
        // Knob-based settings
        CommandKnob.toggle("field.personal.enabled", "Personal field")
            .defaultValue(false)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Personal field: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.personal.visual", "Personal field visual")
            .defaultValue(true)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Personal visual: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.enumValue("field.personal.follow", "Follow mode", FollowMode.class)
            .idMapper(FollowMode::id)
            .parser(FollowMode::fromId)
            .defaultValue(FollowMode.SMOOTH)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Follow mode: {}", v.id());
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildPrediction() {
        var cmd = CommandManager.literal("prediction");
        
        CommandKnob.toggle("field.personal.prediction.enabled", "Movement prediction")
            .defaultValue(true)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Prediction enabled: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.value("field.personal.prediction.lead", "Lead ticks")
            .range(0, 10)
            .unit("ticks")
            .defaultValue(2)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Lead ticks: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.floatValue("field.personal.prediction.max", "Max lead distance")
            .range(0.1f, 5.0f)
            .unit("blocks")
            .defaultValue(1.5f)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Max lead distance: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
}
