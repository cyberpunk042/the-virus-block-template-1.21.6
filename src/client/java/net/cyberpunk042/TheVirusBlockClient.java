package net.cyberpunk042;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.command.ShieldVisualCommand;
import net.cyberpunk042.client.render.CorruptedFireTextures;
import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.client.render.VoidTearVisualManager;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.client.render.entity.CorruptedWormRenderer;
import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.client.screen.PurificationTotemScreen;
import net.cyberpunk042.client.screen.VirusDifficultyScreen;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.network.SkyTintPayload;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.CORRUPTED_GLASS, ModBlocks.CORRUPTED_ICE, ModBlocks.CORRUPTED_PACKED_ICE);
		CorruptedColorProviders.register();
		VoidTearVisualManager.init();
		ShieldFieldVisualManager.init();
		VirusFluidRenderers.register();
		EntityRendererRegistry.register(ModEntities.FALLING_MATRIX_CUBE, FallingBlockEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_WORM, CorruptedWormRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_TNT, TntEntityRenderer::new);
		HandledScreens.register(ModScreenHandlers.PURIFICATION_TOTEM, PurificationTotemScreen::new);
		HandledScreens.register(ModScreenHandlers.VIRUS_DIFFICULTY, VirusDifficultyScreen::new);
		CorruptedFireTextures.bootstrap();
		ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
		ClientPlayNetworking.registerGlobalReceiver(DifficultySyncPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusDifficultyClientState.set(payload.difficulty())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(() -> {
					VirusSkyClientState.reset();
					VirusDifficultyClientState.set(VirusDifficulty.HARD);
				}));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ShieldVisualCommand.register(dispatcher));
	}
}