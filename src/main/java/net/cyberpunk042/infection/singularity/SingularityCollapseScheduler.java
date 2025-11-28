package net.cyberpunk042.infection.singularity;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.SingularityConfig;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Coordinates collapse work that runs on background threads. Workers only read chunk
 * data and return block coordinates so the main thread applies all mutations safely.
 */
public final class SingularityCollapseScheduler implements AutoCloseable {
	private static final int CHUNK_SIZE = 16;

	private final ExecutorService executor;
	private final Queue<ColumnWorkResult> completed = new ConcurrentLinkedQueue<>();
	private final Supplier<BlockPos> centerSupplier;
	private volatile boolean closed;

	public SingularityCollapseScheduler(Supplier<BlockPos> centerSupplier) {
		this.centerSupplier = centerSupplier;
		int available = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		int desired = SingularityConfig.collapseWorkerCount();
		int workers = MathHelper.clamp(desired, 1, available);
		executor = Executors.newFixedThreadPool(workers, new CollapseThreadFactory());
	}

	public void submit(ServerWorld world, SingularityDestructionEngine.ColumnTask task) {
		Runnable job = () -> {
			ColumnWorkResult result = processSlice(world, task);
			completed.add(result);
		};
		if (closed) {
			job.run();
			return;
		}
		try {
			executor.submit(job);
		} catch (RejectedExecutionException ex) {
			job.run();
		}
	}

	public boolean flush(VirusWorldState state, ServerWorld world, SingularityDestructionEngine engine) {
		boolean applied = false;
		ColumnWorkResult result;
		while ((result = completed.poll()) != null) {
			applied = true;
			state.applyWorkerResult(world, engine, result);
		}
		return applied;
	}

	public boolean hasPendingResults() {
		return !completed.isEmpty();
	}

	@Override
	public void close() {
		closed = true;
		executor.shutdownNow();
		completed.clear();
	}

	private ColumnWorkResult processSlice(ServerWorld world,
			SingularityDestructionEngine.ColumnTask task) {
		SingularityChunkContext.pushChunkBypass(task.chunk());
		try {
			long chunkKey = task.chunk().toLong();
			if (!world.isChunkLoaded(chunkKey)) {
				return ColumnWorkResult.missing(task);
			}

			WorldChunk chunk = getLoadedChunk(world, task.chunk());
			if (chunk == null) {
				return ColumnWorkResult.missing(task);
			}

			if (!allowOutsideBorder(world) && isOutsideBorder(world, task.chunk())) {
				double borderRadius = world.getWorldBorder().getSize() * 0.5D;
				return ColumnWorkResult.border(task, borderRadius);
			}

			List<BlockPos> drained = SingularityConfig.drainWaterAhead() ? new ArrayList<>() : List.of();
			List<BlockPos> cleared = new ArrayList<>();
			int baseY = MathHelper.clamp(task.currentY(), task.minY(), task.maxY());

			try {
				if (SingularityConfig.drainWaterAhead()) {
					collectFluidsAhead(chunk, task, baseY, drained);
				}

				if (SingularityConfig.useRingSliceMode()) {
					boolean columnComplete = collectRingSlice(world,
							chunk,
							task,
							cleared,
							centerSupplier.get());
					return ColumnWorkResult.slice(task, task.currentY(), columnComplete, cleared, drained, false);
				}

				if (SingularityConfig.useRingSliceChunkMode()) {
					collectChunkVolume(world, chunk, task, cleared, SingularityConfig.respectProtectedBlocks());
					return ColumnWorkResult.slice(task, task.maxY(), true, cleared, drained, true);
				}

				return ColumnWorkResult.slice(task, task.currentY(), true, cleared, drained, false);
			} catch (IllegalStateException ex) {
				return ColumnWorkResult.missing(task);
			}
		} finally {
			SingularityChunkContext.popChunkBypass(task.chunk());
		}
	}

	private WorldChunk getLoadedChunk(ServerWorld world, ChunkPos pos) {
		try {
			Chunk chunk = world.getChunkManager().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
			return chunk instanceof WorldChunk worldChunk ? worldChunk : null;
		} catch (IllegalStateException ex) {
			return null;
		}
	}

	private boolean allowOutsideBorder(ServerWorld world) {
		return SingularityConfig.allowOutsideBorderLoad()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD);
	}

	private boolean isOutsideBorder(ServerWorld world, ChunkPos pos) {
		WorldBorder border = world.getWorldBorder();
		return !border.contains(pos.getCenterX(), pos.getCenterZ());
	}

	private void collectFluidsAhead(WorldChunk chunk,
			SingularityDestructionEngine.ColumnTask task,
			int baseY,
			List<BlockPos> drained) {
		int offset = SingularityConfig.waterDrainOffset();
		if (offset <= 0) {
			return;
		}
		int topY = Math.min(task.maxY(), baseY + offset);
		if (topY <= baseY) {
			return;
		}
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int y = baseY + 1; y <= topY; y++) {
			for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
				for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
					mutable.set(x, y, z);
					BlockState state = chunk.getBlockState(mutable);
					if (!state.getFluidState().isEmpty()
							&& !state.isOf(ModBlocks.VIRUS_BLOCK)
							&& !state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
						drained.add(mutable.toImmutable());
					}
				}
			}
		}
	}

	private void collectChunkVolume(ServerWorld world,
			WorldChunk chunk,
			SingularityDestructionEngine.ColumnTask task,
			List<BlockPos> cleared,
			boolean respectProtected) {
		ChunkPos chunkPos = chunk.getPos();
		BlockBox chunkBox = new BlockBox(chunkPos.getStartX(),
				task.minY(),
				chunkPos.getStartZ(),
				chunkPos.getEndX(),
				task.maxY(),
				chunkPos.getEndZ());
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int y = task.minY(); y <= task.maxY(); y++) {
			collectLayer(world, chunk, chunkPos, y, mutable, cleared, respectProtected, chunkBox);
		}
	}

	private void collectLayer(ServerWorld world,
			WorldChunk chunk,
			ChunkPos chunkPos,
			int y,
			BlockPos.Mutable mutable,
			List<BlockPos> cleared,
			boolean respectProtected,
			BlockBox box) {
		for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
			for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
				mutable.set(x, y, z);
				BlockState state = chunk.getBlockState(mutable);
				if (shouldSkipState(state, world, mutable, respectProtected)) {
					continue;
				}
				if (!BulkFillHelper.shouldFillShape(x,
						y,
						z,
						box.getMinX(),
						box.getMinY(),
						box.getMinZ(),
						box.getMaxX(),
						box.getMaxY(),
						box.getMaxZ(),
						SingularityConfig.bulkFillShape())) {
					continue;
				}
				cleared.add(mutable.toImmutable());
			}
		}
	}

	private boolean collectRingSlice(ServerWorld world,
			WorldChunk chunk,
			SingularityDestructionEngine.ColumnTask task,
			List<BlockPos> cleared,
			@Nullable BlockPos center) {
		int depth = task.ringSliceDepth();
		if (depth < 0) {
			return true;
		}
		ChunkPos chunkPos = chunk.getPos();
		SingularityRingSlices.SliceFacing facing = SingularityRingSlices.resolve(chunkPos, center);
		BlockBox sliceBox = SingularityRingSlices.boundsForSlice(chunkPos,
				facing,
				depth,
				task.minY(),
				task.maxY());
		if (sliceBox != null
				&& SingularityConfig.bulkFillShape() == SingularityConfig.FillShape.OUTLINE
				&& SingularityConfig.outlineThickness() > 1) {
			sliceBox = SingularityRingSlices.expandForOutline(sliceBox, chunkPos, facing, SingularityConfig.outlineThickness());
		}
		if (sliceBox == null) {
			return depth >= SingularityRingSlices.SLICE_COUNT - 1;
		}
		int[] columns = SingularityRingSlices.columnsForSlice(facing, depth);
		if (columns.length == 0) {
			return depth >= SingularityRingSlices.SLICE_COUNT - 1;
		}
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		boolean respectProtected = SingularityConfig.respectProtectedBlocks();
		for (int code : columns) {
			int localX = code & (CHUNK_SIZE - 1);
			int localZ = (code >> 4) & (CHUNK_SIZE - 1);
			int worldX = chunkPos.getStartX() + localX;
			int worldZ = chunkPos.getStartZ() + localZ;
			collectColumn(world,
					chunk,
					worldX,
					worldZ,
					task.minY(),
					task.maxY(),
					task.step(),
					mutable,
					cleared,
					respectProtected,
					sliceBox);
		}
		return depth >= SingularityRingSlices.SLICE_COUNT - 1;
	}

	private void collectColumn(ServerWorld world,
			WorldChunk chunk,
			int worldX,
			int worldZ,
			int minY,
			int maxY,
			int step,
			BlockPos.Mutable mutable,
			List<BlockPos> cleared,
			boolean respectProtected,
			BlockBox box) {
		int direction = step >= 0 ? 1 : -1;
		int currentY = direction > 0 ? minY : maxY;
		while (true) {
			mutable.set(worldX, currentY, worldZ);
			BlockState state = chunk.getBlockState(mutable);
			if (shouldSkipState(state, world, mutable, respectProtected)) {
				// skip
			} else if (BulkFillHelper.shouldFillShape(worldX,
					currentY,
					worldZ,
					box.getMinX(),
					box.getMinY(),
					box.getMinZ(),
					box.getMaxX(),
					box.getMaxY(),
					box.getMaxZ(),
					SingularityConfig.bulkFillShape())) {
				cleared.add(mutable.toImmutable());
			}
			if ((direction > 0 && currentY >= maxY) || (direction < 0 && currentY <= minY)) {
				break;
			}
			currentY += direction;
		}
	}

	private boolean shouldSkipState(BlockState state, ServerWorld world, BlockPos pos, boolean respectProtected) {
		if (state.isAir()) {
			return true;
		}
		if (state.isOf(ModBlocks.VIRUS_BLOCK) || state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
			return true;
		}
		if (!respectProtected) {
			return false;
		}
		return state.getHardness(world, pos) < 0.0F;
	}

	private static final class CollapseThreadFactory implements ThreadFactory {
		private int index;

		@Override
		public synchronized Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Singularity-Collapse-" + index++);
			thread.setDaemon(true);
			return thread;
		}
	}
}
