package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

public class CorruptedGoldBlock extends Block {
	public static final MapCodec<CorruptedGoldBlock> CODEC = createCodec(CorruptedGoldBlock::new);

	public CorruptedGoldBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

