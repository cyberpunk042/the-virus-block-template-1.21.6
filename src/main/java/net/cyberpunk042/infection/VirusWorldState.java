package net.cyberpunk042.infection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.infection.singularity.SingularityManager;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.WorldEvents;

public class VirusWorldState extends PersistentState {
	public static final String ID = TheVirusBlock.MOD_ID + "_infection_state";
	private static final Codec<VirusWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("infected", false).forGetter(state -> state.infected),
			Codec.INT.optionalFieldOf("tierIndex", 0).forGetter(state -> state.tierIndex),
			Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(state -> state.totalTicks),
			Codec.LONG.optionalFieldOf("ticksInTier", 0L).forGetter(state -> state.ticksInTier),
			Codec.LONG.optionalFieldOf("calmUntilTick", 0L).forGetter(state -> state.calmUntilTick),
			Codec.INT.optionalFieldOf("containmentLevel", 0).forGetter(state -> state.containmentLevel),
			Codec.BOOL.optionalFieldOf("singularitySummoned", false).forGetter(state -> state.singularitySummoned),
			Codec.BOOL.optionalFieldOf("apocalypseMode", false).forGetter(state -> state.apocalypseMode),
			Codec.BOOL.optionalFieldOf("terrainCorrupted", false).forGetter(state -> state.terrainCorrupted),
			Codec.BOOL.optionalFieldOf("shellsCollapsed", false).forGetter(state -> state.shellsCollapsed),
			Codec.BOOL.optionalFieldOf("cleansingActive", false).forGetter(state -> state.cleansingActive),
			BlockPos.CODEC.listOf().optionalFieldOf("virusSources", List.of()).forGetter(VirusWorldState::getVirusSourceList),
			Codec.unboundedMap(VirusEventType.CODEC, Codec.LONG).optionalFieldOf("eventHistory", Map.of()).forGetter(VirusWorldState::getEventHistorySnapshot),
			Codec.LONG.optionalFieldOf("lastMatrixCubeTick", 0L).forGetter(state -> state.lastMatrixCubeTick)
	).apply(instance, (infected, tierIndex, totalTicks, ticksInTier, calmUntilTick, containmentLevel, singularitySummoned, apocalypseMode, terrainCorrupted, shellsCollapsed, cleansingActive, sources, events, lastMatrixCubeTick) -> {
		VirusWorldState state = new VirusWorldState();
		state.infected = infected;
		state.tierIndex = tierIndex;
		state.totalTicks = totalTicks;
		state.ticksInTier = ticksInTier;
		state.calmUntilTick = calmUntilTick;
		state.containmentLevel = containmentLevel;
		state.singularitySummoned = singularitySummoned;
		state.apocalypseMode = apocalypseMode;
		state.terrainCorrupted = terrainCorrupted;
		state.shellsCollapsed = shellsCollapsed;
		state.cleansingActive = cleansingActive;
		sources.forEach(pos -> state.virusSources.add(pos.toImmutable()));
		state.eventHistory.putAll(events);
		state.lastMatrixCubeTick = lastMatrixCubeTick;
		return state;
	}));
	public static final PersistentStateType<VirusWorldState> TYPE = new PersistentStateType<>(ID, VirusWorldState::new, CODEC, DataFixTypes.LEVEL);

	private final Set<BlockPos> virusSources = new HashSet<>();
	private final EnumMap<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Map<BlockPos, Long> shellCooldowns = new HashMap<>();

	private boolean infected;
	private int tierIndex;
	private long totalTicks;
	private long ticksInTier;
	private long calmUntilTick;
	private int containmentLevel;
	private boolean singularitySummoned;
	private boolean apocalypseMode;
	private boolean terrainCorrupted;
	private boolean shellsCollapsed;
	private boolean cleansingActive;
	private long lastMatrixCubeTick;

	public static VirusWorldState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE);
	}

	public void tick(ServerWorld world) {
		if (!infected) {
			return;
		}

		totalTicks++;
		ticksInTier++;

		removeMissingSources(world);

		InfectionTier tier = InfectionTier.byIndex(tierIndex);

		if (!apocalypseMode && tier.getDurationTicks() > 0 && ticksInTier >= tier.getDurationTicks()) {
			advanceTier(world);
		}

		if (containmentLevel > 0 && totalTicks % 200 == 0) {
			containmentLevel = Math.max(0, containmentLevel - 1);
			markDirty();
		}

		BlockMutationHelper.mutateAroundSources(world, virusSources, tier, apocalypseMode);
		reinforceCores(world, tier);
		maybeSpawnMatrixCube(world, tier);

		if (totalTicks < calmUntilTick) {
			return;
		}

		runEvents(world, tier);
	}

	private void runEvents(ServerWorld world, InfectionTier tier) {
		if (tier.getIndex() < 2) {
			return;
		}
		Random random = world.getRandom();
		BlockPos origin = representativePos(world, random);
		if (origin == null) {
			return;
		}

		maybeMutationPulse(world, tier, random);

		if (tier.getIndex() >= 1) {
			maybeSkyfall(world, origin, tier, random);
			maybeCollapseSurge(world, origin, tier, random);
		}

		if (tier.getIndex() >= 2) {
			maybePassiveRevolt(world, origin, tier, random);
			maybeMobBuffStorm(world, origin, tier, random);
			maybeVirusBloom(world, origin, tier, random);
		}

		if (tier.getIndex() >= 3) {
			maybeVoidTear(world, origin, tier, random);
			maybeInversion(world, origin, tier, random);
		}

		if (tier.getIndex() >= 4) {
			maybeEntityDuplication(world, origin, tier, random);
			maybeSpawnSingularity(world, origin);
		}
	}

	private void maybeMutationPulse(ServerWorld world, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.MUTATION_PULSE, 400)) {
			return;
		}

		if (random.nextFloat() > 0.1F + tier.getIndex() * 0.03F) {
			return;
		}

		markEvent(VirusEventType.MUTATION_PULSE);

		int pulses = 2 + tier.getIndex();
		for (int i = 0; i < pulses; i++) {
			BlockMutationHelper.mutateAroundSources(world, virusSources, tier, apocalypseMode);
		}

		BlockPos pulseOrigin = representativePos(world, random);
		if (pulseOrigin != null) {
			world.playSound(null, pulseOrigin, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.AMBIENT, 1.2F, 0.6F + tier.getIndex() * 0.1F);
		}
	}

	private void maybeSkyfall(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.SKYFALL, 360)) {
			return;
		}

		if (random.nextFloat() > 0.08F + tier.getIndex() * 0.03F) {
			return;
		}

		List<ServerPlayerEntity> players = world.getPlayers(player -> player.squaredDistanceTo(origin.getX(), origin.getY(), origin.getZ()) < 4096.0D);
		if (players.isEmpty()) {
			return;
		}

		markEvent(VirusEventType.SKYFALL);
		for (ServerPlayerEntity player : players) {
			int volleys = 8 + tier.getIndex() * 4;
			for (int i = 0; i < volleys; i++) {
				double x = player.getX() + random.nextBetween(-6, 6);
				double z = player.getZ() + random.nextBetween(-6, 6);
				int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
				double y = top + 20.0D;

				ArrowEntity arrow = new ArrowEntity(world, x, y, z, new ItemStack(Items.ARROW), null);
				arrow.setVelocity(0.0D, -1.6D - random.nextDouble(), 0.0D);
				world.spawnEntity(arrow);

				if (tier.getIndex() >= 1 && random.nextFloat() < 0.35F + tier.getIndex() * 0.15F) {
					TntEntity tnt = new TntEntity(world, x, y, z, null);
					tnt.setFuse(Math.max(15, 50 - tier.getIndex() * 8));
					world.spawnEntity(tnt);
				}
			}

			if (random.nextFloat() < 0.4F + tier.getIndex() * 0.1F) {
				spawnArrowBarrage(world, player, tier, random);
			}
		}

		world.syncWorldEvent(WorldEvents.FIRE_EXTINGUISHED, origin, 0);
	}

	private void spawnArrowBarrage(ServerWorld world, ServerPlayerEntity player, InfectionTier tier, Random random) {
		int waves = 3 + tier.getIndex() * 2;
		for (int i = 0; i < waves; i++) {
			double offsetX = player.getX() + random.nextBetween(-2, 2) + random.nextDouble();
			double offsetZ = player.getZ() + random.nextBetween(-2, 2) + random.nextDouble();
			double y = player.getY() + 10.0D + i;
			ArrowEntity arrow = new ArrowEntity(world, offsetX, y, offsetZ, new ItemStack(Items.ARROW), null);
			arrow.setVelocity(0.0D, -1.9D - random.nextDouble() * 0.6D, 0.0D);
			world.spawnEntity(arrow);
		}

		if (tier.getIndex() >= 3) {
			TntEntity tnt = new TntEntity(world, player.getX(), player.getY() + 12.0D, player.getZ(), null);
			tnt.setFuse(Math.max(20, 40 - tier.getIndex() * 6));
			world.spawnEntity(tnt);
		}
	}

	private void maybePassiveRevolt(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.PASSIVE_REVOLT, 800)) {
			return;
		}

		List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class, new Box(origin).expand(32.0D), Entity::isAlive);
		if (animals.isEmpty()) {
			return;
		}

		markEvent(VirusEventType.PASSIVE_REVOLT);

		int revolts = Math.min(animals.size(), 2 + tier.getIndex());
		for (int i = 0; i < revolts; i++) {
			AnimalEntity animal = animals.get(random.nextInt(animals.size()));
			BlockPos pos = animal.getBlockPos();
			animal.discard();

			EntityType.ZOMBIE.spawn(world, entity -> {
				entity.refreshPositionAndAngles(pos, random.nextFloat() * 360.0F, 0.0F);
				entity.setCustomName(Text.translatable("entity.the-virus-block.corrupted_passive").formatted(Formatting.DARK_RED));
			}, pos, SpawnReason.EVENT, true, false);
		}
	}

	private void maybeMobBuffStorm(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.MOB_BUFF_STORM, 700)) {
			return;
		}

		List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, new Box(origin).expand(48.0D), Entity::isAlive);
		if (hostiles.isEmpty()) {
			return;
		}

		markEvent(VirusEventType.MOB_BUFF_STORM);

		for (HostileEntity mob : hostiles) {
			StatusEffectInstance effect = randomMobEffect(random, tier);
			mob.addStatusEffect(effect);
		}
	}

	private StatusEffectInstance randomMobEffect(Random random, InfectionTier tier) {
		int amplifier = tier.getIndex() >= 3 ? 1 : 0;
		int duration = 20 * (80 + random.nextBetween(0, 160));
		return switch (random.nextInt(5)) {
			case 0 -> new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, false, true);
			case 1 -> new StatusEffectInstance(StatusEffects.STRENGTH, duration, amplifier, false, true);
			case 2 -> new StatusEffectInstance(StatusEffects.RESISTANCE, duration, amplifier, false, true);
			case 3 -> new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, false, true);
			default -> new StatusEffectInstance(StatusEffects.REGENERATION, duration, amplifier, false, true);
		};
	}

	private void maybeCollapseSurge(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.COLLAPSE_SURGE, 900)) {
			return;
		}

		if (random.nextFloat() > 0.1F + tier.getIndex() * 0.02F) {
			return;
		}

		markEvent(VirusEventType.COLLAPSE_SURGE);
		for (int i = 0; i < 6 + tier.getIndex() * 2; i++) {
			BlockPos target = origin.add(random.nextBetween(-8, 8), random.nextBetween(-4, 4), random.nextBetween(-8, 8));
			if (!world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}

			BlockState state = world.getBlockState(target);
			if (!state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				continue;
			}

			FallingBlockEntity.spawnFromBlock(world, target, state);
		}
	}

	private void maybeVirusBloom(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.VIRUS_BLOOM, 900)) {
			return;
		}

		if (random.nextFloat() > 0.12F) {
			return;
		}

		markEvent(VirusEventType.VIRUS_BLOOM);
		BlockMutationHelper.corruptFlora(world, origin, 26 + tier.getIndex() * 6);
	}

	private void maybeVoidTear(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.VOID_TEAR, 1000)) {
			return;
		}

		if (random.nextFloat() > 0.12F) {
			return;
		}

		markEvent(VirusEventType.VOID_TEAR);

		BlockPos tearPos = origin.up(random.nextBetween(3, 12));
		Box box = new Box(tearPos).expand(8.0D + tier.getIndex() * 2.0D);
		Vec3d center = Vec3d.ofCenter(tearPos);

		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			Vec3d offset = center.subtract(living.getPos());
			Vec3d pull = offset.normalize().multiply(0.25D + tier.getIndex() * 0.05D);
			living.addVelocity(pull.x, pull.y, pull.z);
		}

		world.syncWorldEvent(WorldEvents.COMPOSTER_USED, tearPos, 0);
	}

	private void maybeInversion(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.INVERSION, 1100)) {
			return;
		}

		if (random.nextFloat() > 0.08F) {
			return;
		}

		markEvent(VirusEventType.INVERSION);

		Box box = new Box(origin).expand(32.0D);
		for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80 + tier.getIndex() * 20, 0));
		}
	}

	private void maybeEntityDuplication(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!canTrigger(VirusEventType.ENTITY_DUPLICATION, 1200)) {
			return;
		}

		List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, new Box(origin).expand(32.0D + tier.getIndex() * 4.0D), Entity::isAlive);
		if (mobs.isEmpty()) {
			return;
		}

		if (random.nextFloat() > 0.12F) {
			return;
		}

		markEvent(VirusEventType.ENTITY_DUPLICATION);

		MobEntity target = mobs.get(random.nextInt(mobs.size()));
		BlockPos spawnPos = target.getBlockPos().add(random.nextBetween(-2, 2), 0, random.nextBetween(-2, 2));
		target.getType().spawn(world, entity -> {
			if (entity instanceof MobEntity mob) {
				mob.refreshPositionAndAngles(spawnPos, target.getYaw(), target.getPitch());
				mob.setHealth(Math.max(2.0F, target.getHealth() * 0.5F));
			}
		}, spawnPos, SpawnReason.EVENT, false, false);
	}

	private void maybeSpawnSingularity(ServerWorld world, BlockPos origin) {
		if (singularitySummoned || !canTrigger(VirusEventType.SINGULARITY, 0)) {
			return;
		}

		BlockPos beaconPos = origin.up(6);
		world.playSound(null, beaconPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2.0F, 0.6F);
		world.playSound(null, beaconPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3.0F, 0.5F);

		singularitySummoned = true;
		apocalypseMode = true;
		markEvent(VirusEventType.SINGULARITY);
		markDirty();
	}

	private void removeMissingSources(ServerWorld world) {
		boolean removed = false;
		Iterator<BlockPos> iterator = virusSources.iterator();
		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}

			if (!world.getBlockState(pos).isOf(ModBlocks.VIRUS_BLOCK)) {
				iterator.remove();
				markDirty();
				removed = true;
			}
		}

		if (removed && virusSources.isEmpty() && infected) {
			endInfection(world);
		}
	}

	private BlockPos representativePos(ServerWorld world, Random random) {
		if (!virusSources.isEmpty()) {
			List<BlockPos> list = new ArrayList<>(virusSources);
			return list.get(random.nextInt(list.size()));
		}

		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			return null;
		}

		return players.get(random.nextInt(players.size())).getBlockPos();
	}

	private List<BlockPos> getVirusSourceList() {
		return List.copyOf(virusSources);
	}

	public Set<BlockPos> getVirusSources() {
		return Set.copyOf(virusSources);
	}

	private Map<VirusEventType, Long> getEventHistorySnapshot() {
		return Map.copyOf(eventHistory);
	}

	private boolean canTrigger(VirusEventType type, long cooldown) {
		long last = eventHistory.getOrDefault(type, -cooldown);
		return totalTicks - last >= cooldown;
	}

	private void markEvent(VirusEventType type) {
		eventHistory.put(type, totalTicks);
		markDirty();
	}

	private void reinforceCores(ServerWorld world, InfectionTier tier) {
		if (virusSources.isEmpty()) {
			return;
		}
		if (shellsCollapsed) {
			return;
		}

		int tierIndex = tier.getIndex();
		if (tierIndex < 3) {
			return;
		}
		for (BlockPos core : virusSources) {
			placeLayer(world, core, 1, ModBlocks.CORRUPTED_STONE, 0, tierIndex);
			if (tierIndex >= 1) {
				placeLayer(world, core, 2, ModBlocks.CORRUPTED_CRYING_OBSIDIAN, 1, tierIndex);
			}
			if (tierIndex >= 2) {
				placeLayer(world, core, 3, ModBlocks.CORRUPTED_DIAMOND, 1, tierIndex);
			}
			if (tierIndex >= 3) {
				placeLayer(world, core, 4, ModBlocks.CORRUPTED_GOLD, 2, tierIndex);
			}
			if (tierIndex >= 4) {
				placeLayer(world, core, 5, ModBlocks.CORRUPTED_IRON, 2, tierIndex);
			}
		}
	}

	private static final long BASE_SHELL_DELAY = 200L;
	private static final long RADIUS_DELAY = 60L;
	private static final long LOW_TIER_DELAY = 80L;
	private static final long SHELL_JITTER = 60L;
	private static final long PLAYER_OCCUPANCY_DELAY = 40L;
private static final Set<Block> SHELL_BLOCKS = Set.of(
		ModBlocks.CORRUPTED_STONE,
		ModBlocks.CORRUPTED_CRYING_OBSIDIAN,
		ModBlocks.CORRUPTED_DIAMOND,
		ModBlocks.CORRUPTED_GOLD,
		ModBlocks.CORRUPTED_IRON);
private static final int MAX_SHELL_RADIUS = 5;
private static final int MAX_SHELL_HEIGHT = 2;

	private void placeLayer(ServerWorld world, BlockPos center, int radius, Block block, int vertical, int tierIndex) {
		final long now = world.getTime();
		final Random random = world.getRandom();
		BlockPos.iterate(center.add(-radius, -vertical, -radius), center.add(radius, vertical, radius)).forEach(pos -> {
			if (pos.equals(center)) {
				return;
			}

			double distance = center.getSquaredDistance(pos);
			if (distance > (double)(radius * radius + Math.max(1, vertical) * 2)) {
				return;
			}

			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				return;
			}

			BlockState current = world.getBlockState(pos);
			BlockPos key = pos.toImmutable();
			if (current.isOf(ModBlocks.VIRUS_BLOCK) || current.isOf(block)) {
				shellCooldowns.remove(key);
				return;
			}

			if (!current.isAir() && current.getHardness(world, pos) < 0.0F) {
				return;
			}

			long ready = shellCooldowns.getOrDefault(key, Long.MIN_VALUE);
			if (ready == Long.MIN_VALUE) {
				long delay = computeShellDelay(radius, tierIndex) + random.nextBetween(0, (int) SHELL_JITTER);
				shellCooldowns.put(key, now + delay);
				return;
			}
			if (now < ready) {
				return;
			}

			if (isPlayerOccupyingBlock(world, pos)) {
				shellCooldowns.put(key, now + PLAYER_OCCUPANCY_DELAY);
				return;
			}

			shellCooldowns.remove(key);
			BlockState newState = stageState(block, tierIndex);
			world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
		});
	}

	private BlockState stageState(Block block, int tierIndex) {
		BlockState state = block.getDefaultState();
		CorruptionStage stage = tierIndex >= 2 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			return state.with(CorruptedStoneBlock.STAGE, stage);
		}
		if (state.contains(CorruptedGlassBlock.STAGE)) {
			return state.with(CorruptedGlassBlock.STAGE, stage);
		}
		return state;
	}

	public void collapseShells(ServerWorld world) {
		if (shellsCollapsed) {
			return;
		}
		shellsCollapsed = true;
		shellCooldowns.clear();
		for (BlockPos core : virusSources) {
			stripShells(world, core);
		}
		world.getPlayers(PlayerEntity::isAlive).forEach(player ->
				player.sendMessage(Text.translatable("message.the-virus-block.shells_collapsed").formatted(Formatting.RED), false));
		markDirty();
	}

	private void stripShells(ServerWorld world, BlockPos center) {
		BlockPos.iterate(center.add(-MAX_SHELL_RADIUS, -MAX_SHELL_HEIGHT, -MAX_SHELL_RADIUS),
				center.add(MAX_SHELL_RADIUS, MAX_SHELL_HEIGHT, MAX_SHELL_RADIUS)).forEach(pos -> {
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				return;
			}
			BlockState state = world.getBlockState(pos);
			if (isShellBlock(state)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		});
	}

	private boolean isShellBlock(BlockState state) {
		return SHELL_BLOCKS.contains(state.getBlock());
	}

	private boolean maybeTeleportVirusSources(ServerWorld world) {
		if (virusSources.isEmpty()) {
			return false;
		}
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED)) {
			return false;
		}
		int chunkRadius = world.getGameRules().getInt(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS);
		if (chunkRadius <= 0) {
			return false;
		}
		int radius = chunkRadius * 16;
		boolean moved = false;
		Random random = world.getRandom();
		for (BlockPos source : List.copyOf(virusSources)) {
			if (random.nextFloat() > 0.25F) {
				continue;
			}
			BlockPos target = findTeleportTarget(world, source, radius, random);
			if (target == null) {
				continue;
			}
			if (teleportSource(world, source, target)) {
				moved = true;
			}
		}
		if (moved) {
			shellCooldowns.clear();
			world.getPlayers(PlayerEntity::isAlive)
					.forEach(player -> player.sendMessage(Text.translatable("message.the-virus-block.teleport").formatted(Formatting.DARK_PURPLE), false));
			markDirty();
		}
		return moved;
	}

	private boolean teleportSource(ServerWorld world, BlockPos from, BlockPos to) {
		if (!world.getBlockState(from).isOf(ModBlocks.VIRUS_BLOCK)) {
			return false;
		}
		world.setBlockState(from, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(to, ModBlocks.VIRUS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		virusSources.remove(from);
		virusSources.add(to.toImmutable());
		return true;
	}

	private BlockPos findTeleportTarget(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int attempts = 0; attempts < 16; attempts++) {
			int x = origin.getX() + random.nextBetween(-radius, radius);
			int z = origin.getZ() + random.nextBetween(-radius, radius);
			BlockPos sample = new BlockPos(x, world.getBottomY(), z);
			if (!world.isChunkLoaded(ChunkPos.toLong(sample))) {
				continue;
			}
			BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, sample);
			BlockPos target = top;
			BlockPos below = target.down();
			if (target.getY() <= world.getBottomY()) {
				continue;
			}
			if (!world.getBlockState(target).isAir()) {
				continue;
			}
			if (!world.getBlockState(below).isSolidBlock(world, below)) {
				continue;
			}
			if (world.getBlockState(target).isOf(ModBlocks.VIRUS_BLOCK)) {
				continue;
			}
			return target;
		}
		return null;
	}

	private long computeShellDelay(int radius, int tierIndex) {
		long delay = BASE_SHELL_DELAY + radius * RADIUS_DELAY;
		int lowTierSteps = Math.max(0, 3 - tierIndex);
		delay += lowTierSteps * LOW_TIER_DELAY;
		return delay;
	}

	private boolean isPlayerOccupyingBlock(ServerWorld world, BlockPos pos) {
		Box box = new Box(pos);
		return !world.getPlayers(player ->
				player.isAlive()
						&& !player.isSpectator()
						&& player.getBoundingBox().intersects(box)).isEmpty();
	}

	private void spawnCoreGuardians(ServerWorld world) {
		if (virusSources.isEmpty()) {
			return;
		}

		Random random = world.getRandom();
		for (BlockPos core : virusSources) {
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
				type.spawn(world, entity -> {
					if (entity instanceof HostileEntity hostile) {
						hostile.setPersistent();
					}
					if (entity instanceof IronGolemEntity golem) {
						golem.setPlayerCreated(false);
					}
				}, spawnPos, SpawnReason.EVENT, true, false);
			}
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

	private void maybeSpawnMatrixCube(ServerWorld world, InfectionTier tier) {
		final int maxActive = 12;
		int active = MatrixCubeBlockEntity.getActiveCount(world);
		if (tier.getIndex() < 3 || active >= maxActive) {
			return;
		}

		Random random = world.getRandom();
		if (totalTicks - lastMatrixCubeTick < 2000) {
			return;
		}

		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			return;
		}

		ServerPlayerEntity anchor = players.get(random.nextInt(players.size()));
		int x = MathHelper.floor(anchor.getX()) + random.nextBetween(-48, 48);
		int z = MathHelper.floor(anchor.getZ()) + random.nextBetween(-48, 48);
		int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
		int maxY = world.getBottomY() + world.getDimension().height() - 1;
		int y = Math.min(maxY, ground + 20 + random.nextBetween(0, 20));
		BlockPos pos = new BlockPos(x, y, z);
		if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
			return;
		}
		if (!world.getBlockState(pos).isAir()) {
			return;
		}

		world.setBlockState(pos, ModBlocks.MATRIX_CUBE.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.0F, 0.6F);
		lastMatrixCubeTick = totalTicks;
		markDirty();
	}

	private void advanceTier(ServerWorld world) {
		if (tierIndex >= InfectionTier.maxIndex()) {
			apocalypseMode = true;
			markDirty();
			return;
		}

		tierIndex++;
		ticksInTier = 0;
		BlockPos pos = representativePos(world, world.getRandom());
		if (pos != null) {
			world.playSound(null, pos, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.BLOCKS, 2.5F, 1.0F);
		}

		Text message = Text.translatable("message.the-virus-block.tier_up", tierIndex + 1).formatted(Formatting.DARK_PURPLE);
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(message, false));
		maybeTeleportVirusSources(world);
		if (tierIndex >= InfectionTier.maxIndex()) {
			spawnCoreGuardians(world);
		}
		markDirty();
	}

	public void registerSource(ServerWorld world, BlockPos pos) {
		if (virusSources.add(pos.toImmutable())) {
			markDirty();
		}

		if (!infected) {
			infected = true;
			tierIndex = 0;
			totalTicks = 0;
			ticksInTier = 0;
			calmUntilTick = 0;
			containmentLevel = 0;
			apocalypseMode = false;
			singularitySummoned = false;
			terrainCorrupted = false;
			shellsCollapsed = false;
			cleansingActive = false;
			eventHistory.clear();
			markDirty();
			GlobalTerrainCorruption.trigger(world, pos);
		}
	}

	public void unregisterSource(ServerWorld world, BlockPos pos) {
		if (!virusSources.remove(pos)) {
			return;
		}

		markDirty();

		if (virusSources.isEmpty()) {
			endInfection(world);
		}
	}

	public boolean forceAdvanceTier(ServerWorld world) {
		if (tierIndex >= InfectionTier.maxIndex()) {
			apocalypseMode = true;
			markDirty();
			return false;
		}

		advanceTier(world);
		return true;
	}

	public void applyContainmentCharge(int amount) {
		containmentLevel = MathHelper.clamp(containmentLevel + amount, 0, 10);
		markDirty();
	}

	public void applyPurification(long ticks) {
		calmUntilTick = Math.max(calmUntilTick, totalTicks + ticks);
		markDirty();
	}

	public void forceContainmentReset(ServerWorld world) {
		for (BlockPos pos : new ArrayList<>(virusSources)) {
			if (world.isChunkLoaded(ChunkPos.toLong(pos))) {
				world.breakBlock(pos, false);
			}
		}

		virusSources.clear();
		beginCleansing();
		VirusTierBossBar.update(world, this);
		GlobalTerrainCorruption.cleanse(world);
		MatrixCubeBlockEntity.destroyAll(world);
		shellCooldowns.clear();
		singularitySummoned = false;
		apocalypseMode = false;
		terrainCorrupted = false;
		shellsCollapsed = false;
		endInfection(world);
	}

	private void endInfection(ServerWorld world) {
		if (!infected) {
			return;
		}

		infected = false;
		tierIndex = 0;
		totalTicks = 0;
		ticksInTier = 0;
		calmUntilTick = 0;
		containmentLevel = 0;
		apocalypseMode = false;
		singularitySummoned = false;
		terrainCorrupted = false;
		shellsCollapsed = false;
		beginCleansing();
		eventHistory.clear();
		shellCooldowns.clear();
		markDirty();

		MatrixCubeBlockEntity.destroyAll(world);
		SingularityManager.clearWaveMobs(world);
		SingularityManager.stop(world);
		Text message = Text.translatable("message.the-virus-block.cleansed").formatted(Formatting.AQUA);
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(message, false));
	}

	public boolean isInfected() {
		return infected;
	}

	public InfectionTier getCurrentTier() {
		return InfectionTier.byIndex(tierIndex);
	}

	public long getTicksInTier() {
		return ticksInTier;
	}

	public int getContainmentLevel() {
		return containmentLevel;
	}

	public boolean isApocalypseMode() {
		return apocalypseMode;
	}

	public boolean isTerrainGloballyCorrupted() {
		return terrainCorrupted;
	}

	public boolean enableTerrainCorruption() {
		if (terrainCorrupted) {
			return false;
		}
		terrainCorrupted = true;
		cleansingActive = false;
		markDirty();
		return true;
	}

	public boolean isCleansingActive() {
		return cleansingActive;
	}

	public void beginCleansing() {
		if (!cleansingActive) {
			cleansingActive = true;
			terrainCorrupted = false;
			markDirty();
		}
	}

	public boolean isCalm() {
		return totalTicks < calmUntilTick;
	}

	public boolean isSingularitySummoned() {
		return singularitySummoned;
	}

	public void disturbByPlayer(ServerWorld world) {
		if (!infected) {
			return;
		}

		InfectionTier tier = InfectionTier.byIndex(tierIndex);
		long duration = tier.getDurationTicks();
		long bonus = 40L + tier.getIndex() * 30L;
		if (tier.getIndex() >= 3) {
			bonus += 60L;
		}
		if (singularitySummoned) {
			bonus += 80L;
		}
		ticksInTier += bonus;
		if (duration > 0) {
			ticksInTier = Math.min(ticksInTier, duration);
		}

		calmUntilTick = Math.min(calmUntilTick, totalTicks);
		BlockPos disturbance = representativePos(world, world.getRandom());
		if (disturbance == null && !virusSources.isEmpty()) {
			disturbance = virusSources.iterator().next();
		}
		if (disturbance == null) {
			disturbance = BlockPos.ORIGIN;
		}
		world.playSound(null, disturbance, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.BLOCKS, 1.0F, 0.4F);
		markDirty();
	}

}

