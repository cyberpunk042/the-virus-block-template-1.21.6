package net.cyberpunk042.infection.events;

import net.cyberpunk042.growth.FuseProfile;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record GrowthFuseEvent(ServerWorld world,
		BlockPos origin,
		Stage stage,
		int ticksRemaining,
		FuseProfile profile) implements InfectionEvent {

	public enum Stage {
		ARMED,
		PULSE,
		DETONATED
	}
}

