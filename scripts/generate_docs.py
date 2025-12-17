#!/usr/bin/env python3
"""
Documentation Generator v2
===========================
Generates documentation from Java source code.

Thresholds:
- Domain < 20 classes: Single CLASS_DIAGRAM.md
- Domain 20-80 classes: README + CLASS_DIAGRAM
- Domain > 80 classes: README + CLASS_DIAGRAM + subdomain splits

Usage:
    python scripts/generate_docs.py           # Generate all docs
    python scripts/generate_docs.py --dry-run # Preview without writing
"""

import sys
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict

SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from lib.java_parser import scan_project, JavaClass, is_valid_java_identifier
from lib.graph_builder import build_graph, ClassGraph
from lib.mermaid_generator import (
    MermaidGenerator, 
    DiagramConfig,
    generate_class_diagram_md,
    generate_readme_md
)


# Thresholds
MIN_FOR_SUBFOLDER = 20      # Only split domain if > this many classes
MIN_FOR_SUB_SUBFOLDER = 80  # Only split further if > this many classes
MAX_CLASSES_PER_DIAGRAM = 40  # Max classes in a single mermaid diagram

PROJECT_ROOT = SCRIPT_DIR.parent
DOCS_DIR = PROJECT_ROOT / "docs"


class DocGenerator:
    """Generates documentation from parsed Java classes."""
    
    def __init__(self, graph: ClassGraph, dry_run: bool = False):
        self.graph = graph
        self.dry_run = dry_run
        self.files_written = 0
        self.config = DiagramConfig(
            max_classes_per_diagram=MAX_CLASSES_PER_DIAGRAM,
            include_external=True,  # Show external parent classes
            show_javadoc=True
        )
    
    def write_file(self, path: Path, content: str):
        """Write a file (respecting dry_run mode)."""
        if self.dry_run:
            print(f"  [DRY-RUN] Would write: {path.relative_to(PROJECT_ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
            print(f"  ✅ {path.relative_to(PROJECT_ROOT)}")
        self.files_written += 1
    
    def generate_main_readme(self):
        """Generate the main docs/README.md."""
        stats = self.graph.get_stats()
        domain_stats = self.graph.get_domain_stats()
        
        # Build domain table
        main_domains = []
        client_domains = []
        shared_domains = []
        
        for domain, s in sorted(domain_stats.items()):
            if domain in ['', 'net']:
                continue
            if domain == 'client':
                continue  # Handle separately
            if domain in ['mixin', 'util', 'config', 'log', 'registry']:
                shared_domains.append((domain, s['total']))
            elif s['main'] > 0:
                main_domains.append((domain, s['total']))
        
        content = f"""# 📚 The Virus Block - Documentation

> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}

## Overview

| Metric | Value |
|--------|-------|
| **Total Classes** | {stats['total_classes']} |
| **Main-side** | {stats['main_classes']} |
| **Client-side** | {stats['client_classes']} |
| **Packages** | {stats['packages']} |

## Main Systems

| Domain | Classes | Description |
|--------|---------|-------------|
| [field](./main/field/) | {domain_stats.get('field', {}).get('total', 0)} | Field system - core visual effects |
| [visual](./main/visual/) | {domain_stats.get('visual', {}).get('total', 0)} | Visual definitions - shapes, patterns |
| [infection](./main/infection/) | {domain_stats.get('infection', {}).get('total', 0)} | Infection/virus spreading system |
| [block](./main/block/) | {domain_stats.get('block', {}).get('total', 0)} | Custom blocks |
| [growth](./main/growth/) | {domain_stats.get('growth', {}).get('total', 0)} | Growth block system |
| [network](./main/network/) | {domain_stats.get('network', {}).get('total', 0)} | Network payloads |
| [command](./main/command/) | {domain_stats.get('command', {}).get('total', 0)} | Commands |

## Client Systems

| Domain | Classes | Description |
|--------|---------|-------------|
| [gui](./client/gui/) | ~120 | GUI panels, widgets, state |
| [visual](./client/visual/) | ~28 | Client rendering |
| [render](./client/render/) | ~21 | Render utilities |
| [field](./client/field/) | ~17 | Client field management |

## See Also

- [ARCHITECTURE.md](./ARCHITECTURE.md) - System overview

## Regenerating

```bash
python3 scripts/generate_docs.py
```
"""
        
        self.write_file(DOCS_DIR / "README.md", content)
    
    def generate_architecture(self):
        """Generate docs/ARCHITECTURE.md."""
        generator = MermaidGenerator(list(self.graph.classes.values()), self.config)
        
        content = f"""# 🏗️ System Architecture

> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}

## Overview

{generator.generate_domain_overview()}

## Main Packages

```
net.cyberpunk042
├── field/          # Field system (visual effects in world)
├── visual/         # Visual configs (shapes, animations)
├── infection/      # Virus spreading system
├── block/          # Custom blocks
├── growth/         # Growth block behavior
├── network/        # Client/server packets
└── command/        # Game commands
```

## Client Packages

```
net.cyberpunk042.client
├── gui/            # GUI system
├── visual/         # Client-side rendering
├── render/         # Render layers & utilities
└── field/          # Client field management
```

## Key Flows

### Field Creation
```
FieldDefinition (JSON) → FieldManager → FieldInstance → Renderer
```

### Visual Rendering
```
Shape → Tessellator → Mesh → Pattern → Fill → GPU
```

### GUI State
```
FieldCustomizerScreen → Panels → FieldEditState → Adapters
```
"""
        
        self.write_file(DOCS_DIR / "ARCHITECTURE.md", content)
    
    def generate_domain_docs(self, domain: str, folder: str, title: str):
        """Generate documentation for a single domain."""
        classes = self.graph.get_domain_classes(domain)
        if not classes:
            return
        
        # Filter to valid classes only
        classes = [c for c in classes if is_valid_java_identifier(c.name)]
        if not classes:
            return
        
        doc_path = DOCS_DIR / folder
        class_count = len(classes)
        
        # Small domain: just CLASS_DIAGRAM.md in parent folder
        if class_count <= MIN_FOR_SUBFOLDER:
            diagram_content = generate_class_diagram_md(
                classes=classes,
                title=title,
                description=f"{class_count} classes in the {domain} module.",
                config=self.config
            )
            self.write_file(doc_path / "CLASS_DIAGRAM.md", diagram_content)
            return
        
        # Medium/Large domain: README + CLASS_DIAGRAM
        # Group by subdomain
        by_subdomain = defaultdict(list)
        for c in classes:
            sub = c.subdomain if c.subdomain else '_root'
            by_subdomain[sub].append(c)
        
        # Build subdirs list for README
        subdirs = []
        for sub in sorted(by_subdomain.keys()):
            if sub.startswith('_'):
                continue
            count = len(by_subdomain[sub])
            if count >= 3:
                subdirs.append((sub, f"{sub.title()} package", count))
        
        # Generate README
        readme_content = generate_readme_md(
            title=title,
            description=f"Contains {class_count} classes for the {domain} system.",
            subdirs=subdirs if class_count > MIN_FOR_SUB_SUBFOLDER else [],
            key_classes=sorted(classes, key=lambda x: len(x.public_methods), reverse=True)[:8]
        )
        self.write_file(doc_path / "README.md", readme_content)
        
        # Generate main CLASS_DIAGRAM with top-level + root classes
        root_classes = by_subdomain.get('_root', [])
        main_classes = root_classes[:MAX_CLASSES_PER_DIAGRAM] if root_classes else classes[:MAX_CLASSES_PER_DIAGRAM]
        
        diagram_content = generate_class_diagram_md(
            classes=main_classes,
            title=f"{title} - Core Classes",
            description=f"Core classes in {domain}.",
            config=self.config
        )
        self.write_file(doc_path / "CLASS_DIAGRAM.md", diagram_content)
        
        # Only split into subfolders if domain is large enough
        if class_count > MIN_FOR_SUB_SUBFOLDER:
            for sub, sub_classes in by_subdomain.items():
                if sub.startswith('_') or len(sub_classes) < 5:
                    continue
                
                sub_diagram = generate_class_diagram_md(
                    classes=sub_classes,
                    title=f"{title} - {sub.title()}",
                    description=f"Classes in {domain}.{sub}",
                    config=self.config
                )
                self.write_file(doc_path / sub / "CLASS_DIAGRAM.md", sub_diagram)
    
    def generate_client_docs(self):
        """Generate client-side documentation."""
        client_classes = self.graph.get_domain_classes('client')
        client_classes = [c for c in client_classes if is_valid_java_identifier(c.name)]
        
        # Group by subdomain (gui, visual, render, field, etc.)
        by_subdomain = defaultdict(list)
        for c in client_classes:
            sub = c.subdomain if c.subdomain else '_other'
            by_subdomain[sub].append(c)
        
        client_path = DOCS_DIR / "client"
        
        for sub, classes in sorted(by_subdomain.items()):
            if sub.startswith('_'):
                continue
            
            class_count = len(classes)
            sub_path = client_path / sub
            
            # Small subdomain
            if class_count <= MIN_FOR_SUBFOLDER:
                diagram_content = generate_class_diagram_md(
                    classes=classes,
                    title=f"Client - {sub.title()}",
                    description=f"{class_count} classes.",
                    config=self.config
                )
                self.write_file(sub_path / "CLASS_DIAGRAM.md", diagram_content)
                continue
            
            # Large subdomain (like gui) - may need further splitting
            by_sub_sub = defaultdict(list)
            for c in classes:
                parts = c.relative_package.split('.')
                sub_sub = parts[2] if len(parts) > 2 else '_root'
                by_sub_sub[sub_sub].append(c)
            
            # README for large subdomains
            subdirs = [(ss, f"{ss.title()} package", len(cs)) 
                      for ss, cs in sorted(by_sub_sub.items()) 
                      if not ss.startswith('_') and len(cs) >= 3]
            
            readme_content = generate_readme_md(
                title=f"Client - {sub.title()}",
                description=f"Contains {class_count} classes.",
                subdirs=subdirs if class_count > MIN_FOR_SUB_SUBFOLDER else [],
                key_classes=sorted(classes, key=lambda x: len(x.public_methods), reverse=True)[:5]
            )
            self.write_file(sub_path / "README.md", readme_content)
            
            # Main CLASS_DIAGRAM
            root_classes = by_sub_sub.get('_root', [])
            main_classes = root_classes if root_classes else classes[:MAX_CLASSES_PER_DIAGRAM]
            diagram_content = generate_class_diagram_md(
                classes=main_classes[:MAX_CLASSES_PER_DIAGRAM],
                title=f"Client - {sub.title()}",
                description=f"Core {sub} classes.",
                config=self.config
            )
            self.write_file(sub_path / "CLASS_DIAGRAM.md", diagram_content)
            
            # Sub-subdomain diagrams only for very large (>80)
            if class_count > MIN_FOR_SUB_SUBFOLDER:
                for sub_sub, sub_sub_classes in by_sub_sub.items():
                    if sub_sub.startswith('_') or len(sub_sub_classes) < 5:
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
        print(f"   Thresholds: split at >{MIN_FOR_SUBFOLDER}, sub-split at >{MIN_FOR_SUB_SUBFOLDER}")
        
        # Main files
        print("\n🏠 Top-level files:")
        self.generate_main_readme()
        self.generate_architecture()
        
        # Main domains
        print("\n📦 Main domains:")
        domains = [
            ('field', 'main/field', 'Field System'),
            ('visual', 'main/visual', 'Visual System'),
            ('infection', 'main/infection', 'Infection System'),
            ('block', 'main/block', 'Blocks'),
            ('growth', 'main/growth', 'Growth System'),
            ('command', 'main/command', 'Commands'),
            ('network', 'main/network', 'Networking'),
            ('entity', 'main/entity', 'Entities'),
            ('item', 'main/item', 'Items'),
        ]
        
        for domain, folder, title in domains:
            self.generate_domain_docs(domain, folder, title)
        
        # Client
        print("\n🖥️ Client:")
        self.generate_client_docs()
        
        # Shared
        print("\n🔗 Shared:")
        shared = [
            ('mixin', 'shared/mixin', 'Mixins'),
            ('util', 'shared/util', 'Utilities'),
            ('config', 'shared/config', 'Configuration'),
            ('log', 'shared/log', 'Logging'),
            ('registry', 'shared/registry', 'Registries'),
        ]
        
        for domain, folder, title in shared:
            self.generate_domain_docs(domain, folder, title)
        
        print(f"\n✅ Done! {self.files_written} files {'would be ' if self.dry_run else ''}written.")


def main():
    parser = argparse.ArgumentParser(description="Generate documentation from Java source")
    parser.add_argument('--dry-run', action='store_true', help='Preview without writing files')
    args = parser.parse_args()
    
    print("=" * 60)
    print("   DOCUMENTATION GENERATOR v2")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠️  DRY RUN MODE")
    
    print("\n📂 Scanning project...")
    classes = scan_project(PROJECT_ROOT)
    graph = build_graph(classes)
    
    print(f"\n📊 {len(classes)} classes, {len(graph.get_domains())} domains")
    
    generator = DocGenerator(graph, dry_run=args.dry_run)
    generator.generate_all()


if __name__ == "__main__":
    main()
