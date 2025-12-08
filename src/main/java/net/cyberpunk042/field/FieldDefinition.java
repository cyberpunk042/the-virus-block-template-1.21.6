package net.cyberpunk042.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cyberpunk042.field.instance.FieldEffect;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable definition of a field's visual and behavioral configuration.
 * 
 * <h2>Core Properties</h2>
 * <ul>
 *   <li><b>id</b>: Unique identifier for this definition</li>
 *   <li><b>type</b>: Field type (SHIELD, PERSONAL, etc.)</li>
 *   <li><b>baseRadius</b>: Base radius before modifiers</li>
 *   <li><b>themeId</b>: Color theme reference</li>
 * </ul>
 * 
 * <h2>Visual Properties</h2>
 * <ul>
 *   <li><b>layers</b>: Visual layers with primitives</li>
 *   <li><b>modifiers</b>: Visual/behavior modifiers</li>
 * </ul>
 * 
 * <h2>Behavioral Properties</h2>
 * <ul>
 *   <li><b>effects</b>: Effects to apply (damage, heal, knockback)</li>
 *   <li><b>prediction</b>: Client prediction config</li>
 * </ul>
 * 
 * @see FieldLayer
 * @see Modifiers
 * @see FieldEffect
 */
public record FieldDefinition(
        Identifier id,
        FieldType type,
        float baseRadius,
        String themeId,
        List<FieldLayer> layers,
        Modifiers modifiers,
        List<FieldEffect> effects,
        PredictionConfig prediction,
        BeamConfig beam
) {
    
    /**
     * Compact constructor with validation.
     */
    public FieldDefinition {
        layers = layers != null ? List.copyOf(layers) : List.of();
        effects = effects != null ? List.copyOf(effects) : List.of();
        modifiers = modifiers != null ? modifiers : Modifiers.DEFAULT;
        beam = beam != null ? beam : BeamConfig.DISABLED;
        if (baseRadius <= 0) baseRadius = 1.0f;
    }
    
    /**
     * Checks if beam rendering is enabled.
     */
    public boolean hasBeam() {
        return beam != null && beam.enabled();
    }
    
    // =========================================================================
    // Computed Properties
    // =========================================================================
    
    /**
     * Gets the effective radius after modifiers.
     */
    public float effectiveRadius() {
        return modifiers.applyRadius(baseRadius);
    }
    
    /**
     * Gets the effective color theme.
     */
    public ColorTheme effectiveTheme() {
        ColorTheme theme = ColorThemeRegistry.get(themeId);
        return theme != null ? theme : ColorTheme.CYBER_GREEN;
    }
    
    /**
     * Checks if this field has any effects configured.
     */
    public boolean hasEffects() {
        return !effects.isEmpty();
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder(Identifier id, FieldType type) {
        return new Builder(id, type);
    }
    
    public static Builder builder(String id, FieldType type) {
        return new Builder(Identifier.of("the-virus-block", id), type);
    }
    
    public static class Builder {
        private final Identifier id;
        private final FieldType type;
        private float baseRadius = 1.0f;
        private String themeId = "cyber_green";
        private final List<FieldLayer> layers = new ArrayList<>();
        private Modifiers modifiers = Modifiers.DEFAULT;
        private final List<FieldEffect> effects = new ArrayList<>();
        private PredictionConfig prediction = null;
        private BeamConfig beam = BeamConfig.DISABLED;
        
        private Builder(Identifier id, FieldType type) {
            this.id = id;
            this.type = type;
        }
        
        public Builder baseRadius(float radius) {
            this.baseRadius = radius;
            return this;
        }
        
        public Builder theme(String themeId) {
            this.themeId = themeId;
            return this;
        }
        
        public Builder layer(FieldLayer layer) {
            this.layers.add(layer);
            return this;
        }
        
        public Builder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }
        
        public Builder effect(FieldEffect effect) {
            this.effects.add(effect);
            return this;
        }
        
        public Builder prediction(PredictionConfig config) {
            this.prediction = config;
            return this;
        }
        
        public Builder beam(BeamConfig beam) {
            this.beam = beam;
            return this;
        }
        
        public Builder beamEnabled(float innerRadius, float outerRadius, int color) {
            this.beam = BeamConfig.custom(innerRadius, outerRadius, color);
            return this;
        }
        
        public FieldDefinition build() {
            if (layers.isEmpty()) {
                layers.add(FieldLayer.sphere("default", baseRadius, 32));
            }
            
            Logging.REGISTRY.topic("field").debug(
                "Built field definition: {} (type={}, radius={:.1f}, layers={}, effects={}, beam={})",
                id, type.id(), baseRadius, layers.size(), effects.size(), beam.enabled());
            
            return new FieldDefinition(id, type, baseRadius, themeId, layers, 
                                       modifiers, effects, prediction, beam);
        }
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    public static FieldDefinition fromJson(JsonObject json, Identifier id) {
        try {
            FieldType type = FieldType.fromId(
                json.has("type") ? json.get("type").getAsString() : "shield");
            
            float baseRadius = json.has("baseRadius") 
                ? json.get("baseRadius").getAsFloat() 
                : json.has("radius") ? json.get("radius").getAsFloat() : 1.0f;
            
            String themeId = json.has("theme") ? json.get("theme").getAsString() : "cyber_green";
            
            // Parse layers
            List<FieldLayer> layers = new ArrayList<>();
            if (json.has("layers")) {
                for (JsonElement elem : json.getAsJsonArray("layers")) {
                    layers.add(FieldLayer.fromJson(elem.getAsJsonObject()));
                }
            }
            
            // Parse modifiers
            Modifiers modifiers = json.has("modifiers") 
                ? Modifiers.fromJson(json.getAsJsonObject("modifiers"))
                : Modifiers.DEFAULT;
            
            // Parse effects
            List<FieldEffect> effects = new ArrayList<>();
            if (json.has("effects")) {
                for (JsonElement elem : json.getAsJsonArray("effects")) {
                    effects.add(FieldEffect.fromJson(elem.getAsJsonObject()));
                }
            }
            
            // Parse prediction
            PredictionConfig prediction = null;
            if (json.has("prediction")) {
                prediction = PredictionConfig.fromJson(json.getAsJsonObject("prediction"));
            }
            
            // Parse beam config
            BeamConfig beam = json.has("beam") 
                ? BeamConfig.fromJson(json.getAsJsonObject("beam"))
                : BeamConfig.DISABLED;
            
            if (layers.isEmpty()) {
                layers.add(FieldLayer.sphere("default", baseRadius, 32));
            }
            
            Logging.REGISTRY.topic("field").debug(
                "Parsed field definition: {} (type={}, radius={:.1f}, layers={}, effects={}, beam={})", 
                id, type.id(), baseRadius, layers.size(), effects.size(), beam.enabled());
            
            return new FieldDefinition(id, type, baseRadius, themeId, layers, 
                                       modifiers, effects, prediction, beam);
        } catch (Exception e) {
            Logging.REGISTRY.topic("field").error(
                "Failed to parse field definition {}: {}", id, e.getMessage());
            // Return a minimal valid definition
            return new FieldDefinition(id, FieldType.SHIELD, 1.0f, "cyber_green", 
                List.of(FieldLayer.sphere("default", 1.0f, 32)), 
                Modifiers.DEFAULT, List.of(), null, BeamConfig.DISABLED);
        }
    }
    
    /**
     * Serializes this definition to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.id());
        json.addProperty("baseRadius", baseRadius);
        json.addProperty("theme", themeId);
        
        // Layers
        JsonArray layersArray = new JsonArray();
        for (FieldLayer layer : layers) {
            layersArray.add(layer.toJson());
        }
        json.add("layers", layersArray);
        
        // Modifiers (only if not default)
        if (!modifiers.equals(Modifiers.DEFAULT)) {
            json.add("modifiers", modifiers.toJson());
        }
        
        // Effects
        if (!effects.isEmpty()) {
            JsonArray effectsArray = new JsonArray();
            for (FieldEffect effect : effects) {
                effectsArray.add(effect.toJson());
            }
            json.add("effects", effectsArray);
        }
        
        // Prediction
        if (prediction != null) {
            json.add("prediction", prediction.toJson());
        }
        
        // Beam
        if (beam != null && beam.enabled()) {
            json.add("beam", beam.toJson());
        }
        
        return json;
    }
}
