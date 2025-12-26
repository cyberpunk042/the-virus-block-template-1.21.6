package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.client.visual.mesh.ray.RayContext;
import net.cyberpunk042.client.visual.mesh.ray.RayPositioner;
import net.cyberpunk042.client.visual.mesh.ray.RayTypeTessellator;
import net.cyberpunk042.client.visual.mesh.ray.RayTypeTessellatorRegistry;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.shape.RayType;
import net.cyberpunk042.visual.shape.RaysShape;

import java.util.Random;

/**
 * Tessellates rays shapes into meshes.
 * 
 * <h2>Architecture</h2>
 * <p>This class is the main orchestrator that:
 * <ul>
 *   <li>Loops through all rays (count × layers)</li>
 *   <li>Computes position/context via {@link RayPositioner}</li>
 *   <li>Delegates geometry generation to {@link RayTypeTessellator} implementations</li>
 * </ul>
 * 
 * <h2>Geometry</h2>
 * <p>Rays are line segments positioned according to the arrangement mode:</p>
 * <ul>
 *   <li>RADIAL - 2D star pattern on XZ plane, layers stack vertically</li>
 *   <li>SPHERICAL - 3D uniform distribution, layers create concentric shells</li>
 *   <li>PARALLEL - Grid of parallel rays pointing along +Y</li>
 *   <li>CONVERGING - Rays pointing toward center, layers create concentric shells</li>
 *   <li>DIVERGING - Rays pointing away from center, layers create concentric shells</li>
 * </ul>
 * 
 * <h2>Distribution Modes</h2>
 * <ul>
 *   <li><b>UNIFORM</b> - All rays have identical length and position within their pattern</li>
 *   <li><b>RANDOM</b> - Each ray has random start offset and random length within the radius range</li>
 *   <li><b>STOCHASTIC</b> - Heavily randomized with variable density and length</li>
 * </ul>
 * 
 * @see RaysShape
 * @see RayPositioner
 * @see RayTypeTessellator
 */
public final class RaysTessellator {
    
    private RaysTessellator() {}
    
    /**
     * Tessellates rays shape into a mesh (without wave deformation).
     * 
     * @param shape The rays shape definition
     * @return Mesh containing the ray geometry
     */
    public static Mesh tessellate(RaysShape shape) {
        return tessellate(shape, null, 0);
    }
    
    /**
     * Tessellates rays shape into a mesh with optional wave deformation.
     * 
     * @param shape The rays shape definition
     * @param wave Wave configuration (or null for no wave)
     * @param time Current time for wave animation
     * @return Mesh containing the ray geometry
     */
    public static Mesh tessellate(RaysShape shape, WaveConfig wave, float time) {
        // Choose mesh type based on ray type
        RayType rayType = shape.effectiveRayType();
        MeshBuilder builder = rayType.is3D() ? MeshBuilder.triangles() : MeshBuilder.lines();
        
        int count = shape.count();
        int layers = Math.max(1, shape.layers());
        
        Random rng = new Random(42); // Deterministic random for stable results
        
        Logging.FIELD.topic("tessellation").debug(
            "Tessellating rays: count={}, layers={}, arrangement={}, rayType={}, wave={}", 
            count, layers, shape.arrangement(), rayType, wave != null && wave.isActive());
        
        // Get the tessellator for this ray type
        RayTypeTessellator tessellator = RayTypeTessellatorRegistry.get(rayType);
        
        // Generate rays for each layer
        for (int layer = 0; layer < layers; layer++) {
            for (int i = 0; i < count; i++) {
                // Compute context for this ray
                RayContext context = RayPositioner.computeContext(shape, i, layer, rng, wave, time);
                
                // Delegate to type-specific tessellator
                tessellator.tessellate(builder, shape, context);
            }
        }
        
        return builder.build();
    }
}
