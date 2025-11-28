package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.config.ColorConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class ShieldPersonalCommand {
	private ShieldPersonalCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("shieldpersonal")
				.then(literal("on")
						.executes(ctx -> {
							ShieldFieldVisualManager.enablePersonalShield(0.0F);
							ctx.getSource().sendFeedback(Text.literal("Personal shield enabled."));
							return 1;
						})
						.then(argument("radius", FloatArgumentType.floatArg(1.0F, 256.0F))
								.executes(ctx -> {
									float radius = FloatArgumentType.getFloat(ctx, "radius");
									ShieldFieldVisualManager.enablePersonalShield(radius);
									ctx.getSource().sendFeedback(Text.literal("Personal shield enabled (radius " + radius + ")."));
									return 1;
								})))
				.then(literal("off")
						.executes(ctx -> {
							ShieldFieldVisualManager.disablePersonalShield();
							ctx.getSource().sendFeedback(Text.literal("Personal shield disabled."));
							return 1;
						}))
				.then(literal("sync")
						.executes(ctx -> {
							ShieldFieldVisualManager.copyWorldProfileToPersonal();
							ctx.getSource().sendFeedback(Text.literal("Personal profile cloned from active shield settings."));
							return 1;
						}))
				.then(literal("visual")
						.then(literal("on").executes(ctx -> setVisualState(ctx.getSource(), true)))
						.then(literal("off").executes(ctx -> setVisualState(ctx.getSource(), false))))
				.then(literal("follow")
						.then(argument("mode", StringArgumentType.word())
								.suggests(ShieldPersonalCommand::suggestFollowModes)
								.executes(ctx -> {
									String mode = StringArgumentType.getString(ctx, "mode");
									if (ShieldFieldVisualManager.setPersonalFollowMode(mode)) {
										ctx.getSource().sendFeedback(Text.literal("Personal shield follow mode -> " + mode));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Unknown follow mode '" + mode + "'."));
									return 0;
								})))
				.then(literal("profile")
						.then(literal("list").executes(ShieldPersonalCommand::listProfiles))
						.then(literal("preset")
								.then(argument("name", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestAllPersonalProfiles)
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (ShieldFieldVisualManager.applyPersonalPreset(name)) {
												ctx.getSource().sendFeedback(Text.literal("Personal preset -> " + name));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Unknown personal preset '" + name + "'."));
											return 0;
										})))
						.then(literal("save")
								.then(argument("name", StringArgumentType.word())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (ShieldFieldVisualManager.savePersonalProfile(name)) {
												ctx.getSource().sendFeedback(Text.literal("Saved personal profile '" + name + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Failed to save personal profile '" + name + "'."));
											return 0;
										})))
						.then(literal("load")
								.then(argument("name", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestAllPersonalProfiles)
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (ShieldFieldVisualManager.loadPersonalProfile(name)) {
												ctx.getSource().sendFeedback(Text.literal("Loaded personal profile '" + name + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Unknown personal profile '" + name + "'."));
											return 0;
										}))))
				.then(literal("color")
						.then(literal("simple")
								.then(argument("name", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestBasicColors)
										.executes(ctx -> applySimpleColor(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
						.then(literal("primary")
								.then(argument("value", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestColorNames)
										.executes(ctx -> setPersonalColor(ctx.getSource(),
												ColorChannel.PRIMARY,
												StringArgumentType.getString(ctx, "value")))))
						.then(literal("secondary")
								.then(argument("value", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestColorNames)
										.executes(ctx -> setPersonalColor(ctx.getSource(),
												ColorChannel.SECONDARY,
												StringArgumentType.getString(ctx, "value")))))
						.then(literal("beam")
								.then(argument("value", StringArgumentType.word())
										.suggests(ShieldPersonalCommand::suggestColorNames)
										.executes(ctx -> setPersonalColor(ctx.getSource(),
												ColorChannel.BEAM,
												StringArgumentType.getString(ctx, "value"))))))
				.then(literal("prediction")
						.then(literal("show").executes(ctx -> showPersonalPrediction(ctx.getSource())))
						.then(literal("enable")
								.then(argument("value", BoolArgumentType.bool())
										.executes(ctx -> setPersonalPredictionEnabled(ctx.getSource(),
												BoolArgumentType.getBool(ctx, "value")))))
						.then(literal("lead")
								.then(argument("ticks", IntegerArgumentType.integer(0, 60))
										.executes(ctx -> setPersonalPredictionLead(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "ticks")))))
						.then(literal("max")
								.then(argument("blocks", FloatArgumentType.floatArg(0.0F, 64.0F))
										.executes(ctx -> setPersonalPredictionMax(ctx.getSource(),
												FloatArgumentType.getFloat(ctx, "blocks")))))
						.then(literal("look")
								.then(argument("offset", FloatArgumentType.floatArg(-16.0F, 16.0F))
										.executes(ctx -> setPersonalPredictionLook(ctx.getSource(),
												FloatArgumentType.getFloat(ctx, "offset")))))
						.then(literal("vertical")
								.then(argument("boost", FloatArgumentType.floatArg(-16.0F, 16.0F))
										.executes(ctx -> setPersonalPredictionVertical(ctx.getSource(),
												FloatArgumentType.getFloat(ctx, "boost"))))));

		CommandNode<FabricClientCommandSource> rootNode = dispatcher.register(root);
		dispatcher.register(literal("personalshield").redirect(rootNode));
	}

	private static int listProfiles(CommandContext<FabricClientCommandSource> ctx) {
		List<String> builtins = new ArrayList<>();
		ShieldFieldVisualManager.getPersonalPresetNames().forEach(builtins::add);
		List<String> saved = ShieldFieldVisualManager.getSavedPersonalProfileNames();
		ctx.getSource().sendFeedback(Text.literal("Personal built-in profiles: " + String.join(", ", builtins)));
		if (saved.isEmpty()) {
			ctx.getSource().sendFeedback(Text.literal("No saved personal profiles."));
		} else {
			ctx.getSource().sendFeedback(Text.literal("Saved personal profiles: " + String.join(", ", saved)));
		}
		ctx.getSource().sendFeedback(Text.literal("Active personal profile: " + ShieldFieldVisualManager.getPersonalProfileName()));
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestAllPersonalProfiles(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		ShieldFieldVisualManager.getPersonalPresetNames().forEach(names::add);
		ShieldFieldVisualManager.getSavedPersonalProfileNames().forEach(names::add);
		for (String name : names) {
			builder.suggest(name);
		}
		return builder.buildFuture();
	}

	private static int setVisualState(FabricClientCommandSource source, boolean visible) {
		ShieldFieldVisualManager.setPersonalShieldVisible(visible);
		source.sendFeedback(Text.literal("Personal shield rendering " + (visible ? "enabled." : "hidden.")));
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestFollowModes(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		for (String mode : ShieldFieldVisualManager.getPersonalFollowModeNames()) {
			builder.suggest(mode);
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestColorNames(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		for (String key : ColorConfig.colorKeys()) {
			builder.suggest(key);
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestBasicColors(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		for (String key : ColorConfig.basicColorKeys()) {
			builder.suggest(key);
		}
		return builder.buildFuture();
	}

	private static int setPersonalColor(FabricClientCommandSource source, ColorChannel channel, String raw) {
		Integer color = ColorConfig.parseUserColor(raw);
		if (color == null) {
			source.sendError(Text.literal("Unknown color '" + raw + "'. Use hex (#RRGGBB) or a name from colors.json."));
			return 0;
		}
		switch (channel) {
			case PRIMARY -> ShieldFieldVisualManager.setPersonalPrimaryColor(color);
			case SECONDARY -> ShieldFieldVisualManager.setPersonalSecondaryColor(color);
			case BEAM -> ShieldFieldVisualManager.setPersonalBeamColor(color);
		}
		String formatted = String.format(Locale.ROOT, "#%08X", color);
		source.sendFeedback(Text.literal("Personal " + channel.display + " color set to " + formatted));
		return 1;
	}

	private static int applySimpleColor(FabricClientCommandSource source, String name) {
		Integer base = ColorConfig.resolveNamedColor(name);
		if (base == null) {
			source.sendError(Text.literal("Unknown color '" + name + "'."));
			return 0;
		}
		int accent = lightenColor(base, 0.25F);
		int beam = lightenColor(base, 0.4F);
		ShieldFieldVisualManager.setPersonalPrimaryColor(base);
		ShieldFieldVisualManager.setPersonalSecondaryColor(accent);
		ShieldFieldVisualManager.setPersonalBeamColor(beam);
		source.sendFeedback(Text.literal("Personal shield colors set to '" + name + "'."));
		return 1;
	}

	private static int lightenColor(int argb, float delta) {
		float a = ((argb >>> 24) & 0xFF) / 255.0F;
		float r = ((argb >>> 16) & 0xFF) / 255.0F;
		float g = ((argb >>> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		r = Math.min(1.0F, r + delta);
		g = Math.min(1.0F, g + delta);
		b = Math.min(1.0F, b + delta);
		int ia = Math.round(a * 255.0F) & 0xFF;
		int ir = Math.round(r * 255.0F) & 0xFF;
		int ig = Math.round(g * 255.0F) & 0xFF;
		int ib = Math.round(b * 255.0F) & 0xFF;
		return (ia << 24) | (ir << 16) | (ig << 8) | ib;
	}

	private static int showPersonalPrediction(FabricClientCommandSource source) {
		source.sendFeedback(Text.literal("Personal shield prediction: " + ShieldFieldVisualManager.describePersonalPrediction()));
		return 1;
	}

	private static int setPersonalPredictionEnabled(FabricClientCommandSource source, boolean enabled) {
		ShieldFieldVisualManager.setPersonalPredictionEnabled(enabled);
		source.sendFeedback(Text.literal("Personal shield prediction " + (enabled ? "enabled" : "disabled")));
		return 1;
	}

	private static int setPersonalPredictionLead(FabricClientCommandSource source, int ticks) {
		ShieldFieldVisualManager.setPersonalPredictionLeadTicks(ticks);
		source.sendFeedback(Text.literal("Personal shield prediction lead -> " + ticks + " ticks"));
		return 1;
	}

	private static int setPersonalPredictionMax(FabricClientCommandSource source, float blocks) {
		ShieldFieldVisualManager.setPersonalPredictionMaxDistance(blocks);
		source.sendFeedback(Text.literal("Personal shield prediction max distance -> " + blocks));
		return 1;
	}

	private static int setPersonalPredictionLook(FabricClientCommandSource source, float offset) {
		ShieldFieldVisualManager.setPersonalPredictionLookAhead(offset);
		source.sendFeedback(Text.literal("Personal shield prediction look-ahead -> " + offset));
		return 1;
	}

	private static int setPersonalPredictionVertical(FabricClientCommandSource source, float boost) {
		ShieldFieldVisualManager.setPersonalPredictionVerticalBoost(boost);
		source.sendFeedback(Text.literal("Personal shield prediction vertical boost -> " + boost));
		return 1;
	}

	private enum ColorChannel {
		PRIMARY("primary"),
		SECONDARY("secondary"),
		BEAM("beam");

		private final String display;

		ColorChannel(String display) {
			this.display = display;
		}
	}
}


