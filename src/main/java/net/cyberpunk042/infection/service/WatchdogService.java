package net.cyberpunk042.infection.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;

/**
 * Simple watchdog registry that can emit structured alerts when probes fail
 * thresholds. The implementation is intentionally lightweight until we wire it
 * into more subsystems.
 */
public final class WatchdogService {
	private final LoggingService logging;
	private final AlertingService alerting;
	private final Map<String, Probe> probes = new ConcurrentHashMap<>();

	public WatchdogService(LoggingService logging, AlertingService alerting) {
		this.logging = logging;
		this.alerting = alerting;
	}

	public void record(String probeId, double value, double threshold, LogChannel channel) {
		if (value <= threshold) {
			return;
		}
		Probe probe = probes.computeIfAbsent(probeId, Probe::new);
		if (probe.markTriggered()) {
			String message = "watchdog triggered probe=%s value=%.2f threshold=%.2f".formatted(probeId, value, threshold);
			logging.warn(channel, message);
			alerting.dispatch(channel, message);
		}
	}

	public void clear(String probeId) {
		probes.remove(probeId);
	}

	private static final class Probe {
		private static final Duration COOLDOWN = Duration.ofSeconds(30);
		private Instant lastAlert = Instant.EPOCH;

		private Probe(String id) {
		}

		private boolean markTriggered() {
			Instant now = Instant.now();
			if (Duration.between(lastAlert, now).compareTo(COOLDOWN) < 0) {
				return false;
			}
			lastAlert = now;
			return true;
		}
	}
}

