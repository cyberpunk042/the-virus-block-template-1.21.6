package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

/**
 * Applies ambient pressure effects (bonus spawns + health penalties) while the infection is active.
 */
public final class AmbientPressureService {

	private static final Identifier EXTREME_HEALTH_MODIFIER_ID =
			Identifier.of(TheVirusBlock.MOD_ID, "extreme_health_penalty");

	private final VirusWorldState host;

	public AmbientPressureService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void tick(InfectionTier tier) {
		ServerWorld world = host.world();
		boostAmbientSpawns(world, tier);
		applyDifficultyRules(tier);
	}

	public void applyDifficultyRules(InfectionTier tier) {
		ServerWorld world = host.world();
		applyDifficultyRulesInternal(world, tier);
	}

	private void boostAmbientSpawns(ServerWorld world, InfectionTier tier) {
		if (host.singularityState().singularityState != SingularityState.DORMANT) {
			return;
		}
		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
			return;
		}
		VirusDifficulty difficulty = host.tiers().difficulty();
		int difficultyScale = switch (difficulty) {
			case EASY -> 0;
			case MEDIUM -> 1;
			case HARD -> 2;
			case EXTREME -> 3;
		};
		if (difficultyScale <= 0) {
			return;
		}
		int tierIndex = tier.getIndex();
		// No mob spawning at tier 1 - start at tier 2
		if (tierIndex < 1) {
			return;
		}
		float tierMultiplier = tier.getMobSpawnMultiplier();
		float scaledAttempts = difficultyScale * tierMultiplier;
		if (host.tiers().isApocalypseMode()) {
			scaledAttempts += 2.0F;
		}
		int attempts = Math.max(1, Math.round(scaledAttempts));
		Random random = world.getRandom();
		List<ServerPlayerEntity> anchors = world.getPlayers(player -> player.isAlive()
				&& !player.isSpectator()
				&& host.combat().isWithinAura(player.getBlockPos()));
		if (anchors.isEmpty()) {
			return;
		}
		for (int i = 0; i < attempts; i++) {
			ServerPlayerEntity anchor = anchors.get(random.nextInt(anchors.size()));
			BlockPos spawnPos = findBoostSpawnPos(world, anchor.getBlockPos(), 16 + tierIndex * 6, random);
			if (spawnPos == null) {
				continue;
			}
			EntityType<? extends MobEntity> type = pickBoostMobType(tierIndex, random);
			if (type == null) {
				continue;
			}
			MobEntity spawned = type.spawn(world,
					entity -> entity.refreshPositionAndAngles(spawnPos, random.nextFloat() * 360.0F, 0.0F),
					spawnPos,
					SpawnReason.EVENT,
					true,
					false);
			VirusMobAllyHelper.mark(spawned);
		}
	}

	private BlockPos findBoostSpawnPos(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int tries = 0; tries < 6; tries++) {
			int dx = random.nextBetween(-radius, radius);
			int dz = random.nextBetween(-radius, radius);
			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			// Optimized: check chunk first without allocating BlockPos
			if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
				continue;
			}
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.getWorldBorder().contains(pos)) {
				continue;
			}
			if (!world.getBlockState(pos).isAir() || world.getBlockState(pos.down()).isAir()) {
				continue;
			}
			return pos;
		}
		return null;
	}

	private EntityType<? extends MobEntity> pickBoostMobType(int tierIndex, Random random) {
		if (tierIndex >= 3 && random.nextFloat() < 0.25F) {
			return EntityType.ENDERMAN;
		}
		if (tierIndex >= 2 && random.nextFloat() < 0.35F) {
			return random.nextBoolean() ? EntityType.HUSK : EntityType.DROWNED;
		}
		return switch (random.nextInt(3)) {
			case 0 -> EntityType.ZOMBIE;
			case 1 -> EntityType.SKELETON;
			default -> EntityType.SPIDER;
		};
	}

	private void applyDifficultyRulesInternal(ServerWorld world, InfectionTier tier) {
		VirusDifficulty difficulty = host.tiers().difficulty();
		if (difficulty == VirusDifficulty.EXTREME) {
			applyExtremeHealthPenalty(world, difficulty, tier);
		} else {
			clearExtremeHealthPenalty(world);
		}
	}

	private void applyExtremeHealthPenalty(ServerWorld world, VirusDifficulty difficulty, InfectionTier tier) {
		double penalty = difficulty.getHealthPenaltyForTier(tier.getIndex());
		if (penalty >= 0.0D) {
			clearExtremeHealthPenalty(world);
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
			if (attribute == null) {
				continue;
			}
			attribute.removeModifier(EXTREME_HEALTH_MODIFIER_ID);
			attribute.addPersistentModifier(new EntityAttributeModifier(EXTREME_HEALTH_MODIFIER_ID, penalty, Operation.ADD_VALUE));
			double max = attribute.getValue();
			if (player.getHealth() > max) {
				player.setHealth((float) max);
			}
		}
	}

	private void clearExtremeHealthPenalty(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
			if (attribute != null) {
				attribute.removeModifier(EXTREME_HEALTH_MODIFIER_ID);
			}
		}
	}
}

