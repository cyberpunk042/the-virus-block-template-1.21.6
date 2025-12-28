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
        
        // === MAP RAY TYPE TO SPHERE DEFORMATION ===
        RayType rayType = shape != null ? shape.effectiveRayType() : RayType.DROPLET;
        SphereDeformation deformation = mapToSphereDeformation(rayType);
        float intensity = context.shapeIntensity();
        
        // === FLOW CONFIG ===
        net.cyberpunk042.visual.animation.RayFlowConfig flowConfig = context.flowConfig();
        
        // === RADIATIVE PHASE (compute FIRST - needed for positioning) ===
        final float userPhase = context.effectiveShapeState().phase();
        final boolean animationPlaying = flowConfig != null && flowConfig.hasRadiative();
        
        // Animation phase: clean 0→1→0 cycle for smooth wrapping
        // userPhase is applied as visual offset LATER (to positionT), not to the phase itself
        float animPhase;
        if (animationPlaying) {
            float timeOffset = (context.time() * flowConfig.radiativeSpeed()) % 1.0f;
            if (timeOffset < 0) timeOffset += 1.0f;
            animPhase = timeOffset;  // Clean cycle - no userPhase offset here
        } else {
            animPhase = userPhase;   // Manual mode uses userPhase directly
        }
        
        // Add wave distribution offset for this ray
        float waveOffset = net.cyberpunk042.client.visual.mesh.ray.flow.FlowPhaseStage.computeRayPhaseOffset(
            shape, context.index(), context.count());
        
        // Add layer-based phase offset for emission animation to respect layers
        // Each layer is offset by a fraction of the total phase range
        int layerCount = shape != null ? Math.max(1, shape.layers()) : 1;
        float layerOffset = 0f;
        if (layerCount > 1 && animationPlaying) {
            // Spread layers evenly across the phase cycle
            layerOffset = (float) context.layerIndex() / layerCount;
        }
        
        float rayPhase = (animPhase + waveOffset + layerOffset) % 1.0f;
        if (rayPhase < 0) rayPhase += 1.0f;
        
        // === COMPUTE CLIP RANGE (where the shape is on the ray) ===
        net.cyberpunk042.visual.energy.RadiativeInteraction radiativeMode = 
            shape != null ? shape.effectiveRadiativeInteraction() 
            : net.cyberpunk042.visual.energy.RadiativeInteraction.NONE;
        float segmentLength = shape != null ? shape.effectiveSegmentLength() : 1.0f;
        boolean startFullLength = shape != null && shape.startFullLength();
        
        var clipRange = net.cyberpunk042.client.visual.mesh.ray.tessellation.RadiativeInteractionFactory.compute(
            radiativeMode, rayPhase, segmentLength, startFullLength);
        
        // === APPLY EDGE MODE (HOW the shape looks at edges) ===
        net.cyberpunk042.visual.shape.EdgeTransitionMode edgeMode = context.effectiveShapeState().edgeMode();
        TessEdgeResult edgeResult = net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeModeFactory.compute(
            edgeMode, clipRange);
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        // Position is the CENTER of the clip range, plus userPhase offset during animation
        float basePositionT = (clipRange.start() + clipRange.end()) * 0.5f;
        float positionT;
        if (animationPlaying) {
            // Add userPhase as visual origin offset (wraps smoothly)
            positionT = (basePositionT + userPhase) % 1.0f;
            if (positionT < 0) positionT += 1.0f;
        } else {
            positionT = basePositionT;
        }
        
        // === COMPUTE CENTER POSITION ===
        float length = context.length();
        
        // EdgeMode scale affects WIDTH only (radius)
        // RadiativeInteraction scale affects overall shape size (from modes like OSCILLATION)
        float edgeScale = edgeResult.scale();         // From EdgeMode (CLIP=1, SCALE=varies, FADE=1)
        float radiativeScale = clipRange.scale();     // From RadiativeInteraction (OSCILLATION etc)
        
        // Shape dimensions
        float baseShapeLength = context.shapeLength();
        float axialLength = baseShapeLength * radiativeScale;  // NOT affected by edgeScale
        float baseRadius = (baseShapeLength * 0.5f) * radiativeScale * edgeScale;  // Width is affected by edgeScale
        
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
            // With curvature: position on curved path at positionT
            center = RayGeometryUtils.computeCurvedPosition(start, end, positionT, curvature, curvatureIntensity);
            direction = RayGeometryUtils.computeCurvedTangent(start, end, positionT, curvature, curvatureIntensity);
        } else {
            // Without curvature: linear interpolation at positionT
            center = new float[] {
                start[0] + (end[0] - start[0]) * positionT,
                start[1] + (end[1] - start[1]) * positionT,
                start[2] + (end[2] - start[2]) * positionT
            };
            direction = context.orientationVector();
        }
        
        // === MESH RESOLUTION ===
        int totalSegs = Math.max(12, context.lineResolution());
        int rings = Math.max(MIN_RINGS, totalSegs / 2);
        int segments = Math.max(MIN_SEGMENTS, totalSegs / 2);
        
        // === EDGE MODE PARAMETERS ===
        float visibleTStart = edgeResult.clipStart();
        float visibleTEnd = edgeResult.clipEnd();
        float alpha = edgeResult.alpha();
        float edgeDistStart = edgeResult.edgeDistanceStart();
        float edgeDistEnd = edgeResult.edgeDistanceEnd();
        
        // === FLICKER ANIMATION ===
        if (flowConfig != null && flowConfig.hasFlicker()) {
            float flickerAlpha = net.cyberpunk042.client.visual.mesh.ray.flow.FlowFlickerStage.computeFlickerAlpha(
                flowConfig.effectiveFlicker(), context.time(), context.index(),
                flowConfig.flickerIntensity(), flowConfig.flickerFrequency());
            alpha *= flickerAlpha;
        }
        
        // === CHECK FOR FIELD DEFORMATION ===
        net.cyberpunk042.visual.shape.FieldDeformationMode deformMode = context.fieldDeformation();
        GeoDeformationStrategy fieldDeformation = GeoDeformationFactory.get(deformMode);
        
        // === CHECK FOR TRAVEL EFFECT ===
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
                0xFFFFFFFF,  // Color will be set by renderer
                edgeDistStart, edgeDistEnd
            );
        } else {
            // No field deformation - use standard generator
            GeoPolarSurfaceGenerator.generateWithSphereDeformation(
                builder, deformation, intensity, center, direction, axialLength, baseRadius,
                rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, alpha,
                travelMode, travelPhase, chaseCount, chaseWidth,
                null, null, 0f, 0f,
                0xFFFFFFFF,  // Color will be set by renderer
                edgeDistStart, edgeDistEnd
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
