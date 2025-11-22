package net.cyberpunk042.block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.singularity.SingularityManager;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class VirusBlockProtection {
	public static final int MIN_SURVIVAL_BREAK_TIER = 3;
	private static final int COOLDOWN_TICKS = 10;

	private static final Map<UUID, Long> blockedUntil = new ConcurrentHashMap<>();

	private VirusBlockProtection() {
	}

	public static void init() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld serverWorld)
					|| !(player instanceof ServerPlayerEntity serverPlayer)
					|| !state.isOf(ModBlocks.VIRUS_BLOCK)) {
				return true;
			}

			VirusWorldState infection = VirusWorldState.get(serverWorld);
			if (SingularityManager.canBreakVirusBlock(serverWorld)) {
				return true;
			}

			if (shouldBlockBreak(serverWorld, serverPlayer, infection)) {
				resyncBlock(serverPlayer, serverWorld, pos, state);
				return false;
			}

			return true;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> blockedUntil.remove(handler.player.getUuid()));
	}

	public static void recordBlockedAttempt(ServerPlayerEntity player, ServerWorld world) {
		blockedUntil.put(player.getUuid(), world.getTime() + COOLDOWN_TICKS);
	}

	private static boolean shouldBlockBreak(ServerWorld world, ServerPlayerEntity player, VirusWorldState infection) {
		long now = world.getTime();
		Long lock = blockedUntil.get(player.getUuid());
		if (lock != null && now < lock) {
			return true;
		}

		InfectionTier tier = infection.getCurrentTier();
		boolean blocked = !player.isCreative() && tier.getIndex() < MIN_SURVIVAL_BREAK_TIER;
		if (blocked) {
			recordBlockedAttempt(player, world);
		}

		return blocked;
	}

	private static void resyncBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
		world.getChunkManager().markForUpdate(pos);
		player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
	}
}

