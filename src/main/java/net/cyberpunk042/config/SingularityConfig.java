package net.cyberpunk042.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;

import net.cyberpunk042.config.InfectionConfigRegistry.ConfigHandle;

/**
 * Server-side configuration for Singularity behaviour. Values here act as hard
 * limits; gamerules can further constrain but never exceed these settings.
 */
public final class SingularityConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("the-virus-block")
			.resolve("singularity.json");

	private static final double DEFAULT_BARRIER_START_RADIUS = 120.0D;
	private static final double DEFAULT_BARRIER_END_RADIUS = 0.5D;
	private static final long DEFAULT_BARRIER_DURATION_TICKS = 1000L;
	private static final long DEFAULT_FUSE_EXPLOSION_DELAY_TICKS = 400L;
	private static final int DEFAULT_FUSE_ANIMATION_DELAY_TICKS = 20;
	private static final int DEFAULT_FUSE_PULSE_INTERVAL = 8;

	private static Settings settings = new Settings();

	static {
		InfectionConfigRegistry.register(ConfigHandle.common("singularity", SingularityConfig::load, SingularityConfig::save));
	}

	private SingularityConfig() {
	}

	public static void load() {
		Path parent = CONFIG_PATH.getParent();
		if (parent != null) {
			try {
				Files.createDirectories(parent);
			} catch (IOException ignored) {
			}
		}
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				Settings loaded = GSON.fromJson(reader, Settings.class);
				if (loaded != null) {
					settings = loaded;
					collapseMode();
					collapseTickDelay();
					radiusDelays();
					barrierStartRadius();
					barrierEndRadius();
					barrierInterpolationTicks();
					collapseEnabled();
					fuseExplosionDelayTicks();
					fuseAnimationDelayTicks();
					fusePulseInterval();
					chunkPreGenEnabled();
					chunkPreloadEnabled();
					chunkPreGenRadiusBlocks();
					chunkPreGenChunksPerTick();
					chunkPreloadChunksPerTick();
					collapseViewDistance();
					collapseSimulationDistance();
					collapseBroadcastMode();
					collapseBroadcastRadius();
					collapseDefaultSyncProfile();
					collapseWatchdogEnabled();
					loggingWatchdog();
				}
			} catch (IOException ignored) {
			}
		} else {
			save();
		}
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
			collapseMode();
			collapseTickDelay();
			radiusDelays();
			barrierStartRadius();
			barrierEndRadius();
			barrierInterpolationTicks();
			collapseEnabled();
			fuseExplosionDelayTicks();
			fuseAnimationDelayTicks();
			fusePulseInterval();
			chunkPreGenEnabled();
			chunkPreloadEnabled();
			chunkPreGenRadiusBlocks();
			chunkPreGenChunksPerTick();
			chunkPreloadChunksPerTick();
			collapseViewDistance();
			collapseSimulationDistance();
			collapseBroadcastMode();
			collapseBroadcastRadius();
			collapseDefaultSyncProfile();
			collapseWatchdogEnabled();
			loggingWatchdog();
			GSON.toJson(settings, writer);
		} catch (IOException ignored) {
		}
	}

	public static boolean allowChunkGeneration() {
		return settings.allowChunkGeneration;
	}

	public static boolean allowOutsideBorderLoad() {
		return settings.allowOutsideBorderLoad;
	}

	public static boolean debugLogging() {
		return settings.debugLogging;
	}

	public static boolean drainWaterAhead() {
		return settings.drainWaterAhead;
	}

	public static int waterDrainOffset() {
		return Math.max(1, settings.waterDrainOffset);
	}

	public static boolean useMultithreadedCollapse() {
		return settings.multithreadCollapse;
	}

	public static CollapseMode collapseMode() {
		CollapseMode resolved = CollapseMode.fromId(settings.collapseMode);
		if (resolved == null) {
			resolved = CollapseMode.RING_SLICE;
			settings.collapseMode = resolved.id;
		}
		return resolved;
	}

	public static boolean useRingSliceChunkMode() {
		return collapseMode() == CollapseMode.RING_SLICE_CHUNK;
	}

	public static boolean collapseParticles() {
		return settings.collapseParticles;
	}

	public static CollapseFillMode bulkFillMode() {
		CollapseFillMode mode = CollapseFillMode.fromId(settings.fillMode);
		if (mode == null) {
			mode = CollapseFillMode.AIR;
			settings.fillMode = mode.id;
		}
		return mode;
	}

	public static FillShape bulkFillShape() {
		FillShape shape = FillShape.fromId(settings.fillShape);
		if (shape == null) {
			shape = FillShape.MATRIX;
			settings.fillShape = shape.id;
		}
		return shape;
	}

	public static int outlineThickness() {
		int value = Math.max(1, settings.outlineThickness);
		settings.outlineThickness = value;
		return value;
	}

	public static boolean useNativeFill() {
		return settings.useNativeFill;
	}

	public static int collapseWorkerCount() {
		int desired = Math.max(1, settings.collapseWorkerCount);
		if (desired >= 4) {
			desired -= desired % 4;
		}
		if (desired == 0) {
			desired = 4;
		}
		return desired;
	}

	public static boolean respectProtectedBlocks() {
		return settings.respectProtectedBlocks;
	}

	public static boolean useRingSliceMode() {
		return collapseMode() == CollapseMode.RING_SLICE;
	}

	public static int collapseTickDelay() {
		int value = Math.max(1, settings.collapseTickDelay);
		settings.collapseTickDelay = value;
		return value;
	}

	public static List<RadiusDelay> radiusDelays() {
		ensureRadiusDelays();
		return Collections.unmodifiableList(settings.radiusDelays);
	}

	public static boolean chunkPreGenEnabled() {
		return settings.chunkPreGenEnabled;
	}

	public static boolean chunkPreloadEnabled() {
		return settings.chunkPreloadEnabled;
	}

	public static int chunkPreGenRadiusBlocks() {
		int radius = settings.chunkPreGenRadiusBlocks;
		if (radius < 0) {
			radius = 0;
		}
		settings.chunkPreGenRadiusBlocks = radius;
		return radius;
	}

	public static int chunkPreGenChunksPerTick() {
		int value = Math.max(1, settings.chunkPreGenChunksPerTick);
		settings.chunkPreGenChunksPerTick = value;
		return value;
	}

	public static int chunkPreloadChunksPerTick() {
		int value = Math.max(1, settings.chunkPreloadChunksPerTick);
		settings.chunkPreloadChunksPerTick = value;
		return value;
	}

	public static LogSpamSettings loggingWatchdog() {
		if (settings.loggingWatchdog == null) {
			settings.loggingWatchdog = new LogSpamSettings();
		}
		return settings.loggingWatchdog;
	}

	public static boolean collapseWatchdogEnabled() {
		return watchdogSettings().enabled;
	}

	public static int watchdogFuseMaxExtraTicks() {
		return clampPositive(watchdogSettings().fuseMaxExtraTicks, 200);
	}

	public static int watchdogCollapseWarnTicks() {
		return clampPositive(watchdogSettings().collapseWarnTicks, 200);
	}

	public static int watchdogCollapseAbortTicks() {
		return clampPositive(watchdogSettings().collapseAbortTicks, 400);
	}

	public static boolean watchdogAutoSkipFuse() {
		return watchdogSettings().autoSkipFuse;
	}

	public static boolean watchdogAutoSkipCollapse() {
		return watchdogSettings().autoSkipCollapse;
	}

	private static CollapseWatchdogSettings watchdogSettings() {
		if (settings.watchdog == null) {
			settings.watchdog = new CollapseWatchdogSettings();
		}
		return settings.watchdog;
	}

	private static int clampPositive(int value, int fallback) {
		return value > 0 ? value : Math.max(1, fallback);
	}

	public static int collapseViewDistance() {
		int value = Math.max(0, settings.collapseViewDistance);
		settings.collapseViewDistance = value;
		return value;
	}

	public static int collapseSimulationDistance() {
		int value = Math.max(0, settings.collapseSimulationDistance);
		settings.collapseSimulationDistance = value;
		return value;
	}

	public static CollapseBroadcastMode collapseBroadcastMode() {
		CollapseBroadcastMode mode = CollapseBroadcastMode.fromId(settings.collapseBroadcastMode);
		if (mode == null) {
			mode = CollapseBroadcastMode.IMMEDIATE;
			settings.collapseBroadcastMode = mode.id;
		}
		return mode;
	}

	public static int collapseBroadcastRadius() {
		int value = Math.max(0, settings.collapseBroadcastRadius);
		settings.collapseBroadcastRadius = value;
		return value;
	}

	public static CollapseSyncProfile collapseDefaultSyncProfile() {
		CollapseSyncProfile profile = CollapseSyncProfile.fromId(settings.collapseDefaultProfile);
		if (profile == null) {
			profile = CollapseSyncProfile.FULL;
			settings.collapseDefaultProfile = profile.id();
		}
		return profile;
	}

	public static void setCollapseViewDistance(int chunks) {
		int sanitized = Math.max(0, chunks);
		if (settings.collapseViewDistance != sanitized) {
			settings.collapseViewDistance = sanitized;
			save();
		}
	}

	public static void setCollapseSimulationDistance(int chunks) {
		int sanitized = Math.max(0, chunks);
		if (settings.collapseSimulationDistance != sanitized) {
			settings.collapseSimulationDistance = sanitized;
			save();
		}
	}

	public static void setCollapseBroadcastMode(CollapseBroadcastMode mode) {
		CollapseBroadcastMode resolved = mode == null ? CollapseBroadcastMode.IMMEDIATE : mode;
		if (!resolved.id.equals(settings.collapseBroadcastMode)) {
			settings.collapseBroadcastMode = resolved.id;
			save();
		}
	}

	public static void setCollapseBroadcastRadius(int radius) {
		int sanitized = Math.max(0, radius);
		if (settings.collapseBroadcastRadius != sanitized) {
			settings.collapseBroadcastRadius = sanitized;
			save();
		}
	}

	public static void setCollapseDefaultSyncProfile(CollapseSyncProfile profile) {
		CollapseSyncProfile resolved = profile == null ? CollapseSyncProfile.FULL : profile;
		if (!resolved.id().equals(settings.collapseDefaultProfile)) {
			settings.collapseDefaultProfile = resolved.id();
			save();
		}
	}

	public static double barrierStartRadius() {
		double radius = settings.barrierStartRadius > 0.0D ? settings.barrierStartRadius : DEFAULT_BARRIER_START_RADIUS;
		if (radius < 16.0D) {
			radius = 16.0D;
		}
		settings.barrierStartRadius = radius;
		return radius;
	}

	public static double barrierEndRadius() {
		double start = barrierStartRadius();
		double end = settings.barrierEndRadius > 0.0D ? settings.barrierEndRadius : DEFAULT_BARRIER_END_RADIUS;
		if (end < 0.5D) {
			end = 0.5D;
		}
		if (end > start) {
			end = start;
		}
		settings.barrierEndRadius = end;
		return end;
	}

	public static long barrierInterpolationTicks() {
		long duration = settings.barrierInterpolationTicks > 0L ? settings.barrierInterpolationTicks : DEFAULT_BARRIER_DURATION_TICKS;
		if (duration < 20L) {
			duration = 20L;
		}
		settings.barrierInterpolationTicks = duration;
		return duration;
	}

	public static boolean barrierAutoReset() {
		return settings.barrierAutoReset;
	}

	public static long barrierResetDelayTicks() {
		long value = settings.barrierResetDelayTicks;
		if (value < 0L) {
			value = 0L;
		}
		settings.barrierResetDelayTicks = value;
		return value;
	}

	public static boolean postResetEnabled() {
		return settings.postResetEnabled;
	}

	public static long postResetDelayTicks() {
		long value = Math.max(0L, settings.postResetDelayTicks);
		settings.postResetDelayTicks = value;
		return value;
	}

	public static int postResetTickDelay() {
		int value = Math.max(1, settings.postResetTickDelay <= 0 ? 1 : (int) settings.postResetTickDelay);
		settings.postResetTickDelay = value;
		return value;
	}

	public static int postResetChunksPerTick() {
		int value = Math.max(1, settings.postResetChunksPerTick);
		settings.postResetChunksPerTick = value;
		return value;
	}

	public static int postResetBatchRadius() {
		int value = Math.max(0, settings.postResetBatchRadius);
		settings.postResetBatchRadius = value;
		return value;
	}

	public static boolean collapseEnabled() {
		return settings.collapseEnabled;
	}

	public static long fuseExplosionDelayTicks() {
		long value = settings.fuseExplosionDelayTicks > 0L ? settings.fuseExplosionDelayTicks : DEFAULT_FUSE_EXPLOSION_DELAY_TICKS;
		if (value < 20L) {
			value = 20L;
		}
		settings.fuseExplosionDelayTicks = value;
		return value;
	}

	public static int fuseAnimationDelayTicks() {
		int value = settings.fuseAnimationDelayTicks >= 0 ? settings.fuseAnimationDelayTicks : DEFAULT_FUSE_ANIMATION_DELAY_TICKS;
		settings.fuseAnimationDelayTicks = value;
		return value;
	}

	public static int fusePulseInterval() {
		int value = settings.fusePulseInterval > 0 ? settings.fusePulseInterval : DEFAULT_FUSE_PULSE_INTERVAL;
		settings.fusePulseInterval = value;
		return value;
	}

	private static void ensureRadiusDelays() {
		if (settings.radiusDelays == null || settings.radiusDelays.isEmpty()) {
			settings.radiusDelays = defaultRadiusDelays();
		}
		settings.radiusDelays.removeIf(delay -> delay == null || delay.side <= 0 || delay.ticks <= 0);
		settings.radiusDelays.sort(Comparator.comparingInt(delay -> delay.side));
	}

	private static List<RadiusDelay> defaultRadiusDelays() {
		List<RadiusDelay> defaults = new ArrayList<>();
		defaults.add(new RadiusDelay(1, 150));
		defaults.add(new RadiusDelay(3, 100));
		defaults.add(new RadiusDelay(9, 40));
		defaults.add(new RadiusDelay(15, 20));
		defaults.sort(Comparator.comparingInt(delay -> delay.side));
		return defaults;
	}

	public enum CollapseMode {
		RING_SLICE("ring_slice"),
		RING_SLICE_CHUNK("ring_slice_chunk");

		private final String id;

		CollapseMode(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public static CollapseMode fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (CollapseMode mode : values()) {
				if (mode.id.equalsIgnoreCase(id)) {
					return mode;
				}
			}
			return null;
		}
	}

	public enum CollapseBroadcastMode {
		IMMEDIATE("immediate"),
		DELAYED("delayed"),
		SUMMARY("summary");

		private final String id;

		CollapseBroadcastMode(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		public static CollapseBroadcastMode fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (CollapseBroadcastMode mode : values()) {
				if (mode.id.equalsIgnoreCase(id)) {
					return mode;
				}
			}
			return null;
		}
	}

	public enum CollapseSyncProfile {
		FULL("full"),
		CINEMATIC("cinematic"),
		MINIMAL("minimal");

		private final String id;

		CollapseSyncProfile(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		@Nullable
		public static CollapseSyncProfile fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (CollapseSyncProfile profile : values()) {
				if (profile.id.equalsIgnoreCase(id)) {
					return profile;
				}
			}
			return null;
		}
	}

	public enum CollapseFillMode {
		AIR("air"),
		DESTROY("destroy");

		public final String id;

		CollapseFillMode(String id) {
			this.id = id;
		}

		@Nullable
		public static CollapseFillMode fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (CollapseFillMode mode : values()) {
				if (mode.id.equalsIgnoreCase(id)) {
					return mode;
				}
			}
			return null;
		}
	}

	public enum FillShape {
		COLUMN("column"),
		ROW("row"),
		VECTOR("vector"),
		MATRIX("matrix"),
		OUTLINE("outline");

		public final String id;

		FillShape(String id) {
			this.id = id;
		}

		@Nullable
		public static FillShape fromId(String id) {
			if (id == null || id.isBlank()) {
				return null;
			}
			for (FillShape shape : values()) {
				if (shape.id.equalsIgnoreCase(id)) {
					return shape;
				}
			}
			return null;
		}
	}

	public static final class RadiusDelay {
		public int side;

		@SerializedName(value = "ticks", alternate = {"slices"})
		public int ticks;

		public RadiusDelay() {
		}

		public RadiusDelay(int side, int ticks) {
			this.side = side;
			this.ticks = ticks;
		}
	}

	private static final class Settings {
		boolean allowChunkGeneration = true;
		boolean allowOutsideBorderLoad = true;
		boolean debugLogging = true;
		boolean drainWaterAhead = true;
		int waterDrainOffset = 1;
		boolean multithreadCollapse = false;
		boolean respectProtectedBlocks = true;
		String collapseMode = CollapseMode.RING_SLICE.id;
		boolean collapseParticles = false;
		String fillMode = CollapseFillMode.AIR.id;
		String fillShape = FillShape.OUTLINE.id;
		int outlineThickness = 2;
		boolean useNativeFill = true;
		int collapseWorkerCount = 1;
		int collapseTickDelay = 1;
		List<RadiusDelay> radiusDelays = defaultRadiusDelays();
		boolean collapseEnabled = true;
		long fuseExplosionDelayTicks = DEFAULT_FUSE_EXPLOSION_DELAY_TICKS;
		int fuseAnimationDelayTicks = DEFAULT_FUSE_ANIMATION_DELAY_TICKS;
		int fusePulseInterval = DEFAULT_FUSE_PULSE_INTERVAL;
		boolean chunkPreGenEnabled = true;
		boolean chunkPreloadEnabled = true;
		int chunkPreGenRadiusBlocks = 0;
		int chunkPreGenChunksPerTick = 8;
		int chunkPreloadChunksPerTick = 4;
		int collapseViewDistance = 0;
		int collapseSimulationDistance = 0;
		String collapseBroadcastMode = CollapseBroadcastMode.IMMEDIATE.id;
		int collapseBroadcastRadius = 96;
		String collapseDefaultProfile = CollapseSyncProfile.FULL.id();
		double barrierStartRadius = DEFAULT_BARRIER_START_RADIUS;
		double barrierEndRadius = DEFAULT_BARRIER_END_RADIUS;
		long barrierInterpolationTicks = DEFAULT_BARRIER_DURATION_TICKS;
		boolean barrierAutoReset = false;
		long barrierResetDelayTicks = 200;
		boolean postResetEnabled = true;
		long postResetDelayTicks = 25;
		int postResetTickDelay = 1;
		int postResetChunksPerTick = 2;
		int postResetBatchRadius = 1;
		LogSpamSettings loggingWatchdog = new LogSpamSettings();
		CollapseWatchdogSettings watchdog = new CollapseWatchdogSettings();
	}

	private static final class CollapseWatchdogSettings {
		boolean enabled = false;
		int fuseMaxExtraTicks = 200;
		int collapseWarnTicks = 200;
		int collapseAbortTicks = 400;
		boolean autoSkipFuse = false;
		boolean autoSkipCollapse = false;
	}

	public static final class LogSpamSettings {
		public boolean enableSpamDetection = true;
		@SerializedName(value = "perSecondThreshold", alternate = {"perTickThreshold"})
		public int perSecondThreshold = 10;
		public int perMinuteThreshold = 200;
		@SerializedName(value = "suppressWhenTriggered", alternate = {"autoSuppress"})
		public boolean suppressWhenTriggered = true;
	}
}

