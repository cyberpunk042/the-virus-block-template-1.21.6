package net.cyberpunk042.client.gui.layout;

/**
 * Fullscreen layout: Immersive editor with 3D viewport.
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ [≡] Field Customizer                                    [─][×] │ ← Title bar
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │                    3D VIEWPORT (PREVIEW)                        │
 * │                                                                 │
 * │                         ████████                                │
 * │                        ██████████                               │
 * │                       ████████████                              │
 * │                        ██████████                               │
 * │                         ████████                                │
 * │                                                                 │
 * │   [orbit with mouse]  [zoom with scroll]                        │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ [Quick] [Advanced] [Debug] [Profiles]                           │ ← Tab bar
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │                    CONTENT PANEL                                │
 * │              (scrollable, shows active tab)                     │
 * │                                                                 │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ Layer: base | Primitive: sphere | Dirty: ●                      │ ← Status bar
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class FullscreenLayout implements LayoutManager {
    
    // Configuration
    private static final int TITLE_BAR_HEIGHT = 24;
    private static final int TAB_BAR_HEIGHT = 28;
    private static final int STATUS_BAR_HEIGHT = 24;
    private static final int MARGIN = 12;
    private static final int PREVIEW_MIN_HEIGHT = 200;
    private static final float PREVIEW_RATIO = 0.45f;  // 45% of available height for preview
    
    // Calculated bounds
    private Bounds panel = Bounds.EMPTY;
    private Bounds titleBar = Bounds.EMPTY;
    private Bounds preview = Bounds.EMPTY;
    private Bounds tabBar = Bounds.EMPTY;
    private Bounds content = Bounds.EMPTY;
    private Bounds statusBar = Bounds.EMPTY;
    
    @Override
    public GuiMode getMode() {
        return GuiMode.FULLSCREEN;
    }
    
    @Override
    public void calculate(int screenWidth, int screenHeight) {
        // Full panel with margin
        panel = new Bounds(MARGIN, MARGIN, screenWidth - MARGIN * 2, screenHeight - MARGIN * 2);
        
        // Title bar at top
        titleBar = panel.sliceTop(TITLE_BAR_HEIGHT);
        Bounds remaining = panel.withoutTop(TITLE_BAR_HEIGHT);
        
        // Status bar at bottom
        statusBar = remaining.sliceBottom(STATUS_BAR_HEIGHT);
        remaining = remaining.withoutBottom(STATUS_BAR_HEIGHT);
        
        // Tab bar above status
        tabBar = remaining.sliceBottom(TAB_BAR_HEIGHT);
        remaining = remaining.withoutBottom(TAB_BAR_HEIGHT);
        
        // Split remaining between preview and content
        int availableHeight = remaining.height();
        int previewHeight = Math.max(PREVIEW_MIN_HEIGHT, (int)(availableHeight * PREVIEW_RATIO));
        
        preview = remaining.sliceTop(previewHeight).inset(MARGIN);
        content = remaining.withoutTop(previewHeight).inset(MARGIN, 0);
    }
    
    @Override
    public Bounds getPanelBounds() {
        return panel;
    }
    
    @Override
    public Bounds getTitleBarBounds() {
        return titleBar;
    }
    
    @Override
    public Bounds getPreviewBounds() {
        return preview;
    }
    
    @Override
    public Bounds getTabBarBounds() {
        return tabBar;
    }
    
    @Override
    public Bounds getContentBounds() {
        return content;
    }
    
    @Override
    public Bounds getStatusBarBounds() {
        return statusBar;
    }
    
    @Override
    public boolean shouldPauseGame() { return true; }
    
    @Override
    public boolean allowsPlayerMovement() { return false; }
    
    @Override
    public boolean showsGameWorld() { return false; }
    
    @Override
    public boolean hasPreviewWidget() { return true; }
}

