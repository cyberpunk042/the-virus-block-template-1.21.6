#!/usr/bin/env python3
"""
Fix unnecessary (ServerWorld) casts throughout the codebase.
ServerPlayerEntity.getWorld() already returns ServerWorld in MC 1.21+
"""

import os
import re

BASE_PATH = "/mnt/c/Users/Jean/the-virus-block-template-1.21.6/src/main/java"

def find_java_files(root):
    """Find all .java files recursively."""
    for dirpath, _, filenames in os.walk(root):
        for f in filenames:
            if f.endswith('.java'):
                yield os.path.join(dirpath, f)

def fix_casts_in_file(filepath):
    """Remove unnecessary (ServerWorld) casts ONLY for ServerPlayerEntity variables."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # ONLY remove casts for known ServerPlayerEntity variables:
    # - player.getWorld() where player is ServerPlayerEntity
    # - target.getWorld() where target is ServerPlayerEntity  
    # - handler.player.getWorld()
    #
    # DO NOT remove casts for:
    # - block.getWorld() (BlockEntity returns World)
    # - this.getWorld() (Entity returns World)
    # - entity.getWorld() (Entity returns World)
    
    content = re.sub(
        r'\(ServerWorld\)\s*player\.getWorld\(\)',
        'player.getWorld()',
        content
    )
    
    content = re.sub(
        r'\(ServerWorld\)\s*target\.getWorld\(\)',
        'target.getWorld()',
        content
    )
    
    content = re.sub(
        r'\(ServerWorld\)\s*handler\.player\.getWorld\(\)',
        'handler.player.getWorld()',
        content
    )
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    print("=" * 60)
    print("Fixing unnecessary (ServerWorld) casts")
    print("=" * 60)
    
    fixed_files = []
    for filepath in find_java_files(BASE_PATH):
        if fix_casts_in_file(filepath):
            rel_path = os.path.relpath(filepath, BASE_PATH)
            fixed_files.append(rel_path)
            print(f"  ✅ {rel_path}")
    
    print()
    print(f"Fixed {len(fixed_files)} files")
    
    # Now fix the specific compile errors in DebugFieldTracker and GuiPacketRegistration
    print()
    print("=" * 60)
    print("Fixing specific compile errors")
    print("=" * 60)
    
    # Fix DebugFieldTracker
    tracker_path = os.path.join(BASE_PATH, "net/cyberpunk042/network/gui/DebugFieldTracker.java")
    if os.path.exists(tracker_path):
        with open(tracker_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Fix FieldRegistry.register call
        old_register = '''            // Register temporarily with unique ID
            String debugId = "debug_" + playerUuid.toString().substring(0, 8) + "_" + (++debugCounter);
            Identifier defId = Identifier.of("the-virus-block", debugId);
            FieldRegistry.register(defId, definition);'''
        
        new_register = '''            // Register temporarily with unique ID embedded in the definition
            String debugId = "debug_" + playerUuid.toString().substring(0, 8) + "_" + (++debugCounter);
            // Create new definition with debug ID
            FieldDefinition debugDef = FieldDefinition.builder(debugId)
                .type(definition.type())
                .baseRadius(definition.baseRadius())
                .themeId(definition.themeId())
                .layers(definition.layers())
                .modifiers(definition.modifiers())
                .prediction(definition.prediction())
                .beam(definition.beam())
                .followMode(definition.followMode())
                .bindings(definition.bindings())
                .triggers(definition.triggers())
                .lifecycle(definition.lifecycle())
                .build();
            FieldRegistry.register(debugDef);
            net.minecraft.util.Identifier defId = net.minecraft.util.Identifier.of("the-virus-block", debugId);'''
        
        if old_register in content:
            content = content.replace(old_register, new_register)
            print("  ✅ Fixed FieldRegistry.register in DebugFieldTracker")
        
        with open(tracker_path, 'w', encoding='utf-8') as f:
            f.write(content)
    
    # Fix GuiPacketRegistration
    reg_path = os.path.join(BASE_PATH, "net/cyberpunk042/network/gui/GuiPacketRegistration.java")
    if os.path.exists(reg_path):
        with open(reg_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Fix payload.definitionJson() -> payload.fieldJson()
        content = content.replace("payload.definitionJson()", "payload.fieldJson()")
        
        # Fix DebugFieldS2CPayload constructor
        content = content.replace(
            'new DebugFieldS2CPayload(success, success ? "Debug field spawned" : "Failed to spawn")',
            'new DebugFieldS2CPayload("", success, success ? "Debug field spawned" : "Failed to spawn")'
        )
        content = content.replace(
            'new DebugFieldS2CPayload(success, success ? "Debug field removed" : "No debug field to remove")',
            'new DebugFieldS2CPayload("", success, success ? "Debug field removed" : "No debug field to remove")'
        )
        
        with open(reg_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("  ✅ Fixed GuiPacketRegistration payload methods")
    
    print()
    print("Done!")

if __name__ == "__main__":
    main()

