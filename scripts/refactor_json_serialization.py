#!/usr/bin/env python3
"""
JSON Serialization Refactor Script

This script refactors manual toJson() methods to use JsonSerializer.

Usage:
  python3 scripts/refactor_json_serialization.py --dry-run    # Preview changes
  python3 scripts/refactor_json_serialization.py --execute    # Apply changes

What it does:
1. Finds all Java files with manual toJson() implementations
2. Analyzes the conditional serialization patterns (skipIfDefault, skipIfNull)
3. Generates @JsonField annotations for record components or fields
4. Replaces the toJson() method body with JsonSerializer.toJson(this)
"""

import os
import re
import sys
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Tuple
import argparse

# ═══════════════════════════════════════════════════════════════════════════════
# DATA STRUCTURES
# ═══════════════════════════════════════════════════════════════════════════════

@dataclass
class FieldInfo:
    """Information about a field and its serialization behavior."""
    name: str
    type: str
    json_key: str
    skip_if_default: bool = False
    default_value: Optional[str] = None
    skip_if_null: bool = False
    skip_if_empty: bool = False  # Skip if null or empty (for collections/strings)
    conditional_check: Optional[str] = None  # The original if condition
    compares_to_field: Optional[str] = None  # If comparing to another field
    compares_to_constant: Optional[str] = None  # If comparing to CONSTANT.field
    skip_unless_method: Optional[str] = None  # Method to call on field (e.g., "isActive")
    skip_if_equals_constant: Optional[str] = None  # Static constant name (e.g., "IDENTITY", "NONE")

@dataclass 
class FileRefactor:
    """Refactor plan for a single file."""
    path: Path
    class_name: str
    is_record: bool
    fields: List[FieldInfo] = field(default_factory=list)
    original_to_json: str = ""
    needs_import: bool = True
    can_refactor: bool = True
    has_warnings: bool = False  # Can refactor but needs manual attention
    reason_cannot: str = ""
    # Coverage analysis
    all_json_keys: List[str] = field(default_factory=list)  # All keys found in toJson body
    
    @property
    def detected_keys(self) -> set:
        return {f.json_key for f in self.fields}
    
    @property
    def missed_keys(self) -> list:
        detected = self.detected_keys
        return [k for k in self.all_json_keys if k not in detected]
    
    @property
    def coverage_pct(self) -> int:
        if not self.all_json_keys:
            return 100
        return int(len(self.detected_keys) / len(self.all_json_keys) * 100)
    
    def summary(self) -> str:
        if not self.can_refactor:
            return f"❌ Cannot refactor: {self.reason_cannot}"
        
        annotations = sum(1 for f in self.fields if f.skip_if_default or f.skip_if_null or f.skip_if_empty or f.compares_to_field)
        skip_unless = sum(1 for f in self.fields if f.skip_unless_method)
        skip_if_equals = sum(1 for f in self.fields if f.skip_if_equals_constant)
        resolved_consts = sum(1 for f in self.fields if f.compares_to_constant and f.default_value)
        unresolved_consts = sum(1 for f in self.fields if f.compares_to_constant and not f.default_value)
        
        # Coverage info
        missed = self.missed_keys
        if missed:
            coverage_str = f"⚠️ {self.coverage_pct}% coverage, missed: {', '.join(missed)}"
        else:
            coverage_str = f"✓ 100% coverage"
        
        parts = [f"✅ {len(self.fields)}/{len(self.all_json_keys)} fields ({coverage_str})"]
        if annotations:
            parts.append(f"{annotations} need annotations")
        if skip_unless:
            parts.append(f"🔄 {skip_unless} skipUnless")
        if skip_if_equals:
            parts.append(f"🔄 {skip_if_equals} skipIfEqualsConstant")
        if resolved_consts:
            parts.append(f"✓ {resolved_consts} constants resolved")
        if unresolved_consts:
            parts.append(f"⚠️ {unresolved_consts} constants unresolved")
        
        return ", ".join(parts)

# ═══════════════════════════════════════════════════════════════════════════════
# HELPER FUNCTIONS
# ═══════════════════════════════════════════════════════════════════════════════

def find_method_body(content: str, method_signature: str) -> tuple:
    """Find the full body of a method using brace counting.
    
    Returns (full_match, body_content) or (None, None) if not found.
    """
    # Find the method signature
    sig_match = re.search(method_signature + r'\s*\{', content)
    if not sig_match:
        return None, None
    
    start = sig_match.end() - 1  # Position of opening {
    brace_count = 0
    i = start
    
    while i < len(content):
        if content[i] == '{':
            brace_count += 1
        elif content[i] == '}':
            brace_count -= 1
            if brace_count == 0:
                # Found the matching closing brace
                full_match = content[sig_match.start():i+1]
                body = content[start+1:i]
                return full_match, body
        i += 1
    
    return None, None

# Pattern to find toJson signature (we'll use find_method_body for the full extraction)
TO_JSON_SIGNATURE = r'public\s+JsonObject\s+toJson\s*\(\s*\)'

# ═══════════════════════════════════════════════════════════════════════════════
# PATTERNS
# ═══════════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════════════════════════════════════════════════
# PATTERNS - Each pattern handles ONE specific case clearly
# ═══════════════════════════════════════════════════════════════════════════════

# --- UNCONDITIONAL PATTERNS ---

# Pattern: json.addProperty("key", value)
ADD_PROPERTY_PATTERN = re.compile(r'json\.addProperty\s*\(\s*"(\w+)"\s*,\s*([^)]+)\)')

# Pattern: json.add("key", field.toJson())
ADD_NESTED_TOJSON_PATTERN = re.compile(r'json\.add\s*\(\s*"(\w+)"\s*,\s*(\w+)\.toJson\(\)\s*\)')

# --- CONDITIONAL addProperty PATTERNS ---

# Pattern: if (field != null) { json.addProperty("key", value); }
# Captures: condition, json_key
COND_ADDPROP_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*\)\s*\{\s*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE
)

# Pattern: if (field != Value) { json.addProperty("key", ...); }
# Captures: field, compare_value, json_key
COND_ADDPROP_NOTEQUALS_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*([^)]+)\s*\)\s*\{\s*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE
)

# Pattern: if (!boolField) json.addProperty("key", false);
# Single line, inverted boolean
COND_ADDPROP_NEGATED_INLINE_PATTERN = re.compile(
    r'if\s*\(\s*!(\w+)\s*\)\s+json\.addProperty\s*\(\s*"(\w+)"\s*,\s*([^)]+)\)',
    re.MULTILINE
)

# Pattern: if (field != value) json.addProperty("key", ...);
# Single line, comparison (for DEFAULT.field patterns)
COND_ADDPROP_NOTEQUALS_INLINE_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*([^)]+)\s*\)\s+json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE
)

# Pattern: if (!boolField) { json.addProperty("key", false); }
# Block-style, inverted boolean
COND_ADDPROP_NEGATED_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*!(\w+)\s*\)\s*\{[^}]*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (boolField) { json.addProperty("key", true); }
# Block-style, positive boolean
COND_ADDPROP_BOOL_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*\)\s*\{[^}]*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (boolField) json.addProperty("key", true);
# Single line, positive boolean
COND_ADDPROP_BOOL_INLINE_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*\)\s+json\.addProperty\s*\(\s*"(\w+)"\s*,\s*([^)]+)\)',
    re.MULTILINE
)

# --- CONDITIONAL json.add WITH .toJson() PATTERNS ---

# Pattern: if (field != null) { json.add("key", field.toJson()); }
# Captures: field, json_key, field_for_toJson
COND_ADD_TOJSON_NULL_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*\)\s*\{[^}]*json\.add\s*\(\s*"(\w+)"\s*,\s*(\w+)\.toJson\(\)',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (field != null) json.add("key", field.toJson());
# Single line version
COND_ADD_TOJSON_NULL_INLINE_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*\)\s+json\.add\s*\(\s*"(\w+)"\s*,\s*(\w+)\.toJson\(\)',
    re.MULTILINE
)

# Pattern: if (field != null && field.isActive()) { json.add("key", field.toJson()); }
# Captures: field, method, json_key
COND_ADD_TOJSON_METHOD_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*&&\s*\1\.(\w+)\(\)\s*\)\s*\{[^}]*json\.add\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# --- CONDITIONAL json.add WITH ARRAY PATTERNS ---

# Pattern: if (field != null) { JsonArray...; json.add("key", arr); }
# For Vector3f and similar manual array building
COND_ADD_ARRAY_NULL_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*\)\s*\{[^}]*json\.add\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (field != null && !field.isEmpty()) { ... json.add("key", ...); }
# For collections with isEmpty check - uses [\s\S]*? to handle nested braces
COND_ADD_ARRAY_NOTEMPTY_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*null\s*&&\s*!\1\.isEmpty\(\)\s*\)\s*\{[\s\S]*?json\.add\s*\(\s*"(\w+)"',
    re.MULTILINE
)

# --- MORE CONDITIONAL PATTERNS (for Transform.java etc.) ---

# Pattern: if (field != EnumType.CONSTANT) { json.addProperty("key", ...); }
# Captures: field, EnumType.CONSTANT, json_key
COND_ADDPROP_ENUM_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*(\w+\.\w+)\s*\)\s*\{[^}]*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (field != otherField) { json.addProperty("key", ...); }
# Captures: field, otherField, json_key
COND_ADDPROP_FIELD_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*(\w+)\s*\)\s*\{[^}]*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (field != 0) { json.addProperty("key", field); } 
# For numeric literal comparisons - matches 0, 0.0f, 1.0, etc.
# Captures: field, literal, json_key
COND_ADDPROP_LITERAL_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*(\d+(?:\.\d+)?f?)\s*\)\s*\{[^}]*json\.addProperty\s*\(\s*"(\w+)"',
    re.MULTILINE | re.DOTALL
)

# Pattern: if (field != Type.CONSTANT) { json.add("key", field.toJson()); }
# Captures: field, Type.CONSTANT, json_key
COND_ADD_TOJSON_CONSTANT_BLOCK_PATTERN = re.compile(
    r'if\s*\(\s*(\w+)\s*!=\s*(\w+\.\w+)\s*\)\s*\{[^}]*json\.add\s*\(\s*"(\w+)"\s*,\s*\w+\.toJson\(\)',
    re.MULTILINE | re.DOTALL
)

# Match record declaration - handles nested parentheses in annotations
# Matches until ) followed by { or implements (the start of record body)
RECORD_PATTERN = re.compile(r'public\s+record\s+(\w+)\s*\((.+?)\)\s*(?=\{|implements)', re.DOTALL)

# Match class declaration  
CLASS_PATTERN = re.compile(r'public\s+(?:final\s+)?class\s+(\w+)')

# Match field declaration in class
FIELD_PATTERN = re.compile(r'private\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*(?:=\s*([^;]+))?;')

# Match record component
RECORD_COMPONENT_PATTERN = re.compile(r'(?:@\w+(?:\([^)]*\))?\s+)*(\w+(?:<[^>]+>)?)\s+(\w+)')

# ═══════════════════════════════════════════════════════════════════════════════
# ANALYSIS
# ═══════════════════════════════════════════════════════════════════════════════

def analyze_file(filepath: Path) -> Optional[FileRefactor]:
    """Analyze a single Java file for refactoring."""
    try:
        content = filepath.read_text(encoding='utf-8')
    except Exception:
        return None
    
    # Must have toJson method - use brace counting for proper extraction
    to_json_full, to_json_body = find_method_body(content, TO_JSON_SIGNATURE)
    if not to_json_full:
        return None
    
    # Skip if already uses JsonSerializer
    if 'JsonSerializer.toJson' in to_json_body:
        return None
    
    # Determine if record or class
    record_match = RECORD_PATTERN.search(content)
    class_match = CLASS_PATTERN.search(content)
    
    is_record = record_match is not None
    class_name = record_match.group(1) if is_record else (class_match.group(1) if class_match else "Unknown")
    
    refactor = FileRefactor(
        path=filepath,
        class_name=class_name,
        is_record=is_record,
        original_to_json=to_json_full
    )
    
    # Check if import already exists
    refactor.needs_import = 'import net.cyberpunk042.util.json.JsonSerializer;' not in content
    
    # Analyze the toJson body to find serialization patterns
    fields = analyze_to_json_body(to_json_body, content, is_record, record_match)
    refactor.fields = fields
    
    # Extract ALL json keys for coverage analysis
    refactor.all_json_keys = extract_all_json_keys(to_json_body)
    
    # Check for complex patterns we can't handle
    # JsonArray: Check if it's a pattern we can handle
    if 'JsonArray' in to_json_body:
        # Patterns we CAN handle:
        # - Vector3f (TypeAdapter handles it) - .x, .y, .z pattern
        # - List<String> or List<T> (Gson handles it)
        # - Two fields merged into one array (e.g., inputMin/Max -> inputRange) - CANNOT handle
        
        # Check for Vector3f pattern: arr.add(field.x); arr.add(field.y); arr.add(field.z);
        vector3f_pattern = re.search(r'(\w+)\.add\(\w+\.x\);\s*\1\.add\(\w+\.y\);\s*\1\.add\(\w+\.z\)', to_json_body)
        if vector3f_pattern:
            # This is Vector3f - we can handle it with TypeAdapter
            pass
        # Check for loop-over-collection pattern: for (T x : collection) { arr.add(x); }
        # Gson handles List<T> natively, so this is safe
        elif re.search(r'for\s*\([^)]+\s*:\s*\w+\)\s*\{[^}]*\.add\(', to_json_body):
            # This is a loop over a collection - Gson handles it
            pass
        else:
            # Check for the "two fields to one array" pattern: arr.add(fieldMin); arr.add(fieldMax);
            two_field_array = re.search(r'(\w+)\.add\([^)]+\);\s*\1\.add\(', to_json_body)
            if two_field_array:
                refactor.can_refactor = False
                refactor.reason_cannot = "JsonArray combines multiple fields (needs custom adapter)"
    
    # Nested toJson(): JsonSerializer now recursively handles nested objects
    # with @JsonField annotations. ShapeTypeAdapter handles polymorphic Shape.
    # All nested toJson() calls are now supported!
    
    # Check for unresolved constants (field-to-field is now supported via skipIfEqualsField)
    if any(f.compares_to_constant and not f.default_value for f in fields):
        refactor.has_warnings = True
    
    # skipUnless and skipIfEqualsConstant are now implemented in JsonSerializer.java!
    # No warnings needed for these patterns.
    
    return refactor

def analyze_to_json_body(body: str, full_content: str, is_record: bool, record_match) -> List[FieldInfo]:
    """Analyze toJson body to extract field serialization patterns."""
    fields = []
    field_names_seen = set()
    json_keys_seen = set()  # Track JSON keys too to avoid duplicates
    
    def find_or_create_field(field_name: str, json_key: str) -> FieldInfo:
        """Find existing field or create new one. Prevents duplicates by name OR json_key."""
        # Check by field name first
        for f in fields:
            if f.name == field_name:
                return f
        # Also check by json_key (different patterns might extract different field names)
        for f in fields:
            if f.json_key == json_key:
                return f
        # Create new field
        field_type = infer_field_type(field_name, full_content, field_name)
        field_info = FieldInfo(name=field_name, type=field_type, json_key=json_key)
        fields.append(field_info)
        field_names_seen.add(field_name)
        json_keys_seen.add(json_key)
        return field_info
    
    # ═══════════════════════════════════════════════════════════════════════════
    # STEP 1: Unconditional patterns (no if statement)
    # ═══════════════════════════════════════════════════════════════════════════
    
    # Pattern: json.addProperty("key", value)
    for match in ADD_PROPERTY_PATTERN.finditer(body):
        json_key = match.group(1)
        value_expr = match.group(2).strip()
        field_name = extract_field_name(value_expr, json_key)  # Pass json_key as fallback
        # Skip if we've already seen this field or json_key
        if field_name not in field_names_seen and json_key not in json_keys_seen:
            field_names_seen.add(field_name)
            json_keys_seen.add(json_key)
            field_type = infer_field_type(value_expr, full_content, field_name)
            fields.append(FieldInfo(name=field_name, type=field_type, json_key=json_key))
    
    # Pattern: json.add("key", field.toJson())
    for match in ADD_NESTED_TOJSON_PATTERN.finditer(body):
        json_key = match.group(1)
        field_name = match.group(2)
        # Skip if we've already seen this field or json_key
        if field_name not in field_names_seen and json_key not in json_keys_seen:
            field_names_seen.add(field_name)
            json_keys_seen.add(json_key)
            field_type = infer_field_type(field_name, full_content, field_name)
            fields.append(FieldInfo(name=field_name, type=field_type, json_key=json_key))
    
    # ═══════════════════════════════════════════════════════════════════════════
    # STEP 2: Conditional addProperty - if (condition) { json.addProperty(...) }
    # ═══════════════════════════════════════════════════════════════════════════
    
    # Pattern: if (field != null) { json.addProperty("key", ...); }
    for match in COND_ADDPROP_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_null = True
    
    # Pattern: if (field != Value) { json.addProperty("key", ...); }
    for match in COND_ADDPROP_NOTEQUALS_PATTERN.finditer(body):
        field_name = match.group(1)
        compare_value = match.group(2).strip()
        json_key = match.group(3)
        # Skip if already processed by null check pattern
        if field_name in field_names_seen:
            for f in fields:
                if f.name == field_name:
                    analyze_condition(f, f"{field_name} != {compare_value}", full_content)
                    break
        else:
            field_info = find_or_create_field(field_name, json_key)
            analyze_condition(field_info, f"{field_name} != {compare_value}", full_content)
    
    # Pattern: if (!boolField) json.addProperty("key", false);
    for match in COND_ADDPROP_NEGATED_INLINE_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        field_info = find_or_create_field(field_name, json_key)
        # Inverted boolean: skip if field is true (default)
        field_info.skip_if_default = True
        field_info.default_value = "true"
    
    # Pattern: if (field != value) json.addProperty("key", ...);
    # For patterns like: if (rate != DEFAULT.rate) json.addProperty("rate", rate);
    for match in COND_ADDPROP_NOTEQUALS_INLINE_PATTERN.finditer(body):
        field_name = match.group(1)
        compare_value = match.group(2).strip()
        json_key = match.group(3)
        # Skip if already processed by block pattern
        if field_name in field_names_seen:
            for f in fields:
                if f.name == field_name:
                    analyze_condition(f, f"{field_name} != {compare_value}", full_content)
                    break
        else:
            field_info = find_or_create_field(field_name, json_key)
            analyze_condition(field_info, f"{field_name} != {compare_value}", full_content)
    
    # Pattern: if (boolField) json.addProperty("key", true);
    for match in COND_ADDPROP_BOOL_INLINE_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        # Find existing field or create new one, then update with condition
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_default = True
        field_info.default_value = "false"
    
    # Pattern: if (!boolField) { json.addProperty("key", false); }
    # Block-style, inverted boolean
    for match in COND_ADDPROP_NEGATED_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        # Find existing field or create new one, then update with condition
        field_info = find_or_create_field(field_name, json_key)
        # Inverted boolean: skip if field is true (default)
        field_info.skip_if_default = True
        field_info.default_value = "true"
    
    # Pattern: if (boolField) { json.addProperty("key", true); }
    # Block-style, positive boolean
    for match in COND_ADDPROP_BOOL_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        # Find existing field or create new one, then update with condition
        field_info = find_or_create_field(field_name, json_key)
        # Positive boolean: skip if field is false (default)
        field_info.skip_if_default = True
        field_info.default_value = "false"
    
    # ═══════════════════════════════════════════════════════════════════════════
    # STEP 3: Conditional json.add - if (condition) { json.add("key", x.toJson()) }
    # ═══════════════════════════════════════════════════════════════════════════
    
    # Pattern: if (field != null) { json.add("key", field.toJson()); }
    for match in COND_ADD_TOJSON_NULL_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_null = True
    
    # Pattern: if (field != null) json.add("key", field.toJson());
    for match in COND_ADD_TOJSON_NULL_INLINE_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_null = True
    
    # Pattern: if (field != null && field.isActive()) { json.add("key", ...); }
    for match in COND_ADD_TOJSON_METHOD_PATTERN.finditer(body):
        field_name = match.group(1)
        method_name = match.group(2)
        json_key = match.group(3)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_unless_method = method_name
    
    # ═══════════════════════════════════════════════════════════════════════════
    # STEP 4: Conditional json.add for arrays/collections
    # ═══════════════════════════════════════════════════════════════════════════
    
    # Pattern: if (field != null && !field.isEmpty()) { ... json.add("key", ...); }
    for match in COND_ADD_ARRAY_NOTEMPTY_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_empty = True
    
    # Pattern: if (field != null) { JsonArray...; json.add("key", arr); }
    # (Fallback for Vector3f and similar that need null check)
    for match in COND_ADD_ARRAY_NULL_PATTERN.finditer(body):
        field_name = match.group(1)
        json_key = match.group(2)
        if field_name not in field_names_seen:
            field_info = find_or_create_field(field_name, json_key)
            field_info.skip_if_null = True
    
    # ═══════════════════════════════════════════════════════════════════════════
    # STEP 5: Specific comparison patterns (Transform.java etc.)
    # ═══════════════════════════════════════════════════════════════════════════
    
    # Pattern: if (field != EnumType.CONSTANT) { json.addProperty("key", ...); }
    for match in COND_ADDPROP_ENUM_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        enum_constant = match.group(2)  # e.g., "Anchor.CENTER"
        json_key = match.group(3)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_equals_constant = enum_constant
    
    # Pattern: if (field != otherField) { json.addProperty("key", ...); }
    # Must come AFTER enum and literal patterns to avoid false matches
    for match in COND_ADDPROP_FIELD_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        other_field = match.group(2).strip()
        json_key = match.group(3)
        # Skip if it's an enum constant (contains .)
        if '.' in other_field:
            continue
        # Skip if other_field is "null"
        if other_field == 'null':
            continue
        # Skip if it's a numeric literal (0, 1.0f, etc.)
        if re.match(r'^-?\d+(\.\d+)?f?$', other_field):
            continue
        # Skip if already processed
        if field_name in field_names_seen:
            for f in fields:
                if f.name == field_name:
                    f.compares_to_field = other_field
                    break
        else:
            field_info = find_or_create_field(field_name, json_key)
            field_info.compares_to_field = other_field
    
    # Pattern: if (field != 0) { json.addProperty("key", field); }
    for match in COND_ADDPROP_LITERAL_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        literal = match.group(2)  # e.g., "0", "0.0f", "1.0"
        json_key = match.group(3)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_default = True
        field_info.default_value = literal
    
    # Pattern: if (field != Type.CONSTANT) { json.add("key", field.toJson()); }
    for match in COND_ADD_TOJSON_CONSTANT_BLOCK_PATTERN.finditer(body):
        field_name = match.group(1)
        constant = match.group(2)  # e.g., "DecayConfig.NONE"
        json_key = match.group(3)
        field_info = find_or_create_field(field_name, json_key)
        field_info.skip_if_equals_constant = constant
    
    return fields

def extract_field_name(value_expr: str, json_key: str = None) -> str:
    """Extract field name from value expression.
    
    Args:
        value_expr: The value being serialized (e.g., "field", "field.name()", "true")
        json_key: The JSON key being used (fallback if value is a literal)
    """
    value_expr = value_expr.strip()
    
    # Handle literal values - use json_key as field name
    if value_expr in ('true', 'false', 'null'):
        return json_key if json_key else value_expr
    
    # Handle numeric literals - use json_key
    if re.match(r'^-?\d+(\.\d+)?[fFdDlL]?$', value_expr):
        return json_key if json_key else value_expr
    
    # Handle enum.name() - extract the enum field name
    # e.g., "anchor.name()" or "anchor.name(" -> "anchor"
    if '.name(' in value_expr:
        return value_expr.split('.name(')[0].strip()
    
    # Handle .id() calls similarly
    if '.id(' in value_expr:
        return value_expr.split('.id(')[0].strip()
    
    # Handle this.field
    if value_expr.startswith('this.'):
        return value_expr[5:]
    
    # Handle method calls like field.toString() - extract field part
    if '.' in value_expr and '(' in value_expr:
        return value_expr.split('.')[0].strip()
    
    # Handle direct field reference
    if value_expr.isidentifier():
        return value_expr
    
    # Fallback to json_key if available
    return json_key if json_key else value_expr

def extract_all_json_keys(to_json_body: str) -> List[str]:
    """Extract ALL json keys from toJson body for coverage analysis.
    
    This finds every json.addProperty("key", ...) and json.add("key", ...)
    call to establish the total number of fields being serialized.
    """
    keys = []
    
    # Pattern for json.addProperty("key", ...)
    for match in re.finditer(r'json\.addProperty\s*\(\s*"(\w+)"', to_json_body):
        keys.append(match.group(1))
    
    # Pattern for json.add("key", ...) - NOT for temporary arrays
    # We want to match json.add("fieldName", ...) but not arr.add(...)
    for match in re.finditer(r'json\.add\s*\(\s*"(\w+)"', to_json_body):
        keys.append(match.group(1))
    
    return keys

def infer_field_type(value_expr: str, content: str, field_name: str) -> str:
    """Infer the field type from context."""
    if '.name()' in value_expr:
        return 'Enum'
    
    # Search for field declaration
    pattern = rf'(?:private|public)\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+{re.escape(field_name)}\b'
    match = re.search(pattern, content)
    if match:
        return match.group(1)
    
    return 'unknown'

def analyze_condition(field: FieldInfo, condition: str, full_content: str) -> None:
    """Analyze a conditional check to determine skip behavior."""
    field.conditional_check = condition
    
    # Pattern: if (field != null && field.isActive()) - skipUnless method call
    method_call_pattern = rf'{re.escape(field.name)}\s*!=\s*null\s*&&\s*{re.escape(field.name)}\.(\w+)\(\)'
    method_match = re.match(method_call_pattern, condition)
    if method_match:
        field.skip_unless_method = method_match.group(1)
        return
    
    # Pattern: if (field != null && field != ClassName.CONSTANT) - skipIfEqualsConstant static constant
    const_compare_pattern = rf'{re.escape(field.name)}\s*!=\s*null\s*&&\s*{re.escape(field.name)}\s*!=\s*(\w+)\.(\w+)'
    const_compare_match = re.match(const_compare_pattern, condition)
    if const_compare_match:
        class_name = const_compare_match.group(1)
        const_name = const_compare_match.group(2)
        field.skip_if_equals_constant = const_name
        return
    
    # Pattern: if (fieldName) - boolean field, skip if false
    if condition == field.name:
        field.skip_if_default = True
        return
    
    # Pattern: if (!fieldName) - boolean field, skip if true
    if condition == f'!{field.name}':
        field.skip_if_default = True
        field.default_value = "true"
        return
    
    # Pattern: if (fieldName != null) - skip if null
    if condition == f'{field.name} != null':
        field.skip_if_null = True
        return
    
    # Pattern: if (fieldName != null && !fieldName.isEmpty()) - skip if null or empty
    empty_pattern = rf'{re.escape(field.name)}\s*!=\s*null\s*&&\s*!{re.escape(field.name)}\.isEmpty\(\)'
    if re.match(empty_pattern, condition):
        field.skip_if_empty = True
        return
    
    # Pattern: if (fieldName != something)
    not_equals = re.match(rf'{re.escape(field.name)}\s*!=\s*(.+)', condition)
    if not_equals:
        default_expr = not_equals.group(1).strip()
        
        # Check if it's a Type.CONSTANT reference (e.g., Anchor.CENTER, Facing.FIXED)
        # Matches: PascalCase.CONSTANT or UPPERCASE.CONSTANT
        const_match = re.match(r'([A-Z][a-zA-Z0-9_]*)\.([A-Z][A-Z0-9_]*)', default_expr)
        if const_match:
            type_name = const_match.group(1)
            const_name = const_match.group(2)
            # For enums, use skipIfEqualsConstant directly
            field.skip_if_equals_constant = const_name
            return
        
        # Check if it's a CONSTANT.field reference (e.g., DEFAULT.rate)
        const_field_match = re.match(r'([A-Z_]+)\.(\w+)', default_expr)
        if const_field_match:
            const_name = const_field_match.group(1)
            const_field = const_field_match.group(2)
            field.compares_to_constant = default_expr
            # Try to resolve the constant value
            resolved = resolve_constant_value(full_content, const_name, const_field)
            if resolved:
                field.skip_if_default = True
                field.default_value = resolved
            return
        
        # Check if it's comparing to another field (fieldName != otherField)
        if re.match(r'^[a-z]\w*$', default_expr):  # lowercase = likely a field
            field.compares_to_field = default_expr
            field.skip_if_default = True
            # Can't use simple default, mark for special handling
            return
        
        # It's a literal value
        field.skip_if_default = True
        # Clean up the value (remove f suffix from floats, etc.)
        field.default_value = default_expr.rstrip('f').rstrip('F').rstrip('d').rstrip('D')
        return


def resolve_constant_value(content: str, const_name: str, field_name: str) -> Optional[str]:
    """Try to resolve a constant value like DEFAULT.rate from the file."""
    # Pattern for record instantiation: CONST = new Record(val1, val2, ...)
    # Handle multiline constructors with closing );
    pattern = rf'public\s+static\s+final\s+\w+\s+{const_name}\s*=\s*new\s+\w+\s*\(([^;]+?)\);'
    match = re.search(pattern, content, re.DOTALL)
    if match:
        args_str = match.group(1)
        # Clean up: remove newlines, extra spaces
        args_str = re.sub(r'\s+', ' ', args_str).strip()
        args = [a.strip() for a in args_str.split(',')]
        
        # Find the position of the field in the record definition
        field_index = find_record_component_index(content, field_name)
        if field_index is not None and field_index < len(args):
            val = args[field_index]
            # Clean up: remove suffixes and casts
            val = val.rstrip('f').rstrip('F').rstrip('d').rstrip('D').rstrip('L').rstrip('l')
            return val
    
    return None


def find_record_component_index(content: str, field_name: str) -> Optional[int]:
    """Find the index of a record component by name."""
    record_match = RECORD_PATTERN.search(content)
    if not record_match:
        return None
    
    components_str = record_match.group(2)
    # Clean up: remove newlines, extra spaces
    components_str = re.sub(r'\s+', ' ', components_str).strip()
    
    # Split by comma but handle nested generics and annotations
    depth = 0
    current = ""
    components = []
    for char in components_str:
        if char in '<(':
            depth += 1
        elif char in '>)':
            depth -= 1
        elif char == ',' and depth == 0:
            components.append(current.strip())
            current = ""
            continue
        current += char
    if current.strip():
        components.append(current.strip())
    
    idx = 0
    for comp in components:
        if not comp:
            continue
        
        # Remove annotations: @Range(ValueRange.POSITIVE) float rate -> float rate
        comp_clean = re.sub(r'@\w+\([^)]*\)\s*', '', comp)
        comp_clean = re.sub(r'@\w+\s*', '', comp_clean)
        
        # Extract field name: "float rate" -> "rate", "int count" -> "count"
        parts = comp_clean.strip().split()
        if len(parts) >= 2:
            name = parts[-1]  # Last word is the name
            if name == field_name:
                return idx
        
        idx += 1
    
    return None

# ═══════════════════════════════════════════════════════════════════════════════
# CODE GENERATION
# ═══════════════════════════════════════════════════════════════════════════════

def generate_annotation(field: FieldInfo) -> Optional[str]:
    """Generate @JsonField annotation for a field if needed."""
    parts = []
    
    # Field-to-field comparison - use skipIfEqualsField
    if field.compares_to_field:
        return f'@JsonField(skipIfEqualsField = "{field.compares_to_field}")'
    
    # Method call condition - use skipUnless
    if field.skip_unless_method:
        return f'@JsonField(skipUnless = "{field.skip_unless_method}")'
    
    # Static constant comparison - use skipIfEqualsConstant
    if field.skip_if_equals_constant:
        return f'@JsonField(skipIfEqualsConstant = "{field.skip_if_equals_constant}")'
    
    if field.skip_if_default:
        parts.append("skipIfDefault = true")
        if field.default_value and field.default_value not in ('0', '0.0', 'false', 'null'):
            # Clean up the default value
            dv = field.default_value.replace('f', '').replace('d', '')
            parts.append(f'defaultValue = "{dv}"')
    
    if field.skip_if_null:
        parts.append("skipIfNull = true")
    
    if field.skip_if_empty:
        parts.append("skipIfEmpty = true")
    
    if not parts:
        return None
    
    return f"@JsonField({', '.join(parts)})"


def generate_annotation_report(field: FieldInfo) -> str:
    """Generate a human-readable report about what annotation is needed."""
    if field.compares_to_field:
        return f'@JsonField(skipIfEqualsField = "{field.compares_to_field}")'
    if field.skip_unless_method:
        return f'@JsonField(skipUnless = "{field.skip_unless_method}")'
    if field.skip_if_equals_constant:
        return f'@JsonField(skipIfEqualsConstant = "{field.skip_if_equals_constant}")'
    if field.skip_if_empty:
        return '@JsonField(skipIfEmpty = true)'
    if field.compares_to_constant:
        if field.default_value:
            return f'@JsonField(skipIfDefault = true, defaultValue = "{field.default_value}") (from {field.compares_to_constant})'
        else:
            return f"⚠️ Compares to constant {field.compares_to_constant} - could not resolve value"
    ann = generate_annotation(field)
    return ann if ann else "(no annotation needed)"

def generate_refactored_content(refactor: FileRefactor, original_content: str) -> str:
    """Generate the refactored file content."""
    content = original_content
    
    # Check which imports are needed
    needs_serializer_import = 'import net.cyberpunk042.util.json.JsonSerializer;' not in content
    needs_json_field_import = (
        'import net.cyberpunk042.util.json.JsonField;' not in content and
        any(
            f.skip_if_default or f.skip_if_null or f.skip_if_empty or f.compares_to_field or 
            f.skip_unless_method or f.skip_if_equals_constant
            for f in refactor.fields
        )
    )
    
    # Add imports if needed
    if needs_serializer_import or needs_json_field_import:
        serializer_import = "import net.cyberpunk042.util.json.JsonSerializer;\n"
        json_field_import = "import net.cyberpunk042.util.json.JsonField;\n"
        
        # Find last import line
        last_import = 0
        for match in re.finditer(r'^import [^;]+;$', content, re.MULTILINE):
            last_import = match.end()
        
        if last_import > 0:
            imports_to_add = ""
            if needs_json_field_import:
                imports_to_add += json_field_import
            if needs_serializer_import:
                imports_to_add += serializer_import
            content = content[:last_import] + "\n" + imports_to_add + content[last_import:]
    
    # For records, add annotations to record components
    if refactor.is_record:
        content = add_record_annotations(content, refactor)
    else:
        # For classes, add annotations to field declarations
        content = add_class_field_annotations(content, refactor)
    
    # Replace toJson method body
    new_to_json = """public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }"""
    
    # Use direct string replacement with the original method we captured
    content = content.replace(refactor.original_to_json, new_to_json)
    
    return content

def add_record_annotations(content: str, refactor: FileRefactor) -> str:
    """Add @JsonField annotations to record components."""
    record_match = RECORD_PATTERN.search(content)
    if not record_match:
        return content
    
    original_components = record_match.group(2)
    
    # Parse and annotate each component
    components = []
    for line in original_components.split(','):
        line = line.strip()
        if not line:
            continue
        
        # Find which field this corresponds to
        for f in refactor.fields:
            if f.name in line and not line.startswith('@JsonField'):
                annotation = generate_annotation(f)
                if annotation:
                    # Check if there are existing annotations
                    if '@' in line:
                        # Insert before the type
                        type_match = re.search(r'(\w+(?:<[^>]+>)?)\s+' + re.escape(f.name), line)
                        if type_match:
                            insert_pos = line.find(type_match.group(1))
                            line = line[:insert_pos] + annotation + ' ' + line[insert_pos:]
                    else:
                        line = annotation + ' ' + line
                break
        
        components.append(line)
    
    new_components = ',\n    '.join(components)
    new_record_decl = f"public record {refactor.class_name}(\n    {new_components}\n)"
    
    content = content[:record_match.start()] + new_record_decl + content[record_match.end():]
    
    return content

def add_class_field_annotations(content: str, refactor: FileRefactor) -> str:
    """Add @JsonField annotations to class fields."""
    for f in refactor.fields:
        annotation = generate_annotation(f)
        if not annotation:
            continue
        
        # Find the field declaration
        pattern = rf'(private\s+(?:final\s+)?{re.escape(f.type)}\s+{re.escape(f.name)})'
        match = re.search(pattern, content)
        if match and '@JsonField' not in content[max(0, match.start()-50):match.start()]:
            # Add annotation before field
            content = content[:match.start()] + annotation + ' ' + content[match.start():]
    
    return content

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════

def find_candidate_files(src_dir: Path) -> List[Path]:
    """Find all Java files with toJson methods."""
    candidates = []
    
    for java_file in src_dir.rglob("*.java"):
        if 'build' in java_file.parts or '.gradle' in java_file.parts:
            continue
        if '_legacy' in java_file.parts or '_old' in str(java_file):
            continue
        
        try:
            content = java_file.read_text(encoding='utf-8')
            if 'public JsonObject toJson()' in content and 'JsonSerializer.toJson' not in content:
                candidates.append(java_file)
        except:
            pass
    
    return candidates

def main():
    parser = argparse.ArgumentParser(description='Refactor JSON serialization to use JsonSerializer')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--execute', action='store_true', help='Actually apply the changes')
    parser.add_argument('--safe-only', action='store_true', help='Only process files without warnings (skip field-to-field comparisons)')
    parser.add_argument('--file', type=str, help='Only process a specific file')
    args = parser.parse_args()
    
    if not args.dry_run and not args.execute:
        print("Please specify --dry-run or --execute")
        print("  --dry-run    : Preview changes without modifying files")
        print("  --execute    : Apply changes to files")
        print("  --safe-only  : Only process files without warnings")
        sys.exit(1)
    
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    src_dir = project_root / 'src'
    
    print("=" * 80)
    print("JSON SERIALIZATION REFACTOR")
    print("=" * 80)
    print(f"Mode: {'DRY RUN (no changes)' if args.dry_run else 'EXECUTE (will modify files)'}")
    print()
    
    # Find candidates
    if args.file:
        candidates = [Path(args.file)]
    else:
        candidates = find_candidate_files(src_dir)
    
    print(f"Found {len(candidates)} files with toJson() methods to analyze")
    print()
    
    # Analyze each file
    refactors: List[FileRefactor] = []
    for filepath in candidates:
        refactor = analyze_file(filepath)
        if refactor:
            refactors.append(refactor)
    
    # Report - categorize files
    safe_refactor = [r for r in refactors if r.can_refactor and not r.has_warnings]
    warn_refactor = [r for r in refactors if r.can_refactor and r.has_warnings]
    cannot_refactor = [r for r in refactors if not r.can_refactor]
    
    print(f"✅ Safe to auto-refactor: {len(safe_refactor)} files")
    print(f"⚠️  Needs manual attention: {len(warn_refactor)} files")
    print(f"❌ Cannot auto-refactor: {len(cannot_refactor)} files")
    print()
    
    # Show safe files
    if safe_refactor:
        print("=" * 80)
        print("✅ SAFE TO AUTO-REFACTOR")
        print("=" * 80)
        for r in safe_refactor:
            short_path = str(r.path).split('src')[-1]
            print(f"\n📁 {r.class_name} ({short_path})")
            print(f"   Type: {'record' if r.is_record else 'class'}")
            print(f"   {r.summary()}")
            
            # Show fields that need annotations
            annotated = [f for f in r.fields if f.skip_if_default or f.skip_if_null or f.skip_if_empty or f.skip_unless_method or f.skip_if_equals_constant or f.compares_to_field]
            if annotated:
                print("   Annotations:")
                for f in annotated:
                    report = generate_annotation_report(f)
                    print(f"     - {f.name}: {report}")
    
    # Show files needing manual attention
    if warn_refactor:
        print()
        print("=" * 80)
        print("⚠️  NEEDS MANUAL ATTENTION (needs new annotations)")
        print("=" * 80)
        for r in warn_refactor:
            short_path = str(r.path).split('src')[-1]
            print(f"\n📁 {r.class_name} ({short_path})")
            print(f"   Type: {'record' if r.is_record else 'class'}")
            print(f"   {r.summary()}")
            
            # Show all field details
            for f in r.fields:
                if f.skip_if_default or f.skip_if_null or f.skip_if_empty or f.compares_to_field or f.compares_to_constant or f.skip_unless_method or f.skip_if_equals_constant:
                    report = generate_annotation_report(f)
                    print(f"     - {f.name}: {report}")
    
    if cannot_refactor:
        print()
        print("=" * 80)
        print("FILES THAT CANNOT BE AUTO-REFACTORED")
        print("=" * 80)
        for r in cannot_refactor:
            short_path = str(r.path).split('src')[-1]
            print(f"\n❌ {r.class_name} ({short_path})")
            print(f"   Reason: {r.reason_cannot}")
    
    # Determine which files to process
    files_to_process = safe_refactor if args.safe_only else (safe_refactor + warn_refactor)
    
    # Execute if requested
    if args.execute and files_to_process:
        print()
        print("=" * 80)
        print("APPLYING CHANGES")
        print("=" * 80)
        
        if args.safe_only:
            print(f"(--safe-only mode: processing {len(safe_refactor)} safe files, skipping {len(warn_refactor)} with warnings)")
        
        success = 0
        failed = 0
        for r in files_to_process:
            try:
                original = r.path.read_text(encoding='utf-8')
                refactored = generate_refactored_content(r, original)
                
                # Create backup
                backup_path = r.path.with_suffix('.java.bak')
                backup_path.write_text(original, encoding='utf-8')
                
                # Write refactored
                r.path.write_text(refactored, encoding='utf-8')
                status = "✅" if not r.has_warnings else "⚠️"
                print(f"{status} Refactored: {r.class_name}")
                success += 1
                
            except Exception as e:
                print(f"❌ Failed: {r.class_name} - {e}")
                failed += 1
        
        print()
        print(f"Done! {success} files refactored, {failed} failed")
        print("Backup files created with .java.bak extension")
        print("Run './gradlew compileJava' to verify the changes compile correctly")
    
    elif args.dry_run:
        print()
        print("=" * 80)
        print("DRY RUN COMPLETE - No files were modified")
        print("=" * 80)
        if args.safe_only:
            print(f"Run with --execute --safe-only to apply changes to {len(safe_refactor)} safe files")
        else:
            print(f"Run with --execute to apply changes to {len(files_to_process)} files")
            print(f"Run with --execute --safe-only to apply changes to {len(safe_refactor)} safe files only")

if __name__ == '__main__':
    main()

