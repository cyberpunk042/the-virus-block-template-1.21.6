package net.cyberpunk042.growth;

import net.minecraft.util.Identifier;

/**
 * Immutable configuration describing how a progressive growth block behaves.
 * Instances are loaded from JSON and referenced by runtime block entities.
 */
public record GrowthBlockDefinition(
		Identifier id,
		boolean growthEnabled,
		int rateTicks,
		double rateScale,
		double startScale,
		double targetScale,
		double minScale,
		double maxScale,
		boolean hasCollision,
		boolean doesDestruction,
		boolean hasFuse,
		boolean isWobbly,
		boolean isPulling,
		boolean isPushing,
		double pullingForce,
		double pushingForce,
		double touchDamage,
		Identifier glowProfileId,
		Identifier particleProfileId,
		Identifier fieldProfileId,
		Identifier pullProfileId,
		Identifier pushProfileId,
		Identifier fuseProfileId,
		Identifier explosionProfileId) {

	public static GrowthBlockDefinition defaults() {
		return new GrowthBlockDefinition(
				Identifier.of("the-virus-block", "magma"),
				true,
				5,
				1.0D,
				0.35D,
				1.0D,
				0.25D,
				1.4D,
				true,
				false,
				true,
				true,
				true,
				false,
				1.0D,
				0.5D,
				4.0D,
				Identifier.of("the-virus-block", "prototype"),
				Identifier.of("the-virus-block", "default"),
				Identifier.of("the-virus-block", "default_field"),
				Identifier.of("the-virus-block", "default_pull"),
				Identifier.of("the-virus-block", "default_push"),
				Identifier.of("the-virus-block", "default_fuse"),
				Identifier.of("the-virus-block", "default_explosion"));
	}

	public int sanitizedRate() {
		return Math.max(1, rateTicks);
	}

	public double clampedRateScale() {
		return Math.max(0.01D, rateScale);
	}

	public double clampedStartScale() {
		return Math.max(minScale, Math.min(maxScale, startScale));
	}

	public double clampedTargetScale() {
		return Math.max(minScale, Math.min(maxScale, targetScale));
	}
}

