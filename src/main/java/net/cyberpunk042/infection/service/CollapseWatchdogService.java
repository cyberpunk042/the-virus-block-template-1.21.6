package net.cyberpunk042.infection.service;

import java.util.Objects;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.cyberpunk042.util.InfectionLog;

/**
 * Centralizes collapse watchdog plumbing (scheduler/fuse/collapse watchdogs) and
 * diagnostic helpers such as telemetry/presentation fallbacks and logging.
 */
public final class CollapseWatchdogService {

	private static final SingularityTelemetryService FALLBACK_TELEMETRY = new SingularityTelemetryService(null);

	private final SingularityWatchdogController watchdogController = new SingularityWatchdogController();
	private final SingularityPresentationService presentationFallback = new SingularityPresentationService();

	public CollapseWatchdogService(VirusWorldState host) {
		Objects.requireNonNull(host, "host");
	}

	public SingularityWatchdogController controller() {
		return watchdogController;
	}

	public void resetCollapseStallTicks() {
		watchdogController.resetCollapseStallTicks();
	}

	public int collapseStallTicks() {
		return watchdogController.collapseStallTicks();
	}

	public int diagnosticsSampleInterval() {
		return SingularityDiagnostics.sampleIntervalTicks();
	}

	public SingularityPresentationService presentationService() {
		InfectionServiceContainer c = InfectionServices.container();
		return c != null ? c.presentation() : presentationFallback;
	}

	public SingularityTelemetryService telemetryService() {
		InfectionServiceContainer c = InfectionServices.container();
		return c != null ? c.telemetry() : FALLBACK_TELEMETRY;
	}

	public LoggingService loggingService() {
		InfectionServiceContainer c = InfectionServices.container();
		return c != null ? c.logging() : null;
	}

	public void log(LogChannel channel, String message, Object... args) {
		LoggingService logging = loggingService();
		if (logging != null) {
			logging.info(channel, message, args);
		} else {
			InfectionLog.info(channel, message, args);
		}
	}
}

