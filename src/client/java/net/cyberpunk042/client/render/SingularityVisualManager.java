package net.cyberpunk042.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.client.render.util.BeaconBeamRenderer;
import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.config.ColorConfig.ColorSlot;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class SingularityVisualManager {
	private static final Map<BlockPos, SingularityVisual> ACTIVE = new ConcurrentHashMap<>();
	private static final Identifier DEFAULT_PRIMARY_TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/misc/singularity_sphere_1.png");
	private static final Identifier DEFAULT_CORE_TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/misc/singularity_sphere_2.png");
	private static final Identifier BEAM_TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;

	private static float innerBeamRadius;
	private static float outerBeamRadius;
	private static int sphereLatSteps;
	private static int sphereLonSteps;
	private static int coreSphereOverlapTicks;
	private static int blackBeamDelayTicks;
	private static float primaryBeamAlpha;
	private static float coreBeamAlpha;
	private static float blackBeamGrowth;
	private static float redBeamGrowth;
	private static float beamAlphaStep;
	private static SphereProfile primarySphere;
	private static SphereProfile coreSphere;
	private static SingularityVisualConfig CONFIG;

	private SingularityVisualManager() {
	}

	static {
		reloadConfigInternal();
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.isPaused()) {
				return;
			}
			ACTIVE.values().forEach(visual -> visual.tick(client));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(ACTIVE::clear));
		WorldRenderEvents.AFTER_ENTITIES.register(SingularityVisualManager::render);
	}

	public static void add(BlockPos pos) {
		ACTIVE.put(pos.toImmutable(), new SingularityVisual(pos));
	}

	public static void remove(BlockPos pos) {
		ACTIVE.remove(pos);
	}

	public static SingularityVisualConfig getConfig() {
		return CONFIG.copy();
	}

	public static boolean setConfigValue(String key, String rawValue) {
		if (CONFIG.setValue(key, rawValue)) {
			applyConfig(CONFIG);
			return true;
		}
		return false;
	}

	public static boolean saveConfig() {
		return SingularityVisualStore.save(CONFIG);
	}

	public static void reloadConfig() {
		reloadConfigInternal();
	}

	@Nullable
	public static SingularityVisual get(BlockPos pos) {
		return ACTIVE.get(pos);
	}

	private static void render(WorldRenderContext context) {
		if (ACTIVE.isEmpty() || context.world() == null) {
			return;
		}
		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) {
			return;
		}
		MatrixStack matrices = context.matrixStack();
		Vec3d camPos = context.camera().getPos();
		long worldTime = context.world().getTime();
		float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
		for (SingularityVisual visual : ACTIVE.values()) {
			if (visual.isPrimarySphereVisible()) {
				renderSphere(visual.getOrigin(), visual.getPhaseOffset(), visual.getPrimaryTexture(),
						visual.getPrimaryScale(), visual.getPrimaryAlpha(), visual.getPrimarySpinMultiplier(),
						matrices, consumers, camPos, worldTime, tickDelta);
			}
			if (visual.isCoreSphereVisible()) {
				renderSphere(visual.getOrigin(), visual.getPhaseOffset(), visual.getCoreTexture(),
						visual.getCoreScale(), visual.getCoreAlpha(), visual.getCoreSpinMultiplier(),
						matrices, consumers, camPos, worldTime, tickDelta);
			}
			if (visual.isBeamVisible()) {
				renderBeam(context, visual, matrices, consumers, camPos, worldTime, tickDelta);
			}
		}
	}

	private static void renderSphere(BlockPos origin, float phaseOffset, Identifier texture, float scale, float alpha,
	                                 float spinMultiplier, MatrixStack matrices, VertexConsumerProvider consumers,
	                                 Vec3d camPos, long worldTime, float tickDelta) {
		if (scale <= 0.01F || alpha <= 0.01F) {
			return;
		}
		Vec3d center = Vec3d.ofCenter(origin);
		float spin = (worldTime + tickDelta) * 0.01F * spinMultiplier + phaseOffset;
		matrices.push();
		matrices.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);
		matrices.scale(scale, scale, scale);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(spin));
		VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
		renderSphereMesh(matrices.peek(), consumer, alpha);
		matrices.pop();
	}

	private static void renderBeam(WorldRenderContext context, SingularityVisual visual, MatrixStack matrices,
	                               VertexConsumerProvider consumers, Vec3d camPos, long worldTime, float tickDelta) {
		float beamAlpha = visual.getBeamAlpha();
		float scale = visual.getBeamScale();
		if (beamAlpha <= 0.01F || scale <= 0.01F) {
			return;
		}
		Vec3d center = Vec3d.ofCenter(visual.getOrigin());
		double beamStartY = center.y + 0.5D; // start slightly above block center
		int topY = context.world().getBottomY() + context.world().getDimension().height();
		int height = Math.max(4, topY - MathHelper.floor(beamStartY));
		matrices.push();
		matrices.translate(center.x - camPos.x, beamStartY - camPos.y, center.z - camPos.z);
		int innerColor = colorWithAlpha(visual.getBeamInnerColor(), beamAlpha);
		int outerColor = colorWithAlpha(visual.getBeamOuterColor(), beamAlpha);
		float innerRadius = innerBeamRadius * MathHelper.clamp(scale, 0.02F, 1.0F);
		float outerRadius = outerBeamRadius * MathHelper.clamp(scale, 0.04F, 1.0F);
		BeaconBeamRenderer.render(matrices, consumers, BEAM_TEXTURE, innerRadius, outerRadius, height, worldTime, tickDelta, innerColor, outerColor);
		matrices.pop();
	}

	public static final class SingularityVisual {
		private final BlockPos origin;
		private final float phaseOffset;
		private PrimaryPhase primaryPhase = PrimaryPhase.GROW;
		private CorePhase corePhase = CorePhase.DORMANT;
		private int primaryAge;
		private int coreAge;
		private int beamTicks;
		private float primaryScale = primarySphere.minScale();
		private float primaryAlpha = primarySphere.growStartAlpha();
		private float coreScale;
		private float coreAlpha;
		private float beamAlpha;
		private float beamScale;
		private BeamTint beamTint = BeamTint.BLACK;
		private boolean beamCuePlayed;

		private SingularityVisual(BlockPos pos) {
			this.origin = pos.toImmutable();
			int hash = Long.hashCode(pos.asLong());
			this.phaseOffset = (float) (hash & 0xFFFF) / 65535.0F * MathHelper.TAU;
		}

		private void tick(MinecraftClient client) {
			beamTicks++;
			updatePrimary(client);
			updateCore();
			updateBeam();
		}

		private void updatePrimary(MinecraftClient client) {
			switch (primaryPhase) {
				case GROW -> {
					updatePrimaryGrow();
					if (primaryAge >= primarySphere.growTicks()) {
						primaryPhase = PrimaryPhase.SHRINK;
						primaryAge = 0;
					}
				}
				case SHRINK -> {
					if (corePhase == CorePhase.DORMANT) {
						int remaining = Math.max(0, primarySphere.shrinkTicks() - primaryAge);
						if (remaining <= coreSphereOverlapTicks) {
							startCoreSphere(client);
						}
					}
					updatePrimaryShrink();
					if (primaryAge >= primarySphere.shrinkTicks()) {
						primaryPhase = PrimaryPhase.INACTIVE;
						primaryScale = primarySphere.minScale();
						primaryAlpha = primarySphere.shrinkEndAlpha();
					}
				}
				case INACTIVE -> {
					if (corePhase == CorePhase.DORMANT) {
						startCoreSphere(client);
					}
				}
			}
		}

		private void setCorePhase(CorePhase next) {
			if (corePhase == next) {
				return;
			}
			corePhase = next;
			coreAge = 0;
		}

		private void updatePrimaryGrow() {
			float progress = MathHelper.clamp(primaryAge / (float) Math.max(1, primarySphere.growTicks()), 0.0F, 1.0F);
			primaryScale = MathHelper.lerp(progress, primarySphere.minScale(), primarySphere.maxScale());
			primaryAlpha = MathHelper.lerp(progress, primarySphere.growStartAlpha(), primarySphere.growEndAlpha());
			primaryAge++;
		}

		private void updatePrimaryShrink() {
			float progress = MathHelper.clamp(primaryAge / (float) Math.max(1, primarySphere.shrinkTicks()), 0.0F, 1.0F);
			primaryScale = MathHelper.lerp(progress, primarySphere.maxScale(), primarySphere.minScale());
			primaryAlpha = MathHelper.lerp(progress, primarySphere.shrinkStartAlpha(), primarySphere.shrinkEndAlpha());
			primaryAge++;
		}

		private void updateCore() {
			switch (corePhase) {
				case DORMANT -> {
					coreScale = 0.0F;
					coreAlpha = 0.0F;
				}
				case GROW -> {
					float progress = MathHelper.clamp(coreAge / (float) Math.max(1, coreSphere.growTicks()), 0.0F, 1.0F);
					coreScale = MathHelper.lerp(progress, coreSphere.minScale(), coreSphere.maxScale());
					coreAlpha = MathHelper.lerp(progress, coreSphere.growStartAlpha(), coreSphere.growEndAlpha());
					coreAge++;
					if (coreAge >= coreSphere.growTicks()) {
						coreScale = coreSphere.maxScale();
						coreAlpha = coreSphere.growEndAlpha();
						setCorePhase(CorePhase.IDLE);
					}
				}
				case IDLE -> {
					coreScale = coreSphere.maxScale();
					coreAlpha = coreSphere.growEndAlpha();
				}
			}
		}

		private void updateBeam() {
			boolean coreActive = corePhase != CorePhase.DORMANT;
			boolean allowBlack = !coreActive && beamTicks >= blackBeamDelayTicks && primaryPhase != PrimaryPhase.INACTIVE;
			float targetAlpha = coreActive ? coreBeamAlpha : allowBlack ? primaryBeamAlpha : 0.0F;
			if (beamAlpha < targetAlpha) {
				beamAlpha = Math.min(targetAlpha, beamAlpha + beamAlphaStep);
			} else {
				beamAlpha = Math.max(targetAlpha, beamAlpha - beamAlphaStep);
			}
			float targetScale = targetAlpha > 0.0F ? 1.0F : 0.0F;
			float growth = coreActive ? redBeamGrowth : blackBeamGrowth;
			if (beamScale < targetScale) {
				beamScale = Math.min(targetScale, beamScale + growth);
			} else {
				beamScale = Math.max(targetScale, beamScale - blackBeamGrowth);
			}
		}

		private void startCoreSphere(MinecraftClient client) {
			if (corePhase != CorePhase.DORMANT) {
				return;
			}
			setCorePhase(CorePhase.GROW);
			coreScale = coreSphere.minScale();
			coreAlpha = coreSphere.growStartAlpha();
			beamTint = BeamTint.RED;
			if (!beamCuePlayed) {
				playCoreCue(client);
				beamCuePlayed = true;
			}
		}

		private void playCoreCue(MinecraftClient client) {
			if (client.world == null) {
				return;
			}
			Vec3d center = Vec3d.ofCenter(origin);
			if (client.player != null) {
				client.world.playSound(
						client.player,
						center.x,
						center.y,
						center.z,
						SoundEvents.BLOCK_BEACON_ACTIVATE,
						SoundCategory.BLOCKS,
						3.0F,
						0.65F);
			}
		}

		public BlockPos getOrigin() {
			return origin;
		}

		public float getPhaseOffset() {
			return phaseOffset;
		}

		public boolean isPrimarySphereVisible() {
			return primaryPhase != PrimaryPhase.INACTIVE && primaryAlpha > 0.01F;
		}

		public Identifier getPrimaryTexture() {
			return primarySphere.texture();
		}

		public float getPrimaryScale() {
			return primaryScale;
		}

		public float getPrimaryAlpha() {
			return primaryAlpha;
		}

		public float getPrimarySpinMultiplier() {
			return primarySphere.spinMultiplier();
		}

		public boolean isCoreSphereVisible() {
			return corePhase != CorePhase.DORMANT && coreAlpha > 0.01F;
		}

		public Identifier getCoreTexture() {
			return coreSphere.texture();
		}

		public float getCoreScale() {
			return coreScale;
		}

		public float getCoreAlpha() {
			return coreAlpha;
		}

		public float getCoreSpinMultiplier() {
			return coreSphere.spinMultiplier();
		}

		public boolean isBeamVisible() {
			return beamAlpha > 0.01F && beamScale > 0.01F;
		}

		public float getBeamAlpha() {
			return beamAlpha;
		}

		public float getBeamScale() {
			return beamScale;
		}

		public int getBeamInnerColor() {
			return ColorConfig.argb(resolveBeamSlot());
		}

		public int getBeamOuterColor() {
			return ColorConfig.color(resolveBeamSlot()).withAlpha(0x66);
		}

		private ColorSlot resolveBeamSlot() {
			return beamTint == BeamTint.BLACK ? ColorSlot.SINGULARITY_BEAM_SECONDARY : ColorSlot.SINGULARITY_BEAM_PRIMARY;
		}
	}

	private static void reloadConfigInternal() {
		SingularityVisualConfig loaded = SingularityVisualStore.load();
		applyConfig(loaded);
	}

	private static void applyConfig(SingularityVisualConfig config) {
		CONFIG = config;
		sphereLatSteps = config.latSteps();
		sphereLonSteps = config.lonSteps();
		coreSphereOverlapTicks = config.coreOverlapTicks();
		innerBeamRadius = config.innerBeamRadius();
		outerBeamRadius = config.outerBeamRadius();
		blackBeamDelayTicks = config.blackBeamDelayTicks();
		primaryBeamAlpha = config.primaryBeamAlpha();
		coreBeamAlpha = config.coreBeamAlpha();
		blackBeamGrowth = config.blackBeamGrowth();
		redBeamGrowth = config.redBeamGrowth();
		beamAlphaStep = config.beamAlphaStep();
		primarySphere = toSphereProfile(config.primary(), DEFAULT_PRIMARY_TEXTURE);
		coreSphere = toSphereProfile(config.core(), DEFAULT_CORE_TEXTURE);
	}

	private static SphereProfile toSphereProfile(SingularityVisualConfig.SphereConfig cfg, Identifier fallbackTexture) {
		Identifier texture = cfg.textureId();
		if (texture == null) {
			texture = fallbackTexture;
		}
		return new SphereProfile(
				cfg.minScale(),
				cfg.maxScale(),
				cfg.growStartAlpha(),
				cfg.growEndAlpha(),
				cfg.shrinkStartAlpha(),
				cfg.shrinkEndAlpha(),
				cfg.growTicks(),
				cfg.shrinkTicks(),
				cfg.spinMultiplier(),
				texture);
	}

	private static void renderSphereMesh(Entry entry, VertexConsumer consumer, float alpha) {
		for (int lat = 0; lat < sphereLatSteps; lat++) {
			float v0 = lat / (float) sphereLatSteps;
			float v1 = (lat + 1) / (float) sphereLatSteps;
			float theta0 = v0 * MathHelper.PI;
			float theta1 = v1 * MathHelper.PI;
			for (int lon = 0; lon < sphereLonSteps; lon++) {
				float u0 = lon / (float) sphereLonSteps;
				float u1 = (lon + 1) / (float) sphereLonSteps;
				float phi0 = u0 * MathHelper.TAU;
				float phi1 = u1 * MathHelper.TAU;
				addSphereQuad(entry, consumer, theta0, theta1, phi0, phi1, u0, u1, v0, v1, alpha);
			}
		}
	}

	private static void addSphereQuad(Entry entry, VertexConsumer consumer,
	                                  float thetaTop, float thetaBottom,
	                                  float phiLeft, float phiRight,
	                                  float uLeft, float uRight,
	                                  float vTop, float vBottom,
	                                  float alpha) {
		// triangle 1: top-left → top-right → bottom-right
		emitSphereVertex(entry, consumer, thetaTop, phiLeft, uLeft, vTop, alpha);
		emitSphereVertex(entry, consumer, thetaTop, phiRight, uRight, vTop, alpha);
		emitSphereVertex(entry, consumer, thetaBottom, phiRight, uRight, vBottom, alpha);

		// triangle 2: bottom-right → bottom-left → top-left
		emitSphereVertex(entry, consumer, thetaBottom, phiRight, uRight, vBottom, alpha);
		emitSphereVertex(entry, consumer, thetaBottom, phiLeft, uLeft, vBottom, alpha);
		emitSphereVertex(entry, consumer, thetaTop, phiLeft, uLeft, vTop, alpha);
	}

	private static void emitSphereVertex(Entry entry, VertexConsumer consumer, float theta, float phi, float u, float v, float alpha) {
		float sinTheta = MathHelper.sin(theta);
		float x = sinTheta * MathHelper.cos(phi);
		float y = MathHelper.cos(theta);
		float z = sinTheta * MathHelper.sin(phi);
		float pulse = 0.1F * MathHelper.sin(phi * 4.0F);
		float r = MathHelper.clamp(0.85F + pulse, 0.0F, 1.0F);
		float g = MathHelper.clamp(0.15F + pulse * 0.3F, 0.0F, 0.7F);
		float b = MathHelper.clamp(0.25F + pulse * 0.2F, 0.0F, 0.9F);
		int ia = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
		int ir = MathHelper.clamp((int) (r * 255.0F), 0, 255);
		int ig = MathHelper.clamp((int) (g * 255.0F), 0, 255);
		int ib = MathHelper.clamp((int) (b * 255.0F), 0, 255);
		consumer.vertex(entry.getPositionMatrix(), x, y, z)
				.color(ir, ig, ib, ia)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(entry, x, y, z);
	}

	private static int colorWithAlpha(int argb, float alphaScale) {
		int alpha = MathHelper.clamp((int) (((argb >>> 24) & 0xFF) * alphaScale), 0, 255);
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	private enum PrimaryPhase {
		GROW,
		SHRINK,
		INACTIVE
	}

	private enum CorePhase {
		DORMANT,
		GROW,
		IDLE
	}

	private enum BeamTint {
		BLACK,
		RED
	}

	private record SphereProfile(
			float minScale,
			float maxScale,
			float growStartAlpha,
			float growEndAlpha,
			float shrinkStartAlpha,
			float shrinkEndAlpha,
			int growTicks,
			int shrinkTicks,
			float spinMultiplier,
			Identifier texture) {
	}
}

