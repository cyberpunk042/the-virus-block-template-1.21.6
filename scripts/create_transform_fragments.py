#!/usr/bin/env python3
"""
Create transform fragments.
Based on 03_PARAMETERS.md Section 5: TRANSFORM Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
TRANSFORMS_DIR = CONFIG_ROOT / "field_transforms"

TRANSFORM_FRAGMENTS = {
    # === Basic anchors ===
    "transform_default": {
        "name": "Default",
        "description": "Center anchor, no modifications",
        "anchor": "CENTER",
        "offset": [0, 0, 0],
        "rotation": [0, 0, 0],
        "scale": 1.0
    },
    "transform_elevated": {
        "name": "Elevated",
        "description": "Raised above center",
        "anchor": "CENTER",
        "offset": [0, 1.0, 0],
        "scale": 1.0
    },
    "transform_ground": {
        "name": "Ground Level",
        "description": "At feet level",
        "anchor": "FEET",
        "offset": [0, 0.1, 0],
        "scale": 1.0
    },
    "transform_head": {
        "name": "Head Level",
        "description": "At head height",
        "anchor": "HEAD",
        "offset": [0, 0, 0],
        "scale": 1.0
    },
    "transform_above_head": {
        "name": "Above Head",
        "description": "Floating above head",
        "anchor": "HEAD",
        "offset": [0, 0.5, 0],
        "scale": 1.0
    },
    
    # === Rotations ===
    "transform_tilted_x": {
        "name": "Tilted X",
        "description": "30° X-axis tilt",
        "anchor": "CENTER",
        "rotation": [30, 0, 0],
        "scale": 1.0
    },
    "transform_tilted_z": {
        "name": "Tilted Z",
        "description": "30° Z-axis tilt",
        "anchor": "CENTER",
        "rotation": [0, 0, 30],
        "scale": 1.0
    },
    "transform_rotated_45": {
        "name": "Rotated 45°",
        "description": "45° Y-axis rotation",
        "anchor": "CENTER",
        "rotation": [0, 45, 0],
        "scale": 1.0
    },
    
    # === Scale ===
    "transform_scaled_small": {
        "name": "Small",
        "description": "50% scale",
        "anchor": "CENTER",
        "scale": 0.5
    },
    "transform_scaled_large": {
        "name": "Large",
        "description": "150% scale",
        "anchor": "CENTER",
        "scale": 1.5
    },
    "transform_scaled_tiny": {
        "name": "Tiny",
        "description": "25% scale",
        "anchor": "CENTER",
        "scale": 0.25
    },
    "transform_scaled_huge": {
        "name": "Huge",
        "description": "200% scale",
        "anchor": "CENTER",
        "scale": 2.0
    },
    
    # === Billboard/Facing ===
    "transform_billboard": {
        "name": "Billboard",
        "description": "Always faces camera",
        "anchor": "CENTER",
        "facing": "CAMERA",
        "billboard": "FULL"
    },
    "transform_billboard_y": {
        "name": "Billboard Y",
        "description": "Faces camera on Y axis only",
        "anchor": "CENTER",
        "facing": "CAMERA",
        "billboard": "Y_AXIS"
    },
    "transform_player_look": {
        "name": "Player Look",
        "description": "Faces player look direction",
        "anchor": "CENTER",
        "facing": "PLAYER_LOOK"
    },
    
    # === Orbit ===
    "transform_orbit_slow": {
        "name": "Orbit Slow",
        "description": "Slow orbital motion",
        "anchor": "CENTER",
        "orbit": {
            "enabled": True,
            "radius": 1.0,
            "speed": 0.5,
            "axis": "Y",
            "phase": 0.0
        }
    },
    "transform_orbit_medium": {
        "name": "Orbit Medium",
        "description": "Medium orbital motion",
        "anchor": "CENTER",
        "orbit": {
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "Y",
            "phase": 0.0
        }
    },
    "transform_orbit_fast": {
        "name": "Orbit Fast",
        "description": "Fast orbital motion",
        "anchor": "CENTER",
        "orbit": {
            "enabled": True,
            "radius": 1.0,
            "speed": 2.0,
            "axis": "Y",
            "phase": 0.0
        }
    },
    "transform_orbit_wide": {
        "name": "Orbit Wide",
        "description": "Wide radius orbit",
        "anchor": "CENTER",
        "orbit": {
            "enabled": True,
            "radius": 3.0,
            "speed": 0.5,
            "axis": "Y",
            "phase": 0.0
        }
    },
}


def create_fragments():
    TRANSFORMS_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("TRANSFORM FRAGMENTS")
    print("=" * 60)
    
    for name, data in TRANSFORM_FRAGMENTS.items():
        filepath = TRANSFORMS_DIR / f"{name}.json"
        
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

