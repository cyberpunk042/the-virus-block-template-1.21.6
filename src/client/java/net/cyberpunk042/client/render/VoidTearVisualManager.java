package net.cyberpunk042.client.render;

import java.util.Iterator;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.network.VoidTearBurstPayload;
import net.cyberpunk042.network.VoidTearSpawnPayload;
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
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class VoidTearVisualManager {
	private static final Identifier TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/misc/void_tear_sphere.png");
	private static final Map<Long, TearVisual> ACTIVE = new Long2ObjectOpenHashMap<>();
	private static final int LAT_STEPS = 320;
	private static final int LON_STEPS = 250;
	private static final float VISUAL_SCALE = 0.09F;
	private static final float VISUAL_DURATION_MULTIPLIER = 4F;

	private VoidTearVisualManager() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(VoidTearSpawnPayload.ID, (payload, context) ->
				context.client().execute(() -> handleSpawn(payload)));
		ClientPlayNetworking.registerGlobalReceiver(VoidTearBurstPayload.ID, (payload, context) ->
				context.client().execute(() -> handleBurst(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.isPaused()) {
				return;
			}
			Iterator<TearVisual> iterator = ACTIVE.values().iterator();
			while (iterator.hasNext()) {
				TearVisual visual = iterator.next();
				if (!visual.tick()) {
					iterator.remove();
				}
			}
		});
		WorldRenderEvents.AFTER_ENTITIES.register(context -> render(context));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(ACTIVE::clear));
	}

	private static void handleSpawn(VoidTearSpawnPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}
		int visualDuration = Math.max(1, MathHelper.floor(payload.durationTicks() * VISUAL_DURATION_MULTIPLIER));
		ACTIVE.put(payload.id(), new TearVisual(
				new Vec3d(payload.x(), payload.y(), payload.z()),
				payload.radius(),
				visualDuration,
				payload.tierIndex(),
				client.world.random.nextFloat() * MathHelper.TAU));
	}

	private static void handleBurst(VoidTearBurstPayload payload) {
		ACTIVE.remove(payload.id());
	}

	private static void render(WorldRenderContext context) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) {
			return;
		}
		Camera camera = context.camera();
		Vec3d camPos = camera.getPos();
		MatrixStack matrices = context.matrixStack();
		float tickDelta = 0.0F;
		RenderLayer layer = RenderLayer.getEntityTranslucent(TEXTURE);
		for (TearVisual visual : ACTIVE.values()) {
			if (visual.alpha(tickDelta) <= 0.01F) {
				continue;
			}
			matrices.push();
			matrices.translate(visual.position.x - camPos.x, visual.position.y - camPos.y, visual.position.z - camPos.z);
			float rotation = (visual.renderAge(tickDelta) * 0.05F) + visual.phase;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotation * 0.25F));
			float scale = visual.radius * VISUAL_SCALE;
			matrices.scale(scale, scale, scale);
			VertexConsumer consumer = consumers.getBuffer(layer);
			renderSphere(matrices.peek(), consumer, visual, tickDelta);
			matrices.pop();
		}
	}

	private static void renderSphere(Entry entry, VertexConsumer consumer, TearVisual visual, float tickDelta) {
		float alpha = visual.alpha(tickDelta);
		if (alpha <= 0.01F) {
			return;
		}
		float tierBoost = visual.tierIndex * 0.05F;
		float r = MathHelper.clamp(0.45F + tierBoost, 0.0F, 1.0F);
		float g = MathHelper.clamp(0.05F + tierBoost * 0.35F, 0.0F, 1.0F);
		float b = MathHelper.clamp(0.75F + tierBoost * 0.25F, 0.0F, 1.0F);

		Matrix4f matrix = entry.getPositionMatrix();
		Matrix3f normalMatrix = entry.getNormalMatrix();
		for (int lat = 0; lat < LAT_STEPS; lat++) {
			float v0 = lat / (float) LAT_STEPS;
			float v1 = (lat + 1) / (float) LAT_STEPS;
			float theta0 = v0 * MathHelper.PI;
			float theta1 = v1 * MathHelper.PI;
			for (int lon = 0; lon <= LON_STEPS; lon++) {
				float u = lon / (float) LON_STEPS;
				float phi = (u * MathHelper.TAU) + visual.phase;
				addVertex(matrix, normalMatrix, consumer, theta1, phi, u, v1, r, g, b, alpha);
				addVertex(matrix, normalMatrix, consumer, theta0, phi, u, v0, r, g, b, alpha);
			}
		}
	}

	private static void addVertex(Matrix4f matrix, Matrix3f normalMatrix,
	                              VertexConsumer consumer, float theta, float phi, float u, float v,
	                              float r, float g, float b, float alpha) {
		float sinTheta = MathHelper.sin(theta);
		float x = sinTheta * MathHelper.cos(phi);
		float y = MathHelper.cos(theta);
		float z = sinTheta * MathHelper.sin(phi);
		Vector4f position = new Vector4f(x, y, z, 1.0F);
		position.mul(matrix);
		Vector3f normal = new Vector3f(x, y, z);
		normal.mul(normalMatrix);
		int ai = MathHelper.floor(alpha * 255.0F) & 0xFF;
		int ri = MathHelper.floor(r * 255.0F) & 0xFF;
		int gi = MathHelper.floor(g * 255.0F) & 0xFF;
		int bi = MathHelper.floor(b * 255.0F) & 0xFF;
		int color = (ai << 24) | (ri << 16) | (gi << 8) | bi;
		consumer.vertex(position.x(), position.y(), position.z(), color, u, v,
				OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE,
				normal.x(), normal.y(), normal.z());
	}

	private static final class TearVisual {
		final Vec3d position;
		final float radius;
		final int totalTicks;
		final int tierIndex;
		final float phase;
		private float ticksRemaining;

		TearVisual(Vec3d position, float radius, int totalTicks, int tierIndex, float phase) {
			this.position = position;
			this.radius = radius;
			this.totalTicks = Math.max(1, totalTicks);
			this.tierIndex = tierIndex;
			this.phase = phase;
			this.ticksRemaining = this.totalTicks;
		}

		boolean tick() {
			ticksRemaining--;
			return ticksRemaining > 0;
		}

		float renderAge(float tickDelta) {
			return (totalTicks - ticksRemaining) + tickDelta;
		}

		float alpha(float tickDelta) {
			float pulse = 0.94F + 0.04F * MathHelper.sin((renderAge(tickDelta) * 0.35F) + phase);
			float fade = MathHelper.clamp((ticksRemaining + totalTicks * 0.25F) / (float) totalTicks, 0.0F, 1.0F);
			return MathHelper.clamp(pulse * fade, 0.0F, 1.0F);
		}
	}
}

