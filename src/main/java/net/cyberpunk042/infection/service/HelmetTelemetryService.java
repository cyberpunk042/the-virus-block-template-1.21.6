package net.cyberpunk042.infection.service;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks augmented helmet pings for players near virus sources.
 */
public final class HelmetTelemetryService {

	private static final int AUGMENTED_HELMET_PING_INTERVAL = 80;
	private static final double HELMET_PING_MAX_PARTICLE_DISTANCE = 32.0D;
	private static final String[] COMPASS_POINTS = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private final VirusWorldState host;

	public HelmetTelemetryService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void tick() {
		ServerWorld world = host.world();
		Object2IntMap<UUID> helmetTimers = host.infectionState().helmetPingTimers();
		Object2DoubleMap<UUID> heavyPantsWear = host.infectionState().heavyPantsVoidWear();
		if (!host.infectionState().infected() || !host.hasVirusSources()) {
			helmetTimers.clear();
			heavyPantsWear.clear();
			return;
		}
		Set<UUID> tracked = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.isSpectator() || player.isCreative() || !VirusEquipmentHelper.hasAugmentedHelmet(player)) {
				helmetTimers.removeInt(player.getUuid());
				continue;
			}
			UUID uuid = player.getUuid();
			tracked.add(uuid);
			int ticks = helmetTimers.getOrDefault(uuid, 0) + 1;
			if (ticks >= AUGMENTED_HELMET_PING_INTERVAL) {
				helmetTimers.put(uuid, 0);
				pingHelmet(world, player);
			} else {
				helmetTimers.put(uuid, ticks);
			}
		}
		if (tracked.isEmpty()) {
			helmetTimers.clear();
		} else {
			helmetTimers.object2IntEntrySet().removeIf(entry -> !tracked.contains(entry.getKey()));
		}
	}

	private void pingHelmet(ServerWorld world, ServerPlayerEntity player) {
		BlockPos target = findNearestVirusSource(player.getBlockPos());
		if (target == null) {
			player.sendMessage(Text.translatable("message.the-virus-block.helmet_ping_none"), true);
			return;
		}
		Vec3d eye = player.getEyePos();
		Vec3d delta = Vec3d.ofCenter(target).subtract(eye);
		double distance = delta.length();
		if (distance < 0.5D) {
			player.sendMessage(Text.translatable("message.the-virus-block.helmet_ping_here"), true);
			return;
		}
		double visualDistance = Math.min(distance, HELMET_PING_MAX_PARTICLE_DISTANCE);
		spawnHelmetTrail(world, eye, delta, visualDistance);
		player.sendMessage(
				Text.translatable("message.the-virus-block.helmet_ping", Text.literal(describeDirection(delta)),
						MathHelper.floor(distance)),
				true);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_HARP,
				SoundCategory.PLAYERS, 0.35F, 1.8F);
	}

	@Nullable
	private BlockPos findNearestVirusSource(BlockPos origin) {
		if (!host.hasVirusSources()) {
			return null;
		}
		Vec3d originVec = Vec3d.ofCenter(origin);
		BlockPos closest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos source : host.getVirusSources()) {
			double distanceSq = source.toCenterPos().squaredDistanceTo(originVec);
			if (distanceSq < best) {
				best = distanceSq;
				closest = source;
			}
		}
		return closest;
	}

	private void spawnHelmetTrail(ServerWorld world, Vec3d eye, Vec3d delta, double maxDistance) {
		if (maxDistance <= 0.0D) {
			return;
		}
		Vec3d direction = delta.normalize();
		int steps = Math.max(4, MathHelper.floor(maxDistance / 2.0D));
		double step = maxDistance / steps;
		for (int i = 1; i <= steps; i++) {
			Vec3d point = eye.add(direction.multiply(step * i));
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static String describeDirection(Vec3d delta) {
		double angle = Math.toDegrees(Math.atan2(delta.x, delta.z));
		if (angle < 0.0D) {
			angle += 360.0D;
		}
		int index = MathHelper.floor((angle + 22.5D) / 45.0D) & 7;
		return COMPASS_POINTS[index];
	}
}

