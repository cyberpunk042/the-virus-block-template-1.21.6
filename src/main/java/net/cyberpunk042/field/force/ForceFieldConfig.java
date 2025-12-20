package net.cyberpunk042.field.force;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 *   <li><b>Zones</b>: Layered regions with different strengths and falloffs</li>
 *   <li><b>Phases</b>: Time-based behavior changes (pull → push)</li>
 *   <li><b>Constraints</b>: Velocity caps, damping, vertical boost</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "zones": [
 *     { "radius": 15, "strength": 0.1, "falloff": "linear" },
 *     { "radius": 8, "strength": 0.2, "falloff": "quadratic" },
 *     { "radius": 3, "strength": 0.4, "falloff": "constant" }
 *   ],
 *   "phases": [
 *     { "start": 0, "end": 75, "polarity": "pull" },
 *     { "start": 75, "end": 90, "polarity": "hold" },
 *     { "start": 90, "end": 100, "polarity": "push", "strengthMultiplier": 2.0 }
 *   ],
 *   "maxVelocity": 1.5,
 *   "verticalBoost": 0.3,
 *   "damping": 0.02
 * }
 * </pre>
 * 
 * @param zones List of force zones, sorted by radius (outermost first)
 * @param phases List of time-based phases
 * @param maxVelocity Maximum entity velocity (caps excessive speed)
 * @param verticalBoost Vertical force boost on push phases
 * @param damping Velocity damping factor (0 = no damping, 1 = full stop each tick)
 */
public record ForceFieldConfig(
    List<ForceZone> zones,
    List<ForcePhase> phases,
    @JsonField(skipIfDefault = true, defaultValue = "1.5") float maxVelocity,
    @JsonField(skipIfDefault = true) float verticalBoost,
    @JsonField(skipIfDefault = true) float damping
) {
    
    /** Default simple pull field. */
    public static final ForceFieldConfig DEFAULT = new ForceFieldConfig(
        List.of(ForceZone.linear(10, 0.15f)),
        List.of(ForcePhase.pull(0, 100)),
        1.5f, 0f, 0f
    );
    
    /**
     * Compact constructor with validation and sorting.
     */
    public ForceFieldConfig {
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
        damping = Math.max(0, Math.min(1, damping));
    }
    
    /**
     * Returns the outermost zone radius (maximum effect range).
     */
    public float maxRadius() {
        return zones.isEmpty() ? 0 : zones.get(0).radius();
    }
    
    /**
     * Finds the zone that contains the given distance.
     * Returns the innermost (strongest) zone that contains the point.
     * 
     * @param distance Distance from force field center
     * @return The matching zone, or null if outside all zones
     */
    @Nullable
    public ForceZone zoneAt(float distance) {
        // Iterate from innermost to outermost (reversed iteration since sorted descending)
        for (int i = zones.size() - 1; i >= 0; i--) {
            ForceZone zone = zones.get(i);
            if (zone.containsDistance(distance)) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Finds the active phase at the given normalized time.
     * 
     * @param normalizedTime Time as fraction of total duration (0.0-1.0)
     * @return The active phase, or null if no phase matches
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
     * Calculates the effective force strength at a distance and time.
     * Combines zone strength with phase multiplier.
     * 
     * @param distance Distance from force field center
     * @param normalizedTime Time as fraction (0.0-1.0)
     * @return Effective force strength (positive = strength, sign from phase polarity)
     */
    public float effectiveStrength(float distance, float normalizedTime) {
        ForceZone zone = zoneAt(distance);
        if (zone == null) {
            return 0f;
        }
        
        float baseStrength = zone.strengthAt(distance);
        
        ForcePhase phase = phaseAt(normalizedTime);
        if (phase == null) {
            return baseStrength;
        }
        
        return baseStrength * phase.strengthMultiplier();
    }
    
    /**
     * Gets the current polarity at the given time.
     * 
     * @param normalizedTime Time as fraction (0.0-1.0)
     * @return Current polarity (defaults to PULL if no phase matches)
     */
    public ForcePolarity polarityAt(float normalizedTime) {
        ForcePhase phase = phaseAt(normalizedTime);
        return phase != null ? phase.polarity() : ForcePolarity.PULL;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses from JSON.
     */
    public static ForceFieldConfig fromJson(JsonObject json) {
        if (json == null) {
            return DEFAULT;
        }
        
        // Parse zones
        List<ForceZone> zones = new ArrayList<>();
        if (json.has("zones") && json.get("zones").isJsonArray()) {
            JsonArray zonesArray = json.getAsJsonArray("zones");
            for (JsonElement element : zonesArray) {
                if (element.isJsonObject()) {
                    zones.add(ForceZone.fromJson(element.getAsJsonObject()));
                }
            }
        }
        
        // Parse phases
        List<ForcePhase> phases = new ArrayList<>();
        if (json.has("phases") && json.get("phases").isJsonArray()) {
            JsonArray phasesArray = json.getAsJsonArray("phases");
            for (JsonElement element : phasesArray) {
                if (element.isJsonObject()) {
                    phases.add(ForcePhase.fromJson(element.getAsJsonObject()));
                }
            }
        }
        
        float maxVelocity = json.has("maxVelocity") 
            ? json.get("maxVelocity").getAsFloat() 
            : 1.5f;
        float verticalBoost = json.has("verticalBoost") 
            ? json.get("verticalBoost").getAsFloat() 
            : 0f;
        float damping = json.has("damping") 
            ? json.get("damping").getAsFloat() 
            : 0f;
        
        return new ForceFieldConfig(zones, phases, maxVelocity, verticalBoost, damping);
    }
    
    /**
     * Serializes to JSON.
     */
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
        private List<ForceZone> zones = new ArrayList<>();
        private List<ForcePhase> phases = new ArrayList<>();
        private float maxVelocity = 1.5f;
        private float verticalBoost = 0f;
        private float damping = 0f;
        
        public Builder zone(ForceZone zone) { 
            this.zones.add(zone); 
            return this; 
        }
        
        public Builder zone(float radius, float strength, String falloff) {
            this.zones.add(new ForceZone(radius, strength, falloff));
            return this;
        }
        
        public Builder phase(ForcePhase phase) { 
            this.phases.add(phase); 
            return this; 
        }
        
        public Builder pullPhase(float start, float end) {
            this.phases.add(ForcePhase.pull(start, end));
            return this;
        }
        
        public Builder pushPhase(float start, float end, float strength) {
            this.phases.add(ForcePhase.push(start, end, strength));
            return this;
        }
        
        public Builder holdPhase(float start, float end) {
            this.phases.add(ForcePhase.hold(start, end));
            return this;
        }
        
        public Builder maxVelocity(float v) { this.maxVelocity = v; return this; }
        public Builder verticalBoost(float v) { this.verticalBoost = v; return this; }
        public Builder damping(float d) { this.damping = d; return this; }
        
        public ForceFieldConfig build() {
            return new ForceFieldConfig(zones, phases, maxVelocity, verticalBoost, damping);
        }
    }
}
