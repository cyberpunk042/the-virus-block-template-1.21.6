package net.cyberpunk042.command;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.VirusWorldState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

public final class VirusDebugCommands {

	private VirusDebugCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("virusboobytraps")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(ctx -> debugBoobytraps(ctx.getSource(), 8))
					.then(CommandManager.argument("radiusChunks", IntegerArgumentType.integer(1, 16))
							.executes(ctx -> debugBoobytraps(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radiusChunks")))));

			dispatcher.register(CommandManager.literal("virustiers")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(ctx -> showTierPlan(ctx.getSource())));

			dispatcher.register(CommandManager.literal("virusvoidtear")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(ctx -> spawnVoidTear(ctx.getSource())));
		});
	}

	private static int debugBoobytraps(ServerCommandSource source, int radiusChunks) {
		if (source.getPlayer() == null) {
			return 0;
		}
		BoobytrapHelper.debugList(source.getPlayer(), radiusChunks * 16);
		return 1;
	}

	private static int showTierPlan(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		InfectionTier tier = state.tiers().currentTier();
		boolean apocalypse = state.tiers().isApocalypseMode();

		source.sendFeedback(() -> Text.literal("Virus Tier: " + tier.getLevel() + (apocalypse ? " (Apocalypse)" : "")), false);

		source.sendFeedback(() -> Text.literal("Feature status:"), false);
		for (TierFeature feature : TierFeature.values()) {
			String line = describeFeature(world, tier, apocalypse, feature);
			source.sendFeedback(() -> Text.literal(line), false);
		}

		source.sendFeedback(() -> Text.literal("Default unlock plan:"), false);
		EnumMap<InfectionTier, List<TierFeature>> defaultPlan = TierCookbook.defaultPlan();
		for (InfectionTier tierKey : InfectionTier.values()) {
			List<TierFeature> features = defaultPlan.getOrDefault(tierKey, List.of());
			if (features.isEmpty()) {
				source.sendFeedback(() -> Text.literal("Tier " + tierKey.getLevel() + ": (no gated features)"), false);
			} else {
				String list = features.stream()
						.map(feature -> feature.getId() + " [" + feature.getGroup().name().toLowerCase(Locale.ROOT) + "]")
						.collect(Collectors.joining(", "));
				source.sendFeedback(() -> Text.literal("Tier " + tierKey.getLevel() + ": " + list), false);
			}
		}
		return TierFeature.values().length;
	}

	private static int spawnVoidTear(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			source.sendError(Text.literal("Player-only command."));
			return 0;
		}
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		Random random = world.getRandom();
		BlockPos target = player.getBlockPos().add(
				random.nextBetween(-4, 4),
				random.nextBetween(-1, 2),
				random.nextBetween(-4, 4));
		if (state.infection().spawnVoidTearForCommand(world, target)) {
			source.sendFeedback(() -> Text.literal("Spawned Void Tear at " + target.toShortString()), false);
			return 1;
		}
		source.sendError(Text.literal("Unable to spawn Void Tear."));
		return 0;
	}

	private static String describeFeature(ServerWorld world, InfectionTier currentTier, boolean apocalypse, TierFeature feature) {
		boolean enabled = TierCookbook.isEnabled(world, currentTier, apocalypse, feature);
		boolean tierUnlocked = apocalypse || !currentTier.isBelow(feature.getMinTier());
		boolean groupEnabled = feature.getGroup().isGroupEnabled(world);
		boolean gameruleEnabled = feature.getToggleRule().map(rule -> world.getGameRules().getBoolean(rule)).orElse(true);

		StringBuilder builder = new StringBuilder();
		builder.append(" - [").append(enabled ? "ON" : "OFF").append("] ")
				.append(feature.getId())
				.append(" (Tier ").append(feature.getMinTier().getLevel()).append("+ / ")
				.append(feature.getGroup().name().toLowerCase(Locale.ROOT)).append(")");

		feature.getToggleRule().ifPresent(rule -> builder.append(" rule=")
				.append(rule.getName()).append('=')
				.append(world.getGameRules().getBoolean(rule)));

		if (!enabled) {
			builder.append(" <- ");
			if (!groupEnabled) {
				builder.append("group disabled");
			} else if (!tierUnlocked) {
				builder.append("requires tier ").append(feature.getMinTier().getLevel());
			} else if (!gameruleEnabled) {
				builder.append(feature.getToggleRule().map(GameRules.Key::getName).orElse("rule")).append("=false");
			} else {
				builder.append("suppressed");
			}
		}

		return builder.toString();
	}
}

