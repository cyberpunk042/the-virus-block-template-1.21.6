package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.client.render.ShieldTriangleTypeStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class TriangleTypeCommand {
	private TriangleTypeCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("triangletype")
				.then(literal("list").executes(ctx -> {
					ctx.getSource().sendFeedback(Text.literal("Triangle types: " + String.join(", ", ShieldTriangleTypeStore.list())));
					return 1;
				}))
				.then(literal("show")
						.then(argument("name", StringArgumentType.word())
								.suggests(TriangleTypeCommand::suggest)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return ShieldTriangleTypeStore.get(name).map(config -> {
										ctx.getSource().sendFeedback(Text.literal(config.toJson().toString()));
										return 1;
									}).orElseGet(() -> {
										ctx.getSource().sendError(Text.literal("Unknown triangle type '" + name + "'."));
										return 0;
									});
								})))
				.then(literal("set")
						.then(argument("name", StringArgumentType.word())
								.suggests(TriangleTypeCommand::suggest)
								.then(argument("key", StringArgumentType.word())
										.then(argument("value", StringArgumentType.greedyString())
												.executes(ctx -> {
													String name = StringArgumentType.getString(ctx, "name");
													String key = StringArgumentType.getString(ctx, "key");
													String value = StringArgumentType.getString(ctx, "value");
													return ShieldTriangleTypeStore.getEditable(name).map(config -> {
														try {
															config.setValue(key, value);
															ShieldTriangleTypeStore.update(name, config);
															ShieldFieldVisualManager.reloadActiveProfile();
															ctx.getSource().sendFeedback(Text.literal("Triangle type '" + name + "' updated."));
															return 1;
														} catch (IllegalArgumentException ex) {
															ctx.getSource().sendError(Text.literal(ex.getMessage()));
															return 0;
														}
													}).orElseGet(() -> {
														ctx.getSource().sendError(Text.literal("Unknown triangle type '" + name + "'."));
														return 0;
													});
												})))))
				.then(literal("save")
						.then(argument("name", StringArgumentType.word())
								.suggests(TriangleTypeCommand::suggest)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									if (ShieldTriangleTypeStore.save(name)) {
										ctx.getSource().sendFeedback(Text.literal("Saved triangle type '" + name + "' to config folder."));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Failed to save triangle type '" + name + "'."));
									return 0;
								}))));
	}

	private static CompletableFuture<Suggestions> suggest(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		for (String value : ShieldTriangleTypeStore.list()) {
			builder.suggest(value);
		}
		return builder.buildFuture();
	}
}


