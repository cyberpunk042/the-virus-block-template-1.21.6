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
		
		// Client command registration (not in generic nodes)
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// Depth test shader command
			// /depthtest - cycles through modes
			// /depthtest <0-4> - sets specific mode
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("depthtest")
					.executes(ctx -> {
						// No argument = cycle
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
			
			// Direct depth renderer command (NEW - bypasses PostEffectProcessor)
			// /directdepth - cycles through modes
			// /directdepth <0-3> - sets specific mode
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
						com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4))
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
			);
		});
	}
}