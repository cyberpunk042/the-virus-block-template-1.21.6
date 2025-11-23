package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
	public static final ItemGroup VIRUS_GROUP = Registry.register(
			Registries.ITEM_GROUP,
			Identifier.of(TheVirusBlock.MOD_ID, "core"),
			ItemGroup.create(ItemGroup.Row.TOP, 0)
					.icon(() -> new ItemStack(ModBlocks.VIRUS_BLOCK))
					.displayName(Text.translatable("itemGroup.the-virus-block"))
					.entries((displayContext, entries) -> {
						entries.add(ModBlocks.VIRUS_BLOCK);
						entries.add(ModBlocks.CORRUPTED_STONE);
						entries.add(ModBlocks.CORRUPTED_GLASS);
						entries.add(ModBlocks.CORRUPTED_DIRT);
						entries.add(ModBlocks.INFECTED_GRASS);
						entries.add(ModBlocks.CORRUPTED_WOOD);
						entries.add(ModBlocks.CORRUPTED_IRON);
						entries.add(ModBlocks.CORRUPTED_ICE);
						entries.add(ModBlocks.CORRUPTED_CRYING_OBSIDIAN);
						entries.add(ModBlocks.CORRUPTED_DIAMOND);
						entries.add(ModBlocks.CORRUPTED_GOLD);
						entries.add(ModBlocks.MATRIX_CUBE);
						entries.add(ModBlocks.INFECTED_BLOCK);
						entries.add(ModBlocks.INFECTIOUS_CUBE);
						entries.add(ModBlocks.BACTERIA);
						entries.add(ModItems.PURIFICATION_TOTEM);
					})
					.build()
	);

	private ModItemGroups() {
	}

	public static void bootstrap() {
	}
}

