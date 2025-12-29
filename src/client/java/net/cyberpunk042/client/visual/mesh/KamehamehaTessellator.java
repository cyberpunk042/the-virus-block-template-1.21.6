package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.EnergyType;
import net.cyberpunk042.visual.shape.KamehamehaShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import org.joml.Vector3f;

/**
 * Tessellates Kamehameha shapes into triangle meshes.
 * 
 * <h2>Components</h2>
 * <ul>
 *   <li><b>Orb</b> - Spherical energy core at origin</li>
 *   <li><b>OrbCore</b> - Inner intense orb layer (optional)</li>
 *   <li><b>OrbAura</b> - Outer orb glow (optional)</li>
 *   <li><b>Beam</b> - Cylindrical/conical energy stream</li>
 *   <li><b>BeamCore</b> - Inner intense beam (optional)</li>
 *   <li><b>BeamTip</b> - End cap of beam</li>
 *   <li><b>BeamAura</b> - Outer beam glow (optional)</li>
 * </ul>
 * 
 * @see KamehamehaShape
 */
public final class KamehamehaTessellator {
    
    private KamehamehaTessellator() {}
    
    // =========================================================================
    // Main Tessellation Entry Point
    // =========================================================================
    
    /**
     * Tessellates a Kamehameha shape into a complete mesh.
     */
    public static Mesh tessellate(KamehamehaShape shape, VertexPattern pattern,
                                   VisibilityMask visibility, float time) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        if (pattern == null) pattern = QuadPattern.DEFAULT;
        
        // Tessellate orb components
        if (shape.isOrbVisible()) {
            tessellateOrb(builder, shape, time);
            if (shape.hasCore()) {
                tessellateOrbCore(builder, shape, time);
            }
            if (shape.hasAura()) {
                tessellateOrbAura(builder, shape, time);
            }
        }
        
        // Tessellate beam components
        if (shape.isBeamVisible()) {
            tessellateBeam(builder, shape, time);
            if (shape.hasCore()) {
                tessellateBeamCore(builder, shape, time);
            }
            tessellateBeamTip(builder, shape, time);
            if (shape.hasAura()) {
                tessellateBeamAura(builder, shape, time);
            }
        }
        
        Logging.FIELD.topic("tessellate").trace("Kamehameha mesh: {} verts",
            builder.vertexCount());
        
        return builder.build();
    }
    
    // =========================================================================
    // Orb Tessellation
    // =========================================================================
    
    private static void tessellateOrb(MeshBuilder builder, KamehamehaShape shape, float time) {
        float radius = shape.effectiveOrbRadius();
        int segments = shape.orbSegments();
        EnergyType type = shape.orbType();
        float alpha = shape.orbAlpha();
        tessellateSphere(builder, 0, 0, 0, radius, segments, segments / 2, type, time, alpha);
    }
    
    private static void tessellateOrbCore(MeshBuilder builder, KamehamehaShape shape, float time) {
        float radius = shape.orbCoreRadius();
        int segments = Math.max(8, shape.orbSegments() / 2);
        // Core is brighter - use full alpha
        tessellateSphere(builder, 0, 0, 0, radius, segments, segments / 2, shape.orbType(), time, 1.0f);
    }
    
    private static void tessellateOrbAura(MeshBuilder builder, KamehamehaShape shape, float time) {
        float radius = shape.orbAuraRadius();
        int segments = shape.orbSegments();
        // Aura uses its own alpha multiplier
        tessellateSphere(builder, 0, 0, 0, radius, segments, segments / 2, shape.orbType(), time, shape.auraAlpha());
    }
    
    // =========================================================================
    // Beam Tessellation
    // =========================================================================
    
    private static void tessellateBeam(MeshBuilder builder, KamehamehaShape shape, float time) {
        float baseRadius = shape.effectiveBeamBaseRadius();
        float tipRadius = shape.effectiveBeamTipRadius();
        float length = shape.effectiveBeamLength();
        int segments = shape.beamSegments();
        int lengthSegments = shape.beamLengthSegments();
        float startY = shape.effectiveOrbRadius();
        float baseAlpha = shape.beamBaseAlpha();
        float tipAlpha = shape.beamTipAlpha();
        tessellateCylinder(builder, 0, startY, 0, baseRadius, tipRadius, length,
                           segments, lengthSegments, shape.beamType(), time, baseAlpha, tipAlpha);
    }
    
    private static void tessellateBeamCore(MeshBuilder builder, KamehamehaShape shape, float time) {
        float baseRadius = shape.beamCoreRadius();
        float tipRadius = baseRadius * (shape.beamTipRadius() / Math.max(0.001f, shape.beamBaseRadius()));
        float length = shape.effectiveBeamLength();
        int segments = Math.max(8, shape.beamSegments() / 2);
        int lengthSegments = Math.max(2, shape.beamLengthSegments() / 2);
        float startY = shape.effectiveOrbRadius();
        // Core uses full alpha gradient
        tessellateCylinder(builder, 0, startY, 0, baseRadius, tipRadius, length,
                           segments, lengthSegments, shape.beamType(), time, 1.0f, 1.0f);
    }
    
    private static void tessellateBeamTip(MeshBuilder builder, KamehamehaShape shape, float time) {
        float tipRadius = shape.effectiveBeamTipRadius();
        float startY = shape.effectiveOrbRadius() + shape.effectiveBeamLength();
        int segments = shape.beamSegments();
        float tipAlpha = shape.beamTipAlpha();
        
        switch (shape.tipStyle()) {
            case POINTED -> tessellateConeTip(builder, 0, startY, 0, tipRadius, tipRadius * 1.5f, segments, tipAlpha);
            case ROUNDED -> tessellateHemisphere(builder, 0, startY, 0, tipRadius, segments, true, tipAlpha);
            case FLAT -> tessellateFlatCap(builder, 0, startY, 0, tipRadius, segments, true, tipAlpha);
            case VORTEX -> tessellateVortexTip(builder, 0, startY, 0, tipRadius, segments, time, tipAlpha);
        }
    }
    
    private static void tessellateBeamAura(MeshBuilder builder, KamehamehaShape shape, float time) {
        float baseRadius = shape.effectiveBeamBaseRadius() * shape.auraScale();
        float tipRadius = shape.effectiveBeamTipRadius() * shape.auraScale();
        float length = shape.effectiveBeamLength();
        int segments = shape.beamSegments();
        int lengthSegments = Math.max(2, shape.beamLengthSegments() / 2);
        float startY = shape.effectiveOrbRadius();
        // Aura uses auraAlpha as multiplier
        float auraAlpha = shape.auraAlpha();
        tessellateCylinder(builder, 0, startY, 0, baseRadius, tipRadius, length,
                           segments, lengthSegments, shape.beamType(), time, auraAlpha, auraAlpha * 0.5f);
    }
    
    // =========================================================================
    // Primitive Geometry Helpers
    // =========================================================================
    
    private static void tessellateSphere(MeshBuilder builder,
                                          float cx, float cy, float cz, float radius,
                                          int lonSteps, int latSteps,
                                          EnergyType type, float time, float alpha) {
        float distortion = type.hasDistortion() ? 0.1f * (float)Math.sin(time * 3) : 0;
        
        // Create vertex grid
        int[][] indices = new int[latSteps + 1][lonSteps + 1];
        
        for (int lat = 0; lat <= latSteps; lat++) {
            float theta = (float) Math.PI * lat / latSteps;
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            
            for (int lon = 0; lon <= lonSteps; lon++) {
                float phi = (float) (2 * Math.PI * lon / lonSteps);
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);
                
                float r = radius + distortion * (float)Math.sin(phi * 4 + time * 5);
                
                float x = cx + r * sinTheta * cosPhi;
                float y = cy + r * cosTheta;
                float z = cz + r * sinTheta * sinPhi;
                
                float nx = sinTheta * cosPhi;
                float ny = cosTheta;
                float nz = sinTheta * sinPhi;
                
                float u = (float) lon / lonSteps;
                float v = (float) lat / latSteps;
                
                indices[lat][lon] = builder.addVertex(new Vertex(x, y, z, nx, ny, nz, u, v, alpha));
            }
        }
        
        // Generate triangles
        for (int lat = 0; lat < latSteps; lat++) {
            for (int lon = 0; lon < lonSteps; lon++) {
                int i00 = indices[lat][lon];
                int i10 = indices[lat + 1][lon];
                int i11 = indices[lat + 1][lon + 1];
                int i01 = indices[lat][lon + 1];
                
                // Two triangles per quad
                builder.triangle(i00, i10, i11);
                builder.triangle(i00, i11, i01);
            }
        }
    }
    
    private static void tessellateCylinder(MeshBuilder builder,
                                            float cx, float startY, float cz,
                                            float baseRadius, float tipRadius, float length,
                                            int radialSegments, int lengthSegments,
                                            EnergyType type, float time,
                                            float baseAlpha, float tipAlpha) {
        float distortion = type.hasDistortion() ? 0.05f : 0;
        boolean hasRotation = type.hasRotation();
        float rotationOffset = hasRotation ? time * 2 : 0;
        
        // Create vertex grid
        int[][] indices = new int[lengthSegments + 1][radialSegments + 1];
        
        for (int l = 0; l <= lengthSegments; l++) {
            float t = (float) l / lengthSegments;
            float y = startY + length * t;
            float r = baseRadius + (tipRadius - baseRadius) * t;
            
            // Interpolate alpha from base to tip
            float alpha = baseAlpha + (tipAlpha - baseAlpha) * t;
            
            for (int s = 0; s <= radialSegments; s++) {
                float angle = (float) (2 * Math.PI * s / radialSegments) + rotationOffset;
                float d = distortion * (float)Math.sin(angle * 3 + time * 4 + y);
                
                float x = cx + (float)Math.cos(angle) * (r + d);
                float z = cz + (float)Math.sin(angle) * (r + d);
                
                float nx = (float)Math.cos(angle);
                float nz = (float)Math.sin(angle);
                float ny = 0;
                
                float u = t;
                float v = (float) s / radialSegments;
                
                indices[l][s] = builder.addVertex(new Vertex(x, y, z, nx, ny, nz, u, v, alpha));
            }
        }
        
        // Generate triangles
        for (int l = 0; l < lengthSegments; l++) {
            for (int s = 0; s < radialSegments; s++) {
                int i00 = indices[l][s];
                int i01 = indices[l][s + 1];
                int i10 = indices[l + 1][s];
                int i11 = indices[l + 1][s + 1];
                
                builder.triangle(i00, i01, i11);
                builder.triangle(i00, i11, i10);
            }
        }
    }
    
    private static void tessellateConeTip(MeshBuilder builder,
                                           float cx, float baseY, float cz,
                                           float baseRadius, float height, int segments, float alpha) {
        float tipY = baseY + height;
        // Tip fades to 0 alpha at the point
        int tipIdx = builder.addVertex(new Vertex(cx, tipY, cz, 0, 1, 0, 0.5f, 0.5f, 0f));
        
        int[] baseIndices = new int[segments + 1];
        for (int s = 0; s <= segments; s++) {
            float angle = (float) (2 * Math.PI * s / segments);
            float x = cx + (float)Math.cos(angle) * baseRadius;
            float z = cz + (float)Math.sin(angle) * baseRadius;
            float nx = (float)Math.cos(angle);
            float nz = (float)Math.sin(angle);
            baseIndices[s] = builder.addVertex(new Vertex(x, baseY, z, nx, 0.5f, nz, 
                    (float)s / segments, 0, alpha));
        }
        
        for (int s = 0; s < segments; s++) {
            builder.triangle(baseIndices[s], baseIndices[s + 1], tipIdx);
        }
    }
    
    private static void tessellateHemisphere(MeshBuilder builder,
                                              float cx, float baseY, float cz,
                                              float radius, int segments, boolean facingUp, float alpha) {
        int latSteps = segments / 2;
        
        int[][] indices = new int[latSteps + 1][segments + 1];
        
        for (int lat = 0; lat <= latSteps; lat++) {
            float theta = (float) Math.PI * lat / (latSteps * 2);
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            
            // Alpha fades toward the pole
            float latT = (float) lat / latSteps;
            float vertAlpha = alpha * (1f - latT * 0.5f);
            
            for (int lon = 0; lon <= segments; lon++) {
                float phi = (float) (2 * Math.PI * lon / segments);
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);
                
                float y = facingUp ? baseY + radius * cosTheta : baseY - radius * cosTheta;
                float x = cx + radius * sinTheta * cosPhi;
                float z = cz + radius * sinTheta * sinPhi;
                
                float ny = facingUp ? cosTheta : -cosTheta;
                float nx = sinTheta * cosPhi;
                float nz = sinTheta * sinPhi;
                
                indices[lat][lon] = builder.addVertex(new Vertex(x, y, z, nx, ny, nz, 
                        (float)lon / segments, (float)lat / latSteps, vertAlpha));
            }
        }
        
        for (int lat = 0; lat < latSteps; lat++) {
            for (int lon = 0; lon < segments; lon++) {
                int i00 = indices[lat][lon];
                int i01 = indices[lat][lon + 1];
                int i10 = indices[lat + 1][lon];
                int i11 = indices[lat + 1][lon + 1];
                
                builder.triangle(i00, i10, i11);
                builder.triangle(i00, i11, i01);
            }
        }
    }
    
    private static void tessellateFlatCap(MeshBuilder builder,
                                           float cx, float y, float cz,
                                           float radius, int segments, boolean facingUp, float alpha) {
        float ny = facingUp ? 1.0f : -1.0f;
        int centerIdx = builder.addVertex(new Vertex(cx, y, cz, 0, ny, 0, 0.5f, 0.5f, alpha));
        
        int[] edgeIndices = new int[segments + 1];
        for (int s = 0; s <= segments; s++) {
            float angle = (float) (2 * Math.PI * s / segments);
            float x = cx + (float)Math.cos(angle) * radius;
            float z = cz + (float)Math.sin(angle) * radius;
            edgeIndices[s] = builder.addVertex(new Vertex(x, y, z, 0, ny, 0, 
                    (float)s / segments, 0, alpha));
        }
        
        for (int s = 0; s < segments; s++) {
            if (facingUp) {
                builder.triangle(centerIdx, edgeIndices[s], edgeIndices[s + 1]);
            } else {
                builder.triangle(centerIdx, edgeIndices[s + 1], edgeIndices[s]);
            }
        }
    }
    
    private static void tessellateVortexTip(MeshBuilder builder,
                                             float cx, float baseY, float cz,
                                             float radius, int segments, float time, float alpha) {
        float height = radius * 1.5f;
        int lengthSegs = segments / 2;
        int spirals = 3;
        
        int[][] indices = new int[lengthSegs + 1][segments + 1];
        
        for (int l = 0; l <= lengthSegs; l++) {
            float t = (float) l / lengthSegs;
            float y = baseY + height * t;
            float r = radius * (1 - t);
            float twist = t * spirals * (float)Math.PI * 2 + time * 3;
            
            // Alpha fades toward the tip
            float vertAlpha = alpha * (1f - t);
            
            for (int s = 0; s <= segments; s++) {
                float angle = (float) (2 * Math.PI * s / segments) + twist;
                float x = cx + (float)Math.cos(angle) * r;
                float z = cz + (float)Math.sin(angle) * r;
                
                float nx = (float)Math.cos(angle);
                float nz = (float)Math.sin(angle);
                
                indices[l][s] = builder.addVertex(new Vertex(x, y, z, nx, 0.5f, nz, 
                        t, (float)s / segments, vertAlpha));
            }
        }
        
        for (int l = 0; l < lengthSegs; l++) {
            for (int s = 0; s < segments; s++) {
                int i00 = indices[l][s];
                int i01 = indices[l][s + 1];
                int i10 = indices[l + 1][s];
                int i11 = indices[l + 1][s + 1];
                
                builder.triangle(i00, i01, i11);
                builder.triangle(i00, i11, i10);
            }
        }
    }
    
    // =========================================================================
    // Convenience Overloads
    // =========================================================================
    
    public static Mesh tessellate(KamehamehaShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        return tessellate(shape, pattern, visibility, 0);
    }
    
    public static Mesh tessellate(KamehamehaShape shape, VertexPattern pattern) {
        return tessellate(shape, pattern, VisibilityMask.FULL, 0);
    }
    
    public static Mesh tessellate(KamehamehaShape shape) {
        return tessellate(shape, QuadPattern.DEFAULT, VisibilityMask.FULL, 0);
    }
}
