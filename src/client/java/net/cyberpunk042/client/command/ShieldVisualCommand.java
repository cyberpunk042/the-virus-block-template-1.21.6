package net.cyberpunk042.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class ShieldVisualCommand {
	private ShieldVisualCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("shieldvis")
				.then(literal("list").executes(ctx -> {
					FabricClientCommandSource source = ctx.getSource();
					String presets = String.join(", ", ShieldFieldVisualManager.getPresetNames());
					source.sendFeedback(Text.literal("Shield presets: " + presets));
					source.sendFeedback(Text.literal("Current: " + ShieldFieldVisualManager.getActivePresetName()
							+ " (lat=" + ShieldFieldVisualManager.getLatSteps()
							+ ", lon=" + ShieldFieldVisualManager.getLonSteps()
							+ ", swirl=" + ShieldFieldVisualManager.getSwirlStrength()
							+ ", scale=" + ShieldFieldVisualManager.getVisualScale()
							+ ", spin=" + ShieldFieldVisualManager.getSpinSpeed()
							+ ", tilt=" + ShieldFieldVisualManager.getTiltMultiplier() + ")"));
					return 1;
				}))
				.then(literal("cycle").executes(ctx -> {
					String next = ShieldFieldVisualManager.cyclePreset();
					ctx.getSource().sendFeedback(Text.literal("Shield preset -> " + next));
					return 1;
				}))
				.then(literal("preset")
						.then(argument("name", StringArgumentType.word())
								.suggests((ctx, builder) -> ShieldFieldVisualManager.suggestPresetNames(builder))
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									if (ShieldFieldVisualManager.applyPreset(name)) {
										ctx.getSource().sendFeedback(Text.literal("Shield preset set to " + name));
										return 1;
									}
									ctx.getSource().sendError(Text.literal("Unknown preset '" + name + "'"));
									return 0;
								})))
				.then(literal("set")
						.then(literal("lat")
								.then(argument("value", IntegerArgumentType.integer(8, 640))
										.executes(ctx -> {
											int value = IntegerArgumentType.getInteger(ctx, "value");
											if (ShieldFieldVisualManager.setLatSteps(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield lat steps = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Lat value out of range (8-640)."));
											return 0;
										})))
						.then(literal("lon")
								.then(argument("value", IntegerArgumentType.integer(16, 960))
										.executes(ctx -> {
											int value = IntegerArgumentType.getInteger(ctx, "value");
											if (ShieldFieldVisualManager.setLonSteps(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield lon steps = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Lon value out of range (16-960)."));
											return 0;
										})))
						.then(literal("swirl")
								.then(argument("value", FloatArgumentType.floatArg(-8.0F, 8.0F))
										.executes(ctx -> {
											float value = FloatArgumentType.getFloat(ctx, "value");
											if (ShieldFieldVisualManager.setSwirlStrength(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield swirl = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Swirl value out of range (-8 to 8)."));
											return 0;
										}))))
						.then(literal("scale")
								.then(argument("value", FloatArgumentType.floatArg(0.05F, 2.0F))
										.executes(ctx -> {
											float value = FloatArgumentType.getFloat(ctx, "value");
											if (ShieldFieldVisualManager.setScale(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield scale = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Scale out of range (0.05-2)."));
											return 0;
										})))
						.then(literal("spin")
								.then(argument("value", FloatArgumentType.floatArg(-0.2F, 0.2F))
										.executes(ctx -> {
											float value = FloatArgumentType.getFloat(ctx, "value");
											if (ShieldFieldVisualManager.setSpinSpeed(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield spin = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Spin out of range (-0.2 to 0.2)."));
											return 0;
										})))
						.then(literal("tilt")
								.then(argument("value", FloatArgumentType.floatArg(-1.0F, 1.0F))
										.executes(ctx -> {
											float value = FloatArgumentType.getFloat(ctx, "value");
											if (ShieldFieldVisualManager.setTiltMultiplier(value)) {
												ctx.getSource().sendFeedback(Text.literal("Shield tilt = " + value));
												return 1;
											}
											ctx.getSource().sendError(Text.literal("Tilt out of range (-1 to 1)."));
											return 0;
										})))
				);
	}
}

