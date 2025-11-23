package net.cyberpunk042;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.client.render.entity.CorruptedWormRenderer;
import net.cyberpunk042.client.screen.PurificationTotemScreen;
import net.cyberpunk042.network.SkyTintPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.CORRUPTED_GLASS, ModBlocks.CORRUPTED_ICE);
		CorruptedColorProviders.register();
		VirusFluidRenderers.register();
		EntityRendererRegistry.register(ModEntities.FALLING_MATRIX_CUBE, FallingBlockEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_WORM, CorruptedWormRenderer::new);
		HandledScreens.register(ModScreenHandlers.PURIFICATION_TOTEM, PurificationTotemScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(VirusSkyClientState::reset));
	}
}