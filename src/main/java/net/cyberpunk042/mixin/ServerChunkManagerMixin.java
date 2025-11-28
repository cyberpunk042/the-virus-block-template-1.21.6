package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.config.SingularityConfig;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.cyberpunk042.util.LogSpamWatchdog;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {
	@Shadow
	@Final
	private ServerWorld world;

	@Inject(method = "getWorldChunk", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$guardFullChunks(int chunkX, int chunkZ, CallbackInfoReturnable<WorldChunk> cir) {
		if (!shouldGuard()) {
			return;
		}
		if (allowOutsideBorderLoad()) {
			return;
		}
		if (isOutsideBorder(chunkX, chunkZ) && !SingularityChunkContext.isBypassingChunk(chunkX, chunkZ)) {
			if (SingularityConfig.debugLogging()) {
				logGuard("worldChunk", chunkX, chunkZ, "outsideBorder");
			}
			cir.setReturnValue(null);
			cir.cancel();
		}
	}

	@Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$guardChunkGeneration(int chunkX, int chunkZ, ChunkStatus status, boolean create, CallbackInfoReturnable<Chunk> cir) {
		if (!shouldGuard()) {
			return;
		}
		boolean collapseContext = SingularityChunkContext.isActive(world);
		if (!collapseContext) {
			return;
		}
		boolean outsideAllowed = allowOutsideBorderLoad();
		if (outsideAllowed) {
			return;
		}
		if (isOutsideBorder(chunkX, chunkZ) && !SingularityChunkContext.isBypassingChunk(chunkX, chunkZ)) {
			if (SingularityConfig.debugLogging()) {
				logGuard("getChunk", chunkX, chunkZ, "outsideBorder");
			}
			cir.setReturnValue(null);
			cir.cancel();
			return;
		}
		if (create && !allowChunkGeneration()) {
			long chunkPos = ChunkPos.toLong(chunkX, chunkZ);
			if (!world.isChunkLoaded(chunkPos)) {
				if (SingularityConfig.debugLogging()) {
					logGuard("getChunk", chunkX, chunkZ, "generationDisabled");
				}
				cir.setReturnValue(null);
				cir.cancel();
			}
		}
	}

	private boolean shouldGuard() {
		return SingularityChunkContext.shouldGuard(world);
	}

	private boolean allowChunkGeneration() {
		return SingularityConfig.allowChunkGeneration()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION);
	}

	private boolean allowOutsideBorderLoad() {
		return SingularityConfig.allowOutsideBorderLoad()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD);
	}

	private boolean isOutsideBorder(int chunkX, int chunkZ) {
		WorldBorder border = world.getWorldBorder();
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		return !border.contains(pos.getCenterX(), pos.getCenterZ());
	}

	private void logGuard(String phase, int chunkX, int chunkZ, String reason) {
		boolean outsideAllowed = allowOutsideBorderLoad();
		boolean generationAllowed = allowChunkGeneration();
		boolean bypassing = SingularityChunkContext.isBypassingChunk(chunkX, chunkZ);
		double borderRadius = world.getWorldBorder().getSize() * 0.5D;
		String bypassState = SingularityChunkContext.formatBypassState(chunkX, chunkZ);
		String template = "[Singularity] chunk guard denied phase={} chunk=[{},{}] reason={} outsideAllowed={} allowChunkGeneration={} bypass={} borderRadius={} bypassState={} thread={}";
		LogSpamWatchdog.SpamDecision decision = LogSpamWatchdog.observe(LogChannel.SINGULARITY, template);
		if (decision.summary() != null) {
			TheVirusBlock.LOGGER.warn(decision.summary());
		}
		if (decision.suppress()) {
			return;
		}
		TheVirusBlock.LOGGER.warn(template,
				phase,
				chunkX,
				chunkZ,
				reason,
				outsideAllowed,
				generationAllowed,
				bypassing,
				borderRadius,
				bypassState,
				Thread.currentThread().getName());
	}
}

