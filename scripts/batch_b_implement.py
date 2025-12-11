#!/usr/bin/env python3
"""
Batch B: Implement remaining TODO items

1. Add @Range annotations to Modifiers.java
2. Add modifiers commands to FieldEditCommands.java
3. Create FieldBindingCommands.java
"""

import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent

# File paths
MODIFIERS = PROJECT_ROOT / "src/main/java/net/cyberpunk042/field/Modifiers.java"
FIELD_EDIT_COMMANDS = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/command/FieldEditCommands.java"
BINDING_COMMANDS = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/command/FieldBindingCommands.java"

def add_range_annotations():
    """Add @Range annotations to Modifiers.java for CommandScanner support"""
    print("\n=== 1. Adding @Range annotations to Modifiers.java ===")
    
    if not MODIFIERS.exists():
        print("  ❌ Modifiers.java not found")
        return False
    
    content = MODIFIERS.read_text(encoding='utf-8')
    
    # Check if already has @Range
    if '@Range' in content:
        print("  ⚠️  Already has @Range annotations")
        return True
    
    # Add import
    if 'import net.cyberpunk042.visual.validation.Range;' not in content:
        content = content.replace(
            'import net.cyberpunk042.util.json.JsonField;',
            '''import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;'''
        )
        print("  + Added Range/ValueRange imports")
    
    # Add @Range annotations to record fields
    # Multipliers (0.1 to 5.0)
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float radiusMultiplier)',
        r'@Range(ValueRange.POSITIVE) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float strengthMultiplier)',
        r'@Range(ValueRange.POSITIVE) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float alphaMultiplier)',
        r'@Range(ValueRange.UNIT_INTERVAL) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float spinMultiplier)',
        r'@Range(ValueRange.POSITIVE) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float visualScale)',
        r'@Range(ValueRange.POSITIVE) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float tiltMultiplier)',
        r'@Range(ValueRange.UNIT_INTERVAL) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float swirlStrength)',
        r'@Range(ValueRange.UNIT_INTERVAL) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float bobbing)',
        r'@Range(ValueRange.UNIT_INTERVAL) \1\2',
        content
    )
    content = re.sub(
        r'(@JsonField\([^)]+\)\s*)(float breathing)',
        r'@Range(ValueRange.UNIT_INTERVAL) \1\2',
        content
    )
    
    MODIFIERS.write_text(content, encoding='utf-8')
    print("  ✓ Added @Range annotations to Modifiers record")
    return True

def add_modifiers_commands():
    """Add modifiers commands to FieldEditCommands.java"""
    print("\n=== 2. Adding modifiers commands to FieldEditCommands.java ===")
    
    if not FIELD_EDIT_COMMANDS.exists():
        print("  ❌ FieldEditCommands.java not found")
        return False
    
    content = FIELD_EDIT_COMMANDS.read_text(encoding='utf-8')
    
    # Check if already has modifiers commands
    if 'buildModifiersCommands' in content or '"modifiers.' in content:
        print("  ⚠️  Already has modifiers commands")
        return True
    
    # Find the animation commands section and add modifiers after it
    # Look for the pattern where we call buildAnimationCommands
    
    # Add import if needed
    if 'import net.cyberpunk042.field.Modifiers;' not in content:
        content = content.replace(
            'import net.cyberpunk042.visual.animation.*;',
            '''import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.field.Modifiers;'''
        )
        print("  + Added Modifiers import")
    
    # Add method call in registerClientCommands
    if 'buildModifiersCommands(edit)' not in content:
        # Find where buildAnimationCommands is called and add after it
        content = content.replace(
            'buildAnimationCommands(edit);',
            '''buildAnimationCommands(edit);
        buildModifiersCommands(edit);'''
        )
        print("  + Added buildModifiersCommands call")
    
    # Add the method itself before the closing brace of the class
    modifiers_method = '''
    // ═══════════════════════════════════════════════════════════════════════════
    // MODIFIERS COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildModifiersCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var modifiers = ClientCommandManager.literal("modifiers");
        
        // Use CommandScanner to auto-generate from @Range annotations
        CommandScanner.scanRecord(Modifiers.class, "modifiers", modifiers);
        
        // Manual commands for booleans (not covered by @Range)
        FieldEditKnob.toggle("modifiers.inverted", modifiers)
            .path("modifiers.inverted")
            .display("Inverted")
            .tooltip("Inverts push/pull effects")
            .register();
        
        FieldEditKnob.toggle("modifiers.pulsing", modifiers)
            .path("modifiers.pulsing")
            .display("Pulsing")
            .tooltip("Enables pulse animation")
            .register();
        
        edit.then(modifiers);
    }
'''
    
    # Find the last method and insert before class closing
    if 'private static void buildModifiersCommands' not in content:
        # Insert before the final closing brace
        last_brace = content.rfind('}')
        content = content[:last_brace] + modifiers_method + '\n' + content[last_brace:]
        print("  ✓ Added buildModifiersCommands method")
    
    FIELD_EDIT_COMMANDS.write_text(content, encoding='utf-8')
    return True

def create_binding_commands():
    """Create FieldBindingCommands.java"""
    print("\n=== 3. Creating FieldBindingCommands.java ===")
    
    if BINDING_COMMANDS.exists():
        print("  ⚠️  FieldBindingCommands.java already exists")
        return True
    
    content = '''package net.cyberpunk042.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.BindingSources;
import net.cyberpunk042.field.influence.InterpolationCurve;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * Client-side commands for managing field bindings.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/field binding list - List all active bindings</li>
 *   <li>/field binding add &lt;property&gt; &lt;source&gt; - Add a binding</li>
 *   <li>/field binding remove &lt;property&gt; - Remove a binding</li>
 *   <li>/field binding clear - Remove all bindings</li>
 *   <li>/field binding sources - List available binding sources</li>
 * </ul>
 */
public class FieldBindingCommands {
    
    /**
     * Builds the /field binding command tree.
     */
    public static LiteralArgumentBuilder<FabricClientCommandSource> buildBindingCommand() {
        return ClientCommandManager.literal("binding")
            .then(ClientCommandManager.literal("list")
                .executes(FieldBindingCommands::handleList))
            .then(ClientCommandManager.literal("sources")
                .executes(FieldBindingCommands::handleSources))
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("property", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        // Suggest common properties that can be bound
                        for (String prop : BINDABLE_PROPERTIES) {
                            builder.suggest(prop);
                        }
                        return builder.buildFuture();
                    })
                    .then(ClientCommandManager.argument("source", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            // Suggest available binding sources
                            for (String source : BindingSources.getAvailableIds()) {
                                builder.suggest(source);
                            }
                            return builder.buildFuture();
                        })
                        .executes(FieldBindingCommands::handleAddSimple)
                        .then(ClientCommandManager.argument("inputMin", FloatArgumentType.floatArg())
                            .then(ClientCommandManager.argument("inputMax", FloatArgumentType.floatArg())
                                .then(ClientCommandManager.argument("outputMin", FloatArgumentType.floatArg())
                                    .then(ClientCommandManager.argument("outputMax", FloatArgumentType.floatArg())
                                        .executes(FieldBindingCommands::handleAddFull)
                                        .then(ClientCommandManager.argument("curve", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                for (InterpolationCurve curve : InterpolationCurve.values()) {
                                                    builder.suggest(curve.getId());
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(FieldBindingCommands::handleAddWithCurve)))))))))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("property", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        // Suggest currently bound properties
                        FieldEditState state = FieldEditStateHolder.get();
                        for (var binding : state.getBindings()) {
                            builder.suggest(binding.property());
                        }
                        return builder.buildFuture();
                    })
                    .executes(FieldBindingCommands::handleRemove)))
            .then(ClientCommandManager.literal("clear")
                .executes(FieldBindingCommands::handleClear));
    }
    
    // Common properties that can be bound
    private static final String[] BINDABLE_PROPERTIES = {
        "radius",
        "fill.alpha",
        "appearance.primaryColor",
        "modifiers.alphaMultiplier",
        "modifiers.radiusMultiplier",
        "spin.speed",
        "pulse.amplitude"
    };
    
    private static int handleList(CommandContext<FabricClientCommandSource> ctx) {
        FieldEditState state = FieldEditStateHolder.get();
        var bindings = state.getBindings();
        
        if (bindings.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7No active bindings"));
            return 0;
        }
        
        ctx.getSource().sendFeedback(Text.literal("§6Active Bindings:"));
        for (var binding : bindings) {
            ctx.getSource().sendFeedback(Text.literal(String.format(
                "  §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f] %s",
                binding.property(),
                binding.source(),
                binding.inputMin(), binding.inputMax(),
                binding.outputMin(), binding.outputMax(),
                binding.curve().getId()
            )));
        }
        return bindings.size();
    }
    
    private static int handleSources(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("§6Available Binding Sources:"));
        for (String source : BindingSources.getAvailableIds()) {
            ctx.getSource().sendFeedback(Text.literal("  §b" + source));
        }
        return BindingSources.getAvailableIds().size();
    }
    
    private static int handleAddSimple(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s", property, source)));
        return 1;
    }
    
    private static int handleAddFull(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        float inputMin = FloatArgumentType.getFloat(ctx, "inputMin");
        float inputMax = FloatArgumentType.getFloat(ctx, "inputMax");
        float outputMin = FloatArgumentType.getFloat(ctx, "outputMin");
        float outputMax = FloatArgumentType.getFloat(ctx, "outputMax");
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .inputRange(inputMin, inputMax)
            .outputRange(outputMin, outputMax)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f]",
            property, source, inputMin, inputMax, outputMin, outputMax)));
        return 1;
    }
    
    private static int handleAddWithCurve(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        float inputMin = FloatArgumentType.getFloat(ctx, "inputMin");
        float inputMax = FloatArgumentType.getFloat(ctx, "inputMax");
        float outputMin = FloatArgumentType.getFloat(ctx, "outputMin");
        float outputMax = FloatArgumentType.getFloat(ctx, "outputMax");
        String curveStr = StringArgumentType.getString(ctx, "curve");
        InterpolationCurve curve = InterpolationCurve.fromId(curveStr);
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .inputRange(inputMin, inputMax)
            .outputRange(outputMin, outputMax)
            .curve(curve)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f] %s",
            property, source, inputMin, inputMax, outputMin, outputMax, curve.getId())));
        return 1;
    }
    
    private static int handleRemove(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        FieldEditState state = FieldEditStateHolder.get();
        
        if (state.removeBinding(property)) {
            ctx.getSource().sendFeedback(Text.literal("§aRemoved binding for: §e" + property));
            return 1;
        } else {
            ctx.getSource().sendError(Text.literal("No binding found for: " + property));
            return 0;
        }
    }
    
    private static int handleClear(CommandContext<FabricClientCommandSource> ctx) {
        FieldEditState state = FieldEditStateHolder.get();
        int count = state.getBindings().size();
        state.clearBindings();
        ctx.getSource().sendFeedback(Text.literal("§aCleared " + count + " bindings"));
        return count;
    }
}
'''
    
    BINDING_COMMANDS.write_text(content, encoding='utf-8')
    print("  ✓ Created FieldBindingCommands.java")
    return True

def update_binding_config():
    """Add property field to BindingConfig if missing"""
    print("\n=== 4. Checking BindingConfig for property field ===")
    
    binding_config = PROJECT_ROOT / "src/main/java/net/cyberpunk042/field/influence/BindingConfig.java"
    if not binding_config.exists():
        print("  ❌ BindingConfig.java not found")
        return False
    
    content = binding_config.read_text(encoding='utf-8')
    
    # Check if property field exists
    if 'String property' in content:
        print("  ✓ BindingConfig already has property field")
        return True
    
    # Need to add property field to the record
    print("  ⚠️  BindingConfig needs 'property' field for per-property bindings")
    print("  📝 Manual update needed - this is a complex record modification")
    return False

def update_field_edit_state():
    """Add binding methods to FieldEditState if missing"""
    print("\n=== 5. Checking FieldEditState for binding methods ===")
    
    state_file = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"
    if not state_file.exists():
        print("  ❌ FieldEditState.java not found")
        return False
    
    content = state_file.read_text(encoding='utf-8')
    
    methods_needed = []
    if 'addBinding' not in content:
        methods_needed.append('addBinding')
    if 'removeBinding' not in content:
        methods_needed.append('removeBinding')
    if 'getBindings' not in content:
        methods_needed.append('getBindings')
    if 'clearBindings' not in content:
        methods_needed.append('clearBindings')
    
    if not methods_needed:
        print("  ✓ FieldEditState already has binding methods")
        return True
    
    print(f"  ⚠️  Missing methods: {methods_needed}")
    print("  📝 Will add binding methods to FieldEditState")
    
    # Add import if needed
    if 'import net.cyberpunk042.field.influence.BindingConfig;' not in content:
        content = content.replace(
            'import net.cyberpunk042.field.BeamConfig;',
            '''import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.influence.BindingConfig;'''
        )
    
    # Add bindings field if not present
    if 'List<BindingConfig> bindings' not in content:
        # Find a good place to add it - after the layers field
        content = content.replace(
            '@StateField private List<LayerState> layers = new ArrayList<>();',
            '''@StateField private List<LayerState> layers = new ArrayList<>();
    private List<BindingConfig> bindings = new ArrayList<>();'''
        )
    
    # Add methods before the final closing brace
    binding_methods = '''
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDING MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void addBinding(BindingConfig binding) {
        // Remove existing binding for same property if any
        bindings.removeIf(b -> b.property().equals(binding.property()));
        bindings.add(binding);
        markDirty();
    }
    
    public boolean removeBinding(String property) {
        boolean removed = bindings.removeIf(b -> b.property().equals(property));
        if (removed) markDirty();
        return removed;
    }
    
    public List<BindingConfig> getBindings() {
        return java.util.Collections.unmodifiableList(bindings);
    }
    
    public void clearBindings() {
        bindings.clear();
        markDirty();
    }
'''
    
    if 'public void addBinding' not in content:
        last_brace = content.rfind('}')
        content = content[:last_brace] + binding_methods + '\n' + content[last_brace:]
        print("  ✓ Added binding methods to FieldEditState")
    
    state_file.write_text(content, encoding='utf-8')
    return True

def register_binding_commands():
    """Register binding commands in FieldEditCommands"""
    print("\n=== 6. Registering binding commands ===")
    
    if not FIELD_EDIT_COMMANDS.exists():
        print("  ❌ FieldEditCommands.java not found")
        return False
    
    content = FIELD_EDIT_COMMANDS.read_text(encoding='utf-8')
    
    # Check if already registered
    if 'FieldBindingCommands' in content:
        print("  ✓ Binding commands already registered")
        return True
    
    # Add import
    content = content.replace(
        'import net.cyberpunk042.client.command.util.FieldEditKnob;',
        '''import net.cyberpunk042.client.command.util.FieldEditKnob;
import net.cyberpunk042.client.command.FieldBindingCommands;'''
    )
    
    # Add registration - find where we register other commands
    content = content.replace(
        'field.then(FieldProfileCommands.buildStatusCommand());',
        '''field.then(FieldProfileCommands.buildStatusCommand());
        field.then(FieldBindingCommands.buildBindingCommand());'''
    )
    
    FIELD_EDIT_COMMANDS.write_text(content, encoding='utf-8')
    print("  ✓ Registered binding commands")
    return True

def main():
    print("="*70)
    print("BATCH B: Implementing Remaining Items")
    print("="*70)
    
    results = []
    
    # 1. Add @Range annotations to Modifiers
    results.append(("Add @Range to Modifiers", add_range_annotations()))
    
    # 2. Add modifiers commands
    results.append(("Add modifiers commands", add_modifiers_commands()))
    
    # 3. Create binding commands
    results.append(("Create FieldBindingCommands", create_binding_commands()))
    
    # 4. Check BindingConfig
    results.append(("Check BindingConfig", update_binding_config()))
    
    # 5. Update FieldEditState with binding methods
    results.append(("Update FieldEditState", update_field_edit_state()))
    
    # 6. Register binding commands
    results.append(("Register binding commands", register_binding_commands()))
    
    print("\n" + "="*70)
    print("SUMMARY")
    print("="*70)
    
    for name, success in results:
        icon = "✓" if success else "❌"
        print(f"  {icon} {name}")
    
    print("\n" + "="*70)
    print("NEXT STEPS")
    print("="*70)
    print("  1. Run: ./gradlew build")
    print("  2. Fix any compile errors")
    print("  3. Test commands in-game:")
    print("     /field edit modifiers.bobbing 0.5")
    print("     /field binding list")
    print("     /field binding sources")
    print("     /field binding add radius player.health_percent")

if __name__ == "__main__":
    main()

