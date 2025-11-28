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

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

final class PersonalShieldProfileStore {
	private static final Path PROFILE_DIR = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("personal-forcefields");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, ShieldProfileConfig> BUILTIN_PROFILES = new LinkedHashMap<>();
	private static final List<String> BUILTIN_ORDER = new ArrayList<>();

	static {
		registerBuiltin("personal-default", "shield_profiles/personal_default.json");
		registerBuiltin("striped", "shield_profiles/personal/striped.json");
		registerBuiltin("meshed", "shield_profiles/personal/meshed.json");
		registerBuiltin("rings", "shield_profiles/personal/rings.json");
		registerBuiltin("fraction-8", "shield_profiles/personal/fraction-8.json");
		registerBuiltin("fraction-16", "shield_profiles/personal/fraction-16.json");
		if (BUILTIN_ORDER.isEmpty()) {
			registerBuiltin("default", null);
		}
	}

	private PersonalShieldProfileStore() {
	}

	private static void registerBuiltin(String name, @Nullable String resourcePath) {
		String key = sanitizeName(name);
		if (key.isEmpty() || BUILTIN_PROFILES.containsKey(key)) {
			return;
		}
		ShieldProfileConfig config = loadFromResource(resourcePath).orElseGet(ShieldProfileConfig::defaults);
		BUILTIN_ORDER.add(key);
		BUILTIN_PROFILES.put(key, config);
	}

	private static Optional<ShieldProfileConfig> loadFromResource(@Nullable String resourcePath) {
		if (resourcePath == null) {
			return Optional.empty();
		}
		String fullPath = "/assets/the-virus-block/" + resourcePath;
		try (InputStream stream = PersonalShieldProfileStore.class.getResourceAsStream(fullPath)) {
			if (stream == null) {
				return Optional.empty();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return Optional.of(ShieldProfileConfig.fromJson(json));
			}
		} catch (IOException | IllegalStateException ex) {
			return Optional.empty();
		}
	}

	static Map<String, ShieldProfileConfig> getBuiltinProfiles() {
		return Collections.unmodifiableMap(BUILTIN_PROFILES);
	}

	static List<String> getBuiltinOrder() {
		return Collections.unmodifiableList(BUILTIN_ORDER);
	}

	static List<String> listProfiles() {
		if (!Files.isDirectory(PROFILE_DIR)) {
			return Collections.emptyList();
		}
		List<String> names = new ArrayList<>();
		try {
			Files.list(PROFILE_DIR)
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.forEach(path -> {
						String name = path.getFileName().toString();
						names.add(name.substring(0, name.length() - ".json".length()));
					});
		} catch (IOException ignored) {
		}
		Collections.sort(names);
		return names;
	}

	static boolean save(String name, ShieldProfileConfig config) {
		String sanitized = sanitizeName(name);
		if (sanitized.isEmpty()) {
			return false;
		}
		Path file = PROFILE_DIR.resolve(sanitized + ".json");
		try {
			Files.createDirectories(PROFILE_DIR);
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config.toJson(), writer);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	static Optional<ShieldProfileConfig> load(String name) {
		String sanitized = sanitizeName(name);
		if (sanitized.isEmpty()) {
			return Optional.empty();
		}
		Path file = PROFILE_DIR.resolve(sanitized + ".json");
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			return Optional.of(ShieldProfileConfig.fromJson(json));
		} catch (IOException | IllegalStateException ex) {
			return Optional.empty();
		}
	}

	static String sanitizeName(String name) {
		String trimmed = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
				builder.append(c);
			} else if (Character.isWhitespace(c)) {
				builder.append('_');
			}
		}
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_') {
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.toString();
	}
}

