package net.cyberpunk042.infection;

import java.util.Optional;

import net.cyberpunk042.infection.service.AmbientPressureService;
import net.cyberpunk042.infection.service.CollapseSnapshotService;
import net.cyberpunk042.infection.service.CollapseSnapshotService.SingularitySnapshot;
import net.cyberpunk042.infection.service.InfectionLifecycleService;
import net.cyberpunk042.infection.service.InfectionTierService;
import net.cyberpunk042.infection.service.VoidTearService;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.infection.service.VirusSourceService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class InfectionOperations {
	private final VirusWorldState state;
	private final InfectionLifecycleService infectionLifecycleService;
	private final AmbientPressureService ambientPressureService;
	private final CollapseSnapshotService snapshotService;
	private final VoidTearService voidTearService;

	InfectionOperations(VirusWorldState state,
			InfectionLifecycleService infectionLifecycleService,
			AmbientPressureService ambientPressureService,
			CollapseSnapshotService snapshotService,
			VoidTearService voidTearService) {
		this.state = state;
		this.infectionLifecycleService = infectionLifecycleService;
		this.ambientPressureService = ambientPressureService;
		this.snapshotService = snapshotService;
		this.voidTearService = voidTearService;
	}

	public void ensureDebugInfection(ServerWorld world, BlockPos center) {
		infectionLifecycleService.ensureDebugInfection(center);
	}

	public void applyDifficultyRules(ServerWorld world, InfectionTier tier) {
		ambientPressureService.applyDifficultyRules(tier);
	}

	public void broadcast(ServerWorld world, Text text) {
		world.getPlayers(ServerPlayerEntity::isAlive).forEach(player -> player.sendMessage(text, false));
	}

	public Optional<SingularitySnapshot> getSingularitySnapshot() {
		return snapshotService.buildSingularitySnapshot();
	}

	public void applySingularitySnapshot(SingularitySnapshot snapshot) {
		snapshotService.applySingularitySnapshot(snapshot);
	}

	public boolean spawnVoidTearForCommand(ServerWorld world, BlockPos center) {
		return voidTearService.spawnViaCommand(center);
	}

	public BlockPos representativePos(ServerWorld world, Random random, VirusSourceService.State sourceState) {
		return state.sources().representativePos(random, sourceState);
	}

	public boolean applyHealthDamage(ServerWorld world, double amount) {
		if (!state.infectionState().infected() || amount <= 0.0D) {
			return false;
		}

		InfectionTierService.HealthChange change = state.tiers().applyHealthDamage(state.tiers().currentTier(), amount);
		if (!change.changed()) {
			return false;
		}

		if (change.crossedHalfThreshold() && !state.shell().isCollapsed()) {
			state.shell().collapse(world, state.getVirusSources());
		}

		state.markDirty();
		state.presentationCoord().updateBossBars();
		if (change.depleted()) {
			state.sourceControl().forceContainmentReset();
		}
		return true;
	}

	public int claimSurfaceMutations(ServerWorld world, InfectionTier tier, int requested) {
		if (!state.infectionState().infected() || requested <= 0) {
			return 0;
		}
		return state.tiers().claimSurfaceMutations(world, tier, requested, state.infectionState().totalTicks());
	}

	/**
	 * Runs a single frame of active infection processing.
	 * Called each tick when infection is active.
	 * <p>
	 * NOTE: Consider adding try-catch around subsections for defensive error isolation.
	 * Currently, a failure in any step will abort the entire frame.
	 */
	public void runActiveFrame(ServerWorld world) {
		if (!state.infectionState().infected()) {
			return;
		}
		state.infectionState().incrementTicks();
		state.tiers().incrementTierTick();

		state.sources().removeMissingSources(state.sourceState());

		InfectionTier tier = state.tiers().currentTier();
		if (state.tiers().ensureHealthInitialized(tier)) {
			state.markDirty();
		}
		int tierDuration = state.tiers().duration(tier);
		boolean apocalypse = state.tiers().isApocalypseMode();

		if (!apocalypse && tierDuration > 0 && state.tiers().ticksInTier() >= tierDuration 
				&& state.singularityState().singularityState == SingularityState.DORMANT) {
			state.tierProgression().advanceTier();
		}

		if (state.infectionState().totalTicks() % 200 == 0 && state.tiers().degradeContainment()) {
			state.markDirty();
		}

		if (!state.singularity().fusing().isSingularityActive() 
				&& !state.singularity().fusing().shouldSkipSpread()) {
			BlockMutationHelper.mutateAroundSources(world, state.getVirusSources(), tier, apocalypse);
		}
		state.tierProgression().reinforceCores(tier);
		if (state.trySpawnMatrixCube()) {
			state.markDirty();
		}

		applyDifficultyRules(world, tier);
		state.singularity().barrier().tick(tier, tierDuration);
		state.combat().tick(world, tier);
	}
}
