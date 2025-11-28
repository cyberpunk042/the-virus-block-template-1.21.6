package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.config.InfectionLogConfig;
import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.config.SingularityConfig;
import net.cyberpunk042.config.SingularityConfig.CollapseBroadcastMode;
import net.cyberpunk042.config.SingularityConfig.CollapseSyncProfile;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

	private static final SuggestionProvider<ServerCommandSource> BROADCAST_MODE_SUGGESTIONS = (ctx, builder) -> {
		for (CollapseBroadcastMode mode : CollapseBroadcastMode.values()) {
			builder.suggest(mode.id());
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
		if (state.forceStartSingularity(world, seconds, fallback)) {
			source.sendFeedback(() -> Text.literal("Singularity countdown started (" + seconds + "s)."), true);
			return 1;
		}
		source.sendError(Text.literal("Unable to start singularity (no Virus Block center available)."));
		return 0;
	}

	private static int abortSingularity(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		if (state.abortSingularity(world)) {
			source.sendFeedback(() -> Text.literal("Singularity aborted."), true);
			return 1;
		}
		source.sendError(Text.literal("Singularity is not currently running."));
		return 0;
	}

	private static int reportSingularity(ServerCommandSource source) {
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		source.sendFeedback(() -> Text.literal("Singularity state: " + state.getSingularityState()
				+ " (ticks=" + state.getSingularityTicks() + ")"), false);
		return 1;
	}

	private static int setCollapseViewDistance(ServerCommandSource source, int chunks) {
		SingularityConfig.setCollapseViewDistance(chunks);
		source.sendFeedback(() -> Text.literal("Collapse view-distance override set to "
				+ (chunks <= 0 ? "disabled" : chunks + " chunks") + "."), true);
		return chunks;
	}

	private static int setCollapseSimulationDistance(ServerCommandSource source, int chunks) {
		SingularityConfig.setCollapseSimulationDistance(chunks);
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
		SingularityConfig.setCollapseBroadcastMode(mode);
		source.sendFeedback(() -> Text.literal("Collapse broadcast mode set to " + mode.id() + "."), true);
		return 1;
	}

	private static int setCollapseBroadcastRadius(ServerCommandSource source, int blocks) {
		SingularityConfig.setCollapseBroadcastRadius(blocks);
		source.sendFeedback(() -> Text.literal("Collapse broadcast radius set to "
				+ (blocks <= 0 ? "disabled" : blocks + " blocks") + "."), true);
		return blocks;
	}

	private static int setCollapseProfile(ServerCommandSource source, ServerPlayerEntity target, CollapseSyncProfile profile) {
		ServerWorld world = (ServerWorld) target.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		state.setCollapseProfile(target, profile);
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
		CollapseSyncProfile profile = state.getCollapseProfile(target);
		source.sendFeedback(() -> Text.literal(target.getName().getString() + " => " + profile.id()), false);
		return 1;
	}

	private static int setCollapseProfileDefault(ServerCommandSource source, CollapseSyncProfile profile) {
		SingularityConfig.setCollapseDefaultSyncProfile(profile);
		source.sendFeedback(() -> Text.literal("Collapse default sync profile set to " + profile.id() + "."), true);
		return 1;
	}

	private static CollapseSyncProfile requireProfile(String id) throws CommandSyntaxException {
		CollapseSyncProfile profile = CollapseSyncProfile.fromId(id);
		if (profile == null) {
			throw UNKNOWN_PROFILE.create(id);
		}
		return profile;
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
}

