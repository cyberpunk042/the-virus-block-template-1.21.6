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

/**
 * Loads mesh shape configs that define how mesh layers mask their tessellation.
 */
public final class ShieldMeshShapeStore {
	private static final Path SHAPE_DIR = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("meshshapes");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, ShieldMeshShapeConfig> SHAPES = new LinkedHashMap<>();

	static {
		registerBuiltin("sphere_plain", "shield_shapes/sphere_plain.json");
		registerBuiltin("sphere_line", "shield_shapes/sphere_line.json");
		registerBuiltin("sphere_square", "shield_shapes/sphere_square.json");
		registerBuiltin("sphere_arrow", "shield_shapes/sphere_arrow.json");
		registerBuiltin("sphere_checker", "shield_shapes/sphere_checker.json");
		registerBuiltin("sphere_wireframe", "shield_shapes/sphere_wireframe.json");
		registerBuiltin("sphere_bands", "shield_shapes/sphere_bands.json");
		loadOverrides();
	}

	private ShieldMeshShapeStore() {
	}

	private static void registerBuiltin(String name, String resource) {
		loadFromResource(resource).ifPresent(config -> SHAPES.put(sanitizeName(name), config));
	}

	private static Optional<ShieldMeshShapeConfig> loadFromResource(String path) {
		String fullPath = "/assets/the-virus-block/" + path;
		try (InputStream stream = ShieldMeshShapeStore.class.getResourceAsStream(fullPath)) {
			if (stream == null) {
				return Optional.empty();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return Optional.of(ShieldMeshShapeConfig.fromJson(path, json));
			}
		} catch (IOException | IllegalStateException ex) {
			return Optional.empty();
		}
	}

	private static void loadOverrides() {
		if (!Files.isDirectory(SHAPE_DIR)) {
			return;
		}
		try {
			Files.list(SHAPE_DIR)
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.forEach(ShieldMeshShapeStore::loadOverride);
		} catch (IOException ignored) {
		}
	}

	private static void loadOverride(Path file) {
		String name = file.getFileName().toString();
		String sanitized = sanitizeName(name.substring(0, name.length() - ".json".length()));
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			SHAPES.put(sanitized, ShieldMeshShapeConfig.fromJson(sanitized, json));
		} catch (IOException | IllegalStateException ignored) {
		}
	}

	public static List<String> listShapes() {
		List<String> names = new ArrayList<>(SHAPES.keySet());
		Collections.sort(names);
		return Collections.unmodifiableList(names);
	}

	public static Optional<ShieldMeshShapeConfig> getShape(String name) {
		String sanitized = sanitizeName(name);
		ShieldMeshShapeConfig config = SHAPES.get(sanitized);
		return Optional.ofNullable(config).map(original -> original.copy(sanitized));
	}

	public static Optional<ShieldMeshShapeConfig> getEditable(String name) {
		return Optional.ofNullable(SHAPES.get(sanitizeName(name)));
	}

	public static boolean save(String name) {
		String sanitized = sanitizeName(name);
		ShieldMeshShapeConfig config = SHAPES.get(sanitized);
		if (config == null) {
			return false;
		}
		Path file = SHAPE_DIR.resolve(sanitized + ".json");
		try {
			Files.createDirectories(SHAPE_DIR);
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config.toJson(), writer);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	public static boolean saveAs(String name, ShieldMeshShapeConfig config) {
		String sanitized = sanitizeName(name);
		if (sanitized.isEmpty()) {
			return false;
		}
		SHAPES.put(sanitized, config);
		return save(sanitized);
	}

	public static void updateShape(String name, ShieldMeshShapeConfig config) {
		SHAPES.put(sanitizeName(name), config);
	}

	public static String sanitizeName(String name) {
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


