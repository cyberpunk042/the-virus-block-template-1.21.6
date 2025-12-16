package net.cyberpunk042;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.render.CorruptedFireTextures;
import net.cyberpunk042.client.render.SingularityBorderClientState;
import net.cyberpunk042.client.state.SingularityScheduleClientState;
import net.cyberpunk042.client.render.SingularityVisualManager;
import net.cyberpunk042.client.render.blockentity.SingularityBlockEntityRenderer;
import net.cyberpunk042.client.render.blockentity.ProgressiveGrowthBlockEntityRenderer;
import net.cyberpunk042.client.render.item.ProgressiveGrowthItemRenderer;
import net.cyberpunk042.client.render.VoidTearVisualManager;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.beam.GrowthBeamRenderer;
import net.cyberpunk042.client.render.field.GrowthRingFieldRenderer;
import net.cyberpunk042.client.render.VirusHorizonClientState;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.client.render.entity.CorruptedWormRenderer;
import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.client.screen.PurificationTotemScreen;
import net.cyberpunk042.client.screen.VirusDifficultyScreen;
import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.client.network.GuiClientHandlers;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.config.ModConfigBootstrap;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.network.SkyTintPayload;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.network.HorizonTintPayload;
import net.cyberpunk042.network.SingularityBorderPayload;
import net.cyberpunk042.network.SingularitySchedulePayload;
import net.cyberpunk042.network.SingularityVisualStartPayload;
import net.cyberpunk042.network.SingularityVisualStopPayload;
import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.render.entity.TntEntityRenderer;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModConfigBootstrap.prepareClient();
		InfectionConfigRegistry.loadClient();
		BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT,
				ModBlocks.CORRUPTED_GLASS,
				ModBlocks.CORRUPTED_ICE,
				ModBlocks.CORRUPTED_PACKED_ICE,
				ModBlocks.PROGRESSIVE_GROWTH_BLOCK);
		BlockEntityRendererFactories.register(ModBlockEntities.SINGULARITY_BLOCK, SingularityBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(ModBlockEntities.PROGRESSIVE_GROWTH, ProgressiveGrowthBlockEntityRenderer::new);
		ProgressiveGrowthItemRenderer.bootstrap();
		CorruptedColorProviders.register();
		VoidTearVisualManager.init();
		// New field system - register definitions on client too (server loads JSON, client needs code defaults)
		net.cyberpunk042.field.FieldRegistry.registerDefaults();
		net.cyberpunk042.client.field.FieldClientInit.init();
		// GUI client handlers
		GuiClientHandlers.register();
		// Test field renderer for debug field preview (G145)
		TestFieldRenderer.init();
		// Wave shader for GPU-based wave deformation
		net.cyberpunk042.client.visual.render.WaveShaderRegistry.register();
		SingularityVisualManager.init();
		SingularityBorderClientState.init();
		VirusFluidRenderers.register();
		EntityRendererRegistry.register(ModEntities.FALLING_MATRIX_CUBE, FallingBlockEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_WORM, CorruptedWormRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_TNT, TntEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.VIRUS_FUSE, TntEntityRenderer::new);
		HandledScreens.register(ModScreenHandlers.PURIFICATION_TOTEM, PurificationTotemScreen::new);
		HandledScreens.register(ModScreenHandlers.VIRUS_DIFFICULTY, VirusDifficultyScreen::new);
		CorruptedFireTextures.bootstrap();
		GrowthBeamRenderer.init();
		GrowthRingFieldRenderer.init();
		ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
		ClientPlayNetworking.registerGlobalReceiver(HorizonTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusHorizonClientState.apply(payload.enabled(), payload.intensity(), payload.argb())));
		ClientPlayNetworking.registerGlobalReceiver(DifficultySyncPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusDifficultyClientState.set(payload.difficulty())));
		ClientPlayNetworking.registerGlobalReceiver(SingularityVisualStartPayload.ID, (payload, context) ->
				context.client().execute(() -> SingularityVisualManager.add(payload.pos())));
		ClientPlayNetworking.registerGlobalReceiver(SingularityVisualStopPayload.ID, (payload, context) ->
				context.client().execute(() -> SingularityVisualManager.remove(payload.pos())));
		ClientPlayNetworking.registerGlobalReceiver(SingularityBorderPayload.ID, (payload, context) ->
				context.client().execute(() -> SingularityBorderClientState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(SingularitySchedulePayload.ID, (payload, context) ->
				context.client().execute(() -> SingularityScheduleClientState.apply(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(() -> {
					VirusSkyClientState.reset();
					VirusHorizonClientState.reset();
					VirusDifficultyClientState.set(VirusDifficulty.HARD);
					SingularityBorderClientState.reset();
					SingularityScheduleClientState.reset();
					// Clear test field to avoid ghost rendering on reconnect
					net.cyberpunk042.client.gui.state.FieldEditStateHolder.despawnTestField();
				}));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// Old shield commands moved to agent-tools/legacy-shield/
			// New field system uses /fieldtest (server-side command)
			// SingularityVisualCommand removed - using new field system
		});
	}
}