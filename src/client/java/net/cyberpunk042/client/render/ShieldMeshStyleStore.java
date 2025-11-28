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
 * Loads and manages reusable mesh style definitions that layers can reference.
 */
public final class ShieldMeshStyleStore {
	private static final Path STYLE_DIR = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("meshstyles");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final Map<String, ShieldMeshLayerConfig> STYLES = new LinkedHashMap<>();

	static {
		registerBuiltin("sphere_plain", "shield_meshes/sphere_plain.json");
		registerBuiltin("sphere_line", "shield_meshes/sphere_line.json");
		registerBuiltin("sphere_triangle", "shield_meshes/sphere_triangle.json");
		registerBuiltin("sphere_square", "shield_meshes/sphere_square.json");
		registerBuiltin("sphere_wireframe", "shield_meshes/sphere_wireframe.json");
		registerBuiltin("sphere_rings", "shield_meshes/sphere_rings.json");
		registerBuiltin("sphere_spiral", "shield_meshes/sphere_spiral.json");
		registerBuiltin("sphere_core", "shield_meshes/sphere_core.json");
		loadOverrides();
	}

	private ShieldMeshStyleStore() {
	}

	private static void registerBuiltin(String name, String resourcePath) {
		loadFromResource(resourcePath).ifPresent(config -> STYLES.put(sanitizeName(name), config));
	}

	private static Optional<ShieldMeshLayerConfig> loadFromResource(String resourcePath) {
		String fullPath = "/assets/the-virus-block/" + resourcePath;
		try (InputStream stream = ShieldMeshStyleStore.class.getResourceAsStream(fullPath)) {
			if (stream == null) {
				return Optional.empty();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return Optional.of(ShieldMeshLayerConfig.fromJson(resourcePath, json));
			}
		} catch (IOException | IllegalStateException ex) {
			return Optional.empty();
		}
	}

	private static void loadOverrides() {
		if (!Files.isDirectory(STYLE_DIR)) {
			return;
		}
		try {
			Files.list(STYLE_DIR)
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.forEach(path -> loadOverride(path));
		} catch (IOException ignored) {
		}
	}

	private static void loadOverride(Path file) {
		String name = file.getFileName().toString();
		String sanitized = sanitizeName(name.substring(0, name.length() - ".json".length()));
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			ShieldMeshLayerConfig config = ShieldMeshLayerConfig.fromJson(sanitized, json);
			STYLES.put(sanitized, config);
		} catch (IOException | IllegalStateException ignored) {
		}
	}

	public static List<String> listStyles() {
		return new ArrayList<>(STYLES.keySet());
	}

	public static Optional<ShieldMeshLayerConfig> getStyle(String name) {
		String sanitized = sanitizeName(name);
		ShieldMeshLayerConfig config = STYLES.get(sanitized);
		return Optional.ofNullable(config).map(original -> original.copy(sanitized));
	}

	public static Optional<ShieldMeshLayerConfig> getEditable(String name) {
		return Optional.ofNullable(STYLES.get(sanitizeName(name)));
	}

	public static boolean save(String name) {
		ShieldMeshLayerConfig config = STYLES.get(sanitizeName(name));
		if (config == null) {
			return false;
		}
		Path file = STYLE_DIR.resolve(sanitizeName(name) + ".json");
		try {
			Files.createDirectories(STYLE_DIR);
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config.toJson(), writer);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	public static boolean saveAs(String name, ShieldMeshLayerConfig config) {
		String sanitized = sanitizeName(name);
		if (sanitized.isEmpty()) {
			return false;
		}
		STYLES.put(sanitized, config);
		return save(sanitized);
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

	public static Map<String, ShieldMeshLayerConfig> styles() {
		return Collections.unmodifiableMap(STYLES);
	}
}


