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
     * Spheroid with independent equatorial bulge control (planetary distortion).
     * 
     * <p>Unlike volume-preserving spheroidVertex, this allows independent control:
     * <ul>
     *   <li><b>length:</b> Controls polar axis stretch (c = radius * length)</li>
     *   <li><b>bulge:</b> Controls additional equatorial bulge (0 = no extra, 0.5 = 50% wider)</li>
     * </ul>
     * </p>
     * 
     * <p>This simulates a spinning planet where centrifugal force makes the equator
     * bulge outward independently of how tall/short the poles are.</p>
     * 
     * @param theta Polar angle (0 = top pole, π = bottom pole)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param length Polar axis ratio (1 = sphere height, <1 = squashed, >1 = stretched)
     * @param bulge Equatorial bulge factor (0 = sphere width, 0.5 = 50% wider equator)
     */
    public static float[] spheroidWithBulge(float theta, float phi, float radius, 
            float length, float bulge) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Polar radius (c) = radius * length - controlled by length slider
        float c = radius * length;
        
        // Equatorial radius (a) = radius * (1 + bulge) - controlled by intensity slider
        // bulge = 0 -> sphere radius (a = radius)
        // bulge = 0.5 -> 50% extra equatorial width (a = radius * 1.5)
        float a = radius * (1.0f + bulge);
        
        return new float[] {
            a * sinTheta * cosPhi,      // X (equatorial, bulges with intensity)
            c * cosTheta,               // Y (polar axis, controlled by length)
            a * sinTheta * sinPhi       // Z (equatorial, bulges with intensity)
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
     * <p>Hemisphere on top, cylinder in middle, flat cap at bottom.</p>
     */
    public static float[] bulletVertex(float theta, float phi, float radius, float length) {
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        float x, y, z;
        
        // Use last 10% of theta range for the flat base cap
        float capStart = PI * 0.9f;
        
        if (theta < HALF_PI) {
            // Top hemisphere (0 to π/2)
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            
            x = radius * sinTheta * cosPhi;
            y = radius * cosTheta;  // Top part
            z = radius * sinTheta * sinPhi;
        } else if (theta < capStart) {
            // Middle cylinder (π/2 to 0.9π)
            float t = (theta - HALF_PI) / (capStart - HALF_PI);  // 0 to 1
            
            x = radius * cosPhi;
            y = -radius * length * t;  // Extends down based on length
            z = radius * sinPhi;
        } else {
            // Bottom cap (0.9π to π) - flat circular disc
            float t = (theta - capStart) / (PI - capStart);  // 0 to 1
            float capRadius = radius * (1.0f - t);  // Shrinks from full radius to 0
            
            x = capRadius * cosPhi;
            y = -radius * length;  // Fixed at bottom
            z = capRadius * sinPhi;
        }
        
        return new float[] { x, y, z };
    }
    
    /**
     * Cone vertex position.
     * 
     * <p>Tapered sides from tip to base, flat cap at bottom.</p>
     */
    public static float[] coneVertex(float theta, float phi, float radius, float length) {
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Use last 10% of theta range for the flat base cap
        float capStart = PI * 0.9f;
        
        float x, y, z;
        
        if (theta < capStart) {
            // Tapered cone surface (0 to 0.9π)
            // Linear taper: 0 at tip (θ=0), full radius at capStart
            float taper = theta / capStart;
            float ringRadius = radius * taper;
            
            // Y position: tip is at top (y = radius*length), base cap is at bottom (y = -radius*length)
            // Map theta [0, capStart] to y [radius*length, -radius*length]
            float yProgress = theta / capStart;  // 0 to 1
            y = radius * length * (1.0f - 2.0f * yProgress);  // Goes from +length to -length
            
            x = ringRadius * cosPhi;
            z = ringRadius * sinPhi;
        } else {
            // Bottom cap (0.9π to π) - flat circular disc
            float t = (theta - capStart) / (PI - capStart);  // 0 to 1
            float capRadius = radius * (1.0f - t);  // Shrinks from full radius to 0
            
            x = capRadius * cosPhi;
            y = -radius * length;  // Fixed at bottom
            z = capRadius * sinPhi;
        }
        
        return new float[] { x, y, z };
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
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Cloud Deformation (Fluffy, Billowing Shape)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Cloud vertex position using multi-octave spherical noise.
     * 
     * <p>Creates fluffy, billowing cloud shapes by combining multiple frequencies
     * of sine-wave displacement. The result mimics cumulus cloud formations.</p>
     * 
     * <p><b>Magic Formula:</b> Combines 4 octaves of spherical harmonics:
     * <ul>
     *   <li>Octave 1: Large bulges (frequency 2-3)</li>
     *   <li>Octave 2: Medium bumps (frequency 4-6)</li>
     *   <li>Octave 3: Small details (frequency 7-9)</li>
     *   <li>Octave 4: Fine texture (frequency 11-13)</li>
     * </ul>
     * Each octave has decreasing amplitude (1, 0.5, 0.25, 0.125).</p>
     * 
     * @param theta Polar angle (0 = top, π = bottom)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param intensity Cloud "fluffiness" (0 = sphere, 1 = very fluffy)
     * @param length Axial stretch (1 = spherical, >1 = elongated cloud)
     * @return {x, y, z} vertex position
     */
    public static float[] cloudVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // At intensity 0, return pure sphere
        if (intensity < 0.05f) {
            float baseX = radius * sinTheta * cosPhi;
            float baseY = radius * length * cosTheta;
            float baseZ = radius * sinTheta * sinPhi;
            return new float[] { baseX, baseY, baseZ };
        }
        
        // ======================================================================
        // SPHERICAL HARMONICS FORMULA (from research)
        // r'(θ, φ) = R_base + Σ f_l^m * Y_l^m(θ, φ)
        // Using Real Spherical Harmonics for easier computation
        // ======================================================================
        
        // Real Spherical Harmonics Y_l^m(θ, φ)
        // l=0: Y_0^0 = 0.5 * sqrt(1/π) ≈ 0.282 (constant, just sphere - skip)
        
        // l=1: Dipole terms (large-scale asymmetry)
        float y1_0 = cosTheta;  // Y_1^0 ∝ cos(θ) - vertical asymmetry
        float y1_1 = sinTheta * cosPhi;  // Y_1^1 ∝ sin(θ)cos(φ) - x-direction
        float y1_m1 = sinTheta * sinPhi;  // Y_1^-1 ∝ sin(θ)sin(φ) - z-direction
        
        // l=2: Quadrupole terms (4-lobe patterns)
        float cos2Theta = 2 * cosTheta * cosTheta - 1;
        float y2_0 = cos2Theta;  // Y_2^0 ∝ (3cos²θ - 1)
        float y2_1 = sinTheta * cosTheta * cosPhi;  // Y_2^1
        float y2_2 = sinTheta * sinTheta * (float) Math.cos(2 * phi);  // Y_2^2
        
        // l=3: Octupole terms (8-lobe patterns)
        float y3_0 = cosTheta * (5 * cosTheta * cosTheta - 3) * 0.5f;  // Y_3^0
        float y3_1 = sinTheta * (5 * cosTheta * cosTheta - 1) * cosPhi * 0.25f;  // Y_3^1
        float y3_2 = sinTheta * sinTheta * cosTheta * (float) Math.cos(2 * phi);  // Y_3^2
        float y3_3 = sinTheta * sinTheta * sinTheta * (float) Math.cos(3 * phi);  // Y_3^3
        
        // l=4: 16-lobe patterns (fine detail)
        float y4_0 = (35 * cosTheta * cosTheta * cosTheta * cosTheta 
                    - 30 * cosTheta * cosTheta + 3) * 0.125f;  // Y_4^0
        float y4_4 = sinTheta * sinTheta * sinTheta * sinTheta 
                    * (float) Math.cos(4 * phi);  // Y_4^4
        
        // ======================================================================
        // COEFFICIENTS: Create cloud-like shape with billowing lobes
        // ======================================================================
        
        // Intensity controls the amplitude - higher intensity = more dramatic
        float amp = intensity * 0.5f;  // Max 50% displacement
        
        // Combine harmonics with pseudo-random coefficients for organic look
        // The coefficients are chosen to create asymmetric, cloud-like bumps
        float displacement = 
            // l=1: Large asymmetric bulges
            amp * 0.3f * (0.8f * y1_0 + 0.6f * y1_1 + 0.4f * y1_m1) +
            // l=2: Medium 4-lobe pattern
            amp * 0.4f * (0.5f * y2_0 + 0.7f * y2_1 + 0.3f * y2_2) +
            // l=3: Smaller 8-lobe bumps
            amp * 0.5f * (0.4f * y3_0 + 0.5f * y3_1 + 0.6f * y3_2 + 0.3f * y3_3) +
            // l=4: Fine detail (cauliflower texture)
            amp * 0.3f * (0.3f * y4_0 + 0.4f * y4_4);
        
        // Make displacement always positive (outward bulges only)
        // This creates the characteristic cumulus "puffiness"
        displacement = 0.5f + displacement * 0.5f;  // Map to 0.5-1.0 range
        displacement = Math.max(0.3f, displacement);  // Minimum 30% radius
        
        // Add high-frequency noise for surface texture
        float noise = 
            0.1f * (float) Math.sin(7 * theta + 0.3f) * (float) Math.sin(11 * phi + 1.2f) +
            0.05f * (float) Math.sin(13 * theta + 2.1f) * (float) Math.cos(9 * phi + 0.7f);
        
        float radiusFactor = displacement + noise * intensity;
        
        // Apply to base position
        float baseX = radius * sinTheta * cosPhi * radiusFactor;
        float baseY = radius * length * cosTheta * radiusFactor;
        float baseZ = radius * sinTheta * sinPhi * radiusFactor;
        
        return new float[] { baseX, baseY, baseZ };
    }

    /**
     * Molecule vertex position using METABALL IMPLICIT SURFACE formula.
     * 
     * <p><b>RESEARCH-BASED MAGIC FORMULA (Jim Blinn, 1982):</b>
     * Uses the metaball field equation: Influence = R² / distance²
     * 
     * <p>Intensity controls number of atomic lobes:
     * <ul>
     *   <li>0.0 = pure sphere (center only)</li>
     *   <li>0.1 = 1 atom (diatomic)</li>
     *   <li>0.2 = 2 atoms (like H2O)</li>
     *   <li>0.3 = 3 atoms (like NH3)</li>
     *   <li>0.4 = 4 atoms (like CH4 - tetrahedral)</li>
     *   <li>0.5-1.0 = 5-10 atoms (complex molecule)</li>
     * </ul>
     * 
     * <p>Each atom creates a spherical influence field that blends
     * smoothly with neighbors using the metaball equation.</p>
     * 
     * @param theta Polar angle (0 = top, π = bottom)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param intensity Atom count control (0 = sphere, 1 = 10 atoms)
     * @param length Axial stretch
     * @return {x, y, z} vertex position
     */
    public static float[] moleculeVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Surface point direction (normalized)
        float nx = sinTheta * cosPhi;
        float ny = cosTheta;
        float nz = sinTheta * sinPhi;
        
        // At intensity 0, return pure sphere
        if (intensity < 0.05f) {
            float baseX = radius * nx;
            float baseY = radius * length * ny;
            float baseZ = radius * nz;
            return new float[] { baseX, baseY, baseZ };
        }
        
        // Number of atoms: intensity 0.1 = 1 atom, 1.0 = 10 atoms
        int numAtoms = (int) Math.ceil(intensity * 10.0f);
        numAtoms = Math.max(1, Math.min(10, numAtoms));
        
        // Smooth transition for partial atoms
        float partialAtom = (intensity * 10.0f) - (numAtoms - 1);
        partialAtom = Math.max(0f, Math.min(1f, partialAtom));
        
        // Atom positions: golden angle spiral distribution
        // Atoms are placed at distance atomOffset from center
        float goldenAngle = 2.399963f;
        float atomOffset = 0.6f;  // How far atoms are from center
        float atomRadius = 0.5f;  // Size of each atom sphere
        
        // ==== METABALL FIELD CALCULATION ====
        // For each point on surface, calculate sum of all atom influences
        // Influence_i = R² / distance² where distance is from point to atom center
        
        float totalField = 0f;
        float centralField = 1.0f;  // Central sphere always contributes
        
        for (int i = 0; i < numAtoms; i++) {
            // Atom direction using golden angle spiral
            float t = (float) i / Math.max(1, numAtoms - 1);
            float atomTheta = (float) Math.acos(1.0 - 1.6 * t);  // 0° to ~100° from top
            float atomPhi = goldenAngle * i;
            
            float ax = (float) Math.sin(atomTheta) * (float) Math.cos(atomPhi);
            float ay = (float) Math.cos(atomTheta);
            float az = (float) Math.sin(atomTheta) * (float) Math.sin(atomPhi);
            
            // Atom center at offset from origin
            float acx = ax * atomOffset;
            float acy = ay * atomOffset;
            float acz = az * atomOffset;
            
            // Distance from surface point direction to atom center
            // We use (1 - dot) as proxy for angular distance
            float dot = nx * ax + ny * ay + nz * az;
            float angularDist = 1.0f - dot;  // 0 when pointing at atom, 2 when opposite
            
            // Metaball field: R² / (distance² + epsilon)
            // Higher value = closer to atom = more "pull"
            float distSq = angularDist * angularDist + 0.01f;
            float atomField = (atomRadius * atomRadius) / distSq;
            
            // Last atom fades in smoothly
            if (i == numAtoms - 1) {
                atomField *= partialAtom;
            }
            
            // ADDITIVE blending for metaball effect
            totalField += atomField;
        }
        
        // Surface displacement based on total field
        // Field threshold: where field >= 1.0, we get surface
        // Map total field to radius displacement
        float fieldEffect = Math.min(2.0f, totalField);  // Cap at 2x
        float radiusFactor = 0.7f + fieldEffect * 0.4f;  // Range 0.7 to 1.5
        
        // Apply to base position
        float baseX = radius * nx * radiusFactor;
        float baseY = radius * length * ny * radiusFactor;
        float baseZ = radius * nz * radiusFactor;
        
        return new float[] { baseX, baseY, baseZ };
    }
}

