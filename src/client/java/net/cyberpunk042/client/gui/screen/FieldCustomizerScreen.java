package net.cyberpunk042.client.gui.screen;

import net.cyberpunk042.client.gui.layout.*;
import net.cyberpunk042.client.gui.panel.*;
import net.cyberpunk042.client.gui.panel.sub.*;
import net.cyberpunk042.client.gui.preview.FieldPreviewRenderer;
import net.cyberpunk042.client.gui.render.SimplifiedFieldRenderer;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.client.gui.widget.*;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.network.GuiPacketSender;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * G01: Main Field Customizer GUI screen with 2×2 grid layout.
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ [⬜] Field Customizer                                            [×]    │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ [Quick] [Advanced] [Debug] [Profiles]                                   │
 * ├─────────────────────────────┬───────────────────────────────────────────┤
 * │                             │ LAYER: [+] ◀ base ▶ [+]                   │
 * │       3D PREVIEW            │ PRIM:  [◀] sphere_main [+]                │
 * │                             ├───────────────────────────────────────────┤
 * │                             │ [Fill] [Appear] [Visibility] ← Quick      │
 * │                             │ [Anim] [Trans] [Pred] [Orbit] ← Advanced  │
 * ├─────────────────────────────┼───────────────────────────────────────────┤
 * │       SHAPE                 │                                           │
 * │   Type: [Sphere ▼]          │   (sub-tab content continues)             │
 * │   Algo: [ICO ▼]             │                                           │
 * └─────────────────────────────┴───────────────────────────────────────────┘
 * </pre>
 */
public class FieldCustomizerScreen extends Screen {
    
    private static final int TITLE_HEIGHT = 20;
    private static final int TAB_BAR_HEIGHT = 22;
    private static final int STATUS_HEIGHT = 18;
    private static final int MARGIN = 8;
    private static final int SELECTOR_HEIGHT = 22;
    
    // Config persistence
    private static final Path GUI_CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("gui_preferences.properties");
    
    private final FieldEditState state;
    private GuiMode mode;  // Loaded from config or default to FULLSCREEN
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Config Persistence
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static GuiMode loadSavedMode() {
        try {
            if (Files.exists(GUI_CONFIG_PATH)) {
                Properties props = new Properties();
                try (var reader = Files.newBufferedReader(GUI_CONFIG_PATH)) {
                    props.load(reader);
                }
                String modeStr = props.getProperty("gui.mode", "FULLSCREEN");
                return GuiMode.valueOf(modeStr);
            }
        } catch (Exception e) {
            Logging.GUI.topic("config").warn("Failed to load GUI preferences: {}", e.getMessage());
        }
        return GuiMode.FULLSCREEN;
    }
    
    private static void saveMode(GuiMode mode) {
        try {
            Files.createDirectories(GUI_CONFIG_PATH.getParent());
            Properties props = new Properties();
            props.setProperty("gui.mode", mode.name());
            try (var writer = Files.newBufferedWriter(GUI_CONFIG_PATH)) {
                props.store(writer, "GUI Preferences");
            }
        } catch (IOException e) {
            Logging.GUI.topic("config").warn("Failed to save GUI preferences: {}", e.getMessage());
        }
    }
    
    // Layout
    private GridPane grid;
    private Bounds contentBounds;
    
    // Main tabs
    private TabType currentTab = TabType.QUICK;
    private ButtonWidget quickTabBtn, advancedTabBtn, debugTabBtn, profilesTabBtn;
    private ButtonWidget modeToggleBtn, closeBtn, fieldToggleBtn, resetBtn;
    private ButtonWidget traceButton; // Pipeline tracing toggle
    private DropdownWidget<String> presetDropdown;
    private net.minecraft.client.gui.widget.CyclingButtonWidget<Boolean> previewModeCheckbox;
    private net.minecraft.client.gui.widget.CyclingButtonWidget<Boolean> rendererModeToggle;
    private boolean useFullPreviewRenderer = false; // For 3D preview pane only (separate from world renderer)
    private PresetConfirmDialog presetConfirmDialog;
    
    // Config flags
    private static boolean backgroundBlurEnabled = true;  // Configurable
    
    // Compact selectors (using strings for simplicity)
    private CompactSelector<String> layerSelector;
    private CompactSelector<String> primitiveSelector;
    
    // Sub-tab panes (changes based on main tab)
    private SubTabPane quickSubTabs;    // Fill, Appear, Visibility
    private SubTabPane advancedSubTabs; // Anim, Transform, Pred, Orbit
    private SubTabPane debugSubTabs;    // Beam, Trigger, Lifecycle, Modifiers...
    
    // Shape panel (bottom-left quadrant)
    private ShapeSubPanel shapePanel;
    private Bounds shapePanelBounds;
    
    // Profiles panel (dual-panel layout for better UX)
    private ProfilesPanel profilesPanel;
    private Bounds profilesLeftBounds;   // For profiles list (uses left panel area)
    private Bounds profilesRightBounds;  // For profiles details (uses right panel area)
    
    // Status bar
    private StatusBar statusBar;
    
    // Modal dialog (if any)
    private ModalDialog activeModal;
    
    public FieldCustomizerScreen() {
        this(FieldEditStateHolder.getOrCreate());  // Preserve state on reopen
    }
    
    public FieldCustomizerScreen(FieldEditState state) {
        super(Text.literal("Field Customizer"));
        this.state = state;
        this.mode = loadSavedMode();  // Restore last mode
        Logging.GUI.topic("screen").info("FieldCustomizerScreen created, mode={}", mode);
    }
    
    // State change listener for preview refresh
    private Runnable stateChangeListener;
    
    @Override
    protected void init() {
        super.init();
        
        // Reload fragments/presets on open to pick up config files
        FragmentRegistry.reload();
        PresetRegistry.reset();
        // Ensure global holder points to this screen's state (for debug field spawn)
        net.cyberpunk042.client.gui.state.FieldEditStateHolder.set(state);
        
        // Register state change listener (only once)
        if (stateChangeListener == null) {
            stateChangeListener = this::onStateChanged;
            state.addChangeListener(stateChangeListener);
        }
        
        if (mode == GuiMode.FULLSCREEN) {
            initFullscreenMode();
        } else {
            initWindowedMode();
        }
        
        Logging.GUI.topic("screen").debug("FieldCustomizerScreen initialized in {} mode", mode);
    }
    
    /**
     * Called when FieldEditState changes. Refreshes preview and optionally syncs to server.
     */
    private void onStateChanged() {
        // Mark SimplifiedFieldRenderer as dirty for world preview
        SimplifiedFieldRenderer.markDirty();
        
        // If live preview is enabled, sync to server
        if (state.getBool("livePreviewEnabled")) {
            GuiPacketSender.updateDebugField(state.toStateJson());
        }
        
        Logging.GUI.topic("state").trace("State changed, preview refreshed");
    }
    
    private void initFullscreenMode() {
        // Full content area (below title, above status)
        int contentTop = MARGIN + TITLE_HEIGHT + TAB_BAR_HEIGHT;
        int contentBottom = height - MARGIN - STATUS_HEIGHT;
        contentBounds = new Bounds(MARGIN, contentTop, width - MARGIN * 2, contentBottom - contentTop);
        
        // 2×2 grid with 50/50 split (can adjust ratios)
        grid = GridPane.grid2x2(0.4f, 0.5f);  // 40% left for preview, 50% top for selectors+tabs
        grid.setBounds(contentBounds);
        
        initTitleBar();
        initMainTabs();
        initSelectors();
        initSubTabs();
        initShapePanel();
        initStatusBar();
        initPreviewModeCheckbox();
        
        registerWidgets();
    }
    
    private void initWindowedMode() {
        // Windowed mode: two side panels with game world visible in center
        // Panels need to be wide enough to fit controls (~220px min), but not take too much screen
        int panelWidth = Math.min(200, Math.max(180, (width - MARGIN * 4) / 5));  // Narrower for more center space
        
        // Clear fullscreen-only widgets
        previewModeCheckbox = null;
        grid = null;
        int panelHeight = height - MARGIN * 2 - STATUS_HEIGHT;
        
        // Left panel bounds
        Bounds leftPanelBounds = new Bounds(MARGIN, MARGIN, panelWidth, panelHeight);
        
        // Right panel bounds
        Bounds rightPanelBounds = new Bounds(width - MARGIN - panelWidth, MARGIN, panelWidth, panelHeight);
        
        // Store for Profiles dual-panel mode (left panel content area, below tabs)
        int tabAreaHeight = 16 + 18 + 8;  // title + tabs + gap
        profilesLeftBounds = new Bounds(
            leftPanelBounds.x() + 4,
            leftPanelBounds.y() + tabAreaHeight,
            leftPanelBounds.width() - 8,
            leftPanelBounds.height() - tabAreaHeight - 4
        );
        profilesRightBounds = new Bounds(
            rightPanelBounds.x() + 4,
            rightPanelBounds.y() + 4,
            rightPanelBounds.width() - 8,
            rightPanelBounds.height() - 8
        );
        
        // Status bar spans the middle
        Bounds statusBounds = new Bounds(
            MARGIN + panelWidth + MARGIN,
            height - MARGIN - STATUS_HEIGHT,
            width - panelWidth * 2 - MARGIN * 4,
            STATUS_HEIGHT
        );
        
        // Initialize left panel: Title + Tabs + Shape
        initWindowedLeftPanel(leftPanelBounds);
        
        // Initialize right panel: Selectors + Sub-tabs + Profiles
        initWindowedRightPanel(rightPanelBounds);
        
        // Status bar
        statusBar = new StatusBar(state, textRenderer);
        statusBar.setBounds(statusBounds);
        
        registerWidgets();
    }
    
    private void initWindowedLeftPanel(Bounds panelBounds) {
        int y = panelBounds.y();
        int x = panelBounds.x();
        int w = panelBounds.width();
        int titleH = 16;
        int tabH = 18;
        int padding = 4;
        
        // Title bar buttons (right to left): Close, Reset, Mode, Field visibility
        // Each button is 14px wide with 2px gaps
        closeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("×"), btn -> close())
            .dimensions(x + w - 16, y + 2, 14, 12)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Close")))
            .build());
        
        resetBtn = addDrawableChild(ButtonWidget.builder(Text.literal("R"), btn -> resetState())
            .dimensions(x + w - 32, y + 2, 14, 12)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Reset to defaults")))
            .build());
        
        modeToggleBtn = addDrawableChild(ButtonWidget.builder(Text.literal("⬜"), btn -> toggleMode())
            .dimensions(x + w - 48, y + 2, 14, 12)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Fullscreen")))
            .build());
        
        fieldToggleBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(FieldEditStateHolder.isTestFieldActive() ? "◉" : "○"),
                btn -> {
                    FieldEditStateHolder.toggleTestField();
                    btn.setMessage(Text.literal(FieldEditStateHolder.isTestFieldActive() ? "◉" : "○"));
                })
            .dimensions(x + w - 64, y + 2, 14, 12)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Field visibility")))
            .build());
        
        y += titleH;
        
        // Compact tabs [Q][A][D][P]
        int tabW = (w - padding * 5) / 4;
        int tx = x + padding;
        
        quickTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Q"), btn -> switchTab(TabType.QUICK))
            .dimensions(tx, y, tabW, tabH)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Quick")))
            .build());
        tx += tabW + padding;
        
        advancedTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("A"), btn -> switchTab(TabType.ADVANCED))
            .dimensions(tx, y, tabW, tabH)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Advanced")))
            .build());
        tx += tabW + padding;
        
        debugTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("D"), btn -> switchTab(TabType.DEBUG))
            .dimensions(tx, y, tabW, tabH)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Debug")))
            .build());
        tx += tabW + padding;
        
        profilesTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("P"), btn -> switchTab(TabType.PROFILES))
            .dimensions(tx, y, tabW, tabH)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Profiles")))
            .build());
        
        updateMainTabButtons();
        y += tabH + padding;
        
        // Preset row (compact)
        int presetRowH = 16;
        PresetRegistry.loadAll();
        List<String> presetNames = new ArrayList<>(PresetRegistry.listPresets());
        final String presetPlaceholder = "Select…";
        if (presetNames.isEmpty()) {
            presetNames.add("No presets");
        } else {
            presetNames.add(0, presetPlaceholder);
        }
        
        int presetW = w - padding * 2;
        presetDropdown = new DropdownWidget<>(
            textRenderer, x + padding, y, presetW, presetRowH,
            presetNames,
            name -> {
                if (name.equals(presetPlaceholder)) return Text.literal("Select Preset...");
                if (name.equals("No presets")) return Text.literal("No presets");
                return Text.literal(PresetRegistry.getDisplayLabel(name));
            },
            name -> {
                // Apply on selection
                if (!name.equals(presetPlaceholder) && !name.equals("No presets")) {
                    onPresetSelected(name);
                }
            }
        );
        addDrawableChild(presetDropdown);
        
        y += presetRowH + padding;
        
        // Renderer mode toggle (Fast/Accurate for debug field)
        rendererModeToggle = net.minecraft.client.gui.widget.CyclingButtonWidget.<Boolean>builder(
                advanced -> Text.literal(advanced ? "⚡ Accurate" : "⚡ Fast"))
            .values(List.of(false, true))
            .initially(SimplifiedFieldRenderer.isAdvancedModeEnabled())
            .tooltip(value -> net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.literal(value ? "Full FieldRenderer (accurate)" : "SimplifiedFieldRenderer (fast)")))
            .omitKeyText()
            .build(x + padding, y, w - padding * 2, presetRowH, Text.literal(""),
                (btn, advanced) -> {
                    SimplifiedFieldRenderer.setAdvancedModeEnabled(advanced);
                    refreshTabsForRendererMode();
                    ToastNotification.info("Renderer: " + (advanced ? "Accurate" : "Fast"));
                });
        
        y += presetRowH + padding;
        
        // Shape panel fills rest of left panel
        shapePanelBounds = new Bounds(x + padding, y, w - padding * 2, panelBounds.bottom() - y - padding);
        shapePanel = new ShapeSubPanel(this, state, 0);
        shapePanel.setWarningCallback((warning, color) -> {
            if (statusBar != null) {
                if (warning != null) statusBar.setWarning(warning, color);
                else statusBar.clearWarning();
            }
        });
        shapePanel.setShapeChangedCallback(this::registerWidgets);  // Re-register when shape changes
        shapePanel.init(shapePanelBounds.width(), shapePanelBounds.height());
        shapePanel.setBoundsQuiet(shapePanelBounds);  // Use quiet to avoid clearing widgets
        shapePanel.applyBoundsOffset();  // THEN move widgets
    }
    
    private void initWindowedRightPanel(Bounds panelBounds) {
        int y = panelBounds.y();
        int x = panelBounds.x();
        int w = panelBounds.width();
        int titleH = 16;
        int padding = 4;
        
        // Title bar with Reset button on the right (uses class field for persistence)
        resetBtn = addDrawableChild(ButtonWidget.builder(Text.literal("R"), btn -> resetState())
            .dimensions(x + w - 18, y + 2, 14, 12)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Reset to defaults")))
            .build());
        
        y += titleH; // Move past title area
        
        // Compact selectors
        Bounds layerBounds = new Bounds(x + padding, y, w - padding * 2, SELECTOR_HEIGHT);
        List<String> layerNames = new ArrayList<>(getLayerNames());
        layerSelector = new CompactSelector<>("L:", textRenderer, layerNames, s -> s)
            .onSelect(name -> {
                int idx = getLayerNames().indexOf(name);
                state.setSelectedLayerIndex(idx);
                refreshPrimitiveSelector();
                initSubTabs();
                registerWidgets();
            })
            .onAdd(() -> {
                int newIdx = state.addLayer();
                state.setSelectedLayerIndex(newIdx);
                refreshLayerSelector();
                layerSelector.selectIndex(newIdx);
                refreshPrimitiveSelector();
                initSubTabs();
                registerWidgets();
            })
            .onItemClick(name -> showLayerModal(name));
        layerSelector.setBounds(layerBounds);
        layerSelector.selectIndex(state.getSelectedLayerIndex());

        y += SELECTOR_HEIGHT + 2;

        Bounds primBounds = new Bounds(x + padding, y, w - padding * 2, SELECTOR_HEIGHT);
        List<String> primitiveNames = new ArrayList<>(getPrimitiveNames());
        primitiveSelector = new CompactSelector<>("P:", textRenderer, primitiveNames, s -> s)
            .onSelect(name -> {
                int idx = getPrimitiveNames().indexOf(name);
                state.setSelectedPrimitiveIndex(idx);
                initSubTabs();
                registerWidgets();
            })
            .onAdd(() -> {
                int newIdx = state.addPrimitive(state.getSelectedLayerIndex());
                state.setSelectedPrimitiveIndex(newIdx);
                refreshPrimitiveSelector();
                primitiveSelector.selectIndex(newIdx);
                initSubTabs();
                registerWidgets();
            })
            .onItemClick(name -> showPrimitiveModal(name));
        primitiveSelector.setBounds(primBounds);
        primitiveSelector.selectIndex(state.getSelectedPrimitiveIndex());

        y += SELECTOR_HEIGHT + padding;

        // Sub-tabs fill rest of right panel
        Bounds subTabBounds = new Bounds(x + padding, y, w - padding * 2, panelBounds.bottom() - y - padding);
        
        // Initialize sub-tab panes - re-register widgets when tab changes
        quickSubTabs = new SubTabPane(textRenderer)
            .addTab("Fill", createFillContent())
            .addTab("App", createAppearanceContent())
            .addTab("Vis", createVisibilityContent())
            .addTab("Xfm", createUnifiedTransformContent())  // Transform + Orbit unified
            .onTabChange(idx -> refreshSubTabWidgets());
        quickSubTabs.setBounds(subTabBounds);
        
        advancedSubTabs = new SubTabPane(textRenderer)
            .addTab("Anim", createAnimationContent())
            .addTab("Pred", createPredictionContent())
            .addTab("Link", createLinkingContent())
            .addTab("Mod", createModifiersContent())  // Moved from Debug tab
            .onTabChange(idx -> refreshSubTabWidgets());
        advancedSubTabs.setBounds(subTabBounds);
        
        debugSubTabs = new SubTabPane(textRenderer)
            .addTab("Beam", createBeamContent())
            .addTab("Trig", createTriggerContent())
            .addTab("Life", createLifecycleContent())
            .addTab("Bind", createBindingsContent())
            .addTab("Trace", createTraceContent())
            .onTabChange(idx -> refreshSubTabWidgets());
        debugSubTabs.setBounds(subTabBounds);
        
        // Profiles panel uses DUAL bounds (left + right panels) for better layout
        profilesPanel = new ProfilesPanel(this, state);
        profilesPanel.setDualBounds(profilesLeftBounds, profilesRightBounds);
        profilesPanel.init(profilesLeftBounds.width(), profilesLeftBounds.height());
        // No applyBoundsOffset needed - dual mode uses absolute positions
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initTitleBar() {
        int y = MARGIN;
        
        // Mode toggle
        modeToggleBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(mode == GuiMode.FULLSCREEN ? "⬛" : "⬜"), 
                btn -> toggleMode())
            .dimensions(MARGIN, y, 18, 18)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Toggle windowed/fullscreen")))
            .build());
        
        // Field visibility toggle
        fieldToggleBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(FieldEditStateHolder.isTestFieldActive() ? "◉" : "○"),
                btn -> {
                    FieldEditStateHolder.toggleTestField();
                    btn.setMessage(Text.literal(FieldEditStateHolder.isTestFieldActive() ? "◉" : "○"));
                })
            .dimensions(MARGIN + 22, y, 18, 18)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Toggle field visibility")))
            .build());
        
        // Reset button - use text label for visibility
        resetBtn = addDrawableChild(ButtonWidget.builder(Text.literal("RST"), btn -> resetState())
            .dimensions(width - MARGIN - 60, y, 40, 18)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Reset to defaults")))
            .build());
        
        // Close button
        closeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("×"), btn -> close())
            .dimensions(width - MARGIN - 18, y, 18, 18)
            .build());
    }
    
    private void initMainTabs() {
        int y = MARGIN + TITLE_HEIGHT;
        int tabW = 70;
        int x = MARGIN;
        
        quickTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Quick"), btn -> switchTab(TabType.QUICK))
            .dimensions(x, y, tabW, TAB_BAR_HEIGHT - 2)
            .build());
        x += tabW + 2;
        
        advancedTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Advanced"), btn -> switchTab(TabType.ADVANCED))
            .dimensions(x, y, tabW, TAB_BAR_HEIGHT - 2)
            .build());
        x += tabW + 2;
        
        debugTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Debug"), btn -> switchTab(TabType.DEBUG))
            .dimensions(x, y, tabW, TAB_BAR_HEIGHT - 2)
            .build());
        x += tabW + 2;
        
        profilesTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Profiles"), btn -> switchTab(TabType.PROFILES))
            .dimensions(x, y, tabW, TAB_BAR_HEIGHT - 2)
            .build());
        
        // Preset selector and apply button (floating right in tab bar)
        int applyW = 50;
        int presetW = 110;
        int presetX = width - MARGIN - presetW - applyW - 2;
        PresetRegistry.loadAll();  // Ensure presets are loaded
        List<String> presetNames = new ArrayList<>(PresetRegistry.listPresets());
        final String presetPlaceholder = "Select Preset…";
        if (presetNames.isEmpty()) {
            presetNames.add("No presets found");
        } else {
            presetNames.add(0, presetPlaceholder);
        }
        
        // Preset dropdown - proper dropdown that shows list on click
        presetDropdown = new DropdownWidget<>(
            textRenderer, presetX, y, presetW + applyW + 2, TAB_BAR_HEIGHT - 2,
            presetNames,
            name -> {
                if (name.equals(presetPlaceholder)) return Text.literal("Select Preset...");
                if (name.equals("No presets found")) return Text.literal("No presets");
                return Text.literal(PresetRegistry.getDisplayLabel(name));
            },
            name -> {
                // Apply on selection
                if (!name.equals(presetPlaceholder) && !name.equals("No presets found")) {
                    onPresetSelected(name);
                }
            }
        );
        addDrawableChild(presetDropdown);
        
        // Renderer mode toggle (between tabs and preset)
        int toggleW = 70;
        int toggleX = presetX - toggleW - 8;
        rendererModeToggle = addDrawableChild(
            net.minecraft.client.gui.widget.CyclingButtonWidget.<Boolean>builder(
                    advanced -> Text.literal(advanced ? "Accurate" : "Fast"))
                .values(List.of(false, true))
                .initially(SimplifiedFieldRenderer.isAdvancedModeEnabled())
                .tooltip(value -> net.minecraft.client.gui.tooltip.Tooltip.of(
                    Text.literal(value ? "Full renderer (all features)" : "Simplified (fast preview)")))
                .omitKeyText()
                .build(toggleX, y, toggleW, TAB_BAR_HEIGHT - 2, Text.literal(""),
                    (btn, advanced) -> {
                        SimplifiedFieldRenderer.setAdvancedModeEnabled(advanced);
                        refreshTabsForRendererMode();
                        ToastNotification.info("Renderer: " + (advanced ? "Accurate" : "Fast"));
                    })
        );
        
        updateMainTabButtons();
    }
    
    /**
     * Called when a preset is selected from the dropdown.
     * Shows confirmation dialog before applying.
     */
    private void onPresetSelected(String presetName) {
        if (presetName.equals("Select Preset…") || presetName.equals("No presets found")) {
            return;
        }
        
        // Show confirmation dialog
        presetConfirmDialog = new PresetConfirmDialog(presetName, confirmed -> {
            if (confirmed) {
                PresetRegistry.applyPreset(state, presetName);
                ToastNotification.success("Applied preset: " + presetName);
            }
            // Reset dropdown to placeholder after handling
            presetDropdown.setSelectedIndex(0); // Reset to first item (placeholder)
        });
        presetConfirmDialog.show(width, height);
    }
    
    private void initSelectors() {
        Bounds topRight = grid.topRight();
        
        // Layer selector at top of right column
        Bounds layerBounds = topRight.sliceTop(SELECTOR_HEIGHT).inset(4, 2);
        List<String> layerNames = new ArrayList<>(getLayerNames());
        layerSelector = new CompactSelector<>("LAYER:", textRenderer, layerNames, s -> s)
            .onSelect(name -> {
                int idx = getLayerNames().indexOf(name);
                state.setSelectedLayerIndex(idx);
                refreshPrimitiveSelector();
                initSubTabs();
                registerWidgets();
            })
            .onAdd(() -> {
                int newIdx = state.addLayer();
                state.setSelectedLayerIndex(newIdx);
                refreshLayerSelector();
                layerSelector.selectIndex(newIdx);
                refreshPrimitiveSelector();
                initSubTabs();
                registerWidgets();
            })
            .onItemClick(name -> showLayerModal(name));
        layerSelector.setBounds(layerBounds);
        layerSelector.selectIndex(state.getSelectedLayerIndex());

        // Primitive selector below layer
        Bounds primBounds = topRight.withoutTop(SELECTOR_HEIGHT).sliceTop(SELECTOR_HEIGHT).inset(4, 2);
        List<String> primitiveNames = new ArrayList<>(getPrimitiveNames());
        primitiveSelector = new CompactSelector<>("PRIM:", textRenderer, primitiveNames, s -> s)
            .onSelect(name -> {
                int idx = getPrimitiveNames().indexOf(name);
                state.setSelectedPrimitiveIndex(idx);
                initSubTabs();
                registerWidgets();
            })
            .onAdd(() -> {
                int newIdx = state.addPrimitive(state.getSelectedLayerIndex());
                state.setSelectedPrimitiveIndex(newIdx);
                refreshPrimitiveSelector();
                primitiveSelector.selectIndex(newIdx);
                initSubTabs();
                registerWidgets();
            })
            .onItemClick(name -> showPrimitiveModal(name));
        primitiveSelector.setBounds(primBounds);
        primitiveSelector.selectIndex(state.getSelectedPrimitiveIndex());
    }
    
    private void initSubTabs() {
        // In windowed mode, sub-tabs are already initialized in initWindowedRightPanel
        if (grid == null) {
            // Just refresh the tab content, don't reinitialize
            refreshSubTabWidgets();
            return;
        }
        
        // Sub-tab area: top-right, below selectors
        Bounds subTabArea = grid.topRight().withoutTop(SELECTOR_HEIGHT * 2 + 4);
        
        // Quick sub-tabs: Fill, Appearance, Visibility, Transform
        quickSubTabs = new SubTabPane(textRenderer)
            .addTab("Fill", createFillContent())
            .addTab("Appear", createAppearanceContent())
            .addTab("Visibility", createVisibilityContent())
            .addTab("Xfm", createUnifiedTransformContent())  // Transform + Orbit unified
            .onTabChange(idx -> refreshSubTabWidgets());
        quickSubTabs.setBounds(subTabArea);
        
        // Advanced sub-tabs: Animation, Prediction, Linking, Modifiers
        advancedSubTabs = new SubTabPane(textRenderer)
            .addTab("Anim", createAnimationContent())
            .addTab("Predict", createPredictionContent())
            .addTab("Linking", createLinkingContent())
            .addTab("Mods", createModifiersContent())  // Moved from Debug tab
            .onTabChange(idx -> refreshSubTabWidgets());
        advancedSubTabs.setBounds(subTabArea);
        
        // Debug sub-tabs: Beam, Trigger, Lifecycle, Arrange, Bindings, Trace
        debugSubTabs = new SubTabPane(textRenderer)
            .addTab("Beam", createBeamContent())
            .addTab("Trigger", createTriggerContent())
            .addTab("Life", createLifecycleContent())
            .addTab("Bindings", createBindingsContent())
            .addTab("Trace", createTraceContent())
            .onTabChange(idx -> refreshSubTabWidgets());
        debugSubTabs.setBounds(subTabArea);
        
        // Profiles panel uses DUAL bounds for better layout
        // In fullscreen: left = top-left preview area, right = full right column
        Bounds fullLeftColumn = grid.getSpan(0, 0, 1, 2);  // Left column, both rows
        Bounds fullRightColumn = grid.getSpan(1, 0, 1, 2);  // Right column, both rows
        profilesLeftBounds = fullLeftColumn;
        profilesRightBounds = fullRightColumn;
        profilesPanel = new ProfilesPanel(this, state);
        profilesPanel.setDualBounds(profilesLeftBounds, profilesRightBounds);
        profilesPanel.init(profilesLeftBounds.width(), profilesLeftBounds.height());
        profilesPanel.applyBoundsOffset();  // THEN move widgets
    }
    
    private void initShapePanel() {
        // Shape in bottom-left quadrant
        shapePanelBounds = grid.bottomLeft().inset(4);
        shapePanel = new ShapeSubPanel(this, state, 0);
        shapePanel.setWarningCallback((warning, color) -> {
            if (statusBar != null) {
                if (warning != null) statusBar.setWarning(warning, color);
                else statusBar.clearWarning();
            }
        });
        shapePanel.setShapeChangedCallback(this::registerWidgets);  // Re-register when shape changes
        shapePanel.init(shapePanelBounds.width(), shapePanelBounds.height());
        shapePanel.setBoundsQuiet(shapePanelBounds);  // Use quiet to avoid clearing widgets
        shapePanel.applyBoundsOffset();  // THEN move widgets to screen position
    }
    
    private void initStatusBar() {
        // Status bar at bottom of content area, inside the panel frame
        // (not at very bottom of screen which looks disconnected)
        int statusY = height - MARGIN - STATUS_HEIGHT;
        int statusX = MARGIN;
        int statusW = width - MARGIN * 2;
        
        // In fullscreen, the status bar is part of the main panel, so adjust position
        // to be inside the panel frame (same area as contentBounds)
        Bounds statusBounds = new Bounds(statusX, statusY, statusW, STATUS_HEIGHT);
        statusBar = new StatusBar(state, textRenderer);
        statusBar.setBounds(statusBounds);
    }
    
    private void initPreviewModeCheckbox() {
        // Only for fullscreen mode - checkbox in preview area (top-left quadrant)
        if (grid == null) return;
        
        Bounds previewBounds = grid.topLeft();
        int checkW = 60;
        int checkH = 12;
        int checkX = previewBounds.x() + 4;
        int checkY = previewBounds.y() + 4;
        
        previewModeCheckbox = net.minecraft.client.gui.widget.CyclingButtonWidget.<Boolean>builder(
                value -> Text.literal(value ? "☑ Full" : "☐ Fast"))
            .values(List.of(false, true))
            .initially(useFullPreviewRenderer)
            .tooltip(value -> net.minecraft.client.gui.tooltip.Tooltip.of(
                Text.literal(value ? "3D preview uses FieldRenderer (accurate)" : "3D preview uses SimplifiedFieldRenderer (fast)")))
            .omitKeyText()
            .build(checkX, checkY, checkW, checkH, Text.literal(""),
                (btn, value) -> {
                    // ONLY affects the 3D preview pane, NOT the menu tabs or world renderer
                    useFullPreviewRenderer = value;
                });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONTENT PROVIDERS (for sub-tabs)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private SubTabPane.ContentProvider createFillContent() {
        return new PanelWrapper(new FillSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createAppearanceContent() {
        return new PanelWrapper(new AppearanceSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createVisibilityContent() {
        return new PanelWrapper(new VisibilitySubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createAnimationContent() {
        return new PanelWrapper(new AnimationSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createUnifiedTransformContent() {
        return new PanelWrapper(new TransformQuickSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createTransformContent() {
        return new PanelWrapper(new TransformSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createPredictionContent() {
        return new PanelWrapper(new PredictionSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createOrbitContent() {
        return new PanelWrapper(new OrbitSubPanel(this, state));
    }
    
    private SubTabPane.ContentProvider createBeamContent() {
        return new PanelWrapper(new BeamSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createTriggerContent() {
        return new PanelWrapper(new TriggerSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createLifecycleContent() {
        return new PanelWrapper(new LifecycleSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createModifiersContent() {
        return new PanelWrapper(new ModifiersSubPanel(this, state));
    }
    
    private SubTabPane.ContentProvider createArrangeContent() {
        ArrangeSubPanel panel = new ArrangeSubPanel(this, state, textRenderer);
        panel.setWidgetChangedCallback(this::registerWidgets);
        return new PanelWrapper(panel);
    }
    
    private SubTabPane.ContentProvider createLinkingContent() {
        return new PanelWrapper(new LinkingSubPanel(this, state, 0));
    }
    
    private SubTabPane.ContentProvider createTraceContent() {
        return new PanelWrapper(new net.cyberpunk042.client.gui.panel.sub.TraceSubPanel(this, state));
    }
    
    private SubTabPane.ContentProvider createBindingsContent() {
        return new PanelWrapper(new BindingsSubPanel(this, state, 0));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MODE & TAB SWITCHING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void toggleMode() {
        mode = mode.toggle();
        saveMode(mode);  // Persist preference
        // Reinitialize the entire screen for the new mode
        clearChildren();
        init();
        Logging.GUI.topic("screen").info("Toggled to {} mode", mode);
    }
    
    private void resetState() {
        // Reset the edit state to defaults
        state.reset();
        // Rebuild the UI to reflect the reset
        clearChildren();
        init();
        Logging.GUI.topic("screen").info("State reset to defaults");
        ToastNotification.info("Reset to defaults");
    }
    
    private void switchTab(TabType tab) {
        if (tab == TabType.DEBUG && !state.getBool("debugUnlocked")) {
            ToastNotification.warning("Debug mode requires operator access");
            return;
        }
        
        currentTab = tab;
        updateMainTabButtons();
        registerWidgets();
        
        Logging.GUI.topic("screen").debug("Switched to tab: {}", tab);
    }
    
    private void updateMainTabButtons() {
        if (quickTabBtn != null) quickTabBtn.active = currentTab != TabType.QUICK;
        if (advancedTabBtn != null) advancedTabBtn.active = currentTab != TabType.ADVANCED;
        if (debugTabBtn != null) debugTabBtn.active = currentTab != TabType.DEBUG;
        if (profilesTabBtn != null) profilesTabBtn.active = currentTab != TabType.PROFILES;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void registerWidgets() {
        // Clear all except fixed buttons
        clearChildren();
        
        // Re-add fixed elements (buttons that are positioned absolutely)
        if (modeToggleBtn != null) addDrawableChild(modeToggleBtn);
        if (fieldToggleBtn != null) addDrawableChild(fieldToggleBtn);
        if (resetBtn != null) addDrawableChild(resetBtn);  // Reset button in title bar
        if (closeBtn != null) addDrawableChild(closeBtn);
        if (quickTabBtn != null) addDrawableChild(quickTabBtn);
        if (advancedTabBtn != null) addDrawableChild(advancedTabBtn);
        if (debugTabBtn != null) addDrawableChild(debugTabBtn);
        if (profilesTabBtn != null) addDrawableChild(profilesTabBtn);
        if (presetDropdown != null) addDrawableChild(presetDropdown);
        if (rendererModeToggle != null) addDrawableChild(rendererModeToggle);
        if (previewModeCheckbox != null) addDrawableChild(previewModeCheckbox);
        
        // For non-Profiles tabs: add selectors, sub-tabs, and shape panel
        if (currentTab != TabType.PROFILES) {
            // Add selector widgets
            if (layerSelector != null) {
                for (var w : layerSelector.getWidgets()) addDrawableChild(w);
            }
            if (primitiveSelector != null) {
                for (var w : primitiveSelector.getWidgets()) addDrawableChild(w);
            }
            
            // Add sub-tab widgets based on current tab
        switch (currentTab) {
            case QUICK -> {
                    if (quickSubTabs != null) {
                        for (var w : quickSubTabs.getWidgets()) addDrawableChild(w);
                    }
                }
                case ADVANCED -> {
                    if (advancedSubTabs != null) {
                        for (var w : advancedSubTabs.getWidgets()) addDrawableChild(w);
            }
            }
            case DEBUG -> {
                    if (debugSubTabs != null) {
                        for (var w : debugSubTabs.getWidgets()) addDrawableChild(w);
                    }
                }
            }
            
            // Add shape panel widgets
            if (shapePanel != null) {
                for (var w : shapePanel.getWidgets()) addDrawableChild(w);
            }
        } else {
            // Profiles tab: add ProfilesPanel widgets (dual layout)
            if (profilesPanel != null) {
                for (var w : profilesPanel.getWidgets()) addDrawableChild(w);
            }
        }
        
        // Modal widgets need absolute positioning (they're overlays)
        if (activeModal != null && activeModal.isVisible()) {
            for (var w : activeModal.getWidgets()) addDrawableChild(w);
        }
    }
    
    /**
     * Called when a sub-tab changes to re-register its widgets with the screen.
     */
    private void refreshSubTabWidgets() {
        // Just re-run full registration - it clears and re-adds all widgets
        registerWidgets();
    }
    
    /**
     * Refreshes all sub-tab panes when renderer mode changes.
     * This updates the enabled/disabled state of tabs based on feature support.
     */
    private void refreshTabsForRendererMode() {
        if (quickSubTabs != null) quickSubTabs.refreshTabs();
        if (advancedSubTabs != null) advancedSubTabs.refreshTabs();
        if (debugSubTabs != null) debugSubTabs.refreshTabs();
        registerWidgets(); // Re-register to pick up button changes
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODALS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void showLayerModal(String layerName) {
        int idx = state.getSelectedLayerIndex();
        int layerCount = state.getFieldLayers().size();
        final net.minecraft.client.gui.widget.TextFieldWidget[] fieldHolder = new net.minecraft.client.gui.widget.TextFieldWidget[1];
        
        activeModal = new ModalDialog("Rename Layer", textRenderer, width, height)
            .size(300, layerCount > 1 ? 150 : 130)
            .content((bounds, tr) -> {
                var widgets = new java.util.ArrayList<net.minecraft.client.gui.widget.ClickableWidget>();
                var field = new net.minecraft.client.gui.widget.TextFieldWidget(tr, 
                    bounds.x(), bounds.y() + 10, bounds.width(), 20, Text.literal(""));
                field.setText(layerName);
                field.setMaxLength(64);
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                fieldHolder[0] = field;
                widgets.add(field);
                return widgets;
            })
            .addAction("Cancel", () -> {});
        
        // Add Delete button only if more than one layer
        if (layerCount > 1) {
            activeModal.addAction("§cDelete", () -> {
                state.removeLayer(idx);
                refreshLayerSelector();
                layerSelector.selectIndex(Math.max(0, idx - 1));
                ToastNotification.info("Layer deleted");
            });
        }
        
        activeModal.addAction("OK", () -> {
            if (fieldHolder[0] != null) {
                state.renameLayer(idx, fieldHolder[0].getText());
                refreshLayerSelector();
            }
        }, true);
        
        activeModal.onClose(() -> {
            activeModal = null;
            registerWidgets();
        });
        
        activeModal.show();
        registerWidgets();
        focusModalTextField();
    }

    private void showPrimitiveModal(String primName) {
        int layerIdx = state.getSelectedLayerIndex();
        int primIdx = state.getSelectedPrimitiveIndex();
        int primCount = getPrimitiveNames().size();
        final net.minecraft.client.gui.widget.TextFieldWidget[] fieldHolder = new net.minecraft.client.gui.widget.TextFieldWidget[1];
        
        activeModal = new ModalDialog("Rename Primitive", textRenderer, width, height)
            .size(300, primCount > 1 ? 150 : 130)
            .content((bounds, tr) -> {
                var widgets = new java.util.ArrayList<net.minecraft.client.gui.widget.ClickableWidget>();
                var field = new net.minecraft.client.gui.widget.TextFieldWidget(tr, 
                    bounds.x(), bounds.y() + 10, bounds.width(), 20, Text.literal(""));
                field.setText(primName);
                field.setMaxLength(64);
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                fieldHolder[0] = field;
                widgets.add(field);
                return widgets;
            })
            .addAction("Cancel", () -> {});
        
        // Add Delete button only if more than one primitive
        if (primCount > 1) {
            activeModal.addAction("§cDelete", () -> {
                state.removePrimitive(layerIdx, primIdx);
                refreshPrimitiveSelector();
                primitiveSelector.selectIndex(Math.max(0, primIdx - 1));
                ToastNotification.info("Primitive deleted");
            });
        }
        
        activeModal.addAction("OK", () -> {
            if (fieldHolder[0] != null) {
                state.renamePrimitive(layerIdx, primIdx, fieldHolder[0].getText());
                refreshPrimitiveSelector();
            }
        }, true);
        
        activeModal.onClose(() -> {
            activeModal = null;
            registerWidgets();
        });
        
        activeModal.show();
        registerWidgets();
        focusModalTextField();
    }
    
    /**
     * Sets the Screen's focused element to the modal's text field.
     * This is needed because registerWidgets() calls clearChildren() which clears focus.
     */
    private void focusModalTextField() {
        if (activeModal == null) return;
        for (var w : activeModal.getWidgets()) {
            if (w instanceof net.minecraft.client.gui.widget.TextFieldWidget tf) {
                // Ensure widget is fully interactive
                tf.visible = true;
                tf.active = true;
                tf.setEditable(true);
                
                // Set focus on both widget and screen
                tf.setFocused(true);
                this.setFocused(tf);
                
                // Select all text for easy replacement
                tf.setCursorToStart(false);
                tf.setCursorToEnd(true);
                break;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<String> getLayerNames() {
        List<String> names = new ArrayList<>();
        for (FieldLayer layer : state.getFieldLayers()) {
            names.add(layer.id());
        }
        return names;
    }
    
    private List<String> getPrimitiveNames() {
        int layerIdx = state.getSelectedLayerIndex();
        return new ArrayList<>(state.getPrimitivesForLayer(layerIdx));
    }

    private void refreshLayerSelector() {
        if (layerSelector != null) {
            layerSelector.setItems(new ArrayList<>(getLayerNames()));
            layerSelector.selectIndex(state.getSelectedLayerIndex());
            layerSelector.refresh();
        }
    }

    private void refreshPrimitiveSelector() {
        if (primitiveSelector != null) {
            primitiveSelector.setItems(new ArrayList<>(getPrimitiveNames()));
            primitiveSelector.selectIndex(state.getSelectedPrimitiveIndex());
            primitiveSelector.refresh();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK & RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {
        super.tick();
        
        switch (currentTab) {
            case QUICK -> quickSubTabs.tick();
            case ADVANCED -> advancedSubTabs.tick();
            case DEBUG -> debugSubTabs.tick();
            case PROFILES -> profilesPanel.tick();
        }
        
        shapePanel.tick();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mode == GuiMode.FULLSCREEN) {
            renderFullscreenMode(context, mouseX, mouseY, delta);
        } else {
            renderWindowedMode(context, mouseX, mouseY, delta);
        }
        
        // Widgets (buttons, etc.)
        super.render(context, mouseX, mouseY, delta);
        
        // Dropdown overlay (must be rendered after all other widgets for z-order)
        if (presetDropdown != null && presetDropdown.isExpanded()) {
            presetDropdown.renderOverlay(context, mouseX, mouseY);
        }
        
        // Modal overlay + re-render modal widgets on top of overlay
        if (activeModal != null && activeModal.isVisible()) {
            activeModal.render(context, mouseX, mouseY, delta);
            for (var w : activeModal.getWidgets()) {
                w.render(context, mouseX, mouseY, delta);
            }
        }
        
        // Preset confirmation dialog (on top of modals)
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            presetConfirmDialog.render(context, mouseX, mouseY, delta);
        }
        
        // Toasts
        ToastNotification.renderAll(context, textRenderer, width, height);
    }
    
    private void renderFullscreenMode(DrawContext context, int mouseX, int mouseY, float delta) {
        // REAL fullscreen: solid background, NO world rendering behind
        // Fill entire screen with dark background (not just the panel area)
        context.fill(0, 0, width, height, 0xFF0a0a0a);
        
        // Main panel frame (slightly lighter border)
        context.fill(MARGIN - 1, MARGIN - 1, width - MARGIN + 1, height - MARGIN + 1, 0xFF333333);
        context.fill(MARGIN, MARGIN, width - MARGIN, height - MARGIN, 0xDD1a1a1a);
        
        // Title bar
        context.fill(MARGIN, MARGIN, width - MARGIN, MARGIN + TITLE_HEIGHT, 0xFF2a2a2a);
        context.drawTextWithShadow(textRenderer, "⬡ Field Customizer", MARGIN + 24, MARGIN + 6, 0xFFAAFFAA);
        if (state.isDirty()) {
            context.drawText(textRenderer, "●", width - MARGIN - 40, MARGIN + 6, 0xFFFFAA00, false);
        }
        
        // Tab bar background
        context.fill(MARGIN, MARGIN + TITLE_HEIGHT, width - MARGIN, MARGIN + TITLE_HEIGHT + TAB_BAR_HEIGHT, 0xFF222222);
        
        // Grid quadrants backgrounds
        renderQuadrantBackgrounds(context);
        
        // 3D Preview (top-left)
        render3DPreview(context, grid.topLeft(), delta);
        
        // Selectors (render labels)
        layerSelector.render(context, mouseX, mouseY, delta);
        primitiveSelector.render(context, mouseX, mouseY, delta);
        
        // Sub-tabs based on current main tab
        if (currentTab != TabType.PROFILES) {
        switch (currentTab) {
                case QUICK -> quickSubTabs.render(context, mouseX, mouseY, delta);
                case ADVANCED -> advancedSubTabs.render(context, mouseX, mouseY, delta);
                case DEBUG -> debugSubTabs.render(context, mouseX, mouseY, delta);
            }
            // Shape panel (bottom-left) - only when NOT on Profiles tab
            renderPanelInBounds(context, shapePanel, shapePanelBounds, mouseX, mouseY, delta);
        } else {
            // Profiles uses dual-panel layout - renders across BOTH panels
            profilesPanel.render(context, mouseX, mouseY, delta);
        }
        
        // Status bar
        statusBar.render(context, mouseX, mouseY, delta);
    }
    
    private void renderWindowedMode(DrawContext context, int mouseX, int mouseY, float delta) {
        // In windowed mode, don't dim the background - game world is visible
        
        // Use same calculation as initWindowedMode for consistency
        int panelWidth = Math.min(200, Math.max(180, (width - MARGIN * 4) / 5));  // Narrower for more center space
        int panelHeight = height - MARGIN * 2 - STATUS_HEIGHT;
        
        // Left panel
        int lx = MARGIN;
        int ly = MARGIN;
        
        // Panel shadow
        context.fill(lx + 2, ly + 2, lx + panelWidth + 2, ly + panelHeight + 2, 0x44000000);
        // Panel border
        context.fill(lx - 1, ly - 1, lx + panelWidth + 1, ly + panelHeight + 1, 0xFF444444);
        // Panel background
        context.fill(lx, ly, lx + panelWidth, ly + panelHeight, 0xDD1a1a1a);
        // Title bar
        context.fill(lx, ly, lx + panelWidth, ly + 16, 0xFF2a2a2a);
        context.drawTextWithShadow(textRenderer, "⬡ Field", lx + 4, ly + 4, 0xFFAAFFAA);
        if (state.isDirty()) {
            context.drawText(textRenderer, "●", lx + panelWidth - 36, ly + 4, 0xFFFFAA00, false);
        }
        // Tab bar
        context.fill(lx, ly + 16, lx + panelWidth, ly + 16 + 18, 0xFF222222);
        
        // Right panel
        int rx = width - MARGIN - panelWidth;
        int ry = MARGIN;
        
        // Panel shadow
        context.fill(rx + 2, ry + 2, rx + panelWidth + 2, ry + panelHeight + 2, 0x44000000);
        // Panel border
        context.fill(rx - 1, ry - 1, rx + panelWidth + 1, ry + panelHeight + 1, 0xFF444444);
        // Panel background
        context.fill(rx, ry, rx + panelWidth, ry + panelHeight, 0xDD1a1a1a);
        
        // Content depends on current tab
        if (currentTab != TabType.PROFILES) {
            // Normal tabs: Shape on left, Context on right
            renderPanelInBounds(context, shapePanel, shapePanelBounds, mouseX, mouseY, delta);
            
            // Right panel title
            context.fill(rx, ry, rx + panelWidth, ry + 16, 0xFF2a2a2a);
            context.drawTextWithShadow(textRenderer, "Context", rx + 4, ry + 4, 0xFFAAFFAA);
            
            // Selectors
            layerSelector.render(context, mouseX, mouseY, delta);
            primitiveSelector.render(context, mouseX, mouseY, delta);
            
            // Sub-tabs
            switch (currentTab) {
                case QUICK -> quickSubTabs.render(context, mouseX, mouseY, delta);
                case ADVANCED -> advancedSubTabs.render(context, mouseX, mouseY, delta);
                case DEBUG -> debugSubTabs.render(context, mouseX, mouseY, delta);
            }
        } else {
            // Profiles tab: uses BOTH panels with dual-panel layout
            // ProfilesPanel renders its own backgrounds and content for both sides
            profilesPanel.render(context, mouseX, mouseY, delta);
        }

        // Status bar (in center bottom)
        statusBar.render(context, mouseX, mouseY, delta);
    }
    
    private void renderQuadrantBackgrounds(DrawContext context) {
        // Top-left: Preview area
        Bounds tl = grid.topLeft();
        context.fill(tl.x(), tl.y(), tl.right(), tl.bottom(), 0xFF0a0a0a);
        context.drawBorder(tl.x(), tl.y(), tl.width(), tl.height(), 0xFF333333);
        
        // Bottom-left: Shape area
        Bounds bl = grid.bottomLeft();
        context.fill(bl.x(), bl.y(), bl.right(), bl.bottom(), 0xFF151515);
        context.drawBorder(bl.x(), bl.y(), bl.width(), bl.height(), 0xFF333333);
        
        // Right column background (both cells)
        Bounds rightCol = grid.rightColumn();
        context.fill(rightCol.x(), rightCol.y(), rightCol.right(), rightCol.bottom(), 0xFF121212);
    }
    
    /**
     * Renders a panel within its bounds with scissoring.
     * Widgets should already be positioned at their screen coordinates
     * (call panel.applyBoundsOffset() after init if needed).
     */
    private void renderPanelInBounds(DrawContext context, AbstractPanel panel, Bounds bounds, int mouseX, int mouseY, float delta) {
        if (panel == null || bounds == null) return;
        
        context.enableScissor(bounds.x(), bounds.y(), bounds.right(), bounds.bottom());
        panel.render(context, mouseX, mouseY, delta);
        context.disableScissor();
    }
    
    /**
     * Renders a REAL 3D preview of the field shape in the GUI.
     * Uses the centralized FieldPreviewRenderer for consistent 3D rendering.
     */
    private void render3DPreview(DrawContext context, Bounds bounds, float delta) {
        // Background for preview area
        context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF0a0a0a);
        
        // Calculate rotation - use spin if active, otherwise gentle animation
        float time = (System.currentTimeMillis() % 10000) / 10000f * 360f;
        float rotationY = time;
        
        if (state.spin() != null && state.spin().isActive()) {
            float spinSpeed = state.getFloat("spin.speed");
            rotationY = time * spinSpeed / 10f;
        }
        
        // Use selected renderer for 3D preview
        if (useFullPreviewRenderer) {
            // Full/Accurate: Use FieldRenderer (accurate, all features)
            // TODO: Implement proper 3D rendering with FieldRenderer in 2D context
            // For now, fall back to FieldPreviewRenderer
            FieldPreviewRenderer.drawField(context, state, 
                bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                1.0f, 25f, rotationY);
        } else {
            // Fast: Use FieldPreviewRenderer (simplified 2D wireframe)
            FieldPreviewRenderer.drawField(context, state, 
                bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                1.0f, 25f, rotationY);
        }
        
        // Shape label at bottom
        String shapeType = state.getString("shapeType").toLowerCase();
        String modeLabel = useFullPreviewRenderer ? " (Full)" : "";
        context.drawCenteredTextWithShadow(textRenderer, shapeType + modeLabel, bounds.centerX(), bounds.bottom() - 14, 0xFF666688);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeModal != null && activeModal.isVisible()) return true;
        
        // Handle dropdown scroll
        if (presetDropdown != null && presetDropdown.isExpanded()) {
            if (presetDropdown.handleScroll(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        
        switch (currentTab) {
            case QUICK -> { if (quickSubTabs.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true; }
            case ADVANCED -> { if (advancedSubTabs.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true; }
            case DEBUG -> { if (debugSubTabs.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true; }
            case PROFILES -> { if (profilesPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true; }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Preset confirmation dialog intercepts clicks first
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            return presetConfirmDialog.mouseClicked(mouseX, mouseY, button);
        }
        
        // Modal intercepts all clicks; pass through to its widgets
        if (activeModal != null && activeModal.isVisible()) {
            for (var w : activeModal.getWidgets()) {
                if (w.mouseClicked(mouseX, mouseY, button)) {
                    // CRITICAL: Set screen focus when widget is clicked (for TextFieldWidget)
                    this.setFocused(w);
                    return true;
                }
            }
            // Click outside modal - close it (onClose callback handles cleanup)
            if (!activeModal.containsClick(mouseX, mouseY)) {
                activeModal.hide();
            }
            return true;
        }
        
        // Handle dropdown clicks (expanded dropdown intercepts clicks)
        if (presetDropdown != null && presetDropdown.isExpanded()) {
            if (presetDropdown.handleClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Preset confirmation dialog intercepts keys first
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            return presetConfirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Modal intercepts escape and other keys
        if (activeModal != null && activeModal.isVisible()) {
            // Let focused widget handle key first (critical for TextFieldWidget)
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // Escape key closes modal (onClose callback handles cleanup)
            if (activeModal.keyPressed(keyCode)) {
                return true;
            }
            return true; // Consume other keys when modal is open
        }
        
        // Escape to close
        if (keyCode == 256) {
            close();
            return true;
        }
        
        // Tab to toggle mode
        if (keyCode == 258) {
            toggleMode();
            return true;
        }
        
        // Ctrl+Z undo
        if (keyCode == 90 && (modifiers & 2) != 0) {
            if (state.canUndo()) {
                state.undo();
                ToastNotification.info("Undo");
                return true;
            }
        }
        
        // Ctrl+Y redo
        if (keyCode == 89 && (modifiers & 2) != 0) {
            if (state.canRedo()) {
                state.redo();
                ToastNotification.info("Redo");
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            return false; // dialog has no text input
        }
        if (activeModal != null && activeModal.isVisible()) {
            // Let the parent handle charTyped which routes to focused widget
            return super.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return mode == GuiMode.FULLSCREEN;
    }
    
    @Override
    public void close() {
        // Remove state change listener
        if (stateChangeListener != null) {
            state.removeChangeListener(stateChangeListener);
            stateChangeListener = null;
        }
        
        if (state.isDirty()) {
            Logging.GUI.topic("screen").debug("Closing with unsaved changes");
        }
        Logging.GUI.topic("screen").info("FieldCustomizerScreen closed");
        super.close();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PANEL WRAPPER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Adapts AbstractPanel to SubTabPane.ContentProvider
     */
    private static class PanelWrapper implements SubTabPane.ContentProvider {
        private final AbstractPanel panel;
        private boolean initialized = false;
        private Bounds currentBounds;
        
        PanelWrapper(AbstractPanel panel) {
            this.panel = panel;
        }
        
        @Override public void setBounds(Bounds bounds) { 
            // Initialize the panel with dimensions first (creates widgets at 0,0)
            if (!initialized) {
                panel.init(bounds.width(), bounds.height());
                initialized = true;
            }
            
            // If bounds changed, we need to reposition widgets
            if (currentBounds == null || !currentBounds.equals(bounds)) {
                // Reset widgets to origin then apply new offset
                if (currentBounds != null) {
                    panel.offsetWidgets(-currentBounds.x(), -currentBounds.y());
                }
                panel.offsetWidgets(bounds.x(), bounds.y());
                currentBounds = bounds;
            }
            
            // Use setBoundsQuiet to avoid triggering reflow() which would clearWidgets()
            panel.setBoundsQuiet(bounds);
        }
        
        @Override public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() { 
            // Widgets are now at correct screen positions
            return initialized ? panel.getWidgets() : List.of();
        }
        
        @Override public void tick() { panel.tick(); }
        
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!initialized || currentBounds == null) return;
            
            // Scissor to bounds to prevent overflow
            context.enableScissor(currentBounds.x(), currentBounds.y(), currentBounds.right(), currentBounds.bottom());
            
            // Render directly - widgets are at correct screen positions now
            panel.render(context, mouseX, mouseY, delta);
            
            context.disableScissor();
        }
        
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
            if (currentBounds != null && currentBounds.contains(mouseX, mouseY)) {
                return panel.mouseScrolled(mouseX, mouseY, h, v);
            }
            return false;
        }
        
        @Override public boolean isFeatureSupported() { 
            return panel.isFeatureSupported(); 
        }
        
        @Override public String getDisabledTooltip() { 
            return panel.getDisabledTooltip(); 
        }
    }
    
    /**
     * Placeholder content for panels that don't extend AbstractPanel yet.
     */
    private class PlaceholderContent implements SubTabPane.ContentProvider {
        private final String title;
        private final String description;
        private Bounds bounds;
        
        PlaceholderContent(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        @Override public void setBounds(Bounds bounds) { this.bounds = bounds; }
        @Override public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() { return List.of(); }
        @Override public void tick() {}
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            // Draw placeholder info
            context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0x44333333);
            context.drawCenteredTextWithShadow(textRenderer, title, 
                bounds.x() + bounds.width() / 2, bounds.y() + 20, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(textRenderer, description,
                bounds.x() + bounds.width() / 2, bounds.y() + 40, 0xFF888888);
            context.drawCenteredTextWithShadow(textRenderer, "(Not yet migrated)",
                bounds.x() + bounds.width() / 2, bounds.y() + 60, 0xFF666666);
        }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) { return false; }
    }
}
