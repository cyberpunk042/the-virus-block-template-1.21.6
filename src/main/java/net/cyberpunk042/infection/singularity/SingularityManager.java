package net.cyberpunk042.infection.singularity;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class SingularityManager {
	private static final int TOTAL_WAVES = 30;
	public static final float EXPLOSION_DAMAGE = 0.03F / 3.0F;
	public static final float HIT_DAMAGE = 0.0025F / 3.0F;
	private static final ServerBossBar CORE_BAR = new ServerBossBar(
			Text.translatable("block.the-virus-block.virus_block"),
			BossBar.Color.PURPLE,
			BossBar.Style.PROGRESS);
	private static final Map<ServerWorld, Data> DATA = new HashMap<>();
	private static final Map<ServerWorld, Set<WitherEntity>> HIDDEN_WITHERS = new HashMap<>();

	static {
		CORE_BAR.setVisible(false);
		CORE_BAR.setDarkenSky(true);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CORE_BAR.removePlayer(handler.player));
	}

	private SingularityManager() {
	}

	public static void tick(ServerWorld world, VirusWorldState state) {
		boolean active = state.isSingularitySummoned();
		Data data = DATA.get(world);
		if (!active) {
			if (data != null) {
				stop(world);
			}
			return;
		}

		if (data == null) {
			data = new Data();
			DATA.put(world, data);
			CORE_BAR.setPercent(1.0F);
			CORE_BAR.setVisible(true);
			world.playSound(null, BlockPos.ORIGIN, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 4.0F, 0.5F);
		}

		syncPlayers(world);
		tickHiddenWitherBars(world);
		data.tick(world, state);
	}

	public static void onVirusBlockDamage(ServerWorld world, float amount) {
		Data data = DATA.get(world);
		if (data == null) {
			return;
		}

		data.reduceProgress(world, amount);
		VirusWorldState.get(world).reflectCoreDamage(world, amount);
	}

	public static boolean canBreakVirusBlock(ServerWorld world) {
		Data data = DATA.get(world);
		return data != null && data.progress <= 0.0F;
	}

	public static boolean isActive(ServerWorld world) {
		return DATA.containsKey(world);
	}

	public static void stop(ServerWorld world) {
		Data data = DATA.remove(world);
		if (data != null) {
			data.clearWaveMobs(world);
		}
		Set<WitherEntity> tracked = HIDDEN_WITHERS.remove(world);
		if (tracked != null) {
			tracked.clear();
		}
		if (CORE_BAR.getPlayers().isEmpty()) {
			CORE_BAR.setVisible(false);
			return;
		}

		for (ServerPlayerEntity player : List.copyOf(CORE_BAR.getPlayers())) {
			CORE_BAR.removePlayer(player);
		}
		CORE_BAR.setVisible(false);
	}

	public static void clearWaveMobs(ServerWorld world) {
		Data data = DATA.get(world);
		if (data != null) {
			data.clearWaveMobs(world);
		}
	}

	public static boolean shouldPreventFriendlyFire(ServerWorld world, LivingEntity attacker, LivingEntity target) {
		if (world.getGameRules().getBoolean(TheVirusBlock.VIRUS_WAVE_FRIENDLY_FIRE)) {
			return false;
		}
		Data data = DATA.get(world);
		if (data == null) {
			return false;
		}
		return data.isWaveMob(attacker.getUuid()) && data.isWaveMob(target.getUuid());
	}

	private static final class Data {
		private float progress = 1.0F;
		private int wave = 0;
		private long nextWaveTick = 0L;
		private final Set<UUID> waveMobIds = new HashSet<>();
		void tick(ServerWorld world, VirusWorldState state) {
			long time = world.getTime();

			if (time >= nextWaveTick && wave < TOTAL_WAVES) {
				spawnWave(world, ++wave);
				nextWaveTick = time + Math.max(100, 260 - wave * 4L);
			} else if (wave >= TOTAL_WAVES && time >= nextWaveTick) {
				spawnWave(world, wave);
				nextWaveTick = time + 120;
			}

			CORE_BAR.setPercent(MathHelper.clamp(progress, 0.0F, 1.0F));

			if (progress <= 0.0F) {
				world.playSound(null, BlockPos.ORIGIN, SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 5.0F, 0.5F);
				state.forceContainmentReset(world);
				SingularityManager.stop(world);
			}
		}

		void reduceProgress(ServerWorld world, float amount) {
			float previous = progress;
			progress = Math.max(0.0F, progress - amount);
			if (previous > 0.5F && progress <= 0.5F) {
				VirusWorldState.get(world).collapseShells(world);
			}
		}

		private void spawnWave(ServerWorld world, int waveIndex) {
			List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
			if (players.isEmpty()) {
				return;
			}

			Random random = world.getRandom();
			int baseCount = 3 + waveIndex / 2;
			for (int i = 0; i < baseCount; i++) {
				ServerPlayerEntity target = players.get(random.nextInt(players.size()));
				spawnMob(world, pickBaseMob(waveIndex, random), target.getBlockPos().add(random.nextBetween(-16, 16), 0, random.nextBetween(-16, 16)));
			}

			if (waveIndex % 5 == 0) {
				for (int i = 0; i < Math.min(3, waveIndex / 5); i++) {
					ServerPlayerEntity target = players.get(random.nextInt(players.size()));
					spawnMob(world, EntityType.WITHER, target.getBlockPos().add(random.nextBetween(-32, 32), 0, random.nextBetween(-32, 32)));
				}
			}

			if (waveIndex % 10 == 0) {
				ServerPlayerEntity target = players.get(random.nextInt(players.size()));
				spawnDragon(world, target.getBlockPos().add(0, 40, 0));
			}
		}

		private EntityType<?> pickBaseMob(int wave, Random random) {
			EntityType<?>[] pool;
			if (wave < 5) {
				pool = new EntityType[]{EntityType.HUSK, EntityType.DROWNED, EntityType.BLAZE, EntityType.ENDERMAN, EntityType.PHANTOM, EntityType.SLIME};
			} else if (wave < 10) {
				pool = new EntityType[]{EntityType.WITCH, EntityType.EVOKER, EntityType.VEX, EntityType.RAVAGER, EntityType.GHAST, EntityType.HOGLIN};
			} else if (wave < 15) {
				pool = new EntityType[]{EntityType.WARDEN, EntityType.IRON_GOLEM, EntityType.PIGLIN_BRUTE, EntityType.WITHER_SKELETON, EntityType.SHULKER};
			} else {
				pool = new EntityType[]{EntityType.WARDEN, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.IRON_GOLEM, EntityType.EVOKER};
			}

			return pool[random.nextInt(pool.length)];
		}

		private void spawnMob(ServerWorld world, EntityType<?> type, BlockPos pos) {
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				return;
			}

			world.getChunkManager().markForUpdate(pos);
			var entity = type.spawn(world, pos, net.minecraft.entity.SpawnReason.EVENT);
			if (entity == null) {
				return;
			}

			if (entity instanceof HostileEntity hostile) {
				hostile.setPersistent();
			}

			if (entity instanceof IronGolemEntity golem) {
				golem.setPlayerCreated(false);
			}

			if (entity instanceof SlimeEntity slime) {
				slime.setSize(Math.min(4, 1 + wave / 5), true);
			}

			if (entity instanceof WitherEntity wither) {
				trackWither(world, wither);
			}

			if (entity instanceof MobEntity mob) {
				registerWaveMob(mob);
			}
		}

		private void spawnDragon(ServerWorld world, BlockPos pos) {
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				return;
			}

			EnderDragonEntity dragon = EntityType.ENDER_DRAGON.create(world, null, pos, net.minecraft.entity.SpawnReason.EVENT, true, false);
			if (dragon == null) {
				return;
			}

			dragon.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360.0F, 0.0F);
			hideBossBar(dragon);
		}

		private void registerWaveMob(MobEntity mob) {
			waveMobIds.add(mob.getUuid());
		}

		private void clearWaveMobs(ServerWorld world) {
			if (waveMobIds.isEmpty()) {
				return;
			}
			for (UUID uuid : waveMobIds) {
				Entity entity = world.getEntity(uuid);
				if (entity instanceof LivingEntity living) {
					living.discard();
				}
			}
			waveMobIds.clear();
		}

		private boolean isWaveMob(UUID uuid) {
			return waveMobIds.contains(uuid);
		}
	}

	private static void syncPlayers(ServerWorld world) {
		for (ServerPlayerEntity player : List.copyOf(CORE_BAR.getPlayers())) {
			if (player.getWorld() != world) {
				CORE_BAR.removePlayer(player);
			}
		}

		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (!CORE_BAR.getPlayers().contains(player)) {
				CORE_BAR.addPlayer(player);
			}
		}
	}

	private static void trackWither(ServerWorld world, WitherEntity wither) {
		HIDDEN_WITHERS.computeIfAbsent(world, key -> Collections.newSetFromMap(new WeakHashMap<>())).add(wither);
		hideBossBar(wither);
	}

	private static void tickHiddenWitherBars(ServerWorld world) {
		Set<WitherEntity> tracked = HIDDEN_WITHERS.get(world);
		if (tracked == null || tracked.isEmpty()) {
			return;
		}
		tracked.removeIf(entity -> entity == null || !entity.isAlive() || entity.getWorld() != world);
		for (WitherEntity wither : tracked) {
			hideBossBar(wither);
		}
	}

	private static void hideBossBar(Object owner) {
		Class<?> type = owner.getClass();
		while (type != null && type != Object.class) {
			for (Field field : type.getDeclaredFields()) {
				if (!ServerBossBar.class.isAssignableFrom(field.getType())) {
					continue;
				}
				try {
					field.setAccessible(true);
					ServerBossBar bar = (ServerBossBar) field.get(owner);
					if (bar != null) {
						bar.setVisible(false);
						bar.clearPlayers();
					}
				} catch (IllegalAccessException ignored) {
				}
				return;
			}
			type = type.getSuperclass();
		}
	}

}

