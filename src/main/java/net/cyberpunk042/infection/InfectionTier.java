package net.cyberpunk042.infection;

import java.util.Arrays;

public enum InfectionTier {
	ONE(0, 2400, 3, 4.5F),
	TWO(1, 3600, 5, 5.5F),
	THREE(2, 4800, 7, 6.5F),
	FOUR(3, 3600, 9, 8.0F),
	FIVE(4, 4800, 12, 10.0F);

	private final int index;
	private final int durationTicks;
	private final int mutationBursts;
	private final float baseAuraRadius;

	InfectionTier(int index, int durationTicks, int mutationBursts, float baseAuraRadius) {
		this.index = index;
		this.durationTicks = durationTicks;
		this.mutationBursts = mutationBursts;
		this.baseAuraRadius = baseAuraRadius;
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

