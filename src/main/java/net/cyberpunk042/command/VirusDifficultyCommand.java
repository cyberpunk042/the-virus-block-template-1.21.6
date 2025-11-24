package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.screen.handler.VirusDifficultyScreenHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class VirusDifficultyCommand {
	private static final SimpleCommandExceptionType UNKNOWN_DIFFICULTY =
			new SimpleCommandExceptionType(Text.translatable("message.the-virus-block.difficulty.unknown"));

	private VirusDifficultyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("virusdifficulty")
				.executes(ctx -> openDifficultyMenu(ctx.getSource()))
				.then(CommandManager.argument("difficulty", StringArgumentType.word())
						.suggests((ctx, builder) -> CommandSource.suggestMatching(
								java.util.Arrays.stream(VirusDifficulty.values()).map(VirusDifficulty::getId), builder))
						.executes(ctx -> setDifficulty(ctx.getSource(), StringArgumentType.getString(ctx, "difficulty")))));
	}

	private static int openDifficultyMenu(ServerCommandSource source) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		return openMenuFor(player) ? 1 : 0;
	}

	public static boolean openMenuFor(ServerPlayerEntity player) {
		ServerWorld world = (ServerWorld) player.getWorld();
		boolean locked = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_DIFFICULTY_LOCKED);
		if (locked && !player.hasPermissionLevel(2)) {
			player.sendMessage(Text.translatable("message.the-virus-block.difficulty.locked"), true);
			return false;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, playerEntity) -> new VirusDifficultyScreenHandler(syncId, inventory),
				Text.translatable("screen.the-virus-block.difficulty.title")));
		return true;
	}

	private static int setDifficulty(ServerCommandSource source, String value) throws CommandSyntaxException {
		VirusDifficulty difficulty = findDifficulty(value);
		ServerWorld world = source.getWorld();
		boolean locked = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_DIFFICULTY_LOCKED);
		if (locked && !source.hasPermissionLevel(2)) {
			source.sendError(Text.translatable("message.the-virus-block.difficulty.locked"));
			return 0;
		}
		VirusWorldState state = VirusWorldState.get(world);
		state.setDifficulty(world, difficulty);
		source.sendFeedback(() -> Text.translatable("message.the-virus-block.difficulty.set", difficulty.getDisplayName()), true);
		return 1;
	}

	private static VirusDifficulty findDifficulty(String value) throws CommandSyntaxException {
		for (VirusDifficulty diff : VirusDifficulty.values()) {
			if (diff.getId().equalsIgnoreCase(value)) {
				return diff;
			}
		}
		throw UNKNOWN_DIFFICULTY.create();
	}
}

