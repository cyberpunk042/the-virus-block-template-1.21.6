package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

public class CorruptedDiamondBlock extends Block {
	public static final MapCodec<CorruptedDiamondBlock> CODEC = createCodec(CorruptedDiamondBlock::new);

	public CorruptedDiamondBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

