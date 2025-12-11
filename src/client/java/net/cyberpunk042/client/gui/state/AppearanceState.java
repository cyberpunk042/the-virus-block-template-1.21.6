package net.cyberpunk042.client.gui.state;

/**
 * GUI editing state for appearance properties.
 * 
 * <p>Uses raw int colors (for color pickers) rather than String references
 * like the serialization {@link net.cyberpunk042.visual.appearance.Appearance} record.</p>
 */
public record AppearanceState(
    int color,
    float alpha,
    float glow,
    float emissive,
    float saturation,
    int primaryColor,
    int secondaryColor
) {
    public static final AppearanceState DEFAULT = new AppearanceState(
        0xFF00FFFF,  // color (cyan)
        0.8f,        // alpha
        0.5f,        // glow
        0f,          // emissive
        0f,          // saturation
        0xFF00FFFF,  // primaryColor
        0xFFFF00FF   // secondaryColor (magenta)
    );

    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .color(color)
            .alpha(alpha)
            .glow(glow)
            .emissive(emissive)
            .saturation(saturation)
            .primaryColor(primaryColor)
            .secondaryColor(secondaryColor);
    }

    public static class Builder {
        private int color = 0xFF00FFFF;
        private float alpha = 0.8f;
        private float glow = 0.5f;
        private float emissive = 0f;
        private float saturation = 0f;
        private int primaryColor = 0xFF00FFFF;
        private int secondaryColor = 0xFFFF00FF;

        public Builder color(int c) { this.color = c; return this; }
        public Builder alpha(float a) { this.alpha = a; return this; }
        public Builder glow(float g) { this.glow = g; return this; }
        public Builder emissive(float e) { this.emissive = e; return this; }
        public Builder saturation(float s) { this.saturation = s; return this; }
        public Builder primaryColor(int c) { this.primaryColor = c; return this; }
        public Builder secondaryColor(int c) { this.secondaryColor = c; return this; }

        public AppearanceState build() {
            return new AppearanceState(color, alpha, glow, emissive, saturation, primaryColor, secondaryColor);
        }
    }
}


