package net.cyberpunk042.field.force;

import net.minecraft.util.math.Vec3d;

/**
 * Defines the axis for rotational forces.
 * 
 * <p>Used by rotational force modes (VORTEX, ORBIT, TORNADO) to specify
 * the axis around which entities rotate or the direction of lift.
 */
public enum ForceAxis {
    
    /**
     * Rotation around the X axis (east-west).
     * Creates vertical circular motion in the YZ plane.
     */
    X("X", new Vec3d(1, 0, 0)),
    
    /**
     * Rotation around the Y axis (up-down).
     * Creates horizontal circular motion in the XZ plane.
     * This is the most common axis for vortex/tornado effects.
     */
    Y("Y", new Vec3d(0, 1, 0)),
    
    /**
     * Rotation around the Z axis (north-south).
     * Creates vertical circular motion in the XY plane.
     */
    Z("Z", new Vec3d(0, 0, 1));
    
    private final String displayName;
    private final Vec3d vector;
    
    ForceAxis(String displayName, Vec3d vector) {
        this.displayName = displayName;
        this.vector = vector;
    }
    
    /**
     * Human-readable name for GUI display.
     */
    public String displayName() {
        return displayName;
    }
    
    /**
     * Unit vector along this axis.
     */
    public Vec3d vector() {
        return vector;
    }
    
    /**
     * Calculates a tangent vector perpendicular to both this axis and the given radial direction.
     * This is the direction of tangential force for rotation around this axis.
     * 
     * @param radial Direction from entity to center (or vice versa)
     * @return Tangent vector perpendicular to both axis and radial
     */
    public Vec3d tangentFor(Vec3d radial) {
        Vec3d tangent = radial.crossProduct(vector);
        double len = tangent.length();
        if (len < 0.001) {
            // Radial is parallel to axis - use fallback
            return switch (this) {
                case X -> new Vec3d(0, 0, 1);
                case Y -> new Vec3d(1, 0, 0);
                case Z -> new Vec3d(0, 1, 0);
            };
        }
        return tangent.multiply(1.0 / len);
    }
    
    /**
     * Parses an axis from string (case-insensitive).
     * Returns Y if not recognized.
     */
    public static ForceAxis fromId(String id) {
        if (id == null || id.isBlank()) {
            return Y;
        }
        for (ForceAxis axis : values()) {
            if (axis.name().equalsIgnoreCase(id)) {
                return axis;
            }
        }
        return Y;
    }
    
    /**
     * Returns the string ID for serialization.
     */
    public String id() {
        return name();
    }
}
