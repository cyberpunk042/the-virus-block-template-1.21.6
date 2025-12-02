package net.cyberpunk042.client.render.blockentity;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.growth.FieldProfile;
import net.cyberpunk042.growth.GlowProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ProgressiveGrowthBlockEntityRenderer implements BlockEntityRenderer<ProgressiveGrowthBlockEntity> {
	private static final int MAX_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

	public ProgressiveGrowthBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(ProgressiveGrowthBlockEntity entity, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		if (entity.getWorld() == null) {
			return;
		}

		float scale = Math.max(0.01F, entity.getRenderScale(tickDelta));
		GlowProfile glow = entity.resolveGlowProfile();
		if (glow == null) {
			return;
		}

		BlockPos pos = entity.getPos();
		double offsetX = pos.getX() + 0.5D - cameraPos.x;
		double offsetY = pos.getY() + 0.5D - cameraPos.y;
		double offsetZ = pos.getZ() + 0.5D - cameraPos.z;

		matrices.push();
		matrices.translate(offsetX, offsetY, offsetZ);

		GrowthBlockDefinition definition = entity.definitionSnapshot();
		float worldTime = entity.getWorld().getTime() + tickDelta;
		Vec3d wobble = entity.getRenderWobble(definition, tickDelta);
		if (wobble.lengthSquared() > 1.0E-5) {
			matrices.translate(wobble.x, wobble.y, wobble.z);
		}

		float fusePulse = entity.isFuseArmed()
				? (0.35F + 0.35F * MathHelper.sin(worldTime * 0.4F))
				: 0.0F;
		float primaryAlpha = MathHelper.clamp(glow.clampedPrimaryAlpha() + fusePulse, 0.0F, 1.0F);
		float secondaryAlpha = MathHelper.clamp(glow.clampedSecondaryAlpha() + fusePulse * 0.8F, 0.0F, 1.0F);

		float[] primaryColor = decodeHexColor(entity.resolveFuseProfile().primaryColorHex());
		float[] secondaryColor = decodeHexColor(entity.resolveFuseProfile().secondaryColorHex());

		renderLayer(matrices, vertexConsumers, glow.primaryTexture(), scale, primaryAlpha, glow.spinSpeed(), worldTime, primaryColor);
		renderLayer(matrices, vertexConsumers, glow.secondaryTexture(), scale * 0.65F, secondaryAlpha, glow.spinSpeed() * 1.4F, worldTime, secondaryColor);

		FieldProfile field = entity.resolveFieldProfile();
		if (field != null) {
			renderField(matrices, vertexConsumers, field, scale, worldTime);
		}

		matrices.pop();
	}

	private static void renderLayer(MatrixStack matrices, VertexConsumerProvider vertices, Identifier texture, float scale,
			float alpha, float spinSpeed, float worldTime, float[] color) {
		if (texture == null || alpha <= 0.01F) {
			return;
		}
		VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityTranslucent(texture));
		matrices.push();
		matrices.scale(scale, scale, scale);
		float rotation = worldTime * 0.02F * spinSpeed;
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));

		for (int i = 0; i < 4; i++) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (Math.PI / 2 * i)));
			renderQuad(matrices.peek(), consumer, alpha, color);
			matrices.pop();
		}
		matrices.pop();
	}

	private static void renderField(MatrixStack matrices, VertexConsumerProvider vertices, FieldProfile field, float baseScale, float worldTime) {
		float alpha = field.clampedAlpha();
		if (alpha <= 0.01F) {
			return;
		}
		float scale = baseScale * field.clampedScaleMultiplier();
		String mesh = field.meshType() != null ? field.meshType().toLowerCase() : "sphere";
		float[] color = field.decodedColor();
		switch (mesh) {
			case "ring":
				renderRingField(matrices, vertices, field.texture(), scale, alpha, field.spinSpeed(), worldTime, color);
				break;
			case "sphere":
			case "shell":
			default:
				renderLayer(matrices, vertices, field.texture(), scale, alpha, field.spinSpeed(), worldTime, color);
				break;
		}
	}

	private static void renderRingField(MatrixStack matrices, VertexConsumerProvider vertices, Identifier texture, float radius, float alpha, float spinSpeed, float worldTime, float[] color) {
		if (texture == null) {
			return;
		}
		VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityTranslucent(texture));
		matrices.push();
		matrices.scale(radius, radius, radius);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(worldTime * 0.02F * spinSpeed));
		MatrixStack.Entry entry = matrices.peek();
		float outer = 1.0F;
		float inner = Math.max(0.05F, outer - 0.25F);
		int segments = 48;
		int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
		int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
		int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
		int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
		for (int i = 0; i < segments; i++) {
			double angleStart = (Math.PI * 2.0D * i) / segments;
			double angleEnd = (Math.PI * 2.0D * (i + 1)) / segments;
			emitRingQuad(entry, consumer, angleStart, angleEnd, inner, outer, r, g, b, a);
		}
		matrices.pop();
	}

	private static void emitRingQuad(MatrixStack.Entry entry, VertexConsumer consumer, double angleStart, double angleEnd, float inner, float outer, int r, int g, int b, int a) {
		float x1 = (float) (Math.cos(angleStart) * inner);
		float z1 = (float) (Math.sin(angleStart) * inner);
		float x2 = (float) (Math.cos(angleEnd) * inner);
		float z2 = (float) (Math.sin(angleEnd) * inner);
		float x3 = (float) (Math.cos(angleEnd) * outer);
		float z3 = (float) (Math.sin(angleEnd) * outer);
		float x4 = (float) (Math.cos(angleStart) * outer);
		float z4 = (float) (Math.sin(angleStart) * outer);
		consumer.vertex(entry.getPositionMatrix(), x1, 0.0F, z1).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
		consumer.vertex(entry.getPositionMatrix(), x2, 0.0F, z2).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
		consumer.vertex(entry.getPositionMatrix(), x3, 0.0F, z3).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
		consumer.vertex(entry.getPositionMatrix(), x4, 0.0F, z4).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
	}

	private static void renderQuad(Entry entry, VertexConsumer consumer, float alpha, float[] color) {
		float half = 0.5F;
		int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
		emitVertex(entry, consumer, -half, -half, 0.0F, 0.0F, 1.0F, color, a);
		emitVertex(entry, consumer, half, -half, 0.0F, 1.0F, 1.0F, color, a);
		emitVertex(entry, consumer, half, half, 0.0F, 1.0F, 0.0F, color, a);
		emitVertex(entry, consumer, -half, half, 0.0F, 0.0F, 0.0F, color, a);

		emitVertex(entry, consumer, half, half, 0.0F, 1.0F, 0.0F, color, a);
		emitVertex(entry, consumer, -half, half, 0.0F, 0.0F, 0.0F, color, a);
		emitVertex(entry, consumer, -half, -half, 0.0F, 0.0F, 1.0F, color, a);
		emitVertex(entry, consumer, half, -half, 0.0F, 1.0F, 1.0F, color, a);
	}

	private static void emitVertex(Entry entry, VertexConsumer consumer,
			float x, float y, float z, float u, float v, float[] color, int alpha) {
		int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
		int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
		int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
		consumer.vertex(entry.getPositionMatrix(), x, y, z)
				.color(r, g, b, alpha)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(MAX_LIGHT)
				.normal(entry, 0.0F, 1.0F, 0.0F);
	}

	private static float[] decodeHexColor(String hex) {
		if (hex == null || hex.isEmpty()) {
			return new float[] { 1.0F, 1.0F, 1.0F };
		}
		String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
		try {
			int value = (int) Long.parseLong(normalized, 16);
			float r = ((value >> 16) & 0xFF) / 255.0F;
			float g = ((value >> 8) & 0xFF) / 255.0F;
			float b = (value & 0xFF) / 255.0F;
			return new float[] { r, g, b };
		} catch (NumberFormatException ex) {
			return new float[] { 1.0F, 1.0F, 1.0F };
		}
	}
}

