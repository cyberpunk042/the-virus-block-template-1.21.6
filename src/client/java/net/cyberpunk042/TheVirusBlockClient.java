package net.cyberpunk042;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Wire State store to client Init framework for observable progress
		net.cyberpunk042.state.StateInit.connectClient();
		
		// Execute staged client initialization
		net.cyberpunk042.init.Init.clientOrchestrator()
			// Stage 1: Client core (config, render layers)
			.stage(net.cyberpunk042.init.InitStage.of("client_core", "Client Core")
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.CONFIG)
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.RENDER_LAYERS)
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.BLOCK_ENTITY_RENDERERS))
			
			// Stage 2: Visual rendering (colors, textures, entity renderers)
			.stage(net.cyberpunk042.init.InitStage.of("client_visual", "Visuals")
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.COLOR_PROVIDERS)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.FIRE_TEXTURES)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.FLUID_RENDERERS)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.ENTITY_RENDERERS))
			
			// Stage 3: Field system client
			.stage(net.cyberpunk042.init.InitStage.of("client_field", "Field System")
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_REGISTRY)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_NETWORK_RECEIVERS)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_RENDER_EVENT)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_TICK_EVENT)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_DISCONNECT_HANDLER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.BUNDLED_PROFILES)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.JOIN_WARMUP)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.WARMUP_OVERLAY)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.RENDER_WARMUP)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.TEST_RENDERER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FRAGMENT_PRESETS)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FRESNEL_SHADER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.DEPTH_TEST_SHADER))
			
			// Stage 4: GUI
			.stage(net.cyberpunk042.init.InitStage.of("client_gui", "GUI")
				.add(net.cyberpunk042.client.init.nodes.ClientGuiNodes.SCREENS)
				.add(net.cyberpunk042.client.init.nodes.ClientGuiNodes.GUI_HANDLERS))
			
			// Stage 5: Visual effects
			.stage(net.cyberpunk042.init.InitStage.of("client_fx", "Effects")
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.VOID_TEAR)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.SINGULARITY)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.BORDER_STATE)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.GROWTH_BEAM)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.GROWTH_RING))
			
			// Stage 6: Network receivers
			.stage(net.cyberpunk042.init.InitStage.of("client_network", "Network")
				.add(net.cyberpunk042.client.init.nodes.ClientNetworkNodes.RECEIVERS)
				.add(net.cyberpunk042.client.init.nodes.ClientNetworkNodes.DISCONNECT_HANDLER))
			
			.execute();
		
		// Client command registration
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// Depth test shader command
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("depthtest")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.DepthTestShader.cycleMode();
						int mode = net.cyberpunk042.client.visual.shader.DepthTestShader.getMode();
						String name = net.cyberpunk042.client.visual.shader.DepthTestShader.getModeName();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§eDepth Test Mode " + mode + ": §f" + name)
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("mode", 
						com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5))
						.executes(ctx -> {
							int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
							net.cyberpunk042.client.visual.shader.DepthTestShader.setMode(mode);
							String name = net.cyberpunk042.client.visual.shader.DepthTestShader.getModeName();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§eDepth Test Mode " + mode + ": §f" + name)
							);
							return 1;
						})
					)
			);
			
			// Direct depth renderer command
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("directdepth")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.DirectDepthRenderer.cycleMode();
						int mode = net.cyberpunk042.client.visual.shader.DirectDepthRenderer.getMode();
						String name = net.cyberpunk042.client.visual.shader.DirectDepthRenderer.getModeName();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§bDirect Depth Mode " + mode + ": §f" + name)
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("mode", 
						com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 99))
						.executes(ctx -> {
							int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
							net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setMode(mode);
							String name = net.cyberpunk042.client.visual.shader.DirectDepthRenderer.getModeName();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bDirect Depth Mode " + mode + ": §f" + name)
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("reversedz")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.DirectDepthRenderer.toggleReversedZ();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bReversed-Z toggled")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("ring")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("distance",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 500.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("thickness",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 50.0f))
								.executes(ctx -> {
									float dist = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "distance");
									float thick = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "thickness");
									net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setRingParams(dist, thick);
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§bRing: §f" + dist + " blocks ±" + (thick/2))
									);
									return 1;
								})
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("trigger")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.DirectDepthRenderer.triggerAnimation();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bShockwave triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("speed",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 200.0f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "speed");
								net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setAnimationSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bAnimation speed: §f" + speed + " blocks/sec")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("maxradius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(10.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setMaxRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bMax radius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("resolution")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("divisor",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 32))
							.executes(ctx -> {
								int divisor = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "divisor");
								net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setResolution(divisor);
								String quality = divisor == 1 ? "FULL" : "1/" + divisor;
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bResolution: §f" + quality)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.DirectDepthRenderer.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bStatic radius: §f" + radius + " blocks (no animation)")
								);
								return 1;
							})
						)
					)
				);
			
			// Shockwave Glow command (new hybrid renderer)
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwave")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.toggle();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§bShockwave: §f" + 
								(net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.isEnabled() ? "ON" : "OFF"))
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("trigger")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.trigger();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bShockwave triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bShockwave radius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
				);
			
			// GPU Shockwave command (proper FrameGraph integration)
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwavegpu")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.ShockwavePostEffect.toggle();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§d§lGPU Shockwave: §f" + 
								(net.cyberpunk042.client.visual.shader.ShockwavePostEffect.isEnabled() ? "ON" : "OFF"))
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("trigger")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lGPU Shockwave triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lRadius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("thickness")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("thickness",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 50.0f))
							.executes(ctx -> {
								float thickness = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "thickness");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setThickness(thickness);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lThickness: §f" + thickness + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("intensity")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("intensity",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 3.0f))
							.executes(ctx -> {
								float intensity = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "intensity");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setIntensity(intensity);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lIntensity: §f" + intensity)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("speed",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 200.0f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "speed");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lSpeed: §f" + speed + " blocks/sec")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("maxradius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("max",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(10.0f, 500.0f))
							.executes(ctx -> {
								float max = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "max");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setMaxRadius(max);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lMax Radius: §f" + max + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("status")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lGPU Shockwave: §f" + 
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.getStatusString())
							);
							return 1;
						})
					)
			);
			
			// TEST: Explicit pipeline shockwave (bypasses PostEffectProcessor)
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwavetest")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.ShockwaveTestRenderer.toggle();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§e§lTEST Shockwave: §f" + 
								(net.cyberpunk042.client.visual.shader.ShockwaveTestRenderer.isEnabled() ? "ON" : "OFF"))
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.ShockwaveTestRenderer.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§e§lTEST Radius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("mode")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwaveTestRenderer.toggleMode();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§e§lMode toggled - check logs for [shockwave_bind]")
							);
							return 1;
						})
					)
			);
		});
	}
}