package net.cyberpunk042.field.force.falloff;

/**
 * Function that determines force strength based on distance from center.
 * 
 * <p>Falloff functions return a multiplier (typically 0.0 to 1.0) that scales
 * the base force strength. At distance 0, multiplier is typically 1.0 (full strength).
 * At distance >= radius, multiplier is typically 0.0 (no effect).
 * 
 * <h2>Common Falloff Types</h2>
 * <ul>
 *   <li><b>Linear</b>: Strength decreases linearly with distance</li>
 *   <li><b>Quadratic</b>: Strength decreases with square of distance (gravity-like)</li>
 *   <li><b>Gaussian</b>: Smooth bell curve falloff</li>
 *   <li><b>Constant</b>: No falloff, full strength everywhere</li>
 * </ul>
 * 
 * @see FalloffFunctions
 */
@FunctionalInterface
public interface FalloffFunction {
    
    /**
     * Calculates the force multiplier at a given distance.
     * 
     * @param distance Distance from force field center (>= 0)
     * @param radius   Maximum effect radius of the force field
     * @return Multiplier for force strength, typically 0.0 to 1.0
     */
    float apply(float distance, float radius);
}
