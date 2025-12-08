package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.SphereTessellator_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.StripesPrimitive_old;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.SphereShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders latitude stripes on a sphere.
 * 
 * <p>Creates horizontal bands around a sphere, useful for:
 * <ul>
 *   <li>Planet-like bands</li>
 *   <li>Scanning effects</li>
 *   <li>Layered shields</li>
 * </ul>
 */
public final class StripesRenderer_old implements PrimitiveRenderer_old {
    
    private static final String TYPE = "stripes";
    
    public static final StripesRenderer_old INSTANCE = new StripesRenderer_old();
    
    private StripesRenderer_old() {
        Logging.RENDER.topic("stripes").debug("StripesRenderer_old initialized");
    }
    
    @Override
    public String type() {
        return "stripes";
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
        
        if (!(primitive instanceof StripesPrimitive_old stripes)) {
            Logging.RENDER.topic("stripes").warn("Expected StripesPrimitive_old, got: {}", 
                primitive.getClass().getSimpleName());
            return;
        }
        
        SphereShape shape = stripes.getSphereShape();
        float radius = shape.radius();
        int stripeCount = stripes.getStripeCount();
        float stripeRatio = stripes.getStripeRatio();
        boolean alternate = stripes.isAlternate();
        
        // Resolve color
        int color = colorResolver.resolve(stripes.appearance().color());
        float alpha = stripes.appearance().alpha().min();
        int a = (int) (alpha * 255) & 0xFF;
        int argb = (a << 24) | (color & 0x00FFFFFF);
        
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Tessellation detail based on stripe count
        int latSteps = Math.max(stripeCount * 4, 16);
        int lonSteps = latSteps * 2;
        
        // Render each stripe as a partial sphere band
        float stripeHeight = 1.0f / stripeCount;
        
        for (int i = 0; i < stripeCount; i++) {
            // Check if this stripe should be visible
            if (alternate && i % 2 != 0) {
                continue; // Skip odd stripes in alternate mode
            }
            
            float latStart = i * stripeHeight;
            float latEnd = latStart + stripeHeight * stripeRatio;
            
            // Render this stripe band
            renderBand(consumer, matrix, radius, latStart, latEnd, lonSteps, argb, light);
        }
        
        matrices.pop();
        
        Logging.RENDER.topic("stripes").trace(
            "Rendered stripes: count={}, ratio={:.2f}, alternate={}", 
            stripeCount, stripeRatio, alternate);
    }
    
    /**
     * Renders a band (partial sphere between two latitudes).
     */
    private void renderBand(
            VertexConsumer consumer, Matrix4f matrix,
            float radius, float latStart, float latEnd,
            int lonSteps, int argb, int light) {
        
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        
        // Convert latitude fraction to angles (0=top, 1=bottom)
        float thetaStart = latStart * (float) Math.PI;
        float thetaEnd = latEnd * (float) Math.PI;
        
        // Just render two lat rows for the band
        float y1 = radius * (float) Math.cos(thetaStart);
        float r1 = radius * (float) Math.sin(thetaStart);
        float y2 = radius * (float) Math.cos(thetaEnd);
        float r2 = radius * (float) Math.sin(thetaEnd);
        
        for (int lon = 0; lon < lonSteps; lon++) {
            float phi1 = (float) (2 * Math.PI * lon / lonSteps);
            float phi2 = (float) (2 * Math.PI * (lon + 1) / lonSteps);
            
            float cos1 = (float) Math.cos(phi1);
            float sin1 = (float) Math.sin(phi1);
            float cos2 = (float) Math.cos(phi2);
            float sin2 = (float) Math.sin(phi2);
            
            // Four corners of the quad
            float x1a = r1 * cos1, z1a = r1 * sin1;
            float x1b = r1 * cos2, z1b = r1 * sin2;
            float x2a = r2 * cos1, z2a = r2 * sin1;
            float x2b = r2 * cos2, z2b = r2 * sin2;
            
            // Compute normal (radially outward at mid-angle)
            float midPhi = (phi1 + phi2) * 0.5f;
            float nx = (float) Math.cos(midPhi);
            float nz = (float) Math.sin(midPhi);
            
            // Emit quad as two triangles (with full vertex data)
            // Triangle 1
            consumer.vertex(matrix, x1a, y1, z1a).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
            consumer.vertex(matrix, x2a, y2, z2a).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
            consumer.vertex(matrix, x1b, y1, z1b).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
            
            // Triangle 2
            consumer.vertex(matrix, x1b, y1, z1b).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
            consumer.vertex(matrix, x2a, y2, z2a).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
            consumer.vertex(matrix, x2b, y2, z2b).color(r, g, b, a)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(nx, 0, nz);
        }
    }
}
