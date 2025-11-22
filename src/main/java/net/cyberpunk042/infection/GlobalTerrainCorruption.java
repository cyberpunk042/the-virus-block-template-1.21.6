package net.cyberpunk042.infection;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

public final class GlobalTerrainCorruption {

	private GlobalTerrainCorruption() {
	}

	public static void init() {
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_CHUNK_REWRITE_ON_LOAD)) {
				return;
			}
			VirusWorldState state = VirusWorldState.get(world);
			if (state.isTerrainGloballyCorrupted()) {
				corruptChunk(world, chunk);
			}
			if (state.isCleansingActive()) {
				cleanseChunk(world, chunk);
			}
		});
	}

	public static void trigger(ServerWorld world, BlockPos origin) {
		VirusWorldState state = VirusWorldState.get(world);
		if (!state.enableTerrainCorruption()) {
			return;
		}

		ChunkPos originChunk = new ChunkPos(origin);
		corruptChunk(world, world.getChunk(originChunk.x, originChunk.z));
		for (ServerPlayerEntity player : world.getPlayers()) {
			ChunkPos pos = new ChunkPos(player.getBlockPos());
			corruptChunk(world, world.getChunk(pos.x, pos.z));
		}
	}

	private static BlockState convert(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.GRASS_BLOCK) {
			return ModBlocks.INFECTED_GRASS.getDefaultState();
		}
		if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT || block == Blocks.DIRT_PATH || block == Blocks.FARMLAND) {
			return ModBlocks.CORRUPTED_DIRT.getDefaultState();
		}
		if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
			return ModBlocks.CORRUPTED_STONE.getDefaultState();
		}
		return null;
	}

	public static void cleanse(ServerWorld world) {
		VirusWorldState.get(world).beginCleansing();
		for (ServerPlayerEntity player : world.getPlayers()) {
			ChunkPos pos = new ChunkPos(player.getBlockPos());
			cleanseChunk(world, world.getChunk(pos.x, pos.z));
		}
		ChunkPos spawn = new ChunkPos(new BlockPos(0, world.getSeaLevel(), 0));
		cleanseChunk(world, world.getChunk(spawn.x, spawn.z));
	}

	private static BlockState cleanseBlock(BlockState state) {
		Block block = state.getBlock();
		if (block == ModBlocks.VIRUS_BLOCK || block == ModBlocks.MATRIX_CUBE) {
			return Blocks.AIR.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_DIRT || block == ModBlocks.INFECTED_BLOCK || block == ModBlocks.INFECTIOUS_CUBE) {
			return Blocks.DIRT.getDefaultState();
		}
		if (block == ModBlocks.INFECTED_GRASS) {
			return Blocks.GRASS_BLOCK.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_WOOD) {
			return Blocks.OAK_PLANKS.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_STONE || block == ModBlocks.CORRUPTED_IRON || block == ModBlocks.CORRUPTED_GLASS
				|| block == ModBlocks.CORRUPTED_CRYING_OBSIDIAN || block == ModBlocks.CORRUPTED_DIAMOND
				|| block == ModBlocks.CORRUPTED_GOLD) {
			return Blocks.STONE.getDefaultState();
		}
		if (block == ModBlocks.CORRUPTED_ICE) {
			return Blocks.ICE.getDefaultState();
		}
		return null;
	}

	private static void corruptChunk(ServerWorld world, Chunk chunk) {
		if (!(chunk instanceof WorldChunk worldChunk)) {
			return;
		}
		Mutable mutable = new Mutable();
		int minY = world.getBottomY();
		int maxY = world.getBottomY() + world.getDimension().height();
		int startX = worldChunk.getPos().getStartX();
		int startZ = worldChunk.getPos().getStartZ();
		int conversions = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y < maxY; y++) {
					mutable.set(startX + x, y, startZ + z);
					BlockState replacement = convert(worldChunk.getBlockState(mutable));
					if (replacement != null) {
						worldChunk.setBlockState(mutable, replacement, Block.NOTIFY_LISTENERS);
						conversions++;
					}
				}
			}
		}
		CorruptionProfiler.logChunkRewrite(world, worldChunk.getPos(), conversions, false);
	}

	private static void cleanseChunk(ServerWorld world, Chunk chunk) {
		if (!(chunk instanceof WorldChunk worldChunk)) {
			return;
		}
		Mutable mutable = new Mutable();
		int minY = world.getBottomY();
		int maxY = world.getBottomY() + world.getDimension().height();
		int startX = worldChunk.getPos().getStartX();
		int startZ = worldChunk.getPos().getStartZ();
		int conversions = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y < maxY; y++) {
					mutable.set(startX + x, y, startZ + z);
					BlockState replacement = cleanseBlock(worldChunk.getBlockState(mutable));
					if (replacement != null) {
						worldChunk.setBlockState(mutable, replacement, Block.NOTIFY_LISTENERS);
						conversions++;
					}
				}
			}
		}
		CorruptionProfiler.logChunkRewrite(world, worldChunk.getPos(), conversions, true);
	}

}

