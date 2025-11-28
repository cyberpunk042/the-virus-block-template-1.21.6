package net.cyberpunk042.client.render;

import java.util.Locale;

import com.google.gson.JsonObject;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;

/**
 * Defines the logical fill mask for a mesh layer (bands, checker, etc.).
 */
public final class ShieldMeshShapeConfig {
	private ShieldMeshLayerConfig.MeshType type;
	private int bandCount;
	private float bandThickness;
	private float wireThickness;

	private ShieldMeshShapeConfig(String id) {
		this.type = ShieldMeshLayerConfig.MeshType.SOLID;
		this.bandCount = 0;
		this.bandThickness = 0.2F;
		this.wireThickness = 0.05F;
	}

	public static ShieldMeshShapeConfig createDefault(String id) {
		return new ShieldMeshShapeConfig(id);
	}

	public ShieldMeshShapeConfig copy(String newId) {
		ShieldMeshShapeConfig copy = new ShieldMeshShapeConfig(newId);
		copy.type = type;
		copy.bandCount = bandCount;
		copy.bandThickness = bandThickness;
		copy.wireThickness = wireThickness;
		return copy;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", type.name().toLowerCase(Locale.ROOT));
		json.addProperty("band_count", bandCount);
		json.addProperty("band_thickness", bandThickness);
		json.addProperty("wire_thickness", wireThickness);
		return json;
	}

	public static ShieldMeshShapeConfig fromJson(String id, JsonObject json) {
		ShieldMeshShapeConfig config = new ShieldMeshShapeConfig(id);
		if (json == null) {
			return config;
		}
		if (json.has("type") && json.get("type").isJsonPrimitive()) {
			config.type = parseType(json.get("type").getAsString());
		}
		if (json.has("band_count")) {
			config.bandCount = json.get("band_count").getAsInt();
		}
		if (json.has("band_thickness")) {
			config.bandThickness = clamp01(json.get("band_thickness").getAsFloat());
		}
		if (json.has("wire_thickness")) {
			config.wireThickness = clamp01(json.get("wire_thickness").getAsFloat());
		}
		return config;
	}

	public void writeNbt(NbtCompound nbt) {
		nbt.putString("type", type.name());
		nbt.putInt("bandCount", bandCount);
		nbt.putFloat("bandThickness", bandThickness);
		nbt.putFloat("wireThickness", wireThickness);
	}

	public void applyTo(ShieldMeshLayerConfig layer) {
		layer.setMeshType(type);
		layer.setBandCount(bandCount);
		layer.setBandThickness(bandThickness);
		layer.setWireThickness(wireThickness);
	}

	public void setValue(String key, String rawValue) {
		switch (key.toLowerCase(Locale.ROOT)) {
			case "type" -> type = parseType(rawValue);
			case "band_count" -> bandCount = Math.max(0, Integer.parseInt(rawValue));
			case "band_thickness" -> bandThickness = clamp01(Float.parseFloat(rawValue));
			case "wire_thickness" -> wireThickness = clamp01(Float.parseFloat(rawValue));
			default -> throw new IllegalArgumentException("Unknown shape key '" + key + "'");
		}
	}

	private static float clamp01(float value) {
		return MathHelper.clamp(value, 0.0F, 1.0F);
	}

	private static ShieldMeshLayerConfig.MeshType parseType(String raw) {
		if (raw == null || raw.isEmpty()) {
			return ShieldMeshLayerConfig.MeshType.SOLID;
		}
		try {
			return ShieldMeshLayerConfig.MeshType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return ShieldMeshLayerConfig.MeshType.SOLID;
		}
	}
}


