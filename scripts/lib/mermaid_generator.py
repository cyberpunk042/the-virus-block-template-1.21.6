"""
Mermaid Diagram Generator
=========================
Generates Mermaid class diagrams and dependency graphs from parsed Java classes.

Features:
- Inheritance diagrams (extends/implements)
- Dependency diagrams (import-based)
- Automatic splitting for large class sets
- Clean, readable output
"""

import re
from dataclasses import dataclass
from typing import List, Dict, Set, Optional, Tuple
from collections import defaultdict
from datetime import datetime

# Import from siblings
from .java_parser import JavaClass, JavaMethod, sanitize_for_mermaid, is_valid_java_identifier


@dataclass
class DiagramConfig:
    """Configuration for diagram generation."""
    max_classes_per_diagram: int = 30
    show_methods: bool = False  # Show method names in class boxes
    show_fields: bool = False   # Show field names in class boxes
    max_methods_shown: int = 5  # If showing methods, max to display
    include_external: bool = True  # Include external parent classes in diagrams
    show_javadoc: bool = True  # Show javadoc in class details


class MermaidGenerator:
    """Generates Mermaid diagrams from parsed Java classes."""
    
    def __init__(self, classes: List[JavaClass], config: DiagramConfig = None):
        self.classes = classes
        self.config = config or DiagramConfig()
        
        # Build lookup tables using safe names
        self.by_name = {c.name: c for c in classes if is_valid_java_identifier(c.name)}
        self.by_safe_name = {c.safe_name: c for c in classes}
        self.by_fqn = {c.fqn: c for c in classes if is_valid_java_identifier(c.name)}
        self.by_package = defaultdict(list)
        for c in classes:
            if is_valid_java_identifier(c.name):
                self.by_package[c.package].append(c)
    
    def _sanitize_name(self, name: str) -> str:
        """Sanitize a name for Mermaid (remove special chars)."""
        return sanitize_for_mermaid(name)

    
    def _get_class_annotation(self, jc: JavaClass) -> str:
        """Get Mermaid annotation for class type."""
        if jc.class_type == 'interface':
            return '<<interface>>'
        elif jc.class_type == 'enum':
            return '<<enumeration>>'
        elif jc.class_type == 'record':
            return '<<record>>'
        elif jc.is_abstract:
            return '<<abstract>>'
        return ''
    
    def _resolve_type_to_class(self, type_name: str) -> Optional[JavaClass]:
        """Resolve a type name to a JavaClass if it exists in our codebase."""
        # Try direct name lookup
        if type_name in self.by_name:
            return self.by_name[type_name]
        # Try FQN lookup
        if type_name in self.by_fqn:
            return self.by_fqn[type_name]
        return None
    
    def generate_class_diagram(
        self, 
        classes: List[JavaClass] = None,
        title: str = "Class Diagram",
        include_inheritance: bool = True,
        include_dependencies: bool = False
    ) -> str:
        """
        Generate a Mermaid class diagram.
        
        Args:
            classes: Classes to include (defaults to all)
            title: Diagram title
            include_inheritance: Show extends/implements relationships
            include_dependencies: Show import-based dependencies
            
        Returns:
            Mermaid diagram as string
        """
        if classes is None:
            classes = self.classes
        
        # Limit if too many
        if len(classes) > self.config.max_classes_per_diagram:
            classes = classes[:self.config.max_classes_per_diagram]
        
        lines = []
        lines.append("```mermaid")
        lines.append("classDiagram")
        
        # Track what we've defined
        defined_classes = set()
        relationships = []
        
        # Define each class
        for jc in classes:
            name = self._sanitize_name(jc.name)
            if name in defined_classes:
                continue
            defined_classes.add(name)
            
            annotation = self._get_class_annotation(jc)
            
            if self.config.show_methods and jc.public_methods:
                # Class with members
                lines.append(f"    class {name} {{")
                if annotation:
                    lines.append(f"        {annotation}")
                
                # Show public methods
                for method in jc.public_methods[:self.config.max_methods_shown]:
                    ret = self._sanitize_name(method.return_type) if method.return_type else "void"
                    lines.append(f"        +{method.name}() {ret}")
                
                if len(jc.public_methods) > self.config.max_methods_shown:
                    lines.append(f"        +... {len(jc.public_methods) - self.config.max_methods_shown} more")
                
                lines.append("    }")
            else:
                # Simple class definition
                if annotation:
                    lines.append(f"    class {name} {annotation}")
                else:
                    lines.append(f"    class {name}")
        
        # Add inheritance relationships
        if include_inheritance:
            for jc in classes:
                child_name = self._sanitize_name(jc.name)
                if not child_name or child_name == "Unknown":
                    continue
                
                # Extends
                if jc.extends:
                    parent_name = self._sanitize_name(jc.extends)
                    if not parent_name or parent_name == "Unknown":
                        continue
                    
                    # Add parent if not already defined (external class)
                    if parent_name not in defined_classes:
                        if self.config.include_external:
                            lines.append(f"    class {parent_name}")
                            defined_classes.add(parent_name)
                    
                    if parent_name in defined_classes:
                        relationships.append(f"    {parent_name} <|-- {child_name}")
                
                # Implements
                for impl in jc.implements:
                    impl_name = self._sanitize_name(impl)
                    if not impl_name or impl_name == "Unknown":
                        continue
                    
                    if impl_name not in defined_classes:
                        if self.config.include_external:
                            lines.append(f"    class {impl_name} <<interface>>")
                            defined_classes.add(impl_name)
                    
                    if impl_name in defined_classes:
                        relationships.append(f"    {impl_name} <|.. {child_name}")
        
        # Add dependency relationships (uses)
        if include_dependencies:
            for jc in classes:
                src_name = self._sanitize_name(jc.name)
                for imp in jc.internal_imports:
                    tgt_class = self._resolve_type_to_class(imp)
                    if tgt_class:
                        tgt_name = self._sanitize_name(tgt_class.name)
                        if tgt_name in defined_classes and tgt_name != src_name:
                            rel = f"    {src_name} ..> {tgt_name}"
                            if rel not in relationships:
                                relationships.append(rel)
        
        # Add relationships (deduplicated)
        for rel in sorted(set(relationships)):
            lines.append(rel)
        
        lines.append("```")
        
        return '\n'.join(lines)
    
    def generate_inheritance_tree(
        self,
        classes: List[JavaClass] = None,
        title: str = "Inheritance Hierarchy"
    ) -> str:
        """
        Generate an inheritance-focused diagram showing class hierarchies.
        """
        if classes is None:
            classes = self.classes
        
        lines = []
        lines.append("```mermaid")
        lines.append("graph TB")
        
        # Find inheritance relationships
        defined = set()
        
        for jc in classes:
            child = self._sanitize_name(jc.name)
            
            if jc.extends:
                parent = self._sanitize_name(jc.extends)
                # Define nodes with styling
                if parent not in defined:
                    lines.append(f"    {parent}[{parent}]")
                    defined.add(parent)
                if child not in defined:
                    annotation = self._get_class_annotation(jc)
                    if annotation:
                        lines.append(f"    {child}[{child}<br/>{annotation}]")
                    else:
                        lines.append(f"    {child}[{child}]")
                    defined.add(child)
                
                lines.append(f"    {parent} --> {child}")
            
            for impl in jc.implements:
                impl_name = self._sanitize_name(impl)
                if impl_name not in defined:
                    lines.append(f"    {impl_name}([{impl_name}])")  # Rounded for interfaces
                    defined.add(impl_name)
                if child not in defined:
                    lines.append(f"    {child}[{child}]")
                    defined.add(child)
                
                lines.append(f"    {impl_name} -.-> {child}")
        
        lines.append("```")
        
        return '\n'.join(lines)
    
    def generate_package_diagram(
        self,
        packages: List[str] = None,
        title: str = "Package Dependencies"
    ) -> str:
        """
        Generate a package-level dependency diagram.
        """
        if packages is None:
            packages = list(self.by_package.keys())
        
        # Build package dependencies from class imports
        pkg_deps = defaultdict(set)
        for jc in self.classes:
            if jc.package not in packages:
                continue
            for imp in jc.internal_imports:
                # Find target package
                imp_class = self.by_fqn.get(imp)
                if imp_class and imp_class.package != jc.package:
                    if imp_class.package in packages:
                        pkg_deps[jc.package].add(imp_class.package)
        
        lines = []
        lines.append("```mermaid")
        lines.append("graph LR")
        
        # Define package nodes
        defined = set()
        for pkg in packages:
            if not pkg:
                continue
            pkg_id = pkg.replace('.', '_')
            short_name = pkg.split('.')[-1]
            count = len(self.by_package.get(pkg, []))
            if pkg_id not in defined:
                lines.append(f"    {pkg_id}[{short_name}<br/>{count} classes]")
                defined.add(pkg_id)
        
        # Add edges
        for src_pkg, targets in pkg_deps.items():
            src_id = src_pkg.replace('.', '_')
            for tgt_pkg in targets:
                tgt_id = tgt_pkg.replace('.', '_')
                if src_id in defined and tgt_id in defined:
                    lines.append(f"    {src_id} --> {tgt_id}")
        
        lines.append("```")
        
        return '\n'.join(lines)
    
    def generate_domain_overview(self) -> str:
        """
        Generate a high-level domain overview diagram.
        """
        # Group by domain
        domains = defaultdict(list)
        for jc in self.classes:
            domains[jc.domain].append(jc)
        
        lines = []
        lines.append("```mermaid")
        lines.append("graph TB")
        
        # Main subgraph
        lines.append("    subgraph main[Main/Common]")
        for domain in sorted(domains.keys()):
            if domain in ['client', 'mixin', '', 'net']:
                continue
            count = len(domains[domain])
            main_count = len([c for c in domains[domain] if c.source_root == 'main'])
            if main_count > 0:
                lines.append(f"        {domain}[{domain}<br/>{main_count}]")
        lines.append("    end")
        
        # Client subgraph
        lines.append("    subgraph client[Client-Side]")
        client_classes = domains.get('client', [])
        # Group by subdomain
        by_sub = defaultdict(list)
        for c in client_classes:
            by_sub[c.subdomain or 'other'].append(c)
        for sub in sorted(by_sub.keys()):
            count = len(by_sub[sub])
            lines.append(f"        client_{sub}[{sub}<br/>{count}]")
        lines.append("    end")
        
        # Shared subgraph
        lines.append("    subgraph shared[Shared]")
        for domain in ['mixin', 'util', 'config', 'registry']:
            if domain in domains:
                count = len(domains[domain])
                lines.append(f"        {domain}[{domain}<br/>{count}]")
        lines.append("    end")
        
        lines.append("```")
        
        return '\n'.join(lines)


def generate_class_diagram_md(
    classes: List[JavaClass],
    title: str = "Class Diagram",
    description: str = "",
    include_inheritance: bool = True,
    include_dependencies: bool = True,
    config: DiagramConfig = None
) -> str:
    """
    Generate a complete markdown file with class diagram(s).
    
    Args:
        classes: Classes to include
        title: Page title
        description: Optional description text
        include_inheritance: Show inheritance diagram
        include_dependencies: Show dependency diagram
        config: Diagram configuration
        
    Returns:
        Complete markdown file content
    """
    config = config or DiagramConfig()
    
    # Filter out classes with invalid names
    valid_classes = [c for c in classes if is_valid_java_identifier(c.name)]
    
    if not valid_classes:
        return f"# {title}\n\n> No valid classes found.\n"
    
    generator = MermaidGenerator(valid_classes, config)
    
    lines = []
    lines.append(f"# {title}")
    lines.append("")
    lines.append(f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    lines.append("")
    
    if description:
        lines.append(description)
        lines.append("")
    
    # Statistics
    lines.append("## Overview")
    lines.append("")
    lines.append(f"- **Total Classes:** {len([c for c in valid_classes if c.class_type == 'class'])}")
    lines.append(f"- **Interfaces:** {len([c for c in valid_classes if c.is_interface])}")
    lines.append(f"- **Enums:** {len([c for c in valid_classes if c.is_enum])}")
    lines.append(f"- **Records:** {len([c for c in valid_classes if c.class_type == 'record'])}")
    lines.append("")
    
    # Inheritance diagram
    if include_inheritance:
        lines.append("## Class Hierarchy")
        lines.append("")
        lines.append(generator.generate_class_diagram(
            valid_classes, 
            include_inheritance=True, 
            include_dependencies=False
        ))
        lines.append("")
    
    # Dependency diagram (if not too many classes)
    if include_dependencies and len(valid_classes) <= config.max_classes_per_diagram:
        lines.append("## Dependencies")
        lines.append("")
        lines.append(generator.generate_class_diagram(
            valid_classes,
            include_inheritance=False,
            include_dependencies=True
        ))
        lines.append("")
    
    # Class list with details
    lines.append("## Class Details")
    lines.append("")
    
    for jc in sorted(valid_classes, key=lambda x: x.name):
        lines.append(f"### `{jc.name}`")
        lines.append("")
        lines.append(f"**Type:** {jc.class_type}")
        if jc.extends:
            lines.append(f"**Extends:** `{jc.extends}`")
        if jc.implements:
            lines.append(f"**Implements:** {', '.join(f'`{i}`' for i in jc.implements)}")
        if jc.annotations:
            lines.append(f"**Annotations:** {', '.join(f'@{a}' for a in jc.annotations)}")
        lines.append("")
        
        # Include javadoc if available
        if jc.javadoc and config.show_javadoc:
            # Truncate long javadocs
            doc = jc.javadoc[:300] + "..." if len(jc.javadoc) > 300 else jc.javadoc
            lines.append(f"> {doc}")
            lines.append("")
        
        # Show key public methods
        if jc.public_methods:
            method_names = [m.name for m in jc.public_methods[:8]]
            lines.append(f"**Key Methods:** {', '.join(f'`{m}()`' for m in method_names)}")
            lines.append("")
    
    return '\n'.join(lines)




def generate_readme_md(
    title: str,
    description: str,
    subdirs: List[Tuple[str, str, int]],  # (name, description, class_count)
    key_classes: List[JavaClass] = None
) -> str:
    """
    Generate a README.md for a documentation folder.
    
    Args:
        title: Page title
        description: Description text
        subdirs: List of (name, description, class_count) tuples
        key_classes: Optional list of key classes to highlight
        
    Returns:
        Complete markdown file content
    """
    lines = []
    lines.append(f"# {title}")
    lines.append("")
    lines.append(f"> Auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    lines.append("")
    lines.append(description)
    lines.append("")
    
    if subdirs:
        lines.append("## Components")
        lines.append("")
        lines.append("| Folder | Description | Classes |")
        lines.append("|--------|-------------|---------|")
        for name, desc, count in subdirs:
            lines.append(f"| [{name}/](./{name}/) | {desc} | {count} |")
        lines.append("")
    
    if key_classes:
        lines.append("## Key Classes")
        lines.append("")
        for jc in key_classes[:10]:
            lines.append(f"- **`{jc.name}`** - {jc.class_type}")
        lines.append("")
    
    lines.append("## See Also")
    lines.append("")
    lines.append("- [CLASS_DIAGRAM.md](./CLASS_DIAGRAM.md) - Visual class hierarchy")
    lines.append("")
    
    return '\n'.join(lines)
