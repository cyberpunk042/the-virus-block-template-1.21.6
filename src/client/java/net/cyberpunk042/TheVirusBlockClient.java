package net.cyberpunk042;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.network.SkyTintPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.CORRUPTED_GLASS, ModBlocks.CORRUPTED_ICE);
		CorruptedColorProviders.register();
		VirusFluidRenderers.register();
		ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(VirusSkyClientState::reset));
	}
}