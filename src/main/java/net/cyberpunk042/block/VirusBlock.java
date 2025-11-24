package net.cyberpunk042.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import java.util.function.BiConsumer;

import net.cyberpunk042.block.entity.VirusBlockEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.singularity.SingularityManager;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.text.Text;

public class VirusBlock extends BlockWithEntity {
	private static final int LOCK_MESSAGE_INTERVAL = 20;

	public static final MapCodec<VirusBlock> CODEC = createCodec(VirusBlock::new);

	public VirusBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);

		if (world instanceof ServerWorld serverWorld) {
			VirusWorldState.get(serverWorld).registerSource(serverWorld, pos);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!world.getBlockState(pos).isOf(state.getBlock())) {
			VirusWorldState.get(world).unregisterSource(world, pos);
		}

		super.onStateReplaced(state, world, pos, moved);
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		boolean handledItem = stack.isOf(Items.NETHER_STAR) || stack.isOf(Items.DRAGON_BREATH)
				|| stack.isOf(Items.OBSIDIAN) || stack.isOf(Items.CRYING_OBSIDIAN)
				|| stack.isOf(Items.TOTEM_OF_UNDYING);
		if (!handledItem) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		VirusWorldState infection = VirusWorldState.get(serverWorld);

		if (stack.isOf(Items.NETHER_STAR) || stack.isOf(Items.DRAGON_BREATH)) {
			boolean advanced = infection.forceAdvanceTier(serverWorld);
			if (advanced) {
				world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0F, 0.6F);
			}
		} else if (stack.isOf(Items.OBSIDIAN) || stack.isOf(Items.CRYING_OBSIDIAN)) {
			infection.applyContainmentCharge(stack.isOf(Items.CRYING_OBSIDIAN) ? 2 : 1);
			world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.7F, 0.9F);
		} else if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
			infection.applyPurification(20 * 120L);
		}

		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new VirusBlockEntity(pos, state);
	}

	@Override
	protected void onExploded(BlockState state, ServerWorld world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
		VirusWorldState infection = VirusWorldState.get(world);
		if (SingularityManager.isActive(world)) {
			SingularityManager.onVirusBlockDamage(world, SingularityManager.EXPLOSION_DAMAGE);
			return;
		}

		if (infection.isApocalypseMode()) {
			infection.bleedHealth(world, SingularityManager.EXPLOSION_DAMAGE);
		} else {
			infection.disturbByPlayer(world);
		}
	}

	@Override
	public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		super.onProjectileHit(world, state, hit, projectile);
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		VirusWorldState infection = VirusWorldState.get(serverWorld);
		if (SingularityManager.isActive(serverWorld)) {
			SingularityManager.onVirusBlockDamage(serverWorld, SingularityManager.HIT_DAMAGE);
			return;
		}

		if (infection.isApocalypseMode()) {
			infection.bleedHealth(serverWorld, SingularityManager.HIT_DAMAGE);
		} else {
			infection.disturbByPlayer(serverWorld);
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : validateTicker(type, ModBlockEntities.VIRUS_BLOCK, VirusBlockEntity::tick);
	}

	@Override
	protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
		if (player.getWorld() instanceof ServerWorld serverWorld) {
			VirusWorldState infection = VirusWorldState.get(serverWorld);
			if (SingularityManager.isActive(serverWorld)) {
				SingularityManager.onVirusBlockDamage(serverWorld, SingularityManager.HIT_DAMAGE);
				if (!SingularityManager.canBreakVirusBlock(serverWorld) && !player.isCreative()) {
					return 0.0F;
				}
			} else if (infection.isApocalypseMode()) {
				infection.bleedHealth(serverWorld, SingularityManager.HIT_DAMAGE);
			}

			if (!player.isCreative()) {
				int tierIndex = infection.getCurrentTier().getIndex();
				if (tierIndex < VirusBlockProtection.MIN_SURVIVAL_BREAK_TIER) {
					infection.disturbByPlayer(serverWorld);
					if (player instanceof ServerPlayerEntity serverPlayer) {
						VirusBlockProtection.recordBlockedAttempt(serverPlayer, serverWorld);
					}
					if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer.age % LOCK_MESSAGE_INTERVAL == 0) {
						serverPlayer.sendMessage(Text.translatable("message.the-virus-block.block_locked"), true);
					}
					return 0.0F;
				}

				if (!infection.isSingularitySummoned()
						&& tierIndex >= VirusBlockProtection.MIN_SURVIVAL_BREAK_TIER
						&& player instanceof ServerPlayerEntity serverPlayer
						&& serverPlayer.age % 5 == 0) {
					infection.disturbByPlayer(serverWorld);
				}
			}
		}

		return super.calcBlockBreakingDelta(state, player, world, pos);
	}
}

