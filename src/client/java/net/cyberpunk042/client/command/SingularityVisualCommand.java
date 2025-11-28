package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.cyberpunk042.client.render.SingularityVisualManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class SingularityVisualCommand {
	private static final List<String> KNOWN_KEYS = List.of(
			"general.lat_steps",
			"general.lon_steps",
			"general.core_overlap_ticks",
			"beam.inner_radius",
			"beam.outer_radius",
			"beam.black_delay",
			"beam.primary_alpha",
			"beam.core_alpha",
			"beam.black_growth",
			"beam.red_growth",
			"beam.alpha_step",
			"primary.min_scale",
			"primary.max_scale",
			"primary.grow_start_alpha",
			"primary.grow_end_alpha",
			"primary.shrink_start_alpha",
			"primary.shrink_end_alpha",
			"primary.grow_ticks",
			"primary.shrink_ticks",
			"primary.spin_multiplier",
			"primary.texture",
			"core.min_scale",
			"core.max_scale",
			"core.grow_start_alpha",
			"core.grow_end_alpha",
			"core.shrink_start_alpha",
			"core.shrink_end_alpha",
			"core.grow_ticks",
			"core.shrink_ticks",
			"core.spin_multiplier",
			"core.texture");

	private SingularityVisualCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("singularityvis")
				.then(literal("show").executes(ctx -> {
					String json = SingularityVisualManager.getConfig().toJson().toString();
					ctx.getSource().sendFeedback(Text.literal(json));
					return 1;
				}))
				.then(literal("set")
						.then(argument("key", StringArgumentType.word())
								.suggests(SingularityVisualCommand::suggestKeys)
								.then(argument("value", StringArgumentType.greedyString())
										.executes(ctx -> {
											String key = StringArgumentType.getString(ctx, "key");
											String raw = StringArgumentType.getString(ctx, "value");
											if (SingularityVisualManager.setConfigValue(key, raw)) {
												ctx.getSource().sendFeedback(Text.literal("Singularity " + key + " = " + raw));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Unknown key or invalid value '" + key + "'."));
											return 0;
										}))))
				.then(literal("save")
						.executes(ctx -> {
							if (SingularityVisualManager.saveConfig()) {
								ctx.getSource().sendFeedback(Text.literal("Singularity visual config saved."));
								return 1;
							}
							ctx.getSource().sendError(Text.literal("Failed to save singularity visual config."));
							return 0;
						}))
				.then(literal("reload")
						.executes(ctx -> {
							SingularityVisualManager.reloadConfig();
							ctx.getSource().sendFeedback(Text.literal("Singularity visual config reloaded."));
							return 1;
						})));
	}

	private static CompletableFuture<Suggestions> suggestKeys(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		for (String key : KNOWN_KEYS) {
			builder.suggest(key);
		}
		return builder.buildFuture();
	}
}

