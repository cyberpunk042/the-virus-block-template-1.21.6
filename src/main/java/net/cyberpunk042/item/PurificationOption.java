package net.cyberpunk042.item;

import java.util.function.Supplier;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

public enum PurificationOption {
	NO_BOOBYTRAPS("no_boobytraps", () -> new ItemStack(Items.TNT)),
	NO_SHELL("no_shell", () -> new ItemStack(Items.OBSIDIAN)),
	HALF_HP("half_hp", () -> new ItemStack(Items.HEART_OF_THE_SEA));

	private final String key;
	private final Supplier<ItemStack> iconSupplier;

	PurificationOption(String key, Supplier<ItemStack> iconSupplier) {
		this.key = key;
		this.iconSupplier = iconSupplier;
	}

	public ItemStack createIcon() {
		return iconSupplier.get().copy();
	}

	public Text title() {
		return Text.translatable("screen.the-virus-block.purification_totem.option." + key);
	}

	public Text description() {
		return Text.translatable("screen.the-virus-block.purification_totem.description." + key);
	}

	public void apply(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		switch (this) {
			case NO_BOOBYTRAPS -> applyNoBoobytraps(world, player);
			case NO_SHELL -> applyNoShell(world, player, state);
			case HALF_HP -> applyHalfHp(world, player, state);
		}
		state.applyPurification(20 * 120L);
		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0F, 0.7F);
	}

	private static void applyNoBoobytraps(ServerWorld world, ServerPlayerEntity player) {
		GameRules gameRules = world.getGameRules();
		gameRules.get(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED).set(false, world.getServer());
		gameRules.get(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE).set(Math.max(1, gameRules.getInt(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE) / 3), world.getServer());
		gameRules.get(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE).set(Math.max(1, gameRules.getInt(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE) / 3), world.getServer());
		player.sendMessage(Text.translatable("message.the-virus-block.purification.no_boobytraps").formatted(Formatting.GREEN), false);
	}

	private static void applyNoShell(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		state.collapseShells(world);
		player.sendMessage(Text.translatable("message.the-virus-block.purification.no_shell").formatted(Formatting.RED), false);
	}

	private static void applyHalfHp(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		boolean drained = state.drainVirusHealth(world, 0.5D);
		player.sendMessage(Text.translatable(
				drained
						? "message.the-virus-block.purification.half_hp"
						: "message.the-virus-block.purification.half_hp_failed").formatted(Formatting.DARK_RED), false);
	}
}

