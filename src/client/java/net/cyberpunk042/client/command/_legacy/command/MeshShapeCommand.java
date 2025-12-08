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
import net.cyberpunk042.client.render.ShieldMeshShapeStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class MeshShapeCommand {
	private MeshShapeCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("meshshape")
				.then(literal("list").executes(ctx -> {
					List<String> names = new ArrayList<>(ShieldMeshShapeStore.listShapes());
					ctx.getSource().sendFeedback(Text.literal("Mesh shapes: " + String.join(", ", names)));
					return 1;
				}))
				.then(literal("show")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshShapeCommand::suggestShapes)
								.executes(ctx -> showShape(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
				.then(literal("set")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshShapeCommand::suggestShapes)
								.then(argument("key", StringArgumentType.word())
										.then(argument("value", StringArgumentType.greedyString())
												.executes(ctx -> {
													String name = StringArgumentType.getString(ctx, "name");
													String key = StringArgumentType.getString(ctx, "key");
													String value = StringArgumentType.getString(ctx, "value");
													return setShapeValue(ctx.getSource(), name, key, value);
												})))))
				.then(literal("save")
						.then(argument("name", StringArgumentType.word())
								.suggests(MeshShapeCommand::suggestShapes)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									if (ShieldMeshShapeStore.save(name)) {
										ctx.getSource().sendFeedback(Text.literal("Saved mesh shape '" + name + "' to config folder."));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Failed to save mesh shape '" + name + "'."));
									return 0;
								}))));
	}

	private static int showShape(FabricClientCommandSource source, String name) {
		return ShieldMeshShapeStore.getShape(name).map(config -> {
			source.sendFeedback(Text.literal(config.toJson().toString()));
			return 1;
		}).orElseGet(() -> {
			source.sendError(Text.literal("Unknown mesh shape '" + name + "'."));
			return 0;
		});
	}

	private static int setShapeValue(FabricClientCommandSource source, String name, String key, String value) {
		return ShieldMeshShapeStore.getEditable(name).map(config -> {
			try {
				config.setValue(key, value);
				ShieldMeshShapeStore.updateShape(name, config);
				source.sendFeedback(Text.literal("Mesh shape '" + name + "' set " + key + " = " + value));
				ShieldFieldVisualManager.reloadActiveProfile();
				return 1;
			} catch (IllegalArgumentException ex) {
				source.sendError(Text.literal("Invalid shape key '" + key + "': " + ex.getMessage()));
				return 0;
			}
		}).orElseGet(() -> {
			source.sendError(Text.literal("Unknown mesh shape '" + name + "'."));
			return 0;
		});
	}

	private static CompletableFuture<Suggestions> suggestShapes(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		return suggest(builder, ShieldMeshShapeStore.listShapes());
	}

	private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Iterable<String> values) {
		for (String value : values) {
			builder.suggest(value);
		}
		return builder.buildFuture();
	}
}


