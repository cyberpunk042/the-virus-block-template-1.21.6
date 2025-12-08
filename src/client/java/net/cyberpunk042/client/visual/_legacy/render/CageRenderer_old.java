package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.PrimitiveType;
import net.cyberpunk042.field._legacy.primitive.CagePrimitive_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.pattern.EdgePattern;
import net.cyberpunk042.visual.pattern.DynamicEdgePattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders wireframe cage (lat/lon grid on sphere).
 * 
 * <p>Creates a wireframe sphere with latitude and longitude lines,
 * useful for sci-fi force field effects.
 */
public final class CageRenderer_old implements PrimitiveRenderer_old {
    
    private static final String TYPE = "cage";
    
    public static final CageRenderer_old INSTANCE = new CageRenderer_old();
    
    private CageRenderer_old() {
        Logging.RENDER.topic("cage").debug("CageRenderer_old initialized");
    }
    
    @Override
    public String type() {
        return "cage";
    }
    
    @Override
    public boolean supports(String type) {
        return TYPE.equals(type);
    }
    
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver colorResolver,
            RenderOverrides_old overrides) {
        
        if (!(primitive instanceof CagePrimitive_old cage)) {
            Logging.RENDER.topic("cage").warn("Expected CagePrimitive_old, got: {}", 
                primitive.getClass().getSimpleName());
            return;
        }
        
        SphereShape shape = (SphereShape) cage.shape();
        float radius = shape.radius();
        int latLines = cage.getLatLines();
        int lonLines = cage.getLonLines();
        float thickness = cage.wireThickness();
        
        // Get edge pattern (from overrides or appearance)
        // Can be static EdgePattern or dynamic DynamicEdgePattern
        boolean renderLat = true;
        boolean renderLon = true;
        int skipInterval = 1;
        String patternId = "default";
        
        if (overrides != null && overrides.vertexPattern() != null) {
            VertexPattern vp = overrides.vertexPattern();
            if (vp instanceof DynamicEdgePattern dep) {
                renderLat = dep.renderLatitude();
                renderLon = dep.renderLongitude();
                skipInterval = dep.skipInterval();
                patternId = dep.id();
            } else if (vp instanceof EdgePattern ep) {
                renderLat = ep.renderLatitude();
                renderLon = ep.renderLongitude();
                skipInterval = 1; // static patterns use shouldRender
                patternId = ep.id();
            }
        } else if (cage.appearance().pattern() != null) {
            EdgePattern ep = cage.appearance().pattern().edgePattern();
            renderLat = ep.renderLatitude();
            renderLon = ep.renderLongitude();
            patternId = ep.id();
        }
        
        // Resolve color
        int color = colorResolver.resolve(cage.appearance().color());
        float alpha = cage.appearance().alpha().min();
        int a = (int) (alpha * 255) & 0xFF;
        int argb = (a << 24) | (color & 0x00FFFFFF);
        
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Draw latitude lines (horizontal circles) - if pattern allows
        if (renderLat) {
            for (int lat = 1; lat < latLines; lat++) {
                // Skip based on pattern interval
                if ((lat % skipInterval) != 0) {
                    continue;
                }
                
                float latAngle = (float) Math.PI * lat / latLines;
                float y = radius * (float) Math.cos(latAngle);
                float ringRadius = radius * (float) Math.sin(latAngle);
                
                drawCircle(consumer, matrix, 0, y, 0, ringRadius, lonLines * 2, argb, light, true);
            }
        }
        
        // Draw longitude lines (vertical arcs) - if pattern allows
        if (renderLon) {
            for (int lon = 0; lon < lonLines; lon++) {
                // Skip based on pattern interval
                if ((lon % skipInterval) != 0) {
                    continue;
                }
                
                float lonAngle = (float) (2 * Math.PI * lon / lonLines);
                drawLongitudeArc(consumer, matrix, radius, lonAngle, latLines * 2, argb, light);
            }
        }
        
        matrices.pop();
        
        Logging.RENDER.topic("cage").trace(
            "Rendered cage: radius={}, lat={}, lon={}, pattern={}", 
            radius, latLines, lonLines, patternId);
    }
    
    /**
     * Draws a horizontal circle at the given Y position.
     */
    private void drawCircle(
            VertexConsumer consumer, Matrix4f matrix,
            float cx, float cy, float cz,
            float radius, int segments,
            int argb, int light, boolean close) {
        
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        
        float prevX = cx + radius;
        float prevZ = cz;
        
        for (int i = 1; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float x = cx + radius * (float) Math.cos(angle);
            float z = cz + radius * (float) Math.sin(angle);
            
            // Line from previous to current (with full vertex data)
            consumer.vertex(matrix, prevX, cy, prevZ)
                .color(r, g, b, a)
                .texture(0, 0)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0, 1, 0);
            consumer.vertex(matrix, x, cy, z)
                .color(r, g, b, a)
                .texture(0, 0)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0, 1, 0);
            
            prevX = x;
            prevZ = z;
        }
    }
    
    /**
     * Draws a vertical arc (longitude line).
     */
    private void drawLongitudeArc(
            VertexConsumer consumer, Matrix4f matrix,
            float radius, float lonAngle, int segments,
            int argb, int light) {
        
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        
        float cosLon = (float) Math.cos(lonAngle);
        float sinLon = (float) Math.sin(lonAngle);
        
        float prevX = 0, prevY = radius, prevZ = 0;
        
        for (int i = 1; i <= segments; i++) {
            float latAngle = (float) Math.PI * i / segments;
            float y = radius * (float) Math.cos(latAngle);
            float ringRadius = radius * (float) Math.sin(latAngle);
            float x = ringRadius * cosLon;
            float z = ringRadius * sinLon;
            
            // Compute normal for this arc segment
            float nx = (float) Math.cos(lonAngle);
            float nz = (float) Math.sin(lonAngle);
            
            consumer.vertex(matrix, prevX, prevY, prevZ)
                .color(r, g, b, a)
                .texture(0, 0)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nx, 0, nz);
            consumer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .texture(0, 0)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nx, 0, nz);
            
            prevX = x;
            prevY = y;
            prevZ = z;
        }
    }
}
