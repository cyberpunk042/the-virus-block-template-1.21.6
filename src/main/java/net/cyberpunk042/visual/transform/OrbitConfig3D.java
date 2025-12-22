package net.cyberpunk042.visual.transform;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 3D orbital motion configuration with independent per-axis control.
 * 
 * <p>Enables atom-like orbital motion by configuring independent motion
 * on each axis (X, Y, Z). Each axis can have a different motion mode,
 * amplitude, frequency, and phase - allowing complex 3D orbital patterns.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "orbit3d": {
 *   "enabled": true,
 *   "x": { "mode": "CIRCULAR", "amplitude": 2.0, "frequency": 0.5, "phase": 0.0 },
 *   "y": { "mode": "WAVE", "amplitude": 0.3, "frequency": 1.0, "phase": 0.25 },
 *   "z": { "mode": "CIRCULAR", "amplitude": 2.0, "frequency": 0.5, "phase": 0.25 },
 *   "coupling": "INDEPENDENT"
 * }
 * </pre>
 * 
 * <h2>Atom Orbital Examples</h2>
 * <ul>
 *   <li><b>Horizontal orbit</b>: X=CIRCULAR, Y=NONE, Z=CIRCULAR (with 0.25 phase offset)</li>
 *   <li><b>Vertical orbit</b>: X=CIRCULAR, Y=CIRCULAR, Z=NONE</li>
 *   <li><b>3D helix</b>: X=CIRCULAR, Y=WAVE, Z=CIRCULAR</li>
 *   <li><b>Figure-8</b>: X=FIGURE_8, Y=WAVE, Z=WAVE</li>
 *   <li><b>Electron cloud</b>: All axes WAVE with different frequencies</li>
 * </ul>
 * 
 * <h2>Coupling Modes</h2>
 * <ul>
 *   <li><b>INDEPENDENT</b>: Each axis calculates motion independently</li>
 *   <li><b>LINKED_XZ</b>: X and Z form a coupled circular pair (typical horizontal orbit)</li>
 *   <li><b>LINKED_XY</b>: X and Y form a coupled pair (front-facing orbit)</li>
 *   <li><b>LINKED_YZ</b>: Y and Z form a coupled pair (side-facing orbit)</li>
 * </ul>
 * 
 * @see AxisMotionConfig
 * @see MotionMode
 * @see OrbitConfig (legacy single-axis config)
 */
public record OrbitConfig3D(
    @JsonField(skipIfDefault = true) boolean enabled,
    @Nullable @JsonField(skipIfNull = true) AxisMotionConfig x,
    @Nullable @JsonField(skipIfNull = true) AxisMotionConfig y,
    @Nullable @JsonField(skipIfNull = true) AxisMotionConfig z,
    @JsonField(skipIfDefault = true) CouplingMode coupling
) {
    /**
     * How axes interact for coupled motion.
     */
    public enum CouplingMode {
        /** Each axis is fully independent. */
        INDEPENDENT("Independent"),
        /** X and Z are coupled (horizontal orbit plane). */
        LINKED_XZ("Linked XZ"),
        /** X and Y are coupled (frontal orbit plane). */
        LINKED_XY("Linked XY"),
        /** Y and Z are coupled (sagittal orbit plane). */
        LINKED_YZ("Linked YZ");
        
        private final String displayName;
        CouplingMode(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
        
        public static CouplingMode fromId(String id) {
            if (id == null || id.isEmpty()) return INDEPENDENT;
            try {
                return valueOf(id.toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException e) {
                return INDEPENDENT;
            }
        }
    }
    
    /** Disabled 3D orbit. */
    public static final OrbitConfig3D NONE = new OrbitConfig3D(
        false, null, null, null, CouplingMode.INDEPENDENT);
    
    /** Default horizontal circular orbit (XZ plane). */
    public static final OrbitConfig3D HORIZONTAL_ORBIT = new OrbitConfig3D(
        true,
        AxisMotionConfig.circular(1.0f, 0.5f, 0f),
        null,
        AxisMotionConfig.circular(1.0f, 0.5f, 0.25f),
        CouplingMode.LINKED_XZ);
    
    /** Default vertical orbit (XY plane). */
    public static final OrbitConfig3D VERTICAL_ORBIT = new OrbitConfig3D(
        true,
        AxisMotionConfig.circular(1.0f, 0.5f, 0f),
        AxisMotionConfig.circular(1.0f, 0.5f, 0.25f),
        null,
        CouplingMode.LINKED_XY);
    
    /** Electron cloud (multi-frequency waves). */
    public static final OrbitConfig3D ELECTRON_CLOUD = new OrbitConfig3D(
        true,
        AxisMotionConfig.wave(0.5f, 1.0f),
        AxisMotionConfig.wave(0.3f, 1.5f),
        AxisMotionConfig.wave(0.5f, 0.8f),
        CouplingMode.INDEPENDENT);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates a horizontal orbit (XZ plane).
     * @param radius Orbit radius
     * @param frequency Speed
     */
    public static OrbitConfig3D horizontalOrbit(float radius, float frequency) {
        return new OrbitConfig3D(
            true,
            AxisMotionConfig.circular(radius, frequency, 0f),
            null,
            AxisMotionConfig.circular(radius, frequency, 0.25f),
            CouplingMode.LINKED_XZ);
    }
    
    /**
     * Creates a tilted orbit with vertical wobble.
     * @param radius Orbit radius
     * @param frequency Orbit speed
     * @param wobbleAmplitude Vertical wobble amount
     */
    public static OrbitConfig3D tiltedOrbit(float radius, float frequency, float wobbleAmplitude) {
        return new OrbitConfig3D(
            true,
            AxisMotionConfig.circular(radius, frequency, 0f),
            AxisMotionConfig.wave(wobbleAmplitude, frequency * 2),
            AxisMotionConfig.circular(radius, frequency, 0.25f),
            CouplingMode.INDEPENDENT);
    }
    
    /**
     * Creates from legacy OrbitConfig.
     * @param legacy The old single-axis config
     * @return Equivalent 3D config
     */
    public static OrbitConfig3D fromLegacy(OrbitConfig legacy) {
        if (legacy == null || !legacy.isActive()) {
            return NONE;
        }
        
        float radius = legacy.radius();
        float speed = legacy.speed();
        float phase = legacy.phase();
        
        // Convert legacy axis to 3D config
        return switch (legacy.axis()) {
            case Y -> new OrbitConfig3D(
                true,
                AxisMotionConfig.circular(radius, speed, 0f),
                null,
                AxisMotionConfig.circular(radius, speed, phase + 0.25f),
                CouplingMode.LINKED_XZ);
            case X -> new OrbitConfig3D(
                true,
                null,
                AxisMotionConfig.circular(radius, speed, 0f),
                AxisMotionConfig.circular(radius, speed, phase + 0.25f),
                CouplingMode.LINKED_YZ);
            case Z -> new OrbitConfig3D(
                true,
                AxisMotionConfig.circular(radius, speed, 0f),
                AxisMotionConfig.circular(radius, speed, phase + 0.25f),
                null,
                CouplingMode.LINKED_XY);
            default -> HORIZONTAL_ORBIT;
        };
    }
    
    // =========================================================================
    // Query Methods
    // =========================================================================
    
    /** Whether any orbit motion is active. */
    public boolean isActive() {
        if (!enabled) return false;
        return (x != null && x.isActive()) ||
               (y != null && y.isActive()) ||
               (z != null && z.isActive());
    }
    
    /** Factory for disabled orbit. */
    public static OrbitConfig3D disabled() {
        return NONE;
    }
    
    // Immutable with* methods for single-property updates
    public OrbitConfig3D withEnabled(boolean e) { return toBuilder().enabled(e).build(); }
    public OrbitConfig3D withX(AxisMotionConfig c) { return toBuilder().x(c).build(); }
    public OrbitConfig3D withY(AxisMotionConfig c) { return toBuilder().y(c).build(); }
    public OrbitConfig3D withZ(AxisMotionConfig c) { return toBuilder().z(c).build(); }
    public OrbitConfig3D withCoupling(CouplingMode m) { return toBuilder().coupling(m).build(); }
    
    /** Whether X axis has motion. */
    public boolean hasXMotion() { return x != null && x.isActive(); }
    
    /** Whether Y axis has motion. */
    public boolean hasYMotion() { return y != null && y.isActive(); }
    
    /** Whether Z axis has motion. */
    public boolean hasZMotion() { return z != null && z.isActive(); }
    
    /** Count of active axes. */
    public int activeAxisCount() {
        int count = 0;
        if (hasXMotion()) count++;
        if (hasYMotion()) count++;
        if (hasZMotion()) count++;
        return count;
    }
    
    // =========================================================================
    // Motion Calculation
    // =========================================================================
    
    /**
     * Calculates the 3D offset at a given time.
     * 
     * <p>Creates proper circular orbits automatically:
     * <ul>
     * <ul>
     *   <li>2D Orbit modes (CIRCULAR, ELLIPTIC, FIGURE_8): X→XZ plane, Y→YX plane, Z→ZY plane</li>
     *   <li>Single-axis modes (WAVE, OSCILLATION, BOUNCE): Only affect that axis</li>
     * </ul>
     * Multiple axes ADD together for compound motion (tilted orbits, 3D spirals).</p>
     * 
     * @param time Time in ticks
     * @return Offset vector from center
     */
    public Vector3f getOffset(float time) {
        if (!isActive()) {
            return new Vector3f(0, 0, 0);
        }
        
        float offsetX = 0f;
        float offsetY = 0f;
        float offsetZ = 0f;
        
        // X axis motion - orbits in XZ plane (horizontal circle)
        if (x != null && x.isActive()) {
            if (x.is2DOrbit() || x.is3DOrbit()) {
                // 2D/3D orbit: X uses cos, Z uses sin
                float radiusMult = x.getRadiusMultiplier(time);
                offsetX += x.getDisplacementCos(time);
                offsetZ += x.getDisplacement(time) * radiusMult;
                
                // 3D modes add perpendicular (Y) motion
                if (x.is3DOrbit()) {
                    offsetY += x.getDisplacementPerp(time);
                }
            } else {
                // Single-axis: only affects X
                offsetX += x.getDisplacement(time);
            }
        }
        
        // Y axis motion - orbits in YX plane (vertical circle facing Z)
        // This pairs with Z for diagonal orbits when combined with X
        if (y != null && y.isActive()) {
            if (y.is2DOrbit() || y.is3DOrbit()) {
                // 2D/3D orbit: Y uses cos, X uses sin
                float radiusMult = y.getRadiusMultiplier(time);
                offsetY += y.getDisplacementCos(time);
                offsetX += y.getDisplacement(time) * radiusMult;
                
                // 3D modes add perpendicular (Z) motion
                if (y.is3DOrbit()) {
                    offsetZ += y.getDisplacementPerp(time);
                }
            } else {
                // Single-axis: only affects Y
                offsetY += y.getDisplacement(time);
            }
        }
        
        // Z axis motion - orbits in ZY plane (vertical circle facing X)
        if (z != null && z.isActive()) {
            if (z.is2DOrbit() || z.is3DOrbit()) {
                // 2D/3D orbit: Z uses cos, Y uses sin
                float radiusMult = z.getRadiusMultiplier(time);
                offsetZ += z.getDisplacementCos(time);
                offsetY += z.getDisplacement(time) * radiusMult;
                
                // 3D modes add perpendicular (X) motion
                if (z.is3DOrbit()) {
                    offsetX += z.getDisplacementPerp(time);
                }
            } else {
                // Single-axis: only affects Z
                offsetZ += z.getDisplacement(time);
            }
        }
        
        Logging.ANIMATION.topic("orbit3d").trace(
            "3D offset: ({}, {}, {}) at t={}", offsetX, offsetY, offsetZ, time);
        
        return new Vector3f(offsetX, offsetY, offsetZ);
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses OrbitConfig3D from JSON.
     */
    public static OrbitConfig3D fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        
        AxisMotionConfig x = null;
        if (json.has("x")) {
            x = AxisMotionConfig.fromJson(json.getAsJsonObject("x"));
        }
        
        AxisMotionConfig y = null;
        if (json.has("y")) {
            y = AxisMotionConfig.fromJson(json.getAsJsonObject("y"));
        }
        
        AxisMotionConfig z = null;
        if (json.has("z")) {
            z = AxisMotionConfig.fromJson(json.getAsJsonObject("z"));
        }
        
        CouplingMode coupling = CouplingMode.INDEPENDENT;
        if (json.has("coupling")) {
            coupling = CouplingMode.fromId(json.get("coupling").getAsString());
        }
        
        OrbitConfig3D result = new OrbitConfig3D(enabled, x, y, z, coupling);
        
        if (result.isActive()) {
            Logging.FIELD.topic("parse").debug(
                "Parsed OrbitConfig3D: enabled={}, axes={}, coupling={}",
                enabled, result.activeAxisCount(), coupling);
        }
        
        return result;
    }
    
    /**
     * Serializes to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    /** Creates a builder pre-populated with this config's values. */
    public Builder toBuilder() {
        return new Builder()
            .enabled(enabled)
            .x(x)
            .y(y)
            .z(z)
            .coupling(coupling);
    }
    
    public static class Builder {
        private boolean enabled = true;
        private AxisMotionConfig x = null;
        private AxisMotionConfig y = null;
        private AxisMotionConfig z = null;
        private CouplingMode coupling = CouplingMode.INDEPENDENT;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder x(AxisMotionConfig c) { this.x = c; return this; }
        public Builder y(AxisMotionConfig c) { this.y = c; return this; }
        public Builder z(AxisMotionConfig c) { this.z = c; return this; }
        public Builder coupling(CouplingMode m) { this.coupling = m; return this; }
        
        /** Convenience: set X to circular motion. */
        public Builder xCircular(float amplitude, float frequency) {
            this.x = AxisMotionConfig.circular(amplitude, frequency);
            return this;
        }
        
        /** Convenience: set Y to wave motion. */
        public Builder yWave(float amplitude, float frequency) {
            this.y = AxisMotionConfig.wave(amplitude, frequency);
            return this;
        }
        
        /** Convenience: set Z to circular with phase. */
        public Builder zCircular(float amplitude, float frequency, float phase) {
            this.z = AxisMotionConfig.circular(amplitude, frequency, phase);
            return this;
        }
        
        public OrbitConfig3D build() {
            return new OrbitConfig3D(enabled, x, y, z, coupling);
        }
    }
}
