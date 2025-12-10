#!/usr/bin/env python3
"""
Update 03_PARAMETERS.md with correct implementation statuses.
Based on actual code implementations completed.
"""

import re
from pathlib import Path

DOC_PATH = Path("docs/field-system/NEW_REFACTORING_NEW_PHASES/03_PARAMETERS.md")

# Define updates: (pattern_to_find, replacement)
# Format: old_line_content -> new_line_content
UPDATES = [
    # =========================================================================
    # MODIFIERS (Section 1)
    # =========================================================================
    ("| `bobbing` | float | 0.0 | ❌ | Vertical bob animation |",
     "| `bobbing` | float | 0.0 | ✅ | Vertical bob animation |"),
    
    ("| `breathing` | float | 0.0 | ❌ | Scale breathing effect |",
     "| `breathing` | float | 0.0 | ✅ | Scale breathing effect |"),
    
    # =========================================================================
    # BEAM (Section 1)
    # =========================================================================
    ("| `height` | float | auto | ❌ | Beam height (currently auto) |",
     "| `height` | float | 3.0 | ✅ | Beam height |"),
    
    ("| `glow` | float | 0.5 | ❌ | Beam glow intensity |",
     "| `glow` | float | 0.5 | ✅ | Beam glow intensity |"),
    
    ("| `pulse` | float | 0.0 | ❌ | Beam pulse animation |",
     "| `pulse` | object | null | ✅ | Beam pulse config |"),
    
    # =========================================================================
    # FOLLOW MODE (Section 1)
    # =========================================================================
    ("| `followMode.playerOverride` | boolean | true | ❌ | Player can change in GUI |",
     "| `followMode.playerOverride` | boolean | true | ✅ | Player can change in GUI |"),
    
    # =========================================================================
    # LAYER (Section 2)
    # =========================================================================
    ("| `rotation` | Vec3 | (0,0,0) | ❌ | Static rotation (for mirror layers) |",
     "| `rotation` | Vec3 | (0,0,0) | ✅ | Static rotation (via Transform) |"),
    
    ("| `order` | int | auto | ❌ | Render order |",
     "| `order` | int | auto | ✅ | Render order |"),
    
    # =========================================================================
    # RING SHAPE (Section 4.2)
    # =========================================================================
    ("| `arcStart` | float | 0.0 | 0-360 | ❌ | Arc start angle (degrees) |",
     "| `arcStart` | float | 0.0 | 0-360 | ✅ | Arc start angle (degrees) |"),
    
    ("| `arcEnd` | float | 360.0 | 0-360 | ❌ | Arc end angle |",
     "| `arcEnd` | float | 360.0 | 0-360 | ✅ | Arc end angle |"),
    
    ("| `height` | float | 0.0 | 0-∞ | ❌ | Ring height (3D ring) |",
     "| `height` | float | 0.0 | 0-∞ | ✅ | Ring height (3D ring) |"),
    
    ("| `twist` | float | 0.0 | -∞-∞ | ❌ | Twist along arc |",
     "| `twist` | float | 0.0 | -∞-∞ | ✅ | Twist along arc |"),
    
    # =========================================================================
    # DISC SHAPE (Section 4.3)
    # =========================================================================
    ("| `arcStart` | float | 0.0 | 0-360 | ❌ | Arc start (pac-man) |",
     "| `arcStart` | float | 0.0 | 0-360 | ✅ | Arc start (pac-man) |"),
    
    ("| `arcEnd` | float | 360.0 | 0-360 | ❌ | Arc end |",
     "| `arcEnd` | float | 360.0 | 0-360 | ✅ | Arc end |"),
    
    ("| `innerRadius` | float | 0.0 | 0-∞ | ❌ | Inner cutout (makes ring-like) |",
     "| `innerRadius` | float | 0.0 | 0-∞ | ✅ | Inner cutout (makes ring-like) |"),
    
    ("| `rings` | int | 1 | 1-100 | ❌ | Concentric ring divisions |",
     "| `rings` | int | 1 | 1-100 | ✅ | Concentric ring divisions |"),
    
    # =========================================================================
    # PRISM SHAPE (Section 4.4)
    # =========================================================================
    ("| `topRadius` | float | same | 0-∞ | ❌ | Top radius (for tapered) |",
     "| `topRadius` | float | same | 0-∞ | ✅ | Top radius (for tapered) |"),
    
    ("| `twist` | float | 0.0 | -360-360 | ❌ | Twist along height |",
     "| `twist` | float | 0.0 | -360-360 | ✅ | Twist along height |"),
    
    ("| `heightSegments` | int | 1 | 1-100 | ❌ | Vertical divisions |",
     "| `heightSegments` | int | 1 | 1-100 | ✅ | Vertical divisions |"),
    
    ("| `capTop` | boolean | true | - | ❌ | Render top cap |",
     "| `capTop` | boolean | true | - | ✅ | Render top cap |"),
    
    ("| `capBottom` | boolean | true | - | ❌ | Render bottom cap |",
     "| `capBottom` | boolean | true | - | ✅ | Render bottom cap |"),
    
    # =========================================================================
    # POLYHEDRON SHAPE (Section 4.5)
    # =========================================================================
    ("| `subdivisions` | int | 0 | 0-5 | ❌ | Subdivision level |",
     "| `subdivisions` | int | 0 | 0-5 | ✅ | Subdivision level |"),
    
    # =========================================================================
    # CYLINDER SHAPE (Section 4.6)
    # =========================================================================
    ("| `topRadius` | float | same | 0-∞ | ❌ | Top radius (cone-like) |",
     "| `topRadius` | float | same | 0-∞ | ✅ | Top radius (cone-like) |"),
    
    ("| `heightSegments` | int | 1 | 1-100 | ❌ | Height divisions |",
     "| `heightSegments` | int | 1 | 1-100 | ✅ | Height divisions |"),
    
    ("| `capTop` | boolean | true | - | ❌ | Render top cap |",
     "| `capTop` | boolean | true | - | ✅ | Render top cap |"),
    
    ("| `capBottom` | boolean | false | - | ❌ | Render bottom cap |",
     "| `capBottom` | boolean | false | - | ✅ | Render bottom cap |"),
    
    ("| `openEnded` | boolean | true | - | ❌ | No caps (tube) |",
     "| `openEnded` | boolean | true | - | ✅ | No caps (tube) |"),
    
    ("| `arc` | float | 360 | 0-360 | ❌ | Partial cylinder |",
     "| `arc` | float | 360 | 0-360 | ✅ | Partial cylinder |"),
    
    # =========================================================================
    # SPHERE SHAPE (Section 4.1)
    # =========================================================================
    ("| `subdivisions` | int | 0 | 0-5 | ❌ | Icosphere subdivisions (for TYPE_E) |",
     "| `subdivisions` | int | 0 | 0-5 | ✅ | Icosphere subdivisions (for TYPE_E) |"),
    
    # =========================================================================
    # FILL (Section 6)
    # =========================================================================
    ("| `pointSize` | float | 2.0 | ❌ | Point size |",
     "| `pointSize` | float | 2.0 | ✅ | Point size |"),
    
    # =========================================================================
    # VISIBILITY RADIAL (Section 7)
    # =========================================================================
    ("| `centerX` | float | 0.5 | ❌ 📎 | Center X (0-1) |",
     "| `centerX` | float | 0.5 | ✅ | Center X (0-1) |"),
    
    ("| `centerY` | float | 0.5 | ❌ 📎 | Center Y (0-1) |",
     "| `centerY` | float | 0.5 | ✅ | Center Y (0-1) |"),
    
    # =========================================================================
    # ARRANGEMENT (Section 8)
    # =========================================================================
    ("| `arrangement.default` | string | \"filled_1\" | ❌ | Default pattern for all parts |",
     "| `arrangement.default` | string | \"filled_1\" | ✅ | Default pattern for all parts |"),
    
    ("| `arrangement.caps` | string | null | ❌ | Pattern for cap surfaces |",
     "| `arrangement.caps` | string | null | ✅ | Pattern for cap surfaces |"),
    
    ("| `arrangement.sides` | string | null | ❌ | Pattern for side surfaces |",
     "| `arrangement.sides` | string | null | ✅ | Pattern for side surfaces |"),
    
    ("| `arrangement.edges` | string | null | ❌ | Pattern for edge lines |",
     "| `arrangement.edges` | string | null | ✅ | Pattern for edge lines |"),
    
    ("| `arrangement.poles` | string | null | ❌ | Pattern for sphere poles |",
     "| `arrangement.poles` | string | null | ✅ | Pattern for sphere poles |"),
    
    ("| `arrangement.equator` | string | null | ❌ | Pattern for sphere equator |",
     "| `arrangement.equator` | string | null | ✅ | Pattern for sphere equator |"),
    
    # =========================================================================
    # ANIMATION (Section 10)
    # =========================================================================
    ("| `colorCycle` | object | null | ❌ | Color animation |",
     "| `colorCycle` | object | null | ✅ | Color animation |"),
    
    ("| `wobble` | object | null | ❌ | Random movement |",
     "| `wobble` | object | null | ✅ | Random movement |"),
    
    ("| `wave` | object | null | ❌ | Wave deformation |",
     "| `wave` | object | null | ✅ | Wave deformation |"),
    
    # Color Cycle Config section - change from FUTURE to implemented
    ("### Color Cycle Config (FUTURE)",
     "### Color Cycle Config"),
    
    ("| `colors` | List<string> | [] | ❌ | Colors to cycle through |",
     "| `colors` | List<string> | [] | ✅ | Colors to cycle through |"),
    
    ("| `speed` | float | 1.0 | ❌ | Cycle speed |",
     "| `speed` | float | 1.0 | ✅ | Cycle speed |"),
    
    ("| `blend` | boolean | true | ❌ | Smooth blend vs instant |",
     "| `blend` | boolean | true | ✅ | Smooth blend vs instant |"),
    
    # Wobble Config section - change from FUTURE to implemented
    ("### Wobble Config (FUTURE)",
     "### Wobble Config"),
    
    ("| `amplitude` | Vec3 | (0.1,0.1,0.1) | ❌ | Wobble amount per axis |",
     "| `amplitude` | Vec3 | (0.1,0.1,0.1) | ✅ | Wobble amount per axis |"),
    
    ("| `speed` | float | 1.0 | ❌ | Wobble speed |",
     "| `speed` | float | 1.0 | ✅ | Wobble speed |"),
    
    ("| `randomize` | boolean | true | ❌ | Randomize movement |",
     "| `randomize` | boolean | true | ✅ | Randomize movement |"),
    
    # =========================================================================
    # PRIMITIVE LINKING (Section 11)
    # =========================================================================
    ("| `id` | string | null | ❌ | Primitive identifier for linking |",
     "| `id` | string | required | ✅ | Primitive identifier for linking |"),
]

# Update the summary table at the end
SUMMARY_UPDATE = """## 13. Summary: Missing Parameters Count

| Level | Implemented | Missing | Future |
|-------|-------------|---------|--------|
| Field Definition | 21 | 0 | 0 |
| Layer | 12 | 0 | 0 |
| Transform | 18 | 0 | 0 |
| Fill | 10 | 0 | 1 |
| Visibility | 14 | 0 | 0 |
| Arrangement | 10 | 0 | 0 |
| Appearance | 9 | 0 | 0 |
| Animation | 19 | 0 | 0 |
| Primitive Linking | 7 | 0 | 0 |
| **Shapes** | | | |
| - Sphere | 9 | 0 | 1 |
| - Ring | 8 | 0 | 0 |
| - Disc | 7 | 0 | 0 |
| - Prism | 8 | 0 | 0 |
| - Polyhedron | 3 | 0 | 1 |
| - Cylinder | 9 | 0 | 0 |
| - Torus | 0 | 6 | 1 |
| - Cone | 0 | 7 | 0 |
| - Helix | 0 | 8 | 0 |
| **TOTAL** | ~164 | ~21 | ~4 |

> Note: Torus, Cone, and Helix are planned future shapes (Phase 4).
> Missing count reflects only these future shape parameters."""


def update_document():
    content = DOC_PATH.read_text(encoding='utf-8')
    original = content
    
    updates_applied = 0
    
    for old, new in UPDATES:
        if old in content:
            content = content.replace(old, new)
            updates_applied += 1
            print(f"✅ Updated: {old[:60]}...")
        else:
            print(f"⚠️  Not found: {old[:60]}...")
    
    # Update summary section
    summary_pattern = r"## 13\. Summary: Missing Parameters Count.*?(?=\n---|\n## 14\.)"
    if re.search(summary_pattern, content, re.DOTALL):
        content = re.sub(summary_pattern, SUMMARY_UPDATE, content, flags=re.DOTALL)
        print(f"✅ Updated summary table")
    
    # Update header status
    old_status = "> **Status:** ✅ Updated - verified against code (Dec 8, 2024)"
    new_status = "> **Status:** ✅ Updated - verified against code (Dec 9, 2024)"
    content = content.replace(old_status, new_status)
    
    if content != original:
        DOC_PATH.write_text(content, encoding='utf-8')
        print(f"\n{'='*60}")
        print(f"Applied {updates_applied} updates to 03_PARAMETERS.md")
        print(f"{'='*60}")
    else:
        print("\nNo changes needed - document already up to date")


if __name__ == "__main__":
    update_document()

