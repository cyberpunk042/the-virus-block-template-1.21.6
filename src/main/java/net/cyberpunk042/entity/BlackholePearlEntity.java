package net.cyberpunk042.entity;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BlackholePearlEntity extends Entity {
	private static final TrackedData<BlockPos> ANCHOR = DataTracker.registerData(BlackholePearlEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
	private static final TrackedData<Float> SCALE = DataTracker.registerData(BlackholePearlEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> TARGET_SCALE = DataTracker.registerData(BlackholePearlEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final float LERP_RATE = 0.08F;

	public BlackholePearlEntity(EntityType<? extends BlackholePearlEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
		this.setInvulnerable(true);
		this.setSilent(true);
	}

	public static BlackholePearlEntity create(World world, BlockPos anchor) {
		BlackholePearlEntity pearl = new BlackholePearlEntity(ModEntities.BLACKHOLE_PEARL, world);
		pearl.setAnchor(anchor);
		return pearl;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ANCHOR, BlockPos.ORIGIN);
		builder.add(SCALE, 0.4F);
		builder.add(TARGET_SCALE, 0.4F);
	}

	@Override
	protected void readCustomData(ReadView view) {
	}

	@Override
	protected void writeCustomData(WriteView view) {
	}

	@Override
	public void tick() {
		super.tick();
		BlockPos anchor = getAnchor();
		if (anchor != null) {
			this.setPos(anchor.getX() + 0.5D, anchor.getY() + 0.5D, anchor.getZ() + 0.5D);
		}
		if (!this.getWorld().isClient) {
			float current = getScale();
			float target = this.dataTracker.get(TARGET_SCALE);
			current += (target - current) * LERP_RATE;
			this.dataTracker.set(SCALE, current);
		}
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	public void setAnchor(BlockPos anchor) {
		this.dataTracker.set(ANCHOR, anchor);
	}

	@Nullable
	public BlockPos getAnchor() {
		BlockPos tracked = this.dataTracker.get(ANCHOR);
		return tracked == BlockPos.ORIGIN ? null : tracked;
	}

	public void setTargetScale(float target) {
		this.dataTracker.set(TARGET_SCALE, MathHelper.clamp(target, 0.1F, 32.0F));
	}

	public float getScale() {
		return this.dataTracker.get(SCALE);
	}
}

