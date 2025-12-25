package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.visual.appearance.ColorMode;

/**
 * GUI editing state for appearance properties.
 * 
 * <p>Uses raw int colors (for color pickers) rather than String references
 * like the serialization {@link net.cyberpunk042.visual.appearance.Appearance} record.</p>
 */
public record AppearanceState(
    int color,
    float alphaMin,
    float alphaMax,
    float glow,
    float emissive,
    float saturation,
    float brightness,
    float hueShift,
    int primaryColor,
    int secondaryColor,
    float colorBlend,
    ColorMode colorMode,
    float rainbowSpeed
) {
    public static final AppearanceState DEFAULT = new AppearanceState(
        0xFF00FFFF,  // color (cyan)
        1.0f,        // alphaMin
        1.0f,        // alphaMax
        0.5f,        // glow
        0f,          // emissive
        1f,          // saturation (1.0 = no change)
        1f,          // brightness (1.0 = no change)
        0f,          // hueShift (0 = no shift)
        0xFF00FFFF,  // primaryColor
        0xFFFF00FF,  // secondaryColor (magenta)
        0f,          // colorBlend (0 = only primary, 1 = only secondary)
        ColorMode.SOLID,  // colorMode
        1.0f         // rainbowSpeed
    );

    // Legacy compatibility: single alpha accessor
    public float alpha() { return alphaMin; }

    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .color(color)
            .alphaMin(alphaMin)
            .alphaMax(alphaMax)
            .glow(glow)
            .emissive(emissive)
            .saturation(saturation)
            .brightness(brightness)
            .hueShift(hueShift)
            .primaryColor(primaryColor)
            .secondaryColor(secondaryColor)
            .colorBlend(colorBlend)
            .colorMode(colorMode)
            .rainbowSpeed(rainbowSpeed);
    }

    public static class Builder {
        private int color = 0xFF00FFFF;
        private float alphaMin = 1.0f;
        private float alphaMax = 1.0f;
        private float glow = 0.5f;
        private float emissive = 0f;
        private float saturation = 1f;
        private float brightness = 1f;
        private float hueShift = 0f;
        private int primaryColor = 0xFF00FFFF;
        private int secondaryColor = 0xFFFF00FF;
        private float colorBlend = 0f;
        private ColorMode colorMode = ColorMode.SOLID;
        private float rainbowSpeed = 1.0f;

        public Builder color(int c) { this.color = c; return this; }
        public Builder alphaMin(float a) { this.alphaMin = a; return this; }
        public Builder alphaMax(float a) { this.alphaMax = a; return this; }
        public Builder alpha(float a) { this.alphaMin = a; this.alphaMax = a; return this; }
        public Builder glow(float g) { this.glow = g; return this; }
        public Builder emissive(float e) { this.emissive = e; return this; }
        public Builder saturation(float s) { this.saturation = s; return this; }
        public Builder brightness(float b) { this.brightness = b; return this; }
        public Builder hueShift(float h) { this.hueShift = h; return this; }
        public Builder primaryColor(int c) { this.primaryColor = c; return this; }
        public Builder secondaryColor(int c) { this.secondaryColor = c; return this; }
        public Builder colorBlend(float b) { this.colorBlend = b; return this; }
        public Builder colorMode(ColorMode m) { this.colorMode = m; return this; }
        public Builder rainbowSpeed(float s) { this.rainbowSpeed = s; return this; }

        public AppearanceState build() {
            return new AppearanceState(color, alphaMin, alphaMax, glow, emissive, 
                saturation, brightness, hueShift, primaryColor, secondaryColor, colorBlend,
                colorMode, rainbowSpeed);
        }
    }
}
