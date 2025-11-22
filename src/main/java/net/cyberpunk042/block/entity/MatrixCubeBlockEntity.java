package net.cyberpunk042.block.entity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class MatrixCubeBlockEntity extends BlockEntity {
	private static final Map<ServerWorld, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

	public MatrixCubeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MATRIX_CUBE, pos, state);
	}

	public static void tick(World world, BlockPos pos, BlockState state, MatrixCubeBlockEntity cube) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		BlockPos below = pos.down();
		if (below.getY() <= world.getBottomY()) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			return;
		}

		if (!serverWorld.isChunkLoaded(ChunkPos.toLong(below))) {
			return;
		}

		BlockState targetState = serverWorld.getBlockState(below);
		if (isProtected(targetState)) {
			serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			return;
		}

		serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		serverWorld.setBlockState(below, state, Block.NOTIFY_LISTENERS);
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			ACTIVE.computeIfAbsent(serverWorld, w -> ConcurrentHashMap.newKeySet()).add(getPos().toImmutable());
		}
	}

	@Override
	public void markRemoved() {
		if (this.world instanceof ServerWorld serverWorld) {
			Set<BlockPos> set = ACTIVE.get(serverWorld);
			if (set != null) {
				set.remove(this.pos.toImmutable());
			}
		}
		super.markRemoved();
	}

	public static int getActiveCount(ServerWorld world) {
		return ACTIVE.getOrDefault(world, Set.of()).size();
	}

	public static void destroyAll(ServerWorld world) {
		Set<BlockPos> set = ACTIVE.remove(world);
		if (set == null) {
			return;
		}

		for (BlockPos pos : set) {
			if (world.isChunkLoaded(ChunkPos.toLong(pos))) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}

	private static boolean isProtected(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		return block == ModBlocks.VIRUS_BLOCK
				|| block == ModBlocks.CORRUPTED_STONE
				|| block == ModBlocks.CORRUPTED_IRON
				|| block == ModBlocks.CORRUPTED_CRYING_OBSIDIAN
				|| block == ModBlocks.CORRUPTED_DIAMOND
				|| block == ModBlocks.CORRUPTED_GOLD
				|| block == ModBlocks.CORRUPTED_GLASS
				|| block == ModBlocks.CORRUPTED_DIRT
				|| block == ModBlocks.CORRUPTED_WOOD
				|| block == ModBlocks.CORRUPTED_ICE
				|| block == ModBlocks.INFECTED_BLOCK
				|| block == ModBlocks.INFECTIOUS_CUBE;
	}
}

