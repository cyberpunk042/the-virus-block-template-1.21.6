package net.cyberpunk042.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;

public class InfectedBlock extends Block {
	public static final MapCodec<InfectedBlock> CODEC = createCodec(InfectedBlock::new);

	public InfectedBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

