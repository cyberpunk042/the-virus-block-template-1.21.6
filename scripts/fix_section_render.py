#!/usr/bin/env python3
"""
Fix remaining section references in render() methods.
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

FILES_TO_FIX = [
    "AppearanceSubPanel.java",
    "VisibilitySubPanel.java",
    "AnimationSubPanel.java",
    "TransformSubPanel.java",
    "TriggerSubPanel.java",
    "LifecycleSubPanel.java",
    "ArrangementSubPanel.java",
]

def fix_render_method(content: str) -> str:
    """Fix the render method to not use section."""
    
    # Pattern 1: section.render(...); followed by if (section.isExpanded()) { for loop }
    # Replace entire block with just the for loop
    pattern1 = re.compile(
        r'section\.render\([^)]+\);\s*\n\s*'
        r'if \(section\.isExpanded\(\)\) \{\s*\n\s*'
        r'for \(var widget : widgets\) widget\.render\(context, mouseX, mouseY, delta\);\s*\n\s*'
        r'\}',
        re.MULTILINE
    )
    content = pattern1.sub(
        '// Render widgets directly (no expandable section)\n'
        '        for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);',
        content
    )
    
    # Pattern 2: Just section.render line (already handled above, but catch stragglers)
    content = re.sub(
        r'\s*section\.render\([^)]+\);',
        '',
        content
    )
    
    # Pattern 3: if (section.isExpanded()) with block
    content = re.sub(
        r'if \(section\.isExpanded\(\)\) \{\s*\n(\s+for \(var widget : widgets\)[^}]+)\}',
        r'\1',
        content
    )
    
    # Pattern 4: Just if (section.isExpanded()) { ... } - replace with unconditional
    # This handles cases where there's more complex content
    pattern4 = re.compile(
        r'if \(section\.isExpanded\(\)\) \{([^}]+)\}',
        re.DOTALL
    )
    def replace_expanded_block(match):
        body = match.group(1)
        # Dedent the body by 4 spaces
        lines = body.split('\n')
        dedented = []
        for line in lines:
            if line.startswith('            '):
                dedented.append(line[4:])  # Remove one level of indent
            else:
                dedented.append(line)
        return '\n'.join(dedented).strip()
    
    # Only apply if section still referenced
    if 'section.isExpanded()' in content:
        content = pattern4.sub(replace_expanded_block, content)
    
    return content


def main():
    print("Fixing render methods...\n")
    
    for fname in FILES_TO_FIX:
        fpath = PANEL_DIR / fname
        if not fpath.exists():
            print(f"  NOT FOUND: {fname}")
            continue
            
        content = fpath.read_text(encoding='utf-8')
        
        if 'section.' not in content:
            print(f"  SKIP: {fname} - no section references")
            continue
        
        fixed = fix_render_method(content)
        
        if fixed != content:
            fpath.write_text(fixed, encoding='utf-8')
            print(f"  FIXED: {fname}")
        else:
            print(f"  UNCHANGED: {fname}")


if __name__ == "__main__":
    main()

