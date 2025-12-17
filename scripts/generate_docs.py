#!/usr/bin/env python3
"""
Documentation Generator v3 - Minimalist
========================================
Generates focused, interconnected documentation.

Output:
- README.md - Navigation
- ARCHITECTURE.md - Real system architecture  
- 8-10 focused CLASS_DIAGRAM files (one per major system)
- No empty folders, no useless READMEs
"""

import sys
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict, Set

SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass, is_valid_java_identifier
from lib.graph_builder import build_graph, ClassGraph
from lib.mermaid_generator import MermaidGenerator, DiagramConfig

PROJECT_ROOT = SCRIPT_DIR.parent
DOCS_DIR = PROJECT_ROOT / "docs"


# =============================================================================
# SYSTEM DEFINITIONS
# Define which packages/classes belong to which "system" for documentation
# =============================================================================

SYSTEMS = {
    "field_system": {
        "title": "Field System",
        "description": "Core field definitions, effects, bindings, triggers, and lifecycle management.",
        "packages": ["field", "field.definition", "field.effect", "field.influence", "field.instance", "field.loader"],
        "key_classes": ["FieldDefinition", "FieldLayer", "FieldManager", "BindingResolver", "TriggerProcessor"],
    },
    "visual_system": {
        "title": "Visual System", 
        "description": "Shape definitions, pattern system, color themes, animations, and fill modes.",
        "packages": ["visual", "visual.shape", "visual.pattern", "visual.animation", "visual.color", "visual.fill", "visual.transform", "visual.layer", "visual.appearance"],
        "key_classes": ["Shape", "QuadPattern", "AnimationConfig", "ColorTheme", "FillMode"],
    },
    "infection_system": {
        "title": "Infection System",
        "description": "Virus spreading logic, infection services, scenario management, and orchestration.",
        "packages": ["infection", "infection.service", "infection.controller", "infection.scenario", "infection.orchestrator", "infection.event", "infection.registry"],
        "key_classes": ["InfectionService", "InfectionController", "ScenarioConfig"],
    },
    "gui_system": {
        "title": "GUI System",
        "description": "Panel hierarchy, widgets, state management, adapters, and screen layouts.",
        "packages": ["client.gui", "client.gui.panel", "client.gui.panel.sub", "client.gui.widget", "client.gui.state", "client.gui.state.adapter", "client.gui.preview", "client.gui.layout", "client.gui.screen"],
        "key_classes": ["AbstractPanel", "FieldCustomizerScreen", "FieldEditState", "LayoutManager"],
    },
    "rendering_pipeline": {
        "title": "Rendering Pipeline",
        "description": "Mesh building, tessellators, primitive renderers, and render layers.",
        "packages": ["client.visual", "client.visual.mesh", "client.visual.tessellator", "client.visual.render", "client.field.render", "client.render"],
        "key_classes": ["MeshBuilder", "SphereTessellator", "FieldRenderer", "LayerRenderer", "PrimitiveRenderer"],
    },
    "blocks_growth": {
        "title": "Blocks & Growth",
        "description": "Custom blocks, block entities, growth system, and collisions.",
        "packages": ["block", "block.corrupted", "block.infected", "block.entity", "block.growth", "growth", "growth.event", "collision", "entity"],
        "key_classes": ["ProgressiveGrowthBlock", "VirusBlock", "GrowthForceHandler"],
    },
    "network_commands": {
        "title": "Network & Commands",
        "description": "Client-server payloads, command system, and packet handlers.",
        "packages": ["network", "network.payload", "command", "command.argument"],
        "key_classes": ["FieldSpawnC2SPayload", "FieldEditUpdateS2CPayload", "FieldCommand"],
    },
    "infrastructure": {
        "title": "Infrastructure",
        "description": "Logging, registries, configuration, utilities, and mixins.",
        "packages": ["log", "registry", "config", "util", "mixin", "mixin.client"],
        "key_classes": ["Logging", "LogScope", "ModConfig"],
    },
}


class DocGenerator:
    """Generates minimalist, interconnected documentation."""
    
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
            # Create package key matching our SYSTEMS
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
    
    def get_system_classes(self, system_config: dict) -> List[JavaClass]:
        """Get all classes belonging to a system."""
        classes = []
        for pkg in system_config["packages"]:
            classes.extend(self.classes_by_package.get(pkg, []))
        # Filter valid
        return [c for c in classes if is_valid_java_identifier(c.name)]
    
    def generate_readme(self):
        """Generate minimal README with navigation."""
        stats = self.graph.get_stats()
        
        content = f"""# 📚 The Virus Block - Documentation

> Auto-generated {datetime.now().strftime('%Y-%m-%d')} | {stats['total_classes']} classes

## Quick Navigation

| System | Classes | Description |
|--------|---------|-------------|
"""
        for sys_id, cfg in SYSTEMS.items():
            classes = self.get_system_classes(cfg)
            if classes:
                content += f"| [{cfg['title']}](./{sys_id}.md) | {len(classes)} | {cfg['description'][:50]}... |\n"
        
        content += f"""
## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for system overview and data flows.

## Regenerate

```bash
python3 scripts/generate_docs.py
```
"""
        self.write_file(DOCS_DIR / "README.md", content)
    
    def generate_architecture(self):
        """Generate REAL architecture documentation."""
        content = f"""# 🏗️ System Architecture

> The Virus Block - Minecraft 1.21.6 Fabric Mod

## Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER/COMMON                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Field     │  │  Infection  │  │        Blocks           │  │
│  │   System    │──│   System    │──│  ProgressiveGrowth      │  │
│  │             │  │             │  │  VirusBlock, Corrupted  │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                      │                │
│  ┌──────┴────────────────┴──────────────────────┴──────────────┐ │
│  │                     Network Layer                            │ │
│  │  Payloads: FieldSpawn, FieldEdit, InfectionSync, GrowthSync │ │
│  └──────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                           CLIENT                                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────────────┐ │
│  │   GUI System    │    │        Rendering Pipeline           │ │
│  │  ┌───────────┐  │    │  ┌────────┐  ┌────────┐  ┌───────┐ │ │
│  │  │ Panels    │  │    │  │ Field  │──│Tessell-│──│ Mesh  │ │ │
│  │  │ Widgets   │  │    │  │Renderer│  │ators   │  │Builder│ │ │
│  │  │ State     │  │    │  └────────┘  └────────┘  └───────┘ │ │
│  │  └───────────┘  │    │        │                           │ │
│  └────────┬────────┘    │  ┌─────┴─────┐                     │ │
│           │             │  │ RenderLayer│ → GPU               │ │
│           └─────────────┼──│  Shaders  │                     │ │
│                         │  └───────────┘                     │ │
│                         └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flows

### Field Creation Flow
```
User Input (GUI)
    │
    ▼
FieldEditState ─────────────────────────┐
    │                                    │
    ▼                                    ▼
FieldDefinition (JSON) ◄──── FieldProfileStore
    │
    ▼
FieldManager.spawn()
    │
    ▼
FieldInstance (runtime) ────► Network ────► Other Clients
    │
    ▼
FieldRenderer.render()
    │
    ▼
LayerRenderer → PrimitiveRenderer → Tessellator → MeshBuilder → GPU
```

### Visual Rendering Pipeline
```
FieldLayer
    ├── Primitive[] ─────────────────────────┐
    │                                         │
    ▼                                         ▼
┌─────────┐     ┌───────────────┐     ┌─────────────┐
│ Shape   │ ──► │  Tessellator  │ ──► │ MeshBuilder │
│(Sphere, │     │(generates mesh│     │ (emits      │
│ Prism)  │     │ geometry)     │     │  vertices)  │
└─────────┘     └───────────────┘     └──────┬──────┘
                                              │
    ┌─────────────────────────────────────────┘
    │
    ▼
┌─────────┐     ┌───────────────┐     ┌─────────────┐
│ Pattern │ ──► │  Fill Mode    │ ──► │ RenderLayer │ ──► GPU
│(vertex  │     │(solid/wire/   │     │ (shaders,   │
│ order)  │     │ cage)         │     │  blend)     │
└─────────┘     └───────────────┘     └─────────────┘
```

### GUI State Management
```
FieldCustomizerScreen
         │
         ▼
    ┌─────────────────────────────────────────┐
    │           FieldEditState                │
    │  (singleton, holds all editing data)    │
    │  ┌────────────────────────────────────┐ │
    │  │ currentLayerIndex                  │ │
    │  │ currentPrimitiveIndex              │ │
    │  │ layerConfigs[]                     │ │
    │  │ isDirty                            │ │
    │  └────────────────────────────────────┘ │
    └─────────────────────────────────────────┘
              │
              ▼
    ┌─────────────────────────────────────────┐
    │            Adapters                     │
    │  ShapeAdapter, FillAdapter, ...         │
    │  (sync UI ↔ state bidirectionally)      │
    └─────────────────────────────────────────┘
```

## Key Entry Points

| Entry Point | Purpose |
|-------------|---------|
| `TheVirusBlock.onInitialize()` | Mod initialization |
| `TheVirusBlockClient.onInitializeClient()` | Client init |
| `FieldCommand` | `/field` commands |
| `FieldRegistry` | Field definition storage |
| `FieldManager` | Runtime field instances |
| `FieldCustomizerScreen` | GUI entry |

## Package Map

```
net.cyberpunk042
├── field/          # Field definitions & runtime
├── visual/         # Visual configs (shapes, patterns)
├── infection/      # Virus spreading system
├── block/          # Custom blocks
├── growth/         # Growth block behavior  
├── network/        # Client ↔ Server packets
├── command/        # Game commands
├── log/            # Logging framework
├── registry/       # Registries
└── config/         # Mod configuration

net.cyberpunk042.client
├── gui/            # Panels, widgets, screens
├── visual/         # Tessellators, mesh builders
├── field/          # Primitive renderers
└── render/         # Render layers, shaders
```
"""
        self.write_file(DOCS_DIR / "ARCHITECTURE.md", content)
    
    def generate_system_diagram(self, sys_id: str, cfg: dict):
        """Generate a focused class diagram for one system."""
        classes = self.get_system_classes(cfg)
        
        if len(classes) < 3:
            # Too small, skip
            return
        
        gen = MermaidGenerator(classes, self.config)
        
        lines = []
        lines.append(f"# {cfg['title']}")
        lines.append("")
        lines.append(f"> {cfg['description']}")
        lines.append("")
        lines.append(f"**{len(classes)} classes** across packages: {', '.join(cfg['packages'][:5])}")
        lines.append("")
        
        # Key classes callout
        key = [c for c in classes if c.name in cfg.get('key_classes', [])]
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
            classes[:35],  # Limit size
            show_methods=True,
            show_fields=True,
            show_inheritance=True,
            show_dependencies=True
        ))
        lines.append("")
        
        # Add inheritance hierarchy for larger systems
        if len(classes) > 10:
            lines.append("## Inheritance Hierarchy")
            lines.append("")
            lines.append(gen.generate_inheritance_hierarchy())
            lines.append("")
        
        self.write_file(DOCS_DIR / f"{sys_id}.md", '\n'.join(lines))
    
    def generate_all(self):
        """Generate all documentation."""
        print(f"\n📝 Generating documentation to {DOCS_DIR.relative_to(PROJECT_ROOT)}/")
        
        # Clean old docs subdirs (but not files in root)
        if not self.dry_run:
            for subdir in ["main", "client", "shared"]:
                old_path = DOCS_DIR / subdir
                if old_path.exists():
                    import shutil
                    shutil.rmtree(old_path)
                    print(f"  🗑️ Removed old {subdir}/")
        
        # Generate
        print("\n📄 Core files:")
        self.generate_readme()
        self.generate_architecture()
        
        print("\n📊 System diagrams:")
        for sys_id, cfg in SYSTEMS.items():
            classes = self.get_system_classes(cfg)
            if len(classes) >= 3:
                self.generate_system_diagram(sys_id, cfg)
            else:
                print(f"  ⏭️ Skipping {sys_id} ({len(classes)} classes)")
        
        print(f"\n✅ Done! {self.files_written} files written.")


def main():
    parser = argparse.ArgumentParser(description="Generate minimalist documentation")
    parser.add_argument('--dry-run', action='store_true', help='Preview only')
    args = parser.parse_args()
    
    print("=" * 60)
    print("   DOCUMENTATION GENERATOR v3 (Minimalist)")
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
