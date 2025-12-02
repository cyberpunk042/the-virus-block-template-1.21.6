package net.cyberpunk042.client.render.item;

import java.util.Set;

import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class ProgressiveGrowthItemRenderer implements SpecialModelRenderer<ProgressiveGrowthItemRenderer.Argument> {
	private static final Identifier RENDERER_ID = Identifier.of(TheVirusBlock.MOD_ID, "progressive_growth");
	private static final Vector3f[] UNIT_CUBE_EXTENTS = new Vector3f[]{
			new Vector3f(-0.5F, -0.5F, -0.5F),
			new Vector3f(-0.5F, -0.5F, 0.5F),
			new Vector3f(-0.5F, 0.5F, -0.5F),
			new Vector3f(-0.5F, 0.5F, 0.5F),
			new Vector3f(0.5F, -0.5F, -0.5F),
			new Vector3f(0.5F, -0.5F, 0.5F),
			new Vector3f(0.5F, 0.5F, -0.5F),
			new Vector3f(0.5F, 0.5F, 0.5F)
	};
	private static boolean registered;

	private final ProgressiveGrowthBlockEntity entity = new ProgressiveGrowthBlockEntity(BlockPos.ORIGIN, ModBlocks.PROGRESSIVE_GROWTH_BLOCK.getDefaultState());

	public static void bootstrap() {
		if (registered) {
			return;
		}
		registered = true;
		SpecialModelTypes.ID_MAPPER.put(RENDERER_ID, Unbaked.MAP_CODEC);
	}

	@Override
	public void render(@Nullable Argument argument, ItemDisplayContext displayContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null) {
			return;
		}
		entity.setWorld(client.world);
		Identifier definitionId = argument != null && argument.definitionId() != null ? argument.definitionId() : GrowthBlockDefinition.defaults().id();
		entity.setDefinitionId(definitionId);
		GrowthOverrides overrides = argument != null && argument.overrides() != null ? argument.overrides() : GrowthOverrides.empty();
		entity.replaceOverrides(overrides);
		BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
		dispatcher.render(entity, 0.0F, matrices, vertexConsumers);
	}

	@Override
	public void collectVertices(Set<Vector3f> vertices) {
		for (Vector3f corner : UNIT_CUBE_EXTENTS) {
			vertices.add(new Vector3f(corner));
		}
	}

	@Override
	public @Nullable Argument getData(ItemStack stack) {
		Identifier definitionId = ProgressiveGrowthBlock.readDefinitionId(stack);
		GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(stack);
		return new Argument(definitionId, overrides);
	}

	@Environment(EnvType.CLIENT)
	public static final class Unbaked implements SpecialModelRenderer.Unbaked {
		private static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
			return new ProgressiveGrowthItemRenderer();
		}
	}

	public record Argument(@Nullable Identifier definitionId, GrowthOverrides overrides) {
	}
}

