package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.InfectionLogConfig;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SimpleVirusScheduler;
import net.cyberpunk042.infection.command.CommandFacade;
import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.profile.WaterDrainMode;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.VirusSchedulerService;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirusCommand {
	private VirusCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> singularityCommand = CommandManager.literal("singularity")
				.then(CommandManager.literal("start")
						.executes(ctx -> forceSingularity(ctx.getSource(), 60))
						.then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
								.executes(ctx -> forceSingularity(ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "seconds")))))
				.then(CommandManager.literal("abort").executes(ctx -> abortSingularity(ctx.getSource())))
				.then(CommandManager.literal("status").executes(ctx -> reportSingularity(ctx.getSource())))
				.then(CommandManager.literal("viewdistance")
						.then(CommandManager.argument("chunks", IntegerArgumentType.integer(0, 32))
								.executes(ctx -> setCollapseViewDistance(ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "chunks")))))
				.then(CommandManager.literal("simulationdistance")
						.then(CommandManager.argument("chunks", IntegerArgumentType.integer(0, 32))
								.executes(ctx -> setCollapseSimulationDistance(ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "chunks")))))
				.then(CommandManager.literal("broadcast")
						.then(CommandManager.literal("mode")
								.then(CommandManager.argument("mode", StringArgumentType.word())
										.suggests(BROADCAST_MODE_SUGGESTIONS)
										.executes(ctx -> setCollapseBroadcastMode(ctx.getSource(),
												StringArgumentType.getString(ctx, "mode")))))
						.then(CommandManager.literal("radius")
								.then(CommandManager.argument("blocks", IntegerArgumentType.integer(0, 512))
										.executes(ctx -> setCollapseBroadcastRadius(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "blocks")))))
				.then(CommandManager.literal("collapse")
						.then(CommandManager.literal("enable").executes(ctx -> setSingularityCollapseEnabled(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setSingularityCollapseEnabled(ctx.getSource(), false))))
				.then(CommandManager.literal("chunkgeneration")
						.then(CommandManager.literal("enable").executes(ctx -> setChunkGenerationAllowed(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setChunkGenerationAllowed(ctx.getSource(), false))))
				.then(CommandManager.literal("outsideborder")
						.then(CommandManager.literal("enable").executes(ctx -> setOutsideBorderAllowed(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setOutsideBorderAllowed(ctx.getSource(), false))))
				// Legacy commands removed: multithreaded, mode, workers (CollapseProcessor is now the only system)
				.then(CommandManager.literal("diagnostics")
						.then(CommandManager.literal("enable").executes(ctx -> setDiagnosticsEnabled(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setDiagnosticsEnabled(ctx.getSource(), false)))
						.then(CommandManager.literal("chunksamples")
								.then(CommandManager.literal("enable").executes(ctx -> setDiagnosticsChunkSamples(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setDiagnosticsChunkSamples(ctx.getSource(), false))))
						.then(CommandManager.literal("bypasses")
								.then(CommandManager.literal("enable").executes(ctx -> setDiagnosticsBypasses(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setDiagnosticsBypasses(ctx.getSource(), false))))
						.then(CommandManager.literal("interval")
								.then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 6000))
										.executes(ctx -> setDiagnosticsInterval(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "ticks")))))
						.then(CommandManager.literal("spam")
								.then(CommandManager.literal("enable").executes(ctx -> setDiagnosticsSpamEnabled(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setDiagnosticsSpamEnabled(ctx.getSource(), false)))
								.then(CommandManager.literal("persecond")
										.then(CommandManager.argument("threshold", IntegerArgumentType.integer(0, 1000))
												.executes(ctx -> setDiagnosticsSpamPerSecond(ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "threshold")))))
								.then(CommandManager.literal("perminute")
										.then(CommandManager.argument("threshold", IntegerArgumentType.integer(0, 60000))
												.executes(ctx -> setDiagnosticsSpamPerMinute(ctx.getSource(),
														IntegerArgumentType.getInteger(ctx, "threshold")))))
								.then(CommandManager.literal("suppress")
										.then(CommandManager.literal("enable").executes(ctx -> setDiagnosticsSpamSuppress(ctx.getSource(), true)))
										.then(CommandManager.literal("disable").executes(ctx -> setDiagnosticsSpamSuppress(ctx.getSource(), false))))))
				.then(CommandManager.literal("erosion")
						.then(CommandManager.literal("status").executes(ctx -> reportErosionStatus(ctx.getSource())))
						.then(CommandManager.literal("drainwater")
								.then(CommandManager.argument("mode", StringArgumentType.word())
										.suggests(WATER_DRAIN_MODE_SUGGESTIONS)
										.executes(ctx -> setWaterDrainMode(ctx.getSource(),
												StringArgumentType.getString(ctx, "mode")))))
						.then(CommandManager.literal("wateroffset")
								.then(CommandManager.argument("blocks", IntegerArgumentType.integer(0, 16))
										.executes(ctx -> setWaterDrainOffset(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "blocks")))))
						.then(CommandManager.literal("particles")
								.then(CommandManager.literal("enable").executes(ctx -> setCollapseParticles(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setCollapseParticles(ctx.getSource(), false))))
						.then(CommandManager.literal("fillmode")
								.then(CommandManager.argument("mode", StringArgumentType.word())
										.suggests(FILL_MODE_SUGGESTIONS)
										.executes(ctx -> setFillMode(ctx.getSource(),
												StringArgumentType.getString(ctx, "mode")))))
						.then(CommandManager.literal("fillshape")
								.then(CommandManager.argument("shape", StringArgumentType.word())
										.suggests(FILL_SHAPE_SUGGESTIONS)
										.executes(ctx -> setFillShape(ctx.getSource(),
												StringArgumentType.getString(ctx, "shape")))))
						.then(CommandManager.literal("outline")
								.then(CommandManager.argument("thickness", IntegerArgumentType.integer(1, 16))
										.executes(ctx -> setOutlineThickness(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "thickness")))))
						.then(CommandManager.literal("nativefill")
								.then(CommandManager.literal("enable").executes(ctx -> setUseNativeFill(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setUseNativeFill(ctx.getSource(), false))))
						.then(CommandManager.literal("protectedblocks")
								.then(CommandManager.literal("enable").executes(ctx -> setRespectProtectedBlocks(ctx.getSource(), true)))
								.then(CommandManager.literal("disable").executes(ctx -> setRespectProtectedBlocks(ctx.getSource(), false)))))
				.then(CommandManager.literal("profile")
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("player", EntityArgumentType.player())
										.then(CommandManager.argument("profile", StringArgumentType.word())
												.suggests(SYNC_PROFILE_SUGGESTIONS)
												.executes(ctx -> setCollapseProfile(ctx.getSource(),
														EntityArgumentType.getPlayer(ctx, "player"),
														requireProfile(StringArgumentType.getString(ctx, "profile")))))))
						.then(CommandManager.literal("get")
								.then(CommandManager.argument("player", EntityArgumentType.player())
										.executes(ctx -> reportCollapseProfile(ctx.getSource(),
												EntityArgumentType.getPlayer(ctx, "player")))))
						.then(CommandManager.literal("default")
								.then(CommandManager.argument("profile", StringArgumentType.word())
										.suggests(SYNC_PROFILE_SUGGESTIONS)
										.executes(ctx -> setCollapseProfileDefault(ctx.getSource(),
												requireProfile(StringArgumentType.getString(ctx, "profile"))))))));

		LiteralArgumentBuilder<ServerCommandSource> infectionCommand = CommandManager.literal("infection")
				.then(CommandManager.literal("scenario")
						.then(CommandManager.literal("list").executes(ctx -> listScenarios(ctx.getSource())))
						.then(CommandManager.literal("current").executes(ctx -> reportScenario(ctx.getSource())))
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("scenario", IdentifierArgumentType.identifier())
										.suggests(SCENARIO_SUGGESTIONS)
										.executes(ctx -> setScenario(ctx.getSource(),
												IdentifierArgumentType.getIdentifier(ctx, "scenario")))))
						.then(CommandManager.literal("unbind").executes(ctx -> unbindScenario(ctx.getSource()))))
				.then(CommandManager.literal("scheduler")
						.then(CommandManager.literal("status").executes(ctx -> reportSchedulerStatus(ctx.getSource()))))
				.then(CommandManager.literal("profile")
						.then(CommandManager.literal("reload").executes(ctx -> reloadProfiles(ctx.getSource()))))
				.then(CommandManager.literal("service")
						.then(CommandManager.literal("reload").executes(ctx -> reloadServices(ctx.getSource()))))
				.then(CommandManager.literal("singularity")
						.then(CommandManager.literal("state").executes(ctx -> reportInfectionSingularity(ctx.getSource()))));

		dispatcher.register(CommandManager.literal("virusblock")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("teleport")
						.then(CommandManager.literal("enable").executes(ctx -> setTeleportEnabled(ctx.getSource(), true)))
						.then(CommandManager.literal("disable").executes(ctx -> setTeleportEnabled(ctx.getSource(), false)))
						.then(CommandManager.literal("radius")
								.then(CommandManager.argument("chunks", IntegerArgumentType.integer(0, 64))
										.executes(ctx -> setTeleportRadius(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "chunks")))))
						.then(CommandManager.literal("status").executes(ctx -> reportTeleport(ctx.getSource()))))
				.then(singularityCommand)
				.then(infectionCommand)
				.then(CommandManager.literal("fuse")
						.then(CommandManager.literal("test").executes(ctx -> spawnFuseTest(ctx.getSource()))))
				.then(CommandManager.literal("logs")
						.then(CommandManager.literal("list").executes(ctx -> listLogChannels(ctx.getSource())))
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("channel", StringArgumentType.word())
										.suggests(LOG_CHANNEL_SUGGESTIONS)
										.then(CommandManager.argument("enabled", BoolArgumentType.bool())
												.executes(ctx -> setLogChannel(ctx.getSource(),
														StringArgumentType.getString(ctx, "channel"),
														BoolArgumentType.getBool(ctx, "enabled"))))))
						.then(CommandManager.literal("reload").executes(ctx -> reloadLogChannels(ctx.getSource())))));

		VirusDifficultyCommand.register(dispatcher);
		VirusStatsCommand.register(dispatcher);
	}

	private static final SuggestionProvider<ServerCommandSource> LOG_CHANNEL_SUGGESTIONS = (ctx, builder) -> {
		for (LogChannel channel : LogChannel.values()) {
			builder.suggest(channel.id());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> WATER_DRAIN_MODE_SUGGESTIONS = (ctx, builder) -> {
		for (WaterDrainMode mode : WaterDrainMode.values()) {
			builder.suggest(mode.name().toLowerCase(Locale.ROOT));
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> SCENARIO_SUGGESTIONS = (ctx, builder) -> {
		CommandFacade facade = new CommandFacade(VirusWorldState.get(ctx.getSource().getWorld()));
		facade.registeredScenarioIds().forEach(id -> builder.suggest(id.toString()));
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> BROADCAST_MODE_SUGGESTIONS = (ctx, builder) -> {
		for (CollapseBroadcastMode mode : CollapseBroadcastMode.values()) {
			builder.suggest(mode.id());
		}
		return builder.buildFuture();
	};

	// EXECUTION_MODE_SUGGESTIONS removed - CollapseProcessor is now the only system

	private static final SuggestionProvider<ServerCommandSource> FILL_MODE_SUGGESTIONS = (ctx, builder) -> {
		for (CollapseFillMode mode : CollapseFillMode.values()) {
			builder.suggest(mode.id());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> FILL_SHAPE_SUGGESTIONS = (ctx, builder) -> {
		for (CollapseFillShape shape : CollapseFillShape.values()) {
			builder.suggest(shape.id());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> SYNC_PROFILE_SUGGESTIONS = (ctx, builder) -> {
		for (CollapseSyncProfile profile : CollapseSyncProfile.values()) {
			builder.suggest(profile.id());
		}
		return builder.buildFuture();
	};

	private static final DynamicCommandExceptionType UNKNOWN_PROFILE = new DynamicCommandExceptionType(value ->
			Text.literal("Unknown sync profile: " + value));
	private static final DynamicCommandExceptionType UNKNOWN_FILL_MODE = new DynamicCommandExceptionType(value ->
			Text.literal("Unknown fill mode: " + value));
	private static final DynamicCommandExceptionType UNKNOWN_FILL_SHAPE = new DynamicCommandExceptionType(value ->
			Text.literal("Unknown fill shape: " + value));

	private static int setTeleportEnabled(ServerCommandSource source, boolean enabled) {
		source.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED).set(enabled, source.getServer());
		source.sendFeedback(() -> Text.literal("Virus block teleportation set to " + enabled), true);
		return enabled ? 1 : 0;
	}

	private static int setTeleportRadius(ServerCommandSource source, int chunks) {
		source.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS).set(chunks, source.getServer());
		source.sendFeedback(() -> Text.literal("Virus block teleport radius set to " + chunks + " chunks"), true);
		return chunks;
	}

	private static int reportTeleport(ServerCommandSource source) {
		boolean enabled = source.getServer().getGameRules().getBoolean(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED);
		int radius = source.getServer().getGameRules().getInt(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS);
		source.sendFeedback(() -> Text.literal("Virus block teleportation is " + (enabled ? "enabled" : "disabled")
				+ " (radius " + radius + " chunks)"), false);
		return enabled ? 1 : 0;
	}

	private static int forceSingularity(ServerCommandSource source, int seconds) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		BlockPos fallback = BlockPos.ofFloored(source.getPosition());
		if (source.getEntity() != null) {
			fallback = source.getEntity().getBlockPos();
		}
		if (state.collapse().forceStartSingularity(world, seconds, fallback)) {
			source.sendFeedback(() -> Text.literal("Singularity countdown started (" + seconds + "s)."), true);
			return 1;
		}
		source.sendError(Text.literal("Unable to start singularity (no Virus Block center available)."));
		return 0;
	}

	private static int abortSingularity(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		if (state.singularity().lifecycle().abortSingularity()) {
			source.sendFeedback(() -> Text.literal("Singularity aborted."), true);
			return 1;
		}
		source.sendError(Text.literal("Singularity is not currently running."));
		return 0;
	}

	private static int reportSingularity(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		source.sendFeedback(() -> Text.literal("Singularity state: " + state.singularityState().singularityState
				+ " (ticks=" + state.singularityState().singularityTicks + ")"), false);
		return 1;
	}

	private static int setCollapseViewDistance(ServerCommandSource source, int chunks) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseViewDistance(source.getWorld(), chunks)) {
			source.sendError(Text.literal("Failed to update collapse profile; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse view-distance override set to "
				+ (chunks <= 0 ? "disabled" : chunks + " chunks") + "."), true);
		return chunks;
	}

	private static int setCollapseSimulationDistance(ServerCommandSource source, int chunks) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseSimulationDistance(source.getWorld(), chunks)) {
			source.sendError(Text.literal("Failed to update collapse profile; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse simulation-distance override set to "
				+ (chunks <= 0 ? "disabled" : chunks + " chunks") + "."), true);
		return chunks;
	}

	private static int setCollapseBroadcastMode(ServerCommandSource source, String id) {
		CollapseBroadcastMode mode = CollapseBroadcastMode.fromId(id);
		if (mode == null) {
			source.sendError(Text.literal("Unknown broadcast mode: " + id));
			return 0;
		}
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseBroadcastMode(source.getWorld(), mode)) {
			source.sendError(Text.literal("Failed to update collapse profile; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse broadcast mode set to " + mode.id() + "."), true);
		return 1;
	}

	private static int setCollapseBroadcastRadius(ServerCommandSource source, int blocks) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseBroadcastRadius(source.getWorld(), blocks)) {
			source.sendError(Text.literal("Failed to update collapse profile; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse broadcast radius set to "
				+ (blocks <= 0 ? "disabled" : blocks + " blocks") + "."), true);
		return blocks;
	}

	private static int setCollapseProfile(ServerCommandSource source, ServerPlayerEntity target, CollapseSyncProfile profile) {
		ServerWorld world = (ServerWorld) target.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		state.presentationCoord().setCollapseProfile(target, profile);
		String message = "Singularity sync profile for " + target.getName().getString() + " set to " + profile.id();
		source.sendFeedback(() -> Text.literal(message), true);
		if (source.getPlayer() != target) {
			target.sendMessage(Text.literal("Your singularity sync profile is now " + profile.id()), false);
		}
		return 1;
	}

	private static int reportCollapseProfile(ServerCommandSource source, ServerPlayerEntity target) {
		ServerWorld world = (ServerWorld) target.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		CollapseSyncProfile profile = state.presentationCoord().getCollapseProfile(target);
		source.sendFeedback(() -> Text.literal(target.getName().getString() + " => " + profile.id()), false);
		return 1;
	}

	private static int setCollapseProfileDefault(ServerCommandSource source, CollapseSyncProfile profile) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseDefaultProfile(source.getWorld(), profile)) {
			source.sendError(Text.literal("Failed to update collapse profile; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse default sync profile set to " + profile.id() + "."), true);
		return 1;
	}

	private static int setSingularityCollapseEnabled(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setCollapseEnabled(enabled)) {
			source.sendError(Text.literal("Failed to update singularity execution settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Singularity collapse " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setChunkGenerationAllowed(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setChunkGenerationAllowed(enabled)) {
			source.sendError(Text.literal("Failed to update singularity execution settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Singularity chunk generation "
				+ (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setOutsideBorderAllowed(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setOutsideBorderLoadAllowed(enabled)) {
			source.sendError(Text.literal("Failed to update singularity execution settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Outside-border loading "
				+ (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	// Legacy methods removed: setMultithreadedCollapse, setSingularityExecutionMode, setSingularityWorkerCount
	// CollapseProcessor is now the only collapse system

	private static int reportErosionStatus(ServerCommandSource source) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		boolean reported = facade.describeErosion(source.getWorld(), collapse -> {
			source.sendFeedback(() -> Text.literal("Erosion settings:"), false);
			source.sendFeedback(() -> Text
					.literal(" - water_drain_mode = " + collapse.waterDrainMode().name().toLowerCase(Locale.ROOT)), false);
			source.sendFeedback(() -> Text.literal(" - water_drain_offset = " + collapse.waterDrainOffset()), false);
			DimensionProfile.Collapse.WaterDrainDeferred deferred = collapse.waterDrainDeferred();
			source.sendFeedback(() -> Text.literal("   deferred.enabled = " + deferred.enabled()
					+ ", initial_delay_ticks = " + deferred.initialDelayTicks()
					+ ", columns_per_tick = " + deferred.columnsPerTick()), false);
			DimensionProfile.Collapse.PreCollapseWaterDrainage preDrain = collapse.preCollapseWaterDrainage();
			source.sendFeedback(() -> Text.literal(" - pre_collapse_water_drainage.enabled = " + preDrain.enabled()), false);
			source.sendFeedback(() -> Text.literal("   mode = " + preDrain.mode().name().toLowerCase(Locale.ROOT)
					+ ", tick_rate = " + preDrain.tickRate()
					+ ", batch_size = " + preDrain.batchSize()
					+ ", start_delay_ticks = " + preDrain.startDelayTicks()
					+ ", start_from_center = " + preDrain.startFromCenter()), false);
			source.sendFeedback(() -> Text.literal(" - collapse_particles = " + collapse.collapseParticles()), false);
			source.sendFeedback(() -> Text.literal(" - fill_mode = " + collapse.fillMode().id()), false);
			source.sendFeedback(() -> Text.literal(" - fill_shape = " + collapse.fillShape().id()), false);
			source.sendFeedback(() -> Text.literal(" - outline_thickness = " + collapse.outlineThickness()), false);
			source.sendFeedback(() -> Text.literal(" - use_native_fill = " + collapse.useNativeFill()), false);
			source.sendFeedback(() -> Text.literal(" - respect_protected_blocks = " + collapse.respectProtectedBlocks()), false);
		});
		if (!reported) {
			source.sendError(Text.literal("Failed to read erosion settings; check server logs."));
			return 0;
		}
		return 1;
	}

	private static int setWaterDrainMode(ServerCommandSource source, String modeId) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		WaterDrainMode mode = parseWaterDrainMode(modeId);
		if (!facade.setWaterDrainMode(source.getWorld(), mode)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(
				() -> Text.literal("Water drain mode set to " + mode.name().toLowerCase(Locale.ROOT) + "."), true);
		return 1;
	}

	private static int setWaterDrainOffset(ServerCommandSource source, int blocks) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setWaterDrainOffset(source.getWorld(), blocks)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Water drain offset set to " + blocks + " blocks."), true);
		return blocks;
	}

	private static int setCollapseParticles(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setCollapseParticles(source.getWorld(), enabled)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse particles " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setFillMode(ServerCommandSource source, String id) throws CommandSyntaxException {
		CollapseFillMode mode = requireFillMode(id);
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setFillMode(source.getWorld(), mode)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse fill mode set to " + mode.id() + "."), true);
		return 1;
	}

	private static int setFillShape(ServerCommandSource source, String id) throws CommandSyntaxException {
		CollapseFillShape shape = requireFillShape(id);
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setFillShape(source.getWorld(), shape)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Collapse fill shape set to " + shape.id() + "."), true);
		return 1;
	}

	private static int setOutlineThickness(ServerCommandSource source, int thickness) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setOutlineThickness(source.getWorld(), thickness)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Outline thickness set to " + thickness + " blocks."), true);
		return thickness;
	}

	private static int setUseNativeFill(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setUseNativeFill(source.getWorld(), enabled)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Native fill " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setRespectProtectedBlocks(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!ensureScenarioBound(source, facade)) {
			return 0;
		}
		if (!facade.setRespectProtectedBlocks(source.getWorld(), enabled)) {
			source.sendError(Text.literal("Failed to update erosion settings; check server logs."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Protected-block respect " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setDiagnosticsEnabled(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsEnabled(enabled)) {
			source.sendError(Text.literal("Failed to update diagnostics settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Singularity diagnostics " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int setDiagnosticsChunkSamples(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsChunkSamples(enabled)) {
			source.sendError(Text.literal("Failed to update diagnostics settings."));
			return 0;
		}
		source.sendFeedback(
				() -> Text.literal("Collapse chunk sample logging " + (enabled ? "enabled." : "disabled.")),
				true);
		return 1;
	}

	private static int setDiagnosticsBypasses(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsBypasses(enabled)) {
			source.sendError(Text.literal("Failed to update diagnostics settings."));
			return 0;
		}
		source.sendFeedback(
				() -> Text.literal("Collapse bypass logging " + (enabled ? "enabled." : "disabled.")),
				true);
		return 1;
	}

	private static int setDiagnosticsInterval(ServerCommandSource source, int ticks) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsSampleInterval(ticks)) {
			source.sendError(Text.literal("Failed to update diagnostics settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Diagnostics sample interval set to " + ticks + " ticks."), true);
		return ticks;
	}

	private static int setDiagnosticsSpamEnabled(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsSpamEnabled(enabled)) {
			source.sendError(Text.literal("Failed to update diagnostics spam settings."));
			return 0;
		}
		source.sendFeedback(
				() -> Text.literal("Singularity log spam watchdog " + (enabled ? "enabled." : "disabled.")),
				true);
		return 1;
	}

	private static int setDiagnosticsSpamPerSecond(ServerCommandSource source, int threshold) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsSpamPerSecond(threshold)) {
			source.sendError(Text.literal("Failed to update diagnostics spam settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Per-second spam threshold set to " + threshold + "."), true);
		return threshold;
	}

	private static int setDiagnosticsSpamPerMinute(ServerCommandSource source, int threshold) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsSpamPerMinute(threshold)) {
			source.sendError(Text.literal("Failed to update diagnostics spam settings."));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Per-minute spam threshold set to " + threshold + "."), true);
		return threshold;
	}

	private static int setDiagnosticsSpamSuppress(ServerCommandSource source, boolean enabled) {
		CommandFacade facade = commandFacade(source);
		if (!facade.setDiagnosticsSpamSuppress(enabled)) {
			source.sendError(Text.literal("Failed to update diagnostics spam settings."));
			return 0;
		}
		source.sendFeedback(
				() -> Text.literal("Spam suppression " + (enabled ? "enabled." : "disabled.")),
				true);
		return 1;
	}

	private static int listScenarios(ServerCommandSource source) {
		CommandFacade facade = commandFacade(source);
		Set<Identifier> scenarios = facade.registeredScenarioIds();
		if (scenarios.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No infection scenarios are registered."), false);
			return 0;
		}
		Identifier effective = facade.effectiveScenario(source.getWorld()).orElse(null);
		Identifier active = facade.activeScenarioId().orElse(null);
		source.sendFeedback(() -> Text.literal("Registered infection scenarios:"), false);
		scenarios.stream()
				.sorted(Comparator.comparing(Identifier::toString))
				.forEach(id -> {
					boolean isEffective = id != null && id.equals(effective);
					boolean isActive = id != null && id.equals(active);
					StringBuilder label = new StringBuilder(" - ").append(id);
					if (isEffective) {
						label.append(" [dimension]");
					}
					if (isActive) {
						label.append(isEffective ? ", active" : " [active]");
					}
					source.sendFeedback(() -> Text.literal(label.toString()), false);
				});
		return scenarios.size();
	}

	private static int reportScenario(ServerCommandSource source) {
		CommandFacade facade = commandFacade(source);
		Identifier effective = facade.effectiveScenario(source.getWorld()).orElse(null);
		Identifier active = facade.activeScenarioId().orElse(null);
		var explicit = facade.boundScenario(source.getWorld().getRegistryKey());
		if (effective == null) {
			source.sendError(Text.literal("This dimension does not have an infection scenario registered."));
			return 0;
		}
		if (explicit.isPresent()) {
			source.sendFeedback(() -> Text.literal("Scenario binding: " + explicit.get()), false);
		} else {
			source.sendFeedback(() -> Text.literal("Scenario binding: <default>"), false);
		}
		source.sendFeedback(() -> Text.literal("Effective scenario: " + effective), false);
		if (active != null) {
			source.sendFeedback(() -> Text.literal("Active controller: " + active), false);
		} else {
			source.sendFeedback(() -> Text.literal("No scenario is currently attached (host idle)."), false);
		}
		return 1;
	}

	private static int setScenario(ServerCommandSource source, Identifier scenarioId) {
		CommandFacade facade = commandFacade(source);
		if (!facade.bindScenario(source.getWorld(), scenarioId)) {
			source.sendError(Text.literal("Unknown infection scenario: " + scenarioId));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Scenario for this dimension set to " + scenarioId
				+ " (will take effect next tick)."), true);
		return 1;
	}

	private static int unbindScenario(ServerCommandSource source) {
		CommandFacade facade = commandFacade(source);
		boolean hadBinding = facade.unbindScenario(source.getWorld());
		if (hadBinding) {
			source.sendFeedback(() -> Text.literal("Scenario binding cleared; default scenario will be used."), true);
		} else {
			source.sendFeedback(() -> Text.literal("No explicit scenario binding was set; default remains in use."), false);
		}
		return 1;
	}

	private static CommandFacade commandFacade(ServerCommandSource source) {
		return new CommandFacade(VirusWorldState.get(source.getWorld()));
	}

	private static boolean ensureScenarioBound(ServerCommandSource source, CommandFacade facade) {
		if (facade.effectiveScenario(source.getWorld()).isEmpty()) {
			source.sendError(Text.literal("No infection scenario is active for this dimension."));
			return false;
		}
		return true;
	}

	private static CollapseSyncProfile requireProfile(String id) throws CommandSyntaxException {
		CollapseSyncProfile profile = CollapseSyncProfile.fromId(id);
		if (profile == null) {
			throw UNKNOWN_PROFILE.create(id);
		}
		return profile;
	}

	private static CollapseFillMode requireFillMode(String id) throws CommandSyntaxException {
		CollapseFillMode mode = CollapseFillMode.fromId(id);
		if (mode == null) {
			throw UNKNOWN_FILL_MODE.create(id);
		}
		return mode;
	}

	private static CollapseFillShape requireFillShape(String id) throws CommandSyntaxException {
		CollapseFillShape shape = CollapseFillShape.fromId(id);
		if (shape == null) {
			throw UNKNOWN_FILL_SHAPE.create(id);
		}
		return shape;
	}

	private static WaterDrainMode parseWaterDrainMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return WaterDrainMode.OFF;
		}
		try {
			return WaterDrainMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return WaterDrainMode.OFF;
		}
	}

	private static int spawnFuseTest(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		Vec3d pos = source.getPosition();
		Vec3d left = pos.add(-1.5D, 0.0D, 0.0D);
		Vec3d right = pos.add(1.5D, 0.0D, 0.0D);

		TntEntity vanilla = new TntEntity(world, left.x, left.y, left.z, null);
		vanilla.setFuse(80);
		world.spawnEntity(vanilla);

		BlockPos fusePos = BlockPos.ofFloored(right);
		VirusFuseEntity virusFuse = new VirusFuseEntity(world, fusePos);
		virusFuse.setFuse(200);
		if (!world.spawnEntity(virusFuse)) {
			source.sendError(Text.literal("Failed to spawn Virus Fuse entity."));
			return 0;
		}

		source.sendFeedback(() -> Text.literal("Spawned vanilla TNT fuse (left) and Virus fuse (right)."), true);
		return 1;
	}

	private static int listLogChannels(ServerCommandSource source) {
		var snapshot = InfectionLogConfig.snapshot();
		if (snapshot.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No log channels are registered."), false);
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Infection log channels:"), false);
		snapshot.forEach((id, enabled) ->
				source.sendFeedback(() -> Text.literal(" - " + id + ": " + (enabled ? "enabled" : "disabled")), false));
		return snapshot.size();
	}

	private static int setLogChannel(ServerCommandSource source, String id, boolean enabled) {
		LogChannel channel = InfectionLogConfig.findChannel(id);
		if (channel == null) {
			source.sendError(Text.literal("Unknown log channel: " + id));
			return 0;
		}
		InfectionLogConfig.setEnabled(channel, enabled);
		source.sendFeedback(() -> Text.literal("Log channel '" + channel.id() + "' set to " + enabled + "."), true);
		return enabled ? 1 : 0;
	}

	private static int reloadLogChannels(ServerCommandSource source) {
		InfectionLogConfig.reload();
		source.sendFeedback(() -> Text.literal("Reloaded infection log configuration."), true);
		return 1;
	}

	private static int reloadProfiles(ServerCommandSource source) {
		CommandFacade facade = commandFacade(source);
		facade.reloadProfiles(source.getWorld());
		source.sendFeedback(() -> Text.literal("Reloaded dimension profiles; scenarios will reattach next tick."), true);
		return 1;
	}

	private static int reloadServices(ServerCommandSource source) {
		InfectionServices.reload();
		source.sendFeedback(() -> Text.literal("Reloaded infection service container configuration."), true);
		return 1;
	}

	private static int reportSchedulerStatus(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		var diagnostics = state.orchestrator().services().schedulerService().diagnostics();
		String label = String.format(Locale.ROOT,
				"Scheduler %s backlog=%d%s",
				diagnostics.implementation(),
				diagnostics.backlog(),
				diagnostics.usingFallback() ? " [default]" : "");
		source.sendFeedback(() -> Text.literal(label), false);
		List<SimpleVirusScheduler.TaskSnapshot> persisted = diagnostics.persistedTasks();
		if (persisted.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No persisted tasks queued."), false);
			return diagnostics.backlog();
		}
		source.sendFeedback(() -> Text.literal("Persisted tasks:"), false);
		int limit = Math.min(5, persisted.size());
		for (int i = 0; i < limit; i++) {
			SimpleVirusScheduler.TaskSnapshot snapshot = persisted.get(i);
			int remaining = Math.max(0, snapshot.remainingTicks());
			int index = i + 1;
			String line = String.format(Locale.ROOT, "  #%d %s (%d ticks)", index, snapshot.type(), remaining);
			source.sendFeedback(() -> Text.literal(line), false);
		}
		if (persisted.size() > limit) {
			int remaining = persisted.size() - limit;
			source.sendFeedback(() -> Text.literal(" ... plus " + remaining + " more task(s)."), false);
		}
		return diagnostics.backlog();
	}

	private static int reportInfectionSingularity(ServerCommandSource source) {
		return reportSingularity(source);
	}
}

