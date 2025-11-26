package net.cyberpunk042.mixin;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
abstract class MobEntityMixin extends LivingEntity {
	private MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void thevirus$blockAllyTargets(LivingEntity target, CallbackInfo ci) {
		if (target == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		if (serverWorld.getGameRules().getBoolean(TheVirusBlock.VIRUS_MOB_FRIENDLY_FIRE)) {
			return;
		}
		MobEntity self = (MobEntity) (Object) this;
		if (VirusMobAllyHelper.isAlly(self) && VirusMobAllyHelper.isAlly(target)) {
			ci.cancel();
		}
	}
}

