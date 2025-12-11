package net.cyberpunk042.client.gui.util;

/**
 * Global GUI theming constants.
 * Change here → updates everywhere.
 */
public final class GuiConstants {
    
    private GuiConstants() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int WIDGET_HEIGHT = 20;
    public static final int COMPACT_HEIGHT = 14;    // Compact widget height (for dense panels)
    public static final int COMPACT_GAP = 2;        // Gap between compact widgets
    public static final int BUTTON_WIDTH = 120;
    public static final int BUTTON_WIDTH_WIDE = 200;
    public static final int BUTTON_WIDTH_NARROW = 80;
    public static final int SLIDER_WIDTH = 150;
    public static final int PADDING = 4;
    public static final int SECTION_GAP = 12;
    public static final int SECTION_SPACING = 20;
    public static final int MARGIN = 10;
    public static final int TAB_HEIGHT = 24;
    public static final int BOTTOM_BAR_HEIGHT = 28;
    public static final int HEADER_HEIGHT = 16;
    public static final int LABEL_WIDTH = 80;
    
    // Aliases for sub-panel consistency
    public static final int ELEMENT_HEIGHT = WIDGET_HEIGHT;
    public static final int ELEMENT_SPACING = SECTION_GAP;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (ARGB: 0xAARRGGBB)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Backgrounds
    public static final int BG_PRIMARY = 0xE0101010;
    public static final int BG_SECONDARY = 0xFF1A1A1A;
    public static final int BG_SCREEN = BG_PRIMARY;  // Alias
    public static final int BG_PANEL = 0xFF1E1E1E;
    public static final int BG_WIDGET = 0xFF2D2D2D;
    public static final int BG_WIDGET_HOVER = 0xFF3D3D3D;
    public static final int BG_WIDGET_FOCUS = 0xFF4D4D4D;
    public static final int BG_HEADER = 0xFF252525;
    
    // Text
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_DISABLED = 0xFF666666;
    public static final int TEXT_LABEL = 0xFFCCCCCC;
    
    // Accent
    public static final int ACCENT = 0xFF4488FF;
    public static final int ACCENT_HOVER = 0xFF66AAFF;
    
    // Status
    public static final int SUCCESS = 0xFF44DD44;
    public static final int WARNING = 0xFFDDAA00;
    public static final int ERROR = 0xFFFF4444;
    
    // Border
    public static final int BORDER = 0xFF404040;
    public static final int BORDER_FOCUS = 0xFF4488FF;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int FADE_DURATION_MS = 150;
    public static final float HOVER_SCALE = 1.02f;
}
