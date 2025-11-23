package net.cyberpunk042.recipe;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Builds runtime fallback recipes for when the datapack fails to load.
 */
public final class VirusFallbackRecipes {
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> CURED_CUBE_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "cured_infectious_cube"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_NORTH_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_EAST_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_east"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_SOUTH_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_south"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_WEST_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_west"));

	private VirusFallbackRecipes() {
	}

	public static boolean appendMissingRecipes(PreparedRecipes current, List<RecipeEntry<?>> entries) {
		boolean changed = false;
		if (current.get(CURED_CUBE_ID) == null) {
			entries.add(createCuredCubeRecipe());
			TheVirusBlock.LOGGER.warn("Injected fallback recipe for {} because datapack entry was missing", CURED_CUBE_ID.getValue());
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_NORTH_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_NORTH_ID, " C ", "GTI", " D "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_EAST_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_EAST_ID, " G ", "DTC", " I "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_SOUTH_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_SOUTH_ID, " D ", "ITG", " C "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_WEST_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_WEST_ID, " I ", "CTD", " G "));
			changed = true;
		}
		if (changed) {
			TheVirusBlock.LOGGER.warn("Purification Totem fallback recipes were injected because the datapack failed to provide them.");
		}
		return changed;
	}

	private static RecipeEntry<ShapelessRecipe> createCuredCubeRecipe() {
		ShapelessRecipe recipe = new ShapelessRecipe(
				"",
				CraftingRecipeCategory.MISC,
				new ItemStack(ModBlocks.CURED_INFECTIOUS_CUBE),
				List.of(
						Ingredient.ofItems(ModBlocks.INFECTIOUS_CUBE),
						Ingredient.ofItems(Items.MILK_BUCKET)));
		return new RecipeEntry<>(CURED_CUBE_ID, recipe);
	}

	private static RecipeEntry<ShapedRecipe> createTotemRecipe(RegistryKey<net.minecraft.recipe.Recipe<?>> id, String... pattern) {
		Map<Character, Ingredient> key = Map.of(
				'C', Ingredient.ofItems(ModBlocks.CURED_INFECTIOUS_CUBE),
				'G', Ingredient.ofItems(ModBlocks.CORRUPTED_GOLD),
				'I', Ingredient.ofItems(ModBlocks.CORRUPTED_IRON),
				'D', Ingredient.ofItems(ModBlocks.CORRUPTED_DIAMOND),
				'T', Ingredient.ofItems(Items.TOTEM_OF_UNDYING));
		RawShapedRecipe raw = RawShapedRecipe.create(key, Arrays.asList(pattern));
		ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingRecipeCategory.MISC,
				raw,
				new ItemStack(ModItems.PURIFICATION_TOTEM));
		return new RecipeEntry<>(id, recipe);
	}
}

