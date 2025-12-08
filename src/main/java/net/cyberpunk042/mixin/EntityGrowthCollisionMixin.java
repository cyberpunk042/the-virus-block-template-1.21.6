package net.cyberpunk042.mixin;


import net.cyberpunk042.log.Logging;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * Diagnostic mixin for the Yarn-mapped `Entity` class.
 * None of its injections are required for collision correctness;
 * they simply log and mirror vanilla collision state so we can
 * verify that dev builds still hit the same call sites as runtime jars.
 */
@Mixin(Entity.class)
public abstract class EntityGrowthCollisionMixin {
	static {
		if (GrowthCollisionDebug.isEnabled()) {
			Logging.CALLBACKS.info("[GrowthCollision] Entity mixin initialized");
		}
	}

	@Inject(
		method = "findCollisionsForMovement(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/util/math/Box;)Ljava/util/List;",
		at = @At("RETURN"),
		cancellable = true
	)
	/**
	 * Instrumentation only: mirrors the runtime/intermediary hook so we can
	 * confirm the Yarn mapping still points at the correct helper.
	 */
	private static void theVirusBlock$appendGrowthCollisions(
			@Nullable Entity entity,
			World world,
			List<VoxelShape> original,
			Box queryBox,
			CallbackInfoReturnable<List<VoxelShape>> cir) {
		if (!world.isClient && GrowthCollisionDebug.shouldLog(entity)) {
			PlayerEntity player = (PlayerEntity) entity;
			Logging.CALLBACKS.info(
					"[GrowthCollision:hook] yarn entity={} query={}",
					player.getGameProfile().getName(),
					queryBox);
		}
		GrowthCollisionMixinHelper.appendGrowthCollisions(entity, world, original, queryBox, cir);
	}

	@Inject(
		method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
		at = @At("HEAD")
	)
	/**
	 * Instrumentation only: logs head/tail collisions for movement sweeps,
	 * useful when checking odd offsets or glide edge cases.
	 */
	private static void theVirusBlock$logPlayerMovementSweep(
			@Nullable Entity entity,
			Vec3d movement,
			Box queryBox,
			World world,
			List<VoxelShape> vanilla,
			CallbackInfoReturnable<Vec3d> cir) {
		if (!GrowthCollisionDebug.isEnabled()) {
			return;
		}
		if (entity instanceof ServerPlayerEntity player) {
			Logging.CALLBACKS.info(
					"[GrowthCollision:sweep] player={} movement={} query={} baseCollisions={}",
					player.getGameProfile().getName(),
					movement,
					queryBox,
					vanilla != null ? vanilla.size() : -1);
		} else if (entity instanceof PlayerEntity player && world.isClient) {
			Logging.CALLBACKS.info(
					"[GrowthCollision:sweep-client] player={} movement={} query={}",
					player.getGameProfile().getName(),
					movement,
					queryBox);
		}
	}

	@Inject(
		method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
		at = @At("HEAD")
	)
	/**
	 * Instrumentation only: logs every server-side player `move` invocation
	 * so we can correlate bounding boxes with sweep logs.
	 */
	private void theVirusBlock$logEntityMove(MovementType type, Vec3d movement, CallbackInfo ci) {
		if (!GrowthCollisionDebug.isEnabled()) {
			return;
		}
		Entity self = (Entity) (Object) this;
		if (self instanceof ServerPlayerEntity player) {
			Box bbox = player.getBoundingBox();
			Logging.CALLBACKS.info(
					"[GrowthCollision:move] player={} type={} movement={} bbStart={} bbEnd={}",
					player.getGameProfile().getName(),
					type,
					movement,
					bbox.getMinPos(),
					bbox.getMaxPos());
		}
	}
}

