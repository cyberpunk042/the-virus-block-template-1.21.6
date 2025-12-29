package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Kamehameha shape - energy beam attack with charging orb and extending beam.
 * 
 * <h2>Components</h2>
 * <p>A Kamehameha consists of two main components, each with independent configuration:</p>
 * 
 * <h3>ORB (Charging Sphere)</h3>
 * <ul>
 *   <li>Energy type (CLASSIC, RASENGAN, GHOST, FROST, LIGHTNING, WATER, FIRE, UNSTABLE, SPIKED...)</li>
 *   <li>Transition style (FADE, SCALE, FADE_AND_SCALE)</li>
 *   <li>Size and resolution settings</li>
 * </ul>
 * 
 * <h3>BEAM (Energy Stream)</h3>
 * <ul>
 *   <li>Energy type (can differ from orb)</li>
 *   <li>Transition style</li>
 *   <li>Scale axis (LENGTH, WIDTH, BOTH, staged combinations)</li>
 *   <li>Length, radius, and resolution settings</li>
 * </ul>
 * 
 * <h2>Geometry</h2>
 * <pre>
 *                    ┌─────────────────────────────────
 *                   ╱                                   ╲  beamTipRadius
 *     orbRadius    ╱                                     ╲
 *       ╭───╮     ╱ ────────────────────────────────────  ╲
 *      (  ●  )═══<        BEAM (beamLength)                )  → direction
 *       ╰───╯     ╲ ────────────────────────────────────  ╱
 *                  ╲                                     ╱
 *                   ╲                                   ╱  beamTipRadius
 *                    └─────────────────────────────────
 *        ORB                    BEAM
 * </pre>
 * 
 * <h2>Lifecycle Integration</h2>
 * <p>The orb and beam progress fields (0-1) are controlled by LifecycleAnimator:</p>
 * <ul>
 *   <li><b>CHARGE</b> - Orb progress 0→1, beam progress 0</li>
 *   <li><b>FIRE</b> - Orb progress 1, beam progress 0→1</li>
 *   <li><b>SUSTAIN</b> - Both at 1</li>
 *   <li><b>RETRACT</b> - Beam 1→0, then orb 1→0</li>
 * </ul>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>orb</b> (QUAD) - Main charging sphere</li>
 *   <li><b>orbCore</b> (QUAD) - Inner orb core (if hasCore)</li>
 *   <li><b>orbAura</b> (QUAD) - Outer orb glow (if hasAura)</li>
 *   <li><b>beam</b> (QUAD) - Main beam cylinder</li>
 *   <li><b>beamCore</b> (QUAD) - Inner intense beam (if hasCore)</li>
 *   <li><b>beamTip</b> (TRIANGLE/QUAD) - Beam end cap</li>
 *   <li><b>beamAura</b> (QUAD) - Outer beam glow (if hasAura)</li>
 * </ul>
 * 
 * @see EnergyType
 * @see TransitionStyle
 * @see BeamScaleAxis
 * @see net.cyberpunk042.visual.animation.LifecycleAnimator
 */
public record KamehamehaShape(
    // =========================================================================
    // ORB CONFIGURATION
    // =========================================================================
    
    /** Energy visual type for the charging orb. */
    @JsonField(skipIfDefault = true)
    EnergyType orbType,
    
    /** How the orb transitions in/out during lifecycle. */
    @JsonField(skipIfDefault = true)
    TransitionStyle orbTransition,
    
    /** Radius of the charging orb at full charge. */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "0.3")
    float orbRadius,
    
    /** Resolution of the orb (segments). */
    @Range(ValueRange.STEPS) 
    @JsonField(skipIfDefault = true, defaultValue = "16")
    int orbSegments,
    
    /** Orb animation progress (0 = invisible, 1 = full). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "1.0")
    float orbProgress,
    
    // =========================================================================
    // BEAM CONFIGURATION
    // =========================================================================
    
    /** Energy visual type for the beam. */
    @JsonField(skipIfDefault = true)
    EnergyType beamType,
    
    /** How the beam transitions in/out during lifecycle. */
    @JsonField(skipIfDefault = true)
    TransitionStyle beamTransition,
    
    /** Which dimension(s) scale during beam transition. */
    @JsonField(skipIfDefault = true)
    BeamScaleAxis beamScaleAxis,
    
    /** Length of the beam at full extension. */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "5.0")
    float beamLength,
    
    /** Radius of the beam at the base (orb connection). */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "0.25")
    float beamBaseRadius,
    
    /** Radius of the beam at the far tip. */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "0.2")
    float beamTipRadius,
    
    /** Number of radial segments around the beam. */
    @Range(ValueRange.STEPS) 
    @JsonField(skipIfDefault = true, defaultValue = "16")
    int beamSegments,
    
    /** Number of segments along the beam length. */
    @Range(ValueRange.STEPS) 
    @JsonField(skipIfDefault = true, defaultValue = "8")
    int beamLengthSegments,
    
    /** Beam animation progress (0 = invisible, 1 = full). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "1.0")
    float beamProgress,
    
    // =========================================================================
    // BEAM TIP STYLE
    // =========================================================================
    
    /** Shape of the beam tip. */
    @JsonField(skipIfDefault = true)
    TipStyle tipStyle,
    
    // =========================================================================
    // CORE CONFIGURATION (inner intense layer)
    // =========================================================================
    
    /** Whether to render inner core layers. */
    @JsonField(skipIfDefault = true)
    boolean hasCore,
    
    /** Core size relative to main (0-1). */
    @Range(ValueRange.NORMALIZED) 
    @JsonField(skipIfDefault = true, defaultValue = "0.4")
    float coreRatio,
    
    /** Core brightness multiplier. */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "1.5")
    float coreBrightness,
    
    // =========================================================================
    // AURA CONFIGURATION (outer glow layer)
    // =========================================================================
    
    /** Whether to render outer aura layers. */
    @JsonField(skipIfDefault = true)
    boolean hasAura,
    
    /** Aura size relative to main. */
    @Range(ValueRange.POSITIVE) 
    @JsonField(skipIfDefault = true, defaultValue = "1.3")
    float auraScale,
    
    /** Aura alpha multiplier. */
    @Range(ValueRange.NORMALIZED) 
    @JsonField(skipIfDefault = true, defaultValue = "0.3")
    float auraAlpha
    
) implements Shape {
    
    // =========================================================================
    // Tip Styles
    // =========================================================================
    
    public enum TipStyle {
        /** Sharp pointed tip (cone). */
        POINTED("Pointed", true),
        /** Hemispherical rounded tip. */
        ROUNDED("Rounded", false),
        /** Flat cut-off tip. */
        FLAT("Flat", false),
        /** Swirling vortex tip. */
        VORTEX("Vortex", false);
        
        private final String displayName;
        private final boolean usesTriangles;
        
        TipStyle(String displayName, boolean usesTriangles) {
            this.displayName = displayName;
            this.usesTriangles = usesTriangles;
        }
        
        public String displayName() { return displayName; }
        public boolean usesTriangles() { return usesTriangles; }
    }
    
    // =========================================================================
    // Constants & Presets
    // =========================================================================
    
    /** Default Kamehameha (classic blue energy beam). */
    public static final KamehamehaShape DEFAULT = new KamehamehaShape(
        // Orb
        EnergyType.CLASSIC, TransitionStyle.FADE_AND_SCALE, 0.3f, 16, 1.0f,
        // Beam
        EnergyType.CLASSIC, TransitionStyle.SCALE, BeamScaleAxis.LENGTH, 
        5.0f, 0.25f, 0.2f, 16, 8, 1.0f,
        // Tip
        TipStyle.ROUNDED,
        // Core
        true, 0.4f, 1.5f,
        // Aura
        true, 1.3f, 0.3f
    );
    
    /** Rasengan orb (no beam, just spiraling sphere). */
    public static final KamehamehaShape RASENGAN = new KamehamehaShape(
        EnergyType.RASENGAN, TransitionStyle.SCALE, 0.25f, 24, 1.0f,
        EnergyType.RASENGAN, TransitionStyle.SCALE, BeamScaleAxis.BOTH,
        0.0f, 0.0f, 0.0f, 16, 1, 0.0f,  // No beam
        TipStyle.ROUNDED,
        true, 0.5f, 2.0f,
        true, 1.4f, 0.4f
    );
    
    /** Lightning bolt attack. */
    public static final KamehamehaShape LIGHTNING_BOLT = new KamehamehaShape(
        EnergyType.LIGHTNING, TransitionStyle.FADE, 0.15f, 12, 1.0f,
        EnergyType.LIGHTNING, TransitionStyle.FADE, BeamScaleAxis.LENGTH,
        8.0f, 0.1f, 0.05f, 8, 12, 1.0f,
        TipStyle.POINTED,
        true, 0.3f, 2.0f,
        true, 1.5f, 0.5f
    );
    
    /** Fire blast. */
    public static final KamehamehaShape FIRE_BLAST = new KamehamehaShape(
        EnergyType.FIRE, TransitionStyle.FADE_AND_SCALE, 0.4f, 16, 1.0f,
        EnergyType.FIRE, TransitionStyle.SCALE, BeamScaleAxis.WIDTH_THEN_LENGTH,
        4.0f, 0.35f, 0.5f, 16, 6, 1.0f,
        TipStyle.ROUNDED,
        true, 0.6f, 1.8f,
        true, 1.4f, 0.4f
    );
    
    /** Frost beam. */
    public static final KamehamehaShape FROST_BEAM = new KamehamehaShape(
        EnergyType.FROST, TransitionStyle.SCALE, 0.2f, 8, 1.0f,
        EnergyType.FROST, TransitionStyle.SCALE, BeamScaleAxis.LENGTH,
        6.0f, 0.15f, 0.1f, 6, 10, 1.0f,
        TipStyle.POINTED,
        false, 0.0f, 1.0f,
        true, 1.2f, 0.2f
    );
    
    /** Unstable overloaded attack. */
    public static final KamehamehaShape UNSTABLE_OVERLOAD = new KamehamehaShape(
        EnergyType.UNSTABLE, TransitionStyle.FADE_AND_SCALE, 0.5f, 20, 1.0f,
        EnergyType.UNSTABLE, TransitionStyle.FADE_AND_SCALE, BeamScaleAxis.BOTH,
        3.0f, 0.4f, 0.6f, 16, 4, 1.0f,
        TipStyle.VORTEX,
        true, 0.7f, 2.5f,
        true, 1.6f, 0.5f
    );
    
    /** Ghost spirit attack. */
    public static final KamehamehaShape GHOST_WAVE = new KamehamehaShape(
        EnergyType.GHOST, TransitionStyle.FADE, 0.35f, 16, 1.0f,
        EnergyType.GHOST, TransitionStyle.FADE, BeamScaleAxis.LENGTH,
        7.0f, 0.3f, 0.25f, 16, 10, 1.0f,
        TipStyle.ROUNDED,
        false, 0.0f, 1.0f,
        true, 1.5f, 0.2f
    );
    
    /** Void/dark energy. */
    public static final KamehamehaShape VOID_BEAM = new KamehamehaShape(
        EnergyType.VOID, TransitionStyle.SCALE, 0.4f, 16, 1.0f,
        EnergyType.VOID, TransitionStyle.SCALE, BeamScaleAxis.BOTH,
        6.0f, 0.35f, 0.4f, 16, 8, 1.0f,
        TipStyle.VORTEX,
        true, 0.8f, 0.5f,  // Dark core
        true, 1.4f, 0.4f
    );
    
    /** Charging state (orb only, no beam). */
    public static final KamehamehaShape CHARGING = DEFAULT.toBuilder()
        .orbProgress(0.5f)
        .beamProgress(0.0f)
        .build();
    
    public static KamehamehaShape defaults() { return DEFAULT; }
    
    // =========================================================================
    // Shape Interface
    // =========================================================================
    
    @Override
    public String getType() {
        return "kamehameha";
    }
    
    @Override
    public Vector3f getBounds() {
        float effectiveBeamLength = effectiveBeamLength();
        float maxRadius = Math.max(effectiveOrbRadius(), Math.max(effectiveBeamBaseRadius(), effectiveBeamTipRadius()));
        if (hasAura) maxRadius *= auraScale;
        return new Vector3f(maxRadius * 2, effectiveBeamLength + effectiveOrbRadius() * 2, maxRadius * 2);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.QUAD;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        var parts = new java.util.LinkedHashMap<String, CellType>();
        
        // Orb parts
        parts.put("orb", CellType.QUAD);
        if (hasCore) parts.put("orbCore", CellType.QUAD);
        if (hasAura) parts.put("orbAura", CellType.QUAD);
        
        // Beam parts (only if beam has length)
        if (beamLength > 0) {
            parts.put("beam", CellType.QUAD);
            if (hasCore) parts.put("beamCore", CellType.QUAD);
            parts.put("beamTip", tipStyle.usesTriangles() ? CellType.TRIANGLE : CellType.QUAD);
            if (hasAura) parts.put("beamAura", CellType.QUAD);
        }
        
        return parts;
    }
    
    @Override
    public float getRadius() {
        float r = Math.max(orbRadius, beamBaseRadius);
        return hasAura ? r * auraScale : r;
    }
    
    // =========================================================================
    // Computed Properties (for rendering)
    // =========================================================================
    
    /** Effective orb radius after applying progress and transition. */
    public float effectiveOrbRadius() {
        if (!orbTransition.affectsScale()) return orbRadius;
        return orbRadius * orbProgress;
    }
    
    /** Effective orb alpha after applying progress and transition. */
    public float effectiveOrbAlpha() {
        float base = orbType.baseAlpha();
        if (!orbTransition.affectsAlpha()) return base;
        return base * orbProgress;
    }
    
    /** Effective beam length after applying progress and scale axis. */
    public float effectiveBeamLength() {
        return beamLength * beamScaleAxis.lengthScale(beamProgress);
    }
    
    /** Effective beam base radius after applying progress and scale axis. */
    public float effectiveBeamBaseRadius() {
        return beamBaseRadius * beamScaleAxis.widthScale(beamProgress);
    }
    
    /** Effective beam tip radius after applying progress and scale axis. */
    public float effectiveBeamTipRadius() {
        return beamTipRadius * beamScaleAxis.widthScale(beamProgress);
    }
    
    /** Effective beam alpha after applying progress and transition. */
    public float effectiveBeamAlpha() {
        float base = beamType.baseAlpha();
        if (!beamTransition.affectsAlpha()) return base;
        return base * beamProgress;
    }
    
    /** Whether the orb is currently visible. */
    public boolean isOrbVisible() {
        return orbProgress > 0.001f && orbRadius > 0;
    }
    
    /** Whether the beam is currently visible. */
    public boolean isBeamVisible() {
        return beamProgress > 0.001f && beamLength > 0;
    }
    
    /** Get core radius for orb. */
    public float orbCoreRadius() {
        return effectiveOrbRadius() * coreRatio;
    }
    
    /** Get core radius for beam. */
    public float beamCoreRadius() {
        return effectiveBeamBaseRadius() * coreRatio;
    }
    
    /** Get aura radius for orb. */
    public float orbAuraRadius() {
        return effectiveOrbRadius() * auraScale;
    }
    
    // =========================================================================
    // JSON
    // =========================================================================
    
    @Override
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    public static KamehamehaShape fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return builder()
            // Orb
            .orbType(json.has("orbType") ? EnergyType.valueOf(json.get("orbType").getAsString()) : EnergyType.CLASSIC)
            .orbTransition(json.has("orbTransition") ? TransitionStyle.valueOf(json.get("orbTransition").getAsString()) : TransitionStyle.FADE_AND_SCALE)
            .orbRadius(json.has("orbRadius") ? json.get("orbRadius").getAsFloat() : 0.3f)
            .orbSegments(json.has("orbSegments") ? json.get("orbSegments").getAsInt() : 16)
            .orbProgress(json.has("orbProgress") ? json.get("orbProgress").getAsFloat() : 1.0f)
            // Beam
            .beamType(json.has("beamType") ? EnergyType.valueOf(json.get("beamType").getAsString()) : EnergyType.CLASSIC)
            .beamTransition(json.has("beamTransition") ? TransitionStyle.valueOf(json.get("beamTransition").getAsString()) : TransitionStyle.SCALE)
            .beamScaleAxis(json.has("beamScaleAxis") ? BeamScaleAxis.valueOf(json.get("beamScaleAxis").getAsString()) : BeamScaleAxis.LENGTH)
            .beamLength(json.has("beamLength") ? json.get("beamLength").getAsFloat() : 5.0f)
            .beamBaseRadius(json.has("beamBaseRadius") ? json.get("beamBaseRadius").getAsFloat() : 0.25f)
            .beamTipRadius(json.has("beamTipRadius") ? json.get("beamTipRadius").getAsFloat() : 0.2f)
            .beamSegments(json.has("beamSegments") ? json.get("beamSegments").getAsInt() : 16)
            .beamLengthSegments(json.has("beamLengthSegments") ? json.get("beamLengthSegments").getAsInt() : 8)
            .beamProgress(json.has("beamProgress") ? json.get("beamProgress").getAsFloat() : 1.0f)
            // Tip
            .tipStyle(json.has("tipStyle") ? TipStyle.valueOf(json.get("tipStyle").getAsString()) : TipStyle.ROUNDED)
            // Core
            .hasCore(json.has("hasCore") && json.get("hasCore").getAsBoolean())
            .coreRatio(json.has("coreRatio") ? json.get("coreRatio").getAsFloat() : 0.4f)
            .coreBrightness(json.has("coreBrightness") ? json.get("coreBrightness").getAsFloat() : 1.5f)
            // Aura
            .hasAura(json.has("hasAura") && json.get("hasAura").getAsBoolean())
            .auraScale(json.has("auraScale") ? json.get("auraScale").getAsFloat() : 1.3f)
            .auraAlpha(json.has("auraAlpha") ? json.get("auraAlpha").getAsFloat() : 0.3f)
            .build();
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            // Orb
            .orbType(orbType).orbTransition(orbTransition).orbRadius(orbRadius)
            .orbSegments(orbSegments).orbProgress(orbProgress)
            // Beam
            .beamType(beamType).beamTransition(beamTransition).beamScaleAxis(beamScaleAxis)
            .beamLength(beamLength).beamBaseRadius(beamBaseRadius).beamTipRadius(beamTipRadius)
            .beamSegments(beamSegments).beamLengthSegments(beamLengthSegments).beamProgress(beamProgress)
            // Tip
            .tipStyle(tipStyle)
            // Core
            .hasCore(hasCore).coreRatio(coreRatio).coreBrightness(coreBrightness)
            // Aura
            .hasAura(hasAura).auraScale(auraScale).auraAlpha(auraAlpha);
    }
    
    public static class Builder {
        // Orb
        private EnergyType orbType = EnergyType.CLASSIC;
        private TransitionStyle orbTransition = TransitionStyle.FADE_AND_SCALE;
        private float orbRadius = 0.3f;
        private int orbSegments = 16;
        private float orbProgress = 1.0f;
        // Beam
        private EnergyType beamType = EnergyType.CLASSIC;
        private TransitionStyle beamTransition = TransitionStyle.SCALE;
        private BeamScaleAxis beamScaleAxis = BeamScaleAxis.LENGTH;
        private float beamLength = 5.0f;
        private float beamBaseRadius = 0.25f;
        private float beamTipRadius = 0.2f;
        private int beamSegments = 16;
        private int beamLengthSegments = 8;
        private float beamProgress = 1.0f;
        // Tip
        private TipStyle tipStyle = TipStyle.ROUNDED;
        // Core
        private boolean hasCore = true;
        private float coreRatio = 0.4f;
        private float coreBrightness = 1.5f;
        // Aura
        private boolean hasAura = true;
        private float auraScale = 1.3f;
        private float auraAlpha = 0.3f;
        
        // Orb setters
        public Builder orbType(EnergyType v) { this.orbType = v != null ? v : EnergyType.CLASSIC; return this; }
        public Builder orbTransition(TransitionStyle v) { this.orbTransition = v != null ? v : TransitionStyle.FADE_AND_SCALE; return this; }
        public Builder orbRadius(float v) { this.orbRadius = v; return this; }
        public Builder orbSegments(int v) { this.orbSegments = v; return this; }
        public Builder orbProgress(float v) { this.orbProgress = v; return this; }
        
        // Beam setters
        public Builder beamType(EnergyType v) { this.beamType = v != null ? v : EnergyType.CLASSIC; return this; }
        public Builder beamTransition(TransitionStyle v) { this.beamTransition = v != null ? v : TransitionStyle.SCALE; return this; }
        public Builder beamScaleAxis(BeamScaleAxis v) { this.beamScaleAxis = v != null ? v : BeamScaleAxis.LENGTH; return this; }
        public Builder beamLength(float v) { this.beamLength = v; return this; }
        public Builder beamBaseRadius(float v) { this.beamBaseRadius = v; return this; }
        public Builder beamTipRadius(float v) { this.beamTipRadius = v; return this; }
        public Builder beamSegments(int v) { this.beamSegments = v; return this; }
        public Builder beamLengthSegments(int v) { this.beamLengthSegments = v; return this; }
        public Builder beamProgress(float v) { this.beamProgress = v; return this; }
        
        // Tip setter
        public Builder tipStyle(TipStyle v) { this.tipStyle = v != null ? v : TipStyle.ROUNDED; return this; }
        
        // Core setters
        public Builder hasCore(boolean v) { this.hasCore = v; return this; }
        public Builder coreRatio(float v) { this.coreRatio = v; return this; }
        public Builder coreBrightness(float v) { this.coreBrightness = v; return this; }
        
        // Aura setters
        public Builder hasAura(boolean v) { this.hasAura = v; return this; }
        public Builder auraScale(float v) { this.auraScale = v; return this; }
        public Builder auraAlpha(float v) { this.auraAlpha = v; return this; }
        
        public KamehamehaShape build() {
            return new KamehamehaShape(
                orbType, orbTransition, orbRadius, orbSegments, orbProgress,
                beamType, beamTransition, beamScaleAxis,
                beamLength, beamBaseRadius, beamTipRadius, beamSegments, beamLengthSegments, beamProgress,
                tipStyle,
                hasCore, coreRatio, coreBrightness,
                hasAura, auraScale, auraAlpha
            );
        }
    }
}
