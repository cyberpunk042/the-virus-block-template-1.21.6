#!/usr/bin/env python3
"""
Documentation Generator
=======================
Generates complete documentation structure with Mermaid diagrams.

Phase 4 of the documentation generation pipeline.

Usage:
    python scripts/generate_docs.py           # Generate all docs
    python scripts/generate_docs.py --dry-run # Preview without writing
"""

import sys
import json
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict

# Add scripts to path
SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass
from lib.graph_builder import build_graph, ClassGraph
from lib.mermaid_generator import (
    MermaidGenerator, 
    DiagramConfig,
    generate_class_diagram_md,
    generate_readme_md
)


# Configuration
MAX_CLASSES_PER_DIAGRAM = 30
PROJECT_ROOT = SCRIPT_DIR.parent
DOCS_DIR = PROJECT_ROOT / "docs"


class DocGenerator:
    """Generates documentation from parsed Java classes."""
    
    def __init__(self, graph: ClassGraph, dry_run: bool = False):
        self.graph = graph
        self.dry_run = dry_run
        self.files_written = 0
        self.config = DiagramConfig(max_classes_per_diagram=MAX_CLASSES_PER_DIAGRAM)
    
    def write_file(self, path: Path, content: str):
        """Write a file (respecting dry_run mode)."""
        if self.dry_run:
            print(f"  [DRY-RUN] Would write: {path.relative_to(PROJECT_ROOT)} ({len(content)} bytes)")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
            print(f"  ✅ Wrote: {path.relative_to(PROJECT_ROOT)}")
        self.files_written += 1
    
    def generate_main_readme(self):
        """Generate the main docs/README.md."""
        stats = self.graph.get_stats()
        domain_stats = self.graph.get_domain_stats()
        
        content = f"""# 📚 The Virus Block - Documentation

> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}

## Overview

This documentation is automatically generated from the Java source code.

### Statistics

| Metric | Value |
|--------|-------|
| **Total Classes** | {stats['total_classes']} |
| **Interfaces** | {stats['interfaces']} |
| **Enums** | {stats['enums']} |
| **Packages** | {stats['packages']} |
| **Main-side** | {stats['main_classes']} classes |
| **Client-side** | {stats['client_classes']} classes |

## Structure

### Main (Common/Server)

Server-side and shared logic.

| Domain | Classes | Description |
|--------|---------|-------------|
| [field/](./main/field/) | {domain_stats.get('field', {}).get('total', 0)} | Field system - core gameplay mechanic |
| [visual/](./main/visual/) | {domain_stats.get('visual', {}).get('total', 0)} | Visual definitions - shapes, patterns, animations |
| [infection/](./main/infection/) | {domain_stats.get('infection', {}).get('total', 0)} | Infection system - virus spreading logic |
| [block/](./main/block/) | {domain_stats.get('block', {}).get('total', 0)} | Custom blocks |
| [growth/](./main/growth/) | {domain_stats.get('growth', {}).get('total', 0)} | Growth block system |
| [network/](./main/network/) | {domain_stats.get('network', {}).get('total', 0)} | Networking payloads |
| [command/](./main/command/) | {domain_stats.get('command', {}).get('total', 0)} | Commands |

### Client

Client-side rendering and GUI.

| Domain | Classes | Description |
|--------|---------|-------------|
| [gui/](./client/gui/) | 119 | GUI system - panels, widgets, state |
| [visual/](./client/visual/) | 28 | Client-side visual rendering |
| [render/](./client/render/) | 21 | Rendering utilities |
| [field/](./client/field/) | 17 | Client-side field management |

### Shared

Cross-cutting concerns.

| Domain | Classes | Description |
|--------|---------|-------------|
| [mixin/](./shared/mixin/) | {domain_stats.get('mixin', {}).get('total', 0)} | Mixin classes |
| [util/](./shared/util/) | {domain_stats.get('util', {}).get('total', 0)} | Utilities |
| [config/](./shared/config/) | {domain_stats.get('config', {}).get('total', 0)} | Configuration |

## Regenerating Documentation

```bash
# From WSL
python3 scripts/generate_docs.py

# Dry run (preview only)
python3 scripts/generate_docs.py --dry-run
```

## See Also

- [ARCHITECTURE.md](./ARCHITECTURE.md) - High-level system architecture
"""
        
        self.write_file(DOCS_DIR / "README.md", content)
    
    def generate_architecture(self):
        """Generate docs/ARCHITECTURE.md with high-level overview."""
        generator = MermaidGenerator(list(self.graph.classes.values()), self.config)
        
        content = f"""# 🏗️ System Architecture

> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}

## High-Level Overview

{generator.generate_domain_overview()}

## Package Structure

### Main (Server/Common)

```
net.cyberpunk042
├── field/          # Field system core
│   ├── definition/ # Field definitions
│   ├── effect/     # Field effects
│   ├── influence/  # Trigger and lifecycle
│   ├── instance/   # Runtime instances
│   └── loader/     # JSON loading
│
├── visual/         # Visual definitions
│   ├── animation/  # Animation configs
│   ├── shape/      # Shape definitions
│   ├── pattern/    # Pattern system
│   ├── fill/       # Fill modes
│   └── transform/  # Transformations
│
├── infection/      # Virus/Infection system
│   ├── service/    # Core services
│   ├── controller/ # Tick controllers
│   ├── scenario/   # Scenario configs
│   └── orchestrator/ # Phase management
│
├── block/          # Custom blocks
├── growth/         # Growth block system
├── network/        # Packet payloads
└── command/        # Game commands
```

### Client

```
net.cyberpunk042.client
├── gui/            # GUI system
│   ├── panel/      # UI panels
│   ├── widget/     # Reusable widgets
│   ├── state/      # UI state management
│   ├── preview/    # Field preview renderer
│   └── screen/     # Screen classes
│
├── visual/         # Client-side visuals
│   ├── mesh/       # Mesh builders
│   ├── render/     # Render layers
│   └── tessellator/ # Tessellators
│
├── field/          # Client field logic
│   └── render/     # Primitive renderers
│
└── render/         # Rendering utilities
```

## Key Components

### Field System

The field system is the core gameplay mechanic. Fields are visual effects that can be spawned in the world with various shapes, animations, and behaviors.

```
FieldDefinition → FieldLayer → Primitives → Visual Rendering
       ↓              ↓            ↓
   JSON Config    Per-layer    Shape + Fill + Animation
                  settings
```

### Visual Pipeline

```
Shape (Sphere, Prism, etc.)
   ↓
Tessellator (generates mesh)
   ↓
Pattern (vertex arrangement)
   ↓
Fill Mode (solid, wireframe, cage)
   ↓
RenderLayer → GPU
```

### GUI Architecture

```
FieldCustomizerScreen
   ↓
TabNavigation → Panels
   ↓
FieldEditState (singleton)
   ↓
Adapters → Individual configs
```
"""
        
        self.write_file(DOCS_DIR / "ARCHITECTURE.md", content)
    
    def generate_domain_docs(self, domain: str, folder: str, title: str):
        """Generate documentation for a domain."""
        classes = self.graph.get_domain_classes(domain)
        if not classes:
            return
        
        doc_path = DOCS_DIR / folder
        
        # Group by subdomain
        by_subdomain = defaultdict(list)
        for c in classes:
            sub = c.subdomain if c.subdomain else '_root'
            by_subdomain[sub].append(c)
        
        # Generate README
        subdirs = []
        for sub in sorted(by_subdomain.keys()):
            if sub == '_root':
                continue
            count = len(by_subdomain[sub])
            if count >= 3:
                subdirs.append((sub, f"{sub.title()} components", count))
        
        readme_content = generate_readme_md(
            title=title,
            description=f"This module contains {len(classes)} classes for the {domain} system.",
            subdirs=subdirs,
            key_classes=sorted(classes, key=lambda x: len(x.public_methods), reverse=True)[:5]
        )
        self.write_file(doc_path / "README.md", readme_content)
        
        # Generate main CLASS_DIAGRAM
        diagram_content = generate_class_diagram_md(
            classes=classes[:MAX_CLASSES_PER_DIAGRAM],
            title=f"{title} - Class Diagram",
            description=f"Class hierarchy for the {domain} domain.",
            config=self.config
        )
        self.write_file(doc_path / "CLASS_DIAGRAM.md", diagram_content)
        
        # Generate subdomain diagrams
        for sub, sub_classes in by_subdomain.items():
            if sub == '_root' or len(sub_classes) < 3:
                continue
            
            sub_diagram = generate_class_diagram_md(
                classes=sub_classes,
                title=f"{sub.title()} Classes",
                description=f"Classes in the {domain}.{sub} package.",
                config=self.config
            )
            self.write_file(doc_path / sub / "CLASS_DIAGRAM.md", sub_diagram)
    
    def generate_client_docs(self):
        """Generate client-side documentation with proper subdomain splitting."""
        client_classes = self.graph.get_domain_classes('client')
        
        # Group by subdomain
        by_subdomain = defaultdict(list)
        for c in client_classes:
            sub = c.subdomain if c.subdomain else '_other'
            by_subdomain[sub].append(c)
        
        client_path = DOCS_DIR / "client"
        
        # Generate each subdomain
        for sub, classes in sorted(by_subdomain.items()):
            if sub.startswith('_'):
                continue
            
            sub_path = client_path / sub
            
            # Generate CLASS_DIAGRAM
            diagram_content = generate_class_diagram_md(
                classes=classes[:MAX_CLASSES_PER_DIAGRAM],
                title=f"Client {sub.title()}",
                description=f"Client-side {sub} classes.",
                config=self.config
            )
            self.write_file(sub_path / "CLASS_DIAGRAM.md", diagram_content)
            
            # For large subdomains like gui, generate sub-subdomains
            if len(classes) > MAX_CLASSES_PER_DIAGRAM:
                by_sub_sub = defaultdict(list)
                for c in classes:
                    parts = c.relative_package.split('.')
                    sub_sub = parts[2] if len(parts) > 2 else '_root'
                    by_sub_sub[sub_sub].append(c)
                
                for sub_sub, sub_sub_classes in by_sub_sub.items():
                    if sub_sub.startswith('_') or len(sub_sub_classes) < 3:
                        continue
                    
                    sub_sub_diagram = generate_class_diagram_md(
                        classes=sub_sub_classes,
                        title=f"{sub.title()} - {sub_sub.title()}",
                        description=f"Classes in client.{sub}.{sub_sub}",
                        config=self.config
                    )
                    self.write_file(sub_path / sub_sub / "CLASS_DIAGRAM.md", sub_sub_diagram)
    
    def generate_all(self):
        """Generate all documentation."""
        print("\n📝 Generating documentation...")
        
        # Main files
        print("\n🏠 Main documentation files:")
        self.generate_main_readme()
        self.generate_architecture()
        
        # Main domain docs
        print("\n📦 Main domain documentation:")
        domain_configs = [
            ('field', 'main/field', 'Field System'),
            ('visual', 'main/visual', 'Visual Definitions'),
            ('infection', 'main/infection', 'Infection System'),
            ('block', 'main/block', 'Blocks'),
            ('growth', 'main/growth', 'Growth System'),
            ('command', 'main/command', 'Commands'),
            ('network', 'main/network', 'Networking'),
            ('entity', 'main/entity', 'Entities'),
            ('item', 'main/item', 'Items'),
            ('collision', 'main/collision', 'Collision'),
        ]
        
        for domain, folder, title in domain_configs:
            self.generate_domain_docs(domain, folder, title)
        
        # Client docs (special handling)
        print("\n🖥️ Client documentation:")
        self.generate_client_docs()
        
        # Shared docs
        print("\n🔗 Shared documentation:")
        shared_configs = [
            ('mixin', 'shared/mixin', 'Mixins'),
            ('util', 'shared/util', 'Utilities'),
            ('config', 'shared/config', 'Configuration'),
            ('log', 'shared/log', 'Logging'),
            ('registry', 'shared/registry', 'Registries'),
        ]
        
        for domain, folder, title in shared_configs:
            self.generate_domain_docs(domain, folder, title)
        
        print(f"\n✅ Documentation generation complete!")
        print(f"   Files {'that would be ' if self.dry_run else ''}written: {self.files_written}")


def main():
    parser = argparse.ArgumentParser(description="Generate documentation from Java source")
    parser.add_argument('--dry-run', action='store_true', help='Preview without writing files')
    args = parser.parse_args()
    
    print("=" * 60)
    print("   DOCUMENTATION GENERATOR")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠️  DRY RUN MODE - No files will be written")
    
    # Scan project
    print("\n📂 Scanning project...")
    classes = scan_project(PROJECT_ROOT)
    graph = build_graph(classes)
    
    print(f"\n📊 Found {len(classes)} classes across {len(graph.get_domains())} domains")
    
    # Generate documentation
    generator = DocGenerator(graph, dry_run=args.dry_run)
    generator.generate_all()
    
    if not args.dry_run:
        print(f"\n📁 Documentation written to: {DOCS_DIR.relative_to(PROJECT_ROOT)}/")


if __name__ == "__main__":
    main()
