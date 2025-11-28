package net.cyberpunk042.client.render;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class SingularityVisualConfig {
	private static final String DEFAULT_PRIMARY_TEXTURE = "the-virus-block:textures/misc/singularity_sphere_1.png";
	private static final String DEFAULT_CORE_TEXTURE = "the-virus-block:textures/misc/singularity_sphere_2.png";

	private int latSteps;
	private int lonSteps;
	private int coreOverlapTicks;

	private float innerBeamRadius;
	private float outerBeamRadius;
	private int blackBeamDelayTicks;
	private float primaryBeamAlpha;
	private float coreBeamAlpha;
	private float blackBeamGrowth;
	private float redBeamGrowth;
	private float beamAlphaStep;

	private SphereConfig primary;
	private SphereConfig core;

	private SingularityVisualConfig() {
		this.primary = new SphereConfig();
		this.core = new SphereConfig();
	}

	public static SingularityVisualConfig defaults() {
		SingularityVisualConfig config = new SingularityVisualConfig();
		config.latSteps = 80;
		config.lonSteps = 200;
		config.coreOverlapTicks = 15;

		config.innerBeamRadius = 0.24F;
		config.outerBeamRadius = 0.34F;
		config.blackBeamDelayTicks = 30;
		config.primaryBeamAlpha = 0.65F;
		config.coreBeamAlpha = 1.0F;
		config.blackBeamGrowth = 0.12F;
		config.redBeamGrowth = 0.2F;
		config.beamAlphaStep = 0.08F;

		config.primary.setDefaults(0.05F, 4.0F, 0.55F, 1.0F, 1.0F, 0.35F,
				SingularityBlockEntity.ORB_GROW_TICKS, SingularityBlockEntity.ORB_SHRINK_TICKS,
				1.0F, DEFAULT_PRIMARY_TEXTURE);
		config.core.setDefaults(0.2F, 1.5F, 0.75F, 1.0F, 1.0F, 0.6F,
				28, 18, 2.5F, DEFAULT_CORE_TEXTURE);
		return config;
	}

	public SingularityVisualConfig copy() {
		SingularityVisualConfig copy = new SingularityVisualConfig();
		copy.latSteps = latSteps;
		copy.lonSteps = lonSteps;
		copy.coreOverlapTicks = coreOverlapTicks;
		copy.innerBeamRadius = innerBeamRadius;
		copy.outerBeamRadius = outerBeamRadius;
		copy.blackBeamDelayTicks = blackBeamDelayTicks;
		copy.primaryBeamAlpha = primaryBeamAlpha;
		copy.coreBeamAlpha = coreBeamAlpha;
		copy.blackBeamGrowth = blackBeamGrowth;
		copy.redBeamGrowth = redBeamGrowth;
		copy.beamAlphaStep = beamAlphaStep;
		copy.primary = primary.copy();
		copy.core = core.copy();
		return copy;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		JsonObject general = new JsonObject();
		general.addProperty("lat_steps", latSteps);
		general.addProperty("lon_steps", lonSteps);
		general.addProperty("core_overlap_ticks", coreOverlapTicks);
		json.add("general", general);

		JsonObject beam = new JsonObject();
		beam.addProperty("inner_radius", innerBeamRadius);
		beam.addProperty("outer_radius", outerBeamRadius);
		beam.addProperty("black_delay", blackBeamDelayTicks);
		beam.addProperty("primary_alpha", primaryBeamAlpha);
		beam.addProperty("core_alpha", coreBeamAlpha);
		beam.addProperty("black_growth", blackBeamGrowth);
		beam.addProperty("red_growth", redBeamGrowth);
		beam.addProperty("alpha_step", beamAlphaStep);
		json.add("beam", beam);

		json.add("primary", primary.toJson());
		json.add("core", core.toJson());
		return json;
	}

	public static SingularityVisualConfig fromJson(JsonObject json) {
		SingularityVisualConfig config = defaults();
		if (json == null) {
			return config;
		}
		JsonObject general = getObject(json, "general");
		if (general != null) {
			config.latSteps = MathHelper.clamp(getInt(general, "lat_steps", config.latSteps), 8, 2048);
			config.lonSteps = MathHelper.clamp(getInt(general, "lon_steps", config.lonSteps), 16, 4096);
			config.coreOverlapTicks = Math.max(0, getInt(general, "core_overlap_ticks", config.coreOverlapTicks));
		}
		JsonObject beam = getObject(json, "beam");
		if (beam != null) {
			config.innerBeamRadius = getFloat(beam, "inner_radius", config.innerBeamRadius);
			config.outerBeamRadius = getFloat(beam, "outer_radius", config.outerBeamRadius);
			config.blackBeamDelayTicks = Math.max(0, getInt(beam, "black_delay", config.blackBeamDelayTicks));
			config.primaryBeamAlpha = clamp01(getFloat(beam, "primary_alpha", config.primaryBeamAlpha));
			config.coreBeamAlpha = clamp01(getFloat(beam, "core_alpha", config.coreBeamAlpha));
			config.blackBeamGrowth = Math.max(0.0F, getFloat(beam, "black_growth", config.blackBeamGrowth));
			config.redBeamGrowth = Math.max(0.0F, getFloat(beam, "red_growth", config.redBeamGrowth));
			config.beamAlphaStep = Math.max(0.0F, getFloat(beam, "alpha_step", config.beamAlphaStep));
		}
		if (json.has("primary")) {
			config.primary = SphereConfig.fromJson(json.getAsJsonObject("primary"), config.primary);
		}
		if (json.has("core")) {
			config.core = SphereConfig.fromJson(json.getAsJsonObject("core"), config.core);
		}
		return config;
	}

	public boolean setValue(String key, String rawValue) {
		String lower = key.toLowerCase(Locale.ROOT);
		try {
			if (lower.startsWith("primary.")) {
				return primary.setValue(lower.substring("primary.".length()), rawValue);
			}
			if (lower.startsWith("core.")) {
				return core.setValue(lower.substring("core.".length()), rawValue);
			}
			return switch (lower) {
				case "general.lat_steps" -> {
					latSteps = MathHelper.clamp(parseInt(rawValue, latSteps), 8, 2048);
					yield true;
				}
				case "general.lon_steps" -> {
					lonSteps = MathHelper.clamp(parseInt(rawValue, lonSteps), 16, 4096);
					yield true;
				}
				case "general.core_overlap_ticks" -> {
					coreOverlapTicks = Math.max(0, parseInt(rawValue, coreOverlapTicks));
					yield true;
				}
				case "beam.inner_radius" -> {
					innerBeamRadius = Math.max(0.0F, parseFloat(rawValue, innerBeamRadius));
					yield true;
				}
				case "beam.outer_radius" -> {
					outerBeamRadius = Math.max(0.0F, parseFloat(rawValue, outerBeamRadius));
					yield true;
				}
				case "beam.black_delay" -> {
					blackBeamDelayTicks = Math.max(0, parseInt(rawValue, blackBeamDelayTicks));
					yield true;
				}
				case "beam.primary_alpha" -> {
					primaryBeamAlpha = clamp01(parseFloat(rawValue, primaryBeamAlpha));
					yield true;
				}
				case "beam.core_alpha" -> {
					coreBeamAlpha = clamp01(parseFloat(rawValue, coreBeamAlpha));
					yield true;
				}
				case "beam.black_growth" -> {
					blackBeamGrowth = Math.max(0.0F, parseFloat(rawValue, blackBeamGrowth));
					yield true;
				}
				case "beam.red_growth" -> {
					redBeamGrowth = Math.max(0.0F, parseFloat(rawValue, redBeamGrowth));
					yield true;
				}
				case "beam.alpha_step" -> {
					beamAlphaStep = Math.max(0.0F, parseFloat(rawValue, beamAlphaStep));
					yield true;
				}
				default -> false;
			};
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public int latSteps() {
		return latSteps;
	}

	public int lonSteps() {
		return lonSteps;
	}

	public int coreOverlapTicks() {
		return coreOverlapTicks;
	}

	public float innerBeamRadius() {
		return innerBeamRadius;
	}

	public float outerBeamRadius() {
		return outerBeamRadius;
	}

	public int blackBeamDelayTicks() {
		return blackBeamDelayTicks;
	}

	public float primaryBeamAlpha() {
		return primaryBeamAlpha;
	}

	public float coreBeamAlpha() {
		return coreBeamAlpha;
	}

	public float blackBeamGrowth() {
		return blackBeamGrowth;
	}

	public float redBeamGrowth() {
		return redBeamGrowth;
	}

	public float beamAlphaStep() {
		return beamAlphaStep;
	}

	public SphereConfig primary() {
		return primary;
	}

	public SphereConfig core() {
		return core;
	}

	private static JsonObject getObject(JsonObject parent, String key) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static float getFloat(JsonObject parent, String key, float fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsFloat() : fallback;
	}

	private static int getInt(JsonObject parent, String key, int fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
	}

	private static float clamp01(float value) {
		return MathHelper.clamp(value, 0.0F, 1.0F);
	}

	private static float parseFloat(String raw, float fallback) {
		if (raw == null || raw.isEmpty()) {
			return fallback;
		}
		return Float.parseFloat(raw);
	}

	private static int parseInt(String raw, int fallback) {
		if (raw == null || raw.isEmpty()) {
			return fallback;
		}
		return Integer.parseInt(raw);
	}

	static final class SphereConfig {
		private float minScale;
		private float maxScale;
		private float growStartAlpha;
		private float growEndAlpha;
		private float shrinkStartAlpha;
		private float shrinkEndAlpha;
		private int growTicks;
		private int shrinkTicks;
		private float spinMultiplier;
		private String texture;

		private SphereConfig() {
		}

		private void setDefaults(float minScale, float maxScale,
		                         float growStartAlpha, float growEndAlpha,
		                         float shrinkStartAlpha, float shrinkEndAlpha,
		                         int growTicks, int shrinkTicks,
		                         float spinMultiplier, String texture) {
			this.minScale = minScale;
			this.maxScale = maxScale;
			this.growStartAlpha = growStartAlpha;
			this.growEndAlpha = growEndAlpha;
			this.shrinkStartAlpha = shrinkStartAlpha;
			this.shrinkEndAlpha = shrinkEndAlpha;
			this.growTicks = growTicks;
			this.shrinkTicks = shrinkTicks;
			this.spinMultiplier = spinMultiplier;
			this.texture = texture;
		}

		private SphereConfig copy() {
			SphereConfig copy = new SphereConfig();
			copy.minScale = minScale;
			copy.maxScale = maxScale;
			copy.growStartAlpha = growStartAlpha;
			copy.growEndAlpha = growEndAlpha;
			copy.shrinkStartAlpha = shrinkStartAlpha;
			copy.shrinkEndAlpha = shrinkEndAlpha;
			copy.growTicks = growTicks;
			copy.shrinkTicks = shrinkTicks;
			copy.spinMultiplier = spinMultiplier;
			copy.texture = texture;
			return copy;
		}

		private JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("min_scale", minScale);
			json.addProperty("max_scale", maxScale);
			json.addProperty("grow_start_alpha", growStartAlpha);
			json.addProperty("grow_end_alpha", growEndAlpha);
			json.addProperty("shrink_start_alpha", shrinkStartAlpha);
			json.addProperty("shrink_end_alpha", shrinkEndAlpha);
			json.addProperty("grow_ticks", growTicks);
			json.addProperty("shrink_ticks", shrinkTicks);
			json.addProperty("spin_multiplier", spinMultiplier);
			json.addProperty("texture", texture);
			return json;
		}

		private static SphereConfig fromJson(JsonObject json, SphereConfig fallback) {
			SphereConfig cfg = fallback.copy();
			if (json == null) {
				return cfg;
			}
			cfg.minScale = Math.max(0.0F, getFloat(json, "min_scale", cfg.minScale));
			cfg.maxScale = Math.max(cfg.minScale, getFloat(json, "max_scale", cfg.maxScale));
			cfg.growStartAlpha = clamp01(getFloat(json, "grow_start_alpha", cfg.growStartAlpha));
			cfg.growEndAlpha = clamp01(getFloat(json, "grow_end_alpha", cfg.growEndAlpha));
			cfg.shrinkStartAlpha = clamp01(getFloat(json, "shrink_start_alpha", cfg.shrinkStartAlpha));
			cfg.shrinkEndAlpha = clamp01(getFloat(json, "shrink_end_alpha", cfg.shrinkEndAlpha));
			cfg.growTicks = Math.max(1, getInt(json, "grow_ticks", cfg.growTicks));
			cfg.shrinkTicks = Math.max(1, getInt(json, "shrink_ticks", cfg.shrinkTicks));
			cfg.spinMultiplier = getFloat(json, "spin_multiplier", cfg.spinMultiplier);
			if (json.has("texture") && json.get("texture").isJsonPrimitive()) {
				cfg.texture = json.get("texture").getAsString();
			}
			return cfg;
		}

		private boolean setValue(String key, String rawValue) {
			return switch (key) {
				case "min_scale" -> {
					minScale = Math.max(0.0F, parseFloat(rawValue, minScale));
					yield true;
				}
				case "max_scale" -> {
					maxScale = Math.max(minScale, parseFloat(rawValue, maxScale));
					yield true;
				}
				case "grow_start_alpha" -> {
					growStartAlpha = clamp01(parseFloat(rawValue, growStartAlpha));
					yield true;
				}
				case "grow_end_alpha" -> {
					growEndAlpha = clamp01(parseFloat(rawValue, growEndAlpha));
					yield true;
				}
				case "shrink_start_alpha" -> {
					shrinkStartAlpha = clamp01(parseFloat(rawValue, shrinkStartAlpha));
					yield true;
				}
				case "shrink_end_alpha" -> {
					shrinkEndAlpha = clamp01(parseFloat(rawValue, shrinkEndAlpha));
					yield true;
				}
				case "grow_ticks" -> {
					growTicks = Math.max(1, parseInt(rawValue, growTicks));
					yield true;
				}
				case "shrink_ticks" -> {
					shrinkTicks = Math.max(1, parseInt(rawValue, shrinkTicks));
					yield true;
				}
				case "spin_multiplier" -> {
					spinMultiplier = parseFloat(rawValue, spinMultiplier);
					yield true;
				}
				case "texture" -> {
					if (rawValue != null && !rawValue.isBlank()) {
						texture = rawValue.trim();
					}
					yield true;
				}
				default -> false;
			};
		}

		public float minScale() {
		 return minScale;
		}

		public float maxScale() {
			return maxScale;
		}

		public float growStartAlpha() {
			return growStartAlpha;
		}

		public float growEndAlpha() {
			return growEndAlpha;
		}

		public float shrinkStartAlpha() {
			return shrinkStartAlpha;
		}

		public float shrinkEndAlpha() {
			return shrinkEndAlpha;
		}

		public int growTicks() {
			return growTicks;
		}

		public int shrinkTicks() {
			return shrinkTicks;
		}

		public float spinMultiplier() {
			return spinMultiplier;
		}

		public Identifier textureId() {
			return Identifier.tryParse(texture);
		}
	}
}

