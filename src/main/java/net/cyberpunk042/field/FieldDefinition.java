package net.cyberpunk042.field;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.LifecycleConfig;
import net.cyberpunk042.field.influence.TriggerConfig;
import net.cyberpunk042.field.instance.FollowModeConfig;
import net.cyberpunk042.field.instance.PredictionConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Complete definition of a visual field.
 * 
 * <p>Per CLASS_DIAGRAM §1: FieldDefinition contains all configuration for a field.
 * 
 * <p>A field definition contains:
 * <ul>
 *   <li><b>id</b> - Unique identifier (e.g., "shield_default")</li>
 *   <li><b>type</b> - Field category (SHIELD, PERSONAL, etc.)</li>
 *   <li><b>baseRadius</b> - Base scale before modifiers</li>
 *   <li><b>themeId</b> - Color theme reference (e.g., "energy_blue")</li>
 *   <li><b>layers</b> - Ordered list of layers to render</li>
 *   <li><b>modifiers</b> - Visual modifiers (scale, tilt, swirl, etc.)</li>
 *   <li><b>prediction</b> - Movement prediction config (for personal fields)</li>
 *   <li><b>beam</b> - Central beam effect config</li>
 *   <li><b>followMode</b> - How personal fields follow player</li>
 *   <li><b>bindings</b> - Reactive bindings (property → external value)</li>
 *   <li><b>triggers</b> - Event-triggered effects</li>
 *   <li><b>lifecycle</b> - Spawn/despawn animation config</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "id": "shield_default",
 *   "type": "SHIELD",
 *   "baseRadius": 1.0,
 *   "themeId": "energy_blue",
 *   "layers": [...],
 *   "modifiers": { "visualScale": 1.0, "tilt": 0.0 },
 *   "prediction": { "enabled": false },
 *   "beam": { "enabled": false },
 *   "followMode": { "enabled": false },
 *   "bindings": {
 *     "alpha": { "source": "player.health_percent", "outputRange": [0.3, 1.0] }
 *   },
 *   "triggers": [...],
 *   "lifecycle": { "fadeIn": 10, "fadeOut": 10 }
 * }
 * </pre>
 * 
 * @see FieldLayer
 * @see FieldType
 * @see BindingConfig
 * @see Modifiers
 * @see PredictionConfig
 * @see BeamConfig
 * @see FollowModeConfig
 * @see LifecycleConfig
 */
public record FieldDefinition(
    String id,
    FieldType type,
    float baseRadius,
    @Nullable @JsonField(skipIfNull = true) String themeId,
    List<FieldLayer> layers,
    @Nullable @JsonField(skipIfNull = true) Modifiers modifiers,
    @Nullable @JsonField(skipIfNull = true) PredictionConfig prediction,
    @Nullable @JsonField(skipIfNull = true) BeamConfig beam,
    @Nullable @JsonField(skipIfNull = true) FollowModeConfig followMode,
    @JsonField(skipIfEmpty = true) Map<String, BindingConfig> bindings,
    @JsonField(skipIfEmpty = true) List<TriggerConfig> triggers,
    @Nullable @JsonField(skipIfNull = true) LifecycleConfig lifecycle
){
    
    /**
     * Parses a FieldDefinition from JSON.
     * Delegates to FieldLoader for full parsing with $ref resolution and defaults.
     * 
     * @param json the JSON object to parse
     * @return parsed FieldDefinition
     */
    public static FieldDefinition fromJson(JsonObject json) {
        return net.cyberpunk042.field.loader.FieldLoader.fromJson(json);
    }
    
    /**
     * Creates an empty definition.
     */
    public static FieldDefinition empty(String id) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, null, List.of(),
            null, null, null, null, Map.of(), List.of(), null);
    }
    
    /**
     * Creates a definition with layers.
     */
    public static FieldDefinition of(String id, List<FieldLayer> layers) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, null, layers,
            null, null, null, null, Map.of(), List.of(), null);
    }
    
    /**
     * Creates a definition with layers and theme.
     */
    public static FieldDefinition of(String id, List<FieldLayer> layers, String themeId) {
        return new FieldDefinition(
            id, FieldType.SHIELD, 1.0f, themeId, layers,
            null, null, null, null, Map.of(), List.of(), null);
    }
    
    /**
     * Whether this field has any bindings.
     */
    public boolean hasBindings() {
        return bindings != null && !bindings.isEmpty();
    }
    
    /**
     * Whether this field has a beam effect.
     */
    public boolean hasBeam() {
        return beam != null && beam.enabled();
    }
    
    /**
     * Whether this field has any triggers.
     */
    public boolean hasTriggers() {
        return triggers != null && !triggers.isEmpty();
    }
    
    /**
     * Serializes this field definition to JSON.
     * 
     * @return JSON representation of this definition
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    /**
     * Builder for complex definitions.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private FieldType type = FieldType.SHIELD;
        private float baseRadius = 1.0f;
        private String themeId = null;
        private List<FieldLayer> layers = List.of();
        private Modifiers modifiers = null;
        private PredictionConfig prediction = null;
        private BeamConfig beam = null;
        private FollowModeConfig followMode = null;
        private Map<String, BindingConfig> bindings = Map.of();
        private List<TriggerConfig> triggers = List.of();
        private LifecycleConfig lifecycle = null;
        
        public Builder(String id) { this.id = id; }
        
        public Builder type(FieldType t) { this.type = t; return this; }
        public Builder baseRadius(float r) { this.baseRadius = r; return this; }
        public Builder themeId(String t) { this.themeId = t; return this; }
        public Builder layers(List<FieldLayer> l) { this.layers = l; return this; }
        public Builder layers(FieldLayer... l) { this.layers = List.of(l); return this; }
        public Builder modifiers(Modifiers m) { this.modifiers = m; return this; }
        public Builder prediction(PredictionConfig p) { this.prediction = p; return this; }
        public Builder beam(BeamConfig b) { this.beam = b; return this; }
        public Builder followMode(FollowModeConfig f) { this.followMode = f; return this; }
        public Builder bindings(Map<String, BindingConfig> b) { this.bindings = b; return this; }
        public Builder triggers(List<TriggerConfig> t) { this.triggers = t; return this; }
        public Builder lifecycle(LifecycleConfig l) { this.lifecycle = l; return this; }
        
        public FieldDefinition build() {
            return new FieldDefinition(
                id, type, baseRadius, themeId, layers,
                modifiers, prediction, beam, followMode,
                bindings, triggers, lifecycle);
        }
    }
}
