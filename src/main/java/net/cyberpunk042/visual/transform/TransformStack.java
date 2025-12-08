package net.cyberpunk042.visual.transform;

import net.cyberpunk042.log.Logging;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stack-based transform management for nested transformations.
 * 
 * <p>Similar to OpenGL's matrix stack, allows pushing and popping
 * transforms to create hierarchical transformations.
 * 
 * <h2>Usage</h2>
 * <pre>
 * TransformStack stack = new TransformStack();
 * 
 * // Base transform
 * stack.push(Transform.offset(0, 1, 0));
 * 
 * // Nested transform (combines with parent)
 * stack.push(Transform.scale(0.5f));
 * Transform combined = stack.current(); // offset + scale
 * 
 * // Pop back to parent
 * stack.pop();
 * Transform parent = stack.current(); // just offset
 * </pre>
 * 
 * @see Transform
 */
public final class TransformStack {
    
    private static final int MAX_DEPTH = 32;
    
    private final Deque<Transform> stack = new ArrayDeque<>();
    
    /**
     * Creates a new transform stack with identity transform.
     */
    public TransformStack() {
        stack.push(Transform.identity());
    }
    
    /**
     * Creates a transform stack with an initial transform.
     */
    public TransformStack(Transform initial) {
        stack.push(initial != null ? initial : Transform.identity());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Stack Operations
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Pushes a transform onto the stack, combining with current.
     * 
     * @param transform Transform to apply
     * @return this for chaining
     */
    public TransformStack push(Transform transform) {
        if (stack.size() >= MAX_DEPTH) {
            Logging.RENDER.topic("transform").warn(
                "TransformStack overflow (max {}), ignoring push", MAX_DEPTH);
            return this;
        }
        
        Transform current = stack.peek();
        Transform combined = combine(current, transform);
        stack.push(combined);
        
        Logging.RENDER.topic("transform").trace(
            "Push transform (depth={})", stack.size());
        return this;
    }
    
    /**
     * Pops the top transform, returning to parent state.
     * 
     * @return The popped transform
     */
    public Transform pop() {
        if (stack.size() <= 1) {
            Logging.RENDER.topic("transform").warn(
                "TransformStack underflow, cannot pop below identity");
            return stack.peek();
        }
        
        Transform popped = stack.pop();
        Logging.RENDER.topic("transform").trace(
            "Pop transform (depth={})", stack.size());
        return popped;
    }
    
    /**
     * Gets the current combined transform.
     */
    public Transform current() {
        return stack.peek();
    }
    
    /**
     * Gets the current stack depth.
     */
    public int depth() {
        return stack.size();
    }
    
    /**
     * Checks if stack is at root level.
     */
    public boolean isAtRoot() {
        return stack.size() == 1;
    }
    
    /**
     * Resets to identity transform.
     */
    public void reset() {
        stack.clear();
        stack.push(Transform.identity());
        Logging.RENDER.topic("transform").trace("TransformStack reset");
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Convenience Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Pushes an offset transform.
     */
    public TransformStack pushOffset(double x, double y, double z) {
        return push(Transform.offset(x, y, z));
    }
    
    /**
     * Pushes a scale transform.
     */
    public TransformStack pushScale(float scale) {
        return push(Transform.scaled(scale));
    }
    
    /**
     * Pushes a rotation transform (Y-axis).
     */
    public TransformStack pushRotationY(float degrees) {
        return push(new Transform(null, new Vec3d(0, degrees, 0), 1.0f));
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Transform Combination
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Combines two transforms (parent * child).
     */
    private static Transform combine(Transform parent, Transform child) {
        if (parent == null) return child;
        if (child == null) return parent;
        
        // Combine offsets
        Vec3d parentOffset = parent.offset() != null ? parent.offset() : Vec3d.ZERO;
        Vec3d childOffset = child.offset() != null ? child.offset() : Vec3d.ZERO;
        Vec3d combinedOffset = parentOffset.add(
            childOffset.multiply(parent.scale()) // Scale child offset by parent scale
        );
        
        // Combine rotations
        Vec3d parentRot = parent.rotation() != null ? parent.rotation() : Vec3d.ZERO;
        Vec3d childRot = child.rotation() != null ? child.rotation() : Vec3d.ZERO;
        Vec3d combinedRotation = parentRot.add(childRot);
        
        // Combine scales
        float combinedScale = parent.scale() * child.scale();
        
        return new Transform(combinedOffset, combinedRotation, combinedScale);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Scoped Operations
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Executes an action with a temporary transform.
     * Automatically pops when done.
     */
    public void withTransform(Transform transform, Runnable action) {
        push(transform);
        try {
            action.run();
        } finally {
            pop();
        }
    }
    
    /**
     * Executes an action at a specific offset.
     */
    public void atOffset(double x, double y, double z, Runnable action) {
        withTransform(Transform.offset(x, y, z), action);
    }
    
    /**
     * Executes an action with a scale.
     */
    public void withScale(float scale, Runnable action) {
        withTransform(Transform.scaled(scale), action);
    }
}
