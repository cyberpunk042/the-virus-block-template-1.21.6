#!/usr/bin/env python3
"""
Create animation fragments.
Based on 03_PARAMETERS.md Section 10: ANIMATION Level
"""

import json
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
ANIMATIONS_DIR = CONFIG_ROOT / "field_animations"

ANIMATION_FRAGMENTS = {
    # === None ===
    "anim_none": {
        "name": "None",
        "description": "No animation - static",
        "spin": None,
        "pulse": None,
        "phase": 0.0
    },
    
    # === Spin Y-axis ===
    "anim_spin_slow": {
        "name": "Spin Slow",
        "description": "Slow Y-axis rotation",
        "spin": {
            "axis": "Y",
            "speed": 15.0,
            "oscillate": False
        }
    },
    "anim_spin_medium": {
        "name": "Spin Medium",
        "description": "Medium Y-axis rotation",
        "spin": {
            "axis": "Y",
            "speed": 45.0,
            "oscillate": False
        }
    },
    "anim_spin_fast": {
        "name": "Spin Fast",
        "description": "Fast Y-axis rotation",
        "spin": {
            "axis": "Y",
            "speed": 90.0,
            "oscillate": False
        }
    },
    
    # === Spin other axes ===
    "anim_spin_x": {
        "name": "Spin X",
        "description": "X-axis rotation (tumble)",
        "spin": {
            "axis": "X",
            "speed": 30.0,
            "oscillate": False
        }
    },
    "anim_spin_z": {
        "name": "Spin Z",
        "description": "Z-axis rotation (roll)",
        "spin": {
            "axis": "Z",
            "speed": 30.0,
            "oscillate": False
        }
    },
    "anim_spin_oscillate": {
        "name": "Oscillate",
        "description": "Back-and-forth rotation",
        "spin": {
            "axis": "Y",
            "speed": 45.0,
            "oscillate": True,
            "range": 90.0
        }
    },
    
    # === Pulse (scale) ===
    "anim_pulse_gentle": {
        "name": "Pulse Gentle",
        "description": "Gentle scale breathing",
        "pulse": {
            "scale": 1.0,
            "speed": 1.0,
            "waveform": "SINE",
            "min": 0.95,
            "max": 1.05
        }
    },
    "anim_pulse_medium": {
        "name": "Pulse Medium",
        "description": "Medium scale pulse",
        "pulse": {
            "scale": 1.0,
            "speed": 1.0,
            "waveform": "SINE",
            "min": 0.9,
            "max": 1.1
        }
    },
    "anim_pulse_strong": {
        "name": "Pulse Strong",
        "description": "Strong scale pulse",
        "pulse": {
            "scale": 1.0,
            "speed": 1.5,
            "waveform": "SINE",
            "min": 0.8,
            "max": 1.2
        }
    },
    "anim_pulse_square": {
        "name": "Pulse Square",
        "description": "Sharp on/off pulse",
        "pulse": {
            "scale": 1.0,
            "speed": 2.0,
            "waveform": "SQUARE",
            "min": 0.9,
            "max": 1.1
        }
    },
    
    # === Alpha pulse ===
    "anim_alpha_fade": {
        "name": "Alpha Fade",
        "description": "Breathing alpha animation",
        "alphaPulse": {
            "speed": 1.0,
            "min": 0.4,
            "max": 1.0,
            "waveform": "SINE"
        }
    },
    "anim_alpha_flicker": {
        "name": "Alpha Flicker",
        "description": "Quick alpha flicker",
        "alphaPulse": {
            "speed": 3.0,
            "min": 0.6,
            "max": 1.0,
            "waveform": "SAWTOOTH"
        }
    },
    
    # === Wobble ===
    "anim_wobble_soft": {
        "name": "Wobble Soft",
        "description": "Gentle random wobble",
        "wobble": {
            "amplitude": [0.05, 0.03, 0.05],
            "speed": 1.0,
            "randomize": True
        }
    },
    "anim_wobble_strong": {
        "name": "Wobble Strong",
        "description": "Pronounced wobble",
        "wobble": {
            "amplitude": [0.15, 0.1, 0.15],
            "speed": 1.5,
            "randomize": True
        }
    },
    
    # === Color cycle ===
    "anim_color_cycle": {
        "name": "Color Cycle",
        "description": "Smooth color cycling",
        "colorCycle": {
            "colors": ["#FF0000", "#00FF00", "#0000FF"],
            "speed": 1.0,
            "blend": True
        }
    },
    "anim_color_rainbow": {
        "name": "Rainbow",
        "description": "Full rainbow cycle",
        "colorCycle": {
            "colors": ["#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#9400D3"],
            "speed": 0.5,
            "blend": True
        }
    },
    
    # === Combined ===
    "anim_combined_gentle": {
        "name": "Spin + Pulse",
        "description": "Gentle spin with pulse",
        "spin": {
            "axis": "Y",
            "speed": 20.0
        },
        "pulse": {
            "speed": 0.8,
            "min": 0.95,
            "max": 1.05,
            "waveform": "SINE"
        }
    },
    "anim_combined_active": {
        "name": "Active",
        "description": "Spin + pulse + alpha fade",
        "spin": {
            "axis": "Y",
            "speed": 30.0
        },
        "pulse": {
            "speed": 1.0,
            "min": 0.9,
            "max": 1.1,
            "waveform": "SINE"
        },
        "alphaPulse": {
            "speed": 0.5,
            "min": 0.7,
            "max": 1.0,
            "waveform": "SINE"
        }
    },
}


def create_fragments():
    ANIMATIONS_DIR.mkdir(parents=True, exist_ok=True)
    
    created = 0
    skipped = 0
    
    print("=" * 60)
    print("ANIMATION FRAGMENTS")
    print("=" * 60)
    
    for name, data in ANIMATION_FRAGMENTS.items():
        filepath = ANIMATIONS_DIR / f"{name}.json"
        
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

