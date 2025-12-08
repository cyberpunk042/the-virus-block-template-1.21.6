package net.cyberpunk042.block.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class GrowthCollisionTracker {
	private static final ConcurrentHashMap<RegistryKey<World>, CopyOnWriteArraySet<ProgressiveGrowthBlockEntity>> ACTIVE =
			new ConcurrentHashMap<>();

	private GrowthCollisionTracker() {
	}

	public static void register(ProgressiveGrowthBlockEntity entity) {
		if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		ACTIVE
			.computeIfAbsent(serverWorld.getRegistryKey(), key -> new CopyOnWriteArraySet<>())
			.add(entity);
	}

	public static void unregister(ProgressiveGrowthBlockEntity entity) {
		if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		CopyOnWriteArraySet<ProgressiveGrowthBlockEntity> set = ACTIVE.get(serverWorld.getRegistryKey());
		if (set == null) {
			return;
		}
		set.remove(entity);
		if (set.isEmpty()) {
			ACTIVE.remove(serverWorld.getRegistryKey(), set);
		}
	}

	public static Collection<ProgressiveGrowthBlockEntity> active(World world) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return Collections.emptyList();
		}
		Set<ProgressiveGrowthBlockEntity> set = ACTIVE.get(serverWorld.getRegistryKey());
		return set != null ? set : Collections.emptyList();
	}

	public static void forEachActive(Consumer<ProgressiveGrowthBlockEntity> consumer) {
		if (consumer == null) {
			return;
		}
		for (Map.Entry<RegistryKey<World>, CopyOnWriteArraySet<ProgressiveGrowthBlockEntity>> entry : ACTIVE.entrySet()) {
			for (ProgressiveGrowthBlockEntity entity : entry.getValue()) {
				if (entity == null || entity.isRemoved()) {
					continue;
				}
				consumer.accept(entity);
			}
		}
	}
}

