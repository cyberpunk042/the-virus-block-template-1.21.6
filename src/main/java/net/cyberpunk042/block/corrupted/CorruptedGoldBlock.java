package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

public class CorruptedGoldBlock extends Block {
	public static final MapCodec<CorruptedGoldBlock> CODEC = createCodec(CorruptedGoldBlock::new);

	public CorruptedGoldBlock(AbstractBlock.Settings settings) {
		super(settings.allowsSpawning((state, world, pos, entityType) -> false)
				.requiresTool()
				.strength(1.5F, 6.0F)
				.sounds(net.minecraft.sound.BlockSoundGroup.METAL));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

