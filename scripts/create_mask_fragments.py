#!/usr/bin/env python3
"""
Create visibility mask fragments.
Based on 03_PARAMETERS.md Section 7: VISIBILITY MASK Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
MASKS_DIR = CONFIG_ROOT / "field_masks"

MASK_FRAGMENTS = {
    # === Basic ===
    "mask_full": {
        "name": "Full",
        "description": "No masking - complete visibility",
        "mask": "FULL"
    },
    
    # === Bands (horizontal) ===
    "mask_bands_4": {
        "name": "4 Bands",
        "description": "4 horizontal bands",
        "mask": "BANDS",
        "count": 4,
        "thickness": 0.5
    },
    "mask_bands_8": {
        "name": "8 Bands",
        "description": "8 horizontal bands",
        "mask": "BANDS",
        "count": 8,
        "thickness": 0.5
    },
    "mask_bands_thin": {
        "name": "Thin Bands",
        "description": "Narrow horizontal bands",
        "mask": "BANDS",
        "count": 6,
        "thickness": 0.3
    },
    
    # === Stripes (vertical) ===
    "mask_stripes_8": {
        "name": "8 Stripes",
        "description": "8 vertical stripes",
        "mask": "STRIPES",
        "count": 8,
        "thickness": 0.5
    },
    "mask_stripes_16": {
        "name": "16 Stripes",
        "description": "16 vertical stripes",
        "mask": "STRIPES",
        "count": 16,
        "thickness": 0.5
    },
    "mask_stripes_thin": {
        "name": "Thin Stripes",
        "description": "Narrow vertical stripes",
        "mask": "STRIPES",
        "count": 12,
        "thickness": 0.3
    },
    
    # === Checker ===
    "mask_checker": {
        "name": "Checkerboard",
        "description": "Alternating checkerboard pattern",
        "mask": "CHECKER",
        "count": 8,
        "thickness": 0.5
    },
    "mask_checker_fine": {
        "name": "Fine Checker",
        "description": "Small checkerboard squares",
        "mask": "CHECKER",
        "count": 16,
        "thickness": 0.5
    },
    
    # === Radial ===
    "mask_radial_4": {
        "name": "4 Radial",
        "description": "4 radial segments",
        "mask": "RADIAL",
        "count": 4,
        "thickness": 0.5,
        "centerX": 0.5,
        "centerY": 0.5
    },
    "mask_radial_8": {
        "name": "8 Radial",
        "description": "8 radial segments",
        "mask": "RADIAL",
        "count": 8,
        "thickness": 0.5,
        "centerX": 0.5,
        "centerY": 0.5
    },
    
    # === Gradient ===
    "mask_gradient_top": {
        "name": "Fade Top",
        "description": "Fade in from top",
        "mask": "GRADIENT",
        "direction": "VERTICAL",
        "gradientStart": 0.0,
        "gradientEnd": 1.0,
        "falloff": 1.0
    },
    "mask_gradient_bottom": {
        "name": "Fade Bottom",
        "description": "Fade in from bottom",
        "mask": "GRADIENT",
        "direction": "VERTICAL",
        "gradientStart": 1.0,
        "gradientEnd": 0.0,
        "falloff": 1.0
    },
    
    # === Half ===
    "mask_half_top": {
        "name": "Top Half",
        "description": "Only top half visible",
        "mask": "GRADIENT",
        "direction": "VERTICAL",
        "gradientStart": 0.0,
        "gradientEnd": 0.5
    },
    "mask_half_bottom": {
        "name": "Bottom Half",
        "description": "Only bottom half visible",
        "mask": "GRADIENT",
        "direction": "VERTICAL",
        "gradientStart": 0.5,
        "gradientEnd": 1.0
    },
    
    # === Animated ===
    "mask_animated_bands": {
        "name": "Scrolling Bands",
        "description": "Animated scrolling horizontal bands",
        "mask": "BANDS",
        "count": 6,
        "thickness": 0.5,
        "animate": True,
        "animSpeed": 1.0
    },
    "mask_animated_stripes": {
        "name": "Rotating Stripes",
        "description": "Animated rotating vertical stripes",
        "mask": "STRIPES",
        "count": 8,
        "thickness": 0.5,
        "animate": True,
        "animSpeed": 0.5
    },
    
    # === Inverted ===
    "mask_inverted_bands": {
        "name": "Inverted Bands",
        "description": "Inverted band pattern",
        "mask": "BANDS",
        "count": 4,
        "thickness": 0.5,
        "invert": True
    },
    
    # === Feathered ===
    "mask_feathered_bands": {
        "name": "Soft Bands",
        "description": "Bands with soft edges",
        "mask": "BANDS",
        "count": 4,
        "thickness": 0.6,
        "feather": 0.3
    },
}


def create_fragments():
    MASKS_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("MASK FRAGMENTS")
    print("=" * 60)
    
    for name, data in MASK_FRAGMENTS.items():
        filepath = MASKS_DIR / f"{name}.json"
        
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

