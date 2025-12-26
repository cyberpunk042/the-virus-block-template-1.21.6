package net.cyberpunk042.visual.shape;

/**
 * Pure math functions for polar shape deformation.
 * 
 * <p>These functions compute deformed vertex positions from spherical coordinates.
 * θ=0 is the "tip" (top/north pole), θ=π is the "base" (bottom/south pole).</p>
 * 
 * <h2>Shape Categories</h2>
 * <ul>
 *   <li><b>Symmetric:</b> Sphere, Spheroid (oblate/prolate), Ellipsoid</li>
 *   <li><b>Organic:</b> Ovoid, Egg, Pear</li>
 *   <li><b>Directional:</b> Teardrop/Droplet, Bullet, Cone</li>
 * </ul>
 * 
 * <h2>Key Parameters</h2>
 * <ul>
 *   <li><b>length:</b> Axial stretch (1 = sphere, &gt;1 = prolate, &lt;1 = oblate)</li>
 *   <li><b>intensity:</b> Deformation strength (0 = sphere, 1 = full effect)</li>
 * </ul>
 * 
 * @see SphereDeformation
 */
public final class ShapeMath {
    
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = (float) (Math.PI * 0.5);
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    private ShapeMath() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Radius Factor Functions (legacy - for simple radius scaling)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Sphere: constant radius. */
    public static float sphere(float theta) {
        return 1.0f;
    }
    
    /** Droplet radius factor: sin(θ/2)^power */
    public static float droplet(float theta, float power) {
        float base = (float) Math.sin(theta * 0.5f);
        return (float) Math.pow(Math.max(0.0001f, base), power);
    }
    
    /** Egg radius factor: 1 + asymmetry × cos(θ) */
    public static float egg(float theta, float asymmetry) {
        return 1.0f + asymmetry * (float) Math.cos(theta);
    }
    
    /** Bullet radius factor */
    public static float bullet(float theta) {
        if (theta < HALF_PI) {
            return (float) Math.sin(theta);
        }
        return 1.0f;
    }
    
    /** Cone radius factor: θ/π */
    public static float cone(float theta) {
        return theta / PI;
    }
    
    /** Inverted droplet radius factor */
    public static float dropletInverted(float theta, float power) {
        return droplet(PI - theta, power);
    }
    
    /** Blends between sphere and shape */
    public static float blend(float shapeFactor, float intensity) {
        return 1.0f + (shapeFactor - 1.0f) * Math.min(1.0f, intensity);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Direct Vertex Position Functions (Proper Parametric Geometry)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sphere vertex position (standard spherical coordinates).
     */
    public static float[] sphereVertex(float theta, float phi, float radius) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        return new float[] {
            radius * sinTheta * cosPhi,  // X
            radius * cosTheta,           // Y (up)
            radius * sinTheta * sinPhi   // Z
        };
    }
    
    /**
     * Astrophysical spheroid vertex position (oblate with equatorial bulge).
     * 
     * <p><b>True oblate spheroid (like planets):</b><br>
     * When length &lt; 1: polar axis compresses AND equator bulges outward.<br>
     * When length &gt; 1: polar axis stretches AND equator contracts.<br>
     * This matches how rotating planets deform (centrifugal force at equator).</p>
     * 
     * <p>Formula uses volume-preserving deformation where a² * c = r³<br>
     * For oblate (c &lt; r): a = r * sqrt(r/c) = r / sqrt(length)<br>
     * For prolate (c &gt; r): a = r * sqrt(r/c) = r / sqrt(length)</p>
     * 
     * @param theta Polar angle (0 = top pole, π = bottom pole)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius (sphere radius at length=1)
     * @param length Polar axis ratio: &lt;1 = oblate (disc), &gt;1 = prolate (football)
     */
    public static float[] spheroidVertex(float theta, float phi, float radius, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Polar radius (c) = radius * length
        float c = radius * length;
        
        // Equatorial radius (a) using volume-preserving formula: a²c = r³
        // a = r * sqrt(r/c) = r / sqrt(length)
        float a = radius / (float) Math.sqrt(length);
        
        return new float[] {
            a * sinTheta * cosPhi,      // X (equatorial, bulges when length < 1)
            c * cosTheta,               // Y (polar axis)
            a * sinTheta * sinPhi       // Z (equatorial, bulges when length < 1)
        };
    }
    
    /**
     * Ellipsoid vertex position (three unequal axes).
     * 
     * @param theta Polar angle
     * @param phi Azimuthal angle
     * @param radius Base radius (X axis)
     * @param lengthY Y axis scale factor
     * @param lengthZ Z axis scale factor
     */
    public static float[] ellipsoidVertex(float theta, float phi, float radius, 
            float lengthY, float lengthZ) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        return new float[] {
            radius * sinTheta * cosPhi,           // X
            radius * lengthY * cosTheta,          // Y (scaled)
            radius * lengthZ * sinTheta * sinPhi  // Z (scaled)
        };
    }
    
    /**
     * Ovoid vertex position (smooth egg-like, symmetric but stretched at one end).
     * 
     * <p>Similar to egg but with softer asymmetry.</p>
     * 
     * @param theta Polar angle
     * @param phi Azimuthal angle
     * @param radius Base radius
     * @param asymmetry Asymmetry factor (0 = sphere, 0.2-0.4 = typical ovoid)
     * @param length Axial stretch
     */
    public static float[] ovoidVertex(float theta, float phi, float radius, 
            float asymmetry, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Ovoid profile: smoother transition than egg
        // Use cosine-based asymmetry that's gentler
        float radiusFactor = 1.0f + asymmetry * 0.5f * (1.0f + (float) Math.cos(theta));
        float modRadius = radius * radiusFactor;
        
        return new float[] {
            modRadius * sinTheta * cosPhi,  // X
            radius * length * cosTheta,     // Y (axial)
            modRadius * sinTheta * sinPhi   // Z
        };
    }
    
    /**
     * Egg vertex position (asymmetric - fatter at bottom).
     */
    public static float[] eggVertex(float theta, float phi, float radius, 
            float asymmetry, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Egg profile: wider at bottom (θ=π), narrower at top (θ=0)
        float radiusFactor = 1.0f + asymmetry * (float) Math.cos(theta);
        float modRadius = radius * radiusFactor;
        
        return new float[] {
            modRadius * sinTheta * cosPhi,  // X
            radius * length * cosTheta,     // Y (axial)
            modRadius * sinTheta * sinPhi   // Z
        };
    }
    
    /**
     * Pear vertex position (strong base mass, tapered top).
     * 
     * <p>Uses piriform (pear-shaped) curve profile.</p>
     * 
     * @param theta Polar angle (0 = narrow top, π = wide base)
     * @param phi Azimuthal angle
     * @param radius Base radius at widest point
     * @param intensity Pear-ness (0 = sphere, 1 = strong pear)
     * @param length Axial stretch
     */
    public static float[] pearVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Piriform profile: (1 + sin(θ)) * cos(θ) for radial component
        // Creates a pear shape with narrow top and wide bottom
        float normalized = theta / PI;  // 0 to 1
        float pearProfile = (float) Math.pow(normalized, 0.7f + 0.3f * intensity);
        
        // Blend with sphere
        float sphereRadius = radius * sinTheta;
        float pearRadius = radius * pearProfile * sinTheta;
        float finalRadius = sphereRadius * (1 - intensity) + pearRadius * intensity;
        
        return new float[] {
            finalRadius * cosPhi,           // X
            radius * length * cosTheta,     // Y (axial)
            finalRadius * sinPhi            // Z
        };
    }
    
    /**
     * Droplet/teardrop vertex position.
     * 
     * <p>Pointed tip at θ=0, rounded base at θ=π.</p>
     */
    public static float[] dropletVertex(float theta, float phi, float radius, 
            float power, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Teardrop profile: (1 - cos(θ)) creates the taper
        float profile = (1.0f - cosTheta) * (float) Math.pow(Math.abs(sinTheta), power);
        float normalizedProfile = profile * radius * 0.5f;
        
        return new float[] {
            normalizedProfile * cosPhi,     // X
            radius * length * cosTheta,     // Y (axial)
            normalizedProfile * sinPhi      // Z
        };
    }
    
    /**
     * Droplet inverted vertex position (pointy at bottom).
     */
    public static float[] dropletInvertedVertex(float theta, float phi, float radius, 
            float power, float length) {
        float[] pos = dropletVertex(PI - theta, phi, radius, power, length);
        pos[1] = -pos[1];  // Flip Y
        return pos;
    }
    
    /**
     * Bullet/capsule-tip vertex position.
     * 
     * <p>Hemisphere on top, cylinder below.</p>
     */
    public static float[] bulletVertex(float theta, float phi, float radius, float length) {
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        float x, y, z;
        
        if (theta < HALF_PI) {
            // Top hemisphere (0 to π/2)
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            
            x = radius * sinTheta * cosPhi;
            y = radius * cosTheta;  // Top part
            z = radius * sinTheta * sinPhi;
        } else {
            // Bottom cylinder (π/2 to π)
            float t = (theta - HALF_PI) / HALF_PI;  // 0 to 1
            
            x = radius * cosPhi;
            y = -radius * length * t;  // Extends down based on length
            z = radius * sinPhi;
        }
        
        return new float[] { x, y, z };
    }
    
    /**
     * Cone vertex position.
     */
    public static float[] coneVertex(float theta, float phi, float radius, float length) {
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Linear taper: 0 at tip (θ=0), 1 at base (θ=π)
        float taper = theta / PI;
        float ringRadius = radius * taper;
        
        // Map θ to Y with length scaling
        float y = radius * length * (float) Math.cos(theta);
        
        return new float[] {
            ringRadius * cosPhi,  // X
            y,                    // Y
            ringRadius * sinPhi   // Z
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Normal Calculation (Proper Gradient-Based)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes the proper normal for a spheroid at the given position.
     * 
     * <p>For spheroid with equatorial radius a and polar radius c:
     * Normal = normalize(x/a², y/a², z/c²)</p>
     * 
     * <p>Note: For a sphere (a=c), this reduces to normalize(position).</p>
     * 
     * @param x Position X
     * @param y Position Y (polar axis)
     * @param z Position Z
     * @param a Equatorial radius
     * @param c Polar radius (a*length)
     * @return Normalized normal vector {nx, ny, nz}
     */
    public static float[] spheroidNormal(float x, float y, float z, float a, float c) {
        // Gradient of implicit surface: (x/a², y/c², z/a²)
        // Note: Y is the polar axis in our coordinate system
        float a2 = a * a;
        float c2 = c * c;
        
        float nx = x / a2;
        float ny = y / c2;  // Y is polar axis
        float nz = z / a2;
        
        return normalize(new float[] { nx, ny, nz });
    }
    
    /**
     * Normalizes a 3D vector.
     */
    public static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (len < 0.0001f) return new float[] { 0, 1, 0 };  // Default up
        return new float[] { v[0]/len, v[1]/len, v[2]/len };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Blending Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Blends between two vertex positions.
     */
    public static float[] blendVertex(float[] a, float[] b, float t) {
        float inv = 1.0f - t;
        return new float[] {
            a[0] * inv + b[0] * t,
            a[1] * inv + b[1] * t,
            a[2] * inv + b[2] * t
        };
    }
}

