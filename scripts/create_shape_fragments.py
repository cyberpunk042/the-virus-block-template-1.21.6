#!/usr/bin/env python3
"""
Create shape fragments for all shape types.
Based on 03_PARAMETERS.md Section 4: SHAPE Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
SHAPES_DIR = CONFIG_ROOT / "field_shapes"

SHAPE_FRAGMENTS = {
    # =========================================================================
    # SPHERE (Section 4.1)
    # =========================================================================
    "sphere_default": {
        "name": "Sphere Default",
        "description": "Standard sphere with balanced detail",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 64,
        "latStart": 0.0,
        "latEnd": 1.0,
        "algorithm": "LAT_LON"
    },
    "sphere_lowpoly": {
        "name": "Sphere Lowpoly",
        "description": "Low detail for performance",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 8,
        "lonSteps": 16,
        "algorithm": "LAT_LON"
    },
    "sphere_highpoly": {
        "name": "Sphere Highpoly",
        "description": "High detail sphere",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 64,
        "lonSteps": 128,
        "algorithm": "LAT_LON"
    },
    "sphere_ultra": {
        "name": "Sphere Ultra",
        "description": "Maximum detail for close-up",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 128,
        "lonSteps": 256,
        "algorithm": "LAT_LON"
    },
    "sphere_hemisphere_top": {
        "name": "Hemisphere Top",
        "description": "Top half of sphere",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 16,
        "lonSteps": 64,
        "latStart": 0.0,
        "latEnd": 0.5,
        "algorithm": "LAT_LON"
    },
    "sphere_hemisphere_bottom": {
        "name": "Hemisphere Bottom",
        "description": "Bottom half of sphere",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 16,
        "lonSteps": 64,
        "latStart": 0.5,
        "latEnd": 1.0,
        "algorithm": "LAT_LON"
    },
    "sphere_band": {
        "name": "Sphere Band",
        "description": "Equatorial band only",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 16,
        "lonSteps": 64,
        "latStart": 0.3,
        "latEnd": 0.7,
        "algorithm": "LAT_LON"
    },
    "sphere_partial_lon": {
        "name": "Sphere Partial",
        "description": "270° longitude arc",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 48,
        "lonStart": 0.0,
        "lonEnd": 0.75,
        "algorithm": "LAT_LON"
    },
    "sphere_slice": {
        "name": "Sphere Slice",
        "description": "Quarter to three-quarter slice",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 32,
        "lonStart": 0.25,
        "lonEnd": 0.75,
        "algorithm": "LAT_LON"
    },
    "sphere_type_a": {
        "name": "Sphere Type A",
        "description": "TYPE_A tessellation algorithm",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 64,
        "algorithm": "TYPE_A"
    },
    "sphere_type_e": {
        "name": "Sphere Type E",
        "description": "TYPE_E icosphere tessellation",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 64,
        "algorithm": "TYPE_E"
    },
    # Ribbed/lined effects (unbalanced lat/lon)
    "sphere_ribbed_h": {
        "name": "Sphere Ribbed Horizontal",
        "description": "Dense latitude lines, sparse longitude",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 256,
        "lonSteps": 32,
        "algorithm": "LAT_LON"
    },
    "sphere_ribbed_v": {
        "name": "Sphere Ribbed Vertical",
        "description": "Dense longitude meridians, sparse latitude",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 32,
        "lonSteps": 256,
        "algorithm": "LAT_LON"
    },
    "sphere_latitude_dense": {
        "name": "Sphere Latitude Dense",
        "description": "Extreme horizontal ribbing",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 128,
        "lonSteps": 16,
        "algorithm": "LAT_LON"
    },
    "sphere_longitude_dense": {
        "name": "Sphere Longitude Dense",
        "description": "Extreme vertical meridians",
        "type": "sphere",
        "radius": 1.0,
        "latSteps": 16,
        "lonSteps": 128,
        "algorithm": "LAT_LON"
    },
    
    # =========================================================================
    # RING (Section 4.2)
    # =========================================================================
    "ring_default": {
        "name": "Ring Default",
        "description": "Standard ring band",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 64,
        "y": 0.0
    },
    "ring_thick": {
        "name": "Ring Thick",
        "description": "Wide band ring",
        "type": "ring",
        "innerRadius": 0.5,
        "outerRadius": 1.0,
        "segments": 64,
        "y": 0.0
    },
    "ring_thin": {
        "name": "Ring Thin",
        "description": "Hairline thin ring",
        "type": "ring",
        "innerRadius": 0.95,
        "outerRadius": 1.0,
        "segments": 64,
        "y": 0.0
    },
    "ring_arc_half": {
        "name": "Ring Half Arc",
        "description": "180° arc",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 32,
        "arcStart": 0,
        "arcEnd": 180
    },
    "ring_arc_quarter": {
        "name": "Ring Quarter Arc",
        "description": "90° arc",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 16,
        "arcStart": 0,
        "arcEnd": 90
    },
    "ring_arc_third": {
        "name": "Ring Third Arc",
        "description": "120° arc",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 24,
        "arcStart": 0,
        "arcEnd": 120
    },
    "ring_3d": {
        "name": "Ring 3D",
        "description": "Extruded ring with height",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 64,
        "height": 0.2
    },
    "ring_twisted": {
        "name": "Ring Twisted",
        "description": "Ring with 90° twist",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 64,
        "twist": 90
    },
    "ring_elevated": {
        "name": "Ring Elevated",
        "description": "Ring at Y=1.0",
        "type": "ring",
        "innerRadius": 0.8,
        "outerRadius": 1.0,
        "segments": 64,
        "y": 1.0
    },
    
    # =========================================================================
    # DISC (Section 4.3)
    # =========================================================================
    "disc_default": {
        "name": "Disc Default",
        "description": "Standard flat disc",
        "type": "disc",
        "radius": 1.0,
        "segments": 64,
        "y": 0.0
    },
    "disc_donut": {
        "name": "Disc Donut",
        "description": "Disc with center hole",
        "type": "disc",
        "radius": 1.0,
        "segments": 64,
        "innerRadius": 0.4
    },
    "disc_pacman": {
        "name": "Disc Pacman",
        "description": "Pac-man shaped disc",
        "type": "disc",
        "radius": 1.0,
        "segments": 64,
        "arcStart": 30,
        "arcEnd": 330
    },
    "disc_pie_half": {
        "name": "Disc Half",
        "description": "Half disc",
        "type": "disc",
        "radius": 1.0,
        "segments": 32,
        "arcStart": 0,
        "arcEnd": 180
    },
    "disc_pie_quarter": {
        "name": "Disc Quarter",
        "description": "Quarter disc",
        "type": "disc",
        "radius": 1.0,
        "segments": 16,
        "arcStart": 0,
        "arcEnd": 90
    },
    "disc_detailed": {
        "name": "Disc Detailed",
        "description": "Disc with concentric rings",
        "type": "disc",
        "radius": 1.0,
        "segments": 64,
        "rings": 4
    },
    "disc_target": {
        "name": "Disc Target",
        "description": "Target/bullseye pattern",
        "type": "disc",
        "radius": 1.0,
        "segments": 64,
        "innerRadius": 0.2,
        "rings": 3
    },
    
    # =========================================================================
    # PRISM (Section 4.4)
    # =========================================================================
    "prism_triangle": {
        "name": "Prism Triangle",
        "description": "3-sided prism",
        "type": "prism",
        "sides": 3,
        "radius": 1.0,
        "height": 2.0
    },
    "prism_square": {
        "name": "Prism Square",
        "description": "4-sided prism (box)",
        "type": "prism",
        "sides": 4,
        "radius": 1.0,
        "height": 2.0
    },
    "prism_pentagon": {
        "name": "Prism Pentagon",
        "description": "5-sided prism",
        "type": "prism",
        "sides": 5,
        "radius": 1.0,
        "height": 2.0
    },
    "prism_hex": {
        "name": "Prism Hexagon",
        "description": "6-sided prism (default)",
        "type": "prism",
        "sides": 6,
        "radius": 1.0,
        "height": 2.0
    },
    "prism_octagon": {
        "name": "Prism Octagon",
        "description": "8-sided prism",
        "type": "prism",
        "sides": 8,
        "radius": 1.0,
        "height": 2.0
    },
    "prism_tapered": {
        "name": "Prism Tapered",
        "description": "Tapered prism",
        "type": "prism",
        "sides": 6,
        "radius": 1.0,
        "height": 2.0,
        "topRadius": 0.5
    },
    "prism_pyramid": {
        "name": "Prism Pyramid",
        "description": "Pyramid (topRadius=0)",
        "type": "prism",
        "sides": 4,
        "radius": 1.0,
        "height": 2.0,
        "topRadius": 0.0
    },
    "prism_twisted": {
        "name": "Prism Twisted",
        "description": "Twisted prism",
        "type": "prism",
        "sides": 6,
        "radius": 1.0,
        "height": 2.0,
        "twist": 45
    },
    "prism_tall": {
        "name": "Prism Tall",
        "description": "Tall prism column",
        "type": "prism",
        "sides": 6,
        "radius": 0.5,
        "height": 4.0
    },
    "prism_flat": {
        "name": "Prism Flat",
        "description": "Flat/short prism",
        "type": "prism",
        "sides": 6,
        "radius": 1.5,
        "height": 0.3
    },
    "prism_open": {
        "name": "Prism Open",
        "description": "No caps (tube-like)",
        "type": "prism",
        "sides": 6,
        "radius": 1.0,
        "height": 2.0,
        "capTop": False,
        "capBottom": False
    },
    
    # =========================================================================
    # CYLINDER (Section 4.6)
    # =========================================================================
    "cylinder_default": {
        "name": "Cylinder Default",
        "description": "Standard cylinder",
        "type": "cylinder",
        "radius": 0.5,
        "height": 2.0,
        "segments": 32
    },
    "cylinder_tube": {
        "name": "Cylinder Tube",
        "description": "Hollow tube (no caps)",
        "type": "cylinder",
        "radius": 0.5,
        "height": 2.0,
        "segments": 32,
        "capTop": False,
        "capBottom": False
    },
    "cylinder_cone": {
        "name": "Cylinder Cone",
        "description": "Cone shape",
        "type": "cylinder",
        "radius": 1.0,
        "height": 2.0,
        "segments": 32,
        "topRadius": 0.0
    },
    "cylinder_beam": {
        "name": "Cylinder Beam",
        "description": "Tall thin beam",
        "type": "cylinder",
        "radius": 0.1,
        "height": 10.0,
        "segments": 16,
        "capTop": True,
        "capBottom": False
    },
    "cylinder_tapered": {
        "name": "Cylinder Tapered",
        "description": "Tapered cylinder",
        "type": "cylinder",
        "radius": 1.0,
        "height": 2.0,
        "segments": 32,
        "topRadius": 0.5
    },
    "cylinder_partial": {
        "name": "Cylinder Partial",
        "description": "270° arc cylinder",
        "type": "cylinder",
        "radius": 0.5,
        "height": 2.0,
        "segments": 24,
        "arc": 270
    },
    "cylinder_half_pipe": {
        "name": "Cylinder Half Pipe",
        "description": "180° half pipe",
        "type": "cylinder",
        "radius": 1.0,
        "height": 3.0,
        "segments": 16,
        "arc": 180,
        "capTop": False,
        "capBottom": False
    },
    
    # =========================================================================
    # POLYHEDRON (Section 4.5)
    # =========================================================================
    "poly_cube": {
        "name": "Cube",
        "description": "6-faced cube",
        "type": "polyhedron",
        "polyType": "CUBE",
        "radius": 1.0
    },
    "poly_tetra": {
        "name": "Tetrahedron",
        "description": "4-faced pyramid",
        "type": "polyhedron",
        "polyType": "TETRAHEDRON",
        "radius": 1.0
    },
    "poly_octa": {
        "name": "Octahedron",
        "description": "8-faced diamond",
        "type": "polyhedron",
        "polyType": "OCTAHEDRON",
        "radius": 1.0
    },
    "poly_icosa": {
        "name": "Icosahedron",
        "description": "20-faced shape",
        "type": "polyhedron",
        "polyType": "ICOSAHEDRON",
        "radius": 1.0
    },
    "poly_dodeca": {
        "name": "Dodecahedron",
        "description": "12-faced shape",
        "type": "polyhedron",
        "polyType": "DODECAHEDRON",
        "radius": 1.0
    },
    "poly_icosa_sub1": {
        "name": "Icosahedron Subdivided",
        "description": "Icosahedron with 1 subdivision",
        "type": "polyhedron",
        "polyType": "ICOSAHEDRON",
        "radius": 1.0,
        "subdivisions": 1
    },
    "poly_icosa_sub2": {
        "name": "Icosahedron Smooth",
        "description": "Icosahedron with 2 subdivisions",
        "type": "polyhedron",
        "polyType": "ICOSAHEDRON",
        "radius": 1.0,
        "subdivisions": 2
    },
}


def create_fragments():
    SHAPES_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("SHAPE FRAGMENTS")
    print("=" * 60)
    
    for name, data in SHAPE_FRAGMENTS.items():
        filepath = SHAPES_DIR / f"{name}.json"
        
        if filepath.exists():
            skipped += 1
            print(f"  SKIP: {name}.json")
            continue
        
        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"  Created: {name}.json - {data['name']}")
        created += 1
    
    print("=" * 60)
    print(f"Created: {created} | Skipped: {skipped}")
    print(f"Total shape definitions: {len(SHAPE_FRAGMENTS)}")
    print("=" * 60)


if __name__ == "__main__":
    create_fragments()

