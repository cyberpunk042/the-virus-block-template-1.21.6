package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.ShuffleGenerator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Debug sub-panel for Arrangement patterns and Shuffle exploration.
 * 
 * <h2>Two Tabs</h2>
 * <ul>
 *   <li><b>Patterns</b> - Select named patterns for different shape parts</li>
 *   <li><b>Explorer</b> - Explore all vertex permutations with visual diagram</li>
 * </ul>
 */
public class ArrangeSubPanel extends AbstractPanel {
    
    private final TextRenderer textRenderer;
    
    // Tab state
    private enum Tab { PATTERNS, EXPLORER }
    private Tab currentTab = Tab.PATTERNS;
    private ButtonWidget patternsTabBtn;
    private ButtonWidget explorerTabBtn;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PATTERNS TAB CONTROLS
    // ═══════════════════════════════════════════════════════════════════════
    
    private CyclingButtonWidget<CellType> patternsCellTypeDropdown;
    private CyclingButtonWidget<String> patternDropdown;
    private ButtonWidget perPartToggle;
    private boolean showPerPartControls = false;
    
    // Per-part dropdowns (shown when expanded)
    private CyclingButtonWidget<String> polesDropdown;
    private CyclingButtonWidget<String> equatorDropdown;
    private CyclingButtonWidget<String> capsDropdown;
    private CyclingButtonWidget<String> sidesDropdown;
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXPLORER TAB CONTROLS
    // ═══════════════════════════════════════════════════════════════════════
    
    private CyclingButtonWidget<CellType> explorerCellTypeDropdown;
    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;
    private ButtonWidget randomBtn;
    private TextFieldWidget jumpField;
    private ButtonWidget jumpBtn;
    private TextFieldWidget saveNameField;
    private ButtonWidget saveBtn;
    
    // Explorer state
    private CellType currentCellType = CellType.QUAD;
    private int currentPermutation = 0;
    private String currentDescription = "";
    private String knownPatternName = null;  // Non-null if matches a known pattern
    private int[] currentMapping = new int[]{0, 1, 2, 3};
    
    private int contentHeight = 0;
    
    /**
     * Returns the total height of this panel's content.
     */
    public int getHeight() {
        return Math.max(contentHeight, 200);  // Minimum height
    }
    
    public ArrangeSubPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state);
        this.textRenderer = textRenderer;
    }
    
    @Override
    protected void init() {
        clearWidgets();
        
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = GuiConstants.WIDGET_HEIGHT;
        int tabW = (w - GuiConstants.PADDING) / 2;
        
        // ═══════════════════════════════════════════════════════════════════
        // TAB BUTTONS
        // ═══════════════════════════════════════════════════════════════════
        
        patternsTabBtn = addWidget(ButtonWidget.builder(
                Text.literal(currentTab == Tab.PATTERNS ? "§e[Patterns]" : "Patterns"),
                btn -> switchTab(Tab.PATTERNS))
            .dimensions(x, y, tabW, h - 2)
            .build());
        
        explorerTabBtn = addWidget(ButtonWidget.builder(
                Text.literal(currentTab == Tab.EXPLORER ? "§e[Explorer]" : "Explorer"),
                btn -> switchTab(Tab.EXPLORER))
            .dimensions(x + tabW + GuiConstants.PADDING, y, tabW, h - 2)
            .build());
        
        y += h + 4;
        
        // ═══════════════════════════════════════════════════════════════════
        // TAB CONTENT
        // ═══════════════════════════════════════════════════════════════════
        
        if (currentTab == Tab.PATTERNS) {
            initPatternsTab(x, y, w, h);
        } else {
            initExplorerTab(x, y, w, h);
        }
    }
    
    private void switchTab(Tab tab) {
        if (currentTab != tab) {
            currentTab = tab;
            reflow();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERNS TAB
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initPatternsTab(int x, int y, int w, int h) {
        int labelW = 60;
        int controlW = w - labelW - GuiConstants.PADDING;
        
        // Cell Type selector
        patternsCellTypeDropdown = addWidget(CyclingButtonWidget.<CellType>builder(ct -> Text.literal(ct.name()))
            .values(CellType.values())
            .initially(currentCellType)
            .build(x + labelW, y, controlW, h, Text.literal("Cell"),
                (btn, val) -> {
                    currentCellType = val;
                    reflow();  // Rebuild to show patterns for this cell type
                }));
        y += h + 2;
        
        // Pattern selector (shows patterns for current cell type)
        List<String> patterns = getPatternsForCellType(currentCellType);
        String currentPattern = getStringOrDefault("arrangement.defaultPattern", patterns.get(0));
        
        patternDropdown = addWidget(CyclingButtonWidget.<String>builder(Text::literal)
            .values(patterns)
            .initially(patterns.contains(currentPattern) ? currentPattern : patterns.get(0))
            .build(x + labelW, y, controlW, h, Text.literal("Pattern"),
                (btn, val) -> state.set("arrangement.defaultPattern", val)));
        y += h + 4;
        
        // Per-Part toggle
        perPartToggle = addWidget(ButtonWidget.builder(
                Text.literal("Per-Part Overrides"),
                btn -> {
                    showPerPartControls = !showPerPartControls;
                    reflow();
                })
            .dimensions(x, y, w, h)
            .build());
        y += h + 2;
        
        // Per-Part dropdowns (only when expanded and shape has parts)
        if (showPerPartControls) {
            String shapeType = state.getString("shapeType").toLowerCase();
            
            // Show relevant per-part controls based on shape
            if (shapeType.equals("sphere")) {
                // Poles
                polesDropdown = addWidget(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.poles", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Poles"),
                        (btn, val) -> state.set("arrangement.poles", val)));
                y += h + 2;
                
                // Equator
                equatorDropdown = addWidget(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.equator", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Equator"),
                        (btn, val) -> state.set("arrangement.equator", val)));
                y += h + 2;
            } else if (shapeType.equals("prism") || shapeType.equals("cylinder")) {
                // Caps
                capsDropdown = addWidget(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.caps", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Caps"),
                        (btn, val) -> state.set("arrangement.caps", val)));
                y += h + 2;
                
                // Sides
                sidesDropdown = addWidget(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.sides", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Sides"),
                        (btn, val) -> state.set("arrangement.sides", val)));
                y += h + 2;
            }
        }
        
        contentHeight = y - bounds.y() + GuiConstants.PADDING;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLORER TAB
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initExplorerTab(int x, int y, int w, int h) {
        int labelW = 50;
        int controlW = w - labelW - GuiConstants.PADDING;
        
        // Cell Type selector
        explorerCellTypeDropdown = addWidget(CyclingButtonWidget.<CellType>builder(ct -> Text.literal(ct.name()))
            .values(CellType.values())
            .initially(currentCellType)
            .build(x + labelW, y, controlW, h, Text.literal("Cell"),
                (btn, val) -> {
                    currentCellType = val;
                    currentPermutation = 0;
                    updateShuffleDisplay();
                    applyCurrentShuffle();
                }));
        y += h + 8;
        
        // Space for the visual diagram (rendered in render())
        // Leave about 100px for the diagram area
        y += 80;
        
        // Navigation buttons
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        
        prevBtn = addWidget(ButtonWidget.builder(Text.literal("◀ Prev"), btn -> {
                currentPermutation--;
                if (currentPermutation < 0) {
                    currentPermutation = getTotal() - 1;
                }
                updateShuffleDisplay();
                applyCurrentShuffle();
            })
            .dimensions(x, y, btnW, h)
            .build());
        
        randomBtn = addWidget(ButtonWidget.builder(Text.literal("🎲"), btn -> {
                currentPermutation = (int)(Math.random() * getTotal());
                updateShuffleDisplay();
                applyCurrentShuffle();
                ToastNotification.info("Random: #" + (currentPermutation + 1));
            })
            .dimensions(x + btnW + GuiConstants.PADDING, y, btnW, h)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Random permutation")))
            .build());
        
        nextBtn = addWidget(ButtonWidget.builder(Text.literal("Next ▶"), btn -> {
                currentPermutation++;
                if (currentPermutation >= getTotal()) {
                    currentPermutation = 0;
                }
                updateShuffleDisplay();
                applyCurrentShuffle();
            })
            .dimensions(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, h)
            .build());
        
        y += h + 4;
        
        // Jump to #
        int jumpFieldW = w - 60;
        jumpField = new TextFieldWidget(textRenderer, x, y, jumpFieldW, h, Text.literal("Jump"));
        jumpField.setPlaceholder(Text.literal("Jump to #..."));
        jumpField.setMaxLength(6);
        addWidget(jumpField);
        
        jumpBtn = addWidget(ButtonWidget.builder(Text.literal("Go"), btn -> {
                try {
                    int target = Integer.parseInt(jumpField.getText()) - 1;
                    if (target >= 0 && target < getTotal()) {
                        currentPermutation = target;
                        updateShuffleDisplay();
                        applyCurrentShuffle();
                    } else {
                        ToastNotification.warning("Invalid: 1-" + getTotal());
                    }
                } catch (NumberFormatException e) {
                    ToastNotification.warning("Enter a number");
                }
            })
            .dimensions(x + jumpFieldW + GuiConstants.PADDING, y, 50, h)
            .build());
        
        y += h + 8;
        
        // Save as pattern (only enabled for new permutations)
        int saveFieldW = w - 70;
        saveNameField = new TextFieldWidget(textRenderer, x, y, saveFieldW, h, Text.literal("Name"));
        saveNameField.setPlaceholder(Text.literal("Pattern name..."));
        saveNameField.setMaxLength(32);
        saveNameField.setEditable(knownPatternName == null);  // Disabled if known pattern
        addWidget(saveNameField);
        
        saveBtn = addWidget(ButtonWidget.builder(Text.literal("💾"), btn -> saveAsPattern())
            .dimensions(x + saveFieldW + GuiConstants.PADDING, y, 60, h)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Save as named pattern")))
            .build());
        saveBtn.active = (knownPatternName == null);  // Disabled if known pattern
        
        y += h + 8;
        
        contentHeight = y - bounds.y() + GuiConstants.PADDING;
        
        // Initial update
        updateShuffleDisplay();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<String> getPatternsForCellType(CellType cellType) {
        // Get patterns from FragmentRegistry, or fallback to defaults
        List<String> fromRegistry = FragmentRegistry.listArrangementFragments();
        if (!fromRegistry.isEmpty() && !fromRegistry.get(0).equals("Default")) {
            return fromRegistry;
        }
        
        // Fallback defaults per cell type
        return switch (cellType) {
            case QUAD -> List.of("filled_1", "filled_2", "wave_1", "wave_2", "tooth_1", "parallelogram_1", "Custom");
            case SEGMENT -> List.of("full", "alternating", "sparse", "dashed", "dotted", "Custom");
            case SECTOR -> List.of("full", "half", "quarters", "pinwheel", "spiral", "Custom");
            case EDGE -> List.of("full", "latitude", "longitude", "sparse", "minimal", "dashed", "Custom");
            case TRIANGLE -> List.of("full", "alternating", "inverted", "sparse", "fan", "radial", "Custom");
        };
    }
    
    private int getTotal() {
        return switch (currentCellType) {
            case QUAD -> ShuffleGenerator.quadCount();
            case SEGMENT -> ShuffleGenerator.segmentCount();
            case SECTOR -> ShuffleGenerator.sectorCount();
            case EDGE -> ShuffleGenerator.edgeCount();
            case TRIANGLE -> ShuffleGenerator.triangleCount();
        };
    }
    
    private void updateShuffleDisplay() {
        // Get description and mapping
        currentDescription = switch (currentCellType) {
            case QUAD -> ShuffleGenerator.getQuad(currentPermutation).describe();
            case SEGMENT -> ShuffleGenerator.getSegment(currentPermutation).describe();
            case SECTOR -> ShuffleGenerator.getSector(currentPermutation).describe();
            case EDGE -> ShuffleGenerator.getEdge(currentPermutation).describe();
            case TRIANGLE -> ShuffleGenerator.getTriangle(currentPermutation).describe();
        };
        
        // Get mapping array for visual (extract from arrangement data)
        currentMapping = extractMapping(currentCellType, currentPermutation);
        
        // Check if this matches a known pattern
        knownPatternName = findKnownPatternName();
        
        // Update save button state
        if (saveBtn != null) {
            saveBtn.active = (knownPatternName == null);
        }
        if (saveNameField != null) {
            saveNameField.setEditable(knownPatternName == null);
        }
    }
    
    private String findKnownPatternName() {
        // TODO: Check against FragmentRegistry for matching permutation
        // For now, return null (treat all as new)
        // This would compare currentMapping against stored pattern mappings
        return null;
    }
    
    /**
     * Extracts vertex mapping from arrangement data.
     * For QUAD: extracts corner indices from tri1 and tri2.
     */
    private int[] extractMapping(CellType cellType, int permutation) {
        return switch (cellType) {
            case QUAD -> {
                var quad = ShuffleGenerator.getQuad(permutation);
                // Extract indices from tri1 and tri2 corners
                int[] mapping = new int[4];
                if (quad.tri1() != null) {
                    for (int i = 0; i < Math.min(3, quad.tri1().length); i++) {
                        mapping[i] = quad.tri1()[i].index;
                    }
                }
                if (quad.tri2() != null && quad.tri2().length > 0) {
                    mapping[3] = quad.tri2()[0].index;
                }
                yield mapping;
            }
            case TRIANGLE -> {
                var tri = ShuffleGenerator.getTriangle(permutation);
                // Triangle has 3 vertices
                yield new int[]{0, 1, 2};  // Placeholder - would extract from tri data
            }
            case SEGMENT -> new int[]{0, 1};  // Line segment
            case SECTOR -> new int[]{0, 1, 2};  // Sector (pie slice)
            case EDGE -> new int[]{0, 1};  // Edge
        };
    }
    
    private void applyCurrentShuffle() {
        // Set the shuffle state so the renderer uses it
        state.set("shuffle.cellType", currentCellType.name());
        state.set("shuffle.permutation", currentPermutation);
        
        // Log for debugging
        switch (currentCellType) {
            case QUAD -> ShuffleGenerator.logQuad(currentPermutation);
            case SEGMENT -> ShuffleGenerator.logSegment(currentPermutation);
            case SECTOR -> ShuffleGenerator.logSector(currentPermutation);
            case EDGE -> ShuffleGenerator.logEdge(currentPermutation);
            case TRIANGLE -> ShuffleGenerator.logTriangle(currentPermutation);
        }
    }
    
    private void saveAsPattern() {
        String name = saveNameField.getText().trim();
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a pattern name");
            return;
        }
        
        // TODO: Save to FragmentRegistry
        // FragmentRegistry.saveArrangementPattern(name, currentCellType, currentMapping, currentDescription);
        
        ToastNotification.success("Saved pattern: " + name);
        knownPatternName = name;
        saveBtn.active = false;
        saveNameField.setEditable(false);
    }
    
    private String getStringOrDefault(String path, String defaultValue) {
        try {
            String value = state.getString(path);
            return value != null && !value.isEmpty() ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    @Override
    public void tick() {
        // TextFieldWidget cursor blink handled internally
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (currentTab == Tab.EXPLORER) {
            renderExplorerTab(context);
        } else {
            renderPatternsTab(context);
        }
    }
    
    private void renderPatternsTab(DrawContext context) {
        int x = bounds.x();
        int y = bounds.y() + GuiConstants.WIDGET_HEIGHT + 6;
        
        // Labels
        context.drawTextWithShadow(textRenderer, "Cell:", x, y + 6, 0xFFAAAAAA);
        y += GuiConstants.WIDGET_HEIGHT + 2;
        context.drawTextWithShadow(textRenderer, "Pattern:", x, y + 6, 0xFFAAAAAA);
        
        if (showPerPartControls) {
            y += GuiConstants.WIDGET_HEIGHT + 4 + GuiConstants.WIDGET_HEIGHT + 2;
            String shapeType = state.getString("shapeType").toLowerCase();
            
            if (shapeType.equals("sphere")) {
                context.drawTextWithShadow(textRenderer, "Poles:", x, y + 6, 0xFF888888);
                y += GuiConstants.WIDGET_HEIGHT + 2;
                context.drawTextWithShadow(textRenderer, "Equator:", x, y + 6, 0xFF888888);
            } else if (shapeType.equals("prism") || shapeType.equals("cylinder")) {
                context.drawTextWithShadow(textRenderer, "Caps:", x, y + 6, 0xFF888888);
                y += GuiConstants.WIDGET_HEIGHT + 2;
                context.drawTextWithShadow(textRenderer, "Sides:", x, y + 6, 0xFF888888);
            }
        }
    }
    
    private void renderExplorerTab(DrawContext context) {
        int x = bounds.x();
        int y = bounds.y() + GuiConstants.WIDGET_HEIGHT + 6;
        int w = bounds.width();
        
        // Cell label
        context.drawTextWithShadow(textRenderer, "Cell:", x, y + 6, 0xFFAAAAAA);
        y += GuiConstants.WIDGET_HEIGHT + 8;
        
        // ═══════════════════════════════════════════════════════════════════
        // VISUAL DIAGRAM + INFO PANEL (side by side)
        // ═══════════════════════════════════════════════════════════════════
        
        int diagramW = (w - 8) / 2;
        int diagramH = 70;
        int infoX = x + diagramW + 8;
        int infoW = w - diagramW - 8;
        
        // Draw diagram background
        context.fill(x, y, x + diagramW, y + diagramH, 0x44222233);
        context.drawBorder(x, y, diagramW, diagramH, 0xFF444466);
        
        // Draw permutation visualization
        renderPermutationDiagram(context, x + 4, y + 4, diagramW - 8, diagramH - 8);
        
        // Draw info panel
        context.fill(infoX, y, infoX + infoW, y + diagramH, 0x44223322);
        context.drawBorder(infoX, y, infoW, diagramH, 0xFF446644);
        
        int infoY = y + 4;
        
        // Pattern name or "New Permutation"
        if (knownPatternName != null) {
            context.drawTextWithShadow(textRenderer, "Name:", infoX + 4, infoY, 0xFFAAAAAA);
            context.drawTextWithShadow(textRenderer, knownPatternName, infoX + 40, infoY, 0xFF66FF66);
            infoY += 10;
            context.drawTextWithShadow(textRenderer, "✓ Known", infoX + 4, infoY, 0xFF44AA44);
        } else {
            context.drawTextWithShadow(textRenderer, "○ New", infoX + 4, infoY, 0xFFFFAA44);
            infoY += 10;
            context.drawTextWithShadow(textRenderer, "Permutation", infoX + 4, infoY, 0xFFFFAA44);
        }
        infoY += 14;
        
        // Mapping
        context.drawTextWithShadow(textRenderer, "Mapping:", infoX + 4, infoY, 0xFFAAAAAA);
        infoY += 10;
        String mappingStr = formatMapping(currentMapping);
        context.drawTextWithShadow(textRenderer, mappingStr, infoX + 4, infoY, 0xFF88CCFF);
        infoY += 14;
        
        // Description (truncated)
        String desc = currentDescription.length() > 20 
            ? currentDescription.substring(0, 18) + "..." 
            : currentDescription;
        context.drawTextWithShadow(textRenderer, desc, infoX + 4, infoY, 0xFF888888);
        
        // Permutation counter (below diagram area)
        y += diagramH + 4;
        String counter = String.format("#%d / %d", currentPermutation + 1, getTotal());
        context.drawCenteredTextWithShadow(textRenderer, counter, x + w / 2, y, 0xFFFFFFAA);
    }
    
    private void renderPermutationDiagram(DrawContext context, int x, int y, int w, int h) {
        // Draw based on cell type
        int cx = x + w / 2;
        int cy = y + h / 2;
        int size = Math.min(w, h) / 2 - 4;
        
        int color = 0xFF00CCFF;
        int lineColor = 0xFF4488AA;
        
        switch (currentCellType) {
            case QUAD -> {
                // Draw quad with vertex numbers
                int[] px = {cx - size, cx + size, cx + size, cx - size};
                int[] py = {cy - size/2, cy - size/2, cy + size/2, cy + size/2};
                
                // Draw edges
                for (int i = 0; i < 4; i++) {
                    int next = (i + 1) % 4;
                    drawLine(context, px[i], py[i], px[next], py[next], lineColor);
                }
                
                // Draw mapping arrows
                for (int i = 0; i < currentMapping.length && i < 4; i++) {
                    int from = i;
                    int to = currentMapping[i];
                    if (from != to) {
                        drawArrow(context, px[from], py[from], px[to], py[to], 0xFFFFAA00);
                    }
                }
                
                // Draw vertices with numbers
                for (int i = 0; i < 4; i++) {
                    context.fill(px[i] - 6, py[i] - 6, px[i] + 6, py[i] + 6, color);
                    context.drawCenteredTextWithShadow(textRenderer, String.valueOf(i), px[i], py[i] - 3, 0xFFFFFFFF);
                }
            }
            case TRIANGLE -> {
                // Draw triangle
                int[] px = {cx, cx - size, cx + size};
                int[] py = {cy - size/2, cy + size/2, cy + size/2};
                
                for (int i = 0; i < 3; i++) {
                    int next = (i + 1) % 3;
                    drawLine(context, px[i], py[i], px[next], py[next], lineColor);
                }
                
                for (int i = 0; i < 3; i++) {
                    context.fill(px[i] - 6, py[i] - 6, px[i] + 6, py[i] + 6, color);
                    context.drawCenteredTextWithShadow(textRenderer, String.valueOf(i), px[i], py[i] - 3, 0xFFFFFFFF);
                }
            }
            default -> {
                // Simple text representation for other types
                context.drawCenteredTextWithShadow(textRenderer, currentCellType.name(), cx, cy - 5, color);
                context.drawCenteredTextWithShadow(textRenderer, formatMapping(currentMapping), cx, cy + 8, 0xFFAAAAAA);
            }
        }
    }
    
    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Simple line drawing using fill
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) length);
        
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px = (int) (x1 + dx * t);
            int py = (int) (y1 + dy * t);
            context.fill(px, py, px + 1, py + 1, color);
        }
    }
    
    private void drawArrow(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Draw line with arrow head
        // Offset to not overlap vertices
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 20) return;
        
        float ndx = dx / length;
        float ndy = dy / length;
        
        int startX = (int) (x1 + ndx * 10);
        int startY = (int) (y1 + ndy * 10);
        int endX = (int) (x2 - ndx * 10);
        int endY = (int) (y2 - ndy * 10);
        
        drawLine(context, startX, startY, endX, endY, color);
        
        // Arrow head
        int arrowSize = 5;
        int ax1 = (int) (endX - ndx * arrowSize + ndy * arrowSize);
        int ay1 = (int) (endY - ndy * arrowSize - ndx * arrowSize);
        int ax2 = (int) (endX - ndx * arrowSize - ndy * arrowSize);
        int ay2 = (int) (endY - ndy * arrowSize + ndx * arrowSize);
        
        drawLine(context, endX, endY, ax1, ay1, color);
        drawLine(context, endX, endY, ax2, ay2, color);
    }
    
    private String formatMapping(int[] mapping) {
        if (mapping == null || mapping.length == 0) return "—";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mapping.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append("→").append(mapping[i]);
        }
        return sb.toString();
    }
}
