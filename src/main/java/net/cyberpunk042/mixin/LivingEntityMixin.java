package net.cyberpunk042.mixin;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModStatusEffects;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	private static final int CORRUPTED_REGEN_DURATION = 80;
	private static final int CORRUPTED_REGEN_REFRESH_THRESHOLD = 20;

	private LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$adjustDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (source.isIn(DamageTypeTags.IS_FIRE) && self instanceof PlayerEntity player && player.isInLava()) {
			VirusWorldState state = VirusWorldState.get(world);
			if (state.tiers().areLiquidsCorrupted(world)) {
				player.extinguish();
				theVirusBlock$grantCorruptedRegen(player);
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
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_MOB_FRIENDLY_FIRE)
				&& self instanceof MobEntity && attacker instanceof MobEntity
				&& VirusMobAllyHelper.isAlly(self) && VirusMobAllyHelper.isAlly(attacker)) {
			((MobEntity) attacker).setTarget(null);
			cir.setReturnValue(false);
			return;
		}
	}

	@ModifyVariable(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
			at = @At("HEAD"), argsOnly = true, index = 3)
	private float theVirusBlock$personalShieldBeamResistance(float amount, ServerWorld world, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (amount <= 0.0F) {
			return amount;
		}
		if (self.hasStatusEffect(ModStatusEffects.PERSONAL_SHIELD)
				&& source.getAttacker() instanceof GuardianEntity guardian
				&& guardian.getCommandTags().contains(TheVirusBlock.VIRUS_DEFENSE_BEAM_TAG)) {
			return amount * 0.25F;
		}
		return amount;
	}

	@Inject(method = "baseTick", at = @At("TAIL"))
	private void theVirusBlock$lavaBlessing(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof PlayerEntity player) || !(player.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		VirusWorldState state = VirusWorldState.get(serverWorld);
		int tier = state.tiers().currentTier().getIndex();
		boolean liquidsCorrupted = state.tiers().areLiquidsCorrupted(serverWorld);
		if (liquidsCorrupted && player.isInLava()) {
			player.extinguish();
			theVirusBlock$grantCorruptedRegen(player);
		}

		if (liquidsCorrupted && (player.isTouchingWater() || player.isSubmergedInWater()) && player instanceof ServerPlayerEntity serverPlayer) {
			theVirusBlock$degradeArmorInWater(serverPlayer, tier, serverWorld);
		}
	}

	private void theVirusBlock$degradeArmorInWater(ServerPlayerEntity player, int tier, ServerWorld world) {
		if (player.age % 20 != 0) {
			return;
		}
		float chance = Math.min(0.95F, 0.2F + tier * 0.05F);
		if (player.getRandom().nextFloat() > chance) {
			return;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmorSlot()) {
				continue;
			}
			ItemStack stack = player.getEquippedStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (isNetheriteArmor(stack)) {
				continue;
			}
			stack.damage(1 + tier, world, player, item -> player.sendEquipmentBreakStatus(item, slot));
		}
	}

	private static boolean isNetheriteArmor(ItemStack stack) {
		return stack.isOf(Items.NETHERITE_HELMET)
				|| stack.isOf(Items.NETHERITE_CHESTPLATE)
				|| stack.isOf(Items.NETHERITE_LEGGINGS)
				|| stack.isOf(Items.NETHERITE_BOOTS);
	}

	private static void theVirusBlock$grantCorruptedRegen(PlayerEntity player) {
		StatusEffectInstance current = player.getStatusEffect(StatusEffects.REGENERATION);
		if (current != null && current.getDuration() > CORRUPTED_REGEN_REFRESH_THRESHOLD) {
			return;
		}
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, CORRUPTED_REGEN_DURATION, 0, false, true));
	}
}


