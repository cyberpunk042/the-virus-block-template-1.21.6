package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

public class CorruptedDiamondBlock extends Block {
	public static final MapCodec<CorruptedDiamondBlock> CODEC = createCodec(CorruptedDiamondBlock::new);

	public CorruptedDiamondBlock(AbstractBlock.Settings settings) {
		super(settings.allowsSpawning((state, world, pos, entityType) -> false)
				.requiresTool()
				.strength(3.0F, 8.0F)
				.sounds(net.minecraft.sound.BlockSoundGroup.METAL));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

