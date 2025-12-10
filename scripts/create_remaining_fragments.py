#!/usr/bin/env python3
"""
Create remaining fragments: orbit, prediction, link, beam, follow mode
Based on 03_PARAMETERS.md
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")

FRAGMENTS = {
    # =========================================================================
    # ORBIT (Section 3.7)
    # =========================================================================
    "field_orbits": {
        "orbit_default": {
            "name": "Orbit Default",
            "description": "Standard circular orbit",
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "Y",
            "phase": 0.0
        },
        "orbit_fast": {
            "name": "Orbit Fast",
            "description": "Fast orbiting",
            "enabled": True,
            "radius": 1.5,
            "speed": 3.0,
            "axis": "Y",
            "phase": 0.0
        },
        "orbit_slow": {
            "name": "Orbit Slow",
            "description": "Slow elegant orbit",
            "enabled": True,
            "radius": 2.0,
            "speed": 0.3,
            "axis": "Y",
            "phase": 0.0
        },
        "orbit_wide": {
            "name": "Orbit Wide",
            "description": "Large orbit radius",
            "enabled": True,
            "radius": 3.0,
            "speed": 0.5,
            "axis": "Y",
            "phase": 0.0
        },
        "orbit_tight": {
            "name": "Orbit Tight",
            "description": "Small tight orbit",
            "enabled": True,
            "radius": 0.5,
            "speed": 2.0,
            "axis": "Y",
            "phase": 0.0
        },
        "orbit_x_axis": {
            "name": "Orbit X Axis",
            "description": "Orbit around X axis (vertical loop)",
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "X",
            "phase": 0.0
        },
        "orbit_z_axis": {
            "name": "Orbit Z Axis",
            "description": "Orbit around Z axis",
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "Z",
            "phase": 0.0
        },
        "orbit_offset_quarter": {
            "name": "Orbit Phase 90°",
            "description": "Quarter phase offset (for multi-layer)",
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "Y",
            "phase": 90.0
        },
        "orbit_offset_half": {
            "name": "Orbit Phase 180°",
            "description": "Half phase offset (opposite)",
            "enabled": True,
            "radius": 1.5,
            "speed": 1.0,
            "axis": "Y",
            "phase": 180.0
        },
    },
    
    # =========================================================================
    # PREDICTION (Section 3.5)
    # =========================================================================
    "field_predictions": {
        "prediction_default": {
            "name": "Prediction Default",
            "description": "Standard prediction settings",
            "enabled": True,
            "leadTicks": 3,
            "maxDistance": 5.0,
            "lookAhead": True,
            "verticalBoost": 1.0
        },
        "prediction_fast": {
            "name": "Prediction Fast",
            "description": "More aggressive prediction",
            "enabled": True,
            "leadTicks": 5,
            "maxDistance": 8.0,
            "lookAhead": True,
            "verticalBoost": 1.2
        },
        "prediction_subtle": {
            "name": "Prediction Subtle",
            "description": "Minimal prediction",
            "enabled": True,
            "leadTicks": 1,
            "maxDistance": 2.0,
            "lookAhead": False,
            "verticalBoost": 1.0
        },
        "prediction_snappy": {
            "name": "Prediction Snappy",
            "description": "Quick response",
            "enabled": True,
            "leadTicks": 2,
            "maxDistance": 3.0,
            "lookAhead": True,
            "verticalBoost": 1.5
        },
        "prediction_disabled": {
            "name": "Prediction Off",
            "description": "No prediction",
            "enabled": False,
            "leadTicks": 0,
            "maxDistance": 0.0,
            "lookAhead": False,
            "verticalBoost": 1.0
        },
    },
    
    # =========================================================================
    # LINK (Section 2.8 - primitive linking)
    # =========================================================================
    "field_links": {
        "link_default": {
            "name": "Link Default",
            "description": "No linking (independent)",
            "primitiveId": "",
            "radiusMatch": False,
            "radiusOffset": 0.0,
            "follow": False,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_match_radius": {
            "name": "Link Match Radius",
            "description": "Match parent primitive radius",
            "primitiveId": "parent",
            "radiusMatch": True,
            "radiusOffset": 0.0,
            "follow": False,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_offset_outer": {
            "name": "Link Outer Offset",
            "description": "Outside parent radius",
            "primitiveId": "parent",
            "radiusMatch": True,
            "radiusOffset": 0.3,
            "follow": False,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_offset_inner": {
            "name": "Link Inner Offset",
            "description": "Inside parent radius",
            "primitiveId": "parent",
            "radiusMatch": True,
            "radiusOffset": -0.2,
            "follow": False,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_follow": {
            "name": "Link Follow",
            "description": "Follow parent transform",
            "primitiveId": "parent",
            "radiusMatch": False,
            "radiusOffset": 0.0,
            "follow": True,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_mirror": {
            "name": "Link Mirror",
            "description": "Mirror parent animation",
            "primitiveId": "parent",
            "radiusMatch": False,
            "radiusOffset": 0.0,
            "follow": False,
            "mirror": True,
            "phaseOffset": 0.0,
            "scaleWith": False
        },
        "link_opposite_phase": {
            "name": "Link Opposite Phase",
            "description": "180° phase offset",
            "primitiveId": "parent",
            "radiusMatch": False,
            "radiusOffset": 0.0,
            "follow": False,
            "mirror": False,
            "phaseOffset": 180.0,
            "scaleWith": False
        },
        "link_scale_sync": {
            "name": "Link Scale Sync",
            "description": "Scale with parent",
            "primitiveId": "parent",
            "radiusMatch": False,
            "radiusOffset": 0.0,
            "follow": False,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": True
        },
        "link_full_sync": {
            "name": "Link Full Sync",
            "description": "Full parent synchronization",
            "primitiveId": "parent",
            "radiusMatch": True,
            "radiusOffset": 0.0,
            "follow": True,
            "mirror": False,
            "phaseOffset": 0.0,
            "scaleWith": True
        },
    },
    
    # =========================================================================
    # BEAM (Section 6 - Debug)
    # =========================================================================
    "field_beams": {
        "beam_default": {
            "name": "Beam Default",
            "description": "Standard beam configuration",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.5,
            "height": 3.0,
            "glow": 0.5,
            "color": "#00FF00",
            "pulseEnabled": False
        },
        "beam_thin": {
            "name": "Beam Thin",
            "description": "Thin laser beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.1,
            "height": 5.0,
            "glow": 0.8,
            "color": "#FF0000",
            "pulseEnabled": False
        },
        "beam_wide": {
            "name": "Beam Wide",
            "description": "Wide pillar beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 1.0,
            "height": 4.0,
            "glow": 0.3,
            "color": "#0088FF",
            "pulseEnabled": False
        },
        "beam_hollow": {
            "name": "Beam Hollow",
            "description": "Ring beam (hollow center)",
            "enabled": True,
            "innerRadius": 0.4,
            "outerRadius": 0.6,
            "height": 3.0,
            "glow": 0.5,
            "color": "#FFFF00",
            "pulseEnabled": False
        },
        "beam_pulsing": {
            "name": "Beam Pulsing",
            "description": "Pulsing glow beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.5,
            "height": 3.0,
            "glow": 0.7,
            "color": "#FF00FF",
            "pulseEnabled": True,
            "pulseScale": 0.3,
            "pulseSpeed": 2.0,
            "pulseWaveform": "SINE",
            "pulseMin": 0.3,
            "pulseMax": 1.0
        },
        "beam_scanner": {
            "name": "Beam Scanner",
            "description": "Scanner-style pulsing",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.3,
            "height": 2.0,
            "glow": 0.6,
            "color": "#00FFFF",
            "pulseEnabled": True,
            "pulseScale": 0.5,
            "pulseSpeed": 4.0,
            "pulseWaveform": "TRIANGLE",
            "pulseMin": 0.1,
            "pulseMax": 1.0
        },
        "beam_tall": {
            "name": "Beam Tall",
            "description": "Very tall beacon beam",
            "enabled": True,
            "innerRadius": 0.0,
            "outerRadius": 0.4,
            "height": 20.0,
            "glow": 0.4,
            "color": "#FFFFFF",
            "pulseEnabled": False
        },
    },
    
    # =========================================================================
    # FOLLOW MODE (Section 3.4)
    # =========================================================================
    "field_follows": {
        "follow_locked": {
            "name": "Follow Locked",
            "description": "Locked to entity (default)",
            "enabled": True,
            "mode": "LOCKED",
            "playerOverride": False
        },
        "follow_smooth": {
            "name": "Follow Smooth",
            "description": "Smooth interpolated following",
            "enabled": True,
            "mode": "SMOOTH",
            "playerOverride": False
        },
        "follow_delayed": {
            "name": "Follow Delayed",
            "description": "Delayed trailing effect",
            "enabled": True,
            "mode": "DELAYED",
            "playerOverride": False
        },
        "follow_ghost": {
            "name": "Follow Ghost",
            "description": "Ghost-like floating behavior",
            "enabled": True,
            "mode": "GHOST",
            "playerOverride": False
        },
        "follow_player_override": {
            "name": "Follow Player Override",
            "description": "Always follows player camera",
            "enabled": True,
            "mode": "SMOOTH",
            "playerOverride": True
        },
        "follow_disabled": {
            "name": "Follow Disabled",
            "description": "Static position",
            "enabled": False,
            "mode": "LOCKED",
            "playerOverride": False
        },
    },
}


def create_fragments():
    total_created = 0
    total_skipped = 0
    
    for category, fragments in FRAGMENTS.items():
        folder = CONFIG_ROOT / category
        folder.mkdir(parents=True, exist_ok=True)
        
        print("=" * 60)
        print(f"{category.upper()}")
        print("=" * 60)
        
        created = 0
        skipped = 0
        
        for name, data in fragments.items():
            filepath = folder / f"{name}.json"
            
            if filepath.exists():
                skipped += 1
                print(f"  SKIP: {name}.json")
                continue
            
            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2)
            print(f"  Created: {name}.json - {data['name']}")
            created += 1
        
        print(f"  -> Created: {created} | Skipped: {skipped}")
        total_created += created
        total_skipped += skipped
    
    print("=" * 60)
    print(f"TOTAL: Created {total_created} | Skipped {total_skipped}")
    print("=" * 60)


if __name__ == "__main__":
    create_fragments()

