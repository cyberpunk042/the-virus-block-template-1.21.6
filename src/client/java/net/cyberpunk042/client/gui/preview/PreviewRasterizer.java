package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.visual.fill.FillMode;
import net.minecraft.client.gui.DrawContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles triangle rasterization and edge drawing for the preview renderer.
 * 
 * <p>Sorts triangles back-to-front for proper alpha compositing, then
 * renders based on fill mode: solid fills, wireframe edges, cage lines,
 * or vertex points.</p>
 */
public class PreviewRasterizer {
    
    private PreviewRasterizer() {}
    
    /**
     * Renders triangles based on fill mode.
     * 
     * @param ctx Draw context
     * @param triangles List of projected triangles
     * @param mode Fill mode (SOLID, WIREFRAME, CAGE, POINTS)
     * @param edgeColor Color for edge/wireframe drawing
     */
    public static void render(DrawContext ctx, List<PreviewTriangle> triangles, 
                              FillMode mode, int edgeColor) {
        // Sort back-to-front for proper alpha compositing
        triangles.sort((a, b) -> Float.compare(b.depth(), a.depth()));
        
        switch (mode) {
            case SOLID -> renderSolid(ctx, triangles);
            case WIREFRAME -> renderWireframe(ctx, triangles, edgeColor);
            case CAGE -> renderCage(ctx, triangles, edgeColor);
            case POINTS -> renderPoints(ctx, triangles, edgeColor);
        }
    }
    
    private static void renderSolid(DrawContext ctx, List<PreviewTriangle> tris) {
        for (var t : tris) {
            fillTriangle(ctx, t.x1(), t.y1(), t.x2(), t.y2(), t.x3(), t.y3(), t.fillColor());
        }
    }
    
    private static void renderWireframe(DrawContext ctx, List<PreviewTriangle> tris, int edgeColor) {
        // Draw all edges (wireframe = all triangle edges)
        for (var t : tris) {
            drawLine(ctx, t.x1(), t.y1(), t.x2(), t.y2(), edgeColor);
            drawLine(ctx, t.x2(), t.y2(), t.x3(), t.y3(), edgeColor);
            drawLine(ctx, t.x3(), t.y3(), t.x1(), t.y1(), edgeColor);
        }
    }
    
    private static void renderCage(DrawContext ctx, List<PreviewTriangle> tris, int edgeColor) {
        // Cage mode - draw structured grid lines, not all triangle edges
        // For each quad (2 triangles), draw only: bottom edge + left edge
        // This creates a lat/lon grid pattern
        for (int i = 0; i < tris.size(); i += 2) {
            var t1 = tris.get(i);  // First triangle of quad
            // Draw horizontal line (latitude line) - edge from v1 to v4 via the two triangles
            drawLine(ctx, t1.x1(), t1.y1(), t1.x3(), t1.y3(), edgeColor);
            // Draw vertical line (longitude line) - edge from v1 to v2
            drawLine(ctx, t1.x1(), t1.y1(), t1.x2(), t1.y2(), edgeColor);
        }
    }
    
    private static void renderPoints(DrawContext ctx, List<PreviewTriangle> tris, int color) {
        // Collect unique points (using a simple set with rounded coords)
        Set<Long> uniquePoints = new HashSet<>();
        
        for (var t : tris) {
            addPoint(ctx, uniquePoints, t.x1(), t.y1(), color);
            addPoint(ctx, uniquePoints, t.x2(), t.y2(), color);
            addPoint(ctx, uniquePoints, t.x3(), t.y3(), color);
        }
    }
    
    private static void addPoint(DrawContext ctx, Set<Long> visited, float x, float y, int color) {
        int ix = (int) x;
        int iy = (int) y;
        long key = ((long) ix << 32) | (iy & 0xFFFFFFFFL);
        
        if (visited.add(key)) {
            // Draw a 3x3 point
            ctx.fill(ix - 1, iy - 1, ix + 2, iy + 2, color);
        }
    }
    
    // ==========================================================================
    // TRIANGLE FILL - Scanline algorithm
    // ==========================================================================
    
    /**
     * Fills a triangle using scanline rasterization.
     */
    public static void fillTriangle(DrawContext ctx, 
                                     float x1, float y1,
                                     float x2, float y2, 
                                     float x3, float y3, int color) {
        // Sort vertices by Y (top to bottom)
        float tx1 = x1, ty1 = y1, tx2 = x2, ty2 = y2, tx3 = x3, ty3 = y3;
        
        if (ty1 > ty2) { float t = tx1; tx1 = tx2; tx2 = t; t = ty1; ty1 = ty2; ty2 = t; }
        if (ty1 > ty3) { float t = tx1; tx1 = tx3; tx3 = t; t = ty1; ty1 = ty3; ty3 = t; }
        if (ty2 > ty3) { float t = tx2; tx2 = tx3; tx3 = t; t = ty2; ty2 = ty3; ty3 = t; }
        
        // Now ty1 <= ty2 <= ty3
        int minY = (int) Math.ceil(ty1);
        int maxY = (int) Math.floor(ty3);
        
        for (int y = minY; y <= maxY; y++) {
            float leftX, rightX;
            
            if (y < ty2) {
                // Top half: edge from v1 to v2, and edge from v1 to v3
                leftX = interpolateX(y, ty1, tx1, ty2, tx2);
                rightX = interpolateX(y, ty1, tx1, ty3, tx3);
            } else {
                // Bottom half: edge from v2 to v3, and edge from v1 to v3
                leftX = interpolateX(y, ty2, tx2, ty3, tx3);
                rightX = interpolateX(y, ty1, tx1, ty3, tx3);
            }
            
            // Ensure left < right
            if (leftX > rightX) { float t = leftX; leftX = rightX; rightX = t; }
            
            // Draw horizontal scanline
            int ix1 = (int) Math.floor(leftX);
            int ix2 = (int) Math.ceil(rightX);
            if (ix2 > ix1) {
                ctx.fill(ix1, y, ix2, y + 1, color);
            }
        }
    }
    
    private static float interpolateX(float y, float y1, float x1, float y2, float x2) {
        if (Math.abs(y2 - y1) < 0.001f) return x1;
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
    }
    
    // ==========================================================================
    // LINE DRAWING - Bresenham-style
    // ==========================================================================
    
    /**
     * Draws a line efficiently using horizontal spans where possible.
     * Uses a simple approach: draw line as a thin filled rectangle for mostly-horizontal lines,
     * or as connected pixels for diagonal lines (but batched).
     */
    public static void drawLine(DrawContext ctx, float x1, float y1, float x2, float y2, int color) {
        int ix1 = (int) x1;
        int iy1 = (int) y1;
        int ix2 = (int) x2;
        int iy2 = (int) y2;
        
        int dx = Math.abs(ix2 - ix1);
        int dy = Math.abs(iy2 - iy1);
        
        if (dx == 0 && dy == 0) {
            // Single point
            ctx.fill(ix1, iy1, ix1 + 1, iy1 + 1, color);
            return;
        }
        
        if (dy == 0) {
            // Horizontal line - single fill
            int minX = Math.min(ix1, ix2);
            int maxX = Math.max(ix1, ix2);
            ctx.fill(minX, iy1, maxX + 1, iy1 + 1, color);
            return;
        }
        
        if (dx == 0) {
            // Vertical line - single fill
            int minY = Math.min(iy1, iy2);
            int maxY = Math.max(iy1, iy2);
            ctx.fill(ix1, minY, ix1 + 1, maxY + 1, color);
            return;
        }
        
        // Diagonal line - draw using Bresenham but batch by rows
        // For a more efficient approach, draw horizontal spans for each row
        int sx = ix1 < ix2 ? 1 : -1;
        int sy = iy1 < iy2 ? 1 : -1;
        int err = dx - dy;
        
        int x = ix1;
        int y = iy1;
        int rowStartX = x;
        int lastY = y;
        
        while (true) {
            if (y != lastY) {
                // Y changed, flush the horizontal span
                int minX = Math.min(rowStartX, x - sx);
                int maxX = Math.max(rowStartX, x - sx);
                ctx.fill(minX, lastY, maxX + 1, lastY + 1, color);
                rowStartX = x;
                lastY = y;
            }
            
            if (x == ix2 && y == iy2) {
                // Flush final span
                int minX = Math.min(rowStartX, x);
                int maxX = Math.max(rowStartX, x);
                ctx.fill(minX, y, maxX + 1, y + 1, color);
                break;
            }
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
    
    // ==========================================================================
    // COLOR UTILITIES
    // ==========================================================================
    
    /**
     * Applies alpha multiplier to a color.
     * If color has no alpha (0x00RRGGBB), treats it as fully opaque first.
     */
    public static int applyAlpha(int color, float alpha) {
        int existingAlpha = (color >> 24) & 0xFF;
        // If no alpha in input, assume fully opaque
        if (existingAlpha == 0) existingAlpha = 0xFF;
        int a = (int) (existingAlpha * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * Dims a color by a factor.
     */
    public static int dimColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
