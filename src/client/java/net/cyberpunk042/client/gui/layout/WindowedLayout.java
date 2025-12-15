package net.cyberpunk042.client.gui.layout;

/**
 * Windowed layout: Split HUD with game world visible in center.
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                                                             │
 * │  ┌──────────┐                              ┌──────────┐    │
 * │  │  LEFT    │                              │  RIGHT   │    │
 * │  │  PANEL   │      GAME WORLD              │  PANEL   │    │
 * │  │          │                              │          │    │
 * │  │ ▸ Shape  │         (player)             │ Layers   │    │
 * │  │ ▸ Fill   │      (debug field)           │ Actions  │    │
 * │  │ ▸ Color  │                              │          │    │
 * │  │          │                              │          │    │
 * │  └──────────┘                              └──────────┘    │
 * │                                                             │
 * │  ┌───────────────────────────────────────────────────────┐ │
 * │  │                    Status Bar                          │ │
 * │  └───────────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class WindowedLayout implements LayoutManager {
    
    // Configuration - match OLD working code
    private static final int STATUS_BAR_HEIGHT = 18;  // OLD: STATUS_HEIGHT = 18
    private static final int TITLE_BAR_HEIGHT = 16;   // OLD: titleH = 16
    private static final int TAB_HEIGHT = 18;         // OLD: tabH = 18
    private static final int SELECTOR_HEIGHT = 22;    // OLD: SELECTOR_HEIGHT = 22
    private static final int MARGIN = 8;              // OLD: MARGIN = 8
    private static final int PADDING = 4;             // OLD: padding = 4
    
    // Calculated bounds
    private Bounds leftPanel = Bounds.EMPTY;
    private Bounds rightPanel = Bounds.EMPTY;
    private Bounds statusBar = Bounds.EMPTY;
    private Bounds leftTitleBar = Bounds.EMPTY;
    private Bounds rightTitleBar = Bounds.EMPTY;
    private Bounds leftTabBar = Bounds.EMPTY;
    private Bounds rightTabBar = Bounds.EMPTY;
    private Bounds leftContent = Bounds.EMPTY;
    private Bounds rightContent = Bounds.EMPTY;
    
    private int screenWidth;
    private int screenHeight;
    
    @Override
    public GuiMode getMode() {
        return GuiMode.WINDOWED;
    }
    
    @Override
    public void calculate(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // OLD: Dynamic panel sizing - narrower for more center space
        int panelWidth = Math.min(200, Math.max(180, (screenWidth - MARGIN * 4) / 5));
        int panelHeight = screenHeight - MARGIN * 2 - STATUS_BAR_HEIGHT;
        
        // Left panel: anchored to left side, full height
        int leftX = MARGIN;
        int panelY = MARGIN;
        leftPanel = new Bounds(leftX, panelY, panelWidth, panelHeight);
        
        // Right panel: anchored to right side, full height
        int rightX = screenWidth - MARGIN - panelWidth;
        rightPanel = new Bounds(rightX, panelY, panelWidth, panelHeight);
        
        // Status bar: spans bottom, between the panels
        int statusY = screenHeight - MARGIN - STATUS_BAR_HEIGHT;
        statusBar = new Bounds(
            leftX + panelWidth + MARGIN,  // After left panel + margin
            statusY,
            screenWidth - panelWidth * 2 - MARGIN * 4,  // Width between panels
            STATUS_BAR_HEIGHT
        );
        
        // Sub-regions within left panel (OLD: lines 271-305)
        leftTitleBar = leftPanel.sliceTop(TITLE_BAR_HEIGHT);
        Bounds leftRemaining = leftPanel.withoutTop(TITLE_BAR_HEIGHT);
        leftTabBar = leftRemaining.sliceTop(TAB_HEIGHT);
        leftContent = leftRemaining.withoutTop(TAB_HEIGHT).inset(PADDING);
        
        // Sub-regions within right panel (OLD: lines 400-498)
        rightTitleBar = rightPanel.sliceTop(TITLE_BAR_HEIGHT);
        Bounds rightRemaining = rightPanel.withoutTop(TITLE_BAR_HEIGHT);
        rightContent = rightRemaining.inset(PADDING);
    }
    
    @Override
    public Bounds getPanelBounds() {
        // In windowed mode, the "panel" is conceptually the whole screen area we draw to
        // But we don't draw a single panel - we draw left + right + status
        return new Bounds(0, 0, screenWidth, screenHeight);
    }
    
    @Override
    public Bounds getTitleBarBounds() {
        // Return left panel's title bar as the primary (for mode toggle)
        return leftTitleBar;
    }
    
    @Override
    public Bounds getPreviewBounds() {
        // No dedicated preview in windowed mode - game world IS the preview
        return Bounds.EMPTY;
    }
    
    @Override
    public Bounds getTabBarBounds() {
        // Left panel's tab bar is the main one
        return leftTabBar;
    }
    
    @Override
    public Bounds getContentBounds() {
        // In windowed mode, sub-tabs/content are in the RIGHT panel, below selectors (OLD: lines 464-491)
        return rightContent.withoutTop(SELECTOR_HEIGHT * 2 + PADDING);
    }
    
    @Override
    public Bounds getStatusBarBounds() {
        return statusBar;
    }
    
    @Override
    public Bounds getLeftPanelBounds() {
        return leftPanel;
    }
    
    @Override
    public Bounds getRightPanelBounds() {
        return rightPanel;
    }
    
    @Override
    public Bounds getRightTitleBarBounds() {
        return rightTitleBar;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WINDOWED-SPECIFIC ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Bounds getLeftTitleBar() { return leftTitleBar; }
    public Bounds getRightTitleBar() { return rightTitleBar; }
    public Bounds getLeftTabBar() { return leftTabBar; }
    public Bounds getRightTabBar() { return rightTabBar; }
    public Bounds getLeftContent() { return leftContent; }
    public Bounds getRightContent() { return rightContent; }
    
    @Override
    public Bounds getSelectorBounds() {
        // In windowed mode, selectors are in the right panel content area
        return rightContent.sliceTop(SELECTOR_HEIGHT * 2 + 4);
    }
    
    @Override
    public Bounds getShapePanelBounds() {
        // Shape panel is in the left panel content area
        return leftContent;
    }
    
    @Override
    public Bounds getProfilesLeftBounds() {
        // Profiles list uses the left panel content area
        return leftContent.inset(0, 0);
    }
    
    @Override
    public Bounds getProfilesRightBounds() {
        // Profiles details uses the right panel content area
        return rightContent.inset(0, 0);
    }
    
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {
        // Windowed mode: transparent - game world is visible
        // No background fill
    }
    
    @Override
    public void renderFrame(net.minecraft.client.gui.DrawContext context) {
        // Left panel frame + title bar + tab bar
        renderPanelFrame(context, leftPanel);
        context.fill(leftTitleBar.x(), leftTitleBar.y(), leftTitleBar.right(), leftTitleBar.bottom(), 0xFF2a2a2a);
        context.fill(leftTabBar.x(), leftTabBar.y(), leftTabBar.right(), leftTabBar.bottom(), 0xFF222222);
        // Note: Title text "⬡ Field" is rendered by HeaderBar component
        
        // Right panel frame + title bar
        renderPanelFrame(context, rightPanel);
        context.fill(rightTitleBar.x(), rightTitleBar.y(), rightTitleBar.right(), rightTitleBar.bottom(), 0xFF2a2a2a);
        // Note: Title text "Context" should be rendered by a component or FieldCustomizerScreen
    }
    
    private void renderPanelFrame(net.minecraft.client.gui.DrawContext context, Bounds panel) {
        // Shadow
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() + 2, panel.bottom() + 2, 0x44000000);
        // Border
        context.fill(panel.x() - 1, panel.y() - 1, panel.right() + 1, panel.bottom() + 1, 0xFF444444);
        // Background
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xDD1a1a1a);
    }
    
    @Override
    public boolean shouldPauseGame() { return false; }
    
    @Override
    public boolean allowsPlayerMovement() { return true; }
    
    @Override
    public boolean showsGameWorld() { return true; }
    
    @Override
    public boolean hasPreviewWidget() { return false; }
}

