package net.cyberpunk042.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.TierFeatureGroup;
import net.cyberpunk042.infection.VirusWorldState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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
		InfectionTier tier = state.getCurrentTier();
		boolean apocalypse = state.isApocalypseMode();

		source.sendFeedback(() -> Text.literal("Virus Tier: " + tier.getLevel() + (apocalypse ? " (Apocalypse)" : "")), false);

		EnumMap<TierFeatureGroup, List<TierFeature>> grouped = new EnumMap<>(TierFeatureGroup.class);
		for (TierFeature feature : TierCookbook.activeFeatures(world, tier, apocalypse)) {
			grouped.computeIfAbsent(feature.getGroup(), key -> new ArrayList<>()).add(feature);
		}
		if (grouped.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No tier features are currently active."), false);
			return 1;
		}

		for (TierFeatureGroup group : TierFeatureGroup.values()) {
			List<TierFeature> features = grouped.get(group);
			if (features == null || features.isEmpty()) {
				continue;
			}
			features.sort(Comparator.comparing(TierFeature::getId));
			source.sendFeedback(() -> Text.literal(group.name() + ":"), false);
			for (TierFeature feature : features) {
				String line = " - " + feature.getId() + " (>= Tier " + feature.getMinTier().getLevel() + ")";
				source.sendFeedback(() -> Text.literal(line), false);
			}
		}

		source.sendFeedback(() -> Text.literal("Default unlock plan:"), false);
		EnumMap<InfectionTier, List<TierFeature>> defaultPlan = TierCookbook.defaultPlan();
		for (InfectionTier tierKey : InfectionTier.values()) {
			List<TierFeature> features = defaultPlan.get(tierKey);
			if (features == null || features.isEmpty()) {
				continue;
			}
			String list = features.stream()
					.map(feature -> feature.getId() + " [" + feature.getGroup().name().toLowerCase(Locale.ROOT) + "]")
					.collect(Collectors.joining(", "));
			source.sendFeedback(() -> Text.literal("Tier " + tierKey.getLevel() + ": " + list), false);
		}
		return grouped.values().stream().mapToInt(List::size).sum();
	}
}

