package net.cyberpunk042.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

final class SingularityVisualStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_FILE = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("singularity")
			.resolve("visual.json");

	private SingularityVisualStore() {
	}

	static SingularityVisualConfig load() {
		SingularityVisualConfig config = loadFromResource().orElseGet(SingularityVisualConfig::defaults);
		if (Files.exists(CONFIG_FILE)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
				JsonObject override = JsonParser.parseReader(reader).getAsJsonObject();
				return SingularityVisualConfig.fromJson(override);
			} catch (IOException | IllegalStateException ignored) {
			}
		}
		return config;
	}

	static boolean save(SingularityVisualConfig config) {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
				GSON.toJson(config.toJson(), writer);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	private static java.util.Optional<SingularityVisualConfig> loadFromResource() {
		String path = "/assets/the-virus-block/singularity/visual.json";
		try (InputStream stream = SingularityVisualManager.class.getResourceAsStream(path)) {
			if (stream == null) {
				return java.util.Optional.empty();
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return java.util.Optional.of(SingularityVisualConfig.fromJson(json));
			}
		} catch (IOException | IllegalStateException ex) {
			return java.util.Optional.empty();
		}
	}
}

