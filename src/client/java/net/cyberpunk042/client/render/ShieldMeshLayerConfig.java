package net.cyberpunk042.client.render;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.MathHelper;

/**
 * Describes one mesh layer that composes the final shield / forcefield render.
 * Layers can cover arbitrary lat/lon ranges, use different fills, and have
 * independent animation/color/opacity/scale controls.
 */
public final class ShieldMeshLayerConfig {
	private final String id;
	private MeshType meshType;
	private int latSteps;
	private int lonSteps;
	private float latStart;
	private float latEnd;
	private float lonStart;
	private float lonEnd;
	private float radiusMultiplier;
	private float swirlStrength;
	private float phaseOffset;
	private float alphaMin;
	private float alphaMax;
	private int primaryColor;
	private int secondaryColor;
	private int bandCount;
	private float bandThickness;
	private float wireThickness;

	private ShieldMeshLayerConfig(String id) {
		this.id = id;
		this.meshType = MeshType.SOLID;
		this.latSteps = 64;
		this.lonSteps = 160;
		this.latStart = 0.0F;
		this.latEnd = 1.0F;
		this.lonStart = 0.0F;
		this.lonEnd = 1.0F;
		this.radiusMultiplier = 1.0F;
		this.swirlStrength = 0.6F;
		this.phaseOffset = 0.0F;
		this.alphaMin = 0.60F;
		this.alphaMax = 0.98F;
		this.primaryColor = ShieldProfileConfig.parseColor("#FFF5F9FF");
		this.secondaryColor = ShieldProfileConfig.parseColor("#FFA4C0FF");
		this.bandCount = 4;
		this.bandThickness = 0.20F;
		this.wireThickness = 0.05F;
	}

	public static ShieldMeshLayerConfig createDefault(String id) {
		return new ShieldMeshLayerConfig(id);
	}

	public ShieldMeshLayerConfig copy(String newId) {
		ShieldMeshLayerConfig copy = new ShieldMeshLayerConfig(newId);
		copy.meshType = meshType;
		copy.latSteps = latSteps;
		copy.lonSteps = lonSteps;
		copy.latStart = latStart;
		copy.latEnd = latEnd;
		copy.lonStart = lonStart;
		copy.lonEnd = lonEnd;
		copy.radiusMultiplier = radiusMultiplier;
		copy.swirlStrength = swirlStrength;
		copy.phaseOffset = phaseOffset;
		copy.alphaMin = alphaMin;
		copy.alphaMax = alphaMax;
		copy.primaryColor = primaryColor;
		copy.secondaryColor = secondaryColor;
		copy.bandCount = bandCount;
		copy.bandThickness = bandThickness;
		copy.wireThickness = wireThickness;
		return copy;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("mesh_type", meshType.name().toLowerCase(Locale.ROOT));
		json.addProperty("lat_steps", latSteps);
		json.addProperty("lon_steps", lonSteps);
		json.addProperty("lat_start", latStart);
		json.addProperty("lat_end", latEnd);
		json.addProperty("lon_start", lonStart);
		json.addProperty("lon_end", lonEnd);
		json.addProperty("radius_multiplier", radiusMultiplier);
		json.addProperty("swirl_strength", swirlStrength);
		json.addProperty("phase_offset", phaseOffset);
		JsonObject alpha = new JsonObject();
		alpha.addProperty("min", alphaMin);
		alpha.addProperty("max", alphaMax);
		json.add("alpha", alpha);
		JsonObject colors = new JsonObject();
		colors.addProperty("primary", ShieldProfileConfig.formatColor(primaryColor));
		colors.addProperty("secondary", ShieldProfileConfig.formatColor(secondaryColor));
		json.add("colors", colors);
		json.addProperty("band_count", bandCount);
		json.addProperty("band_thickness", bandThickness);
		json.addProperty("wire_thickness", wireThickness);
		return json;
	}

	public static ShieldMeshLayerConfig fromJson(String id, JsonObject json) {
		ShieldMeshLayerConfig layer = new ShieldMeshLayerConfig(id);
		if (json == null) {
			return layer;
		}
		layer.meshType = MeshType.fromString(getString(json, "mesh_type", layer.meshType.name()));
		layer.latSteps = getInt(json, "lat_steps", layer.latSteps);
		layer.lonSteps = getInt(json, "lon_steps", layer.lonSteps);
		layer.latStart = clamp01(getFloat(json, "lat_start", layer.latStart));
		layer.latEnd = clamp01(getFloat(json, "lat_end", layer.latEnd));
		layer.lonStart = clamp01(getFloat(json, "lon_start", layer.lonStart));
		layer.lonEnd = clamp01(getFloat(json, "lon_end", layer.lonEnd));
		layer.radiusMultiplier = Math.max(0.01F, getFloat(json, "radius_multiplier", layer.radiusMultiplier));
		layer.swirlStrength = getFloat(json, "swirl_strength", layer.swirlStrength);
		layer.phaseOffset = getFloat(json, "phase_offset", layer.phaseOffset);
		JsonObject alpha = getObject(json, "alpha");
		if (alpha != null) {
			layer.alphaMin = clamp01(getFloat(alpha, "min", layer.alphaMin));
			layer.alphaMax = clamp01(getFloat(alpha, "max", layer.alphaMax));
		}
		JsonObject color = getObject(json, "colors");
		if (color != null) {
			if (color.has("primary")) {
				layer.primaryColor = ShieldProfileConfig.parseColor(color.get("primary").getAsString());
			}
			if (color.has("secondary")) {
				layer.secondaryColor = ShieldProfileConfig.parseColor(color.get("secondary").getAsString());
			}
		}
		layer.bandCount = getInt(json, "band_count", layer.bandCount);
		layer.bandThickness = clamp01(getFloat(json, "band_thickness", layer.bandThickness));
		layer.wireThickness = clamp01(getFloat(json, "wire_thickness", layer.wireThickness));
		layer.normalizeRanges();
		return layer;
	}

	public void writeNbt(NbtCompound nbt) {
		nbt.putString("meshType", meshType.name());
		nbt.putInt("latSteps", latSteps);
		nbt.putInt("lonSteps", lonSteps);
		nbt.putFloat("latStart", latStart);
		nbt.putFloat("latEnd", latEnd);
		nbt.putFloat("lonStart", lonStart);
		nbt.putFloat("lonEnd", lonEnd);
		nbt.putFloat("radiusMultiplier", radiusMultiplier);
		nbt.putFloat("swirl", swirlStrength);
		nbt.putFloat("phase", phaseOffset);
		nbt.putFloat("alphaMin", alphaMin);
		nbt.putFloat("alphaMax", alphaMax);
		nbt.putInt("primary", primaryColor);
		nbt.putInt("secondary", secondaryColor);
		nbt.putInt("bandCount", bandCount);
		nbt.putFloat("bandThickness", bandThickness);
		nbt.putFloat("wireThickness", wireThickness);
	}

	public static ShieldMeshLayerConfig fromNbt(String id, NbtCompound nbt) {
		ShieldMeshLayerConfig layer = new ShieldMeshLayerConfig(id);
		if (nbt == null) {
			return layer;
		}
		if (nbt.contains("meshType")) {
			layer.meshType = MeshType.fromString(readNbtString(nbt, "meshType"));
		}
		layer.latSteps = readNbtInt(nbt, "latSteps", layer.latSteps);
		layer.lonSteps = readNbtInt(nbt, "lonSteps", layer.lonSteps);
		layer.latStart = clamp01(readNbtFloat(nbt, "latStart", layer.latStart));
		layer.latEnd = clamp01(readNbtFloat(nbt, "latEnd", layer.latEnd));
		layer.lonStart = clamp01(readNbtFloat(nbt, "lonStart", layer.lonStart));
		layer.lonEnd = clamp01(readNbtFloat(nbt, "lonEnd", layer.lonEnd));
		layer.radiusMultiplier = Math.max(0.01F, readNbtFloat(nbt, "radiusMultiplier", layer.radiusMultiplier));
		layer.swirlStrength = readNbtFloat(nbt, "swirl", layer.swirlStrength);
		layer.phaseOffset = readNbtFloat(nbt, "phase", layer.phaseOffset);
		layer.alphaMin = clamp01(readNbtFloat(nbt, "alphaMin", layer.alphaMin));
		layer.alphaMax = clamp01(readNbtFloat(nbt, "alphaMax", layer.alphaMax));
		layer.primaryColor = readNbtInt(nbt, "primary", layer.primaryColor);
		layer.secondaryColor = readNbtInt(nbt, "secondary", layer.secondaryColor);
		layer.bandCount = readNbtInt(nbt, "bandCount", layer.bandCount);
		layer.bandThickness = clamp01(readNbtFloat(nbt, "bandThickness", layer.bandThickness));
		layer.wireThickness = clamp01(readNbtFloat(nbt, "wireThickness", layer.wireThickness));
		layer.normalizeRanges();
		return layer;
	}

	private void normalizeRanges() {
		if (latEnd <= latStart) {
			latEnd = Math.min(1.0F, latStart + 0.05F);
		}
		if (lonEnd <= lonStart) {
			lonEnd = Math.min(1.0F, lonStart + 0.05F);
		}
	}

	public void setValue(String key, String rawValue) {
		String lower = key.toLowerCase(Locale.ROOT);
		try {
			switch (lower) {
				case "mesh_type" -> meshType = MeshType.fromString(rawValue);
				case "lat_steps" -> latSteps = clampInt(rawValue, latSteps, 2, 2048);
				case "lon_steps" -> lonSteps = clampInt(rawValue, lonSteps, 4, 4096);
				case "lat_start" -> {
					latStart = clamp01(parseFloat(rawValue, latStart));
					normalizeRanges();
				}
				case "lat_end" -> {
					latEnd = clamp01(parseFloat(rawValue, latEnd));
					normalizeRanges();
				}
				case "lon_start" -> {
					lonStart = clamp01(parseFloat(rawValue, lonStart));
					normalizeRanges();
				}
				case "lon_end" -> {
					lonEnd = clamp01(parseFloat(rawValue, lonEnd));
					normalizeRanges();
				}
				case "radius_multiplier" -> radiusMultiplier = Math.max(0.01F, parseFloat(rawValue, radiusMultiplier));
				case "swirl_strength" -> swirlStrength = parseFloat(rawValue, swirlStrength);
				case "phase_offset" -> phaseOffset = parseFloat(rawValue, phaseOffset);
				case "alpha.min" -> alphaMin = clamp01(parseFloat(rawValue, alphaMin));
				case "alpha.max" -> alphaMax = clamp01(parseFloat(rawValue, alphaMax));
				case "color.primary" -> primaryColor = ShieldProfileConfig.parseColor(rawValue);
				case "color.secondary" -> secondaryColor = ShieldProfileConfig.parseColor(rawValue);
				case "band.count" -> bandCount = Math.max(0, clampInt(rawValue, bandCount, 0, 512));
				case "band.thickness" -> bandThickness = clamp01(parseFloat(rawValue, bandThickness));
				case "wire.thickness" -> wireThickness = clamp01(parseFloat(rawValue, wireThickness));
				default -> throw new IllegalArgumentException("Unknown mesh key '" + key + "'");
			}
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid value '" + rawValue + "' for " + key, ex);
		}
	}

	public static Set<String> knownKeys() {
		Set<String> keys = new LinkedHashSet<>();
		keys.add("mesh_type");
		keys.add("lat_steps");
		keys.add("lon_steps");
		keys.add("lat_start");
		keys.add("lat_end");
		keys.add("lon_start");
		keys.add("lon_end");
		keys.add("radius_multiplier");
		keys.add("swirl_strength");
		keys.add("phase_offset");
		keys.add("alpha.min");
		keys.add("alpha.max");
		keys.add("color.primary");
		keys.add("color.secondary");
		keys.add("band.count");
		keys.add("band.thickness");
		keys.add("wire.thickness");
		return keys;
	}

	public String id() {
		return id;
	}

	public MeshType meshType() {
		return meshType;
	}

	public int latSteps() {
		return latSteps;
	}

	public int lonSteps() {
		return lonSteps;
	}

	public float latStart() {
		return latStart;
	}

	public float latEnd() {
		return latEnd;
	}

	public float lonStart() {
		return lonStart;
	}

	public float lonEnd() {
		return lonEnd;
	}

	public float radiusMultiplier() {
		return radiusMultiplier;
	}

	public float swirlStrength() {
		return swirlStrength;
	}

	public float phaseOffset() {
		return phaseOffset;
	}

	public float alphaMin() {
		return alphaMin;
	}

	public float alphaMax() {
		return alphaMax;
	}

	public int primaryColor() {
		return primaryColor;
	}

	public int secondaryColor() {
		return secondaryColor;
	}

	public int bandCount() {
		return bandCount;
	}

	public float bandThickness() {
		return bandThickness;
	}

	public float wireThickness() {
		return wireThickness;
	}

	public void setLatSteps(int value) {
		this.latSteps = MathHelper.clamp(value, 2, 2048);
	}

	public void setLonSteps(int value) {
		this.lonSteps = MathHelper.clamp(value, 4, 4096);
	}

	public void setSwirlStrength(float swirl) {
		this.swirlStrength = swirl;
	}

	public void setPhaseOffset(float phase) {
		this.phaseOffset = phase;
	}

	public void setRadiusMultiplier(float multiplier) {
		this.radiusMultiplier = Math.max(0.01F, multiplier);
	}

	public void setLatRange(float start, float end) {
		this.latStart = clamp01(Math.min(start, end));
		this.latEnd = clamp01(Math.max(start, end));
		normalizeRanges();
	}

	public void setLonRange(float start, float end) {
		this.lonStart = clamp01(Math.min(start, end));
		this.lonEnd = clamp01(Math.max(start, end));
		normalizeRanges();
	}

	public void setBandCount(int count) {
		this.bandCount = Math.max(0, count);
	}

	public void setBandThickness(float thickness) {
		this.bandThickness = clamp01(thickness);
	}

	public void setWireThickness(float thickness) {
		this.wireThickness = clamp01(thickness);
	}

	public void setMeshType(MeshType type) {
		this.meshType = type;
	}

	public void setAlphaRange(float min, float max) {
		float clampedMin = MathHelper.clamp(min, 0.0F, 1.0F);
		float clampedMax = MathHelper.clamp(max, clampedMin, 1.0F);
		this.alphaMin = clampedMin;
		this.alphaMax = clampedMax;
	}

	public void setColors(int primary, int secondary) {
		this.primaryColor = primary;
		this.secondaryColor = secondary;
	}

	public boolean shouldRenderCell(int latIndex, int latTotal, int lonIndex, int lonTotal, float latFraction, float lonFraction) {
		return switch (meshType) {
			case SOLID, HEMISPHERE -> true;
			case BANDS -> {
				if (bandCount <= 0) {
					yield true;
				}
				float scaled = latFraction * bandCount;
				float frac = scaled - MathHelper.floor(scaled);
				yield frac <= bandThickness;
			}
			case WIREFRAME -> {
				float latEdge = Math.min(latFraction, 1.0F - latFraction);
				float lonEdge = Math.min(lonFraction, 1.0F - lonFraction);
				yield latEdge <= wireThickness || lonEdge <= wireThickness;
			}
			case CHECKER -> {
				int band = Math.max(1, bandCount);
				int latCell = Math.min(band - 1, MathHelper.floor(latFraction * band));
				int lonCell = Math.min(band - 1, MathHelper.floor(lonFraction * band));
				yield ((latCell + lonCell) & 1) == 0;
			}
		};
	}

	private static float clamp01(float value) {
		return MathHelper.clamp(value, 0.0F, 1.0F);
	}

	private static float parseFloat(String raw, float fallback) {
		return raw.isEmpty() ? fallback : Float.parseFloat(raw);
	}

	private static int clampInt(String raw, int fallback, int min, int max) {
		if (raw.isEmpty()) {
			return fallback;
		}
		int value = Integer.parseInt(raw);
		return MathHelper.clamp(value, min, max);
	}

	enum MeshType {
		SOLID,
		BANDS,
		WIREFRAME,
		CHECKER,
		HEMISPHERE;

		static MeshType fromString(String value) {
			if (value == null || value.isEmpty()) {
				return SOLID;
			}
			try {
				return MeshType.valueOf(value.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ex) {
				return SOLID;
			}
		}
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

	private static String getString(JsonObject parent, String key, String fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
	}

	private static int readNbtInt(NbtCompound nbt, String key, int fallback) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			if (element instanceof AbstractNbtNumber number) {
				return number.intValue();
			}
		}
		return fallback;
	}

	private static float readNbtFloat(NbtCompound nbt, String key, float fallback) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			if (element instanceof AbstractNbtNumber number) {
				return number.floatValue();
			}
		}
		return fallback;
	}

	private static String readNbtString(NbtCompound nbt, String key) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			if (element != null) {
				return element.asString().orElse("");
			}
		}
		return "";
	}
}

