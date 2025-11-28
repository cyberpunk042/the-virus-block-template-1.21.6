package net.cyberpunk042;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.command.MeshShapeCommand;
import net.cyberpunk042.client.command.MeshStyleCommand;
import net.cyberpunk042.client.command.ShieldPersonalCommand;
import net.cyberpunk042.client.command.SingularityVisualCommand;
import net.cyberpunk042.client.command.ShieldVisualCommand;
import net.cyberpunk042.client.command.TriangleTypeCommand;
import net.cyberpunk042.client.render.CorruptedFireTextures;
import net.cyberpunk042.client.render.SingularityBorderClientState;
import net.cyberpunk042.client.state.SingularityScheduleClientState;
import net.cyberpunk042.client.render.SingularityVisualManager;
import net.cyberpunk042.client.render.blockentity.SingularityBlockEntityRenderer;
import net.cyberpunk042.client.render.ShieldFieldVisualManager;
import net.cyberpunk042.client.render.VoidTearVisualManager;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.client.render.entity.BlackholePearlEntityRenderer;
import net.cyberpunk042.client.render.entity.CorruptedWormRenderer;
import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.client.screen.PurificationTotemScreen;
import net.cyberpunk042.client.screen.VirusDifficultyScreen;
import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.config.ModConfigBootstrap;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.network.SkyTintPayload;
import net.cyberpunk042.network.DifficultySyncPayload;
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
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModConfigBootstrap.prepareClient();
		InfectionConfigRegistry.loadClient();
		BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.CORRUPTED_GLASS, ModBlocks.CORRUPTED_ICE, ModBlocks.CORRUPTED_PACKED_ICE);
		BlockEntityRendererFactories.register(ModBlockEntities.SINGULARITY_BLOCK, SingularityBlockEntityRenderer::new);
		CorruptedColorProviders.register();
		VoidTearVisualManager.init();
		ShieldFieldVisualManager.init();
		SingularityVisualManager.init();
		SingularityBorderClientState.init();
		VirusFluidRenderers.register();
		EntityRendererRegistry.register(ModEntities.FALLING_MATRIX_CUBE, FallingBlockEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_WORM, CorruptedWormRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_TNT, TntEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.VIRUS_FUSE, TntEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.BLACKHOLE_PEARL, BlackholePearlEntityRenderer::new);
		HandledScreens.register(ModScreenHandlers.PURIFICATION_TOTEM, PurificationTotemScreen::new);
		HandledScreens.register(ModScreenHandlers.VIRUS_DIFFICULTY, VirusDifficultyScreen::new);
		CorruptedFireTextures.bootstrap();
		ClientPlayNetworking.registerGlobalReceiver(SkyTintPayload.ID, (payload, context) ->
				context.client().execute(() -> VirusSkyClientState.setState(payload.skyCorrupted(), payload.fluidsCorrupted())));
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
					VirusDifficultyClientState.set(VirusDifficulty.HARD);
					SingularityBorderClientState.reset();
					SingularityScheduleClientState.reset();
				}));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			ShieldVisualCommand.register(dispatcher);
			MeshStyleCommand.register(dispatcher);
			MeshShapeCommand.register(dispatcher);
			TriangleTypeCommand.register(dispatcher);
			ShieldPersonalCommand.register(dispatcher);
			SingularityVisualCommand.register(dispatcher);
		});
	}
}