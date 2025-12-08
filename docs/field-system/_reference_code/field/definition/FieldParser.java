package net.cyberpunk042.field.definition;

import com.google.gson.*;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Parses field definitions from JSON files.
 * 
 * <h2>File Format</h2>
 * <pre>
 * {
 *   "type": "shield",
 *   "baseRadius": 3.0,
 *   "theme": "cyber_blue",
 *   "layers": [...],
 *   "modifiers": {...},
 *   "effects": [...]
 * }
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Parse from file
 * FieldDefinition def = FieldParser.parseFile(path);
 * 
 * // Parse from string
 * FieldDefinition def = FieldParser.parseString(json, id);
 * 
 * // Parse from resource
 * FieldDefinition def = FieldParser.parseResource("fields/my_field.json");
 * </pre>
 */
public final class FieldParser {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    
    private FieldParser() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Parse Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Parses a field definition from a JSON string.
     * 
     * @param json the JSON string
     * @param id the identifier to assign
     * @return parsed definition, or null on error
     */
    public static FieldDefinition parseString(String json, Identifier id) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            FieldDefinition def = FieldDefinition.fromJson(obj, id);
            
            Logging.REGISTRY.topic("field-parser").debug(
                "Parsed field from string: {}", id);
            
            return def;
        } catch (JsonSyntaxException e) {
            Logging.REGISTRY.topic("field-parser").error(
                "Invalid JSON for field {}: {}", id, e.getMessage());
            return null;
        } catch (Exception e) {
            Logging.REGISTRY.topic("field-parser").error(
                "Failed to parse field {}: {}", id, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a field definition from a file.
     * 
     * @param path the file path
     * @return parsed definition, or null on error
     */
    public static FieldDefinition parseFile(Path path) {
        try {
            String filename = path.getFileName().toString();
            String id = filename.endsWith(".json") 
                ? filename.substring(0, filename.length() - 5)
                : filename;
            
            Identifier identifier = Identifier.of("the-virus-block", id);
            String json = Files.readString(path, StandardCharsets.UTF_8);
            
            Logging.REGISTRY.topic("field-parser").debug(
                "Reading field file: {}", path);
            
            return parseString(json, identifier);
        } catch (IOException e) {
            Logging.REGISTRY.topic("field-parser").error(
                "Failed to read field file {}: {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a field definition from a classpath resource.
     * 
     * @param resourcePath path relative to assets/the-virus-block/
     * @return parsed definition, or null on error
     */
    public static FieldDefinition parseResource(String resourcePath) {
        String fullPath = "/assets/the-virus-block/" + resourcePath;
        
        try (InputStream stream = FieldParser.class.getResourceAsStream(fullPath)) {
            if (stream == null) {
                Logging.REGISTRY.topic("field-parser").warn(
                    "Resource not found: {}", fullPath);
                return null;
            }
            
            String filename = resourcePath.contains("/") 
                ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                : resourcePath;
            String id = filename.endsWith(".json")
                ? filename.substring(0, filename.length() - 5)
                : filename;
            
            Identifier identifier = Identifier.of("the-virus-block", id);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            
            Logging.REGISTRY.topic("field-parser").debug(
                "Reading field resource: {}", resourcePath);
            
            return parseString(json, identifier);
        } catch (IOException e) {
            Logging.REGISTRY.topic("field-parser").error(
                "Failed to read field resource {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Batch Parse
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Parses all field definitions from a directory.
     * 
     * @param directory the directory to scan
     * @return list of parsed definitions (skips failures)
     */
    public static List<FieldDefinition> parseDirectory(Path directory) {
        List<FieldDefinition> results = new ArrayList<>();
        
        if (!Files.isDirectory(directory)) {
            Logging.REGISTRY.topic("field-parser").warn(
                "Not a directory: {}", directory);
            return results;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path path : stream) {
                FieldDefinition def = parseFile(path);
                if (def != null) {
                    results.add(def);
                }
            }
        } catch (IOException e) {
            Logging.REGISTRY.topic("field-parser").error(
                "Failed to read directory {}: {}", directory, e.getMessage());
        }
        
        Logging.REGISTRY.topic("field-parser").info(
            "Parsed {} field definitions from {}", results.size(), directory);
        
        return results;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Serialization
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Serializes a field definition to JSON string.
     */
    public static String toJsonString(FieldDefinition definition) {
        return GSON.toJson(definition.toJson());
    }
    
    /**
     * Writes a field definition to a file.
     */
    public static void writeToFile(FieldDefinition definition, Path path) throws IOException {
        String json = toJsonString(definition);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        
        Logging.REGISTRY.topic("field-parser").debug(
            "Wrote field definition to: {}", path);
    }
}
