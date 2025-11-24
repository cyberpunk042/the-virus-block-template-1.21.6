package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class VirusCommand {
	private VirusCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("virusblock")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("teleport")
						.then(CommandManager.literal("enable").executes(ctx -> setTeleportEnabled(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setTeleportEnabled(ctx.getSource(), false)))
						.then(CommandManager.literal("radius")
								.then(CommandManager.argument("chunks", IntegerArgumentType.integer(0, 64))
										.executes(ctx -> setTeleportRadius(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "chunks")))))
						.then(CommandManager.literal("status").executes(ctx -> reportTeleport(ctx.getSource())))));
	}

	private static int setTeleportEnabled(ServerCommandSource source, boolean enabled) {
		source.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED).set(enabled, source.getServer());
		source.sendFeedback(() -> Text.literal("Virus block teleportation set to " + enabled), true);
		return enabled ? 1 : 0;
	}

	private static int setTeleportRadius(ServerCommandSource source, int chunks) {
		source.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS).set(chunks, source.getServer());
		source.sendFeedback(() -> Text.literal("Virus block teleport radius set to " + chunks + " chunks"), true);
		return chunks;
	}

	private static int reportTeleport(ServerCommandSource source) {
		boolean enabled = source.getServer().getGameRules().getBoolean(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED);
		int radius = source.getServer().getGameRules().getInt(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS);
		source.sendFeedback(() -> Text.literal("Virus block teleportation is " + (enabled ? "enabled" : "disabled")
				+ " (radius " + radius + " chunks)"), false);
		return enabled ? 1 : 0;
	}
}

