package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.entity.CorruptedWormEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

@SuppressWarnings("deprecation")
public final class ModEntities {

	public static final RegistryKey<EntityType<?>> FALLING_MATRIX_CUBE_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "falling_matrix_cube"));
	public static final RegistryKey<EntityType<?>> CORRUPTED_WORM_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "corrupted_worm"));

	public static final EntityType<FallingMatrixCubeEntity> FALLING_MATRIX_CUBE =
			FabricEntityTypeBuilder.<FallingMatrixCubeEntity>create(SpawnGroup.MISC, FallingMatrixCubeEntity::new)
					.dimensions(EntityDimensions.fixed(0.98F, 0.98F))
					.trackRangeBlocks(64)
					.trackedUpdateRate(10)
					.build(FALLING_MATRIX_CUBE_KEY);
	public static final EntityType<CorruptedWormEntity> CORRUPTED_WORM =
			FabricEntityTypeBuilder.<CorruptedWormEntity>create(SpawnGroup.MONSTER, CorruptedWormEntity::new)
					.dimensions(EntityDimensions.fixed(0.4F, 0.3F))
					.trackRangeBlocks(32)
					.trackedUpdateRate(3)
					.build(CORRUPTED_WORM_KEY);

	private ModEntities() {
	}

	public static void bootstrap() {
		Registry.register(Registries.ENTITY_TYPE, FALLING_MATRIX_CUBE_KEY, FALLING_MATRIX_CUBE);
		Registry.register(Registries.ENTITY_TYPE, CORRUPTED_WORM_KEY, CORRUPTED_WORM);
		FabricDefaultAttributeRegistry.register(CORRUPTED_WORM, SilverfishEntity.createSilverfishAttributes());
	}
}

