package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import net.cyberpunk042.infection.GlobalTerrainCorruption;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class VirusStatsCommand {
	private VirusStatsCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("virusstats")
				.executes(ctx -> report(ctx.getSource())));
	}

	private static int report(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);

		source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.header").formatted(Formatting.AQUA), false);
		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.mod_time",
				Text.literal(formatDuration(world.getTime()))
		).formatted(Formatting.GRAY), false);

		if (!state.isInfected()) {
			source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.infection_idle").formatted(Formatting.RED), false);
			return 1;
		}

		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.infection_time",
				Text.literal(formatDuration(state.getInfectionTicks()))
		).formatted(Formatting.GRAY), false);

		long ticksUntilFinalWave = ticksUntilFinalWave(state);
		if (ticksUntilFinalWave <= 0L) {
			source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.final_wave_now").formatted(Formatting.DARK_RED), false);
		} else {
			source.sendFeedback(() -> Text.translatable(
					"command.the-virus-block.stats.final_wave_eta",
					Text.literal(formatDuration(ticksUntilFinalWave))
			).formatted(Formatting.GOLD), false);
		}

		int trackedChunks = GlobalTerrainCorruption.getTrackedChunkCount(world);
		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.chunks_alive",
				trackedChunks
		).formatted(Formatting.DARK_GREEN), false);

		long singularityTicks = Math.max(0L, ticksUntilFinalWave - 1_200L);
		if (singularityTicks <= 0L) {
			source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.singularity_now").formatted(Formatting.DARK_PURPLE), false);
		} else {
			source.sendFeedback(() -> Text.translatable(
					"command.the-virus-block.stats.singularity_eta",
					Text.literal(formatDuration(singularityTicks))
			).formatted(Formatting.DARK_PURPLE), false);
		}
		return 1;
	}

	private static long ticksUntilFinalWave(VirusWorldState state) {
		if (!state.isInfected() || state.isApocalypseMode()) {
			return 0L;
		}
		InfectionTier current = state.getCurrentTier();
		long remaining = Math.max(0L, current.getDurationTicks() - state.getTicksInTier());
		for (int tier = current.getIndex() + 1; tier <= InfectionTier.maxIndex(); tier++) {
			remaining += InfectionTier.byIndex(tier).getDurationTicks();
		}
		return remaining;
	}

	private static String formatDuration(long ticks) {
		long clamped = Math.max(0L, ticks);
		long totalSeconds = clamped / 20L;
		long days = totalSeconds / 86_400L;
		totalSeconds %= 86_400L;
		long hours = totalSeconds / 3_600L;
		totalSeconds %= 3_600L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;

		StringBuilder builder = new StringBuilder();
		if (days > 0) {
			builder.append(days).append("d ");
		}
		if (hours > 0 || days > 0) {
			builder.append(hours).append("h ");
		}
		if (minutes > 0 || hours > 0 || days > 0) {
			builder.append(minutes).append("m ");
		}
		builder.append(seconds).append("s");
		return builder.toString().trim();
	}
}

