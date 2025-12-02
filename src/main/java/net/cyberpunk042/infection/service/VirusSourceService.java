package net.cyberpunk042.infection.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Service façade for virus core/source lifecycle. This is the choke point for
 * placement, shell interactions, and teleport routines so we can add telemetry
 * without forcing every caller to reach into {@link VirusWorldState}.
 */
public final class VirusSourceService {

	public static final class State {
		private final Set<BlockPos> sources = new HashSet<>();
		private final Set<BlockPos> suppressedUnregisters = new HashSet<>();

		public Set<BlockPos> sources() {
			return sources;
		}

		public Set<BlockPos> suppressedUnregisters() {
			return suppressedUnregisters;
		}
	}

	private final VirusWorldState host;
	private final LoggingService logging;
	private final State state;

	public VirusSourceService(VirusWorldState state, LoggingService logging) {
		this(state, logging, new State());
	}

	public VirusSourceService(VirusWorldState state, LoggingService logging, State data) {
		this.host = Objects.requireNonNull(state, "state");
		this.logging = logging != null ? logging : new LoggingService();
		this.state = data != null ? data : new State();
	}

	public State state() {
		return state;
	}

	public boolean addSource(State state, BlockPos pos) {
		return state.sources.add(pos.toImmutable());
	}

	public boolean removeSource(State state, BlockPos pos) {
		return state.sources.remove(pos);
	}

	public int count(State state) {
		return state.sources.size();
	}

	public boolean isEmpty(State state) {
		return state.sources.isEmpty();
	}

	public List<BlockPos> snapshot(State state) {
		return List.copyOf(state.sources);
	}

	public Set<BlockPos> view(State state) {
		return Collections.unmodifiableSet(state.sources);
	}

	public void clearSources(State state) {
		state.sources.clear();
	}

	public void registerSource(State state, BlockPos pos) {
		ServerWorld world = host.world();
		if (!addSource(state, pos)) {
			return;
		}
		host.markDirty();
		if (!host.infectionState().infected()) {
			host.infectionLifecycle().startInfection(pos);
		}
	}

	public void unregisterSource(State state, BlockPos pos) {
		ServerWorld world = host.world();
		if (consumeSuppressed(state, pos)) {
			return;
		}
		if (!removeSource(state, pos)) {
			return;
		}
		host.markDirty();
		if (state.sources.isEmpty()) {
			host.sourceControl().endInfection();
		}
	}

	public void forceContainmentReset(State state) {
		ServerWorld world = host.world();
		List<BlockPos> snapshot = snapshot(state);
		host.shell().purgeHostilesAround(host.world(), snapshot, 32.0D);
		for (BlockPos pos : snapshot) {
			if (world.isChunkLoaded(ChunkPos.toLong(pos))) {
				world.breakBlock(pos, false);
			}
		}
		clearSources(state);
		host.infectionLifecycle().handleContainmentResetCleanup();
		clearSuppressed(state);
		host.sourceControl().endInfection();
	}

	public boolean teleportSources(State state, int chunkRadius, float teleportChance) {
		ServerWorld world = host.world();
		if (state.sources.isEmpty() || chunkRadius <= 0) {
			return false;
		}
		int radius = chunkRadius * 16;
		boolean moved = false;
		Random random = world.getRandom();
		for (BlockPos source : snapshot(state)) {
			if (random.nextFloat() > teleportChance) {
				continue;
			}
			BlockPos target = findTeleportTarget(world, source, radius, random);
			if (target == null) {
				continue;
			}
			if (teleportSource(world, state, source, target)) {
				moved = true;
			}
		}
		return moved;
	}

	public void endInfection() {
		ServerWorld world = host.world();
		host.sourceControl().endInfection();
	}

	public void removeMissingSources(State state) {
		ServerWorld world = host.world();
		boolean removed = false;
		Iterator<BlockPos> iterator = state.sources.iterator();
		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}
			if (host.singularity().fusing().isVirusCoreBlock(pos, world.getBlockState(pos))) {
				continue;
			}
			boolean singularityActive = host.singularityState().singularityState != SingularityState.DORMANT
					&& world.getBlockState(pos).isOf(ModBlocks.SINGULARITY_BLOCK);
			if (!singularityActive) {
				iterator.remove();
				host.markDirty();
				removed = true;
			}
		}
		if (removed && state.sources.isEmpty() && host.infectionState().infected()) {
			forceContainmentReset(state);
		}
	}

	public void addAll(State state, Iterable<BlockPos> positions) {
		for (BlockPos pos : positions) {
			state.sources.add(pos.toImmutable());
		}
	}

	public void restoreSnapshot(State state, Iterable<BlockPos> positions) {
		clearSources(state);
		addAll(state, positions);
	}

	@Nullable
	public BlockPos representativePos(Random random, State state) {
		ServerWorld world = host.world();
		if (!state.sources.isEmpty()) {
			List<BlockPos> list = snapshot(state);
			return list.get(random.nextInt(list.size()));
		}
		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			return null;
		}
		return players.get(random.nextInt(players.size())).getBlockPos();
	}

	public void suppressUnregister(State state, BlockPos pos) {
		state.suppressedUnregisters.add(pos.toImmutable());
	}

	private boolean consumeSuppressed(State state, BlockPos pos) {
		return state.suppressedUnregisters.remove(pos);
	}

	public void clearSuppressed(State state) {
		state.suppressedUnregisters.clear();
	}

	private boolean teleportSource(ServerWorld world, State state, BlockPos from, BlockPos to) {
		if (!host.singularity().fusing().isVirusCoreBlock(from, world.getBlockState(from))) {
			return false;
		}
		world.setBlockState(from, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(to, ModBlocks.VIRUS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		state.sources.remove(from);
		state.sources.add(to.toImmutable());
		return true;
	}

	@Nullable
	private BlockPos findTeleportTarget(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int attempts = 0; attempts < 16; attempts++) {
			int x = origin.getX() + random.nextBetween(-radius, radius);
			int z = origin.getZ() + random.nextBetween(-radius, radius);
			// Optimized: check chunk before allocating BlockPos
			if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
				continue;
			}
			BlockPos sample = new BlockPos(x, world.getBottomY(), z);
			BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, sample);
			BlockPos target = top;
			BlockPos below = target.down();
			if (target.getY() <= world.getBottomY()) {
				continue;
			}
			if (!world.getBlockState(target).isAir()) {
				continue;
			}
			if (!world.getBlockState(below).isSolidBlock(world, below)) {
				continue;
			}
			if (host.singularity().fusing().isVirusCoreBlock(target, world.getBlockState(target))
					|| world.getBlockState(target).isOf(ModBlocks.SINGULARITY_BLOCK)) {
				continue;
			}
			return target;
		}
		return null;
	}

	public void registerSource(BlockPos pos) {
		ServerWorld world = host.world();
		int before = host.sources().count(this.state);
		host.sources().registerSource(this.state, pos);
		int after = host.sources().count(this.state);
		if (after != before) {
			log("[source] registered {} in {} (total={})", pos, world(world), after);
		} else {
			log("[source] refreshed {} in {} (total={})", pos, world(world), after);
		}
	}

	public void unregisterSource(BlockPos pos) {
		ServerWorld world = host.world();
		int before = host.sources().count(this.state);
		host.sources().unregisterSource(this.state, pos);
		int after = host.sources().count(this.state);
		if (after < before) {
			log("[source] removed {} in {} (total={})", pos, world(world), after);
		} else {
			log("[source] skipped removal for {} in {} (total={})", pos, world(world), after);
		}
	}

	public void collapseShells() {
		ServerWorld world = host.world();
		log("[shell] collapse requested in {} (sources={})", world(world), host.sources().count(this.state));
		host.shell().collapse(world, host.getVirusSources());
	}

	public boolean teleportSources() {
		ServerWorld world = host.world();
		boolean moved = host.sourceControl().maybeTeleportSources();
		log("[teleport] result={} world={} (sources={})", moved, world(world), host.sources().count(this.state));
		return moved;
	}

	public void spawnCoreGuardians() {
		ServerWorld world = host.world();
		log("[guardian] spawn requested in {} (sources={})", world(world), host.sources().count(this.state));
		host.sourceControl().spawnCoreGuardians();
	}

	private void log(String message, Object... args) {
		logging.info(LogChannel.INFECTION, message, args);
	}

	private static Identifier world(ServerWorld world) {
		return world.getRegistryKey().getValue();
	}
}

