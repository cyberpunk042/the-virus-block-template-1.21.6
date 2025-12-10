#!/usr/bin/env python3
"""
Add toBuilder() methods to all record classes that have Builder inner classes.
Scans the codebase and adds toBuilder() where missing.
"""

import re
import os
from pathlib import Path

def find_java_files(base_dir: str) -> list:
    """Find all .java files (excluding .bak files)."""
    java_files = []
    for root, dirs, files in os.walk(base_dir):
        for f in files:
            if f.endswith('.java') and not f.endswith('.bak'):
                java_files.append(os.path.join(root, f))
    return java_files


def parse_record_fields(content: str) -> list:
    """Extract field names from a record declaration."""
    # Match: public record Name( ... ) {
    record_match = re.search(r'public record \w+\s*\((.*?)\)\s*(?:implements|extends|{)', content, re.DOTALL)
    if not record_match:
        return []
    
    params_str = record_match.group(1)
    # Parse each parameter: @Annotations Type name
    fields = []
    # Split by comma, handling generic types
    depth = 0
    current = ""
    for char in params_str:
        if char in '<(':
            depth += 1
        elif char in '>)':
            depth -= 1
        elif char == ',' and depth == 0:
            fields.append(current.strip())
            current = ""
            continue
        current += char
    if current.strip():
        fields.append(current.strip())
    
    # Extract just the field names
    field_names = []
    for field in fields:
        # Get the last word (field name)
        parts = field.split()
        if parts:
            name = parts[-1]
            field_names.append(name)
    
    return field_names


def parse_builder_fields(content: str) -> list:
    """Extract field names and their builder method names from Builder class."""
    # Find Builder class
    builder_match = re.search(r'public static class Builder \{(.*?)\n    \}', content, re.DOTALL)
    if not builder_match:
        return []
    
    builder_content = builder_match.group(1)
    
    # Find builder methods: public Builder fieldName(Type val) { ... return this; }
    method_pattern = r'public Builder (\w+)\s*\([^)]+\)\s*\{'
    methods = re.findall(method_pattern, builder_content)
    
    return methods


def has_to_builder(content: str) -> bool:
    """Check if file already has toBuilder() method."""
    return 'public Builder toBuilder()' in content


def has_builder_class(content: str) -> bool:
    """Check if file has a Builder inner class."""
    return 'public static class Builder' in content and 'public static Builder builder()' in content


def generate_to_builder(field_names: list) -> str:
    """Generate toBuilder() method."""
    lines = [
        "",
        "    /** Create a builder pre-populated with this record's values. */",
        "    public Builder toBuilder() {",
        "        return new Builder()",
    ]
    
    for i, name in enumerate(field_names):
        suffix = ";" if i == len(field_names) - 1 else ""
        lines.append(f"            .{name}({name}){suffix}")
    
    lines.append("    }")
    return "\n".join(lines)


def add_to_builder_to_file(filepath: str, dry_run: bool = False) -> dict:
    """Add toBuilder() to a file if needed. Returns status dict."""
    result = {
        'file': filepath,
        'status': 'skipped',
        'reason': '',
        'fields': []
    }
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if it's a record with a Builder
    if not has_builder_class(content):
        result['reason'] = 'no Builder class'
        return result
    
    if has_to_builder(content):
        result['status'] = 'exists'
        result['reason'] = 'already has toBuilder()'
        return result
    
    # Parse fields from record or builder methods
    record_fields = parse_record_fields(content)
    builder_methods = parse_builder_fields(content)
    
    # Use builder methods as the source of truth (they match what we need to call)
    fields = builder_methods if builder_methods else record_fields
    
    if not fields:
        result['reason'] = 'could not parse fields'
        return result
    
    result['fields'] = fields
    
    # Generate toBuilder() method
    to_builder_code = generate_to_builder(fields)
    
    # Find insertion point: right after "public static Builder builder() { ... }"
    pattern = r'(    public static Builder builder\(\) \{ return new Builder\(\); \})'
    
    if re.search(pattern, content):
        new_content = re.sub(pattern, r'\1' + to_builder_code, content)
        
        if not dry_run:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
        
        result['status'] = 'added'
        result['reason'] = f'{len(fields)} fields'
        return result
    else:
        result['reason'] = 'could not find builder() method'
        return result


def main():
    import sys
    dry_run = '--dry-run' in sys.argv
    
    print("=" * 70)
    print("Adding toBuilder() to all record classes with Builder")
    if dry_run:
        print("🔍 DRY RUN MODE - No changes will be made")
    print("=" * 70)
    
    # Scan directories
    dirs_to_scan = [
        "src/main/java/net/cyberpunk042/visual",
        "src/main/java/net/cyberpunk042/field",
    ]
    
    all_files = []
    for d in dirs_to_scan:
        if os.path.exists(d):
            all_files.extend(find_java_files(d))
    
    print(f"\nFound {len(all_files)} Java files to scan\n")
    
    # Process each file
    results = {'added': [], 'exists': [], 'skipped': []}
    
    for filepath in sorted(all_files):
        result = add_to_builder_to_file(filepath, dry_run)
        results[result['status']].append(result)
    
    # Print summary
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)
    
    if results['added']:
        print(f"\n✅ Would add toBuilder() to {len(results['added'])} files:" if dry_run else f"\n✅ Added toBuilder() to {len(results['added'])} files:")
        for r in results['added']:
            rel_path = os.path.relpath(r['file'])
            print(f"   {rel_path} ({r['reason']})")
            if '--verbose' in sys.argv:
                print(f"      Fields: {', '.join(r['fields'])}")
    
    if results['exists']:
        print(f"\n⏭️  Already have toBuilder(): {len(results['exists'])} files")
        if '--verbose' in sys.argv:
            for r in results['exists']:
                print(f"   {os.path.relpath(r['file'])}")
    
    if '--verbose' in sys.argv and results['skipped']:
        print(f"\n⏭️  Skipped (no Builder): {len(results['skipped'])} files")
    
    print(f"\nTotal: {len(results['added'])} added, {len(results['exists'])} existing, {len(results['skipped'])} skipped")
    
    if dry_run:
        print("\n🔍 DRY RUN - Run without --dry-run to apply changes")


if __name__ == "__main__":
    main()

