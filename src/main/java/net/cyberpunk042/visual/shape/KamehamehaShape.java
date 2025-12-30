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
 *   <li><b>beam</b> (QUAD) - Main beam cylinder</li>
 *   <li><b>beamTip</b> (QUAD) - Beam end cap (dome or flat)</li>
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
    // PROPORTIONAL SIZING
    // =========================================================================
    
    /** If true, beam base radius is automatically derived from orb radius * beamToOrbRatio. */
    @JsonField(skipIfDefault = true)
    boolean proportionalBeam,
    
    /** Ratio of beam base radius to orb radius when proportionalBeam is true. */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "0.8")
    float beamToOrbRatio,
    
    // =========================================================================
    // BEAM TIP STYLE
    // =========================================================================
    
    /** Whether the beam tip has a dome (hemisphere) cap. If false, the tip is flat. */
    @JsonField(skipIfDefault = true, defaultValue = "true")
    boolean hasDomeTip,
    
    // =========================================================================
    // ALPHA GRADIENT CONFIGURATION
    // =========================================================================
    
    /** Orb alpha (standard opacity). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "1.0")
    float orbAlpha,
    
    /** Orb minimum alpha (floor for travel effects). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "0.0")
    float orbMinAlpha,
    
    /** Beam base alpha at orb connection. */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "1.0")
    float beamBaseAlpha,
    
    /** Beam base minimum alpha (floor for travel effects). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "0.0")
    float beamBaseMinAlpha,
    
    /** Beam tip alpha at far end. */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "0.8")
    float beamTipAlpha,
    
    /** Beam tip minimum alpha (floor for travel effects). */
    @Range(ValueRange.NORMALIZED)
    @JsonField(skipIfDefault = true, defaultValue = "0.0")
    float beamTipMinAlpha,
    
    // =========================================================================
    // ORIENTATION CONFIGURATION
    // =========================================================================
    
    /** Axis along which the beam extends. Default is +Z (forward/horizontal). */
    @JsonField(skipIfDefault = true)
    OrientationAxis orientationAxis,
    
    /** Offset distance from origin along the beam axis. Positive = further from player. */
    @JsonField(skipIfDefault = true, defaultValue = "0.0")
    float originOffset
    
) implements Shape {
    
    // TipStyle enum removed - use hasDomeTip boolean instead
    
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
        // Proportional Sizing
        true, 0.8f,
        // Tip (true = dome/hemisphere, false = flat)
        true,
        // Alpha gradient
        1.0f, 0.2f,   // orbAlpha, orbMinAlpha
        1.0f, 0.1f,   // beamBaseAlpha, beamBaseMinAlpha
        0.6f, 0.0f,   // beamTipAlpha, beamTipMinAlpha
        // Orientation
        OrientationAxis.POS_Z, 0f
    );
    
    /** Rasengan orb (no beam, just spiraling sphere). */
    public static final KamehamehaShape RASENGAN = new KamehamehaShape(
        EnergyType.RASENGAN, TransitionStyle.SCALE, 0.25f, 24, 1.0f,
        EnergyType.RASENGAN, TransitionStyle.SCALE, BeamScaleAxis.BOTH,
        0.0f, 0.0f, 0.0f, 16, 1, 0.0f,  // No beam
        false, 0.8f,
        true,
        1.0f, 0.3f, 1.0f, 0.0f, 0.0f, 0.0f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Lightning bolt attack. */
    public static final KamehamehaShape LIGHTNING_BOLT = new KamehamehaShape(
        EnergyType.LIGHTNING, TransitionStyle.FADE, 0.15f, 12, 1.0f,
        EnergyType.LIGHTNING, TransitionStyle.FADE, BeamScaleAxis.LENGTH,
        8.0f, 0.1f, 0.05f, 8, 12, 1.0f,
        false, 0.8f,
        false,  // No dome tip (flat cut-off for lightning)
        0.8f, 0.1f, 1.0f, 0.2f, 0.3f, 0.0f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Fire blast. */
    public static final KamehamehaShape FIRE_BLAST = new KamehamehaShape(
        EnergyType.FIRE, TransitionStyle.FADE_AND_SCALE, 0.4f, 16, 1.0f,
        EnergyType.FIRE, TransitionStyle.SCALE, BeamScaleAxis.WIDTH_THEN_LENGTH,
        4.0f, 0.35f, 0.5f, 16, 6, 1.0f,
        false, 0.8f,
        true,
        1.0f, 0.3f, 0.9f, 0.2f, 0.7f, 0.1f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Frost beam. */
    public static final KamehamehaShape FROST_BEAM = new KamehamehaShape(
        EnergyType.FROST, TransitionStyle.SCALE, 0.2f, 8, 1.0f,
        EnergyType.FROST, TransitionStyle.SCALE, BeamScaleAxis.LENGTH,
        6.0f, 0.15f, 0.1f, 6, 10, 1.0f,
        false, 0.8f,
        false,  // No dome (flat cut)
        0.9f, 0.1f, 0.8f, 0.1f, 0.4f, 0.0f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Unstable overloaded attack. */
    public static final KamehamehaShape UNSTABLE_OVERLOAD = new KamehamehaShape(
        EnergyType.UNSTABLE, TransitionStyle.FADE_AND_SCALE, 0.5f, 20, 1.0f,
        EnergyType.UNSTABLE, TransitionStyle.FADE_AND_SCALE, BeamScaleAxis.BOTH,
        3.0f, 0.4f, 0.6f, 16, 4, 1.0f,
        false, 0.8f,
        true,
        1.0f, 0.4f, 1.0f, 0.3f, 0.8f, 0.2f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Ghost spirit attack. */
    public static final KamehamehaShape GHOST_WAVE = new KamehamehaShape(
        EnergyType.GHOST, TransitionStyle.FADE, 0.35f, 16, 1.0f,
        EnergyType.GHOST, TransitionStyle.FADE, BeamScaleAxis.LENGTH,
        7.0f, 0.3f, 0.25f, 16, 10, 1.0f,
        false, 0.8f,
        true,
        0.6f, 0.1f, 0.5f, 0.0f, 0.2f, 0.0f,
        OrientationAxis.POS_Z, 0f
    );
    
    /** Void/dark energy. */
    public static final KamehamehaShape VOID_BEAM = new KamehamehaShape(
        EnergyType.VOID, TransitionStyle.SCALE, 0.4f, 16, 1.0f,
        EnergyType.VOID, TransitionStyle.SCALE, BeamScaleAxis.BOTH,
        6.0f, 0.35f, 0.4f, 16, 8, 1.0f,
        false, 0.8f,
        true,
        0.8f, 0.3f, 0.7f, 0.2f, 0.5f, 0.1f,
        OrientationAxis.POS_Z, 0f
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
        
        // Beam parts (only if beam has length)
        if (beamLength > 0) {
            parts.put("beam", CellType.QUAD);
            parts.put("beamTip", hasDomeTip ? CellType.QUAD : CellType.QUAD);
        }
        
        return parts;
    }
    
    @Override
    public float getRadius() {
        return Math.max(orbRadius, beamBaseRadius);
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
    
    /** Effective beam base radius after applying progress, scale axis, and proportional sizing. */
    public float effectiveBeamBaseRadius() {
        float baseR = proportionalBeam ? (orbRadius * beamToOrbRatio) : beamBaseRadius;
        return baseR * beamScaleAxis.widthScale(beamProgress);
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
    
    // =========================================================================
    // Combined Progress (UX Helper)
    // =========================================================================
    
    /**
     * Gets the combined progress value [0, 2].
     * <ul>
     *   <li>0.0 - 1.0: Orb charging (beam invisible)</li>
     *   <li>1.0 - 2.0: Beam extending (orb at full)</li>
     * </ul>
     */
    public float combinedProgress() {
        // If orb isn't full yet, progress is just orbProgress
        if (orbProgress < 1.0f) {
            return orbProgress;
        }
        // Orb is full, add beam progress
        return 1.0f + beamProgress;
    }
    
    /**
     * Creates a new shape with the combined progress value applied.
     * <ul>
     *   <li>0.0 - 1.0: Sets orbProgress, beamProgress = 0</li>
     *   <li>1.0 - 2.0: Sets orbProgress = 1, beamProgress = value - 1</li>
     * </ul>
     * 
     * @param combined Progress value from 0 to 2
     * @return New shape with appropriate orb/beam progress
     */
    public KamehamehaShape withCombinedProgress(float combined) {
        combined = Math.max(0f, Math.min(2f, combined));
        if (combined <= 1.0f) {
            return toBuilder().orbProgress(combined).beamProgress(0f).build();
        } else {
            return toBuilder().orbProgress(1f).beamProgress(combined - 1f).build();
        }
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
            // Proportional Sizing
            .proportionalBeam(!json.has("proportionalBeam") || json.get("proportionalBeam").getAsBoolean())
            .beamToOrbRatio(json.has("beamToOrbRatio") ? json.get("beamToOrbRatio").getAsFloat() : 0.8f)
            // Tip
            .hasDomeTip(!json.has("hasDomeTip") || json.get("hasDomeTip").getAsBoolean())
            // Alpha gradient
            .orbAlpha(json.has("orbAlpha") ? json.get("orbAlpha").getAsFloat() : 1.0f)
            .orbMinAlpha(json.has("orbMinAlpha") ? json.get("orbMinAlpha").getAsFloat() : 0.0f)
            .beamBaseAlpha(json.has("beamBaseAlpha") ? json.get("beamBaseAlpha").getAsFloat() : 1.0f)
            .beamBaseMinAlpha(json.has("beamBaseMinAlpha") ? json.get("beamBaseMinAlpha").getAsFloat() : 0.0f)
            .beamTipAlpha(json.has("beamTipAlpha") ? json.get("beamTipAlpha").getAsFloat() : 0.8f)
            .beamTipMinAlpha(json.has("beamTipMinAlpha") ? json.get("beamTipMinAlpha").getAsFloat() : 0.0f)
            // Orientation
            .orientationAxis(json.has("orientationAxis") ? OrientationAxis.valueOf(json.get("orientationAxis").getAsString()) : OrientationAxis.POS_Z)
            .originOffset(json.has("originOffset") ? json.get("originOffset").getAsFloat() : 0.0f)
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
            // Proportional Sizing
            .proportionalBeam(proportionalBeam).beamToOrbRatio(beamToOrbRatio)
            // Tip
            .hasDomeTip(hasDomeTip)
            // Alpha gradient
            .orbAlpha(orbAlpha).orbMinAlpha(orbMinAlpha)
            .beamBaseAlpha(beamBaseAlpha).beamBaseMinAlpha(beamBaseMinAlpha)
            .beamTipAlpha(beamTipAlpha).beamTipMinAlpha(beamTipMinAlpha)
            // Orientation
            .orientationAxis(orientationAxis).originOffset(originOffset);
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
        // Proportional Sizing
        private boolean proportionalBeam = true;
        private float beamToOrbRatio = 0.8f;
        // Tip
        private boolean hasDomeTip = true;
        // Alpha gradient
        private float orbAlpha = 1.0f;
        private float orbMinAlpha = 0.0f;
        private float beamBaseAlpha = 1.0f;
        private float beamBaseMinAlpha = 0.0f;
        private float beamTipAlpha = 1.0f;
        private float beamTipMinAlpha = 0.0f;
        // Orientation
        private OrientationAxis orientationAxis = OrientationAxis.POS_Z;
        private float originOffset = 0.0f;
        
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
        
        // Proportional Sizing setters
        public Builder proportionalBeam(boolean v) { this.proportionalBeam = v; return this; }
        public Builder beamToOrbRatio(float v) { this.beamToOrbRatio = Math.max(0.1f, Math.min(1.5f, v)); return this; }
        
        // Tip setter
        public Builder hasDomeTip(boolean v) { this.hasDomeTip = v; return this; }
        
        // Alpha gradient setters
        public Builder orbAlpha(float v) { this.orbAlpha = v; return this; }
        public Builder orbMinAlpha(float v) { this.orbMinAlpha = v; return this; }
        public Builder beamBaseAlpha(float v) { this.beamBaseAlpha = v; return this; }
        public Builder beamBaseMinAlpha(float v) { this.beamBaseMinAlpha = v; return this; }
        public Builder beamTipAlpha(float v) { this.beamTipAlpha = v; return this; }
        public Builder beamTipMinAlpha(float v) { this.beamTipMinAlpha = v; return this; }
        
        // Orientation setters
        public Builder orientationAxis(OrientationAxis v) { this.orientationAxis = v != null ? v : OrientationAxis.POS_Z; return this; }
        public Builder originOffset(float v) { this.originOffset = v; return this; }
        
        public KamehamehaShape build() {
            return new KamehamehaShape(
                orbType, orbTransition, orbRadius, orbSegments, orbProgress,
                beamType, beamTransition, beamScaleAxis,
                beamLength, beamBaseRadius, beamTipRadius, beamSegments, beamLengthSegments, beamProgress,
                proportionalBeam, beamToOrbRatio,
                hasDomeTip,
                orbAlpha, orbMinAlpha, beamBaseAlpha, beamBaseMinAlpha, beamTipAlpha, beamTipMinAlpha,
                orientationAxis, originOffset
            );
        }
    }
}
