package net.cyberpunk042;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.cyberpunk042.infection.VirusInfectionSystem;
import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.registry.ModItemGroups;
import net.cyberpunk042.registry.ModItems;
import net.cyberpunk042.command.VirusDebugCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.cyberpunk042.network.SkyTintPayload;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class TheVirusBlock implements ModInitializer {
	public static final String MOD_ID = "the-virus-block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier SKY_TINT_PACKET = Identifier.of(MOD_ID, "sky_tint");
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_WAVE_FRIENDLY_FIRE =
			GameRuleRegistry.register("virusWaveFriendlyFire", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BLOCK_TELEPORT_ENABLED =
			GameRuleRegistry.register("virusBlockTeleportEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BLOCK_TELEPORT_RADIUS =
			GameRuleRegistry.register("virusBlockTeleportRadius", GameRules.Category.MISC, GameRuleFactory.createIntRule(16, 0, 64));
	public static final String CORRUPTED_WORM_TAG = MOD_ID + ":corrupted_worm";
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_PLAYER_RADIUS =
			GameRuleRegistry.register("virusSpreadPlayerRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(48, 8, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_PLAYER_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadPlayerAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(96, 1, 1024));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_SOURCE_RADIUS =
			GameRuleRegistry.register("virusSpreadSourceRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(64, 8, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_SOURCE_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadSourceAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(48, 1, 1024));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_VERTICAL_RADIUS =
			GameRuleRegistry.register("virusSpreadVerticalRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(16, 4, 80));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_INITIAL_RADIUS =
			GameRuleRegistry.register("virusSpreadInitialRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(96, 8, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_INITIAL_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadInitialAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(600, 1, 8192));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_CLEANSE_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadCleanseAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(1500, 1, 16384));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CORRUPTION_PROFILER =
			GameRuleRegistry.register("virusCorruptionProfiler", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MUTATION_ATTEMPTS =
			GameRuleRegistry.register("virusMutationAttemptsPerTick", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(64, 0, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MUTATION_RADIUS =
			GameRuleRegistry.register("virusMutationRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(16, 4, 64));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CHUNK_REWRITE_ON_LOAD =
			GameRuleRegistry.register("virusCorruptChunksOnLoad", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAPS_ENABLED =
			GameRuleRegistry.register("virusBoobytrapsEnabled", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_INFECTED =
			GameRuleRegistry.register("virusBoobytrapChanceInfected", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(35, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_INFECTIOUS =
			GameRuleRegistry.register("virusBoobytrapChanceInfectious", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(30, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_BACTERIA =
			GameRuleRegistry.register("virusBoobytrapChanceBacteria", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(27, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTIOUS_SPREAD_ATTEMPTS =
			GameRuleRegistry.register("virusInfectiousSpreadAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(12, 0, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTIOUS_SPREAD_RADIUS =
			GameRuleRegistry.register("virusInfectiousSpreadRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(6, 1, 64));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_SPREAD_ATTEMPTS =
			GameRuleRegistry.register("virusBacteriaSpreadAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(10, 0, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_SPREAD_RADIUS =
			GameRuleRegistry.register("virusBacteriaSpreadRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(5, 1, 64));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_PULSE_INTERVAL =
			GameRuleRegistry.register("virusBacteriaPulseInterval", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(80, 10, 400));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTED_BLOCK_EXPLODE_CHANCE =
			GameRuleRegistry.register("virusInfectedBlockExplodeChance", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(10, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_PLAYER_DAMAGE =
			GameRuleRegistry.register("virusBoobytrapPlayerDamage", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(8, 0, 40));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAP_DAMAGE_PLAYERS_ONLY =
			GameRuleRegistry.register("virusBoobytrapDamagePlayersOnly", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAP_DAMAGE_BLOCKS =
			GameRuleRegistry.register("virusBoobytrapDamageBlocks", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_INFECTED =
			GameRuleRegistry.register("virusBoobytrapPowerInfected", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(4, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_INFECTIOUS =
			GameRuleRegistry.register("virusBoobytrapPowerInfectious", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(3, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_BACTERIA =
			GameRuleRegistry.register("virusBoobytrapPowerBacteria", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(3, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_DAMAGE =
			GameRuleRegistry.register("virusMatrixCubeDamage", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(8, 0, 40));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_MAX_ACTIVE =
			GameRuleRegistry.register("virusMatrixCubeMaxActive", GameRules.Category.MOBS, GameRuleFactory.createIntRule(200, 1, 500));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_SPAWN_INTERVAL =
			GameRuleRegistry.register("virusMatrixCubeSpawnInterval", GameRules.Category.MOBS, GameRuleFactory.createIntRule(40, 20, 6000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_SPAWN_ATTEMPTS =
			GameRuleRegistry.register("virusMatrixCubeSpawnAttempts", GameRules.Category.MOBS, GameRuleFactory.createIntRule(20, 1, 200));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_TIER2_EVENTS_ENABLED =
			GameRuleRegistry.register("virusTier2EventsEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_TIER3_EXTRAS_ENABLED =
			GameRuleRegistry.register("virusTier3ExtrasEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_MUTATION_PULSE_ENABLED =
			GameRuleRegistry.register("virusEventMutationPulseEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_SKYFALL_ENABLED =
			GameRuleRegistry.register("virusEventSkyfallEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_COLLAPSE_SURGE_ENABLED =
			GameRuleRegistry.register("virusEventCollapseSurgeEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_PASSIVE_REVOLT_ENABLED =
			GameRuleRegistry.register("virusEventPassiveRevoltEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_MOB_BUFF_STORM_ENABLED =
			GameRuleRegistry.register("virusEventMobBuffStormEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_VIRUS_BLOOM_ENABLED =
			GameRuleRegistry.register("virusEventVirusBloomEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_VOID_TEAR_ENABLED =
			GameRuleRegistry.register("virusEventVoidTearEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_INVERSION_ENABLED =
			GameRuleRegistry.register("virusEventInversionEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_ENTITY_DUPLICATION_ENABLED =
			GameRuleRegistry.register("virusEventEntityDuplicationEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_SINGULARITY_ENABLED =
			GameRuleRegistry.register("virusEventSingularityEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_LIQUID_MUTATION_ENABLED =
			GameRuleRegistry.register("virusLiquidMutationEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(SkyTintPayload.ID, SkyTintPayload.CODEC);
		ModBlocks.bootstrap();
		ModItems.bootstrap();
		ModBlockEntities.bootstrap();
		ModEntities.bootstrap();
		ModItemGroups.bootstrap();
		VirusDebugCommands.register();
		VirusInfectionSystem.init();

		LOGGER.info("The Virus Block is primed. Containment is impossible.");
	}
}