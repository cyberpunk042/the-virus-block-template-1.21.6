#!/usr/bin/env python3
"""
Migrate panels from AbstractPanel to LayoutPanel (v2).

Usage:
    python3 scripts/migrate_panels_to_v2.py --panel AppearanceSubPanel
    python3 scripts/migrate_panels_to_v2.py --all
    python3 scripts/migrate_panels_to_v2.py --list
"""

import argparse
import re
from pathlib import Path
from dataclasses import dataclass

SRC_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")
V2_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/v2")

# Panel migration templates
PANEL_TEMPLATE = '''package net.cyberpunk042.client.gui.panel.v2;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

/**
 * V2 {name} using new LayoutPanel architecture.
 * Migrated from: {original_class}
 */
public class {class_name} extends LayoutPanel {{
    
{fields}
    
    public {class_name}(Screen parent, FieldEditState state, TextRenderer textRenderer) {{
        super(parent, state, textRenderer);
    }}
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {{
        int w = contentWidth();
        int h = 20;
        
{build_content}
    }}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {{
        // Draw section header
        context.drawTextWithShadow(textRenderer, "{section_name}", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }}
    
    @Override
    public void tick() {{
        // Sync from state if needed
    }}
}}
'''


@dataclass
class WidgetInfo:
    name: str
    type: str
    field_type: str  # Slider, Toggle, ColorButton, etc.
    state_path: str
    range_min: float = 0
    range_max: float = 1
    format_str: str = "%.2f"
    label: str = ""


def parse_panel(filepath: Path) -> dict:
    """Parse an old-style panel and extract widget definitions."""
    content = filepath.read_text(encoding='utf-8')
    
    info = {
        'class_name': filepath.stem,
        'widgets': [],
        'section_name': '',
        'original_class': filepath.stem,
    }
    
    # Extract section name
    section_match = re.search(r'"([^"]+)".*//.*(?:Start|collapsed|section)', content, re.I)
    if section_match:
        info['section_name'] = section_match.group(1)
    else:
        # Try to extract from ExpandableSection constructor
        section_match = re.search(r'ExpandableSection\([^)]+,\s*"([^"]+)"', content)
        if section_match:
            info['section_name'] = section_match.group(1)
    
    # Extract sliders
    for match in re.finditer(
        r'(\w+Slider)\s*=\s*LabeledSlider\.builder\("([^"]+)"\)' +
        r'[^;]*\.range\(([^,]+),\s*([^)]+)\)[^;]*\.initial\(([^)]+)\)[^;]*\.format\("([^"]+)"\)' +
        r'[^;]*\.onChange\([^)]*state\.set\("([^"]+)"',
        content, re.DOTALL
    ):
        w = WidgetInfo(
            name=match.group(1),
            type='slider',
            field_type='LabeledSlider',
            label=match.group(2),
            range_min=float(match.group(3).strip('f')),
            range_max=float(match.group(4).strip('f')),
            state_path=match.group(7),
            format_str=match.group(6),
        )
        info['widgets'].append(w)
    
    # Extract toggles
    for match in re.finditer(
        r'(\w+Toggle)\s*=\s*GuiWidgets\.toggle\([^)]+,\s*"([^"]+)"[^)]+state\.([^,]+)[^)]*\)',
        content
    ):
        w = WidgetInfo(
            name=match.group(1),
            type='toggle',
            field_type='CyclingButtonWidget<Boolean>',
            label=match.group(2),
            state_path=match.group(3).strip(),
        )
        info['widgets'].append(w)
    
    # Extract color buttons
    for match in re.finditer(
        r'(\w+ColorBtn)\s*=\s*new\s+ColorButton\([^)]+,\s*"([^"]+)"[^)]+state\.getInt\("([^"]+)"\)',
        content
    ):
        w = WidgetInfo(
            name=match.group(1),
            type='color',
            field_type='ColorButton',
            label=match.group(2),
            state_path=match.group(3),
        )
        info['widgets'].append(w)
    
    return info


def generate_v2_panel(info: dict) -> str:
    """Generate v2 panel code from parsed info."""
    
    # Generate field declarations
    fields = []
    for w in info['widgets']:
        fields.append(f"    private {w.field_type} {w.name};")
    
    # Generate build content
    build_lines = []
    for w in info['widgets']:
        if w.type == 'slider':
            build_lines.append(f'''        // {w.label}
        {w.name} = new LabeledSlider(
            0, 0, w, h,
            Text.literal("{w.label}"),
            {w.range_min}f, {w.range_max}f, state.getFloat("{w.state_path}"),
            "{w.format_str}",
            v -> state.set("{w.state_path}", v)
        );
        layout.add({w.name}, p -> p.marginTop(4).marginLeft(4));''')
        elif w.type == 'toggle':
            build_lines.append(f'''        // {w.label}
        {w.name} = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("{w.state_path}"))
            .build(0, 0, w, h, Text.literal("{w.label}"), (btn, val) -> {{
                state.set("{w.state_path}", val);
            }});
        layout.add({w.name}, p -> p.marginTop(4).marginLeft(4));''')
        elif w.type == 'color':
            build_lines.append(f'''        // {w.label} color
        // TODO: Migrate ColorButton to new architecture
        // {w.name} = new ColorButton(...);''')
    
    # Format name for class
    old_name = info['class_name']
    new_name = old_name.replace('SubPanel', 'Panel')
    
    return PANEL_TEMPLATE.format(
        name=info['section_name'] or new_name,
        class_name=new_name,
        original_class=old_name,
        fields='\n'.join(fields) if fields else '    // No widget fields',
        build_content='\n\n'.join(build_lines) if build_lines else '        // TODO: Add widgets',
        section_name=info['section_name'] or new_name,
    )


def migrate_panel(name: str):
    """Migrate a single panel to v2."""
    src_file = SRC_DIR / f"{name}.java"
    if not src_file.exists():
        print(f"ERROR: Source file not found: {src_file}")
        return False
    
    V2_DIR.mkdir(parents=True, exist_ok=True)
    
    info = parse_panel(src_file)
    v2_code = generate_v2_panel(info)
    
    new_name = name.replace('SubPanel', 'Panel')
    dst_file = V2_DIR / f"{new_name}.java"
    
    dst_file.write_text(v2_code, encoding='utf-8')
    print(f"✓ Created {dst_file}")
    print(f"  Widgets migrated: {len(info['widgets'])}")
    for w in info['widgets']:
        print(f"    - {w.name} ({w.type})")
    
    return True


def list_panels():
    """List all panels available for migration."""
    print("Panels available for migration:")
    for f in sorted(SRC_DIR.glob("*SubPanel.java")):
        print(f"  - {f.stem}")


def main():
    parser = argparse.ArgumentParser(description="Migrate panels to v2 LayoutPanel")
    parser.add_argument("--panel", help="Migrate a specific panel")
    parser.add_argument("--all", action="store_true", help="Migrate all panels")
    parser.add_argument("--list", action="store_true", help="List available panels")
    
    args = parser.parse_args()
    
    if args.list:
        list_panels()
    elif args.all:
        success = 0
        for f in SRC_DIR.glob("*SubPanel.java"):
            if migrate_panel(f.stem):
                success += 1
        print(f"\nMigrated {success} panels")
    elif args.panel:
        migrate_panel(args.panel)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()

