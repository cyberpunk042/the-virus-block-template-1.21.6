package net.cyberpunk042.client.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.cyberpunk042.config.ColorConfig;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.MathHelper;

/**
 * Fully configurable profile for shield / forcefield visuals.
 */
public final class ShieldProfileConfig {

	private float radius;
	private float visualScale;
	private float spinSpeed;
	private float tiltMultiplier;
	private int primaryColor;
	private int secondaryColor;
	private float minAlpha;
	private float maxAlpha;
	private boolean beamEnabled;
	private float beamInnerRadius;
	private float beamOuterRadius;
	private int beamColor;
	private boolean predictionEnabled;
	private int predictionLeadTicks;
	private float predictionMaxDistance;
	private float predictionLookAhead;
	private float predictionVerticalBoost;

	private final LinkedHashMap<String, ShieldMeshLayerConfig> meshLayers = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> layerStyles = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> layerShapes = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> layerTriangles = new LinkedHashMap<>();

	private ShieldProfileConfig(float radius,
	                            float visualScale,
	                            float spinSpeed,
	                            float tiltMultiplier,
	                            int primaryColor,
	                            int secondaryColor,
	                            float minAlpha,
	                            float maxAlpha,
	                            boolean beamEnabled,
	                            float beamInnerRadius,
	                            float beamOuterRadius,
	                            int beamColor,
	                            boolean predictionEnabled,
	                            int predictionLeadTicks,
	                            float predictionMaxDistance,
	                            float predictionLookAhead,
	                            float predictionVerticalBoost) {
		this.radius = radius;
		this.visualScale = visualScale;
		this.spinSpeed = spinSpeed;
		this.tiltMultiplier = tiltMultiplier;
		this.primaryColor = primaryColor;
		this.secondaryColor = secondaryColor;
		this.minAlpha = minAlpha;
		this.maxAlpha = maxAlpha;
		this.beamEnabled = beamEnabled;
		this.beamInnerRadius = beamInnerRadius;
		this.beamOuterRadius = beamOuterRadius;
		this.beamColor = beamColor;
		this.predictionEnabled = predictionEnabled;
		this.predictionLeadTicks = predictionLeadTicks;
		this.predictionMaxDistance = predictionMaxDistance;
		this.predictionLookAhead = predictionLookAhead;
		this.predictionVerticalBoost = predictionVerticalBoost;
	}

	public static ShieldProfileConfig defaults() {
		ShieldProfileConfig config = new ShieldProfileConfig(
				18.0F,
				0.30F,
				0.03F,
				0.0F,
				parseColor("#FFF5F9FF"),
				parseColor("#FFA4C0FF"),
				0.60F,
				0.98F,
				true,
				0.045F,
				0.06F,
				parseColor("#FFEAFAFF"),
				false,
				6,
				3.5F,
				0.5F,
				0.0F);
		config.meshLayers.put("shell", ShieldMeshLayerConfig.createDefault("shell"));
		return config;
	}

	public ShieldProfileConfig copy() {
		ShieldProfileConfig copy = new ShieldProfileConfig(
				radius,
				visualScale,
				spinSpeed,
				tiltMultiplier,
				primaryColor,
				secondaryColor,
				minAlpha,
				maxAlpha,
				beamEnabled,
				beamInnerRadius,
				beamOuterRadius,
				beamColor,
				predictionEnabled,
				predictionLeadTicks,
				predictionMaxDistance,
				predictionLookAhead,
				predictionVerticalBoost);
		meshLayers.forEach((id, layer) -> copy.meshLayers.put(id, layer.copy(id)));
		layerStyles.forEach(copy.layerStyles::put);
		layerShapes.forEach(copy.layerShapes::put);
		layerTriangles.forEach(copy.layerTriangles::put);
		return copy;
	}

	public float radius() {
		return radius;
	}

	public ShieldProfileConfig setRadius(float radius) {
		this.radius = Math.max(1.0F, radius);
		return this;
	}

	public float visualScale() {
		return visualScale;
	}

	public ShieldProfileConfig setVisualScale(float scale) {
		this.visualScale = MathHelper.clamp(scale, 0.01F, 5.0F);
		return this;
	}

	public float spinSpeed() {
		return spinSpeed;
	}

	public ShieldProfileConfig setSpinSpeed(float speed) {
		this.spinSpeed = MathHelper.clamp(speed, -0.5F, 0.5F);
		return this;
	}

	public float tiltMultiplier() {
		return tiltMultiplier;
	}

	public ShieldProfileConfig setTiltMultiplier(float tilt) {
		this.tiltMultiplier = MathHelper.clamp(tilt, -2.0F, 2.0F);
		return this;
	}

	public int primaryColor() {
		return primaryColor;
	}

	public ShieldProfileConfig setPrimaryColor(int color) {
		this.primaryColor = color;
		getPrimaryLayer().setColors(primaryColor, secondaryColor);
		return this;
	}

	public int secondaryColor() {
		return secondaryColor;
	}

	public ShieldProfileConfig setSecondaryColor(int color) {
		this.secondaryColor = color;
		getPrimaryLayer().setColors(primaryColor, secondaryColor);
		return this;
	}

	public ShieldProfileConfig setAlphaRange(float min, float max) {
		float clampedMin = MathHelper.clamp(min, 0.0F, 1.0F);
		float clampedMax = MathHelper.clamp(max, clampedMin, 1.0F);
		this.minAlpha = clampedMin;
		this.maxAlpha = clampedMax;
		getPrimaryLayer().setAlphaRange(minAlpha, maxAlpha);
		return this;
	}

	public float minAlpha() {
		return minAlpha;
	}

	public float maxAlpha() {
		return maxAlpha;
	}

	public boolean beamEnabled() {
		return beamEnabled;
	}

	public ShieldProfileConfig setBeamEnabled(boolean enabled) {
		this.beamEnabled = enabled;
		return this;
	}

	public ShieldProfileConfig setBeamRadii(float inner, float outer) {
		this.beamInnerRadius = MathHelper.clamp(inner, 0.0F, 1.0F);
		this.beamOuterRadius = Math.max(beamInnerRadius, MathHelper.clamp(outer, 0.0F, 1.0F));
		return this;
	}

	public float beamInnerRadius() {
		return beamInnerRadius;
	}

	public float beamOuterRadius() {
		return beamOuterRadius;
	}

	public ShieldProfileConfig setBeamColor(int color) {
		this.beamColor = color;
		return this;
	}

	public int beamColor() {
		return beamColor;
	}

	public boolean predictionEnabled() {
		return predictionEnabled;
	}

	public ShieldProfileConfig setPredictionEnabled(boolean enabled) {
		this.predictionEnabled = enabled;
		return this;
	}

	public int predictionLeadTicks() {
		return predictionLeadTicks;
	}

	public ShieldProfileConfig setPredictionLeadTicks(int ticks) {
		this.predictionLeadTicks = MathHelper.clamp(ticks, 0, 60);
		return this;
	}

	public float predictionMaxDistance() {
		return predictionMaxDistance;
	}

	public ShieldProfileConfig setPredictionMaxDistance(float distance) {
		this.predictionMaxDistance = MathHelper.clamp(distance, 0.0F, 64.0F);
		return this;
	}

	public float predictionLookAhead() {
		return predictionLookAhead;
	}

	public ShieldProfileConfig setPredictionLookAhead(float scale) {
		this.predictionLookAhead = MathHelper.clamp(scale, -16.0F, 16.0F);
		return this;
	}

	public float predictionVerticalBoost() {
		return predictionVerticalBoost;
	}

	public ShieldProfileConfig setPredictionVerticalBoost(float boost) {
		this.predictionVerticalBoost = MathHelper.clamp(boost, -16.0F, 16.0F);
		return this;
	}

	public int latSteps() {
		return getPrimaryLayer().latSteps();
	}

	public ShieldProfileConfig setLatSteps(int value) {
		getPrimaryLayer().setLatSteps(value);
		return this;
	}

	public int lonSteps() {
		return getPrimaryLayer().lonSteps();
	}

	public ShieldProfileConfig setLonSteps(int value) {
		getPrimaryLayer().setLonSteps(value);
		return this;
	}

	public float swirlStrength() {
		return getPrimaryLayer().swirlStrength();
	}

	public ShieldProfileConfig setSwirlStrength(float value) {
		getPrimaryLayer().setSwirlStrength(value);
		return this;
	}

	public Map<String, ShieldMeshLayerConfig> meshLayers() {
		ensureLayers();
		return Collections.unmodifiableMap(meshLayers);
	}

	public Map<String, String> layerStyles() {
		return Collections.unmodifiableMap(layerStyles);
	}

	public String getLayerStyle(String id) {
		return layerStyles.get(id);
	}

	public Map<String, String> layerShapes() {
		return Collections.unmodifiableMap(layerShapes);
	}

	public String getLayerShape(String id) {
		return layerShapes.get(id);
	}

	public Map<String, String> layerTriangles() {
		return Collections.unmodifiableMap(layerTriangles);
	}

	public String getLayerTriangle(String id) {
		return layerTriangles.get(id);
	}

	public boolean addLayer(String id) {
		ensureLayers();
		String sanitized = sanitizeLayerId(id);
		if (sanitized.isEmpty() || meshLayers.containsKey(sanitized)) {
			return false;
		}
		ShieldMeshLayerConfig layer = ShieldMeshLayerConfig.createDefault(sanitized);
		layer.setColors(primaryColor, secondaryColor);
		layer.setAlphaRange(minAlpha, maxAlpha);
		meshLayers.put(sanitized, layer);
		layerStyles.put(sanitized, null);
		layerShapes.put(sanitized, null);
		layerTriangles.put(sanitized, "default");
		return true;
	}

	public boolean removeLayer(String id) {
		if (meshLayers.size() <= 1) {
			return false;
		}
		if (meshLayers.remove(id) != null) {
			layerStyles.remove(id);
			layerShapes.remove(id);
			layerTriangles.remove(id);
			return true;
		}
		return false;
	}

	public boolean setLayerValue(String id, String key, String value) {
		ShieldMeshLayerConfig layer = meshLayers.get(id);
		if (layer == null) {
			return false;
		}
		try {
			if ("style".equalsIgnoreCase(key)) {
				return applyLayerStyle(id, value);
			}
			if ("shape".equalsIgnoreCase(key)) {
				return applyLayerShape(id, value);
			}
			if ("triangle".equalsIgnoreCase(key)) {
				return applyLayerTriangle(id, value);
			}
			layer.setValue(key, value);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public String toPrettyString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(toJson());
	}

	public Set<String> knownKeys() {
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		Collections.addAll(keys,
				"radius",
				"animation.scale",
				"animation.spin",
				"animation.tilt",
				"color.primary",
				"color.secondary",
				"alpha.min",
				"alpha.max",
				"beam.enabled",
				"beam.inner",
				"beam.outer",
				"beam.color",
				"prediction.enabled",
				"prediction.lead",
				"prediction.max",
				"prediction.look",
				"prediction.vertical",
				"mesh.lat",
				"mesh.lon",
				"mesh.swirl");
		meshLayers.forEach((id, layer) -> {
			keys.add("mesh." + id + ".style");
			keys.add("mesh." + id + ".shape");
			keys.add("mesh." + id + ".triangle");
			for (String layerKey : ShieldMeshLayerConfig.knownKeys()) {
				keys.add("mesh." + id + "." + layerKey);
			}
		});
		return Collections.unmodifiableSet(keys);
	}

	public boolean setValue(String key, String rawValue) {
		String lower = key.toLowerCase(Locale.ROOT);
		if (lower.startsWith("mesh.") && lower.length() > 5) {
			String remainder = lower.substring(5);
			int dot = remainder.indexOf('.');
			if (dot > 0) {
				String id = remainder.substring(0, dot);
				String layerKey = remainder.substring(dot + 1);
				return setLayerValue(id, layerKey, rawValue);
			}
			return false;
		}
		return switch (lower) {
			case "radius" -> {
				setRadius(parseFloat(rawValue, radius));
				yield true;
			}
			case "mesh.lat" -> {
				setLatSteps(parseInt(rawValue, latSteps()));
				yield true;
			}
			case "mesh.lon" -> {
				setLonSteps(parseInt(rawValue, lonSteps()));
				yield true;
			}
			case "mesh.swirl" -> {
				setSwirlStrength(parseFloat(rawValue, swirlStrength()));
				yield true;
			}
			case "animation.scale" -> {
				setVisualScale(parseFloat(rawValue, visualScale));
				yield true;
			}
			case "animation.spin" -> {
				setSpinSpeed(parseFloat(rawValue, spinSpeed));
				yield true;
			}
			case "animation.tilt" -> {
				setTiltMultiplier(parseFloat(rawValue, tiltMultiplier));
				yield true;
			}
			case "color.primary" -> {
				setPrimaryColor(parseColor(rawValue));
				yield true;
			}
			case "color.secondary" -> {
				setSecondaryColor(parseColor(rawValue));
				yield true;
			}
			case "alpha.min" -> {
				setAlphaRange(parseFloat(rawValue, minAlpha), maxAlpha);
				yield true;
			}
			case "alpha.max" -> {
				setAlphaRange(minAlpha, parseFloat(rawValue, maxAlpha));
				yield true;
			}
			case "beam.enabled" -> {
				setBeamEnabled(parseBoolean(rawValue, beamEnabled));
				yield true;
			}
			case "beam.inner" -> {
				setBeamRadii(parseFloat(rawValue, beamInnerRadius), beamOuterRadius);
				yield true;
			}
			case "beam.outer" -> {
				setBeamRadii(beamInnerRadius, parseFloat(rawValue, beamOuterRadius));
				yield true;
			}
			case "beam.color" -> {
				setBeamColor(parseColor(rawValue));
				yield true;
			}
			case "prediction.enabled" -> {
				setPredictionEnabled(parseBoolean(rawValue, predictionEnabled));
				yield true;
			}
			case "prediction.lead" -> {
				setPredictionLeadTicks(parseInt(rawValue, predictionLeadTicks));
				yield true;
			}
			case "prediction.max" -> {
				setPredictionMaxDistance(parseFloat(rawValue, predictionMaxDistance));
				yield true;
			}
			case "prediction.look" -> {
				setPredictionLookAhead(parseFloat(rawValue, predictionLookAhead));
				yield true;
			}
			case "prediction.vertical" -> {
				setPredictionVerticalBoost(parseFloat(rawValue, predictionVerticalBoost));
				yield true;
			}
			default -> false;
		};
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("radius", radius);

		JsonObject mesh = new JsonObject();
		mesh.addProperty("lat_steps", getPrimaryLayer().latSteps());
		mesh.addProperty("lon_steps", getPrimaryLayer().lonSteps());
		mesh.addProperty("swirl_strength", getPrimaryLayer().swirlStrength());
		JsonObject layersJson = new JsonObject();
		for (Map.Entry<String, ShieldMeshLayerConfig> entry : meshLayers.entrySet()) {
			JsonObject layerJson = entry.getValue().toJson();
			String styleId = layerStyles.get(entry.getKey());
			if (styleId != null && !styleId.isEmpty()) {
				layerJson.addProperty("style", styleId);
			}
			String shapeId = layerShapes.get(entry.getKey());
			if (shapeId != null && !shapeId.isEmpty()) {
				layerJson.addProperty("shape", shapeId);
			}
			String triangleId = layerTriangles.get(entry.getKey());
			if (triangleId != null && !triangleId.isEmpty() && !"default".equals(triangleId)) {
				layerJson.addProperty("triangle", triangleId);
			}
			layersJson.add(entry.getKey(), layerJson);
		}
		mesh.add("layers", layersJson);
		root.add("mesh", mesh);

		JsonObject animation = new JsonObject();
		animation.addProperty("scale", visualScale);
		animation.addProperty("spin_speed", spinSpeed);
		animation.addProperty("tilt", tiltMultiplier);
		root.add("animation", animation);

		JsonObject color = new JsonObject();
		color.addProperty("primary", formatColor(primaryColor));
		color.addProperty("secondary", formatColor(secondaryColor));
		root.add("color", color);

		JsonObject alpha = new JsonObject();
		alpha.addProperty("min", minAlpha);
		alpha.addProperty("max", maxAlpha);
		root.add("alpha", alpha);

		JsonObject beam = new JsonObject();
		beam.addProperty("enabled", beamEnabled);
		beam.addProperty("inner_radius", beamInnerRadius);
		beam.addProperty("outer_radius", beamOuterRadius);
		beam.addProperty("color", formatColor(beamColor));
		root.add("beam", beam);

		JsonObject prediction = new JsonObject();
		prediction.addProperty("enabled", predictionEnabled);
		prediction.addProperty("lead_ticks", predictionLeadTicks);
		prediction.addProperty("max_distance", predictionMaxDistance);
		prediction.addProperty("look_ahead", predictionLookAhead);
		prediction.addProperty("vertical_boost", predictionVerticalBoost);
		root.add("prediction", prediction);

		return root;
	}

	public static ShieldProfileConfig fromJson(JsonObject json) {
		ShieldProfileConfig config = ShieldProfileConfig.defaults();
		if (json == null) {
			return config;
		}
		if (json.has("radius")) {
			config.setRadius(readFloat(json, "radius", config.radius));
		}
		JsonObject mesh = getObject(json, "mesh");
		if (mesh != null) {
			Integer latOverride = mesh.has("lat_steps") ? readInt(mesh, "lat_steps", config.latSteps()) : null;
			Integer lonOverride = mesh.has("lon_steps") ? readInt(mesh, "lon_steps", config.lonSteps()) : null;
			Float swirlOverride = mesh.has("swirl_strength") ? readFloat(mesh, "swirl_strength", config.swirlStrength()) : null;
			JsonObject layersJson = getObject(mesh, "layers");
			if (layersJson != null && !layersJson.entrySet().isEmpty()) {
				config.meshLayers.clear();
				config.layerStyles.clear();
				config.layerShapes.clear();
				config.layerTriangles.clear();
				for (Map.Entry<String, JsonElement> entry : layersJson.entrySet()) {
					String id = sanitizeLayerId(entry.getKey());
					if (id.isEmpty()) {
						continue;
					}
					JsonObject layerJson = entry.getValue().getAsJsonObject();
					String styleId = null;
					if (layerJson.has("style")) {
						styleId = ShieldMeshStyleStore.sanitizeName(layerJson.get("style").getAsString());
					}
					ShieldMeshLayerConfig layerConfig = ShieldMeshLayerConfig.createDefault(id);
					if (styleId != null && !styleId.isEmpty()) {
						layerConfig = ShieldMeshStyleStore.getStyle(styleId).orElse(layerConfig).copy(id);
						config.layerStyles.put(id, styleId);
					} else {
						config.layerStyles.put(id, null);
					}
					String shapeId = null;
					if (layerJson.has("shape")) {
						shapeId = ShieldMeshShapeStore.sanitizeName(layerJson.get("shape").getAsString());
						final ShieldMeshLayerConfig layerRef = layerConfig;
						ShieldMeshShapeStore.getShape(shapeId).ifPresent(shape -> shape.applyTo(layerRef));
					}
					config.layerShapes.put(id, shapeId);
					String triangleId = "default";
					if (layerJson.has("triangle")) {
						triangleId = ShieldTriangleTypeStore.sanitize(layerJson.get("triangle").getAsString());
						if (triangleId.isEmpty()) {
							triangleId = "default";
						}
					}
					config.layerTriangles.put(id, triangleId);
					applyLayerOverrides(layerConfig, layerJson);
					config.meshLayers.put(id, layerConfig);
				}
			}
			if (latOverride != null) {
				config.setLatSteps(latOverride);
			}
			if (lonOverride != null) {
				config.setLonSteps(lonOverride);
			}
			if (swirlOverride != null) {
				config.setSwirlStrength(swirlOverride);
			}
		}
		JsonObject animation = getObject(json, "animation");
		if (animation != null) {
			if (animation.has("scale")) {
				config.setVisualScale(readFloat(animation, "scale", config.visualScale));
			}
			if (animation.has("spin_speed")) {
				config.setSpinSpeed(readFloat(animation, "spin_speed", config.spinSpeed));
			}
			if (animation.has("tilt")) {
				config.setTiltMultiplier(readFloat(animation, "tilt", config.tiltMultiplier()));
			}
		}
		JsonObject color = getObject(json, "color");
		if (color != null) {
			if (color.has("primary")) {
				config.setPrimaryColor(parseColor(color.get("primary").getAsString()));
			}
			if (color.has("secondary")) {
				config.setSecondaryColor(parseColor(color.get("secondary").getAsString()));
			}
		}
		JsonObject alpha = getObject(json, "alpha");
		if (alpha != null) {
			float min = readFloat(alpha, "min", config.minAlpha);
			float max = readFloat(alpha, "max", config.maxAlpha);
			config.setAlphaRange(min, max);
		}
		JsonObject beam = getObject(json, "beam");
		if (beam != null) {
			config.setBeamEnabled(readBoolean(beam, "enabled", config.beamEnabled));
			config.setBeamRadii(
					readFloat(beam, "inner_radius", config.beamInnerRadius),
					readFloat(beam, "outer_radius", config.beamOuterRadius));
			if (beam.has("color")) {
				config.setBeamColor(parseColor(beam.get("color").getAsString()));
			}
		}
		JsonObject prediction = getObject(json, "prediction");
		if (prediction != null) {
			if (prediction.has("enabled")) {
				config.setPredictionEnabled(readBoolean(prediction, "enabled", config.predictionEnabled));
			}
			if (prediction.has("lead_ticks")) {
				config.setPredictionLeadTicks(readInt(prediction, "lead_ticks", config.predictionLeadTicks));
			}
			if (prediction.has("max_distance")) {
				config.setPredictionMaxDistance(readFloat(prediction, "max_distance", config.predictionMaxDistance));
			}
			if (prediction.has("look_ahead")) {
				config.setPredictionLookAhead(readFloat(prediction, "look_ahead", config.predictionLookAhead));
			}
			if (prediction.has("vertical_boost")) {
				config.setPredictionVerticalBoost(readFloat(prediction, "vertical_boost", config.predictionVerticalBoost));
			}
		}
		config.ensureLayers();
		return config;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putFloat("radius", radius);
		nbt.putFloat("scale", visualScale);
		nbt.putFloat("spin", spinSpeed);
		nbt.putFloat("tilt", tiltMultiplier);
		nbt.putInt("primary", primaryColor);
		nbt.putInt("secondary", secondaryColor);
		nbt.putFloat("alphaMin", minAlpha);
		nbt.putFloat("alphaMax", maxAlpha);
		nbt.putBoolean("beamEnabled", beamEnabled);
		nbt.putFloat("beamInner", beamInnerRadius);
		nbt.putFloat("beamOuter", beamOuterRadius);
		nbt.putInt("beamColor", beamColor);
		nbt.putBoolean("predictionEnabled", predictionEnabled);
		nbt.putInt("predictionLead", predictionLeadTicks);
		nbt.putFloat("predictionMax", predictionMaxDistance);
		nbt.putFloat("predictionLook", predictionLookAhead);
		nbt.putFloat("predictionVertical", predictionVerticalBoost);
		NbtList layers = new NbtList();
		meshLayers.forEach((id, layer) -> {
			NbtCompound entry = new NbtCompound();
			entry.putString("id", id);
			layer.writeNbt(entry);
			layers.add(entry);
		});
		nbt.put("layers", layers);
		return nbt;
	}

	public static ShieldProfileConfig fromNbt(@Nullable NbtCompound nbt) {
		if (nbt == null) {
			return defaults();
		}
		ShieldProfileConfig cfg = defaults();
		if (nbt.contains("radius")) {
			cfg.setRadius(readNbtFloat(nbt, "radius", cfg.radius()));
		}
		if (nbt.contains("scale")) {
			cfg.setVisualScale(readNbtFloat(nbt, "scale", cfg.visualScale()));
		}
		if (nbt.contains("spin")) {
			cfg.setSpinSpeed(readNbtFloat(nbt, "spin", cfg.spinSpeed()));
		}
		if (nbt.contains("tilt")) {
			cfg.setTiltMultiplier(readNbtFloat(nbt, "tilt", cfg.tiltMultiplier()));
		}
		if (nbt.contains("primary")) {
			cfg.setPrimaryColor(readNbtInt(nbt, "primary", cfg.primaryColor()));
		}
		if (nbt.contains("secondary")) {
			cfg.setSecondaryColor(readNbtInt(nbt, "secondary", cfg.secondaryColor()));
		}
		if (nbt.contains("alphaMin") || nbt.contains("alphaMax")) {
			cfg.setAlphaRange(
					readNbtFloat(nbt, "alphaMin", cfg.minAlpha()),
					readNbtFloat(nbt, "alphaMax", cfg.maxAlpha()));
		}
		if (nbt.contains("beamEnabled")) {
			cfg.setBeamEnabled(readNbtBoolean(nbt, "beamEnabled", cfg.beamEnabled()));
		}
		if (nbt.contains("beamInner") || nbt.contains("beamOuter")) {
			cfg.setBeamRadii(
					readNbtFloat(nbt, "beamInner", cfg.beamInnerRadius()),
					readNbtFloat(nbt, "beamOuter", cfg.beamOuterRadius()));
		}
		if (nbt.contains("beamColor")) {
			cfg.setBeamColor(readNbtInt(nbt, "beamColor", cfg.beamColor()));
		}
		if (nbt.contains("predictionEnabled")) {
			cfg.setPredictionEnabled(readNbtBoolean(nbt, "predictionEnabled", cfg.predictionEnabled()));
		}
		if (nbt.contains("predictionLead")) {
			cfg.setPredictionLeadTicks(readNbtInt(nbt, "predictionLead", cfg.predictionLeadTicks()));
		}
		if (nbt.contains("predictionMax")) {
			cfg.setPredictionMaxDistance(readNbtFloat(nbt, "predictionMax", cfg.predictionMaxDistance()));
		}
		if (nbt.contains("predictionLook")) {
			cfg.setPredictionLookAhead(readNbtFloat(nbt, "predictionLook", cfg.predictionLookAhead()));
		}
		if (nbt.contains("predictionVertical")) {
			cfg.setPredictionVerticalBoost(readNbtFloat(nbt, "predictionVertical", cfg.predictionVerticalBoost()));
		}
		if (nbt.contains("layers")) {
			cfg.meshLayers.clear();
			NbtElement raw = nbt.get("layers");
			if (raw instanceof NbtList layers) {
				for (NbtElement element : layers) {
					if (element instanceof NbtCompound compound) {
						String id = sanitizeLayerId(readNbtString(compound, "id"));
						if (id.isEmpty()) {
							continue;
						}
						cfg.meshLayers.put(id, ShieldMeshLayerConfig.fromNbt(id, compound));
					}
				}
			}
		}
		cfg.ensureLayers();
		return cfg;
	}

	public static ShieldProfileConfig fromJsonString(String json) {
		return fromJson(JsonParser.parseString(json).getAsJsonObject());
	}

	public void applyJson(JsonObject overrides) {
		ShieldProfileConfig updated = fromJson(overrides);
		this.radius = updated.radius;
		this.visualScale = updated.visualScale;
		this.spinSpeed = updated.spinSpeed;
		this.tiltMultiplier = updated.tiltMultiplier;
		this.primaryColor = updated.primaryColor;
		this.secondaryColor = updated.secondaryColor;
		this.minAlpha = updated.minAlpha;
		this.maxAlpha = updated.maxAlpha;
		this.beamEnabled = updated.beamEnabled;
		this.beamInnerRadius = updated.beamInnerRadius;
		this.beamOuterRadius = updated.beamOuterRadius;
		this.beamColor = updated.beamColor;
		this.predictionEnabled = updated.predictionEnabled;
		this.predictionLeadTicks = updated.predictionLeadTicks;
		this.predictionMaxDistance = updated.predictionMaxDistance;
		this.predictionLookAhead = updated.predictionLookAhead;
		this.predictionVerticalBoost = updated.predictionVerticalBoost;
		this.meshLayers.clear();
		updated.meshLayers.forEach((id, layer) -> this.meshLayers.put(id, layer.copy(id)));
		this.layerStyles.clear();
		this.layerStyles.putAll(updated.layerStyles);
		this.layerShapes.clear();
		this.layerShapes.putAll(updated.layerShapes);
		this.layerTriangles.clear();
		this.layerTriangles.putAll(updated.layerTriangles);
		ensureLayers();
	}

	public static ShieldProfileConfig fromJsonElement(@Nullable JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return defaults();
		}
		if (element.isJsonObject()) {
			return fromJson(element.getAsJsonObject());
		}
		throw new IllegalArgumentException("Expected JSON object for shield profile");
	}

	public static ShieldProfileConfig fromNbtCompound(@Nullable NbtCompound nbt) {
		return fromNbt(nbt);
	}

	private ShieldMeshLayerConfig getPrimaryLayer() {
		ensureLayers();
		return meshLayers.entrySet().iterator().next().getValue();
	}

	private void ensureLayers() {
		if (meshLayers.isEmpty()) {
			ShieldMeshLayerConfig layer = ShieldMeshLayerConfig.createDefault("shell");
			layer.setColors(primaryColor, secondaryColor);
			layer.setAlphaRange(minAlpha, maxAlpha);
			meshLayers.put("shell", layer);
			layerStyles.put("shell", null);
			layerShapes.put("shell", null);
			layerTriangles.put("shell", "default");
		}
	}

	private boolean applyLayerStyle(String layerId, String styleName) {
		String sanitized = ShieldMeshStyleStore.sanitizeName(styleName);
		return ShieldMeshStyleStore.getStyle(sanitized).map(style -> {
			ShieldMeshLayerConfig config = style.copy(layerId);
			meshLayers.put(layerId, config);
			layerStyles.put(layerId, sanitized);
			return true;
		}).orElse(false);
	}

	private boolean applyLayerShape(String layerId, String shapeName) {
		String sanitized = ShieldMeshShapeStore.sanitizeName(shapeName);
		if (sanitized.isEmpty()) {
			layerShapes.put(layerId, null);
			return true;
		}
		return ShieldMeshShapeStore.getShape(sanitized).map(shape -> {
			ShieldMeshLayerConfig layer = meshLayers.get(layerId);
			if (layer == null) {
				return false;
			}
			shape.applyTo(layer);
			layerShapes.put(layerId, sanitized);
			return true;
		}).orElse(false);
	}

	private boolean applyLayerTriangle(String layerId, String triangleName) {
		String sanitized = ShieldTriangleTypeStore.sanitize(triangleName);
		if (sanitized.isEmpty()) {
			layerTriangles.put(layerId, "default");
			return true;
		}
		return ShieldTriangleTypeStore.get(sanitized).map(config -> {
			layerTriangles.put(layerId, sanitized);
			return true;
		}).orElse(false);
	}

	private static boolean parseBoolean(String value, boolean fallback) {
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
			return Boolean.parseBoolean(value);
		}
		return fallback;
	}

	private static float parseFloat(String value, float fallback) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	public static int parseColor(String value) {
		Integer parsed = ColorConfig.parseUserColor(value);
		if (parsed == null) {
			throw new IllegalArgumentException("Invalid color value: " + value);
		}
		return parsed;
	}

	public static String formatColor(int argb) {
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	private static JsonObject getObject(JsonObject parent, String key) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static float readFloat(JsonObject parent, String key, float fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsFloat() : fallback;
	}

	private static int readInt(JsonObject parent, String key, int fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
	}

	private static boolean readBoolean(JsonObject parent, String key, boolean fallback) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
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

	private static int readNbtInt(NbtCompound nbt, String key, int fallback) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			if (element instanceof AbstractNbtNumber number) {
				return number.intValue();
			}
		}
		return fallback;
	}

	private static boolean readNbtBoolean(NbtCompound nbt, String key, boolean fallback) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			if (element instanceof AbstractNbtNumber number) {
				return number.longValue() != 0L;
			}
		}
		return fallback;
	}

	private static String readNbtString(NbtCompound nbt, String key) {
		if (nbt.contains(key)) {
			NbtElement element = nbt.get(key);
			return element == null ? "" : element.asString().orElse("");
		}
		return "";
	}

	private static String sanitizeLayerId(String id) {
		String trimmed = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	private static void applyLayerOverrides(ShieldMeshLayerConfig layer, JsonObject json) {
		if (json.has("mesh_type")) {
			layer.setValue("mesh_type", json.get("mesh_type").getAsString());
		}
		if (json.has("lat_steps")) {
			layer.setLatSteps(readInt(json, "lat_steps", layer.latSteps()));
		}
		if (json.has("lon_steps")) {
			layer.setLonSteps(readInt(json, "lon_steps", layer.lonSteps()));
		}
		if (json.has("lat_start") || json.has("lat_end")) {
			float start = json.has("lat_start") ? json.get("lat_start").getAsFloat() : layer.latStart();
			float end = json.has("lat_end") ? json.get("lat_end").getAsFloat() : layer.latEnd();
			layer.setLatRange(start, end);
		}
		if (json.has("lon_start") || json.has("lon_end")) {
			float start = json.has("lon_start") ? json.get("lon_start").getAsFloat() : layer.lonStart();
			float end = json.has("lon_end") ? json.get("lon_end").getAsFloat() : layer.lonEnd();
			layer.setLonRange(start, end);
		}
		if (json.has("radius_multiplier")) {
			layer.setRadiusMultiplier(readFloat(json, "radius_multiplier", layer.radiusMultiplier()));
		}
		if (json.has("swirl_strength")) {
			layer.setSwirlStrength(readFloat(json, "swirl_strength", layer.swirlStrength()));
		}
		if (json.has("phase_offset")) {
			layer.setPhaseOffset(readFloat(json, "phase_offset", layer.phaseOffset()));
		}
		JsonObject alpha = getObject(json, "alpha");
		if (alpha != null) {
			layer.setAlphaRange(
					readFloat(alpha, "min", layer.alphaMin()),
					readFloat(alpha, "max", layer.alphaMax()));
		}
		JsonObject colors = getObject(json, "colors");
		if (colors != null) {
			int primary = colors.has("primary") ? parseColor(colors.get("primary").getAsString()) : layer.primaryColor();
			int secondary = colors.has("secondary") ? parseColor(colors.get("secondary").getAsString()) : layer.secondaryColor();
			layer.setColors(primary, secondary);
		}
		if (json.has("band_count")) {
			layer.setBandCount(readInt(json, "band_count", layer.bandCount()));
		}
		if (json.has("band_thickness")) {
			layer.setBandThickness(readFloat(json, "band_thickness", layer.bandThickness()));
		}
		if (json.has("wire_thickness")) {
			layer.setWireThickness(readFloat(json, "wire_thickness", layer.wireThickness()));
		}
	}
}


