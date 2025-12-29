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
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.WAVE_SHADER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FRESNEL_SHADER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.CORONA_SHADER))
			
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
			// Reserved for future client commands
		});
	}
}