package net.cyberpunk042.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import net.fabricmc.loader.api.FabricLoader;

import net.cyberpunk042.config.InfectionConfigRegistry.ConfigHandle;

/**
 * Configuration layer for selective logging across the infection architecture.
 * Admins can toggle individual channels without rebuilding the mod, which makes
 * it possible to focus on specific subsystems when diagnosing issues.
 */
public final class InfectionLogConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("the-virus-block")
			.resolve("logs.json");

	private static final EnumMap<LogChannel, Boolean> FLAGS = new EnumMap<>(LogChannel.class);
	private static LogSettings settings = new LogSettings();

	static {
		InfectionConfigRegistry.register(ConfigHandle.common("logs", InfectionLogConfig::load, InfectionLogConfig::save));
	}

	private InfectionLogConfig() {
	}

	public static synchronized void load() {
		boolean dirty = false;
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				LogSettings loaded = GSON.fromJson(reader, LogSettings.class);
				if (loaded != null) {
					settings = loaded;
				}
			} catch (IOException ignored) {
			}
		} else {
			dirty = true;
		}
		if (settings.channels == null) {
			settings.channels = new LinkedHashMap<>();
		}
		if (settings.watchdog == null) {
			settings.watchdog = new LogWatchdogSettings();
			dirty = true;
		}
		if (settings.legacyWatchdog != null) {
			settings.watchdog = mergeWatchdogs(settings.watchdog, settings.legacyWatchdog);
			settings.legacyWatchdog = null;
			dirty = true;
		}
		for (LogChannel channel : LogChannel.values()) {
			Boolean value = settings.channels.get(channel.id());
			if (value == null) {
				value = channel.defaultEnabled();
				settings.channels.put(channel.id(), value);
				dirty = true;
			}
			FLAGS.put(channel, value);
		}
		if (dirty) {
			save();
		}
	}

	public static synchronized void save() {
		if (settings.channels == null) {
			settings.channels = new LinkedHashMap<>();
		}
		try {
			if (CONFIG_PATH.getParent() != null) {
				Files.createDirectories(CONFIG_PATH.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(settings, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static synchronized void setEnabled(LogChannel channel, boolean enabled) {
		settings.channels.put(channel.id(), enabled);
		FLAGS.put(channel, enabled);
		save();
	}

	public static void reload() {
		load();
	}

	public static boolean isEnabled(LogChannel channel) {
		return FLAGS.getOrDefault(channel, channel.defaultEnabled());
	}

	public static Map<String, Boolean> snapshot() {
		if (settings.channels == null) {
			return Map.of();
		}
		return Map.copyOf(settings.channels);
	}

	public static LogChannel findChannel(String id) {
		if (id == null) {
			return null;
		}
		for (LogChannel channel : LogChannel.values()) {
			if (channel.id.equalsIgnoreCase(id)) {
				return channel;
			}
		}
		return null;
	}

	public enum LogChannel {
		SINGULARITY("singularity", "Singularity", true, "Ring planner + collapse telemetry"),
		SINGULARITY_VISUAL("singularityVisual", "SingularityVisual", true, "Singularity block client/server staging logs"),
		SINGULARITY_FUSE("singularityFuse", "Fuse", true, "Shell fuse entity orchestration"),
		SCHEDULER("scheduler", "Scheduler", false, "Task queue updates (future controllers)"),
		INFECTION("infection", "Infection", false, "Virus source lifecycle (placement, shells, teleport)"),
		EFFECTS("effects", "Effects", false, "Guardian beams, FX bus dispatch diagnostics");

		private final String id;
		private final String label;
		private final boolean defaultEnabled;
		private final String description;

		LogChannel(String id, String label, boolean defaultEnabled, String description) {
			this.id = id;
			this.label = label;
			this.defaultEnabled = defaultEnabled;
			this.description = description;
		}

		public String id() {
			return id;
		}

		public String label() {
			return label;
		}

		public boolean defaultEnabled() {
			return defaultEnabled;
		}

		public String description() {
			return description;
		}

	}

	public static LogWatchdogSettings watchdog() {
		return settings.watchdog;
	}

	public static final class LogWatchdogSettings {
		public boolean enableSpamDetection = false;
		@SerializedName(value = "perSecondThreshold", alternate = {"perTickThreshold"})
		public int perSecondThreshold = 80;
		public int perMinuteThreshold = 1000;
		@SerializedName(value = "suppressWhenTriggered", alternate = {"autoSuppress"})
		public boolean suppressWhenTriggered = false;
	}

	private static LogWatchdogSettings mergeWatchdogs(LogWatchdogSettings primary, LogWatchdogSettings legacy) {
		if (legacy == null) {
			return primary;
		}
		if (primary == null) {
			return legacy;
		}
		LogWatchdogSettings merged = new LogWatchdogSettings();
		merged.enableSpamDetection = legacy.enableSpamDetection || primary.enableSpamDetection;
		merged.perSecondThreshold = legacy.perSecondThreshold > 0 ? legacy.perSecondThreshold : primary.perSecondThreshold;
		merged.perMinuteThreshold = legacy.perMinuteThreshold > 0 ? legacy.perMinuteThreshold : primary.perMinuteThreshold;
		merged.suppressWhenTriggered = legacy.suppressWhenTriggered || primary.suppressWhenTriggered;
		return merged;
	}

	private static final class LogSettings {
		Map<String, Boolean> channels = new LinkedHashMap<>();
		@SerializedName("watchdog")
		LogWatchdogSettings watchdog = new LogWatchdogSettings();
		@SerializedName("loggingWatchdog")
		LogWatchdogSettings legacyWatchdog;
	}
}

