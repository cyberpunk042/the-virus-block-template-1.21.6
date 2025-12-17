#!/usr/bin/env python3
"""
Documentation Generator v4 - Hierarchical
==========================================
Generates focused, interconnected documentation with parent-child structure.

Output:
docs/
├── README.md           # Root navigation
├── ARCHITECTURE.md     # System overview
├── field/              # Field system
│   ├── README.md       # Field overview + summary diagram
│   ├── core.md         # Core classes
│   └── effects.md      # Effects & triggers
├── gui/                # GUI system
│   ├── README.md       # GUI overview + summary diagram
│   ├── panels.md
│   ├── widgets.md
│   └── state.md
├── visual.md           # Visual system (single file)
├── rendering.md        # Rendering pipeline (single file)
└── ...
"""

import sys
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict, Set, Tuple

SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass, is_valid_java_identifier
from lib.graph_builder import build_graph, ClassGraph
from lib.mermaid_generator import MermaidGenerator, DiagramConfig

PROJECT_ROOT = SCRIPT_DIR.parent
DOCS_DIR = PROJECT_ROOT / "docs"


# =============================================================================
# SYSTEM GROUPS - Parent folders with children
# =============================================================================

SYSTEM_GROUPS = {
    "field": {
        "title": "Field System",
        "description": "Complete field system - definitions, effects, bindings, triggers, lifecycle.",
        "children": {
            "core": {
                "title": "Core Classes",
                "description": "Field definitions, layers, registry, and manager.",
                "packages": ["field", "field.loader"],
                "key_classes": ["FieldDefinition", "FieldLayer", "FieldManager", "FieldRegistry"],
            },
            "effects": {
                "title": "Effects & Triggers",
                "description": "Effects, bindings, triggers, and lifecycle management.",
                "packages": ["field.effect", "field.influence", "field.instance"],
                "key_classes": ["EffectProcessor", "BindingResolver", "TriggerProcessor", "FieldInstance"],
            },
        },
        "summary_diagram": """```mermaid
graph TD
    subgraph Field Core
        FD[FieldDefinition] --> FL[FieldLayer]
        FL --> Primitive
        FM[FieldManager] --> FD
        FR[FieldRegistry] --> FD
    end
    
    subgraph Effects & Triggers
        FI[FieldInstance] --> FM
        FI --> EP[EffectProcessor]
        FI --> TP[TriggerProcessor]
        BR[BindingResolver] --> FI
    end
    
    FM --> FI
```""",
    },
    "gui": {
        "title": "GUI System",
        "description": "Complete GUI - panels, widgets, state management, and layouts.",
        "children": {
            "panels": {
                "title": "Panels",
                "description": "Panel hierarchy - main panels and sub-panels.",
                "packages": ["client.gui.panel", "client.gui.panel.sub"],
                "key_classes": ["AbstractPanel", "AdvancedPanel", "QuickPanel", "LayerPanel"],
            },
            "widgets": {
                "title": "Widgets",
                "description": "Reusable UI components - buttons, sliders, dropdowns.",
                "packages": ["client.gui.widget", "client.gui.util"],
                "key_classes": ["LabeledSlider", "ColorButton", "DropdownWidget", "CompactSelector"],
            },
            "state": {
                "title": "State & Adapters",
                "description": "State management, adapters, and layout managers.",
                "packages": ["client.gui.state", "client.gui.state.adapter", "client.gui.layout", "client.gui.screen", "client.gui.preview", "client.gui"],
                "key_classes": ["FieldEditState", "AbstractAdapter", "LayoutManager", "FieldCustomizerScreen"],
            },
        },
        "summary_diagram": """```mermaid
graph LR
    FCS[FieldCustomizerScreen] --> LM[LayoutManager]
    FCS --> Panels
    
    subgraph Panels
        AP[AbstractPanel]
        AP --> QuickPanel
        AP --> AdvancedPanel
        AdvancedPanel --> SubPanels[SubPanels]
    end
    
    subgraph Widgets
        LabeledSlider
        ColorButton
        DropdownWidget
    end
    
    SubPanels --> Widgets
    
    FES[FieldEditState] --> Adapters
    Adapters --> Panels
```""",
    },
}

# Single-file systems (no parent folder needed)
SINGLE_SYSTEMS = {
    "visual": {
        "title": "Visual System",
        "description": "Shape definitions, patterns, colors, animations, and fill modes.",
        "packages": ["visual", "visual.shape", "visual.pattern", "visual.animation", "visual.color", "visual.fill", "visual.transform", "visual.appearance", "visual.visibility", "visual.layer", "visual.util"],
        "key_classes": ["Shape", "QuadPattern", "AnimationConfig", "ColorTheme", "FillMode"],
    },
    "rendering": {
        "title": "Rendering Pipeline",
        "description": "Mesh building, tessellators, primitive renderers, and render layers.",
        "packages": ["client.visual", "client.visual.mesh", "client.visual.tessellator", "client.visual.render", "client.field.render", "client.field"],
        "key_classes": ["MeshBuilder", "SphereTessellator", "FieldRenderer", "LayerRenderer", "PrimitiveRenderer"],
    },
    "blocks": {
        "title": "Blocks & Growth",
        "description": "Custom blocks, block entities, and growth system.",
        "packages": ["block", "block.corrupted", "block.infected", "block.entity", "block.growth", "growth", "growth.event", "collision", "entity"],
        "key_classes": ["ProgressiveGrowthBlock", "VirusBlock", "GrowthForceHandler"],
    },
    "infection": {
        "title": "Infection System",
        "description": "Virus spreading, infection services, and scenarios.",
        "packages": ["infection", "infection.service", "infection.controller", "infection.scenario", "infection.orchestrator", "infection.event", "infection.registry", "infection.spread", "infection.spawn"],
        "key_classes": ["InfectionService", "InfectionController", "ScenarioConfig"],
    },
    "network": {
        "title": "Network & Commands",
        "description": "Client-server payloads and command system.",
        "packages": ["network", "network.payload", "command", "command.argument"],
        "key_classes": ["FieldSpawnC2SPayload", "FieldCommand"],
    },
    "infrastructure": {
        "title": "Infrastructure",
        "description": "Logging, registries, configuration, and utilities.",
        "packages": ["log", "registry", "config", "util", "mixin", "mixin.client"],
        "key_classes": ["Logging", "LogScope", "ModConfig"],
    },
}


class DocGenerator:
    """Generates hierarchical documentation."""
    
    def __init__(self, graph: ClassGraph, dry_run: bool = False):
        self.graph = graph
        self.dry_run = dry_run
        self.files_written = 0
        self.config = DiagramConfig(
            max_classes_per_diagram=35,
            max_methods_per_class=5,
            include_external=True,
            show_composition=True
        )
        
        # Index classes by relative package
        self.classes_by_package = defaultdict(list)
        for c in graph.classes.values():
            rel_pkg = c.relative_package.replace('net.cyberpunk042.', '').replace('net.cyberpunk042.client.', 'client.')
            self.classes_by_package[rel_pkg].append(c)
    
    def write_file(self, path: Path, content: str):
        """Write file (respecting dry_run)."""
        if self.dry_run:
            print(f"  [DRY] {path.relative_to(PROJECT_ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
            print(f"  ✅ {path.relative_to(PROJECT_ROOT)}")
        self.files_written += 1
    
    def get_system_classes(self, packages: list) -> List[JavaClass]:
        """Get all classes belonging to given packages."""
        classes = []
        for pkg in packages:
            classes.extend(self.classes_by_package.get(pkg, []))
        return [c for c in classes if is_valid_java_identifier(c.name)]
    
    def generate_root_readme(self):
        """Generate root README with navigation."""
        stats = self.graph.get_stats()
        
        content = f"""# 📚 The Virus Block - Documentation

> Auto-generated {datetime.now().strftime('%Y-%m-%d')} | {stats['total_classes']} classes

## Systems

| System | Description |
|--------|-------------|
"""
        # Grouped systems
        for group_id, group in SYSTEM_GROUPS.items():
            content += f"| [{group['title']}](./{group_id}/) | {group['description']} |\n"
        
        # Single systems
        for sys_id, cfg in SINGLE_SYSTEMS.items():
            classes = self.get_system_classes(cfg['packages'])
            if len(classes) >= 3:
                content += f"| [{cfg['title']}](./{sys_id}.md) | {cfg['description']} |\n"
        
        content += """
## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for system flows and interconnections.
"""
        self.write_file(DOCS_DIR / "README.md", content)
    
    def generate_architecture(self):
        """Generate architecture with Mermaid diagrams."""
        content = f"""# 🏗️ System Architecture

> The Virus Block - Minecraft 1.21.6 Fabric Mod

## High-Level Overview

```mermaid
graph TB
    subgraph Common/Server
        FS[Field System]
        VS[Visual System]
        IS[Infection System]
        BS[Blocks & Growth]
        NET[Network]
    end
    
    subgraph Client
        GUI[GUI System]
        RP[Rendering Pipeline]
    end
    
    FS --> VS
    FS --> NET
    IS --> BS
    BS --> NET
    NET --> RP
    GUI --> FS
    RP --> VS
```

## System Connections

```mermaid
flowchart LR
    subgraph Input
        User([User]) --> GUI
    end
    
    subgraph State
        GUI --> FES[FieldEditState]
        FES --> FD[FieldDefinition]
    end
    
    subgraph Storage
        FD --> JSON[(JSON)]
        JSON --> FR[FieldRegistry]
    end
    
    subgraph Runtime
        FR --> FM[FieldManager]
        FM --> FI[FieldInstance]
    end
    
    subgraph Render
        FI --> NET[Network]
        NET --> FRenderer[FieldRenderer]
        FRenderer --> GPU([GPU])
    end
```

## Data Flow

```
User (GUI)
    │
    ▼
FieldEditState ──────► FieldDefinition.toJson()
    │                         │
    ▼                         ▼
Adapters ◄───────── FieldProfileStore.save()
    │
    ▼
FieldManager.spawn() ──────────────────────┐
    │                                       │
    ▼                                       ▼
FieldInstance ───────► Network Payload ───► Other Clients
    │
    ▼
FieldRenderer.render()
    │
    ▼
LayerRenderer → PrimitiveRenderer → Tessellator → MeshBuilder → GPU
```

## Key Entry Points

| Entry | Purpose |
|-------|---------|
| `TheVirusBlock.onInitialize()` | Server init |
| `TheVirusBlockClient.onInitializeClient()` | Client init |
| `FieldCustomizerScreen` | GUI entry |
| `FieldCommand` | `/field` commands |
| `FieldManager` | Runtime fields |
"""
        self.write_file(DOCS_DIR / "ARCHITECTURE.md", content)
    
    def generate_group_readme(self, group_id: str, group: dict):
        """Generate parent README with summary diagram."""
        children = group['children']
        
        content = f"""# {group['title']}

> {group['description']}

## Overview

{group.get('summary_diagram', '')}

## Modules

| Module | Description |
|--------|-------------|
"""
        for child_id, child in children.items():
            classes = self.get_system_classes(child['packages'])
            content += f"| [{child['title']}](./{child_id}.md) | {len(classes)} classes - {child['description']} |\n"
        
        content += """
---
See also: [ARCHITECTURE.md](../ARCHITECTURE.md)
"""
        self.write_file(DOCS_DIR / group_id / "README.md", content)
    
    def generate_class_diagram_file(self, path: Path, title: str, description: str, classes: List[JavaClass], key_classes: list):
        """Generate a class diagram markdown file."""
        if len(classes) < 3:
            return
        
        gen = MermaidGenerator(classes, self.config)
        
        lines = []
        lines.append(f"# {title}")
        lines.append("")
        lines.append(f"> {description}")
        lines.append("")
        lines.append(f"**{len(classes)} classes**")
        lines.append("")
        
        # Key classes
        key = [c for c in classes if c.name in key_classes]
        if key:
            lines.append("## Key Classes")
            lines.append("")
            for c in key:
                ext = f" → `{c.extends}`" if c.extends else ""
                lines.append(f"- **`{c.name}`** ({c.class_type}){ext}")
            lines.append("")
        
        # Main diagram
        lines.append("## Class Diagram")
        lines.append("")
        lines.append(gen.generate_class_diagram(
            classes[:35],
            show_methods=True,
            show_fields=True,
            show_inheritance=True,
            show_dependencies=True
        ))
        lines.append("")
        
        self.write_file(path, '\n'.join(lines))
    
    def generate_all(self):
        """Generate all documentation."""
        print(f"\n📝 Generating documentation to {DOCS_DIR.relative_to(PROJECT_ROOT)}/")
        
        # Clean old files
        if not self.dry_run:
            import shutil
            for item in DOCS_DIR.iterdir():
                if item.name in ['main', 'client', 'shared'] + list(SYSTEM_GROUPS.keys()):
                    if item.is_dir():
                        shutil.rmtree(item)
                        print(f"  🗑️ Removed {item.name}/")
                elif item.suffix == '.md' and item.name not in ['README.md', 'ARCHITECTURE.md']:
                    # Remove old single-file systems
                    if item.stem in list(SINGLE_SYSTEMS.keys()) + ['field_core', 'field_effects', 'gui_panels', 'gui_widgets', 'gui_state', 'field_system', 'gui_system']:
                        item.unlink()
                        print(f"  🗑️ Removed {item.name}")
        
        print("\n📄 Core files:")
        self.generate_root_readme()
        self.generate_architecture()
        
        print("\n📁 System groups:")
        for group_id, group in SYSTEM_GROUPS.items():
            self.generate_group_readme(group_id, group)
            
            for child_id, child in group['children'].items():
                classes = self.get_system_classes(child['packages'])
                if len(classes) >= 3:
                    self.generate_class_diagram_file(
                        DOCS_DIR / group_id / f"{child_id}.md",
                        child['title'],
                        child['description'],
                        classes,
                        child.get('key_classes', [])
                    )
                else:
                    print(f"  ⏭️ Skipping {group_id}/{child_id} ({len(classes)} classes)")
        
        print("\n📊 Single-file systems:")
        for sys_id, cfg in SINGLE_SYSTEMS.items():
            classes = self.get_system_classes(cfg['packages'])
            if len(classes) >= 3:
                self.generate_class_diagram_file(
                    DOCS_DIR / f"{sys_id}.md",
                    cfg['title'],
                    cfg['description'],
                    classes,
                    cfg.get('key_classes', [])
                )
            else:
                print(f"  ⏭️ Skipping {sys_id} ({len(classes)} classes)")
        
        print(f"\n✅ Done! {self.files_written} files written.")


def main():
    parser = argparse.ArgumentParser(description="Generate hierarchical documentation")
    parser.add_argument('--dry-run', action='store_true', help='Preview only')
    args = parser.parse_args()
    
    print("=" * 60)
    print("   DOCUMENTATION GENERATOR v4 (Hierarchical)")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠️  DRY RUN MODE")
    
    print("\n📂 Scanning project...")
    classes = scan_project(PROJECT_ROOT)
    graph = build_graph(classes)
    
    print(f"\n📊 Found {len(classes)} classes")
    
    generator = DocGenerator(graph, dry_run=args.dry_run)
    generator.generate_all()


if __name__ == "__main__":
    main()
