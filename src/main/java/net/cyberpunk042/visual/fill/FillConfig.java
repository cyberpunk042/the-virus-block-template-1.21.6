package net.cyberpunk042.visual.fill;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for how a primitive's surface is rendered.
 * 
 * <h2>Fill Modes</h2>
 * <ul>
 *   <li>{@link FillMode#SOLID} - Filled triangles (default)</li>
 *   <li>{@link FillMode#WIREFRAME} - All tessellation edges</li>
 *   <li>{@link FillMode#CAGE} - Structured grid lines (uses shape-specific CageOptions)</li>
 *   <li>{@link FillMode#POINTS} - Vertices only (future)</li>
 * </ul>
 * 
 * <h2>Cage Options</h2>
 * <p>When mode is CAGE, use shape-specific options:</p>
 * <ul>
 *   <li>{@link SphereCageOptions} - For spheres</li>
 *   <li>{@link PrismCageOptions} - For prisms</li>
 *   <li>{@link CylinderCageOptions} - For cylinders</li>
 *   <li>{@link PolyhedronCageOptions} - For polyhedra</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "fill": {
 *   "mode": "CAGE",
 *   "wireThickness": 1.0,
 *   "doubleSided": false,
 *   "cage": {
 *     "lineWidth": 2.0,
 *     "latitudeCount": 8,
 *     "longitudeCount": 16
 *   }
 * }
 * </pre>
 * 
 * @see FillMode
 * @see CageOptions
 */
public record FillConfig(
    FillMode mode,
    @Range(ValueRange.POSITIVE_NONZERO) float wireThickness,
    boolean doubleSided,
    boolean depthTest,
    boolean depthWrite,
    @Nullable CageOptions cage
) {
    /** Default solid fill. */
    public static final FillConfig SOLID = new FillConfig(
        FillMode.SOLID, 1.0f, false, true, false, null);
    
    /** Default wireframe fill. */
    public static final FillConfig WIREFRAME = new FillConfig(
        FillMode.WIREFRAME, 1.0f, true, true, false, null);
    
    /** Sphere cage fill. */
    public static final FillConfig SPHERE_CAGE = new FillConfig(
        FillMode.CAGE, 1.0f, true, true, false, SphereCageOptions.DEFAULT);
    
    /** Prism cage fill. */
    public static final FillConfig PRISM_CAGE = new FillConfig(
        FillMode.CAGE, 1.0f, true, true, false, PrismCageOptions.DEFAULT);
    
    /** Cylinder cage fill. */
    public static final FillConfig CYLINDER_CAGE = new FillConfig(
        FillMode.CAGE, 1.0f, true, true, false, CylinderCageOptions.DEFAULT);
    
    /** Polyhedron cage fill. */
    public static final FillConfig POLYHEDRON_CAGE = new FillConfig(
        FillMode.CAGE, 1.0f, true, true, false, PolyhedronCageOptions.DEFAULT);
    
    // =========================================================================
    // Convenience Checks
    // =========================================================================
    
    /** Whether this is solid fill. */
    public boolean isSolid() { return mode == FillMode.SOLID; }
    
    /** Whether this is any line-based fill. */
    public boolean isLineBased() { 
        return mode == FillMode.WIREFRAME || mode == FillMode.CAGE; 
    }
    
    /** Whether this uses cage mode. */
    public boolean isCage() { return mode == FillMode.CAGE; }
    
    /** Gets cage as SphereCageOptions, or null if not sphere cage. */
    public @Nullable SphereCageOptions sphereCage() {
        return cage instanceof SphereCageOptions s ? s : null;
    }
    
    /** Gets cage as PrismCageOptions, or null if not prism cage. */
    public @Nullable PrismCageOptions prismCage() {
        return cage instanceof PrismCageOptions p ? p : null;
    }
    
    /** Gets cage as CylinderCageOptions, or null if not cylinder cage. */
    public @Nullable CylinderCageOptions cylinderCage() {
        return cage instanceof CylinderCageOptions c ? c : null;
    }
    
    /** Gets cage as PolyhedronCageOptions, or null if not polyhedron cage. */
    public @Nullable PolyhedronCageOptions polyhedronCage() {
        return cage instanceof PolyhedronCageOptions p ? p : null;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this fill config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (mode != FillMode.SOLID) json.addProperty("mode", mode.name());
        if (wireThickness != 1.0f) json.addProperty("wireThickness", wireThickness);
        if (doubleSided) json.addProperty("doubleSided", true);
        if (!depthTest) json.addProperty("depthTest", false);
        if (depthWrite) json.addProperty("depthWrite", true);
        if (cage != null) {
            JsonObject cageJson = new JsonObject();
            cageJson.addProperty("lineWidth", cage.lineWidth());
            cageJson.addProperty("showEdges", cage.showEdges());
            json.add("cage", cageJson);
        }
        return json;
    }


    
    public static class Builder {
        private FillMode mode = FillMode.SOLID;
        private @Range(ValueRange.POSITIVE_NONZERO) float wireThickness = 1.0f;
        private boolean doubleSided = false;
        private boolean depthTest = true;
        private boolean depthWrite = false;
        private @Nullable CageOptions cage = null;
        
        public Builder mode(FillMode m) { this.mode = m; return this; }
        public Builder wireThickness(float t) { this.wireThickness = t; return this; }
        public Builder doubleSided(boolean d) { this.doubleSided = d; return this; }
        public Builder depthTest(boolean d) { this.depthTest = d; return this; }
        public Builder depthWrite(boolean d) { this.depthWrite = d; return this; }
        public Builder cage(CageOptions c) { this.cage = c; return this; }
        
        // Convenience methods for shape-specific cages
        public Builder sphereCage(SphereCageOptions c) { 
            this.mode = FillMode.CAGE;
            this.cage = c; 
            return this; 
        }
        public Builder prismCage(PrismCageOptions c) { 
            this.mode = FillMode.CAGE;
            this.cage = c; 
            return this; 
        }
        public Builder cylinderCage(CylinderCageOptions c) { 
            this.mode = FillMode.CAGE;
            this.cage = c; 
            return this; 
        }
        public Builder polyhedronCage(PolyhedronCageOptions c) { 
            this.mode = FillMode.CAGE;
            this.cage = c; 
            return this; 
        }
        
        public FillConfig build() {
            return new FillConfig(mode, wireThickness, doubleSided, depthTest, depthWrite, cage);
        }
    }
}
