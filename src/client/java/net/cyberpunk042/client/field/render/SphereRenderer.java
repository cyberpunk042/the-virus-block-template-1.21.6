package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.mesh.GeometryMath;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.SphereCageOptions;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

// Wave animation is supported via the parent class's wave-aware emission

/**
 * Renders sphere primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * <p>Implements proper cage rendering with configurable latitude/longitude lines.</p>
 * 
 * @see SphereShape
 * @see SphereTessellator
 * @see SphereCageOptions
 */
public final class SphereRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "sphere";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof SphereShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Validate pattern is compatible with sphere's QUAD cells
            pattern = arrangement.resolvePattern("main", shape.primaryCellType());
            if (pattern == null) {
                // Pattern mismatch logged to chat - skip rendering
                return null;
            }
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config
        return SphereTessellator.tessellate(shape, pattern, visibility);
    }
    
    /**
     * Emits sphere as cage (latitude/longitude grid lines) with optional wave animation.
     * 
     * <p>Uses {@link SphereCageOptions} for configuration:
     * <ul>
     *   <li>latitudeCount - Number of horizontal rings</li>
     *   <li>longitudeCount - Number of vertical meridians</li>
     *   <li>showEquator - Highlight the equator line</li>
     *   <li>showPoles - Add small pole markers</li>
     * </ul>
     * 
     * <p>Uses {@link MathHelper#sin} and {@link MathHelper#cos} for fast
     * lookup-table based trigonometry.</p>
     */
    @Override
    protected void emitCage(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (!(primitive.shape() instanceof SphereShape shape)) {
            // Fallback to wireframe if not a sphere
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        SphereCageOptions cageOptions = fill != null ? fill.sphereCage() : null;
        if (cageOptions == null) {
            cageOptions = SphereCageOptions.DEFAULT;
        }
        
        float radius = shape.radius();
        int latCount = cageOptions.latitudeCount();
        int lonCount = cageOptions.longitudeCount();
        float thickness = fill != null ? fill.wireThickness() : 1.0f;
        
        // Build cage mesh with lines
        MeshBuilder builder = MeshBuilder.lines();
        
        // === LATITUDE LINES (horizontal rings) ===
        for (int lat = 1; lat < latCount; lat++) {
            float theta = GeometryMath.PI * lat / latCount;  // 0 to PI
            float ringRadius = MathHelper.sin(theta) * radius;
            float y = MathHelper.cos(theta) * radius;
            
            // Draw ring as line segments
            int segments = Math.max(lonCount * 2, 16);
            for (int i = 0; i < segments; i++) {
                float phi1 = GeometryMath.TWO_PI * i / segments;
                float phi2 = GeometryMath.TWO_PI * (i + 1) / segments;
                
                float x1 = MathHelper.cos(phi1) * ringRadius;
                float z1 = MathHelper.sin(phi1) * ringRadius;
                float x2 = MathHelper.cos(phi2) * ringRadius;
                float z2 = MathHelper.sin(phi2) * ringRadius;
                
                // Add line segment using Vertex.pos() factory
                int v1 = builder.addVertex(Vertex.pos(x1, y, z1));
                int v2 = builder.addVertex(Vertex.pos(x2, y, z2));
                builder.line(v1, v2);
            }
        }
        
        // === LONGITUDE LINES (vertical meridians) ===
        for (int lon = 0; lon < lonCount; lon++) {
            float phi = GeometryMath.TWO_PI * lon / lonCount;
            
            // Draw meridian as line segments from pole to pole
            int segments = Math.max(latCount * 2, 16);
            for (int i = 0; i < segments; i++) {
                float theta1 = GeometryMath.PI * i / segments;
                float theta2 = GeometryMath.PI * (i + 1) / segments;
                
                // Spherical to cartesian
                float x1 = MathHelper.sin(theta1) * MathHelper.cos(phi) * radius;
                float y1 = MathHelper.cos(theta1) * radius;
                float z1 = MathHelper.sin(theta1) * MathHelper.sin(phi) * radius;
                
                float x2 = MathHelper.sin(theta2) * MathHelper.cos(phi) * radius;
                float y2 = MathHelper.cos(theta2) * radius;
                float z2 = MathHelper.sin(theta2) * MathHelper.sin(phi) * radius;
                
                int v1 = builder.addVertex(Vertex.pos(x1, y1, z1));
                int v2 = builder.addVertex(Vertex.pos(x2, y2, z2));
                builder.line(v1, v2);
            }
        }
        
        // Emit the cage mesh with wave support
        Mesh cageMesh = builder.build();
        if (waveConfig != null && waveConfig.isActive()) {
            VertexEmitter emitter = new VertexEmitter(matrices, consumer);
            emitter.color(color).light(light).wave(waveConfig, time);
            emitter.emitWireframe(cageMesh, thickness);
        } else {
            VertexEmitter.emitWireframe(matrices.peek(), consumer, cageMesh, color, thickness, light);
        }
    }
}
