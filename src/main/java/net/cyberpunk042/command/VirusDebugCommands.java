package net.cyberpunk042.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.cyberpunk042.infection.BoobytrapHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class VirusDebugCommands {

	private VirusDebugCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("virusboobytraps")
						.requires(source -> source.hasPermissionLevel(2))
						.executes(ctx -> debugBoobytraps(ctx.getSource(), 8))
						.then(CommandManager.argument("radiusChunks", IntegerArgumentType.integer(1, 16))
								.executes(ctx -> debugBoobytraps(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radiusChunks"))))));
	}

	private static int debugBoobytraps(ServerCommandSource source, int radiusChunks) {
		if (source.getPlayer() == null) {
			return 0;
		}
		BoobytrapHelper.debugList(source.getPlayer(), radiusChunks * 16);
		return 1;
	}
}

