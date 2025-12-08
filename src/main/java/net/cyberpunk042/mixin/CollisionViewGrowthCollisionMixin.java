package net.cyberpunk042.mixin;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;

/**
 * Support/observability mixin for any `CollisionView` that is also a `World`.
 * Helpful for logging and for catching non-player callers, but not the root fix—
 * the `ServerPlayNetworkHandler` wrap is what enforces collisions server-side.
 */
@Mixin(CollisionView.class)
public interface CollisionViewGrowthCollisionMixin {
	@Inject(
		method = "getCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Vec3d;)Ljava/lang/Iterable;",
		at = @At("RETURN"),
		cancellable = true
	)
	default void theVirusBlock$appendGrowthCollisions(
			@Nullable Entity entity,
			Box queryBox,
			Vec3d movementReference,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		if (entity == null || !(this instanceof World world) || world.isClient) {
			return;
		}
		if (GrowthCollisionDebug.shouldLog(entity)) {
			Logging.CALLBACKS.info("[GrowthCollision:cv-hook] entity={} query={}", entity, queryBox);
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			return;
		}

		List<VoxelShape> merged = new ArrayList<>();
		if (vanilla != null) {
			for (VoxelShape shape : vanilla) {
				merged.add(shape);
			}
		}
		merged.addAll(extras);
		cir.setReturnValue(merged);
	}

	@Inject(
		method = "getBlockCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/lang/Iterable;",
		at = @At("RETURN"),
		cancellable = true
	)
	default void theVirusBlock$appendGrowthBlockCollisions(
			@Nullable Entity entity,
			Box queryBox,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		if (entity == null || !(this instanceof World world) || world.isClient) {
			return;
		}
		if (GrowthCollisionDebug.shouldLog(entity)) {
			Logging.CALLBACKS.info("[GrowthCollision:cv-blockHook] entity={} query={}", entity, queryBox);
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			return;
		}

		List<VoxelShape> merged = new ArrayList<>();
		if (vanilla != null) {
			for (VoxelShape shape : vanilla) {
				merged.add(shape);
			}
		}
		merged.addAll(extras);
		cir.setReturnValue(merged);
	}
}


