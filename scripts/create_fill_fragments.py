#!/usr/bin/env python3
"""
Create fill mode fragments.
Based on 03_PARAMETERS.md Section 6: FILL Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
FILLS_DIR = CONFIG_ROOT / "field_fills"

FILL_FRAGMENTS = {
    "fill_solid": {
        "name": "Solid",
        "description": "Standard solid fill - default rendering",
        "mode": "SOLID",
        "doubleSided": False,
        "depthTest": True,
        "depthWrite": True
    },
    "fill_solid_doublesided": {
        "name": "Solid Double-Sided",
        "description": "Solid fill visible from both sides",
        "mode": "SOLID",
        "doubleSided": True,
        "depthTest": True,
        "depthWrite": True
    },
    "fill_wireframe": {
        "name": "Wireframe",
        "description": "All edges rendered as lines",
        "mode": "WIREFRAME",
        "wireThickness": 1.0,
        "doubleSided": True
    },
    "fill_wireframe_thick": {
        "name": "Wireframe Thick",
        "description": "Bold wireframe lines",
        "mode": "WIREFRAME",
        "wireThickness": 2.0,
        "doubleSided": True
    },
    "fill_wireframe_thin": {
        "name": "Wireframe Thin",
        "description": "Delicate thin wireframe",
        "mode": "WIREFRAME",
        "wireThickness": 0.5,
        "doubleSided": True
    },
    "fill_cage_default": {
        "name": "Cage Default",
        "description": "Globe-like latitude/longitude grid",
        "mode": "CAGE",
        "wireThickness": 1.0,
        "doubleSided": True,
        "cage": {
            "latitudeCount": 8,
            "longitudeCount": 16,
            "showEquator": True,
            "showPoles": True
        }
    },
    "fill_cage_sparse": {
        "name": "Cage Sparse",
        "description": "Minimal grid lines",
        "mode": "CAGE",
        "wireThickness": 1.0,
        "doubleSided": True,
        "cage": {
            "latitudeCount": 4,
            "longitudeCount": 8,
            "showEquator": True,
            "showPoles": False
        }
    },
    "fill_cage_dense": {
        "name": "Cage Dense",
        "description": "High density grid",
        "mode": "CAGE",
        "wireThickness": 0.5,
        "doubleSided": True,
        "cage": {
            "latitudeCount": 16,
            "longitudeCount": 32,
            "showEquator": True,
            "showPoles": True
        }
    },
    "fill_nodepth": {
        "name": "No Depth Test",
        "description": "Always visible through geometry",
        "mode": "SOLID",
        "doubleSided": True,
        "depthTest": False,
        "depthWrite": False
    },
}


def create_fragments():
    FILLS_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("FILL FRAGMENTS")
    print("=" * 60)
    
    for name, data in FILL_FRAGMENTS.items():
        filepath = FILLS_DIR / f"{name}.json"
        
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
    print("=" * 60)


if __name__ == "__main__":
    create_fragments()

