package net.cyberpunk042.infection.service;

import java.util.List;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

/**
 * Handles guardian spawn waves around virus cores so {@link VirusWorldState}
 * keeps only the scheduling decisions. Future telemetry and configurable pools
 * can extend this service without touching the host state.
 */
public final class GuardianSpawnService {
	private final LoggingService logging;

	public GuardianSpawnService(LoggingService logging) {
		this.logging = logging;
	}

	public void spawnCoreGuardians(ServerWorld world, List<BlockPos> cores) {
		if (cores.isEmpty()) {
			return;
		}
		Random random = world.getRandom();
		for (BlockPos core : cores) {
			if (!world.isChunkLoaded(ChunkPos.toLong(core))) {
				continue;
			}
			int guards = 4 + random.nextInt(3);
			for (int i = 0; i < guards; i++) {
				BlockPos spawnPos = findGuardianSpawnPos(world, core, random);
				if (spawnPos == null) {
					continue;
				}
				EntityType<?> type = pickGuardianType(random, i == 0);
				Entity entity = type.spawn(world, spawned -> {
					if (spawned instanceof HostileEntity hostile) {
						hostile.setPersistent();
					}
					if (spawned instanceof IronGolemEntity golem) {
						golem.setPlayerCreated(false);
					}
				}, spawnPos, SpawnReason.EVENT, true, false);
				if (entity instanceof MobEntity mob) {
					VirusMobAllyHelper.mark(mob);
				}
			}
			logging.info(LogChannel.INFECTION, "[guardian] spawned wave near {}", core);
		}
	}

	private BlockPos findGuardianSpawnPos(ServerWorld world, BlockPos center, Random random) {
		for (int attempts = 0; attempts < 12; attempts++) {
			BlockPos candidate = center.add(random.nextBetween(-6, 6), random.nextBetween(-2, 3), random.nextBetween(-6, 6));
			if (!world.isChunkLoaded(ChunkPos.toLong(candidate))) {
				continue;
			}
			if (!world.getBlockState(candidate).isAir()) {
				continue;
			}
			BlockPos below = candidate.down();
			BlockState support = world.getBlockState(below);
			if (!support.isSolidBlock(world, below)) {
				continue;
			}
			return candidate;
		}
		return null;
	}

	private EntityType<?> pickGuardianType(Random random, boolean priority) {
		if (priority && random.nextBoolean()) {
			return EntityType.WARDEN;
		}
		EntityType<?>[] pool = new EntityType[]{
				EntityType.WITHER_SKELETON,
				EntityType.PIGLIN_BRUTE,
				EntityType.EVOKER,
				EntityType.HOGLIN,
				EntityType.IRON_GOLEM,
				EntityType.BLAZE
		};
		return pool[random.nextInt(pool.length)];
	}
}

