package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * G27-G29: Color button with swatch display, hex input, and theme colors.
 * Click to open a simple hex input popup or cycle theme colors.
 */
public class ColorButton extends ButtonWidget {
    
    // Theme color constants
    public static final int THEME_PRIMARY = 0xFF00FFFF;    // Cyan
    public static final int THEME_SECONDARY = 0xFFFF00FF;  // Magenta
    public static final int THEME_ACCENT = 0xFFFFFF00;     // Yellow
    public static final int THEME_SUCCESS = 0xFF00FF00;    // Green
    public static final int THEME_WARNING = 0xFFFFAA00;    // Orange
    public static final int THEME_ERROR = 0xFFFF0000;      // Red
    
    private static final int[] THEME_COLORS = {
        THEME_PRIMARY, THEME_SECONDARY, THEME_ACCENT, 
        THEME_SUCCESS, THEME_WARNING, THEME_ERROR
    };
    private static final String[] THEME_NAMES = {
        "@primary", "@secondary", "@accent",
        "@success", "@warning", "@error"
    };
    
    private int currentColor;
    private int themeIndex = -1; // -1 = custom color
    private final Consumer<Integer> onChange;
    
    /**
     * Creates a color button.
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param label Display label
     * @param initialColor Initial color (ARGB)
     * @param onChange Callback when color changes
     */
    public ColorButton(int x, int y, int width, String label, int initialColor, Consumer<Integer> onChange) {
        super(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label), 
              btn -> ((ColorButton) btn).cycleColor(), DEFAULT_NARRATION_SUPPLIER);
        this.currentColor = initialColor;
        this.onChange = onChange;
        
        // Check if initial is a theme color
        for (int i = 0; i < THEME_COLORS.length; i++) {
            if (THEME_COLORS[i] == initialColor) {
                themeIndex = i;
                break;
            }
        }
        
        Logging.GUI.topic("widget").trace("ColorButton created: {}", label);
    }
    
    private void cycleColor() {
        themeIndex = (themeIndex + 1) % THEME_COLORS.length;
        currentColor = THEME_COLORS[themeIndex];
        Logging.GUI.topic("widget").trace("ColorButton cycled to: {} ({})", 
            THEME_NAMES[themeIndex], Integer.toHexString(currentColor));
        if (onChange != null) {
            onChange.accept(currentColor);
        }
    }
    
    /**
     * Sets a custom hex color.
     * @param hexColor Color in format "#RRGGBB" or "RRGGBB"
     */
    public void setHexColor(String hexColor) {
        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            currentColor = 0xFF000000 | Integer.parseInt(hex, 16);
            themeIndex = -1; // Custom color
            if (onChange != null) {
                onChange.accept(currentColor);
            }
            Logging.GUI.topic("widget").trace("ColorButton set to hex: #{}", hex);
        } catch (NumberFormatException e) {
            Logging.GUI.topic("widget").warn("Invalid hex color: {}", hexColor);
        }
    }
    
    /**
     * Gets the current color.
     */
    public int getColor() {
        return currentColor;
    }
    
    /**
     * Gets the current color as hex string.
     */
    public String getHexColor() {
        return String.format("#%06X", currentColor & 0xFFFFFF);
    }
    
    /**
     * Gets the theme name if using a theme color, or null.
     */
    public String getThemeName() {
        return themeIndex >= 0 ? THEME_NAMES[themeIndex] : null;
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        
        // Draw color swatch on the right side
        int swatchSize = height - 4;
        int swatchX = getX() + width - swatchSize - 2;
        int swatchY = getY() + 2;
        
        // Border
        context.fill(swatchX - 1, swatchY - 1, swatchX + swatchSize + 1, swatchY + swatchSize + 1, 0xFF000000);
        // Color fill
        context.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, currentColor);
    }
    
    public static ColorButton create(int x, int y, String label, int color, Consumer<Integer> onChange) {
        return new ColorButton(x, y, GuiConstants.BUTTON_WIDTH, label, color, onChange);
    }
}
