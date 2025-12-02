package net.cyberpunk042.infection.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.network.ShieldFieldRemovePayload;
import net.cyberpunk042.network.ShieldFieldSpawnPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World.ExplosionSourceType;

public final class ShieldFieldService {

	/**
	 * Represents an active shield field protecting an area from infection.
	 */
	public static record ShieldField(long id, BlockPos center, double radius, long createdTick) {}

	private static final double SHIELD_FIELD_RADIUS = 12.0D;

	private final VirusWorldState host;
	private final Map<Long, ShieldField> activeShields = new HashMap<>();

	public ShieldFieldService(VirusWorldState host) {
		this.host = host;
	}

	public void tick() {
		ServerWorld world = host.world();
		if (activeShields.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<Long, ShieldField>> iterator = activeShields.entrySet().iterator();
		while (iterator.hasNext()) {
			ShieldField field = iterator.next().getValue();
			BlockPos center = field.center();
			if (!world.isChunkLoaded(ChunkPos.toLong(center))) {
				continue;
			}
			if (host.combat().isWithinAura(center)) {
				triggerFailure(world, center);
				continue;
			}
			if (!isAnchorIntact(world, center)) {
				iterator.remove();
				broadcastRemoval(world, field.id());
				world.playSound(null, center, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.8F);
				notifyStatus(world, center, false);
				continue;
			}
			world.spawnParticles(
					ParticleTypes.END_ROD,
					center.getX() + 0.5D,
					center.getY() + 1.2D,
					center.getZ() + 0.5D,
					4,
					0.25D,
					0.25D,
					0.25D,
					0.0D);
		}
	}

	/**
	 * Evaluates if a block position should have an active shield.
	 * @param world the world (required for event handlers that run outside tick)
	 * @param pos the position to evaluate
	 */
	public void evaluateCandidate(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		if (!state.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)) {
			deactivate(world, pos);
			return;
		}
		if (!isCocoonComplete(world, pos)) {
			deactivate(world, pos);
			return;
		}
		if (host.combat().isWithinAura(pos)) {
			triggerFailure(world, pos);
			return;
		}
		activate(world, pos);
	}

	/**
	 * Removes a shield at the given position.
	 * @param world the world (required for event handlers that run outside tick)
	 * @param pos the position to remove
	 */
	public void removeShieldAt(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return;
		}
		deactivate(world, pos);
	}

	public void purgeHostilesAround(List<BlockPos> centers, double radius) {
		ServerWorld world = host.world();
		if (centers.isEmpty()) {
			return;
		}
		Box bounds;
		Set<HostileEntity> victims = new HashSet<>();
		for (BlockPos center : centers) {
			bounds = new Box(center).expand(radius);
			victims.addAll(world.getEntitiesByClass(HostileEntity.class, bounds, Entity::isAlive));
		}
		if (victims.isEmpty()) {
			return;
		}
		for (HostileEntity mob : victims) {
			mob.kill(world);
		}
		world.playSound(
				null,
				centers.get(0),
				SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
				SoundCategory.HOSTILE,
				1.2F,
				0.4F);
	}

	public boolean isShielding(BlockPos pos) {
		if (activeShields.isEmpty()) {
			return false;
		}
		Vec3d sample = Vec3d.ofCenter(pos);
		for (ShieldField field : activeShields.values()) {
			double radius = field.radius();
			double radiusSq = radius * radius;
			if (field.center().toCenterPos().squaredDistanceTo(sample) <= radiusSq) {
				return true;
			}
		}
		return false;
	}

	public boolean isPlayerShielded(ServerPlayerEntity player) {
		return player != null && isShielding(player.getBlockPos());
	}

	public void sendSnapshots(ServerPlayerEntity player) {
		if (activeShields.isEmpty() || player == null) {
			return;
		}
		for (ShieldField field : activeShields.values()) {
			Vec3d center = Vec3d.ofCenter(field.center());
			ShieldFieldSpawnPayload payload = new ShieldFieldSpawnPayload(
					field.id(),
					center.x,
					center.y,
					center.z,
					(float) field.radius());
			host.collapseModule().watchdog().presentationService().sendPayload(player, payload);
		}
	}

	public List<ShieldField> snapshot() {
		return activeShields.isEmpty() ? List.of() : List.copyOf(activeShields.values());
	}

	public void restoreSnapshot(List<ShieldField> snapshot) {
		activeShields.clear();
		if (snapshot == null || snapshot.isEmpty()) {
			return;
		}
		for (ShieldField field : snapshot) {
			activeShields.put(field.id(), field);
		}
	}

	private void activate(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		if (activeShields.containsKey(key)) {
			return;
		}
		ShieldField field = new ShieldField(key, pos.toImmutable(), SHIELD_FIELD_RADIUS, world.getTime());
		activeShields.put(key, field);
		host.markDirty();
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.05F);
		broadcastSpawn(world, field);
		openSkyshaft(world, pos);
		notifyStatus(world, pos, true);
	}

	private void deactivate(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		ShieldField removed = activeShields.remove(key);
		if (removed == null) {
			return;
		}
		host.markDirty();
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.85F);
		broadcastRemoval(world, removed.id());
		notifyStatus(world, pos, false);
	}

	private void triggerFailure(ServerWorld world, BlockPos pos) {
		long key = pos.asLong();
		ShieldField removed = activeShields.remove(key);
		if (removed != null) {
			broadcastRemoval(world, removed.id());
			host.markDirty();
		}
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.7F);
		world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.8F, 0.4F);
		world.createExplosion(
				null,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				20.0F,
				ExplosionSourceType.NONE);
		world.breakBlock(pos, false);
		host.collapseModule().watchdog().presentationService().notifyShieldFailure(host.world(), pos);
	}

	private boolean isCocoonComplete(ServerWorld world, BlockPos center) {
		for (Direction direction : Direction.values()) {
			if (!world.getBlockState(center.offset(direction)).isOf(Blocks.OBSIDIAN)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAnchorIntact(ServerWorld world, BlockPos center) {
		BlockState state = world.getBlockState(center);
		if (!state.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)) {
			return false;
		}
		return isCocoonComplete(world, center);
	}

	private void notifyStatus(ServerWorld world, BlockPos pos, boolean active) {
		double radiusSq = (SHIELD_FIELD_RADIUS * SHIELD_FIELD_RADIUS) * 2.0D;
		host.collapseModule().watchdog().presentationService().notifyShieldStatus(host.world(), pos, active, radiusSq);
	}

	private void broadcastSpawn(ServerWorld world, ShieldField field) {
		Vec3d center = Vec3d.ofCenter(field.center());
		ShieldFieldSpawnPayload payload = new ShieldFieldSpawnPayload(field.id(), center.x, center.y, center.z, (float) field.radius());
		host.collapseModule().watchdog().presentationService().broadcastPayload(host.world(), payload);
	}

	private void broadcastRemoval(ServerWorld world, long id) {
		ShieldFieldRemovePayload payload = new ShieldFieldRemovePayload(id);
		host.collapseModule().watchdog().presentationService().broadcastPayload(host.world(), payload);
	}

	private void openSkyshaft(ServerWorld world, BlockPos center) {
		BlockPos.Mutable cursor = new BlockPos.Mutable();
		int ceiling = world.getBottomY() + world.getDimension().height();
		for (int y = center.getY() + 2; y < ceiling; y++) {
			cursor.set(center.getX(), y, center.getZ());
			if (!world.isChunkLoaded(ChunkPos.toLong(cursor))) {
				continue;
			}
			if (isShielding(cursor)) {
				continue;
			}
			BlockState state = world.getBlockState(cursor);
			if (state.isAir() || state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)) {
				continue;
			}
			if (state.getHardness(world, cursor) < 0.0F || state.isOf(Blocks.BEDROCK)) {
				continue;
			}
			world.setBlockState(cursor, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		world.spawnParticles(
				ParticleTypes.END_ROD,
				center.getX() + 0.5D,
				center.getY() + 1.2D,
				center.getZ() + 0.5D,
				30,
				0.35D,
				0.35D,
				0.35D,
				0.01D);
	}
}

