package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.DiscTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders disc primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see DiscShape
 * @see DiscTessellator
 */
public final class DiscRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "disc";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof DiscShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Try to resolve pattern - if it fails, continue without pattern
            pattern = arrangement.resolvePattern("surface", shape.primaryCellType());
            // Don't return null if pattern fails - render without pattern instead
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config including wave
    return DiscTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
    
    /**
     * Emits disc cage: radial lines from center + concentric rings.
     * 
     * <p>Uses {@link net.cyberpunk042.visual.fill.DiscCageOptions} for configuration.</p>
     */
    @Override
    protected void emitCage(
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumer consumer,
            net.cyberpunk042.client.visual.mesh.Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.fill.FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (!(primitive.shape() instanceof DiscShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.discCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.DiscCageOptions.DEFAULT;
        }
        
        float outerRadius = shape.radius();
        float innerRadius = shape.innerRadius();  // 0 for solid disc
        int radialLines = cageOptions.radialLines();
        int concentricRings = cageOptions.concentricRings();
        boolean showCenter = cageOptions.showCenter();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === RADIAL LINES ===
        for (int i = 0; i < radialLines; i++) {
            float angle = (float) (2 * Math.PI * i / radialLines);
            float ox = (float) Math.cos(angle) * outerRadius;
            float oz = (float) Math.sin(angle) * outerRadius;
            float ix = (float) Math.cos(angle) * innerRadius;
            float iz = (float) Math.sin(angle) * innerRadius;
            
            var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(ix, 0, iz);
            var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(ox, 0, oz);
            
            if (applyWave) {
                v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
            }
            
            int idx1 = builder.addVertex(v1);
            int idx2 = builder.addVertex(v2);
            builder.line(idx1, idx2);
        }
        
        // === CONCENTRIC RINGS ===
        int ringSegments = Math.max(radialLines * 4, 32);
        // Always include outer edge, optionally include inner edge and intermediate rings
        for (int ring = 0; ring <= concentricRings + 1; ring++) {
            float t = (float) ring / (concentricRings + 1);
            float radius = innerRadius + t * (outerRadius - innerRadius);
            
            // Skip center point if inner radius is 0 and not showing center
            if (ring == 0 && innerRadius < 0.001f && !showCenter) continue;
            
            for (int i = 0; i < ringSegments; i++) {
                float angle1 = (float) (2 * Math.PI * i / ringSegments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / ringSegments);
                
                float x1 = (float) Math.cos(angle1) * radius;
                float z1 = (float) Math.sin(angle1) * radius;
                float x2 = (float) Math.cos(angle2) * radius;
                float z2 = (float) Math.sin(angle2) * radius;
                
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x1, 0, z1);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x2, 0, z2);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        var cageMesh = builder.build();
        var emitter = new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
}
