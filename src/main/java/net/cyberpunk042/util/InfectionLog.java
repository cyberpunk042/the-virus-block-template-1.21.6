package net.cyberpunk042.util;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.InfectionLogConfig;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;

/**
 * Thin wrapper around the mod logger that respects {@link InfectionLogConfig}
 * channel toggles. This keeps noisy subsystems opt-in while still using the
 * existing SLF4J formatting pipeline.
 */
public final class InfectionLog {
	private InfectionLog() {
	}

	public static void info(LogChannel channel, String message, Object... args) {
		if (!InfectionLogConfig.isEnabled(channel)) {
			return;
		}
		LogSpamWatchdog.SpamDecision decision = LogSpamWatchdog.observe(channel, message);
		if (decision.summary() != null) {
			TheVirusBlock.LOGGER.warn(decision.summary());
		}
		if (decision.suppress()) {
			return;
		}
		TheVirusBlock.LOGGER.info(prefix(message), withLabel(channel, args));
	}

	private static String prefix(String message) {
		return "[{}] " + message;
	}

	private static Object[] withLabel(LogChannel channel, Object[] args) {
		Object[] expanded = new Object[args.length + 1];
		expanded[0] = channel.label();
		System.arraycopy(args, 0, expanded, 1, args.length);
		return expanded;
	}
}

