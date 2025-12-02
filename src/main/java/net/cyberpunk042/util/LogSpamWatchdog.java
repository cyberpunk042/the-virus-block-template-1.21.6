package net.cyberpunk042.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.cyberpunk042.config.InfectionLogConfig;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.config.InfectionLogConfig.LogWatchdogSettings;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;

/**
 * Tracks per-template log rates so we can detect and optionally suppress spammy log
 * streams. Each log template (message string + channel) maintains a sliding window count
 * for both per-second and per-minute windows. When a window crosses the configured threshold
 * we emit a summary line and (optionally) suppress further logs for that window.
 */
public final class LogSpamWatchdog {
	private static final Map<TemplateKey, TemplateStats> STATS = new ConcurrentHashMap<>();

	private LogSpamWatchdog() {
	}

	public static SpamDecision observe(LogChannel channel, String template) {
		ActiveSettings settings = resolveSettings(channel);
		if (!settings.enabled()) {
			return SpamDecision.allow();
		}
		int perSecondThreshold = Math.max(0, settings.perSecondThreshold());
		int perMinuteThreshold = Math.max(0, settings.perMinuteThreshold());
		if (perSecondThreshold <= 0 && perMinuteThreshold <= 0) {
			return SpamDecision.allow();
		}
		long nowMillis = Instant.now().toEpochMilli();
		long secondBucket = nowMillis / 1000L;
		long minuteBucket = nowMillis / 60000L;
		TemplateKey key = new TemplateKey(channel, template);
		TemplateStats stats = STATS.computeIfAbsent(key, TemplateStats::new);
		int secondCount;
		int minuteCount;
		long total;
		boolean secondExceeded;
		boolean minuteExceeded;
		synchronized (stats) {
			if (stats.secondBucket != secondBucket) {
				stats.secondBucket = secondBucket;
				stats.secondCount = 0;
			}
			if (stats.minuteBucket != minuteBucket) {
				stats.minuteBucket = minuteBucket;
				stats.minuteCount = 0;
			}
			secondCount = ++stats.secondCount;
			minuteCount = ++stats.minuteCount;
			total = ++stats.totalCount;
			secondExceeded = perSecondThreshold > 0 && secondCount >= perSecondThreshold;
			minuteExceeded = perMinuteThreshold > 0 && minuteCount >= perMinuteThreshold;
		}
		if (!secondExceeded && !minuteExceeded) {
			return SpamDecision.allow();
		}
		StringBuilder reasons = new StringBuilder();
		boolean notify = false;
		if (secondExceeded) {
			synchronized (stats) {
				if (stats.lastSecondAlertBucket != secondBucket) {
					stats.lastSecondAlertBucket = secondBucket;
					notify = true;
				}
			}
			if (reasons.length() > 0) {
				reasons.append('+');
			}
			reasons.append("perSecond(").append(secondCount).append(')');
		}
		if (minuteExceeded) {
			synchronized (stats) {
				if (stats.lastMinuteAlertBucket != minuteBucket) {
					stats.lastMinuteAlertBucket = minuteBucket;
					notify = true;
				}
			}
			if (reasons.length() > 0) {
				reasons.append('+');
			}
			reasons.append("perMinute(").append(minuteCount).append(')');
		}
		boolean suppress = settings.suppressWhenTriggered();
		if (suppress) {
			synchronized (stats) {
				stats.suppressedCount++;
			}
		}
		String summary = null;
		if (notify) {
			long suppressed;
			synchronized (stats) {
				suppressed = stats.suppressedCount;
			}
			summary = String.format(
					"[watchdog] channel=%s template=\"%s\" spamReason=%s rate=%d/s %d/min total=%d suppressed=%d",
					channel.label(),
					trimTemplate(template),
					reasons,
					secondCount,
					minuteCount,
					total,
					suppressed);
		}
		return new SpamDecision(suppress, summary);
	}

	private static String trimTemplate(String template) {
		if (template == null) {
			return "<null>";
		}
		return template.length() <= 160 ? template : template.substring(0, 157) + "...";
	}

	private static ActiveSettings resolveSettings(LogChannel channel) {
		if (channel == LogChannel.SINGULARITY) {
			ServiceConfig.LogSpamSettings singularity = SingularityDiagnostics.logSpamSettings();
			if (singularity.enableSpamDetection) {
				return new ActiveSettings(true,
						singularity.perSecondThreshold,
						singularity.perMinuteThreshold,
						singularity.suppressWhenTriggered);
			}
		}
		LogWatchdogSettings global = InfectionLogConfig.watchdog();
		if (global != null && global.enableSpamDetection) {
			return new ActiveSettings(true,
					global.perSecondThreshold,
					global.perMinuteThreshold,
					global.suppressWhenTriggered);
		}
		return ActiveSettings.DISABLED;
	}

	private record TemplateKey(LogChannel channel, String template) {
		TemplateKey {
			template = template == null ? "<null>" : template;
		}
	}

	private static final class TemplateStats {
		long secondBucket;
		int secondCount;
		long minuteBucket;
		int minuteCount;
		long lastSecondAlertBucket = -1;
		long lastMinuteAlertBucket = -1;
		long totalCount;
		long suppressedCount;

		TemplateStats(TemplateKey key) {
		}
	}

	public record SpamDecision(boolean suppress, String summary) {
		private static final SpamDecision ALLOW = new SpamDecision(false, null);

		public static SpamDecision allow() {
			return ALLOW;
		}
	}

	private record ActiveSettings(boolean enabled,
			int perSecondThreshold,
			int perMinuteThreshold,
			boolean suppressWhenTriggered) {
		private static final ActiveSettings DISABLED = new ActiveSettings(false, 0, 0, false);
	}
}

