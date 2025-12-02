package net.cyberpunk042.growth;

import net.minecraft.util.Identifier;

/**
 * Visual preset for progressive growth blocks. Controls textures and
 * alpha/spin hints for the client renderer.
 */
public record GlowProfile(
		Identifier id,
		Identifier primaryTexture,
		Identifier secondaryTexture,
		float primaryAlpha,
		float secondaryAlpha,
		float spinSpeed) {

	public static GlowProfile defaults() {
		return new GlowProfile(
				Identifier.of("the-virus-block", "magma"),
				Identifier.of("the-virus-block", "textures/misc/glow_magma_primary.png"),
				Identifier.of("the-virus-block", "textures/misc/glow_magma_secondary.png"),
				0.65F,
				1.0F,
				1.0F);
	}

	public float clampedPrimaryAlpha() {
		return clampAlpha(primaryAlpha);
	}

	public float clampedSecondaryAlpha() {
		return clampAlpha(secondaryAlpha);
	}

	private static float clampAlpha(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}

