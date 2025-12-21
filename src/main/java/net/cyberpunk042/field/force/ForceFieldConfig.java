package net.cyberpunk042.field.force;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.mode.*;
import net.cyberpunk042.field.force.phase.ForcePhase;
import net.cyberpunk042.field.force.phase.ForcePolarity;
import net.cyberpunk042.field.force.zone.ForceZone;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Complete configuration for a force field's physical behavior.
 * 
 * <p>A force field consists of:
 * <ul>
 *   <li><b>Mode</b>: Primary behavior type (RADIAL, VORTEX, ORBIT, etc.)</li>
 *   <li><b>Zones</b>: Layered spatial regions with different strengths/falloffs</li>
 *   <li><b>Phases</b>: Time-based behavior changes (pull → hold → push)</li>
 *   <li><b>Constraints</b>: Velocity caps, damping</li>
 * </ul>
 * 
 * <p>Modes and zones work together:
 * <ul>
 *   <li>Zones define WHERE forces apply and base strength</li>
 *   <li>Mode defines HOW forces are calculated (radial, tangential, lift)</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "mode": "vortex",
 *   "zones": [
 *     { "radius": 15, "strength": 0.1, "falloff": "linear" },
 *     { "radius": 8, "strength": 0.2, "falloff": "quadratic" }
 *   ],
 *   "phases": [
 *     { "start": 0, "end": 75, "polarity": "pull" },
 *     { "start": 75, "end": 100, "polarity": "push", "strengthMultiplier": 2.0 }
 *   ],
 *   "vortex": { "tangentialStrength": 0.15, "spinAxis": "Y" },
 *   "maxVelocity": 1.5,
 *   "damping": 0.02
 * }
 * </pre>
 * 
 * @param mode Force calculation behavior type
 * @param zones Spatial force zones (can have multiple nested)
 * @param phases Time-based phases
 * @param maxVelocity Maximum entity velocity cap
 * @param verticalBoost Vertical force boost (legacy support)
 * @param damping Velocity damping factor (0-1)
 * @param vortex Mode-specific config for VORTEX
 * @param orbit Mode-specific config for ORBIT
 * @param tornado Mode-specific config for TORNADO
 * @param ring Mode-specific config for RING
 * @param implosion Mode-specific config for IMPLOSION
 * @param explosion Mode-specific config for EXPLOSION
 */
public record ForceFieldConfig(
    // Primary settings
    @JsonField(skipIfDefault = true, defaultValue = "RADIAL") ForceMode mode,
    List<ForceZone> zones,
    List<ForcePhase> phases,
    
    // Global constraints
    @JsonField(skipIfDefault = true, defaultValue = "1.5") float maxVelocity,
    @JsonField(skipIfDefault = true) float verticalBoost,
    @JsonField(skipIfDefault = true) float damping,
    
    // Mode-specific configurations (optional, only used when mode matches)
    @Nullable @JsonField(skipIfNull = true) PullModeConfig pull,
    @Nullable @JsonField(skipIfNull = true) PushModeConfig push,
    @Nullable @JsonField(skipIfNull = true) VortexModeConfig vortex,
    @Nullable @JsonField(skipIfNull = true) OrbitModeConfig orbit,
    @Nullable @JsonField(skipIfNull = true) TornadoModeConfig tornado,
    @Nullable @JsonField(skipIfNull = true) RingModeConfig ring,
    @Nullable @JsonField(skipIfNull = true) ImplosionModeConfig implosion,
    @Nullable @JsonField(skipIfNull = true) ExplosionModeConfig explosion
) {
    
    /** Default simple pull field. */
    public static final ForceFieldConfig DEFAULT = new ForceFieldConfig(
        ForceMode.RADIAL,
        List.of(ForceZone.linear(10, 0.15f)),
        List.of(ForcePhase.pull(0, 100)),
        1.5f, 0f, 0f,
        null, null, null, null, null, null, null, null
    );
    
    /**
     * Compact constructor with validation and sorting.
     */
    public ForceFieldConfig {
        if (mode == null) mode = ForceMode.RADIAL;
        
        // Ensure zones are sorted by radius (descending - outermost first)
        if (zones == null) zones = List.of();
        if (!zones.isEmpty()) {
            List<ForceZone> sorted = new ArrayList<>(zones);
            sorted.sort(Comparator.comparing(ForceZone::radius).reversed());
            zones = Collections.unmodifiableList(sorted);
        }
        
        // Ensure phases are sorted by start time
        if (phases == null) phases = List.of();
        if (!phases.isEmpty()) {
            List<ForcePhase> sorted = new ArrayList<>(phases);
            sorted.sort(Comparator.comparing(ForcePhase::startPercent));
            phases = Collections.unmodifiableList(sorted);
        }
        
        // Validate constraints
        maxVelocity = Math.max(0.1f, maxVelocity);
        verticalBoost = Math.max(0, verticalBoost);
        damping = Math.max(0f, Math.min(1f, damping));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Zone Access
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns the outermost zone radius (maximum effect range).
     */
    public float maxRadius() {
        return zones.isEmpty() ? 10f : zones.get(0).radius();
    }
    
    /**
     * Finds the zone that contains the given distance.
     * Returns the innermost (strongest) zone that contains the point.
     */
    @Nullable
    public ForceZone zoneAt(float distance) {
        // Iterate from innermost to outermost
        for (int i = zones.size() - 1; i >= 0; i--) {
            ForceZone zone = zones.get(i);
            if (zone.containsDistance(distance)) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Calculates the effective strength at a distance (from zone + falloff).
     */
    public float strengthAt(float distance) {
        ForceZone zone = zoneAt(distance);
        return zone != null ? zone.strengthAt(distance) : 0f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Phase Access
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Finds the active phase at the given normalized time.
     */
    @Nullable
    public ForcePhase phaseAt(float normalizedTime) {
        for (ForcePhase phase : phases) {
            if (phase.containsTime(normalizedTime)) {
                return phase;
            }
        }
        return null;
    }
    
    /**
     * Gets the current polarity at the given time.
     */
    public ForcePolarity polarityAt(float normalizedTime) {
        ForcePhase phase = phaseAt(normalizedTime);
        return phase != null ? phase.polarity() : ForcePolarity.PULL;
    }
    
    /**
     * Gets the strength multiplier from the current phase.
     */
    public float phaseMultiplier(float normalizedTime) {
        ForcePhase phase = phaseAt(normalizedTime);
        return phase != null ? phase.strengthMultiplier() : 1f;
    }
    
    /**
     * Calculates the effective force strength at a distance and time.
     * Combines zone strength with phase multiplier.
     */
    public float effectiveStrength(float distance, float normalizedTime) {
        float baseStrength = strengthAt(distance);
        
        ForcePhase phase = phaseAt(normalizedTime);
        if (phase == null) {
            return baseStrength;
        }
        
        return baseStrength * phase.strengthMultiplier();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Mode Helpers
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns whether this config uses tangential forces (spinning).
     */
    public boolean hasTangentialForces() {
        return mode == ForceMode.VORTEX || mode == ForceMode.ORBIT || 
               mode == ForceMode.TORNADO || mode == ForceMode.RING;
    }
    
    /**
     * Returns the tangential strength multiplier for the current mode.
     */
    public float tangentialStrength() {
        return switch (mode) {
            case VORTEX -> vortex != null ? vortex.tangentialStrength() : 0f;
            case ORBIT -> orbit != null ? orbit.orbitSpeed() : 0f;
            case TORNADO -> tornado != null ? tornado.spinSpeed() : 0f;
            case RING -> ring != null ? ring.orbitSpeed() : 0f;
            default -> 0f;
        };
    }
    
    /**
     * Returns the spin axis for tangential modes.
     */
    public ForceAxis spinAxis() {
        return switch (mode) {
            case VORTEX -> vortex != null ? vortex.spinAxis() : ForceAxis.Y;
            case ORBIT -> orbit != null ? orbit.orbitAxis() : ForceAxis.Y;
            case TORNADO -> tornado != null ? tornado.spinAxis() : ForceAxis.Y;
            case RING -> ring != null ? ring.orbitAxis() : ForceAxis.Y;
            default -> ForceAxis.Y;
        };
    }
    
    /**
     * Returns whether spin is clockwise.
     */
    public boolean isClockwise() {
        return switch (mode) {
            case VORTEX -> vortex == null || vortex.clockwise();
            case ORBIT -> orbit == null || orbit.clockwise();
            case TORNADO -> tornado == null || tornado.clockwise();
            case RING -> ring == null || ring.clockwise();
            default -> true;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON Serialization
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ForceFieldConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        // Parse mode (default to RADIAL for backward compatibility)
        ForceMode mode = ForceMode.RADIAL;
        if (json.has("mode")) {
            mode = ForceMode.fromId(json.get("mode").getAsString());
        }
        
        // Parse zones
        List<ForceZone> zones = new ArrayList<>();
        if (json.has("zones") && json.get("zones").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("zones")) {
                if (element.isJsonObject()) {
                    zones.add(ForceZone.fromJson(element.getAsJsonObject()));
                }
            }
        }
        
        // Parse phases
        List<ForcePhase> phases = new ArrayList<>();
        if (json.has("phases") && json.get("phases").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("phases")) {
                if (element.isJsonObject()) {
                    phases.add(ForcePhase.fromJson(element.getAsJsonObject()));
                }
            }
        }
        
        // Parse global constraints
        float maxVelocity = json.has("maxVelocity") ? json.get("maxVelocity").getAsFloat() : 1.5f;
        float verticalBoost = json.has("verticalBoost") ? json.get("verticalBoost").getAsFloat() : 0f;
        float damping = json.has("damping") ? json.get("damping").getAsFloat() : 0f;
        
        // Parse mode-specific configs
        PullModeConfig pull = json.has("pull") ? PullModeConfig.fromJson(json.getAsJsonObject("pull")) : null;
        PushModeConfig push = json.has("push") ? PushModeConfig.fromJson(json.getAsJsonObject("push")) : null;
        VortexModeConfig vortex = json.has("vortex") ? VortexModeConfig.fromJson(json.getAsJsonObject("vortex")) : null;
        OrbitModeConfig orbit = json.has("orbit") ? OrbitModeConfig.fromJson(json.getAsJsonObject("orbit")) : null;
        TornadoModeConfig tornado = json.has("tornado") ? TornadoModeConfig.fromJson(json.getAsJsonObject("tornado")) : null;
        RingModeConfig ring = json.has("ring") ? RingModeConfig.fromJson(json.getAsJsonObject("ring")) : null;
        ImplosionModeConfig implosion = json.has("implosion") ? ImplosionModeConfig.fromJson(json.getAsJsonObject("implosion")) : null;
        ExplosionModeConfig explosion = json.has("explosion") ? ExplosionModeConfig.fromJson(json.getAsJsonObject("explosion")) : null;
        
        return new ForceFieldConfig(
            mode, zones, phases, maxVelocity, verticalBoost, damping,
            pull, push, vortex, orbit, tornado, ring, implosion, explosion
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ForceMode mode = ForceMode.RADIAL;
        private List<ForceZone> zones = new ArrayList<>();
        private List<ForcePhase> phases = new ArrayList<>();
        private float maxVelocity = 1.5f;
        private float verticalBoost = 0f;
        private float damping = 0f;
        
        private PullModeConfig pull = null;
        private PushModeConfig push = null;
        private VortexModeConfig vortex = null;
        private OrbitModeConfig orbit = null;
        private TornadoModeConfig tornado = null;
        private RingModeConfig ring = null;
        private ImplosionModeConfig implosion = null;
        private ExplosionModeConfig explosion = null;
        
        // Mode
        public Builder mode(ForceMode m) { this.mode = m; return this; }
        
        // Zones
        public Builder zone(ForceZone z) { this.zones.add(z); return this; }
        public Builder zone(float radius, float strength, String falloff) {
            return zone(ForceZone.builder()
                .radius(radius)
                .radialStrength(strength)
                .falloff(falloff)
                .build());
        }
        public Builder zones(List<ForceZone> z) { this.zones = new ArrayList<>(z); return this; }
        
        // Phases
        public Builder phase(ForcePhase p) { this.phases.add(p); return this; }
        public Builder pullPhase(float start, float end) { 
            this.phases.add(ForcePhase.pull(start, end)); return this; 
        }
        public Builder pushPhase(float start, float end, float strength) { 
            this.phases.add(ForcePhase.push(start, end, strength)); return this; 
        }
        public Builder holdPhase(float start, float end) { 
            this.phases.add(ForcePhase.hold(start, end)); return this; 
        }
        public Builder phases(List<ForcePhase> p) { this.phases = new ArrayList<>(p); return this; }
        
        // Global constraints
        public Builder maxVelocity(float v) { this.maxVelocity = v; return this; }
        public Builder verticalBoost(float v) { this.verticalBoost = v; return this; }
        public Builder damping(float d) { this.damping = d; return this; }
        
        // Mode-specific
        public Builder pull(PullModeConfig c) { this.pull = c; this.mode = ForceMode.PULL; return this; }
        public Builder push(PushModeConfig c) { this.push = c; this.mode = ForceMode.PUSH; return this; }
        public Builder vortex(VortexModeConfig c) { this.vortex = c; this.mode = ForceMode.VORTEX; return this; }
        public Builder orbit(OrbitModeConfig c) { this.orbit = c; this.mode = ForceMode.ORBIT; return this; }
        public Builder tornado(TornadoModeConfig c) { this.tornado = c; this.mode = ForceMode.TORNADO; return this; }
        public Builder ring(RingModeConfig c) { this.ring = c; this.mode = ForceMode.RING; return this; }
        public Builder implosion(ImplosionModeConfig c) { this.implosion = c; this.mode = ForceMode.IMPLOSION; return this; }
        public Builder explosion(ExplosionModeConfig c) { this.explosion = c; this.mode = ForceMode.EXPLOSION; return this; }
        
        public ForceFieldConfig build() {
            return new ForceFieldConfig(
                mode, zones, phases, maxVelocity, verticalBoost, damping,
                pull, push, vortex, orbit, tornado, ring, implosion, explosion
            );
        }
    }
}
