package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import net.cyberpunk042.log.Logging;

/**
 * Adapter for shockwave visual effect configuration.
 * 
 * <p>Shockwave is a <b>field-level</b> effect that sits above the layer/primitive
 * hierarchy. It's not stored per-primitive but alongside the field definition.</p>
 * 
 * <p>Handles paths like {@code shockwave.shapeType}, {@code shockwave.orbitDistance},
 * {@code shockwave.ringSpeed}, etc.</p>
 * 
 * <p>This adapter stores the configuration for JSON persistence AND syncs changes 
 * to the live ShockwavePostEffect for immediate visual feedback.</p>
 * 
 * <p>Future: Can reference a primitive to use its geometry as the shockwave shape source.</p>
 */
@StateCategory("shockwave")
public class ShockwaveAdapter extends AbstractAdapter {
    
    private ShockwaveConfig config = ShockwaveConfig.DEFAULT;
    
    // Optional: reference to a primitive to use as shape source
    // Format: "layerIndex.primitiveIndex" or null
    private String shapeSourceRef = null;
    
    // Polygon side count (for POLYGON shape type, default 6 = hexagon)
    private int polygonSides = 6;
    
    // Currently selected preset name (for UI persistence after rebuild)
    private String currentPresetName = "Default";
    
    public String category() { return "shockwave"; }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            // Shape source reference (for linking to a primitive's geometry)
            case "shapeSourceRef" -> shapeSourceRef;
            case "currentPresetName" -> currentPresetName;
            
            // Shape
            case "shapeType" -> config.shapeType();
            case "mainRadius" -> config.mainRadius();
            case "orbitalRadius" -> config.orbitalRadius();
            case "orbitDistance" -> config.orbitDistance();
            case "orbitalCount" -> config.orbitalCount();
            case "polygonSides" -> polygonSides;
            
            // Ring geometry
            case "ringCount" -> config.ringCount();
            case "ringSpacing" -> config.ringSpacing();
            case "ringThickness" -> config.ringThickness();
            case "ringMaxRadius" -> config.ringMaxRadius();
            case "ringSpeed" -> config.ringSpeed();
            case "ringGlowWidth" -> config.ringGlowWidth();
            case "ringIntensity" -> config.ringIntensity();
            case "ringContractMode" -> config.ringContractMode();
            
            // Ring color
            case "ringColorR" -> config.ringColorR();
            case "ringColorG" -> config.ringColorG();
            case "ringColorB" -> config.ringColorB();
            case "ringColorOpacity" -> config.ringColorOpacity();
            
            // Orbital body
            case "orbitalBodyR" -> config.orbitalBodyR();
            case "orbitalBodyG" -> config.orbitalBodyG();
            case "orbitalBodyB" -> config.orbitalBodyB();
            
            // Orbital corona
            case "orbitalCoronaR" -> config.orbitalCoronaR();
            case "orbitalCoronaG" -> config.orbitalCoronaG();
            case "orbitalCoronaB" -> config.orbitalCoronaB();
            case "orbitalCoronaA" -> config.orbitalCoronaA();
            case "orbitalCoronaWidth" -> config.orbitalCoronaWidth();
            case "orbitalCoronaIntensity" -> config.orbitalCoronaIntensity();
            case "orbitalRimPower" -> config.orbitalRimPower();
            case "orbitalRimFalloff" -> config.orbitalRimFalloff();
            
            // Beam geometry
            case "beamHeight" -> config.beamHeight();
            case "beamWidth" -> config.beamWidth();
            case "beamWidthScale" -> config.beamWidthScale();
            case "beamTaper" -> config.beamTaper();
            
            // Beam body
            case "beamBodyR" -> config.beamBodyR();
            case "beamBodyG" -> config.beamBodyG();
            case "beamBodyB" -> config.beamBodyB();
            
            // Beam corona
            case "beamCoronaR" -> config.beamCoronaR();
            case "beamCoronaG" -> config.beamCoronaG();
            case "beamCoronaB" -> config.beamCoronaB();
            case "beamCoronaA" -> config.beamCoronaA();
            case "beamCoronaWidth" -> config.beamCoronaWidth();
            case "beamCoronaIntensity" -> config.beamCoronaIntensity();
            case "beamRimPower" -> config.beamRimPower();
            case "beamRimFalloff" -> config.beamRimFalloff();
            
            // Animation timing
            case "orbitalSpeed" -> config.orbitalSpeed();
            case "orbitalSpawnDuration" -> config.orbitalSpawnDuration();
            case "orbitalRetractDuration" -> config.orbitalRetractDuration();
            case "beamGrowDuration" -> config.beamGrowDuration();
            case "beamShrinkDuration" -> config.beamShrinkDuration();
            case "beamHoldDuration" -> config.beamHoldDuration();
            case "beamWidthGrowFactor" -> config.beamWidthGrowFactor();
            case "beamLengthGrowFactor" -> config.beamLengthGrowFactor();
            
            // Easing types
            case "orbitalSpawnEasing" -> config.orbitalSpawnEasing();
            case "orbitalRetractEasing" -> config.orbitalRetractEasing();
            case "beamGrowEasing" -> config.beamGrowEasing();
            case "beamShrinkEasing" -> config.beamShrinkEasing();
            
            // Delays
            case "orbitalSpawnDelay" -> config.orbitalSpawnDelay();
            case "beamStartDelay" -> config.beamStartDelay();
            case "retractDelay" -> config.retractDelay();
            case "autoRetractOnRingEnd" -> config.autoRetractOnRingEnd();
            
            // Screen effects
            case "blackout" -> config.blackout();
            case "vignetteAmount" -> config.vignetteAmount();
            case "vignetteRadius" -> config.vignetteRadius();
            case "tintR" -> config.tintR();
            case "tintG" -> config.tintG();
            case "tintB" -> config.tintB();
            case "tintAmount" -> config.tintAmount();
            
            // Blend
            case "blendRadius" -> config.blendRadius();
            
            // Global scale & positioning
            case "globalScale" -> config.globalScale();
            case "followPosition" -> config.followPosition();
            
            default -> {
                Logging.GUI.topic("adapter").warn("Unknown shockwave property: {}", prop);
                yield null;
            }
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        ShockwaveConfig.Builder b = config.toBuilder();
        
        switch (prop) {
            // Shape
            // Shape source reference (for linking to a primitive's geometry)
            case "shapeSourceRef" -> shapeSourceRef = value != null ? value.toString() : null;
            case "currentPresetName" -> { currentPresetName = value != null ? value.toString() : "Default"; return; }  // No sync needed for UI-only field
            
            // Shape
            case "shapeType" -> {
                ShapeType type = value instanceof ShapeType st ? st 
                    : ShapeType.valueOf(value.toString());
                b.shapeType(type);
            }
            case "mainRadius" -> b.mainRadius(toFloat(value));
            case "orbitalRadius" -> b.orbitalRadius(toFloat(value));
            case "orbitDistance" -> b.orbitDistance(toFloat(value));
            case "orbitalCount" -> b.orbitalCount(toInt(value));
            case "polygonSides" -> polygonSides = toInt(value);
            
            // Ring geometry
            case "ringCount" -> b.ringCount(toInt(value));
            case "ringSpacing" -> b.ringSpacing(toFloat(value));
            case "ringThickness" -> b.ringThickness(toFloat(value));
            case "ringMaxRadius" -> b.ringMaxRadius(toFloat(value));
            case "ringSpeed" -> b.ringSpeed(toFloat(value));
            case "ringGlowWidth" -> b.ringGlowWidth(toFloat(value));
            case "ringIntensity" -> b.ringIntensity(toFloat(value));
            case "ringContractMode" -> b.ringContractMode(toBool(value));
            
            // Ring color
            case "ringColorR" -> b.ringColorR(toFloat(value));
            case "ringColorG" -> b.ringColorG(toFloat(value));
            case "ringColorB" -> b.ringColorB(toFloat(value));
            case "ringColorOpacity" -> b.ringColorOpacity(toFloat(value));
            
            // Orbital body
            case "orbitalBodyR" -> b.orbitalBodyR(toFloat(value));
            case "orbitalBodyG" -> b.orbitalBodyG(toFloat(value));
            case "orbitalBodyB" -> b.orbitalBodyB(toFloat(value));
            
            // Orbital corona
            case "orbitalCoronaR" -> b.orbitalCoronaR(toFloat(value));
            case "orbitalCoronaG" -> b.orbitalCoronaG(toFloat(value));
            case "orbitalCoronaB" -> b.orbitalCoronaB(toFloat(value));
            case "orbitalCoronaA" -> b.orbitalCoronaA(toFloat(value));
            case "orbitalCoronaWidth" -> b.orbitalCoronaWidth(toFloat(value));
            case "orbitalCoronaIntensity" -> b.orbitalCoronaIntensity(toFloat(value));
            case "orbitalRimPower" -> b.orbitalRimPower(toFloat(value));
            case "orbitalRimFalloff" -> b.orbitalRimFalloff(toFloat(value));
            
            // Beam geometry
            case "beamHeight" -> b.beamHeight(toFloat(value));
            case "beamWidth" -> b.beamWidth(toFloat(value));
            case "beamWidthScale" -> b.beamWidthScale(toFloat(value));
            case "beamTaper" -> b.beamTaper(toFloat(value));
            
            // Beam body
            case "beamBodyR" -> b.beamBodyR(toFloat(value));
            case "beamBodyG" -> b.beamBodyG(toFloat(value));
            case "beamBodyB" -> b.beamBodyB(toFloat(value));
            
            // Beam corona
            case "beamCoronaR" -> b.beamCoronaR(toFloat(value));
            case "beamCoronaG" -> b.beamCoronaG(toFloat(value));
            case "beamCoronaB" -> b.beamCoronaB(toFloat(value));
            case "beamCoronaA" -> b.beamCoronaA(toFloat(value));
            case "beamCoronaWidth" -> b.beamCoronaWidth(toFloat(value));
            case "beamCoronaIntensity" -> b.beamCoronaIntensity(toFloat(value));
            case "beamRimPower" -> b.beamRimPower(toFloat(value));
            case "beamRimFalloff" -> b.beamRimFalloff(toFloat(value));
            
            // Animation timing
            case "orbitalSpeed" -> b.orbitalSpeed(toFloat(value));
            case "orbitalSpawnDuration" -> b.orbitalSpawnDuration(toFloat(value));
            case "orbitalRetractDuration" -> b.orbitalRetractDuration(toFloat(value));
            case "beamGrowDuration" -> b.beamGrowDuration(toFloat(value));
            case "beamShrinkDuration" -> b.beamShrinkDuration(toFloat(value));
            case "beamHoldDuration" -> b.beamHoldDuration(toFloat(value));
            case "beamWidthGrowFactor" -> b.beamWidthGrowFactor(toFloat(value));
            case "beamLengthGrowFactor" -> b.beamLengthGrowFactor(toFloat(value));
            
            // Easing types
            case "orbitalSpawnEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.orbitalSpawnEasing(type);
            }
            case "orbitalRetractEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.orbitalRetractEasing(type);
            }
            case "beamGrowEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.beamGrowEasing(type);
            }
            case "beamShrinkEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.beamShrinkEasing(type);
            }
            
            // Delays
            case "orbitalSpawnDelay" -> b.orbitalSpawnDelay(toFloat(value));
            case "beamStartDelay" -> b.beamStartDelay(toFloat(value));
            case "retractDelay" -> b.retractDelay(toFloat(value));
            case "autoRetractOnRingEnd" -> b.autoRetractOnRingEnd(toBool(value));
            
            // Screen effects
            case "blackout" -> b.blackout(toFloat(value));
            case "vignetteAmount" -> b.vignetteAmount(toFloat(value));
            case "vignetteRadius" -> b.vignetteRadius(toFloat(value));
            case "tintR" -> b.tintR(toFloat(value));
            case "tintG" -> b.tintG(toFloat(value));
            case "tintB" -> b.tintB(toFloat(value));
            case "tintAmount" -> b.tintAmount(toFloat(value));
            
            // Blend
            case "blendRadius" -> b.blendRadius(toFloat(value));
            
            // Global scale & positioning
            case "globalScale" -> b.globalScale(toFloat(value));
            case "followPosition" -> b.followPosition(toBool(value));
            
            default -> Logging.GUI.topic("adapter").warn("Unknown shockwave property: {}", prop);
        }
        
        config = b.build();
        
        // Sync to live effect for immediate visual feedback
        syncToPostEffect();
    }
    
    /**
     * Syncs the current config to ShockwavePostEffect for live preview.
     * Applies globalScale to all size-related parameters.
     */
    public void syncToPostEffect() {
        float scale = config.globalScale();
        
        // Shape (with scale applied to distances/radii)
        // Build ShapeConfig based on actual shapeType from config
        ShapeConfig shapeConfig;
        switch (config.shapeType()) {
            case POINT -> shapeConfig = new ShapeConfig(ShapeType.POINT, 0, 0, 0, 0, 0);
            case SPHERE -> shapeConfig = ShapeConfig.sphere(config.mainRadius() * scale);
            case TORUS -> shapeConfig = ShapeConfig.torus(config.mainRadius() * scale, config.orbitalRadius() * scale);
            case POLYGON -> shapeConfig = ShapeConfig.polygon(polygonSides, config.mainRadius() * scale);
            case ORBITAL -> shapeConfig = ShapeConfig.orbital(
                config.mainRadius() * scale, 
                config.orbitalRadius() * scale, 
                config.orbitDistance() * scale, 
                config.orbitalCount());
            default -> shapeConfig = ShapeConfig.sphere(config.mainRadius() * scale);
        }
        ShockwavePostEffect.setShape(shapeConfig);
        
        // Ring parameters (with scale applied to size parameters)
        ShockwavePostEffect.setRingCount(config.ringCount());
        ShockwavePostEffect.setRingSpacing(config.ringSpacing() * scale);
        ShockwavePostEffect.setThickness(config.ringThickness() * scale);
        ShockwavePostEffect.setMaxRadius(config.ringMaxRadius() * scale);
        ShockwavePostEffect.setSpeed(config.ringSpeed() * scale);
        ShockwavePostEffect.setGlowWidth(config.ringGlowWidth() * scale);
        ShockwavePostEffect.setIntensity(config.ringIntensity());  // Intensity not scaled
        ShockwavePostEffect.setContractMode(config.ringContractMode());
        ShockwavePostEffect.setRingColor(
            config.ringColorR(), config.ringColorG(), 
            config.ringColorB(), config.ringColorOpacity()
        );
        
        // Orbital visual (corona width scaled)
        ShockwavePostEffect.setOrbitalVisual(new OrbitalVisualConfig(
            new Color3f(config.orbitalBodyR(), config.orbitalBodyG(), config.orbitalBodyB()),
            new CoronaConfig(
                new Color4f(config.orbitalCoronaR(), config.orbitalCoronaG(), 
                            config.orbitalCoronaB(), config.orbitalCoronaA()),
                config.orbitalCoronaWidth() * scale, config.orbitalCoronaIntensity(),
                config.orbitalRimPower(), config.orbitalRimFalloff()
            )
        ));
        
        // Beam visual (width and height scaled)
        ShockwavePostEffect.setBeamVisual(new BeamVisualConfig(
            new Color3f(config.beamBodyR(), config.beamBodyG(), config.beamBodyB()),
            new CoronaConfig(
                new Color4f(config.beamCoronaR(), config.beamCoronaG(), 
                            config.beamCoronaB(), config.beamCoronaA()),
                config.beamCoronaWidth() * scale, config.beamCoronaIntensity(),
                config.beamRimPower(), config.beamRimFalloff()
            ),
            config.beamWidth() * scale, config.beamWidthScale(), config.beamTaper()
        ));
        
        // Animation timing (beam height scaled)
        ShockwavePostEffect.setAnimationTiming(new AnimationTimingConfig(
            config.orbitalSpeed(),
            config.orbitalSpawnDuration(),
            config.orbitalRetractDuration(),
            config.orbitalSpawnEasing(),
            config.orbitalRetractEasing(),
            config.beamHeight() * scale,  // Scale beam height
            config.beamGrowDuration(),
            config.beamShrinkDuration(),
            config.beamHoldDuration(),
            config.beamWidthGrowFactor(),
            config.beamLengthGrowFactor(),
            config.beamGrowEasing(),
            config.beamShrinkEasing(),
            config.orbitalSpawnDelay(),
            config.beamStartDelay(),
            config.retractDelay(),
            config.autoRetractOnRingEnd()
        ));
        
        // Screen effects
        ShockwavePostEffect.setBlackout(config.blackout());
        ShockwavePostEffect.setVignette(config.vignetteAmount(), config.vignetteRadius());
        ShockwavePostEffect.setTint(config.tintR(), config.tintG(), config.tintB(), config.tintAmount());
        
        // Blend
        ShockwavePostEffect.setBlendRadius(config.blendRadius());
        
        // Follow position
        ShockwavePostEffect.setFollowCamera(config.followPosition());
    }
    
    // Helper conversion methods
    private float toFloat(Object v) { 
        return v instanceof Number n ? n.floatValue() : 0f; 
    }
    
    private int toInt(Object v) { 
        return v instanceof Number n ? n.intValue() : 0; 
    }
    
    private boolean toBool(Object v) { 
        return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); 
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ShockwaveConfig config() { return config; }
    
    public void setConfig(ShockwaveConfig config) { 
        this.config = config;
        syncToPostEffect();
    }
    
    public String shapeSourceRef() { return shapeSourceRef; }
    
    public void setShapeSourceRef(String ref) { this.shapeSourceRef = ref; }
    
    /**
     * Syncs geometry from a source primitive.
     * Called when shapeSourceRef is set and the primitive's geometry should be used.
     * 
     * @param primitive the primitive to use as shape source
     */
    public void syncFromPrimitive(net.cyberpunk042.field.primitive.Primitive primitive) {
        if (primitive == null) return;
        
        var shape = primitive.shape();
        if (shape == null) return;
        
        ShockwaveConfig.Builder b = config.toBuilder();
        
        // Determine shape type and extract radius
        if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(s.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) {
            b.shapeType(ShapeType.TORUS);  // Ring shapes map to TORUS shockwave type
            b.mainRadius(r.outerRadius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(c.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.PolyhedronShape p) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(p.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.TorusShape t) {
            b.shapeType(ShapeType.TORUS);  // Torus shapes use TORUS shockwave type
            b.mainRadius(t.majorRadius() + t.minorRadius());
        }
        // Add more shape types as needed
        
        config = b.build();
        syncToPostEffect();
        Logging.GUI.topic("adapter").debug("Synced shockwave from primitive: {}", primitive.id());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // POSITION FOLLOWING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if shockwave should follow source position each frame.
     */
    public boolean isFollowPositionEnabled() {
        return config.followPosition();
    }
    
    /**
     * Returns the current global scale multiplier.
     */
    public float getGlobalScale() {
        return config.globalScale();
    }
    
    /**
     * Get the current config (for read-only access).
     */
    public ShockwaveConfig getConfig() {
        return config;
    }
    
    @Override
    public void reset() {
        this.config = ShockwaveConfig.DEFAULT;
        this.shapeSourceRef = null;
        syncToPostEffect();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON PERSISTENCE (field-level storage)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Serialize shockwave config to JSON for field definition storage.
     */
    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        
        // Shape source reference (optional)
        if (shapeSourceRef != null) {
            json.addProperty("shapeSourceRef", shapeSourceRef);
        }
        
        // Shape
        json.addProperty("shapeType", config.shapeType().name());
        json.addProperty("mainRadius", config.mainRadius());
        json.addProperty("orbitalRadius", config.orbitalRadius());
        json.addProperty("orbitDistance", config.orbitDistance());
        json.addProperty("orbitalCount", config.orbitalCount());
        
        // Ring geometry
        json.addProperty("ringCount", config.ringCount());
        json.addProperty("ringSpacing", config.ringSpacing());
        json.addProperty("ringThickness", config.ringThickness());
        json.addProperty("ringMaxRadius", config.ringMaxRadius());
        json.addProperty("ringSpeed", config.ringSpeed());
        json.addProperty("ringGlowWidth", config.ringGlowWidth());
        json.addProperty("ringIntensity", config.ringIntensity());
        json.addProperty("ringContractMode", config.ringContractMode());
        
        // Ring color
        json.addProperty("ringColorR", config.ringColorR());
        json.addProperty("ringColorG", config.ringColorG());
        json.addProperty("ringColorB", config.ringColorB());
        json.addProperty("ringColorOpacity", config.ringColorOpacity());
        
        // Orbital body
        json.addProperty("orbitalBodyR", config.orbitalBodyR());
        json.addProperty("orbitalBodyG", config.orbitalBodyG());
        json.addProperty("orbitalBodyB", config.orbitalBodyB());
        
        // Orbital corona
        json.addProperty("orbitalCoronaR", config.orbitalCoronaR());
        json.addProperty("orbitalCoronaG", config.orbitalCoronaG());
        json.addProperty("orbitalCoronaB", config.orbitalCoronaB());
        json.addProperty("orbitalCoronaA", config.orbitalCoronaA());
        json.addProperty("orbitalCoronaWidth", config.orbitalCoronaWidth());
        json.addProperty("orbitalCoronaIntensity", config.orbitalCoronaIntensity());
        json.addProperty("orbitalRimPower", config.orbitalRimPower());
        json.addProperty("orbitalRimFalloff", config.orbitalRimFalloff());
        
        // Beam geometry
        json.addProperty("beamHeight", config.beamHeight());
        json.addProperty("beamWidth", config.beamWidth());
        json.addProperty("beamWidthScale", config.beamWidthScale());
        json.addProperty("beamTaper", config.beamTaper());
        
        // Beam body
        json.addProperty("beamBodyR", config.beamBodyR());
        json.addProperty("beamBodyG", config.beamBodyG());
        json.addProperty("beamBodyB", config.beamBodyB());
        
        // Beam corona
        json.addProperty("beamCoronaR", config.beamCoronaR());
        json.addProperty("beamCoronaG", config.beamCoronaG());
        json.addProperty("beamCoronaB", config.beamCoronaB());
        json.addProperty("beamCoronaA", config.beamCoronaA());
        json.addProperty("beamCoronaWidth", config.beamCoronaWidth());
        json.addProperty("beamCoronaIntensity", config.beamCoronaIntensity());
        json.addProperty("beamRimPower", config.beamRimPower());
        json.addProperty("beamRimFalloff", config.beamRimFalloff());
        
        // Animation timing
        json.addProperty("orbitalSpeed", config.orbitalSpeed());
        json.addProperty("orbitalSpawnDuration", config.orbitalSpawnDuration());
        json.addProperty("orbitalRetractDuration", config.orbitalRetractDuration());
        json.addProperty("beamGrowDuration", config.beamGrowDuration());
        json.addProperty("beamShrinkDuration", config.beamShrinkDuration());
        json.addProperty("beamHoldDuration", config.beamHoldDuration());
        json.addProperty("beamWidthGrowFactor", config.beamWidthGrowFactor());
        json.addProperty("beamLengthGrowFactor", config.beamLengthGrowFactor());
        
        // Easing types
        json.addProperty("orbitalSpawnEasing", config.orbitalSpawnEasing().name());
        json.addProperty("orbitalRetractEasing", config.orbitalRetractEasing().name());
        json.addProperty("beamGrowEasing", config.beamGrowEasing().name());
        json.addProperty("beamShrinkEasing", config.beamShrinkEasing().name());
        
        // Delays
        json.addProperty("orbitalSpawnDelay", config.orbitalSpawnDelay());
        json.addProperty("beamStartDelay", config.beamStartDelay());
        json.addProperty("retractDelay", config.retractDelay());
        json.addProperty("autoRetractOnRingEnd", config.autoRetractOnRingEnd());
        
        // Screen effects
        json.addProperty("blackout", config.blackout());
        json.addProperty("vignetteAmount", config.vignetteAmount());
        json.addProperty("vignetteRadius", config.vignetteRadius());
        json.addProperty("tintR", config.tintR());
        json.addProperty("tintG", config.tintG());
        json.addProperty("tintB", config.tintB());
        json.addProperty("tintAmount", config.tintAmount());
        
        // Blend
        json.addProperty("blendRadius", config.blendRadius());
        
        // Global scale & positioning
        json.addProperty("globalScale", config.globalScale());
        json.addProperty("followPosition", config.followPosition());
        
        return json;
    }
    
    /**
     * Load shockwave config from JSON (field definition storage).
     */
    public void loadFromJson(com.google.gson.JsonObject json) {
        if (json == null) {
            reset();
            return;
        }
        
        // Shape source reference
        shapeSourceRef = json.has("shapeSourceRef") ? json.get("shapeSourceRef").getAsString() : null;
        
        ShockwaveConfig.Builder b = ShockwaveConfig.DEFAULT.toBuilder();
        
        // Shape
        if (json.has("shapeType")) {
            try {
                b.shapeType(ShapeType.valueOf(json.get("shapeType").getAsString()));
            } catch (IllegalArgumentException e) { /* keep default */ }
        }
        if (json.has("mainRadius")) b.mainRadius(json.get("mainRadius").getAsFloat());
        if (json.has("orbitalRadius")) b.orbitalRadius(json.get("orbitalRadius").getAsFloat());
        if (json.has("orbitDistance")) b.orbitDistance(json.get("orbitDistance").getAsFloat());
        if (json.has("orbitalCount")) b.orbitalCount(json.get("orbitalCount").getAsInt());
        
        // Ring geometry
        if (json.has("ringCount")) b.ringCount(json.get("ringCount").getAsInt());
        if (json.has("ringSpacing")) b.ringSpacing(json.get("ringSpacing").getAsFloat());
        if (json.has("ringThickness")) b.ringThickness(json.get("ringThickness").getAsFloat());
        if (json.has("ringMaxRadius")) b.ringMaxRadius(json.get("ringMaxRadius").getAsFloat());
        if (json.has("ringSpeed")) b.ringSpeed(json.get("ringSpeed").getAsFloat());
        if (json.has("ringGlowWidth")) b.ringGlowWidth(json.get("ringGlowWidth").getAsFloat());
        if (json.has("ringIntensity")) b.ringIntensity(json.get("ringIntensity").getAsFloat());
        if (json.has("ringContractMode")) b.ringContractMode(json.get("ringContractMode").getAsBoolean());
        
        // Ring color
        if (json.has("ringColorR")) b.ringColorR(json.get("ringColorR").getAsFloat());
        if (json.has("ringColorG")) b.ringColorG(json.get("ringColorG").getAsFloat());
        if (json.has("ringColorB")) b.ringColorB(json.get("ringColorB").getAsFloat());
        if (json.has("ringColorOpacity")) b.ringColorOpacity(json.get("ringColorOpacity").getAsFloat());
        
        // Orbital body
        if (json.has("orbitalBodyR")) b.orbitalBodyR(json.get("orbitalBodyR").getAsFloat());
        if (json.has("orbitalBodyG")) b.orbitalBodyG(json.get("orbitalBodyG").getAsFloat());
        if (json.has("orbitalBodyB")) b.orbitalBodyB(json.get("orbitalBodyB").getAsFloat());
        
        // Orbital corona
        if (json.has("orbitalCoronaR")) b.orbitalCoronaR(json.get("orbitalCoronaR").getAsFloat());
        if (json.has("orbitalCoronaG")) b.orbitalCoronaG(json.get("orbitalCoronaG").getAsFloat());
        if (json.has("orbitalCoronaB")) b.orbitalCoronaB(json.get("orbitalCoronaB").getAsFloat());
        if (json.has("orbitalCoronaA")) b.orbitalCoronaA(json.get("orbitalCoronaA").getAsFloat());
        if (json.has("orbitalCoronaWidth")) b.orbitalCoronaWidth(json.get("orbitalCoronaWidth").getAsFloat());
        if (json.has("orbitalCoronaIntensity")) b.orbitalCoronaIntensity(json.get("orbitalCoronaIntensity").getAsFloat());
        if (json.has("orbitalRimPower")) b.orbitalRimPower(json.get("orbitalRimPower").getAsFloat());
        if (json.has("orbitalRimFalloff")) b.orbitalRimFalloff(json.get("orbitalRimFalloff").getAsFloat());
        
        // Beam geometry
        if (json.has("beamHeight")) b.beamHeight(json.get("beamHeight").getAsFloat());
        if (json.has("beamWidth")) b.beamWidth(json.get("beamWidth").getAsFloat());
        if (json.has("beamWidthScale")) b.beamWidthScale(json.get("beamWidthScale").getAsFloat());
        if (json.has("beamTaper")) b.beamTaper(json.get("beamTaper").getAsFloat());
        
        // Beam body
        if (json.has("beamBodyR")) b.beamBodyR(json.get("beamBodyR").getAsFloat());
        if (json.has("beamBodyG")) b.beamBodyG(json.get("beamBodyG").getAsFloat());
        if (json.has("beamBodyB")) b.beamBodyB(json.get("beamBodyB").getAsFloat());
        
        // Beam corona
        if (json.has("beamCoronaR")) b.beamCoronaR(json.get("beamCoronaR").getAsFloat());
        if (json.has("beamCoronaG")) b.beamCoronaG(json.get("beamCoronaG").getAsFloat());
        if (json.has("beamCoronaB")) b.beamCoronaB(json.get("beamCoronaB").getAsFloat());
        if (json.has("beamCoronaA")) b.beamCoronaA(json.get("beamCoronaA").getAsFloat());
        if (json.has("beamCoronaWidth")) b.beamCoronaWidth(json.get("beamCoronaWidth").getAsFloat());
        if (json.has("beamCoronaIntensity")) b.beamCoronaIntensity(json.get("beamCoronaIntensity").getAsFloat());
        if (json.has("beamRimPower")) b.beamRimPower(json.get("beamRimPower").getAsFloat());
        if (json.has("beamRimFalloff")) b.beamRimFalloff(json.get("beamRimFalloff").getAsFloat());
        
        // Animation timing
        if (json.has("orbitalSpeed")) b.orbitalSpeed(json.get("orbitalSpeed").getAsFloat());
        if (json.has("orbitalSpawnDuration")) b.orbitalSpawnDuration(json.get("orbitalSpawnDuration").getAsFloat());
        if (json.has("orbitalRetractDuration")) b.orbitalRetractDuration(json.get("orbitalRetractDuration").getAsFloat());
        if (json.has("beamGrowDuration")) b.beamGrowDuration(json.get("beamGrowDuration").getAsFloat());
        if (json.has("beamShrinkDuration")) b.beamShrinkDuration(json.get("beamShrinkDuration").getAsFloat());
        if (json.has("beamHoldDuration")) b.beamHoldDuration(json.get("beamHoldDuration").getAsFloat());
        if (json.has("beamWidthGrowFactor")) b.beamWidthGrowFactor(json.get("beamWidthGrowFactor").getAsFloat());
        if (json.has("beamLengthGrowFactor")) b.beamLengthGrowFactor(json.get("beamLengthGrowFactor").getAsFloat());
        
        // Easing types
        if (json.has("orbitalSpawnEasing")) b.orbitalSpawnEasing(EasingType.valueOf(json.get("orbitalSpawnEasing").getAsString()));
        if (json.has("orbitalRetractEasing")) b.orbitalRetractEasing(EasingType.valueOf(json.get("orbitalRetractEasing").getAsString()));
        if (json.has("beamGrowEasing")) b.beamGrowEasing(EasingType.valueOf(json.get("beamGrowEasing").getAsString()));
        if (json.has("beamShrinkEasing")) b.beamShrinkEasing(EasingType.valueOf(json.get("beamShrinkEasing").getAsString()));
        
        // Delays
        if (json.has("orbitalSpawnDelay")) b.orbitalSpawnDelay(json.get("orbitalSpawnDelay").getAsFloat());
        if (json.has("beamStartDelay")) b.beamStartDelay(json.get("beamStartDelay").getAsFloat());
        if (json.has("retractDelay")) b.retractDelay(json.get("retractDelay").getAsFloat());
        if (json.has("autoRetractOnRingEnd")) b.autoRetractOnRingEnd(json.get("autoRetractOnRingEnd").getAsBoolean());
        
        // Screen effects
        if (json.has("blackout")) b.blackout(json.get("blackout").getAsFloat());
        if (json.has("vignetteAmount")) b.vignetteAmount(json.get("vignetteAmount").getAsFloat());
        if (json.has("vignetteRadius")) b.vignetteRadius(json.get("vignetteRadius").getAsFloat());
        if (json.has("tintR")) b.tintR(json.get("tintR").getAsFloat());
        if (json.has("tintG")) b.tintG(json.get("tintG").getAsFloat());
        if (json.has("tintB")) b.tintB(json.get("tintB").getAsFloat());
        if (json.has("tintAmount")) b.tintAmount(json.get("tintAmount").getAsFloat());
        
        // Blend
        if (json.has("blendRadius")) b.blendRadius(json.get("blendRadius").getAsFloat());
        
        // Global scale & positioning
        if (json.has("globalScale")) b.globalScale(json.get("globalScale").getAsFloat());
        if (json.has("followPosition")) b.followPosition(json.get("followPosition").getAsBoolean());
        
        config = b.build();
        syncToPostEffect();
        
        Logging.GUI.topic("adapter").debug("ShockwaveAdapter loaded from JSON");
    }
}
