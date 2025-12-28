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
        
        // Base position (spheroid)
        float baseX = radius * sinTheta * cosPhi;
        float baseY = radius * length * cosTheta;
        float baseZ = radius * sinTheta * sinPhi;
        
        // THE MAGIC FORMULA FOR DRAMATIC CLOUDS
        // Uses multiple spherical "bulges" at pseudo-random positions
        // Each bulge creates a distinct lobe like a cumulus cloud
        
        // Direction from center (normalized)
        float nx = sinTheta * cosPhi;
        float ny = cosTheta;
        float nz = sinTheta * sinPhi;
        
        // Define 8 major bulge positions using golden angle distribution
        // This creates well-distributed but organic-looking lobes
        float goldenAngle = 2.399963f;  // ~137.5 degrees in radians
        float[][] bulgePositions = new float[8][3];
        for (int i = 0; i < 8; i++) {
            float t = (float) i / 7.0f;  // 0 to 1
            float bulgeTheta = (float) Math.acos(1 - 2 * t);  // Even distribution
            float bulgePhi = goldenAngle * i;
            bulgePositions[i][0] = (float) Math.sin(bulgeTheta) * (float) Math.cos(bulgePhi);
            bulgePositions[i][1] = (float) Math.cos(bulgeTheta);
            bulgePositions[i][2] = (float) Math.sin(bulgeTheta) * (float) Math.sin(bulgePhi);
        }
        
        // Calculate bulge factor - cumulative effect of all lobes
        float totalBulge = 0f;
        float bulgeWidth = 0.6f + 0.4f * intensity;  // Wider bulges = more overlap = cloudier
        
        for (int i = 0; i < 8; i++) {
            // Dot product = similarity to bulge direction (-1 to 1)
            float dot = nx * bulgePositions[i][0] + 
                       ny * bulgePositions[i][1] + 
                       nz * bulgePositions[i][2];
            
            // Vary bulge strength per lobe for organic feel
            float bulgeStrength = 0.5f + 0.5f * (float) Math.sin(i * 1.7f + 0.3f);
            
            // Gaussian-like falloff for smooth bulges
            // Higher dot = closer to bulge center = more displacement
            float proximity = (dot + 1f) * 0.5f;  // Remap to 0-1
            float bulge = (float) Math.pow(proximity, 2.0f / bulgeWidth) * bulgeStrength;
            totalBulge = Math.max(totalBulge, bulge);  // Use max for distinct lobes
        }
        
        // Add multi-frequency noise for surface detail
        float detailNoise = 
            0.15f * (float) Math.sin(5 * theta + 0.3f) * (float) Math.sin(7 * phi + 1.2f) +
            0.10f * (float) Math.sin(8 * theta + 1.5f) * (float) Math.cos(6 * phi + 0.7f) +
            0.05f * (float) Math.sin(11 * theta + 2.1f) * (float) Math.sin(9 * phi + 0.9f);
        
        // Combine bulges with detail noise
        // At intensity 0: sphere
        // At intensity 1: dramatic multi-lobe cloud with up to 80% size variation
        float displacement = totalBulge + detailNoise * 0.5f;
        float radiusFactor = 1.0f + displacement * intensity * 0.8f;
        
        // Apply displacement outward from center
        return new float[] {
            baseX * radiusFactor,
            baseY * radiusFactor,
            baseZ * radiusFactor
        };
    }
    /**
     * Molecule vertex position - intensity-controlled branching spheres.
     * 
     * <p><b>MAGIC FORMULA:</b> Creates molecular structures where intensity
     * controls the number of atomic branches:
     * <ul>
     *   <li>0.0 = pure sphere (single atom)</li>
     *   <li>0.2 = 1 branch (diatomic like H2)</li>
     *   <li>0.4 = 2 branches (like H2O - water)</li>
     *   <li>0.6 = 3 branches (like NH3 - ammonia)</li>
     *   <li>0.8 = 4 branches (like CH4 - methane, tetrahedral)</li>
     *   <li>1.0 = 5 branches (complex molecule)</li>
     * </ul>
     * Each branch is a distinct spherical lobe with smooth metaball-like
     * merging at the connection to the central sphere.</p>
     * 
     * @param theta Polar angle (0 = top, π = bottom)
     * @param phi Azimuthal angle (0 to 2π)
     * @param radius Base radius
     * @param intensity Branch count control (0 = sphere, 1 = 5 branches)
     * @param length Axial stretch
     * @return {x, y, z} vertex position
     */
    public static float[] moleculeVertex(float theta, float phi, float radius, 
            float intensity, float length) {
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        
        // Base position (spheroid)
        float baseX = radius * sinTheta * cosPhi;
        float baseY = radius * length * cosTheta;
        float baseZ = radius * sinTheta * sinPhi;
        
        // At intensity 0, return pure sphere
        if (intensity < 0.05f) {
            return new float[] { baseX, baseY, baseZ };
        }
        
        // Direction from center (normalized)
        float nx = sinTheta * cosPhi;
        float ny = cosTheta;
        float nz = sinTheta * sinPhi;
        
        // Calculate number of active branches based on intensity
        // 0.2 = 1 branch, 0.4 = 2, 0.6 = 3, 0.8 = 4, 1.0 = 5
        int maxBranches = (int) Math.ceil(intensity * 5.0f);
        maxBranches = Math.max(1, Math.min(5, maxBranches));
        
        // How "complete" is the current branch (for smooth transitions)
        float branchCompleteness = (intensity * 5.0f) - (maxBranches - 1);
        branchCompleteness = Math.clamp(branchCompleteness, 0.0f, 1.0f);
        
        // Define branch directions using golden angle spiral for natural distribution
        // First branch at top, others spiral around
        float goldenAngle = 2.399963f;  // ~137.5 degrees
        float[][] branchDirs = new float[5][3];
        
        // Branch 1: Top (like H in H2)
        branchDirs[0] = new float[] { 0f, 1f, 0f };
        
        // Branches 2-5: Distributed around using golden angle
        for (int i = 1; i < 5; i++) {
            float t = (float) i / 4.0f;  // 0.25, 0.5, 0.75, 1.0
            float branchTheta = (float) (Math.PI * 0.3 + t * Math.PI * 0.5);  // 54° to 144° from top
            float branchPhi = goldenAngle * i;
            branchDirs[i][0] = (float) Math.sin(branchTheta) * (float) Math.cos(branchPhi);
            branchDirs[i][1] = (float) Math.cos(branchTheta);
            branchDirs[i][2] = (float) Math.sin(branchTheta) * (float) Math.sin(branchPhi);
        }
        
        // Calculate bulge from each active branch
        float totalBulge = 0f;
        float branchRadius = 0.6f;  // How far the branch sticks out
        float branchWidth = 0.5f;   // How wide each branch lobe is
        
        for (int i = 0; i < maxBranches; i++) {
            // Calculate branch strength (last branch fades in based on completeness)
            float strength = (i == maxBranches - 1) ? branchCompleteness : 1.0f;
            
            // Dot product = similarity to branch direction (-1 to 1)
            float dot = nx * branchDirs[i][0] + ny * branchDirs[i][1] + nz * branchDirs[i][2];
            
            // Metaball-like field function: r² / d²
            // Creates smooth bulges that merge nicely at the center
            float proximity = (dot + 1f) * 0.5f;  // Remap to 0-1
            
            // Sharp falloff for distinct spherical lobes
            float bulge = (float) Math.pow(proximity, 3.0f / branchWidth);
            bulge = bulge * strength * branchRadius;
            
            // Use max for distinct lobes rather than additive blending
            totalBulge = Math.max(totalBulge, bulge);
        }
        
        // Add neck/connection region between center and branches
        // This creates the characteristic molecular bond look
        float connectionFactor = 1.0f - totalBulge * 0.3f;  // Slight pinching
        connectionFactor = Math.max(0.7f, connectionFactor);
        
        // Final radius: base + branch bulge
        // The sphere contracts slightly where branches connect (neck)
        // but expands dramatically at branch ends
        float radiusFactor = connectionFactor + totalBulge * 1.2f;
        
        return new float[] {
            baseX * radiusFactor,
            baseY * radiusFactor,
            baseZ * radiusFactor
        };
    }
}

