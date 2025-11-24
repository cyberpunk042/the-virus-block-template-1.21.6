package net.cyberpunk042.client.color;

import java.util.Map;

import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

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
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (!VirusSkyClientState.areFluidsCorrupted()) {
				return 0xFFFFFFFF;
			}
			if (state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) {
				return 0xFF4d0f7a;
			}
			return 0xFFa11a1a;
		}, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE);
	}
}

