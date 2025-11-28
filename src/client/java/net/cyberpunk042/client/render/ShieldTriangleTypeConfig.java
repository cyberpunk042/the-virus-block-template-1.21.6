package net.cyberpunk042.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Describes how to build a quad from two triangles by naming the vertex order.
 */
public final class ShieldTriangleTypeConfig {
	private final List<Corner[]> triangles = new ArrayList<>();

	private ShieldTriangleTypeConfig() {
		Corner[] first = {Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT};
		Corner[] second = {Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT};
		triangles.add(first);
		triangles.add(second);
	}

	public static ShieldTriangleTypeConfig createDefault(String id) {
		return new ShieldTriangleTypeConfig();
	}

	public ShieldTriangleTypeConfig copy(String newId) {
		ShieldTriangleTypeConfig copy = new ShieldTriangleTypeConfig();
		copy.triangles.clear();
		for (Corner[] tri : triangles) {
			Corner[] cloned = new Corner[tri.length];
			System.arraycopy(tri, 0, cloned, 0, tri.length);
			copy.triangles.add(cloned);
		}
		return copy;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		JsonArray array = new JsonArray();
		for (Corner[] tri : triangles) {
			JsonArray triArray = new JsonArray();
			for (Corner corner : tri) {
				triArray.add(corner.name().toLowerCase(Locale.ROOT));
			}
			array.add(triArray);
		}
		json.add("triangles", array);
		return json;
	}

	public static ShieldTriangleTypeConfig fromJson(String id, JsonObject json) {
		ShieldTriangleTypeConfig config = new ShieldTriangleTypeConfig();
		if (json == null) {
			return config;
		}
		JsonArray array = json.getAsJsonArray("triangles");
		if (array != null && !array.isEmpty()) {
			config.triangles.clear();
			for (JsonElement element : array) {
				if (!element.isJsonArray()) {
					continue;
				}
				JsonArray triArray = element.getAsJsonArray();
				if (triArray.size() < 3) {
					continue;
				}
				Corner[] tri = new Corner[3];
				for (int i = 0; i < 3; i++) {
					String cornerName = triArray.get(i).getAsString();
					tri[i] = Corner.fromString(cornerName);
				}
				config.triangles.add(tri);
			}
		}
		if (config.triangles.isEmpty()) {
			return createDefault(id);
		}
		return config;
	}

	public List<Corner[]> triangles() {
		return triangles;
	}

	public void setValue(String key, String rawValue) {
		switch (key.toLowerCase(Locale.ROOT)) {
			case "triangle0" -> setTriangle(0, rawValue);
			case "triangle1" -> setTriangle(1, rawValue);
			default -> throw new IllegalArgumentException("Unknown triangle key '" + key + "'");
		}
	}

	private void setTriangle(int index, String rawValue) {
		String[] tokens = rawValue.split(",");
		if (tokens.length < 3) {
			throw new IllegalArgumentException("Triangle definition requires 3 corners");
		}
		Corner[] tri = new Corner[3];
		for (int i = 0; i < 3; i++) {
			tri[i] = Corner.fromString(tokens[i].trim());
		}
		while (triangles.size() <= index) {
			Corner[] fallback = triangles.isEmpty()
					? new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT}
					: triangles.get(triangles.size() - 1);
			Corner[] copy = new Corner[fallback.length];
			System.arraycopy(fallback, 0, copy, 0, fallback.length);
			triangles.add(copy);
		}
		triangles.set(index, tri);
	}

	public enum Corner {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT;

		static Corner fromString(String value) {
			if (value == null || value.isEmpty()) {
				return TOP_LEFT;
			}
			try {
				return Corner.valueOf(value.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ex) {
				return TOP_LEFT;
			}
		}
	}
}


