package net.cyberpunk042.client.render;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.network.ShieldFieldRemovePayload;
import net.cyberpunk042.network.ShieldFieldSpawnPayload;
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
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class ShieldFieldVisualManager {
	private static final Identifier TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/misc/anti-virus-shield.png");
	private static final Identifier BEAM_TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;
	private static final float INNER_BEAM_RADIUS = 0.045F;
	private static final float OUTER_BEAM_RADIUS = 0.06F;
	private static final Map<Long, ShieldVisual> ACTIVE = new Long2ObjectOpenHashMap<>();
	private static float visualScale = 0.3F;
	private static float spinSpeed = 0.03F;
	private static float tiltMultiplier = 0F;
	private static final ShieldVisualPreset[] PRESETS = new ShieldVisualPreset[]{
			new ShieldVisualPreset("default", 640, 80, 0.0F),
			new ShieldVisualPreset("smooth", 64, 96, 0.0F),
			new ShieldVisualPreset("swirl", 48, 96, 2.4F),
			new ShieldVisualPreset("rings", 54, 80, 0.6F)
	};
	private static int currentPresetIndex = 0;
	private static String activePresetName = PRESETS[0].name();
	private static int latSteps = PRESETS[0].latSteps();
	private static int lonSteps = PRESETS[0].lonSteps();
	private static float swirlStrength = PRESETS[0].swirlStrength();

	private ShieldFieldVisualManager() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(ShieldFieldSpawnPayload.ID, (payload, context) ->
				context.client().execute(() -> handleSpawn(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ShieldFieldRemovePayload.ID, (payload, context) ->
				context.client().execute(() -> handleRemove(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.isPaused()) {
				return;
			}
			ACTIVE.values().forEach(ShieldVisual::tick);
		});
		WorldRenderEvents.AFTER_ENTITIES.register(ShieldFieldVisualManager::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(ACTIVE::clear));
	}

	private static void handleSpawn(ShieldFieldSpawnPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}
		ACTIVE.put(payload.id(), new ShieldVisual(
				new Vec3d(payload.x(), payload.y(), payload.z()),
				payload.radius(),
				client.world.random.nextFloat() * MathHelper.TAU));
	}

	private static void handleRemove(ShieldFieldRemovePayload payload) {
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
		RenderLayer layer = RenderLayer.getEntityTranslucent(TEXTURE);
		for (ShieldVisual visual : ACTIVE.values()) {
			float alpha = visual.alpha();
			if (alpha <= 0.02F) {
				continue;
			}
			float scale = Math.max(0.5F, visual.radius * visualScale);
			float phase = spinSpeed == 0.0F ? 0.0F : visual.phase;

			matrices.push();
			matrices.translate(visual.position.x - camPos.x, visual.position.y - camPos.y, visual.position.z - camPos.z);
			float rotation = (visual.age * spinSpeed) + phase;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotation * tiltMultiplier));
			matrices.scale(scale, scale, scale);
			VertexConsumer consumer = consumers.getBuffer(layer);
			renderSphere(matrices.peek(), consumer, visual, alpha * 0.92F, phase);
			matrices.pop();

			renderBeam(context, visual);
		}
	}

	private static void renderBeam(WorldRenderContext context, ShieldVisual visual) {
		if (context.world() == null) {
			return;
		}
		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) {
			return;
		}
		MatrixStack matrices = context.matrixStack();
		Vec3d camPos = context.camera().getPos();
		Vec3d center = visual.position;
		double beamStartY = center.y + 1.5D;
		matrices.push();
		matrices.translate(center.x - camPos.x, beamStartY - camPos.y, center.z - camPos.z);
		long worldTime = context.world().getTime();
		float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
		int topY = context.world().getBottomY() + context.world().getDimension().height();
		int remaining = Math.max(4, topY - MathHelper.floor(beamStartY));
		renderCustomBeam(matrices, consumers, remaining, worldTime, tickDelta);
		matrices.pop();
	}

	private static void renderSphere(Entry entry, VertexConsumer consumer, ShieldVisual visual, float alpha, float phaseOffset) {
		Matrix4f matrix = entry.getPositionMatrix();
		Matrix3f normalMatrix = entry.getNormalMatrix();
		int latCount = MathHelper.clamp(latSteps, 8, 640);
		int lonCount = MathHelper.clamp(lonSteps, 16, 960);
		for (int lat = 0; lat < latCount; lat++) {
			float v0 = lat / (float) latCount;
			float v1 = (lat + 1) / (float) latCount;
			float theta0 = v0 * MathHelper.PI;
			float theta1 = v1 * MathHelper.PI;
			for (int lon = 0; lon <= lonCount; lon++) {
				float u = lon / (float) lonCount;
				float phi = (u * MathHelper.TAU) + phaseOffset + swirlStrength * (v0 - 0.5F);
				addVertex(matrix, normalMatrix, consumer, theta1, phi, u, v1, alpha);
				addVertex(matrix, normalMatrix, consumer, theta0, phi, u, v0, alpha);
			}
		}
	}

	private static void addVertex(Matrix4f matrix, Matrix3f normalMatrix,
	                              VertexConsumer consumer, float theta, float phi, float u, float v, float alpha) {
		float sinTheta = MathHelper.sin(theta);
		float x = sinTheta * MathHelper.cos(phi);
		float y = MathHelper.cos(theta);
		float z = sinTheta * MathHelper.sin(phi);
		Vector4f position = new Vector4f(x, y, z, 1.0F);
		position.mul(matrix);
		Vector3f normal = new Vector3f(x, y, z);
		normal.mul(normalMatrix);
		float base = 0.95F;
		float pulse = 0.02F * MathHelper.sin(phi);
		float r = MathHelper.clamp(base + pulse, 0.0F, 1.0F);
		float g = MathHelper.clamp(base + pulse, 0.0F, 1.0F);
		float b = MathHelper.clamp(base + 0.01F, 0.0F, 1.0F);
		int ai = MathHelper.floor(alpha * 255.0F) & 0xFF;
		int ri = MathHelper.floor(r * 255.0F) & 0xFF;
		int gi = MathHelper.floor(g * 255.0F) & 0xFF;
		int bi = MathHelper.floor(b * 255.0F) & 0xFF;
		int color = (ai << 24) | (ri << 16) | (gi << 8) | bi;
		consumer.vertex(position.x(), position.y(), position.z(), color, u, v,
				OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE,
				normal.x(), normal.y(), normal.z());
	}

	private static final class ShieldVisual {
		final Vec3d position;
		final float radius;
		final float phase;
		private float age;

		ShieldVisual(Vec3d position, float radius, float phase) {
			this.position = position;
			this.radius = radius;
			this.phase = phase;
		}

		void tick() {
			age += 1.0F;
		}

		float alpha() {
			float pulse = 0.88F + 0.1F * MathHelper.sin((age * 0.04F) + phase);
			return MathHelper.clamp(pulse, 0.6F, 0.98F);
		}
	}

	private static void applyPreset(ShieldVisualPreset preset, int index) {
		latSteps = preset.latSteps();
		lonSteps = preset.lonSteps();
		swirlStrength = preset.swirlStrength();
		activePresetName = preset.name();
		currentPresetIndex = index;
	}

	public static boolean applyPreset(String name) {
		for (int i = 0; i < PRESETS.length; i++) {
			if (PRESETS[i].name().equalsIgnoreCase(name)) {
				applyPreset(PRESETS[i], i);
				return true;
			}
		}
		return false;
	}

	public static String cyclePreset() {
		int next = (currentPresetIndex + 1) % PRESETS.length;
		applyPreset(PRESETS[next], next);
		return activePresetName;
	}

	public static boolean setLatSteps(int value) {
		if (value < 8 || value > 640) {
			return false;
		}
		latSteps = value;
		markCustom();
		return true;
	}

	public static boolean setLonSteps(int value) {
		if (value < 16 || value > 960) {
			return false;
		}
		lonSteps = value;
		markCustom();
		return true;
	}

	public static boolean setSwirlStrength(float value) {
		if (value < -8.0F || value > 8.0F) {
			return false;
		}
		swirlStrength = value;
		markCustom();
		return true;
	}

	private static void markCustom() {
		activePresetName = "custom";
		currentPresetIndex = -1;
	}

	public static String getActivePresetName() {
		return activePresetName;
	}

	public static int getLatSteps() {
		return latSteps;
	}

	public static int getLonSteps() {
		return lonSteps;
	}

	public static float getSwirlStrength() {
		return swirlStrength;
	}

	public static float getVisualScale() {
		return visualScale;
	}

	public static float getSpinSpeed() {
		return spinSpeed;
	}

	public static float getTiltMultiplier() {
		return tiltMultiplier;
	}

	public static Iterable<String> getPresetNames() {
		return Arrays.stream(PRESETS).map(ShieldVisualPreset::name).toList();
	}

	public static CompletableFuture<Suggestions> suggestPresetNames(SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Arrays.stream(PRESETS).map(ShieldVisualPreset::name), builder);
	}

	public static boolean setScale(float value) {
		if (value < 0.05F || value > 2.0F) {
			return false;
		}
		visualScale = value;
		markCustom();
		return true;
	}

	public static boolean setSpinSpeed(float value) {
		if (value < -0.2F || value > 0.2F) {
			return false;
		}
		spinSpeed = value;
		markCustom();
		return true;
	}

	public static boolean setTiltMultiplier(float value) {
		if (value < -1.0F || value > 1.0F) {
			return false;
		}
		tiltMultiplier = value;
		markCustom();
		return true;
	}

	private record ShieldVisualPreset(String name, int latSteps, int lonSteps, float swirlStrength) {
	}

	private static void renderCustomBeam(MatrixStack matrices, VertexConsumerProvider consumers, int height, long worldTime, float tickDelta) {
		float cycle = Math.floorMod(worldTime, 40L) + tickDelta;
		float phase = MathHelper.fractionalPart(cycle * 0.2F - MathHelper.floor(cycle * 0.1F));
		float vStart = -1.0F + phase;
		VertexConsumer inner = consumers.getBuffer(RenderLayer.getBeaconBeam(BEAM_TEXTURE, false));
		VertexConsumer outer = consumers.getBuffer(RenderLayer.getBeaconBeam(BEAM_TEXTURE, true));
		renderBeamPrism(matrices.peek(), inner, INNER_BEAM_RADIUS, height, vStart, vStart + height * (0.5F / INNER_BEAM_RADIUS), 0xFFFFFFFF);
		renderBeamPrism(matrices.peek(), outer, OUTER_BEAM_RADIUS, height, vStart, vStart + height, 0x40FFFFFF);
	}

	private static void renderBeamPrism(MatrixStack.Entry entry, VertexConsumer consumer, float radius, int height, float vStart, float vEnd, int argb) {
		renderFacePair(entry, consumer, -radius, -radius, radius, -radius, height, vStart, vEnd, argb, 0.0F, 0.0F, -1.0F); // north
		renderFacePair(entry, consumer, radius, -radius, radius, radius, height, vStart, vEnd, argb, 1.0F, 0.0F, 0.0F);     // east
		renderFacePair(entry, consumer, -radius, radius, radius, radius, height, vStart, vEnd, argb, 0.0F, 0.0F, 1.0F);     // south
		renderFacePair(entry, consumer, -radius, -radius, -radius, radius, height, vStart, vEnd, argb, -1.0F, 0.0F, 0.0F);  // west
	}

	private static void renderFacePair(MatrixStack.Entry entry, VertexConsumer consumer,
	                                   float x1, float z1, float x2, float z2,
	                                   int height, float vStart, float vEnd,
	                                   int argb, float normalX, float normalY, float normalZ) {
		renderFace(entry, consumer, x1, z1, x2, z2, height, vStart, vEnd, argb, normalX, normalY, normalZ);
		renderFace(entry, consumer, x2, z2, x1, z1, height, vStart, vEnd, argb, -normalX, -normalY, -normalZ);
	}

	private static void renderFace(MatrixStack.Entry entry, VertexConsumer consumer,
	                               float x1, float z1, float x2, float z2,
	                               int height, float vStart, float vEnd,
	                               int argb, float normalX, float normalY, float normalZ) {
		addBeamVertex(entry, consumer, x1, 0.0F, z1, 0.0F, vStart, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x1, height, z1, 0.0F, vEnd, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x2, height, z2, 1.0F, vEnd, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x2, 0.0F, z2, 1.0F, vStart, argb, normalX, normalY, normalZ);
	}

	private static void addBeamVertex(MatrixStack.Entry entry, VertexConsumer consumer,
	                                  float x, float y, float z, float u, float v,
	                                  int argb, float normalX, float normalY, float normalZ) {
		int a = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;
		consumer.vertex(entry.getPositionMatrix(), x, y, z)
				.color(r, g, b, a)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(entry, normalX, normalY, normalZ);
	}
}

