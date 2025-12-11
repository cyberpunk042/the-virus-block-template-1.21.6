#!/usr/bin/env python3
"""
Batch B: Analyze and implement remaining TODO items

Tasks to analyze:
1. G-FCMD-11: /field binding commands
2. G-MOD-01/02: bobbing, breathing modifiers  
3. G-LAYER-01/02: blendMode, order
4. G39/G40: Client config

For each:
- Check if infrastructure exists
- Identify patterns to follow
- Generate implementation if straightforward
- Suggest manual steps if complex
"""

import re
from pathlib import Path
from dataclasses import dataclass
from typing import List, Optional

PROJECT_ROOT = Path(__file__).parent.parent

# Key files
MODIFIERS = PROJECT_ROOT / "src/main/java/net/cyberpunk042/visual/animation/Modifiers.java"
FIELD_EDIT_STATE = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"
BINDING_SOURCES = PROJECT_ROOT / "src/main/java/net/cyberpunk042/field/influence/BindingSources.java"
BINDING_CONFIG = PROJECT_ROOT / "src/main/java/net/cyberpunk042/field/influence/BindingConfig.java"
FIELD_EDIT_COMMANDS = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/command/FieldEditCommands.java"
GUI_CONFIG_DIR = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/config"

@dataclass
class AnalysisResult:
    item: str
    status: str  # 'done', 'partial', 'missing', 'skip'
    details: str
    suggestion: Optional[str] = None
    code_to_add: Optional[str] = None

def analyze_modifiers():
    """Check if bobbing/breathing modifiers are handled by CommandScanner"""
    print("\n=== 1. Analyzing Modifiers (bobbing, breathing) ===")
    
    if not MODIFIERS.exists():
        return AnalysisResult("G-MOD-01/02", "missing", "Modifiers.java not found")
    
    content = MODIFIERS.read_text(encoding='utf-8')
    
    # Check for @Range annotations
    has_bobbing_range = '@Range' in content and 'bobbing' in content
    has_breathing_range = '@Range' in content and 'breathing' in content
    has_default = 'DEFAULT' in content
    
    # Check FieldEditCommands for modifiers
    if FIELD_EDIT_COMMANDS.exists():
        cmd_content = FIELD_EDIT_COMMANDS.read_text(encoding='utf-8')
        has_modifiers_cmd = 'modifiers' in cmd_content.lower()
    else:
        has_modifiers_cmd = False
    
    # Check FieldEditState for modifiers field
    if FIELD_EDIT_STATE.exists():
        state_content = FIELD_EDIT_STATE.read_text(encoding='utf-8')
        has_modifiers_state = 'Modifiers' in state_content and '@StateField' in state_content
    else:
        has_modifiers_state = False
    
    print(f"  Modifiers.java:")
    print(f"    @Range on bobbing: {has_bobbing_range}")
    print(f"    @Range on breathing: {has_breathing_range}")
    print(f"    Has DEFAULT: {has_default}")
    print(f"  FieldEditState has modifiers: {has_modifiers_state}")
    print(f"  FieldEditCommands has modifiers: {has_modifiers_cmd}")
    
    if has_bobbing_range and has_breathing_range and has_modifiers_state:
        if has_modifiers_cmd:
            return AnalysisResult(
                "G-MOD-01/02", "done",
                "Modifiers already have @Range and are in FieldEditState. Commands exist.",
                "Just verify CommandScanner picks them up"
            )
        else:
            return AnalysisResult(
                "G-MOD-01/02", "partial",
                "Modifiers have @Range but not in commands yet",
                "Add CommandScanner.scanRecord(Modifiers.class, 'modifiers', edit) to buildAnimationCommands()",
                code_to_add="""
        // Modifiers
        var modifiers = ClientCommandManager.literal("modifiers");
        CommandScanner.scanRecord(Modifiers.class, "modifiers", modifiers);
        edit.then(modifiers);"""
            )
    else:
        return AnalysisResult(
            "G-MOD-01/02", "missing",
            f"Missing: @Range={not (has_bobbing_range and has_breathing_range)}, state={not has_modifiers_state}",
            "Need to add @Range annotations to Modifiers record fields"
        )

def analyze_layer_blendmode():
    """Check if layer blendMode/order are used by renderer"""
    print("\n=== 2. Analyzing Layer blendMode/order ===")
    
    # Check LayerState in FieldEditState
    if FIELD_EDIT_STATE.exists():
        content = FIELD_EDIT_STATE.read_text(encoding='utf-8')
        has_blend = 'blendMode' in content
        has_order = 'order' in content
        print(f"  FieldEditState.LayerState has blendMode: {has_blend}")
        print(f"  FieldEditState.LayerState has order: {has_order}")
    else:
        has_blend = False
        has_order = False
    
    # Check if renderer uses these
    renderer_files = list((PROJECT_ROOT / "src/client/java/net/cyberpunk042/client").rglob("*Renderer*.java"))
    renderer_uses_blend = False
    renderer_uses_order = False
    
    for rf in renderer_files:
        rc = rf.read_text(encoding='utf-8')
        if 'blendMode' in rc or 'BlendMode' in rc:
            renderer_uses_blend = True
            print(f"    Found blendMode in {rf.name}")
        if 'layer.order' in rc or 'getOrder' in rc:
            renderer_uses_order = True
            print(f"    Found order in {rf.name}")
    
    if not renderer_uses_blend and not renderer_uses_order:
        return AnalysisResult(
            "G-LAYER-01/02", "skip",
            "Layer blendMode/order exist in state but renderer doesn't use them",
            "Skip for now - add when renderer supports layer blending"
        )
    
    return AnalysisResult(
        "G-LAYER-01/02", "partial",
        f"State has fields, renderer uses blend={renderer_uses_blend}, order={renderer_uses_order}",
        "Add layer blendMode dropdown and order slider to LayerPanel"
    )

def analyze_binding_commands():
    """Check binding infrastructure for /field binding commands"""
    print("\n=== 3. Analyzing Binding Commands ===")
    
    # Check BindingSources
    if BINDING_SOURCES.exists():
        content = BINDING_SOURCES.read_text(encoding='utf-8')
        sources = re.findall(r'register\("(\w+)"', content)
        print(f"  Available binding sources: {sources}")
    else:
        sources = []
        print("  BindingSources.java not found")
    
    # Check BindingConfig
    if BINDING_CONFIG.exists():
        content = BINDING_CONFIG.read_text(encoding='utf-8')
        has_from_json = 'fromJson' in content
        has_to_json = 'toJson' in content
        print(f"  BindingConfig has fromJson: {has_from_json}")
        print(f"  BindingConfig has toJson: {has_to_json}")
    else:
        has_from_json = False
        has_to_json = False
    
    # Check if FieldEditState has bindings
    if FIELD_EDIT_STATE.exists():
        state_content = FIELD_EDIT_STATE.read_text(encoding='utf-8')
        has_bindings = 'bindings' in state_content.lower() or 'Binding' in state_content
        print(f"  FieldEditState has bindings: {has_bindings}")
    else:
        has_bindings = False
    
    if not has_bindings:
        return AnalysisResult(
            "G-FCMD-11", "missing",
            "FieldEditState doesn't have bindings field yet",
            """Need to:
1. Add List<BindingConfig> bindings field to FieldEditState
2. Add methods: addBinding(), removeBinding(), listBindings()
3. Create FieldBindingCommands.java with list/add/remove/clear commands""",
            code_to_add="""
// Add to FieldEditState.java:
@StateField private List<BindingConfig> bindings = new ArrayList<>();

public void addBinding(String property, String source) {
    bindings.add(new BindingConfig(property, source));
    markDirty();
}

public boolean removeBinding(String property) {
    boolean removed = bindings.removeIf(b -> b.property().equals(property));
    if (removed) markDirty();
    return removed;
}

public List<BindingConfig> getBindings() {
    return Collections.unmodifiableList(bindings);
}

public void clearBindings() {
    bindings.clear();
    markDirty();
}"""
        )
    
    return AnalysisResult(
        "G-FCMD-11", "partial",
        f"Infrastructure exists: sources={len(sources)}, bindings in state={has_bindings}",
        "Create /field binding commands in FieldBindingCommands.java"
    )

def analyze_client_config():
    """Check client config infrastructure"""
    print("\n=== 4. Analyzing Client Config ===")
    
    config_dir = GUI_CONFIG_DIR
    config_file = config_dir / "GuiConfig.java"
    
    if config_file.exists():
        content = config_file.read_text(encoding='utf-8')
        has_undo = 'maxUndoSteps' in content
        has_tooltips = 'showTooltips' in content
        has_tab = 'rememberTabState' in content
        has_debug = 'debugMenuEnabled' in content
        print(f"  GuiConfig exists with:")
        print(f"    maxUndoSteps: {has_undo}")
        print(f"    showTooltips: {has_tooltips}")
        print(f"    rememberTabState: {has_tab}")
        print(f"    debugMenuEnabled: {has_debug}")
        
        if all([has_undo, has_tooltips, has_tab, has_debug]):
            return AnalysisResult("G39/G40", "done", "GuiConfig already complete")
        else:
            missing = []
            if not has_undo: missing.append('maxUndoSteps')
            if not has_tooltips: missing.append('showTooltips')
            if not has_tab: missing.append('rememberTabState')
            if not has_debug: missing.append('debugMenuEnabled')
            return AnalysisResult(
                "G39/G40", "partial",
                f"GuiConfig missing: {missing}",
                "Add missing fields to GuiConfig"
            )
    else:
        print(f"  GuiConfig.java not found")
        print(f"  Config dir exists: {config_dir.exists()}")
        
        return AnalysisResult(
            "G39/G40", "missing",
            "GuiConfig.java doesn't exist",
            "Create GuiConfig record with load/save to JSON",
            code_to_add="""
// Create: src/client/java/net/cyberpunk042/client/gui/config/GuiConfig.java
package net.cyberpunk042.client.gui.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public record GuiConfig(
    int maxUndoSteps,
    boolean showTooltips,
    boolean rememberTabState,
    boolean debugMenuEnabled
) {
    public static final GuiConfig DEFAULT = new GuiConfig(50, true, true, false);
    
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("the-virus-block/gui_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static GuiConfig instance;
    
    public static GuiConfig get() {
        if (instance == null) instance = load();
        return instance;
    }
    
    public static GuiConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, GuiConfig.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DEFAULT;
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            instance = this;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public GuiConfig withMaxUndoSteps(int v) { return new GuiConfig(v, showTooltips, rememberTabState, debugMenuEnabled); }
    public GuiConfig withShowTooltips(boolean v) { return new GuiConfig(maxUndoSteps, v, rememberTabState, debugMenuEnabled); }
    public GuiConfig withRememberTabState(boolean v) { return new GuiConfig(maxUndoSteps, showTooltips, v, debugMenuEnabled); }
    public GuiConfig withDebugMenuEnabled(boolean v) { return new GuiConfig(maxUndoSteps, showTooltips, rememberTabState, v); }
}"""
        )

def generate_summary(results: List[AnalysisResult]):
    """Generate final summary and action plan"""
    print("\n" + "="*70)
    print("BATCH B ANALYSIS SUMMARY")
    print("="*70)
    
    for r in results:
        icon = {'done': '✅', 'partial': '🔶', 'missing': '❌', 'skip': '⏭️'}[r.status]
        print(f"\n{icon} {r.item}: {r.status.upper()}")
        print(f"   {r.details}")
        if r.suggestion:
            print(f"   💡 {r.suggestion}")
    
    print("\n" + "-"*70)
    print("ACTION PLAN")
    print("-"*70)
    
    actions = []
    for r in results:
        if r.status == 'done':
            continue
        if r.status == 'skip':
            actions.append(f"⏭️  {r.item}: Skip (renderer doesn't use)")
        elif r.code_to_add:
            actions.append(f"🔧 {r.item}: Generate code (see below)")
        else:
            actions.append(f"📝 {r.item}: Manual implementation needed")
    
    for i, action in enumerate(actions, 1):
        print(f"  {i}. {action}")
    
    # Output code suggestions
    code_results = [r for r in results if r.code_to_add]
    if code_results:
        print("\n" + "="*70)
        print("CODE TO ADD")
        print("="*70)
        for r in code_results:
            print(f"\n### {r.item} ###")
            print(r.code_to_add)

def main():
    print("="*70)
    print("BATCH B: Remaining Items Analysis")
    print("="*70)
    
    results = []
    
    # 1. Modifiers
    results.append(analyze_modifiers())
    
    # 2. Layer blend/order
    results.append(analyze_layer_blendmode())
    
    # 3. Binding commands
    results.append(analyze_binding_commands())
    
    # 4. Client config
    results.append(analyze_client_config())
    
    # Summary
    generate_summary(results)
    
    print("\n" + "="*70)
    print("✅ Analysis complete!")
    print("="*70)

if __name__ == "__main__":
    main()

