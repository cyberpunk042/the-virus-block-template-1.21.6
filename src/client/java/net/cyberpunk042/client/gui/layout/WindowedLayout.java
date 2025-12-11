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
    
    // Configuration
    private static final int SIDE_PANEL_WIDTH = 180;
    private static final int SIDE_PANEL_HEIGHT = 300;
    private static final int STATUS_BAR_HEIGHT = 20;
    private static final int TITLE_BAR_HEIGHT = 16;
    private static final int TAB_HEIGHT = 18;
    private static final int MARGIN = 8;
    private static final int GAP = 6;
    
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
        
        // Left panel: anchored to left side
        int leftX = MARGIN;
        int panelY = (screenHeight - SIDE_PANEL_HEIGHT) / 2;
        leftPanel = new Bounds(leftX, panelY, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT);
        
        // Right panel: anchored to right side
        int rightX = screenWidth - MARGIN - SIDE_PANEL_WIDTH;
        rightPanel = new Bounds(rightX, panelY, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT);
        
        // Status bar: spans bottom, between the panels
        int statusY = screenHeight - MARGIN - STATUS_BAR_HEIGHT;
        statusBar = new Bounds(
            leftX + SIDE_PANEL_WIDTH + GAP,
            statusY,
            rightX - leftX - SIDE_PANEL_WIDTH - GAP * 2,
            STATUS_BAR_HEIGHT
        );
        
        // Sub-regions within left panel
        leftTitleBar = leftPanel.sliceTop(TITLE_BAR_HEIGHT);
        Bounds leftRemaining = leftPanel.withoutTop(TITLE_BAR_HEIGHT);
        leftTabBar = leftRemaining.sliceTop(TAB_HEIGHT);
        leftContent = leftRemaining.withoutTop(TAB_HEIGHT).inset(GAP);
        
        // Sub-regions within right panel
        rightTitleBar = rightPanel.sliceTop(TITLE_BAR_HEIGHT);
        Bounds rightRemaining = rightPanel.withoutTop(TITLE_BAR_HEIGHT);
        rightTabBar = rightRemaining.sliceTop(TAB_HEIGHT);
        rightContent = rightRemaining.withoutTop(TAB_HEIGHT).inset(GAP);
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
        // Left panel content is the main content
        return leftContent;
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
    public boolean shouldPauseGame() { return false; }
    
    @Override
    public boolean allowsPlayerMovement() { return true; }
    
    @Override
    public boolean showsGameWorld() { return true; }
    
    @Override
    public boolean hasPreviewWidget() { return false; }
}

