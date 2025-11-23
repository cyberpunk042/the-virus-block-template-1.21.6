package net.cyberpunk042.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;

public class CuredInfectiousCubeBlock extends Block {
	public static final MapCodec<CuredInfectiousCubeBlock> CODEC = createCodec(CuredInfectiousCubeBlock::new);

	public CuredInfectiousCubeBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

