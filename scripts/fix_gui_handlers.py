#!/usr/bin/env python3
"""
Fix compile errors in DebugFieldTracker and GuiPacketRegistration.
"""

import os

BASE_PATH = "/mnt/c/Users/Jean/the-virus-block-template-1.21.6"

def fix_file(filepath, replacements):
    full_path = os.path.join(BASE_PATH, filepath)
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        if old in content:
            content = content.replace(old, new)
            print(f"  ✅ Fixed: {old[:50]}...")
        else:
            print(f"  ⚠️  Not found: {old[:50]}...")
    
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    print("=" * 60)
    print("Fixing GUI handler compile errors")
    print("=" * 60)
    
    # Fix DebugFieldTracker.java
    print("\n1. DebugFieldTracker.java")
    fix_file("src/main/java/net/cyberpunk042/network/gui/DebugFieldTracker.java", [
        # Remove unnecessary cast - getWorld() returns ServerWorld directly
        ("(ServerWorld) player.getWorld()", "player.getWorld()"),
        # Remove unused Identifier import
        ("import net.minecraft.util.Identifier;\n", ""),
        # Fix FieldRegistry.register - takes just definition, not (id, definition)
        # Replace the whole block that creates debugDef
        (
            '''            // Register temporarily with unique ID
            String debugId = "debug_" + playerUuid.toString().substring(0, 8) + "_" + (++debugCounter);
            Identifier defId = Identifier.of("the-virus-block", debugId);
            FieldRegistry.register(defId, definition);''',
            '''            // Register temporarily with unique ID embedded in the definition
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
        ),
    ])
    
    # Fix GuiPacketRegistration.java
    print("\n2. GuiPacketRegistration.java")
    fix_file("src/main/java/net/cyberpunk042/network/gui/GuiPacketRegistration.java", [
        # Fix payload.definitionJson() -> payload.fieldJson()
        ("payload.definitionJson()", "payload.fieldJson()"),
        # Fix DebugFieldS2CPayload constructor - takes (String fieldJson, boolean active, String status)
        ('new DebugFieldS2CPayload(success, success ? "Debug field spawned" : "Failed to spawn")',
         'new DebugFieldS2CPayload("", success, success ? "Debug field spawned" : "Failed to spawn")'),
        ('new DebugFieldS2CPayload(success, success ? "Debug field removed" : "No debug field to remove")',
         'new DebugFieldS2CPayload("", success, success ? "Debug field removed" : "No debug field to remove")'),
    ])
    
    print("\nDone!")

if __name__ == "__main__":
    main()











