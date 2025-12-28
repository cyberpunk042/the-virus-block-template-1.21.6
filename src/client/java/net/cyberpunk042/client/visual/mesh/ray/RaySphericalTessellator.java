package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoDeformationFactory;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoDeformationStrategy;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoPolarSurfaceGenerator;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeResult;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.shape.RaysShape;
import net.cyberpunk042.visual.shape.RayType;
import net.cyberpunk042.visual.shape.SphereDeformation;

/**
 * Tessellator for 3D spherical ray types (DROPLET, CONE, EGG, BULLET, etc.).
 * 
 * <p>Uses the existing {@link SphereDeformation} infrastructure from ShapeMath
 * to generate proper parametric shapes with full intensity support:
 * <ul>
 *   <li>intensity 0 = sphere</li>
 *   <li>intensity 1 = full deformation (sharp droplet, etc.)</li>
 * </ul>
 * </p>
 * 
 * <p>This tessellator handles RayTypes that map to SphereDeformation shapes.
 * Non-spherical ray types (CUBES, CRYSTALS, etc.) need separate tessellators.</p>
 * 
 * @see SphereDeformation
 * @see GeoPolarSurfaceGenerator
 */
public class RaySphericalTessellator implements RayTypeTessellator {
    
    /** Singleton instance. */
    public static final RaySphericalTessellator INSTANCE = new RaySphericalTessellator();
    
    /** Minimum rings for smooth 3D shape. */
    private static final int MIN_RINGS = 6;
    
    /** Minimum segments for round cross-section. */
    private static final int MIN_SEGMENTS = 6;
    
    private RaySphericalTessellator() {} // Use INSTANCE
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context) {
        tessellate(builder, shape, context, null, null);
    }
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context,
                          net.cyberpunk042.visual.pattern.VertexPattern pattern,
                          net.cyberpunk042.visual.visibility.VisibilityMask visibility) {
        // Get edge result from Stage/Phase model
        TessEdgeResult edgeResult = context.computeEdgeResult();
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        // === MAP RAY TYPE TO SPHERE DEFORMATION ===
        RayType rayType = shape != null ? shape.effectiveRayType() : RayType.DROPLET;
        SphereDeformation deformation = mapToSphereDeformation(rayType);
        float intensity = context.shapeIntensity();
        
        // === COMPUTE POSITION & ORIENTATION ===
        float length = context.length();
        float baseRadius = length * 0.5f;
        float scale = edgeResult.scale();
        baseRadius *= scale;
        
        float[] start = context.start();
        float[] end = context.end();
        float[] center;
        float[] direction;
        
        // Apply curvature if present
        net.cyberpunk042.visual.shape.RayCurvature curvature = context.curvature();
        float curvatureIntensity = context.curvatureIntensity();
        boolean hasCurvature = curvature != null 
            && curvature != net.cyberpunk042.visual.shape.RayCurvature.NONE 
            && curvatureIntensity > 0.001f;
        
        if (hasCurvature) {
            float flowPositionOffset = context.flowPositionOffset();
            float travelRange = context.travelRange();
            float t = (travelRange > 0.01f) ? (flowPositionOffset / travelRange) : 0.5f;
            t = Math.max(0.0f, Math.min(1.0f, t));
            
            center = RayGeometryUtils.computeCurvedPosition(start, end, t, curvature, curvatureIntensity);
            direction = RayGeometryUtils.computeCurvedTangent(start, end, t, curvature, curvatureIntensity);
        } else {
            center = new float[] {
                (start[0] + end[0]) * 0.5f,
                (start[1] + end[1]) * 0.5f,
                (start[2] + end[2]) * 0.5f
            };
            direction = context.orientationVector();
        }
        
        // === MESH RESOLUTION ===
        int totalSegs = Math.max(12, context.lineResolution());
        int rings = Math.max(MIN_RINGS, totalSegs / 2);
        int segments = Math.max(MIN_SEGMENTS, totalSegs / 2);
        
        // === FLOW PARAMETERS ===
        float axialLength = context.shapeLength() * scale;
        float visibleTStart = edgeResult.clipStart();
        float visibleTEnd = edgeResult.clipEnd();
        float alpha = edgeResult.alpha();
        
        // === CHECK FOR FIELD DEFORMATION ===
        net.cyberpunk042.visual.shape.FieldDeformationMode deformMode = context.fieldDeformation();
        GeoDeformationStrategy fieldDeformation = GeoDeformationFactory.get(deformMode);
        
        // === CHECK FOR TRAVEL EFFECT ===
        net.cyberpunk042.visual.animation.RayFlowConfig flowConfig = context.flowConfig();
        EnergyTravel travelMode = null;
        float travelPhase = 0f;
        int chaseCount = 1;
        float chaseWidth = 0.3f;
        
        if (flowConfig != null && flowConfig.hasTravel()) {
            travelMode = flowConfig.effectiveTravel();
            travelPhase = (context.time() * flowConfig.travelSpeed()) % 1.0f;
            chaseCount = flowConfig.chaseCount();
            chaseWidth = flowConfig.chaseWidth();
        }
        
        // === GENERATE MESH ===
        if (fieldDeformation != null && fieldDeformation.isActive()) {
            // With deformation - use full generator with deformation callback
            float[] fieldCenter = new float[] {0, 0, 0};
            float outerRadius = shape != null ? shape.outerRadius() : 3.0f;
            float deformIntensity = context.fieldDeformationIntensity();
            
            GeoPolarSurfaceGenerator.generateWithSphereDeformation(
                builder, deformation, intensity, center, direction, axialLength, baseRadius,
                rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, alpha,
                travelMode, travelPhase, chaseCount, chaseWidth,
                fieldDeformation, fieldCenter, deformIntensity, outerRadius,
                0xFFFFFFFF  // Color will be set by renderer
            );
        } else {
            // No field deformation - use standard generator
            GeoPolarSurfaceGenerator.generateWithSphereDeformation(
                builder, deformation, intensity, center, direction, axialLength, baseRadius,
                rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, alpha,
                travelMode, travelPhase, chaseCount, chaseWidth,
                null, null, 0f, 0f,
                0xFFFFFFFF  // Color will be set by renderer
            );
        }
    }
    
    /**
     * Maps RayType to the appropriate SphereDeformation.
     * 
     * @param rayType The ray type
     * @return Corresponding sphere deformation, defaults to DROPLET
     */
    private static SphereDeformation mapToSphereDeformation(RayType rayType) {
        if (rayType == null) {
            return SphereDeformation.DROPLET;
        }
        
        return switch (rayType) {
            // Direct mappings
            case DROPLET -> SphereDeformation.DROPLET;
            case CONE -> SphereDeformation.CONE;
            case ARROW -> SphereDeformation.CONE;  // Arrow uses cone (pointed tip)
            case CAPSULE -> SphereDeformation.BULLET;  // Capsule uses bullet (hemisphere + cylinder)
            
            // Energy effects - use spheroid or droplet
            case KAMEHAMEHA -> SphereDeformation.SPHEROID;  // Spherical energy ball
            case LASER -> SphereDeformation.SPHEROID;  // Uniform beam
            case FIRE_JET -> SphereDeformation.DROPLET;  // Wide base, narrow tip
            case PLASMA -> SphereDeformation.DROPLET;  // Organic shape
            
            // Particle types - use sphere (no deformation)
            case BEADS -> SphereDeformation.NONE;  // Perfect spheres
            
            // Organic types
            case TENDRIL -> SphereDeformation.EGG;  // Organic asymmetry
            case SPINE -> SphereDeformation.BULLET;  // Segmented with rounded ends
            case ROOT -> SphereDeformation.PEAR;  // Natural taper
            
            // Fallback for non-spherical types (shouldn't reach here)
            case LINE, LIGHTNING, CUBES, STARS, CRYSTALS -> SphereDeformation.NONE;
        };
    }
    
    @Override
    public String name() {
        return "RaySphericalTessellator";
    }
}
