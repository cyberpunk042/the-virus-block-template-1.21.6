package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent API for building panel content with automatic bidirectional binding.
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * ContentBuilder content = new ContentBuilder(state, bindings, widgets, startY, panelWidth);
 * 
 * content.slider("Radius", "sphere.radius").range(0.1f, 10f).add();
 * content.slider("Phase", "link.phaseOffset").range(0, 360).degrees().add();
 * 
 * content.row()
 *     .toggle("Follow", "link.follow")
 *     .toggle("Scale", "link.scaleWith")
 *     .end();
 * 
 * contentHeight = content.getContentHeight();
 * </pre>
 * 
 * <p><b>All widgets are automatically:</b></p>
 * <ul>
 *   <li>Positioned based on current Y position</li>
 *   <li>Bound bidirectionally to state (user changes update state, state changes update display)</li>
 *   <li>Added to the panel's widget and binding lists</li>
 * </ul>
 */
public class ContentBuilder {
    
    private static final int COMPACT_HEIGHT = 16;
    private static final int WIDGET_GAP = 2;
    private static final int SECTION_GAP = 4;
    
    private final FieldEditState state;
    private final List<Bound<?, ?, ?>> bindings;
    private final List<ClickableWidget> widgets;
    
    private final int startY;
    private final int panelWidth;
    private int currentY;
    
    public ContentBuilder(FieldEditState state, 
                         List<Bound<?, ?, ?>> bindings,
                         List<ClickableWidget> widgets,
                         int startY, 
                         int panelWidth) {
        this.state = state;
        this.bindings = bindings;
        this.widgets = widgets;
        this.startY = startY;
        this.panelWidth = panelWidth;
        this.currentY = startY + WIDGET_GAP;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int leftPadding() { 
        return GuiConstants.PADDING; 
    }
    
    private int availableWidth() { 
        return panelWidth - GuiConstants.PADDING * 2; 
    }
    
    /** Adds vertical spacing between sections. */
    public ContentBuilder gap() {
        currentY += SECTION_GAP;
        return this;
    }
    
    /** Advances Y by one full row height. Use when adding manual widgets outside ContentBuilder. */
    public ContentBuilder advanceRow() {
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /** Advances Y by a custom amount. Use for manual widgets with non-standard heights. */
    public ContentBuilder advanceBy(int amount) {
        currentY += amount;
        return this;
    }
    
    /** Gets current Y position for manual widget placement. */
    public int getCurrentY() {
        return currentY;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // STATIC TEXT (labels, headers, descriptions)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // For now, these just add to a list that the panel can render in its render method.
    // The actual rendering happens in the panel's render() via renderLabels().
    
    private record LabelEntry(int x, int y, String text, int color, boolean small) {}
    private final List<LabelEntry> labels = new ArrayList<>();
    
    /**
     * Adds a section header with separator styling.
     * Example: "── Ray Motion ──"
     * 
     * @param text The header text
     * @return this builder for chaining
     */
    public ContentBuilder sectionHeader(String text) {
        labels.add(new LabelEntry(GuiConstants.PADDING, currentY + 2, "§f── " + text + " ──", 0xFFFFFF, false));
        currentY += 12; // Headers are slightly taller
        return this;
    }
    
    /**
     * Adds a small info text line (for descriptions, hints).
     * Uses gray color and smaller spacing.
     * 
     * @param text The info text
     * @return this builder for chaining
     */
    public ContentBuilder infoText(String text) {
        labels.add(new LabelEntry(GuiConstants.PADDING, currentY, "§7" + text, 0x888888, true));
        currentY += 10; // Small text uses less space
        return this;
    }
    
    /**
     * Adds a label with custom color.
     * 
     * @param text The label text
     * @param color ARGB color
     * @return this builder for chaining
     */
    public ContentBuilder label(String text, int color) {
        labels.add(new LabelEntry(GuiConstants.PADDING, currentY, text, color, false));
        currentY += 12;
        return this;
    }
    
    /**
     * Gets the list of labels to render. Call from panel's render() method.
     */
    public List<LabelEntry> getLabels() {
        return labels;
    }
    
    /**
     * Offsets all label positions by dx, dy.
     * Call this when widgets are offset (e.g., in applyBoundsOffset).
     */
    public void offsetLabels(int dx, int dy) {
        for (int i = 0; i < labels.size(); i++) {
            LabelEntry old = labels.get(i);
            labels.set(i, new LabelEntry(old.x + dx, old.y + dy, old.text, old.color, old.small));
        }
    }
    
    /**
     * Renders all labels. Call this from the panel's render() method.
     * 
     * @param context The draw context
     * @param scrollOffset Current scroll offset
     * @param boundsX The panel's x position in screen coordinates
     * @param boundsY The panel's y position in screen coordinates
     */
    public void renderLabels(net.minecraft.client.gui.DrawContext context, int scrollOffset, int boundsX, int boundsY) {
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        // Debug: log first label details
        if (!labels.isEmpty()) {
            LabelEntry first = labels.get(0);
            // Labels are now pre-offset like widgets, so use coords directly (just apply scroll)
            int firstRenderX = first.x;
            int firstRenderY = first.y - scrollOffset;
            net.cyberpunk042.log.Logging.GUI.topic("labels").info(
                "First label '{}' at screen ({}, {}), entry.y={}, color=0x{}", 
                first.text.substring(0, Math.min(20, first.text.length())),
                firstRenderX, firstRenderY, first.y, Integer.toHexString(first.color));
        }
        
        for (LabelEntry entry : labels) {
            // Labels are already at absolute screen coordinates (offset applied via offsetLabels)
            // Just apply scroll offset for Y
            int renderX = entry.x;
            int renderY = entry.y - scrollOffset;
            context.drawTextWithShadow(textRenderer, entry.text, renderX, renderY, entry.color);
        }
    }
    
    /** Gets the total content height (for panel sizing). */
    public int getContentHeight() {
        return currentY - startY + GuiConstants.PADDING;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FULL-WIDTH WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Creates a full-width slider bound to a state path. */
    public SliderBuilder slider(String label, String statePath) {
        return new SliderBuilder(label, statePath, leftPadding(), currentY, availableWidth(), true);
    }
    
    /** Creates a full-width toggle bound to a state path. */
    public ContentBuilder toggle(String label, String statePath) {
        createToggleWidget(label, statePath, leftPadding(), currentY, availableWidth());
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /** Creates a full-width dropdown bound to a state path. */
    public <E extends Enum<E>> ContentBuilder dropdown(String label, String statePath, Class<E> enumClass) {
        createDropdownWidget(label, statePath, enumClass, leftPadding(), currentY, availableWidth());
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /**
     * Creates an icon toggle button (e.g., "↻" for inheritRotation, "R" for scaleWithRadius).
     * Displays colored icon: active=green (§a), inactive=gray (§7).
     * 
     * @param icon The icon character(s) to display
     * @param statePath State path for the boolean value
     */
    public ContentBuilder iconToggle(String icon, String statePath) {
        createIconToggleWidget(icon, statePath, leftPadding(), currentY, 20);
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /**
     * Conditional content - adds widgets only when condition is true.
     * The condition is evaluated at build time.
     * 
     * <p>Example:</p>
     * <pre>
     * content.when(mode.needsSecondaryParams(), secondary -> {
     *     secondary.sliderPair("Amp2", path + ".amplitude2", 0, 10,
     *                        "Freq2", path + ".frequency2", -20, 20);
     * });
     * </pre>
     * 
     * @param condition Evaluated at build time
     * @param builder Consumer that adds widgets if condition is true
     */
    public ContentBuilder when(boolean condition, java.util.function.Consumer<ContentBuilder> builder) {
        if (condition) {
            builder.accept(this);
        }
        return this;
    }
    
    /**
     * Conditional content with supplier - for conditions that read from state.
     */
    public ContentBuilder when(java.util.function.BooleanSupplier condition, 
                               java.util.function.Consumer<ContentBuilder> builder) {
        if (condition.getAsBoolean()) {
            builder.accept(this);
        }
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PAIRED WIDGETS (side by side)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Creates a row of 2 sliders with equal width. */
    public ContentBuilder sliderPair(String label1, String path1, float min1, float max1,
                                     String label2, String path2, float min2, float max2) {
        int halfWidth = (availableWidth() - WIDGET_GAP) / 2;
        int secondX = leftPadding() + halfWidth + WIDGET_GAP;
        
        createSliderWidget(label1, path1, leftPadding(), currentY, halfWidth, min1, max1, "%.2f", v -> v, v -> v);
        createSliderWidget(label2, path2, secondX, currentY, halfWidth, min2, max2, "%.2f", v -> v, v -> v);
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /** Creates a row of 2 degree sliders (0-360° display, 0-1 state). */
    public ContentBuilder sliderPairDegrees(String label1, String path1, String label2, String path2) {
        int halfWidth = (availableWidth() - WIDGET_GAP) / 2;
        int secondX = leftPadding() + halfWidth + WIDGET_GAP;
        
        Function<Float, Float> toDisplay = value -> value * 360f;
        Function<Float, Float> toState = value -> value / 360f;
        
        createSliderWidget(label1, path1, leftPadding(), currentY, halfWidth, 0, 360, "%.0f", toDisplay, toState);
        createSliderWidget(label2, path2, secondX, currentY, halfWidth, 0, 360, "%.0f", toDisplay, toState);
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /** Creates a row of 2 enum dropdowns with equal width. */
    public <E1 extends Enum<E1>, E2 extends Enum<E2>> ContentBuilder dropdownPair(
            String label1, String path1, Class<E1> enum1,
            String label2, String path2, Class<E2> enum2) {
        int halfWidth = (availableWidth() - WIDGET_GAP) / 2;
        int secondX = leftPadding() + halfWidth + WIDGET_GAP;
        
        createDropdownWidget(label1, path1, enum1, leftPadding(), currentY, halfWidth);
        createDropdownWidget(label2, path2, enum2, secondX, currentY, halfWidth);
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /** Creates a row of 2 toggles with equal width. */
    public ContentBuilder togglePair(String label1, String path1, String label2, String path2) {
        int halfWidth = (availableWidth() - WIDGET_GAP) / 2;
        int secondX = leftPadding() + halfWidth + WIDGET_GAP;
        
        createToggleWidget(label1, path1, leftPadding(), currentY, halfWidth);
        createToggleWidget(label2, path2, secondX, currentY, halfWidth);
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VECTOR3F TRIPLET SLIDERS (X, Y, Z components)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a row of 3 sliders bound to a Vector3f path (e.g., "transform.offset").
     * Each slider updates one component while preserving others.
     * 
     * @param labelX Label for X slider (e.g., "X")
     * @param labelY Label for Y slider (e.g., "Y") 
     * @param labelZ Label for Z slider (e.g., "Z")
     * @param basePath State path to Vector3f (e.g., "transform.offset")
     * @param min Minimum value for all sliders
     * @param max Maximum value for all sliders
     */
    public ContentBuilder vec3Row(String labelX, String labelY, String labelZ,
                                  String basePath, float min, float max) {
        return vec3Row(labelX, labelY, labelZ, basePath, min, max, "%.1f");
    }
    
    /**
     * Creates a row of 3 sliders bound to a Vector3f path with custom format.
     */
    public ContentBuilder vec3Row(String labelX, String labelY, String labelZ,
                                  String basePath, float min, float max, String format) {
        int thirdWidth = (availableWidth() - WIDGET_GAP * 2) / 3;
        int x2 = leftPadding() + thirdWidth + WIDGET_GAP;
        int x3 = leftPadding() + (thirdWidth + WIDGET_GAP) * 2;
        
        createVec3ComponentSlider(labelX, basePath, 0, leftPadding(), currentY, thirdWidth, min, max, format);
        createVec3ComponentSlider(labelY, basePath, 1, x2, currentY, thirdWidth, min, max, format);
        createVec3ComponentSlider(labelZ, basePath, 2, x3, currentY, thirdWidth, min, max, format);
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /**
     * Creates a row of 3 rotation sliders with degree display.
     */
    public ContentBuilder rotationRow(String labelX, String labelY, String labelZ, String basePath) {
        int thirdWidth = (availableWidth() - WIDGET_GAP * 2) / 3;
        int x2 = leftPadding() + thirdWidth + WIDGET_GAP;
        int x3 = leftPadding() + (thirdWidth + WIDGET_GAP) * 2;
        
        createVec3ComponentSlider(labelX, basePath, 0, leftPadding(), currentY, thirdWidth, -180, 180, "%.0f°");
        createVec3ComponentSlider(labelY, basePath, 1, x2, currentY, thirdWidth, -180, 180, "%.0f°");
        createVec3ComponentSlider(labelZ, basePath, 2, x3, currentY, thirdWidth, -180, 180, "%.0f°");
        
        currentY += COMPACT_HEIGHT + WIDGET_GAP;
        return this;
    }
    
    /**
     * Creates a single slider for one component of a Vector3f.
     * Updates only that component while preserving others.
     */
    private void createVec3ComponentSlider(String label, String basePath, int componentIndex,
            int x, int y, int width, float min, float max, String format) {
        
        // Getter: read the component from the vector
        Supplier<Float> getter = () -> {
            org.joml.Vector3f vec = state.getVec3f(basePath);
            if (vec == null) return 0f;
            return switch (componentIndex) {
                case 0 -> vec.x;
                case 1 -> vec.y;
                case 2 -> vec.z;
                default -> 0f;
            };
        };
        
        // Setter: update only this component, preserve others
        Consumer<Float> setter = value -> {
            org.joml.Vector3f current = state.getVec3f(basePath);
            org.joml.Vector3f updated = current != null 
                ? new org.joml.Vector3f(current) 
                : new org.joml.Vector3f();
            switch (componentIndex) {
                case 0 -> updated.x = value;
                case 1 -> updated.y = value;
                case 2 -> updated.z = value;
            }
            state.set(basePath, updated);
        };
        
        float initialValue = getter.get();
        
        LabeledSlider slider = LabeledSlider.builder(label)
            .position(x, y)
            .width(width)
            .range(min, max)
            .initial(initialValue)
            .format(format)
            .onChange(setter)
            .build();
        
        Bound<LabeledSlider, Float, Float> binding = new Bound<>(
            slider, getter, setter, v -> v, v -> v, slider::setValue
        );
        
        widgets.add(slider);
        bindings.add(binding);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ROW BUILDER (multiple widgets side by side)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Starts a row of widgets. Use .toggle(), .slider() on the row, then .end() to finalize. */
    public RowBuilder row() {
        return new RowBuilder();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET CREATION (internal helpers)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void createToggleWidget(String label, String statePath, int x, int y, int width) {
        Supplier<Boolean> getter = () -> state.getBool(statePath);
        Consumer<Boolean> setter = value -> state.set(statePath, value);
        
        CyclingButtonWidget<Boolean> toggleWidget = CyclingButtonWidget.<Boolean>builder(
                value -> Text.literal(value ? "§a✓" + label : "§7" + label))
            .values(true, false)
            .initially(getter.get())
            .omitKeyText()
            .build(x, y, width, COMPACT_HEIGHT, Text.literal(""),
                (button, value) -> setter.accept(value));
        
        Bound<CyclingButtonWidget<Boolean>, Boolean, Boolean> binding = 
            Bound.toggle(toggleWidget, getter, setter);
        
        widgets.add(toggleWidget);
        bindings.add(binding);
    }
    
    private <E extends Enum<E>> void createDropdownWidget(String label, String statePath, 
            Class<E> enumClass, int x, int y, int width) {
        E[] values = enumClass.getEnumConstants();
        
        Supplier<E> getter = () -> {
            String name = state.getString(statePath);
            if (name == null) return values[0];
            try { return Enum.valueOf(enumClass, name); }
            catch (Exception e) { return values[0]; }
        };
        Consumer<E> setter = value -> state.set(statePath, value.name());
        
        CyclingButtonWidget<E> dropdownWidget = CyclingButtonWidget.<E>builder(
                value -> Text.literal(label + ": " + value.name()))
            .values(values)
            .initially(getter.get())
            .omitKeyText()
            .build(x, y, width, COMPACT_HEIGHT, Text.literal(""),
                (button, value) -> setter.accept(value));
        
        Bound<CyclingButtonWidget<E>, E, E> binding = Bound.dropdown(dropdownWidget, getter, setter);
        
        widgets.add(dropdownWidget);
        bindings.add(binding);
    }
    
    private void createSliderWidget(String label, String statePath, int x, int y, int width,
            float min, float max, String format,
            Function<Float, Float> toDisplay, Function<Float, Float> toState) {
        
        Supplier<Float> stateGetter = () -> state.getFloat(statePath);
        Consumer<Float> stateSetter = value -> state.set(statePath, value);
        
        float displayValue = toDisplay.apply(stateGetter.get());
        
        LabeledSlider sliderWidget = LabeledSlider.builder(label)
            .position(x, y)
            .width(width)
            .range(min, max)
            .initial(displayValue)
            .format(format)
            .onChange(displayVal -> stateSetter.accept(toState.apply(displayVal)))
            .build();
        
        Bound<LabeledSlider, Float, Float> binding = new Bound<>(
            sliderWidget,
            stateGetter,
            stateSetter,
            toDisplay,
            toState,
            sliderWidget::setValue
        );
        
        widgets.add(sliderWidget);
        bindings.add(binding);
    }
    
    /**
     * Creates an icon toggle button with colored icon (§a for active, §7 for inactive).
     */
    private void createIconToggleWidget(String icon, String statePath, int x, int y, int width) {
        Supplier<Boolean> getter = () -> state.getBool(statePath);
        Consumer<Boolean> setter = value -> state.set(statePath, value);
        
        CyclingButtonWidget<Boolean> iconToggle = CyclingButtonWidget.<Boolean>builder(
                value -> Text.literal(value ? "§a" + icon : "§7" + icon))
            .values(true, false)
            .initially(getter.get())
            .omitKeyText()
            .build(x, y, width, COMPACT_HEIGHT, Text.literal(""),
                (button, value) -> setter.accept(value));
        
        Bound<CyclingButtonWidget<Boolean>, Boolean, Boolean> binding = 
            Bound.toggle(iconToggle, getter, setter);
        
        widgets.add(iconToggle);
        bindings.add(binding);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLIDER BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public class SliderBuilder {
        private final String label;
        private final String statePath;
        private final int x, y, width;
        private final boolean advancesY;
        
        private float min = 0, max = 1;
        private String format = "%.2f";
        private Function<Float, Float> toDisplay = value -> value;
        private Function<Float, Float> toState = value -> value;
        
        SliderBuilder(String label, String statePath, int x, int y, int width, boolean advancesY) {
            this.label = label;
            this.statePath = statePath;
            this.x = x;
            this.y = y;
            this.width = width;
            this.advancesY = advancesY;
        }
        
        public SliderBuilder range(float min, float max) {
            this.min = min;
            this.max = max;
            return this;
        }
        
        public SliderBuilder format(String formatString) {
            this.format = formatString;
            return this;
        }
        
        /** State stores 0-1, display shows 0-360°. */
        public SliderBuilder degrees() {
            this.toDisplay = value -> value * 360f;
            this.toState = value -> value / 360f;
            return this;
        }
        
        /** Custom display transform. */
        public SliderBuilder transform(Function<Float, Float> toDisplay, Function<Float, Float> toState) {
            this.toDisplay = toDisplay;
            this.toState = toState;
            return this;
        }
        
        /** Finalizes and adds the slider to the panel. */
        public ContentBuilder add() {
            createSliderWidget(label, statePath, x, y, width, min, max, format, toDisplay, toState);
            if (advancesY) {
                currentY += COMPACT_HEIGHT + WIDGET_GAP;
            }
            return ContentBuilder.this;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ROW BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public class RowBuilder {
        private final List<RowItem> items = new ArrayList<>();
        
        /** Adds a toggle to this row. */
        public RowBuilder toggle(String label, String statePath) {
            items.add(new RowItem(RowItemType.TOGGLE, label, statePath, 0, 0, null, 0));
            return this;
        }
        
        /** Adds an icon toggle to this row. */
        public RowBuilder iconToggle(String icon, String statePath) {
            items.add(new RowItem(RowItemType.ICON_TOGGLE, icon, statePath, 0, 0, null, 20));
            return this;
        }
        
        /** Adds a slider to this row. */
        public RowBuilder slider(String label, String statePath, float min, float max) {
            items.add(new RowItem(RowItemType.SLIDER, label, statePath, min, max, null, 0));
            return this;
        }
        
        /** Adds a degree slider to this row. */
        public RowBuilder sliderDegrees(String label, String statePath) {
            items.add(new RowItem(RowItemType.SLIDER_DEGREES, label, statePath, 0, 360, "%.0f", 0));
            return this;
        }
        
        /** Finalizes the row and positions all widgets. */
        public ContentBuilder end() {
            int itemCount = items.size();
            
            // Calculate fixed width items (icon toggles) vs flexible items
            int fixedWidth = 0;
            int flexibleCount = 0;
            for (RowItem item : items) {
                if (item.fixedWidth > 0) {
                    fixedWidth += item.fixedWidth;
                } else {
                    flexibleCount++;
                }
            }
            
            int totalGaps = (itemCount - 1) * WIDGET_GAP;
            int flexibleWidth = (availableWidth() - fixedWidth - totalGaps) / Math.max(1, flexibleCount);
            
            int currentX = leftPadding();
            for (RowItem item : items) {
                int itemWidth = item.fixedWidth > 0 ? item.fixedWidth : flexibleWidth;
                
                switch (item.type) {
                    case TOGGLE -> createToggleWidget(item.label, item.statePath, currentX, currentY, itemWidth);
                    case ICON_TOGGLE -> createIconToggleWidget(item.label, item.statePath, currentX, currentY, itemWidth);
                    case SLIDER -> createSliderWidget(item.label, item.statePath, currentX, currentY, itemWidth,
                        item.min, item.max, "%.2f", v -> v, v -> v);
                    case SLIDER_DEGREES -> createSliderWidget(item.label, item.statePath, currentX, currentY, itemWidth,
                        0, 360, "%.0f", v -> v * 360f, v -> v / 360f);
                }
                
                currentX += itemWidth + WIDGET_GAP;
            }
            
            currentY += COMPACT_HEIGHT + WIDGET_GAP;
            return ContentBuilder.this;
        }
    }
    
    private enum RowItemType { TOGGLE, ICON_TOGGLE, SLIDER, SLIDER_DEGREES }
    
    private record RowItem(RowItemType type, String label, String statePath, float min, float max, String format, int fixedWidth) {}
}
