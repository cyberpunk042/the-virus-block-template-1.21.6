#!/usr/bin/env python3
"""
Fix animation enabled setters AND getters - comprehensive fix including naming inconsistencies.

Issues to fix:
1. Animation configs use isActive(), not enabled field
2. JSON key "alphaFadeEnabled" should match config name "alphaPulse"
3. getBool("xxx.enabled") should be xxx().isActive()

Approach:
- Disable = set config to NONE
- Enable = config exists (values set separately)
- Fix alphaFade → alphaPulse naming
- Getters use isActive() method
"""

import os
import re
import sys
from pathlib import Path

CLIENT_DIR = "src/client/java"

# Animation configs: (config_path, class_name, json_key_variants)
# json_key_variants are all the possible JSON keys that might be used
ANIMATION_CONFIGS = {
    "spin": ("SpinConfig", ["spin", "spinEnabled"]),
    "pulse": ("PulseConfig", ["pulse", "pulseEnabled"]),
    "beam.pulse": ("PulseConfig", ["beam.pulse"]),  # PulseConfig nested in BeamConfig
    "alphaPulse": ("AlphaPulseConfig", ["alphaPulse", "alphaPulseEnabled", "alphaFade", "alphaFadeEnabled"]),
    "colorCycle": ("ColorCycleConfig", ["colorCycle", "colorCycleEnabled"]),
    "wobble": ("WobbleConfig", ["wobble", "wobbleEnabled"]),
    "wave": ("WaveConfig", ["wave", "waveEnabled"]),
}

# Required imports
IMPORTS = {
    "SpinConfig": "import net.cyberpunk042.visual.animation.SpinConfig;",
    "PulseConfig": "import net.cyberpunk042.visual.animation.PulseConfig;",
    "AlphaPulseConfig": "import net.cyberpunk042.visual.animation.AlphaPulseConfig;",
    "ColorCycleConfig": "import net.cyberpunk042.visual.animation.ColorCycleConfig;",
    "WobbleConfig": "import net.cyberpunk042.visual.animation.WobbleConfig;",
    "WaveConfig": "import net.cyberpunk042.visual.animation.WaveConfig;",
}


def process_file(filepath: str, apply: bool = False) -> tuple:
    """Process a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    needed_imports = set()
    
    for config_path, (config_class, json_keys) in ANIMATION_CONFIGS.items():
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 1: Direct setter with boolean variable
        # state.set("spin.enabled", enabled);
        # → if (!enabled) state.set("spin", SpinConfig.NONE);
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            pattern1 = rf'state\.set\("{json_key}\.enabled",\s*(\w+)\);'
            
            def make_replace1(cfg_path, cfg_class):
                def replace1(m):
                    var_name = m.group(1)
                    needed_imports.add(cfg_class)
                    replacement = f'if (!{var_name}) state.set("{cfg_path}", {cfg_class}.NONE);'
                    changes.append((m.group(0), replacement))
                    return replacement
                return replace1
            
            content = re.sub(pattern1, make_replace1(config_path, config_class), content)
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 2: Lambda setter
        # v -> state.set("xxx.enabled", v)
        # → v -> { if (!v) state.set("xxx", XxxConfig.NONE); }
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            pattern2 = rf'v\s*->\s*state\.set\("{json_key}\.enabled",\s*v\)'
            
            def make_replace2(cfg_path, cfg_class):
                def replace2(m):
                    needed_imports.add(cfg_class)
                    replacement = f'v -> {{ if (!v) state.set("{cfg_path}", {cfg_class}.NONE); }}'
                    changes.append((m.group(0), replacement))
                    return replacement
                return replace2
            
            content = re.sub(pattern2, make_replace2(config_path, config_class), content)
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 2b: Lambda with onUserChange wrapper
        # v -> onUserChange(() -> state.set("xxx.enabled", v))
        # → v -> onUserChange(() -> { if (!v) state.set("xxx", XxxConfig.NONE); })
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            pattern2b = rf'v\s*->\s*onUserChange\(\(\)\s*->\s*state\.set\("{json_key}\.enabled",\s*v\)\)'
            
            def make_replace2b(cfg_path, cfg_class):
                def replace2b(m):
                    needed_imports.add(cfg_class)
                    replacement = f'v -> onUserChange(() -> {{ if (!v) state.set("{cfg_path}", {cfg_class}.NONE); }})'
                    changes.append((m.group(0), replacement))
                    return replacement
                return replace2b
            
            content = re.sub(pattern2b, make_replace2b(config_path, config_class), content)
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 3: JSON conditional - if (json.has("xxxEnabled")) state.set(...)
        # → if (json.has("xxxEnabled") && !json.get("xxxEnabled").getAsBoolean()) state.set("xxx", XxxConfig.NONE);
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            # Handle both "xxxEnabled" and just "xxx" as JSON keys
            for key_suffix in [json_key + "Enabled", json_key] if not json_key.endswith("Enabled") else [json_key]:
                pattern3 = rf'if\s*\((\w+)\.has\("{re.escape(key_suffix)}"\)\)\s*state\.set\("[^"]+\.enabled",\s*\1\.get\("{re.escape(key_suffix)}"\)\.getAsBoolean\(\)\);'
                
                def make_replace3(cfg_path, cfg_class, key):
                    def replace3(m):
                        json_var = m.group(1)
                        needed_imports.add(cfg_class)
                        replacement = f'if ({json_var}.has("{key}") && !{json_var}.get("{key}").getAsBoolean()) state.set("{cfg_path}", {cfg_class}.NONE);'
                        changes.append((m.group(0), replacement))
                        return replacement
                    return replace3
                
                content = re.sub(pattern3, make_replace3(config_path, config_class, key_suffix), content)
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 4: Nested JSON - if (xxx.has("enabled")) state.set("yyy.enabled", ...)
        # Used in FragmentRegistry where variable name matches config
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            # Match variable names like "spin", "pulse", "alpha" (for alphaPulse)
            var_names = [json_key]
            if config_path == "alphaPulse":
                var_names = ["alpha", "alphaPulse", "alphaFade"]
            
            for var_name in var_names:
                pattern4 = rf'if\s*\({re.escape(var_name)}\.has\("enabled"\)\)\s*state\.set\("[^"]+\.enabled",\s*{re.escape(var_name)}\.get\("enabled"\)\.getAsBoolean\(\)\);'
                
                def make_replace4(cfg_path, cfg_class, vname):
                    def replace4(m):
                        needed_imports.add(cfg_class)
                        replacement = f'if ({vname}.has("enabled") && !{vname}.get("enabled").getAsBoolean()) state.set("{cfg_path}", {cfg_class}.NONE);'
                        changes.append((m.group(0), replacement))
                        return replacement
                    return replace4
                
                content = re.sub(pattern4, make_replace4(config_path, config_class, var_name), content)
        
        # ═══════════════════════════════════════════════════════════════════
        # Pattern 5: Direct JSON getter without if-has (mergeData pattern)
        # state.set("xxx.enabled", mergeData.get("xxxEnabled").getAsBoolean());
        # ═══════════════════════════════════════════════════════════════════
        for json_key in json_keys:
            for key_suffix in [json_key + "Enabled", json_key] if not json_key.endswith("Enabled") else [json_key]:
                pattern5 = rf'state\.set\("[^"]+\.enabled",\s*(\w+)\.get\("{re.escape(key_suffix)}"\)\.getAsBoolean\(\)\);'
                
                def make_replace5(cfg_path, cfg_class, key):
                    def replace5(m):
                        json_var = m.group(1)
                        needed_imports.add(cfg_class)
                        replacement = f'if (!{json_var}.get("{key}").getAsBoolean()) state.set("{cfg_path}", {cfg_class}.NONE);'
                        changes.append((m.group(0), replacement))
                        return replacement
                    return replace5
                
                content = re.sub(pattern5, make_replace5(config_path, config_class, key_suffix), content)
    
    # ═══════════════════════════════════════════════════════════════════
    # Add imports if needed
    # ═══════════════════════════════════════════════════════════════════
    if needed_imports and content != original:
        for config_class in needed_imports:
            import_line = IMPORTS[config_class]
            if import_line not in content:
                # Find first import statement
                import_match = re.search(r'^import\s+', content, re.MULTILINE)
                if import_match:
                    insert_pos = import_match.start()
                    content = content[:insert_pos] + import_line + "\n" + content[insert_pos:]
    
    if apply and content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    
    return len(changes), changes, needed_imports


def find_java_files(directory: str) -> list:
    """Find all Java files in directory."""
    java_files = []
    for root, _, files in os.walk(directory):
        for f in files:
            if f.endswith('.java'):
                java_files.append(os.path.join(root, f))
    return java_files


def main():
    apply = '--apply' in sys.argv
    
    print("=" * 60)
    if apply:
        print("APPLYING CHANGES")
    else:
        print("DRY RUN - No changes will be made")
        print("Use --apply to apply changes")
    print("=" * 60)
    
    java_files = find_java_files(CLIENT_DIR)
    total_changes = 0
    files_changed = 0
    
    for filepath in sorted(java_files):
        count, changes, imports = process_file(filepath, apply)
        if count > 0:
            files_changed += 1
            total_changes += count
            rel_path = filepath.replace("\\", "/")
            print(f"\n📁 {rel_path} ({count} changes)")
            for old, new in changes:
                print(f"  OLD: {old[:70]}{'...' if len(old) > 70 else ''}")
                print(f"  NEW: {new[:70]}{'...' if len(new) > 70 else ''}")
                print()
            if imports:
                print(f"  + imports: {', '.join(sorted(imports))}")
    
    print("\n" + "=" * 60)
    print(f"Summary: {total_changes} changes in {files_changed} files")
    if not apply:
        print("Run with --apply to apply these changes")
    print("=" * 60)


if __name__ == "__main__":
    main()
