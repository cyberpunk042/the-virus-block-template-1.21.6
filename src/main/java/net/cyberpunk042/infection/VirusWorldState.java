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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.infection.singularity.SingularityManager;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.TierFeatureGroup;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.WorldEvents;

public class VirusWorldState extends PersistentState {
	public static final String ID = TheVirusBlock.MOD_ID + "_infection_state";
	private static final int FLAG_SINGULARITY = 1;
	private static final int FLAG_APOCALYPSE = 1 << 1;
	private static final int FLAG_TERRAIN = 1 << 2;
	private static final int FLAG_SHELLS = 1 << 3;
	private static final int FLAG_CLEANSING = 1 << 4;
	private static final Codec<VirusWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("infected", false).forGetter(state -> state.infected),
			Codec.INT.optionalFieldOf("tierIndex", 0).forGetter(state -> state.tierIndex),
			Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(state -> state.totalTicks),
			Codec.LONG.optionalFieldOf("ticksInTier", 0L).forGetter(state -> state.ticksInTier),
			Codec.LONG.optionalFieldOf("calmUntilTick", 0L).forGetter(state -> state.calmUntilTick),
			Codec.INT.optionalFieldOf("containmentLevel", 0).forGetter(state -> state.containmentLevel),
			Codec.INT.optionalFieldOf("stateFlags", 0).forGetter(VirusWorldState::encodeFlags),
			Codec.DOUBLE.optionalFieldOf("healthScale", 1.0D).forGetter(state -> state.healthScale),
			Codec.DOUBLE.optionalFieldOf("currentHealth", 0.0D).forGetter(state -> state.currentHealth),
			Codec.LONG.listOf().optionalFieldOf("pillarChunks", List.of()).forGetter(VirusWorldState::getPillarChunksSnapshot),
			BlockPos.CODEC.listOf().optionalFieldOf("virusSources", List.of()).forGetter(VirusWorldState::getVirusSourceList),
			Codec.unboundedMap(VirusEventType.CODEC, Codec.LONG).optionalFieldOf("eventHistory", Map.of()).forGetter(VirusWorldState::getEventHistorySnapshot),
			Codec.LONG.optionalFieldOf("lastMatrixCubeTick", 0L).forGetter(state -> state.lastMatrixCubeTick)
	).apply(instance, (infected, tierIndex, totalTicks, ticksInTier, calmUntilTick, containmentLevel, stateFlags, healthScale, currentHealth, pillarChunks, sources, events, lastMatrixCubeTick) -> {
		VirusWorldState state = new VirusWorldState();
		state.infected = infected;
		state.tierIndex = tierIndex;
		state.totalTicks = totalTicks;
		state.ticksInTier = ticksInTier;
		state.calmUntilTick = calmUntilTick;
		state.containmentLevel = containmentLevel;
		state.singularitySummoned = (stateFlags & FLAG_SINGULARITY) != 0;
		state.apocalypseMode = (stateFlags & FLAG_APOCALYPSE) != 0;
		state.terrainCorrupted = (stateFlags & FLAG_TERRAIN) != 0;
		state.shellsCollapsed = (stateFlags & FLAG_SHELLS) != 0;
		state.cleansingActive = (stateFlags & FLAG_CLEANSING) != 0;
		state.healthScale = healthScale;
		state.currentHealth = currentHealth;
		pillarChunks.forEach(chunk -> state.pillarChunks.add(chunk.longValue()));
		sources.forEach(pos -> state.virusSources.add(pos.toImmutable()));
		state.eventHistory.putAll(events);
		state.lastMatrixCubeTick = lastMatrixCubeTick;
		return state;
	}));
	public static final PersistentStateType<VirusWorldState> TYPE = new PersistentStateType<>(ID, VirusWorldState::new, CODEC, DataFixTypes.LEVEL);

	private final Set<BlockPos> virusSources = new HashSet<>();
	private final EnumMap<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Map<BlockPos, Long> shellCooldowns = new HashMap<>();
	private final LongSet pillarChunks = new LongOpenHashSet();

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
	private double healthScale = 1.0D;
	private double currentHealth;
	private int surfaceMutationBudget;
	private long surfaceMutationBudgetTick = -1L;
	@SuppressWarnings("unused")
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
		ensureHealthInitialized(tier);
		int tierDuration = getTierDuration(tier);

		if (!apocalypseMode && tierDuration > 0 && ticksInTier >= tierDuration) {
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
		boolean tier2Active = TierCookbook.anyEnabled(world, tier, apocalypseMode, TierFeatureGroup.TIER2_EVENT);
		boolean tier3Active = TierCookbook.anyEnabled(world, tier, apocalypseMode, TierFeatureGroup.TIER3_EXTRA);
		if (!tier2Active && !tier3Active) {
			return;
		}
		Random random = world.getRandom();
		BlockPos origin = representativePos(world, random);
		if (origin == null) {
			return;
		}

		if (tier2Active) {
			maybeMutationPulse(world, tier, random);
			maybeSkyfall(world, origin, tier, random);
			maybeCollapseSurge(world, origin, tier, random);
			maybePassiveRevolt(world, origin, tier, random);
			maybeMobBuffStorm(world, origin, tier, random);
			maybeVirusBloom(world, origin, tier, random);
		}

		if (tier3Active) {
			maybeVoidTear(world, origin, tier, random);
			maybeInversion(world, origin, tier, random);
			maybeEntityDuplication(world, origin, tier, random);
			maybeSpawnSingularity(world, origin);
		}
	}

	private void maybeMutationPulse(ServerWorld world, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_MUTATION_PULSE)) {
			return;
		}
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
		CorruptionProfiler.logTierEvent(world, VirusEventType.MUTATION_PULSE, pulseOrigin, "pulses=" + pulses);
	}

	private void maybeSkyfall(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_SKYFALL)) {
			return;
		}
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
		int volleys = 8 + tier.getIndex() * 4;
		int tntSpawned = 0;
		int arrowBarrageCount = 0;
		int totalArrows = 0;
		for (ServerPlayerEntity player : players) {
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
					tntSpawned++;
				}
			}
			totalArrows += volleys;

			if (random.nextFloat() < 0.4F + tier.getIndex() * 0.1F) {
				spawnArrowBarrage(world, player, tier, random);
				arrowBarrageCount++;
			}
		}

		world.syncWorldEvent(WorldEvents.FIRE_EXTINGUISHED, origin, 0);
		CorruptionProfiler.logTierEvent(world, VirusEventType.SKYFALL, origin,
				"players=" + players.size() + " arrows=" + totalArrows + " tnt=" + tntSpawned + " barrages=" + arrowBarrageCount);
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
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_PASSIVE_REVOLT)) {
			return;
		}
		if (!canTrigger(VirusEventType.PASSIVE_REVOLT, 800)) {
			return;
		}

		List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class, new Box(origin).expand(32.0D), Entity::isAlive);
		if (animals.isEmpty()) {
			return;
		}

		markEvent(VirusEventType.PASSIVE_REVOLT);

		int revolts = Math.min(animals.size(), 2 + tier.getIndex());
		int converted = 0;
		for (int i = 0; i < revolts; i++) {
			AnimalEntity animal = animals.get(random.nextInt(animals.size()));
			BlockPos pos = animal.getBlockPos();
			animal.discard();

			EntityType.ZOMBIE.spawn(world, entity -> {
				entity.refreshPositionAndAngles(pos, random.nextFloat() * 360.0F, 0.0F);
				entity.setCustomName(Text.translatable("entity.the-virus-block.corrupted_passive").formatted(Formatting.DARK_RED));
			}, pos, SpawnReason.EVENT, true, false);
			converted++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.PASSIVE_REVOLT, origin, "converted=" + converted);
	}

	private void maybeMobBuffStorm(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_MOB_BUFF_STORM)) {
			return;
		}
		if (!canTrigger(VirusEventType.MOB_BUFF_STORM, 700)) {
			return;
		}

		List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, new Box(origin).expand(48.0D), Entity::isAlive);
		if (hostiles.isEmpty()) {
			return;
		}

		markEvent(VirusEventType.MOB_BUFF_STORM);

		int buffed = 0;
		for (HostileEntity mob : hostiles) {
			StatusEffectInstance effect = randomMobEffect(random, tier);
			mob.addStatusEffect(effect);
			buffed++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.MOB_BUFF_STORM, origin, "buffed=" + buffed);
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
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_COLLAPSE_SURGE)) {
			return;
		}
		if (!canTrigger(VirusEventType.COLLAPSE_SURGE, 900)) {
			return;
		}

		if (random.nextFloat() > 0.1F + tier.getIndex() * 0.02F) {
			return;
		}

		markEvent(VirusEventType.COLLAPSE_SURGE);
		int columns = 0;
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
			columns++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.COLLAPSE_SURGE, origin, "columns=" + columns);
	}

	private void maybeVirusBloom(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_VIRUS_BLOOM)) {
			return;
		}
		if (!canTrigger(VirusEventType.VIRUS_BLOOM, 900)) {
			return;
		}

		if (random.nextFloat() > 0.12F) {
			return;
		}

		markEvent(VirusEventType.VIRUS_BLOOM);
		int radius = 26 + tier.getIndex() * 6;
		BlockMutationHelper.corruptFlora(world, origin, radius);
		CorruptionProfiler.logTierEvent(world, VirusEventType.VIRUS_BLOOM, origin, "radius=" + radius);
	}

	private void maybeVoidTear(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_VOID_TEAR)) {
			return;
		}
		if (!canTrigger(VirusEventType.VOID_TEAR, 1000)) {
			return;
		}

		if (random.nextFloat() > 0.12F) {
			return;
		}

		markEvent(VirusEventType.VOID_TEAR);

		BlockPos tearPos = origin.up(random.nextBetween(3, 12));
		double radius = 8.0D + tier.getIndex() * 2.0D;
		Box box = new Box(tearPos).expand(radius);
		Vec3d center = Vec3d.ofCenter(tearPos);

		int affected = 0;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			Vec3d offset = center.subtract(living.getPos());
			Vec3d pull = offset.normalize().multiply(0.25D + tier.getIndex() * 0.05D);
			living.addVelocity(pull.x, pull.y, pull.z);
			affected++;
		}

		world.syncWorldEvent(WorldEvents.COMPOSTER_USED, tearPos, 0);
		CorruptionProfiler.logTierEvent(world, VirusEventType.VOID_TEAR, tearPos, "affected=" + affected + " radius=" + radius);
	}

	private void maybeInversion(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_INVERSION)) {
			return;
		}
		if (!canTrigger(VirusEventType.INVERSION, 1100)) {
			return;
		}

		if (random.nextFloat() > 0.08F) {
			return;
		}

		markEvent(VirusEventType.INVERSION);

		Box box = new Box(origin).expand(32.0D);
		int affected = 0;
		for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80 + tier.getIndex() * 20, 0));
			affected++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.INVERSION, origin, "affected=" + affected);
	}

	private void maybeEntityDuplication(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, apocalypseMode, TierFeature.EVENT_ENTITY_DUPLICATION)) {
			return;
		}
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
		Entity spawned = target.getType().spawn(world, entity -> {
			if (entity instanceof MobEntity mob) {
				mob.refreshPositionAndAngles(spawnPos, target.getYaw(), target.getPitch());
				mob.setHealth(Math.max(2.0F, target.getHealth() * 0.5F));
			}
		}, spawnPos, SpawnReason.EVENT, false, false);
		if (spawned != null) {
			CorruptionProfiler.logTierEvent(world, VirusEventType.ENTITY_DUPLICATION, spawnPos,
					"type=" + spawned.getType().toString());
		}
	}

	private void maybeSpawnSingularity(ServerWorld world, BlockPos origin) {
		if (!TierCookbook.isEnabled(world, getCurrentTier(), apocalypseMode, TierFeature.EVENT_SINGULARITY)) {
			return;
		}
		if (singularitySummoned || !canTrigger(VirusEventType.SINGULARITY, 0)) {
			return;
		}

		BlockPos beaconPos = origin.up(6);
		world.playSound(null, beaconPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2.0F, 0.6F);
		world.playSound(null, beaconPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3.0F, 0.5F);

		singularitySummoned = true;
		apocalypseMode = true;
		markEvent(VirusEventType.SINGULARITY);
		CorruptionProfiler.logTierEvent(world, VirusEventType.SINGULARITY, beaconPos, null);
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

	private List<Long> getPillarChunksSnapshot() {
		List<Long> list = new ArrayList<>(pillarChunks.size());
		pillarChunks.forEach((long value) -> list.add(value));
		return list;
	}

	public Set<BlockPos> getVirusSources() {
		return Set.copyOf(virusSources);
	}

	private Map<VirusEventType, Long> getEventHistorySnapshot() {
		return Map.copyOf(eventHistory);
	}

	public boolean areLiquidsCorrupted(ServerWorld world) {
		return infected && TierCookbook.isEnabled(world, getCurrentTier(), apocalypseMode, TierFeature.LIQUID_CORRUPTION);
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
		if (tierIndex >= 2) {
			spawnCorruptedPillars(world, tierIndex);
		}
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

	private void spawnCorruptedPillars(ServerWorld world, int tierIndex) {
		Random random = world.getRandom();
		for (BlockPos core : virusSources) {
			ChunkPos chunk = new ChunkPos(core);
			long key = chunk.toLong();
			if (pillarChunks.contains(key)) {
				continue;
			}
			if (!world.isChunkLoaded(chunk.x, chunk.z)) {
				continue;
			}
			BlockPos base = findPillarBase(world, chunk, random);
			if (base == null) {
				continue;
			}
			buildPillar(world, base, tierIndex, random);
			pillarChunks.add(key);
			markDirty();
		}
	}

	private BlockPos findPillarBase(ServerWorld world, ChunkPos chunk, Random random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			int x = chunk.getStartX() + random.nextBetween(1, 14);
			int z = chunk.getStartZ() + random.nextBetween(1, 14);
			if (!world.isChunkLoaded(chunk.x, chunk.z)) {
				continue;
			}
			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			if (topY < world.getBottomY()) {
				continue;
			}
			return new BlockPos(x, topY, z);
		}
		return null;
	}

	private void buildPillar(ServerWorld world, BlockPos base, int tierIndex, Random random) {
		int height = 4 + random.nextBetween(0, 3);
		BlockState state = ModBlocks.CORRUPTED_STONE.getDefaultState();
		CorruptionStage stage = tierIndex >= 4 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			state = state.with(CorruptedStoneBlock.STAGE, stage);
		}
		for (int i = 0; i < height; i++) {
			BlockPos target = base.up(i);
			world.setBlockState(target, state, Block.NOTIFY_LISTENERS);
		}
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
		int maxActive = Math.max(1, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_MAX_ACTIVE));
		int active = MatrixCubeBlockEntity.getActiveCount(world);
		if (active >= maxActive) {
			CorruptionProfiler.logMatrixCubeSkip(world, "active_limit", null, active, maxActive);
			return;
		}

		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			CorruptionProfiler.logMatrixCubeSkip(world, "no_players", null, active, maxActive);
			return;
		}

		Random random = world.getRandom();
		int attempts = Math.max(1, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_SPAWN_ATTEMPTS));
		int placements = 0;
		for (int attempt = 0; attempt < attempts && active < maxActive; attempt++) {
			ServerPlayerEntity anchor = players.get(random.nextInt(players.size()));
			int x = MathHelper.floor(anchor.getX()) + random.nextBetween(-48, 48);
			int z = MathHelper.floor(anchor.getZ()) + random.nextBetween(-48, 48);
			BlockPos columnPos = new BlockPos(x, world.getBottomY(), z);
			if (!world.isChunkLoaded(ChunkPos.toLong(columnPos))) {
				continue;
			}

			int maxY = world.getBottomY() + world.getDimension().height() - 1;
			int anchorY = MathHelper.floor(anchor.getY());
			int minSeaLevel = world.getSeaLevel() + 64;
			int minAnchor = anchorY + 96;
			int base = Math.max(minSeaLevel, minAnchor);
			int y = Math.min(maxY - 4, base + random.nextBetween(0, 24));
			BlockPos pos = new BlockPos(x, y, z);
			CorruptionProfiler.logMatrixCubeAttempt(world, anchor.getBlockPos(), pos, world.getSeaLevel(), anchorY, base, maxY);
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}
			if (!world.getBlockState(pos).isAir()) {
				continue;
			}

			FallingMatrixCubeEntity entity = new FallingMatrixCubeEntity(world, pos, ModBlocks.MATRIX_CUBE.getDefaultState());
			MatrixCubeBlockEntity.register(world, entity.getUuid(), pos);
			entity.markRegistered();
			if (world.spawnEntity(entity)) {
				world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.0F, 0.6F);
				CorruptionProfiler.logMatrixCubeSpawn(world, pos, active, maxActive);
				CorruptionProfiler.logMatrixCubeEntity(world, pos);
				active++;
				placements++;
			} else {
				MatrixCubeBlockEntity.unregister(world, entity.getUuid());
			}
		}

		if (placements == 0) {
			CorruptionProfiler.logMatrixCubeSkip(world, "attempts_exhausted", "attempts=" + attempts, active, maxActive);
		} else {
			markDirty();
		}
	}

	private void advanceTier(ServerWorld world) {
		if (tierIndex >= InfectionTier.maxIndex()) {
			apocalypseMode = true;
			markDirty();
			return;
		}

		tierIndex++;
		ticksInTier = 0;
		resetHealthForTier(InfectionTier.byIndex(tierIndex));
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
			resetHealthForTier(InfectionTier.byIndex(tierIndex));
			pillarChunks.clear();
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

	public boolean reduceMaxHealth(ServerWorld world, double factor) {
		if (!infected || factor <= 0.0D || factor >= 1.0D) {
			return false;
		}

		double previousMax = Math.max(1.0D, getMaxHealth());
		double newMax = Math.max(1.0D, previousMax * factor);
		if (Math.abs(newMax - previousMax) < 0.0001D) {
			return false;
		}

		double percent = previousMax <= 0.0D ? 1.0D : MathHelper.clamp(currentHealth / previousMax, 0.0D, 1.0D);
		healthScale = Math.max(0.1D, healthScale * factor);
		double actualMax = getMaxHealth();
		currentHealth = Math.max(1.0D, Math.min(actualMax, percent * actualMax));
		markDirty();
		VirusTierBossBar.update(world, this);
		return true;
	}

	public boolean bleedHealth(ServerWorld world, double fraction) {
		if (!infected || fraction <= 0.0D) {
			return false;
		}

		double amount = Math.max(1.0D, getMaxHealth() * MathHelper.clamp(fraction, 0.0D, 1.0D));
		return applyHealthDamage(world, amount);
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
		pillarChunks.clear();
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
		healthScale = 1.0D;
		currentHealth = 0.0D;
		beginCleansing();
		eventHistory.clear();
		shellCooldowns.clear();
		pillarChunks.clear();
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

	public double getMaxHealth() {
		return getMaxHealthForTier(getCurrentTier());
	}

	public double getCurrentHealth() {
		return currentHealth;
	}

	public double getHealthPercent() {
		double max = getMaxHealth();
		if (max <= 0.0D) {
			return 0.0D;
		}
		return MathHelper.clamp(currentHealth / max, 0.0D, 1.0D);
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
		long bonus = 40L + tier.getIndex() * 30L;
		if (tier.getIndex() >= 3) {
			bonus += 60L;
		}
		if (singularitySummoned) {
			bonus += 80L;
		}
		applyDisturbance(world, bonus);
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

	public int claimSurfaceMutations(ServerWorld world, InfectionTier tier, boolean apocalypseMode, int requested) {
		if (!infected || requested <= 0) {
			return 0;
		}

		if (surfaceMutationBudgetTick != totalTicks) {
			int base = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SURFACE_CORRUPT_ATTEMPTS), 0, 4096);
			int scaled = MathHelper.clamp(base * Math.max(1, tier.getLevel()), 0, 4096);
			if (apocalypseMode) {
				scaled = Math.min(4096, scaled + base / 2);
			}
			surfaceMutationBudget = scaled;
			surfaceMutationBudgetTick = totalTicks;
		}

		if (surfaceMutationBudget <= 0) {
			return 0;
		}

		int granted = Math.min(requested, surfaceMutationBudget);
		surfaceMutationBudget -= granted;
		return granted;
	}

	private void ensureHealthInitialized(InfectionTier tier) {
		if (healthScale <= 0.0D) {
			healthScale = 1.0D;
		}
		double max = getMaxHealthForTier(tier);
		if (currentHealth <= 0.0D || currentHealth > max) {
			currentHealth = max;
			markDirty();
		}
	}

	private void resetHealthForTier(InfectionTier tier) {
		healthScale = 1.0D;
		currentHealth = getMaxHealthForTier(tier);
		markDirty();
	}

	public void reflectCoreDamage(ServerWorld world, float fraction) {
		if (!infected || fraction <= 0.0F || !SingularityManager.isActive(world)) {
			return;
		}

		double damage = Math.max(1.0D, getMaxHealth() * MathHelper.clamp(fraction, 0.0F, 1.0F));
		applyHealthDamage(world, damage);
	}

	public void handleExplosionImpact(ServerWorld world, Vec3d center, double radius) {
		if (!infected || radius <= 0.0D || virusSources.isEmpty()) {
			return;
		}

		double radiusSq = radius * radius;
		boolean hit = false;
		for (BlockPos source : virusSources) {
			if (source.toCenterPos().squaredDistanceTo(center) <= radiusSq) {
				hit = true;
				break;
			}
		}
		if (!hit) {
			return;
		}

		long bonus = MathHelper.floor(60L + radius * 6.0D);
		bonus = MathHelper.clamp(bonus, 40L, 400L);
		if (SingularityManager.isActive(world)) {
			double damageScale = MathHelper.clamp(radius / 4.0D, 0.5D, 3.5D);
			float coreDamage = SingularityManager.EXPLOSION_DAMAGE * (float) damageScale;
			SingularityManager.onVirusBlockDamage(world, coreDamage);
			applyHealthDamage(world, getMaxHealth() * (damageScale * 0.02D));
			return;
		}

		if (apocalypseMode) {
			double damageScale = MathHelper.clamp(radius / 4.0D, 0.5D, 3.5D);
			applyHealthDamage(world, getMaxHealth() * (damageScale * 0.02D));
			return;
		}

		applyDisturbance(world, bonus);
	}

	private void applyDisturbance(ServerWorld world, long bonus) {
		if (bonus <= 0) {
			return;
		}

		ticksInTier += bonus;
		int duration = getTierDuration(getCurrentTier());
		if (duration > 0 && ticksInTier > duration) {
			ticksInTier = duration;
		}
		calmUntilTick = Math.min(calmUntilTick, totalTicks);
		markDirty();
	}

	public boolean applyHealthDamage(ServerWorld world, double amount) {
		if (!infected || amount <= 0.0D) {
			return false;
		}

		double previous = currentHealth;
		currentHealth = Math.max(0.0D, currentHealth - amount);
		if (Math.abs(previous - currentHealth) < 0.0001D) {
			return false;
		}

		double max = getMaxHealth();
		if (!shellsCollapsed && previous > max * 0.5D && currentHealth <= max * 0.5D) {
			collapseShells(world);
		}

		markDirty();
		VirusTierBossBar.update(world, this);
		if (currentHealth <= 0.0D) {
			handleVirusDefeat(world);
		}
		return true;
	}

	private void handleVirusDefeat(ServerWorld world) {
		forceContainmentReset(world);
	}

	private double getMaxHealthForTier(InfectionTier tier) {
		double base = Math.max(1.0D, tier.getBaseHealth());
		double scale = MathHelper.clamp(healthScale, 0.1D, 2.5D);
		return Math.max(1.0D, base * scale);
	}

	public int getCurrentTierDuration() {
		return getTierDuration(getCurrentTier());
	}

	private int getTierDuration(InfectionTier tier) {
		return tier.getDurationTicks();
	}

	private static int encodeFlags(VirusWorldState state) {
		int flags = 0;
		if (state.singularitySummoned) {
			flags |= FLAG_SINGULARITY;
		}
		if (state.apocalypseMode) {
			flags |= FLAG_APOCALYPSE;
		}
		if (state.terrainCorrupted) {
			flags |= FLAG_TERRAIN;
		}
		if (state.shellsCollapsed) {
			flags |= FLAG_SHELLS;
		}
		if (state.cleansingActive) {
			flags |= FLAG_CLEANSING;
		}
		return flags;
	}

}

