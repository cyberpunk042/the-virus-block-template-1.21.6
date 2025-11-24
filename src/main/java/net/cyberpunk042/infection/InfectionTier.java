package net.cyberpunk042.infection;

import java.util.Arrays;

public enum InfectionTier {
	ONE(0, 2400, 3, 4.5F, 120.0D),
	TWO(1, 3600, 5, 5.5F, 220.0D),
	THREE(2, 4800, 7, 6.5F, 320.0D),
	FOUR(3, 3600, 9, 8.0F, 420.0D),
	FIVE(4, 4800, 12, 10.0F, 600.0D);

	private final int index;
	private final int durationTicks;
	private final int mutationBursts;
	private final float baseAuraRadius;
	private final double baseHealth;

	InfectionTier(int index, int durationTicks, int mutationBursts, float baseAuraRadius, double baseHealth) {
		this.index = index;
		this.durationTicks = durationTicks;
		this.mutationBursts = mutationBursts;
		this.baseAuraRadius = baseAuraRadius;
		this.baseHealth = baseHealth;
	}

	public int getIndex() {
		return index;
	}

	public int getLevel() {
		return index + 1;
	}

	public boolean isAtLeast(InfectionTier other) {
		return this.index >= other.index;
	}

	public boolean isBelow(InfectionTier other) {
		return this.index < other.index;
	}

	public int getDurationTicks() {
		return durationTicks;
	}

	public int getMutationBursts() {
		return mutationBursts;
	}

	public float getBaseAuraRadius() {
		return baseAuraRadius;
	}

	public double getBaseHealth() {
		return baseHealth;
	}

	public InfectionTier next() {
		return values()[Math.min(values().length - 1, index + 1)];
	}

	public static InfectionTier byIndex(int index) {
		return Arrays.stream(values())
				.filter(tier -> tier.index == index)
				.findFirst()
				.orElse(FIVE);
	}

	public static int maxIndex() {
		return values()[values().length - 1].index;
	}
}

