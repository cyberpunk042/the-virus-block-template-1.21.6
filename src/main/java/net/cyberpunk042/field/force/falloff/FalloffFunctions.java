package net.cyberpunk042.field.force.falloff;

import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Standard falloff function implementations.
 * 
 * <p>This class provides common falloff patterns used in force field calculations.
 * All functions are stateless and thread-safe.
 * 
 * <h2>Available Functions</h2>
 * <ul>
 *   <li>{@link #CONSTANT} - No falloff (1.0 everywhere within radius)</li>
 *   <li>{@link #LINEAR} - Linear decrease from 1.0 at center to 0.0 at edge</li>
 *   <li>{@link #QUADRATIC} - Inverse square falloff (physics-accurate gravity)</li>
 *   <li>{@link #CUBIC} - Faster falloff for sharp boundaries</li>
 *   <li>{@link #GAUSSIAN} - Smooth bell curve (configurable sigma)</li>
 *   <li>{@link #EXPONENTIAL} - Exponential decay</li>
 * </ul>
 * 
 * @see FalloffFunction
 */
public final class FalloffFunctions {
    
    private FalloffFunctions() {} // No instantiation
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Standard Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Constant falloff - full strength everywhere within radius.
     * Returns 1.0 for distance < radius, 0.0 otherwise.
     */
    public static final FalloffFunction CONSTANT = (distance, radius) -> 
        distance < radius ? 1.0f : 0.0f;
    
    /**
     * Linear falloff - strength decreases linearly with distance.
     * Formula: 1 - (distance / radius)
     */
    public static final FalloffFunction LINEAR = (distance, radius) -> {
        if (radius <= 0) return 0.0f;
        return MathHelper.clamp(1.0f - (distance / radius), 0.0f, 1.0f);
    };
    
    /**
     * Quadratic (inverse square) falloff - physics-accurate for gravity-like forces.
     * Formula: 1 - (distance / radius)²
     * 
     * <p>Note: This is normalized to 0-1 range, not true inverse square (1/r²)
     * which would go to infinity at center.
     */
    public static final FalloffFunction QUADRATIC = (distance, radius) -> {
        if (radius <= 0) return 0.0f;
        float normalized = distance / radius;
        return MathHelper.clamp(1.0f - (normalized * normalized), 0.0f, 1.0f);
    };
    
    /**
     * Cubic falloff - sharper than quadratic, good for concentrated effects.
     * Formula: 1 - (distance / radius)³
     */
    public static final FalloffFunction CUBIC = (distance, radius) -> {
        if (radius <= 0) return 0.0f;
        float normalized = distance / radius;
        return MathHelper.clamp(1.0f - (normalized * normalized * normalized), 0.0f, 1.0f);
    };
    
    /**
     * Gaussian falloff - smooth bell curve, very natural feeling.
     * Uses sigma = radius / 3, so 99.7% of effect is within radius.
     * Formula: e^(-(distance² / (2σ²)))
     */
    public static final FalloffFunction GAUSSIAN = (distance, radius) -> {
        if (radius <= 0) return 0.0f;
        float sigma = radius / 3.0f;
        float exponent = -(distance * distance) / (2.0f * sigma * sigma);
        return (float) Math.exp(exponent);
    };
    
    /**
     * Exponential decay falloff.
     * Formula: e^(-3 * distance / radius)
     */
    public static final FalloffFunction EXPONENTIAL = (distance, radius) -> {
        if (radius <= 0) return 0.0f;
        return (float) Math.exp(-3.0f * distance / radius);
    };
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a Gaussian falloff with custom sigma.
     * 
     * @param sigmaRatio Sigma as fraction of radius (default 0.33 = 3 sigmas in radius)
     * @return Gaussian falloff function with specified sigma
     */
    public static FalloffFunction gaussian(float sigmaRatio) {
        return (distance, radius) -> {
            if (radius <= 0) return 0.0f;
            float sigma = radius * sigmaRatio;
            float exponent = -(distance * distance) / (2.0f * sigma * sigma);
            return (float) Math.exp(exponent);
        };
    }
    
    /**
     * Creates a power falloff with custom exponent.
     * Formula: 1 - (distance / radius)^power
     * 
     * @param power The exponent (1 = linear, 2 = quadratic, 3 = cubic)
     * @return Power falloff function
     */
    public static FalloffFunction power(float power) {
        return (distance, radius) -> {
            if (radius <= 0) return 0.0f;
            float normalized = distance / radius;
            return MathHelper.clamp(1.0f - (float) Math.pow(normalized, power), 0.0f, 1.0f);
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Lookup
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets a falloff function by name.
     * 
     * @param name Falloff name (constant, linear, quadratic, cubic, gaussian, exponential)
     * @return The matching function, or LINEAR as default
     */
    @NotNull
    public static FalloffFunction fromName(String name) {
        if (name == null || name.isEmpty()) {
            return LINEAR;
        }
        
        return switch (name.toLowerCase()) {
            case "constant", "none" -> CONSTANT;
            case "linear" -> LINEAR;
            case "quadratic", "quad" -> QUADRATIC;
            case "cubic" -> CUBIC;
            case "gaussian", "gauss" -> GAUSSIAN;
            case "exponential", "exp" -> EXPONENTIAL;
            default -> LINEAR;
        };
    }
}
