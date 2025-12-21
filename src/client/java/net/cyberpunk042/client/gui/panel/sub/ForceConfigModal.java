package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.CompactSelector;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ModalDialog;
import net.cyberpunk042.field.force.*;
import net.cyberpunk042.field.force.mode.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full configuration modal for force fields.
 * 
 * <h2>Layout Structure</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  ⚡ Force Field Configuration                              [×]  │
 * ├────────────────────────────────┬────────────────────────────────┤
 * │  ═══ ZONES ═══                │  ═══ PHASES ═══                │
 * │  [+] ◀│ Zone 1 │▶ [🗑]        │  [+] ◀│ Phase 1 │▶ [🗑]        │
 * │  Radius: ═══════════⬤══       │  Start %: ═══════⬤════        │
 * │  Strength: ══⬤═════════       │  End %: ══════════════⬤       │
 * │  Falloff: [Linear      ▼]     │  Polarity: [● PULL ▼]          │
 * │                               │  Multiplier: ════⬤═══          │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  MODE: [Radial ▼]  Max Vel: ═══⬤═══  Damping: ═⬤═══           │
 * │  Vertical Boost: ══⬤═══                                        │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                          [Save]    [Cancel]      │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ForceConfigModal {
    
    private static final String[] FALLOFF_OPTIONS = {
        "Linear", "Quadratic", "Cubic", "Gaussian", "Exponential", "Constant", "Inverse"
    };
    
    private final ModalDialog dialog;
    private final TextRenderer textRenderer;
    private final Consumer<ForceFieldConfig> onSave;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<ZoneState> zones = new ArrayList<>();
    private final List<PhaseState> phases = new ArrayList<>();
    private int selectedZoneIndex = 0;
    private int selectedPhaseIndex = 0;
    
    // Mode and global constraints
    private ForceMode mode = ForceMode.RADIAL;
    private float maxVelocity = 1.5f;
    private float verticalBoost = 0f;
    private float damping = 0.02f;
    
    // Mode-specific configs (initialized with defaults)
    private VortexModeConfig vortexConfig = VortexModeConfig.DEFAULT;
    private OrbitModeConfig orbitConfig = OrbitModeConfig.DEFAULT;
    private TornadoModeConfig tornadoConfig = TornadoModeConfig.DEFAULT;
    private RingModeConfig ringConfig = RingModeConfig.DEFAULT;
    private ImplosionModeConfig implosionConfig = ImplosionModeConfig.DEFAULT;
    private ExplosionModeConfig explosionConfig = ExplosionModeConfig.DEFAULT;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Constructor
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ForceConfigModal(int screenWidth, int screenHeight, TextRenderer textRenderer,
                            ForceFieldConfig initialConfig, Consumer<ForceFieldConfig> onSave) {
        this.textRenderer = textRenderer;
        this.onSave = onSave;
        
        loadFrom(initialConfig != null ? initialConfig : ForceFieldConfig.DEFAULT);
        
        // Modal size: 75% of screen width, 70% height
        int modalW = Math.min((int)(screenWidth * 0.75), 500);
        int modalH = Math.min((int)(screenHeight * 0.70), 400);
        
        dialog = new ModalDialog("⚡ Force Field Configuration", textRenderer, screenWidth, screenHeight)
            .size(modalW, modalH)
            .content(this::buildContent)
            .addAction("Save", this::save, true)
            .addAction("Cancel", () -> {});
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Load Config
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void loadFrom(ForceFieldConfig config) {
        mode = config.mode();
        maxVelocity = config.maxVelocity();
        verticalBoost = config.verticalBoost();
        damping = config.damping();
        
        // Load zones
        zones.clear();
        for (ForceZone zone : config.zones()) {
            zones.add(new ZoneState(zone.radius(), zone.strength(), zone.falloffName()));
        }
        if (zones.isEmpty()) {
            zones.add(new ZoneState(10f, 0.1f, "linear"));
        }
        
        // Load phases
        phases.clear();
        for (ForcePhase phase : config.phases()) {
            phases.add(new PhaseState(phase.startPercent(), phase.endPercent(),
                                      phase.polarity(), phase.strengthMultiplier()));
        }
        if (phases.isEmpty()) {
            phases.add(new PhaseState(0, 100, ForcePolarity.PULL, 1.0f));
        }
        
        // Load mode-specific configs
        if (config.vortex() != null) vortexConfig = config.vortex();
        if (config.orbit() != null) orbitConfig = config.orbit();
        if (config.tornado() != null) tornadoConfig = config.tornado();
        if (config.ring() != null) ringConfig = config.ring();
        if (config.implosion() != null) implosionConfig = config.implosion();
        if (config.explosion() != null) explosionConfig = config.explosion();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Build Content
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ClickableWidget> buildContent(Bounds bounds, TextRenderer tr) {
        List<ClickableWidget> widgets = new ArrayList<>();
        
        int padding = GuiConstants.PADDING;
        int rowH = GuiConstants.WIDGET_HEIGHT;
        int gap = 3;
        
        // Calculate layout
        int contentW = bounds.width() - padding * 2;
        int colW = (contentW - padding) / 2;
        int leftX = bounds.x() + padding;
        int rightX = leftX + colW + padding;
        int topY = bounds.y() + padding;
        
        // Reserve space for bottom controls (mode + constraints)
        int bottomSectionHeight = rowH * 2 + padding * 4;
        int bottomY = bounds.y() + bounds.height() - bottomSectionHeight;
        
        // Available height for zone/phase editors
        int editorMaxHeight = bottomY - topY - padding;
        
        // ─────────────────────────────────────────────────────────────────────
        // LEFT COLUMN: ZONES
        // ─────────────────────────────────────────────────────────────────────
        widgets.addAll(buildZoneColumn(leftX, topY, colW, editorMaxHeight, tr));
        
        // ─────────────────────────────────────────────────────────────────────
        // RIGHT COLUMN: PHASES
        // ─────────────────────────────────────────────────────────────────────
        widgets.addAll(buildPhaseColumn(rightX, topY, colW, editorMaxHeight, tr));
        
        // ─────────────────────────────────────────────────────────────────────
        // BOTTOM: Mode Selector + Global Constraints
        // ─────────────────────────────────────────────────────────────────────
        widgets.addAll(buildBottomControls(leftX, bottomY, contentW, tr));
        
        return widgets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Zone Column
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ClickableWidget> buildZoneColumn(int x, int y, int w, int maxH, TextRenderer tr) {
        List<ClickableWidget> widgets = new ArrayList<>();
        int rowH = GuiConstants.WIDGET_HEIGHT;
        int gap = 3;
        
        // Header with selector
        List<String> zoneNames = new ArrayList<>();
        for (int i = 0; i < zones.size(); i++) {
            zoneNames.add("Zone " + (i + 1));
        }
        if (zoneNames.isEmpty()) zoneNames.add("(none)");
        
        CompactSelector<String> zoneSelector = new CompactSelector<>("", tr, zoneNames, s -> s)
            .onSelect(name -> {
                int idx = zoneNames.indexOf(name);
                if (idx >= 0 && idx < zones.size()) {
                    selectedZoneIndex = idx;
                    dialog.rebuild();
                }
            })
            .onAdd(() -> {
                // Add new zone with reasonable defaults
                float maxRadius = zones.stream().map(z -> z.radius).max(Float::compare).orElse(10f);
                zones.add(new ZoneState(maxRadius + 5f, 0.1f, "linear"));
                selectedZoneIndex = zones.size() - 1;
                dialog.rebuild();
            })
            .selectIndex(selectedZoneIndex);
        zoneSelector.setBounds(new Bounds(x, y, w, 20));
        widgets.addAll(zoneSelector.getWidgets());
        y += 24;
        
        // Zone controls (if any zone selected)
        if (!zones.isEmpty() && selectedZoneIndex >= 0 && selectedZoneIndex < zones.size()) {
            ZoneState zone = zones.get(selectedZoneIndex);
            
            // Radius
            widgets.add(LabeledSlider.builder("Radius")
                .position(x, y).width(w)
                .range(1f, 50f).initial(zone.radius).format("%.1f blocks")
                .onChange(v -> zone.radius = v)
                .build());
            y += rowH + gap;
            
            // Strength
            widgets.add(LabeledSlider.builder("Strength")
                .position(x, y).width(w)
                .range(0.01f, 1.0f).initial(zone.strength).format("%.3f")
                .onChange(v -> zone.strength = v)
                .build());
            y += rowH + gap;
            
            // Falloff dropdown
            final ZoneState finalZone = zone;
            widgets.add(CyclingButtonWidget.<String>builder(s -> Text.literal(s))
                .values(FALLOFF_OPTIONS)
                .initially(capitalize(zone.falloff))
                .build(x, y, w, rowH, Text.literal("Falloff"),
                       (btn, val) -> finalZone.falloff = val.toLowerCase()));
            y += rowH + gap;
            
            // Delete button (only if multiple zones)
            if (zones.size() > 1) {
                widgets.add(GuiWidgets.button(x, y, w, "🗑 Delete Zone",
                    "Remove this zone", () -> {
                        zones.remove(selectedZoneIndex);
                        selectedZoneIndex = Math.max(0, Math.min(selectedZoneIndex, zones.size() - 1));
                        dialog.rebuild();
                    }));
            }
        }
        
        return widgets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Phase Column
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ClickableWidget> buildPhaseColumn(int x, int y, int w, int maxH, TextRenderer tr) {
        List<ClickableWidget> widgets = new ArrayList<>();
        int rowH = GuiConstants.WIDGET_HEIGHT;
        int gap = 3;
        
        // Header with selector
        List<String> phaseNames = new ArrayList<>();
        for (int i = 0; i < phases.size(); i++) {
            PhaseState p = phases.get(i);
            phaseNames.add("Phase " + (i + 1) + " (" + p.polarity.name().charAt(0) + ")");
        }
        if (phaseNames.isEmpty()) phaseNames.add("(none)");
        
        CompactSelector<String> phaseSelector = new CompactSelector<>("", tr, phaseNames, s -> s)
            .onSelect(name -> {
                int idx = phaseNames.indexOf(name);
                if (idx >= 0 && idx < phases.size()) {
                    selectedPhaseIndex = idx;
                    dialog.rebuild();
                }
            })
            .onAdd(() -> {
                // Add new phase starting where last one ends
                float lastEnd = phases.isEmpty() ? 0 : phases.get(phases.size() - 1).endPercent;
                if (lastEnd >= 100) lastEnd = 90; // Make room
                phases.add(new PhaseState(lastEnd, 100, ForcePolarity.PUSH, 1.0f));
                selectedPhaseIndex = phases.size() - 1;
                dialog.rebuild();
            })
            .selectIndex(selectedPhaseIndex);
        phaseSelector.setBounds(new Bounds(x, y, w, 20));
        widgets.addAll(phaseSelector.getWidgets());
        y += 24;
        
        // Phase controls
        if (!phases.isEmpty() && selectedPhaseIndex >= 0 && selectedPhaseIndex < phases.size()) {
            PhaseState phase = phases.get(selectedPhaseIndex);
            
            // Start %
            widgets.add(LabeledSlider.builder("Start %")
                .position(x, y).width(w)
                .range(0, 100).initial(phase.startPercent).format("%.0f%%").step(5)
                .onChange(v -> phase.startPercent = v)
                .build());
            y += rowH + gap;
            
            // End %
            widgets.add(LabeledSlider.builder("End %")
                .position(x, y).width(w)
                .range(0, 100).initial(phase.endPercent).format("%.0f%%").step(5)
                .onChange(v -> phase.endPercent = v)
                .build());
            y += rowH + gap;
            
            // Polarity
            final PhaseState finalPhase = phase;
            widgets.add(CyclingButtonWidget.<ForcePolarity>builder(p -> Text.literal("● " + p.name()))
                .values(ForcePolarity.values())
                .initially(phase.polarity)
                .build(x, y, w, rowH, Text.literal("Polarity"),
                       (btn, val) -> finalPhase.polarity = val));
            y += rowH + gap;
            
            // Multiplier
            widgets.add(LabeledSlider.builder("Multiplier")
                .position(x, y).width(w)
                .range(0.1f, 5.0f).initial(phase.multiplier).format("%.1fx")
                .onChange(v -> phase.multiplier = v)
                .build());
            y += rowH + gap;
            
            // Delete button (only if multiple phases)
            if (phases.size() > 1) {
                widgets.add(GuiWidgets.button(x, y, w, "🗑 Delete Phase",
                    "Remove this phase", () -> {
                        phases.remove(selectedPhaseIndex);
                        selectedPhaseIndex = Math.max(0, Math.min(selectedPhaseIndex, phases.size() - 1));
                        dialog.rebuild();
                    }));
            }
        }
        
        return widgets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Bottom Controls: Mode + Constraints
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<ClickableWidget> buildBottomControls(int x, int y, int w, TextRenderer tr) {
        List<ClickableWidget> widgets = new ArrayList<>();
        int rowH = GuiConstants.WIDGET_HEIGHT;
        int gap = GuiConstants.PADDING;
        
        // First row: Mode + Max Velocity + Damping
        int thirdW = (w - gap * 2) / 3;
        
        // Mode dropdown
        List<String> modeNames = Arrays.stream(ForceMode.values())
            .map(ForceMode::displayName).toList();
        
        widgets.add(CyclingButtonWidget.<String>builder(s -> Text.literal(s))
            .values(modeNames.toArray(new String[0]))
            .initially(mode.displayName())
            .build(x, y, thirdW, rowH, Text.literal("Mode"),
                   (btn, val) -> {
                       for (ForceMode m : ForceMode.values()) {
                           if (m.displayName().equals(val)) {
                               mode = m;
                               break;
                           }
                       }
                   }));
        
        // Max Velocity
        widgets.add(LabeledSlider.builder("Max Vel")
            .position(x + thirdW + gap, y).width(thirdW)
            .range(0.1f, 3.0f).initial(maxVelocity).format("%.2f")
            .onChange(v -> maxVelocity = v)
            .build());
        
        // Damping
        widgets.add(LabeledSlider.builder("Damping")
            .position(x + thirdW * 2 + gap * 2, y).width(thirdW)
            .range(0f, 0.5f).initial(damping).format("%.3f")
            .onChange(v -> damping = v)
            .build());
        
        y += rowH + gap;
        
        // Second row: Vertical Boost (half width)
        int halfW = (w - gap) / 2;
        widgets.add(LabeledSlider.builder("V-Boost")
            .position(x, y).width(halfW)
            .range(0f, 1.0f).initial(verticalBoost).format("%.2f")
            .onChange(v -> verticalBoost = v)
            .build());
        
        return widgets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "Linear";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void save() {
        ForceFieldConfig.Builder builder = ForceFieldConfig.builder()
            .mode(mode)
            .maxVelocity(maxVelocity)
            .verticalBoost(verticalBoost)
            .damping(damping);
        
        // Add all zones
        for (ZoneState z : zones) {
            builder.zone(z.radius, z.strength, z.falloff);
        }
        
        // Add all phases
        for (PhaseState p : phases) {
            builder.phase(ForcePhase.builder()
                .start(p.startPercent)
                .end(p.endPercent)
                .polarity(p.polarity)
                .strength(p.multiplier)
                .build());
        }
        
        // Add mode-specific config if applicable
        switch (mode) {
            case VORTEX -> builder.vortex(vortexConfig);
            case ORBIT -> builder.orbit(orbitConfig);
            case TORNADO -> builder.tornado(tornadoConfig);
            case RING -> builder.ring(ringConfig);
            case IMPLOSION -> builder.implosion(implosionConfig);
            case EXPLOSION -> builder.explosion(explosionConfig);
            default -> {} // RADIAL, PULL, PUSH don't need extra config
        }
        
        ForceFieldConfig config = builder.build();
        onSave.accept(config);
        dialog.hide();
        
        Logging.GUI.topic("force").info("Saved force config: mode={}, {} zones, {} phases",
            mode, zones.size(), phases.size());
    }
    
    public ModalDialog getDialog() {
        return dialog;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State Classes
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static class ZoneState {
        float radius;
        float strength;
        String falloff;
        
        ZoneState(float radius, float strength, String falloff) {
            this.radius = radius;
            this.strength = strength;
            this.falloff = falloff != null ? falloff : "linear";
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
            this.polarity = polarity != null ? polarity : ForcePolarity.PULL;
            this.multiplier = multiplier;
        }
    }
}
