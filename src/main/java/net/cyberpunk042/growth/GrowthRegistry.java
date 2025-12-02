package net.cyberpunk042.growth;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.service.ConfigService;
import net.minecraft.util.Identifier;

/**
 * Loads/snapshots all progressive growth profiles (glow, force, fuse, block
 * definitions) from {@code config/the-virus-block}.
 */
public final class GrowthRegistry {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter())
			.create();

	/** Serializes Identifier as a string "namespace:path" instead of an object */
	private static class IdentifierTypeAdapter extends com.google.gson.TypeAdapter<Identifier> {
		@Override
		public void write(com.google.gson.stream.JsonWriter out, Identifier value) throws java.io.IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value.toString());
			}
		}

		@Override
		public Identifier read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
			if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			return Identifier.tryParse(in.nextString());
		}
	}

private final Map<Identifier, GlowProfile> glowProfiles;
private final Map<Identifier, ParticleProfile> particleProfiles;
private final Map<Identifier, ForceProfile> forceProfiles;
private final Map<Identifier, FieldProfile> fieldProfiles;
private final Map<Identifier, FuseProfile> fuseProfiles;
private final Map<Identifier, ExplosionProfile> explosionProfiles;
private final Map<Identifier, GrowthBlockDefinition> definitions;
private final GrowthBlockDefinition defaultDefinition;

private GrowthRegistry(Map<Identifier, GlowProfile> glowProfiles,
		Map<Identifier, ParticleProfile> particleProfiles,
		Map<Identifier, ForceProfile> forceProfiles,
		Map<Identifier, FieldProfile> fieldProfiles,
		Map<Identifier, FuseProfile> fuseProfiles,
		Map<Identifier, ExplosionProfile> explosionProfiles,
		Map<Identifier, GrowthBlockDefinition> definitions,
		GrowthBlockDefinition defaultDefinition) {
	this.glowProfiles = glowProfiles;
	this.particleProfiles = particleProfiles;
	this.forceProfiles = forceProfiles;
	this.fieldProfiles = fieldProfiles;
	this.fuseProfiles = fuseProfiles;
	this.explosionProfiles = explosionProfiles;
 	this.definitions = definitions;
	this.defaultDefinition = defaultDefinition;
}

	public static GrowthRegistry load(ConfigService config) {
	Path glowDir = config.resolve("glow_profiles");
	Path particleDir = config.resolve("particle_profiles");
	Path forceDir = config.resolve("force_profiles");
	Path fieldDir = config.resolve("field_profiles");
	Path fuseDir = config.resolve("fuse_profiles");
	Path explosionDir = config.resolve("explosion_profiles");
	Path definitionDir = config.resolve("growth_blocks");

	ensureDefaults(glowDir, particleDir, forceDir, fieldDir, fuseDir, explosionDir, definitionDir);

	Map<Identifier, GlowProfile> glow = loadGlowProfiles(glowDir);
	Map<Identifier, ParticleProfile> particles = loadParticleProfiles(particleDir);
	Map<Identifier, ForceProfile> forces = loadForceProfiles(forceDir);
	Map<Identifier, FieldProfile> fields = loadFieldProfiles(fieldDir);
	Map<Identifier, FuseProfile> fuse = loadFuseProfiles(fuseDir);
	Map<Identifier, ExplosionProfile> explosions = loadExplosionProfiles(explosionDir);
	Map<Identifier, GrowthBlockDefinition> definitions = loadDefinitions(definitionDir);

		if (glow.isEmpty()) {
			GlowProfile defaults = GlowProfile.defaults();
			glow = Map.of(defaults.id(), defaults);
		}
		if (particles.isEmpty()) {
			ParticleProfile defaults = ParticleProfile.defaults();
			particles = Map.of(defaults.id(), defaults);
		}
	if (forces.isEmpty()) {
		ForceProfile defaults = ForceProfile.defaultsPull();
		forces = Map.of(defaults.id(), defaults);
	}
	if (fields.isEmpty()) {
		FieldProfile defaults = FieldProfile.defaults();
		fields = Map.of(defaults.id(), defaults);
		}
		if (fuse.isEmpty()) {
			FuseProfile defaults = FuseProfile.defaults();
			fuse = Map.of(defaults.id(), defaults);
		}
		if (explosions.isEmpty()) {
			ExplosionProfile defaults = ExplosionProfile.defaults();
			explosions = Map.of(defaults.id(), defaults);
		}
		if (definitions.isEmpty()) {
			GrowthBlockDefinition defaults = GrowthBlockDefinition.defaults();
			definitions = Map.of(defaults.id(), defaults);
		}

		GrowthBlockDefinition defaultDefinition = definitions.values()
				.stream()
				.findFirst()
				.orElse(GrowthBlockDefinition.defaults());
	return new GrowthRegistry(glow, particles, forces, fields, fuse, explosions, definitions, defaultDefinition);
	}

	public GlowProfile glowProfile(Identifier id) {
		return glowProfiles.getOrDefault(id, GlowProfile.defaults());
	}

	public ParticleProfile particleProfile(Identifier id) {
		if (id == null) {
			return ParticleProfile.defaults();
		}
		return particleProfiles.getOrDefault(id, ParticleProfile.defaults());
	}

public ForceProfile forceProfile(Identifier id) {
	return forceProfile(id, ForceProfile.defaultsPull());
}

public ForceProfile forceProfile(Identifier id, ForceProfile fallback) {
	ForceProfile resolvedFallback = fallback != null ? fallback : ForceProfile.defaultsPull();
	if (id == null) {
		return resolvedFallback;
	}
	return forceProfiles.getOrDefault(id, resolvedFallback);
}

public FieldProfile fieldProfile(Identifier id) {
	if (id == null) {
		return FieldProfile.defaults();
	}
	return fieldProfiles.getOrDefault(id, FieldProfile.defaults());
}

	public FuseProfile fuseProfile(Identifier id) {
		if (id == null) {
			return FuseProfile.defaults();
		}
		return fuseProfiles.getOrDefault(id, FuseProfile.defaults());
	}

	public ExplosionProfile explosionProfile(Identifier id) {
		if (id == null) {
			return ExplosionProfile.defaults();
		}
		return explosionProfiles.getOrDefault(id, ExplosionProfile.defaults());
	}

	public GrowthBlockDefinition definition(Identifier id) {
		if (id == null) {
			return defaultDefinition;
		}
		return definitions.getOrDefault(id, defaultDefinition);
	}

	public GrowthBlockDefinition defaultDefinition() {
		return defaultDefinition;
	}

	public List<Identifier> definitionIds() {
		return definitions.keySet().stream().sorted().toList();
	}

	public boolean hasDefinition(Identifier id) {
		return id != null && definitions.containsKey(id);
	}

private static void ensureDefaults(Path glowDir, Path particleDir, Path forceDir, Path fieldDir, Path fuseDir, Path explosionDir, Path definitionDir) {
		try {
			Files.createDirectories(glowDir);
			Files.createDirectories(particleDir);
		Files.createDirectories(forceDir);
			Files.createDirectories(fieldDir);
			Files.createDirectories(fuseDir);
			Files.createDirectories(explosionDir);
			Files.createDirectories(definitionDir);
		} catch (IOException ex) {
			TheVirusBlock.LOGGER.error("[GrowthRegistry] Failed creating directories", ex);
		}
		writeIfMissing(glowDir.resolve("prototype.json"), GlowProfile.defaults());
		writeIfMissing(glowDir.resolve("magma.json"), new GlowProfile(
				Identifier.of("the-virus-block", "magma"),
				Identifier.of("the-virus-block", "textures/misc/glow_magma_primary.png"),
				Identifier.of("the-virus-block", "textures/misc/glow_magma_secondary.png"),
				0.65F,
				1.0F,
				1.0F));
		writeIfMissing(glowDir.resolve("lava.json"), new GlowProfile(
				Identifier.of("the-virus-block", "lava"),
				Identifier.of("the-virus-block", "textures/misc/glow_lava_primary.png"),
				Identifier.of("the-virus-block", "textures/misc/glow_lava_secondary.png"),
				0.8F,
				0.95F,
				1.3F));
		writeIfMissing(glowDir.resolve("beam.json"), new GlowProfile(
				Identifier.of("the-virus-block", "beam"),
				Identifier.of("the-virus-block", "textures/misc/glow_beam_primary.png"),
				Identifier.of("the-virus-block", "textures/misc/glow_beam_secondary.png"),
				0.7F,
				0.9F,
				2.0F));
		writeIfMissing(glowDir.resolve("glowstone.json"), new GlowProfile(
				Identifier.of("the-virus-block", "glowstone"),
				Identifier.of("the-virus-block", "textures/misc/glow_glowstone_primary.png"),
				Identifier.of("the-virus-block", "textures/misc/glow_glowstone_secondary.png"),
				0.5F,
				0.75F,
				0.6F));
		writeIfMissing(particleDir.resolve("default.json"), ParticleProfile.defaults());
		writeIfMissing(forceDir.resolve("default_pull.json"), ForceProfile.defaultsPull());
		writeIfMissing(forceDir.resolve("default_push.json"), ForceProfile.defaultsPush());
		writeIfMissing(forceDir.resolve("vortex_pull.json"), ForceProfile.vortexPull());
		writeIfMissing(forceDir.resolve("shock_push.json"), ForceProfile.shockPush());
		writeIfMissing(forceDir.resolve("ring_hold.json"), new ForceProfile(
				Identifier.of("the-virus-block", "ring_hold"),
				12,
				9.5D,
				0.0D,
				0.0D,
				1.0D,
				0.0D,
				1.0D,
				false,
				0.0D,
				0xFFFFFFFF,
				Identifier.of("minecraft", "enchant"),
				0,
				0.0F,
				Identifier.of("minecraft", "entity.enderman.teleport"),
				0.0D,
				10,
				ForceProfile.RingBehavior.KEEP_ON_RING,
				1,
				0.0D,
				1.1D,
				1.2D,
				List.of(Identifier.of("the-virus-block", "ring_default"))));
		writeIfMissing(fieldDir.resolve("default.json"), FieldProfile.defaults());
		writeIfMissing(fieldDir.resolve("ring_default.json"), new FieldProfile(
				Identifier.of("the-virus-block", "ring_default"),
				"ring",
				Identifier.of("the-virus-block", "textures/misc/singularity_sphere_1.png"),
				0.65F,
				1.2F,
				1.0F,
				"#FFFFFFFF"));
		writeIfMissing(fuseDir.resolve("default.json"), FuseProfile.defaults());
		writeIfMissing(explosionDir.resolve("default.json"), ExplosionProfile.defaults());
		writeIfMissing(definitionDir.resolve("prototype.json"), GrowthBlockDefinition.defaults());
	}

	private static void writeIfMissing(Path path, Object data) {
		if (Files.exists(path)) {
			return;
		}
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(data, writer);
		} catch (IOException ex) {
			TheVirusBlock.LOGGER.error("[GrowthRegistry] Failed to write {}", path.getFileName(), ex);
		}
	}

	private static Map<Identifier, GlowProfile> loadGlowProfiles(Path dir) {
		Map<Identifier, GlowProfile> profiles = new HashMap<>();
		loadObjects(dir, (path, json) -> {
			GlowProfile profile = parseGlowProfile(json);
			if (profile != null) {
				profiles.put(profile.id(), profile);
			} else {
				TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping glow profile {}", path.getFileName());
			}
		});
		return Collections.unmodifiableMap(profiles);
	}

	private static Map<Identifier, ParticleProfile> loadParticleProfiles(Path dir) {
		Map<Identifier, ParticleProfile> profiles = new HashMap<>();
		loadObjects(dir, (path, json) -> {
			ParticleProfile profile = parseParticleProfile(json);
			if (profile != null) {
				profiles.put(profile.id(), profile);
			} else {
				TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping particle profile {}", path.getFileName());
			}
		});
		return Collections.unmodifiableMap(profiles);
	}

private static Map<Identifier, ForceProfile> loadForceProfiles(Path dir) {
	Map<Identifier, ForceProfile> profiles = new HashMap<>();
	loadObjects(dir, (path, json) -> {
		ForceProfile profile = parseForceProfile(json);
		if (profile != null) {
			profiles.put(profile.id(), profile);
		} else {
			TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping force profile {}", path.getFileName());
		}
	});
	return Collections.unmodifiableMap(profiles);
}

private static Map<Identifier, FieldProfile> loadFieldProfiles(Path dir) {
	Map<Identifier, FieldProfile> profiles = new HashMap<>();
	loadObjects(dir, (path, json) -> {
		FieldProfile profile = parseFieldProfile(json);
		if (profile != null) {
			profiles.put(profile.id(), profile);
		} else {
			TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping field profile {}", path.getFileName());
		}
	});
	return Collections.unmodifiableMap(profiles);
}

	private static Map<Identifier, FuseProfile> loadFuseProfiles(Path dir) {
		Map<Identifier, FuseProfile> profiles = new HashMap<>();
		loadObjects(dir, (path, json) -> {
			FuseProfile profile = parseFuseProfile(json);
			if (profile != null) {
				profiles.put(profile.id(), profile);
			} else {
				TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping fuse profile {}", path.getFileName());
			}
		});
		return Collections.unmodifiableMap(profiles);
	}

private static Map<Identifier, ExplosionProfile> loadExplosionProfiles(Path dir) {
	Map<Identifier, ExplosionProfile> profiles = new HashMap<>();
	loadObjects(dir, (path, json) -> {
		ExplosionProfile profile = parseExplosionProfile(json);
		if (profile != null) {
			profiles.put(profile.id(), profile);
		} else {
			TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping explosion profile {}", path.getFileName());
		}
	});
	return Collections.unmodifiableMap(profiles);
}

	private static Map<Identifier, GrowthBlockDefinition> loadDefinitions(Path dir) {
		Map<Identifier, GrowthBlockDefinition> profiles = new HashMap<>();
		loadObjects(dir, (path, json) -> {
			GrowthBlockDefinition definition = parseDefinition(json);
			if (definition != null) {
				profiles.put(definition.id(), definition);
			} else {
				TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping growth block {}", path.getFileName());
			}
		});
		return Collections.unmodifiableMap(profiles);
	}

	private static void loadObjects(Path dir, JsonFileConsumer consumer) {
		try (var stream = Files.list(dir)) {
			stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
				try (var reader = Files.newBufferedReader(path)) {
					JsonElement parsed = JsonParser.parseReader(reader);
					if (!parsed.isJsonObject()) {
						TheVirusBlock.LOGGER.warn("[GrowthRegistry] Skipping {} (not an object)", path.getFileName());
						return;
					}
					consumer.accept(path, parsed.getAsJsonObject());
				} catch (Exception ex) {
					TheVirusBlock.LOGGER.error("[GrowthRegistry] Failed parsing {}", path.getFileName(), ex);
				}
			});
		} catch (IOException ex) {
			TheVirusBlock.LOGGER.error("[GrowthRegistry] Failed listing {}", dir, ex);
		}
	}

	private static GlowProfile parseGlowProfile(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		Identifier primary = parseId(json, "primary_texture", GlowProfile.defaults().primaryTexture());
		Identifier secondary = parseId(json, "secondary_texture", GlowProfile.defaults().secondaryTexture());
		float primaryAlpha = json.has("primary_alpha") ? json.get("primary_alpha").getAsFloat() : GlowProfile.defaults().primaryAlpha();
		float secondaryAlpha = json.has("secondary_alpha") ? json.get("secondary_alpha").getAsFloat() : GlowProfile.defaults().secondaryAlpha();
		float spinSpeed = json.has("spin_speed") ? json.get("spin_speed").getAsFloat() : GlowProfile.defaults().spinSpeed();
		return new GlowProfile(id, primary, secondary, primaryAlpha, secondaryAlpha, spinSpeed);
	}

	private static ParticleProfile parseParticleProfile(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		ParticleProfile defaults = ParticleProfile.defaults();
		Identifier particle = parseId(json, "particle", defaults.particleId());
		int count = json.has("count") ? json.get("count").getAsInt() : defaults.count();
		double speed = json.has("speed") ? json.get("speed").getAsDouble() : defaults.speed();
		Identifier sound = parseId(json, "sound", defaults.soundId());
		int soundInterval = json.has("sound_interval") ? json.get("sound_interval").getAsInt() : defaults.soundIntervalTicks();
		int intervalTicks = json.has("interval_ticks") ? json.get("interval_ticks").getAsInt() : defaults.intervalTicks();
		ParticleProfile.Shape shape = defaults.shape();
		if (json.has("shape")) {
			String shapeRaw = json.get("shape").getAsString();
			try {
				shape = ParticleProfile.Shape.valueOf(shapeRaw.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				shape = defaults.shape();
			}
		}
		double radius = json.has("radius") ? json.get("radius").getAsDouble() : defaults.radius();
		double height = json.has("height") ? json.get("height").getAsDouble() : defaults.height();
		double offsetX = json.has("offset_x") ? json.get("offset_x").getAsDouble() : defaults.offsetX();
		double offsetY = json.has("offset_y") ? json.get("offset_y").getAsDouble() : defaults.offsetY();
		double offsetZ = json.has("offset_z") ? json.get("offset_z").getAsDouble() : defaults.offsetZ();
		double jitterX = json.has("jitter_x") ? json.get("jitter_x").getAsDouble() : defaults.jitterX();
		double jitterY = json.has("jitter_y") ? json.get("jitter_y").getAsDouble() : defaults.jitterY();
		double jitterZ = json.has("jitter_z") ? json.get("jitter_z").getAsDouble() : defaults.jitterZ();
		boolean followScale = json.has("follow_scale") ? json.get("follow_scale").getAsBoolean() : defaults.followScale();
		return new ParticleProfile(
				id,
				particle,
				count,
				speed,
				sound,
				soundInterval,
				intervalTicks,
				shape,
				radius,
				height,
				offsetX,
				offsetY,
				offsetZ,
				jitterX,
				jitterY,
				jitterZ,
				followScale);
	}

private static ForceProfile parseForceProfile(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		int interval = json.has("interval_ticks") ? json.get("interval_ticks").getAsInt() : 20;
		double radius = json.has("radius") ? json.get("radius").getAsDouble() : 10.0D;
		double strength = json.has("strength") ? json.get("strength").getAsDouble() : 0.8D;
		double verticalBoost = json.has("vertical_boost") ? json.get("vertical_boost").getAsDouble() : 0.25D;
		double falloff = json.has("falloff") ? json.get("falloff").getAsDouble() : 0.85D;
		double startProgress = json.has("start_progress") ? json.get("start_progress").getAsDouble() : 0.0D;
		double endProgress = json.has("end_progress") ? json.get("end_progress").getAsDouble() : 1.0D;
		boolean guardianBeams = json.has("guardian_beams") && json.get("guardian_beams").getAsBoolean();
		double edgeFalloff = json.has("edge_falloff") ? json.get("edge_falloff").getAsDouble() : 0.0D;
		int beamColor = json.has("beam_color") ? Integer.decode(json.get("beam_color").getAsString()) : 0xFFFFFF;
		Identifier particle = parseId(json, "particle", Identifier.of("minecraft", "poof"));
		int particleCount = json.has("particle_count") ? json.get("particle_count").getAsInt() : 6;
		float particleSpeed = json.has("particle_speed") ? json.get("particle_speed").getAsFloat() : 0.02F;
		Identifier sound = parseId(json, "sound", Identifier.of("minecraft", "block.beacon.activate"));
		double impactDamage = json.has("impact_damage") ? json.get("impact_damage").getAsDouble() : 0.0D;
		int impactCooldown = json.has("impact_cooldown_ticks") ? json.get("impact_cooldown_ticks").getAsInt() : 10;
		ForceProfile.RingBehavior ringBehavior = json.has("ring_behavior")
				? ForceProfile.RingBehavior.fromString(json.get("ring_behavior").getAsString())
				: ForceProfile.RingBehavior.NONE;
		int ringCount = json.has("ring_count") ? json.get("ring_count").getAsInt() : 0;
		double ringSpacing = json.has("ring_spacing") ? json.get("ring_spacing").getAsDouble() : 0.0D;
		double ringWidth = json.has("ring_width") ? json.get("ring_width").getAsDouble() : 0.0D;
		double ringStrength = json.has("ring_strength") ? json.get("ring_strength").getAsDouble() : 1.0D;
		List<Identifier> ringFields = json.has("ring_field_profiles")
				? parseIdArray(json, "ring_field_profiles")
				: List.of();
	return new ForceProfile(id,
				interval,
				radius,
				strength,
				verticalBoost,
				falloff,
				startProgress,
				endProgress,
				guardianBeams,
				edgeFalloff,
				beamColor,
				particle,
				particleCount,
				particleSpeed,
				sound,
				impactDamage,
				impactCooldown,
				ringBehavior,
				ringCount,
				ringSpacing,
				ringWidth,
				ringStrength,
				ringFields);
	}

private static FieldProfile parseFieldProfile(JsonObject json) {
	Identifier id = parseId(json, "id");
	if (id == null) {
		return null;
	}
	FieldProfile defaults = FieldProfile.defaults();
	String mesh = json.has("mesh") ? json.get("mesh").getAsString() : defaults.meshType();
	Identifier texture = parseId(json, "texture", defaults.texture());
	float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : defaults.alpha();
	float spinSpeed = json.has("spin_speed") ? json.get("spin_speed").getAsFloat() : defaults.spinSpeed();
	float scaleMultiplier = json.has("scale_multiplier") ? json.get("scale_multiplier").getAsFloat() : defaults.scaleMultiplier();
	String color = json.has("color") ? json.get("color").getAsString() : defaults.colorHex();
	return new FieldProfile(id, mesh, texture, alpha, spinSpeed, scaleMultiplier, color);
}

	private static FuseProfile parseFuseProfile(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		FuseProfile defaults = FuseProfile.defaults();
		String triggerRaw = json.has("trigger") ? json.get("trigger").getAsString() : defaults.trigger().name();
		FuseProfile.Trigger trigger = FuseProfile.Trigger.fromString(triggerRaw);
		double autoProgress = json.has("auto_progress") ? json.get("auto_progress").getAsDouble() : defaults.autoProgress();
		boolean requiresItem = json.has("requires_item") ? json.get("requires_item").getAsBoolean() : defaults.requiresItem();
		boolean consumeItem = json.has("consume_item") ? json.get("consume_item").getAsBoolean() : defaults.consumeItem();
		List<Identifier> allowedItems = json.has("allowed_items") ? parseIdArray(json, "allowed_items") : defaults.allowedItems();
		int explosionDelay = json.has("explosion_delay_ticks") ? json.get("explosion_delay_ticks").getAsInt() : defaults.explosionDelayTicks();
		int shellCollapse = json.has("shell_collapse_ticks") ? json.get("shell_collapse_ticks").getAsInt() : defaults.shellCollapseTicks();
		int pulseInterval = json.has("pulse_interval_ticks") ? json.get("pulse_interval_ticks").getAsInt() : defaults.pulseIntervalTicks();
		double collapseTarget = json.has("collapse_target_scale") ? json.get("collapse_target_scale").getAsDouble() : defaults.collapseTargetScale();
		String primaryColor = json.has("primary_color") ? json.get("primary_color").getAsString() : defaults.primaryColorHex();
		String secondaryColor = json.has("secondary_color") ? json.get("secondary_color").getAsString() : defaults.secondaryColorHex();
		Identifier particle = parseId(json, "particle", defaults.particleId());
		Identifier sound = parseId(json, "sound", defaults.soundId());
		return new FuseProfile(
				id,
				trigger,
				autoProgress,
				requiresItem,
				consumeItem,
				allowedItems,
				explosionDelay,
				shellCollapse,
				pulseInterval,
				collapseTarget,
				primaryColor,
				secondaryColor,
				particle,
				sound);
	}

	private static ExplosionProfile parseExplosionProfile(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		ExplosionProfile defaults = ExplosionProfile.defaults();
		double radius = json.has("radius") ? json.get("radius").getAsDouble() : defaults.radius();
		int charges = json.has("charges") ? json.get("charges").getAsInt() : defaults.charges();
		int amount = json.has("amount") ? json.get("amount").getAsInt() : defaults.amount();
		int amountDelay = json.has("amount_delay_ticks") ? json.get("amount_delay_ticks").getAsInt() : defaults.amountDelayTicks();
		double maxDamage = json.has("max_damage") ? json.get("max_damage").getAsDouble() : defaults.maxDamage();
		double damageScaling = json.has("damage_scaling") ? json.get("damage_scaling").getAsDouble() : defaults.damageScaling();
		boolean causesFire = json.has("causes_fire") ? json.get("causes_fire").getAsBoolean() : defaults.causesFire();
		boolean breaksBlocks = json.has("breaks_blocks") ? json.get("breaks_blocks").getAsBoolean() : defaults.breaksBlocks();
		return new ExplosionProfile(id, radius, charges, amount, amountDelay, maxDamage, damageScaling, causesFire, breaksBlocks);
	}

	private static GrowthBlockDefinition parseDefinition(JsonObject json) {
		Identifier id = parseId(json, "id");
		if (id == null) {
			return null;
		}
		GrowthBlockDefinition defaults = GrowthBlockDefinition.defaults();
		boolean growthEnabled = json.has("growth_enabled") ? json.get("growth_enabled").getAsBoolean() : defaults.growthEnabled();
		int rate = json.has("rate") ? json.get("rate").getAsInt() : defaults.rateTicks();
		double rateScale = json.has("scale_by_rate") ? json.get("scale_by_rate").getAsDouble() : defaults.rateScale();
		double start = json.has("start") ? json.get("start").getAsDouble() : defaults.startScale();
		double target = json.has("target") ? json.get("target").getAsDouble() : defaults.targetScale();
		double min = json.has("min") ? json.get("min").getAsDouble() : defaults.minScale();
		double max = json.has("max") ? json.get("max").getAsDouble() : defaults.maxScale();
		boolean hasCollision = json.has("has_collision") ? json.get("has_collision").getAsBoolean() : defaults.hasCollision();
		boolean doesDestruction = json.has("does_destruction") ? json.get("does_destruction").getAsBoolean() : defaults.doesDestruction();
		boolean hasFuse = json.has("has_fuse") ? json.get("has_fuse").getAsBoolean() : defaults.hasFuse();
		boolean isWobbly = json.has("is_wobbly") ? json.get("is_wobbly").getAsBoolean() : defaults.isWobbly();
		boolean isPulling = json.has("is_pulling") ? json.get("is_pulling").getAsBoolean() : defaults.isPulling();
		boolean isPushing = json.has("is_pushing") ? json.get("is_pushing").getAsBoolean() : defaults.isPushing();
		double pullingForce = json.has("pulling_force") ? json.get("pulling_force").getAsDouble() : defaults.pullingForce();
		double pushingForce = json.has("pushing_force") ? json.get("pushing_force").getAsDouble() : defaults.pushingForce();
		double touchDamage = json.has("touch_damage") ? json.get("touch_damage").getAsDouble() : defaults.touchDamage();
		Identifier glowProfile = parseId(json, "glow_profile", defaults.glowProfileId());
		Identifier particleProfile = parseId(json, "particle_profile", defaults.particleProfileId());
		Identifier fieldProfile = parseId(json, "field_profile", defaults.fieldProfileId());
		Identifier pullProfile = parseId(json, "pull_profile", defaults.pullProfileId());
		Identifier pushProfile = parseId(json, "push_profile", defaults.pushProfileId());
		Identifier fuseProfile = parseId(json, "fuse_profile", defaults.fuseProfileId());
		Identifier explosionProfile = parseId(json, "explosion_profile", defaults.explosionProfileId());
		return new GrowthBlockDefinition(id,
				growthEnabled,
				rate,
				rateScale,
				start,
				target,
				min,
				max,
				hasCollision,
				doesDestruction,
				hasFuse,
				isWobbly,
				isPulling,
				isPushing,
				pullingForce,
				pushingForce,
				touchDamage,
				glowProfile,
				particleProfile,
				fieldProfile,
				pullProfile,
				pushProfile,
				fuseProfile,
				explosionProfile);
	}

	private static Identifier parseId(JsonObject json, String key) {
		if (json == null || key == null || !json.has(key)) {
			return null;
		}
		return parseId(json, key, null);
	}

	private static Identifier parseId(JsonObject json, String key, Identifier fallback) {
		if (json == null || key == null || !json.has(key)) {
			return fallback;
		}
		com.google.gson.JsonElement element = json.get(key);
		// Handle both string format "namespace:path" and object format {"namespace":"...", "path":"..."}
		if (element.isJsonPrimitive()) {
			String raw = element.getAsString();
			Identifier parsed = Identifier.tryParse(raw);
			return parsed != null ? parsed : fallback;
		} else if (element.isJsonObject()) {
			JsonObject idObj = element.getAsJsonObject();
			String namespace = idObj.has("namespace") ? idObj.get("namespace").getAsString() : "minecraft";
			String path = idObj.has("path") ? idObj.get("path").getAsString() : null;
			if (path != null) {
				return Identifier.of(namespace, path);
			}
		}
		return fallback;
	}

	private static List<Identifier> parseIdArray(JsonObject json, String key) {
		if (json == null || key == null || !json.has(key) || !json.get(key).isJsonArray()) {
			return List.of();
		}
		List<Identifier> ids = new ArrayList<>();
		json.getAsJsonArray(key).forEach(element -> {
			if (element.isJsonPrimitive()) {
				Identifier parsed = Identifier.tryParse(element.getAsString());
				if (parsed != null) {
					ids.add(parsed);
				}
			}
		});
		return List.copyOf(ids);
	}

	@FunctionalInterface
	private interface JsonFileConsumer {
		void accept(Path path, JsonObject json) throws Exception;
	}
}

