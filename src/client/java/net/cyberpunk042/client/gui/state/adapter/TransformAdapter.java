package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Transform;

import java.util.Set;

/**
 * Adapter for transform state (offset, rotation, scale, orbit, etc.).
 */
@StateCategory("transform")
public class TransformAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private Transform transform = Transform.IDENTITY;
    
    @Override
    public String category() { return "transform"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.transform = orDefault(source.transform(), Transform.IDENTITY);
        Logging.GUI.topic("adapter").trace("TransformAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.transform(transform);
    }
    
    public Transform transform() { return transform; }
    public void setTransform(Transform transform) { this.transform = transform; }
    
    @Override
    public void reset() {
        this.transform = Transform.IDENTITY;
    }
}
