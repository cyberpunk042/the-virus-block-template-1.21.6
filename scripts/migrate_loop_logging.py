#!/usr/bin/env python3
"""
LogScope Migration Tool

Finds logging calls inside loops and helps migrate them to LogScope pattern.

Usage:
    python scripts/migrate_loop_logging.py [path]
"""

import os
import sys

# Add scripts dir to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from lib.scanner import Scanner
from lib.transformer import Transformer
from lib.display import show_summary, show_transformation, show_action_menu, show_report, clr, C
from lib.applier import Applier


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else 'src'
    
    if not os.path.exists(path):
        print(f"Error: '{path}' not found", file=sys.stderr)
        sys.exit(1)
    
    # Phase 1: Scan
    scanner = Scanner()
    loops = scanner.scan_directory(path)
    
    if not loops:
        print(clr("\n✓ No logging calls inside loops!", C.GREEN))
        return
    
    # Sort: nested first, then by file
    loops.sort(key=lambda l: (-l.depth, l.file_path, l.start_line))
    
    # Phase 2: Generate transformations
    transformer = Transformer()
    transformations = [transformer.transform(loop) for loop in loops]
    
    # Phase 3: Display summary
    show_summary(loops, path)
    
    try:
        input(clr("\n  Press Enter to review each loop...", C.DIM))
    except (KeyboardInterrupt, EOFError):
        return
    
    # Phase 4: Interactive review and apply
    applier = Applier()
    applied = []
    skipped = []
    
    for i, trans in enumerate(transformations, 1):
        show_transformation(i, len(transformations), trans, path)
        action = show_action_menu(trans.loop.has_batchable)
        
        if action == 'q':
            break
        elif action == 'a':
            success, msg = applier.apply(trans)
            if success:
                print(clr(f"  ✓ {msg}", C.GREEN))
                applied.append(trans.loop)
            else:
                print(clr(f"  ✗ {msg}", C.RED))
                skipped.append((trans.loop, msg))
        elif action == 's':
            reason = input(clr("  Reason? ", C.YELLOW)).strip()
            skipped.append((trans.loop, reason))
            print(clr("  ⊘ Skipped", C.DIM))
        # 'n' just continues
    
    # Show report
    show_report(applied, skipped, path)


if __name__ == '__main__':
    main()
