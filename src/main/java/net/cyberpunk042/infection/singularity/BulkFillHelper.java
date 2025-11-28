package net.cyberpunk042.infection.singularity;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.config.SingularityConfig;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * Minimal helper that emulates the vanilla /fill command for collapse purposes.
 * It replaces every block within the provided box with air, respecting
 * protected blocks when requested.
 */
public final class BulkFillHelper {
	private static final int FILL_COMMAND_LIMIT = 32768;

	private BulkFillHelper() {
	}

	public static int clearVolume(ServerWorld world,
			BlockBox box,
			SingularityConfig.CollapseFillMode mode,
			SingularityConfig.FillShape shape,
			boolean respectProtected,
			int updateFlags) {
		if (shouldUseNativeFill(shape, box)) {
			int result = runNativeFill(world, box, mode, null);
			if (result >= 0) {
				return result;
			}
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockState air = Blocks.AIR.getDefaultState();
		int minX = box.getMinX();
		int minY = box.getMinY();
		int minZ = box.getMinZ();
		int maxX = box.getMaxX();
		int maxY = box.getMaxY();
		int maxZ = box.getMaxZ();
		int cleared = 0;
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!shouldFillShape(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, shape)) {
						continue;
					}
					mutable.set(x, y, z);
					BlockState state = world.getBlockState(mutable);
					if (shouldSkip(state)) {
						continue;
					}
					if (respectProtected && state.getHardness(world, mutable) < 0.0F) {
						continue;
					}
					if (mode == SingularityConfig.CollapseFillMode.DESTROY) {
						if (world.breakBlock(mutable, false)) {
							cleared++;
						}
						continue;
					}
					world.setBlockState(mutable, air, updateFlags);
					cleared++;
				}
			}
		}
		return cleared;
	}

	private static boolean shouldUseNativeFill(SingularityConfig.FillShape shape, BlockBox box) {
		if (!SingularityConfig.useNativeFill()) {
			return false;
		}
		if (shape != SingularityConfig.FillShape.MATRIX) {
			return false;
		}
		long volume = boxVolume(box);
		return volume > 0 && volume <= FILL_COMMAND_LIMIT;
	}

	private static int runNativeFill(ServerWorld world,
			BlockBox box,
			SingularityConfig.CollapseFillMode mode,
			@Nullable String replaceFilter) {
		MinecraftServer server = world.getServer();
		if (server == null) {
			return -1;
		}
		ServerCommandSource source = server.getCommandSource()
				.withWorld(world)
				.withPosition(new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()))
				.withRotation(Vec2f.ZERO)
				.withLevel(4)
				.withSilent();
		final String command;
		if (replaceFilter != null && !replaceFilter.isBlank()) {
			command = String.format(Locale.ROOT,
					"fill %d %d %d %d %d %d minecraft:air replace %s",
					box.getMinX(),
					box.getMinY(),
					box.getMinZ(),
					box.getMaxX(),
					box.getMaxY(),
					box.getMaxZ(),
					replaceFilter);
		} else {
			String modeArg = mode == SingularityConfig.CollapseFillMode.DESTROY ? "destroy" : "replace";
			command = String.format(Locale.ROOT,
					"fill %d %d %d %d %d %d minecraft:air %s",
					box.getMinX(),
					box.getMinY(),
					box.getMinZ(),
					box.getMaxX(),
					box.getMaxY(),
					box.getMaxZ(),
					modeArg);
		}
		try {
			server.getCommandManager().executeWithPrefix(source, command);
			return (int) boxVolume(box);
		} catch (Exception ex) {
			return -1;
		}
	}

	public static boolean clearFluidRange(ServerWorld world, BlockBox box, String blockId) {
		if (box == null || blockId == null || blockId.isBlank()) {
			return false;
		}
		if (boxVolume(box) > FILL_COMMAND_LIMIT) {
			return false;
		}
		return runNativeFill(world, box, SingularityConfig.CollapseFillMode.AIR, blockId) >= 0;
	}

	private static long boxVolume(BlockBox box) {
		return (long) (box.getMaxX() - box.getMinX() + 1)
				* (box.getMaxY() - box.getMinY() + 1)
				* (box.getMaxZ() - box.getMinZ() + 1);
	}

	public static boolean shouldFillShape(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ,
			SingularityConfig.FillShape shape) {
		return switch (shape) {
			case MATRIX -> true;
			case COLUMN -> {
				int centerX = (minX + maxX) >> 1;
				int centerZ = (minZ + maxZ) >> 1;
				yield x == centerX && z == centerZ;
			}
			case ROW -> {
				int centerY = (minY + maxY) >> 1;
				int centerZ = (minZ + maxZ) >> 1;
				yield y == centerY && z == centerZ;
			}
			case VECTOR -> shouldFillVector(x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
			case OUTLINE -> outlineMask(x,
					y,
					z,
					minX,
					minY,
					minZ,
					maxX,
					maxY,
					maxZ,
					SingularityConfig.outlineThickness());
		};
	}

	private static boolean shouldFillVector(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ) {
		int sizeX = maxX - minX + 1;
		int sizeY = maxY - minY + 1;
		int sizeZ = maxZ - minZ + 1;
		if (sizeX <= 1) {
			return true;
		}
		if (sizeZ <= 1) {
			return true;
		}
		if (sizeY <= 1) {
			return true;
		}
		if (sizeY >= sizeX && sizeY >= sizeZ) {
			int centerY = (minY + maxY) >> 1;
			return y == centerY;
		}
		if (sizeX >= sizeZ) {
			int centerX = (minX + maxX) >> 1;
			return x == centerX;
		}
		int centerZ = (minZ + maxZ) >> 1;
		return z == centerZ;
	}

	private static boolean shouldSkip(BlockState state) {
		return state.isAir()
				|| state.isOf(ModBlocks.VIRUS_BLOCK)
				|| state.isOf(ModBlocks.SINGULARITY_BLOCK);
	}

	private static boolean outlineMask(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ,
			int thickness) {
		if (thickness <= 0) {
			return x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
		}
		int dx = Math.min(Math.abs(x - minX), Math.abs(maxX - x));
		int dy = Math.min(Math.abs(y - minY), Math.abs(maxY - y));
		int dz = Math.min(Math.abs(z - minZ), Math.abs(maxZ - z));
		return dx < thickness || dy < thickness || dz < thickness;
	}
}

