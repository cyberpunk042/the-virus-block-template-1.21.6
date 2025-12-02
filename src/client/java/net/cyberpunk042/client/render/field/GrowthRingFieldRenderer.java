package net.cyberpunk042.client.render.field;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.joml.Matrix4f;

import net.cyberpunk042.growth.FieldProfile;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.network.GrowthRingFieldPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class GrowthRingFieldRenderer {
	private static final Deque<RingVisual> ACTIVE = new ArrayDeque<>();

	private GrowthRingFieldRenderer() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(GrowthRingFieldPayload.ID, (payload, context) ->
				context.client().execute(() -> handlePayload(payload)));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Iterator<RingVisual> iterator = ACTIVE.iterator();
			while (iterator.hasNext()) {
				RingVisual visual = iterator.next();
				if (visual.tick()) {
					iterator.remove();
				}
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(ACTIVE::clear));

		WorldRenderEvents.AFTER_ENTITIES.register(GrowthRingFieldRenderer::renderWorld);
	}

	private static void handlePayload(GrowthRingFieldPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null || !world.getRegistryKey().equals(payload.worldKey())) {
			return;
		}
		Vec3d origin = Vec3d.ofCenter(payload.origin());
		for (GrowthRingFieldPayload.RingEntry entry : payload.rings()) {
			ACTIVE.add(new RingVisual(origin, entry.fieldProfileId(), entry.radius(), entry.width(), entry.durationTicks()));
		}
	}

	private static void renderWorld(WorldRenderContext context) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		VertexConsumerProvider consumers = context.consumers();
		Camera camera = context.camera();
		ClientWorld world = context.world();
		if (consumers == null || camera == null || world == null) {
			return;
		}
		float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
		float worldTime = world.getTime() + tickDelta;
		MatrixStack matrices = context.matrixStack();
		Vec3d camPos = camera.getPos();

		for (RingVisual visual : ACTIVE) {
			FieldProfile profile = resolveFieldProfile(visual.fieldId());
			if (profile == null) {
				continue;
			}
			float alpha = profile.clampedAlpha() * visual.alphaFactor();
			if (alpha <= 0.01F) {
				continue;
			}
			int argb = composeColor(profile.decodedColor(), alpha);
			matrices.push();
			matrices.translate(visual.origin().x - camPos.x, visual.origin().y - camPos.y, visual.origin().z - camPos.z);
			renderRing(matrices, consumers, profile, visual, worldTime, argb);
			matrices.pop();
		}
	}

	private static void renderRing(MatrixStack matrices, VertexConsumerProvider consumers, FieldProfile profile, RingVisual visual, float worldTime, int argb) {
		Identifier texture = profile.texture();
		if (texture == null) {
			return;
		}
		VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(worldTime * 0.02F * profile.spinSpeed()));

		MatrixStack.Entry entry = matrices.peek();
		Matrix4f position = entry.getPositionMatrix();
		float inner = Math.max(0.05F, visual.innerRadius());
		float outer = Math.max(inner + 0.05F, visual.outerRadius());
		int segments = 48;
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

		for (int i = 0; i < segments; i++) {
			double angleStart = (Math.PI * 2.0D * i) / segments;
			double angleEnd = (Math.PI * 2.0D * (i + 1)) / segments;
			emitRingQuad(position, consumer, angleStart, angleEnd, inner, outer, argb, light);
		}

		matrices.pop();
	}

	private static void emitRingQuad(Matrix4f matrix, VertexConsumer consumer, double angleStart, double angleEnd, float inner, float outer, int argb, int light) {
		float x1 = (float) (Math.cos(angleStart) * inner);
		float z1 = (float) (Math.sin(angleStart) * inner);
		float x2 = (float) (Math.cos(angleEnd) * inner);
		float z2 = (float) (Math.sin(angleEnd) * inner);
		float x3 = (float) (Math.cos(angleEnd) * outer);
		float z3 = (float) (Math.sin(angleEnd) * outer);
		float x4 = (float) (Math.cos(angleStart) * outer);
		float z4 = (float) (Math.sin(angleStart) * outer);

		int a = (argb >> 24) & 0xFF;
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;

		consumer.vertex(matrix, x1, 0.01F, z1)
				.color(r, g, b, a)
				.texture(0.0F, 1.0F)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0.0F, 1.0F, 0.0F);
		consumer.vertex(matrix, x2, 0.01F, z2)
				.color(r, g, b, a)
				.texture(1.0F, 1.0F)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0.0F, 1.0F, 0.0F);
		consumer.vertex(matrix, x3, 0.01F, z3)
				.color(r, g, b, a)
				.texture(1.0F, 0.0F)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0.0F, 1.0F, 0.0F);
		consumer.vertex(matrix, x4, 0.01F, z4)
				.color(r, g, b, a)
				.texture(0.0F, 0.0F)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0.0F, 1.0F, 0.0F);
	}

	private static FieldProfile resolveFieldProfile(Identifier id) {
		if (id == null) {
			return FieldProfile.defaults();
		}
		InfectionServiceContainer container = InfectionServices.container();
		if (container == null) {
			return FieldProfile.defaults();
		}
		return container.growth().fieldProfile(id);
	}

	private static int composeColor(float[] color, float alpha) {
		int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
		int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
		int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
		int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static final class RingVisual {
		private final Vec3d origin;
		private final Identifier fieldId;
		private final float radius;
		private final float width;
		private final int maxTicks;
		private int ticksRemaining;

		private RingVisual(Vec3d origin, Identifier fieldId, float radius, float width, int duration) {
			this.origin = origin;
			this.fieldId = fieldId;
			this.radius = radius;
			this.width = width;
			this.maxTicks = Math.max(1, duration);
			this.ticksRemaining = this.maxTicks;
		}

		private boolean tick() {
			return --ticksRemaining <= 0;
		}

		private float alphaFactor() {
			return MathHelper.clamp(ticksRemaining / (float) maxTicks, 0.0F, 1.0F);
		}

		private float innerRadius() {
			return Math.max(0.05F, radius - (width * 0.5F));
		}

		private float outerRadius() {
			return Math.max(innerRadius() + 0.05F, radius + (width * 0.5F));
		}

		private Vec3d origin() {
			return origin;
		}

		private Identifier fieldId() {
			return fieldId;
		}
	}
}

