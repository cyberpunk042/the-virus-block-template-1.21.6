package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.client.render.ShieldMeshStyleStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class MeshStyleCommand {
	private MeshStyleCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("meshstyle")
				.then(literal("list").executes(ctx -> {
					List<String> names = new ArrayList<>(ShieldMeshStyleStore.listStyles());
					ctx.getSource().sendFeedback(Text.literal("Mesh styles: " + String.join(", ", names)));
					return 1;
				}))
				.then(literal("show")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshStyleCommand::suggestStyles)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return showStyle(ctx.getSource(), name);
								})))
				.then(literal("set")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshStyleCommand::suggestStyles)
								.then(argument("key", StringArgumentType.word())
										.then(argument("value", StringArgumentType.greedyString())
												.executes(ctx -> {
													String name = StringArgumentType.getString(ctx, "name");
													String key = StringArgumentType.getString(ctx, "key");
													String value = StringArgumentType.getString(ctx, "value");
													return setStyleValue(ctx.getSource(), name, key, value);
												})))))
				.then(literal("save")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshStyleCommand::suggestStyles)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									if (ShieldMeshStyleStore.save(name)) {
										ctx.getSource().sendFeedback(Text.literal("Saved mesh style '" + name + "' to config folder."));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Failed to save mesh style '" + name + "'."));
									return 0;
								}))));
	}

	private static int showStyle(FabricClientCommandSource source, String name) {
		return ShieldMeshStyleStore.getStyle(name).map(config -> {
			source.sendFeedback(Text.literal(config.toJson().toString()));
			return 1;
		}).orElseGet(() -> {
			source.sendError(Text.literal("Unknown mesh style '" + name + "'."));
			return 0;
		});
	}

	private static int setStyleValue(FabricClientCommandSource source, String name, String key, String value) {
		return ShieldMeshStyleStore.getEditable(name).map(config -> {
			try {
				config.setValue(key, value);
				source.sendFeedback(Text.literal("Mesh style '" + name + "' set " + key + " = " + value));
				ShieldFieldVisualManager.reloadActiveProfile();
				return 1;
			} catch (IllegalArgumentException ex) {
				source.sendError(Text.literal("Invalid mesh key '" + key + "': " + ex.getMessage()));
				return 0;
			}
		}).orElseGet(() -> {
			source.sendError(Text.literal("Unknown mesh style '" + name + "'."));
			return 0;
		});
	}

	private static CompletableFuture<Suggestions> suggestStyles(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		return suggestFromIterable(builder, ShieldMeshStyleStore.listStyles());
	}

	private static CompletableFuture<Suggestions> suggestFromIterable(SuggestionsBuilder builder, Iterable<String> values) {
		for (String value : values) {
			builder.suggest(value);
		}
		return builder.buildFuture();
	}
}


