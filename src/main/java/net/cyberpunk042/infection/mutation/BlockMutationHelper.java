package net.cyberpunk042.infection.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public final class BlockMutationHelper {
	private BlockMutationHelper() {
	}

	public static void mutateAroundSources(ServerWorld world, Set<BlockPos> sources, InfectionTier tier, boolean apocalypseMode) {
		Random random = world.getRandom();
		boolean mutated = false;

		List<ServerPlayerEntity> players = world.getPlayers(ServerPlayerEntity::isAlive);
		if (!players.isEmpty()) {
			List<BlockPos> playerAnchors = players.stream().map(ServerPlayerEntity::getBlockPos).toList();
			int attemptsRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_PLAYER_ATTEMPTS), 0, 4096);
			int radiusRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_PLAYER_RADIUS), 4, 256);
			mutated |= mutateFromAnchors(world, playerAnchors, attemptsRule, radiusRule, tier, apocalypseMode, random);
		}

		if (!sources.isEmpty()) {
			List<BlockPos> sourceAnchors = new ArrayList<>(sources);
			int attemptsRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_SOURCE_ATTEMPTS), 0, 4096);
			int radiusRule = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SPREAD_SOURCE_RADIUS), 4, 256);
			mutated |= mutateFromAnchors(world, sourceAnchors, attemptsRule, radiusRule, tier, apocalypseMode, random);
		}

		if (!mutated && !sources.isEmpty()) {
			mutateFromAnchors(world, new ArrayList<>(sources), 32, 16, tier, apocalypseMode, random);
		}
	}

	private static boolean mutateFromAnchors(ServerWorld world, List<BlockPos> anchors, int attemptsRule, int radiusRule, InfectionTier tier, boolean apocalypseMode, Random random) {
		if (anchors.isEmpty() || attemptsRule <= 0 || radiusRule <= 0) {
			return false;
		}

		int attempts = scaleAttempts(attemptsRule, tier, apocalypseMode);
		int radius = scaleRadius(radiusRule, tier, apocalypseMode);
		boolean mutated = false;
		for (int i = 0; i < attempts; i++) {
			BlockPos origin = anchors.get(random.nextInt(anchors.size()));
			BlockPos target = randomOffset(world, origin, radius, random);
			if (target == null) {
				continue;
			}
			mutateBlock(world, target, random, tier, apocalypseMode);
			mutated = true;
		}
		return mutated;
	}

	private static int scaleAttempts(int base, InfectionTier tier, boolean apocalypseMode) {
		int attempts = base + Math.max(1, base / 4) * tier.getIndex();
		if (apocalypseMode) {
			attempts += base;
		}
		return Math.min(4096, attempts);
	}

	private static int scaleRadius(int base, InfectionTier tier, boolean apocalypseMode) {
		int radius = base + tier.getIndex() * Math.max(2, base / 8);
		if (apocalypseMode) {
			radius += 8;
		}
		return MathHelper.clamp(radius, 4, 256);
	}

	@Nullable
	private static BlockPos randomOffset(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int attempt = 0; attempt < 5; attempt++) {
			int x = origin.getX() + random.nextBetween(-radius, radius);
			int y = origin.getY() + random.nextBetween(-radius, radius);
			int z = origin.getZ() + random.nextBetween(-radius, radius);

			BlockPos horizontal = new BlockPos(x, world.getBottomY(), z);
			if (!isChunkLoaded(world, horizontal)) {
				continue;
			}

			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			int clampedY = MathHelper.clamp(y, world.getBottomY(), Math.max(world.getBottomY(), topY));
			return new BlockPos(x, clampedY, z);
		}

		return null;
	}

	private static void mutateBlock(ServerWorld world, BlockPos pos, Random random, InfectionTier tier, boolean apocalypseMode) {
		if (!isChunkLoaded(world, pos)) {
			return;
		}

		BlockState original = world.getBlockState(pos);
		if (original.isAir()) {
			return;
		}

		if (original.isOf(ModBlocks.VIRUS_BLOCK)
				|| original.isOf(ModBlocks.INFECTED_BLOCK)
				|| original.isOf(ModBlocks.INFECTIOUS_CUBE)
				|| original.isOf(ModBlocks.BACTERIA)) {
			return;
		}

		BlockState replacement = pickReplacement(original, random);
		if (replacement == null) {
			return;
		}
		replacement = applyStage(replacement, tier);

		if (apocalypseMode && random.nextFloat() < 0.1F) {
			world.breakBlock(pos, false);
			return;
		}

		world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
	}

	private static BlockState pickReplacement(BlockState original, Random random) {
		if (original.isIn(BlockTags.LOGS) || original.isIn(BlockTags.PLANKS)) {
			return ModBlocks.CORRUPTED_WOOD.getDefaultState();
		}

		if (original.isOf(Blocks.GRASS_BLOCK)) {
			return ModBlocks.INFECTED_GRASS.getDefaultState();
		}

		if (original.isIn(BlockTags.DIRT) || original.isOf(Blocks.FARMLAND) || original.isOf(Blocks.PODZOL)) {
			return ModBlocks.CORRUPTED_DIRT.getDefaultState();
		}

		if (isGlassLike(original) || original.isIn(BlockTags.IMPERMEABLE)) {
			return ModBlocks.CORRUPTED_GLASS.getDefaultState();
		}

		if (original.isIn(BlockTags.ICE)) {
			return ModBlocks.CORRUPTED_ICE.getDefaultState();
		}

		if (original.isIn(BlockTags.BASE_STONE_OVERWORLD) || original.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
			return ModBlocks.CORRUPTED_STONE.getDefaultState();
		}

		if (original.isOf(Blocks.IRON_BLOCK) || original.isOf(Blocks.ANVIL)) {
			return ModBlocks.CORRUPTED_IRON.getDefaultState();
		}

		if (original.isOf(Blocks.OBSIDIAN) || original.isOf(Blocks.CRYING_OBSIDIAN)) {
			return ModBlocks.CORRUPTED_CRYING_OBSIDIAN.getDefaultState();
		}

		if (original.isOf(Blocks.DIAMOND_BLOCK) || original.isIn(BlockTags.DIAMOND_ORES)) {
			return ModBlocks.CORRUPTED_DIAMOND.getDefaultState();
		}

		if (original.isOf(Blocks.GOLD_BLOCK) || original.isIn(BlockTags.GOLD_ORES)) {
			return ModBlocks.CORRUPTED_GOLD.getDefaultState();
		}

		if (original.isIn(BlockTags.LEAVES)) {
			return Blocks.SCULK.getDefaultState();
		}

		return switch (random.nextInt(8)) {
			case 0 -> ModBlocks.CORRUPTED_STONE.getDefaultState();
			case 1 -> ModBlocks.CORRUPTED_DIRT.getDefaultState();
			case 2 -> ModBlocks.CORRUPTED_WOOD.getDefaultState();
			case 3 -> ModBlocks.CORRUPTED_GLASS.getDefaultState();
			case 4 -> ModBlocks.CORRUPTED_IRON.getDefaultState();
			case 5 -> ModBlocks.CORRUPTED_CRYING_OBSIDIAN.getDefaultState();
			case 6 -> ModBlocks.CORRUPTED_DIAMOND.getDefaultState();
			default -> ModBlocks.CORRUPTED_GOLD.getDefaultState();
		};
	}

	public static void corruptFlora(ServerWorld world, BlockPos origin, int radius) {
		Random random = world.getRandom();
		for (int i = 0; i < 32; i++) {
			BlockPos target = randomOffset(world, origin, radius, random);
			if (target == null) {
				continue;
			}
			if (!isChunkLoaded(world, target)) {
				continue;
			}

			BlockState state = world.getBlockState(target);
			if (state.isOf(ModBlocks.INFECTED_BLOCK) || state.isOf(ModBlocks.INFECTIOUS_CUBE) || state.isOf(ModBlocks.BACTERIA)) {
				continue;
			}
			if (state.isIn(BlockTags.SAPLINGS) || state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.FERN) || state.isOf(Blocks.TALL_GRASS)) {
				world.setBlockState(target, ModBlocks.CORRUPTED_WOOD.getDefaultState(), Block.NOTIFY_ALL);
			} else if (state.isIn(BlockTags.LEAVES)) {
				world.setBlockState(target, Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}

	private static boolean isGlassLike(BlockState state) {
		return state.isOf(Blocks.GLASS)
				|| state.isOf(Blocks.TINTED_GLASS)
				|| state.getBlock() instanceof TransparentBlock
				|| state.getBlock() instanceof PaneBlock;
	}

	private static boolean isChunkLoaded(ServerWorld world, BlockPos pos) {
		return world.isChunkLoaded(ChunkPos.toLong(pos));
	}

	private static BlockState applyStage(BlockState state, InfectionTier tier) {
		CorruptionStage stage = tier.getIndex() >= 2 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			return state.with(CorruptedStoneBlock.STAGE, stage);
		}
		if (state.contains(CorruptedGlassBlock.STAGE)) {
			return state.with(CorruptedGlassBlock.STAGE, stage);
		}
		return state;
	}
}

