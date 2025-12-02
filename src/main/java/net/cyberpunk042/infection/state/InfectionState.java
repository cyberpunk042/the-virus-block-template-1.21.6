package net.cyberpunk042.infection.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.infection.VirusEventType;

/**
 * Aggregates fields related to the infection progression so {@link net.cyberpunk042.infection.VirusWorldState}
 * doesn't have to expose raw maps and counters.
 */
public final class InfectionState {

	private boolean infected;
	private boolean dormant;
	private long totalTicks;
	private boolean terrainCorrupted;
	private boolean cleansingActive;
	private boolean boobytrapDefaultsCaptured;
	private boolean defaultBoobytrapsEnabled = true;
	private int defaultWormSpawnChance = -1;
	private int defaultWormTrapSpawnChance = -1;
	private long lastMatrixCubeTick;
	private final Map<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Object2IntMap<UUID> infectiousInventoryTicks = new Object2IntOpenHashMap<>();
	private final Object2LongMap<UUID> infectiousInventoryWarnCooldowns = new Object2LongOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousContactTicks = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> helmetPingTimers = new Object2IntOpenHashMap<>();
	private final Object2DoubleMap<UUID> heavyPantsVoidWear = new Object2DoubleOpenHashMap<>();

	public boolean infected() {
		return infected;
	}

	public void setInfected(boolean infected) {
		this.infected = infected;
	}

	public boolean dormant() {
		return dormant;
	}

	public void setDormant(boolean dormant) {
		this.dormant = dormant;
	}

	public long totalTicks() {
		return totalTicks;
	}

	public void incrementTicks() {
		totalTicks++;
	}

	public void resetTicks() {
		totalTicks = 0L;
	}

	public void setTotalTicks(long totalTicks) {
		this.totalTicks = Math.max(0L, totalTicks);
	}

	public Map<VirusEventType, Long> eventHistory() {
		return eventHistory;
	}

	public Object2IntMap<UUID> infectiousInventoryTicks() {
		return infectiousInventoryTicks;
	}

	public Object2LongMap<UUID> infectiousInventoryWarnCooldowns() {
		return infectiousInventoryWarnCooldowns;
	}

	public Object2IntMap<UUID> infectiousContactTicks() {
		return infectiousContactTicks;
	}

	public Object2IntMap<UUID> helmetPingTimers() {
		return helmetPingTimers;
	}

	public Object2DoubleMap<UUID> heavyPantsVoidWear() {
		return heavyPantsVoidWear;
	}

	public boolean terrainCorrupted() {
		return terrainCorrupted;
	}

	public void setTerrainCorrupted(boolean terrainCorrupted) {
		this.terrainCorrupted = terrainCorrupted;
	}

	public boolean cleansingActive() {
		return cleansingActive;
	}

	public void setCleansingActive(boolean cleansingActive) {
		this.cleansingActive = cleansingActive;
	}

	public boolean boobytrapDefaultsCaptured() {
		return boobytrapDefaultsCaptured;
	}

	public void setBoobytrapDefaultsCaptured(boolean captured) {
		this.boobytrapDefaultsCaptured = captured;
	}

	public boolean defaultBoobytrapsEnabled() {
		return defaultBoobytrapsEnabled;
	}

	public void setDefaultBoobytrapsEnabled(boolean enabled) {
		this.defaultBoobytrapsEnabled = enabled;
	}

	public int defaultWormSpawnChance() {
		return defaultWormSpawnChance;
	}

	public void setDefaultWormSpawnChance(int chance) {
		this.defaultWormSpawnChance = chance;
	}

	public int defaultWormTrapSpawnChance() {
		return defaultWormTrapSpawnChance;
	}

	public void setDefaultWormTrapSpawnChance(int chance) {
		this.defaultWormTrapSpawnChance = chance;
	}

	public long lastMatrixCubeTick() {
		return lastMatrixCubeTick;
	}

	public void setLastMatrixCubeTick(long tick) {
		this.lastMatrixCubeTick = tick;
	}
}

