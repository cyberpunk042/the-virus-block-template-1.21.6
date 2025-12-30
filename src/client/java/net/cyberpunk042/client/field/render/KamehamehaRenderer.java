package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.KamehamehaTessellator;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.KamehamehaShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;


/**
 * Renders Kamehameha primitives (energy beam with orb).
 * 
 * <p>Handles the composite orb + beam structure with separate
 * patterns for each component and multi-layer rendering for
 * core and aura effects.</p>
 * 
 * @see KamehamehaShape
 * @see KamehamehaTessellator
 */
public final class KamehamehaRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "kamehameha";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof KamehamehaShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("main", CellType.QUAD);
        }
        if (pattern == null) {
            pattern = QuadPattern.DEFAULT;
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        if (visibility == null) {
            visibility = VisibilityMask.FULL;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("orbRadius", shape.effectiveOrbRadius())
            .kv("beamLength", shape.effectiveBeamLength())
            .kv("orbType", shape.orbType().name())
            .kv("beamType", shape.beamType().name())
            .debug("[KAMEHAMEHA] Tessellating shape");
        
        return KamehamehaTessellator.tessellate(shape, pattern, visibility, time);
    }
    
    /**
     * Emits Kamehameha cage: rings around orb + lines along beam.
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        
        if (!(primitive.shape() instanceof KamehamehaShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        MeshBuilder builder = MeshBuilder.lines();
        
        // Cage lines around orb
        if (shape.isOrbVisible()) {
            tessellateOrbCage(builder, shape, 8, 4);
        }
        
        // Cage lines along beam
        if (shape.isBeamVisible()) {
            tessellateBeamCage(builder, shape, 8, 4);
        }
        
        // Emit the cage mesh
        Mesh cageMesh = builder.build();
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
    
    /**
     * Tessellates cage rings for the orb.
     */
    private void tessellateOrbCage(MeshBuilder builder, KamehamehaShape shape,
                                    int vertLines, int horzRings) {
        float radius = shape.effectiveOrbRadius();
        if (radius <= 0) return;
        
        int segments = 32;
        
        // Horizontal rings (latitude lines)
        for (int ring = 0; ring <= horzRings; ring++) {
            float t = (float) ring / horzRings;
            float theta = (float) (Math.PI * t);
            float y = radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.sin(theta);
            
            if (ringRadius < 0.01f) continue;
            
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (2 * Math.PI * i / segments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                float x1 = (float) Math.cos(angle1) * ringRadius;
                float z1 = (float) Math.sin(angle1) * ringRadius;
                float x2 = (float) Math.cos(angle2) * ringRadius;
                float z2 = (float) Math.sin(angle2) * ringRadius;
                
                int idx1 = builder.addVertex(Vertex.pos(x1, y, z1));
                int idx2 = builder.addVertex(Vertex.pos(x2, y, z2));
                builder.line(idx1, idx2);
            }
        }
        
        // Vertical lines (longitude lines)
        for (int i = 0; i < vertLines; i++) {
            float phi = (float) (2 * Math.PI * i / vertLines);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            
            for (int lat = 0; lat < horzRings; lat++) {
                float theta1 = (float) (Math.PI * lat / horzRings);
                float theta2 = (float) (Math.PI * (lat + 1) / horzRings);
                
                float y1 = radius * (float) Math.cos(theta1);
                float y2 = radius * (float) Math.cos(theta2);
                float r1 = radius * (float) Math.sin(theta1);
                float r2 = radius * (float) Math.sin(theta2);
                
                float x1 = cosPhi * r1;
                float z1 = sinPhi * r1;
                float x2 = cosPhi * r2;
                float z2 = sinPhi * r2;
                
                int idx1 = builder.addVertex(Vertex.pos(x1, y1, z1));
                int idx2 = builder.addVertex(Vertex.pos(x2, y2, z2));
                builder.line(idx1, idx2);
            }
        }
    }
    
    /**
     * Tessellates cage lines for the beam.
     */
    private void tessellateBeamCage(MeshBuilder builder, KamehamehaShape shape,
                                     int vertLines, int horzRings) {
        float startY = shape.effectiveOrbRadius();
        float length = shape.effectiveBeamLength();
        float baseRadius = shape.effectiveBeamBaseRadius();
        float tipRadius = shape.effectiveBeamTipRadius();
        
        if (length <= 0 || baseRadius <= 0) return;
        
        int segments = 32;
        
        // Vertical lines along beam
        for (int i = 0; i < vertLines; i++) {
            float angle = (float) (2 * Math.PI * i / vertLines);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float x1 = cos * baseRadius;
            float z1 = sin * baseRadius;
            float x2 = cos * tipRadius;
            float z2 = sin * tipRadius;
            
            int idx1 = builder.addVertex(Vertex.pos(x1, startY, z1));
            int idx2 = builder.addVertex(Vertex.pos(x2, startY + length, z2));
            builder.line(idx1, idx2);
        }
        
        // Horizontal rings along beam
        for (int ring = 0; ring <= horzRings; ring++) {
            float t = (float) ring / horzRings;
            float y = startY + length * t;
            float r = baseRadius + (tipRadius - baseRadius) * t;
            
            if (r < 0.01f) continue;
            
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (2 * Math.PI * i / segments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                float x1 = (float) Math.cos(angle1) * r;
                float z1 = (float) Math.sin(angle1) * r;
                float x2 = (float) Math.cos(angle2) * r;
                float z2 = (float) Math.sin(angle2) * r;
                
                int idx1 = builder.addVertex(Vertex.pos(x1, y, z1));
                int idx2 = builder.addVertex(Vertex.pos(x2, y, z2));
                builder.line(idx1, idx2);
            }
        }
    }
}
