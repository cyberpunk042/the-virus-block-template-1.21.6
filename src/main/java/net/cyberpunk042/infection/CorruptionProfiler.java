package net.cyberpunk042.infection;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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

	private static boolean isEnabled(ServerWorld world) {
		return world.getGameRules().getBoolean(TheVirusBlock.VIRUS_CORRUPTION_PROFILER);
	}

	private static Identifier worldId(ServerWorld world) {
		return world.getRegistryKey().getValue();
	}
}

