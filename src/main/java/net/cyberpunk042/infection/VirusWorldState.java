package net.cyberpunk042.infection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.entity.CorruptedTntEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.mixin.GuardianEntityAccessor;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.network.VoidTearBurstPayload;
import net.cyberpunk042.network.VoidTearSpawnPayload;
import net.cyberpunk042.network.ShieldFieldSpawnPayload;
import net.cyberpunk042.network.ShieldFieldRemovePayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GuardianEntity;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

public class VirusWorldState extends PersistentState {
	public static final String ID = TheVirusBlock.MOD_ID + "_infection_state";
	private static final int FLAG_APOCALYPSE = 1 << 1;
	private static final int FLAG_TERRAIN = 1 << 2;
	private static final int FLAG_SHELLS = 1 << 3;
	private static final int FLAG_CLEANSING = 1 << 4;
	private static final Codec<ShieldField> SHIELD_FIELD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("id").forGetter(ShieldField::id),
			BlockPos.CODEC.fieldOf("center").forGetter(ShieldField::center),
			Codec.DOUBLE.fieldOf("radius").forGetter(ShieldField::radius),
			Codec.LONG.fieldOf("createdTick").forGetter(ShieldField::createdTick)
	).apply(instance, ShieldField::new));
private static final Codec<BoobytrapDefaults> BOOBYTRAP_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("captured").forGetter(BoobytrapDefaults::captured),
			Codec.BOOL.fieldOf("enabled").forGetter(BoobytrapDefaults::enabled),
			Codec.INT.fieldOf("spawn").forGetter(BoobytrapDefaults::spawn),
			Codec.INT.fieldOf("trap").forGetter(BoobytrapDefaults::trap)
	).apply(instance, BoobytrapDefaults::new));
private static final Codec<SpreadSnapshot> SPREAD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.listOf().fieldOf("pillarChunks").forGetter(SpreadSnapshot::pillars),
			BlockPos.CODEC.listOf().fieldOf("virusSources").forGetter(SpreadSnapshot::sources)
	).apply(instance, SpreadSnapshot::new));
private static final Codec<VirusWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("infected", false).forGetter(state -> state.infected),
			Codec.BOOL.optionalFieldOf("dormant", false).forGetter(state -> state.dormant),
			Codec.INT.optionalFieldOf("tierIndex", 0).forGetter(state -> state.tierIndex),
			Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(state -> state.totalTicks),
			Codec.LONG.optionalFieldOf("ticksInTier", 0L).forGetter(state -> state.ticksInTier),
			Codec.INT.optionalFieldOf("containmentLevel", 0).forGetter(state -> state.containmentLevel),
			Codec.INT.optionalFieldOf("stateFlags", 0).forGetter(VirusWorldState::encodeFlags),
			Codec.DOUBLE.optionalFieldOf("healthScale", 1.0D).forGetter(state -> state.healthScale),
			Codec.DOUBLE.optionalFieldOf("currentHealth", 0.0D).forGetter(state -> state.currentHealth),
			Codec.STRING.optionalFieldOf("difficulty", VirusDifficulty.HARD.getId()).forGetter(state -> state.difficulty.getId()),
			Codec.BOOL.optionalFieldOf("difficultyPromptShown", false).forGetter(state -> state.difficultyPromptShown),
			BOOBYTRAP_CODEC.optionalFieldOf("boobytrapDefaults").forGetter(VirusWorldState::getBoobytrapDefaultsRecord),
			SPREAD_CODEC.optionalFieldOf("spreadData").forGetter(state -> state.getSpreadSnapshot()),
			Codec.unboundedMap(VirusEventType.CODEC, Codec.LONG).optionalFieldOf("eventHistory", Map.of()).forGetter(VirusWorldState::getEventHistorySnapshot),
			Codec.LONG.optionalFieldOf("lastMatrixCubeTick", 0L).forGetter(state -> state.lastMatrixCubeTick),
			SHIELD_FIELD_CODEC.listOf().optionalFieldOf("shieldFields", List.of()).forGetter(VirusWorldState::getShieldFieldsSnapshot)
	).apply(instance, (infected, dormant, tierIndex, totalTicks, ticksInTier, containmentLevel, stateFlags, healthScale, currentHealth, difficultyId, difficultyPromptShown, boobytrapDefaults, spreadData, events, lastMatrixCubeTick, shields) -> {
		VirusWorldState state = new VirusWorldState();
		state.infected = infected;
		state.dormant = dormant;
		state.tierIndex = tierIndex;
		state.totalTicks = totalTicks;
		state.ticksInTier = ticksInTier;
		state.containmentLevel = containmentLevel;
		state.apocalypseMode = (stateFlags & FLAG_APOCALYPSE) != 0;
		state.terrainCorrupted = (stateFlags & FLAG_TERRAIN) != 0;
		state.shellsCollapsed = (stateFlags & FLAG_SHELLS) != 0;
		state.cleansingActive = (stateFlags & FLAG_CLEANSING) != 0;
		state.healthScale = healthScale;
		state.currentHealth = currentHealth;
		state.difficulty = VirusDifficulty.fromId(difficultyId);
		state.difficultyPromptShown = difficultyPromptShown;
		boobytrapDefaults.ifPresent(def -> {
			state.boobytrapDefaultsCaptured = def.captured();
			state.defaultBoobytrapsEnabled = def.enabled();
			state.defaultWormSpawnChance = def.spawn();
			state.defaultWormTrapSpawnChance = def.trap();
		});
		spreadData.ifPresent(data -> {
			data.pillars().forEach(chunk -> state.pillarChunks.add(chunk.longValue()));
			data.sources().forEach(pos -> state.virusSources.add(pos.toImmutable()));
		});
		state.eventHistory.putAll(events);
		state.lastMatrixCubeTick = lastMatrixCubeTick;
		shields.forEach(field -> state.activeShields.put(field.id(), field));
		return state;
	}));
	public static final PersistentStateType<VirusWorldState> TYPE = new PersistentStateType<>(ID, VirusWorldState::new, CODEC, DataFixTypes.LEVEL);

	private final Set<BlockPos> virusSources = new HashSet<>();
	private final EnumMap<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Map<BlockPos, Long> shellCooldowns = new HashMap<>();
	private final Object2LongMap<UUID> guardianBeams = new Object2LongOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousInventoryTicks = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousContactTicks = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> helmetPingTimers = new Object2IntOpenHashMap<>();
	private final Object2DoubleMap<UUID> heavyPantsVoidWear = new Object2DoubleOpenHashMap<>();
	private final LongSet pillarChunks = new LongOpenHashSet();
	private final List<VoidTearInstance> activeVoidTears = new ArrayList<>();
	private final List<PendingVoidTear> pendingVoidTears = new ArrayList<>();
	private final Map<Long, ShieldField> activeShields = new HashMap<>();
	private long nextVoidTearId;
	private static final double CLEANSE_PURGE_RADIUS = 32.0D;
	private static final int INFECTIOUS_CONTACT_THRESHOLD = 40;
	private static final int INFECTIOUS_CONTACT_INTERVAL = 20;
	private static final int INFECTIOUS_INVENTORY_THRESHOLD = 40;
	private static final int INFECTIOUS_INVENTORY_INTERVAL = 80;
	private static final int RUBBER_CONTACT_THRESHOLD_BONUS = 60;
	private static final int RUBBER_CONTACT_INTERVAL_BONUS = 10;
	private static final float RUBBER_CONTACT_DAMAGE = 0.5F;
	private static final double HEAVY_PANTS_VOID_TEAR_WEAR = 4.0D / 3.0D;
	private static final int AUGMENTED_HELMET_PING_INTERVAL = 80;
	private static final double HELMET_PING_MAX_PARTICLE_DISTANCE = 32.0D;
	private static final String[] COMPASS_POINTS = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private boolean infected;
	private boolean dormant;
	private int tierIndex;
	private long totalTicks;
	private long ticksInTier;
	private int containmentLevel;
	private boolean apocalypseMode;
	private boolean terrainCorrupted;
	private boolean shellsCollapsed;
	private boolean cleansingActive;
	private double healthScale = 1.0D;
	private double currentHealth;
	private boolean boobytrapDefaultsCaptured;
	private boolean defaultBoobytrapsEnabled = true;
	private int defaultWormSpawnChance = -1;
	private int defaultWormTrapSpawnChance = -1;
	private int surfaceMutationBudget;
	private long surfaceMutationBudgetTick = -1L;
	@SuppressWarnings("unused")
	private long lastMatrixCubeTick;
	private VirusDifficulty difficulty = VirusDifficulty.HARD;
	private boolean difficultyPromptShown;
	private static final Identifier EXTREME_HEALTH_MODIFIER_ID = Identifier.of(TheVirusBlock.MOD_ID, "extreme_health_penalty");
	private Optional<BoobytrapDefaults> getBoobytrapDefaultsRecord() {
		if (!boobytrapDefaultsCaptured) {
			return Optional.empty();
		}
		return Optional.of(new BoobytrapDefaults(true, defaultBoobytrapsEnabled, defaultWormSpawnChance, defaultWormTrapSpawnChance));
	}

	private Optional<SpreadSnapshot> getSpreadSnapshot() {
		List<Long> pillars = getPillarChunksSnapshot();
		List<BlockPos> sources = getVirusSourceList();
		if (pillars.isEmpty() && sources.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new SpreadSnapshot(pillars, sources));
	}

	public static VirusWorldState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE);
	}

	public VirusDifficulty getDifficulty() {
		return difficulty;
	}

	public boolean isDormant() {
		return dormant;
	}

	public void setDormant(boolean dormant) {
		if (this.dormant != dormant) {
			this.dormant = dormant;
			markDirty();
		}
	}

	private void captureBoobytrapDefaults(ServerWorld world) {
		if (boobytrapDefaultsCaptured) {
			return;
		}
		GameRules rules = world.getGameRules();
		defaultBoobytrapsEnabled = rules.getBoolean(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED);
		defaultWormSpawnChance = rules.getInt(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE);
		defaultWormTrapSpawnChance = rules.getInt(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE);
		boobytrapDefaultsCaptured = true;
		markDirty();
	}

	public void disableBoobytraps(ServerWorld world) {
		captureBoobytrapDefaults(world);
		GameRules rules = world.getGameRules();
		rules.get(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED).set(false, world.getServer());
		int spawnRate = Math.max(1, getDefaultWormSpawnChance() / 3);
		int trapRate = Math.max(1, getDefaultWormTrapSpawnChance() / 3);
		rules.get(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE).set(spawnRate, world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE).set(trapRate, world.getServer());
		setDormant(true);
	}

	private void restoreBoobytrapRules(ServerWorld world) {
		captureBoobytrapDefaults(world);
		GameRules rules = world.getGameRules();
		rules.get(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED).set(defaultBoobytrapsEnabled, world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE).set(getDefaultWormSpawnChance(), world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE).set(getDefaultWormTrapSpawnChance(), world.getServer());
		setDormant(false);
	}

	private int getDefaultWormSpawnChance() {
		return defaultWormSpawnChance > 0 ? defaultWormSpawnChance : 6;
	}

	private int getDefaultWormTrapSpawnChance() {
		return defaultWormTrapSpawnChance > 0 ? defaultWormTrapSpawnChance : 135;
	}

	public boolean hasShownDifficultyPrompt() {
		return difficultyPromptShown;
	}

	public void markDifficultyPromptShown() {
		if (!difficultyPromptShown) {
			difficultyPromptShown = true;
			markDirty();
		}
	}

	public void setDifficulty(ServerWorld world, VirusDifficulty newDifficulty) {
		if (this.difficulty == newDifficulty) {
			return;
		}
		this.difficulty = newDifficulty;
		markDirty();
		applyDifficultyRules(world, getCurrentTier());
		syncDifficulty(world);
	}

	private void syncDifficulty(ServerWorld world) {
		DifficultySyncPayload payload = new DifficultySyncPayload(difficulty);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public void tick(ServerWorld world) {
		tickVoidTears(world);
		tickShieldFields(world);
		if (!infected) {
			return;
		}
		totalTicks++;
		ticksInTier++;

		removeMissingSources(world);
		tickGuardianBeams(world);

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

		applyDifficultyRules(world, tier);
		runEvents(world, tier);
		boostAmbientSpawns(world, tier);
		applyInfectiousContactDamage(world);
		applyInfectiousInventoryEffects(world);
		pulseHelmetTrackers(world);
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
		}
	}

	private void boostAmbientSpawns(ServerWorld world, InfectionTier tier) {
		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
			return;
		}
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
		float tierMultiplier = tier.getMobSpawnMultiplier();
		float scaledAttempts = difficultyScale * tierMultiplier;
		if (apocalypseMode) {
			scaledAttempts += 2.0F;
		}
		int attempts = Math.max(1, Math.round(scaledAttempts));
		Random random = world.getRandom();
		List<ServerPlayerEntity> anchors = world.getPlayers(player -> player.isAlive() && !player.isSpectator() && isWithinAura(player.getBlockPos()));
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
			MobEntity spawned = type.spawn(world, entity -> entity.refreshPositionAndAngles(spawnPos, random.nextFloat() * 360.0F, 0.0F),
					spawnPos, SpawnReason.EVENT, true, false);
			VirusMobAllyHelper.mark(spawned);
		}
	}

	private void applyInfectiousContactDamage(ServerWorld world) {
		if (!infected) {
			infectiousContactTicks.clear();
			return;
		}
		Set<UUID> active = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.isSpectator() || player.isCreative()) {
				infectiousContactTicks.removeInt(player.getUuid());
				continue;
			}
			BlockPos feet = player.getBlockPos().down();
			if (!world.getBlockState(feet).isOf(ModBlocks.INFECTIOUS_CUBE)) {
				infectiousContactTicks.removeInt(player.getUuid());
				continue;
			}
			UUID uuid = player.getUuid();
			active.add(uuid);
			int ticks = infectiousContactTicks.getOrDefault(uuid, 0) + 1;
			infectiousContactTicks.put(uuid, ticks);
			boolean wearingBoots = VirusEquipmentHelper.hasRubberBoots(player);
			int threshold = INFECTIOUS_CONTACT_THRESHOLD + (wearingBoots ? RUBBER_CONTACT_THRESHOLD_BONUS : 0);
			int interval = INFECTIOUS_CONTACT_INTERVAL + (wearingBoots ? RUBBER_CONTACT_INTERVAL_BONUS : 0);
			if (ticks > threshold && (ticks - threshold) % interval == 0) {
				float damage = wearingBoots ? RUBBER_CONTACT_DAMAGE : 1.0F;
				player.damage(world, world.getDamageSources().magic(), damage);
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_SILVERFISH_HURT, SoundCategory.BLOCKS, 0.6F, 1.2F);
				if (wearingBoots) {
					VirusEquipmentHelper.damageRubberBoots(player, 1);
				}
			}
		}
		if (infectiousContactTicks.isEmpty()) {
			return;
		}
		infectiousContactTicks.object2IntEntrySet().removeIf(entry -> !active.contains(entry.getKey()));
	}

	private void applyInfectiousInventoryEffects(ServerWorld world) {
		if (!infected) {
			infectiousInventoryTicks.clear();
			return;
		}
		ItemStack infectiousStack = ModBlocks.INFECTIOUS_CUBE.asItem().getDefaultStack();
		Set<UUID> retained = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			UUID uuid = player.getUuid();
			if (player.isSpectator() || player.isCreative()
					|| player.getInventory().count(infectiousStack.getItem()) <= 0) {
				infectiousInventoryTicks.removeInt(uuid);
				continue;
			}
			retained.add(uuid);
			int ticks = infectiousInventoryTicks.getOrDefault(uuid, 0) + 1;
			infectiousInventoryTicks.put(uuid, ticks);
			if (ticks > INFECTIOUS_INVENTORY_THRESHOLD
					&& (ticks - INFECTIOUS_INVENTORY_THRESHOLD) % INFECTIOUS_INVENTORY_INTERVAL == 0) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 50, 0));
			}
		}
		if (retained.isEmpty()) {
			infectiousInventoryTicks.clear();
		} else {
			infectiousInventoryTicks.object2IntEntrySet().removeIf(entry -> !retained.contains(entry.getKey()));
		}
	}

	private void pulseHelmetTrackers(ServerWorld world) {
		if (!infected || virusSources.isEmpty()) {
			helmetPingTimers.clear();
			heavyPantsVoidWear.clear();
			return;
		}
		Set<UUID> tracked = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.isSpectator() || player.isCreative() || !VirusEquipmentHelper.hasAugmentedHelmet(player)) {
				helmetPingTimers.removeInt(player.getUuid());
				continue;
			}
			UUID uuid = player.getUuid();
			tracked.add(uuid);
			int ticks = helmetPingTimers.getOrDefault(uuid, 0) + 1;
			if (ticks >= AUGMENTED_HELMET_PING_INTERVAL) {
				helmetPingTimers.put(uuid, 0);
				pingHelmet(world, player);
			} else {
				helmetPingTimers.put(uuid, ticks);
			}
		}
		if (tracked.isEmpty()) {
			helmetPingTimers.clear();
		} else {
			helmetPingTimers.object2IntEntrySet().removeIf(entry -> !tracked.contains(entry.getKey()));
		}
	}

	private void pingHelmet(ServerWorld world, ServerPlayerEntity player) {
		BlockPos target = findNearestVirusSource(player.getBlockPos());
		if (target == null) {
			player.sendMessage(Text.translatable("message.the-virus-block.helmet_ping_none"), true);
			return;
		}
		Vec3d eye = player.getEyePos();
		Vec3d delta = Vec3d.ofCenter(target).subtract(eye);
		double distance = delta.length();
		if (distance < 0.5D) {
			player.sendMessage(Text.translatable("message.the-virus-block.helmet_ping_here"), true);
			return;
		}
		double visualDistance = Math.min(distance, HELMET_PING_MAX_PARTICLE_DISTANCE);
		spawnHelmetTrail(world, eye, delta, visualDistance);
		player.sendMessage(
				Text.translatable("message.the-virus-block.helmet_ping", Text.literal(describeDirection(delta)),
						MathHelper.floor(distance)),
				true);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_HARP,
				SoundCategory.PLAYERS, 0.35F, 1.8F);
	}

	private void accumulateHeavyPantsWear(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		double accumulated = heavyPantsVoidWear.getOrDefault(uuid, 0.0D) + HEAVY_PANTS_VOID_TEAR_WEAR;
		int damage = (int) accumulated;
		if (damage > 0) {
			VirusEquipmentHelper.damageHeavyPants(player, damage);
			accumulated -= damage;
		}
		heavyPantsVoidWear.put(uuid, accumulated);
	}

	@Nullable
	private BlockPos findNearestVirusSource(BlockPos origin) {
		if (virusSources.isEmpty()) {
			return null;
		}
		Vec3d originVec = Vec3d.ofCenter(origin);
		BlockPos closest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos source : virusSources) {
			double distanceSq = source.toCenterPos().squaredDistanceTo(originVec);
			if (distanceSq < best) {
				best = distanceSq;
				closest = source;
			}
		}
		return closest;
	}

	private void spawnHelmetTrail(ServerWorld world, Vec3d eye, Vec3d delta, double maxDistance) {
		if (maxDistance <= 0.0D) {
			return;
		}
		Vec3d direction = delta.normalize();
		int steps = Math.max(4, MathHelper.floor(maxDistance / 2.0D));
		double step = maxDistance / steps;
		for (int i = 1; i <= steps; i++) {
			Vec3d point = eye.add(direction.multiply(step * i));
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static String describeDirection(Vec3d delta) {
		double angle = Math.toDegrees(Math.atan2(delta.x, delta.z));
		if (angle < 0.0D) {
			angle += 360.0D;
		}
		int index = MathHelper.floor((angle + 22.5D) / 45.0D) & 7;
		return COMPASS_POINTS[index];
	}

	private BlockPos findBoostSpawnPos(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int tries = 0; tries < 6; tries++) {
			int dx = random.nextBetween(-radius, radius);
			int dz = random.nextBetween(-radius, radius);
			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}
			if (!world.getWorldBorder().contains(pos)) {
				continue;
			}
			BlockState state = world.getBlockState(pos);
			BlockState below = world.getBlockState(pos.down());
			if (!state.isAir() || below.isAir()) {
				continue;
			}
			return pos;
		}
		return null;
	}

	@Nullable
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

	private void applyDifficultyRules(ServerWorld world, InfectionTier tier) {
		if (difficulty == VirusDifficulty.EXTREME) {
			applyExtremeHealthPenalty(world, tier);
		} else {
			clearExtremeHealthPenalty(world);
		}
	}

	private void applyExtremeHealthPenalty(ServerWorld world, InfectionTier tier) {
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

	private void tickGuardianBeams(ServerWorld world) {
		if (guardianBeams.isEmpty()) {
			return;
		}
		long now = world.getTime();
		ObjectIterator<Object2LongMap.Entry<UUID>> iterator = guardianBeams.object2LongEntrySet().iterator();
		while (iterator.hasNext()) {
			Object2LongMap.Entry<UUID> entry = iterator.next();
			if (now >= entry.getLongValue()) {
				Entity entity = world.getEntity(entry.getKey());
				if (entity != null) {
					entity.discard();
				}
				iterator.remove();
			}
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
		players.removeIf(this::isPlayerShielded);
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
				arrow.addCommandTag(TheVirusBlock.CORRUPTION_PROJECTILE_TAG);
				arrow.setVelocity(0.0D, -1.6D - random.nextDouble(), 0.0D);
				world.spawnEntity(arrow);

				if (tier.getIndex() >= 1 && random.nextFloat() < 0.35F + tier.getIndex() * 0.15F) {
					CorruptedTntEntity tnt = CorruptedTntEntity.spawn(world, x, y, z, null,
							Math.max(15, 50 - tier.getIndex() * 8));
					tnt.addCommandTag(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG);
					tntSpawned++;
				}
			}
			totalArrows += volleys;

			if (tier.getIndex() >= 2 && random.nextFloat() < 0.4F + tier.getIndex() * 0.1F) {
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
			arrow.addCommandTag(TheVirusBlock.CORRUPTION_PROJECTILE_TAG);
			arrow.setVelocity(0.0D, -1.9D - random.nextDouble() * 0.6D, 0.0D);
			world.spawnEntity(arrow);
		}

		if (tier.getIndex() >= 3) {
			CorruptedTntEntity tnt = CorruptedTntEntity.spawn(world, player.getX(), player.getY() + 12.0D, player.getZ(), null,
					Math.max(20, 40 - tier.getIndex() * 6));
			tnt.addCommandTag(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG);
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

			MobEntity zombie = EntityType.ZOMBIE.spawn(world, entity -> {
				entity.refreshPositionAndAngles(pos, random.nextFloat() * 360.0F, 0.0F);
				entity.setCustomName(Text.translatable("entity.the-virus-block.corrupted_passive").formatted(Formatting.DARK_RED));
			}, pos, SpawnReason.EVENT, true, false);
			VirusMobAllyHelper.mark(zombie);
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
		if (!isVoidTearAllowed(tier)) {
			return;
		}
		if (!canTrigger(VirusEventType.VOID_TEAR, 200)) {
			return;
		}

		if (random.nextFloat() > 0.2F) {
			return;
		}

		markEvent(VirusEventType.VOID_TEAR);

		boolean targetPlayer = random.nextFloat() < getPlayerVoidTearChance();
		BlockPos targetPos;
		if (targetPlayer) {
			targetPos = pickPlayerVoidTearPos(world, random);
			if (targetPos == null) {
				return;
			}
		} else {
			if (origin == null) {
				return;
			}
			targetPos = sampleTearOffset(world, origin, random, 12, 6);
			if (targetPos == null) {
				return;
			}
		}
		createVoidTear(world, targetPos, tier, targetPlayer ? "player" : "core");
	}

	public boolean spawnVoidTearForCommand(ServerWorld world, BlockPos center) {
		if (center == null) {
			return false;
		}
		createVoidTear(world, center, getCurrentTier(), "command");
		return true;
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
			if (spawned instanceof MobEntity mob) {
				VirusMobAllyHelper.mark(mob);
			}
			CorruptionProfiler.logTierEvent(world, VirusEventType.ENTITY_DUPLICATION, spawnPos,
					"type=" + spawned.getType().toString());
		}
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

	public double getActiveAuraRadius() {
		double radius = Math.max(3.0D, getCurrentTier().getBaseAuraRadius() - getContainmentLevel() * 0.6D);
		if (apocalypseMode) {
			radius += 4.0D;
		}
		return radius;
	}

	public boolean isWithinAura(BlockPos pos) {
		if (!infected || virusSources.isEmpty()) {
			return false;
		}
		double radius = getActiveAuraRadius();
		double radiusSq = radius * radius;
		Vec3d target = Vec3d.ofCenter(pos);
		for (BlockPos source : virusSources) {
			if (source.toCenterPos().squaredDistanceTo(target) <= radiusSq) {
				return true;
			}
		}
		return false;
	}

	private Map<VirusEventType, Long> getEventHistorySnapshot() {
		return Map.copyOf(eventHistory);
	}

	private List<ShieldField> getShieldFieldsSnapshot() {
		return activeShields.isEmpty() ? List.of() : List.copyOf(activeShields.values());
	}

	public boolean areLiquidsCorrupted(ServerWorld world) {
		return infected && TierCookbook.isEnabled(world, getCurrentTier(), apocalypseMode, TierFeature.LIQUID_CORRUPTION);
	}

	private boolean canTrigger(VirusEventType type, long cooldown) {
		double modifier = Math.max(0.1D, difficulty.getEventOddsMultiplier());
		long adjustedCooldown = Math.max(20L, MathHelper.floor(cooldown / modifier));
		long last = eventHistory.getOrDefault(type, -adjustedCooldown);
		return totalTicks - last >= adjustedCooldown;
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
	private static final double SHIELD_FIELD_RADIUS = 12.0D;
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
			if (shouldPushDuringShell()) {
				pushPlayersFromBlock(world, pos, radius);
			}
			world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
		});
	}

	private boolean shouldPushDuringShell() {
		InfectionTier tier = getCurrentTier();
		return tier.getIndex() < 3 || ticksInTier < tier.getDurationTicks() / 2;
	}
	
	private void pushPlayersFromBlock(ServerWorld world, BlockPos formingPos, int radius) {
		Vec3d origin = Vec3d.ofCenter(formingPos);
		double pushRadius = Math.max(4.0D, radius + 4.0D);
		double pushRadiusSq = pushRadius * pushRadius;
		double difficultyKnockback = difficulty.getKnockbackMultiplier();
		if (difficultyKnockback <= 0.0D) {
			return;
		}
		double baseStrength = (1.2D + radius * 0.15D) * difficultyKnockback;
		double verticalBoost = (0.5D + radius * 0.05D) * difficultyKnockback;

		int affected = 0;
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			double distSq = player.squaredDistanceTo(origin);
			if (distSq > pushRadiusSq) {
				continue;
			}
			Vec3d offset = player.getPos().subtract(origin);
			Vec3d horizontal = new Vec3d(offset.x, 0.0D, offset.z);
			if (horizontal.lengthSquared() < 1.0E-4) {
				double angle = world.getRandom().nextDouble() * Math.PI * 2.0D;
				horizontal = new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle));
			}
			Vec3d pushVec = horizontal.normalize().multiply(baseStrength);
			player.addVelocity(pushVec.x, verticalBoost, pushVec.z);
			player.velocityModified = true;
			spawnGuardianBeam(world, formingPos, player);
			affected++;
		}

		if (affected > 0) {
			world.playSound(
					null,
					formingPos,
					SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
					SoundCategory.HOSTILE,
					1.25F,
					0.55F);
		}
	}

	private static final int GUARDIAN_BEAM_DURATION = 60;

	private void spawnGuardianBeam(ServerWorld world, BlockPos origin, ServerPlayerEntity target) {
		GuardianEntity guardian = EntityType.GUARDIAN.create(world, SpawnReason.TRIGGERED);
		if (guardian == null) {
			return;
		}
		guardian.refreshPositionAndAngles(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, world.getRandom().nextFloat() * 360.0F, 0.0F);
		guardian.setAiDisabled(true);
		guardian.setInvisible(true);
		guardian.clearStatusEffects();
		guardian.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, GUARDIAN_BEAM_DURATION + 20, 0, false, false));
		guardian.setGlowing(false);
		guardian.setCustomNameVisible(false);
		guardian.setSilent(true);
		guardian.setNoGravity(true);
		guardian.setInvulnerable(true);
		guardian.setTarget(target);
		if (!world.spawnEntity(guardian)) {
			return;
		}
		((GuardianEntityAccessor) guardian).theVirusBlock$setBeamTarget(target.getId());
		world.sendEntityStatus(guardian, EntityStatuses.PLAY_GUARDIAN_ATTACK_SOUND);
		guardianBeams.put(guardian.getUuid(), world.getTime() + GUARDIAN_BEAM_DURATION);
	}

	private void spawnCorruptedPillars(ServerWorld world, int tierIndex) {
		Random random = world.getRandom();
		int chunkRadius = MathHelper.clamp(1 + tierIndex, 1, 8);
		for (BlockPos core : virusSources) {
			ChunkPos origin = new ChunkPos(core);
			for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
				for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
					ChunkPos chunk = new ChunkPos(origin.x + dx, origin.z + dz);
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
		guardianBeams.clear();
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
		float teleportChance = getTeleportChance();
		for (BlockPos source : List.copyOf(virusSources)) {
			if (random.nextFloat() > teleportChance) {
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
			guardianBeams.clear();
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
			captureBoobytrapDefaults(world);
			restoreBoobytrapRules(world);
			dormant = false;
			tierIndex = 0;
			totalTicks = 0;
			ticksInTier = 0;
			containmentLevel = 0;
			apocalypseMode = false;
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
		List<BlockPos> snapshot = new ArrayList<>(virusSources);
		purgeHostilesAround(world, snapshot);
		for (BlockPos pos : snapshot) {
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
		apocalypseMode = false;
		terrainCorrupted = false;
		shellsCollapsed = false;
		dormant = false;
		endInfection(world);
	}

	private void endInfection(ServerWorld world) {
		if (!infected) {
			return;
		}

		restoreBoobytrapRules(world);
		infected = false;
		dormant = false;
		tierIndex = 0;
		totalTicks = 0;
		ticksInTier = 0;
		containmentLevel = 0;
		apocalypseMode = false;
		terrainCorrupted = false;
		shellsCollapsed = false;
		healthScale = 1.0D;
		currentHealth = 0.0D;
		beginCleansing();
		eventHistory.clear();
		shellCooldowns.clear();
		pillarChunks.clear();
		guardianBeams.clear();
		for (VoidTearInstance tear : activeVoidTears) {
			sendVoidTearBurst(world, tear);
		}
		activeVoidTears.clear();
		pendingVoidTears.clear();
		markDirty();

		MatrixCubeBlockEntity.destroyAll(world);
		Text message = Text.translatable("message.the-virus-block.cleansed").formatted(Formatting.AQUA);
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(message, false));
	}

	private void createVoidTear(ServerWorld world, BlockPos basePos, InfectionTier tier, String source) {
		BlockPos tearPos = basePos.toImmutable();
		double radius = (8.0D + tier.getIndex() * 2.0D) * 0.7D;
		double pullStrength = 0.25D + tier.getIndex() * 0.08D;
		double damageRadius = Math.max(2.0D, radius * 0.4D);
		float damage = 1.5F + tier.getIndex() * 0.75F;
		int durationTicks = 100;
		long activationTick = world.getTime() + VOID_TEAR_WARNING_DELAY;
		long tearId = ++nextVoidTearId;
		pendingVoidTears.add(new PendingVoidTear(
				tearId,
				tearPos,
				radius,
				pullStrength,
				damageRadius * damageRadius,
				damage,
				activationTick,
				durationTicks,
				tier.getIndex(),
				source));
		warnPlayersNearTear(world, tearPos, radius);
	}

	private void tickVoidTears(ServerWorld world) {
		long now = world.getTime();
		Random random = world.getRandom();
		armPendingVoidTears(world, random, now);
		if (activeVoidTears.isEmpty()) {
			return;
		}
		Iterator<VoidTearInstance> iterator = activeVoidTears.iterator();
		while (iterator.hasNext()) {
			VoidTearInstance tear = iterator.next();
			if (now >= tear.expiryTick()) {
				applyVoidTearForce(world, tear, true);
				sendVoidTearBurst(world, tear);
				spawnVoidTearBurst(world, tear);
				iterator.remove();
				continue;
			}
			applyVoidTearForce(world, tear, false);
			notifyPlayersDuringVoidTear(world, tear, now);
			erodeVoidTearBlocks(world, tear, random);
			spawnVoidTearParticles(world, tear);
		}
	}

	private void armPendingVoidTears(ServerWorld world, Random random, long now) {
		if (pendingVoidTears.isEmpty()) {
			return;
		}
		Iterator<PendingVoidTear> iterator = pendingVoidTears.iterator();
		while (iterator.hasNext()) {
			PendingVoidTear pending = iterator.next();
			if (now < pending.activationTick()) {
				continue;
			}
			iterator.remove();
			activatePendingVoidTear(world, pending, random);
		}
	}

	private void activatePendingVoidTear(ServerWorld world, PendingVoidTear pending, Random random) {
		BlockPos tearPos = pending.centerBlock();
		long expiry = pending.activationTick() + pending.durationTicks();
		VoidTearInstance tear = new VoidTearInstance(
				pending.id(),
				tearPos,
				Vec3d.ofCenter(tearPos),
				pending.radius(),
				pending.pullStrength(),
				pending.damageRadiusSq(),
				pending.damage(),
				expiry,
				pending.durationTicks(),
				pending.tierIndex());
		activeVoidTears.add(tear);
		carveVoidTearChamber(world, tearPos, pending.radius());
		sendVoidTearSpawn(world, tear);
		world.syncWorldEvent(WorldEvents.COMPOSTER_USED, tearPos, 0);
		int affected = applyVoidTearForce(world, tear, false);
		erodeVoidTearBlocks(world, tear, random);
		spawnVoidTearParticles(world, tear);
		CorruptionProfiler.logTierEvent(world, VirusEventType.VOID_TEAR,
				tearPos,
				"source=" + pending.source() + " affected=" + affected + " radius=" + pending.radius() + " duration=100");
	}

	private void tickShieldFields(ServerWorld world) {
		if (activeShields.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<Long, ShieldField>> iterator = activeShields.entrySet().iterator();
		while (iterator.hasNext()) {
			ShieldField field = iterator.next().getValue();
			BlockPos center = field.center();
			if (!world.isChunkLoaded(ChunkPos.toLong(center))) {
				continue;
			}
			if (isWithinAura(center)) {
				triggerShieldFailure(world, center);
				continue;
			}
			if (!isShieldAnchorIntact(world, center)) {
				iterator.remove();
				broadcastShieldRemoval(world, field.id());
				world.playSound(null, center, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.8F);
				notifyShieldStatus(world, center, false);
				continue;
			}
			world.spawnParticles(
					ParticleTypes.END_ROD,
					center.getX() + 0.5D,
					center.getY() + 1.2D,
					center.getZ() + 0.5D,
					4,
					0.25D,
					0.25D,
					0.25D,
					0.0D);
		}
	}

	private static final double TEAR_BURST_VERTICAL_BOOST = 0.35D;

	private int applyVoidTearForce(ServerWorld world, VoidTearInstance tear, boolean finalBlast) {
		long now = world.getTime();
		double effectRadius = Math.max(6.0D, tear.radius() * 1.4D);
		Box box = new Box(tear.centerBlock()).expand(effectRadius);
		int affected = 0;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			if (isShielding(living.getBlockPos())) {
				continue;
			}
			Vec3d center = tear.centerVec();
			Vec3d offset;
			long ticksRemaining = tear.expiryTick() - now;
			double duration = Math.max(1.0D, tear.durationTicks());
			double lifeFrac = MathHelper.clamp(1.0D - (ticksRemaining / duration), 0.0D, 1.0D);
			boolean pushPhase = finalBlast || lifeFrac >= 0.75D;
			boolean pullPhase = !pushPhase && lifeFrac < 0.5D;
			if (!pullPhase && !pushPhase) {
				continue;
			}
			offset = pushPhase ? living.getPos().subtract(center) : center.subtract(living.getPos());
			double distanceSq = offset.lengthSquared();
			if (distanceSq < 1.0E-4) {
				continue;
			}
			double distance = Math.sqrt(distanceSq);
			if (distance > effectRadius) {
				continue;
			}
			if (living instanceof ServerPlayerEntity player && VirusEquipmentHelper.hasHeavyPants(player)) {
				accumulateHeavyPantsWear(player);
				continue;
			}
			double proximity = 1.0D - Math.min(1.0D, distance / effectRadius);
			Vec3d direction = offset.normalize();
			double baseStrength = tear.pullStrength() * (pushPhase ? 4.2D : 3.2D);
			double strength = baseStrength * (0.15D + 0.85D * proximity);
			Vec3d impulse = direction.multiply(strength);
			double vertical = MathHelper.clamp(impulse.y + (pushPhase ? TEAR_BURST_VERTICAL_BOOST * 0.5D : 0.0D),
					-0.25D,
					0.25D);
			living.addVelocity(impulse.x, vertical, impulse.z);
			living.velocityModified = true;
			living.velocityDirty = true;
			living.fallDistance = 0.0F;
			affected++;
			if (pullPhase && distanceSq <= tear.damageRadiusSq()) {
				living.damage(world, world.getDamageSources().explosion(null, null), tear.damage());
			}
		}
		return affected;
	}

	private void erodeVoidTearBlocks(ServerWorld world, VoidTearInstance tear, Random random) {
		int attempts = Math.max(2, 2 + tear.tierIndex());
		double radiusSq = tear.radius() * tear.radius();
		for (int i = 0; i < attempts; i++) {
			BlockPos target = sampleVoidTearPos(random, tear, radiusSq);
			if (!world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}
			if (isShielding(target)) {
				continue;
			}
			BlockState state = world.getBlockState(target);
			if (state.isAir() || state.getHardness(world, target) < 0.0F || state.isOf(ModBlocks.VIRUS_BLOCK)) {
				continue;
			}
			if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				world.setBlockState(target, ModBlocks.CORRUPTED_STONE.getDefaultState(), Block.FORCE_STATE);
			} else {
				world.setBlockState(target, ModBlocks.CORRUPTED_GLASS.getDefaultState(), Block.FORCE_STATE);
			}
		}
	}

	private BlockPos sampleVoidTearPos(Random random, VoidTearInstance tear, double radiusSq) {
		for (int tries = 0; tries < 6; tries++) {
			double dx = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			double dy = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			double dz = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			if (dx * dx + dy * dy + dz * dz > radiusSq) {
				continue;
			}
			return BlockPos.ofFloored(tear.centerVec().x + dx, tear.centerVec().y + dy, tear.centerVec().z + dz);
		}
		return tear.centerBlock();
	}

	private void spawnVoidTearParticles(ServerWorld world, VoidTearInstance tear) {
		world.spawnParticles(
				ParticleTypes.REVERSE_PORTAL,
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				24,
				tear.radius() * 0.2D,
				tear.radius() * 0.2D,
				tear.radius() * 0.2D,
				0.02D);
	}

	private void spawnVoidTearBurst(ServerWorld world, VoidTearInstance tear) {
		world.spawnParticles(
				ParticleTypes.SONIC_BOOM,
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				2,
				tear.radius() * 0.1D,
				tear.radius() * 0.1D,
				tear.radius() * 0.1D,
				0.0D);
		world.playSound(
				null,
				tear.centerBlock(),
				SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
				SoundCategory.HOSTILE,
				0.8F,
				0.5F);
	}

	private void sendVoidTearSpawn(ServerWorld world, VoidTearInstance tear) {
		int duration = tear.durationTicks();
		VoidTearSpawnPayload payload = new VoidTearSpawnPayload(tear.id(), tear.centerVec().x, tear.centerVec().y,
				tear.centerVec().z, (float) tear.radius(), duration, tear.tierIndex());
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private void sendVoidTearBurst(ServerWorld world, VoidTearInstance tear) {
		VoidTearBurstPayload payload = new VoidTearBurstPayload(tear.id(), tear.centerVec().x, tear.centerVec().y,
				tear.centerVec().z, (float) tear.radius());
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private record VoidTearInstance(long id, BlockPos centerBlock, Vec3d centerVec, double radius, double pullStrength,
	                                double damageRadiusSq, float damage, long expiryTick, int durationTicks, int tierIndex) {
	}

	private record PendingVoidTear(long id, BlockPos centerBlock, double radius, double pullStrength,
	                               double damageRadiusSq, float damage, long activationTick, int durationTicks,
	                               int tierIndex, String source) {
	}

private record ShieldField(long id, BlockPos center, double radius, long createdTick) {
}

	private record BoobytrapDefaults(boolean captured, boolean enabled, int spawn, int trap) {
	}

	private record SpreadSnapshot(List<Long> pillars, List<BlockPos> sources) {
	}

	private float getTeleportChance() {
		return switch (difficulty) {
			case EASY -> 0.20F;
			case MEDIUM -> 0.30F;
			case HARD -> 0.40F;
			case EXTREME -> 0.50F;
		};
	}

	private boolean isVoidTearAllowed(InfectionTier tier) {
		int tierIndex = tier.getIndex();
		int tierThreeIndex = InfectionTier.THREE.getIndex();
		if (tierIndex < tierThreeIndex) {
			return false;
		}
		if (tierIndex == tierThreeIndex) {
			return true;
		}
		return difficulty == VirusDifficulty.HARD || difficulty == VirusDifficulty.EXTREME;
	}

	private float getPlayerVoidTearChance() {
		return switch (difficulty) {
			case EXTREME -> 0.4F;
			case HARD -> 0.25F;
			default -> 0.15F;
		};
	}

	@Nullable
	private BlockPos pickPlayerVoidTearPos(ServerWorld world, Random random) {
		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			return null;
		}
		ServerPlayerEntity player = players.get(random.nextInt(players.size()));
		return sampleTearOffset(world, player.getBlockPos(), random, 16, 8);
	}

	private BlockPos sampleTearOffset(ServerWorld world, BlockPos base, Random random, int horizontalRange, int verticalRange) {
		if (base == null) {
			return null;
		}
		BlockPos candidate = base;
		for (int attempts = 0; attempts < 12; attempts++) {
			int offsetX = random.nextBetween(-horizontalRange, horizontalRange);
			int offsetY = random.nextBetween(-verticalRange, verticalRange);
			int offsetZ = random.nextBetween(-horizontalRange, horizontalRange);
			BlockPos test = base.add(offsetX, offsetY, offsetZ);
			int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, test.getX(), test.getZ());
			int clampedY = MathHelper.clamp(test.getY(), world.getBottomY() + 4, topY - 4);
			test = new BlockPos(test.getX(), clampedY, test.getZ());
			if (!world.isChunkLoaded(ChunkPos.toLong(test))) {
				continue;
			}
			return test;
		}
		return candidate;
	}

	private void warnPlayersNearTear(ServerWorld world, BlockPos center, double radius) {
		if (center == null) {
			return;
		}
		double warnRange = Math.max(20.0D, radius * 2.5D);
		double warnRangeSq = warnRange * warnRange;
		Vec3d centerVec = Vec3d.ofCenter(center);
		Text warning = Text.translatable("message.the-virus-block.void_tear.warning").formatted(Formatting.LIGHT_PURPLE);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.squaredDistanceTo(centerVec) <= warnRangeSq) {
				player.sendMessage(warning, true);
			}
		}
	}

	private static final int VOID_TEAR_WARNING_DELAY = 20;
	private static final int VOID_TEAR_PULL_NOTICE_INTERVAL = 5;

	private void notifyPlayersDuringVoidTear(ServerWorld world, VoidTearInstance tear, long now) {
		if ((now + tear.id()) % VOID_TEAR_PULL_NOTICE_INTERVAL != 0) {
			return;
		}
		double notifyRadius = Math.max(8.0D, tear.radius() * 1.6D);
		double notifyRadiusSq = notifyRadius * notifyRadius;
		Vec3d center = tear.centerVec();
		long ticksRemaining = tear.expiryTick() - now;
		double duration = Math.max(1.0D, tear.durationTicks());
		double lifeFrac = MathHelper.clamp(1.0D - (ticksRemaining / duration), 0.0D, 1.0D);
		boolean pushPhase = lifeFrac >= 0.65D;
		boolean pullPhase = !pushPhase && lifeFrac < 0.5D;
		if (!pushPhase && !pullPhase) {
			return;
		}
		Text message = pushPhase
				? Text.translatable("message.the-virus-block.void_tear.push").formatted(Formatting.LIGHT_PURPLE)
				: Text.translatable("message.the-virus-block.void_tear.pull").formatted(Formatting.LIGHT_PURPLE);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.squaredDistanceTo(center) <= notifyRadiusSq) {
				player.sendMessage(message, true);
			}
		}
	}

	private void carveVoidTearChamber(ServerWorld world, BlockPos center, double radius) {
		int carveRadius = Math.max(2, MathHelper.ceil(radius * 0.325D));
		int depth = Math.max(2, carveRadius + 1);
		int radiusSq = carveRadius * carveRadius;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int dx = -carveRadius; dx <= carveRadius; dx++) {
			for (int dz = -carveRadius; dz <= carveRadius; dz++) {
				if (dx * dx + dz * dz > radiusSq) {
					continue;
				}
				for (int dy = 0; dy <= depth; dy++) {
					mutable.set(center.getX() + dx, center.getY() - dy, center.getZ() + dz);
					if (!world.isChunkLoaded(ChunkPos.toLong(mutable))) {
						continue;
					}
					if (isShielding(mutable)) {
						continue;
					}
					BlockState state = world.getBlockState(mutable);
					if (isVoidTearProtected(state)) {
						continue;
					}
					if (!state.isAir()) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
				for (int dy = 1; dy <= 2; dy++) {
					mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!world.isChunkLoaded(ChunkPos.toLong(mutable))) {
						continue;
					}
					if (isShielding(mutable)) {
						continue;
					}
					BlockState state = world.getBlockState(mutable);
					if (isVoidTearProtected(state)) {
						continue;
					}
					if (!state.isAir()) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
			}
		}
	}

	private boolean isVoidTearProtected(BlockState state) {
		return state.isOf(Blocks.OBSIDIAN)
				|| state.isOf(Blocks.CRYING_OBSIDIAN)
				|| state.isOf(Blocks.NETHER_PORTAL)
				|| state.isOf(Blocks.END_PORTAL)
				|| state.isOf(Blocks.END_PORTAL_FRAME);
	}

	public void evaluateShieldCandidate(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		if (!state.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)) {
			deactivateShield(world, pos);
			return;
		}
		if (!isShieldCocoonComplete(world, pos)) {
			deactivateShield(world, pos);
			return;
		}
		if (isWithinVirusInfluence(pos)) {
			triggerShieldFailure(world, pos);
			return;
		}
		activateShield(world, pos);
	}

	public void removeShieldAt(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return;
		}
		deactivateShield(world, pos);
	}

	private void activateShield(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		if (activeShields.containsKey(key)) {
			return;
		}
		ShieldField field = new ShieldField(key, pos.toImmutable(), SHIELD_FIELD_RADIUS, world.getTime());
		activeShields.put(key, field);
		markDirty();
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.05F);
		broadcastShieldSpawn(world, field);
		openShieldSkyshaft(world, pos);
		notifyShieldStatus(world, pos, true);
	}

	private void purgeHostilesAround(ServerWorld world, List<BlockPos> centers) {
		if (centers.isEmpty()) {
			return;
		}
		Set<HostileEntity> victims = new HashSet<>();
		for (BlockPos center : centers) {
			Box bounds = new Box(center).expand(CLEANSE_PURGE_RADIUS);
			victims.addAll(world.getEntitiesByClass(HostileEntity.class, bounds, Entity::isAlive));
		}
		if (victims.isEmpty()) {
			return;
		}
		for (HostileEntity mob : victims) {
			mob.kill(world);
		}
		world.playSound(
				null,
				centers.get(0),
				SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
				SoundCategory.HOSTILE,
				1.2F,
				0.4F);
	}

	private void deactivateShield(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		ShieldField removed = activeShields.remove(key);
		if (removed == null) {
			return;
		}
		markDirty();
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.85F);
		broadcastShieldRemoval(world, removed.id());
		notifyShieldStatus(world, pos, false);
	}

	private void triggerShieldFailure(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		ShieldField removed = activeShields.remove(key);
		if (removed != null) {
			broadcastShieldRemoval(world, removed.id());
			markDirty();
		}
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.7F);
		world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.8F, 0.4F);
		world.createExplosion(
				null,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				20.0F,
				ExplosionSourceType.NONE);
		world.breakBlock(pos, false);
		notifyShieldFailure(world, pos);
	}

	private boolean isShieldCocoonComplete(ServerWorld world, BlockPos center) {
		for (Direction direction : Direction.values()) {
			if (!world.getBlockState(center.offset(direction)).isOf(Blocks.OBSIDIAN)) {
				return false;
			}
		}
		return true;
	}

	private boolean isShieldAnchorIntact(ServerWorld world, BlockPos center) {
		BlockState state = world.getBlockState(center);
		if (!state.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)) {
			return false;
		}
		return isShieldCocoonComplete(world, center);
	}

	private boolean isWithinVirusInfluence(BlockPos pos) {
		return isWithinAura(pos);
	}

	private boolean isShielding(BlockPos pos) {
		if (activeShields.isEmpty()) {
			return false;
		}
		Vec3d sample = Vec3d.ofCenter(pos);
		for (ShieldField field : activeShields.values()) {
			double radius = field.radius();
			double radiusSq = radius * radius;
			if (field.center().toCenterPos().squaredDistanceTo(sample) <= radiusSq) {
				return true;
			}
		}
		return false;
	}

	private boolean isPlayerShielded(ServerPlayerEntity player) {
		return isShielding(player.getBlockPos());
	}

	public boolean isShielded(BlockPos pos) {
		return isShielding(pos);
	}


	private void notifyShieldStatus(ServerWorld world, BlockPos pos, boolean active) {
		Text message = active
				? Text.translatable("message.the-virus-block.shield_field.online").formatted(Formatting.AQUA)
				: Text.translatable("message.the-virus-block.shield_field.offline").formatted(Formatting.GRAY);
		Vec3d center = Vec3d.ofCenter(pos);
		double radiusSq = (SHIELD_FIELD_RADIUS * SHIELD_FIELD_RADIUS) * 2.0D;
		world.getPlayers(player -> player.squaredDistanceTo(center) <= radiusSq)
				.forEach(player -> player.sendMessage(message, true));
	}

	private void notifyShieldFailure(ServerWorld world, BlockPos pos) {
		Text message = Text.translatable("message.the-virus-block.shield_field.rejected").formatted(Formatting.RED);
		Vec3d center = Vec3d.ofCenter(pos);
		world.getPlayers(player -> player.squaredDistanceTo(center) <= 1024.0D)
				.forEach(player -> player.sendMessage(message, true));
	}

	private void broadcastShieldSpawn(ServerWorld world, ShieldField field) {
		Vec3d center = Vec3d.ofCenter(field.center());
		ShieldFieldSpawnPayload payload = new ShieldFieldSpawnPayload(field.id(), center.x, center.y, center.z,
				(float) field.radius());
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private void broadcastShieldRemoval(ServerWorld world, long id) {
		ShieldFieldRemovePayload payload = new ShieldFieldRemovePayload(id);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public void sendShieldSnapshots(ServerPlayerEntity player) {
		if (activeShields.isEmpty()) {
			return;
		}
		for (ShieldField field : activeShields.values()) {
			Vec3d center = Vec3d.ofCenter(field.center());
			ShieldFieldSpawnPayload payload = new ShieldFieldSpawnPayload(
					field.id(),
					center.x,
					center.y,
					center.z,
					(float) field.radius());
			ServerPlayNetworking.send(player, payload);
		}
	}

	private void openShieldSkyshaft(ServerWorld world, BlockPos center) {
		BlockPos.Mutable cursor = new BlockPos.Mutable();
		int ceiling = world.getBottomY() + world.getDimension().height();
		int startY = center.getY() + 2;
		for (int y = startY; y < ceiling; y++) {
			cursor.set(center.getX(), y, center.getZ());
			if (!world.isChunkLoaded(ChunkPos.toLong(cursor))) {
				continue;
			}
			BlockState state = world.getBlockState(cursor);
			if (state.isAir()) {
				continue;
			}
			if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)) {
				continue;
			}
			if (state.getHardness(world, cursor) < 0.0F || state.isOf(Blocks.BEDROCK)) {
				continue;
			}
			world.setBlockState(cursor, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		world.spawnParticles(
				ParticleTypes.END_ROD,
				center.getX() + 0.5D,
				center.getY() + 1.2D,
				center.getZ() + 0.5D,
				16,
				0.2D,
				0.4D,
				0.2D,
				0.02D);
	}

	public boolean isInfected() {
		return infected;
	}

	public InfectionTier getCurrentTier() {
		return InfectionTier.byIndex(tierIndex);
	}

	public long getInfectionTicks() {
		return totalTicks;
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

	public float getBoobytrapIntensity() {
		if (apocalypseMode) {
			return 1.6F;
		}
		return getCurrentTier().getBoobytrapMultiplier();
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

	public void disturbByPlayer(ServerWorld world) {
		if (!infected) {
			return;
		}

		InfectionTier tier = InfectionTier.byIndex(tierIndex);
		long bonus = 40L + tier.getIndex() * 30L;
		if (tier.getIndex() >= 3) {
			bonus += 60L;
		}
		dormant = false;
		applyDisturbance(world, bonus);
		dormant = false;
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

	public void handleExplosionImpact(ServerWorld world, @Nullable Entity source, Vec3d center, double radius) {
		if (!infected || radius <= 0.0D || virusSources.isEmpty()) {
			return;
		}

		if (source != null && source.getCommandTags().contains(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG)) {
			return;
		}

		double radiusSq = radius * radius;
		boolean hit = false;
		for (BlockPos core : virusSources) {
			if (core.toCenterPos().squaredDistanceTo(center) <= radiusSq) {
				hit = true;
				break;
			}
		}
		if (!hit) {
			return;
		}

		long bonus = MathHelper.floor(60L + radius * 6.0D);
		bonus = MathHelper.clamp(bonus, 40L, 400L);

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
		return Math.max(1, MathHelper.ceil(tier.getDurationTicks() * difficulty.getDurationMultiplier()));
	}

	private static int encodeFlags(VirusWorldState state) {
		int flags = 0;
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

