package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoDeformationFactory;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoDeformationStrategy;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoDropletProfile;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoPolarSurfaceGenerator;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoRadiusProfile;
import net.cyberpunk042.client.visual.mesh.ray.geometry3d.GeoRadiusProfileFactory;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeResult;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.shape.RaysShape;
import net.cyberpunk042.visual.shape.RayType;

/**
 * Tessellator for 3D ray types (DROPLET, CONE, etc.).
 * 
 * <p>Uses the modular geometry3d system:
 * <ul>
 *   <li>{@link GeoRadiusProfile} - Defines shape (droplet, cone, egg, bullet)</li>
 *   <li>{@link GeoDeformationStrategy} - Optional deformation (gravity, wave)</li>
 *   <li>{@link GeoPolarSurfaceGenerator} - Generates the mesh</li>
 * </ul>
 * </p>
 * 
 * @see GeoRadiusProfileFactory
 * @see GeoPolarSurfaceGenerator
 */
public class RayDropletTessellator implements RayTypeTessellator {
    
    /** Singleton instance. */
    public static final RayDropletTessellator INSTANCE = new RayDropletTessellator();
    
    /** Minimum rings for smooth 3D shape. */
    private static final int MIN_RINGS = 6;
    
    /** Minimum segments for round cross-section. */
    private static final int MIN_SEGMENTS = 6;
    
    private RayDropletTessellator() {} // Use INSTANCE
    
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
        
        // === SELECT RADIUS PROFILE ===
        RayType rayType = shape != null ? shape.effectiveRayType() : RayType.DROPLET;
        GeoRadiusProfile profile = GeoRadiusProfileFactory.get(rayType);
        if (profile == null) {
            profile = GeoDropletProfile.DEFAULT;
        }
        
        // Custom intensity if available
        float intensity = context.shapeIntensity();
        if (intensity > 0.01f && intensity != 1.0f) {
            profile = GeoRadiusProfileFactory.getCustom(rayType, intensity);
        }
        
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
        
        // === CHECK FOR DEFORMATION ===
        net.cyberpunk042.visual.shape.FieldDeformationMode deformMode = context.fieldDeformation();
        GeoDeformationStrategy deformation = GeoDeformationFactory.get(deformMode);
        
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
        if (deformation != null && deformation.isActive()) {
            // With deformation - use full generator with deformation callback
            float[] fieldCenter = new float[] {0, 0, 0};
            float outerRadius = shape != null ? shape.outerRadius() : 3.0f;
            float deformIntensity = context.fieldDeformationIntensity();
            
            GeoPolarSurfaceGenerator.generateWithDeformation(
                builder, profile, center, direction, axialLength, baseRadius,
                rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, alpha,
                travelMode, travelPhase, chaseCount, chaseWidth,
                deformation, fieldCenter, deformIntensity, outerRadius,
                0xFFFFFFFF  // Color will be set by renderer
            );
        } else {
            // No deformation - use standard generator
            GeoPolarSurfaceGenerator.generateFull(
                builder, profile, center, direction, axialLength, baseRadius,
                rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, alpha,
                travelMode, travelPhase, chaseCount, chaseWidth,
                0xFFFFFFFF  // Color will be set by renderer
            );
        }
    }
}

