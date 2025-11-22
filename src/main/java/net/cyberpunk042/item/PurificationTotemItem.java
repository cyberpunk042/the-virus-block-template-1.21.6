package net.cyberpunk042.item;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class PurificationTotemItem extends Item {
	public PurificationTotemItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			VirusWorldState state = VirusWorldState.get(serverWorld);
			if (state.isInfected()) {
				state.applyPurification(20 * 120L);
				world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 0.5F);
				user.sendMessage(Text.translatable("item.the-virus-block.purification_totem.used"), true);
				if (!user.getAbilities().creativeMode) {
					stack.decrement(1);
				}
				return ActionResult.SUCCESS;
			}
		}

		return ActionResult.PASS;
	}
}

