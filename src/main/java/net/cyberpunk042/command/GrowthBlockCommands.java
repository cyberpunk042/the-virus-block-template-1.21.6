package net.cyberpunk042.command;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.scheduler.GrowthField;
import net.cyberpunk042.growth.scheduler.GrowthMutation;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.growth.scheduler.GrowthScheduler;
import net.cyberpunk042.infection.service.ConfigService;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class GrowthBlockCommands {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final double DEFAULT_TRACE_DISTANCE = 8.0D;
	private static final Map<String, GrowthField> FIELD_LOOKUP = Arrays.stream(GrowthField.values())
			.collect(Collectors.toMap(field -> field.name().toLowerCase(Locale.ROOT), field -> field));
	private static final Map<GrowthField, String> DEFINITION_JSON_KEYS = buildDefinitionJsonKeyMap();
	private static final SimpleCommandExceptionType NO_GROWTH_STACK = new SimpleCommandExceptionType(Text.literal("Hold a progressive growth block in either hand."));
	private static final SimpleCommandExceptionType NO_BLOCK_TARGET = new SimpleCommandExceptionType(Text.literal("Look at a progressive growth block within reach."));
	private static final SimpleCommandExceptionType NO_TARGET = new SimpleCommandExceptionType(Text.literal("No growth block target found. Look at one or hold one in hand."));
	private static final SimpleCommandExceptionType EMPTY_MUTATION = new SimpleCommandExceptionType(Text.literal("Provide at least one field=value pair."));
	private static final DynamicCommandExceptionType UNKNOWN_FIELD = new DynamicCommandExceptionType(name -> Text.literal("Unknown growth field: " + name));
	private static final DynamicCommandExceptionType INVALID_VALUE = new DynamicCommandExceptionType(value -> Text.literal("Invalid value for growth override: " + value));
	private static final DynamicCommandExceptionType MALFORMED_PAIR = new DynamicCommandExceptionType(token ->
			Text.literal("Expected field=value pairs but found '" + token + "'"));
	private static final DynamicCommandExceptionType UNKNOWN_DEFINITION = new DynamicCommandExceptionType(id -> Text.literal("Unknown growth definition: " + id));
	private static final DynamicCommandExceptionType DEFAULTS_FILE_MISSING = new DynamicCommandExceptionType(id -> Text.literal("Could not locate a JSON file for definition " + id + " under config/the-virus-block/growth_blocks."));
	private static final DynamicCommandExceptionType DEFAULTS_IO_ERROR = new DynamicCommandExceptionType(path -> Text.literal("Failed to update defaults: " + path));

	private GrowthBlockCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(GrowthBlockCommands::registerInternal);
	}

	private static void registerInternal(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
		dispatcher.register(literal("growthblock")
				.requires(source -> source.hasPermissionLevel(2))
				.then(literal("reload").executes(GrowthBlockCommands::reloadConfigs))
				.then(literal("list").executes(GrowthBlockCommands::listDefinitions))
				.then(literal("apply")
						.then(literal("block")
								.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
										.executes(ctx -> applyToBlock(ctx, StringArgumentType.getString(ctx, "mutations")))))
						.then(literal("hand")
								.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
										.executes(ctx -> applyToHand(ctx, StringArgumentType.getString(ctx, "mutations")))))
						.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
								.executes(ctx -> applyAuto(ctx, StringArgumentType.getString(ctx, "mutations")))))
				.then(literal("schedule")
						.then(literal("block")
								.then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
										.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
												.executes(ctx -> scheduleBlock(ctx,
														IntegerArgumentType.getInteger(ctx, "delay"),
														StringArgumentType.getString(ctx, "mutations"))))))
						.then(literal("pos")
								.then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
										.then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
												.then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
														.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
																.executes(ctx -> scheduleAtPosition(ctx,
																		DimensionArgumentType.getDimensionArgument(ctx, "dimension"),
																		BlockPosArgumentType.getLoadedBlockPos(ctx, "position"),
																		IntegerArgumentType.getInteger(ctx, "delay"),
																		StringArgumentType.getString(ctx, "mutations"))))))))
						.then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
								.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
										.executes(ctx -> scheduleAuto(ctx,
												IntegerArgumentType.getInteger(ctx, "delay"),
												StringArgumentType.getString(ctx, "mutations"))))))
				.then(literal("inspect")
						.then(literal("block").executes(GrowthBlockCommands::inspectBlock))
						.then(literal("hand").executes(GrowthBlockCommands::inspectHand))
						.executes(GrowthBlockCommands::inspectAuto))
				.then(literal("defaults")
						.then(literal("show")
								.then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
										.executes(ctx -> showDefaults(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition")))))
						.then(literal("set")
								.then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
										.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
												.executes(ctx -> setDefaults(ctx,
														IdentifierArgumentType.getIdentifier(ctx, "definition"),
														StringArgumentType.getString(ctx, "mutations"))))))
						.then(literal("reload").executes(GrowthBlockCommands::reloadConfigs)))
				.then(literal("give")
						.then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
								.executes(ctx -> giveToSource(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition"), 1))
								.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(ctx -> giveToSource(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition"),
												IntegerArgumentType.getInteger(ctx, "count"))))
								.then(CommandManager.argument("target", EntityArgumentType.player())
										.executes(ctx -> giveToPlayer(ctx,
												EntityArgumentType.getPlayer(ctx, "target"),
												IdentifierArgumentType.getIdentifier(ctx, "definition"),
												1))
										.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(ctx -> giveToPlayer(ctx,
														EntityArgumentType.getPlayer(ctx, "target"),
														IdentifierArgumentType.getIdentifier(ctx, "definition"),
														IntegerArgumentType.getInteger(ctx, "count"))))))));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> literal(String name) {
		return CommandManager.literal(name);
	}

	private static int reloadConfigs(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		InfectionServices.reload();
		ctx.getSource().sendFeedback(() -> Text.literal("Growth configs reloaded."), true);
		return 1;
	}

	private static int listDefinitions(CommandContext<ServerCommandSource> ctx) {
		GrowthRegistry registry = InfectionServices.get().growth();
		ctx.getSource().sendFeedback(() -> Text.literal("Loaded definitions: " + registry.definitionIds()), false);
		return registry.definitionIds().size();
	}

	private static int applyAuto(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
		if (block != null) {
			return applyMutationToBlock(ctx, block, parsed);
		}
		StackTarget target = findHeldGrowthBlock(player);
		if (target != null) {
			return applyMutationToStack(ctx, target, parsed);
		}
		throw NO_TARGET.create();
	}

	private static int applyToBlock(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		ProgressiveGrowthBlockEntity block = requireGrowthBlock(ctx.getSource());
		return applyMutationToBlock(ctx, block, parsed);
	}

	private static int applyToHand(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		StackTarget target = findHeldGrowthBlock(player);
		if (target == null) {
			throw NO_GROWTH_STACK.create();
		}
		return applyMutationToStack(ctx, target, parsed);
	}

	private static int inspectAuto(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
		if (block != null) {
			return inspectBlock(ctx);
		}
		StackTarget target = findHeldGrowthBlock(player);
		if (target != null) {
			return inspectHand(ctx);
		}
		throw NO_TARGET.create();
	}

	private static int inspectBlock(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
		if (block == null) {
			throw NO_BLOCK_TARGET.create();
		}
		GrowthOverrides overrides = block.overridesSnapshot();
		reportDefinition(ctx.getSource(),
				"Block at " + formatPos(block.getPos()),
				block.getDefinitionId(),
				block.definitionSnapshot(),
				overrides);
		return 1;
	}

	private static int inspectHand(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		StackTarget target = findHeldGrowthBlock(player);
		if (target == null) {
			throw NO_GROWTH_STACK.create();
		}
		GrowthRegistry registry = InfectionServices.get().growth();
		Identifier definitionId = ProgressiveGrowthBlock.readDefinitionId(target.stack());
		if (definitionId == null) {
			definitionId = registry.defaultDefinition().id();
		}
		GrowthBlockDefinition base = registry.definition(definitionId);
		GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(target.stack());
		GrowthBlockDefinition applied = overrides.isEmpty() ? base : overrides.apply(base);
		reportDefinition(ctx.getSource(),
				"Hand (" + target.hand().name().toLowerCase(Locale.ROOT) + ")",
				definitionId,
				applied,
				overrides);
		return 1;
	}

	private static int scheduleAuto(CommandContext<ServerCommandSource> ctx, int delay, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
		if (block == null) {
			throw NO_BLOCK_TARGET.create();
		}
		return scheduleForEntity(ctx, block, parsed, delay);
	}

	private static int scheduleBlock(CommandContext<ServerCommandSource> ctx, int delay, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		ProgressiveGrowthBlockEntity block = requireGrowthBlock(ctx.getSource());
		return scheduleForEntity(ctx, block, parsed, delay);
	}

	private static int scheduleAtPosition(CommandContext<ServerCommandSource> ctx, ServerWorld world, BlockPos pos, int delay, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		return scheduleOnWorld(ctx, world, pos, delay, parsed);
	}

	private static int scheduleForEntity(CommandContext<ServerCommandSource> ctx, ProgressiveGrowthBlockEntity block, ParsedMutation parsed, int delay) throws CommandSyntaxException {
		if (!(block.getWorld() instanceof ServerWorld serverWorld)) {
			throw NO_BLOCK_TARGET.create();
		}
		return scheduleOnWorld(ctx, serverWorld, block.getPos(), delay, parsed);
	}

	private static int scheduleOnWorld(CommandContext<ServerCommandSource> ctx, ServerWorld world, BlockPos pos, int delay, ParsedMutation parsed) {
		GrowthScheduler.schedule(world, pos, parsed.mutation(), delay);
		String delayText = delay == 1 ? "1 tick" : delay + " ticks";
		ctx.getSource().sendFeedback(() -> Text.literal("Scheduled " + parsed.summary()
				+ " at " + formatPos(pos)
				+ " in " + formatDimension(world)
				+ " after " + delayText + "."), true);
		return 1;
	}

	private static GrowthBlockDefinition requireDefinition(GrowthRegistry registry, Identifier definitionId) throws CommandSyntaxException {
		if (registry == null || definitionId == null || !registry.hasDefinition(definitionId)) {
			throw UNKNOWN_DEFINITION.create(String.valueOf(definitionId));
		}
		return registry.definition(definitionId);
	}

	private static Path requireDefinitionFile(Identifier definitionId) throws CommandSyntaxException {
		try {
			Path file = findDefinitionFile(definitionId);
			if (file == null) {
				throw DEFAULTS_FILE_MISSING.create(String.valueOf(definitionId));
			}
			return file;
		} catch (IOException ex) {
			throw DEFAULTS_IO_ERROR.create(ex.getMessage());
		}
	}

	private static Path findDefinitionFile(Identifier definitionId) throws IOException {
		ConfigService config = InfectionServices.get().config();
		Path dir = config.resolve("growth_blocks");
		if (!Files.exists(dir)) {
			return null;
		}
		Path guess = dir.resolve(definitionId.getPath() + ".json");
		if (Files.exists(guess) && matchesDefinitionFile(guess, definitionId)) {
			return guess;
		}
		try (var stream = Files.walk(dir)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".json"))
					.filter(path -> matchesDefinitionFile(path, definitionId))
					.findFirst()
					.orElse(null);
		}
	}

	private static boolean matchesDefinitionFile(Path path, Identifier definitionId) {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				return false;
			}
			JsonObject json = parsed.getAsJsonObject();
			if (!json.has("id")) {
				return false;
			}
			Identifier fileId = Identifier.tryParse(json.get("id").getAsString());
			return definitionId.equals(fileId);
		} catch (IOException ex) {
			return false;
		}
	}

	private static JsonObject readDefinitionJson(Path path) throws CommandSyntaxException {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				throw DEFAULTS_IO_ERROR.create(path + " is not a JSON object");
			}
			return parsed.getAsJsonObject();
		} catch (IOException ex) {
			throw DEFAULTS_IO_ERROR.create(path + ": " + ex.getMessage());
		}
	}

	private static void writeDefinitionJson(Path path, JsonObject json) throws CommandSyntaxException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(json, writer);
		} catch (IOException ex) {
			throw DEFAULTS_IO_ERROR.create(path + ": " + ex.getMessage());
		}
	}

	private static boolean applyChangesToJson(JsonObject json, List<FieldChange> changes) {
		boolean changed = false;
		for (FieldChange change : changes) {
			String key = DEFINITION_JSON_KEYS.get(change.field());
			if (key == null) {
				continue;
			}
			if (change.cleared()) {
				if (json.has(key)) {
					json.remove(key);
					changed = true;
				}
				continue;
			}
			Object value = change.value();
			if (value instanceof Boolean bool) {
				if (!json.has(key) || json.get(key).getAsBoolean() != bool) {
					json.addProperty(key, bool);
					changed = true;
				}
			} else if (value instanceof Integer integer) {
				if (!json.has(key) || json.get(key).getAsInt() != integer) {
					json.addProperty(key, integer);
					changed = true;
				}
			} else if (value instanceof Double dbl) {
				if (!json.has(key) || json.get(key).getAsDouble() != dbl) {
					json.addProperty(key, dbl);
					changed = true;
				}
			} else if (value instanceof Identifier id) {
				String text = id.toString();
				if (!json.has(key) || !json.get(key).getAsString().equals(text)) {
					json.addProperty(key, text);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static String formatConfigPath(Path path) {
		ConfigService config = InfectionServices.get().config();
		Path root = config.root();
		try {
			return root.relativize(path).toString().replace('\\', '/');
		} catch (IllegalArgumentException ex) {
			return path.toString();
		}
	}


	private static int showDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId) throws CommandSyntaxException {
		GrowthRegistry registry = InfectionServices.get().growth();
		GrowthBlockDefinition definition = requireDefinition(registry, definitionId);
		reportDefinition(ctx.getSource(), "Defaults", definitionId, definition, GrowthOverrides.empty());
		try {
			Path file = findDefinitionFile(definitionId);
			if (file != null) {
				ctx.getSource().sendFeedback(() -> Text.literal("JSON: " + formatConfigPath(file)), false);
			}
		} catch (IOException ex) {
			ctx.getSource().sendError(Text.literal("Warning: unable to inspect JSON file (" + ex.getMessage() + ")"));
		}
		return 1;
	}

	private static int setDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId, String raw) throws CommandSyntaxException {
		ParsedMutation parsed = parseMutation(raw);
		GrowthRegistry registry = InfectionServices.get().growth();
		requireDefinition(registry, definitionId);
		Path file = requireDefinitionFile(definitionId);
		JsonObject json = readDefinitionJson(file);
		boolean changed = applyChangesToJson(json, parsed.changes());
		if (!changed) {
			ctx.getSource().sendFeedback(() -> Text.literal("No JSON changes were applied to " + formatConfigPath(file) + "."), false);
			return 0;
		}
		writeDefinitionJson(file, json);
		InfectionServices.reload();
		ctx.getSource().sendFeedback(
				() -> Text.literal("Updated " + formatConfigPath(file) + " (" + parsed.summary() + ") and reloaded growth registry."),
				true);
		return 1;
	}

	private static int giveToSource(CommandContext<ServerCommandSource> ctx, Identifier definitionId, int count) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		return giveToPlayer(ctx, player, definitionId, count);
	}

	private static int giveToPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier definitionId, int count) throws CommandSyntaxException {
		GrowthRegistry registry = InfectionServices.get().growth();
		GrowthBlockDefinition definition = requireDefinition(registry, definitionId);
		int clamped = Math.min(64, Math.max(1, count));
		ItemStack stack = new ItemStack(ModBlocks.PROGRESSIVE_GROWTH_BLOCK, clamped);
		ProgressiveGrowthBlock.applyDefinitionId(stack, definition.id());
		ItemStack gift = stack.copy();
		boolean inserted = target.getInventory().insertStack(gift);
		if (!inserted) {
			target.dropItem(gift, false);
		}
		ctx.getSource().sendFeedback(() -> Text.literal("Gave " + stack.getCount() + " growth block(s) with definition " + definition.id() + " to " + target.getGameProfile().getName()), true);
		return stack.getCount();
	}

	private static int applyMutationToBlock(CommandContext<ServerCommandSource> ctx, ProgressiveGrowthBlockEntity block, ParsedMutation parsed) {
		boolean changed = block.applyMutation(parsed.mutation());
		if (!changed) {
			ctx.getSource().sendFeedback(() -> Text.literal("No override changes were applied; the block already matches the requested values."), false);
			return 0;
		}
		BlockPos pos = block.getPos();
		ctx.getSource().sendFeedback(() -> Text.literal("Applied " + parsed.summary() + " to growth block at " + formatPos(pos) + "."), true);
		return 1;
	}

	private static int applyMutationToStack(CommandContext<ServerCommandSource> ctx, StackTarget target, ParsedMutation parsed) {
		GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(target.stack());
		boolean changed = overrides.applyMutation(parsed.mutation());
		if (!changed) {
			ctx.getSource().sendFeedback(() -> Text.literal("No override changes were applied to the held stack."), false);
			return 0;
		}
		ProgressiveGrowthBlock.applyOverrides(target.stack(), overrides);
		ctx.getSource().sendFeedback(() -> Text.literal("Applied " + parsed.summary() + " to the " + target.hand().name().toLowerCase(Locale.ROOT) + " stack."), true);
		return 1;
	}

	private static ParsedMutation parseMutation(String raw) throws CommandSyntaxException {
		if (raw == null) {
			throw EMPTY_MUTATION.create();
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			throw EMPTY_MUTATION.create();
		}
		GrowthMutation mutation = new GrowthMutation();
		StringJoiner summary = new StringJoiner(", ");
		List<FieldChange> changes = new ArrayList<>();
		for (String token : trimmed.split("\\s+")) {
			if (token.isEmpty()) {
				continue;
			}
			int separator = token.indexOf('=');
			if (separator < 0) {
				throw MALFORMED_PAIR.create(token);
			}
			String key = token.substring(0, separator);
			String value = token.substring(separator + 1);
			GrowthField field = resolveField(key);
			FieldChange change = applyFieldValue(mutation, field, value);
			changes.add(change);
			boolean cleared = change.cleared();
			if (cleared) {
				summary.add(field.name().toLowerCase(Locale.ROOT) + "=<cleared>");
			} else {
				summary.add(field.name().toLowerCase(Locale.ROOT) + "=" + value);
			}
		}
		if (mutation.isEmpty()) {
			throw EMPTY_MUTATION.create();
		}
		return new ParsedMutation(mutation, summary.toString(), List.copyOf(changes));
	}

	private static GrowthField resolveField(String raw) throws CommandSyntaxException {
		if (raw == null || raw.isEmpty()) {
			throw UNKNOWN_FIELD.create(raw);
		}
		String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
		GrowthField field = FIELD_LOOKUP.get(normalized);
		if (field == null) {
			throw UNKNOWN_FIELD.create(raw);
		}
		return field;
	}

	private static FieldChange applyFieldValue(GrowthMutation mutation, GrowthField field, String rawValue) throws CommandSyntaxException {
		String value = rawValue == null ? "" : rawValue.trim();
		if (value.isEmpty() || isClearToken(value)) {
			mutation.clear(field);
			return new FieldChange(field, null, true);
		}
		try {
			return switch (field.type()) {
				case BOOLEAN -> {
					boolean parsed = parseBoolean(value);
					mutation.setBoolean(field, parsed);
					yield new FieldChange(field, parsed, false);
				}
				case INT -> {
					int parsed = Integer.parseInt(value);
					mutation.setInt(field, parsed);
					yield new FieldChange(field, parsed, false);
				}
				case DOUBLE -> {
					double parsed = Double.parseDouble(value);
					mutation.setDouble(field, parsed);
					yield new FieldChange(field, parsed, false);
				}
				case IDENTIFIER -> {
					Identifier id = Identifier.tryParse(value);
					if (id == null) {
						throw INVALID_VALUE.create(value + " (expected identifier)");
					}
					mutation.setIdentifier(field, id);
					yield new FieldChange(field, id, false);
				}
			};
		} catch (NumberFormatException ex) {
			throw INVALID_VALUE.create(value + " (for " + field.name().toLowerCase(Locale.ROOT) + ")");
		}
	}

	private static boolean parseBoolean(String value) throws CommandSyntaxException {
		String normalized = value.toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "true", "1", "yes", "y", "on" -> true;
			case "false", "0", "no", "n", "off" -> false;
			default -> throw INVALID_VALUE.create(value + " (expected boolean)");
		};
	}

	private static boolean isClearToken(String value) {
		String normalized = value.toLowerCase(Locale.ROOT);
		return normalized.isEmpty()
				|| normalized.equals("clear")
				|| normalized.equals("none")
				|| normalized.equals("null")
				|| normalized.equals("-");
	}

	private static ProgressiveGrowthBlockEntity requireGrowthBlock(ServerCommandSource source) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();
		ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
		if (block == null) {
			throw NO_BLOCK_TARGET.create();
		}
		return block;
	}

	private static ProgressiveGrowthBlockEntity traceGrowthBlock(ServerPlayerEntity player) {
		HitResult hit = player.raycast(DEFAULT_TRACE_DISTANCE, 1.0F, false);
		if (hit instanceof BlockHitResult blockHit) {
			if (player.getWorld().getBlockEntity(blockHit.getBlockPos()) instanceof ProgressiveGrowthBlockEntity growth) {
				return growth;
			}
		}
		return null;
	}

	private static StackTarget findHeldGrowthBlock(ServerPlayerEntity player) {
		ItemStack main = player.getMainHandStack();
		if (isGrowthStack(main)) {
			return new StackTarget(main, Hand.MAIN_HAND);
		}
		ItemStack off = player.getOffHandStack();
		if (isGrowthStack(off)) {
			return new StackTarget(off, Hand.OFF_HAND);
		}
		return null;
	}

	private static boolean isGrowthStack(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.isOf(ModBlocks.PROGRESSIVE_GROWTH_BLOCK.asItem());
	}

	private static void reportDefinition(ServerCommandSource source, String label, Identifier definitionId, GrowthBlockDefinition definition, GrowthOverrides overrides) {
		source.sendFeedback(() -> Text.literal(label + " definition: " + definitionId), false);
		source.sendFeedback(() -> Text.literal("Growth: enabled=" + definition.growthEnabled()
				+ " rateTicks=" + definition.rateTicks()
				+ " rateScale=" + formatDouble(definition.rateScale())), false);
		source.sendFeedback(() -> Text.literal("Scale: start=" + formatDouble(definition.startScale())
				+ " target=" + formatDouble(definition.targetScale())
				+ " min=" + formatDouble(definition.minScale())
				+ " max=" + formatDouble(definition.maxScale())), false);
		source.sendFeedback(() -> Text.literal("Flags: collision=" + definition.hasCollision()
				+ " destruction=" + definition.doesDestruction()
				+ " fuse=" + definition.hasFuse()
				+ " wobble=" + definition.isWobbly()), false);
		source.sendFeedback(() -> Text.literal("Forces: pulling=" + definition.isPulling() + " (" + formatDouble(definition.pullingForce())
				+ ") pushing=" + definition.isPushing() + " (" + formatDouble(definition.pushingForce())
				+ ") touchDamage=" + formatDouble(definition.touchDamage())), false);
		source.sendFeedback(() -> Text.literal("Profiles: glow=" + definition.glowProfileId()
				+ " particle=" + definition.particleProfileId()
				+ " field=" + definition.fieldProfileId()), false);
		source.sendFeedback(() -> Text.literal("Force profiles: pull=" + definition.pullProfileId()
				+ " push=" + definition.pushProfileId()), false);
		source.sendFeedback(() -> Text.literal("Fuse & explosion: fuse=" + definition.fuseProfileId()
				+ " explosion=" + definition.explosionProfileId()), false);
		source.sendFeedback(() -> Text.literal("Overrides: " + formatOverrides(overrides)), false);
	}

	private static String formatOverrides(GrowthOverrides overrides) {
		if (overrides == null || overrides.isEmpty()) {
			return "none";
		}
		return overrides.snapshot().entrySet().stream()
				.map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT) + "=" + entry.getValue())
				.collect(Collectors.joining(", "));
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static String formatDimension(ServerWorld world) {
		return world.getRegistryKey().getValue().toString();
	}

	private static String formatDouble(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return "NaN";
		}
		if (Math.abs(value - Math.rint(value)) < 1.0E-4) {
			return Long.toString(Math.round(value));
		}
		return String.format(Locale.ROOT, "%.3f", value);
	}

	private record StackTarget(ItemStack stack, Hand hand) {
	}

	private record ParsedMutation(GrowthMutation mutation, String summary, List<FieldChange> changes) {
	}

	private record FieldChange(GrowthField field, Object value, boolean cleared) {
	}

	private static Map<GrowthField, String> buildDefinitionJsonKeyMap() {
		EnumMap<GrowthField, String> map = new EnumMap<>(GrowthField.class);
		map.put(GrowthField.GROWTH_ENABLED, "growth_enabled");
		map.put(GrowthField.RATE_TICKS, "rate");
		map.put(GrowthField.RATE_SCALE, "scale_by_rate");
		map.put(GrowthField.START_SCALE, "start");
		map.put(GrowthField.TARGET_SCALE, "target");
		map.put(GrowthField.MIN_SCALE, "min");
		map.put(GrowthField.MAX_SCALE, "max");
		map.put(GrowthField.HAS_COLLISION, "has_collision");
		map.put(GrowthField.DOES_DESTRUCTION, "does_destruction");
		map.put(GrowthField.HAS_FUSE, "has_fuse");
		map.put(GrowthField.IS_WOBBLY, "is_wobbly");
		map.put(GrowthField.IS_PULLING, "is_pulling");
		map.put(GrowthField.IS_PUSHING, "is_pushing");
		map.put(GrowthField.PULLING_FORCE, "pulling_force");
		map.put(GrowthField.PUSHING_FORCE, "pushing_force");
		map.put(GrowthField.TOUCH_DAMAGE, "touch_damage");
		map.put(GrowthField.GLOW_PROFILE, "glow_profile");
		map.put(GrowthField.PARTICLE_PROFILE, "particle_profile");
		map.put(GrowthField.FIELD_PROFILE, "field_profile");
		map.put(GrowthField.PULL_PROFILE, "pull_profile");
		map.put(GrowthField.PUSH_PROFILE, "push_profile");
		map.put(GrowthField.FUSE_PROFILE, "fuse_profile");
		map.put(GrowthField.EXPLOSION_PROFILE, "explosion_profile");
		return Map.copyOf(map);
	}
}

