#!/usr/bin/env python3
"""
Documentation Generator v5 - Proper Hierarchy
==============================================
- field.md (summary diagram) + field/ (details)
- Top-level summary shows key classes and relationships
"""

import sys
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict, Tuple

SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass, is_valid_java_identifier
from lib.graph_builder import build_graph, ClassGraph
from lib.mermaid_generator import MermaidGenerator, DiagramConfig

PROJECT_ROOT = SCRIPT_DIR.parent
DOCS_DIR = PROJECT_ROOT / "docs"


# =============================================================================
# SYSTEM GROUPS - With summary and detail splits
# =============================================================================

GROUPED_SYSTEMS = {
    "field": {
        "title": "Field System",
        "description": "Complete field system architecture.",
        "all_packages": ["field", "field.loader", "field.effect", "field.influence", "field.instance"],
        "summary_key_classes": ["FieldDefinition", "FieldLayer", "FieldManager", "FieldRegistry", "FieldInstance", "TriggerProcessor", "BindingResolver", "EffectProcessor"],
        "children": {
            "core": {
                "title": "Core Classes",
                "packages": ["field", "field.loader"],
            },
            "effects": {
                "title": "Effects & Triggers",
                "packages": ["field.effect", "field.influence", "field.instance"],
            },
        },
    },
    "gui": {
        "title": "GUI System",
        "description": "Complete graphical user interface architecture.",
        "all_packages": ["client.gui", "client.gui.panel", "client.gui.panel.sub", "client.gui.widget", "client.gui.state", "client.gui.state.adapter", "client.gui.preview", "client.gui.layout", "client.gui.screen", "client.gui.util"],
        "summary_key_classes": ["FieldCustomizerScreen", "AbstractPanel", "FieldEditState", "LayoutManager", "LabeledSlider", "AbstractAdapter"],
        "children": {
            "panels": {
                "title": "Panels",
                "packages": ["client.gui.panel", "client.gui.panel.sub"],
            },
            "widgets": {
                "title": "Widgets",
                "packages": ["client.gui.widget", "client.gui.util"],
            },
            "state": {
                "title": "State & Adapters",
                "packages": ["client.gui.state", "client.gui.state.adapter", "client.gui.layout", "client.gui.screen", "client.gui.preview", "client.gui"],
            },
        },
    },
}

# Single-file systems (no split needed)
SINGLE_SYSTEMS = {
    "visual": {
        "title": "Visual System",
        "description": "Shapes, patterns, colors, animations, and fill modes.",
        "packages": ["visual", "visual.shape", "visual.pattern", "visual.animation", "visual.color", "visual.fill", "visual.transform", "visual.appearance", "visual.visibility", "visual.layer"],
        "key_classes": ["Shape", "ShapeType", "QuadPattern", "TrianglePattern", "AnimationConfig", "ColorTheme", "FillMode", "FillConfig"],
    },
    "rendering": {
        "title": "Rendering Pipeline",
        "description": "Mesh building, tessellators, and renderers.",
        "packages": ["client.visual", "client.visual.mesh", "client.visual.tessellator", "client.visual.render", "client.field.render", "client.field"],
        "key_classes": ["MeshBuilder", "SphereTessellator", "PrismTessellator", "FieldRenderer", "LayerRenderer", "AbstractPrimitiveRenderer"],
    },
    "blocks": {
        "title": "Blocks & Growth",
        "description": "Custom blocks, block entities, growth system.",
        "packages": ["block", "block.corrupted", "block.infected", "block.entity", "block.growth", "growth", "entity"],
        "key_classes": ["ProgressiveGrowthBlock", "VirusBlock", "ProgressiveGrowthBlockEntity", "GrowthForceHandler"],
    },
    "infection": {
        "title": "Infection System",
        "description": "Virus spreading and scenario management.",
        "packages": ["infection", "infection.service", "infection.controller", "infection.scenario", "infection.orchestrator", "infection.spread", "infection.spawn"],
        "key_classes": ["InfectionService", "InfectionController", "ScenarioConfig"],
    },
    "network": {
        "title": "Network & Commands",
        "description": "Client-server packets and commands.",
        "packages": ["network", "network.payload", "command", "command.argument"],
        "key_classes": ["FieldSpawnC2SPayload", "FieldEditUpdateS2CPayload", "FieldCommand"],
    },
}


class DocGenerator:
    def __init__(self, graph: ClassGraph, dry_run: bool = False):
        self.graph = graph
        self.dry_run = dry_run
        self.files_written = 0
        self.config = DiagramConfig(
            max_classes_per_diagram=40,
            max_methods_per_class=5,
            include_external=True,
            show_composition=True
        )
        
        self.classes_by_package = defaultdict(list)
        for c in graph.classes.values():
            rel_pkg = c.relative_package.replace('net.cyberpunk042.', '').replace('net.cyberpunk042.client.', 'client.')
            self.classes_by_package[rel_pkg].append(c)
    
    def write_file(self, path: Path, content: str):
        if self.dry_run:
            print(f"  [DRY] {path.relative_to(PROJECT_ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
            print(f"  ✅ {path.relative_to(PROJECT_ROOT)}")
        self.files_written += 1
    
    def get_classes(self, packages: list) -> List[JavaClass]:
        classes = []
        for pkg in packages:
            classes.extend(self.classes_by_package.get(pkg, []))
        return [c for c in classes if is_valid_java_identifier(c.name)]
    
    def generate_class_diagram(self, classes: List[JavaClass], key_classes: list = None, simplified: bool = False) -> str:
        """Generate a mermaid class diagram."""
        if not classes:
            return ""
        
        gen = MermaidGenerator(classes, self.config)
        
        if simplified and key_classes:
            # For summary: Pass ALL classes to generator so relationships work,
            # but only show methods for key classes
            # We'll generate a focused diagram showing key classes with their relationships
            key = [c for c in classes if c.name in key_classes]
            # Add classes that key classes connect to
            connected = set()
            for kc in key:
                for f in kc.fields:
                    if f.type:
                        # Extract type names
                        for t in [f.type.split('<')[0].split('[')[0]] + \
                                 ([f.type.split('<')[1].split('>')[0].split(',')[0].strip()] if '<' in f.type else []):
                            for c in classes:
                                if c.name == t or t.endswith(c.name):
                                    connected.add(c.name)
                if kc.extends:
                    connected.add(kc.extends.split('<')[0])
            
            # Include key classes + their direct connections
            focus_classes = [c for c in classes if c.name in key_classes or c.name in connected]
            if focus_classes:
                return gen.generate_class_diagram(focus_classes[:40], show_methods=True, show_fields=False)
        
        return gen.generate_class_diagram(classes[:40])
    
    def generate_readme(self):
        stats = self.graph.get_stats()
        content = f"""# 📚 The Virus Block - Documentation

> Auto-generated {datetime.now().strftime('%Y-%m-%d')} | {stats['total_classes']} classes

## Systems

| System | Description |
|--------|-------------|
"""
        for group_id, group in GROUPED_SYSTEMS.items():
            content += f"| [{group['title']}](./{group_id}.md) | {group['description']} |\n"
        for sys_id, cfg in SINGLE_SYSTEMS.items():
            classes = self.get_classes(cfg['packages'])
            if len(classes) >= 3:
                content += f"| [{cfg['title']}](./{sys_id}.md) | {cfg['description']} |\n"
        
        content += "\n## Architecture\n\nSee [ARCHITECTURE.md](./ARCHITECTURE.md)\n"
        self.write_file(DOCS_DIR / "README.md", content)
    
    def generate_architecture(self):
        content = """# 🏗️ System Architecture

## High-Level Overview

```mermaid
graph TB
    subgraph Common
        FS[Field System] --> VS[Visual System]
        IS[Infection System] --> BS[Blocks]
    end
    
    subgraph Client
        GUI[GUI System] --> FS
        RP[Rendering Pipeline] --> VS
    end
    
    FS --> NET[Network] --> RP
```

## Data Flow

```
User → GUI → FieldEditState → FieldDefinition → JSON
                                     ↓
                             FieldRegistry
                                     ↓
                             FieldManager → FieldInstance
                                     ↓
                               Network → Other Clients
                                     ↓
                             FieldRenderer → GPU
```
"""
        self.write_file(DOCS_DIR / "ARCHITECTURE.md", content)
    
    def generate_group_summary(self, group_id: str, group: dict):
        """Generate TOP-LEVEL summary file with class diagram."""
        all_classes = self.get_classes(group['all_packages'])
        key_classes = group.get('summary_key_classes', [])
        
        content = f"""# {group['title']}

> {group['description']}

**{len(all_classes)} classes** across {len(group['all_packages'])} packages.

## Architecture

{self.generate_class_diagram(all_classes, key_classes, simplified=True)}

## Modules

| Module | Classes | Description |
|--------|---------|-------------|
"""
        for child_id, child in group['children'].items():
            child_classes = self.get_classes(child['packages'])
            content += f"| [{child['title']}](./{group_id}/{child_id}.md) | {len(child_classes)} | {', '.join(child['packages'])} |\n"
        
        content += f"\n---\n[Back to README](./README.md)\n"
        
        self.write_file(DOCS_DIR / f"{group_id}.md", content)
    
    def generate_detail_file(self, group_id: str, child_id: str, child: dict):
        """Generate detail file inside the folder."""
        classes = self.get_classes(child['packages'])
        if len(classes) < 3:
            return
        
        gen = MermaidGenerator(classes, self.config)
        
        content = f"""# {child['title']}

> Packages: {', '.join(child['packages'])}

**{len(classes)} classes**

## Class Diagram

{gen.generate_class_diagram(classes[:35])}

---
[Back to {GROUPED_SYSTEMS[group_id]['title']}](../{group_id}.md)
"""
        self.write_file(DOCS_DIR / group_id / f"{child_id}.md", content)
    
    def generate_single_system(self, sys_id: str, cfg: dict):
        """Generate single-file system diagram."""
        classes = self.get_classes(cfg['packages'])
        if len(classes) < 3:
            return
        
        gen = MermaidGenerator(classes, self.config)
        
        content = f"""# {cfg['title']}

> {cfg['description']}

**{len(classes)} classes**

## Key Classes

"""
        key = [c for c in classes if c.name in cfg.get('key_classes', [])]
        for c in key:
            ext = f" → `{c.extends}`" if c.extends else ""
            content += f"- **`{c.name}`** ({c.class_type}){ext}\n"
        
        content += f"""
## Class Diagram

{gen.generate_class_diagram(classes[:35])}

---
[Back to README](./README.md)
"""
        self.write_file(DOCS_DIR / f"{sys_id}.md", content)
    
    def clean_old_files(self):
        """Remove old generated files."""
        import shutil
        # Remove old folder structure
        for old_dir in ['main', 'client', 'shared']:
            old_path = DOCS_DIR / old_dir
            if old_path.exists():
                shutil.rmtree(old_path)
                print(f"  🗑️ Removed {old_dir}/")
        
        # Remove old files that don't match new structure
        old_files = ['field_core.md', 'field_effects.md', 'field_system.md', 
                     'gui_system.md', 'gui_panels.md', 'gui_widgets.md', 'gui_state.md']
        for old_file in old_files:
            old_path = DOCS_DIR / old_file
            if old_path.exists():
                old_path.unlink()
                print(f"  🗑️ Removed {old_file}")
    
    def generate_all(self):
        print(f"\n📝 Generating documentation to {DOCS_DIR.relative_to(PROJECT_ROOT)}/")
        
        if not self.dry_run:
            self.clean_old_files()
        
        print("\n📄 Core files:")
        self.generate_readme()
        self.generate_architecture()
        
        print("\n📁 Grouped systems:")
        for group_id, group in GROUPED_SYSTEMS.items():
            # Top-level summary
            self.generate_group_summary(group_id, group)
            
            # Detail files in subfolder
            for child_id, child in group['children'].items():
                self.generate_detail_file(group_id, child_id, child)
        
        print("\n📊 Single-file systems:")
        for sys_id, cfg in SINGLE_SYSTEMS.items():
            classes = self.get_classes(cfg['packages'])
            if len(classes) >= 3:
                self.generate_single_system(sys_id, cfg)
            else:
                print(f"  ⏭️ Skipping {sys_id} ({len(classes)} classes)")
        
        print(f"\n✅ Done! {self.files_written} files written.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    
    print("=" * 60)
    print("   DOCUMENTATION GENERATOR v5")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠️  DRY RUN MODE")
    
    print("\n📂 Scanning project...")
    classes = scan_project(PROJECT_ROOT)
    graph = build_graph(classes)
    print(f"📊 Found {len(classes)} classes")
    
    generator = DocGenerator(graph, dry_run=args.dry_run)
    generator.generate_all()


if __name__ == "__main__":
    main()
