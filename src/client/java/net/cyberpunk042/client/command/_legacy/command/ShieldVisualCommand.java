package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.client.render.ShieldProfileConfig;
import net.cyberpunk042.client.render.ShieldFieldVisualManager.EditTarget;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class ShieldVisualCommand {
	private ShieldVisualCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("shieldvis")
				.then(literal("list").executes(ctx -> {
					FabricClientCommandSource source = ctx.getSource();
					List<String> builtins = new ArrayList<>();
					ShieldFieldVisualManager.getPresetNames().forEach(builtins::add);
					List<String> saved = new ArrayList<>();
					ShieldFieldVisualManager.getSavedProfileNames().forEach(saved::add);
					source.sendFeedback(Text.literal("Built-in profiles: " + String.join(", ", builtins)));
					if (!saved.isEmpty()) {
						source.sendFeedback(Text.literal("Saved profiles: " + String.join(", ", saved)));
					}
					source.sendFeedback(Text.literal("Editing target: "
							+ ShieldFieldVisualManager.getEditTarget().name().toLowerCase(Locale.ROOT)));

					source.sendFeedback(Text.literal(formatSummary(
							"World",
							ShieldFieldVisualManager.getActivePresetName(),
							ShieldFieldVisualManager.getCurrentProfile())));
					for (String line : ShieldFieldVisualManager.describeLayers()) {
						source.sendFeedback(Text.literal("  " + line));
					}

					source.sendFeedback(Text.literal(formatSummary(
							"Personal",
							ShieldFieldVisualManager.getPersonalProfileName(),
							ShieldFieldVisualManager.getPersonalProfile())));
					for (String line : ShieldFieldVisualManager.describePersonalLayers()) {
						source.sendFeedback(Text.literal("  " + line));
					}
					return 1;
				}))
				.then(literal("target")
						.then(literal("world").executes(ctx -> setTarget(ctx.getSource(), EditTarget.WORLD)))
						.then(literal("personal").executes(ctx -> setTarget(ctx.getSource(), EditTarget.PERSONAL))))
				.then(literal("cycle").executes(ctx -> {
					String next = ShieldFieldVisualManager.cyclePreset();
					ctx.getSource().sendFeedback(Text.literal("Shield preset -> " + next));
					return 1;
				}))
				.then(literal("preset")
						.then(argument("name", StringArgumentType.word())
								.suggests(ShieldVisualCommand::suggestAllProfiles)
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									if (applyPresetForTarget(name)) {
										ctx.getSource().sendFeedback(Text.literal("Preset set to " + name));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Unknown preset '" + name + "'"));
									return 0;
								})))
				.then(literal("profiles")
						.then(literal("list").executes(ctx -> {
							List<String> saved = new ArrayList<>();
							ShieldFieldVisualManager.getSavedProfileNames().forEach(saved::add);
							if (saved.isEmpty()) {
								ctx.getSource().sendFeedback(Text.literal("No saved profiles."));
							} else {
								ctx.getSource().sendFeedback(Text.literal("Saved profiles: " + String.join(", ", saved)));
							}
							return 1;
						}))
						.then(literal("save")
								.then(argument("name", StringArgumentType.word())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (saveProfileForTarget(name)) {
												ctx.getSource().sendFeedback(Text.literal("Saved profile '" + name + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Failed to save profile '" + name + "'."));
											return 0;
										})))
						.then(literal("load")
								.then(argument("name", StringArgumentType.word())
										.suggests(ShieldVisualCommand::suggestAllProfiles)
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (loadProfileForTarget(name)) {
												ctx.getSource().sendFeedback(Text.literal("Loaded profile '" + name + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Unknown profile '" + name + "'."));
											return 0;
										}))))
				.then(literal("config")
						.then(literal("show").executes(ctx -> {
							String json = ShieldFieldVisualManager.getEditableProfile().toPrettyString();
							ctx.getSource().sendFeedback(Text.literal(json));
							return 1;
						}))
						.then(literal("set")
								.then(argument("key", StringArgumentType.word())
										.suggests(ShieldVisualCommand::suggestConfigKeys)
										.then(argument("value", StringArgumentType.greedyString())
												.executes(ctx -> {
													String key = StringArgumentType.getString(ctx, "key");
													String raw = StringArgumentType.getString(ctx, "value");
													if (setConfigForTarget(key, raw)) {
														ctx.getSource().sendFeedback(Text.literal("Set " + key + " = " + raw));
														return 1;
													}
													ctx.getSource().sendError(Text.literal("Unknown config key '" + key + "'."));
													return 0;
												})))))
				.then(literal("mesh")
						.then(literal("add")
								.then(argument("id", StringArgumentType.word())
										.executes(ctx -> {
											String id = StringArgumentType.getString(ctx, "id");
											if (addLayerForTarget(id)) {
												ctx.getSource().sendFeedback(Text.literal("Added mesh layer '" + id + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Mesh layer '" + id + "' already exists or invalid."));
											return 0;
										})))
						.then(literal("remove")
								.then(argument("id", StringArgumentType.word())
										.suggests(ShieldVisualCommand::suggestLayerIds)
										.executes(ctx -> {
											String id = StringArgumentType.getString(ctx, "id");
											if (removeLayerForTarget(id)) {
												ctx.getSource().sendFeedback(Text.literal("Removed mesh layer '" + id + "'."));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Cannot remove mesh layer '" + id + "'."));
											return 0;
										})))
						.then(literal("set")
								.then(argument("id", StringArgumentType.word())
										.suggests(ShieldVisualCommand::suggestLayerIds)
										.then(argument("key", StringArgumentType.word())
												.suggests(ShieldVisualCommand::suggestLayerKeys)
												.then(argument("value", StringArgumentType.greedyString())
														.executes(ctx -> {
															String id = StringArgumentType.getString(ctx, "id");
															String key = StringArgumentType.getString(ctx, "key");
															String raw = StringArgumentType.getString(ctx, "value");
															if (setLayerValueForTarget(id, key, raw)) {
																ctx.getSource().sendFeedback(Text.literal("Layer '" + id + "' " + key + " = " + raw));
																return 1;
															}
															ctx.getSource().sendError(Text.literal("Failed to set " + key + " on layer '" + id + "'."));
															return 0;
														}))))))
				.then(literal("spawn")
						.executes(ctx -> spawnLocal(ctx.getSource(), ShieldFieldVisualManager.getEditableProfile().radius(), 10))
						.then(argument("radius", FloatArgumentType.floatArg(1.0F, 512.0F))
								.executes(ctx -> spawnLocal(ctx.getSource(), FloatArgumentType.getFloat(ctx, "radius"), 10))
								.then(argument("seconds", IntegerArgumentType.integer(1, 600))
										.executes(ctx -> spawnLocal(ctx.getSource(),
												FloatArgumentType.getFloat(ctx, "radius"),
												IntegerArgumentType.getInteger(ctx, "seconds"))))))
				.then(literal("set")
						.then(literal("lat")
								.then(argument("value", IntegerArgumentType.integer(8, 640))
										.executes(ctx -> setSimple(ctx.getSource(), "lat", setLatForTarget(IntegerArgumentType.getInteger(ctx, "value"))))))
						.then(literal("lon")
								.then(argument("value", IntegerArgumentType.integer(16, 960))
										.executes(ctx -> setSimple(ctx.getSource(), "lon", setLonForTarget(IntegerArgumentType.getInteger(ctx, "value"))))))
						.then(literal("swirl")
								.then(argument("value", FloatArgumentType.floatArg(-8.0F, 8.0F))
										.executes(ctx -> setSimple(ctx.getSource(), "swirl", setSwirlForTarget(FloatArgumentType.getFloat(ctx, "value"))))))
						.then(literal("scale")
								.then(argument("value", FloatArgumentType.floatArg(0.05F, 2.0F))
										.executes(ctx -> setSimple(ctx.getSource(), "scale", setScaleForTarget(FloatArgumentType.getFloat(ctx, "value"))))))
						.then(literal("spin")
								.then(argument("value", FloatArgumentType.floatArg(-0.2F, 0.2F))
										.executes(ctx -> setSimple(ctx.getSource(), "spin", setSpinForTarget(FloatArgumentType.getFloat(ctx, "value"))))))
						.then(literal("tilt")
								.then(argument("value", FloatArgumentType.floatArg(-1.0F, 1.0F))
										.executes(ctx -> setSimple(ctx.getSource(), "tilt", setTiltForTarget(FloatArgumentType.getFloat(ctx, "value"))))))
				));

		dispatcher.register(literal("personalshield")
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
												FloatArgumentType.getFloat(ctx, "boost")))))));
	}

	private static CompletableFuture<Suggestions> suggestConfigKeys(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		return suggestFromIterable(builder, ShieldFieldVisualManager.getEditableConfigKeys());
	}

	private static CompletableFuture<Suggestions> suggestLayerIds(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		return suggestFromIterable(builder, ShieldFieldVisualManager.getEditableLayerIds());
	}

	private static CompletableFuture<Suggestions> suggestLayerKeys(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		return suggestFromIterable(builder, ShieldFieldVisualManager.getLayerKeySuggestions());
	}

	private static CompletableFuture<Suggestions> suggestAllProfiles(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		ShieldFieldVisualManager.getPresetNames().forEach(names::add);
		ShieldFieldVisualManager.getSavedProfileNames().forEach(names::add);
		return suggestFromIterable(builder, names);
	}

	private static CompletableFuture<Suggestions> suggestFromIterable(SuggestionsBuilder builder, Iterable<String> values) {
		for (String value : values) {
			builder.suggest(value);
		}
		return builder.buildFuture();
	}

	private static int spawnLocal(FabricClientCommandSource source, float radius, int seconds) {
		Entity entity = source.getEntity();
		if (entity == null) {
			source.sendError(Text.literal("Only players can spawn shield previews."));
			return 0;
		}
		Vec3d pos = entity.getPos();
		ShieldFieldVisualManager.spawnLocal(pos, radius, seconds * 20);
		source.sendFeedback(Text.literal("Spawned shield preview radius " + radius + " for " + seconds + "s."));
		return 1;
	}

	private static int setSimple(FabricClientCommandSource source, String label, boolean success) {
		if (success) {
			source.sendFeedback(Text.literal("Shield " + label + " updated."));
			return 1;
		}
		source.sendError(Text.literal("Value out of range for " + label + "."));
		return 0;
	}

	private static int setTarget(FabricClientCommandSource source, EditTarget target) {
		ShieldFieldVisualManager.setEditTarget(target);
		source.sendFeedback(Text.literal("Editing target -> " + target.name().toLowerCase(Locale.ROOT)));
		return 1;
	}

	private static String formatSummary(String label, String name, ShieldProfileConfig profile) {
		return String.format(Locale.ROOT,
				"%s profile [%s]: lat=%d lon=%d swirl=%.2f scale=%.2f spin=%.3f tilt=%.2f",
				label,
				name,
				profile.latSteps(),
				profile.lonSteps(),
				profile.swirlStrength(),
				profile.visualScale(),
				profile.spinSpeed(),
				profile.tiltMultiplier());
	}

	private static boolean applyPresetForTarget(String name) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.applyPersonalPreset(name)
				: ShieldFieldVisualManager.applyPreset(name);
	}

	private static boolean saveProfileForTarget(String name) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.savePersonalProfile(name)
				: ShieldFieldVisualManager.saveCurrentProfile(name);
	}

	private static boolean loadProfileForTarget(String name) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.loadPersonalProfile(name)
				: ShieldFieldVisualManager.loadProfile(name);
	}

	private static boolean setConfigForTarget(String key, String rawValue) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalConfigValue(key, rawValue)
				: ShieldFieldVisualManager.setConfigValue(key, rawValue);
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

	private static boolean addLayerForTarget(String id) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.addPersonalMeshLayer(id)
				: ShieldFieldVisualManager.addMeshLayer(id);
	}

	private static boolean removeLayerForTarget(String id) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.removePersonalMeshLayer(id)
				: ShieldFieldVisualManager.removeMeshLayer(id);
	}

	private static boolean setLayerValueForTarget(String id, String key, String rawValue) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalLayerValue(id, key, rawValue)
				: ShieldFieldVisualManager.setLayerConfigValue(id, key, rawValue);
	}

	private static boolean setLatForTarget(int value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalLatSteps(value)
				: ShieldFieldVisualManager.setLatSteps(value);
	}

	private static boolean setLonForTarget(int value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalLonSteps(value)
				: ShieldFieldVisualManager.setLonSteps(value);
	}

	private static boolean setSwirlForTarget(float value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalSwirlStrength(value)
				: ShieldFieldVisualManager.setSwirlStrength(value);
	}

	private static boolean setScaleForTarget(float value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalScale(value)
				: ShieldFieldVisualManager.setScale(value);
	}

	private static boolean setSpinForTarget(float value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalSpinSpeed(value)
				: ShieldFieldVisualManager.setSpinSpeed(value);
	}

	private static boolean setTiltForTarget(float value) {
		return ShieldFieldVisualManager.isEditingPersonal()
				? ShieldFieldVisualManager.setPersonalTiltMultiplier(value)
				: ShieldFieldVisualManager.setTiltMultiplier(value);
	}
}
