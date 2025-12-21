package net.cyberpunk042.field.force.path;

import net.minecraft.util.math.Vec3d;

/**
 * Defines a parametric 3D curve that entities can follow.
 * 
 * <p>A path is defined by P(t) where t ∈ [0, 1].
 * The path can be open (start ≠ end) or closed (loop).
 */
public interface ForcePath {
    
    /**
     * Get position on path at parameter t.
     * @param t Parameter from 0 to 1
     * @return World position on the path
     */
    Vec3d positionAt(float t);
    
    /**
     * Get tangent direction at parameter t.
     * This is the direction an entity should move along the path.
     * @param t Parameter from 0 to 1
     * @return Normalized tangent vector
     */
    Vec3d tangentAt(float t);
    
    /**
     * Find the nearest point on the path to a given position.
     * @param worldPos Position to find nearest point for
     * @return Parameter t of nearest point
     */
    float nearestT(Vec3d worldPos);
    
    /**
     * Whether this path loops (t=1 connects to t=0).
     */
    boolean isLoop();
    
    /**
     * Get the approximate total length of the path.
     */
    float length();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Path Types
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Simple circle in the XZ plane.
     */
    static ForcePath circle(Vec3d center, float radius) {
        return new CirclePath(center, radius);
    }
    
    /**
     * Infinity sign (lemniscate) in the XZ plane.
     * Entities trace a figure-8 pattern.
     */
    static ForcePath infinity(Vec3d center, float size) {
        return new InfinityPath(center, size);
    }
    
    /**
     * Vertical helix (spiral staircase).
     * Entities spiral upward around center.
     */
    static ForcePath helix(Vec3d center, float radius, float height, int turns) {
        return new HelixPath(center, radius, height, turns);
    }
    
    /**
     * Flat spiral (whirlpool).
     * Entities spiral inward or outward.
     */
    static ForcePath spiral(Vec3d center, float innerRadius, float outerRadius, int turns) {
        return new SpiralPath(center, innerRadius, outerRadius, turns);
    }
}

/**
 * Simple circle path.
 */
class CirclePath implements ForcePath {
    private final Vec3d center;
    private final float radius;
    
    CirclePath(Vec3d center, float radius) {
        this.center = center;
        this.radius = radius;
    }
    
    @Override
    public Vec3d positionAt(float t) {
        double angle = t * Math.PI * 2;
        return center.add(
            Math.cos(angle) * radius,
            0,
            Math.sin(angle) * radius
        );
    }
    
    @Override
    public Vec3d tangentAt(float t) {
        double angle = t * Math.PI * 2;
        // Tangent is perpendicular to radius
        return new Vec3d(-Math.sin(angle), 0, Math.cos(angle));
    }
    
    @Override
    public float nearestT(Vec3d worldPos) {
        Vec3d relative = worldPos.subtract(center);
        double angle = Math.atan2(relative.z, relative.x);
        if (angle < 0) angle += Math.PI * 2;
        return (float)(angle / (Math.PI * 2));
    }
    
    @Override
    public boolean isLoop() { return true; }
    
    @Override
    public float length() { return (float)(2 * Math.PI * radius); }
}

/**
 * Infinity/lemniscate path (figure-8).
 */
class InfinityPath implements ForcePath {
    private final Vec3d center;
    private final float size;
    
    InfinityPath(Vec3d center, float size) {
        this.center = center;
        this.size = size;
    }
    
    @Override
    public Vec3d positionAt(float t) {
        // Lemniscate of Bernoulli: x = a*cos(t)/(1+sin²(t)), z = a*sin(t)*cos(t)/(1+sin²(t))
        // Simplified version using parametric:
        double angle = t * Math.PI * 2;
        double sinA = Math.sin(angle);
        double cosA = Math.cos(angle);
        double denom = 1 + sinA * sinA;
        
        return center.add(
            size * cosA / denom,
            0,
            size * sinA * cosA / denom
        );
    }
    
    @Override
    public Vec3d tangentAt(float t) {
        // Numerical derivative
        float dt = 0.01f;
        Vec3d p1 = positionAt(t);
        Vec3d p2 = positionAt((t + dt) % 1f);
        Vec3d tangent = p2.subtract(p1);
        double len = tangent.length();
        return len > 0.001 ? tangent.multiply(1.0 / len) : new Vec3d(1, 0, 0);
    }
    
    @Override
    public float nearestT(Vec3d worldPos) {
        // Brute force search for lemniscate (complex curve)
        float bestT = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < 64; i++) {
            float t = i / 64f;
            double dist = worldPos.squaredDistanceTo(positionAt(t));
            if (dist < bestDist) {
                bestDist = dist;
                bestT = t;
            }
        }
        return bestT;
    }
    
    @Override
    public boolean isLoop() { return true; }
    
    @Override
    public float length() { return size * 5.2f; } // Approximate
}

/**
 * Helix path (spiral upward).
 */
class HelixPath implements ForcePath {
    private final Vec3d center;
    private final float radius;
    private final float height;
    private final int turns;
    
    HelixPath(Vec3d center, float radius, float height, int turns) {
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.turns = turns;
    }
    
    @Override
    public Vec3d positionAt(float t) {
        double angle = t * Math.PI * 2 * turns;
        return center.add(
            Math.cos(angle) * radius,
            t * height,
            Math.sin(angle) * radius
        );
    }
    
    @Override
    public Vec3d tangentAt(float t) {
        float dt = 0.01f;
        Vec3d p1 = positionAt(t);
        Vec3d p2 = positionAt(Math.min(1f, t + dt));
        Vec3d tangent = p2.subtract(p1);
        double len = tangent.length();
        return len > 0.001 ? tangent.multiply(1.0 / len) : new Vec3d(0, 1, 0);
    }
    
    @Override
    public float nearestT(Vec3d worldPos) {
        // Estimate based on height and angle
        double relativeY = worldPos.y - center.y;
        float tFromHeight = (float)(relativeY / height);
        tFromHeight = Math.max(0, Math.min(1, tFromHeight));
        
        // Refine with angle
        Vec3d relative = worldPos.subtract(center);
        double angle = Math.atan2(relative.z, relative.x);
        if (angle < 0) angle += Math.PI * 2;
        
        // Combine estimates
        return tFromHeight;
    }
    
    @Override
    public boolean isLoop() { return false; }
    
    @Override
    public float length() {
        // Helix length = sqrt((2πr*turns)² + h²)
        double circumference = 2 * Math.PI * radius * turns;
        return (float)Math.sqrt(circumference * circumference + height * height);
    }
}

/**
 * Flat spiral path (whirlpool).
 */
class SpiralPath implements ForcePath {
    private final Vec3d center;
    private final float innerRadius;
    private final float outerRadius;
    private final int turns;
    
    SpiralPath(Vec3d center, float innerRadius, float outerRadius, int turns) {
        this.center = center;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.turns = turns;
    }
    
    @Override
    public Vec3d positionAt(float t) {
        double angle = t * Math.PI * 2 * turns;
        float radius = innerRadius + (outerRadius - innerRadius) * t;
        return center.add(
            Math.cos(angle) * radius,
            0,
            Math.sin(angle) * radius
        );
    }
    
    @Override
    public Vec3d tangentAt(float t) {
        float dt = 0.01f;
        Vec3d p1 = positionAt(t);
        Vec3d p2 = positionAt(Math.min(1f, t + dt));
        Vec3d tangent = p2.subtract(p1);
        double len = tangent.length();
        return len > 0.001 ? tangent.multiply(1.0 / len) : new Vec3d(1, 0, 0);
    }
    
    @Override
    public float nearestT(Vec3d worldPos) {
        // Brute force for spiral
        float bestT = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < 64; i++) {
            float t = i / 64f;
            double dist = worldPos.squaredDistanceTo(positionAt(t));
            if (dist < bestDist) {
                bestDist = dist;
                bestT = t;
            }
        }
        return bestT;
    }
    
    @Override
    public boolean isLoop() { return false; }
    
    @Override
    public float length() {
        // Approximate
        return (float)(Math.PI * (innerRadius + outerRadius) * turns);
    }
}
