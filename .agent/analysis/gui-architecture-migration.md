# GUI Architecture Migration Analysis

## Overview

This documents maps the **OLD working FieldCustomizerScreen** (1627 lines, from git HEAD) to the **NEW component-based architecture**. The goal is to identify what logic/features exist in the OLD file and ensure they are properly implemented in the NEW components.

## File Structure Comparison

| OLD File Section | Lines | NEW Component | Status |
|-----------------|-------|---------------|--------|
| Constants & Fields | 51-142 | Distributed + FieldCustomizerScreen | ❓ |
| Config Persistence | 68-99 | GuiConfigPersistence | ✅ Exists |
| Constructor | 143-156 | FieldCustomizerScreen | ✅ Same |
| init() | 157-180 | FieldCustomizerScreen | ❓ Simplified |
| initFullscreenMode() | 197-216 | FullscreenLayout + ComponentInit | ❓ |
| initWindowedMode() | 218-268 | WindowedLayout + ComponentInit | ❓ |
| initWindowedLeftPanel() | 270-398 | HeaderBar + TabBar + ShapePanel | ❓ |
| initWindowedRightPanel() | 400-498 | SelectorBar + ContentArea | ❓ |
| initTitleBar() | 504-536 | HeaderBar | ❓ |
| initMainTabs() | 538-613 | TabBar | ❓ |
| onPresetSelected() | 619-634 | TabBar callback | ✅ |
| initSelectors() | 636-684 | SelectorBar | ❓ |
| initSubTabs() | 686-735 | ContentArea | ❓ |
| initShapePanel() | 737-751 | FieldCustomizerScreen | ✅ |
| initStatusBar() | 753-765 | StatusBar | ✅ |
| initPreviewModeCheckbox() | 767-789 | FieldCustomizerScreen | ❓ |
| Content Providers | 791-859 | ContentProviderFactory | ❓ |
| toggleMode() | 865-872 | FieldCustomizerScreen | ✅ |
| resetState() | 874-882 | FieldCustomizerScreen | ✅ |
| switchTab() | 884-895 | TabBar/FieldCustomizerScreen | ❓ |
| updateMainTabButtons() | 897-902 | TabBar | ❓ MISSING |
| registerWidgets() | 908-969 | FieldCustomizerScreen | ❓ Different |
| refreshSubTabWidgets() | 974-977 | ContentArea | ❓ |
| refreshTabsForRendererMode() | 983-988 | TabBar + ContentArea | ❓ |
| showLayerModal() | 994-1041 | ModalFactory | ✅ Exists |
| showPrimitiveModal() | 1043-1091 | ModalFactory | ✅ Exists |
| focusModalTextField() | 1097-1116 | ModalFactory | ❓ |
| getLayerNames() | 1122-1128 | FieldCustomizerScreen | ✅ |
| getPrimitiveNames() | 1130-1133 | FieldCustomizerScreen | ✅ |
| refreshLayerSelector() | 1135-1141 | SelectorBar | ❓ |
| refreshPrimitiveSelector() | 1143-1149 | SelectorBar | ❓ |
| tick() | 1155-1167 | FieldCustomizerScreen + Components | ❓ |
| render() | 1169-1200 | FieldCustomizerScreen | ❓ Different |
| renderFullscreenMode() | 1202-1247 | FullscreenLayout + render | ❓ CRITICAL |
| renderWindowedMode() | 1249-1313 | WindowedLayout + render | ❓ |
| renderQuadrantBackgrounds() | 1315-1329 | FullscreenLayout | ❓ |
| renderPanelInBounds() | 1336-1342 | FieldCustomizerScreen | ✅ |
| render3DPreview() | 1348-1380 | FieldCustomizerScreen | ✅ |
| mouseScrolled() | 1386-1405 | FieldCustomizerScreen | ✅ |
| mouseClicked() | 1407-1437 | FieldCustomizerScreen | ✅ |
| keyPressed() | 1440-1491 | FieldCustomizerScreen | ✅ |
| charTyped() | 1493-1503 | FieldCustomizerScreen | ✅ |
| shouldPause() | 1505-1508 | LayoutManager | ✅ |
| close() | 1510-1523 | FieldCustomizerScreen | ✅ |
| PanelWrapper | 1532-1595 | ContentProviderFactory | ❓ |
| PlaceholderContent | 1600-1625 | Not needed | N/A |

---

## Critical Missing Features

### 1. Grid Field in Screen
**OLD (line 102):**
```java
private GridPane grid;
```

**NEW:** The new FieldCustomizerScreen does NOT have a `grid` field. The grid is inside FullscreenLayout but not exposed.

**Fix needed:** Either:
- Add `getGrid()` to LayoutManager interface
- Or replicate grid-dependent logic in the layout class

### 2. Direct Widget Creation
**OLD:** Creates widgets directly using `addDrawableChild(ButtonWidget.builder(...).build())`

**NEW:** Delegates to component classes which create widgets internally.

**Issue:** If components don't position widgets correctly, they overlap.

### 3. Fullscreen Rendering Structure
**OLD renderFullscreenMode() (lines 1202-1247):**
```java
// 1. Solid background fill
context.fill(0, 0, width, height, 0xFF0a0a0a);
// 2. Main panel frame
context.fill(MARGIN - 1, MARGIN - 1, ..., 0xFF333333);
// 3. Title bar fill  
context.fill(MARGIN, MARGIN, width - MARGIN, MARGIN + TITLE_HEIGHT, 0xFF2a2a2a);
// 4. Tab bar fill
context.fill(...);
// 5. Grid quadrant backgrounds (uses grid.topLeft(), grid.bottomLeft(), etc.)
renderQuadrantBackgrounds(context);
// 6. 3D Preview render
render3DPreview(context, grid.topLeft(), delta);
// 7. Selector render
layerSelector.render(...);
primitiveSelector.render(...);
// 8. Sub-tab render based on current tab
// 9. Shape panel render (with scissor)
// 10. Status bar render
```

**NEW render() delegates to layout.renderBackground() and layout.renderFrame()** but may be missing:
- Title text ("⬡ Field Customizer")
- Dirty indicator (●)
- Tab bar background
- Quadrant backgrounds
- Selector rendering
- SubTab rendering

### 4. updateMainTabButtons()
**OLD (lines 897-902):**
```java
private void updateMainTabButtons() {
    if (quickTabBtn != null) quickTabBtn.active = currentTab != TabType.QUICK;
    if (advancedTabBtn != null) advancedTabBtn.active = currentTab != TabType.ADVANCED;
    if (debugTabBtn != null) debugTabBtn.active = currentTab != TabType.DEBUG;
    if (profilesTabBtn != null) profilesTabBtn.active = currentTab != TabType.PROFILES;
}
```

**NEW TabBar:** Has `updateTabButtonStyles()` but it's EMPTY (lines 203-207 in TabBar.java):
```java
private void updateTabButtonStyles() {
    // In Minecraft, we typically adjust alpha or use a different approach
    // For now, we just ensure the active tab is visually distinct
    // This could be enhanced with custom rendering
}
```

**CRITICAL BUG:** Tabs are never deactivated visually! Active tab should have `.active = false`.

### 5. registerWidgets() Logic
**OLD (lines 908-969):** Detailed widget registration with specific order:
1. Clear children
2. Add fixed buttons (mode, field, reset, close, tabs)
3. Add preset dropdown and renderer toggle
4. Add preview checkbox
5. For non-Profiles: add selectors, sub-tabs by current tab, shape panel
6. For Profiles: add profiles panel
7. Add modal widgets if visible

**NEW:** Uses `addWidgetsFrom(component)` but may miss some widgets or add them in wrong order.

---

## Constants Comparison

| Constant | OLD Value | NEW (FullscreenLayout) | Match? |
|----------|----------|------------------------|--------|
| TITLE_HEIGHT | 20 | 24 | ❌ |
| TAB_BAR_HEIGHT | 22 | 28 | ❌ |
| STATUS_HEIGHT | 18 | 24 | ❌ |
| MARGIN | 8 | 12 | ❌ |
| SELECTOR_HEIGHT | 22 | 24 | ❌ |

> **CRITICAL:** All constants are different! This shifts every element's position.

---

## Implementation Priority

### Phase 1: Fix Layout Constants
1. Update FullscreenLayout constants to match OLD values
2. Update WindowedLayout constants to match OLD values

### Phase 2: Fix TabBar
1. Add `updateTabButtonStyles()` to set `.active = false` for current tab
2. Verify tab button positions match OLD code

### Phase 3: Fix HeaderBar  
1. Verify button positions match OLD code (lines 504-536)
2. Ensure title rendering happens in render()

### Phase 4: Fix Rendering
1. Add quadrant background rendering
2. Ensure selectors render
3. Ensure sub-tabs render based on current tab

### Phase 5: Fix Widget Registration
1. Match the exact registration order from OLD code
2. Ensure all widgets are registered

### Phase 6: Verify ContentArea
1. Ensure ContentProviderFactory creates all content providers
2. Verify bounds are passed correctly

---

## Files to Modify

1. `FullscreenLayout.java` - Constants, getGrid(), renderFrame()
2. `WindowedLayout.java` - Constants  
3. `HeaderBar.java` - Button positions
4. `TabBar.java` - updateTabButtonStyles() implementation
5. `SelectorBar.java` - Verify bounds
6. `ContentArea.java` - Verify bounds and rendering
7. `FieldCustomizerScreen.java` - Widget registration order

---

## Detailed Method Mapping

### OLD initFullscreenMode() → NEW
```
Lines 197-216:
1. Calculate contentBounds from MARGIN, TITLE_HEIGHT, TAB_BAR_HEIGHT, STATUS_HEIGHT ✓
2. Create GridPane.grid2x2(0.4f, 0.5f) and set bounds ← NEEDS grid field
3. initTitleBar() → HeaderBar.setBounds()
4. initMainTabs() → TabBar.setBounds()  
5. initSelectors() → SelectorBar.setBounds()
6. initSubTabs() → ContentArea.setBounds()
7. initShapePanel() ← Direct in screen
8. initStatusBar() → StatusBar.setBounds()
9. initPreviewModeCheckbox() ← Direct in screen
10. registerWidgets() ← Different in new
```

### OLD render() → NEW
```
Lines 1169-1200:
1. if fullscreen → renderFullscreenMode() else renderWindowedMode()
2. super.render() (widgets)
3. Dropdown overlay
4. Modal overlay with widget re-render
5. Preset confirm dialog
6. Toast notifications
```

The NEW version combines fullscreen/windowed into single path via layout, but may miss mode-specific logic.
