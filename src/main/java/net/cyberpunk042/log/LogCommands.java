package net.cyberpunk042.log;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Commands for managing the logging system at runtime.
 */
public class LogCommands {
    
    private static final SuggestionProvider<ServerCommandSource> CHANNEL_SUGGESTIONS = (ctx, builder) -> {
        Logging.channels().stream().map(Channel::id).forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    private static final SuggestionProvider<ServerCommandSource> LEVEL_SUGGESTIONS = (ctx, builder) -> {
        Stream.of(LogLevel.values()).map(Enum::name).forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("virus").then(literal("logs")
                .executes(LogCommands::dashboard)
                .then(literal("list").executes(LogCommands::list))
                .then(literal("set")
                    .then(argument("channel", StringArgumentType.word())
                        .suggests(CHANNEL_SUGGESTIONS)
                        .then(argument("level", StringArgumentType.word())
                            .suggests(LEVEL_SUGGESTIONS)
                            .executes(LogCommands::setLevel))))
                .then(literal("chat")
                    .then(literal("list").executes(LogCommands::chatList))
                    .then(argument("channel", StringArgumentType.word())
                        .suggests(CHANNEL_SUGGESTIONS)
                        .then(literal("on").executes(ctx -> setChatForward(ctx, true)))
                        .then(literal("off").executes(ctx -> setChatForward(ctx, false)))))
                .then(literal("watchdog")
                    .then(literal("on").executes(ctx -> setWatchdog(ctx, true)))
                    .then(literal("off").executes(ctx -> setWatchdog(ctx, false)))
                    .then(argument("perSec", IntegerArgumentType.integer(1))
                        .then(argument("perMin", IntegerArgumentType.integer(1))
                            .executes(LogCommands::setWatchdogThresholds))))
                .then(literal("reload").executes(LogCommands::reload))
                .then(literal("reset").executes(LogCommands::reset))
                .then(literal("save").executes(LogCommands::save))
            )
        );
    }
    
    private static int dashboard(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Logging Dashboard ==="), false);
        src.sendFeedback(() -> Text.literal("§7Channels: §f" + Logging.channels().size()), false);
        src.sendFeedback(() -> Text.literal("§7Watchdog: §f" + (LogWatchdog.isEnabled() ? "ON" : "OFF")), false);
        src.sendFeedback(() -> Text.literal("§7Chat: §f" + (LogConfig.chatEnabled() ? "ON" : "OFF")), false);
        src.sendFeedback(() -> Text.literal("§7Use §e/virus logs list§7 to see all channels"), false);
        return 1;
    }
    
    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Log Channels ==="), false);
        for (Channel ch : Logging.channels()) {
            String level = ch.level().name();
            String chat = ch.chatForward() ? " §a[chat]" : "";
            src.sendFeedback(() -> Text.literal("§7" + ch.id() + ": §f" + level + chat), false);
        }
        return 1;
    }
    
    private static int setLevel(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        String levelStr = StringArgumentType.getString(ctx, "level");
        
        Channel ch = Logging.channel(channelId);
        if (ch == null) {
            ctx.getSource().sendError(Text.literal("Unknown channel: " + channelId));
            return 0;
        }
        
        LogLevel level = LogLevel.parse(levelStr);
        ch.setLevel(level);
        ctx.getSource().sendFeedback(() -> Text.literal("§aSet §f" + channelId + "§a to §f" + level.name()), false);
        return 1;
    }
    
    private static int chatList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Chat Forwarding ==="), false);
        for (Channel ch : Logging.channels()) {
            if (ch.chatForward()) {
                src.sendFeedback(() -> Text.literal("§a✓ §f" + ch.id()), false);
            }
        }
        return 1;
    }
    
    private static int setChatForward(CommandContext<ServerCommandSource> ctx, boolean value) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        Channel ch = Logging.channel(channelId);
        if (ch == null) {
            ctx.getSource().sendError(Text.literal("Unknown channel: " + channelId));
            return 0;
        }
        ch.setChatForward(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aChat forwarding for §f" + channelId + "§a: " + (value ? "ON" : "OFF")), false);
        return 1;
    }
    
    private static int setWatchdog(CommandContext<ServerCommandSource> ctx, boolean value) {
        LogWatchdog.setEnabled(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aWatchdog: " + (value ? "ON" : "OFF")), false);
        return 1;
    }
    
    private static int setWatchdogThresholds(CommandContext<ServerCommandSource> ctx) {
        int perSec = IntegerArgumentType.getInteger(ctx, "perSec");
        int perMin = IntegerArgumentType.getInteger(ctx, "perMin");
        LogWatchdog.setThresholds(perSec, perMin);
        ctx.getSource().sendFeedback(() -> Text.literal("§aWatchdog thresholds: §f" + perSec + "/s, " + perMin + "/m"), false);
        return 1;
    }
    
    private static int reload(CommandContext<ServerCommandSource> ctx) {
        LogConfig.load();
        ctx.getSource().sendFeedback(() -> Text.literal("§aReloaded logging configuration"), false);
        return 1;
    }
    
    private static int reset(CommandContext<ServerCommandSource> ctx) {
        Logging.reset();
        ctx.getSource().sendFeedback(() -> Text.literal("§aReset all channels to defaults"), false);
        return 1;
    }
    
    private static int save(CommandContext<ServerCommandSource> ctx) {
        LogConfig.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§aSaved logging configuration"), false);
        return 1;
    }
}
