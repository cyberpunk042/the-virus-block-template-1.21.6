package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Nested tab container with horizontal tab buttons and content area.
 * 
 * <pre>
 * [Fill] [Appear] [Visibility]
 * ├─────────────────────────────┤
 * │  (active tab content)       │
 * │                             │
 * └─────────────────────────────┘
 * </pre>
 */
public class SubTabPane {
    
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_GAP = 2;
    
    private final TextRenderer textRenderer;
    private final List<TabEntry> tabs = new ArrayList<>();
    private int activeIndex = 0;
    
    private Bounds bounds = Bounds.EMPTY;
    private Bounds tabBarBounds = Bounds.EMPTY;
    private Bounds contentBounds = Bounds.EMPTY;
    
    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    private Consumer<Integer> onTabChange;
    
    public SubTabPane(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Adds a tab with label and content provider.
     */
    public SubTabPane addTab(String label, ContentProvider content) {
        tabs.add(new TabEntry(label, content));
        return this;
    }
    
    /**
     * Adds a tab with an AbstractPanel as content.
     */
    public SubTabPane addTab(String label, AbstractPanel panel) {
        tabs.add(new TabEntry(label, new PanelContentProvider(panel)));
        return this;
    }
    
    /**
     * Adds a tab with a LayoutPanel (v2) as content.
     */
    public SubTabPane addTab(String label, LayoutPanel panel) {
        tabs.add(new TabEntry(label, new LayoutPanelContentProvider(panel)));
        return this;
    }
    
    public SubTabPane onTabChange(Consumer<Integer> callback) {
        this.onTabChange = callback;
        return this;
    }
    
    public SubTabPane setActiveTab(int index) {
        if (index >= 0 && index < tabs.size() && index != activeIndex) {
            activeIndex = index;
            updateTabButtons();
            updateContentBounds();
            if (onTabChange != null) {
                onTabChange.accept(index);
            }
        }
        return this;
    }
    
    public int getActiveTab() {
        return activeIndex;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        
        // Tab bar at top
        tabBarBounds = bounds.sliceTop(TAB_HEIGHT);
        
        // Content below
        contentBounds = bounds.withoutTop(TAB_HEIGHT + 2);
        
        rebuildWidgets();
    }
    
    public Bounds getContentBounds() {
        return contentBounds;
    }
    
    private void rebuildWidgets() {
        tabButtons.clear();
        
        if (bounds.isEmpty() || tabs.isEmpty()) return;
        
        // Calculate tab widths
        int totalGaps = (tabs.size() - 1) * TAB_GAP;
        int availableWidth = tabBarBounds.width() - totalGaps;
        int tabWidth = availableWidth / tabs.size();
        
        int x = tabBarBounds.x();
        int y = tabBarBounds.y();
        
        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            TabEntry tab = tabs.get(i);
            
            ButtonWidget btn = ButtonWidget.builder(Text.literal(tab.label), b -> setActiveTab(tabIndex))
                .dimensions(x, y, tabWidth, TAB_HEIGHT)
                .build();
            
            tabButtons.add(btn);
            x += tabWidth + TAB_GAP;
        }
        
        updateTabButtons();
        updateContentBounds();
    }
    
    private void updateTabButtons() {
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = i != activeIndex;
        }
    }
    
    private void updateContentBounds() {
        if (activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.setBounds(contentBounds);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<ClickableWidget> getWidgets() {
        List<ClickableWidget> all = new ArrayList<>(tabButtons);
        if (activeIndex < tabs.size()) {
            all.addAll(tabs.get(activeIndex).content.getWidgets());
        }
        return all;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void tick() {
        if (activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.tick();
        }
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds.isEmpty()) return;
        
        // Tab bar background
        context.fill(tabBarBounds.x(), tabBarBounds.y(), 
                     tabBarBounds.right(), tabBarBounds.bottom(), 0xFF222233);
        
        // Active tab indicator
        if (!tabButtons.isEmpty() && activeIndex < tabButtons.size()) {
            ButtonWidget activeBtn = tabButtons.get(activeIndex);
            context.fill(activeBtn.getX(), activeBtn.getY() + activeBtn.getHeight() - 2,
                        activeBtn.getX() + activeBtn.getWidth(), activeBtn.getY() + activeBtn.getHeight(),
                        0xFF4488FF);
        }
        
        // Content background (matches right column of grid)
        context.fill(contentBounds.x(), contentBounds.y(),
                     contentBounds.right(), contentBounds.bottom(), 0xFF12121a);
        
        // Render active content
        if (activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.render(context, mouseX, mouseY, delta);
        }
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!contentBounds.contains(mouseX, mouseY)) return false;
        if (activeIndex < tabs.size()) {
            return tabs.get(activeIndex).content.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INNER TYPES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private record TabEntry(String label, ContentProvider content) {}
    
    /**
     * Interface for tab content providers.
     */
    public interface ContentProvider {
        void setBounds(Bounds bounds);
        List<ClickableWidget> getWidgets();
        void tick();
        void render(DrawContext context, int mouseX, int mouseY, float delta);
        default boolean mouseScrolled(double mouseX, double mouseY, double h, double v) { return false; }
    }
    
    /**
     * Adapter for AbstractPanel as ContentProvider.
     */
    private static class PanelContentProvider implements ContentProvider {
        private final AbstractPanel panel;
        
        PanelContentProvider(AbstractPanel panel) {
            this.panel = panel;
        }
        
        @Override public void setBounds(Bounds bounds) { panel.setBounds(bounds); }
        @Override public List<ClickableWidget> getWidgets() { return panel.getWidgets(); }
        @Override public void tick() { panel.tick(); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            panel.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
            return panel.mouseScrolled(mouseX, mouseY, h, v);
        }
    }
    
    /**
     * Adapter for LayoutPanel (v2) as ContentProvider.
     */
    private static class LayoutPanelContentProvider implements ContentProvider {
        private final LayoutPanel panel;
        
        LayoutPanelContentProvider(LayoutPanel panel) {
            this.panel = panel;
        }
        
        @Override public void setBounds(Bounds bounds) { panel.setBounds(bounds); }
        @Override public List<ClickableWidget> getWidgets() { return panel.getWidgets(); }
        @Override public void tick() { panel.tick(); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            panel.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
            return panel.mouseScrolled(mouseX, mouseY, h, v);
        }
    }
}

