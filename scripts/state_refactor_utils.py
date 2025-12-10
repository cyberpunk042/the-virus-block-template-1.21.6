#!/usr/bin/env python3
"""
Utility module for refactoring FieldEditState categories.
Uses existing record classes with toBuilder() pattern.
"""

from dataclasses import dataclass, field
from typing import List, Optional
import re

@dataclass
class FieldDef:
    """Definition of a field in a record."""
    name: str           # e.g., "latSteps"
    type: str           # e.g., "int", "float", "String", "SphereAlgorithm"
    json_key: str       # e.g., "sphereLatSteps" (for JSON serialization)
    getter_prefix: str = "get"  # "get" or "is" for booleans
    
    def getter_name(self):
        prefix = self.getter_prefix
        return f"{prefix}{self.name[0].upper()}{self.name[1:]}"
    
    def setter_name(self):
        return f"set{self.name[0].upper()}{self.name[1:]}"


@dataclass 
class RecordDef:
    """Definition of a record class to wrap."""
    class_name: str              # e.g., "SpinConfig"
    package: str                 # e.g., "net.cyberpunk042.visual.animation"
    default_value: str           # e.g., "SpinConfig.NONE" or "SpinConfig.builder().build()"
    fields: List[FieldDef]       # Fields in the record
    has_builder: bool = True     # Whether it has toBuilder()
    extra_imports: List[str] = field(default_factory=list)  # Additional imports needed


@dataclass
class CategoryDef:
    """Definition of a state category."""
    name: str                    # e.g., "Animation"
    state_class_name: str        # e.g., "AnimationState"
    records: List[RecordDef]     # Records this category wraps
    instance_var: str            # e.g., "animation" -> state.animation()
    section_comment: str         # Comment in FieldEditState to find/replace


def generate_imports(categories: List[CategoryDef]) -> str:
    """Generate import statements for all categories."""
    imports = set()
    for cat in categories:
        for rec in cat.records:
            imports.add(f"import {rec.package}.{rec.class_name};")
            for extra in rec.extra_imports:
                imports.add(f"import {extra};")
    return "\n".join(sorted(imports))


def generate_state_class(cat: CategoryDef) -> str:
    """Generate an inner state class for a category."""
    lines = [
        f"    /**",
        f"     * Manages {cat.name.lower()} parameters using actual record types.",
        f"     * When a parameter changes, the immutable record is rebuilt via toBuilder().",
        f"     */",
        f"    public class {cat.state_class_name} {{",
    ]
    
    # Fields
    for rec in cat.records:
        var_name = rec.class_name[0].lower() + rec.class_name[1:]
        lines.append(f"        private {rec.class_name} {var_name} = {rec.default_value};")
    
    lines.append("")
    
    # Getters/setters for each record's fields
    for rec in cat.records:
        var_name = rec.class_name[0].lower() + rec.class_name[1:]
        
        # Record-level getter/setter
        lines.append(f"        // --- {rec.class_name} ---")
        lines.append(f"        public {rec.class_name} get{rec.class_name}() {{ return {var_name}; }}")
        lines.append(f"        public void set{rec.class_name}({rec.class_name} v) {{ {var_name} = v; markDirty(); }}")
        lines.append("")
        
        # Field-level getters/setters
        for f in rec.fields:
            getter = f.getter_name()
            setter = f.setter_name()
            
            # Getter
            if f.type == "String" and "." in f.type == False:
                # Enum stored as string
                lines.append(f"        public {f.type} {getter}() {{ return {var_name}.{f.name}(); }}")
            elif f.type.endswith("Enum"):
                # Return enum name as String
                actual_type = f.type.replace("Enum", "")
                lines.append(f"        public String {getter}() {{ return {var_name}.{f.name}().name(); }}")
            else:
                lines.append(f"        public {f.type} {getter}() {{ return {var_name}.{f.name}(); }}")
            
            # Setter
            if rec.has_builder:
                if f.type.endswith("Enum"):
                    actual_type = f.type.replace("Enum", "")
                    lines.append(f"        public void {setter}(String v) {{ {var_name} = {var_name}.toBuilder().{f.name}({actual_type}.valueOf(v)).build(); markDirty(); }}")
                else:
                    lines.append(f"        public void {setter}({f.type} v) {{ {var_name} = {var_name}.toBuilder().{f.name}(v).build(); markDirty(); }}")
            else:
                lines.append(f"        // TODO: {rec.class_name} needs toBuilder() method")
        
        lines.append("")
    
    # JSON methods
    lines.extend(generate_json_methods(cat))
    
    lines.append("    }")
    return "\n".join(lines)


def generate_json_methods(cat: CategoryDef) -> List[str]:
    """Generate toJson/fromJson methods for a category."""
    lines = [
        "        // --- JSON serialization ---",
        "        public void toJson(com.google.gson.JsonObject json) {"
    ]
    
    for rec in cat.records:
        var_name = rec.class_name[0].lower() + rec.class_name[1:]
        lines.append(f"            // {rec.class_name}")
        for f in rec.fields:
            if f.type.endswith("Enum"):
                lines.append(f'            json.addProperty("{f.json_key}", {var_name}.{f.name}().name());')
            elif f.type == "boolean":
                lines.append(f'            json.addProperty("{f.json_key}", {var_name}.{f.name}());')
            else:
                lines.append(f'            json.addProperty("{f.json_key}", {var_name}.{f.name}());')
    
    lines.append("        }")
    lines.append("")
    lines.append("        public void fromJson(com.google.gson.JsonObject json) {")
    
    for rec in cat.records:
        var_name = rec.class_name[0].lower() + rec.class_name[1:]
        lines.append(f"            // {rec.class_name}")
        lines.append(f"            {rec.class_name}.Builder {var_name}B = {var_name}.toBuilder();")
        for f in rec.fields:
            json_getter = get_json_getter(f.type)
            if f.type.endswith("Enum"):
                actual_type = f.type.replace("Enum", "")
                lines.append(f'            if (json.has("{f.json_key}")) {var_name}B.{f.name}({actual_type}.valueOf(json.get("{f.json_key}").getAsString()));')
            else:
                lines.append(f'            if (json.has("{f.json_key}")) {var_name}B.{f.name}(json.get("{f.json_key}").{json_getter}());')
        lines.append(f"            {var_name} = {var_name}B.build();")
    
    lines.append("        }")
    return lines


def get_json_getter(java_type: str) -> str:
    """Get the Gson getter method for a Java type."""
    type_map = {
        "int": "getAsInt",
        "float": "getAsFloat",
        "double": "getAsDouble",
        "boolean": "getAsBoolean",
        "String": "getAsString",
        "long": "getAsLong",
    }
    return type_map.get(java_type, "getAsString")


def generate_forwarding_methods(cat: CategoryDef) -> str:
    """Generate forwarding methods for backward compatibility."""
    lines = [
        f"    // --- {cat.name} forwarding (delegates to {cat.instance_var}()) ---",
    ]
    
    for rec in cat.records:
        for f in rec.fields:
            getter = f.getter_name()
            setter = f.setter_name()
            
            if f.type.endswith("Enum"):
                lines.append(f"    public String {getter}() {{ return {cat.instance_var}.{getter}(); }}")
                lines.append(f"    public void {setter}(String v) {{ {cat.instance_var}.{setter}(v); }}")
            else:
                lines.append(f"    public {f.type} {getter}() {{ return {cat.instance_var}.{getter}(); }}")
                lines.append(f"    public void {setter}({f.type} v) {{ {cat.instance_var}.{setter}(v); }}")
    
    return "\n".join(lines)


# ============================================================================
# Category Definitions - Add new categories here!
# ============================================================================

def define_animation_category() -> CategoryDef:
    """Define the Animation category."""
    return CategoryDef(
        name="Animation",
        state_class_name="AnimationState",
        instance_var="animations",
        section_comment="// Animation state (G76-G80)",
        records=[
            RecordDef(
                class_name="SpinConfig",
                package="net.cyberpunk042.visual.animation",
                default_value="SpinConfig.NONE",
                fields=[
                    FieldDef("enabled", "boolean", "spinEnabled", "is"),
                    FieldDef("axis", "String", "spinAxis"),
                    FieldDef("speed", "float", "spinSpeed"),
                ],
            ),
            RecordDef(
                class_name="PulseConfig",
                package="net.cyberpunk042.visual.animation",
                default_value="PulseConfig.NONE",
                fields=[
                    FieldDef("enabled", "boolean", "pulseEnabled", "is"),
                    FieldDef("frequency", "float", "pulseFrequency"),
                    FieldDef("amplitude", "float", "pulseAmplitude"),
                ],
            ),
            # Add more animation records...
        ]
    )


def define_transform_category() -> CategoryDef:
    """Define the Transform category."""
    return CategoryDef(
        name="Transform",
        state_class_name="TransformState", 
        instance_var="transform",
        section_comment="// TRANSFORM STATE",
        records=[
            RecordDef(
                class_name="Transform",
                package="net.cyberpunk042.visual.transform",
                default_value="Transform.IDENTITY",
                has_builder=True,
                fields=[
                    # We need to check what fields Transform has
                ],
            ),
            RecordDef(
                class_name="OrbitConfig",
                package="net.cyberpunk042.visual.transform",
                default_value="OrbitConfig.NONE",
                fields=[
                    FieldDef("enabled", "boolean", "orbitEnabled", "is"),
                    FieldDef("radius", "float", "orbitRadius"),
                    FieldDef("speed", "float", "orbitSpeed"),
                    FieldDef("phase", "float", "orbitPhase"),
                ],
            ),
        ]
    )


if __name__ == "__main__":
    # Test with animation category
    anim = define_animation_category()
    print("=" * 60)
    print("IMPORTS:")
    print(generate_imports([anim]))
    print("=" * 60)
    print("STATE CLASS:")
    print(generate_state_class(anim))
    print("=" * 60)
    print("FORWARDING:")
    print(generate_forwarding_methods(anim))

