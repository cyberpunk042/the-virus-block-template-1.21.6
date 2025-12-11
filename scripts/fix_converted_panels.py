#!/usr/bin/env python3
"""
Fix the converted panels - clean up duplicate code and add missing methods.

Issues to fix:
1. Remove duplicate field assignments (state, layout are inherited)
2. Add tick() method override
3. Remove conflicting final field declarations
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

def fix_panel(filepath: Path) -> bool:
    """Fix common conversion issues in a panel file."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    
    # Remove assignments to inherited final 'state' field  
    content = re.sub(r'\s*this\.state = state;\s*\n', '\n', content)
    
    # Remove duplicate layout assignments (keep just one in init)
    # The script added multiple - remove all but leave one
    layout_assignments = list(re.finditer(r'\s*this\.layout = new GuiLayout\([^;]+;\s*\n', content))
    if len(layout_assignments) > 1:
        # Keep only the first one, remove the rest
        for match in reversed(layout_assignments[1:]):
            content = content[:match.start()] + '\n' + content[match.end():]
    
    # Remove 'final' from field declarations that need to be set in init()
    content = re.sub(r'private final (ExpandableSection section)', r'private \1', content)
    content = re.sub(r'private final (GuiLayout layout)', r'private \1', content)
    content = re.sub(r'private final (List<String> availableSources)', r'private \1', content)
    
    # Add tick() method if missing
    if '@Override' not in content or 'public void tick()' not in content:
        # Find the last } of the class
        last_brace = content.rfind('}')
        if last_brace > 0:
            tick_method = '''
    @Override
    public void tick() {
        // No per-tick updates needed
    }
'''
            # Check if tick() already exists
            if 'public void tick()' not in content:
                content = content[:last_brace] + tick_method + '\n' + content[last_brace:]
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        return True
    return False

def fix_callers():
    """Fix caller sites in AdvancedPanel and DebugPanel."""
    
    # Fix AdvancedPanel
    adv_path = PANEL_DIR.parent / "AdvancedPanel.java"
    if adv_path.exists():
        content = adv_path.read_text(encoding='utf-8')
        
        # Update LinkingSubPanel constructor call
        content = re.sub(
            r'new LinkingSubPanel\(state, 0, contentY, width\)',
            r'new LinkingSubPanel(parent, state, contentY)',
            content
        )
        
        # Update PredictionSubPanel constructor call  
        content = re.sub(
            r'new PredictionSubPanel\(state, 0, contentY, width\)',
            r'new PredictionSubPanel(parent, state, contentY)',
            content
        )
        
        # Update FollowModeSubPanel constructor call
        content = re.sub(
            r'new FollowModeSubPanel\(state, 0, contentY, width\)',
            r'new FollowModeSubPanel(parent, state, contentY)',
            content
        )
        
        adv_path.write_text(content, encoding='utf-8')
        print("Fixed: AdvancedPanel.java")
    
    # Fix DebugPanel
    dbg_path = PANEL_DIR.parent / "DebugPanel.java"
    if dbg_path.exists():
        content = dbg_path.read_text(encoding='utf-8')
        
        # Update BindingsSubPanel constructor call
        content = re.sub(
            r'new BindingsSubPanel\(state, 0, contentY, width\)',
            r'new BindingsSubPanel(parent, state, contentY)',
            content
        )
        
        # Update BeamSubPanel constructor call
        content = re.sub(
            r'new BeamSubPanel\(state, 0, contentY, width\)',
            r'new BeamSubPanel(parent, state, contentY)',
            content
        )
        
        dbg_path.write_text(content, encoding='utf-8')
        print("Fixed: DebugPanel.java")

def main():
    print("Fixing converted panels...")
    
    panels = [
        "BeamSubPanel.java",
        "LinkingSubPanel.java",
        "PredictionSubPanel.java", 
        "FollowModeSubPanel.java",
        "BindingsSubPanel.java",
    ]
    
    for filename in panels:
        filepath = PANEL_DIR / filename
        if filepath.exists():
            if fix_panel(filepath):
                print(f"Fixed: {filename}")
            else:
                print(f"No changes: {filename}")
    
    print("\nFixing callers...")
    fix_callers()
    
    print("\nDone!")

if __name__ == "__main__":
    main()

