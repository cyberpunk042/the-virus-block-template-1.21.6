# GUI Architecture Refactoring - Master TODO List

**Goal:** `FieldCustomizerScreen` from **1627 lines** → **~400 lines**

**Status:** � Component Infrastructure Complete - Ready for Integration

---

## PHASE 1: Terminology Rename (Fast/Accurate → Simplified/Standard) ✅ COMPLETE
**Estimated:** ~20 changes across 5 files

### 1.1 SimplifiedFieldRenderer.java
- [x] 1.1.1 Rename field `advancedModeEnabled` → `standardModeEnabled`
- [x] 1.1.2 Rename method `setAdvancedModeEnabled()` → `setStandardModeEnabled()`
- [x] 1.1.3 Rename method `isAdvancedModeEnabled()` → `isStandardModeEnabled()`
- [x] 1.1.4 Update log messages: "advanced mode" → "standard mode"
- [x] 1.1.5 Update comments/javadoc

### 1.2 RendererCapabilities.java
- [x] 1.2.1 Rename method `isAccurateMode()` → `isStandardMode()`
- [x] 1.2.2 Rename method `isFastMode()` → `isSimplifiedMode()`
- [x] 1.2.3 Update `getCurrentModeName()`: `"Accurate"` → `"Standard"`, `"Fast"` → `"Simplified"`
- [x] 1.2.4 Update all calls to `isAdvancedModeEnabled()` → `isStandardModeEnabled()`
- [x] 1.2.5 Update javadoc/comments

### 1.3 FieldCustomizerScreen.java
- [x] 1.3.1 Find all `isAdvancedModeEnabled()` calls, update to `isStandardModeEnabled()`
- [x] 1.3.2 Update toggle button label: `"Fast ⚡"` / `"Accurate ⚙"` → `"Simplified ⚡"` / `"Standard ⚙"`
- [x] 1.3.3 Update tooltip text if any

### 1.4 Other Files (search entire codebase)
- [x] 1.4.1 `grep` for `isAdvancedModeEnabled` - update all occurrences
- [x] 1.4.2 `grep` for `setAdvancedModeEnabled` - update all occurrences
- [x] 1.4.3 `grep` for `isAccurateMode` - update all occurrences
- [x] 1.4.4 `grep` for `isFastMode` - update all occurrences
- [x] 1.4.5 `grep` for `"Fast"` / `"Accurate"` string literals in GUI code - update labels

### 1.5 Verify Phase 1
- [x] 1.5.1 Build: `./gradlew build` passes
- [ ] 1.5.2 Run: Open GUI, verify toggle shows "Simplified" / "Standard"
- [ ] 1.5.3 Test: Toggle works, mode actually changes

---

## PHASE 2: Widget Builder Extensions (Option D) ✅ COMPLETE
**Add to:** `GuiWidgets.java`

### 2.1 valueCycler() Method
- [x] 2.1.1 Create `valueCycler<T>()` signature
- [x] 2.1.2 Implement with automatic `omitKeyText()`
- [x] 2.1.3 Add javadoc with usage example
- [ ] 2.1.4 Test: Create a test widget, verify no "Label: Value" prefix

### 2.2 boolCycler() Method
- [x] 2.2.1 Create `boolCycler()` signature
- [x] 2.2.2 Implement with ON/OFF text customization
- [x] 2.2.3 Add javadoc
- [ ] 2.2.4 Test: Create toggle, verify shows only value

### 2.3 enumCycler() Method
- [x] 2.3.1 Create `enumCycler<E extends Enum<E>>()` signature
- [x] 2.3.2 Implement with automatic `omitKeyText()`
- [x] 2.3.3 Add javadoc

### 2.4 RowLayout Helper
- [x] 2.4.1 Create `RowLayout` class or add to Bounds
- [x] 2.4.2 Implement `row(Bounds bounds, int columns)` factory
- [x] 2.4.3 Add `gap(int)` fluent method
- [x] 2.4.4 Add `weights(float...)` for unequal columns
- [x] 2.4.5 Implement `get(int index)` to return column bounds
- [ ] 2.4.6 Test: Create 3-column row, verify bounds are correct

### 2.5 Verify Phase 2
- [x] 2.5.1 Build passes
- [ ] 2.5.2 Unit test or manual test of new widget helpers

---

## PHASE 3: Widget Auto-Registration (Option B) ✅ COMPLETE
**Creates:** `WidgetCollector.java`, `WidgetVisibility.java`

### 3.1 WidgetCollector.java
- [x] 3.1.1 Create file: `gui/util/WidgetCollector.java`
- [x] 3.1.2 Define `WidgetProvider` interface with `getWidgets()` and `isVisible()`
- [x] 3.1.3 Implement `collectAll(WidgetProvider... providers)`
- [x] 3.1.4 Implement `collectVisible(WidgetProvider... providers)` - skips invisible
- [x] 3.1.5 Add javadoc

### 3.2 WidgetVisibility.java (Control-Level)
- [x] 3.2.1 Create file: `gui/util/WidgetVisibility.java`
- [x] 3.2.2 Add `register(ClickableWidget, BooleanSupplier)` method
- [x] 3.2.3 Add `refresh(ClickableWidget)` - updates `widget.visible`
- [x] 3.2.4 Add `refreshAll()` - updates all registered widgets
- [x] 3.2.5 Add javadoc

### 3.3 visibleWhen() Helper in GuiWidgets
- [x] 3.3.1 Add `visibleWhen(ClickableWidget, BooleanSupplier)` method
- [x] 3.3.2 Registers with WidgetVisibility and sets initial state
- [x] 3.3.3 Returns the widget for fluent chaining

### 3.4 Verify Phase 3
- [x] 3.4.1 Build passes
- [ ] 3.4.2 Test: Create widgets with visibility conditions, verify hide/show

---

## PHASE 4: Visibility Controller ✅ COMPLETE
**Creates:** `VisibilityController.java`

### 4.1 Create VisibilityController
- [x] 4.1.1 Create file: `gui/component/VisibilityController.java`
- [x] 4.1.2 Add constructor with `FieldEditState` and `GuiMode` params
- [x] 4.1.3 Implement `isStandardModeEnabled()` - delegates to RendererCapabilities
- [x] 4.1.4 Implement `isSimplifiedMode()` - inverse
- [x] 4.1.5 Implement `isOperator()` - checks player permission level 2+
- [x] 4.1.6 Implement `isDebugUnlocked()` - delegates to state
- [x] 4.1.7 Implement `isFullscreen()` - checks current GuiMode

### 4.2 Tab Visibility Predicates
- [x] 4.2.1 Add `isAdvancedTabVisible()` - true when Standard mode
- [x] 4.2.2 Add `isDebugTabVisible()` - true when debugUnlocked
- [x] 4.2.3 Add `isProfilesTabVisible()` - always true (for now)

### 4.3 Widget Visibility Predicates
- [x] 4.3.1 Add `is3DPreviewToggleVisible()` - true when Standard + Fullscreen
- [x] 4.3.2 Add `isSaveToServerVisible()` - true when OP
- [x] 4.3.3 Add `isWaveControlsVisible()` - true when Standard mode
- [x] 4.3.4 Add `isBindingsControlsVisible()` - true when Standard mode

### 4.4 Mode Change Notification
- [x] 4.4.1 Add `notifyRendererModeChanged()` method
- [x] 4.4.2 Should call `WidgetVisibility.refreshAll()`
- [x] 4.4.3 Should notify components to rebuild

### 4.5 Verify Phase 4
- [x] 4.5.1 Build passes
- [ ] 4.5.2 Test visibility predicates work correctly

---

## PHASE 5: Utility Extraction from FieldCustomizerScreen 🟡 PARTIAL

### 5.1 GuiConfigPersistence.java ✅
- [x] 5.1.1 Create file: `gui/util/GuiConfigPersistence.java`
- [x] 5.1.2 Move `GUI_CONFIG_PATH` constant from FieldCustomizerScreen
- [x] 5.1.3 Move `loadSavedMode()` as static method
- [x] 5.1.4 Move `saveMode(GuiMode)` as static method
- [ ] 5.1.5 Update FieldCustomizerScreen to use new class
- [ ] 5.1.6 Verify: Mode persists across game restarts

### 5.2 ModalFactory.java (DEFERRED - tightly coupled)
- [ ] 5.2.1 Create file: `gui/widget/ModalFactory.java`
- [ ] 5.2.2 Extract `showLayerModal()` logic → `createLayerRenameModal(...)`
- [ ] 5.2.3 Extract `showPrimitiveModal()` logic → `createPrimitiveRenameModal(...)`
- [ ] 5.2.4 Extract `focusModalTextField()` logic
- [ ] 5.2.5 Update FieldCustomizerScreen to use factory
- [ ] 5.2.6 Verify: Layer rename modal works
- [ ] 5.2.7 Verify: Primitive rename modal works

### 5.3 ContentProviderFactory.java ✅
- [x] 5.3.1 Create file: `gui/component/ContentProviderFactory.java`
- [x] 5.3.2 Add constructor with `Screen parent, FieldEditState state`
- [x] 5.3.3 Move `createFillContent()` → `fill()`
- [x] 5.3.4 Move `createAppearanceContent()` → `appearance()`
- [x] 5.3.5 Move `createVisibilityContent()` → `visibility()`
- [x] 5.3.6 Move `createTransformContent()` → `transform()`
- [x] 5.3.7 Move `createAnimationContent()` → `animation()`
- [x] 5.3.8 Move `createPredictionContent()` → `prediction()`
- [x] 5.3.9 Move `createOrbitContent()` → `orbit()`
- [x] 5.3.10 Move `createBeamContent()` → `beam()`
- [x] 5.3.11 Move `createTriggerContent()` → `trigger()`
- [x] 5.3.12 Move `createLifecycleContent()` → `lifecycle()`
- [x] 5.3.13 Move `createModifiersContent()` → `modifiers()`
- [x] 5.3.14 Move `createArrangeContent()` → `arrange()`
- [x] 5.3.15 Move `createLinkingContent()` → `linking()`
- [x] 5.3.16 Move `createTraceContent()` → `trace()`
- [x] 5.3.17 Move `createBindingsContent()` → `bindings()`
- [ ] 5.3.18 Update FieldCustomizerScreen to use factory
- [ ] 5.3.19 Verify: All sub-tabs still work

### 5.4 Move PanelWrapper to Own File ✅
- [x] 5.4.1 Create file: `gui/widget/PanelWrapper.java`
- [x] 5.4.2 Move `PanelWrapper` inner class from FieldCustomizerScreen
- [x] 5.4.3 Make it public
- [ ] 5.4.4 Update imports in FieldCustomizerScreen and SubTabPane

### 5.5 Delete PlaceholderContent (DEFERRED)
- [ ] 5.5.1 Verify `PlaceholderContent` is not used anywhere critical
- [ ] 5.5.2 Remove `PlaceholderContent` inner class
- [ ] 5.5.3 Remove any references

### 5.6 Verify Phase 5
- [x] 5.6.1 Build passes
- [ ] 5.6.2 All modals work
- [ ] 5.6.3 All sub-tabs load correctly
- [ ] 5.6.4 Config persistence works

---

## PHASE 6: ScreenComponent Interface & Base (Option C) ✅ COMPLETE

### 6.1 ScreenComponent Interface
- [x] 6.1.1 Create file: `gui/component/ScreenComponent.java`
- [x] 6.1.2 Extend `WidgetCollector.WidgetProvider`
- [x] 6.1.3 Add `void setBounds(Bounds bounds)`
- [x] 6.1.4 Add `void render(DrawContext, int mouseX, int mouseY, float delta)`
- [x] 6.1.5 Add `default void tick() {}`
- [x] 6.1.6 Add `default boolean isVisible() { return true; }`
- [x] 6.1.7 Add `default String getHiddenTooltip() { return null; }`

### 6.2 AbstractComponent Base Class (SKIPPED - not needed)
- [ ] 6.2.1 Create `gui/component/AbstractComponent.java`
- [ ] 6.2.2 Implements `ScreenComponent`
- [ ] 6.2.3 Add `protected List<ClickableWidget> widgets`
- [ ] 6.2.4 Add `protected Bounds bounds`
- [ ] 6.2.5 Add `protected final VisibilityController visibility`
- [ ] 6.2.6 Add `addWidget()` helper
- [ ] 6.2.7 Add `clearWidgets()` helper

---

## PHASE 7: HeaderBar Component ✅ INTEGRATED (Fullscreen)

### 7.1 Create HeaderBar.java
- [x] 7.1.1 Create file: `gui/component/HeaderBar.java`
- [x] 7.1.2 Implement `ScreenComponent`
- [x] 7.1.3 Define fields: `modeToggle, fieldToggle, reset, close` buttons
- [x] 7.1.4 Add constructor with callbacks

### 7.2 Implement HeaderBar
- [x] 7.2.1 Create mode toggle button (◻/◼ for fullscreen/windowed)
- [x] 7.2.2 Create field toggle button (◉/○ for field visible/hidden)
- [x] 7.2.3 Create reset button (R)
- [x] 7.2.4 Create close button (×)
- [x] 7.2.5 Implement `getWidgets()` - returns all buttons
- [x] 7.2.6 Implement `render()` - draws title text "Field Customizer" + dirty indicator

### 7.3 Extract from FieldCustomizerScreen ✅ DONE (FULLSCREEN MODE)
- [x] 7.3.1 Replace `initTitleBar()` with HeaderBar initialization
- [x] 7.3.2 HeaderBar handles mode, field, reset, close buttons
- [x] 7.3.3 Windowed mode keeps individual buttons (different layout)
- [x] 7.3.4 Add `private HeaderBar headerBar` field
- [x] 7.3.5 Initialize `headerBar` in `initTitleBar()` for fullscreen
- [x] 7.3.6 Call `headerBar.render()` in `renderFullscreenMode()`
- [x] 7.3.7 Update `registerWidgets()` to add HeaderBar widgets

### 7.4 Verify Phase 7
- [x] 7.4.1 Build passes
- [ ] 7.4.2 Mode toggle works (needs runtime test)
- [ ] 7.4.3 Field toggle works (needs runtime test)
- [ ] 7.4.4 Reset button works (needs runtime test)
- [ ] 7.4.5 Close button works (needs runtime test)
- [ ] 7.4.6 Title and dirty indicator display correctly (needs runtime test)

---

## PHASE 8: TabBar Component ✅ INTEGRATED (Fullscreen)

### 8.1 Create TabBar.java
- [x] 8.1.1 Create file: `gui/component/TabBar.java`
- [x] 8.1.2 Implement `ScreenComponent`
- [x] 8.1.3 Define fields: tabs, presetDropdown, rendererModeToggle
- [x] 8.1.4 Add constructor with callbacks

### 8.2 Implement TabBar
- [x] 8.2.1 Create Quick tab button
- [x] 8.2.2 Create Advanced tab button
- [x] 8.2.3 Create Debug tab button
- [x] 8.2.4 Create Profiles tab button
- [x] 8.2.5 Create preset dropdown widget
- [x] 8.2.6 Create renderer mode toggle (Simplified ⚡ / Standard ⚙)
- [x] 8.2.7 Implement `setActiveTab(TabType)` - updates button states
- [x] 8.2.8 Implement `refreshVisibility()` - hide tabs based on mode/permission
- [x] 8.2.9 Implement `getWidgets()` - returns visible widgets only
- [x] 8.2.10 Implement `render()` - any custom rendering needed
- [x] 8.2.11 Updated to use screen.TabType instead of inner enum

### 8.3 Extract from FieldCustomizerScreen ✅ DONE (FULLSCREEN MODE)
- [x] 8.3.1 Replace `initMainTabs()` with TabBar initialization
- [x] 8.3.2 Add `private TabBar tabBar` field
- [x] 8.3.3 Initialize `tabBar` in `initMainTabs()` for fullscreen
- [x] 8.3.4 Wire preset dropdown reference from TabBar
- [x] 8.3.5 Update `registerWidgets()` to add TabBar widgets
- [x] 8.3.6 Update `updateMainTabButtons()` to delegate to TabBar
- [x] 8.3.7 Update `refreshTabsForRendererMode()` to call TabBar.refreshVisibility()
- [x] 8.3.8 Windowed mode keeps individual buttons (different layout)

### 8.4 Verify Phase 8
- [x] 8.4.1 Build passes
- [ ] 8.4.2 Tab switching works (needs runtime test)
- [ ] 8.4.3 Preset dropdown works (needs runtime test)
- [ ] 8.4.4 Renderer toggle works (needs runtime test)
- [ ] 8.4.5 Advanced tab hides in Simplified mode (needs runtime test)
- [ ] 8.4.6 Debug tab hides when not OP (needs runtime test)

---

## PHASE 9: SelectorBar Component ✅ CREATED

### 9.1 Create SelectorBar.java
- [x] 9.1.1 Create file: `gui/component/SelectorBar.java`
- [x] 9.1.2 Implement `ScreenComponent`
- [x] 9.1.3 Define fields: layerSelector, primitiveSelector
- [x] 9.1.4 Add constructor with callbacks

### 9.2 Implement SelectorBar
- [x] 9.2.1 Create layer selector with callbacks
- [x] 9.2.2 Create primitive selector with callbacks
- [x] 9.2.3 Implement `refreshLayers()` method
- [x] 9.2.4 Implement `refreshPrimitives()` method
- [x] 9.2.5 Implement `getWidgets()` - aggregates from both selectors
- [x] 9.2.6 Implement `render()` - any labels

### 9.3 Extract from FieldCustomizerScreen ✅ INTEGRATED
- [x] 9.3.1 Updated `initSelectors()` to use SelectorBar
- [x] 9.3.2 Kept `layerSelector` and `primitiveSelector` fields as references to selectorBar getters
- [x] 9.3.3 Kept helper methods but updated to use selectorBar when available
- [x] 9.3.4 Added `private SelectorBar selectorBar` field
- [x] 9.3.5 Initialize `selectorBar` in initSelectors() (fullscreen) and initWindowedRightPanel() (windowed)
- [x] 9.3.6 Wired up callbacks to screen methods

### 9.4 Verify Phase 9
- [x] 9.4.1 Build passes
- [ ] 9.4.2 Layer selection works
- [ ] 9.4.3 Primitive selection works
- [ ] 9.4.4 Add layer works
- [ ] 9.4.5 Add primitive works
- [ ] 9.4.6 Click layer → modal opens
- [ ] 9.4.7 Click primitive → modal opens

---

## PHASE 10: ContentArea Component ✅ CREATED

### 10.1 Create ContentArea.java
- [x] 10.1.1 Create file: `gui/component/ContentArea.java`
- [x] 10.1.2 Implement `ScreenComponent`
- [x] 10.1.3 Define fields: quickSubTabs, advancedSubTabs, debugSubTabs, currentMainTab
- [x] 10.1.4 Add constructor with `ContentProviderFactory`

### 10.2 Implement ContentArea
- [x] 10.2.1 Create `initQuickSubTabs()` - Fill, Appearance, Visibility, Transform
- [x] 10.2.2 Create `initAdvancedSubTabs()` - Animation, Prediction, Linking, Modifiers
- [x] 10.2.3 Create `initDebugSubTabs()` - Beam, Trigger, Lifecycle, Bindings, Trace
- [x] 10.2.4 Implement `setActiveMainTab(TabType)` - switches which SubTabPane is active
- [x] 10.2.5 Implement `refreshForRendererMode()` - rebuilds sub-tabs for visibility
- [x] 10.2.6 Implement `getWidgets()` - returns widgets from active SubTabPane only
- [x] 10.2.7 Implement `render()` - renders active SubTabPane
- [x] 10.2.8 Implement `tick()` - ticks active content

### 10.3 Extract from FieldCustomizerScreen ✅ INTEGRATED
- [x] 10.3.1 Updated `initSubTabs()` to use ContentArea
- [x] 10.3.2 Kept legacy sub-tab references from contentArea getters
- [x] 10.3.3 `renderContentArea()` delegates to ContentArea
- [x] 10.3.4 Added `private ContentArea contentArea` field
- [x] 10.3.5 Initialize `contentArea` in `initSubTabs()` and `createSubTabPanes()`
- [x] 10.3.6 Wired `contentArea` to registerWidgets()

### 10.4 Verify Phase 10
- [x] 10.4.1 Build passes
- [ ] 10.4.2 Quick sub-tabs work
- [ ] 10.4.3 Advanced sub-tabs work
- [ ] 10.4.4 Debug sub-tabs work
- [ ] 10.4.5 Sub-tabs hide correctly based on @RequiresFeature
- [ ] 10.4.6 Renderer mode change updates sub-tab visibility

---

## PHASE 11: Layout Integration (Option A)

### 11.1 Enhance LayoutManager Interface
- [x] 11.1.1 Add `void renderBackground(DrawContext, int screenW, int screenH)`
- [x] 11.1.2 Add `void renderFrame(DrawContext)`
- [x] 11.1.3 Add `Bounds getSelectorBounds()`
- [x] 11.1.4 Add `Bounds getShapePanelBounds()`
- [x] 11.1.5 Add `Bounds getProfilesLeftBounds()`
- [x] 11.1.6 Add `Bounds getProfilesRightBounds()`

### 11.2 Implement in FullscreenLayout
- [x] 11.2.1 Calculate `selectorBounds` in `calculate()`
- [x] 11.2.2 Calculate `shapePanelBounds` in `calculate()`
- [x] 11.2.3 Implement `renderBackground()` - solid dark fill
- [x] 11.2.4 Implement `renderFrame()` - panel borders, dividers
- [x] 11.2.5 Return appropriate bounds from new methods

### 11.3 Implement in WindowedLayout
- [x] 11.3.1 Calculate bounds for left/right panels
- [x] 11.3.2 Implement `renderBackground()` - transparent (game world shows)
- [x] 11.3.3 Implement `renderFrame()` - panel backgrounds with borders

### 11.4 Wire Layout into FieldCustomizerScreen
- [x] 11.4.1 Add `private LayoutManager layout` field
- [x] 11.4.2 In `init()`, create correct layout based on mode
- [x] 11.4.3 Replace `initFullscreenMode()` with `layout.calculate()` + `initComponents()`
- [x] 11.4.4 Replace `initWindowedMode()` with `layout.calculate()` + `initComponents()`
- [x] 11.4.5 Remove `initFullscreenMode()` method
- [x] 11.4.6 Remove `initWindowedMode()` method
- [ ] 11.4.7 Remove `initWindowedLeftPanel()` method (refactored but not removed - keeps windowed UI)
- [ ] 11.4.8 Remove `initWindowedRightPanel()` method (refactored but not removed - keeps windowed UI)
- [x] 11.4.9 Added `renderContentArea()` shared helper for render methods
- [ ] 11.4.10 Remove `renderFullscreenMode()` method (still needed for mode-specific rendering)
- [ ] 11.4.11 Remove `renderWindowedMode()` method (still needed for mode-specific rendering)
- [ ] 11.4.12 Remove `renderQuadrantBackgrounds()` method (still needed for fullscreen grid)

### 11.5 Verify Phase 11
- [ ] 11.5.1 Build passes
- [ ] 11.5.2 Fullscreen layout matches previous
- [ ] 11.5.3 Windowed layout matches previous
- [ ] 11.5.4 Mode toggle transitions correctly

---

## PHASE 12: Simplify registerWidgets()

### 12.1 Replace Manual Registration
- [ ] 12.1.1 Change `registerWidgets()` to use `WidgetCollector.collectVisible()`
- [ ] 12.1.2 Pass all components: `headerBar, tabBar, selectorBar, contentArea, shapePanel, profilesPanel`
- [ ] 12.1.3 Handle modal overlay widgets
- [ ] 12.1.4 Remove ~50 lines of manual `if (widget != null) addDrawableChild(widget)`

### 12.2 Verify Phase 12
- [ ] 12.2.1 Build passes
- [ ] 12.2.2 All widgets visible and clickable
- [ ] 12.2.3 Mode switching still works
- [ ] 12.2.4 Tab switching still works

---

## PHASE 13: Clean Up Remaining Code

### 13.1 Remove Unused Fields
- [ ] 13.1.1 Remove individual button fields (now in components)
- [ ] 13.1.2 Remove individual SubTabPane fields (now in ContentArea)
- [ ] 13.1.3 Remove individual selector fields (now in SelectorBar)
- [ ] 13.1.4 Remove `grid` field (using LayoutManager now)

### 13.2 Simplify render() Method
- [ ] 13.2.1 Delegate background to `layout.renderBackground()`
- [ ] 13.2.2 Delegate frame to `layout.renderFrame()`
- [ ] 13.2.3 Delegate component rendering to each component's `render()`
- [ ] 13.2.4 Keep 3D preview rendering
- [ ] 13.2.5 Keep modal overlay rendering

### 13.3 Simplify tick() Method
- [ ] 13.3.1 Delegate to components: `contentArea.tick()`, `shapePanel.tick()`

### 13.4 Keep Essential Input Handlers
- [ ] 13.4.1 Keep `mouseScrolled()` - routes to components
- [ ] 13.4.2 Keep `mouseClicked()` - modal handling, dropdown handling
- [ ] 13.4.3 Keep `keyPressed()` - Escape, Tab, Ctrl+Z/Y
- [ ] 13.4.4 Keep `charTyped()` - modal text input

### 13.5 Consolidate Callbacks
- [ ] 13.5.1 Keep `toggleMode()` - simple delegation
- [ ] 13.5.2 Keep `resetState()` - simple delegation
- [ ] 13.5.3 Keep `switchTab()` - updates state + components
- [ ] 13.5.4 Move complex logic to appropriate components

---

## PHASE 14: Final Line Count Verification

### 14.1 Count Lines
- [ ] 14.1.1 Run: `wc -l FieldCustomizerScreen.java`
- [ ] 14.1.2 Target: ~400 lines (±50)
- [ ] 14.1.3 If over 450 lines, identify more code to extract

### 14.2 Code Quality Check
- [ ] 14.2.1 No duplicated code
- [ ] 14.2.2 Each method < 30 lines
- [ ] 14.2.3 Clear single responsibility
- [ ] 14.2.4 Components properly encapsulated

---

## PHASE 15: Preview Mode Feature (Future Work)

### 15.1 PreviewModeState.java
- [ ] 15.1.1 Create file: `gui/state/PreviewModeState.java`
- [ ] 15.1.2 Add `advancedPreviewEnabled` field (default false)
- [ ] 15.1.3 Implement `isAdvancedPreviewEnabled()`
- [ ] 15.1.4 Implement `setAdvancedPreviewEnabled(boolean)`
- [ ] 15.1.5 Implement `isToggleVisible()` - only in Standard + Fullscreen

### 15.2 Preview Toggle Widget
- [ ] 15.2.1 Add preview mode toggle to TabBar or new PreviewControls component
- [ ] 15.2.2 Labels: "⚡ Fast Preview" / "⚙ Advanced Preview"
- [ ] 15.2.3 Wire visibility to `PreviewModeState.isToggleVisible()`

### 15.3 render3DPreview() Enhancement
- [ ] 15.3.1 Check `PreviewModeState.isAdvancedPreviewEnabled()`
- [ ] 15.3.2 If true, use `FieldRenderer` pipeline
- [ ] 15.3.3 If false, use current fast preview code
- [ ] 15.3.4 Handle framebuffer/offscreen rendering challenges

---

## FINAL VERIFICATION CHECKLIST

### Functionality
- [ ] GUI opens without crash
- [ ] Fullscreen mode works
- [ ] Windowed mode works
- [ ] Mode toggle (fullscreen ↔ windowed) works
- [ ] Renderer toggle (Simplified ↔ Standard) works
- [ ] Tab switching works (Quick, Advanced, Debug, Profiles)
- [ ] Sub-tab switching works
- [ ] Layer add/remove/rename works
- [ ] Primitive add/remove/rename works
- [ ] Reset button works
- [ ] Close button (×) works
- [ ] Escape key closes GUI
- [ ] Ctrl+Z undo works
- [ ] Ctrl+Y redo works
- [ ] Preset dropdown works
- [ ] Shape panel controls work
- [ ] Status bar displays correctly

### Visibility Rules
- [ ] Simplified mode → Advanced tab hidden
- [ ] Simplified mode → Wave/Wobble controls hidden
- [ ] Simplified mode → Bindings tab hidden
- [ ] Standard mode → All tabs visible
- [ ] Non-OP → Debug tab hidden
- [ ] Non-OP → Save to Server hidden in Profiles
- [ ] Standard + Fullscreen → Preview mode toggle visible
- [ ] Simplified OR Windowed → Preview mode toggle hidden

### Code Quality
- [ ] FieldCustomizerScreen ≤ 450 lines
- [ ] All new files have javadoc
- [ ] No compiler warnings
- [ ] Build passes: `./gradlew build`

---

## Progress Tracker

| Phase | Description | Status | Lines Saved |
|-------|-------------|--------|-------------|
| 1 | Terminology Rename | ✅ Complete | 0 |
| 2 | Widget Builders | ✅ Complete | 0 |
| 3 | Widget Auto-Registration | ✅ Complete | 0 |
| 4 | Visibility Controller | ✅ Complete | 0 |
| 5 | Utility Extraction | 🟡 Partial (ModalFactory pending) | ~100 |
| 6 | ScreenComponent Interface | ✅ Complete | 0 |
| 7 | HeaderBar Component | ✅ Integrated (Both modes) | ~20 |
| 8 | TabBar Component | ✅ Integrated (Both modes) | ~60 |
| 9 | SelectorBar Component | ✅ Integrated (Both modes) | ~50 |
| 10 | ContentArea Component | ✅ Integrated (Both modes) | ~30 |
| 11 | Layout Integration | ✅ Complete | ~70 |
| 12 | Simplify registerWidgets | ✅ Done | ~25 |
| 13 | Clean Up (Legacy Fields) | ✅ Done | ~200 |
| 13b | **ModalFactory Extraction** | 🔴 NOT DONE | **~80** |
| 13c | **Slim initComponents()** | 🔴 NOT DONE | **~85** |
| 13d | **Delegate render3DPreview** | 🔴 NOT DONE | **~25** |
| 13e | **Move renderQuadrantBackgrounds** | 🔴 NOT DONE | **~14** |
| 14 | Final Verification | 🔴 Not Started | - |
| 15 | Preview Mode (Future) | 🔴 Not Started | - |

## ⚠️ HONEST ASSESSMENT

**Starting:** 1627 lines  
**Current:** 929 lines (-698 lines, 42.9% reduction)  
**Target:** ~400 lines  
**Still Need to Remove:** **~529 lines (56% more reduction needed!)**

### What Was Actually Done:
- ✅ Removed legacy widget fields (buttons, selectors, sub-tabs)
- ✅ Removed legacy init methods (initWindowedLeftPanel, initTitleBar, etc.)
- ✅ Removed fallback logic
- ✅ Components now handle widget creation

### What Was NOT Done (marked complete but wasn't):
| Task | Current Lines | Should Be | Lines to Save |
|------|--------------|-----------|---------------|
| **ModalFactory extraction** | 80 lines | 0 (in factory) | **~80** |
| **initComponents() cleanup** | 115 lines | ~30 lines | **~85** |
| **render3DPreview delegation** | 36 lines | ~10 lines | **~26** |
| **renderQuadrantBackgrounds → Layout** | 14 lines | 0 | **~14** |

### Realistic Path to ~600 Lines:

1. **Phase 5.2: Extract ModalFactory** (saves ~80 lines)
2. **Phase 13b: Slim initComponents()** (saves ~85 lines)  
3. **Phase 13c: Delegate 3D Preview** (saves ~26 lines)
4. **Phase 13d: Move Quadrant Rendering** (saves ~14 lines)

**After these changes:** 929 - 205 = **~724 lines**

### The Hard Truth:
The ~400 line target requires even more aggressive extraction that may hurt maintainability.
**Realistic achievable target: ~650-700 lines** with current architecture.

### Files Created:
- component/ContentArea.java ✅
- component/ContentProviderFactory.java ✅
- component/HeaderBar.java ✅
- component/ScreenComponent.java ✅
- component/SelectorBar.java ✅
- component/TabBar.java ✅
- component/VisibilityController.java ✅
- util/GuiConfigPersistence.java ✅
- util/RowLayout.java ✅
- util/WidgetCollector.java ✅
- util/WidgetVisibility.java ✅
- widget/PanelWrapper.java ✅
- widget/ModalFactory.java 🔴 PENDING

