package net.cyberpunk042.client.color;

import java.util.Map;

import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;

public final class CorruptedColorProviders {
	private static final Map<Block, Integer> COLOR_MAP = Map.of(
			ModBlocks.CORRUPTED_CRYING_OBSIDIAN, 0x7c44ff,
			ModBlocks.CORRUPTED_DIAMOND, 0x4bffe3,
			ModBlocks.CORRUPTED_GOLD, 0xffc54f
	);

	private CorruptedColorProviders() {
	}

	public static void register() {
		COLOR_MAP.forEach((block, color) -> ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> color, block));
	}
}

