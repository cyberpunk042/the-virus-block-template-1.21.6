package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;

/**
 * Adapter for primitive linking state.
 * 
 * <p>Handles paths like "link.radiusOffset", "link.phaseOffset", "link.mirrorAxis".</p>
 */
@StateCategory("link")
public class LinkAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    // Current primitive ID (read-only from primitive)
    private String primitiveId = "";
    
    // Link configuration fields
    @StateField private float radiusOffset = 0f;
    @StateField private float phaseOffset = 0f;
    @StateField private String mirrorAxis = "NONE";
    @StateField private boolean followLinked = false;
    @StateField private boolean scaleWithLinked = false;
    
    // Target IDs (the actual link targets)
    @StateField private String radiusMatchTarget = null;
    @StateField private String followTarget = null;
    @StateField private String scaleWithTarget = null;
    
    @Override
    public String category() { return "link"; }
    
    @Override
    public void loadFrom(Primitive source) {
        if (source == null) {
            reset();
            return;
        }
        
        this.primitiveId = source.id() != null ? source.id() : "";
        
        PrimitiveLink link = source.link();
        if (link != null) {
            this.radiusOffset = link.radiusOffset();
            this.phaseOffset = link.phaseOffset();
            this.mirrorAxis = link.mirror() != null ? link.mirror().name() : "NONE";
            this.followLinked = link.follow() != null;
            this.scaleWithLinked = link.scaleWith() != null;
            this.radiusMatchTarget = link.radiusMatch();
            this.followTarget = link.follow();
            this.scaleWithTarget = link.scaleWith();
        } else {
            this.radiusOffset = 0f;
            this.phaseOffset = 0f;
            this.mirrorAxis = "NONE";
            this.followLinked = false;
            this.scaleWithLinked = false;
            this.radiusMatchTarget = null;
            this.followTarget = null;
            this.scaleWithTarget = null;
        }
        
        Logging.GUI.topic("adapter").trace("LinkAdapter loaded: id={}", primitiveId);
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        // Build link if any values are set
        if (hasAnyLink()) {
            Axis mirror = !"NONE".equals(mirrorAxis) ? Axis.valueOf(mirrorAxis) : null;
            
            PrimitiveLink link = new PrimitiveLink(
                radiusMatchTarget,
                radiusOffset,
                followLinked ? followTarget : null,
                mirror,
                phaseOffset,
                scaleWithLinked ? scaleWithTarget : null
            );
            builder.link(link);
        }
    }
    
    private boolean hasAnyLink() {
        return radiusOffset != 0f || phaseOffset != 0f || !"NONE".equals(mirrorAxis)
            || followLinked || scaleWithLinked || radiusMatchTarget != null;
    }
    
    /**
     * Override get to handle paths like "link.primitiveId", "link.radiusOffset", etc.
     */
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            case "primitiveId" -> primitiveId;
            case "radiusOffset" -> radiusOffset;
            case "phaseOffset" -> phaseOffset;
            case "mirrorAxis" -> mirrorAxis;
            case "followLinked" -> followLinked;
            case "scaleWithLinked" -> scaleWithLinked;
            case "radiusMatchTarget" -> radiusMatchTarget;
            case "followTarget" -> followTarget;
            case "scaleWithTarget" -> scaleWithTarget;
            default -> super.get(path);
        };
    }
    
    /**
     * Override set to handle paths like "link.radiusOffset", etc.
     */
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        switch (prop) {
            case "primitiveId" -> {} // Read-only
            case "radiusOffset" -> this.radiusOffset = toFloat(value);
            case "phaseOffset" -> this.phaseOffset = toFloat(value);
            case "mirrorAxis" -> this.mirrorAxis = value != null ? value.toString() : "NONE";
            case "followLinked" -> this.followLinked = toBool(value);
            case "scaleWithLinked" -> this.scaleWithLinked = toBool(value);
            case "radiusMatchTarget" -> this.radiusMatchTarget = value != null ? value.toString() : null;
            case "followTarget" -> this.followTarget = value != null ? value.toString() : null;
            case "scaleWithTarget" -> this.scaleWithTarget = value != null ? value.toString() : null;
            default -> super.set(path, value);
        }
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    // Typed accessors
    public String primitiveId() { return primitiveId; }
    public float radiusOffset() { return radiusOffset; }
    public float phaseOffset() { return phaseOffset; }
    public String mirrorAxis() { return mirrorAxis; }
    public boolean followLinked() { return followLinked; }
    public boolean scaleWithLinked() { return scaleWithLinked; }
    
    @Override
    public void reset() {
        this.primitiveId = "";
        this.radiusOffset = 0f;
        this.phaseOffset = 0f;
        this.mirrorAxis = "NONE";
        this.followLinked = false;
        this.scaleWithLinked = false;
        this.radiusMatchTarget = null;
        this.followTarget = null;
        this.scaleWithTarget = null;
    }
}
