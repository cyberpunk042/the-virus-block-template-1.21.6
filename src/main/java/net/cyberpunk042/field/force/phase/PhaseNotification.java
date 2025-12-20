package net.cyberpunk042.field.force.phase;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Notification configuration for a phase transition.
 * 
 * <p>Notifications can be shown to players when a force phase changes.
 * The offset allows showing warnings before the actual phase transition,
 * or delayed announcements after.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "message": "⚠️ Collapse imminent!",
 *   "offset": -20,
 *   "type": "warning"
 * }
 * </pre>
 * 
 * @param message The text message to display
 * @param offsetTicks Offset in ticks relative to phase start
 *                    -20 = 20 ticks BEFORE phase starts (warning)
 *                    +10 = 10 ticks AFTER phase starts (delayed)
 *                     0 = exactly when phase starts
 * @param type Notification type for styling (info, warning, danger)
 */
public record PhaseNotification(
    String message,
    @JsonField(skipIfDefault = true) int offsetTicks,
    @JsonField(skipIfDefault = true, defaultValue = "info") String type
) {
    
    /** Empty notification (no message). */
    public static final PhaseNotification NONE = new PhaseNotification("", 0, "info");
    
    /**
     * Compact constructor with validation.
     */
    public PhaseNotification {
        if (message == null) message = "";
        if (type == null || type.isEmpty()) type = "info";
    }
    
    /**
     * Returns true if this notification has a message to show.
     */
    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }
    
    /**
     * Returns true if this is a pre-warning (shows before phase).
     */
    public boolean isPreWarning() {
        return offsetTicks < 0;
    }
    
    /**
     * Returns the absolute tick offset (always positive).
     */
    public int absoluteOffset() {
        return Math.abs(offsetTicks);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates an info notification at phase start.
     */
    public static PhaseNotification info(String message) {
        return new PhaseNotification(message, 0, "info");
    }
    
    /**
     * Creates a warning notification before phase start.
     * 
     * @param message Warning message
     * @param ticksBefore How many ticks before phase to show (positive value)
     */
    public static PhaseNotification warning(String message, int ticksBefore) {
        return new PhaseNotification(message, -Math.abs(ticksBefore), "warning");
    }
    
    /**
     * Creates a danger notification at phase start.
     */
    public static PhaseNotification danger(String message) {
        return new PhaseNotification(message, 0, "danger");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses from JSON.
     */
    public static PhaseNotification fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        String message = json.has("message") ? json.get("message").getAsString() : "";
        int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
        String type = json.has("type") ? json.get("type").getAsString() : "info";
        
        return new PhaseNotification(message, offset, type);
    }
    
    /**
     * Serializes to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
