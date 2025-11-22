package net.cyberpunk042.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;

public class InfectiousCubeBlock extends Block {
	public static final MapCodec<InfectiousCubeBlock> CODEC = createCodec(InfectiousCubeBlock::new);

	public InfectiousCubeBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

