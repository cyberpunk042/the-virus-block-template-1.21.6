package net.cyberpunk042.infection.service;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;

/**
 * Central hook for emitting operator-facing alerts. Today it simply proxies to
 * the logging service, but the abstraction makes it easy to add webhooks,
 * Discord, etc. later without touching call sites.
 */
public final class AlertingService {
	private final LoggingService logging;

	public AlertingService(LoggingService logging) {
		this.logging = logging;
	}

	public void dispatch(LogChannel channel, String message, Object... args) {
		logging.warn(channel, "[alert] " + message, args);
	}
}

