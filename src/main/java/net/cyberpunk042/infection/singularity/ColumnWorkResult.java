package net.cyberpunk042.infection.singularity;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;

public record ColumnWorkResult(SingularityDestructionEngine.ColumnTask task,
		int nextY,
		boolean columnComplete,
		List<BlockPos> clearedBlocks,
		List<BlockPos> drainedBlocks,
		boolean chunkMode,
		@Nullable FailureReason failureReason,
		double borderRadius) {

	public static ColumnWorkResult missing(SingularityDestructionEngine.ColumnTask task) {
		return new ColumnWorkResult(task, task.currentY(), true, List.of(), List.of(), false, FailureReason.MISSING_CHUNK, 0.0D);
	}

	public static ColumnWorkResult border(SingularityDestructionEngine.ColumnTask task, double borderRadius) {
		return new ColumnWorkResult(task, task.currentY(), true, List.of(), List.of(), false, FailureReason.OUTSIDE_BORDER, borderRadius);
	}

	public static ColumnWorkResult slice(SingularityDestructionEngine.ColumnTask task,
			int nextY,
			boolean completed,
			List<BlockPos> cleared,
			List<BlockPos> drained,
			boolean chunkMode) {
		return new ColumnWorkResult(task, nextY, completed, Collections.unmodifiableList(cleared), Collections.unmodifiableList(drained), chunkMode, null, 0.0D);
	}

	public enum FailureReason {
		MISSING_CHUNK,
		OUTSIDE_BORDER
	}
}

