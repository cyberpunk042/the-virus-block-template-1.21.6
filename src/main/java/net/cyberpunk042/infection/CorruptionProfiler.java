package net.cyberpunk042.infection;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

final class CorruptionProfiler {
	private CorruptionProfiler() {
	}

	static void logChunkRewrite(ServerWorld world, ChunkPos pos, int conversions, boolean cleanse) {
		if (conversions <= 0 || !isEnabled(world)) {
			return;
		}
		String action = cleanse ? "cleansed" : "corrupted";
		TheVirusBlock.LOGGER.info("[CorruptionProfiler:{}] chunk {} {} {} blocks",
				worldId(world), pos, action, conversions);
	}

	static void logBoobytrapPlacement(ServerWorld world, BlockPos pos, String type) {
		if (!isEnabled(world)) {
			return;
		}
		TheVirusBlock.LOGGER.info("[CorruptionProfiler:{}] boobytrap {} armed at {}",
				worldId(world), type, pos);
	}

	static void logBoobytrapSpread(ServerWorld world, BlockPos pos, String type, int conversions) {
		if (conversions <= 0 || !isEnabled(world)) {
			return;
		}
		TheVirusBlock.LOGGER.info("[CorruptionProfiler:{}] boobytrap {} spread {} blocks around {}",
				worldId(world), type, conversions, pos);
	}

	static void logBoobytrapTrigger(ServerWorld world, BlockPos pos, String type, String reason, int affectedPlayers) {
		if (!isEnabled(world)) {
			return;
		}
		TheVirusBlock.LOGGER.info("[CorruptionProfiler:{}] boobytrap {} triggered ({}) at {} hitting {} players",
				worldId(world), type, reason, pos, affectedPlayers);
	}

	private static boolean isEnabled(ServerWorld world) {
		return world.getGameRules().getBoolean(TheVirusBlock.VIRUS_CORRUPTION_PROFILER);
	}

	private static Identifier worldId(ServerWorld world) {
		return world.getRegistryKey().getValue();
	}
}

