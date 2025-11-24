package net.cyberpunk042.mixin;

import java.util.List;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.explosion.ExplosionImpl")
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private ServerWorld world;

	@Shadow
	@Final
	private Vec3d pos;

	@Shadow
	@Final
	private float power;

	@Inject(method = "explode", at = @At("TAIL"))
	private void theVirusBlock$handleExplosion(CallbackInfo ci) {
		VirusWorldState state = VirusWorldState.get(world);
		if (!state.isInfected()) {
			return;
		}

		double radius = Math.max(3.0D, power * 6.0F);
		state.handleExplosionImpact(world, pos, radius);
	}
}

