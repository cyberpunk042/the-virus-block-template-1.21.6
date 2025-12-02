package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Locale;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.cyberpunk042.util.InfectionLog;

/**
 * Emits collapse telemetry snapshots so {@link net.cyberpunk042.infection.VirusWorldState}
 * no longer has to hand-roll logging payloads.
 */
public final class SingularityTelemetryService {
	private final LoggingService logging;

	public SingularityTelemetryService(LoggingService logging) {
		this.logging = logging;
	}

	public void logCollapseSnapshot(CollapseSnapshot snapshot) {
		if (!SingularityDiagnostics.enabled() || snapshot == null) {
			return;
		}
		String players = formatPlayers(snapshot.players());
		log(LogChannel.SINGULARITY, "players {}", players);
		double minX = snapshot.centerX() - snapshot.radius();
		double maxX = snapshot.centerX() + snapshot.radius();
		double minZ = snapshot.centerZ() - snapshot.radius();
		double maxZ = snapshot.centerZ() + snapshot.radius();
		log(LogChannel.SINGULARITY,
				"border center=({}, {}) radius={} box=[{}..{}]x[{}..{}] ring={} pending={} columns={} layers={}",
				round(snapshot.centerX()),
				round(snapshot.centerZ()),
				round(snapshot.radius()),
				round(minX),
				round(maxX),
				round(minZ),
				round(maxZ),
				snapshot.ringIndex(),
				snapshot.pendingChunks(),
				snapshot.columnsPerTick(),
				snapshot.layersPerSlice());
	}

	public void logStateChange(SingularityState current, SingularityState next, String reason) {
		if (!SingularityDiagnostics.enabled()) {
			return;
		}
		log(LogChannel.SINGULARITY,
				"[state] {} -> {} reason={}",
				current,
				next,
				(reason == null || reason.isBlank()) ? "<none>" : reason);
	}

	public void logCollapseStall(CollapseStallSnapshot snapshot) {
		if (!SingularityDiagnostics.enabled() || snapshot == null) {
			return;
		}
		int ticks = snapshot.stallTicks();
		if (ticks <= 0) {
			return;
		}
		int interval = snapshot.sampleIntervalTicks() > 0 ? snapshot.sampleIntervalTicks() : 20;
		if (ticks % interval != 0) {
			return;
		}
		log(LogChannel.SINGULARITY,
				"[collapse] stalled ticks={} ringPending={} enginePending={} schedulerPending={} preloadComplete={} chunkQueue={} cooldown={}",
				ticks,
				snapshot.ringPendingChunks(),
				snapshot.enginePending(),
				snapshot.schedulerPending(),
				snapshot.preloadComplete(),
				snapshot.chunkQueueSize(),
				snapshot.cooldownTicks());
	}

	private static double round(double value) {
		return Math.round(value * 100.0D) / 100.0D;
	}

	private static String formatPlayers(List<PlayerSample> players) {
		if (players == null || players.isEmpty()) {
			return "<none>";
		}
		StringBuilder builder = new StringBuilder();
		for (PlayerSample sample : players) {
			if (builder.length() > 0) {
				builder.append("; ");
			}
			builder.append(sample.name())
					.append("=")
					.append(formatCoord(sample.x()))
					.append(",")
					.append(formatCoord(sample.y()))
					.append(",")
					.append(formatCoord(sample.z()));
		}
		return builder.toString();
	}

	private static String formatCoord(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private void log(LogChannel channel, String message, Object... args) {
		if (logging != null) {
			logging.info(channel, message, args);
		} else {
			InfectionLog.info(channel, message, args);
		}
	}

	public record CollapseSnapshot(
			double centerX,
			double centerZ,
			double radius,
			int ringIndex,
			int pendingChunks,
			int columnsPerTick,
			int layersPerSlice,
			List<PlayerSample> players) {
	}

	public record PlayerSample(String name, double x, double y, double z) {
	}

	public record CollapseStallSnapshot(
			int stallTicks,
			int ringPendingChunks,
			int enginePending,
			boolean schedulerPending,
			boolean preloadComplete,
			int chunkQueueSize,
			int cooldownTicks,
			int sampleIntervalTicks) {
	}
}

