# GUI Refactoring Proof: FieldCustomizerScreen → ~400 Lines

## Current State: 1627 lines

### Line-by-Line Breakdown

| Section | Lines | Range | Extractable? | Target Component |
|---------|-------|-------|--------------|------------------|
| Imports + javadoc | 50 | 1-50 | Keep | Screen |
| Constants | 8 | 53-58 | Move to shared | `GuiConstants` |
| **Config persistence** | 28 | 59-99 | Extract | `GuiConfigPersistence` |
| Fields (widgets) | 42 | 100-142 | Reduce with components | Distributed |
| Constructors | 14 | 143-156 | Keep | Screen |
| `init()` + `onStateChanged()` | 38 | 157-195 | Keep but simplify | Screen |
| **`initFullscreenMode()`** | 20 | 197-216 | Replace with layout | `LayoutManager` |
| **`initWindowedMode()`** | 50 | 218-268 | Replace with layout | `LayoutManager` |
| **`initWindowedLeftPanel()`** | 128 | 270-398 | **EXTRACT** | `HeaderBar + TabBar` |
| **`initWindowedRightPanel()`** | 98 | 400-498 | **EXTRACT** | `SelectorBar` |
| **`initTitleBar()`** | 33 | 504-536 | **EXTRACT** | `HeaderBar` |
| **`initMainTabs()`** | 75 | 538-613 | **EXTRACT** | `TabBar` |
| `onPresetSelected()` | 20 | 615-634 | Keep | Screen or TabBar |
| **`initSelectors()`** | 48 | 636-684 | **EXTRACT** | `SelectorBar` |
| **`initSubTabs()`** | 50 | 686-735 | **EXTRACT** | `ContentArea` |
| **`initShapePanel()`** | 15 | 737-751 | Simplify | Screen |
| `initStatusBar()` | 13 | 753-765 | Simplify | Screen |
| `initPreviewModeCheckbox()` | 23 | 767-789 | Keep | Screen |
| **Content Providers** | 68 | 791-859 | **EXTRACT** | `ContentProviderFactory` |
| Mode/Tab switching | 38 | 861-902 | Keep | Screen |
| **`registerWidgets()`** | 61 | 904-969 | **AUTO-GEN** | `WidgetCollector` |
| refresh methods | 18 | 971-988 | Simplify | Screen |
| **Modals (`showLayerModal`, `showPrimitiveModal`)** | 125 | 990-1116 | **EXTRACT** | `ModalFactory` |
| Helper methods | 27 | 1118-1149 | Keep | Screen |
| `tick()` | 13 | 1151-1167 | Simplify | Screen |
| `render()` | 32 | 1169-1200 | Simplify | Screen |
| **`renderFullscreenMode()`** | 46 | 1202-1247 | **DELEGATE** | Layout.render() |
| **`renderWindowedMode()`** | 65 | 1249-1313 | **DELEGATE** | Layout.render() |
| `renderQuadrantBackgrounds()` | 15 | 1315-1329 | Move to layout | Layout |
| `renderPanelInBounds()` | 12 | 1331-1342 | Keep | Screen |
| `render3DPreview()` | 37 | 1344-1380 | Keep | Screen |
| Input handlers | 118 | 1382-1503 | Keep (essential) | Screen |
| `shouldPause()`, `close()` | 19 | 1505-1523 | Keep | Screen |
| **`PanelWrapper` inner class** | 64 | 1525-1595 | **EXTRACT** | Own file |
| **`PlaceholderContent` inner class** | 32 | 1597-1627 | **DELETE** (not needed) | N/A |

---

## Extraction Summary

| What to Extract | Lines Removed | New File |
|-----------------|---------------|----------|
| `GuiConfigPersistence` (save/load mode) | -28 | `util/GuiConfigPersistence.java` |
| `HeaderBar` (title bar buttons) | -80 | `component/HeaderBar.java` |
| `TabBar` (main tabs + preset + renderer toggle) | -130 | `component/TabBar.java` |
| `SelectorBar` (layer + primitive selectors) | -60 | `component/SelectorBar.java` |
| `ContentArea` (sub-tabs setup) | -50 | `component/ContentArea.java` |
| `ContentProviderFactory` (content provider methods) | -68 | `component/ContentProviderFactory.java` |
| `registerWidgets()` → `WidgetCollector` pattern | -50 | One-liner call |
| `ModalFactory` (layer/primitive modals) | -125 | `widget/ModalFactory.java` |
| `PanelWrapper` to own file | -64 | `widget/PanelWrapper.java` |
| Delete `PlaceholderContent` | -32 | N/A |
| Render delegation to LayoutManager | -110 | Uses existing layout classes |

**Total Lines Removed: ~797**

---

## New FieldCustomizerScreen Structure (~400 lines)

```java
public class FieldCustomizerScreen extends Screen {
    
    // ─────────────────────────────────────────────────────────────────
    // FIELDS (~20 lines)
    // ─────────────────────────────────────────────────────────────────
    private final FieldEditState state;
    private GuiMode mode;
    private LayoutManager layout;  // Replaces grid, contentBounds, etc.
    
    // Components (replace 40+ individual widget fields)
    private HeaderBar headerBar;
    private TabBar tabBar;
    private SelectorBar selectorBar;
    private ContentArea contentArea;
    private StatusBar statusBar;
    private ShapeSubPanel shapePanel;
    private ProfilesPanel profilesPanel;
    
    // Modal (single reference)
    private ModalDialog activeModal;
    
    // Tab state
    private TabType currentTab = TabType.QUICK;
    
    // ─────────────────────────────────────────────────────────────────
    // CONSTRUCTORS (~15 lines)
    // ─────────────────────────────────────────────────────────────────
    public FieldCustomizerScreen() { ... }
    public FieldCustomizerScreen(FieldEditState state) { ... }
    
    // ─────────────────────────────────────────────────────────────────
    // INIT (~40 lines)
    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        
        // 1. Create layout strategy
        layout = mode == GuiMode.FULLSCREEN 
            ? new FullscreenLayout() 
            : new WindowedLayout();
        layout.calculate(width, height);
        
        // 2. Create components (they auto-create their widgets)
        headerBar = new HeaderBar(layout.getTitleBarBounds(), textRenderer, 
            this::toggleMode, this::resetState, this::close, 
            () -> FieldEditStateHolder.toggleTestField());
        
        tabBar = new TabBar(layout.getTabBarBounds(), textRenderer,
            this::switchTab, this::onPresetSelected, this::refreshTabsForRendererMode);
        
        selectorBar = new SelectorBar(layout.getSelectorBounds(), textRenderer, state,
            this::onLayerSelected, this::onPrimitiveSelected,
            this::showLayerModal, this::showPrimitiveModal);
        
        contentArea = new ContentArea(layout.getContentBounds(), textRenderer, state,
            this, currentTab);
        
        statusBar = new StatusBar(state, textRenderer);
        statusBar.setBounds(layout.getStatusBarBounds());
        
        initShapePanel();
        initProfilesPanel();
        
        // 3. Register all widgets (one line!)
        registerWidgets();
    }
    
    // ─────────────────────────────────────────────────────────────────
    // WIDGET REGISTRATION (~15 lines with WidgetCollector)
    // ─────────────────────────────────────────────────────────────────
    private void registerWidgets() {
        clearChildren();
        
        List<ClickableWidget> widgets = WidgetCollector.collectAll(
            headerBar, tabBar, 
            currentTab == TabType.PROFILES ? null : selectorBar,
            currentTab == TabType.PROFILES ? profilesPanel : contentArea,
            currentTab == TabType.PROFILES ? null : shapePanel
        );
        
        for (var w : widgets) {
            addDrawableChild(w);
        }
        
        // Modal overlay
        if (activeModal != null && activeModal.isVisible()) {
            for (var w : activeModal.getWidgets()) {
                addDrawableChild(w);
            }
        }
    }
    
    // ─────────────────────────────────────────────────────────────────
    // TAB/MODE SWITCHING (~30 lines)
    // ─────────────────────────────────────────────────────────────────
    private void toggleMode() { ... }
    private void switchTab(TabType tab) { ... }
    private void updateCurrentTab() { ... }
    
    // ─────────────────────────────────────────────────────────────────
    // STATE CALLBACKS (~20 lines)
    // ─────────────────────────────────────────────────────────────────
    private void onLayerSelected(String name) { ... }
    private void onPrimitiveSelected(String name) { ... }
    private void onPresetSelected(String presetName) { ... }
    
    // ─────────────────────────────────────────────────────────────────
    // MODALS (~20 lines - delegates to ModalFactory)
    // ─────────────────────────────────────────────────────────────────
    private void showLayerModal(String layerName) {
        activeModal = ModalFactory.createLayerModal(state, layerName, textRenderer, 
            width, height, () -> { activeModal = null; registerWidgets(); });
        registerWidgets();
    }
    
    private void showPrimitiveModal(String primName) {
        activeModal = ModalFactory.createPrimitiveModal(state, primName, textRenderer,
            width, height, () -> { activeModal = null; registerWidgets(); });
        registerWidgets();
    }
    
    // ─────────────────────────────────────────────────────────────────
    // TICK & RENDER (~60 lines)
    // ─────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        contentArea.tick();
        shapePanel.tick();
        if (currentTab == TabType.PROFILES) profilesPanel.tick();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        layout.renderBackground(context, width, height);
        
        // Components
        layout.renderFrame(context);
        headerBar.render(context, mouseX, mouseY, delta);
        tabBar.render(context, mouseX, mouseY, delta);
        
        if (currentTab != TabType.PROFILES) {
            selectorBar.render(context, mouseX, mouseY, delta);
            contentArea.render(context, mouseX, mouseY, delta);
            renderPanelInBounds(context, shapePanel, ...);
        } else {
            profilesPanel.render(context, mouseX, mouseY, delta);
        }
        
        if (layout.hasPreviewWidget()) {
            render3DPreview(context, layout.getPreviewBounds(), delta);
        }
        
        statusBar.render(context, mouseX, mouseY, delta);
        
        // Widgets
        super.render(context, mouseX, mouseY, delta);
        
        // Overlays
        renderModalsAndToasts(context, mouseX, mouseY, delta);
    }
    
    // ─────────────────────────────────────────────────────────────────
    // INPUT HANDLERS (~100 lines - these are essential, keep as-is)
    // ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(...) { ... }
    
    @Override
    public boolean mouseClicked(...) { ... }
    
    @Override
    public boolean keyPressed(...) { ... }
    
    @Override
    public boolean charTyped(...) { ... }
    
    // ─────────────────────────────────────────────────────────────────
    // LIFECYCLE (~20 lines)
    // ─────────────────────────────────────────────────────────────────
    @Override
    public boolean shouldPause() { return mode == GuiMode.FULLSCREEN; }
    
    @Override
    public void close() { ... }
    
    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS (~30 lines)
    // ─────────────────────────────────────────────────────────────────
    private void initShapePanel() { ... }
    private void initProfilesPanel() { ... }
    private void render3DPreview(DrawContext context, Bounds bounds, float delta) { ... }
    private void renderPanelInBounds(...) { ... }
    private void renderModalsAndToasts(...) { ... }
}
```

### Line Count Estimate

| Section | Lines |
|---------|-------|
| Fields + Constructors | 35 |
| init() | 40 |
| registerWidgets() | 15 |
| Tab/Mode switching | 30 |
| State callbacks | 20 |
| Modals (delegated) | 20 |
| tick() + render() | 60 |
| Input handlers | 100 |
| Lifecycle | 20 |
| Private helpers | 30 |
| **TOTAL** | **~370** |

---

## New Files Created

| File | Purpose | Est. Lines |
|------|---------|------------|
| `component/HeaderBar.java` | Title bar buttons | ~80 |
| `component/TabBar.java` | Main tabs + preset dropdown | ~130 |
| `component/SelectorBar.java` | Layer + primitive selectors | ~80 |
| `component/ContentArea.java` | Sub-tabs management | ~100 |
| `component/ContentProviderFactory.java` | Lazy panel creation | ~80 |
| `util/GuiConfigPersistence.java` | Mode save/load | ~40 |
| `util/WidgetCollector.java` | Auto-registration | ~40 |
| `widget/ModalFactory.java` | Layer/Prim rename modals | ~150 |
| `widget/PanelWrapper.java` | (already in SubTabPane) | Move |

**Net change**: -797 from Screen, +700 in new files  
**Benefit**: Each file has single responsibility, testable in isolation

---

## Options A, B, C, D Coverage

| Option | Description | Implementation |
|--------|-------------|----------------|
| **A** | Layout Strategies | Use existing `LayoutManager` + add `renderBackground()`, `renderFrame()` |
| **B** | Widget Container | New `WidgetCollector.collectAll()` utility |
| **C** | Component-Based | `HeaderBar`, `TabBar`, `SelectorBar`, `ContentArea` |
| **D** | Widget Builders | Add `valueCycler()`, `boolCycler()` to `GuiWidgets.java` |

---

## Dependencies / Risk Analysis

| Extraction | Risk | Mitigation |
|------------|------|------------|
| HeaderBar | Low | Just button creation, no state |
| TabBar | Low | Callbacks passed in |
| SelectorBar | Medium | Uses `CompactSelector` + state callbacks |
| ContentArea | Medium | SubTabPane management |
| ModalFactory | Low | Self-contained dialogs |
| WidgetCollector | Low | Simple collection utility |
| LayoutManager changes | Medium | Add render methods to existing interface |

---

## Verification

After refactoring, the following tests should pass:

1. ✅ Open GUI → all buttons visible
2. ✅ Switch fullscreen ↔ windowed → UI updates correctly
3. ✅ Switch tabs → content changes
4. ✅ Add/remove layer → selector updates
5. ✅ Add/remove primitive → selector updates
6. ✅ Click layer name → modal opens, can rename
7. ✅ Click primitive name → modal opens, can rename
8. ✅ Reset button → state resets
9. ✅ Escape → GUI closes
10. ✅ Ctrl+Z/Y → undo/redo works
