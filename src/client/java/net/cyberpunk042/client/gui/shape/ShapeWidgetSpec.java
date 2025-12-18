package net.cyberpunk042.client.gui.shape;

import net.cyberpunk042.visual.shape.SphereAlgorithm;

import java.util.List;

/**
 * Data-driven widget specifications for shape controls.
 * 
 * <p>Each shape type declares its UI controls as a list of specs.
 * The {@link ShapeControlBuilder} converts these specs into actual widgets.</p>
 * 
 * <h3>Design Pattern: Specification Pattern + Factory</h3>
 * <ul>
 *   <li>Specs are pure data (immutable records)</li>
 *   <li>Builder interprets specs to create widgets</li>
 *   <li>Adding new shapes requires only adding specs, not code</li>
 * </ul>
 */
public final class ShapeWidgetSpec {
    
    private ShapeWidgetSpec() {} // Static utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET SPECIFICATION RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Specification for a labeled slider widget.
     * 
     * @param label Display label (e.g., "Radius", "Height")
     * @param stateKey State key for read/write (e.g., "sphere.radius")
     * @param min Minimum value
     * @param max Maximum value
     * @param format Display format (e.g., "%.2f", "%d", "%.0f°")
     * @param step Optional snap step (null for continuous, 1 for integers)
     * @param halfWidth If true, widget takes half the available width (for pairing)
     */
    public record SliderSpec(
        String label,
        String stateKey,
        float min,
        float max,
        String format,
        Float step,
        boolean halfWidth
    ) {
        /** Full-width slider factory */
        public static SliderSpec full(String label, String stateKey, float min, float max, String format) {
            return new SliderSpec(label, stateKey, min, max, format, null, false);
        }
        
        /** Full-width integer slider factory */
        public static SliderSpec fullInt(String label, String stateKey, int min, int max) {
            return new SliderSpec(label, stateKey, min, max, "%d", 1f, false);
        }
        
        /** Half-width slider factory (for pairing two sliders on one row) */
        public static SliderSpec half(String label, String stateKey, float min, float max, String format) {
            return new SliderSpec(label, stateKey, min, max, format, null, true);
        }
        
        /** Half-width integer slider factory */
        public static SliderSpec halfInt(String label, String stateKey, int min, int max) {
            return new SliderSpec(label, stateKey, min, max, "%d", 1f, true);
        }
        
        /** Half-width degree slider (0-360) */
        public static SliderSpec halfDegree(String label, String stateKey) {
            return new SliderSpec(label, stateKey, 0f, 360f, "%.0f°", null, true);
        }
        
        /** Half-width signed degree slider (-360 to 360) */
        public static SliderSpec halfDegreeSigned(String label, String stateKey) {
            return new SliderSpec(label, stateKey, -360f, 360f, "%.0f°", null, true);
        }
    }
    
    /**
     * Specification for a checkbox widget.
     * 
     * @param label Display label
     * @param stateKey State key for boolean value
     * @param tooltip Tooltip text
     * @param halfWidth If true, widget takes half width
     */
    public record CheckboxSpec(
        String label,
        String stateKey,
        String tooltip,
        boolean halfWidth
    ) {
        public static CheckboxSpec half(String label, String stateKey, String tooltip) {
            return new CheckboxSpec(label, stateKey, tooltip, true);
        }
        
        public static CheckboxSpec full(String label, String stateKey, String tooltip) {
            return new CheckboxSpec(label, stateKey, tooltip, false);
        }
    }
    
    /**
     * Specification for an enum dropdown widget.
     * 
     * @param <E> Enum type
     * @param label Display label
     * @param stateKey State key
     * @param enumClass The enum class
     * @param defaultValue Default enum value
     * @param halfWidth If true, takes half width
     */
    public record EnumDropdownSpec<E extends Enum<E>>(
        String label,
        String stateKey,
        Class<E> enumClass,
        E defaultValue,
        boolean halfWidth
    ) {
        public static <E extends Enum<E>> EnumDropdownSpec<E> half(
                String label, String stateKey, Class<E> enumClass, E defaultValue) {
            return new EnumDropdownSpec<>(label, stateKey, enumClass, defaultValue, true);
        }
        
        public static <E extends Enum<E>> EnumDropdownSpec<E> full(
                String label, String stateKey, Class<E> enumClass, E defaultValue) {
            return new EnumDropdownSpec<>(label, stateKey, enumClass, defaultValue, false);
        }
    }
    
    /**
     * Marker for ending a row (forces next widget to new line).
     * Use this when you have an odd number of half-width widgets.
     */
    public record RowBreak() {}
    
    /**
     * Specification for a section label/header.
     */
    public record SectionHeader(String text) {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns widget specs for the given shape type.
     * 
     * @param shapeType Shape type id (e.g., "sphere", "cylinder")
     * @return List of widget specifications in display order
     */
    public static List<Object> forShape(String shapeType) {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> SPHERE_SPECS;
            case "ring" -> RING_SPECS;
            case "disc" -> DISC_SPECS;
            case "prism" -> PRISM_SPECS;
            case "cylinder", "beam" -> CYLINDER_SPECS;
            case "cube", "tetrahedron", "octahedron", "dodecahedron", "icosahedron" -> POLYHEDRON_SPECS;
            case "torus" -> TORUS_SPECS;
            case "capsule" -> CAPSULE_SPECS;
            case "cone" -> CONE_SPECS;
            default -> List.of();
        };
    }
    
    // ───────────────────────────────────────────────────────────────────────────
    // SPHERE
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> SPHERE_SPECS = List.of(
        // Row 1: Radius + Algorithm
        SliderSpec.half("Radius", "sphere.radius", 0.1f, 20.0f, "%.2f"),
        EnumDropdownSpec.half("Algo", "sphere.algorithm", SphereAlgorithm.class, SphereAlgorithm.LAT_LON),
        
        // Row 2: Lat + Lon steps
        SliderSpec.halfInt("Lat", "sphere.latSteps", 4, 256),
        SliderSpec.halfInt("Lon", "sphere.lonSteps", 4, 256),
        
        // Row 3: Lat Start + End (fraction 0-1)
        SliderSpec.half("Start", "sphere.latStart", 0f, 1f, "%.2f"),
        SliderSpec.half("End", "sphere.latEnd", 0f, 1f, "%.2f")
        
        // Note: QuadPattern dropdown is added separately in ShapeSubPanel
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // RING
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> RING_SPECS = List.of(
        // Row 1: Inner R + Outer R
        SliderSpec.half("Inner R", "ring.innerRadius", 0.0f, 10.0f, "%.2f"),
        SliderSpec.half("Outer R", "ring.outerRadius", 0.0f, 20.0f, "%.2f"),
        
        // Row 2: Segments + Height
        SliderSpec.halfInt("Segs", "ring.segments", 3, 512),
        SliderSpec.half("Height", "ring.height", 0f, 10f, "%.2f"),
        
        // Row 3: Y Pos + Twist
        SliderSpec.half("Y Pos", "ring.y", -5f, 5f, "%.2f"),
        SliderSpec.halfDegreeSigned("Twist", "ring.twist"),
        
        // Row 4: Arc Start + End
        SliderSpec.halfDegree("Arc St", "ring.arcStart"),
        SliderSpec.halfDegree("Arc End", "ring.arcEnd")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // DISC
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> DISC_SPECS = List.of(
        // Row 1: Radius + Inner Radius
        SliderSpec.half("Radius", "disc.radius", 0.0f, 20.0f, "%.2f"),
        SliderSpec.half("Inner R", "disc.innerRadius", 0f, 10f, "%.2f"),
        
        // Row 2: Segments + Rings
        SliderSpec.halfInt("Segs", "disc.segments", 3, 512),
        SliderSpec.halfInt("Rings", "disc.rings", 1, 100),
        
        // Row 3: Y Position (full width)
        SliderSpec.full("Y Pos", "disc.y", -5f, 5f, "%.2f"),
        
        // Row 4: Arc Start + End
        SliderSpec.halfDegree("Arc St", "disc.arcStart"),
        SliderSpec.halfDegree("Arc End", "disc.arcEnd")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // PRISM
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> PRISM_SPECS = List.of(
        // Row 1: Sides + Radius
        SliderSpec.halfInt("Sides", "prism.sides", 3, 64),
        SliderSpec.half("Radius", "prism.radius", 0.1f, 20.0f, "%.2f"),
        
        // Row 2: Height + Top Radius
        SliderSpec.half("Height", "prism.height", 0.1f, 20.0f, "%.2f"),
        SliderSpec.half("Top R", "prism.topRadius", 0.0f, 20.0f, "%.2f"),
        
        // Row 3: Twist + Height Segments
        SliderSpec.halfDegreeSigned("Twist", "prism.twist"),
        SliderSpec.halfInt("H Segs", "prism.heightSegments", 1, 100),
        
        // Row 4: Cap Top + Cap Bottom
        CheckboxSpec.half("Cap Top", "prism.capTop", "Render top cap"),
        CheckboxSpec.half("Cap Bottom", "prism.capBottom", "Render bottom cap")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CYLINDER
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CYLINDER_SPECS = List.of(
        // Row 1: Radius + Top Radius
        SliderSpec.half("Radius", "cylinder.radius", 0.1f, 20.0f, "%.2f"),
        SliderSpec.half("Top R", "cylinder.topRadius", 0f, 20f, "%.2f"),
        
        // Row 2: Height + Segments
        SliderSpec.half("Height", "cylinder.height", 0.1f, 256f, "%.2f"),
        SliderSpec.halfInt("Segs", "cylinder.segments", 3, 256),
        
        // Row 3: Arc + Height Segments
        SliderSpec.halfDegree("Arc", "cylinder.arc"),
        SliderSpec.halfInt("H Segs", "cylinder.heightSegments", 1, 100),
        
        // Row 4: Cap Top + Cap Bottom
        CheckboxSpec.half("Cap Top", "cylinder.capTop", "Render top cap"),
        CheckboxSpec.half("Cap Bot", "cylinder.capBottom", "Render bottom cap")
        
        // Note: "Open Ended" checkbox is special logic, handled in ShapeSubPanel
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // POLYHEDRON (Shared by cube, tetrahedron, octahedron, dodecahedron, icosahedron)
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> POLYHEDRON_SPECS = List.of(
        // Full width size
        SliderSpec.full("Size", "polyhedron.radius", 0.5f, 10f, "%.1f"),
        
        // Full width subdivisions
        SliderSpec.fullInt("Subdivisions", "polyhedron.subdivisions", 0, 4)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // TORUS
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> TORUS_SPECS = List.of(
        SliderSpec.full("Ring Radius", "torus.majorRadius", 0.5f, 5f, "%.2f"),
        SliderSpec.full("Tube Radius", "torus.minorRadius", 0.1f, 2f, "%.2f"),
        new SliderSpec("Ring Segments", "torus.majorSegments", 8, 64, "%d", 4f, false),
        new SliderSpec("Tube Segments", "torus.minorSegments", 4, 32, "%d", 2f, false)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CAPSULE
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CAPSULE_SPECS = List.of(
        SliderSpec.full("Radius", "capsule.radius", 0.1f, 3f, "%.2f"),
        SliderSpec.full("Height", "capsule.height", 0.5f, 10f, "%.1f"),
        new SliderSpec("Segments", "capsule.segments", 8, 64, "%d", 4f, false)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CONE
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CONE_SPECS = List.of(
        SliderSpec.full("Bottom Radius", "cone.bottomRadius", 0.1f, 5f, "%.2f"),
        SliderSpec.full("Top Radius", "cone.topRadius", 0f, 5f, "%.2f"),
        SliderSpec.full("Height", "cone.height", 0.5f, 10f, "%.1f"),
        new SliderSpec("Segments", "cone.segments", 8, 64, "%d", 4f, false)
    );
}
