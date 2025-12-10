#!/usr/bin/env python3
"""
Refactor FieldEditState to use ShapeState with actual shape records.

This script:
1. Creates a ShapeState inner class that holds shape records
2. Replaces individual shape fields with ShapeState
3. Updates getters/setters to delegate to ShapeState
4. Updates toStateJson/fromStateJson to serialize ALL shape params
5. Fixes getCurrentShapeFragmentName() references
"""

import re
import os

TARGET_FILE = "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"

# The new ShapeState inner class
SHAPE_STATE_CLASS = '''
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE STATE - Uses actual immutable shape records (Hybrid A+C pattern)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages shape parameters using the actual shape record types.
     * When a parameter changes, the immutable record is rebuilt via toBuilder().
     */
    public class ShapeState {
        private SphereShape sphere = SphereShape.DEFAULT;
        private RingShape ring = RingShape.DEFAULT;
        private DiscShape disc = DiscShape.DEFAULT;
        private PrismShape prism = PrismShape.builder().build();
        private CylinderShape cylinder = CylinderShape.builder().build();
        private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
        
        // --- Sphere ---
        public SphereShape getSphere() { return sphere; }
        public void setSphere(SphereShape s) { sphere = s; markDirty(); }
        
        public int getSphereLatSteps() { return sphere.latSteps(); }
        public void setSphereLatSteps(int v) { sphere = sphere.toBuilder().latSteps(v).build(); markDirty(); }
        public int getSphereLonSteps() { return sphere.lonSteps(); }
        public void setSphereLonSteps(int v) { sphere = sphere.toBuilder().lonSteps(v).build(); markDirty(); }
        public float getSphereLatStart() { return sphere.latStart(); }
        public void setSphereLatStart(float v) { sphere = sphere.toBuilder().latStart(v).build(); markDirty(); }
        public float getSphereLatEnd() { return sphere.latEnd(); }
        public void setSphereLatEnd(float v) { sphere = sphere.toBuilder().latEnd(v).build(); markDirty(); }
        public String getSphereAlgorithm() { return sphere.algorithm().name(); }
        public void setSphereAlgorithm(String v) { 
            sphere = sphere.toBuilder().algorithm(SphereAlgorithm.valueOf(v)).build(); 
            markDirty(); 
        }
        
        // --- Ring ---
        public RingShape getRing() { return ring; }
        public void setRing(RingShape r) { ring = r; markDirty(); }
        
        public float getRingInnerRadius() { return ring.innerRadius(); }
        public void setRingInnerRadius(float v) { ring = ring.toBuilder().innerRadius(v).build(); markDirty(); }
        public float getRingOuterRadius() { return ring.outerRadius(); }
        public void setRingOuterRadius(float v) { ring = ring.toBuilder().outerRadius(v).build(); markDirty(); }
        public int getRingSegments() { return ring.segments(); }
        public void setRingSegments(int v) { ring = ring.toBuilder().segments(v).build(); markDirty(); }
        public float getRingHeight() { return ring.height(); }
        public void setRingHeight(float v) { ring = ring.toBuilder().height(v).build(); markDirty(); }
        public float getRingY() { return ring.y(); }
        public void setRingY(float v) { ring = ring.toBuilder().y(v).build(); markDirty(); }
        public float getRingArcStart() { return ring.arcStart(); }
        public void setRingArcStart(float v) { ring = ring.toBuilder().arcStart(v).build(); markDirty(); }
        public float getRingArcEnd() { return ring.arcEnd(); }
        public void setRingArcEnd(float v) { ring = ring.toBuilder().arcEnd(v).build(); markDirty(); }
        public float getRingTwist() { return ring.twist(); }
        public void setRingTwist(float v) { ring = ring.toBuilder().twist(v).build(); markDirty(); }
        
        // --- Disc ---
        public DiscShape getDisc() { return disc; }
        public void setDisc(DiscShape d) { disc = d; markDirty(); }
        
        public float getDiscRadius() { return disc.radius(); }
        public void setDiscRadius(float v) { disc = disc.toBuilder().radius(v).build(); markDirty(); }
        public int getDiscSegments() { return disc.segments(); }
        public void setDiscSegments(int v) { disc = disc.toBuilder().segments(v).build(); markDirty(); }
        public float getDiscY() { return disc.y(); }
        public void setDiscY(float v) { disc = disc.toBuilder().y(v).build(); markDirty(); }
        public float getDiscInnerRadius() { return disc.innerRadius(); }
        public void setDiscInnerRadius(float v) { disc = disc.toBuilder().innerRadius(v).build(); markDirty(); }
        public float getDiscArcStart() { return disc.arcStart(); }
        public void setDiscArcStart(float v) { disc = disc.toBuilder().arcStart(v).build(); markDirty(); }
        public float getDiscArcEnd() { return disc.arcEnd(); }
        public void setDiscArcEnd(float v) { disc = disc.toBuilder().arcEnd(v).build(); markDirty(); }
        public int getDiscRings() { return disc.rings(); }
        public void setDiscRings(int v) { disc = disc.toBuilder().rings(v).build(); markDirty(); }
        
        // --- Prism ---
        public PrismShape getPrism() { return prism; }
        public void setPrism(PrismShape p) { prism = p; markDirty(); }
        
        public int getPrismSides() { return prism.sides(); }
        public void setPrismSides(int v) { prism = prism.toBuilder().sides(v).build(); markDirty(); }
        public float getPrismRadius() { return prism.radius(); }
        public void setPrismRadius(float v) { prism = prism.toBuilder().radius(v).build(); markDirty(); }
        public float getPrismHeight() { return prism.height(); }
        public void setPrismHeight(float v) { prism = prism.toBuilder().height(v).build(); markDirty(); }
        public float getPrismTopRadius() { return prism.topRadius(); }
        public void setPrismTopRadius(float v) { prism = prism.toBuilder().topRadius(v).build(); markDirty(); }
        public float getPrismTwist() { return prism.twist(); }
        public void setPrismTwist(float v) { prism = prism.toBuilder().twist(v).build(); markDirty(); }
        public int getPrismHeightSegments() { return prism.heightSegments(); }
        public void setPrismHeightSegments(int v) { prism = prism.toBuilder().heightSegments(v).build(); markDirty(); }
        public boolean isPrismCapTop() { return prism.capTop(); }
        public void setPrismCapTop(boolean v) { prism = prism.toBuilder().capTop(v).build(); markDirty(); }
        public boolean isPrismCapBottom() { return prism.capBottom(); }
        public void setPrismCapBottom(boolean v) { prism = prism.toBuilder().capBottom(v).build(); markDirty(); }
        
        // --- Cylinder ---
        public CylinderShape getCylinder() { return cylinder; }
        public void setCylinder(CylinderShape c) { cylinder = c; markDirty(); }
        
        public float getCylinderRadius() { return cylinder.radius(); }
        public void setCylinderRadius(float v) { cylinder = cylinder.toBuilder().radius(v).build(); markDirty(); }
        public float getCylinderHeight() { return cylinder.height(); }
        public void setCylinderHeight(float v) { cylinder = cylinder.toBuilder().height(v).build(); markDirty(); }
        public int getCylinderSegments() { return cylinder.segments(); }
        public void setCylinderSegments(int v) { cylinder = cylinder.toBuilder().segments(v).build(); markDirty(); }
        public float getCylinderTopRadius() { return cylinder.topRadius(); }
        public void setCylinderTopRadius(float v) { cylinder = cylinder.toBuilder().topRadius(v).build(); markDirty(); }
        public float getCylinderArc() { return cylinder.arc(); }
        public void setCylinderArc(float v) { cylinder = cylinder.toBuilder().arc(v).build(); markDirty(); }
        public int getCylinderHeightSegments() { return cylinder.heightSegments(); }
        public void setCylinderHeightSegments(int v) { cylinder = cylinder.toBuilder().heightSegments(v).build(); markDirty(); }
        public boolean isCylinderCapTop() { return cylinder.capTop(); }
        public void setCylinderCapTop(boolean v) { cylinder = cylinder.toBuilder().capTop(v).build(); markDirty(); }
        public boolean isCylinderCapBottom() { return cylinder.capBottom(); }
        public void setCylinderCapBottom(boolean v) { cylinder = cylinder.toBuilder().capBottom(v).build(); markDirty(); }
        public boolean isCylinderOpenEnded() { return !cylinder.capTop() && !cylinder.capBottom(); }
        public void setCylinderOpenEnded(boolean v) { 
            cylinder = cylinder.toBuilder().capTop(!v).capBottom(!v).build(); 
            markDirty(); 
        }
        
        // --- Polyhedron ---
        public PolyhedronShape getPolyhedron() { return polyhedron; }
        public void setPolyhedron(PolyhedronShape p) { polyhedron = p; markDirty(); }
        
        public String getPolyType() { return polyhedron.polyType().name(); }
        public void setPolyType(String v) { 
            polyhedron = polyhedron.toBuilder().polyType(PolyType.valueOf(v)).build(); 
            markDirty(); 
        }
        public float getPolyRadius() { return polyhedron.radius(); }
        public void setPolyRadius(float v) { polyhedron = polyhedron.toBuilder().radius(v).build(); markDirty(); }
        public int getPolySubdivisions() { return polyhedron.subdivisions(); }
        public void setPolySubdivisions(int v) { polyhedron = polyhedron.toBuilder().subdivisions(v).build(); markDirty(); }
        
        // --- Get current shape based on shapeType ---
        public Shape getCurrentShape() {
            return switch (shapeType.toLowerCase()) {
                case "sphere" -> sphere;
                case "ring" -> ring;
                case "disc" -> disc;
                case "prism" -> prism;
                case "cylinder" -> cylinder;
                case "polyhedron" -> polyhedron;
                default -> sphere;
            };
        }
        
        // --- JSON serialization ---
        public void toJson(com.google.gson.JsonObject json) {
            // Sphere
            json.addProperty("sphereLatSteps", sphere.latSteps());
            json.addProperty("sphereLonSteps", sphere.lonSteps());
            json.addProperty("sphereLatStart", sphere.latStart());
            json.addProperty("sphereLatEnd", sphere.latEnd());
            json.addProperty("sphereAlgorithm", sphere.algorithm().name());
            
            // Ring
            json.addProperty("ringInnerRadius", ring.innerRadius());
            json.addProperty("ringOuterRadius", ring.outerRadius());
            json.addProperty("ringSegments", ring.segments());
            json.addProperty("ringHeight", ring.height());
            json.addProperty("ringY", ring.y());
            json.addProperty("ringArcStart", ring.arcStart());
            json.addProperty("ringArcEnd", ring.arcEnd());
            json.addProperty("ringTwist", ring.twist());
            
            // Disc
            json.addProperty("discRadius", disc.radius());
            json.addProperty("discSegments", disc.segments());
            json.addProperty("discY", disc.y());
            json.addProperty("discInnerRadius", disc.innerRadius());
            json.addProperty("discArcStart", disc.arcStart());
            json.addProperty("discArcEnd", disc.arcEnd());
            json.addProperty("discRings", disc.rings());
            
            // Prism
            json.addProperty("prismSides", prism.sides());
            json.addProperty("prismRadius", prism.radius());
            json.addProperty("prismHeight", prism.height());
            json.addProperty("prismTopRadius", prism.topRadius());
            json.addProperty("prismTwist", prism.twist());
            json.addProperty("prismHeightSegments", prism.heightSegments());
            json.addProperty("prismCapTop", prism.capTop());
            json.addProperty("prismCapBottom", prism.capBottom());
            
            // Cylinder
            json.addProperty("cylinderRadius", cylinder.radius());
            json.addProperty("cylinderHeight", cylinder.height());
            json.addProperty("cylinderSegments", cylinder.segments());
            json.addProperty("cylinderTopRadius", cylinder.topRadius());
            json.addProperty("cylinderArc", cylinder.arc());
            json.addProperty("cylinderHeightSegments", cylinder.heightSegments());
            json.addProperty("cylinderCapTop", cylinder.capTop());
            json.addProperty("cylinderCapBottom", cylinder.capBottom());
            
            // Polyhedron
            json.addProperty("polyType", polyhedron.polyType().name());
            json.addProperty("polyRadius", polyhedron.radius());
            json.addProperty("polySubdivisions", polyhedron.subdivisions());
        }
        
        public void fromJson(com.google.gson.JsonObject json) {
            // Sphere
            SphereShape.Builder sb = sphere.toBuilder();
            if (json.has("sphereLatSteps")) sb.latSteps(json.get("sphereLatSteps").getAsInt());
            if (json.has("sphereLonSteps")) sb.lonSteps(json.get("sphereLonSteps").getAsInt());
            if (json.has("sphereLatStart")) sb.latStart(json.get("sphereLatStart").getAsFloat());
            if (json.has("sphereLatEnd")) sb.latEnd(json.get("sphereLatEnd").getAsFloat());
            if (json.has("sphereAlgorithm")) sb.algorithm(SphereAlgorithm.valueOf(json.get("sphereAlgorithm").getAsString()));
            sphere = sb.build();
            
            // Ring
            RingShape.Builder rb = ring.toBuilder();
            if (json.has("ringInnerRadius")) rb.innerRadius(json.get("ringInnerRadius").getAsFloat());
            if (json.has("ringOuterRadius")) rb.outerRadius(json.get("ringOuterRadius").getAsFloat());
            if (json.has("ringSegments")) rb.segments(json.get("ringSegments").getAsInt());
            if (json.has("ringHeight")) rb.height(json.get("ringHeight").getAsFloat());
            if (json.has("ringY")) rb.y(json.get("ringY").getAsFloat());
            if (json.has("ringArcStart")) rb.arcStart(json.get("ringArcStart").getAsFloat());
            if (json.has("ringArcEnd")) rb.arcEnd(json.get("ringArcEnd").getAsFloat());
            if (json.has("ringTwist")) rb.twist(json.get("ringTwist").getAsFloat());
            ring = rb.build();
            
            // Disc
            DiscShape.Builder db = disc.toBuilder();
            if (json.has("discRadius")) db.radius(json.get("discRadius").getAsFloat());
            if (json.has("discSegments")) db.segments(json.get("discSegments").getAsInt());
            if (json.has("discY")) db.y(json.get("discY").getAsFloat());
            if (json.has("discInnerRadius")) db.innerRadius(json.get("discInnerRadius").getAsFloat());
            if (json.has("discArcStart")) db.arcStart(json.get("discArcStart").getAsFloat());
            if (json.has("discArcEnd")) db.arcEnd(json.get("discArcEnd").getAsFloat());
            if (json.has("discRings")) db.rings(json.get("discRings").getAsInt());
            disc = db.build();
            
            // Prism
            PrismShape.Builder pb = prism.toBuilder();
            if (json.has("prismSides")) pb.sides(json.get("prismSides").getAsInt());
            if (json.has("prismRadius")) pb.radius(json.get("prismRadius").getAsFloat());
            if (json.has("prismHeight")) pb.height(json.get("prismHeight").getAsFloat());
            if (json.has("prismTopRadius")) pb.topRadius(json.get("prismTopRadius").getAsFloat());
            if (json.has("prismTwist")) pb.twist(json.get("prismTwist").getAsFloat());
            if (json.has("prismHeightSegments")) pb.heightSegments(json.get("prismHeightSegments").getAsInt());
            if (json.has("prismCapTop")) pb.capTop(json.get("prismCapTop").getAsBoolean());
            if (json.has("prismCapBottom")) pb.capBottom(json.get("prismCapBottom").getAsBoolean());
            prism = pb.build();
            
            // Cylinder
            CylinderShape.Builder cb = cylinder.toBuilder();
            if (json.has("cylinderRadius")) cb.radius(json.get("cylinderRadius").getAsFloat());
            if (json.has("cylinderHeight")) cb.height(json.get("cylinderHeight").getAsFloat());
            if (json.has("cylinderSegments")) cb.segments(json.get("cylinderSegments").getAsInt());
            if (json.has("cylinderTopRadius")) cb.topRadius(json.get("cylinderTopRadius").getAsFloat());
            if (json.has("cylinderArc")) cb.arc(json.get("cylinderArc").getAsFloat());
            if (json.has("cylinderHeightSegments")) cb.heightSegments(json.get("cylinderHeightSegments").getAsInt());
            if (json.has("cylinderCapTop")) cb.capTop(json.get("cylinderCapTop").getAsBoolean());
            if (json.has("cylinderCapBottom")) cb.capBottom(json.get("cylinderCapBottom").getAsBoolean());
            cylinder = cb.build();
            
            // Polyhedron
            PolyhedronShape.Builder plb = polyhedron.toBuilder();
            if (json.has("polyType")) plb.polyType(PolyType.valueOf(json.get("polyType").getAsString()));
            if (json.has("polyRadius")) plb.radius(json.get("polyRadius").getAsFloat());
            if (json.has("polySubdivisions")) plb.subdivisions(json.get("polySubdivisions").getAsInt());
            polyhedron = plb.build();
        }
    }
    
    // The single ShapeState instance
    private final ShapeState shapes = new ShapeState();
    
    /** Get the shape state container. */
    public ShapeState shapes() { return shapes; }
'''

# New imports needed
NEW_IMPORTS = '''import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.shape.PrismShape;
import net.cyberpunk042.visual.shape.CylinderShape;
import net.cyberpunk042.visual.shape.PolyhedronShape;
import net.cyberpunk042.visual.shape.PolyType;
import net.cyberpunk042.visual.shape.Shape;
'''

# Forwarding methods for backward compatibility
FORWARDING_METHODS = '''
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE PARAMETER FORWARDING (delegates to shapes())
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Sphere
    public int getSphereLatSteps() { return shapes.getSphereLatSteps(); }
    public void setSphereLatSteps(int v) { shapes.setSphereLatSteps(v); }
    public int getSphereLonSteps() { return shapes.getSphereLonSteps(); }
    public void setSphereLonSteps(int v) { shapes.setSphereLonSteps(v); }
    public float getSphereLatStart() { return shapes.getSphereLatStart(); }
    public void setSphereLatStart(float v) { shapes.setSphereLatStart(v); }
    public float getSphereLatEnd() { return shapes.getSphereLatEnd(); }
    public void setSphereLatEnd(float v) { shapes.setSphereLatEnd(v); }
    public String getSphereAlgorithm() { return shapes.getSphereAlgorithm(); }
    public void setSphereAlgorithm(String v) { shapes.setSphereAlgorithm(v); }

    // Ring
    public float getRingInnerRadius() { return shapes.getRingInnerRadius(); }
    public void setRingInnerRadius(float v) { shapes.setRingInnerRadius(v); }
    public float getRingOuterRadius() { return shapes.getRingOuterRadius(); }
    public void setRingOuterRadius(float v) { shapes.setRingOuterRadius(v); }
    public int getRingSegments() { return shapes.getRingSegments(); }
    public void setRingSegments(int v) { shapes.setRingSegments(v); }
    public float getRingHeight() { return shapes.getRingHeight(); }
    public void setRingHeight(float v) { shapes.setRingHeight(v); }
    public float getRingY() { return shapes.getRingY(); }
    public void setRingY(float v) { shapes.setRingY(v); }
    public float getRingArcStart() { return shapes.getRingArcStart(); }
    public void setRingArcStart(float v) { shapes.setRingArcStart(v); }
    public float getRingArcEnd() { return shapes.getRingArcEnd(); }
    public void setRingArcEnd(float v) { shapes.setRingArcEnd(v); }
    public float getRingTwist() { return shapes.getRingTwist(); }
    public void setRingTwist(float v) { shapes.setRingTwist(v); }

    // Disc
    public float getDiscRadius() { return shapes.getDiscRadius(); }
    public void setDiscRadius(float v) { shapes.setDiscRadius(v); }
    public int getDiscSegments() { return shapes.getDiscSegments(); }
    public void setDiscSegments(int v) { shapes.setDiscSegments(v); }
    public float getDiscY() { return shapes.getDiscY(); }
    public void setDiscY(float v) { shapes.setDiscY(v); }
    public float getDiscInnerRadius() { return shapes.getDiscInnerRadius(); }
    public void setDiscInnerRadius(float v) { shapes.setDiscInnerRadius(v); }
    public float getDiscArcStart() { return shapes.getDiscArcStart(); }
    public void setDiscArcStart(float v) { shapes.setDiscArcStart(v); }
    public float getDiscArcEnd() { return shapes.getDiscArcEnd(); }
    public void setDiscArcEnd(float v) { shapes.setDiscArcEnd(v); }
    public int getDiscRings() { return shapes.getDiscRings(); }
    public void setDiscRings(int v) { shapes.setDiscRings(v); }

    // Prism
    public int getPrismSides() { return shapes.getPrismSides(); }
    public void setPrismSides(int v) { shapes.setPrismSides(v); }
    public float getPrismRadius() { return shapes.getPrismRadius(); }
    public void setPrismRadius(float v) { shapes.setPrismRadius(v); }
    public float getPrismHeight() { return shapes.getPrismHeight(); }
    public void setPrismHeight(float v) { shapes.setPrismHeight(v); }
    public float getPrismTopRadius() { return shapes.getPrismTopRadius(); }
    public void setPrismTopRadius(float v) { shapes.setPrismTopRadius(v); }
    public float getPrismTwist() { return shapes.getPrismTwist(); }
    public void setPrismTwist(float v) { shapes.setPrismTwist(v); }
    public int getPrismHeightSegments() { return shapes.getPrismHeightSegments(); }
    public void setPrismHeightSegments(int v) { shapes.setPrismHeightSegments(v); }
    public boolean isPrismCapTop() { return shapes.isPrismCapTop(); }
    public void setPrismCapTop(boolean v) { shapes.setPrismCapTop(v); }
    public boolean isPrismCapBottom() { return shapes.isPrismCapBottom(); }
    public void setPrismCapBottom(boolean v) { shapes.setPrismCapBottom(v); }

    // Cylinder
    public float getCylinderRadius() { return shapes.getCylinderRadius(); }
    public void setCylinderRadius(float v) { shapes.setCylinderRadius(v); }
    public float getCylinderHeight() { return shapes.getCylinderHeight(); }
    public void setCylinderHeight(float v) { shapes.setCylinderHeight(v); }
    public int getCylinderSegments() { return shapes.getCylinderSegments(); }
    public void setCylinderSegments(int v) { shapes.setCylinderSegments(v); }
    public float getCylinderTopRadius() { return shapes.getCylinderTopRadius(); }
    public void setCylinderTopRadius(float v) { shapes.setCylinderTopRadius(v); }
    public float getCylinderArc() { return shapes.getCylinderArc(); }
    public void setCylinderArc(float v) { shapes.setCylinderArc(v); }
    public int getCylinderHeightSegments() { return shapes.getCylinderHeightSegments(); }
    public void setCylinderHeightSegments(int v) { shapes.setCylinderHeightSegments(v); }
    public boolean isCylinderCapTop() { return shapes.isCylinderCapTop(); }
    public void setCylinderCapTop(boolean v) { shapes.setCylinderCapTop(v); }
    public boolean isCylinderCapBottom() { return shapes.isCylinderCapBottom(); }
    public void setCylinderCapBottom(boolean v) { shapes.setCylinderCapBottom(v); }
    public boolean isCylinderOpenEnded() { return shapes.isCylinderOpenEnded(); }
    public void setCylinderOpenEnded(boolean v) { shapes.setCylinderOpenEnded(v); }

    // Polyhedron
    public String getPolyType() { return shapes.getPolyType(); }
    public void setPolyType(String v) { shapes.setPolyType(v); }
    public float getPolyRadius() { return shapes.getPolyRadius(); }
    public void setPolyRadius(float v) { shapes.setPolyRadius(v); }
    public int getPolySubdivisions() { return shapes.getPolySubdivisions(); }
    public void setPolySubdivisions(int v) { shapes.setPolySubdivisions(v); }
'''

def main():
    import sys
    dry_run = '--dry-run' in sys.argv
    
    print("Refactoring FieldEditState to use ShapeState...")
    if dry_run:
        print("🔍 DRY RUN MODE - No changes will be made")
    print("=" * 60)
    
    with open(TARGET_FILE, 'r', encoding='utf-8') as f:
        content = f.read()
    original_content = content
    
    # 1. Add imports after the existing imports
    import_insert_point = content.find("import java.util.ArrayList;")
    if import_insert_point != -1:
        content = content[:import_insert_point] + NEW_IMPORTS + content[import_insert_point:]
        print("✅ Added shape imports")
    else:
        print("⚠️  Could not find import insertion point")
    
    # 2. Find and remove the old SHAPE PARAMETERS section (lines ~863-1002)
    # Pattern: from "// SHAPE PARAMETERS" comment to just before "// BEAM CONFIG"
    shape_start_pattern = r'    // ═+\n    // SHAPE PARAMETERS \(used by ShapeSubPanel\).*?\n    // ═+\n'
    shape_start_match = re.search(shape_start_pattern, content)
    
    if shape_start_match:
        shape_start = shape_start_match.start()
        # Find the next major section (BEAM CONFIG)
        beam_config_pattern = r'    // ═+\n    // BEAM CONFIG'
        beam_match = re.search(beam_config_pattern, content[shape_start:])
        
        if beam_match:
            shape_end = shape_start + beam_match.start()
            # Remove the old shape section and insert new ShapeState
            content = content[:shape_start] + SHAPE_STATE_CLASS + "\n" + FORWARDING_METHODS + "\n\n" + content[shape_end:]
            print("✅ Replaced SHAPE PARAMETERS section with ShapeState class")
        else:
            print("⚠️  Could not find BEAM CONFIG section")
    else:
        print("⚠️  Could not find SHAPE PARAMETERS section - trying alternative")
        # Alternative: look for just "SHAPE PARAMETERS"
        alt_pattern = r'// SHAPE PARAMETERS'
        alt_match = re.search(alt_pattern, content)
        if alt_match:
            print(f"   Found at position {alt_match.start()}")
    
    # 3. Update toStateJson() to use shapes.toJson()
    # Replace the old sphere lines with shapes.toJson(json) call
    old_json_lines = '''        json.addProperty("sphereLatSteps", sphereLatSteps);
        json.addProperty("sphereLonSteps", sphereLonSteps);'''
    new_json_line = '''        // Shape parameters (all shapes)
        shapes.toJson(json);'''
    
    if old_json_lines in content:
        content = content.replace(old_json_lines, new_json_line)
        print("✅ Updated toStateJson() to use shapes.toJson()")
    else:
        print("⚠️  Could not find sphereLatSteps in toStateJson()")
    
    # 4. Update fromStateJson() to use shapes.fromJson()
    old_from_lines = '''        if (json.has("sphereLatSteps")) sphereLatSteps = json.get("sphereLatSteps").getAsInt();
        if (json.has("sphereLonSteps")) sphereLonSteps = json.get("sphereLonSteps").getAsInt();'''
    new_from_line = '''        // Shape parameters (all shapes)
        shapes.fromJson(json);'''
    
    if old_from_lines in content:
        content = content.replace(old_from_lines, new_from_line)
        print("✅ Updated fromStateJson() to use shapes.fromJson()")
    else:
        print("⚠️  Could not find sphereLatSteps in fromStateJson()")
    
    # 5. Update getCurrentShapeFragmentName() to use shapes.getSphereLatSteps()
    old_fragment = 'sphereLatSteps == 32 && sphereLonSteps == 64'
    new_fragment = 'shapes.getSphereLatSteps() == 32 && shapes.getSphereLonSteps() == 64'
    
    if old_fragment in content:
        content = content.replace(old_fragment, new_fragment)
        print("✅ Updated getCurrentShapeFragmentName() to use shapes()")
    else:
        print("⚠️  Could not find sphereLatSteps in getCurrentShapeFragmentName()")
    
    # Summary
    print("=" * 60)
    
    if dry_run:
        # Show diff summary
        original_lines = original_content.count('\n')
        new_lines = content.count('\n')
        print(f"📊 Line count: {original_lines} → {new_lines} ({new_lines - original_lines:+d})")
        
        # Check what sections were found/modified
        print("\n📋 Changes that would be made:")
        print("   1. Add shape imports (9 lines)")
        print("   2. Replace SHAPE PARAMETERS section with ShapeState class (~300 lines)")
        print("   3. Add forwarding methods for backward compatibility (~80 lines)")
        print("   4. Update toStateJson() to call shapes.toJson()")
        print("   5. Update fromStateJson() to call shapes.fromJson()")
        print("   6. Update getCurrentShapeFragmentName() to use shapes()")
        
        print("\n🔍 DRY RUN - No files modified. Run without --dry-run to apply changes.")
    else:
        # Backup
        with open(TARGET_FILE + '.bak', 'w', encoding='utf-8') as f:
            f.write(original_content)
        print(f"✅ Backup created: {TARGET_FILE}.bak")
        
        # Write the result
        with open(TARGET_FILE, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"✅ Refactoring complete!")
        print(f"   Review changes in {TARGET_FILE}")
        print("   Run './gradlew build' to verify compilation")

if __name__ == "__main__":
    main()
