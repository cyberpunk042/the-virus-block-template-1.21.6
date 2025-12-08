package net.cyberpunk042.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.SpherePrimitive_old;
import net.cyberpunk042.field._legacy.primitive.RingPrimitive_old;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.log.Logging;

import java.util.ArrayList;
import java.util.List;

/**
 * A visual layer within a field definition.
 */
public record FieldLayer(
        String id,
        List<Primitive> primitives,
        String colorRef,
        float alpha,
        float spin,
        float tilt,
        float pulse,
        float phaseOffset
) {
    
    public static FieldLayer of(String id, Primitive primitive) {
        return new FieldLayer(id, List.of(primitive), "@primary", 1.0f, 0, 0, 0, 0);
    }
    
    public static FieldLayer of(String id, List<Primitive> primitives) {
        return new FieldLayer(id, List.copyOf(primitives), "@primary", 1.0f, 0, 0, 0, 0);
    }
    
    /**
     * Creates a simple sphere layer.
     */
    public static FieldLayer sphere(String id, float radius, int detail) {
        Primitive prim = SpherePrimitive_old.create(radius, detail);
        return of(id, prim);
    }
    
    /**
     * Creates a ring layer.
     */
    public static FieldLayer ring(String id, float y, float inner, float outer, int segments) {
        Primitive prim = RingPrimitive_old.create(inner, outer, segments, y);
        return of(id, prim);
    }
    
    public FieldLayer withColor(String colorRef) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, phaseOffset);
    }
    
    public FieldLayer withAlpha(float alpha) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, phaseOffset);
    }
    
    public FieldLayer withSpin(float spin) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, phaseOffset);
    }
    
    public FieldLayer withTilt(float tilt) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, phaseOffset);
    }
    
    public FieldLayer withPulse(float pulse) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, phaseOffset);
    }
    
    public FieldLayer withPhaseOffset(float offset) {
        return new FieldLayer(id, primitives, colorRef, alpha, spin, tilt, pulse, offset);
    }
    
    public static FieldLayer fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "layer";
        String color = json.has("color") ? json.get("color").getAsString() : "@primary";
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 1.0f;
        float spin = json.has("spin") ? json.get("spin").getAsFloat() : 0;
        float tilt = json.has("tilt") ? json.get("tilt").getAsFloat() : 0;
        float pulse = json.has("pulse") ? json.get("pulse").getAsFloat() : 0;
        float phase = json.has("phase") ? json.get("phase").getAsFloat() : 0;
        
        List<Primitive> primitives = new ArrayList<>();
        if (json.has("primitives")) {
            for (var elem : json.getAsJsonArray("primitives")) {
                Primitive p = parsePrimitive(elem.getAsJsonObject());
                if (p != null) primitives.add(p);
            }
        }
        
        if (primitives.isEmpty()) {
            primitives.add(SpherePrimitive_old.create(1.0f, 32));
        }
        
        return new FieldLayer(id, primitives, color, alpha, spin, tilt, pulse, phase);
    }
    
    /**
     * Parses a primitive from JSON.
     * 
     * <p>Supports two formats:
     * <ol>
     *   <li>FLAT (legacy): {"type": "sphere", "radius": 1.0, "detail": 32}</li>
     *   <li>NESTED (new): {"type": "sphere", "shape": {"radius": 1.0, "latSteps": 16, "lonSteps": 32}, ...}</li>
     * </ol>
     */
    private static Primitive parsePrimitive(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "sphere";
        
        // Check for nested "shape" object (new format)
        JsonObject shapeJson = json.has("shape") ? json.getAsJsonObject("shape") : json;
        JsonObject appearanceJson = json.has("appearance") ? json.getAsJsonObject("appearance") : null;
        JsonObject transformJson = json.has("transform") ? json.getAsJsonObject("transform") : null;
        
        // Parse appearance, animation, transform
        Appearance appearance = parseAppearance(appearanceJson);
        Animation animation = parseAnimation(json.has("animation") ? json.getAsJsonObject("animation") : null);
        net.cyberpunk042.visual.transform.Transform transform = 
            net.cyberpunk042.visual.transform.Transform.fromJson(transformJson);
        
        return switch (type.toLowerCase()) {
            case "sphere" -> {
                // Use SphereShape.fromJson to parse all fields including algorithm
                net.cyberpunk042.visual.shape.SphereShape sphereShape = 
                    net.cyberpunk042.visual.shape.SphereShape.fromJson(shapeJson);
                
                // Create primitive with full shape (includes algorithm!)
                SpherePrimitive_old sphere = SpherePrimitive_old.create(
                    sphereShape,
                    transform,
                    appearance != null ? appearance : net.cyberpunk042.visual.appearance.Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none()
                );
                
                Logging.REGISTRY.topic("field").debug(
                    "Parsed sphere: radius={}, lat={}, lon={}, algo={}", 
                    sphereShape.radius(), sphereShape.latSteps(), sphereShape.lonSteps(), sphereShape.algorithm());
                yield sphere;
            }
            case "ring" -> {
                float inner, outer;
                
                // Support two formats:
                // 1. innerRadius/outerRadius (explicit)
                // 2. radius/thickness (compute inner/outer from center radius and thickness)
                if (shapeJson.has("innerRadius") || shapeJson.has("outerRadius")) {
                    inner = getFloat(shapeJson, "innerRadius", 0.8f);
                    outer = getFloat(shapeJson, "outerRadius", 1.0f);
                } else {
                    // radius = center of the ring, thickness = width of the ring band
                    float centerRadius = getFloat(shapeJson, "radius", 1.0f);
                    float thickness = getFloat(shapeJson, "thickness", 0.1f);
                    inner = centerRadius - thickness / 2;
                    outer = centerRadius + thickness / 2;
                }
                
                int segments = getInt(shapeJson, "segments", 48);
                float y = getFloat(shapeJson, "y", 0);
                
                RingPrimitive_old ring = RingPrimitive_old.create(inner, outer, segments, y);
                if (appearance != null) {
                    ring = ring.withAppearance(appearance);
                }
                if (animation != null) {
                    ring = ring.withAnimation(animation);
                }
                
                Logging.REGISTRY.topic("field").debug(
                    "Parsed ring: inner={}, outer={}, y={}", inner, outer, y);
                yield ring;
            }
            case "disc" -> {
                float radius = getFloat(shapeJson, "radius", 1.0f);
                float y = getFloat(shapeJson, "y", 0);
                int segments = getInt(shapeJson, "segments", 32);
                
                var discShape = net.cyberpunk042.visual.shape.DiscShape.of(y, radius, segments);
                var disc = new net.cyberpunk042.field._legacy.primitive.DiscPrimitive_old(
                    discShape,
                    transform,
                    appearance != null ? appearance : Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none()
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed disc: radius={}, y={}", radius, y);
                yield disc;
            }
            case "beam" -> {
                float radius = getFloat(shapeJson, "radius", 0.5f);
                float height = getFloat(shapeJson, "height", 10.0f);
                
                var beamShape = net.cyberpunk042.visual.shape.CylinderShape.of(radius, height);
                var beam = new net.cyberpunk042.field._legacy.primitive.CylinderPrimitive_old(
                    beamShape,
                    transform,
                    appearance != null ? appearance : Appearance.glowing("@beam", 0.5f),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none(),
                    1.0f
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed beam: radius={}, height={}", radius, height);
                yield beam;
            }
            case "prism" -> {
                int sides = getInt(shapeJson, "sides", 6);
                float radius = getFloat(shapeJson, "radius", 1.0f);
                float height = getFloat(shapeJson, "height", 2.0f);
                boolean capped = !shapeJson.has("capped") || shapeJson.get("capped").getAsBoolean();
                
                // PrismShape constructor is (sides, height, radius)
                var prismShape = new net.cyberpunk042.visual.shape.PrismShape(sides, height, radius);
                var prism = new net.cyberpunk042.field._legacy.primitive.PrismPrimitive_old(
                    prismShape,
                    transform,
                    appearance != null ? appearance : Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none(),
                    capped
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed prism: sides={}, radius={}, height={}", sides, radius, height);
                yield prism;
            }
            case "polyhedron" -> {
                String polyType = shapeJson.has("polyType") ? shapeJson.get("polyType").getAsString() : "icosahedron";
                float radius = getFloat(shapeJson, "radius", 1.0f);
                
                // Map string to PolyhedronShape factory method
                var polyShape = switch (polyType.toLowerCase()) {
                    case "cube" -> net.cyberpunk042.visual.shape.PolyhedronShape.cube(radius);
                    case "octahedron" -> net.cyberpunk042.visual.shape.PolyhedronShape.octahedron(radius);
                    case "dodecahedron" -> net.cyberpunk042.visual.shape.PolyhedronShape.dodecahedron(radius);
                    case "tetrahedron" -> net.cyberpunk042.visual.shape.PolyhedronShape.tetrahedron(radius);
                    default -> net.cyberpunk042.visual.shape.PolyhedronShape.icosahedron(radius);
                };
                var poly = new net.cyberpunk042.field._legacy.primitive.PolyhedronPrimitive_old(
                    polyShape,
                    transform,
                    appearance != null ? appearance : Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none()
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed polyhedron: type={}, radius={}", polyType, radius);
                yield poly;
            }
            case "cage" -> {
                float radius = getFloat(shapeJson, "radius", 1.0f);
                int latLines = getInt(shapeJson, "latLines", 8);
                int lonLines = getInt(shapeJson, "lonLines", 16);
                
                var cage = new net.cyberpunk042.field._legacy.primitive.CagePrimitive_old(
                    net.cyberpunk042.visual.shape.SphereShape.of(radius),
                    transform,
                    appearance != null ? appearance : Appearance.wireframe("@wire"),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none(),
                    1.0f, latLines, lonLines
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed cage: radius={}, lat={}, lon={}", radius, latLines, lonLines);
                yield cage;
            }
            case "stripes" -> {
                float radius = getFloat(shapeJson, "radius", 1.0f);
                int count = getInt(shapeJson, "count", 8);
                float width = getFloat(shapeJson, "width", 0.5f);
                boolean alternate = shapeJson.has("alternate") && shapeJson.get("alternate").getAsBoolean();
                
                var stripes = new net.cyberpunk042.field._legacy.primitive.StripesPrimitive_old(
                    net.cyberpunk042.visual.shape.SphereShape.of(radius),
                    transform,
                    appearance != null ? appearance : Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none(),
                    count, width, alternate
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed stripes: radius={}, count={}, width={}, alternate={}", radius, count, width, alternate);
                yield stripes;
            }
            case "rings" -> {
                // Multiple concentric rings
                float innerRadius = getFloat(shapeJson, "innerRadius", 0.5f);
                float outerRadius = getFloat(shapeJson, "outerRadius", 2.0f);
                int count = getInt(shapeJson, "count", 3);
                float thickness = getFloat(shapeJson, "thickness", 0.1f);
                
                // RingsPrimitive_old(Transform, Appearance, Animation, count, inner, outer, thickness)
                var rings = new net.cyberpunk042.field._legacy.primitive.RingsPrimitive_old(
                    transform,
                    appearance != null ? appearance : Appearance.defaults(),
                    animation != null ? animation : net.cyberpunk042.visual.animation.Animation.none(),
                    count, innerRadius, outerRadius, thickness
                );
                
                Logging.REGISTRY.topic("field").debug("Parsed rings: count={}, inner={}, outer={}", count, innerRadius, outerRadius);
                yield rings;
            }
            default -> {
                Logging.REGISTRY.topic("field").warn(
                    "Unknown primitive type '{}', defaulting to sphere", type);
                // Return default sphere instead of null
                yield SpherePrimitive_old.create(1.0f, 32);
            }
        };
    }
    
    /**
     * Parses appearance from JSON.
     */
    private static Appearance parseAppearance(JsonObject json) {
        if (json == null) return null;
        
        String color = json.has("color") ? json.get("color").getAsString() : "@primary";
        
        // Parse alpha - can be float or object with min/max
        float alphaMin = 0.6f;
        float alphaMax = 0.8f;
        if (json.has("alpha")) {
            var alphaElem = json.get("alpha");
            if (alphaElem.isJsonPrimitive()) {
                alphaMin = alphaMax = alphaElem.getAsFloat();
            } else if (alphaElem.isJsonObject()) {
                JsonObject alphaObj = alphaElem.getAsJsonObject();
                alphaMin = getFloat(alphaObj, "min", 0.6f);
                alphaMax = getFloat(alphaObj, "max", 0.8f);
            }
        }
        
        // Parse fill mode (solid or wireframe)
        net.cyberpunk042.visual.appearance.FillMode fill = net.cyberpunk042.visual.appearance.FillMode.SOLID;
        if (json.has("fill")) {
            String fillStr = json.get("fill").getAsString().toUpperCase();
            if ("WIREFRAME".equals(fillStr) || "WIRE".equals(fillStr)) {
                fill = net.cyberpunk042.visual.appearance.FillMode.WIREFRAME;
            }
        }
        
        // Parse pattern (bands, checker, etc.) - uses PatternConfig.fromJson which also parses trianglePattern
        net.cyberpunk042.visual.appearance.PatternConfig pattern = net.cyberpunk042.visual.appearance.PatternConfig.NONE;
        if (json.has("pattern")) {
            var patternElem = json.get("pattern");
            if (patternElem.isJsonObject()) {
                pattern = net.cyberpunk042.visual.appearance.PatternConfig.fromJson(patternElem.getAsJsonObject());
            }
        }
        
        float glow = getFloat(json, "glow", 0);
        float wireThickness = getFloat(json, "wireThickness", 1.0f);
        
        // Build full appearance
        net.cyberpunk042.visual.appearance.AlphaRange alpha = 
            net.cyberpunk042.visual.appearance.AlphaRange.of(alphaMin, alphaMax);
        
        return new Appearance(color, alpha, fill, pattern, glow, wireThickness);
    }
    
    /**
     * Parses animation from JSON.
     */
    private static Animation parseAnimation(JsonObject json) {
        if (json == null) return Animation.none();
        return Animation.fromJson(json);
    }
    
    // Helper methods for JSON parsing
    private static float getFloat(JsonObject json, String key, float def) {
        return json != null && json.has(key) ? json.get(key).getAsFloat() : def;
    }
    
    private static int getInt(JsonObject json, String key, int def) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : def;
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("color", colorRef);
        if (alpha != 1.0f) json.addProperty("alpha", alpha);
        if (spin != 0) json.addProperty("spin", spin);
        if (tilt != 0) json.addProperty("tilt", tilt);
        if (pulse != 0) json.addProperty("pulse", pulse);
        if (phaseOffset != 0) json.addProperty("phase", phaseOffset);
        
        // Serialize primitives
        if (!primitives.isEmpty()) {
            JsonArray primArray = new JsonArray();
            for (Primitive p : primitives) {
                primArray.add(primitiveToJson(p));
            }
            json.add("primitives", primArray);
        }
        return json;
    }
    
    private static JsonObject primitiveToJson(Primitive p) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", p.type());
        // Add shape-specific properties based on type
        if (p instanceof SpherePrimitive_old sphere) {
            obj.addProperty("radius", sphere.getRadius());
            obj.addProperty("detail", sphere.getDetail());
        } else if (p instanceof RingPrimitive_old ring) {
            obj.addProperty("innerRadius", ring.getInnerRadius());
            obj.addProperty("outerRadius", ring.getOuterRadius());
            obj.addProperty("segments", ring.getSegments());
        }
        return obj;
    }
}
