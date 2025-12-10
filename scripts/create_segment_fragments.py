#!/usr/bin/env python3
"""
Create segment arrangement fragments for rings.

Segment patterns define which parts of a ring are visible.
Each pattern has a name, description, and configuration.
"""

import json
import os
from pathlib import Path

CONFIG_ROOT = Path("config/the-virus-block")
ARRANGEMENTS_DIR = CONFIG_ROOT / "field_arrangements"

# =============================================================================
# SEGMENT PATTERNS
# =============================================================================

SEGMENT_FRAGMENTS = {
    # --- Basic Patterns ---
    "segment_all": {
        "name": "All Segments",
        "description": "Complete ring - all segments visible",
        "cellType": "SEGMENT",
        "pattern": "full",
        "visibility": 1.0
    },
    "segment_alternate": {
        "name": "Alternating",
        "description": "Every other segment visible - classic stripe effect",
        "cellType": "SEGMENT",
        "pattern": "alternating",
        "skip": 1
    },
    "segment_sparse": {
        "name": "Sparse",
        "description": "Few segments visible - minimal look",
        "cellType": "SEGMENT",
        "pattern": "sparse",
        "visibility": 0.25,
        "skip": 3
    },
    "segment_dense": {
        "name": "Dense",
        "description": "Most segments visible - nearly complete",
        "cellType": "SEGMENT",
        "pattern": "dense",
        "visibility": 0.75,
        "skip": 0,
        "show": 3
    },
    
    # --- Fractional Patterns ---
    "segment_half": {
        "name": "Half Circle",
        "description": "Semicircle - only half the ring",
        "cellType": "SEGMENT",
        "pattern": "arc",
        "arcStart": 0.0,
        "arcEnd": 0.5
    },
    "segment_thirds": {
        "name": "Thirds",
        "description": "Three equal segments with gaps",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 3,
        "gapRatio": 0.5
    },
    "segment_quarters": {
        "name": "Quarters",
        "description": "Four equal segments - compass points",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 4,
        "gapRatio": 0.5
    },
    "segment_sixths": {
        "name": "Sixths",
        "description": "Six equal segments - hexagonal feel",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 6,
        "gapRatio": 0.4
    },
    "segment_eighths": {
        "name": "Eighths",
        "description": "Eight equal segments - octagonal feel",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 8,
        "gapRatio": 0.3
    },
    
    # --- Dash Patterns ---
    "segment_dashed": {
        "name": "Dashed",
        "description": "Long-short dash pattern",
        "cellType": "SEGMENT",
        "pattern": "dashed",
        "dashLength": 0.15,
        "gapLength": 0.05
    },
    "segment_dotted": {
        "name": "Dotted",
        "description": "Small dots around the ring",
        "cellType": "SEGMENT",
        "pattern": "dotted",
        "dotLength": 0.03,
        "gapLength": 0.07
    },
    "segment_morse": {
        "name": "Morse",
        "description": "Dot-dash alternating pattern",
        "cellType": "SEGMENT",
        "pattern": "morse",
        "sequence": [0.05, 0.02, 0.15, 0.02]
    },
    
    # --- Positional Patterns ---
    "segment_single": {
        "name": "Single Arc",
        "description": "Just one segment visible",
        "cellType": "SEGMENT",
        "pattern": "arc",
        "arcStart": 0.0,
        "arcEnd": 0.15
    },
    "segment_opposing": {
        "name": "Opposing",
        "description": "Two segments 180° apart",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 2,
        "gapRatio": 0.6
    },
    "segment_cross": {
        "name": "Cross",
        "description": "Four segments at 90° intervals",
        "cellType": "SEGMENT",
        "pattern": "uniform",
        "count": 4,
        "gapRatio": 0.7
    },
    
    # --- Special Patterns ---
    "segment_asymmetric": {
        "name": "Asymmetric",
        "description": "Uneven distribution - organic feel",
        "cellType": "SEGMENT",
        "pattern": "custom",
        "arcs": [
            {"start": 0.0, "end": 0.2},
            {"start": 0.35, "end": 0.4},
            {"start": 0.6, "end": 0.85}
        ]
    },
    "segment_fade": {
        "name": "Fade Out",
        "description": "Segments get smaller/thinner",
        "cellType": "SEGMENT",
        "pattern": "gradient",
        "direction": "clockwise",
        "startVisibility": 1.0,
        "endVisibility": 0.0
    },
    "segment_burst": {
        "name": "Burst",
        "description": "Clustered segments at one point",
        "cellType": "SEGMENT",
        "pattern": "custom",
        "arcs": [
            {"start": 0.0, "end": 0.08},
            {"start": 0.1, "end": 0.15},
            {"start": 0.17, "end": 0.20},
            {"start": 0.22, "end": 0.24}
        ]
    },
    "segment_spiral": {
        "name": "Spiral",
        "description": "Segments arranged in spiral pattern",
        "cellType": "SEGMENT",
        "pattern": "spiral",
        "turns": 1.5,
        "segmentCount": 12
    },
    "segment_wave": {
        "name": "Wave",
        "description": "Wavy segment distribution",
        "cellType": "SEGMENT",
        "pattern": "wave",
        "frequency": 4,
        "amplitude": 0.3
    },
}


def create_fragments(dry_run=False):
    """Create all segment arrangement fragments."""
    created = 0
    skipped = 0
    
    # Ensure directory exists
    if not ARRANGEMENTS_DIR.exists():
        if dry_run:
            print(f"[DRY-RUN] Would create directory: {ARRANGEMENTS_DIR}")
        else:
            ARRANGEMENTS_DIR.mkdir(parents=True, exist_ok=True)
            print(f"Created directory: {ARRANGEMENTS_DIR}")
    
    print(f"\n{'='*60}")
    print("SEGMENT ARRANGEMENT FRAGMENTS")
    print(f"{'='*60}\n")
    
    for filename, data in SEGMENT_FRAGMENTS.items():
        filepath = ARRANGEMENTS_DIR / f"{filename}.json"
        
        if filepath.exists():
            skipped += 1
            print(f"  SKIP (exists): {filename}.json")
            continue
        
        if dry_run:
            print(f"  [DRY-RUN] Would create: {filename}.json")
            print(f"            Name: {data['name']}")
        else:
            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2)
            print(f"  Created: {filename}.json - {data['name']}")
        
        created += 1
    
    print(f"\n{'='*60}")
    print(f"Summary: {created} created, {skipped} skipped (already exist)")
    print(f"Total segment patterns: {len(SEGMENT_FRAGMENTS)}")
    if dry_run:
        print("(Dry run - no files were actually created)")
    print(f"{'='*60}")


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Create segment arrangement fragments")
    parser.add_argument("--dry-run", action="store_true", 
                        help="Show what would be created without writing files")
    args = parser.parse_args()
    
    create_fragments(dry_run=args.dry_run)


if __name__ == "__main__":
    main()

