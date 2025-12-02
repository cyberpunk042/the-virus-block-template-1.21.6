package net.cyberpunk042.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock.ShapeType;
import net.cyberpunk042.growth.FieldProfile;
import net.cyberpunk042.growth.ExplosionProfile;
import net.cyberpunk042.growth.ForceProfile;
import net.cyberpunk042.growth.FuseProfile;
import net.cyberpunk042.growth.GlowProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.ParticleProfile;
import net.cyberpunk042.growth.scheduler.GrowthMutation;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.events.GrowthForceEvent;
import net.cyberpunk042.infection.events.GrowthForceEvent.ForceType;
import net.cyberpunk042.infection.events.GrowthFuseEvent;
import net.cyberpunk042.infection.events.GrowthBeamEvent;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.network.GrowthBeamPayload;
import net.cyberpunk042.network.GrowthRingFieldPayload;
import net.cyberpunk042.registry.ModBlockEntities;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Runtime state for {@link ProgressiveGrowthBlock}. Handles scale animation,
 * fuse timing, force fields, and touch damage. Rendering will read the synced
 * scale + profiles client-side.
 */
public class ProgressiveGrowthBlockEntity extends BlockEntity {
	private static final double WOBBLE_AMPLITUDE_XZ = 0.08D;
	private static final double WOBBLE_AMPLITUDE_Y = 0.04D;
	private static final double WOBBLE_SPEED_X = 0.23D;
	private static final double WOBBLE_SPEED_Y = 0.17D;
	private static final double WOBBLE_SPEED_Z = 0.19D;
	private static final double MIN_SCALE_SPAN = 1.0E-4D;
	private Identifier definitionId = GrowthBlockDefinition.defaults().id();
	private double currentScale = GrowthBlockDefinition.defaults().startScale();
	private double previousScale = currentScale;
	private double targetScale = GrowthBlockDefinition.defaults().targetScale();
	private boolean fuseArmed;
	private int fuseTicks;
	private int collapseTicks;
	private int collapseDuration;
	private double collapseStartScale;
	private double collapseEndScale;
	private int scaleCooldown;
	private int pullCooldown;
	private int pushCooldown;
	private int ambientCooldown;
	private int ambientSoundCooldown;
	private double wobbleOffsetX;
	private double wobbleOffsetY;
	private double wobbleOffsetZ;
	private VoxelShape outlineShape = VoxelShapes.fullCube();
	private VoxelShape collisionShape = VoxelShapes.empty();
	private final Object2LongMap<UUID> touchCooldowns = new Object2LongOpenHashMap<>();
	private final Object2LongMap<UUID> forceDamageCooldowns = new Object2LongOpenHashMap<>();
	private boolean lastGrowthEnabled = GrowthBlockDefinition.defaults().growthEnabled();
	private boolean lastWobbleEnabled = GrowthBlockDefinition.defaults().isWobbly();
	private boolean lastHasCollision = GrowthBlockDefinition.defaults().hasCollision();
	private int remainingCharges = ExplosionProfile.defaults().sanitizedCharges();
	private int burstExplosionsRemaining;
	private int burstDelayTicks;
	private int burstDelayInterval;
	private float burstRadius;
	private boolean burstCausesFire;
	private boolean burstBreaksBlocks;
	private double burstMaxDamage;
	private double burstDamageScaling;
	private GrowthOverrides overrides = GrowthOverrides.empty();

	public ProgressiveGrowthBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PROGRESSIVE_GROWTH, pos, state);
		touchCooldowns.defaultReturnValue(0L);
		forceDamageCooldowns.defaultReturnValue(0L);
		this.previousScale = currentScale;
		GrowthBlockDefinition definition = resolveDefinition();
		resetTransientState(definition);
		rebuildShapes(definition);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, ProgressiveGrowthBlockEntity entity) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		entity.tickServer(serverWorld);
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, ProgressiveGrowthBlockEntity entity) {
		entity.tickClient();
	}

	private void tickServer(ServerWorld world) {
		GrowthBlockDefinition definition = resolveDefinition();
		boolean dirty = false;
		boolean shapeDirty = false;
		boolean growthEnabled = definition.growthEnabled();
		boolean allowScaleTick = growthEnabled || fuseArmed;
		boolean hasCollision = definition.hasCollision();

		if (!growthEnabled && lastGrowthEnabled && !fuseArmed) {
			previousScale = currentScale;
			scaleCooldown = definition.sanitizedRate();
		}

		if (allowScaleTick) {
			if (scaleCooldown-- <= 0) {
				previousScale = currentScale;
				double before = currentScale;
				stepScaleTowardsTarget(definition);
				scaleCooldown = definition.sanitizedRate();
				if (Math.abs(before - currentScale) > 1.0E-4) {
					dirty = true;
					shapeDirty = true;
				}
			}
		} else {
			scaleCooldown = definition.sanitizedRate();
		}

		FuseProfile fuseProfile = resolveFuseProfile();
		if (!definition.hasFuse()) {
			if (fuseArmed || isBurstActive()) {
				disarmFuse();
				dirty = true;
			}
		} else if (!fuseArmed && !isBurstActive() && remainingCharges > 0
				&& fuseProfile.trigger() == FuseProfile.Trigger.AUTO
				&& shouldAutoArm(definition, fuseProfile)) {
			armFuse(world, definition, fuseProfile);
			dirty = true;
		}

		FuseResult fuseResult = fuseArmed ? tickFuse(world, definition, fuseProfile) : FuseResult.NONE;
		if (fuseResult.removed()) {
			return;
		}
		if (fuseResult.dirty()) {
			dirty = true;
		}
		if (fuseResult.shapeDirty()) {
			shapeDirty = true;
		}

		if (growthEnabled) {
			if (definition.isPulling() && definition.pullingForce() > 0.0D) {
				ForceProfile profile = resolveForceProfile(definition.pullProfileId(), false);
				if (pullCooldown-- <= 0) {
					applyForce(world, definition, profile, definition.pullingForce(), true);
					pullCooldown = profile.sanitizedInterval();
				}
			}
			if (definition.isPushing() && definition.pushingForce() > 0.0D) {
				ForceProfile profile = resolveForceProfile(definition.pushProfileId(), true);
				if (pushCooldown-- <= 0) {
					applyForce(world, definition, profile, definition.pushingForce(), false);
					pushCooldown = profile.sanitizedInterval();
				}
			}
		} else {
			pullCooldown = Math.max(0, Math.min(pullCooldown, definition.sanitizedRate()));
			pushCooldown = Math.max(0, Math.min(pushCooldown, definition.sanitizedRate()));
		}

		boolean wobbleEnabled = definition.isWobbly();
		if (wobbleEnabled != lastWobbleEnabled) {
			wobbleOffsetX = 0.0D;
			wobbleOffsetY = 0.0D;
			wobbleOffsetZ = 0.0D;
			shapeDirty = true;
			lastWobbleEnabled = wobbleEnabled;
		}

		if (hasCollision != lastHasCollision) {
			shapeDirty = true;
			lastHasCollision = hasCollision;
		}

		if (updateWobble(definition, world.getTime())) {
			dirty = true;
			shapeDirty = true;
		}

		if (shapeDirty) {
			rebuildShapes(definition);
		}

		applyVolumeTouchDamage(world, definition);
		if (definition.doesDestruction()) {
			destroyIntersectingBlocks(world, definition);
		}
		emitAmbientParticles(world, definition);
		if (tickPendingBursts(world, definition)) {
			return;
		}

		lastGrowthEnabled = growthEnabled;

		if (dirty) {
			markDirty();
			sync();
		}
	}

	private void tickClient() {
		GrowthBlockDefinition definition = resolveDefinition();
		previousScale = currentScale;
		double before = currentScale;
		stepScaleTowardsTarget(definition);
		boolean shapeDirty = Math.abs(before - currentScale) > 1.0E-4;
		boolean hasCollision = definition.hasCollision();
		if (hasCollision != lastHasCollision) {
			lastHasCollision = hasCollision;
			shapeDirty = true;
		}
		if (updateWobble(definition, world != null ? world.getTime() : 0L)) {
			shapeDirty = true;
		}
		if (shapeDirty) {
			rebuildShapes(definition);
		}
	}

	private void stepScaleTowardsTarget(GrowthBlockDefinition definition) {
		double min = definition.minScale();
		double max = sanitizedMaxScale(definition);
		double diff = targetScale - currentScale;
		if (Math.abs(diff) < 1.0E-4) {
			currentScale = MathHelper.clamp(currentScale, min, max);
			return;
		}
		double maxStep = 0.05D * definition.clampedRateScale();
		double step = Math.copySign(Math.min(Math.abs(diff), maxStep), diff);
		currentScale = MathHelper.clamp(currentScale + step, min, max);
	}

	private void armFuse(ServerWorld world, GrowthBlockDefinition definition, FuseProfile profile) {
		fuseArmed = true;
		fuseTicks = profile.sanitizedExplosionDelay();
		collapseDuration = profile.sanitizedShellCollapse();
		collapseTicks = collapseDuration;
		collapseStartScale = currentScale;
		collapseEndScale = resolveCollapseTarget(definition, profile);
		postFuseEvent(world, GrowthFuseEvent.Stage.ARMED, fuseTicks);
	}

	private FuseResult tickFuse(ServerWorld world, GrowthBlockDefinition definition, FuseProfile profile) {
		if (!fuseArmed) {
			return FuseResult.NONE;
		}
		if (fuseTicks-- <= 0) {
			boolean removed = triggerDetonation(world, definition);
			return new FuseResult(true, true, removed);
		}
		boolean shapeDirty = false;
		if (fuseTicks % Math.max(1, profile.sanitizedPulseInterval()) == 0) {
			emitFusePulse(world, profile);
		}
		if (collapseTicks > 0 && collapseDuration > 0) {
			double fraction = 1.0D - (double) collapseTicks / (double) collapseDuration;
			double collapseTarget = MathHelper.lerp(fraction, collapseStartScale, collapseEndScale);
			targetScale = Math.min(targetScale, collapseTarget);
			collapseTicks--;
			shapeDirty = true;
		}
		return new FuseResult(true, shapeDirty, false);
	}

	private boolean triggerDetonation(ServerWorld world, GrowthBlockDefinition definition) {
		fuseArmed = false;
		fuseTicks = 0;
		collapseTicks = 0;
		collapseDuration = 0;
		startBurstSequence(definition, resolveExplosionProfile());
		postFuseEvent(world, GrowthFuseEvent.Stage.DETONATED, 0);
		return tickPendingBursts(world, definition);
	}

	private void emitFusePulse(ServerWorld world, FuseProfile profile) {
		ParticleEffect effect = resolveParticle(profile.particleId());
		if (effect != null) {
			world.spawnParticles(effect,
					pos.getX() + 0.5D,
					pos.getY() + 0.8D,
					pos.getZ() + 0.5D,
					4,
					0.15D,
					0.15D,
					0.15D,
					0.01D);
		}
		playSound(world, profile.soundId(), 0.7F, 1.0F);
		postFuseEvent(world, GrowthFuseEvent.Stage.PULSE, fuseTicks);
	}

	private void applyForce(ServerWorld world, GrowthBlockDefinition definition, ForceProfile profile, double forceScale, boolean pulling) {
		double progress = growthProgress(definition);
		if (progress < profile.clampedStartProgress() || progress > profile.clampedEndProgress()) {
			return;
		}

		double baseRadius = Math.max(0.5D, profile.clampedRadius() * forceScale);
		Box box = new Box(pos).expand(baseRadius);
		Vec3d center = Vec3d.ofCenter(pos);
		List<RingBand> ringBands = profile.hasRingConfig() ? buildRingBands(profile) : Collections.emptyList();
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
				entity -> entity.isAlive() && !entity.isSpectator());
		if (!targets.isEmpty()) {
			for (LivingEntity living : targets) {
				Vec3d offset = living.getPos().subtract(center);
				double distance = Math.max(0.001D, offset.length());
				if (distance > baseRadius) {
					continue;
				}
				double normalized = 1.0D - Math.min(1.0D, distance / baseRadius);
				double impulse = computeImpulse(profile, forceScale, normalized, distance, baseRadius);
				if (impulse <= 0.0D) {
					continue;
				}
				Vec3d radial = offset.normalize();
				double directionSign = pulling ? -1.0D : 1.0D;
				double impulseScale = 1.0D;
				if (!ringBands.isEmpty()) {
					RingBand band = findNearestRingBand(ringBands, distance);
					RingForceDecision decision = band != null ? evaluateRingDecision(profile, band, distance) : null;
					if (decision == null) {
						continue;
					}
					directionSign = decision.directionSign();
					impulseScale = decision.strengthMultiplier();
				}
				Vec3d direction = radial.multiply(directionSign);
				double verticalBoost = profile.verticalBoost() * Math.pow(normalized, 0.65D);
				Vec3d push = direction.multiply(impulse * impulseScale);
				living.addVelocity(push.x, verticalBoost, push.z);
				living.velocityModified = true;
				tryApplyForceDamage(world, living, profile);
				if (profile.guardianBeams()) {
					postBeamEvent(world, living, profile, pulling);
				}
			}
		}
		if (!ringBands.isEmpty()) {
			sendRingFieldVisuals(world, ringBands, profile);
		}
		spawnForceParticles(world, profile);
		playSound(world, profile.soundId(), 0.9F, pulling ? 0.8F : 1.1F);
		postForceEvent(world, profile, pulling ? GrowthForceEvent.ForceType.PULL : GrowthForceEvent.ForceType.PUSH, baseRadius, profile.clampedStrength() * forceScale);
	}

	private void tryApplyForceDamage(ServerWorld world, LivingEntity living, ForceProfile profile) {
		double damage = profile.clampedImpactDamage();
		if (damage <= 0.0D) {
			return;
		}
		int cooldown = profile.sanitizedImpactCooldown();
		if (cooldown <= 0) {
			cooldown = 1;
		}
		UUID id = living.getUuid();
		long now = world.getTime();
		long nextAllowed = forceDamageCooldowns.getLong(id);
		if (nextAllowed > now) {
			return;
		}
		living.damage(world, world.getDamageSources().magic(), (float) damage);
		forceDamageCooldowns.put(id, now + cooldown);
	}

	private double computeImpulse(ForceProfile profile, double forceScale, double normalized, double distance, double radius) {
		double strength = profile.clampedStrength() * forceScale;
		double falloff = Math.pow(normalized, profile.clampedFalloff());
		double edgeFactor = 1.0D;
		double edgeFalloff = profile.clampedEdgeFalloff();
		if (edgeFalloff > 0.0D) {
			double edgeDistance = Math.max(0.0D, (radius - distance) / (radius * edgeFalloff));
			edgeFactor = Math.min(1.0D, edgeDistance);
		}
		return strength * falloff * edgeFactor;
	}

	private List<RingBand> buildRingBands(ForceProfile profile) {
		if (!profile.hasRingConfig()) {
			return Collections.emptyList();
		}
		int count = profile.sanitizedRingCount();
		if (count <= 0) {
			return Collections.emptyList();
		}
		double base = Math.max(0.25D, profile.baseRingRadius());
		double spacing = Math.max(0.0D, profile.sanitizedRingSpacing());
		double width = Math.max(0.05D, profile.sanitizedRingWidth());
		List<Identifier> fields = profile.ringFieldProfiles();
		List<RingBand> bands = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			double radius = base + (spacing * i);
			double inner = Math.max(0.0D, radius - width * 0.5D);
			double outer = radius + width * 0.5D;
			Identifier field = i < fields.size() ? fields.get(i) : null;
			bands.add(new RingBand(radius, inner, outer, field));
		}
		return bands;
	}

	@Nullable
	private RingBand findNearestRingBand(List<RingBand> bands, double distance) {
		if (bands.isEmpty()) {
			return null;
		}
		RingBand nearest = null;
		double best = Double.MAX_VALUE;
		for (RingBand band : bands) {
			double diff = Math.abs(distance - band.radius());
			if (diff < best) {
				best = diff;
				nearest = band;
			}
		}
		return nearest;
	}

	@Nullable
	private RingForceDecision evaluateRingDecision(ForceProfile profile, RingBand band, double distance) {
		if (band == null) {
			return null;
		}
		double inner = band.inner();
		double outer = band.outer();
		double strength = profile.clampedRingStrength();
		return switch (profile.ringBehavior()) {
			case KEEP_ON_RING -> {
				if (distance >= inner && distance <= outer) {
					yield null;
				}
				double delta = distance - band.radius();
				double width = Math.max(outer - inner, 0.001D);
				double normalized = MathHelper.clamp(Math.abs(delta) / width, 0.25D, 1.0D);
				yield new RingForceDecision(delta < 0.0D ? 1.0D : -1.0D, strength * normalized);
			}
			case KEEP_INSIDE -> {
				if (distance < inner) {
					yield new RingForceDecision(1.0D, strength);
				}
				if (distance > outer) {
					yield new RingForceDecision(-1.0D, strength);
				}
				yield null;
			}
			case KEEP_OUTSIDE -> {
				if (distance <= outer) {
					yield new RingForceDecision(1.0D, strength);
				}
				yield null;
			}
			case NONE -> null;
			default -> null;
		};
	}

	private void sendRingFieldVisuals(ServerWorld world, List<RingBand> bands, ForceProfile profile) {
		if (bands.isEmpty()) {
			return;
		}
		List<GrowthRingFieldPayload.RingEntry> entries = new ArrayList<>();
		int duration = Math.max(10, profile.sanitizedInterval());
		for (RingBand band : bands) {
			if (band.fieldProfileId() == null) {
				continue;
			}
			entries.add(new GrowthRingFieldPayload.RingEntry(
					band.fieldProfileId(),
					(float) band.radius(),
					(float) Math.max(0.05D, band.outer() - band.inner()),
					duration));
		}
		if (entries.isEmpty()) {
			return;
		}
		GrowthRingFieldPayload payload = new GrowthRingFieldPayload(world.getRegistryKey(), pos.toImmutable(), entries);
		for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private void applyVolumeTouchDamage(ServerWorld world, GrowthBlockDefinition definition) {
		double damage = definition.touchDamage();
		if (damage <= 0.0D) {
			return;
		}
		Box damageBox = outlineShape.getBoundingBox().offset(pos);
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, damageBox, LivingEntity::isAlive);
		if (targets.isEmpty()) {
			return;
		}
		for (LivingEntity living : targets) {
			damageLivingEntity(world, living, definition);
		}
	}

	private void destroyIntersectingBlocks(ServerWorld world, GrowthBlockDefinition definition) {
		Box box = outlineShape.getBoundingBox().offset(pos);
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minY = MathHelper.floor(box.minY);
		int maxY = MathHelper.ceil(box.maxY);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					mutable.set(x, y, z);
					if (mutable.equals(pos)) {
						continue;
					}
					BlockState state = world.getBlockState(mutable);
					if (state.isAir()) {
						continue;
					}
					if (state.getHardness(world, mutable) < 0.0F) {
						continue;
					}
					world.breakBlock(mutable, false);
				}
			}
		}
	}

	private void applyExplosionDamage(ServerWorld world, double radius, double maxDamage, double scaling) {
		if (radius <= 0.0D || maxDamage <= 0.0D) {
			return;
		}
		Box box = new Box(pos).expand(radius);
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive);
		if (targets.isEmpty()) {
			return;
		}
		Vec3d center = Vec3d.ofCenter(pos);
		for (LivingEntity living : targets) {
			double distance = living.getPos().distanceTo(center);
			if (distance > radius) {
				continue;
			}
			double normalized = Math.max(0.0D, 1.0D - distance / radius);
			double falloff = Math.pow(normalized, scaling);
			double damageValue;
			if (Double.isInfinite(maxDamage)) {
				damageValue = falloff <= 0.0D ? 0.0D : Double.POSITIVE_INFINITY;
			} else {
				damageValue = maxDamage * falloff;
			}
			if (damageValue <= 0.0D) {
				continue;
			}
			float damageAmount = damageValue >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) damageValue;
			living.damage(world, world.getDamageSources().explosion(null, null), damageAmount);
		}
	}

	private void startBurstSequence(GrowthBlockDefinition definition, ExplosionProfile explosion) {
		burstExplosionsRemaining = explosion.sanitizedAmount();
		burstDelayInterval = explosion.sanitizedAmountDelay();
		burstDelayTicks = 0;
		burstRadius = explosion.sanitizedRadius();
		burstCausesFire = explosion.causesFire();
		burstBreaksBlocks = definition.doesDestruction() && explosion.breaksBlocks();
		burstMaxDamage = explosion.sanitizedMaxDamage();
		burstDamageScaling = explosion.clampedDamageScaling();
	}

	private boolean tickPendingBursts(ServerWorld world, GrowthBlockDefinition definition) {
		if (!isBurstActive()) {
			return false;
		}
		if (burstDelayTicks > 0) {
			burstDelayTicks--;
			return false;
		}
		executeBurstExplosion(world);
		if (--burstExplosionsRemaining > 0) {
			burstDelayTicks = burstDelayInterval;
			return false;
		}
		return finalizeBurst(world, definition);
	}

	private void executeBurstExplosion(ServerWorld world) {
		World.ExplosionSourceType type = burstBreaksBlocks
				? World.ExplosionSourceType.BLOCK
				: World.ExplosionSourceType.NONE;
		world.createExplosion(
				null,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				burstRadius,
				burstCausesFire,
				type);
		applyExplosionDamage(world, burstRadius, burstMaxDamage, burstDamageScaling);
	}

	private boolean finalizeBurst(ServerWorld world, GrowthBlockDefinition definition) {
		remainingCharges = Math.max(0, remainingCharges - 1);
		clearBurstState();
		if (remainingCharges <= 0) {
			world.removeBlock(pos, false);
			return true;
		}
		markDirty();
		sync();
		return false;
	}

	private boolean isBurstActive() {
		return burstExplosionsRemaining > 0;
	}

	private void clearBurstState() {
		burstExplosionsRemaining = 0;
		burstDelayTicks = 0;
		burstDelayInterval = 0;
		burstRadius = 0.0F;
		burstCausesFire = false;
		burstBreaksBlocks = false;
		burstMaxDamage = 0.0D;
		burstDamageScaling = 1.0D;
	}

	private void emitAmbientParticles(ServerWorld world, GrowthBlockDefinition definition) {
		ParticleProfile profile = resolveParticleProfile();
		if (profile == null) {
			return;
		}
		boolean spawnParticles = profile.sanitizedCount() > 0 && profile.particleId() != null;
		boolean playSound = profile.soundId() != null;
		if (!spawnParticles && !playSound) {
			ambientCooldown = 0;
			ambientSoundCooldown = 0;
			return;
		}

		if (spawnParticles) {
			if (ambientCooldown-- <= 0) {
				ambientCooldown = profile.sanitizedInterval();
				spawnConfiguredParticles(world, profile);
			}
		} else {
			ambientCooldown = 0;
		}

		if (playSound) {
			if (ambientSoundCooldown-- <= 0) {
				playSound(world, profile.soundId(), 0.6F, 1.0F);
				ambientSoundCooldown = profile.sanitizedSoundInterval();
			}
		} else {
			ambientSoundCooldown = 0;
		}
	}

	private void spawnConfiguredParticles(ServerWorld world, ParticleProfile profile) {
		ParticleEffect effect = resolveParticle(profile.particleId());
		if (effect == null) {
			return;
		}
		int count = profile.sanitizedCount();
		if (count <= 0) {
			return;
		}
		double scaleFactor = profile.followScale() ? Math.max(0.1D, currentScale) : 1.0D;
		Vec3d base = Vec3d.ofCenter(pos)
				.add(profile.offsetX(), profile.offsetY(), profile.offsetZ());
		for (int i = 0; i < count; i++) {
			Vec3d offset = computeParticleOffset(world, profile, scaleFactor);
			Vec3d spawn = base.add(offset);
			world.spawnParticles(effect,
					spawn.x,
					spawn.y,
					spawn.z,
					1,
					0.0D,
					0.0D,
					0.0D,
					profile.sanitizedSpeed());
		}
	}

	private Vec3d computeParticleOffset(ServerWorld world, ParticleProfile profile, double scaleFactor) {
		double radius = profile.clampedRadius() * scaleFactor;
		double height = profile.clampedHeight() * scaleFactor;
		double jitterX = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterX();
		double jitterY = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterY();
		double jitterZ = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterZ();
		Vec3d base = switch (profile.shape()) {
			case SHELL -> randomDirection(world).multiply(radius);
			case RING -> {
				double angle = world.random.nextDouble() * Math.PI * 2.0D;
				double y = (world.random.nextDouble() - 0.5D) * height;
				yield new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
			}
			case COLUMN -> {
				double angle = world.random.nextDouble() * Math.PI * 2.0D;
				double r = world.random.nextDouble() * radius;
				double y = (world.random.nextDouble() - 0.5D) * height;
				yield new Vec3d(Math.cos(angle) * r, y, Math.sin(angle) * r);
			}
			case SPHERE -> randomPointInSphere(world, radius);
			default -> randomPointInSphere(world, radius);
		};
		return base.add(jitterX, jitterY, jitterZ);
	}

	private Vec3d randomPointInSphere(ServerWorld world, double radius) {
		for (int attempts = 0; attempts < 8; attempts++) {
			double x = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
			double y = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
			double z = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
			if (x * x + y * y + z * z <= radius * radius) {
				return new Vec3d(x, y, z);
			}
		}
		return randomDirection(world).multiply(radius);
	}

	private Vec3d randomDirection(ServerWorld world) {
		double theta = world.random.nextDouble() * Math.PI * 2.0D;
		double phi = Math.acos(2.0D * world.random.nextDouble() - 1.0D);
		double sinPhi = Math.sin(phi);
		return new Vec3d(
				sinPhi * Math.cos(theta),
				Math.cos(phi),
				sinPhi * Math.sin(theta));
	}

	private boolean updateWobble(GrowthBlockDefinition definition, long time) {
		Vec3d sample = sampleWobble(time, definition);
		boolean changed = Math.abs(sample.x - wobbleOffsetX) > 1.0E-4
				|| Math.abs(sample.y - wobbleOffsetY) > 1.0E-4
				|| Math.abs(sample.z - wobbleOffsetZ) > 1.0E-4;
		if (changed) {
			wobbleOffsetX = sample.x;
			wobbleOffsetY = sample.y;
			wobbleOffsetZ = sample.z;
		}
		return changed;
	}

	private Vec3d sampleWobble(double time, GrowthBlockDefinition definition) {
		if (!definition.isWobbly()) {
			return Vec3d.ZERO;
		}
		double scaleFactor = MathHelper.clamp(currentScale, 0.2D, 1.4D);
		double amplitudeScale = Math.max(0.35D, 1.1D - scaleFactor * 0.35D);
		double x = Math.sin(time * WOBBLE_SPEED_X) * WOBBLE_AMPLITUDE_XZ * amplitudeScale;
		double z = Math.cos(time * WOBBLE_SPEED_Z) * WOBBLE_AMPLITUDE_XZ * amplitudeScale;
		double y = Math.sin(time * WOBBLE_SPEED_Y) * WOBBLE_AMPLITUDE_Y * amplitudeScale;
		return new Vec3d(x, y, z);
	}

	public Vec3d getRenderWobble(GrowthBlockDefinition definition, float tickDelta) {
		if (world == null || !definition.isWobbly()) {
			return Vec3d.ZERO;
		}
		double time = world.getTime() + tickDelta;
		return sampleWobble(time, definition);
	}

	private void damageLivingEntity(ServerWorld world, LivingEntity living, GrowthBlockDefinition definition) {
		double damage = definition.touchDamage();
		if (damage <= 0.0D) {
			return;
		}
		long now = world.getTime();
		long ready = touchCooldowns.getOrDefault(living.getUuid(), 0L);
		if (now < ready) {
			return;
		}
		touchCooldowns.put(living.getUuid(), now + 10L);
		living.damage(world, world.getDamageSources().magic(), (float) damage);
	}

	private boolean matchesManualTrigger(FuseProfile profile, ManualFuseCause cause) {
		return switch (profile.trigger()) {
			case RIGHT_CLICK, ITEM_USE -> cause == ManualFuseCause.INTERACT;
			case ATTACK -> cause == ManualFuseCause.ATTACK;
			default -> false;
		};
	}

	private boolean validateArmItem(FuseProfile profile, ItemStack stack) {
		boolean needsItem = profile.requiresItem() || profile.trigger() == FuseProfile.Trigger.ITEM_USE;
		if (!needsItem) {
			return true;
		}
		if (stack.isEmpty()) {
			return false;
		}
		if (profile.allowedItems().isEmpty()) {
			return true;
		}
		Identifier heldId = Registries.ITEM.getId(stack.getItem());
		return profile.allowedItems().contains(heldId);
	}

	private void consumeArmItem(PlayerEntity player, ItemStack stack, FuseProfile profile) {
		boolean needsItem = profile.requiresItem() || profile.trigger() == FuseProfile.Trigger.ITEM_USE;
		if (!needsItem || stack.isEmpty()) {
			return;
		}
		if (profile.consumeItem()) {
			stack.decrement(1);
		} else if (stack.isDamageable()) {
			stack.damage(1, player);
		}
	}

	private void spawnForceParticles(ServerWorld world, ForceProfile profile) {
		ParticleEffect effect = resolveParticle(profile.particleId());
		if (effect == null || profile.particleCount() <= 0) {
			return;
		}
		world.spawnParticles(effect,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				profile.particleCount(),
				0.4D,
				0.4D,
				0.4D,
				profile.particleSpeed());
	}

	private void playSound(ServerWorld world, Identifier soundId, float volume, float pitch) {
		if (soundId == null) {
			return;
		}
		SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
		if (sound == null) {
			return;
		}
		world.playSound(null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
	}

	public void onEntityCollision(Entity entity) {
		if (!(entity instanceof LivingEntity living) || world == null || world.isClient) {
			return;
		}
		GrowthBlockDefinition definition = resolveDefinition();
		damageLivingEntity((ServerWorld) world, living, definition);
	}

	public boolean canManualFusePreview(ManualFuseCause cause, ItemStack stack) {
		GrowthBlockDefinition definition = resolveDefinition();
		if (!definition.hasFuse() || fuseArmed || remainingCharges <= 0 || isBurstActive()) {
			return false;
		}
		FuseProfile profile = resolveFuseProfile();
		return matchesManualTrigger(profile, cause) && validateArmItem(profile, stack);
	}

	public boolean handleManualFuse(ServerWorld world, PlayerEntity player, Hand hand, ManualFuseCause cause) {
		GrowthBlockDefinition definition = resolveDefinition();
		if (!definition.hasFuse() || fuseArmed || remainingCharges <= 0 || isBurstActive()) {
			return false;
		}
		FuseProfile profile = resolveFuseProfile();
		if (!matchesManualTrigger(profile, cause)) {
			return false;
		}
		ItemStack stack = hand != null ? player.getStackInHand(hand) : ItemStack.EMPTY;
		if (!validateArmItem(profile, stack)) {
			return false;
		}
		consumeArmItem(player, stack, profile);
		armFuse(world, definition, profile);
		markDirty();
		sync();
		return true;
	}

	public VoxelShape shape(ShapeType type) {
		return switch (type) {
			case COLLISION -> collisionShape;
			default -> outlineShape;
		};
	}

	private void rebuildShapes(GrowthBlockDefinition definition) {
		double clamped = MathHelper.clamp(currentScale, 0.05D, 1.8D);
		double half = clamped * 0.5D;
		double minX = Math.max(0.0D, 0.5D - half);
		double maxX = Math.min(1.0D, 0.5D + half);
		double minZ = minX;
		double maxZ = maxX;
		double minY = 0.0D;
		double maxY = Math.min(1.0D, 0.2D + clamped);

		double shiftX = MathHelper.clamp(wobbleOffsetX, -minX, 1.0D - maxX);
		double shiftZ = MathHelper.clamp(wobbleOffsetZ, -minZ, 1.0D - maxZ);
		double shiftY = MathHelper.clamp(wobbleOffsetY, -minY, 1.0D - maxY);

		minX += shiftX;
		maxX += shiftX;
		minZ += shiftZ;
		maxZ += shiftZ;
		minY += shiftY;
		maxY += shiftY;

		this.outlineShape = VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
		this.collisionShape = definition.hasCollision() ? outlineShape : VoxelShapes.empty();
	}

	public Identifier getDefinitionId() {
		return definitionId;
	}

	public void setDefinitionId(Identifier id) {
		if (id == null || id.equals(this.definitionId)) {
			return;
		}
		this.definitionId = id;
		GrowthBlockDefinition definition = resolveDefinition();
		resetTransientState(definition);
		rebuildShapes(definition);
		markDirty();
		sync();
	}

	public void replaceOverrides(GrowthOverrides newOverrides) {
		this.overrides = newOverrides == null ? GrowthOverrides.empty() : newOverrides;
		GrowthBlockDefinition definition = resolveDefinition();
		rebuildShapes(definition);
		lastGrowthEnabled = definition.growthEnabled();
		lastWobbleEnabled = definition.isWobbly();
		lastHasCollision = definition.hasCollision();
		markDirty();
		sync();
	}

	public GrowthOverrides overridesSnapshot() {
		if (overrides == null || overrides.isEmpty()) {
			return GrowthOverrides.empty();
		}
		return GrowthOverrides.fromNbt(overrides.toNbt());
	}

	public boolean applyMutation(GrowthMutation mutation) {
		if (mutation == null || mutation.isEmpty()) {
			return false;
		}
		if (overrides == null) {
			overrides = GrowthOverrides.empty();
		}
		boolean changed = overrides.applyMutation(mutation);
		if (!changed) {
			return false;
		}
		GrowthBlockDefinition definition = resolveDefinition();
		rebuildShapes(definition);
		lastGrowthEnabled = definition.growthEnabled();
		lastWobbleEnabled = definition.isWobbly();
		lastHasCollision = definition.hasCollision();
		markDirty();
		sync();
		return true;
	}

	private GrowthBlockDefinition resolveDefinition() {
		GrowthRegistry registry = registry();
		GrowthBlockDefinition definition;
		if (registry == null) {
			definition = GrowthBlockDefinition.defaults();
		} else {
			definition = registry.definition(definitionId);
		}
		if (overrides != null && !overrides.isEmpty()) {
			definition = overrides.apply(definition);
		}
		double min = definition.minScale();
		double max = sanitizedMaxScale(definition);
		targetScale = MathHelper.clamp(targetScale, min, max);
		currentScale = MathHelper.clamp(currentScale, min, max);
		return definition;
	}

	private double growthProgress(GrowthBlockDefinition definition) {
		double min = definition.minScale();
		double max = sanitizedMaxScale(definition);
		double span = Math.max(MIN_SCALE_SPAN, max - min);
		return MathHelper.clamp((currentScale - min) / span, 0.0D, 1.0D);
	}

	private double resolveCollapseTarget(GrowthBlockDefinition definition, FuseProfile profile) {
		return profile.collapseTargetScaleOrDefault(definition);
	}

	private boolean shouldAutoArm(GrowthBlockDefinition definition, FuseProfile profile) {
		if (remainingCharges <= 0 || isBurstActive() || profile.trigger() != FuseProfile.Trigger.AUTO) {
			return false;
		}
		return growthProgress(definition) >= profile.clampedAutoProgress();
	}

	private ForceProfile resolveForceProfile(@Nullable Identifier id, boolean push) {
		GrowthRegistry registry = registry();
		ForceProfile fallback = push ? ForceProfile.defaultsPush() : ForceProfile.defaultsPull();
		if (registry == null) {
			return fallback;
		}
		return registry.forceProfile(id, fallback);
	}

	public FieldProfile resolveFieldProfile() {
		GrowthRegistry registry = registry();
		if (registry == null) {
			return FieldProfile.defaults();
		}
		return registry.fieldProfile(resolveDefinition().fieldProfileId());
	}

	public FuseProfile resolveFuseProfile() {
		GrowthRegistry registry = registry();
		if (registry == null) {
			return FuseProfile.defaults();
		}
		return registry.fuseProfile(resolveDefinition().fuseProfileId());
	}

	public ExplosionProfile resolveExplosionProfile() {
		GrowthRegistry registry = registry();
		if (registry == null) {
			return ExplosionProfile.defaults();
		}
		return registry.explosionProfile(resolveDefinition().explosionProfileId());
	}

	public GlowProfile resolveGlowProfile() {
		GrowthRegistry registry = registry();
		if (registry == null) {
			return GlowProfile.defaults();
		}
		return registry.glowProfile(resolveDefinition().glowProfileId());
	}

	public GrowthBlockDefinition definitionSnapshot() {
		return resolveDefinition();
	}

	public float getRenderScale(float tickDelta) {
		return (float) MathHelper.lerp(tickDelta, previousScale, currentScale);
	}

	public boolean isFuseArmed() {
		return fuseArmed;
	}

	public int getFuseTicks() {
		return fuseTicks;
	}

	@Nullable
	private ParticleEffect resolveParticle(@Nullable Identifier id) {
		if (id == null) {
			return null;
		}
		if (!Registries.PARTICLE_TYPE.containsId(id)) {
			return null;
		}
		ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
		return type instanceof ParticleEffect effect ? effect : null;
	}

	private ParticleProfile resolveParticleProfile() {
		GrowthRegistry registry = registry();
		if (registry == null) {
			return ParticleProfile.defaults();
		}
		return registry.particleProfile(resolveDefinition().particleProfileId());
	}

	private void postForceEvent(ServerWorld world, ForceProfile profile, ForceType type, double radius, double strength) {
		VirusWorldState state = VirusWorldState.get(world);
		state.orchestrator().services().effectBus().post(new GrowthForceEvent(world, pos.toImmutable(), type, radius, strength, profile));
	}

	private void postBeamEvent(ServerWorld world, LivingEntity target, ForceProfile profile, boolean pulling) {
		VirusWorldState state = VirusWorldState.get(world);
		Vec3d targetPos = target.getBoundingBox().getCenter();
		float[] color = profile.beamColorFloats();
		int duration = Math.max(10, profile.sanitizedInterval());
		state.orchestrator().services().effectBus().post(new GrowthBeamEvent(
				world,
				pos.toImmutable(),
				target.getId(),
				targetPos,
				pulling,
				color,
				duration));
		sendBeamPayload(world, targetPos, target, pulling, color, duration);
	}

	private void postFuseEvent(ServerWorld world, GrowthFuseEvent.Stage stage, int ticksRemaining) {
		VirusWorldState state = VirusWorldState.get(world);
		state.orchestrator().services().effectBus().post(new GrowthFuseEvent(world, pos.toImmutable(), stage, ticksRemaining, resolveFuseProfile()));
	}

	private void sendBeamPayload(ServerWorld world, Vec3d targetPos, LivingEntity target, boolean pulling, float[] color, int duration) {
		float red = color.length > 0 ? color[0] : 1.0F;
		float green = color.length > 1 ? color[1] : 0.5F;
		float blue = color.length > 2 ? color[2] : 0.5F;
		GrowthBeamPayload payload = new GrowthBeamPayload(
				world.getRegistryKey(),
				pos.toImmutable(),
				target.getId(),
				targetPos.x,
				targetPos.y,
				targetPos.z,
				pulling,
				red,
				green,
				blue,
				duration);
		for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	@Nullable
	private GrowthRegistry registry() {
		InfectionServiceContainer c = InfectionServices.container();
		return c != null ? c.growth() : null;
	}

	private double sanitizedMaxScale(GrowthBlockDefinition definition) {
		double min = definition.minScale();
		double max = definition.maxScale();
		if (max - min < MIN_SCALE_SPAN) {
			return min + MIN_SCALE_SPAN;
		}
		return max;
	}

	private void resetTransientState(GrowthBlockDefinition definition) {
		double min = definition.minScale();
		double max = sanitizedMaxScale(definition);
		currentScale = MathHelper.clamp(currentScale, min, max);
		previousScale = currentScale;
		targetScale = MathHelper.clamp(targetScale, min, max);
		scaleCooldown = definition.sanitizedRate();
		pullCooldown = 0;
		pushCooldown = 0;
		ambientCooldown = 0;
		ambientSoundCooldown = 0;
		wobbleOffsetX = 0.0D;
		wobbleOffsetY = 0.0D;
		wobbleOffsetZ = 0.0D;
		disarmFuse();
		forceDamageCooldowns.clear();
		lastGrowthEnabled = definition.growthEnabled();
		lastWobbleEnabled = definition.isWobbly();
		lastHasCollision = definition.hasCollision();
		collapseEndScale = currentScale;
		remainingCharges = resolveExplosionProfile().sanitizedCharges();
		clearBurstState();
	}

	private void disarmFuse() {
		if (!fuseArmed) {
			clearBurstState();
			return;
		}
		fuseArmed = false;
		fuseTicks = 0;
		collapseTicks = 0;
		collapseDuration = 0;
		collapseStartScale = currentScale;
		collapseEndScale = currentScale;
		clearBurstState();
	}

	private record FuseResult(boolean dirty, boolean shapeDirty, boolean removed) {
		static final FuseResult NONE = new FuseResult(false, false, false);
	}

	public enum ManualFuseCause {
		INTERACT,
		ATTACK
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putString("DefinitionId", definitionId.toString());
		view.putDouble("CurrentScale", currentScale);
		view.putDouble("TargetScale", targetScale);
		view.putBoolean("FuseArmed", fuseArmed);
		view.putInt("FuseTicks", fuseTicks);
		view.putInt("CollapseTicks", collapseTicks);
		view.putInt("CollapseDuration", collapseDuration);
		view.putDouble("CollapseStartScale", collapseStartScale);
		view.putDouble("CollapseEndScale", collapseEndScale);
		view.putInt("ScaleCooldown", scaleCooldown);
		view.putInt("PullCooldown", pullCooldown);
		view.putInt("PushCooldown", pushCooldown);
		view.putInt("AmbientCooldown", ambientCooldown);
		view.putInt("AmbientSoundCooldown", ambientSoundCooldown);
		view.putDouble("WobbleOffsetX", wobbleOffsetX);
		view.putDouble("WobbleOffsetY", wobbleOffsetY);
		view.putDouble("WobbleOffsetZ", wobbleOffsetZ);
		view.putInt("RemainingCharges", remainingCharges);
		view.putInt("BurstExplosionsRemaining", burstExplosionsRemaining);
		view.putInt("BurstDelayTicks", burstDelayTicks);
		view.putInt("BurstDelayInterval", burstDelayInterval);
		view.putFloat("BurstRadius", burstRadius);
		view.putBoolean("BurstCausesFire", burstCausesFire);
		view.putBoolean("BurstBreaksBlocks", burstBreaksBlocks);
		view.putDouble("BurstMaxDamage", burstMaxDamage);
		view.putDouble("BurstDamageScaling", burstDamageScaling);
		view.putString("Overrides", overrides.toSnbt());
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.definitionId = Identifier.tryParse(view.getString("DefinitionId", definitionId.toString()));
		this.currentScale = view.getDouble("CurrentScale", currentScale);
		this.targetScale = view.getDouble("TargetScale", targetScale);
		this.fuseArmed = view.getBoolean("FuseArmed", false);
		this.fuseTicks = view.getInt("FuseTicks", 0);
		this.collapseTicks = view.getInt("CollapseTicks", 0);
		this.collapseDuration = view.getInt("CollapseDuration", 0);
		this.collapseStartScale = view.getDouble("CollapseStartScale", currentScale);
		this.collapseEndScale = view.getDouble("CollapseEndScale", currentScale);
		this.ambientSoundCooldown = view.getInt("AmbientSoundCooldown", 0);
		this.wobbleOffsetX = view.getDouble("WobbleOffsetX", 0.0D);
		this.wobbleOffsetY = view.getDouble("WobbleOffsetY", 0.0D);
		this.wobbleOffsetZ = view.getDouble("WobbleOffsetZ", 0.0D);
		int profileCharges = resolveExplosionProfile().sanitizedCharges();
		this.remainingCharges = MathHelper.clamp(view.getInt("RemainingCharges", profileCharges), 0, profileCharges);
		this.burstExplosionsRemaining = view.getInt("BurstExplosionsRemaining", 0);
		this.burstDelayTicks = view.getInt("BurstDelayTicks", 0);
		this.burstDelayInterval = view.getInt("BurstDelayInterval", 0);
		this.burstRadius = view.getFloat("BurstRadius", 0.0F);
		this.burstCausesFire = view.getBoolean("BurstCausesFire", false);
		this.burstBreaksBlocks = view.getBoolean("BurstBreaksBlocks", false);
		this.burstMaxDamage = view.getDouble("BurstMaxDamage", 0.0D);
		this.burstDamageScaling = view.getDouble("BurstDamageScaling", 1.0D);
		String overridesRaw = view.getString("Overrides", "");
		this.overrides = GrowthOverrides.fromSnbt(overridesRaw);
		GrowthBlockDefinition definition = resolveDefinition();
		this.scaleCooldown = view.getInt("ScaleCooldown", definition.sanitizedRate());
		this.pullCooldown = view.getInt("PullCooldown", 0);
		this.pushCooldown = view.getInt("PushCooldown", 0);
		this.ambientCooldown = view.getInt("AmbientCooldown", 0);
		lastGrowthEnabled = definition.growthEnabled();
		lastWobbleEnabled = definition.isWobbly();
		lastHasCollision = definition.hasCollision();
		rebuildShapes(definition);
	}

	private void sync() {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		serverWorld.getChunkManager().markForUpdate(pos);
		serverWorld.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
	}

	private record RingBand(double radius, double inner, double outer, Identifier fieldProfileId) {
	}

	private record RingForceDecision(double directionSign, double strengthMultiplier) {
	}

}

