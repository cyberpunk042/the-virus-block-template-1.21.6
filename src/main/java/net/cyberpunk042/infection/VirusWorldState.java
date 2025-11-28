package net.cyberpunk042.infection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.config.SingularityConfig;
import net.cyberpunk042.infection.singularity.ColumnWorkResult;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.cyberpunk042.infection.singularity.BulkFillHelper;
import net.cyberpunk042.infection.singularity.SingularityRingSlices;
import net.cyberpunk042.infection.singularity.SingularityCollapseScheduler;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.cyberpunk042.entity.BlackholePearlEntity;
import net.cyberpunk042.entity.CorruptedTntEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.infection.singularity.SingularityDestructionEngine;
import net.cyberpunk042.mixin.GuardianEntityAccessor;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.network.VoidTearBurstPayload;
import net.minecraft.block.LightBlock;
import net.cyberpunk042.network.VoidTearSpawnPayload;
import net.cyberpunk042.network.ShieldFieldSpawnPayload;
import net.cyberpunk042.network.ShieldFieldRemovePayload;
import net.cyberpunk042.network.SingularityBorderPayload;
import net.cyberpunk042.network.SingularitySchedulePayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.util.InfectionLog;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.border.WorldBorder;
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
			HealthSnapshot.CODEC.optionalFieldOf("health", HealthSnapshot.DEFAULT).forGetter(HealthSnapshot::of),
			Codec.STRING.optionalFieldOf("difficulty", VirusDifficulty.HARD.getId()).forGetter(state -> state.difficulty.getId()),
			Codec.BOOL.optionalFieldOf("difficultyPromptShown", false).forGetter(state -> state.difficultyPromptShown),
			BOOBYTRAP_CODEC.optionalFieldOf("boobytrapDefaults").forGetter(VirusWorldState::getBoobytrapDefaultsRecord),
			SPREAD_CODEC.optionalFieldOf("spreadData").forGetter(state -> state.getSpreadSnapshot()),
			Codec.unboundedMap(VirusEventType.CODEC, Codec.LONG).optionalFieldOf("eventHistory", Map.of()).forGetter(VirusWorldState::getEventHistorySnapshot),
			Codec.LONG.optionalFieldOf("lastMatrixCubeTick", 0L).forGetter(state -> state.lastMatrixCubeTick),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("singularityProfiles", Map.of()).forGetter(VirusWorldState::getProfileSnapshot),
			instance.group(
					SHIELD_FIELD_CODEC.listOf().optionalFieldOf("shieldFields", List.of()).forGetter(VirusWorldState::getShieldFieldsSnapshot),
					SingularitySnapshot.CODEC.optionalFieldOf("singularity").forGetter(VirusWorldState::getSingularitySnapshot),
					Codec.BOOL.optionalFieldOf("singularityPreGenComplete", false).forGetter(state -> state.singularityPreGenComplete),
					Codec.INT.optionalFieldOf("singularityPreGenMissing", 0).forGetter(state -> state.singularityPreGenMissingChunks)
			).apply(instance, SingularityPersistenceTail::new)
	).apply(instance, (infected, dormant, tierIndex, totalTicks, ticksInTier, containmentLevel, stateFlags, healthSnapshot, difficultyId, difficultyPromptShown, boobytrapDefaults, spreadData, events, lastMatrixCubeTick, profileMap, persistenceTail) -> {
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
		state.healthScale = healthSnapshot.scale();
		state.currentHealth = healthSnapshot.current();
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
		state.applyProfileSnapshot(profileMap);
		persistenceTail.shields().forEach(field -> state.activeShields.put(field.id(), field));
		state.shellRebuildPending = false;
		state.tierFiveBarrierActive = false;
		persistenceTail.snapshot().ifPresentOrElse(state::applySingularitySnapshot, state::clearSingularityState);
		state.singularityPreGenComplete = persistenceTail.preGenComplete();
		state.singularityPreGenMissingChunks = persistenceTail.preGenMissing();
		return state;
	}));
	public static final PersistentStateType<VirusWorldState> TYPE = new PersistentStateType<>(ID, VirusWorldState::new, CODEC, DataFixTypes.LEVEL);

	private final Set<BlockPos> virusSources = new HashSet<>();
	private final EnumMap<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Map<BlockPos, Long> shellCooldowns = new HashMap<>();
	private final Object2LongMap<UUID> guardianBeams = new Object2LongOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousInventoryTicks = new Object2IntOpenHashMap<>();
	private final Object2LongMap<UUID> infectiousInventoryWarnCooldowns = new Object2LongOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousContactTicks = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> helmetPingTimers = new Object2IntOpenHashMap<>();
	private final Object2DoubleMap<UUID> heavyPantsVoidWear = new Object2DoubleOpenHashMap<>();
	private boolean shellRebuildPending;
	private boolean tierFiveBarrierActive;
	private long nextTierFiveBarrierPushTick;
	private boolean finalVulnerabilityBlastTriggered;
	private SingularityState singularityState = SingularityState.DORMANT;
	private long singularityTicks;
	@Nullable
	private BlockPos singularityCenter;
	private boolean singularityShellCollapsed;
	private int singularityCollapseRadius;
	private boolean singularityCollapseDescending;
	private int singularityCollapseTotalChunks;
	private int singularityCollapseCompletedChunks;
	private int singularityCollapseBarDelay;
	private int singularityCollapseCompleteHold;
	private int singularityCollapseTickCooldown;
	private int singularityRingTicks;
	private long singularityFuseElapsed;
	private int singularityPhaseDelay;
	private int singularityFusePulseTicker;
private boolean singularityBorderActive;
	private boolean singularityDistanceOverrideActive;
	private int singularityViewDistanceSnapshot = -1;
	private int singularitySimulationDistanceSnapshot = -1;
	private boolean singularityBorderPendingDeployment;
	private boolean singularityBorderHasSnapshot;
	private double singularityBorderCenterX;
	private double singularityBorderCenterZ;
	private double singularityBorderInitialDiameter;
	private double singularityBorderTargetDiameter;
	private double singularityBorderLastDiameter;
	private int singularityBorderResetCountdown = -1;
	private int singularityCollapseStallTicks;
	private int singularityFuseWatchdogTicks;
	private SingularityState singularityLastLoggedState = SingularityState.DORMANT;
	private long singularityBorderDuration;
	private long singularityBorderElapsed;
	private double singularityBorderOriginalCenterX;
	private double singularityBorderOriginalCenterZ;
	private double singularityBorderOriginalDiameter;
	private double singularityBorderOriginalSafeZone;
	private double singularityBorderOriginalDamagePerBlock;
	private int singularityBorderOriginalWarningBlocks;
	private int singularityBorderOriginalWarningTime;
	private final DoubleArrayList singularityRingThresholds = new DoubleArrayList();
	private final IntArrayList singularityRingChunkCounts = new IntArrayList();
	private final IntArrayList singularityRingRadii = new IntArrayList();
	private List<List<ChunkPos>> singularityRingChunks = new ArrayList<>();
	private int singularityRingIndex;
	private int singularityRingPendingChunks;
	private double singularityInitialBorderDiameter;
	private double singularityFinalBorderDiameter;
	private long singularityTotalRingTicks;
	private long singularityRingTickAccumulator;
	private double singularityBorderOuterRadius;
	private double singularityBorderInnerRadius;
	private int singularityRingActualCount;
	private final Deque<Long> singularityChunkQueue = new ArrayDeque<>();
	private final Deque<Long> singularityResetQueue = new ArrayDeque<>();
	private final LongSet singularityResetProcessed = new LongOpenHashSet();
	private int singularityResetDelay;
	private SingularityDestructionEngine singularityDestructionEngine = SingularityDestructionEngine.create();
	private boolean singularityDestructionDirty = true;
	private int singularityLayersPerSlice = SINGULARITY_COLLAPSE_MIN_LAYERS_PER_SLICE;
	private int singularityLastColumnsPerTick = -1;
	private int singularityLastLayersPerSlice = -1;
private Deque<Long> singularityPreloadQueue = new ArrayDeque<>();
private boolean singularityPreloadComplete;
private int singularityPreloadMissingChunks;
private int singularityDebugLogCooldown = SINGULARITY_DEBUG_LOG_INTERVAL;
	private final LongSet singularityPinnedChunks = new LongOpenHashSet();
	private final LongSet collapseBufferedChunks = new LongOpenHashSet();
	private final Object2ObjectOpenHashMap<UUID, SingularityConfig.CollapseSyncProfile> singularityPlayerProfiles = new Object2ObjectOpenHashMap<>();
private final Deque<Long> singularityPreGenQueue = new ArrayDeque<>();
private boolean singularityPreGenComplete;
private int singularityPreGenMissingChunks;
private int singularityPreGenTotalChunks;
@Nullable
private ChunkPos singularityPreGenCenter;
private int singularityPreGenLogCooldown = SINGULARITY_PREGEN_LOG_INTERVAL;
private int singularityPreloadLogCooldown = SINGULARITY_PRELOAD_LOG_INTERVAL;
private int singularityRemovalLogCooldown = 20;
	private final Map<BlockPos, UUID> activeFuseEntities = new HashMap<>();
	@Nullable
	private SingularityCollapseScheduler collapseScheduler;
	@Nullable
	private UUID blackholePearlId;
	private static final boolean ENABLE_BLACKHOLE_PEARL = false;
	private final Set<BlockPos> singularityGlowNodes = new HashSet<>();
	private final Map<BlockPos, BlockState> fuseClearedBlocks = new HashMap<>();
	private final Set<BlockPos> suppressedUnregisters = new HashSet<>();
private final LongSet pillarChunks = new LongOpenHashSet();
	private final List<VoidTearInstance> activeVoidTears = new ArrayList<>();
	private final List<PendingVoidTear> pendingVoidTears = new ArrayList<>();
	private final Map<Long, ShieldField> activeShields = new HashMap<>();
	private long nextVoidTearId;
	private static final double CLEANSE_PURGE_RADIUS = 32.0D;
	private static final int INFECTIOUS_CONTACT_THRESHOLD = 40;
	private static final int INFECTIOUS_CONTACT_INTERVAL = 20;
	private static final int INFECTIOUS_INVENTORY_THRESHOLD = 20;
	private static final int INFECTIOUS_INVENTORY_INTERVAL = 60;
	private static final int INFECTIOUS_INVENTORY_WARNING_COOLDOWN = 200;
	private static final int RUBBER_CONTACT_THRESHOLD_BONUS = 60;
	private static final int RUBBER_CONTACT_INTERVAL_BONUS = 10;
	private static final float RUBBER_CONTACT_DAMAGE = 0.5F;
	private static final double HEAVY_PANTS_VOID_TEAR_WEAR = 4.0D / 3.0D;
	private static final int AUGMENTED_HELMET_PING_INTERVAL = 80;
	private static final double HELMET_PING_MAX_PARTICLE_DISTANCE = 32.0D;
	private static final int TIER_FIVE_BARRIER_RADIUS = 6;
	private static final int TIER_FIVE_BARRIER_INTERVAL = 15;
	private static final int FINAL_VULNERABILITY_BLAST_RADIUS = 3;
	private static final double FINAL_VULNERABILITY_BLAST_SCALE = 0.6D;
	private static final int SINGULARITY_COLLAPSE_PARTICLE_DENSITY = 6;
	private static final int SINGULARITY_RING_DURATION = 200;
	private static final int SINGULARITY_CORE_CHARGE_TICKS = 80;
	private static final int SINGULARITY_RING_START_DELAY = 40;
	private static final int SINGULARITY_RESET_DELAY_TICKS = 160;
private static final int SINGULARITY_COLLAPSE_MIN_COLUMNS_PER_TICK = 16;
private static final int SINGULARITY_COLLAPSE_MAX_COLUMNS_PER_TICK = 256;
private static final int SINGULARITY_COLLAPSE_MIN_LAYERS_PER_SLICE = 1;
private static final double SINGULARITY_BORDER_MIN_DIAMETER = 24.0D;
private static final double SINGULARITY_BORDER_FINAL_DIAMETER = 1.0D;
	private static final int SINGULARITY_COLLAPSE_CHUNKS_PER_STEP = 1;
private static final int SINGULARITY_RING_COLUMNS_PER_TICK = 64;
private static final int SINGULARITY_PREGEN_LOG_INTERVAL = 40;
private static final int SINGULARITY_PRELOAD_LOG_INTERVAL = 20;
private static final int SINGULARITY_COLLAPSE_BAR_DELAY_TICKS = 60;
private static final int SINGULARITY_COLLAPSE_COMPLETE_HOLD_TICKS = 40;
private static final int SINGULARITY_DEBUG_LOG_INTERVAL = 40;
	private static final BlockPos[] SINGULARITY_GLOW_OFFSETS = new BlockPos[]{
			new BlockPos(0, 1, 0),
			new BlockPos(0, -1, 0),
			new BlockPos(1, 0, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(0, 0, -1)
	};
	private static final float BLACKHOLE_PRIMARY_SCALE = 1.2F;
	private static final float BLACKHOLE_CORE_SCALE = 1.8F;
	private static final double SINGULARITY_RING_PULL_RADIUS = 24.0D;
	private static final double SINGULARITY_RING_PULL_STRENGTH = 0.35D;
	private static final DustColorTransitionParticleEffect SINGULARITY_FUSE_GLOW =
			new DustColorTransitionParticleEffect(0xFFFFFF, 0xFF3333, 1.1F);
	private static final String[] COMPASS_POINTS = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private static int configuredCollapseMaxRadius() {
		return Math.max(16, (int) Math.round(SingularityConfig.barrierStartRadius()));
	}

	private static double configuredBarrierStartRadius() {
		return SingularityConfig.barrierStartRadius();
	}

	private static double configuredBarrierEndRadius() {
		return MathHelper.clamp(SingularityConfig.barrierEndRadius(), 0.5D, configuredBarrierStartRadius());
	}

	private static long configuredBorderDurationTicks() {
		return Math.max(20L, SingularityConfig.barrierInterpolationTicks());
	}

	private static int configuredPregenRadiusChunks() {
		return Math.max(0, MathHelper.ceil(configuredPreGenRadiusBlocks() / 16.0D));
	}

	private static long configuredFuseExplosionDelayTicks() {
		return Math.max(20L, SingularityConfig.fuseExplosionDelayTicks());
	}

	private static int configuredFuseShellCollapseTicks() {
		return Math.max(0, SingularityConfig.fuseAnimationDelayTicks());
	}

	private static int configuredFusePulseInterval() {
		return Math.max(1, SingularityConfig.fusePulseInterval());
	}

	private static double configuredPreGenRadiusBlocks() {
		int configured = SingularityConfig.chunkPreGenRadiusBlocks();
		return configured > 0 ? configured : configuredBarrierStartRadius();
	}

	private static boolean configuredPreGenEnabled() {
		return SingularityConfig.chunkPreGenEnabled();
	}

	private static boolean configuredPreloadEnabled() {
		return SingularityConfig.chunkPreloadEnabled();
	}

	private static int configuredPreGenChunksPerTick() {
		return Math.max(1, SingularityConfig.chunkPreGenChunksPerTick());
	}

	private static int configuredPreloadChunksPerTick() {
		return Math.max(1, SingularityConfig.chunkPreloadChunksPerTick());
	}

	private static int configuredCollapseTickInterval() {
		return Math.max(1, SingularityConfig.collapseTickDelay());
	}

	private void applyCollapseDistanceOverrides(ServerWorld world) {
		int targetView = SingularityConfig.collapseViewDistance();
		int targetSim = SingularityConfig.collapseSimulationDistance();
		if (targetView <= 0 && targetSim <= 0) {
			return;
		}
		MinecraftServer server = world.getServer();
		PlayerManager manager = server.getPlayerManager();
		boolean changed = false;
		if (!singularityDistanceOverrideActive) {
			singularityViewDistanceSnapshot = manager.getViewDistance();
			singularitySimulationDistanceSnapshot = manager.getSimulationDistance();
		}
		if (targetView > 0 && manager.getViewDistance() != targetView) {
			manager.setViewDistance(targetView);
			changed = true;
		}
		if (targetSim > 0 && manager.getSimulationDistance() != targetSim) {
			manager.setSimulationDistance(targetSim);
			changed = true;
		}
		if (changed) {
			singularityDistanceOverrideActive = true;
			if (SingularityConfig.debugLogging()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[vanillaSync] viewDistance={} (orig={}) simulationDistance={} (orig={})",
						manager.getViewDistance(),
						singularityViewDistanceSnapshot,
						manager.getSimulationDistance(),
						singularitySimulationDistanceSnapshot);
			}
		} else if (!singularityDistanceOverrideActive) {
			singularityViewDistanceSnapshot = -1;
			singularitySimulationDistanceSnapshot = -1;
		}
	}

	private void restoreCollapseDistanceOverrides(MinecraftServer server) {
		if (!singularityDistanceOverrideActive) {
			return;
		}
		PlayerManager manager = server.getPlayerManager();
		if (singularityViewDistanceSnapshot >= 0 && manager.getViewDistance() != singularityViewDistanceSnapshot) {
			manager.setViewDistance(singularityViewDistanceSnapshot);
		}
		if (singularitySimulationDistanceSnapshot >= 0
				&& manager.getSimulationDistance() != singularitySimulationDistanceSnapshot) {
			manager.setSimulationDistance(singularitySimulationDistanceSnapshot);
		}
		if (SingularityConfig.debugLogging()) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[vanillaSync] restored viewDistance={} simulationDistance={}",
					manager.getViewDistance(),
					manager.getSimulationDistance());
		}
		singularityDistanceOverrideActive = false;
		singularityViewDistanceSnapshot = -1;
		singularitySimulationDistanceSnapshot = -1;
	}

	private Map<String, String> getProfileSnapshot() {
		if (singularityPlayerProfiles.isEmpty()) {
			return Map.of();
		}
		Map<String, String> snapshot = new HashMap<>();
		ObjectIterator<Object2ObjectMap.Entry<UUID, SingularityConfig.CollapseSyncProfile>> iterator = singularityPlayerProfiles.object2ObjectEntrySet().iterator();
		while (iterator.hasNext()) {
			Object2ObjectMap.Entry<UUID, SingularityConfig.CollapseSyncProfile> entry = iterator.next();
			snapshot.put(entry.getKey().toString(), entry.getValue().id());
		}
		return snapshot;
	}

	private void applyProfileSnapshot(Map<String, String> entries) {
		singularityPlayerProfiles.clear();
		if (entries == null || entries.isEmpty()) {
			return;
		}
		entries.forEach(this::applyProfileSnapshotEntry);
	}

	private void applyProfileSnapshotEntry(String uuidString, String profileId) {
		try {
			UUID uuid = UUID.fromString(uuidString);
			SingularityConfig.CollapseSyncProfile profile = SingularityConfig.CollapseSyncProfile.fromId(profileId);
			if (profile != null && profile != SingularityConfig.collapseDefaultSyncProfile()) {
				singularityPlayerProfiles.put(uuid, profile);
			}
		} catch (IllegalArgumentException ignored) {
		}
	}

	public SingularityConfig.CollapseSyncProfile getCollapseProfile(ServerPlayerEntity player) {
		return getCollapseProfile(player.getUuid());
	}

	private SingularityConfig.CollapseSyncProfile getCollapseProfile(UUID uuid) {
		SingularityConfig.CollapseSyncProfile profile = singularityPlayerProfiles.get(uuid);
		return profile != null ? profile : SingularityConfig.collapseDefaultSyncProfile();
	}

	public void setCollapseProfile(ServerPlayerEntity player, SingularityConfig.CollapseSyncProfile profile) {
		if (profile == null) {
			profile = SingularityConfig.collapseDefaultSyncProfile();
		}
		UUID uuid = player.getUuid();
		SingularityConfig.CollapseSyncProfile defaultProfile = SingularityConfig.collapseDefaultSyncProfile();
		SingularityConfig.CollapseSyncProfile previous = singularityPlayerProfiles.get(uuid);
		if (profile == defaultProfile) {
			if (previous != null) {
				singularityPlayerProfiles.remove(uuid);
				markDirty();
			}
		} else if (previous != profile) {
			singularityPlayerProfiles.put(uuid, profile);
			markDirty();
		}
		if (profile == SingularityConfig.CollapseSyncProfile.CINEMATIC && isCollapseActive()) {
			sendCollapseSchedule(player);
		}
	}

	public void syncProfileOnJoin(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		if (getCollapseProfile(player) == SingularityConfig.CollapseSyncProfile.CINEMATIC && isCollapseActive()) {
			sendCollapseSchedule(player);
		}
	}

	private boolean shouldPlayerTriggerBroadcast(ServerPlayerEntity player) {
		return getCollapseProfile(player) != SingularityConfig.CollapseSyncProfile.MINIMAL;
	}

	private boolean isCollapseActive() {
		return singularityState == SingularityState.FUSING
				|| singularityState == SingularityState.COLLAPSE
				|| singularityState == SingularityState.CORE
				|| singularityState == SingularityState.RING
				|| singularityState == SingularityState.DISSIPATION;
	}

	private CollapseBroadcastDecision evaluateBroadcastDecision(ServerWorld world, ChunkPos chunk) {
		SingularityConfig.CollapseBroadcastMode mode = SingularityConfig.collapseBroadcastMode();
		int radius = Math.max(0, SingularityConfig.collapseBroadcastRadius());
		if (mode == SingularityConfig.CollapseBroadcastMode.IMMEDIATE || radius <= 0) {
			return CollapseBroadcastDecision.immediate(mode);
		}
		boolean notifyNow = isChunkWithinBroadcastRadius(world, chunk, radius);
		return notifyNow ? CollapseBroadcastDecision.immediate(mode) : CollapseBroadcastDecision.buffered(mode);
	}

	private boolean isChunkWithinBroadcastRadius(ServerWorld world, ChunkPos chunk, int radius) {
		double thresholdSq = radius * (double) radius;
		double centerX = chunk.getCenterX();
		double centerZ = chunk.getCenterZ();
		for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive() && !player.isSpectator())) {
			if (!shouldPlayerTriggerBroadcast(player)) {
				continue;
			}
			double dx = player.getX() - centerX;
			double dz = player.getZ() - centerZ;
			if (dx * dx + dz * dz <= thresholdSq) {
				return true;
			}
		}
		return false;
	}

	private void markChunkBroadcastBuffered(ChunkPos chunk, CollapseBroadcastDecision decision) {
		if (collapseBufferedChunks.add(chunk.toLong())) {
			SingularityChunkContext.recordBroadcastBuffered(chunk, decision.mode());
		}
	}

	private void flushBufferedChunks(ServerWorld world) {
		flushBufferedChunks(world, false);
	}

	private void flushBufferedChunks(ServerWorld world, boolean force) {
		if (collapseBufferedChunks.isEmpty()) {
			return;
		}
		SingularityConfig.CollapseBroadcastMode mode = SingularityConfig.collapseBroadcastMode();
		int radius = Math.max(0, SingularityConfig.collapseBroadcastRadius());
		LongIterator iterator = collapseBufferedChunks.iterator();
		while (iterator.hasNext()) {
			long packed = iterator.nextLong();
			ChunkPos chunk = new ChunkPos(packed);
			boolean shouldFlush = force
					|| mode == SingularityConfig.CollapseBroadcastMode.IMMEDIATE
					|| radius <= 0
					|| isChunkWithinBroadcastRadius(world, chunk, radius);
			if (!shouldFlush) {
				continue;
			}
			BlockPos pos = new BlockPos(chunk.getCenterX(), world.getBottomY(), chunk.getCenterZ());
			world.getChunkManager().markForUpdate(pos);
			SingularityChunkContext.recordBroadcastFlushed(chunk);
			iterator.remove();
		}
	}

	private static final class CollapseBroadcastDecision {
		private final boolean notifyPlayers;
		private final SingularityConfig.CollapseBroadcastMode mode;
		private final int updateFlags;

		private CollapseBroadcastDecision(boolean notifyPlayers, SingularityConfig.CollapseBroadcastMode mode) {
			this.notifyPlayers = notifyPlayers;
			this.mode = mode;
			this.updateFlags = notifyPlayers ? Block.NOTIFY_LISTENERS : Block.NOTIFY_NEIGHBORS;
		}

		static CollapseBroadcastDecision immediate(SingularityConfig.CollapseBroadcastMode mode) {
			return new CollapseBroadcastDecision(true, mode);
		}

		static CollapseBroadcastDecision buffered(SingularityConfig.CollapseBroadcastMode mode) {
			return new CollapseBroadcastDecision(false, mode);
		}

		boolean notifyPlayers() {
			return notifyPlayers;
		}

		int updateFlags() {
			return updateFlags;
		}

		SingularityConfig.CollapseBroadcastMode mode() {
			return mode;
		}
	}

	private void broadcastCollapseSchedule(ServerWorld world) {
		if (!isCollapseActive()) {
			return;
		}
		SingularitySchedulePayload payload = buildSchedulePayload();
		if (payload == null) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive() && !player.isSpectator())) {
			if (getCollapseProfile(player) == SingularityConfig.CollapseSyncProfile.CINEMATIC) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private void sendCollapseSchedule(ServerPlayerEntity player) {
		if (player == null || !isCollapseActive()) {
			return;
		}
		SingularitySchedulePayload payload = buildSchedulePayload();
		if (payload == null) {
			return;
		}
		ServerPlayNetworking.send(player, payload);
	}

	private SingularitySchedulePayload buildSchedulePayload() {
		if (singularityRingChunkCounts.isEmpty() || singularityRingThresholds.isEmpty()) {
			return null;
		}
		List<SingularitySchedulePayload.RingEntry> rings = new ArrayList<>();
		int ringCount = Math.min(singularityRingChunkCounts.size(), singularityRingThresholds.size());
		for (int i = 0; i < ringCount; i++) {
			int chunks = singularityRingChunkCounts.getInt(i);
			if (chunks <= 0) {
				continue;
			}
			int side = i < singularityRingRadii.size() ? Math.max(1, singularityRingRadii.getInt(i)) : 1;
			double threshold = singularityRingThresholds.getDouble(i);
			int tickInterval = resolveTickIntervalForSide(side);
			rings.add(new SingularitySchedulePayload.RingEntry(i, chunks, side, threshold, tickInterval));
		}
		if (rings.isEmpty()) {
			return null;
		}
		return new SingularitySchedulePayload(rings);
	}

	private int resolveTickIntervalForSide(int sideLength) {
		for (SingularityConfig.RadiusDelay delay : SingularityConfig.radiusDelays()) {
			if (sideLength <= delay.side) {
				return Math.max(1, delay.ticks);
			}
		}
		return configuredCollapseTickInterval();
	}

	private boolean isSingularityCollapseEnabled(ServerWorld world) {
		return SingularityConfig.collapseEnabled()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_COLLAPSE_ENABLED);
	}
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
		applyInfectiousContactDamage(world);
		applyInfectiousInventoryEffects(world);
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

		if (!apocalypseMode && tierDuration > 0 && ticksInTier >= tierDuration && singularityState == SingularityState.DORMANT) {
			advanceTier(world);
		}

		if (containmentLevel > 0 && totalTicks % 200 == 0) {
			containmentLevel = Math.max(0, containmentLevel - 1);
			markDirty();
		}

		if (!isSingularityActive() && !shouldSkipSpreadThisTick(world)) {
			BlockMutationHelper.mutateAroundSources(world, virusSources, tier, apocalypseMode);
		}
		reinforceCores(world, tier);
		maybeSpawnMatrixCube(world, tier);

		applyDifficultyRules(world, tier);
		maybePushTierFiveBarrier(world, tier);
		tickSingularity(world);
		runEvents(world, tier);
		boostAmbientSpawns(world, tier);
		pulseHelmetTrackers(world);
	}

	private void runEvents(ServerWorld world, InfectionTier tier) {
		if (singularityState != SingularityState.DORMANT) {
			return;
		}
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
		if (singularityState != SingularityState.DORMANT) {
			return;
		}
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

	private void maybePushTierFiveBarrier(ServerWorld world, InfectionTier tier) {
		if (tier.getIndex() < InfectionTier.maxIndex() || apocalypseMode) {
			nextTierFiveBarrierPushTick = 0L;
			deactivateTierFiveBarrier(world);
			return;
		}
		long tierDuration = getTierDuration(tier);
		if (tierDuration <= 0 || ticksInTier >= tierDuration / 2) {
			nextTierFiveBarrierPushTick = 0L;
			deactivateTierFiveBarrier(world);
			return;
		}
		if (virusSources.isEmpty()) {
			deactivateTierFiveBarrier(world);
			return;
		}
		long now = world.getTime();
		if (now < nextTierFiveBarrierPushTick) {
			return;
		}
		nextTierFiveBarrierPushTick = now + TIER_FIVE_BARRIER_INTERVAL;
		boolean pushed = false;
		for (BlockPos source : virusSources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			pushPlayersFromBlock(world, source, TIER_FIVE_BARRIER_RADIUS, 0.9D, false);
			pushed = true;
		}
		if (pushed) {
			tierFiveBarrierActive = true;
		}
	}

	private void tickSingularity(ServerWorld world) {
		ensureSingularityGamerules(world);
		tickSingularityBorder(world);
		if (!infected) {
			if (singularityState != SingularityState.DORMANT) {
				clearFuseEntities(world);
				revertSingularityBlock(world, false);
				resetSingularityState(world);
				markDirty();
			}
			return;
		}
		if (singularityState == SingularityState.DORMANT) {
			tickSingularityPreGeneration(world);
		}
		switch (singularityState) {
			case DORMANT -> {
				if (apocalypseMode) {
					return;
				}
				long remaining = getTicksUntilFinalWave();
				long fuseDelay = configuredFuseExplosionDelayTicks();
				if (remaining > 0 && remaining <= fuseDelay && currentHealth <= 0.0D) {
					logSingularityStateChange(SingularityState.FUSING,
							"finalWaveWarning remaining=" + remaining + " fuseDelay=" + fuseDelay);
					singularityState = SingularityState.FUSING;
					singularityTicks = fuseDelay;
					singularityShellCollapsed = false;
					singularityFusePulseTicker = 0;
					singularityFuseElapsed = 0;
					singularityCollapseTotalChunks = 0;
					singularityCollapseCompletedChunks = 0;
					singularityCollapseBarDelay = 0;
					singularityCollapseCompleteHold = 0;
					singularityCenter = representativePos(world, world.getRandom());
					applyCollapseDistanceOverrides(world);
					if (isSingularityCollapseEnabled(world)) {
						prepareSingularityChunkQueue(world);
					} else {
						singularityChunkQueue.clear();
					}
					broadcast(world, Text.translatable("message.the-virus-block.singularity_warning").formatted(Formatting.LIGHT_PURPLE));
					markDirty();
				}
				clearFuseEntities(world);
			}
			case FUSING -> {
				tickSingularityPreGeneration(world);
				tickSingularityChunkPreload(world);
				tickFuseWatchdog(world);
				if (singularityTicks > 0) {
					singularityTicks--;
					singularityFuseElapsed++;
					emitFuseEffects(world);
					maintainFuseEntities(world);
					if (!singularityShellCollapsed && singularityFuseElapsed >= configuredFuseShellCollapseTicks()) {
						singularityShellCollapsed = true;
						if (!shellsCollapsed) {
							collapseShells(world);
						}
					}
					if (singularityTicks <= 0) {
						detonateFuseCore(world);
						if (isSingularityCollapseEnabled(world)) {
							logSingularityStateChange(SingularityState.COLLAPSE,
									"fuseComplete elapsed=" + singularityFuseElapsed + " allowCollapse="
											+ isSingularityCollapseEnabled(world));
							singularityState = SingularityState.COLLAPSE;
							singularityDebugLogCooldown = 0;
							singularityCollapseRadius = computeInitialCollapseRadius(world);
							singularityCollapseDescending = true;
							prepareSingularityChunkQueue(world);
							singularityCollapseBarDelay = SINGULARITY_COLLAPSE_BAR_DELAY_TICKS;
							singularityCollapseCompleteHold = SINGULARITY_COLLAPSE_COMPLETE_HOLD_TICKS;
							singularityCollapseTickCooldown = 0;
							singularityBorderElapsed = 0L;
							singularityBorderPendingDeployment = true;
							if (singularityCenter == null) {
								singularityCenter = representativePos(world, world.getRandom());
								if (singularityCenter == null && !virusSources.isEmpty()) {
									singularityCenter = virusSources.iterator().next();
								}
							}
							activateSingularityBlock(world);
							broadcast(world, Text.translatable("message.the-virus-block.singularity_collapse").formatted(Formatting.DARK_PURPLE));
							markDirty();
						} else {
							skipSingularityCollapse(world, "disabled before activation");
						}
					}
				}
			}
			case COLLAPSE -> processSingularityCollapse(world);
			case CORE -> processSingularityCore(world);
			case RING -> processSingularityRing(world);
			case DISSIPATION -> processSingularityDissipation(world);
			case RESET -> processSingularityReset(world);
		}
		maintainBlackholePearl(world);
	}

	private void emitFuseEffects(ServerWorld world) {
		if (virusSources.isEmpty()) {
			return;
		}
		float intensity = 1.0F - (float) singularityTicks / Math.max(1.0F, (float) configuredFuseExplosionDelayTicks());
		int particleCount = MathHelper.clamp(2 + (int) (intensity * 6), 2, 10);
		int pulseInterval = Math.max(4, configuredFusePulseInterval() - MathHelper.floor(intensity * 4));
		singularityFusePulseTicker++;
		boolean pulse = singularityFusePulseTicker >= pulseInterval;
		if (pulse) {
			singularityFusePulseTicker = 0;
		}
		for (BlockPos source : virusSources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			Vec3d fusePos = resolveFusePosition(world, source);
			double baseX = fusePos.x;
			double baseY = fusePos.y;
			double baseZ = fusePos.z;
			for (int i = 0; i < particleCount; i++) {
				double x = baseX + world.random.nextGaussian() * 0.3D;
				double y = baseY + world.random.nextGaussian() * 0.3D;
				double z = baseZ + world.random.nextGaussian() * 0.3D;
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
			}
			if (pulse) {
				world.spawnParticles(ParticleTypes.FLASH,
						baseX,
						baseY,
						baseZ,
						1,
						0.0D,
						0.0D,
						0.0D,
						0.0D);
				world.spawnParticles(SINGULARITY_FUSE_GLOW,
						baseX,
						baseY + 0.1D,
						baseZ,
						6,
						0.25D,
						0.2D,
						0.25D,
						0.0D);
				world.spawnParticles(ParticleTypes.GLOW,
						baseX,
						baseY + 0.2D,
						baseZ,
						4,
						0.2D,
						0.2D,
						0.2D,
						0.0D);
				world.playSound(null,
						baseX,
						baseY,
						baseZ,
						SoundEvents.ENTITY_TNT_PRIMED,
						SoundCategory.BLOCKS,
						0.8F + intensity * 0.4F,
						0.8F + world.random.nextFloat() * 0.2F);
			}
		}
	}

	private void maintainFuseEntities(ServerWorld world) {
		activeFuseEntities.entrySet().removeIf(entry -> {
			Entity entity = world.getEntity(entry.getValue());
			if (entity instanceof VirusFuseEntity fuse && fuse.isAlive()) {
				return false;
			}
			InfectionLog.info(LogChannel.SINGULARITY_FUSE, "Removing dead fuse entity at {}", entry.getKey());
			return true;
		});
		for (BlockPos source : virusSources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			clearBlockForFuse(world, source);
			if (activeFuseEntities.containsKey(source)) {
				continue;
			}
			VirusFuseEntity fuse = new VirusFuseEntity(world, source);
			if (world.spawnEntity(fuse)) {
				activeFuseEntities.put(source.toImmutable(), fuse.getUuid());
				InfectionLog.info(LogChannel.SINGULARITY_FUSE, "Spawned fuse entity at {}", source);
			} else {
				TheVirusBlock.LOGGER.warn("[Fuse] Failed to spawn fuse entity at {}", source);
			}
		}
	}

	private void maintainBlackholePearl(ServerWorld world) {
		if (!ENABLE_BLACKHOLE_PEARL) {
			clearBlackholePearl(world);
			return;
		}
		if (singularityState == SingularityState.DORMANT || singularityCenter == null) {
			clearBlackholePearl(world);
			return;
		}
		if (!world.isChunkLoaded(ChunkPos.toLong(singularityCenter))) {
			return;
		}
		BlackholePearlEntity pearl = getBlackholePearl(world);
		if (pearl == null) {
			pearl = BlackholePearlEntity.create(world, singularityCenter);
			pearl.setTargetScale(getBlackholeTargetScale());
			if (world.spawnEntity(pearl)) {
				blackholePearlId = pearl.getUuid();
			}
		} else {
			pearl.setAnchor(singularityCenter);
			pearl.setTargetScale(getBlackholeTargetScale());
		}
	}

	private float getBlackholeTargetScale() {
		return switch (singularityState) {
			case CORE, RING, DISSIPATION -> BLACKHOLE_CORE_SCALE;
			default -> BLACKHOLE_PRIMARY_SCALE;
		};
	}

	private void clearBlackholePearl(ServerWorld world) {
		if (blackholePearlId == null) {
			return;
		}
		Entity entity = world.getEntity(blackholePearlId);
		if (entity instanceof BlackholePearlEntity pearl) {
			pearl.discard();
		}
		blackholePearlId = null;
	}

	@Nullable
	private BlackholePearlEntity getBlackholePearl(ServerWorld world) {
		if (blackholePearlId == null) {
			return null;
		}
		Entity entity = world.getEntity(blackholePearlId);
		if (entity instanceof BlackholePearlEntity pearl) {
			return pearl;
		}
		blackholePearlId = null;
		return null;
	}

	private void installSingularityGlow(ServerWorld world) {
		if (singularityCenter == null) {
			return;
		}
		removeSingularityGlow(world);
		for (BlockPos offset : SINGULARITY_GLOW_OFFSETS) {
			BlockPos target = singularityCenter.add(offset);
			if (!world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}
			BlockState state = world.getBlockState(target);
			if (!state.isAir() && !state.isOf(Blocks.LIGHT)) {
				continue;
			}
			BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
			world.setBlockState(target, light, Block.NOTIFY_LISTENERS);
			singularityGlowNodes.add(target.toImmutable());
		}
	}

	private void removeSingularityGlow(ServerWorld world) {
		if (singularityGlowNodes.isEmpty()) {
			if (singularityCenter == null) {
				return;
			}
		}
		for (BlockPos pos : singularityGlowNodes) {
			if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		singularityGlowNodes.clear();
		if (singularityCenter != null) {
			for (BlockPos offset : SINGULARITY_GLOW_OFFSETS) {
				BlockPos target = singularityCenter.add(offset);
				if (world.getBlockState(target).isOf(Blocks.LIGHT)) {
					world.setBlockState(target, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
	}

	private void clearBlockForFuse(ServerWorld world, BlockPos pos) {
		if (fuseClearedBlocks.containsKey(pos)) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		if (!state.isOf(ModBlocks.VIRUS_BLOCK)) {
			return;
		}
		suppressSourceUnregister(pos);
		fuseClearedBlocks.put(pos.toImmutable(), state);
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.SKIP_DROPS);
	}

	private void restoreClearedFuseBlocks(ServerWorld world) {
		if (fuseClearedBlocks.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<BlockPos, BlockState>> iterator = fuseClearedBlocks.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<BlockPos, BlockState> entry = iterator.next();
			BlockPos pos = entry.getKey();
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}
			BlockState current = world.getBlockState(pos);
			if (current.isAir()) {
				world.setBlockState(pos, entry.getValue(), Block.NOTIFY_LISTENERS);
			}
			iterator.remove();
		}
	}

	public void onWorldLoad(ServerWorld world) {
		ensureSingularityGamerules(world);
		if (singularityBorderActive || singularityBorderHasSnapshot) {
			restoreSingularityBorder(world);
		}
		if (singularityState == SingularityState.DORMANT) {
			resetSingularityPreparation("worldLoad");
		}
	}

	private void ensureSingularityGamerules(ServerWorld world) {
		if (world == null || world.getServer() == null) {
			return;
		}
		applySingularityGamerule(world, TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION, SingularityConfig.allowChunkGeneration());
		applySingularityGamerule(world, TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD, SingularityConfig.allowOutsideBorderLoad());
	}

	private void applySingularityGamerule(ServerWorld world,
			GameRules.Key<GameRules.BooleanRule> rule,
			boolean desired) {
		GameRules.BooleanRule gamerule = world.getGameRules().get(rule);
		if (gamerule.get() != desired) {
			gamerule.set(desired, world.getServer());
		}
	}

	private void resetSingularityPreparation(String reason) {
		singularityChunkQueue.clear();
		singularityPreGenQueue.clear();
		singularityPreloadQueue.clear();
		singularityPinnedChunks.clear();
		singularityPreGenMissingChunks = 0;
		singularityPreloadMissingChunks = 0;
		singularityPreGenComplete = false;
		singularityPreloadComplete = false;
		singularityPreGenLogCooldown = 0;
		singularityPreloadLogCooldown = 0;
		singularityResetQueue.clear();
		singularityResetProcessed.clear();
		singularityResetDelay = 0;
		singularityDestructionEngine = SingularityDestructionEngine.create();
		singularityDestructionDirty = true;
		if (SingularityConfig.debugLogging()) {
			InfectionLog.info(LogChannel.SINGULARITY, "[prep] reset collapse staging reason={}", reason);
		}
	}

	public boolean isFuseClearedBlock(BlockPos pos) {
		return fuseClearedBlocks.containsKey(pos);
	}

	private boolean isVirusCoreBlock(BlockPos pos, BlockState state) {
		return state.isOf(ModBlocks.VIRUS_BLOCK) || fuseClearedBlocks.containsKey(pos);
	}

	private void suppressSourceUnregister(BlockPos pos) {
		suppressedUnregisters.add(pos.toImmutable());
	}

	public boolean shouldSkipUnregister(BlockPos pos) {
		return suppressedUnregisters.remove(pos);
	}

	private void clearFuseEntities(ServerWorld world) {
		clearFuseEntities(world, true);
	}

	private void clearFuseEntities(ServerWorld world, boolean restoreBlocks) {
		if (activeFuseEntities.isEmpty()) {
			if (restoreBlocks) {
				restoreClearedFuseBlocks(world);
			}
			return;
		}
		for (UUID uuid : activeFuseEntities.values()) {
			Entity entity = world.getEntity(uuid);
			if (entity != null) {
				entity.discard();
			}
		}
		activeFuseEntities.clear();
		if (restoreBlocks) {
			restoreClearedFuseBlocks(world);
		}
	}

	private boolean isSingularitySuppressionActive() {
		return singularityState == SingularityState.FUSING;
	}

	private boolean isSingularityActive() {
		return singularityState != SingularityState.DORMANT;
	}

	private float getSingularitySuppressionProgress() {
		if (!isSingularitySuppressionActive()) {
			return 0.0F;
		}
		return MathHelper.clamp(1.0F - (float) singularityTicks / (float) configuredFuseExplosionDelayTicks(), 0.0F, 1.0F);
	}

	private double getSingularityActivityMultiplier() {
		if (!isSingularitySuppressionActive()) {
			return 1.0D;
		}
		return MathHelper.clamp(1.0D - getSingularitySuppressionProgress(), 0.0D, 1.0D);
	}

	private float getMatrixCubeSingularityFactor() {
		return switch (singularityState) {
			case DORMANT -> 1.0F;
			case FUSING -> 0.35F;
			default -> 0.0F;
		};
	}

	private boolean shouldSkipSpreadThisTick(ServerWorld world) {
		float progress = getSingularitySuppressionProgress();
		if (progress <= 0.0F) {
			return false;
		}
		if (progress >= 0.999F) {
			return true;
		}
		return world.random.nextFloat() < progress;
	}

	private int computeInitialCollapseRadius(ServerWorld world) {
		int radius = configuredCollapseMaxRadius();
		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (!players.isEmpty()) {
			double maxDistance = 0.0D;
			BlockPos center = singularityCenter != null ? singularityCenter : players.get(0).getBlockPos();
			for (ServerPlayerEntity player : players) {
				double distance = Math.max(player.squaredDistanceTo(Vec3d.ofCenter(center)), 0.0D);
				maxDistance = Math.max(maxDistance, Math.sqrt(distance));
			}
			radius = (int) Math.min(configuredCollapseMaxRadius(), Math.max(32.0D, maxDistance + 32.0D));
		}
		return Math.max(16, radius);
	}

	private void tickSingularityBorder(ServerWorld world) {
		if (!singularityBorderActive) {
			SingularityChunkContext.disableBorderGuard(world);
			singularityBorderResetCountdown = -1;
			return;
		}
		SingularityChunkContext.enableBorderGuard(world);
		singularityBorderLastDiameter = world.getWorldBorder().getSize();
		if (singularityBorderDuration > 0L) {
			singularityBorderElapsed = Math.min(singularityBorderDuration, singularityBorderElapsed + 1);
		}
		maybeAutoResetBorder(world);
	}

	private void deploySingularityBorder(ServerWorld world) {
		if (singularityCenter == null || singularityRingThresholds.isEmpty()) {
			return;
		}
		WorldBorder border = world.getWorldBorder();
		captureOriginalBorder(border);
		double centerX = singularityCenter.getX() + 0.5D;
		double centerZ = singularityCenter.getZ() + 0.5D;
		double outerRadius = configuredBarrierStartRadius();
		double finalRadius = configuredBarrierEndRadius();
		double initialDiameter = Math.max(SINGULARITY_BORDER_MIN_DIAMETER, outerRadius * 2.0D);
		double finalDiameter = Math.max(SINGULARITY_BORDER_FINAL_DIAMETER, finalRadius * 2.0D);
		border.setCenter(centerX, centerZ);
		border.setSize(initialDiameter);
		singularityBorderActive = true;
		singularityBorderPendingDeployment = false;
		singularityBorderCenterX = centerX;
		singularityBorderCenterZ = centerZ;
		singularityBorderInitialDiameter = initialDiameter;
		singularityBorderTargetDiameter = finalDiameter;
		singularityBorderLastDiameter = initialDiameter;
		singularityBorderResetCountdown = -1;
		singularityInitialBorderDiameter = initialDiameter;
		singularityFinalBorderDiameter = finalDiameter;
		int ringStages = Math.max(1, singularityRingActualCount);
		singularityTotalRingTicks = (long) ringStages * configuredCollapseTickInterval();
		long durationTicks = configuredBorderDurationTicks();
		long durationMillis = Math.max(50L, durationTicks * 50L);
		InfectionLog.info(LogChannel.SINGULARITY,
				"Deploying border center=({},{}) outerRadius={} finalRadius={} initialDiameter={} finalDiameter={} durationTicks={}",
				String.format("%.2f", centerX),
				String.format("%.2f", centerZ),
				String.format("%.2f", outerRadius),
				String.format("%.2f", finalRadius),
				String.format("%.2f", initialDiameter),
				String.format("%.2f", finalDiameter),
				durationTicks);
		singularityBorderDuration = durationTicks;
		singularityBorderElapsed = 0L;
		singularityRingTickAccumulator = 0L;
		singularityRingIndex = -1;
		singularityRingPendingChunks = 0;
		markDirty();
		InfectionLog.info(LogChannel.SINGULARITY,
				"Border initialized size={} (should equal initialDiameter)",
				String.format("%.2f", border.getSize()));
		syncSingularityBorder(world, initialDiameter, 0L, finalDiameter);
		world.getWorldBorder().interpolateSize(initialDiameter, finalDiameter, durationMillis);
		SingularityChunkContext.enableBorderGuard(world);
	}

	private void restoreSingularityBorder(ServerWorld world) {
		if (!singularityBorderActive && !singularityBorderHasSnapshot) {
			return;
		}
		WorldBorder border = world.getWorldBorder();
		if (singularityBorderHasSnapshot) {
			border.setCenter(singularityBorderOriginalCenterX, singularityBorderOriginalCenterZ);
			border.setSize(singularityBorderOriginalDiameter);
			border.setSafeZone(singularityBorderOriginalSafeZone);
			border.setDamagePerBlock(singularityBorderOriginalDamagePerBlock);
			border.setWarningBlocks(singularityBorderOriginalWarningBlocks);
			border.setWarningTime(singularityBorderOriginalWarningTime);
		}
		clearSingularityBorderState();
		SingularityChunkContext.disableBorderGuard(world);
		markDirty();
		syncSingularityBorder(world);
	}

	private void maybeAutoResetBorder(ServerWorld world) {
		if (!SingularityConfig.barrierAutoReset() || world == null) {
			return;
		}
		if (!singularityBorderActive) {
			singularityBorderResetCountdown = -1;
			return;
		}
		boolean shrinkFinished = singularityBorderDuration <= 0L || singularityBorderElapsed >= singularityBorderDuration;
		boolean collapseFinished = (singularityState != SingularityState.COLLAPSE && singularityState != SingularityState.RESET)
				|| singularityChunkQueue.isEmpty();
		if (!shrinkFinished || !collapseFinished) {
			return;
		}
		if (singularityBorderResetCountdown < 0) {
			long delay = Math.max(0L, SingularityConfig.barrierResetDelayTicks());
			singularityBorderResetCountdown = delay > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delay;
			return;
		}
		if (singularityBorderResetCountdown-- > 0) {
			return;
		}
		restoreSingularityBorder(world);
	}

	private void syncSingularityBorder(ServerWorld world) {
		syncSingularityBorder(world, world != null ? world.getWorldBorder().getSize() : singularityBorderLastDiameter, singularityBorderElapsed, singularityBorderTargetDiameter);
	}

	private void syncSingularityBorder(ServerWorld world, double reportedDiameter, long reportedElapsed, double reportedTargetDiameter) {
		if (world == null) {
			return;
		}
		double diameter = reportedDiameter;
		double centerX = singularityBorderCenterX;
		double centerZ = singularityBorderCenterZ;
		if (centerX == 0.0D && singularityCenter != null) {
			centerX = singularityCenter.getX() + 0.5D;
		}
		if (centerZ == 0.0D && singularityCenter != null) {
			centerZ = singularityCenter.getZ() + 0.5D;
		}
		SingularityBorderPayload payload = new SingularityBorderPayload(
				singularityBorderActive,
				centerX,
				centerZ,
				singularityBorderInitialDiameter,
				diameter,
				reportedTargetDiameter,
				singularityBorderDuration,
				reportedElapsed,
				singularityState.name()
		);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private void captureOriginalBorder(WorldBorder border) {
		if (singularityBorderHasSnapshot) {
			return;
		}
		singularityBorderOriginalCenterX = border.getCenterX();
		singularityBorderOriginalCenterZ = border.getCenterZ();
		singularityBorderOriginalDiameter = border.getSize();
		singularityBorderOriginalSafeZone = border.getSafeZone();
		singularityBorderOriginalDamagePerBlock = border.getDamagePerBlock();
		singularityBorderOriginalWarningBlocks = border.getWarningBlocks();
		singularityBorderOriginalWarningTime = border.getWarningTime();
		singularityBorderHasSnapshot = true;
	}

	private void clearSingularityBorderState() {
		singularityBorderActive = false;
		singularityBorderPendingDeployment = false;
		singularityBorderHasSnapshot = false;
		singularityBorderCenterX = 0.0D;
		singularityBorderCenterZ = 0.0D;
		singularityBorderInitialDiameter = 0.0D;
		singularityBorderTargetDiameter = 0.0D;
		singularityBorderDuration = 0L;
		singularityBorderElapsed = 0L;
		singularityBorderLastDiameter = 0.0D;
		singularityBorderOriginalCenterX = 0.0D;
		singularityBorderOriginalCenterZ = 0.0D;
		singularityBorderOriginalDiameter = 0.0D;
		singularityBorderOriginalSafeZone = 0.0D;
		singularityBorderOriginalDamagePerBlock = 0.0D;
		singularityBorderOriginalWarningBlocks = 0;
		singularityBorderOriginalWarningTime = 0;
		singularityRingThresholds.clear();
		singularityRingChunkCounts.clear();
		singularityRingChunks.clear();
		singularityRingIndex = -1;
		singularityRingPendingChunks = 0;
		singularityInitialBorderDiameter = 0.0D;
		singularityFinalBorderDiameter = 0.0D;
		singularityTotalRingTicks = 0L;
		singularityRingTickAccumulator = 0L;
		singularityBorderOuterRadius = 0.0D;
		singularityBorderInnerRadius = 0.0D;
		singularityRingActualCount = 0;
		singularityBorderResetCountdown = -1;
	}

	private void activateSingularityBlock(ServerWorld world) {
		if (singularityCenter == null || !world.isChunkLoaded(ChunkPos.toLong(singularityCenter))) {
			return;
		}
		BlockState state = world.getBlockState(singularityCenter);
		if (!state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
			world.setBlockState(singularityCenter, ModBlocks.SINGULARITY_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
		}
		installSingularityGlow(world);
		fuseClearedBlocks.remove(singularityCenter);
		BlockEntity blockEntity = world.getBlockEntity(singularityCenter);
		if (blockEntity instanceof SingularityBlockEntity singularityBlock) {
			singularityBlock.startSequence(world);
		}
	}

	private void detonateFuseCore(ServerWorld world) {
		if (singularityCenter == null) {
			return;
		}
		Vec3d fusePos = resolveFusePosition(world, singularityCenter);
		BlockPos elevatedCenter = BlockPos.ofFloored(fusePos);
		if (!elevatedCenter.equals(singularityCenter)) {
			singularityCenter = elevatedCenter;
		}
		world.playSound(null,
				fusePos.x,
				fusePos.y,
				fusePos.z,
				SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS,
				4.0F,
				0.6F);
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
				fusePos.x,
				fusePos.y,
				fusePos.z,
				2,
				0.2D,
				0.2D,
				0.2D,
				0.01D);
		pushPlayersFromBlock(world, singularityCenter, FINAL_VULNERABILITY_BLAST_RADIUS, FINAL_VULNERABILITY_BLAST_SCALE, false);
		world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, singularityCenter, Block.getRawIdFromState(ModBlocks.VIRUS_BLOCK.getDefaultState()));
		clearFuseEntities(world, false);
	}

	private Vec3d resolveFusePosition(ServerWorld world, BlockPos source) {
		UUID fuseId = activeFuseEntities.get(source);
		if (fuseId != null) {
			Entity entity = world.getEntity(fuseId);
			if (entity instanceof VirusFuseEntity fuse) {
				return fuse.getPos();
			}
		}
		return Vec3d.ofCenter(source);
	}

	private void ensureSingularityCenter(ServerWorld world) {
		if (singularityCenter != null) {
			return;
		}
		BlockPos candidate = representativePos(world, world.getRandom());
		if (candidate == null && !virusSources.isEmpty()) {
			candidate = virusSources.iterator().next();
		}
		if (candidate == null) {
			candidate = world.getSpawnPos();
		}
		if (candidate != null) {
			singularityCenter = candidate.toImmutable();
		}
	}

	private void revertSingularityBlock(ServerWorld world, boolean remove) {
		if (singularityCenter == null || !world.isChunkLoaded(ChunkPos.toLong(singularityCenter))) {
			return;
		}
		BlockState state = world.getBlockState(singularityCenter);
		if (!state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
			return;
		}
		BlockState replacement = remove ? Blocks.AIR.getDefaultState() : ModBlocks.VIRUS_BLOCK.getDefaultState();
		world.setBlockState(singularityCenter, replacement, Block.NOTIFY_LISTENERS);
		SingularityBlockEntity.notifyStop(world, singularityCenter);
		world.getChunkManager().markForUpdate(singularityCenter);
		removeSingularityGlow(world);
		fuseClearedBlocks.remove(singularityCenter);
	}

	private void prepareSingularityChunkQueue(ServerWorld world) {
		singularityChunkQueue.clear();
		singularityCollapseTotalChunks = 0;
		singularityCollapseCompletedChunks = 0;
		singularityRingThresholds.clear();
		singularityRingChunkCounts.clear();
		singularityRingRadii.clear();
		singularityRingIndex = -1;
		singularityRingPendingChunks = 0;
		singularityInitialBorderDiameter = 0.0D;
		singularityFinalBorderDiameter = 0.0D;
		singularityTotalRingTicks = 0L;
		singularityRingTickAccumulator = 0L;
		ensureSingularityCenter(world);
		if (singularityCenter == null) {
			TheVirusBlock.LOGGER.warn("[Singularity] Unable to prepare collapse queue because center is null");
			return;
		}
		ChunkPos centerChunk = new ChunkPos(singularityCenter);
		int maxRadiusInChunks = Math.max(1, MathHelper.ceil(configuredCollapseMaxRadius() / 16.0F));
		boolean capturedOuterRadius = false;
		double outerRadius = 0.0D;
		double innerRadius = 0.0D;
		int actualRingCount = 0;
		LongSet seen = new LongOpenHashSet();
		for (int radius = maxRadiusInChunks; radius >= 0; radius--) {
			int before = singularityChunkQueue.size();
			appendChunkRing(centerChunk, radius, seen);
			int added = singularityChunkQueue.size() - before;
			if (added > 0) {
				double threshold = computeRingThreshold(radius);
				singularityRingThresholds.add(threshold);
				singularityRingChunkCounts.add(added);
				singularityRingRadii.add(radius);
				if (!capturedOuterRadius) {
					outerRadius = threshold;
					capturedOuterRadius = true;
				}
				innerRadius = threshold;
				actualRingCount++;
			}
		}
		if (singularityChunkQueue.isEmpty()) {
			long packed = centerChunk.toLong();
			singularityChunkQueue.addLast(packed);
			double threshold = computeRingThreshold(0);
			singularityRingThresholds.add(threshold);
			singularityRingChunkCounts.add(1);
			singularityRingRadii.add(0);
			if (!capturedOuterRadius) {
				outerRadius = threshold;
				capturedOuterRadius = true;
			}
			innerRadius = threshold;
			actualRingCount = Math.max(1, actualRingCount);
		}

		if (!singularityRingThresholds.isEmpty() && outerRadius > 0.0D) {
			double scale = configuredBarrierStartRadius() / outerRadius;
			for (int i = 0; i < singularityRingThresholds.size(); i++) {
				double scaled = singularityRingThresholds.getDouble(i) * scale;
				singularityRingThresholds.set(i, scaled);
			}
			outerRadius *= scale;
			innerRadius *= scale;
		}

		double finalThreshold = Math.max(SINGULARITY_BORDER_FINAL_DIAMETER * 0.5D, 0.0D);
		singularityRingThresholds.add(finalThreshold);
		singularityRingChunkCounts.add(0);
		singularityRingRadii.add(0);
		singularityCollapseTotalChunks = Math.max(1, singularityChunkQueue.size());
		singularityCollapseCompletedChunks = 0;
		singularityBorderOuterRadius = capturedOuterRadius ? outerRadius : configuredBarrierStartRadius();
		singularityBorderInnerRadius = innerRadius > 0.0D ? innerRadius : singularityBorderOuterRadius;
		singularityRingActualCount = Math.max(1, actualRingCount);
		rebuildSingularityDestruction(world);
		broadcastCollapseSchedule(world);
	}

	private void appendChunkRing(ChunkPos center, int radius, LongSet seen) {
		appendChunkRing(center, radius, seen, singularityChunkQueue);
	}

	private void appendChunkRing(ChunkPos center, int radius, LongSet seen, Deque<Long> target) {
		if (radius == 0) {
			addChunkToQueue(center.x, center.z, seen, target);
			return;
		}
		int minX = center.x - radius;
		int maxX = center.x + radius;
		int minZ = center.z - radius;
		int maxZ = center.z + radius;
		for (int x = minX; x <= maxX; x++) {
			addChunkToQueue(x, minZ, seen, target);
			addChunkToQueue(x, maxZ, seen, target);
		}
		for (int z = minZ + 1; z <= maxZ - 1; z++) {
			addChunkToQueue(minX, z, seen, target);
			addChunkToQueue(maxX, z, seen, target);
		}
	}

	private void addChunkToQueue(int chunkX, int chunkZ, LongSet seen, Deque<Long> target) {
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		long packed = pos.toLong();
		if (seen.add(packed)) {
			target.addLast(packed);
		}
	}

	private void rebuildSingularityDestruction(ServerWorld world) {
		singularityDestructionEngine = SingularityDestructionEngine.create();
		singularityDestructionEngine.setActiveColumnsPerTick(Math.max(16, SINGULARITY_COLLAPSE_CHUNKS_PER_STEP * 64));
		int minY = world.getBottomY();
		int maxY = world.getBottomY() + world.getDimension().height() - 1;
		singularityDestructionEngine.setColumnBounds(minY, maxY);
		List<Long> queueSnapshot = new ArrayList<>(singularityChunkQueue);
		singularityResetQueue.clear();
		singularityResetQueue.addAll(queueSnapshot);
		singularityResetProcessed.clear();
		singularityResetDelay = 0;
		int offset = 0;
		boolean reusePinnedChunks = singularityPreloadComplete && areAllCollapseChunksPinned(queueSnapshot);
		if (reusePinnedChunks) {
			if (!singularityPreloadQueue.isEmpty()) {
				singularityPreloadQueue.clear();
			}
			if (SingularityConfig.debugLogging()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[preload] reuse pinned chunks={} (skipping reload)",
						singularityPinnedChunks.size());
			}
		} else {
			singularityPreloadQueue = new ArrayDeque<>(queueSnapshot);
			singularityPreloadComplete = singularityPreloadQueue.isEmpty();
			singularityPreloadMissingChunks = 0;
		}
		singularityRingChunks = new ArrayList<>(singularityRingChunkCounts.size());
		for (int i = 0; i < singularityRingChunkCounts.size(); i++) {
			int count = singularityRingChunkCounts.getInt(i);
			List<ChunkPos> ringChunks;
			if (count <= 0) {
				ringChunks = Collections.emptyList();
			} else {
				ringChunks = new ArrayList<>(count);
				for (int j = 0; j < count && offset < queueSnapshot.size(); j++, offset++) {
					ringChunks.add(new ChunkPos(queueSnapshot.get(offset)));
				}
				double threshold = singularityRingThresholds.getDouble(i);
				singularityDestructionEngine.enqueueRing(i, threshold, ringChunks);
				if (SingularityConfig.debugLogging()) {
					InfectionLog.info(LogChannel.SINGULARITY, "ring {}: threshold={} chunks={}",
							i,
							roundDistance(threshold),
							count);
				}
			}
			singularityRingChunks.add(ringChunks);
		}
		singularityDestructionDirty = false;
		if (SingularityConfig.debugLogging()) {
			InfectionLog.info(LogChannel.SINGULARITY, "planner built {} rings ({} chunks queued)", singularityRingChunkCounts.size(), singularityChunkQueue.size());
		}
	}

	private void ensureDestructionEngine(ServerWorld world) {
		if (singularityDestructionEngine == null || singularityDestructionDirty) {
			rebuildSingularityDestruction(world);
		}
	}

	private double computeRingThreshold(int chunkRadius) {
		double diagonal = Math.max(1.0D, (chunkRadius + 1) * 16.0D * MathHelper.SQUARE_ROOT_OF_TWO);
		return diagonal;
	}

	private List<Double> copyRingThresholds() {
		List<Double> list = new ArrayList<>(singularityRingThresholds.size());
		for (double value : singularityRingThresholds) {
			list.add(value);
		}
		return list;
	}

	private List<Integer> copyRingCounts() {
		List<Integer> list = new ArrayList<>(singularityRingChunkCounts.size());
		for (int value : singularityRingChunkCounts) {
			list.add(value);
		}
		return list;
	}

	private List<Integer> copyRingRadii() {
		List<Integer> list = new ArrayList<>(singularityRingRadii.size());
		for (int value : singularityRingRadii) {
			list.add(value);
		}
		return list;
	}

	private void tickCollapseBarDelay() {
		if (singularityCollapseBarDelay >= 0) {
			singularityCollapseBarDelay--;
		}
	}

	private void tickSingularityChunkPreload(ServerWorld world) {
		if (!configuredPreloadEnabled()) {
			if (!singularityPreloadComplete) {
				singularityPreloadQueue.clear();
				singularityPreloadMissingChunks = 0;
				singularityPreloadComplete = true;
				InfectionLog.info(LogChannel.SINGULARITY, "[preload] skipped (disabled via config)");
			}
			return;
		}
		if (singularityPreloadComplete) {
			return;
		}
		if (singularityPreloadQueue.isEmpty()) {
			finishSingularityChunkPreload(world);
			return;
		}
		ServerChunkManager chunkManager = world.getChunkManager();
		boolean allowGeneration = SingularityConfig.allowChunkGeneration()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION);
		int budget = configuredPreloadChunksPerTick();
		int before = singularityPreloadQueue.size();
		while (budget-- > 0 && !singularityPreloadQueue.isEmpty()) {
			long packed = singularityPreloadQueue.pollFirst();
			ChunkPos pos = new ChunkPos(packed);
			SingularityChunkContext.pushChunkBypass(pos);
			try {
				if (world.isChunkLoaded(packed)) {
					pinSingularityChunk(world, pos);
					if (SingularityConfig.debugLogging()) {
						InfectionLog.info(LogChannel.SINGULARITY, "preload chunk ready {} (already loaded)", pos);
					}
					continue;
				}
				boolean outsideBorder = !world.getWorldBorder().contains(pos.getCenterX(), pos.getCenterZ());
				try {
					Chunk chunk = chunkManager.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
					if (chunk == null) {
						String reason = outsideBorder ? "outsideBorder" : (allowGeneration ? "missingChunk" : "generationDisabled");
						handlePreloadFailure(pos, allowGeneration, reason);
					} else {
						pinSingularityChunk(world, pos);
						if (SingularityConfig.debugLogging()) {
							if (allowGeneration) {
								InfectionLog.info(LogChannel.SINGULARITY, "preload chunk loaded {}", pos);
							} else {
								InfectionLog.info(LogChannel.SINGULARITY,
										"preload chunk loaded {} (generation disabled, disk fetch only)",
										pos);
							}
						}
					}
				} catch (IllegalStateException ex) {
					logChunkLoadException(world, "preload", pos, allowGeneration, ex);
					handlePreloadFailure(pos, allowGeneration, ex.getMessage());
				}
			} finally {
				SingularityChunkContext.popChunkBypass(pos);
			}
		}
		logPreloadProgress(before - singularityPreloadQueue.size(), singularityPreloadQueue.size());
		if (singularityPreloadQueue.isEmpty()) {
			finishSingularityChunkPreload(world);
		}
	}

	private void tickSingularityPreGeneration(ServerWorld world) {
		if (!configuredPreGenEnabled()) {
			if (!singularityPreGenComplete) {
				singularityPreGenQueue.clear();
				singularityPreGenMissingChunks = 0;
				singularityPreGenComplete = true;
				InfectionLog.info(LogChannel.SINGULARITY, "[preGen] skipped (disabled via config)");
			}
			return;
		}
		if (singularityPreGenComplete) {
			return;
		}
		if (singularityState != SingularityState.DORMANT && singularityState != SingularityState.FUSING) {
			return;
		}
		ensureSingularityCenter(world);
		if (singularityCenter == null) {
			return;
		}
		if (singularityPreGenQueue.isEmpty()) {
			rebuildSingularityPreGenQueue(world);
		}
		if (singularityPreGenQueue.isEmpty()) {
			return;
		}
		ServerChunkManager chunkManager = world.getChunkManager();
		int budget = configuredPreGenChunksPerTick();
		int before = singularityPreGenQueue.size();
		while (budget-- > 0 && !singularityPreGenQueue.isEmpty()) {
			long packed = singularityPreGenQueue.pollFirst();
			ChunkPos pos = new ChunkPos(packed);
			try {
				chunkManager.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
			} catch (IllegalStateException ex) {
				logChunkLoadException(world, "pregen", pos, true, ex);
				singularityPreGenMissingChunks++;
			}
		}
		logPreGenProgress(before - singularityPreGenQueue.size(), singularityPreGenQueue.size());
		if (singularityPreGenQueue.isEmpty()) {
			singularityPreGenComplete = true;
			InfectionLog.info(LogChannel.SINGULARITY,
					"[preGen] complete center={} total={} missing={}",
					singularityPreGenCenter,
					singularityPreGenTotalChunks,
					singularityPreGenMissingChunks);
			singularityPreGenQueue.clear();
			singularityPreGenLogCooldown = SINGULARITY_PREGEN_LOG_INTERVAL;
			markDirty();
		}
	}

	private void rebuildSingularityPreGenQueue(ServerWorld world) {
		if (singularityCenter == null) {
			return;
		}
		ChunkPos centerChunk = new ChunkPos(singularityCenter);
		singularityPreGenCenter = centerChunk;
		singularityPreGenQueue.clear();
		singularityPreGenMissingChunks = 0;
		singularityPreGenComplete = false;
		LongSet seen = new LongOpenHashSet();
		for (int radius = configuredPregenRadiusChunks(); radius >= 0; radius--) {
			appendChunkRing(centerChunk, radius, seen, singularityPreGenQueue);
		}
		singularityPreGenTotalChunks = singularityPreGenQueue.size();
		InfectionLog.info(LogChannel.SINGULARITY,
				"[preGen] queued {} chunks within {} blocks (center={})",
				singularityPreGenTotalChunks,
				String.format("%.2f", configuredPreGenRadiusBlocks()),
				singularityPreGenCenter);
		markDirty();
	}

	private void logPreGenProgress(int processed, int remaining) {
		if (remaining > 0 && processed <= 0 && singularityPreGenLogCooldown > 0) {
			singularityPreGenLogCooldown--;
			return;
		}
		singularityPreGenLogCooldown = SINGULARITY_PREGEN_LOG_INTERVAL;
		InfectionLog.info(LogChannel.SINGULARITY,
				"[preGen] processed={} remaining={} missing={} center={}",
				processed,
				remaining,
				singularityPreGenMissingChunks,
				singularityPreGenCenter);
	}

	private void handlePreloadFailure(ChunkPos pos, boolean generationAttempted, String reason) {
		singularityPreloadMissingChunks++;
		if (SingularityConfig.debugLogging()) {
			TheVirusBlock.LOGGER.warn("[Singularity] chunk preload failed for {} (generationAllowed={} reason={})",
					pos,
					generationAttempted,
					reason == null ? "<unknown>" : reason);
		}
	}

	private void finishSingularityChunkPreload(ServerWorld world) {
		if (singularityPreloadComplete) {
			return;
		}
		singularityPreloadComplete = true;
		InfectionLog.info(LogChannel.SINGULARITY,
				"[preload] complete missing={} pinned={}",
				singularityPreloadMissingChunks,
				singularityPinnedChunks.size());
		singularityPreloadLogCooldown = SINGULARITY_PRELOAD_LOG_INTERVAL;
		if (singularityPreloadMissingChunks > 0) {
			TheVirusBlock.LOGGER.warn("[Singularity] {} collapse chunks were not prepared before the event. Enable chunk generation or pre-load the area to avoid skips.", singularityPreloadMissingChunks);
		}
	}

	private void logPreloadProgress(int processed, int remaining) {
		if (remaining > 0 && processed <= 0 && singularityPreloadLogCooldown > 0) {
			singularityPreloadLogCooldown--;
			return;
		}
		singularityPreloadLogCooldown = SINGULARITY_PRELOAD_LOG_INTERVAL;
		InfectionLog.info(LogChannel.SINGULARITY,
				"[preload] processed={} remaining={} missing={} pinned={}",
				processed,
				remaining,
				singularityPreloadMissingChunks,
				singularityPinnedChunks.size());
	}

	private void logChunkLoadException(ServerWorld world,
			String phase,
			ChunkPos chunk,
			boolean generationAllowed,
			IllegalStateException error) {
		if (!SingularityConfig.debugLogging()) {
			return;
		}
		boolean bypassing = SingularityChunkContext.isBypassingChunk(chunk.x, chunk.z);
		boolean outsideBorder = !world.getWorldBorder().contains(chunk.getCenterX(), chunk.getCenterZ());
		double distance = chunkDistanceFromCenter(chunk.toLong());
		TheVirusBlock.LOGGER.warn("[Singularity] chunk load failed phase={} chunk={} allowGen={} bypass={} outsideBorder={} distance={} thread={}",
				phase,
				chunk,
				generationAllowed,
				bypassing,
				outsideBorder,
				String.format("%.2f", distance),
				Thread.currentThread().getName(),
				error);
	}

	private void processSingularityCollapse(ServerWorld world) {
		if (!isSingularityCollapseEnabled(world)) {
			skipSingularityCollapse(world, "disabled mid-collapse");
			return;
		}
		tickCollapseBarDelay();
		tickSingularityChunkPreload(world);
		flushBufferedChunks(world);
		if (singularityCollapseBarDelay <= 0 && singularityBorderPendingDeployment) {
			deploySingularityBorder(world);
		}
		ensureSingularityCenter(world);
		if (singularityCenter == null) {
			return;
		}
		ensureDestructionEngine(world);
		if (singularityChunkQueue.isEmpty() && singularityRingPendingChunks <= 0 && singularityRingIndex >= singularityRingThresholds.size() - 1) {
			if (singularityCollapseBarDelay <= 0) {
				if (singularityCollapseCompleteHold > 0) {
					singularityCollapseCompleteHold--;
					return;
				}
				if (singularityCollapseCompletedChunks >= singularityCollapseTotalChunks) {
					logSingularityStateChange(SingularityState.CORE, "collapse complete");
					singularityState = SingularityState.CORE;
					singularityRingTicks = SINGULARITY_RING_DURATION;
					singularityPhaseDelay = SINGULARITY_CORE_CHARGE_TICKS;
					broadcast(world, Text.translatable("message.the-virus-block.singularity_core").formatted(Formatting.RED));
					syncSingularityBorder(world);
					markDirty();
				}
			}
			return;
		}
		advanceRingIndexIfNeeded(world);
		if (singularityRingPendingChunks <= 0) {
			return;
		}
		if (!SingularityConfig.useMultithreadedCollapse()) {
			shutdownCollapseScheduler();
		}
		if (singularityCollapseTickCooldown > 0) {
			singularityCollapseTickCooldown--;
			return;
		}
		updateCollapseThroughput(world);
		logSingularityDebugInfo(world);
		boolean progressed;
		SingularityChunkContext.push(world);
		try {
			if (SingularityConfig.useMultithreadedCollapse()) {
				progressed = processCollapseWithScheduler(world);
			} else {
				progressed = singularityDestructionEngine.consumeColumns(this::processCollapseColumn, world);
			}
		} finally {
			SingularityChunkContext.pop(world);
		}
		if (progressed) {
			singularityCollapseStallTicks = 0;
			singularityCollapseTickCooldown = resolveTickIntervalForCurrentRing();
			singularityCollapseRadius = (int) Math.round(singularityDestructionEngine.getCurrentRadius());
		} else {
			logSingularityStall(world);
			singularityCollapseTickCooldown = 0;
		}
		if (singularityChunkQueue.isEmpty()) {
			singularityCollapseRadius = 0;
			singularityCollapseCompletedChunks = Math.min(singularityCollapseCompletedChunks, singularityCollapseTotalChunks);
		}
		flushBufferedChunks(world);
		markDirty();
	}

	private void skipSingularityCollapse(ServerWorld world, String reason) {
		logSingularityStateChange(SingularityState.DISSIPATION, reason);
		singularityState = SingularityState.DISSIPATION;
		singularityRingTicks = 0;
		singularityRingPendingChunks = 0;
		singularityRingIndex = -1;
		singularityCollapseTickCooldown = 0;
		singularityCollapseBarDelay = 0;
		singularityCollapseCompleteHold = 0;
		singularityCollapseRadius = 0;
		singularityChunkQueue.clear();
		singularityBorderPendingDeployment = false;
		singularityBorderDuration = 0L;
		singularityBorderElapsed = 0L;
		releaseSingularityChunkTickets(world);
		restoreSingularityBorder(world);
		singularityPhaseDelay = SINGULARITY_RESET_DELAY_TICKS;
		singularityRingTickAccumulator = 0L;
		singularityRingActualCount = 0;
		singularityRingThresholds.clear();
		singularityRingChunkCounts.clear();
		singularityRingChunks.clear();
		singularityRingRadii.clear();
		singularityDestructionDirty = true;
		shutdownCollapseScheduler();
		syncSingularityBorder(world);
		if (SingularityConfig.debugLogging()) {
			InfectionLog.info(LogChannel.SINGULARITY, "[skipCollapse] {}", reason);
		}
		flushBufferedChunks(world, true);
		collapseBufferedChunks.clear();
		broadcast(world, Text.translatable("message.the-virus-block.singularity_detonated").formatted(Formatting.DARK_PURPLE));
		markDirty();
	}

	private SingularityDestructionEngine.ColumnTask processCollapseColumn(SingularityDestructionEngine.ColumnTask task, ServerWorld world) {
		SingularityChunkContext.pushChunkBypass(task.chunk());
		try {
			ServerChunkManager chunkManager = world.getChunkManager();
			long chunkLong = task.chunk().toLong();
			boolean outsideAllowed = SingularityConfig.allowOutsideBorderLoad()
					&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD);
			boolean bypassingChunk = SingularityChunkContext.isBypassingChunk(task.chunk().x, task.chunk().z);
			if (!outsideAllowed && !bypassingChunk) {
				WorldBorder border = world.getWorldBorder();
				double chunkCenterX = task.chunk().getCenterX();
				double chunkCenterZ = task.chunk().getCenterZ();
				if (!border.contains(chunkCenterX, chunkCenterZ)) {
					double borderRadius = border.getSize() * 0.5D;
					double chunkDistance = chunkDistanceFromCenter(chunkLong);
					SingularityChunkContext.sampleBorderRejectedChunk(task.chunk(), chunkDistance, borderRadius);
					SingularityChunkContext.recordSkippedBorder();
					SingularityChunkContext.recordChunkBlocked(task.chunk(), "outside border");
					markCollapseChunkComplete(world, task.chunk());
					return null;
				}
			}

			boolean chunkLoaded = world.isChunkLoaded(chunkLong);
			boolean generationAllowed = SingularityConfig.allowChunkGeneration()
					&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION);
			if (!chunkLoaded && generationAllowed) {
				try {
					Chunk chunk = chunkManager.getChunk(task.chunk().x, task.chunk().z, ChunkStatus.FULL, true);
					if (chunk != null) {
						if (shouldPinCollapseChunks()) {
							pinSingularityChunk(world, task.chunk());
						}
						if (SingularityConfig.debugLogging()) {
							InfectionLog.info(LogChannel.SINGULARITY, "chunk loaded on demand {}", task.chunk());
						}
						return task;
					}
				} catch (IllegalStateException ex) {
					logChunkLoadException(world, "collapse", task.chunk(), generationAllowed, ex);
					// Chunk manager refused generation; treat as missing.
					SingularityChunkContext.recordSkippedMissing();
					SingularityChunkContext.recordChunkBlocked(task.chunk(), ex.getMessage());
				}
			}
			if (!chunkLoaded) {
				if (shouldAwaitMissingChunks()) {
					return task;
				}
				double chunkDistance = chunkDistanceFromCenter(chunkLong);
				SingularityChunkContext.sampleMissingChunk(task.chunk(), chunkDistance, generationAllowed);
				SingularityChunkContext.recordSkippedMissing();
				markCollapseChunkComplete(world, task.chunk());
				return null;
			}

			if (shouldPinCollapseChunks()) {
				pinSingularityChunk(world, task.chunk());
			}

			if (SingularityConfig.drainWaterAhead()) {
				drainFluidsAhead(world, task, layerBaseY(task));
			}

			if (SingularityConfig.useRingSliceMode()) {
				RingSliceStatus status = processRingSliceSingleThread(world, task);
				if (status == RingSliceStatus.RETRY) {
					return task.withSticky(true);
				}
				if (status == RingSliceStatus.COMPLETE) {
					markCollapseChunkComplete(world, task.chunk());
				}
				return null;
			}

			if (SingularityConfig.useRingSliceChunkMode()) {
				boolean chunkComplete = processChunkFillSingleThread(world, task);
				if (chunkComplete) {
					markCollapseChunkComplete(world, task.chunk());
				}
				return null;
			}

			return null;
		} finally {
			SingularityChunkContext.popChunkBypass(task.chunk());
		}
	}

	private void markCollapseChunkComplete(ServerWorld world, ChunkPos chunk) {
		SingularityChunkContext.recordColumnCompleted();
		long packed = chunk.toLong();
		singularityChunkQueue.remove(packed);
		if (singularityRingPendingChunks > 0) {
			singularityRingPendingChunks--;
		}
		if (singularityCollapseCompletedChunks < singularityCollapseTotalChunks) {
			singularityCollapseCompletedChunks++;
		}
		updateCollapseRadiusFromChunk(packed);
		singularityCollapseDescending = !singularityCollapseDescending;
		spawnChunkVeil(world, chunk);
		if (SingularityConfig.debugLogging()) {
			InfectionLog.info(LogChannel.SINGULARITY, "chunk processed {} pending={}", chunk, singularityRingPendingChunks);
		}
		singularityDestructionEngine.removeChunkTasks(chunk);
	}

	private void drainFluidsAhead(ServerWorld world, SingularityDestructionEngine.ColumnTask task, int baseY) {
		int offset = SingularityConfig.waterDrainOffset();
		if (offset <= 0) {
			return;
		}
		ChunkPos chunk = task.chunk();
		int minY = baseY + 1;
		int maxY = Math.min(task.maxY(), baseY + offset);
		if (maxY <= baseY) {
			return;
		}
		BlockBox slice = new BlockBox(chunk.getStartX(),
				minY,
				chunk.getStartZ(),
				chunk.getEndX(),
				maxY,
				chunk.getEndZ());
		SingularityConfig.FillShape shape = SingularityConfig.bulkFillShape();
		boolean canUseNativeFill = shape == SingularityConfig.FillShape.MATRIX;
		if (canUseNativeFill && BulkFillHelper.clearFluidRange(world, slice, "minecraft:water")) {
			return;
		}
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int y = minY; y <= maxY; y++) {
			for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
				for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
					if (!BulkFillHelper.shouldFillShape(x,
							y,
							z,
							slice.getMinX(),
							slice.getMinY(),
							slice.getMinZ(),
							slice.getMaxX(),
							slice.getMaxY(),
							slice.getMaxZ(),
							shape)) {
						continue;
					}
					mutable.set(x, y, z);
					BlockState state;
					try {
						state = world.getBlockState(mutable);
					} catch (IllegalStateException ex) {
						SingularityChunkContext.recordSkippedMissing();
						return;
					}
					if (state.isAir()
							|| state.isOf(ModBlocks.VIRUS_BLOCK)
							|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
							|| state.getFluidState().isEmpty()) {
						continue;
					}
					try {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
						SingularityChunkContext.recordWaterCleared();
					} catch (IllegalStateException ex) {
						SingularityChunkContext.recordSkippedMissing();
						return;
					}
				}
			}
		}
	}

	private int layerBaseY(SingularityDestructionEngine.ColumnTask task) {
		return MathHelper.clamp(task.currentY(), task.minY(), task.maxY());
	}

	private void spawnChunkVeil(ServerWorld world, ChunkPos chunk) {
		if (singularityCenter == null) {
			return;
		}
		double centerX = chunk.getCenterX();
		double centerZ = chunk.getCenterZ();
		double centerY = singularityCenter.getY() + world.random.nextGaussian() * 2.0D;
		for (int i = 0; i < 8; i++) {
			double angle = (Math.PI * 2 * i) / 8.0D + world.random.nextDouble() * 0.15D;
			double radius = 8.0D + world.random.nextDouble() * 4.0D;
			double x = centerX + Math.cos(angle) * radius;
			double z = centerZ + Math.sin(angle) * radius;
			double y = centerY + world.random.nextGaussian() * 1.5D;
			world.spawnParticles(ParticleTypes.SCULK_SOUL, x, y, z, 3, 0.2D, 0.2D, 0.2D, 0.01D);
			world.spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y + 0.8D, z, 2, 0.05D, 0.1D, 0.05D, 0.05D);
		}
		world.playSound(null,
				centerX,
				centerY,
				centerZ,
				SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
				SoundCategory.HOSTILE,
				1.2F,
				0.6F + world.random.nextFloat() * 0.3F);
	}

	private void updateCollapseRadiusFromChunk(long chunkLong) {
		singularityCollapseRadius = (int) Math.max(0.0D, Math.round(chunkDistanceFromCenter(chunkLong)));
	}

	private double chunkDistanceFromCenter(long chunkLong) {
		if (singularityCenter == null) {
			return 0.0D;
		}
		ChunkPos center = new ChunkPos(singularityCenter);
		ChunkPos current = new ChunkPos(chunkLong);
		double dx = (current.x - center.x) * 16.0D;
		double dz = (current.z - center.z) * 16.0D;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private double getCurrentCollapseRadius(ServerWorld world) {
		return Math.max(0.0D, world.getWorldBorder().getSize() * 0.5D);
	}

	private static double roundDistance(double value) {
		return Math.round(value * 100.0D) / 100.0D;
	}

	private void spawnCollapseParticles(ServerWorld world, BlockPos pos) {
		if (!SingularityConfig.collapseParticles()) {
			return;
		}
		world.spawnParticles(
				ParticleTypes.ASH,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				SINGULARITY_COLLAPSE_PARTICLE_DENSITY,
				0.25D,
				0.25D,
				0.25D,
				0.01D);
		if (world.random.nextInt(20) == 0) {
			world.playSound(null, pos, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.HOSTILE, 0.8F, 0.4F + world.random.nextFloat() * 0.2F);
		}
	}

	private void carveColumn(ServerWorld world, BlockPos target, int startY, int endY, int step) {
		long chunkLong = ChunkPos.toLong(target);
		if (!world.isChunkLoaded(chunkLong)) {
			return;
		}
		BlockPos.Mutable pos = new BlockPos.Mutable(target.getX(), startY, target.getZ());
		for (int y = startY; step > 0 ? y <= endY : y >= endY; y += step) {
			pos.setY(y);
			BlockState state = world.getBlockState(pos);
			if (state.isAir()
					|| state.isOf(ModBlocks.VIRUS_BLOCK)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
					|| state.getHardness(world, pos) < 0.0F) {
				continue;
			}
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			spawnCollapseParticles(world, pos);
		}
	}

	private void advanceRingIndexIfNeeded(ServerWorld world) {
		if (!singularityBorderActive || singularityRingThresholds.isEmpty()) {
			return;
		}
		double currentRadius = getCurrentCollapseRadius(world);
		if (singularityRingIndex < 0) {
			if (currentRadius > singularityRingThresholds.getDouble(0)) {
				return;
			}
			singularityRingIndex = 0;
			if (!singularityRingChunkCounts.isEmpty()) {
				int chunks = singularityRingChunkCounts.getInt(0);
				singularityRingPendingChunks += chunks;
				singularityDestructionEngine.activateRing(0,
						singularityCollapseDescending,
						SingularityConfig.useRingSliceMode());
				logRingActivation(world, 0, currentRadius);
				updateCollapseThroughput(world);
			}
			return;
		}
		while (singularityRingIndex + 1 < singularityRingThresholds.size()
				&& currentRadius <= singularityRingThresholds.getDouble(singularityRingIndex + 1)) {
			singularityRingIndex++;
			if (singularityRingIndex < singularityRingChunkCounts.size()) {
				int chunks = singularityRingChunkCounts.getInt(singularityRingIndex);
				singularityRingPendingChunks += chunks;
				singularityDestructionEngine.activateRing(singularityRingIndex,
						singularityCollapseDescending,
						SingularityConfig.useRingSliceMode());
				logRingActivation(world, singularityRingIndex, currentRadius);
				updateCollapseThroughput(world);
			}
		}
	}

	private boolean processCollapseWithScheduler(ServerWorld world) {
		SingularityCollapseScheduler scheduler = ensureCollapseScheduler();
		int budget = singularityDestructionEngine.getActiveColumnsPerTick();
		boolean submitted = false;
		for (int i = 0; i < budget; i++) {
			SingularityDestructionEngine.ColumnTask task = singularityDestructionEngine.pollColumn();
			if (task == null) {
				break;
			}
			SingularityChunkContext.recordColumnProcessed();
			if (submitColumnTask(world, scheduler, task)) {
				submitted = true;
			}
		}
		boolean applied = scheduler.flush(this, world, singularityDestructionEngine);
		return submitted || applied || scheduler.hasPendingResults();
	}

	private boolean submitColumnTask(ServerWorld world, SingularityCollapseScheduler scheduler, SingularityDestructionEngine.ColumnTask task) {
		long chunkLong = task.chunk().toLong();
		boolean outsideAllowed = SingularityConfig.allowOutsideBorderLoad()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD);
		if (!outsideAllowed) {
			WorldBorder border = world.getWorldBorder();
			double chunkCenterX = task.chunk().getCenterX();
			double chunkCenterZ = task.chunk().getCenterZ();
			if (!border.contains(chunkCenterX, chunkCenterZ)) {
				double borderRadius = border.getSize() * 0.5D;
				double chunkDistance = chunkDistanceFromCenter(chunkLong);
				SingularityChunkContext.sampleBorderRejectedChunk(task.chunk(), chunkDistance, borderRadius);
				SingularityChunkContext.recordSkippedBorder();
				markCollapseChunkComplete(world, task.chunk());
				return true;
			}
		}

		boolean chunkLoaded = world.isChunkLoaded(chunkLong);
		boolean generationAllowed = SingularityConfig.allowChunkGeneration()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION);
		ServerChunkManager chunkManager = world.getChunkManager();
		if (!chunkLoaded && generationAllowed) {
			SingularityChunkContext.pushChunkBypass(task.chunk());
			try {
				Chunk chunk = chunkManager.getChunk(task.chunk().x, task.chunk().z, ChunkStatus.FULL, true);
				if (chunk != null) {
					pinSingularityChunk(world, task.chunk());
					singularityDestructionEngine.requeueColumn(task.withSticky(true));
					return true;
				}
			} catch (IllegalStateException ex) {
				logChunkLoadException(world, "scheduler", task.chunk(), generationAllowed, ex);
				SingularityChunkContext.recordSkippedMissing();
			} finally {
				SingularityChunkContext.popChunkBypass(task.chunk());
			}
		}
		if (!chunkLoaded) {
			if (shouldAwaitMissingChunks()) {
				singularityDestructionEngine.requeueColumn(task.withSticky(true));
				return false;
			}
			SingularityChunkContext.sampleMissingChunk(task.chunk(), chunkDistanceFromCenter(chunkLong), generationAllowed);
			SingularityChunkContext.recordSkippedMissing();
			markCollapseChunkComplete(world, task.chunk());
			return true;
		}

		scheduler.submit(world, task);
		return true;
	}

	public void applyWorkerResult(ServerWorld world,
			SingularityDestructionEngine engine,
			ColumnWorkResult result) {
		ColumnWorkResult.FailureReason failure = result.failureReason();
		if (failure != null) {
			if (failure == ColumnWorkResult.FailureReason.MISSING_CHUNK) {
				if (shouldAwaitMissingChunks()) {
					singularityDestructionEngine.requeueColumn(result.task().withSticky(false));
					return;
				}
				SingularityChunkContext.recordSkippedMissing();
				markCollapseChunkComplete(world, result.task().chunk());
			} else if (failure == ColumnWorkResult.FailureReason.OUTSIDE_BORDER) {
				SingularityChunkContext.sampleBorderRejectedChunk(result.task().chunk(),
						chunkDistanceFromCenter(result.task().chunk().toLong()),
						result.borderRadius());
				SingularityChunkContext.recordSkippedBorder();
				markCollapseChunkComplete(world, result.task().chunk());
			}
			return;
		}

		int drainedApplied = 0;
		for (BlockPos drained : result.drainedBlocks()) {
			if (trySetAir(world, drained, false)) {
				drainedApplied++;
				SingularityChunkContext.recordWaterCleared();
			}
		}
		int clearedApplied = 0;
		for (BlockPos cleared : result.clearedBlocks()) {
			if (tryClearCollapseBlock(world, cleared, true)) {
				spawnCollapseParticles(world, cleared);
				clearedApplied++;
			}
		}
		if (clearedApplied > 0) {
			SingularityChunkContext.recordBlocksCleared(clearedApplied);
		}
		if (SingularityConfig.debugLogging() && (clearedApplied > 0 || drainedApplied > 0 || result.columnComplete())) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[workerResult] chunk={} cleared={} drained={} nextY={} complete={} chunkMode={}",
					result.task().chunk(),
					clearedApplied,
					drainedApplied,
					result.nextY(),
					result.columnComplete(),
					result.chunkMode());
		}

		if (result.columnComplete()) {
			markCollapseChunkComplete(world, result.task().chunk());
			singularityDestructionEngine.onColumnComplete(result.task());
			return;
		}

		// No requeue on ring slice modes; next depth tasks are already present.
	}

	private boolean trySetAir(ServerWorld world, BlockPos pos, boolean respectImmunity) {
		BlockState state;
		try {
			state = world.getBlockState(pos);
		} catch (IllegalStateException ex) {
			return false;
		}
		if (state.isAir()) {
			return false;
		}
		if (respectImmunity) {
			if (state.isOf(ModBlocks.VIRUS_BLOCK)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
					|| state.getHardness(world, pos) < 0.0F) {
				return false;
			}
		}
		try {
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			return true;
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	private boolean tryClearCollapseBlock(ServerWorld world, BlockPos pos, boolean respectImmunity) {
		if (SingularityConfig.bulkFillMode() == SingularityConfig.CollapseFillMode.DESTROY) {
			BlockState state;
			try {
				state = world.getBlockState(pos);
			} catch (IllegalStateException ex) {
				return false;
			}
			if (state.isAir()) {
				return false;
			}
			if (respectImmunity) {
				if (state.isOf(ModBlocks.VIRUS_BLOCK)
						|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
						|| state.getHardness(world, pos) < 0.0F) {
					return false;
				}
			}
			try {
				world.breakBlock(pos, false);
				return true;
			} catch (IllegalStateException ex) {
				return false;
			}
		}
		return trySetAir(world, pos, respectImmunity);
	}

	private boolean shouldAwaitMissingChunks() {
		return !singularityPreloadComplete;
	}

	private SingularityCollapseScheduler ensureCollapseScheduler() {
		if (collapseScheduler == null) {
			collapseScheduler = new SingularityCollapseScheduler(() -> singularityCenter);
		}
		return collapseScheduler;
	}

	private void shutdownCollapseScheduler() {
		if (collapseScheduler != null) {
			collapseScheduler.close();
			collapseScheduler = null;
		}
	}

	private void pinSingularityChunk(ServerWorld world, ChunkPos pos) {
		long packed = pos.toLong();
		if (singularityPinnedChunks.add(packed)) {
			try {
				world.setChunkForced(pos.x, pos.z, true);
				if (SingularityConfig.debugLogging()) {
					InfectionLog.info(LogChannel.SINGULARITY, "pinned chunk {}", pos);
				}
			} catch (IllegalStateException ex) {
				singularityPinnedChunks.remove(packed);
				if (SingularityConfig.debugLogging()) {
					TheVirusBlock.LOGGER.warn("[Singularity] failed to pin chunk {} ({})", pos, ex.getMessage());
				}
			}
		}
	}

	private void releaseSingularityChunkTickets(ServerWorld world) {
		if (singularityPinnedChunks.isEmpty()) {
			return;
		}
		int released = singularityPinnedChunks.size();
		for (long packed : singularityPinnedChunks.toLongArray()) {
			ChunkPos pos = new ChunkPos(packed);
			try {
				world.setChunkForced(pos.x, pos.z, false);
			} catch (IllegalStateException ex) {
				if (SingularityConfig.debugLogging()) {
					TheVirusBlock.LOGGER.warn("[Singularity] failed to unpin chunk {} ({})", pos, ex.getMessage());
				}
			}
		}
		singularityPinnedChunks.clear();
		InfectionLog.info(LogChannel.SINGULARITY, "[unload] released {} forced chunks", released);
	}

	private boolean areAllCollapseChunksPinned(List<Long> queueSnapshot) {
		if (queueSnapshot.isEmpty()) {
			return true;
		}
		if (singularityPinnedChunks.isEmpty() || singularityPinnedChunks.size() < queueSnapshot.size()) {
			return false;
		}
		for (Long packed : queueSnapshot) {
			if (packed == null) {
				continue;
			}
			if (!singularityPinnedChunks.contains(packed.longValue())) {
				return false;
			}
		}
		return true;
	}

	private RingSliceStatus processRingSliceSingleThread(ServerWorld world, SingularityDestructionEngine.ColumnTask task) {
		if (singularityCenter == null) {
			return RingSliceStatus.COMPLETE;
		}
		int depth = task.ringSliceDepth();
		if (depth < 0) {
			return RingSliceStatus.COMPLETE;
		}
		ChunkPos chunkPos = task.chunk();
		ServerChunkManager chunkManager = world.getChunkManager();
		boolean chunkAvailable;
		try {
			chunkAvailable = chunkManager.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) != null;
		} catch (IllegalStateException ex) {
			chunkAvailable = false;
		}
		if (!chunkAvailable) {
			SingularityChunkContext.recordSkippedMissing();
			if (SingularityConfig.debugLogging()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[ringSlice] chunk={} depth={} deferred reason=missingChunk",
						chunkPos,
						depth);
			}
			return RingSliceStatus.RETRY;
		}
		SingularityRingSlices.SliceFacing facing = SingularityRingSlices.resolve(chunkPos, singularityCenter);
		BlockBox sliceBox = SingularityRingSlices.boundsForSlice(chunkPos,
				facing,
				depth,
				task.minY(),
				task.maxY());
		if (sliceBox != null
				&& SingularityConfig.bulkFillShape() == SingularityConfig.FillShape.OUTLINE
				&& SingularityConfig.outlineThickness() > 1) {
			sliceBox = SingularityRingSlices.expandForOutline(sliceBox, chunkPos, facing, SingularityConfig.outlineThickness());
		}
		if (sliceBox == null) {
			return depth >= SingularityRingSlices.SLICE_COUNT - 1 ? RingSliceStatus.COMPLETE : RingSliceStatus.CONTINUE;
		}
		boolean respectProtected = SingularityConfig.respectProtectedBlocks();
		CollapseBroadcastDecision broadcastDecision = evaluateBroadcastDecision(world, chunkPos);
		int clearedBlocks;
		try {
			clearedBlocks = BulkFillHelper.clearVolume(world,
					sliceBox,
					SingularityConfig.bulkFillMode(),
					SingularityConfig.bulkFillShape(),
					respectProtected,
					broadcastDecision.updateFlags());
		} catch (IllegalStateException ex) {
			SingularityChunkContext.recordSkippedMissing();
			SingularityChunkContext.recordChunkBlocked(chunkPos, ex.getMessage());
			return RingSliceStatus.RETRY;
		}
		if (clearedBlocks > 0) {
			SingularityChunkContext.recordBlocksCleared(clearedBlocks);
			if (SingularityConfig.debugLogging() && shouldSampleRemovalLog()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[ringSlice] chunk={} depth={} clearedBlocks={} facing={}",
						chunkPos,
						depth,
						clearedBlocks,
						facing);
			}
		}
		if (!broadcastDecision.notifyPlayers()) {
			markChunkBroadcastBuffered(task.chunk(), broadcastDecision);
		}
		return depth >= SingularityRingSlices.SLICE_COUNT - 1 ? RingSliceStatus.COMPLETE : RingSliceStatus.CONTINUE;
	}

	private boolean processChunkFillSingleThread(ServerWorld world, SingularityDestructionEngine.ColumnTask task) {
		ChunkPos chunkPos = task.chunk();
		BlockBox chunkBox = new BlockBox(chunkPos.getStartX(),
				task.minY(),
				chunkPos.getStartZ(),
				chunkPos.getEndX(),
				task.maxY(),
				chunkPos.getEndZ());
		boolean respectProtected = SingularityConfig.respectProtectedBlocks();
		CollapseBroadcastDecision broadcastDecision = evaluateBroadcastDecision(world, chunkPos);
		int clearedBlocks;
		try {
			clearedBlocks = BulkFillHelper.clearVolume(world,
					chunkBox,
					SingularityConfig.bulkFillMode(),
					SingularityConfig.bulkFillShape(),
					respectProtected,
					broadcastDecision.updateFlags());
		} catch (IllegalStateException ex) {
			SingularityChunkContext.recordSkippedMissing();
			SingularityChunkContext.recordChunkBlocked(chunkPos, ex.getMessage());
			return false;
		}
		if (clearedBlocks > 0) {
			SingularityChunkContext.recordBlocksCleared(clearedBlocks);
			if (SingularityConfig.debugLogging() && shouldSampleRemovalLog()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[ringChunk] chunk={} clearedBlocks={}",
						chunkPos,
						clearedBlocks);
			}
		}
		if (!broadcastDecision.notifyPlayers()) {
			markChunkBroadcastBuffered(task.chunk(), broadcastDecision);
		}
		return true;
	}

	private void logSingularityDebugInfo(ServerWorld world) {
		if (!SingularityConfig.debugLogging() || singularityCenter == null || singularityState != SingularityState.COLLAPSE) {
			return;
		}
		if (--singularityDebugLogCooldown > 0) {
			return;
		}
		singularityDebugLogCooldown = SINGULARITY_DEBUG_LOG_INTERVAL;

		double borderRadius = getCurrentCollapseRadius(world);
		double centerX = singularityCenter.getX() + 0.5D;
		double centerZ = singularityCenter.getZ() + 0.5D;
		double minX = centerX - borderRadius;
		double maxX = centerX + borderRadius;
		double minZ = centerZ - borderRadius;
		double maxZ = centerZ + borderRadius;

		List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && !player.isSpectator());
		StringBuilder playerInfo = new StringBuilder();
		for (ServerPlayerEntity player : players) {
			if (playerInfo.length() > 0) {
				playerInfo.append("; ");
			}
			Vec3d pos = player.getPos();
			playerInfo.append(player.getName().getString())
					.append("=")
					.append(roundDistance(pos.x))
					.append(",")
					.append(roundDistance(pos.y))
					.append(",")
					.append(roundDistance(pos.z));
		}
		if (playerInfo.length() == 0) {
			playerInfo.append("<none>");
		}

		int activeColumns = singularityDestructionEngine != null ? singularityDestructionEngine.getActiveColumnsPerTick() : 0;

		InfectionLog.info(LogChannel.SINGULARITY, "players {}", playerInfo);
		InfectionLog.info(LogChannel.SINGULARITY, "border center=({}, {}) radius={} box=[{}..{}]x[{}..{}] ring={} pending={} columns={} layers={}",
				roundDistance(centerX),
				roundDistance(centerZ),
				roundDistance(borderRadius),
				roundDistance(minX),
				roundDistance(maxX),
				roundDistance(minZ),
				roundDistance(maxZ),
				singularityRingIndex,
				singularityRingPendingChunks,
				activeColumns,
				singularityLayersPerSlice);
	}

	private void logSingularityStall(ServerWorld world) {
		if (!SingularityConfig.debugLogging()
				|| singularityState != SingularityState.COLLAPSE
				|| singularityRingPendingChunks <= 0) {
			return;
		}
		singularityCollapseStallTicks++;
		handleCollapseWatchdog(world);
		if (singularityCollapseStallTicks % 20 != 0) {
			return;
		}
		int enginePending = singularityDestructionEngine != null ? singularityDestructionEngine.getPendingColumns() : 0;
		boolean schedulerPending = collapseScheduler != null && collapseScheduler.hasPendingResults();
		InfectionLog.info(LogChannel.SINGULARITY,
				"[collapse] stalled ticks={} ringPending={} enginePending={} schedulerPending={} preloadComplete={} chunkQueue={} cooldown={}",
				singularityCollapseStallTicks,
				singularityRingPendingChunks,
				enginePending,
				schedulerPending,
				singularityPreloadComplete,
				singularityChunkQueue.size(),
				singularityCollapseTickCooldown);
	}

	private void logSingularityStateChange(SingularityState next, String reason) {
		if (!SingularityConfig.debugLogging()) {
			singularityLastLoggedState = next;
			if (next != SingularityState.FUSING) {
				singularityFuseWatchdogTicks = 0;
			}
			if (next != SingularityState.COLLAPSE) {
				singularityCollapseStallTicks = 0;
			}
			return;
		}
		if (singularityLastLoggedState == next && singularityState == next) {
			return;
		}
		InfectionLog.info(LogChannel.SINGULARITY,
				"[state] {} -> {} reason={}",
				singularityState,
				next,
				reason == null ? "<none>" : reason);
		singularityLastLoggedState = next;
		if (next != SingularityState.FUSING) {
			singularityFuseWatchdogTicks = 0;
		}
		if (next != SingularityState.COLLAPSE) {
			singularityCollapseStallTicks = 0;
		}
	}

	private void tickFuseWatchdog(ServerWorld world) {
		if (!SingularityConfig.collapseWatchdogEnabled()) {
			singularityFuseWatchdogTicks = 0;
			return;
		}
		if (singularityState != SingularityState.FUSING || !singularityPreloadComplete) {
			singularityFuseWatchdogTicks = 0;
			return;
		}
		singularityFuseWatchdogTicks++;
		int limit = SingularityConfig.watchdogFuseMaxExtraTicks();
		if (limit > 0 && singularityFuseWatchdogTicks >= limit) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[watchdog] fuse stalled ticks={} preloadComplete={} collapseDelay={} autoSkip={}",
					singularityFuseWatchdogTicks,
					singularityPreloadComplete,
					singularityCollapseBarDelay,
					SingularityConfig.watchdogAutoSkipFuse());
			singularityFuseWatchdogTicks = 0;
			if (SingularityConfig.watchdogAutoSkipFuse()) {
				skipSingularityCollapse(world, "fuse watchdog exceeded " + limit + " ticks");
			}
		}
	}

	private void handleCollapseWatchdog(ServerWorld world) {
		if (!SingularityConfig.collapseWatchdogEnabled()) {
			return;
		}
		int warn = SingularityConfig.watchdogCollapseWarnTicks();
		int abort = SingularityConfig.watchdogCollapseAbortTicks();
		if (warn > 0 && singularityCollapseStallTicks == warn) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[watchdog] collapse stalled warn ringPending={} queue={} schedulerPending={}",
					singularityRingPendingChunks,
					singularityChunkQueue.size(),
					collapseScheduler != null && collapseScheduler.hasPendingResults());
		}
		if (abort > 0 && singularityCollapseStallTicks >= abort) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[watchdog] collapse abort ringPending={} queue={} autoSkip={}",
					singularityRingPendingChunks,
					singularityChunkQueue.size(),
					SingularityConfig.watchdogAutoSkipCollapse());
			singularityCollapseStallTicks = 0;
			if (SingularityConfig.watchdogAutoSkipCollapse()) {
				skipSingularityCollapse(world, "collapse watchdog exceeded " + abort + " ticks");
			}
		}
	}

	private enum RingSliceStatus {
		RETRY,
		CONTINUE,
		COMPLETE
	}

	private boolean shouldSampleRemovalLog() {
		if (--singularityRemovalLogCooldown > 0) {
			return false;
		}
		singularityRemovalLogCooldown = 20;
		return true;
	}

	private boolean shouldPinCollapseChunks() {
		SingularityConfig.CollapseMode mode = SingularityConfig.collapseMode();
		return mode != SingularityConfig.CollapseMode.RING_SLICE
				&& mode != SingularityConfig.CollapseMode.RING_SLICE_CHUNK;
	}

	private int currentRingSideLength() {
		if (singularityRingRadii.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		int index = singularityRingIndex;
		if (index < 0) {
			index = 0;
		}
		if (index >= singularityRingRadii.size()) {
			index = singularityRingRadii.size() - 1;
		}
		int radius = Math.max(0, singularityRingRadii.getInt(index));
		return radius * 2 + 1;
	}

	private int currentRingChunkCount() {
		if (singularityRingChunkCounts.isEmpty()) {
			return Math.max(1, singularityRingPendingChunks);
		}
		int index = singularityRingIndex;
		if (index < 0) {
			index = 0;
		}
		if (index >= singularityRingChunkCounts.size()) {
			index = singularityRingChunkCounts.size() - 1;
		}
		int count = singularityRingChunkCounts.getInt(index);
		if (count <= 0) {
			count = Math.max(1, singularityRingPendingChunks);
		}
		return Math.max(1, count);
	}

	private int maxSlicesPerTickForCurrentRing() {
		return SingularityRingSlices.SLICE_COUNT;
	}

	private int applyRingSliceSlowdown(int columns) {
		if (!SingularityConfig.useRingSliceMode()) {
			return columns;
		}
		int chunkCount = currentRingChunkCount();
		int sliceCap = maxSlicesPerTickForCurrentRing();
		long cap = (long) chunkCount * sliceCap;
		if (cap > Integer.MAX_VALUE) {
			cap = Integer.MAX_VALUE;
		}
		int capped = (int) cap;
		return Math.max(chunkCount, Math.min(columns, capped));
	}

	private int resolveTickIntervalForCurrentRing() {
		int side = currentRingSideLength();
		for (SingularityConfig.RadiusDelay delay : SingularityConfig.radiusDelays()) {
			if (side <= delay.side) {
				return Math.max(1, delay.ticks);
			}
		}
		return configuredCollapseTickInterval();
	}

	private void updateCollapseThroughput(ServerWorld world) {
		if (singularityDestructionEngine == null) {
			return;
		}
		singularityLayersPerSlice = 1;
		if (singularityRingPendingChunks <= 0 || singularityBorderDuration <= 0) {
			singularityDestructionEngine.setActiveColumnsPerTick(SINGULARITY_COLLAPSE_MIN_COLUMNS_PER_TICK);
			return;
		}
		int pendingColumns = Math.max(1, singularityDestructionEngine.getPendingColumns());
		int columns = Math.max(SINGULARITY_COLLAPSE_MIN_COLUMNS_PER_TICK,
				Math.min(SINGULARITY_COLLAPSE_MAX_COLUMNS_PER_TICK, pendingColumns));
		columns = applyRingSliceSlowdown(columns);
		singularityDestructionEngine.setActiveColumnsPerTick(columns);

		if (SingularityConfig.debugLogging()
				&& (columns != singularityLastColumnsPerTick || singularityLayersPerSlice != singularityLastLayersPerSlice)) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"throughput columns={} layers={} pending={}",
					columns,
					singularityLayersPerSlice,
					singularityRingPendingChunks);
		}
		singularityLastColumnsPerTick = columns;
		singularityLastLayersPerSlice = singularityLayersPerSlice;
	}

	private void logRingActivation(ServerWorld world, int ringIndex, double borderRadius) {
		if (!SingularityConfig.debugLogging()) {
			return;
		}
		if (ringIndex < 0 || ringIndex >= singularityRingThresholds.size()) {
			return;
		}
		double threshold = singularityRingThresholds.getDouble(ringIndex);
		int chunkCount = ringIndex < singularityRingChunkCounts.size() ? singularityRingChunkCounts.getInt(ringIndex) : 0;
		List<ChunkPos> chunks = ringIndex < singularityRingChunks.size() ? singularityRingChunks.get(ringIndex) : Collections.emptyList();
		double minDist = Double.POSITIVE_INFINITY;
		double maxDist = 0.0D;
		for (ChunkPos chunk : chunks) {
			double distance = chunkDistanceFromCenter(chunk.toLong());
			minDist = Math.min(minDist, distance);
			maxDist = Math.max(maxDist, distance);
		}
		if (chunks.isEmpty()) {
			minDist = 0.0D;
			maxDist = 0.0D;
		}
		InfectionLog.info(LogChannel.SINGULARITY,
				"activating ring {} chunks={} threshold={} borderRadius={} chunkDist={}..{}",
				ringIndex,
				chunkCount,
				roundDistance(threshold),
				roundDistance(borderRadius),
				roundDistance(minDist),
				roundDistance(maxDist));
	}

	private void processSingularityCore(ServerWorld world) {
		if (singularityCenter == null) {
			return;
		}
		if (singularityPhaseDelay > 0) {
			singularityPhaseDelay--;
			world.spawnParticles(ParticleTypes.ENCHANT,
					singularityCenter.getX() + 0.5D,
					singularityCenter.getY() + 0.5D,
					singularityCenter.getZ() + 0.5D,
					6,
					0.3D,
					0.3D,
					0.3D,
					0.02D);
			if (singularityPhaseDelay % 10 == 0) {
				world.playSound(null,
						singularityCenter.getX() + 0.5D,
						singularityCenter.getY() + 0.5D,
						singularityCenter.getZ() + 0.5D,
						SoundEvents.BLOCK_BEACON_AMBIENT,
						SoundCategory.HOSTILE,
						1.5F,
						0.6F);
			}
			markDirty();
			return;
		}
		world.playSound(null, singularityCenter, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 4.0F, 0.5F);
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
				singularityCenter.getX() + 0.5D,
				singularityCenter.getY() + 0.5D,
				singularityCenter.getZ() + 0.5D,
				2,
				0.2D,
				0.2D,
				0.2D,
				0.01D);
		pushPlayersFromBlock(world, singularityCenter, 12, 2.0D, false);
		world.createExplosion(null, null, null, singularityCenter.getX() + 0.5D, singularityCenter.getY() + 0.5D, singularityCenter.getZ() + 0.5D, 10.0F, false, ExplosionSourceType.TNT);
		logSingularityStateChange(SingularityState.RING, "core detonation");
		singularityState = SingularityState.RING;
		singularityPhaseDelay = SINGULARITY_RING_START_DELAY;
		syncSingularityBorder(world);
		markDirty();
	}

	private void processSingularityRing(ServerWorld world) {
		if (singularityCenter == null) {
			logSingularityStateChange(SingularityState.DISSIPATION, "ring center missing");
			singularityState = SingularityState.DISSIPATION;
			syncSingularityBorder(world);
			markDirty();
			return;
		}
		if (singularityPhaseDelay > 0) {
			singularityPhaseDelay--;
			world.spawnParticles(ParticleTypes.SMALL_FLAME,
					singularityCenter.getX() + 0.5D,
					singularityCenter.getY() + 0.5D,
					singularityCenter.getZ() + 0.5D,
					4,
					0.2D,
					0.2D,
					0.2D,
					0.01D);
			markDirty();
			return;
		}
		if (singularityRingTicks <= 0) {
			logSingularityStateChange(SingularityState.DISSIPATION, "ring finished");
			singularityState = SingularityState.DISSIPATION;
			singularityPhaseDelay = SINGULARITY_RESET_DELAY_TICKS;
			syncSingularityBorder(world);
			markDirty();
			return;
		}
		singularityRingTicks--;
		double radius = 8.0D + (SINGULARITY_RING_DURATION - singularityRingTicks) * 0.25D;
		int segments = 48;
		for (int i = 0; i < segments; i++) {
			double angle = (Math.PI * 2 * i) / segments;
			double x = singularityCenter.getX() + 0.5D + Math.cos(angle) * radius;
			double z = singularityCenter.getZ() + 0.5D + Math.sin(angle) * radius;
			double y = singularityCenter.getY() + Math.sin(world.random.nextDouble() * Math.PI) * 2.0D;
			world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			BlockStateParticleEffect debris = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DEEPSLATE.getDefaultState());
			world.spawnParticles(debris, x, y + 1.5D, z, 1, 0.0D, 0.1D, 0.0D, 0.02D);
		}
		for (int i = 0; i < SINGULARITY_RING_COLUMNS_PER_TICK; i++) {
			double angle = world.random.nextDouble() * Math.PI * 2;
			double columnRadius = radius + world.random.nextBetween(-2, 2);
			double x = singularityCenter.getX() + 0.5D + Math.cos(angle) * columnRadius;
			double z = singularityCenter.getZ() + 0.5D + Math.sin(angle) * columnRadius;
			BlockPos destroy = BlockPos.ofFloored(x, singularityCenter.getY(), z);
			int minY = world.getBottomY();
			int maxY = world.getBottomY() + world.getDimension().height() - 1;
			boolean fromBottom = world.random.nextBoolean();
			carveColumn(world, destroy, fromBottom ? minY : maxY, fromBottom ? maxY : minY, fromBottom ? 1 : -1);
		}
		if (singularityRingTicks % 20 == 0) {
			world.playSound(null,
					singularityCenter.getX() + 0.5D,
					singularityCenter.getY() + 0.5D,
					singularityCenter.getZ() + 0.5D,
					SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(),
					SoundCategory.AMBIENT);
		}
		if (singularityRingTicks % 10 == 0) {
			world.playSound(null,
					singularityCenter.getX() + 0.5D,
					singularityCenter.getY() + 0.5D,
					singularityCenter.getZ() + 0.5D,
					SoundEvents.BLOCK_END_PORTAL_SPAWN,
					SoundCategory.HOSTILE,
					1.1F,
					0.5F + world.random.nextFloat() * 0.2F);
		}
		pullEntitiesTowardRing(world, radius + SINGULARITY_RING_PULL_RADIUS);
	}

	private void processSingularityDissipation(ServerWorld world) {
		if (singularityCenter == null) {
			clearFuseEntities(world);
			resetSingularityState(world);
			markDirty();
			return;
		}
		world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
				singularityCenter.getX() + 0.5D,
				singularityCenter.getY() + 0.5D,
				singularityCenter.getZ() + 0.5D,
				16,
				2.0D,
				2.0D,
				2.0D,
				0.05D);
		if (world.random.nextInt(5) == 0) {
			world.playSound(null,
					singularityCenter.getX() + 0.5D,
					singularityCenter.getY() + 0.5D,
					singularityCenter.getZ() + 0.5D,
					SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
					SoundCategory.HOSTILE);
		}
		if (singularityPhaseDelay > 0) {
			singularityPhaseDelay--;
			markDirty();
			return;
		}
		if (shouldStartPostReset()) {
			startPostCollapseReset(world);
			return;
		}
		finishSingularity(world);
	}

	private boolean shouldStartPostReset() {
		return SingularityConfig.postResetEnabled()
				&& !singularityResetQueue.isEmpty();
	}

	private void startPostCollapseReset(ServerWorld world) {
		if (singularityResetQueue.isEmpty() || !SingularityConfig.postResetEnabled()) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[reset] skipped enabled={} queue={}",
					SingularityConfig.postResetEnabled(),
					singularityResetQueue.size());
			finishSingularity(world);
			return;
		}
		singularityState = SingularityState.RESET;
		singularityResetDelay = (int) Math.max(0L, SingularityConfig.postResetDelayTicks());
		singularityResetProcessed.clear();
		InfectionLog.info(LogChannel.SINGULARITY,
				"[reset] init queue={} delay={} chunksPerTick={} batchRadius={} tickDelay={}",
				singularityResetQueue.size(),
				singularityResetDelay,
				SingularityConfig.postResetChunksPerTick(),
				SingularityConfig.postResetBatchRadius(),
				SingularityConfig.postResetTickDelay());
		broadcast(world, Text.literal("Singularity remnant reset initializing...").formatted(Formatting.LIGHT_PURPLE));
		markDirty();
	}

	private void processSingularityReset(ServerWorld world) {
		if (!SingularityConfig.postResetEnabled()) {
			finishSingularity(world);
			return;
		}
		if (singularityResetDelay > 0) {
			singularityResetDelay--;
			markDirty();
			return;
		}
		singularityResetDelay = Math.max(1, SingularityConfig.postResetTickDelay());
		int perTick = Math.max(1, SingularityConfig.postResetChunksPerTick());
		int processed = 0;
		while (processed < perTick && !singularityResetQueue.isEmpty()) {
			Long chunkKey = singularityResetQueue.pollFirst();
			if (chunkKey == null) {
				continue;
			}
			if (singularityResetProcessed.contains(chunkKey.longValue())) {
				continue;
			}
			resetChunkBatch(world, new ChunkPos(chunkKey));
			processed++;
		}
		if (singularityResetQueue.isEmpty()) {
			InfectionLog.info(LogChannel.SINGULARITY,
					"[reset] complete processed={} queue=0",
					singularityResetProcessed.size());
			finishSingularity(world);
		} else if (processed > 0) {
			if (SingularityConfig.debugLogging()) {
				InfectionLog.info(LogChannel.SINGULARITY,
						"[reset] processed={} remaining={}",
						processed,
						singularityResetQueue.size());
			}
			markDirty();
		} else {
			int tickBudget = Math.max(1, SingularityConfig.postResetTickDelay());
			singularityResetDelay = tickBudget;
		}
	}

	private void resetChunkBatch(ServerWorld world, ChunkPos base) {
		int radius = Math.max(0, SingularityConfig.postResetBatchRadius());
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				ChunkPos target = new ChunkPos(base.x + dx, base.z + dz);
				resetSingleChunk(world, target);
			}
		}
	}

	private void resetSingleChunk(ServerWorld world, ChunkPos pos) {
		long key = pos.toLong();
		if (!singularityResetProcessed.add(key)) {
			return;
		}
		MinecraftServer server = world.getServer();
		if (server == null) {
			return;
		}
		try {
			BlockPos origin = new BlockPos(pos.getStartX(), world.getBottomY(), pos.getStartZ());
			ServerCommandSource source = server.getCommandSource()
					.withWorld(world)
					.withPosition(Vec3d.ofCenter(origin))
					.withLevel(4)
					.withSilent();
			String command = String.format(Locale.ROOT, "chunk reset %d %d", pos.x, pos.z);
			server.getCommandManager().executeWithPrefix(source, command);
			if (SingularityConfig.debugLogging()) {
				InfectionLog.info(LogChannel.SINGULARITY, "[reset] chunk {}", pos);
			}
		} catch (Exception ex) {
			TheVirusBlock.LOGGER.warn("[Singularity] chunk reset failed chunk={} reason={}", pos, ex.toString());
		}
	}

	private void pullEntitiesTowardRing(ServerWorld world, double radius) {
		if (singularityCenter == null) {
			return;
		}
		Vec3d center = Vec3d.ofCenter(singularityCenter);
		Box range = new Box(singularityCenter).expand(radius);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, range, Entity::isAlive)) {
			Vec3d delta = center.subtract(living.getPos());
			double distance = delta.length();
			if (distance < 0.1D) {
				continue;
			}
			double modifier = MathHelper.clamp(1.0D - (distance / (radius + 4.0D)), 0.0D, 1.0D);
			if (modifier <= 0.0D) {
				continue;
			}
			Vec3d impulse = delta.normalize().multiply(SINGULARITY_RING_PULL_STRENGTH * modifier);
			living.addVelocity(impulse.x, impulse.y * 0.2D, impulse.z);
			living.velocityDirty = true;
			living.velocityModified = true;
		}
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, range, entity -> !entity.isRemoved())) {
			Vec3d delta = center.subtract(item.getPos());
			double distance = delta.length();
			if (distance < 0.1D) {
				continue;
			}
			double modifier = MathHelper.clamp(1.0D - (distance / (radius + 4.0D)), 0.0D, 1.0D);
			if (modifier <= 0.0D) {
				continue;
			}
			Vec3d impulse = delta.normalize().multiply(SINGULARITY_RING_PULL_STRENGTH * 0.5D * modifier);
			item.addVelocity(impulse.x, impulse.y * 0.1D, impulse.z);
			item.velocityDirty = true;
		}
	}

	private void finishSingularity(ServerWorld world) {
		clearFuseEntities(world, false);
		revertSingularityBlock(world, true);
		flushBufferedChunks(world, true);
		collapseBufferedChunks.clear();
		resetSingularityState(world);
		broadcast(world, Text.translatable("message.the-virus-block.singularity_dissipated").formatted(Formatting.BLUE));
		forceContainmentReset(world);
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> {
			player.sendMessage(Text.translatable("message.the-virus-block.cleansed").formatted(Formatting.AQUA), false);
		});
		markDirty();
	}

	public boolean forceStartSingularity(ServerWorld world, int seconds, BlockPos fallbackCenter) {
		ensureSingularityGamerules(world);
		restoreSingularityBorder(world);
		if (!infected) {
			ensureDebugInfection(world, fallbackCenter);
		}
		if (virusSources.isEmpty()) {
			virusSources.add(fallbackCenter.toImmutable());
		}
		int clampedSeconds = MathHelper.clamp(seconds, 5, 120);
		InfectionTier tier = InfectionTier.byIndex(InfectionTier.maxIndex());
		tierIndex = tier.getIndex();
		ticksInTier = getTierDuration(tier);
		apocalypseMode = false;
		finalVulnerabilityBlastTriggered = false;
		logSingularityStateChange(SingularityState.FUSING, "force command seconds=" + clampedSeconds);
		singularityState = SingularityState.FUSING;
		singularityTicks = Math.min(configuredFuseExplosionDelayTicks(), clampedSeconds * 20L);
		singularityShellCollapsed = false;
		singularityCollapseRadius = 0;
		singularityCollapseDescending = true;
		singularityCollapseTotalChunks = 0;
		singularityCollapseCompletedChunks = 0;
		singularityCollapseBarDelay = 0;
		singularityRingTicks = 0;
		singularityPhaseDelay = 0;
		singularityFusePulseTicker = 0;
		singularityFuseElapsed = 0;
		resetSingularityPreparation("forceStart");
		if (isSingularityCollapseEnabled(world)) {
			prepareSingularityChunkQueue(world);
		} else {
			singularityChunkQueue.clear();
		}
		if (singularityCenter == null || !world.isChunkLoaded(ChunkPos.toLong(singularityCenter))) {
			singularityCenter = representativePos(world, world.getRandom());
		}
		if (singularityCenter == null) {
			singularityCenter = fallbackCenter.toImmutable();
		}
		maintainFuseEntities(world);
		applyCollapseDistanceOverrides(world);
		markDirty();
		broadcast(world, Text.translatable("message.the-virus-block.singularity_forced").formatted(Formatting.LIGHT_PURPLE));
		return true;
	}

	public boolean abortSingularity(ServerWorld world) {
		if (singularityState == SingularityState.DORMANT) {
			return false;
		}
		clearFuseEntities(world);
		revertSingularityBlock(world, false);
		resetSingularityState(world);
		markDirty();
		broadcast(world, Text.translatable("message.the-virus-block.singularity_aborted").formatted(Formatting.GRAY));
		return true;
	}

	private void ensureDebugInfection(ServerWorld world, BlockPos center) {
		infected = true;
		dormant = false;
		apocalypseMode = false;
		captureBoobytrapDefaults(world);
		restoreBoobytrapRules(world);
		virusSources.clear();
		virusSources.add(center.toImmutable());
		if (world.isChunkLoaded(ChunkPos.toLong(center)) && world.getBlockState(center).isAir()) {
			world.setBlockState(center, ModBlocks.VIRUS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		fuseClearedBlocks.clear();
		suppressedUnregisters.clear();
		markDirty();
	}

	private void triggerFinalBarrierBlast(ServerWorld world) {
		if (finalVulnerabilityBlastTriggered || virusSources.isEmpty()) {
			return;
		}
		finalVulnerabilityBlastTriggered = true;
		nextTierFiveBarrierPushTick = 0L;
		for (BlockPos source : virusSources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			pushPlayersFromBlock(world, source, FINAL_VULNERABILITY_BLAST_RADIUS, FINAL_VULNERABILITY_BLAST_SCALE, false);
			world.spawnParticles(
					ParticleTypes.SONIC_BOOM,
					source.getX() + 0.5D,
					source.getY() + 1.0D,
					source.getZ() + 0.5D,
					6,
					0.25D,
					0.25D,
					0.25D,
					0.0D);
			world.playSound(null, source, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0F, 0.65F);
		}
		deactivateTierFiveBarrier(world);
	}

	private void deactivateTierFiveBarrier(ServerWorld world) {
		if (!tierFiveBarrierActive) {
			return;
		}
		tierFiveBarrierActive = false;
		broadcast(world, Text.translatable("message.the-virus-block.barrier_offline").formatted(Formatting.DARK_PURPLE));
	}

	private void applyInfectiousContactDamage(ServerWorld world) {
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
			if (threshold > 0 && ticks == threshold) {
				player.sendMessage(Text.translatable("message.the-virus-block.infectious_contact_warning"), true);
			}
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
		ItemStack infectiousStack = ModBlocks.INFECTIOUS_CUBE.asItem().getDefaultStack();
		Set<UUID> retained = new HashSet<>();
		long now = world.getTime();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			UUID uuid = player.getUuid();
			if (player.isSpectator() || player.isCreative()
					|| player.getInventory().count(infectiousStack.getItem()) <= 0) {
				infectiousInventoryTicks.removeInt(uuid);
				infectiousInventoryWarnCooldowns.removeLong(uuid);
				continue;
			}
			retained.add(uuid);
			int ticks = infectiousInventoryTicks.getOrDefault(uuid, 0) + 1;
			infectiousInventoryTicks.put(uuid, ticks);
			if (INFECTIOUS_INVENTORY_THRESHOLD > 0 && ticks == INFECTIOUS_INVENTORY_THRESHOLD) {
				long nextAllowed = infectiousInventoryWarnCooldowns.getOrDefault(uuid, 0L);
				if (now >= nextAllowed) {
					player.sendMessage(Text.translatable("message.the-virus-block.infectious_inventory_warning"), true);
					infectiousInventoryWarnCooldowns.put(uuid, now + INFECTIOUS_INVENTORY_WARNING_COOLDOWN);
				}
			}
			if (ticks > INFECTIOUS_INVENTORY_THRESHOLD
					&& (ticks - INFECTIOUS_INVENTORY_THRESHOLD) % INFECTIOUS_INVENTORY_INTERVAL == 0) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 120, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
			}
		}
		if (retained.isEmpty()) {
			infectiousInventoryTicks.clear();
			infectiousInventoryWarnCooldowns.clear();
		} else {
			infectiousInventoryTicks.object2IntEntrySet().removeIf(entry -> !retained.contains(entry.getKey()));
			infectiousInventoryWarnCooldowns.object2LongEntrySet().removeIf(entry -> !retained.contains(entry.getKey()));
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

	private void broadcast(ServerWorld world, Text text) {
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(text, false));
	}

	private Optional<SingularitySnapshot> getSingularitySnapshot() {
		return Optional.of(new SingularitySnapshot(
				singularityState.name(),
				singularityTicks,
				singularityCenter,
				singularityShellCollapsed,
				singularityCollapseRadius,
				singularityCollapseDescending,
				singularityRingTicks,
				singularityPhaseDelay,
				singularityFusePulseTicker,
				singularityFuseElapsed,
				new ArrayList<>(singularityChunkQueue),
				singularityCollapseTotalChunks,
				singularityCollapseCompletedChunks,
				new CollapseTimingSnapshot(singularityCollapseBarDelay, singularityCollapseCompleteHold),
				new ResetSnapshot(new ArrayList<>(singularityResetQueue), singularityResetDelay),
				getSingularityBorderSnapshot().orElse(null)));
	}

	private Optional<SingularityBorderSnapshot> getSingularityBorderSnapshot() {
		if (!singularityBorderActive && !singularityBorderHasSnapshot) {
			return Optional.empty();
		}
		SingularityBorderOriginalSnapshot original = null;
		if (singularityBorderHasSnapshot) {
			original = new SingularityBorderOriginalSnapshot(
					singularityBorderOriginalCenterX,
					singularityBorderOriginalCenterZ,
					singularityBorderOriginalDiameter,
					singularityBorderOriginalSafeZone,
					singularityBorderOriginalDamagePerBlock,
					singularityBorderOriginalWarningBlocks,
					singularityBorderOriginalWarningTime);
		}
		SingularityBorderTimelineSnapshot timeline = new SingularityBorderTimelineSnapshot(
				copyRingThresholds(),
				copyRingCounts(),
				copyRingRadii(),
				singularityInitialBorderDiameter,
				singularityFinalBorderDiameter,
				singularityTotalRingTicks,
				singularityRingTickAccumulator,
				singularityRingIndex,
				singularityRingPendingChunks,
				singularityBorderOuterRadius,
				singularityBorderInnerRadius,
				singularityRingActualCount);
		return Optional.of(new SingularityBorderSnapshot(
				singularityBorderActive,
				singularityBorderCenterX,
				singularityBorderCenterZ,
				singularityBorderInitialDiameter,
				singularityBorderTargetDiameter,
				singularityBorderDuration,
				singularityBorderElapsed,
				singularityBorderLastDiameter,
				original,
				timeline));
	}

	private void applySingularitySnapshot(SingularitySnapshot snapshot) {
		try {
			singularityState = SingularityState.valueOf(snapshot.state());
		} catch (IllegalArgumentException ex) {
			singularityState = SingularityState.DORMANT;
		}
		singularityLastLoggedState = singularityState;
		singularityTicks = Math.max(0L, snapshot.ticks());
		singularityCenter = snapshot.centerPos();
		singularityShellCollapsed = snapshot.shellCollapsed();
		singularityCollapseRadius = Math.max(0, snapshot.collapseRadius());
		singularityCollapseDescending = snapshot.collapseDescending();
		singularityRingTicks = snapshot.ringTicks();
		singularityPhaseDelay = snapshot.phaseDelay();
		singularityFusePulseTicker = MathHelper.clamp(snapshot.fuseTicker(), 0, configuredFusePulseInterval());
		singularityFuseElapsed = Math.max(0L, snapshot.fuseElapsed());
		singularityChunkQueue.clear();
		snapshot.chunkQueue().forEach(chunk -> singularityChunkQueue.addLast(chunk.longValue()));
		singularityCollapseTotalChunks = Math.max(0, snapshot.collapseTotal());
		singularityCollapseCompletedChunks = MathHelper.clamp(snapshot.collapseCompleted(), 0, singularityCollapseTotalChunks);
		CollapseTimingSnapshot timings = snapshot.timings();
		if (timings != null) {
			singularityCollapseBarDelay = timings.delay();
			singularityCollapseCompleteHold = Math.max(0, timings.hold());
		} else {
			singularityCollapseBarDelay = 0;
			singularityCollapseCompleteHold = 0;
		}
		ResetSnapshot reset = snapshot.reset();
		singularityResetQueue.clear();
		if (reset != null) {
			reset.queue().forEach(chunk -> singularityResetQueue.addLast(chunk.longValue()));
			singularityResetDelay = Math.max(0, reset.delay());
		} else {
			singularityResetDelay = 0;
		}
		singularityResetProcessed.clear();
		applySingularityBorderSnapshot(snapshot.border());
		singularityDestructionEngine = SingularityDestructionEngine.create();
		singularityDestructionDirty = true;
	}

	private void applySingularityBorderSnapshot(@Nullable SingularityBorderSnapshot snapshot) {
		if (snapshot == null) {
			clearSingularityBorderState();
			return;
		}
		singularityBorderActive = snapshot.active();
		singularityBorderCenterX = snapshot.centerX();
		singularityBorderCenterZ = snapshot.centerZ();
		singularityBorderInitialDiameter = snapshot.initialDiameter();
		singularityBorderTargetDiameter = snapshot.targetDiameter();
		singularityBorderDuration = snapshot.duration();
		singularityBorderElapsed = snapshot.elapsed();
		singularityBorderLastDiameter = snapshot.lastDiameter();
		SingularityBorderOriginalSnapshot original = snapshot.original();
		singularityBorderHasSnapshot = original != null;
		if (original != null) {
			singularityBorderOriginalCenterX = original.centerX();
			singularityBorderOriginalCenterZ = original.centerZ();
			singularityBorderOriginalDiameter = original.diameter();
			singularityBorderOriginalSafeZone = original.safeZone();
			singularityBorderOriginalDamagePerBlock = original.damagePerBlock();
			singularityBorderOriginalWarningBlocks = original.warningBlocks();
			singularityBorderOriginalWarningTime = original.warningTime();
		} else {
			singularityBorderOriginalCenterX = 0.0D;
			singularityBorderOriginalCenterZ = 0.0D;
			singularityBorderOriginalDiameter = 0.0D;
			singularityBorderOriginalSafeZone = 0.0D;
			singularityBorderOriginalDamagePerBlock = 0.0D;
			singularityBorderOriginalWarningBlocks = 0;
			singularityBorderOriginalWarningTime = 0;
		}
		SingularityBorderTimelineSnapshot timeline = snapshot.timeline();
		singularityRingThresholds.clear();
		singularityRingChunkCounts.clear();
		singularityRingRadii.clear();
		if (timeline != null) {
			for (Double threshold : timeline.thresholds()) {
				if (threshold != null) {
					singularityRingThresholds.add(threshold.doubleValue());
				}
			}
			for (Integer count : timeline.ringCounts()) {
				if (count != null) {
					singularityRingChunkCounts.add(Math.max(0, count.intValue()));
				}
			}
			for (Integer radius : timeline.ringRadii()) {
				if (radius != null) {
					singularityRingRadii.add(Math.max(0, radius.intValue()));
				}
			}
			singularityInitialBorderDiameter = timeline.initialDiameter();
			singularityFinalBorderDiameter = timeline.finalDiameter();
			singularityTotalRingTicks = Math.max(0L, timeline.totalTicks());
			singularityRingTickAccumulator = MathHelper.clamp(timeline.elapsedTicks(), 0L, singularityTotalRingTicks);
			singularityRingIndex = timeline.ringIndex();
			singularityRingPendingChunks = Math.max(0, timeline.pendingChunks());
			singularityBorderOuterRadius = Math.max(0.0D, timeline.outerRadius());
			singularityBorderInnerRadius = Math.max(0.0D, timeline.innerRadius());
			singularityRingActualCount = Math.max(1, timeline.ringCount());
			singularityBorderPendingDeployment = false;
		} else {
			singularityInitialBorderDiameter = singularityBorderInitialDiameter;
			singularityFinalBorderDiameter = singularityBorderTargetDiameter;
			singularityTotalRingTicks = 0L;
			singularityRingTickAccumulator = 0L;
			singularityRingIndex = -1;
			singularityRingPendingChunks = 0;
			singularityBorderOuterRadius = 0.0D;
			singularityBorderInnerRadius = 0.0D;
			singularityRingActualCount = 0;
			singularityBorderPendingDeployment = false;
		}
	}

	private void resetSingularityState(ServerWorld world) {
		releaseSingularityChunkTickets(world);
		restoreCollapseDistanceOverrides(world.getServer());
		clearBlackholePearl(world);
		removeSingularityGlow(world);
		restoreSingularityBorder(world);
		clearSingularityState();
	}

	private void clearSingularityState() {
		singularityState = SingularityState.DORMANT;
		singularityLastLoggedState = SingularityState.DORMANT;
		singularityCollapseStallTicks = 0;
		singularityFuseWatchdogTicks = 0;
		singularityTicks = 0L;
		singularityCenter = null;
		singularityShellCollapsed = false;
		singularityCollapseRadius = 0;
		singularityCollapseDescending = true;
		singularityRingTicks = 0;
		singularityPhaseDelay = 0;
		singularityFusePulseTicker = 0;
		singularityFuseElapsed = 0;
		singularityChunkQueue.clear();
		singularityResetQueue.clear();
		singularityResetProcessed.clear();
		singularityResetDelay = 0;
		singularityCollapseTickCooldown = 0;
		singularityCollapseTotalChunks = 0;
		singularityCollapseCompletedChunks = 0;
		singularityCollapseBarDelay = 0;
		singularityCollapseCompleteHold = 0;
		singularityDestructionEngine = SingularityDestructionEngine.create();
		singularityDestructionDirty = true;
		shutdownCollapseScheduler();
		singularityPreloadQueue.clear();
		singularityPreloadComplete = false;
		singularityPreloadMissingChunks = 0;
		singularityDebugLogCooldown = SINGULARITY_DEBUG_LOG_INTERVAL;
		fuseClearedBlocks.clear();
		collapseBufferedChunks.clear();
		suppressedUnregisters.clear();
		blackholePearlId = null;
		singularityGlowNodes.clear();
		clearSingularityBorderState();
		singularityRingRadii.clear();
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
			if (shouldSkipSpreadThisTick(world)) {
				break;
			}
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

			BlockState state = world.getBlockState(pos);
			boolean isCore = isVirusCoreBlock(pos, state);
			if (!isCore) {
				boolean singularityActive = singularityState != SingularityState.DORMANT && state.isOf(ModBlocks.SINGULARITY_BLOCK);
				if (!singularityActive) {
					iterator.remove();
					markDirty();
					removed = true;
				}
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
			if (isVirusCoreBlock(pos, current) || current.isOf(ModBlocks.SINGULARITY_BLOCK) || current.isOf(block)) {
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
			if (shellRebuildPending) {
				shellRebuildPending = false;
				broadcast(world, Text.translatable("message.the-virus-block.shells_reforming").formatted(Formatting.DARK_AQUA));
			}
		});
	}

	private boolean shouldPushDuringShell() {
		InfectionTier tier = getCurrentTier();
		return tier.getIndex() < 3 || ticksInTier < tier.getDurationTicks() / 2;
	}
	
	private void pushPlayersFromBlock(ServerWorld world, BlockPos formingPos, int radius) {
		pushPlayersFromBlock(world, formingPos, radius, 1.0D, true);
	}

	private void pushPlayersFromBlock(ServerWorld world, BlockPos formingPos, int radius, double strengthScale, boolean spawnGuardian) {
		Vec3d origin = Vec3d.ofCenter(formingPos);
		double pushRadius = Math.max(4.0D, radius + 4.0D);
		double pushRadiusSq = pushRadius * pushRadius;
		double difficultyKnockback = difficulty.getKnockbackMultiplier();
		if (difficultyKnockback <= 0.0D) {
			return;
		}
		double scale = Math.max(0.1D, strengthScale);
		double baseStrength = (1.2D + radius * 0.15D) * difficultyKnockback * scale;
		double verticalBoost = (0.5D + radius * 0.05D) * difficultyKnockback * scale;

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
			if (spawnGuardian) {
				spawnGuardianBeam(world, formingPos, player);
			}
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
		guardian.addCommandTag(TheVirusBlock.VIRUS_DEFENSE_BEAM_TAG);
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
		shellRebuildPending = true;
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
		if (!isVirusCoreBlock(from, world.getBlockState(from))) {
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
			if (isVirusCoreBlock(target, world.getBlockState(target))
					|| world.getBlockState(target).isOf(ModBlocks.SINGULARITY_BLOCK)) {
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
		double highTierScale = 1.0D;
		if (tierIndex >= 4) {
			highTierScale = 0.35D;
		} else if (tierIndex >= 3) {
			highTierScale = 0.6D;
		}
		delay = Math.max(PLAYER_OCCUPANCY_DELAY, Math.round(delay * highTierScale));
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
		float singularityFactor = getMatrixCubeSingularityFactor();
		if (singularityFactor <= 0.0F) {
			MatrixCubeBlockEntity.trimActive(world, 0);
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_halt", null, MatrixCubeBlockEntity.getActiveCount(world), 0);
			return;
		}
		maxActive = Math.max(1, MathHelper.floor(maxActive * singularityFactor));
		MatrixCubeBlockEntity.trimActive(world, maxActive);
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
		double activityMultiplier = getSingularityActivityMultiplier();
		if (activityMultiplier <= 0.0D) {
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_freeze", null, active, maxActive);
			return;
		}
		int attempts = Math.max(1, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_SPAWN_ATTEMPTS));
		attempts = Math.max(0, MathHelper.floor(attempts * activityMultiplier * singularityFactor));
		if (attempts <= 0) {
			CorruptionProfiler.logMatrixCubeSkip(world, "singularity_freeze", null, active, maxActive);
			return;
		}
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
			triggerFinalBarrierBlast(world);
			apocalypseMode = true;
			markDirty();
			return;
		}

		tierIndex++;
		ticksInTier = 0;
		if (tierIndex >= 3) {
			shellRebuildPending = true;
		}
		shellCooldowns.clear();
		nextTierFiveBarrierPushTick = 0L;
		finalVulnerabilityBlastTriggered = false;
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
			shellRebuildPending = tierIndex >= 3;
			tierFiveBarrierActive = false;
			cleansingActive = false;
			resetHealthForTier(InfectionTier.byIndex(tierIndex));
			pillarChunks.clear();
			eventHistory.clear();
			resetSingularityState(world);
			tierFiveBarrierActive = false;
			nextTierFiveBarrierPushTick = 0L;
			finalVulnerabilityBlastTriggered = false;
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
		fuseClearedBlocks.clear();
		suppressedUnregisters.clear();
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
		shellRebuildPending = false;
		resetSingularityState(world);
		deactivateTierFiveBarrier(world);
		healthScale = 1.0D;
		currentHealth = 0.0D;
		beginCleansing();
		eventHistory.clear();
		shellCooldowns.clear();
		pillarChunks.clear();
		guardianBeams.clear();
		nextTierFiveBarrierPushTick = 0L;
		finalVulnerabilityBlastTriggered = false;
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
			if (state.isAir()
					|| state.getHardness(world, target) < 0.0F
					|| isVirusCoreBlock(target, state)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
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

	public long getTicksUntilFinalWave() {
		if (!infected || apocalypseMode) {
			return 0L;
		}
		InfectionTier current = getCurrentTier();
		long remaining = Math.max(0L, getTierDuration(current) - ticksInTier);
		for (int tier = current.getIndex() + 1; tier <= InfectionTier.maxIndex(); tier++) {
			remaining += getTierDuration(InfectionTier.byIndex(tier));
		}
		return remaining;
	}

	public SingularityState getSingularityState() {
		return singularityState;
	}

	public long getSingularityTicks() {
		return singularityTicks;
	}

	public float getSingularityFuseProgress() {
		return getSingularitySuppressionProgress();
	}

	public float getSingularityCollapseProgress() {
		if (singularityState != SingularityState.COLLAPSE) {
			return 0.0F;
		}
		if (singularityCollapseBarDelay > 0) {
			return 1.0F;
		}
		if (singularityBorderDuration > 0L) {
			float elapsed = MathHelper.clamp((float) singularityBorderElapsed / (float) singularityBorderDuration, 0.0F, 1.0F);
			float progress = 1.0F - elapsed;
			return MathHelper.clamp(progress, 0.0F, 1.0F);
		}
		if (singularityCollapseTotalChunks <= 0) {
			return 0.0F;
		}
		float completed = MathHelper.clamp((float) singularityCollapseCompletedChunks / (float) singularityCollapseTotalChunks, 0.0F, 1.0F);
		return MathHelper.clamp(1.0F - completed, 0.0F, 1.0F);
	}

	public int getSingularityCollapseDelayTicks() {
		return Math.max(0, singularityCollapseBarDelay);
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

	public enum SingularityState {
		DORMANT,
		FUSING,
		COLLAPSE,
		CORE,
		RING,
		DISSIPATION,
		RESET
	}

	private record HealthSnapshot(double scale, double current) {
		private static final HealthSnapshot DEFAULT = new HealthSnapshot(1.0D, 0.0D);
		private static final Codec<HealthSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.optionalFieldOf("scale", 1.0D).forGetter(HealthSnapshot::scale),
				Codec.DOUBLE.optionalFieldOf("current", 0.0D).forGetter(HealthSnapshot::current)
		).apply(inst, HealthSnapshot::new));

		private static HealthSnapshot of(VirusWorldState state) {
			return new HealthSnapshot(state.healthScale, state.currentHealth);
		}
	}

	private record SingularityPersistenceTail(
			List<ShieldField> shields,
			Optional<SingularitySnapshot> snapshot,
			boolean preGenComplete,
			int preGenMissing) {
	}

	private record SingularitySnapshot(
			String state,
			long ticks,
			@Nullable BlockPos centerPos,
			boolean shellCollapsed,
			int collapseRadius,
			boolean collapseDescending,
			int ringTicks,
			int phaseDelay,
			int fuseTicker,
			long fuseElapsed,
			List<Long> chunkQueue,
			int collapseTotal,
			int collapseCompleted,
			@Nullable CollapseTimingSnapshot timings,
			@Nullable ResetSnapshot reset,
			@Nullable SingularityBorderSnapshot border) {
		private static final Codec<SingularitySnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.STRING.optionalFieldOf("state", SingularityState.DORMANT.name()).forGetter(SingularitySnapshot::state),
				Codec.LONG.optionalFieldOf("ticks", 0L).forGetter(SingularitySnapshot::ticks),
				BlockPos.CODEC.optionalFieldOf("center").forGetter(snapshot -> Optional.ofNullable(snapshot.centerPos())),
				Codec.BOOL.optionalFieldOf("shellCollapsed", false).forGetter(SingularitySnapshot::shellCollapsed),
				Codec.INT.optionalFieldOf("collapseRadius", 0).forGetter(SingularitySnapshot::collapseRadius),
				Codec.BOOL.optionalFieldOf("collapseDescending", true).forGetter(SingularitySnapshot::collapseDescending),
				Codec.INT.optionalFieldOf("ringTicks", 0).forGetter(SingularitySnapshot::ringTicks),
				Codec.INT.optionalFieldOf("phaseDelay", 0).forGetter(SingularitySnapshot::phaseDelay),
				Codec.INT.optionalFieldOf("fuseTicker", 0).forGetter(SingularitySnapshot::fuseTicker),
				Codec.LONG.optionalFieldOf("fuseElapsed", 0L).forGetter(SingularitySnapshot::fuseElapsed),
				Codec.LONG.listOf().optionalFieldOf("chunkQueue", List.of()).forGetter(SingularitySnapshot::chunkQueue),
				Codec.INT.optionalFieldOf("collapseTotal", 0).forGetter(SingularitySnapshot::collapseTotal),
				Codec.INT.optionalFieldOf("collapseCompleted", 0).forGetter(SingularitySnapshot::collapseCompleted),
				CollapseTimingSnapshot.CODEC.optionalFieldOf("timings").forGetter(snapshot -> Optional.ofNullable(snapshot.timings())),
				ResetSnapshot.CODEC.optionalFieldOf("reset").forGetter(snapshot -> Optional.ofNullable(snapshot.reset())),
				SingularityBorderSnapshot.CODEC.optionalFieldOf("border").forGetter(snapshot -> Optional.ofNullable(snapshot.border()))
		).apply(inst, (state,
				ticks,
				center,
				shellCollapsed,
				collapseRadius,
				collapseDescending,
				ringTicks,
				phaseDelay,
				fuseTicker,
				fuseElapsed,
				queue,
				total,
				completed,
				timings,
				reset,
				border) -> new SingularitySnapshot(state,
						ticks,
						center.orElse(null),
						shellCollapsed,
						collapseRadius,
						collapseDescending,
						ringTicks,
						phaseDelay,
						fuseTicker,
						fuseElapsed,
						queue,
						total,
						completed,
						timings.orElse(null),
						reset.orElse(null),
						border.orElse(null))));
	}

	private record CollapseTimingSnapshot(int delay, int hold) {
		private static final Codec<CollapseTimingSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.INT.optionalFieldOf("delay", 0).forGetter(CollapseTimingSnapshot::delay),
				Codec.INT.optionalFieldOf("hold", 0).forGetter(CollapseTimingSnapshot::hold)
		).apply(inst, CollapseTimingSnapshot::new));
	}

	private record ResetSnapshot(List<Long> queue, int delay) {
		private static final Codec<ResetSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.LONG.listOf().optionalFieldOf("queue", List.of()).forGetter(ResetSnapshot::queue),
				Codec.INT.optionalFieldOf("delay", 0).forGetter(ResetSnapshot::delay)
		).apply(inst, ResetSnapshot::new));
	}

	private record SingularityBorderSnapshot(
			boolean active,
			double centerX,
			double centerZ,
			double initialDiameter,
			double targetDiameter,
			long duration,
			long elapsed,
			double lastDiameter,
			@Nullable SingularityBorderOriginalSnapshot original,
			@Nullable SingularityBorderTimelineSnapshot timeline) {
		private static final Codec<SingularityBorderSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.BOOL.optionalFieldOf("active", false).forGetter(SingularityBorderSnapshot::active),
				Codec.DOUBLE.optionalFieldOf("centerX", 0.0D).forGetter(SingularityBorderSnapshot::centerX),
				Codec.DOUBLE.optionalFieldOf("centerZ", 0.0D).forGetter(SingularityBorderSnapshot::centerZ),
				Codec.DOUBLE.optionalFieldOf("initialDiameter", 0.0D).forGetter(SingularityBorderSnapshot::initialDiameter),
				Codec.DOUBLE.optionalFieldOf("targetDiameter", 0.0D).forGetter(SingularityBorderSnapshot::targetDiameter),
				Codec.LONG.optionalFieldOf("duration", 0L).forGetter(SingularityBorderSnapshot::duration),
				Codec.LONG.optionalFieldOf("elapsed", 0L).forGetter(SingularityBorderSnapshot::elapsed),
				Codec.DOUBLE.optionalFieldOf("lastDiameter", 0.0D).forGetter(SingularityBorderSnapshot::lastDiameter),
				SingularityBorderOriginalSnapshot.CODEC.optionalFieldOf("original").forGetter(snapshot -> Optional.ofNullable(snapshot.original())),
				SingularityBorderTimelineSnapshot.CODEC.optionalFieldOf("timeline").forGetter(snapshot -> Optional.ofNullable(snapshot.timeline()))
		).apply(inst, (active, centerX, centerZ, initialDiameter, targetDiameter, duration, elapsed, lastDiameter, original, timeline) ->
				new SingularityBorderSnapshot(active, centerX, centerZ, initialDiameter, targetDiameter, duration, elapsed, lastDiameter, original.orElse(null), timeline.orElse(null))));
	}

	private record SingularityBorderOriginalSnapshot(
			double centerX,
			double centerZ,
			double diameter,
			double safeZone,
			double damagePerBlock,
			int warningBlocks,
			int warningTime) {
		private static final Codec<SingularityBorderOriginalSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.optionalFieldOf("centerX", 0.0D).forGetter(SingularityBorderOriginalSnapshot::centerX),
				Codec.DOUBLE.optionalFieldOf("centerZ", 0.0D).forGetter(SingularityBorderOriginalSnapshot::centerZ),
				Codec.DOUBLE.optionalFieldOf("diameter", 0.0D).forGetter(SingularityBorderOriginalSnapshot::diameter),
				Codec.DOUBLE.optionalFieldOf("safeZone", 0.0D).forGetter(SingularityBorderOriginalSnapshot::safeZone),
				Codec.DOUBLE.optionalFieldOf("damagePerBlock", 0.0D).forGetter(SingularityBorderOriginalSnapshot::damagePerBlock),
				Codec.INT.optionalFieldOf("warningBlocks", 0).forGetter(SingularityBorderOriginalSnapshot::warningBlocks),
				Codec.INT.optionalFieldOf("warningTime", 0).forGetter(SingularityBorderOriginalSnapshot::warningTime)
		).apply(inst, SingularityBorderOriginalSnapshot::new));
	}

	private record SingularityBorderTimelineSnapshot(
			List<Double> thresholds,
			List<Integer> ringCounts,
			List<Integer> ringRadii,
			double initialDiameter,
			double finalDiameter,
			long totalTicks,
			long elapsedTicks,
			int ringIndex,
			int pendingChunks,
			double outerRadius,
			double innerRadius,
			int ringCount) {
		private static final Codec<SingularityBorderTimelineSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.listOf().optionalFieldOf("thresholds", List.of()).forGetter(SingularityBorderTimelineSnapshot::thresholds),
				Codec.INT.listOf().optionalFieldOf("ringCounts", List.of()).forGetter(SingularityBorderTimelineSnapshot::ringCounts),
				Codec.INT.listOf().optionalFieldOf("ringRadii", List.of()).forGetter(SingularityBorderTimelineSnapshot::ringRadii),
				Codec.DOUBLE.optionalFieldOf("initialDiameter", 0.0D).forGetter(SingularityBorderTimelineSnapshot::initialDiameter),
				Codec.DOUBLE.optionalFieldOf("finalDiameter", 0.0D).forGetter(SingularityBorderTimelineSnapshot::finalDiameter),
				Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(SingularityBorderTimelineSnapshot::totalTicks),
				Codec.LONG.optionalFieldOf("elapsedTicks", 0L).forGetter(SingularityBorderTimelineSnapshot::elapsedTicks),
				Codec.INT.optionalFieldOf("ringIndex", -1).forGetter(SingularityBorderTimelineSnapshot::ringIndex),
				Codec.INT.optionalFieldOf("pendingChunks", 0).forGetter(SingularityBorderTimelineSnapshot::pendingChunks),
				Codec.DOUBLE.optionalFieldOf("outerRadius", 0.0D).forGetter(SingularityBorderTimelineSnapshot::outerRadius),
				Codec.DOUBLE.optionalFieldOf("innerRadius", 0.0D).forGetter(SingularityBorderTimelineSnapshot::innerRadius),
				Codec.INT.optionalFieldOf("ringCount", 0).forGetter(SingularityBorderTimelineSnapshot::ringCount)
		).apply(inst, SingularityBorderTimelineSnapshot::new));
	}

}

