package net.cyberpunk042.field.force.phase;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for a single phase in a force field's lifecycle.
 * 
 * <p>A phase defines the force behavior during a portion of the field's lifetime.
 * Multiple phases create dynamic force effects (e.g., pull → hold → push).
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "start": 0,
 *   "end": 75,
 *   "polarity": "pull",
 *   "strengthMultiplier": 1.0,
 *   "notification": {
 *     "message": "Gravity well forming...",
 *     "offset": 0
 *   }
 * }
 * </pre>
 * 
 * @param startPercent Phase start as percentage of total duration (0-100)
 * @param endPercent Phase end as percentage of total duration (0-100)
 * @param polarity Force direction during this phase
 * @param strengthMultiplier Multiplier applied to base force strength
 * @param notification Optional message shown when phase activates
 */
public record ForcePhase(
    float startPercent,
    float endPercent,
    ForcePolarity polarity,
    @JsonField(skipIfDefault = true, defaultValue = "1.0") float strengthMultiplier,
    @Nullable @JsonField(skipIfNull = true) PhaseNotification notification
) {
    
    /**
     * Compact constructor with validation.
     */
    public ForcePhase {
        startPercent = Math.max(0, Math.min(100, startPercent));
        endPercent = Math.max(startPercent, Math.min(100, endPercent));
        if (polarity == null) polarity = ForcePolarity.PULL;
        if (strengthMultiplier <= 0) strengthMultiplier = 1.0f;
    }
    
    /**
     * Returns true if the given normalized time (0.0-1.0) is within this phase.
     * 
     * @param normalizedTime Time as fraction of total duration (0.0 = start, 1.0 = end)
     */
    public boolean containsTime(float normalizedTime) {
        float percent = normalizedTime * 100f;
        return percent >= startPercent && percent < endPercent;
    }
    
    /**
     * Returns the progress within this phase (0.0 at phase start, 1.0 at phase end).
     * Returns -1 if time is outside this phase.
     * 
     * @param normalizedTime Time as fraction of total duration
     */
    public float phaseProgress(float normalizedTime) {
        float percent = normalizedTime * 100f;
        if (percent < startPercent || percent >= endPercent) {
            return -1f;
        }
        float phaseRange = endPercent - startPercent;
        if (phaseRange <= 0) return 0f;
        return (percent - startPercent) / phaseRange;
    }
    
    /**
     * Returns true if this phase has a notification to show.
     */
    public boolean hasNotification() {
        return notification != null && notification.hasMessage();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a pull phase.
     */
    public static ForcePhase pull(float start, float end) {
        return new ForcePhase(start, end, ForcePolarity.PULL, 1.0f, null);
    }
    
    /**
     * Creates a push phase.
     */
    public static ForcePhase push(float start, float end, float strength) {
        return new ForcePhase(start, end, ForcePolarity.PUSH, strength, null);
    }
    
    /**
     * Creates a hold phase.
     */
    public static ForcePhase hold(float start, float end) {
        return new ForcePhase(start, end, ForcePolarity.HOLD, 1.0f, null);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses from JSON.
     */
    public static ForcePhase fromJson(JsonObject json) {
        if (json == null) {
            return pull(0, 100);
        }
        
        // Support both "start"/"startPercent" and "end"/"endPercent"
        float start = 0;
        if (json.has("startPercent")) {
            start = json.get("startPercent").getAsFloat();
        } else if (json.has("start")) {
            start = json.get("start").getAsFloat();
        }
        
        float end = 100;
        if (json.has("endPercent")) {
            end = json.get("endPercent").getAsFloat();
        } else if (json.has("end")) {
            end = json.get("end").getAsFloat();
        }
        
        ForcePolarity polarity = ForcePolarity.PULL;
        if (json.has("polarity")) {
            polarity = ForcePolarity.fromId(json.get("polarity").getAsString());
        }
        
        // Support both "strength" and "strengthMultiplier"
        float multiplier = 1.0f;
        if (json.has("strengthMultiplier")) {
            multiplier = json.get("strengthMultiplier").getAsFloat();
        } else if (json.has("strength")) {
            multiplier = json.get("strength").getAsFloat();
        }
        
        PhaseNotification notification = null;
        if (json.has("notification") && json.get("notification").isJsonObject()) {
            notification = PhaseNotification.fromJson(json.getAsJsonObject("notification"));
        }
        
        return new ForcePhase(start, end, polarity, multiplier, notification);
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
        private float startPercent = 0;
        private float endPercent = 100;
        private ForcePolarity polarity = ForcePolarity.PULL;
        private float strengthMultiplier = 1.0f;
        private PhaseNotification notification = null;
        
        public Builder start(float percent) { this.startPercent = percent; return this; }
        public Builder end(float percent) { this.endPercent = percent; return this; }
        public Builder polarity(ForcePolarity p) { this.polarity = p; return this; }
        public Builder strength(float m) { this.strengthMultiplier = m; return this; }
        public Builder notification(PhaseNotification n) { this.notification = n; return this; }
        
        public Builder notification(String message) { 
            this.notification = PhaseNotification.info(message); 
            return this; 
        }
        
        public Builder warning(String message, int ticksBefore) { 
            this.notification = PhaseNotification.warning(message, ticksBefore); 
            return this; 
        }
        
        public ForcePhase build() {
            return new ForcePhase(startPercent, endPercent, polarity, strengthMultiplier, notification);
        }
    }
}
