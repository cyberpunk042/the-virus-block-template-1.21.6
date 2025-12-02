package net.cyberpunk042.infection.service;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.util.InfectionLog;

/**
 * Thin facade over {@link InfectionLog} so future services can swap the
 * underlying logging sink without touching call sites.
 */
public final class LoggingService {

	public void info(LogChannel channel, String message, Object... args) {
		InfectionLog.info(channel, message, args);
	}

	public void warn(LogChannel channel, String message, Object... args) {
		InfectionLog.info(channel, "[warn] " + message, args);
	}

	public void error(LogChannel channel, String message, Object... args) {
		InfectionLog.info(channel, "[error] " + message, args);
	}
}

