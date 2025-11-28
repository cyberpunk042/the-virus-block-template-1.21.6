package net.cyberpunk042.client.render.entity;

import net.cyberpunk042.entity.BlackholePearlEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class BlackholePearlEntityRenderer extends EntityRenderer<BlackholePearlEntity, BlackholePearlRenderState> {
	private static final ItemStack PEARL_STACK = new ItemStack(Items.ENDER_PEARL);
	private static final float BASE_SCALE = 4.0F;
	private final ItemModelManager itemModelManager;

	public BlackholePearlEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.itemModelManager = context.getItemModelManager();
		this.shadowRadius = 0.0F;
		this.shadowOpacity = 0.0F;
	}

	@Override
	public BlackholePearlRenderState createRenderState() {
		return new BlackholePearlRenderState();
	}

	@Override
	public void updateRenderState(BlackholePearlEntity entity, BlackholePearlRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.age = entity.age + tickDelta;
		state.uniqueOffset = (entity.getUuid().hashCode() & 0xFFFF) / 65535.0F;
		state.scale = Math.max(0.5F, entity.getScale());
		state.update(entity, PEARL_STACK, this.itemModelManager);
	}

	@Override
	public void render(BlackholePearlRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		if (state.itemRenderState.isEmpty()) {
			return;
		}
		matrices.push();
		Box box = state.itemRenderState.getModelBoundingBox();
		float baseYOffset = -((float) box.minY) + 0.0625F;
		float bob = MathHelper.sin(state.age / 10.0F + state.uniqueOffset) * 0.1F + 0.1F;
		matrices.translate(0.0F, baseYOffset + bob, 0.0F);
		matrices.multiply(this.dispatcher.getRotation());
		matrices.scale(state.scale * BASE_SCALE, state.scale * BASE_SCALE, state.scale * BASE_SCALE);
		state.itemRenderState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
		super.render(state, matrices, vertexConsumers, light);
	}

}

