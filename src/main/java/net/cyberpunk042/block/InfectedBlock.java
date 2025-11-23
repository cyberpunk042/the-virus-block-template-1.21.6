package net.cyberpunk042.block;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.BoobytrapHelper.Type;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class InfectedBlock extends Block {
	public static final MapCodec<InfectedBlock> CODEC = createCodec(InfectedBlock::new);

	public InfectedBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int chance = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_INFECTED_BLOCK_EXPLODE_CHANCE), 0, 1000);
		if (chance <= 0) {
			return;
		}
		if (random.nextInt(1000) < chance) {
			BoobytrapHelper.triggerExplosion(world, pos, Type.INFECTED_BLOCK, "spontaneous");
			world.breakBlock(pos, false);
		}
	}
}

