#!/usr/bin/env python3
"""
Create quad, sector, and edge arrangement fragments.
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
ARRANGEMENTS_DIR = CONFIG_ROOT / "field_arrangements"

# =============================================================================
# QUAD PATTERNS (spheres, prisms, polyhedra)
# =============================================================================

QUAD_FRAGMENTS = {
    "quad_all": {
        "name": "All Quads",
        "description": "All quads visible - complete surface",
        "cellType": "QUAD",
        "pattern": "full"
    },
    "quad_alternate": {
        "name": "Checkerboard",
        "description": "Alternating checkerboard pattern",
        "cellType": "QUAD",
        "pattern": "checkerboard"
    },
    "quad_stripes_h": {
        "name": "Horizontal Stripes",
        "description": "Horizontal stripe bands",
        "cellType": "QUAD",
        "pattern": "stripes",
        "direction": "horizontal",
        "count": 8
    },
    "quad_stripes_v": {
        "name": "Vertical Stripes",
        "description": "Vertical stripe bands",
        "cellType": "QUAD",
        "pattern": "stripes",
        "direction": "vertical",
        "count": 16
    },
    "quad_sparse": {
        "name": "Sparse",
        "description": "25% of quads visible - minimal",
        "cellType": "QUAD",
        "pattern": "sparse",
        "visibility": 0.25
    },
    "quad_dense": {
        "name": "Dense",
        "description": "75% of quads visible - nearly complete",
        "cellType": "QUAD",
        "pattern": "dense",
        "visibility": 0.75
    },
    "quad_diagonal": {
        "name": "Diagonal",
        "description": "Diagonal stripe pattern",
        "cellType": "QUAD",
        "pattern": "diagonal",
        "angle": 45
    },
    "quad_cross": {
        "name": "Cross",
        "description": "Cross/plus pattern on surface",
        "cellType": "QUAD",
        "pattern": "cross",
        "thickness": 0.2
    },
    "quad_dots": {
        "name": "Dots",
        "description": "Scattered dot pattern",
        "cellType": "QUAD",
        "pattern": "dots",
        "spacing": 4
    },
    "quad_gradient_h": {
        "name": "Gradient Horizontal",
        "description": "Fade from left to right",
        "cellType": "QUAD",
        "pattern": "gradient",
        "direction": "horizontal"
    },
    "quad_gradient_v": {
        "name": "Gradient Vertical",
        "description": "Fade from top to bottom",
        "cellType": "QUAD",
        "pattern": "gradient",
        "direction": "vertical"
    },
    "quad_triangle": {
        "name": "Triangle Split",
        "description": "Triangular subdivisions",
        "cellType": "QUAD",
        "pattern": "triangle",
        "subdivision": "diagonal"
    },
    "quad_wave": {
        "name": "Wave",
        "description": "Wavy stripe distribution",
        "cellType": "QUAD",
        "pattern": "wave",
        "frequency": 3,
        "amplitude": 0.3
    },
    "quad_random": {
        "name": "Random",
        "description": "Pseudo-random distribution",
        "cellType": "QUAD",
        "pattern": "random",
        "seed": 42,
        "density": 0.5
    },
    "quad_border": {
        "name": "Border Only",
        "description": "Only edge quads of surface",
        "cellType": "QUAD",
        "pattern": "border",
        "thickness": 1
    },
}

# =============================================================================
# SECTOR PATTERNS (discs)
# =============================================================================

SECTOR_FRAGMENTS = {
    "sector_all": {
        "name": "Full Disc",
        "description": "Complete disc - all sectors",
        "cellType": "SECTOR",
        "pattern": "full"
    },
    "sector_alternate": {
        "name": "Alternating",
        "description": "Alternating pie slices",
        "cellType": "SECTOR",
        "pattern": "alternating"
    },
    "sector_half": {
        "name": "Half",
        "description": "Semicircle - half disc",
        "cellType": "SECTOR",
        "pattern": "arc",
        "arcEnd": 0.5
    },
    "sector_quarters": {
        "name": "Quarters",
        "description": "Four pie pieces",
        "cellType": "SECTOR",
        "pattern": "uniform",
        "count": 4,
        "gapRatio": 0.5
    },
    "sector_sixths": {
        "name": "Sixths",
        "description": "Six pie pieces",
        "cellType": "SECTOR",
        "pattern": "uniform",
        "count": 6,
        "gapRatio": 0.4
    },
    "sector_eighths": {
        "name": "Eighths",
        "description": "Eight pie pieces",
        "cellType": "SECTOR",
        "pattern": "uniform",
        "count": 8,
        "gapRatio": 0.3
    },
    "sector_single": {
        "name": "Single Slice",
        "description": "One pie slice",
        "cellType": "SECTOR",
        "pattern": "arc",
        "arcStart": 0.0,
        "arcEnd": 0.125
    },
    "sector_opposing": {
        "name": "Opposing",
        "description": "Two opposite pie slices",
        "cellType": "SECTOR",
        "pattern": "uniform",
        "count": 2,
        "gapRatio": 0.6
    },
    "sector_pinwheel": {
        "name": "Pinwheel",
        "description": "Spinning pinwheel pattern",
        "cellType": "SECTOR",
        "pattern": "pinwheel",
        "blades": 6,
        "twist": 30
    },
    "sector_spiral": {
        "name": "Spiral",
        "description": "Spiral out from center",
        "cellType": "SECTOR",
        "pattern": "spiral",
        "turns": 2
    },
    "sector_rings": {
        "name": "Concentric Rings",
        "description": "Ring bands from center",
        "cellType": "SECTOR",
        "pattern": "rings",
        "count": 4
    },
    "sector_bullseye": {
        "name": "Bullseye",
        "description": "Center dot with ring",
        "cellType": "SECTOR",
        "pattern": "bullseye",
        "centerRadius": 0.2,
        "ringStart": 0.5,
        "ringEnd": 0.7
    },
    "sector_pacman": {
        "name": "Pac-Man",
        "description": "Disc with mouth cutout",
        "cellType": "SECTOR",
        "pattern": "arc",
        "arcStart": 0.1,
        "arcEnd": 0.9
    },
    "sector_fade": {
        "name": "Radial Fade",
        "description": "Fade from center outward",
        "cellType": "SECTOR",
        "pattern": "radialGradient",
        "centerVisibility": 1.0,
        "edgeVisibility": 0.0
    },
}

# =============================================================================
# EDGE PATTERNS (cages/wireframes)
# =============================================================================

EDGE_FRAGMENTS = {
    "edge_all": {
        "name": "All Edges",
        "description": "All edges visible - full wireframe",
        "cellType": "EDGE",
        "pattern": "full"
    },
    "edge_sparse": {
        "name": "Sparse",
        "description": "Reduced edge density",
        "cellType": "EDGE",
        "pattern": "sparse",
        "visibility": 0.3
    },
    "edge_dense": {
        "name": "Dense",
        "description": "High edge density",
        "cellType": "EDGE",
        "pattern": "dense",
        "visibility": 0.8
    },
    "edge_latitude": {
        "name": "Latitude Only",
        "description": "Horizontal lines only",
        "cellType": "EDGE",
        "pattern": "latitude",
        "showLongitude": False
    },
    "edge_longitude": {
        "name": "Longitude Only",
        "description": "Vertical lines only",
        "cellType": "EDGE",
        "pattern": "longitude",
        "showLatitude": False
    },
    "edge_diagonal": {
        "name": "Diagonal Grid",
        "description": "Diagonal edge pattern",
        "cellType": "EDGE",
        "pattern": "diagonal",
        "angle": 45
    },
    "edge_dashed": {
        "name": "Dashed",
        "description": "Dashed line edges",
        "cellType": "EDGE",
        "pattern": "dashed",
        "dashLength": 0.1,
        "gapLength": 0.05
    },
    "edge_corners": {
        "name": "Corners Only",
        "description": "Only corner/vertex edges",
        "cellType": "EDGE",
        "pattern": "corners"
    },
    "edge_minimal": {
        "name": "Minimal",
        "description": "Bare minimum structure",
        "cellType": "EDGE",
        "pattern": "minimal",
        "latitudeCount": 3,
        "longitudeCount": 6
    },
}


def create_fragments():
    """Create all arrangement fragments."""
    ARRANGEMENTS_DIR.mkdir(parents=True, exist_ok=True)
    
    all_fragments = {
        **QUAD_FRAGMENTS,
        **SECTOR_FRAGMENTS,
        **EDGE_FRAGMENTS,
    }
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("ARRANGEMENT FRAGMENTS")
    print("=" * 60)
    
    for name, data in all_fragments.items():
        filepath = ARRANGEMENTS_DIR / f"{name}.json"
        
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
    print(f"  Quad: {len(QUAD_FRAGMENTS)} | Sector: {len(SECTOR_FRAGMENTS)} | Edge: {len(EDGE_FRAGMENTS)}")
    print("=" * 60)


if __name__ == "__main__":
    create_fragments()

