package net.cyberpunk042.infection.singularity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.ChunkPos;

/**
 * Coordinates the layered collapse of the world during a Singularity event.
 * This engine is intentionally data-oriented and serializable so that the
 * collapse can pause and resume across saves or when players leave the area.
 */
public final class SingularityDestructionEngine {
	private final Deque<ColumnTask> pendingColumns = new ArrayDeque<>();
	private final List<RingPlan> rings = new ArrayList<>();
	private int activeColumnsPerTick = 8;
	private int columnMinY = 0;
	private int columnMaxY = 256;
	private double targetRadius;
	private double currentRadius;
	private boolean finished;

	private SingularityDestructionEngine() {
	}

	public static SingularityDestructionEngine create() {
		return new SingularityDestructionEngine();
	}

	public void setActiveColumnsPerTick(int value) {
		activeColumnsPerTick = Math.max(1, value);
	}

	public int getPendingColumns() {
		return pendingColumns.size();
	}

	public int getActiveColumnsPerTick() {
		return activeColumnsPerTick;
	}

	@Nullable
	public ColumnTask pollColumn() {
		return pendingColumns.pollFirst();
	}

	public void requeueColumn(@Nullable ColumnTask task) {
		if (task != null) {
			enqueue(task);
		}
	}

	public void onColumnComplete(ColumnTask task) {
		currentRadius = Math.max(currentRadius, task.radius());
	}

	public void setColumnBounds(int minY, int maxY) {
		columnMinY = Math.min(minY, maxY);
		columnMaxY = Math.max(minY, maxY);
	}

	public void setTargetRadius(double radius) {
		targetRadius = Math.max(0.0D, radius);
	}

	public double getTargetRadius() {
		return targetRadius;
	}

	public double getCurrentRadius() {
		return currentRadius;
	}

	public boolean isFinished() {
		return finished && pendingColumns.isEmpty() && rings.stream().allMatch(RingPlan::activated);
	}

	public void markFinished() {
		finished = true;
	}

	public void enqueueRing(int ringIndex, double radiusThreshold, List<ChunkPos> chunks) {
		rings.add(new RingPlan(ringIndex, radiusThreshold, chunks, false));
	}

	public void activateRing(int ringIndex, boolean fromBottom, boolean ringSliceMode) {
		if (ringIndex < 0 || ringIndex >= rings.size()) {
			return;
		}
		RingPlan plan = rings.get(ringIndex);
		if (plan.activated()) {
			return;
		}
		plan.activate(pendingColumns, columnMinY, columnMaxY, fromBottom, ringSliceMode);
	}

	/**
	 * Removes up to N chunk slices and feeds them to the provided consumer.
	 *
	 * @return true if any work was performed.
	 */
	public <T> boolean consumeColumns(ColumnConsumer<T> consumer, T context) {
		if (pendingColumns.isEmpty()) {
			return false;
		}
		int processed = 0;
		while (processed < activeColumnsPerTick && !pendingColumns.isEmpty()) {
			ColumnTask task = pendingColumns.pollFirst();
			if (task == null) {
				break;
			}
			SingularityChunkContext.recordColumnProcessed();
			ColumnTask nextTask = consumer.consume(task, context);
			if (nextTask != null) {
				enqueue(nextTask);
			} else {
				currentRadius = Math.max(currentRadius, task.radius());
			}
			processed++;
		}
		return processed > 0;
	}

	public interface ColumnConsumer<T> {
		/**
		 * Process a column slice. Return the next task to queue (often {@code task.advance()}), or
		 * {@code null} when the column is complete. Returning the same task allows retrying (e.g., chunk not loaded).
		 */
		@Nullable ColumnTask consume(ColumnTask task, T context);
	}

	public record ColumnTask(ChunkPos chunk,
			int currentY,
			int minY,
			int maxY,
			int step,
			double radius,
			boolean sticky,
			int ringSliceDepth) {
		public ColumnTask(ChunkPos chunk, int currentY, int minY, int maxY, int step, double radius) {
			this(chunk, currentY, minY, maxY, step, radius, false, -1);
		}

		public ColumnTask(ChunkPos chunk,
				int currentY,
				int minY,
				int maxY,
				int step,
				double radius,
				boolean sticky) {
			this(chunk, currentY, minY, maxY, step, radius, sticky, -1);
		}

		public @Nullable ColumnTask advance() {
			int nextY = currentY + step;
			if ((step > 0 && nextY > maxY) || (step < 0 && nextY < minY)) {
				return null;
			}
			return new ColumnTask(chunk, nextY, minY, maxY, step, radius, sticky, ringSliceDepth);
		}

		public ColumnTask withSticky(boolean value) {
			if (this.sticky == value) {
				return this;
			}
			return new ColumnTask(chunk, currentY, minY, maxY, step, radius, value, ringSliceDepth);
		}

	}

	public static final class RingPlan {
		@SuppressWarnings("unused")
		private final int index;
		private final double radiusThreshold;
		private final List<ChunkPos> chunks;
		private boolean activated;

		public RingPlan(int index, double radiusThreshold, List<ChunkPos> chunks, boolean activated) {
			this.index = index;
			this.radiusThreshold = radiusThreshold;
			this.chunks = chunks;
			this.activated = activated;
		}

		public boolean activated() {
			return activated;
		}

		void activate(Deque<ColumnTask> queue, int minY, int maxY, boolean fromBottom, boolean ringSliceMode) {
			if (activated) {
				return;
			}
			activated = true;
			int startY = fromBottom ? minY : maxY;
			int step = fromBottom ? 1 : -1;
			if (ringSliceMode) {
				for (int depth = 0; depth < SingularityRingSlices.SLICE_COUNT; depth++) {
					for (ChunkPos chunk : chunks) {
						queue.addLast(new ColumnTask(chunk, startY, minY, maxY, step, radiusThreshold, false, depth));
					}
				}
				return;
			}
			for (ChunkPos chunk : chunks) {
				queue.addLast(new ColumnTask(chunk, startY, minY, maxY, step, radiusThreshold));
			}
		}
	}

	private void enqueue(ColumnTask task) {
		if (task.sticky()) {
			pendingColumns.addFirst(task);
		} else {
			pendingColumns.addLast(task);
		}
	}

	public void removeChunkTasks(ChunkPos chunk) {
		if (pendingColumns.isEmpty()) {
			return;
		}
		pendingColumns.removeIf(task -> task.chunk().equals(chunk));
	}
}

