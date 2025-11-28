package net.cyberpunk042.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class ShieldTriangleTypeStore {
	private static final Path TRIANGLE_DIR = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("triangletypes");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, ShieldTriangleTypeConfig> TYPES = new LinkedHashMap<>();

	static {
		registerBuiltin("default", "shield_triangles/triangle_default.json");
		registerBuiltin("square", "shield_triangles/triangle_square.json");
		registerBuiltin("line", "shield_triangles/triangle_line.json");
		registerBuiltin("arrow", "shield_triangles/triangle_arrow.json");
		registerBuiltin("parallelogram", "shield_triangles/parallelogram.json");
		registerBuiltin("filled_1", "shield_triangles/filled_1.json");
		registerBuiltin("meshed_1", "shield_triangles/meshed_1.json");
		registerBuiltin("meshed_2", "shield_triangles/meshed_2.json");
		registerBuiltin("meshed_3", "shield_triangles/meshed_3.json");
		registerBuiltin("meshed_4", "shield_triangles/meshed_4.json");
		registerBuiltin("meshed_5", "shield_triangles/meshed_5.json");
		registerBuiltin("meshed_6", "shield_triangles/meshed_6.json");
		registerBuiltin("meshed_7", "shield_triangles/meshed_7.json");
		registerBuiltin("parallelogram2", "shield_triangles/parallelogram_2.json");
		registerBuiltin("triangle_hole", "shield_triangles/triangle_hole.json");
		registerBuiltin("triangle_facing", "shield_triangles/triangle_facing.json");
		registerBuiltin("triangle_facing_spaced", "shield_triangles/triangle_facing_spaced.json");
		registerBuiltin("parallelogram2", "shield_triangles/parallelogram_2.json");
		for (int i = 1; i <= 36; i++) {
			registerBuiltin("triangle_other_type_" + i, "shield_triangles/triangle_other_type_" + i + ".json");
		}
		for (int i = 1; i <= 6; i++) {
			registerBuiltin("triangle_single_type_a_" + i, "shield_triangles/triangle_single_type_a_" + i + ".json");
			registerBuiltin("triangle_single_type_b_" + i, "shield_triangles/triangle_single_type_b_" + i + ".json");
		}
		for (int i = 1; i <= 6; i++) {
			registerBuiltin("triangle_triple_type_" + i, "shield_triangles/triangle_triple_type_" + i + ".json");
		}
		loadOverrides();
	}

	private ShieldTriangleTypeStore() {
	}

	private static void registerBuiltin(String name, String resourcePath) {
		loadFromResource(resourcePath).ifPresent(config -> TYPES.put(sanitize(name), config));
	}

	private static Optional<ShieldTriangleTypeConfig> loadFromResource(String path) {
		String fullPath = "/assets/the-virus-block/" + path;
		try (InputStream stream = ShieldTriangleTypeStore.class.getResourceAsStream(fullPath)) {
			if (stream == null) {
				return Optional.empty();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return Optional.of(ShieldTriangleTypeConfig.fromJson(path, json));
			}
		} catch (IOException | IllegalStateException ex) {
			return Optional.empty();
		}
	}

	private static void loadOverrides() {
		if (!Files.isDirectory(TRIANGLE_DIR)) {
			return;
		}
		try {
			Files.list(TRIANGLE_DIR)
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.forEach(ShieldTriangleTypeStore::loadOverride);
		} catch (IOException ignored) {
		}
	}

	private static void loadOverride(Path file) {
		String name = file.getFileName().toString();
		String sanitized = sanitize(name.substring(0, name.length() - ".json".length()));
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			TYPES.put(sanitized, ShieldTriangleTypeConfig.fromJson(sanitized, json));
		} catch (IOException | IllegalStateException ignored) {
		}
	}

	public static List<String> list() {
		List<String> names = new ArrayList<>(TYPES.keySet());
		Collections.sort(names);
		return Collections.unmodifiableList(names);
	}

	public static Optional<ShieldTriangleTypeConfig> get(String name) {
		String sanitized = sanitize(name);
		ShieldTriangleTypeConfig config = TYPES.get(sanitized);
		return Optional.ofNullable(config).map(original -> original.copy(sanitized));
	}

	public static Optional<ShieldTriangleTypeConfig> getEditable(String name) {
		return Optional.ofNullable(TYPES.get(sanitize(name)));
	}

	public static boolean save(String name) {
		String sanitized = sanitize(name);
		ShieldTriangleTypeConfig config = TYPES.get(sanitized);
		if (config == null) {
			return false;
		}
		Path file = TRIANGLE_DIR.resolve(sanitized + ".json");
		try {
			Files.createDirectories(TRIANGLE_DIR);
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config.toJson(), writer);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	public static void update(String name, ShieldTriangleTypeConfig config) {
		TYPES.put(sanitize(name), config);
	}

	public static ShieldTriangleTypeConfig getOrDefault(String name) {
		ShieldTriangleTypeConfig fallback = TYPES.get("default");
		if (fallback == null) {
			fallback = ShieldTriangleTypeConfig.createDefault("default");
		}
		final ShieldTriangleTypeConfig fallbackFinal = fallback;
		return get(name).orElseGet(() -> fallbackFinal.copy(name == null || name.isEmpty() ? "default" : name));
	}

	public static String sanitize(String name) {
		String trimmed = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}


