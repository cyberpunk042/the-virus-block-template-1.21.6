#!/usr/bin/env python3
"""Add toBuilder() methods to all Shape records."""

import re
import os

SHAPE_DIR = "src/main/java/net/cyberpunk042/visual/shape"

# Map each shape to its record fields (in order) from the record definition
SHAPES = {
    "DiscShape": ["radius", "segments", "y", "arcStart", "arcEnd", "innerRadius", "rings"],
    "SphereShape": ["radius", "latSteps", "lonSteps", "latStart", "latEnd", "lonStart", "lonEnd", "algorithm"],
    "PrismShape": ["sides", "radius", "height", "topRadius", "twist", "heightSegments", "capTop", "capBottom"],
    "CylinderShape": ["radius", "height", "segments", "topRadius", "heightSegments", "capTop", "capBottom", "arc"],
    "PolyhedronShape": ["polyType", "radius", "subdivisions"],
}

def generate_to_builder(shape_name, fields):
    """Generate toBuilder() method for a shape."""
    lines = [
        "",
        "    /** Create a builder pre-populated with this shape's values. */",
        "    public Builder toBuilder() {",
        "        return new Builder()",
    ]
    for i, field in enumerate(fields):
        suffix = ";" if i == len(fields) - 1 else ""
        lines.append(f"            .{field}({field}){suffix}")
    lines.append("    }")
    return "\n".join(lines)

def add_to_builder_to_file(filepath, shape_name, fields):
    """Add toBuilder() method to a shape file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if already has toBuilder
    if "public Builder toBuilder()" in content:
        print(f"  {shape_name}: already has toBuilder(), skipping")
        return False
    
    # Find the line "public static Builder builder()" and insert after it
    pattern = r'(    public static Builder builder\(\) \{ return new Builder\(\); \})'
    
    to_builder_code = generate_to_builder(shape_name, fields)
    
    if re.search(pattern, content):
        new_content = re.sub(pattern, r'\1' + to_builder_code, content)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"  {shape_name}: added toBuilder()")
        return True
    else:
        print(f"  {shape_name}: couldn't find builder() method!")
        return False

def main():
    print("Adding toBuilder() to shape records...")
    
    for shape_name, fields in SHAPES.items():
        filepath = os.path.join(SHAPE_DIR, f"{shape_name}.java")
        if os.path.exists(filepath):
            add_to_builder_to_file(filepath, shape_name, fields)
        else:
            print(f"  {shape_name}: file not found at {filepath}")
    
    print("\nDone!")

if __name__ == "__main__":
    main()

