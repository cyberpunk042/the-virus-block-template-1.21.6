#!/usr/bin/env python3
"""
Create appearance fragments.
Based on 03_PARAMETERS.md Section 9: APPEARANCE Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
APPEARANCES_DIR = CONFIG_ROOT / "field_appearances"

APPEARANCE_FRAGMENTS = {
    # === Basic ===
    "appear_default": {
        "name": "Default",
        "description": "Standard appearance",
        "color": "@primary",
        "alpha": 1.0,
        "glow": 0.0,
        "emissive": 0.0,
        "saturation": 1.0,
        "brightness": 1.0
    },
    
    # === Glow variations ===
    "appear_glowy": {
        "name": "Glowy",
        "description": "High glow effect",
        "color": "@primary",
        "alpha": 0.9,
        "glow": 0.8,
        "emissive": 0.3
    },
    "appear_neon": {
        "name": "Neon",
        "description": "Bright neon look",
        "color": "@primary",
        "alpha": 1.0,
        "glow": 1.0,
        "emissive": 0.5,
        "saturation": 1.3,
        "brightness": 1.2
    },
    "appear_soft_glow": {
        "name": "Soft Glow",
        "description": "Gentle glow effect",
        "color": "@primary",
        "alpha": 0.85,
        "glow": 0.4,
        "emissive": 0.1
    },
    
    # === Transparency variations ===
    "appear_subtle": {
        "name": "Subtle",
        "description": "Low visibility, faint",
        "color": "@primary",
        "alpha": 0.4,
        "glow": 0.1,
        "saturation": 0.7
    },
    "appear_ghostly": {
        "name": "Ghostly",
        "description": "Faint, ethereal look",
        "color": "@primary",
        "alpha": 0.3,
        "glow": 0.2,
        "emissive": 0.1,
        "saturation": 0.5
    },
    "appear_transparent": {
        "name": "Transparent",
        "description": "Highly see-through",
        "color": "@primary",
        "alpha": 0.2,
        "glow": 0.0
    },
    
    # === Emissive variations ===
    "appear_emissive": {
        "name": "Emissive",
        "description": "Strong self-illumination",
        "color": "@primary",
        "alpha": 1.0,
        "glow": 0.5,
        "emissive": 0.8
    },
    "appear_radiant": {
        "name": "Radiant",
        "description": "Maximum emission and glow",
        "color": "@primary",
        "alpha": 1.0,
        "glow": 1.0,
        "emissive": 1.0,
        "brightness": 1.3
    },
    
    # === Saturation variations ===
    "appear_saturated": {
        "name": "Saturated",
        "description": "Vibrant, intense colors",
        "color": "@primary",
        "alpha": 1.0,
        "saturation": 1.5
    },
    "appear_desaturated": {
        "name": "Desaturated",
        "description": "Muted, washed-out colors",
        "color": "@primary",
        "alpha": 1.0,
        "saturation": 0.3
    },
    "appear_grayscale": {
        "name": "Grayscale",
        "description": "No color saturation",
        "color": "@primary",
        "alpha": 1.0,
        "saturation": 0.0
    },
    
    # === Brightness variations ===
    "appear_bright": {
        "name": "Bright",
        "description": "High brightness",
        "color": "@primary",
        "alpha": 1.0,
        "brightness": 1.5
    },
    "appear_dark": {
        "name": "Dark",
        "description": "Low brightness",
        "color": "@primary",
        "alpha": 1.0,
        "brightness": 0.5
    },
    
    # === Combined presets ===
    "appear_hologram": {
        "name": "Hologram",
        "description": "Sci-fi holographic look",
        "color": "@primary",
        "alpha": 0.6,
        "glow": 0.7,
        "emissive": 0.4,
        "saturation": 0.8
    },
    "appear_energy": {
        "name": "Energy",
        "description": "Energized, powerful look",
        "color": "@primary",
        "alpha": 0.9,
        "glow": 0.9,
        "emissive": 0.6,
        "saturation": 1.2,
        "brightness": 1.1
    },
}


def create_fragments():
    APPEARANCES_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("APPEARANCE FRAGMENTS")
    print("=" * 60)
    
    for name, data in APPEARANCE_FRAGMENTS.items():
        filepath = APPEARANCES_DIR / f"{name}.json"
        
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

