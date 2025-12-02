package net.cyberpunk042.mixin;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity {
	protected ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "tickMovement", at = @At("TAIL"))
	private void theVirusBlock$extinguishDuringInfection(CallbackInfo ci) {
		if (!(getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		if (VirusWorldState.get(serverWorld).infectionState().infected() && this.isOnFire()) {
			this.setFireTicks(0);
		}
	}
}

