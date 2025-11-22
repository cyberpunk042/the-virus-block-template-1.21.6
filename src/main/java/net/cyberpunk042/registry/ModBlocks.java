package net.cyberpunk042.registry;

import java.util.function.Function;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.InfectedBlock;
import net.cyberpunk042.block.InfectedGrassBlock;
import net.cyberpunk042.block.InfectiousCubeBlock;
import net.cyberpunk042.block.MatrixCubeBlock;
import net.cyberpunk042.block.VirusBlock;
import net.cyberpunk042.block.corrupted.CorruptedCryingObsidianBlock;
import net.cyberpunk042.block.corrupted.CorruptedDiamondBlock;
import net.cyberpunk042.block.corrupted.CorruptedDirtBlock;
import net.cyberpunk042.block.corrupted.CorruptedGlassBlock;
import net.cyberpunk042.block.corrupted.CorruptedIceBlock;
import net.cyberpunk042.block.corrupted.CorruptedIronBlock;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptedWoodBlock;
import net.cyberpunk042.block.corrupted.CorruptedGoldBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	public static final VirusBlock VIRUS_BLOCK = register("virus_block", VirusBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.DARK_AQUA)
					.requiresTool()
					.luminance(state -> 6)
					.strength(5.0F, 2000.0F));

	public static final CorruptedStoneBlock CORRUPTED_STONE = register("corrupted_stone", CorruptedStoneBlock::new,
			AbstractBlock.Settings.create()
					.ticksRandomly()
					.mapColor(MapColor.DARK_RED)
					.sounds(BlockSoundGroup.SAND)
					.strength(0.35F, 1.2F));

	public static final CorruptedGlassBlock CORRUPTED_GLASS = register("corrupted_glass", CorruptedGlassBlock::new,
			AbstractBlock.Settings.create()
					.ticksRandomly()
					.mapColor(MapColor.LIGHT_BLUE)
					.nonOpaque()
					.sounds(BlockSoundGroup.GLASS)
					.strength(0.2F, 0.5F));

	public static final CorruptedDirtBlock CORRUPTED_DIRT = register("corrupted_dirt", CorruptedDirtBlock::new,
			AbstractBlock.Settings.copy(Blocks.DIRT)
					.ticksRandomly()
					.mapColor(MapColor.BROWN)
					.sounds(BlockSoundGroup.ROOTED_DIRT)
					.strength(0.5F, 0.8F));

	public static final Block INFECTED_GRASS = register("infected_grass", InfectedGrassBlock::new,
			AbstractBlock.Settings.create()
					.ticksRandomly()
					.mapColor(MapColor.DARK_GREEN)
					.sounds(BlockSoundGroup.GRASS)
					.nonOpaque()
					.strength(0.6F));

	public static final CorruptedWoodBlock CORRUPTED_WOOD = register("corrupted_wood", CorruptedWoodBlock::new,
			AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
					.burnable()
					.mapColor(MapColor.TEAL));

	public static final CorruptedIronBlock CORRUPTED_IRON = register("corrupted_iron", CorruptedIronBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.requiresTool()
					.ticksRandomly()
					.sounds(BlockSoundGroup.NETHERITE)
					.strength(1.8F, 12.0F));

	public static final CorruptedIceBlock CORRUPTED_ICE = register("corrupted_ice", CorruptedIceBlock::new,
			AbstractBlock.Settings.copy(Blocks.ICE)
					.slipperiness(1.2F)
					.nonOpaque()
					.ticksRandomly()
					.mapColor(MapColor.CYAN));

	public static final CorruptedCryingObsidianBlock CORRUPTED_CRYING_OBSIDIAN = register("corrupted_crying_obsidian", CorruptedCryingObsidianBlock::new,
			AbstractBlock.Settings.copy(Blocks.CRYING_OBSIDIAN)
					.mapColor(MapColor.DEEPSLATE_GRAY)
					.luminance(state -> 8)
					.requiresTool()
					.strength(2.2F, 10.0F));

	public static final CorruptedDiamondBlock CORRUPTED_DIAMOND = register("corrupted_diamond", CorruptedDiamondBlock::new,
			AbstractBlock.Settings.copy(Blocks.DIAMOND_BLOCK)
					.mapColor(MapColor.DIAMOND_BLUE)
					.requiresTool()
					.strength(1.5F, 8.0F)
					.luminance(state -> 4));

	public static final CorruptedGoldBlock CORRUPTED_GOLD = register("corrupted_gold", CorruptedGoldBlock::new,
			AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
					.mapColor(MapColor.GOLD)
					.requiresTool()
					.strength(1.2F, 6.0F)
					.luminance(state -> 2));

	public static final MatrixCubeBlock MATRIX_CUBE = register("matrix_cube", MatrixCubeBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.LIME)
					.strength(-1.0F, 3600000.0F)
					.nonOpaque()
					.luminance(state -> 12)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK)
					.noCollision());

	public static final Block INFECTED_BLOCK = register("infected_block", InfectedBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.PALE_GREEN)
					.requiresTool()
					.sounds(BlockSoundGroup.BASALT)
					.strength(1.2F, 9.0F)
					.luminance(state -> 2));

	public static final Block INFECTIOUS_CUBE = register("infectious_cube", InfectiousCubeBlock::new,
			AbstractBlock.Settings.create()
					.mapColor(MapColor.PURPLE)
					.nonOpaque()
					.sounds(BlockSoundGroup.AMETHYST_BLOCK)
					.luminance(state -> 7)
					.strength(0.8F, 2.0F));

	private ModBlocks() {
	}

	private static <T extends Block> T register(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings settings) {
		Identifier id = Identifier.of(TheVirusBlock.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		T block = factory.apply(settings.registryKey(blockKey));
		Registry.register(Registries.BLOCK, blockKey, block);
		BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
		Registry.register(Registries.ITEM, itemKey, blockItem);
		return block;
	}

	public static void bootstrap() {
		// no-op; ensures static initializer runs
	}
}

