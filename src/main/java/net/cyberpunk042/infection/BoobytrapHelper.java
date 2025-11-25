package net.cyberpunk042.infection;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.item.PurificationTotemUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public final class BoobytrapHelper {

	public enum Type {
		INFECTED_BLOCK,
		INFECTIOUS_CUBE,
		BACTERIA
	}

	public record TrapSelection(Type type, BlockState state) {
	}

	private BoobytrapHelper() {
	}

	@Nullable
	public static TrapSelection selectTrap(ServerWorld world) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED)) {
			return null;
		}
		Random random = world.getRandom();
		int roll = random.nextInt(1000);
		int infectedChance = clampChance(world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_CHANCE_INFECTED));
		if (roll < infectedChance) {
			return new TrapSelection(Type.INFECTED_BLOCK, ModBlocks.INFECTED_BLOCK.getDefaultState());
		}
		roll -= infectedChance;
		int infectiousChance = clampChance(world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_CHANCE_INFECTIOUS));
		if (roll < infectiousChance) {
			return new TrapSelection(Type.INFECTIOUS_CUBE, ModBlocks.INFECTIOUS_CUBE.getDefaultState());
		}
		roll -= infectiousChance;
		int bacteriaChance = clampChance(world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_CHANCE_BACTERIA));
		if (roll < bacteriaChance) {
			return new TrapSelection(Type.BACTERIA, ModBlocks.BACTERIA.getDefaultState());
		}
		return null;
	}

	public static int spread(ServerWorld world, BlockPos origin, Type type, int attempts, int radius) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED)) {
			return 0;
		}
		VirusWorldState state = VirusWorldState.get(world);
		int cappedAttempts = Math.min(512, Math.max(0, attempts));
		int cappedRadius = Math.min(64, Math.max(1, radius));
		Random random = world.getRandom();
		int conversions = 0;
		for (int i = 0; i < cappedAttempts; i++) {
			BlockPos target = randomOffset(world, random, origin, cappedRadius);
			if (target == null || !world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}
			if (state.isShielded(target)) {
				continue;
			}
			BlockState replacement = GlobalTerrainCorruption.convert(world.getBlockState(target));
			if (replacement == null) {
				continue;
			}
			world.setBlockState(target, replacement, Block.NOTIFY_LISTENERS);
			conversions++;
		}
		if (conversions > 0) {
			CorruptionProfiler.logBoobytrapSpread(world, origin, type.name(), conversions);
		}
		return conversions;
	}

	public static void triggerExplosion(ServerWorld world, BlockPos pos, Type type, String reason) {
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED)) {
			return;
		}
		if (VirusWorldState.get(world).isShielded(pos)) {
			return;
		}
		float power = getPower(world, type);
		boolean playersOnly = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BOOBYTRAP_DAMAGE_PLAYERS_ONLY);
		boolean damageBlocks = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_BOOBYTRAP_DAMAGE_BLOCKS);
		int affectedPlayers = 0;

		if (playersOnly || !damageBlocks) {
			double radius = power * 2.5D + 2.0D;
			Box area = new Box(pos).expand(radius);
			List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, area, ServerPlayerEntity::isAlive);
			float damage = Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_PLAYER_DAMAGE));
			for (ServerPlayerEntity player : players) {
				if (PurificationTotemUtil.isHolding(player)) {
					continue;
				}
				player.damage(world, world.getDamageSources().explosion(null, null), damage);
				applyMalus(world, player);
			}
			affectedPlayers = players.size();
			world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		if (!playersOnly && damageBlocks) {
			world.createExplosion(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, power, World.ExplosionSourceType.BLOCK);
		}

		CorruptionProfiler.logBoobytrapTrigger(world, pos, type.name(), reason, affectedPlayers);
	}

	private static int clampChance(int value) {
		return Math.max(0, Math.min(1000, value));
	}

	private static float getPower(ServerWorld world, Type type) {
		return switch (type) {
			case INFECTED_BLOCK -> Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_POWER_INFECTED));
			case INFECTIOUS_CUBE -> Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_POWER_INFECTIOUS));
			case BACTERIA -> Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_BOOBYTRAP_POWER_BACTERIA));
		};
	}

	private static void applyMalus(ServerWorld world, ServerPlayerEntity player) {
		Random random = world.getRandom();
		if (random.nextFloat() > 0.35F) {
			return;
		}
		StatusEffectInstance effect = switch (random.nextInt(4)) {
			case 0 -> new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0);
			case 1 -> new StatusEffectInstance(StatusEffects.POISON, 100, 0);
			case 2 -> new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0);
			default -> new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 1);
		};
		player.addStatusEffect(effect);
	}

	@Nullable
	private static BlockPos randomOffset(ServerWorld world, Random random, BlockPos origin, int radius) {
		if (radius <= 0) {
			return null;
		}
		int x = origin.getX() + random.nextBetween(-radius, radius);
		int top = world.getBottomY() + world.getDimension().height() - 1;
		int y = origin.getY() + random.nextBetween(-radius, radius);
		y = MathHelper.clamp(y, world.getBottomY(), top);
		int z = origin.getZ() + random.nextBetween(-radius, radius);
		return new BlockPos(x, y, z);
	}

	public static void debugList(ServerPlayerEntity player, int radius) {
		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos origin = player.getBlockPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int found = 0;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					BlockState state = world.getBlockState(mutable);
					Type type = trapType(state.getBlock());
					if (type != null) {
						player.sendMessage(Text.literal("Boobytrap " + type.name() + " at " + mutable.toShortString()), false);
						found++;
					}
				}
			}
		}
		if (found == 0) {
			player.sendMessage(Text.literal("No boobytraps within " + radius + " blocks."), false);
		} else {
			player.sendMessage(Text.literal("Total boobytraps found: " + found), false);
		}
	}

	public static BlockPos placeTrap(ServerWorld world, WorldChunk chunk, BlockPos.Mutable original, BlockState originalState, TrapSelection trap) {
		BlockPos target = snapToSurface(world, original, originalState);
		if (VirusWorldState.get(world).isShielded(target)) {
			return target;
		}
		world.setBlockState(target, trap.state(), Block.NOTIFY_LISTENERS);
		world.getChunkManager().markForUpdate(target);
		CorruptionProfiler.logBoobytrapPlacement(world, target, trap.type().name());
		return target;
	}

	public static void applyTrap(ServerWorld world, BlockPos pos, TrapSelection trap) {
		if (VirusWorldState.get(world).isShielded(pos)) {
			return;
		}
		world.setBlockState(pos, trap.state(), Block.NOTIFY_LISTENERS);
		world.getChunkManager().markForUpdate(pos);
		CorruptionProfiler.logBoobytrapPlacement(world, pos, trap.type().name());
	}

	private static BlockPos snapToSurface(ServerWorld world, BlockPos.Mutable original, BlockState originalState) {
		int x = original.getX();
		int z = original.getZ();
		int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		int maxY = world.getBottomY() + world.getDimension().height() - 1;
		if (topY >= world.getBottomY()) {
			int clampedY = MathHelper.clamp(topY, world.getBottomY(), maxY);
			BlockPos surface = new BlockPos(x, clampedY, z);
			BlockState existing = world.getBlockState(surface);
			if (canReplaceSurface(existing)) {
				return surface;
			}
		}
		return original.toImmutable();
	}

	private static boolean isTrap(Block block) {
		return block == ModBlocks.INFECTED_BLOCK || block == ModBlocks.INFECTIOUS_CUBE || block == ModBlocks.BACTERIA;
	}

	public static boolean canReplaceSurface(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		if (block == ModBlocks.VIRUS_BLOCK || block == ModBlocks.MATRIX_CUBE || isTrap(block)
				|| block instanceof BedBlock
				|| block == Blocks.OBSIDIAN
				|| block == Blocks.CRYING_OBSIDIAN
				|| block == Blocks.NETHER_PORTAL
				|| block == Blocks.END_PORTAL
				|| block == Blocks.END_PORTAL_FRAME
				|| block == Blocks.END_GATEWAY
				|| block == Blocks.RESPAWN_ANCHOR) {
			return false;
		}
		return true;
	}

	private static Type trapType(Block block) {
		if (block == ModBlocks.INFECTED_BLOCK) {
			return Type.INFECTED_BLOCK;
		}
		if (block == ModBlocks.INFECTIOUS_CUBE) {
			return Type.INFECTIOUS_CUBE;
		}
		if (block == ModBlocks.BACTERIA) {
			return Type.BACTERIA;
		}
		return null;
	}

}

