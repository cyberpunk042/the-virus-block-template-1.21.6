package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.*;

/**
 * Interface for mesh tessellation strategies.
 * Implementations generate meshes for different primitive shapes.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Static convenience method (recommended)
 * Mesh mesh = Tessellator.tessellate(new SphereShape(1.0f), detail);
 * 
 * // Or use specific tessellator builders for more control
 * Mesh mesh = SphereTessellator_old.create().radius(1.0f).latSteps(32).tessellate(0);
 * </pre>
 * 
 * @see SphereTessellator_old
 * @see RingTessellator_old
 * @see PrismTessellator_old
 * @see PolyhedraTessellator_old
 */
public interface Tessellator {
    
    // ========================================================================
    // Static Factory Method (per ARCHITECTURE.md)
    // ========================================================================
    
    /**
     * Tessellates a shape into a mesh with the given level of detail.
     * 
     * <p>This is the primary API for tessellation. It automatically selects
     * the appropriate tessellator based on shape type.
     * 
     * @param shape The shape to tessellate
     * @param detail Level of detail (higher = more vertices)
     * @return Generated mesh
     * @throws IllegalArgumentException if shape type is not supported
     */
    static Mesh tessellate(Shape shape, int detail) {
        if (shape == null) {
            throw new IllegalArgumentException("Shape cannot be null");
        }
        
        Logging.RENDER.topic("tessellate").debug(
            "Tessellating {} with detail={}", shape.getClass().getSimpleName(), detail);
        
        return switch (shape) {
            case SphereShape s -> SphereTessellator_old.create()
                .radius(s.radius())
                .latSteps(Math.max(8, detail))
                .lonSteps(Math.max(8, detail * 2))
                .tessellate(detail);
                
            case RingShape r -> RingTessellator_old.create()
                .y(r.y())
                .innerRadius(r.radius() - r.thickness() / 2)
                .outerRadius(r.radius() + r.thickness() / 2)
                .segments(Math.max(8, detail))
                .tessellate(detail);
                
            case PrismShape p -> PrismTessellator_old.create()
                .sides(p.sides())
                .height(p.height())
                .radius(p.radius())
                .tessellate(detail);
                
            case PolyhedronShape p -> null; // TODO: Implement polyhedron tessellation
                
            case CylinderShape b -> PrismTessellator_old.create()
                .sides(b.segments())
                .height(b.height())
                .radius(b.radius())
                .tessellate(detail);
                
            case DiscShape d -> RingTessellator_old.create()
                .y(d.y())
                .innerRadius(0)
                .outerRadius(d.radius())
                .segments(Math.max(8, detail))
                .tessellate(detail);
                
            default -> {
                Logging.RENDER.topic("tessellate").warn(
                    "Unknown shape type: {}, returning empty mesh", shape.getClass().getSimpleName());
                yield Mesh.empty();
            }
        };
    }
    
    // ========================================================================
    // Instance Method
    // ========================================================================
    
    /**
     * Generates a mesh with the given level of detail.
     * Higher detail = more vertices/triangles.
     * 
     * @param detail Level of detail (interpretation depends on implementation)
     * @return Generated mesh
     */
    Mesh tessellate(int detail);
    
    /**
     * Returns a default detail level appropriate for this tessellator.
     */
    default int defaultDetail() {
        return 16;
    }
    
    /**
     * Returns the minimum valid detail level.
     */
    default int minDetail() {
        return 4;
    }
    
    /**
     * Returns the maximum valid detail level.
     */
    default int maxDetail() {
        return 128;
    }
    
    /**
     * Clamps detail to valid range.
     */
    default int clampDetail(int detail) {
        return Math.max(minDetail(), Math.min(maxDetail(), detail));
    }
}
