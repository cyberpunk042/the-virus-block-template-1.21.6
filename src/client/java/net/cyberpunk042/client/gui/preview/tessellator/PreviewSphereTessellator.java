package net.cyberpunk042.client.gui.preview.tessellator;

import net.cyberpunk042.client.gui.preview.PreviewProjector;
import net.cyberpunk042.client.gui.preview.PreviewTessellator;
import net.cyberpunk042.client.gui.preview.PreviewTriangle;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Preview tessellator for sphere shapes.
 * 
 * <p>Generates triangles for a sphere using lat/lon grid tessellation.
 * This is DISTINCT from SphereTessellator used in world rendering.</p>
 */
public class PreviewSphereTessellator implements PreviewTessellator {
    
    private static final PreviewSphereTessellator INSTANCE = new PreviewSphereTessellator();
    
    public static PreviewSphereTessellator getInstance() {
        return INSTANCE;
    }
    
    private PreviewSphereTessellator() {}
    
    @Override
    public String getShapeType() {
        return "sphere";
    }
    
    @Override
    public List<PreviewTriangle> tessellate(PreviewProjector projector, 
                                             FieldEditState state, 
                                             int color, 
                                             int detail) {
        List<PreviewTriangle> triangles = new ArrayList<>();
        
        // Get sphere parameters from state
        float radius = state.getFloat("sphere.radius");
        if (radius <= 0) radius = 3f;
        
        // Get visibility mask
        net.cyberpunk042.visual.visibility.VisibilityMask mask = null;
        float time = 0f;
        try {
            mask = state.mask();
            // Animation time for animated masks
            time = (System.currentTimeMillis() % 100000) / 50f;
        } catch (Exception ignored) {}
        
        // Detail controls segment count
        int latSteps = detail;
        int lonSteps = detail * 2;
        
        // Generate lat/lon grid as quads, split into triangles
        for (int lat = 0; lat < latSteps; lat++) {
            for (int lon = 0; lon < lonSteps; lon++) {
                // Calculate UV for this quad's center (for visibility check)
                float u = (lon + 0.5f) / lonSteps;  // 0-1, center of quad
                float v = (lat + 0.5f) / latSteps;  // 0-1, center of quad
                
                // Check visibility mask
                if (mask != null && mask.hasMask() && !mask.isVisible(u, v, time)) {
                    continue;  // Skip this quad - not visible
                }
                
                // Get 4 corner vertices of this quad
                float[] v1 = sphereVertex(lat, lon, latSteps, lonSteps, radius);
                float[] v2 = sphereVertex(lat + 1, lon, latSteps, lonSteps, radius);
                float[] v3 = sphereVertex(lat + 1, lon + 1, latSteps, lonSteps, radius);
                float[] v4 = sphereVertex(lat, lon + 1, latSteps, lonSteps, radius);
                
                // Apply mask alpha for feathered edges (optional - requires alpha support)
                int quadColor = color;
                if (mask != null && mask.feather() > 0) {
                    float alpha = mask.getAlpha(u, v, time);
                    int existingAlpha = (color >> 24) & 0xFF;
                    int newAlpha = (int) (existingAlpha * alpha);
                    quadColor = (newAlpha << 24) | (color & 0x00FFFFFF);
                }
                
                // Split quad into 2 triangles and project
                triangles.add(projector.projectTriangle(v1, v2, v3, quadColor));
                triangles.add(projector.projectTriangle(v1, v3, v4, quadColor));
            }
        }
        
        return triangles;
    }
    
    /**
     * Calculates a vertex on the sphere surface.
     * 
     * @param lat Latitude index (0 = south pole, latSteps = north pole)
     * @param lon Longitude index (0 to lonSteps)
     * @param latSteps Total latitude divisions
     * @param lonSteps Total longitude divisions
     * @param radius Sphere radius
     * @return float[3] {x, y, z}
     */
    private float[] sphereVertex(int lat, int lon, int latSteps, int lonSteps, float radius) {
        // Latitude angle: -PI/2 (south) to +PI/2 (north)
        float latAngle = (float) Math.PI * lat / latSteps - (float) Math.PI / 2;
        // Longitude angle: 0 to 2*PI
        float lonAngle = 2 * (float) Math.PI * lon / lonSteps;
        
        float cosLat = MathHelper.cos(latAngle);
        float sinLat = MathHelper.sin(latAngle);
        float cosLon = MathHelper.cos(lonAngle);
        float sinLon = MathHelper.sin(lonAngle);
        
        return new float[] {
            cosLon * cosLat * radius,  // X
            sinLat * radius,            // Y (up)
            sinLon * cosLat * radius   // Z
        };
    }
}
