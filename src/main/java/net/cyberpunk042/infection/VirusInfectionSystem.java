package net.cyberpunk042.infection;

import net.cyberpunk042.block.VirusBlockProtection;
import net.cyberpunk042.command.VirusCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

public final class VirusInfectionSystem {
	private VirusInfectionSystem() {
	}

	public static void init() {
		VirusBlockProtection.init();
		VirusTierBossBar.init();
		GlobalTerrainCorruption.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> VirusCommand.register(dispatcher));

		ServerWorldEvents.LOAD.register((server, world) -> VirusWorldState.get(world));
		ServerTickEvents.END_WORLD_TICK.register(VirusInfectionSystem::tickWorld);
	}

	private static void tickWorld(ServerWorld world) {
		VirusWorldState state = VirusWorldState.get(world);
		state.tick(world);
		VirusTierBossBar.update(world, state);
	}
}

