package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.CompactSelector;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ModalDialog;
import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.phase.ForcePhase;
import net.cyberpunk042.field.force.phase.ForcePolarity;
import net.cyberpunk042.field.force.zone.ForceZone;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full configuration modal for force fields.
 * 
 * <p>Two-column layout with CompactSelector navigation:
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  ⚡ Force Field Configuration                              [×]  │
 * ├────────────────────────────────┬────────────────────────────────┤
 * │  [+] ◀│ Zone 1 │▶ [+]         │  [+] ◀│ Phase 1 │▶ [+]         │
 * ├────────────────────────────────┼────────────────────────────────┤
 * │  Radius: ═══════════⬤══ 10.0  │  Start %: ═══════⬤════ 0%      │
 * │  Strength: ══⬤═════════ 0.15  │  End %: ══════════════⬤ 100%   │
 * │  Falloff: [Linear        ▼]   │  Polarity: [● PULL ▼]          │
 * │           [🗑 Delete Zone]     │  Multiplier: ════⬤═══ 1.0x     │
 * │                                │           [🗑 Delete Phase]    │
 * ├────────────────────────────────┴────────────────────────────────┤
 * │  CONSTRAINTS                                                     │
 * │  Max Velocity: ═══⬤═══  Vert Boost: ═══⬤═══  Damping: ═⬤═══   │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                          [Save]    [Cancel]      │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ForceConfigModal {
    
    // Falloff options (display name only, no prefix)
    private static final String[] FALLOFF_OPTIONS = {
        "Linear", "Quadratic", "Cubic", "Gaussian", "Exponential", "Constant"
    };
    
    private final ModalDialog dialog;
    private final TextRenderer textRenderer;
    private final Consumer<ForceFieldConfig> onSave;
    
    // Centralized state management (following LayerManager pattern)
    private final ForceConfigState state = new ForceConfigState();
    
    // Constraint fields
    private float maxVelocity = 1.5f;
    private float verticalBoost = 0.35f;
    private float damping = 0.02f;
    
    public ForceConfigModal(int screenWidth, int screenHeight, TextRenderer textRenderer,
                            ForceFieldConfig initialConfig, Consumer<ForceFieldConfig> onSave) {
        this.textRenderer = textRenderer;
        this.onSave = onSave;
        
        // Load initial config into state
        state.loadFrom(initialConfig != null ? initialConfig : ForceFieldConfig.DEFAULT);
        
        // Load constraints
        if (initialConfig != null) {
            maxVelocity = initialConfig.maxVelocity();
            verticalBoost = initialConfig.verticalBoost();
            damping = initialConfig.damping();
        }
        
        // Create modal - 80% of screen, centered
        int modalW = (int)(screenWidth * 0.8);
        int modalH = (int)(screenHeight * 0.75);
        
        dialog = new ModalDialog("⚡ Force Field Configuration", textRenderer, screenWidth, screenHeight)
            .size(modalW, modalH)
            .content(this::buildContent)
            .addAction("Save", this::save, true)
            .addAction("Cancel", () -> {});
        
        // Wire up selection change to rebuild UI
        state.setOnSelectionChange(() -> dialog.rebuild());
    }
    
    private List<ClickableWidget> buildContent(Bounds bounds, TextRenderer tr) {
        List<ClickableWidget> widgets = new ArrayList<>();
        
        int padding = GuiConstants.PADDING;
        int rowH = GuiConstants.WIDGET_HEIGHT;
        
        // Calculate two-column layout
        int contentW = bounds.width() - padding * 2;
        int colW = (contentW - padding) / 2;
        
        int leftX = bounds.x() + padding;
        int rightX = leftX + colW + padding;
        int topY = bounds.y() + padding;
        
        // ═══════════════════════════════════════════════════════════════════
        // LEFT COLUMN: ZONES
        // ═══════════════════════════════════════════════════════════════════
        int leftY = topY;
        
        // Zone selector (CompactSelector)
        List<String> zoneNames = new ArrayList<>();
        for (int i = 0; i < state.getZones().size(); i++) {
            zoneNames.add("Zone " + (i + 1));
        }
        if (zoneNames.isEmpty()) zoneNames.add("(none)");
        
        CompactSelector<String> zoneSelector = new CompactSelector<>("", tr, zoneNames, s -> s)
            .onSelect(name -> {
                int idx = zoneNames.indexOf(name);
                if (idx >= 0 && idx < state.getZones().size()) {
                    state.setSelectedZoneIndex(idx);
                }
            })
            .onAdd(state::addZone)
            .selectIndex(state.getSelectedZoneIndex());
        zoneSelector.setBounds(new Bounds(leftX, leftY, colW, 20));
        widgets.addAll(zoneSelector.getWidgets());
        leftY += 24;
        
        // Zone controls (only if zone exists)
        ZoneState zone = state.getSelectedZone();
        if (zone != null) {
            // Radius
            widgets.add(LabeledSlider.builder("Radius")
                .position(leftX, leftY).width(colW)
                .range(0.5f, 50f).initial(zone.radius).format("%.1f")
                .onChange(v -> zone.radius = v)
                .build());
            leftY += rowH + padding;
            
            // Strength
            widgets.add(LabeledSlider.builder("Strength")
                .position(leftX, leftY).width(colW)
                .range(0.01f, 1.0f).initial(zone.strength).format("%.3f")
                .onChange(v -> zone.strength = v)
                .build());
            leftY += rowH + padding;
            
            // Falloff dropdown - fixed label (no double prefix)
            final ZoneState finalZone = zone;
            widgets.add(CyclingButtonWidget.<String>builder(s -> Text.literal(s))
                .values(FALLOFF_OPTIONS)
                .initially(capitalize(zone.falloff))
                .build(leftX, leftY, colW, rowH, Text.literal("Falloff"),
                       (btn, val) -> finalZone.falloff = val.toLowerCase()));
            leftY += rowH + padding;
            
            // Delete button (only if >1 zone)
            if (state.getZones().size() > 1) {
                widgets.add(GuiWidgets.button(leftX, leftY, colW, "🗑 Delete Zone", 
                    "Remove this zone", () -> state.removeZone(state.getSelectedZoneIndex())));
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // RIGHT COLUMN: PHASES
        // ═══════════════════════════════════════════════════════════════════
        int rightY = topY;
        
        // Phase selector (CompactSelector)
        List<String> phaseNames = new ArrayList<>();
        for (int i = 0; i < state.getPhases().size(); i++) {
            phaseNames.add("Phase " + (i + 1));
        }
        if (phaseNames.isEmpty()) phaseNames.add("(none)");
        
        CompactSelector<String> phaseSelector = new CompactSelector<>("", tr, phaseNames, s -> s)
            .onSelect(name -> {
                int idx = phaseNames.indexOf(name);
                if (idx >= 0 && idx < state.getPhases().size()) {
                    state.setSelectedPhaseIndex(idx);
                }
            })
            .onAdd(state::addPhase)
            .selectIndex(state.getSelectedPhaseIndex());
        phaseSelector.setBounds(new Bounds(rightX, rightY, colW, 20));
        widgets.addAll(phaseSelector.getWidgets());
        rightY += 24;
        
        // Phase controls (only if phase exists)
        PhaseState phase = state.getSelectedPhase();
        if (phase != null) {
            // Start %
            widgets.add(LabeledSlider.builder("Start %")
                .position(rightX, rightY).width(colW)
                .range(0, 100).initial(phase.startPercent).format("%.0f%%").step(5)
                .onChange(v -> phase.startPercent = v)
                .build());
            rightY += rowH + padding;
            
            // End %
            widgets.add(LabeledSlider.builder("End %")
                .position(rightX, rightY).width(colW)
                .range(0, 100).initial(phase.endPercent).format("%.0f%%").step(5)
                .onChange(v -> phase.endPercent = v)
                .build());
            rightY += rowH + padding;
            
            // Polarity - Simple cycling button with all three polarities
            final PhaseState finalPhase = phase;
            ForcePolarity[] polarities = ForcePolarity.values();
            widgets.add(CyclingButtonWidget.<ForcePolarity>builder(p -> Text.literal("● " + p.name()))
                .values(polarities)
                .initially(phase.polarity)
                .build(rightX, rightY, colW, rowH, Text.literal("Polarity"),
                       (btn, val) -> finalPhase.polarity = val));
            rightY += rowH + padding;
            
            // Multiplier
            widgets.add(LabeledSlider.builder("Multiplier")
                .position(rightX, rightY).width(colW)
                .range(0.1f, 5.0f).initial(phase.multiplier).format("%.1fx")
                .onChange(v -> phase.multiplier = v)
                .build());
            rightY += rowH + padding;
            
            // Delete button (only if >1 phase)
            if (state.getPhases().size() > 1) {
                widgets.add(GuiWidgets.button(rightX, rightY, colW, "🗑 Delete Phase",
                    "Remove this phase", () -> state.removePhase(state.getSelectedPhaseIndex())));
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // BOTTOM SECTION: CONSTRAINTS
        // ═══════════════════════════════════════════════════════════════════
        int bottomY = bounds.y() + bounds.height() - 50;
        
        // Constraints row
        int thirdW = (contentW - padding * 2) / 3;
        
        // Max Velocity
        widgets.add(LabeledSlider.builder("Max Vel")
            .position(leftX, bottomY).width(thirdW)
            .range(0.1f, 3.0f).initial(maxVelocity).format("%.2f")
            .onChange(v -> maxVelocity = v)
            .build());
        
        // Vertical Boost
        widgets.add(LabeledSlider.builder("V.Boost")
            .position(leftX + thirdW + padding, bottomY).width(thirdW)
            .range(0f, 1.0f).initial(verticalBoost).format("%.2f")
            .onChange(v -> verticalBoost = v)
            .build());
        
        // Damping
        widgets.add(LabeledSlider.builder("Damping")
            .position(leftX + thirdW * 2 + padding * 2, bottomY).width(thirdW)
            .range(0f, 0.5f).initial(damping).format("%.3f")
            .onChange(v -> damping = v)
            .build());
        
        return widgets;
    }
    
    private void save() {
        // Build config from state
        ForceFieldConfig.Builder builder = ForceFieldConfig.builder()
            .maxVelocity(maxVelocity)
            .verticalBoost(verticalBoost)
            .damping(damping);
        
        for (ZoneState z : state.getZones()) {
            builder.zone(z.radius, z.strength, z.falloff);
        }
        
        for (PhaseState p : state.getPhases()) {
            builder.phase(ForcePhase.builder()
                .start(p.startPercent)
                .end(p.endPercent)
                .polarity(p.polarity)
                .strength(p.multiplier)
                .build());
        }
        
        ForceFieldConfig config = builder.build();
        onSave.accept(config);
        dialog.hide();
        
        Logging.GUI.topic("force").info("Saved force config with {} zones, {} phases", 
            state.getZones().size(), state.getPhases().size());
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "Linear";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    public ModalDialog getDialog() {
        return dialog;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ForceConfigState - Centralized state management (follows LayerManager pattern)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages zones, phases, and selection state.
     * Follows LayerManager pattern for selection management.
     */
    private class ForceConfigState {
        private final List<ZoneState> zones = new ArrayList<>();
        private final List<PhaseState> phases = new ArrayList<>();
        private int selectedZoneIndex = 0;
        private int selectedPhaseIndex = 0;
        private Runnable onSelectionChange;
        
        void setOnSelectionChange(Runnable callback) {
            this.onSelectionChange = callback;
        }
        
        // Zone selection
        ZoneState getSelectedZone() {
            if (zones.isEmpty()) return null;
            return zones.get(Math.min(selectedZoneIndex, zones.size() - 1));
        }
        
        int getSelectedZoneIndex() { return selectedZoneIndex; }
        
        void setSelectedZoneIndex(int index) {
            selectedZoneIndex = Math.max(0, Math.min(index, zones.size() - 1));
            if (onSelectionChange != null) onSelectionChange.run();
        }
        
        // Phase selection
        PhaseState getSelectedPhase() {
            if (phases.isEmpty()) return null;
            return phases.get(Math.min(selectedPhaseIndex, phases.size() - 1));
        }
        
        int getSelectedPhaseIndex() { return selectedPhaseIndex; }
        
        void setSelectedPhaseIndex(int index) {
            selectedPhaseIndex = Math.max(0, Math.min(index, phases.size() - 1));
            if (onSelectionChange != null) onSelectionChange.run();
        }
        
        // Zone CRUD
        List<ZoneState> getZones() { return zones; }
        
        void addZone() {
            float minRadius = zones.stream().map(z -> z.radius).min(Float::compare).orElse(10f);
            zones.add(new ZoneState(Math.max(1, minRadius * 0.5f), 0.2f, "linear"));
            selectedZoneIndex = zones.size() - 1;
            if (onSelectionChange != null) onSelectionChange.run();
            Logging.GUI.topic("force").debug("Added zone, total: {}", zones.size());
        }
        
        void removeZone(int index) {
            if (zones.size() > 1 && index >= 0 && index < zones.size()) {
                zones.remove(index);
                selectedZoneIndex = Math.min(selectedZoneIndex, zones.size() - 1);
                if (onSelectionChange != null) onSelectionChange.run();
                Logging.GUI.topic("force").debug("Removed zone {}, total: {}", index, zones.size());
            }
        }
        
        // Phase CRUD
        List<PhaseState> getPhases() { return phases; }
        
        void addPhase() {
            float lastEnd = phases.isEmpty() ? 0 : phases.get(phases.size() - 1).endPercent;
            if (lastEnd < 100) {
                phases.add(new PhaseState(lastEnd, 100, ForcePolarity.PUSH, 1.0f));
            } else {
                phases.add(new PhaseState(90, 100, ForcePolarity.PUSH, 1.0f));
            }
            selectedPhaseIndex = phases.size() - 1;
            if (onSelectionChange != null) onSelectionChange.run();
            Logging.GUI.topic("force").debug("Added phase, total: {}", phases.size());
        }
        
        void removePhase(int index) {
            if (phases.size() > 1 && index >= 0 && index < phases.size()) {
                phases.remove(index);
                selectedPhaseIndex = Math.min(selectedPhaseIndex, phases.size() - 1);
                if (onSelectionChange != null) onSelectionChange.run();
                Logging.GUI.topic("force").debug("Removed phase {}, total: {}", index, phases.size());
            }
        }
        
        // Load from config
        void loadFrom(ForceFieldConfig config) {
            zones.clear();
            phases.clear();
            
            for (ForceZone zone : config.zones()) {
                zones.add(new ZoneState(zone.radius(), zone.strength(), zone.falloffName()));
            }
            if (zones.isEmpty()) zones.add(new ZoneState(10f, 0.1f, "linear"));
            
            for (ForcePhase phase : config.phases()) {
                phases.add(new PhaseState(phase.startPercent(), phase.endPercent(), 
                                          phase.polarity(), phase.strengthMultiplier()));
            }
            if (phases.isEmpty()) phases.add(new PhaseState(0, 100, ForcePolarity.PULL, 1.0f));
            
            selectedZoneIndex = 0;
            selectedPhaseIndex = 0;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Mutable State Classes
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static class ZoneState {
        float radius;
        float strength;
        String falloff;
        
        ZoneState(float radius, float strength, String falloff) {
            this.radius = radius;
            this.strength = strength;
            this.falloff = falloff;
        }
    }
    
    private static class PhaseState {
        float startPercent;
        float endPercent;
        ForcePolarity polarity;
        float multiplier;
        
        PhaseState(float start, float end, ForcePolarity polarity, float multiplier) {
            this.startPercent = start;
            this.endPercent = end;
            this.polarity = polarity;
            this.multiplier = multiplier;
        }
    }
}
