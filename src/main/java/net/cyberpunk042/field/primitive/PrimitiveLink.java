package net.cyberpunk042.field.primitive;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;
import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Links a primitive to ONE target primitive for coordinated behavior.
 * 
 * <p>Per ARCHITECTURE §9: Primitives within a layer can be linked
 * for coordinated behavior like radius matching, position following,
 * mirroring, and animation phase offset.</p>
 * 
 * <h2>Structure</h2>
 * <p>Each link has ONE target and boolean flags for what to link:</p>
 * <table>
 *   <tr><th>Field</th><th>Description</th></tr>
 *   <tr><td>{@code target}</td><td>The primitive ID to link to (required)</td></tr>
 *   <tr><td>{@code radiusMatch}</td><td>Match target's radius</td></tr>
 *   <tr><td>{@code radiusOffset}</td><td>Offset from matched radius</td></tr>
 *   <tr><td>{@code follow}</td><td>Follow target's position</td></tr>
 *   <tr><td>{@code mirror}</td><td>Mirror on specified axis</td></tr>
 *   <tr><td>{@code phaseOffset}</td><td>Animation phase offset</td></tr>
 *   <tr><td>{@code scaleWith}</td><td>Scale proportionally with target</td></tr>
 *   <tr><td>{@code orbitSync}</td><td>Sync orbit config with target</td></tr>
 *   <tr><td>{@code orbitPhaseOffset}</td><td>Offset orbit phase</td></tr>
 *   <tr><td>{@code colorMatch}</td><td>Match target's color</td></tr>
 *   <tr><td>{@code alphaMatch}</td><td>Match target's alpha</td></tr>
 * </table>
 * 
 * <h2>Multiple Links</h2>
 * <p>A primitive can have multiple links (List&lt;PrimitiveLink&gt;). Each link
 * targets one primitive and defines how they're connected.</p>
 * 
 * <h2>Atom Orbital Example</h2>
 * <pre>
 * // 4 electrons linked to nucleus with orbit sync
 * electron1: links: [{ target: "nucleus", orbitSync: true, orbitPhaseOffset: 0.0 }]
 * electron2: links: [{ target: "nucleus", orbitSync: true, orbitPhaseOffset: 0.25 }]
 * electron3: links: [{ target: "nucleus", orbitSync: true, orbitPhaseOffset: 0.5 }]
 * electron4: links: [{ target: "nucleus", orbitSync: true, orbitPhaseOffset: 0.75 }]
 * </pre>
 * 
 * <h2>Cycle Prevention</h2>
 * <p>Links are resolved in primitive declaration order. A primitive can only
 * link to primitives declared BEFORE it. This prevents circular references.</p>
 * 
 * @see Primitive
 * @see LinkResolver
 */
public record PrimitiveLink(
    // Target (required)
    String target,
    
    // Position/Shape linking (boolean flags + offsets)
    @JsonField(skipIfDefault = true) boolean radiusMatch,
    @JsonField(skipIfDefault = true) float radiusOffset,
    @JsonField(skipIfDefault = true) boolean follow,
    @Nullable @JsonField(skipIfNull = true) Axis mirror,
    @JsonField(skipIfDefault = true) float phaseOffset,
    @JsonField(skipIfDefault = true) boolean scaleWith,
    
    // Orbit linking
    @JsonField(skipIfDefault = true) boolean orbitSync,
    @JsonField(skipIfDefault = true) float orbitPhaseOffset,
    
    // Appearance linking
    @JsonField(skipIfDefault = true) boolean colorMatch,
    @JsonField(skipIfDefault = true) boolean alphaMatch
){
    
    /** No linking (null target). */
    public static final PrimitiveLink NONE = new PrimitiveLink(
        null, false, 0, false, null, 0, false, false, 0, false, false);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /** Creates a link to a target with default settings. */
    public static PrimitiveLink to(String targetId) {
        return new PrimitiveLink(targetId, false, 0, false, null, 0, false, false, 0, false, false);
    }
    
    /** Creates a radius match link. */
    public static PrimitiveLink radiusMatch(String targetId, float offset) {
        return new PrimitiveLink(targetId, true, offset, false, null, 0, false, false, 0, false, false);
    }
    
    /** Creates a follow link. */
    public static PrimitiveLink follow(String targetId) {
        return new PrimitiveLink(targetId, false, 0, true, null, 0, false, false, 0, false, false);
    }
    
    /** Creates a mirror link. */
    public static PrimitiveLink mirror(String targetId, Axis axis) {
        return new PrimitiveLink(targetId, false, 0, false, axis, 0, false, false, 0, false, false);
    }
    
    /** Creates an orbit sync link with phase offset. */
    public static PrimitiveLink orbitSync(String targetId, float orbitPhaseOffset) {
        return new PrimitiveLink(targetId, false, 0, false, null, 0, false, true, orbitPhaseOffset, false, false);
    }
    
    /** Creates a color/alpha match link. */
    public static PrimitiveLink appearance(String targetId, boolean color, boolean alpha) {
        return new PrimitiveLink(targetId, false, 0, false, null, 0, false, false, 0, color, alpha);
    }
    
    // =========================================================================
    // Query Methods
    // =========================================================================
    
    /** Checks if this link has a valid target. */
    public boolean isValid() {
        return target != null && !target.isEmpty();
    }
    
    /** Checks if this link has any active link types. */
    public boolean hasAnyLinkType() {
        return radiusMatch || follow || mirror != null || scaleWith 
            || orbitSync || colorMatch || alphaMatch
            || phaseOffset != 0 || orbitPhaseOffset != 0 || radiusOffset != 0;
    }
    
    /** Checks if this link has orbit synchronization. */
    public boolean hasOrbitLink() {
        return orbitSync || orbitPhaseOffset != 0;
    }
    
    /** Checks if this link has appearance linking. */
    public boolean hasAppearanceLink() {
        return colorMatch || alphaMatch;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public Builder toBuilder() {
        return new Builder()
            .target(target)
            .radiusMatch(radiusMatch)
            .radiusOffset(radiusOffset)
            .follow(follow)
            .mirror(mirror)
            .phaseOffset(phaseOffset)
            .scaleWith(scaleWith)
            .orbitSync(orbitSync)
            .orbitPhaseOffset(orbitPhaseOffset)
            .colorMatch(colorMatch)
            .alphaMatch(alphaMatch);
    }
    
    public static class Builder {
        private String target;
        private boolean radiusMatch;
        private float radiusOffset;
        private boolean follow;
        private Axis mirror;
        private float phaseOffset;
        private boolean scaleWith;
        private boolean orbitSync;
        private float orbitPhaseOffset;
        private boolean colorMatch;
        private boolean alphaMatch;
        
        public Builder target(String t) { this.target = t; return this; }
        public Builder radiusMatch(boolean v) { this.radiusMatch = v; return this; }
        public Builder radiusOffset(float v) { this.radiusOffset = v; return this; }
        public Builder follow(boolean v) { this.follow = v; return this; }
        public Builder mirror(Axis v) { this.mirror = v; return this; }
        public Builder phaseOffset(float v) { this.phaseOffset = v; return this; }
        public Builder scaleWith(boolean v) { this.scaleWith = v; return this; }
        public Builder orbitSync(boolean v) { this.orbitSync = v; return this; }
        public Builder orbitPhaseOffset(float v) { this.orbitPhaseOffset = v; return this; }
        public Builder colorMatch(boolean v) { this.colorMatch = v; return this; }
        public Builder alphaMatch(boolean v) { this.alphaMatch = v; return this; }
        
        public PrimitiveLink build() {
            return new PrimitiveLink(target, radiusMatch, radiusOffset, follow, mirror,
                phaseOffset, scaleWith, orbitSync, orbitPhaseOffset, colorMatch, alphaMatch);
        }
    }
    
    // =========================================================================
    // JSON Serialization
    // =========================================================================
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    public static PrimitiveLink fromJson(JsonObject json) {
        try {
            String target = json.has("target") ? json.get("target").getAsString() : null;
            boolean radiusMatch = json.has("radiusMatch") && json.get("radiusMatch").getAsBoolean();
            float radiusOffset = json.has("radiusOffset") ? json.get("radiusOffset").getAsFloat() : 0;
            boolean follow = json.has("follow") && json.get("follow").getAsBoolean();
            Axis mirror = json.has("mirror") ? Axis.valueOf(json.get("mirror").getAsString()) : null;
            float phaseOffset = json.has("phaseOffset") ? json.get("phaseOffset").getAsFloat() : 0;
            boolean scaleWith = json.has("scaleWith") && json.get("scaleWith").getAsBoolean();
            boolean orbitSync = json.has("orbitSync") && json.get("orbitSync").getAsBoolean();
            float orbitPhaseOffset = json.has("orbitPhaseOffset") ? json.get("orbitPhaseOffset").getAsFloat() : 0;
            boolean colorMatch = json.has("colorMatch") && json.get("colorMatch").getAsBoolean();
            boolean alphaMatch = json.has("alphaMatch") && json.get("alphaMatch").getAsBoolean();
            
            return new PrimitiveLink(target, radiusMatch, radiusOffset, follow, mirror,
                phaseOffset, scaleWith, orbitSync, orbitPhaseOffset, colorMatch, alphaMatch);
        } catch (Exception e) {
            Logging.FIELD.topic("link").warn("Failed to parse PrimitiveLink: {}", e.getMessage());
            return NONE;
        }
    }
}
