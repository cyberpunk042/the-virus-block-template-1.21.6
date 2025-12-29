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
    
    /**
     * Cloud vertex position using multi-octave spherical noise.
     * Legacy overload - delegates to full version with default bumpSize.
     */
    public static float[] cloudVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        return cloudVertex(theta, phi, radius, intensity, length, 6, 0.5f, 0.5f);
    }
    
    /**
     * Cloud vertex position with count and smoothness (backward compat).
     * Delegates to full version with default bumpSize.
     */
    public static float[] cloudVertex(float theta, float phi, float radius, 
            float intensity, float length, int count, float smoothness) {
        return cloudVertex(theta, phi, radius, intensity, length, count, smoothness, 0.5f);
    }
    
    /**
     * Cloud vertex position using SPHERICAL GAUSSIAN bumps.
     * 
     * <p><b>Algorithm:</b> Uses Spherical Gaussian (SG) functions to create smooth,
     * localized bumps on the sphere surface. Each bump is defined by:
     * G(v) = e^(λ * (v·p - 1)) where v is vertex direction and p is bump center.</p>
     * 
     * <p><b>Mathematical Foundation:</b></p>
     * <ul>
     *   <li>Bump centers distributed using Fibonacci spiral (golden angle = 2.399963 rad)</li>
     *   <li>Spherical Gaussian falloff: aligned (v·p=1) → full bump, opposite (v·p=-1) → no bump</li>
     *   <li>Sharpness λ controls bump width (higher = narrower)</li>
     *   <li>Bumps are ADDITIVE, not normalized by count</li>
     * </ul>
     * 
     * @param theta Polar angle (0 = top, π = bottom)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param intensity Overall bump prominence (0 = sphere, 1 = very bumpy)
     * @param length Axial stretch (1 = spherical, >1 = elongated)
     * @param count Number of bumps (1-20), evenly distributed on sphere
     * @param smoothness Bump roundness (0 = sharp spikes, 1 = soft billows)
     * @param bumpSize Individual bump amplitude (0.1-2.0)
     * @return {x, y, z} vertex position
     */
    public static float[] cloudVertex(float theta, float phi, float radius, 
            float intensity, float length, int count, float smoothness, float bumpSize) {
        
        // Base trigonometry
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Current vertex direction (unit vector on sphere)
        float vx = sinTheta * cosPhi;
        float vy = cosTheta;
        float vz = sinTheta * sinPhi;
        
        // At intensity 0, return pure spheroid
        if (intensity < 0.01f) {
            return new float[] {
                radius * vx,
                radius * length * vy,
                radius * vz
            };
        }
        
        // Clamp parameters
        count = Math.max(1, Math.min(20, count));
        smoothness = Math.max(0f, Math.min(1f, smoothness));
        bumpSize = Math.max(0.1f, Math.min(2f, bumpSize));
        
        // ======================================================================
        // SPHERICAL GAUSSIAN ALGORITHM
        // ======================================================================
        
        // Map smoothness to sharpness (λ)
        // Low smoothness (0) → high sharpness → narrow, distinct bumps
        // High smoothness (1) → low sharpness → wide, blended bumps
        float sharpness = 2.0f + (1.0f - smoothness) * 20.0f;  // Range: 2 to 22
        
        // Golden angle for Fibonacci spiral distribution (even coverage)
        float goldenAngle = 2.399963f;  // ≈ 137.5°
        
        // Accumulate bump displacements
        float totalDisplacement = 0.0f;
        
        for (int i = 0; i < count; i++) {
            // Distribute bump centers using Fibonacci spiral
            // This provides near-optimal even distribution on sphere
            float t = (float) (i + 0.5f) / (float) count;  // 0.5 offset for centering
            float bumpTheta = (float) Math.acos(1.0f - 2.0f * t);  // 0 to π
            float bumpPhi = goldenAngle * i;  // Spiral around
            
            // Bump center direction (unit vector)
            float px = (float) Math.sin(bumpTheta) * (float) Math.cos(bumpPhi);
            float py = (float) Math.cos(bumpTheta);
            float pz = (float) Math.sin(bumpTheta) * (float) Math.sin(bumpPhi);
            
            // Dot product = cos(angular_distance between vertex and bump center)
            // Range: 1 (aligned) to -1 (opposite)
            float dot = vx * px + vy * py + vz * pz;
            
            // Spherical Gaussian: G(v) = e^(λ * (dot - 1))
            // When aligned (dot = 1): e^0 = 1 (full bump)
            // When opposite (dot = -1): e^(-2λ) ≈ 0 (no bump)
            float gaussian = (float) Math.exp(sharpness * (dot - 1.0f));
            
            // Add bump contribution with individual size variation
            // Slight per-bump size variation for natural look
            float sizeVariation = 0.8f + 0.4f * (float) Math.sin(i * 1.7f + 0.5f);
            totalDisplacement += gaussian * bumpSize * sizeVariation;
        }
        
        // ======================================================================
        // APPLY DISPLACEMENT
        // ======================================================================
        
        // Scale displacement by intensity
        // NO normalization by count - more bumps = bumpier surface (intentional)
        float displacement = totalDisplacement * intensity * 0.25f;
        
        // Final radius: base + displacement (ADDITIVE, not multiplicative)
        float finalRadius = radius * (1.0f + displacement);
        
        // Apply to position with axial stretch
        return new float[] {
            finalRadius * vx,
            finalRadius * length * vy,
            finalRadius * vz
        };
    }

    /**
     * Molecule vertex position using METABALL IMPLICIT SURFACE formula.
     * Legacy overload - delegates to full version.
     */
    public static float[] moleculeVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        // Legacy: use fixed count of 4, default smoothness
        return moleculeVertex(theta, phi, radius, intensity, length, 4, 0.5f, 0.4f, 0.6f);
    }
    
    /**
     * Molecule vertex position with count and smoothness (backward compat).
     * Delegates to full version with default atomSize and separation.
     */
    public static float[] moleculeVertex(float theta, float phi, float radius, 
            float intensity, float length, int count, float smoothness) {
        return moleculeVertex(theta, phi, radius, intensity, length, count, smoothness, 0.4f, 0.6f);
    }
    
    /**
     * Molecule vertex position using METABALL IMPLICIT SURFACE algorithm.
     * 
     * <p><b>Algorithm (Jim Blinn, 1982):</b> Each atom creates a spherical
     * influence field with inverse-square falloff. Surface is where total
     * field exceeds threshold.</p>
     * 
     * <p><b>Key difference from CLOUD:</b></p>
     * <ul>
     *   <li>CLOUD: Bumps are centered ON the sphere surface</li>
     *   <li>MOLECULE: Atoms are positioned AROUND the center at a distance</li>
     * </ul>
     * 
     * <p><b>Blending Modes (based on smoothness):</b></p>
     * <ul>
     *   <li>Low smoothness: MAX blending → distinct, separate atoms</li>
     *   <li>High smoothness: ADDITIVE blending → merged blob</li>
     * </ul>
     * 
     * @param theta Polar angle (0 = top, π = bottom)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param intensity Overall atom prominence (0 = sphere, 1 = full atoms)
     * @param length Axial stretch
     * @param count Number of atoms (1-12)
     * @param smoothness Blend mode (0 = distinct spheres, 1 = merged blob)
     * @param atomSize Size of each atom (0.1-1.0)
     * @param separation Distance of atoms from center (0.3-1.5)
     * @return {x, y, z} vertex position
     */
    public static float[] moleculeVertex(float theta, float phi, float radius, 
            float intensity, float length, int count, float smoothness, 
            float atomSize, float separation) {
        
        // Base trigonometry
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Current vertex direction (unit vector on sphere)
        float vx = sinTheta * cosPhi;
        float vy = cosTheta;
        float vz = sinTheta * sinPhi;
        
        // At intensity 0, return pure spheroid
        if (intensity < 0.01f) {
            return new float[] {
                radius * vx,
                radius * length * vy,
                radius * vz
            };
        }
        
        // Clamp parameters
        count = Math.max(1, Math.min(12, count));
        smoothness = Math.max(0f, Math.min(1f, smoothness));
        atomSize = Math.max(0.1f, Math.min(1f, atomSize));
        separation = Math.max(0.3f, Math.min(1.5f, separation));
        
        // ======================================================================
        // METABALL ALGORITHM
        // ======================================================================
        
        // Golden angle for Fibonacci spiral distribution
        float goldenAngle = 2.399963f;
        
        // Calculate influence from all atoms
        float maxInfluence = 0.0f;
        float sumInfluence = 0.0f;
        
        for (int i = 0; i < count; i++) {
            // Distribute atoms using Fibonacci spiral (even coverage)
            float t = (float) (i + 0.5f) / (float) count;
            float atomTheta = (float) Math.acos(1.0f - 2.0f * t);
            float atomPhi = goldenAngle * i;
            
            // Atom direction (unit vector)
            float ax = (float) Math.sin(atomTheta) * (float) Math.cos(atomPhi);
            float ay = (float) Math.cos(atomTheta);
            float az = (float) Math.sin(atomTheta) * (float) Math.sin(atomPhi);
            
            // Atom position in space (at distance 'separation' from center)
            // Atoms are NOT on the surface, they're orbiting around center
            float atomDist = separation;
            float atomX = ax * atomDist;
            float atomY = ay * atomDist;
            float atomZ = az * atomDist;
            
            // Dot product = cos(angle between vertex direction and atom direction)
            float dot = vx * ax + vy * ay + vz * az;
            
            // Angular distance: 0 when aligned, 2 when opposite
            // This approximates "how close is this ray to passing through the atom"
            float angularDist = 1.0f - dot;
            
            // Metaball field: inverse-square falloff
            // Higher atomSize = larger influence radius
            float influenceRadius = atomSize * 0.8f;
            float distSq = angularDist * angularDist;
            float epsilon = 0.05f;  // Prevent division by zero
            
            // Classic metaball: R² / (r² + ε)
            float influence = (influenceRadius * influenceRadius) / (distSq + epsilon);
            
            // Track both max and sum for blending modes
            maxInfluence = Math.max(maxInfluence, influence);
            sumInfluence += influence;
        }
        
        // ======================================================================
        // BLEND MODE: MAX vs ADDITIVE based on smoothness
        // ======================================================================
        
        // Interpolate between max-blending and additive-blending
        // Low smoothness (0) → use MAX → distinct atoms
        // High smoothness (1) → use SUM → merged blob
        float blendFactor = smoothness;
        float finalInfluence = maxInfluence * (1.0f - blendFactor) + 
                               (sumInfluence / count) * blendFactor;
        
        // ======================================================================
        // MAP INFLUENCE TO DISPLACEMENT
        // ======================================================================
        
        // Convert influence to a bump displacement
        // Influence ranges from ~0 to potentially very high near atoms
        // We want displacement to saturate at a reasonable maximum
        
        // Normalize influence to 0-1 range using smooth saturation
        float normalizedInfluence = finalInfluence / (1.0f + finalInfluence);
        
        // Apply intensity
        float displacement = normalizedInfluence * intensity * 0.6f;
        
        // Final radius: base + displacement (ADDITIVE)
        float finalRadius = radius * (1.0f + displacement);
        
        // Apply to position with axial stretch
        return new float[] {
            finalRadius * vx,
            finalRadius * length * vy,
            finalRadius * vz
        };
    }
}

