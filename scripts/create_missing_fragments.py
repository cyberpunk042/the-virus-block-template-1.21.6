#!/usr/bin/env python3
"""
Create all missing fragment files for the field system.

Run with --dry-run to see what would be created without writing files.
Run without flags to create all files.
"""

import json
import os
from pathlib import Path
import argparse

CONFIG_ROOT = Path("config/the-virus-block")

# =============================================================================
# FRAGMENT DEFINITIONS
# =============================================================================

FRAGMENTS = {
    # -------------------------------------------------------------------------
    # ARRANGEMENTS (per pattern type)
    # -------------------------------------------------------------------------
    "field_arrangements": {
        # Quad patterns (for spheres, prisms, polyhedra)
        "quad_default": {
            "name": "Quad Default",
            "description": "Standard quad pattern - all cells visible",
            "patternType": "quad",
            "pattern": "all"
        },
        "quad_alternating": {
            "name": "Quad Alternating",
            "description": "Checkerboard quad pattern",
            "patternType": "quad",
            "pattern": "alternating"
        },
        "quad_stripes": {
            "name": "Quad Stripes",
            "description": "Horizontal stripes pattern",
            "patternType": "quad",
            "pattern": "stripes"
        },
        "quad_sparse": {
            "name": "Quad Sparse",
            "description": "Every other cell in both directions",
            "patternType": "quad",
            "pattern": "sparse"
        },
        "quad_wave": {
            "name": "Quad Wave",
            "description": "Wave pattern on quads",
            "patternType": "quad",
            "pattern": "wave"
        },
        "quad_triangle": {
            "name": "Quad Triangle",
            "description": "Triangular pattern on quads",
            "patternType": "quad",
            "pattern": "triangle_1"
        },
        # Segment patterns (for rings)
        "segment_default": {
            "name": "Segment Default",
            "description": "All segments visible",
            "patternType": "segment",
            "pattern": "all"
        },
        "segment_alternating": {
            "name": "Segment Alternating",
            "description": "Every other segment visible",
            "patternType": "segment",
            "pattern": "alternating"
        },
        "segment_gaps": {
            "name": "Segment Gaps",
            "description": "Segments with gaps",
            "patternType": "segment",
            "pattern": "gaps",
            "gapCount": 4
        },
        "segment_sparse": {
            "name": "Segment Sparse",
            "description": "Few segments visible",
            "patternType": "segment",
            "pattern": "sparse"
        },
        "segment_dashed": {
            "name": "Segment Dashed",
            "description": "Dashed segment pattern",
            "patternType": "segment",
            "pattern": "dashed"
        },
        "segment_zigzag": {
            "name": "Segment Zigzag",
            "description": "Zigzag segment pattern",
            "patternType": "segment",
            "pattern": "zigzag"
        },
        # Sector patterns (for discs)
        "sector_default": {
            "name": "Sector Default",
            "description": "All sectors visible",
            "patternType": "sector",
            "pattern": "all"
        },
        "sector_alternating": {
            "name": "Sector Alternating",
            "description": "Every other sector",
            "patternType": "sector",
            "pattern": "alternating"
        },
        "sector_half": {
            "name": "Sector Half",
            "description": "Half disc visible",
            "patternType": "sector",
            "pattern": "half"
        },
        "sector_quarter": {
            "name": "Sector Quarter",
            "description": "Quarter disc visible",
            "patternType": "sector",
            "pattern": "quarter"
        },
        "sector_pinwheel": {
            "name": "Sector Pinwheel",
            "description": "Pinwheel sector pattern",
            "patternType": "sector",
            "pattern": "pinwheel"
        },
        "sector_spiral": {
            "name": "Sector Spiral",
            "description": "Spiral sector pattern",
            "patternType": "sector",
            "pattern": "spiral"
        },
        # Edge patterns (for cages/wireframes)
        "edge_default": {
            "name": "Edge Default",
            "description": "All edges visible",
            "patternType": "edge",
            "pattern": "all"
        },
        "edge_sparse": {
            "name": "Edge Sparse",
            "description": "Reduced edge density",
            "patternType": "edge",
            "pattern": "sparse"
        },
        "edge_dense": {
            "name": "Edge Dense",
            "description": "High edge density",
            "patternType": "edge",
            "pattern": "dense"
        },
        "edge_latitude": {
            "name": "Edge Latitude Only",
            "description": "Only latitude lines",
            "patternType": "edge",
            "pattern": "latitude"
        },
        "edge_longitude": {
            "name": "Edge Longitude Only",
            "description": "Only longitude lines",
            "patternType": "edge",
            "pattern": "longitude"
        },
    },
    
    # -------------------------------------------------------------------------
    # SHAPES (missing variants)
    # -------------------------------------------------------------------------
    "field_shapes": {
        # Polyhedra
        "default_cube": {
            "name": "Cube",
            "description": "6-sided polyhedron",
            "type": "polyhedron",
            "polyType": "CUBE",
            "radius": 1.0,
            "subdivisions": 0
        },
        "default_tetrahedron": {
            "name": "Tetrahedron",
            "description": "4-sided polyhedron",
            "type": "polyhedron",
            "polyType": "TETRAHEDRON",
            "radius": 1.0,
            "subdivisions": 0
        },
        "default_octahedron": {
            "name": "Octahedron",
            "description": "8-sided polyhedron",
            "type": "polyhedron",
            "polyType": "OCTAHEDRON",
            "radius": 1.0,
            "subdivisions": 0
        },
        "default_icosahedron": {
            "name": "Icosahedron",
            "description": "20-sided polyhedron",
            "type": "polyhedron",
            "polyType": "ICOSAHEDRON",
            "radius": 1.0,
            "subdivisions": 0
        },
        "default_dodecahedron": {
            "name": "Dodecahedron",
            "description": "12-sided polyhedron",
            "type": "polyhedron",
            "polyType": "DODECAHEDRON",
            "radius": 1.0,
            "subdivisions": 0
        },
        # Lowpoly variants
        "lowpoly_ring": {
            "name": "Lowpoly Ring",
            "description": "Low-complexity ring for performance",
            "type": "ring",
            "innerRadius": 0.8,
            "outerRadius": 1.2,
            "segments": 16,
            "height": 0.0
        },
        "lowpoly_disc": {
            "name": "Lowpoly Disc",
            "description": "Low-complexity disc for performance",
            "type": "disc",
            "radius": 1.0,
            "segments": 12
        },
        "lowpoly_prism": {
            "name": "Lowpoly Prism",
            "description": "Low-complexity prism (4 sides)",
            "type": "prism",
            "radius": 1.0,
            "height": 2.0,
            "sides": 4
        },
        "lowpoly_cylinder": {
            "name": "Lowpoly Cylinder",
            "description": "Low-complexity cylinder for performance",
            "type": "cylinder",
            "radius": 1.0,
            "height": 2.0,
            "segments": 12
        },
        # Partial shapes
        "partial_sphere": {
            "name": "Partial Sphere",
            "description": "Sphere with arc (270 degrees)",
            "type": "sphere",
            "radius": 1.0,
            "latSteps": 24,
            "lonSteps": 48,
            "arcStart": 0.0,
            "arcEnd": 0.75
        },
        "partial_ring": {
            "name": "Partial Ring",
            "description": "Ring arc (180 degrees)",
            "type": "ring",
            "innerRadius": 0.8,
            "outerRadius": 1.2,
            "segments": 48,
            "arcStart": 0.0,
            "arcEnd": 0.5
        },
    },
    
    # -------------------------------------------------------------------------
    # FILLS (missing variants)
    # -------------------------------------------------------------------------
    "field_fills": {
        "solid": {
            "name": "Solid",
            "description": "Standard solid fill",
            "mode": "SOLID",
            "doubleSided": False,
            "depthTest": True
        },
        "wireframe": {
            "name": "Wireframe",
            "description": "Edge wireframe",
            "mode": "WIREFRAME",
            "wireThickness": 1.0
        },
        "cage_sparse": {
            "name": "Cage Sparse",
            "description": "Sparse latitude/longitude grid",
            "mode": "CAGE",
            "latCount": 4,
            "lonCount": 8
        },
    },
    
    # -------------------------------------------------------------------------
    # MASKS (missing variants)
    # -------------------------------------------------------------------------
    "field_masks": {
        "half_top": {
            "name": "Half Top",
            "description": "Top half visible",
            "type": "GRADIENT",
            "offset": 0.5,
            "inverted": False
        },
        "half_bottom": {
            "name": "Half Bottom",
            "description": "Bottom half visible",
            "type": "GRADIENT",
            "offset": 0.5,
            "inverted": True
        },
        "quarter": {
            "name": "Quarter",
            "description": "Quarter section visible",
            "type": "RADIAL",
            "count": 4,
            "thickness": 0.25
        },
        "spiral": {
            "name": "Spiral",
            "description": "Spiral pattern",
            "type": "CUSTOM",
            "pattern": "spiral",
            "density": 4
        },
    },
    
    # -------------------------------------------------------------------------
    # ANIMATIONS (missing variants)
    # -------------------------------------------------------------------------
    "field_animations": {
        "spin_medium": {
            "name": "Spin Medium",
            "description": "Medium rotation speed",
            "spin": {
                "enabled": True,
                "axis": "Y",
                "speed": 30.0
            }
        },
        "pulse_medium": {
            "name": "Pulse Medium",
            "description": "Medium pulse intensity",
            "pulse": {
                "enabled": True,
                "mode": "SCALE",
                "frequency": 1.0,
                "amplitude": 0.15
            }
        },
        "pulse_strong": {
            "name": "Pulse Strong",
            "description": "Strong pulsing effect",
            "pulse": {
                "enabled": True,
                "mode": "SCALE",
                "frequency": 1.5,
                "amplitude": 0.25
            }
        },
        "wobble_soft": {
            "name": "Wobble Soft",
            "description": "Gentle wobble effect",
            "wobble": {
                "enabled": True,
                "amplitudeX": 0.05,
                "amplitudeY": 0.05,
                "amplitudeZ": 0.05,
                "speed": 1.0
            }
        },
        "wobble_strong": {
            "name": "Wobble Strong",
            "description": "Pronounced wobble effect",
            "wobble": {
                "enabled": True,
                "amplitudeX": 0.15,
                "amplitudeY": 0.15,
                "amplitudeZ": 0.15,
                "speed": 1.5
            }
        },
        "color_cycle_slow": {
            "name": "Color Cycle Slow",
            "description": "Slow color cycling",
            "colorCycle": {
                "enabled": True,
                "speed": 0.5,
                "blend": "HSV"
            }
        },
        "color_cycle_fast": {
            "name": "Color Cycle Fast",
            "description": "Fast color cycling",
            "colorCycle": {
                "enabled": True,
                "speed": 2.0,
                "blend": "HSV"
            }
        },
        "breathing_slow": {
            "name": "Breathing Slow",
            "description": "Slow breathing alpha",
            "breathing": True,
            "alphaFade": {
                "enabled": True,
                "min": 0.4,
                "max": 0.9,
                "speed": 0.5
            }
        },
        "breathing_fast": {
            "name": "Breathing Fast",
            "description": "Fast breathing alpha",
            "breathing": True,
            "alphaFade": {
                "enabled": True,
                "min": 0.3,
                "max": 1.0,
                "speed": 2.0
            }
        },
        "orbit_slow": {
            "name": "Orbit Slow",
            "description": "Slow orbital motion",
            "orbit": {
                "enabled": True,
                "radius": 0.5,
                "speed": 15.0,
                "axis": "Y"
            }
        },
        "orbit_fast": {
            "name": "Orbit Fast",
            "description": "Fast orbital motion",
            "orbit": {
                "enabled": True,
                "radius": 0.5,
                "speed": 45.0,
                "axis": "Y"
            }
        },
    },
    
    # -------------------------------------------------------------------------
    # TRANSFORMS (NEW FOLDER)
    # -------------------------------------------------------------------------
    "field_transforms": {
        "default": {
            "name": "Default",
            "description": "No transform modifications",
            "anchor": "CENTER",
            "scale": 1.0,
            "offset": [0, 0, 0],
            "rotation": [0, 0, 0]
        },
        "elevated": {
            "name": "Elevated",
            "description": "Raised above origin",
            "anchor": "BASE",
            "scale": 1.0,
            "offset": [0, 1.0, 0],
            "rotation": [0, 0, 0]
        },
        "ground_level": {
            "name": "Ground Level",
            "description": "Positioned at feet level",
            "anchor": "BASE",
            "scale": 1.0,
            "offset": [0, 0.1, 0],
            "rotation": [0, 0, 0]
        },
        "tilted_x": {
            "name": "Tilted X",
            "description": "Tilted on X axis",
            "anchor": "CENTER",
            "scale": 1.0,
            "offset": [0, 0, 0],
            "rotation": [30, 0, 0]
        },
        "tilted_z": {
            "name": "Tilted Z",
            "description": "Tilted on Z axis",
            "anchor": "CENTER",
            "scale": 1.0,
            "offset": [0, 0, 0],
            "rotation": [0, 0, 30]
        },
        "scaled_small": {
            "name": "Scaled Small",
            "description": "Scaled down 50%",
            "anchor": "CENTER",
            "scale": 0.5,
            "offset": [0, 0, 0],
            "rotation": [0, 0, 0]
        },
        "scaled_large": {
            "name": "Scaled Large",
            "description": "Scaled up 150%",
            "anchor": "CENTER",
            "scale": 1.5,
            "offset": [0, 0, 0],
            "rotation": [0, 0, 0]
        },
        "billboard": {
            "name": "Billboard",
            "description": "Always faces camera",
            "anchor": "CENTER",
            "scale": 1.0,
            "billboard": True
        },
    },
    
    # -------------------------------------------------------------------------
    # APPEARANCES (NEW FOLDER)
    # -------------------------------------------------------------------------
    "field_appearances": {
        "default": {
            "name": "Default",
            "description": "Standard appearance",
            "glow": 0.3,
            "emissive": 0.0,
            "saturation": 1.0
        },
        "glowy": {
            "name": "Glowy",
            "description": "High glow effect",
            "glow": 0.8,
            "emissive": 0.3,
            "saturation": 1.0
        },
        "subtle": {
            "name": "Subtle",
            "description": "Low visibility",
            "glow": 0.1,
            "emissive": 0.0,
            "saturation": 0.7
        },
        "neon": {
            "name": "Neon",
            "description": "Bright neon look",
            "glow": 1.0,
            "emissive": 0.5,
            "saturation": 1.2
        },
        "soft": {
            "name": "Soft",
            "description": "Soft diffused look",
            "glow": 0.4,
            "emissive": 0.1,
            "saturation": 0.8
        },
        "emissive_high": {
            "name": "Emissive High",
            "description": "Strong self-illumination",
            "glow": 0.5,
            "emissive": 0.8,
            "saturation": 1.0
        },
        "ghostly": {
            "name": "Ghostly",
            "description": "Faint ghostly appearance",
            "glow": 0.2,
            "emissive": 0.1,
            "saturation": 0.5,
            "alpha": 0.4
        },
    },
    
    # -------------------------------------------------------------------------
    # ORBITS (NEW FOLDER)
    # -------------------------------------------------------------------------
    "field_orbits": {
        "none": {
            "name": "None",
            "description": "No orbital motion",
            "enabled": False
        },
        "slow_y": {
            "name": "Slow Y Orbit",
            "description": "Slow orbit around Y axis",
            "enabled": True,
            "radius": 0.5,
            "speed": 15.0,
            "axis": "Y",
            "phase": 0.0
        },
        "medium_y": {
            "name": "Medium Y Orbit",
            "description": "Medium orbit around Y axis",
            "enabled": True,
            "radius": 0.5,
            "speed": 30.0,
            "axis": "Y",
            "phase": 0.0
        },
        "fast_y": {
            "name": "Fast Y Orbit",
            "description": "Fast orbit around Y axis",
            "enabled": True,
            "radius": 0.5,
            "speed": 60.0,
            "axis": "Y",
            "phase": 0.0
        },
        "slow_x": {
            "name": "Slow X Orbit",
            "description": "Slow orbit around X axis",
            "enabled": True,
            "radius": 0.5,
            "speed": 15.0,
            "axis": "X",
            "phase": 0.0
        },
        "tilted": {
            "name": "Tilted Orbit",
            "description": "Orbit on tilted axis",
            "enabled": True,
            "radius": 0.5,
            "speed": 20.0,
            "axis": "TILTED",
            "phase": 0.0
        },
    },
    
    # -------------------------------------------------------------------------
    # PREDICTIONS (NEW FOLDER)
    # -------------------------------------------------------------------------
    "field_predictions": {
        "disabled": {
            "name": "Disabled",
            "description": "No prediction",
            "enabled": False
        },
        "light": {
            "name": "Light",
            "description": "Minimal prediction (2 ticks)",
            "enabled": True,
            "leadTicks": 2,
            "maxDistance": 4.0,
            "lookAhead": 0.3,
            "verticalBoost": 0.0
        },
        "medium": {
            "name": "Medium",
            "description": "Moderate prediction (4 ticks)",
            "enabled": True,
            "leadTicks": 4,
            "maxDistance": 8.0,
            "lookAhead": 0.5,
            "verticalBoost": 0.0
        },
        "aggressive": {
            "name": "Aggressive",
            "description": "Strong prediction (6 ticks)",
            "enabled": True,
            "leadTicks": 6,
            "maxDistance": 12.0,
            "lookAhead": 0.7,
            "verticalBoost": 0.1
        },
    },
    
    # -------------------------------------------------------------------------
    # LINKS (NEW FOLDER)
    # -------------------------------------------------------------------------
    "field_links": {
        "none": {
            "name": "None",
            "description": "No linking",
            "primitiveId": None,
            "radiusMatch": False,
            "follow": False,
            "mirror": False
        },
        "radius_match": {
            "name": "Radius Match",
            "description": "Match linked primitive's radius",
            "radiusMatch": True,
            "radiusOffset": 0.0
        },
        "radius_offset": {
            "name": "Radius Offset",
            "description": "Offset from linked radius",
            "radiusMatch": True,
            "radiusOffset": 0.5
        },
        "mirror": {
            "name": "Mirror",
            "description": "Mirror linked primitive",
            "mirror": True
        },
        "phase_offset": {
            "name": "Phase Offset",
            "description": "Offset animation phase",
            "phaseOffset": 0.5
        },
    },
    
    # -------------------------------------------------------------------------
    # BEAMS (missing variants)
    # -------------------------------------------------------------------------
    "field_beams": {
        "short": {
            "name": "Short",
            "description": "Short beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.3,
            "height": 2.0,
            "glow": 0.5
        },
        "wide": {
            "name": "Wide",
            "description": "Wide beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 1.0,
            "height": 5.0,
            "glow": 0.4
        },
    },
}


def create_fragments(dry_run=False):
    """Create all missing fragment files."""
    created = 0
    skipped = 0
    
    for folder, fragments in FRAGMENTS.items():
        folder_path = CONFIG_ROOT / folder
        
        # Create folder if needed
        if not folder_path.exists():
            if dry_run:
                print(f"[DRY-RUN] Would create folder: {folder_path}")
            else:
                folder_path.mkdir(parents=True, exist_ok=True)
                print(f"Created folder: {folder_path}")
        
        for name, data in fragments.items():
            file_path = folder_path / f"{name}.json"
            
            if file_path.exists():
                skipped += 1
                print(f"  SKIP (exists): {file_path.name}")
                continue
            
            if dry_run:
                print(f"  [DRY-RUN] Would create: {file_path.name}")
            else:
                with open(file_path, 'w') as f:
                    json.dump(data, f, indent=2)
                print(f"  Created: {file_path.name}")
            
            created += 1
    
    print(f"\n{'=' * 50}")
    print(f"Summary: {created} created, {skipped} skipped (already exist)")
    if dry_run:
        print("(Dry run - no files were actually created)")


def main():
    parser = argparse.ArgumentParser(description="Create missing field fragments")
    parser.add_argument("--dry-run", action="store_true", 
                        help="Show what would be created without writing files")
    args = parser.parse_args()
    
    print("=" * 60)
    print("FIELD FRAGMENT CREATOR")
    print("=" * 60)
    
    # Count totals
    total = sum(len(frags) for frags in FRAGMENTS.values())
    folders = len(FRAGMENTS)
    new_folders = sum(1 for f in FRAGMENTS if not (CONFIG_ROOT / f).exists())
    
    print(f"\nWill process {total} fragments across {folders} folders")
    print(f"New folders to create: {new_folders}")
    print(f"Target: {CONFIG_ROOT}\n")
    
    if args.dry_run:
        print("*** DRY RUN MODE ***\n")
    
    create_fragments(dry_run=args.dry_run)


if __name__ == "__main__":
    main()

