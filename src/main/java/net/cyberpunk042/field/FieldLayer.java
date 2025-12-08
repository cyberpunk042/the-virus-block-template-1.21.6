package net.cyberpunk042.field;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.layer.BlendMode;
import net.cyberpunk042.visual.transform.Transform;

import java.util.List;

/**
 * A layer within a field definition.
 * 
 * <p>Layers group primitives and apply shared configuration:
 * <ul>
 *   <li><b>transform</b> - Applied to all primitives in layer</li>
 *   <li><b>animation</b> - Shared animation (spin, pulse)</li>
 *   <li><b>alpha</b> - Layer-wide alpha multiplier</li>
 *   <li><b>visible</b> - Toggle layer visibility</li>
 *   <li><b>blendMode</b> - How layer blends with others</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "id": "main_layer",
 *   "primitives": [...],
 *   "transform": { "offset": [0, 1, 0] },
 *   "animation": { "spin": { "speed": 0.02 } },
 *   "alpha": 1.0,
 *   "visible": true,
 *   "blendMode": "NORMAL"
 * }
 * </pre>
 * 
 * @see FieldDefinition
 * @see Primitive
 */
public record FieldLayer(
    String id,
    List<Primitive> primitives,
    Transform transform,
    Animation animation,
    float alpha,
    boolean visible,
    BlendMode blendMode
) {
    
    /**
     * Creates an empty layer.
     */
    public static FieldLayer empty(String id) {
        return new FieldLayer(id, List.of(), Transform.IDENTITY, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Creates a simple layer with primitives.
     */
    public static FieldLayer of(String id, List<Primitive> primitives) {
        return new FieldLayer(id, primitives, Transform.IDENTITY, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Creates a layer with transform.
     */
    public static FieldLayer of(String id, List<Primitive> primitives, Transform transform) {
        return new FieldLayer(id, primitives, transform, Animation.NONE, 1.0f, true, BlendMode.NORMAL);
    }
    
    /**
     * Builder for complex layers.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }
    /**
     * Serializes this layer to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("alpha", alpha);
        json.addProperty("visible", visible);
        json.addProperty("blendMode", blendMode.name());
        
        // Serialize primitives
        JsonArray primitivesArray = new JsonArray();
        for (Primitive primitive : primitives) {
            if (primitive instanceof net.cyberpunk042.field.loader.SimplePrimitive simple) {
                primitivesArray.add(simple.toJson());
            } else {
                // For other Primitive implementations, create basic JSON
                JsonObject primJson = new JsonObject();
                primJson.addProperty("id", primitive.id());
                primJson.addProperty("type", primitive.type());
                primitivesArray.add(primJson);
            }
        }
        json.add("primitives", primitivesArray);
        
        // Serialize transform
        if (transform != null && transform != Transform.IDENTITY) {
            json.add("transform", transform.toJson());
        }
        
        // Serialize animation
        if (animation != null && animation != Animation.NONE) {
            json.add("animation", animation.toJson());
        }
        
        return json;
    }


    
    public static class Builder {
        private final String id;
        private List<Primitive> primitives = List.of();
        private Transform transform = Transform.IDENTITY;
        private Animation animation = Animation.NONE;
        private float alpha = 1.0f;
        private boolean visible = true;
        private BlendMode blendMode = BlendMode.NORMAL;
        
        public Builder(String id) { this.id = id; }
        
        public Builder primitives(List<Primitive> p) { this.primitives = p; return this; }
        public Builder primitives(Primitive... p) { this.primitives = List.of(p); return this; }
        public Builder transform(Transform t) { this.transform = t; return this; }
        public Builder animation(Animation a) { this.animation = a; return this; }
        public Builder alpha(float a) { this.alpha = a; return this; }
        public Builder visible(boolean v) { this.visible = v; return this; }
        public Builder blendMode(BlendMode b) { this.blendMode = b; return this; }
        
        public FieldLayer build() {
            return new FieldLayer(id, primitives, transform, animation, alpha, visible, blendMode);
        }
    }
}
