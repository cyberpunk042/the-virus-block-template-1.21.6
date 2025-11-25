package net.cyberpunk042.entity;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class CorruptedTntEntity extends TntEntity {
	public CorruptedTntEntity(EntityType<? extends TntEntity> type, World world) {
		super(type, world);
	}

	public CorruptedTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
		this(ModEntities.CORRUPTED_TNT, world);
		this.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
		double angle = world.random.nextDouble() * 2.0D * Math.PI;
		this.setVelocity(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
		this.setFuse(80);
	}

	@Override
	public BlockState getBlockState() {
		return ModBlocks.CORRUPTED_TNT.getDefaultState();
	}

	public static CorruptedTntEntity spawn(ServerWorld world, double x, double y, double z, @Nullable LivingEntity igniter, int fuse) {
		CorruptedTntEntity entity = new CorruptedTntEntity(world, x, y, z, igniter);
		entity.setFuse(fuse);
		world.spawnEntity(entity);
		return entity;
	}
}

