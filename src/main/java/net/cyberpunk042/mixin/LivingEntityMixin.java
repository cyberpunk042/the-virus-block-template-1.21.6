package net.cyberpunk042.mixin;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.singularity.SingularityManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	private LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$adjustDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (source.isIn(DamageTypeTags.IS_FIRE) && self instanceof PlayerEntity player && player.isInLava()) {
			if (VirusWorldState.get(world).getCurrentTier().getIndex() >= 2) {
				player.extinguish();
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, false, true));
				cir.setReturnValue(false);
				return;
			}
		}

		Entity attackerEntity = source.getAttacker();
		if (!(attackerEntity instanceof LivingEntity attacker)) {
			return;
		}

		if (attacker.getWorld() != self.getWorld()) {
			return;
		}

		if (SingularityManager.shouldPreventFriendlyFire(world, attacker, self)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "baseTick", at = @At("TAIL"))
	private void theVirusBlock$lavaBlessing(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof PlayerEntity player) || !(player.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		int tier = VirusWorldState.get(serverWorld).getCurrentTier().getIndex();
		if (tier >= 2 && player.isInLava()) {
			player.extinguish();
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, false, true));
		}

		if (tier >= 1 && (player.isTouchingWater() || player.isSubmergedInWater())) {
			// Water no longer degrades armor; feature temporarily disabled while investigating lag.
		}
	}
}


