package net.cyberpunk042.collision;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.block.entity.GrowthCollisionTracker;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public final class GrowthCollisionMixinHelper {
	private static final int DEBUG_BOX_LIMIT = 4;
	private static final double EPS_FORMAT_SCALE = 1.0E6D;
	private static final double SCENARIO_VERTICAL_EPSILON = 0.03125D;
	private static final double SCENARIO_AXIS_EPSILON = 1.0E-4D;

	private GrowthCollisionMixinHelper() {
	}

	public static void appendGrowthCollisions(
			@Nullable Entity entity,
			World world,
			List<VoxelShape> vanillaCollisions,
			Box queryBox,
			CallbackInfoReturnable<List<VoxelShape>> cir) {
		List<VoxelShape> extras = gatherGrowthCollisions(entity, world, queryBox, vanillaCollisions);
		if (extras.isEmpty()) {
			return;
		}
		List<VoxelShape> combined = new ArrayList<>(cir.getReturnValue());
		combined.addAll(extras);
		cir.setReturnValue(combined);
	}

	public static List<VoxelShape> gatherGrowthCollisions(
			@Nullable Entity entity,
			World world,
			Box queryBox,
			@Nullable Iterable<VoxelShape> vanillaCollisions) {
		if (entity == null || world == null || world.isClient) {
			return Collections.emptyList();
		}

		Collection<ProgressiveGrowthBlockEntity> actives = GrowthCollisionTracker.active(world);
		if (actives.isEmpty()) {
			return Collections.emptyList();
		}

		List<VoxelShape> extras = new ArrayList<>();
		int appended = 0;
		int debugBoxesLogged = 0;
		EnumSet<CollisionScenario> scenariosLogged = EnumSet.noneOf(CollisionScenario.class);
		for (ProgressiveGrowthBlockEntity growth : actives) {
			if (growth == null || growth.isRemoved() || growth.getWorld() != world || !growth.hasCollision()) {
				continue;
			}
			List<Box> panelBoxes = growth.getCollisionPanels();
			boolean usedPanels = panelBoxes != null && !panelBoxes.isEmpty();
			VoxelShape worldShape = growth.worldShape(ProgressiveGrowthBlock.ShapeType.COLLISION);
			if (!usedPanels && (worldShape == null || worldShape.isEmpty())) {
				continue;
			}
			Iterable<Box> boxes = usedPanels ? panelBoxes : worldShape.getBoundingBoxes();
			for (Box localBox : boxes) {
				Box worldBox = usedPanels ? localBox : localBox.offset(growth.getPos());
				boolean intersects = worldBox.intersects(queryBox);
				debugBoxesLogged += logBox(entity, growth, worldBox, queryBox, intersects, debugBoxesLogged, scenariosLogged);
				if (!intersects) {
					continue;
				}
				extras.add(VoxelShapes.cuboid(worldBox));
				appended++;
			}
		}

		if (appended > 0) {
			logCollision(entity, actives.size(), appended, queryBox);
		} else {
			logNoCollision(entity, actives.size(), queryBox);
		}
		return extras;
	}

	private static void logCollision(Entity entity, int actives, int appended, Box queryBox) {
		if (!GrowthCollisionDebug.shouldLog(entity)) {
			return;
		}
		PlayerEntity player = (PlayerEntity) entity;
		Logging.COLLISION.info(
				"[GrowthCollision] player={} actives={} appended={} query={}",
				player.getName().getString(),
				actives,
				appended,
				queryBox);
	}

	private static void logNoCollision(Entity entity, int actives, Box queryBox) {
		if (!GrowthCollisionDebug.shouldLog(entity)) {
			return;
		}
		PlayerEntity player = (PlayerEntity) entity;
		Logging.COLLISION.info(
				"[GrowthCollision] player={} actives={} appended=0 query={} (no intersections)",
				player.getName().getString(),
				actives,
				queryBox);
	}

	private static int logBox(
			Entity entity,
			ProgressiveGrowthBlockEntity growth,
			Box worldBox,
			Box queryBox,
			boolean intersects,
			int alreadyLogged,
			EnumSet<CollisionScenario> scenariosLogged) {
		if (!GrowthCollisionDebug.shouldLog(entity)) {
			return 0;
		}
		CollisionScenario scenario = classifyScenario(queryBox, worldBox);
		if (alreadyLogged >= DEBUG_BOX_LIMIT && scenariosLogged.contains(scenario)) {
			return 0;
		}
		scenariosLogged.add(scenario);
		PlayerEntity player = (PlayerEntity) entity;
		double overlapX = overlap(queryBox.minX, queryBox.maxX, worldBox.minX, worldBox.maxX);
		double overlapY = overlap(queryBox.minY, queryBox.maxY, worldBox.minY, worldBox.maxY);
		double overlapZ = overlap(queryBox.minZ, queryBox.maxZ, worldBox.minZ, worldBox.maxZ);
		Logging.COLLISION.info(
				"[GrowthCollision:debug] player={} growth={} scenario={} worldBox={} query={} intersects={} overlap=({}, {}, {})",
				player.getName().getString(),
				growth.getPos(),
				scenario,
				worldBox,
				queryBox,
				intersects,
				formatOverlap(overlapX),
				formatOverlap(overlapY),
				formatOverlap(overlapZ));
		return 1;
	}

	private static double overlap(double minA, double maxA, double minB, double maxB) {
		return Math.min(maxA, maxB) - Math.max(minA, minB);
	}

	private static CollisionScenario classifyScenario(Box entityBox, Box worldBox) {
		double aboveGap = entityBox.minY - worldBox.maxY;
		if (aboveGap >= -SCENARIO_VERTICAL_EPSILON) {
			return CollisionScenario.TOP;
		}
		double belowGap = worldBox.minY - entityBox.maxY;
		if (belowGap >= -SCENARIO_VERTICAL_EPSILON) {
			return CollisionScenario.BOTTOM;
		}

		double overlapX = Math.max(0.0D, overlap(entityBox.minX, entityBox.maxX, worldBox.minX, worldBox.maxX));
		double overlapZ = Math.max(0.0D, overlap(entityBox.minZ, entityBox.maxZ, worldBox.minZ, worldBox.maxZ));
		if (overlapX < SCENARIO_AXIS_EPSILON && overlapZ < SCENARIO_AXIS_EPSILON) {
			return CollisionScenario.UNKNOWN;
		}

		double centerX = center(entityBox.minX, entityBox.maxX) - center(worldBox.minX, worldBox.maxX);
		double centerZ = center(entityBox.minZ, entityBox.maxZ) - center(worldBox.minZ, worldBox.maxZ);

		if (Math.abs(centerX) < SCENARIO_AXIS_EPSILON && Math.abs(centerZ) < SCENARIO_AXIS_EPSILON) {
			return CollisionScenario.INSIDE;
		}

		if (Math.abs(centerX) >= Math.abs(centerZ)) {
			return centerX >= 0.0D ? CollisionScenario.SIDE_POS_X : CollisionScenario.SIDE_NEG_X;
		}
		return centerZ >= 0.0D ? CollisionScenario.SIDE_POS_Z : CollisionScenario.SIDE_NEG_Z;
	}

	private static double center(double min, double max) {
		return (min + max) * 0.5D;
	}

	private static double formatOverlap(double value) {
		return Math.round(value * EPS_FORMAT_SCALE) / EPS_FORMAT_SCALE;
	}

	private static int sizeOf(@Nullable Iterable<VoxelShape> vanillaCollisions) {
		if (vanillaCollisions instanceof Collection<?> collection) {
			return collection.size();
		}
		return -1;
	}

	private enum CollisionScenario {
		TOP,
		BOTTOM,
		SIDE_POS_X,
		SIDE_NEG_X,
		SIDE_POS_Z,
		SIDE_NEG_Z,
		INSIDE,
		UNKNOWN
	}
}

