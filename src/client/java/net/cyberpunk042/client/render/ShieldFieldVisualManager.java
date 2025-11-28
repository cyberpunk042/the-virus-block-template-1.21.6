package net.cyberpunk042.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.client.render.util.BeaconBeamRenderer;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class ShieldFieldVisualManager {
	private static final Identifier TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/misc/anti-virus-shield.png");
	private static final Identifier BEAM_TEXTURE = BeaconBlockEntityRenderer.BEAM_TEXTURE;
	private static final float PERSONAL_RADIUS_SCALE = 0.5F;
	private static final double PERSONAL_CENTER_SHIFT = -0.1D;
	private static final Long2ObjectOpenHashMap<ShieldVisual> ACTIVE = new Long2ObjectOpenHashMap<>();
	private static final Map<String, ShieldProfileConfig> BUILTIN_PROFILES = new LinkedHashMap<>();
	private static final List<String> BUILTIN_ORDER = new ArrayList<>();
	private static final Map<String, ShieldProfileConfig> PERSONAL_BUILTINS = PersonalShieldProfileStore.getBuiltinProfiles();
	private static final List<String> PERSONAL_BUILTIN_ORDER = PersonalShieldProfileStore.getBuiltinOrder();
	private static long localVisualId = Long.MIN_VALUE;
	private static String activeProfileName;
	private static ShieldProfileConfig currentProfile;
	private static String personalProfileName;
	private static ShieldProfileConfig personalProfile;
	private static ShieldVisual personalVisual;
	private static boolean personalShieldEnabled;
	private static boolean personalShieldVisible = true;
	private static boolean personalUsesAutoScale;
	private static float personalOverrideRadius;
	private static int personalProfileRevision;
	private static PersonalFollowMode personalFollowMode = PersonalFollowMode.SMOOTH;
	private static EditTarget editTarget = EditTarget.WORLD;

	public enum EditTarget {
		WORLD,
		PERSONAL
	}

	static {
		BUILTIN_PROFILES.putAll(ShieldProfileStore.getBuiltinProfiles());
		BUILTIN_ORDER.addAll(ShieldProfileStore.getBuiltinOrder());
		if (BUILTIN_ORDER.isEmpty()) {
			String fallback = "default";
			BUILTIN_ORDER.add(fallback);
			BUILTIN_PROFILES.put(fallback, ShieldProfileConfig.defaults());
		}
		String first = BUILTIN_ORDER.get(0);
		activeProfileName = first;
		currentProfile = BUILTIN_PROFILES.getOrDefault(first, ShieldProfileConfig.defaults()).copy();
		if (PERSONAL_BUILTIN_ORDER.isEmpty()) {
			personalProfileName = "personal-default";
			personalProfile = ShieldProfileConfig.defaults();
		} else {
			String personalFirst = PERSONAL_BUILTIN_ORDER.get(0);
			personalProfileName = personalFirst;
			personalProfile = PERSONAL_BUILTINS.getOrDefault(personalFirst, ShieldProfileConfig.defaults()).copy();
		}
		disablePersonalBeam(personalProfile);
		applyPersonalPredictionDefaults(personalProfile);
		personalProfileRevision = 0;
		personalUsesAutoScale = true;
		personalOverrideRadius = 0.0F;
	}

	private ShieldFieldVisualManager() {
	}

	public static EditTarget getEditTarget() {
		return editTarget;
	}

	public static void setEditTarget(EditTarget target) {
		editTarget = target == null ? EditTarget.WORLD : target;
	}

	public static boolean isEditingPersonal() {
		return editTarget == EditTarget.PERSONAL;
	}

	public static boolean isPersonalShieldVisible() {
		return personalShieldVisible;
	}

	public static void setPersonalShieldVisible(boolean visible) {
		personalShieldVisible = visible;
	}

	public static PersonalFollowMode getPersonalFollowMode() {
		return personalFollowMode;
	}

	public static void setPersonalFollowMode(PersonalFollowMode mode) {
		if (mode != null) {
			personalFollowMode = mode;
		}
	}

	public static boolean setPersonalFollowMode(String name) {
		PersonalFollowMode mode = PersonalFollowMode.fromName(name);
		if (mode == null) {
			return false;
		}
		personalFollowMode = mode;
		return true;
	}

	public static List<String> getPersonalFollowModeNames() {
		List<String> names = new ArrayList<>();
		for (PersonalFollowMode mode : PersonalFollowMode.values()) {
			names.add(mode.id());
		}
		return names;
	}

	public static ShieldProfileConfig getEditableProfile() {
		return isEditingPersonal() ? personalProfile : currentProfile;
	}

	public static Iterable<String> getEditableLayerIds() {
		return getEditableProfile().meshLayers().keySet();
	}

	public static Iterable<String> getEditableConfigKeys() {
		return getEditableProfile().knownKeys();
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
			Iterator<Long2ObjectMap.Entry<ShieldVisual>> iterator = ACTIVE.long2ObjectEntrySet().iterator();
			while (iterator.hasNext()) {
				ShieldVisual visual = iterator.next().getValue();
				if (visual.tick()) {
					iterator.remove();
				}
			}
			updatePersonalShield(client);
		});
		WorldRenderEvents.AFTER_ENTITIES.register(ShieldFieldVisualManager::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(() -> {
					ACTIVE.clear();
					personalVisual = null;
					personalShieldEnabled = false;
				}));
	}

	private static void handleSpawn(ShieldFieldSpawnPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}
		ShieldProfileConfig config = currentProfile.copy();
		config.setRadius(payload.radius());
		long id = payload.id();
		ShieldVisual visual = new ShieldVisual(
				new Vec3d(payload.x(), payload.y(), payload.z()),
				config,
				false,
				-1,
				client.world.random.nextFloat() * MathHelper.TAU);
		ACTIVE.put(id, visual);
	}

	private static void handleRemove(ShieldFieldRemovePayload payload) {
		ACTIVE.remove(payload.id());
	}

	private static void render(WorldRenderContext context) {
		if (ACTIVE.isEmpty() && (!personalShieldEnabled || personalVisual == null)) {
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
			ShieldProfileConfig config = visual.config;
			float scale = Math.max(0.5F, visual.radius() * config.visualScale());
			float rotation = (visual.age * config.spinSpeed()) + visual.phase;

			matrices.push();
			matrices.translate(visual.position.x - camPos.x, visual.position.y - camPos.y, visual.position.z - camPos.z);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotation * config.tiltMultiplier()));
			matrices.scale(scale, scale, scale);
			VertexConsumer consumer = consumers.getBuffer(layer);
			renderSphere(matrices.peek(), consumer, visual, config, alpha * 0.92F);
			matrices.pop();

			renderBeam(context, visual, config);
		}

		if (personalShieldEnabled && personalShieldVisible && personalVisual != null) {
			float alpha = personalVisual.alpha();
			if (alpha > 0.02F) {
				ShieldProfileConfig config = personalVisual.config;
				float scale = Math.max(0.5F, personalVisual.radius() * config.visualScale());
				float rotation = (personalVisual.age * config.spinSpeed()) + personalVisual.phase;

				matrices.push();
				matrices.translate(personalVisual.position.x - camPos.x, personalVisual.position.y - camPos.y, personalVisual.position.z - camPos.z);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
				matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotation * config.tiltMultiplier()));
				matrices.scale(scale, scale, scale);
				VertexConsumer consumer = consumers.getBuffer(layer);
				renderSphere(matrices.peek(), consumer, personalVisual, config, alpha * 0.92F);
				matrices.pop();

				renderBeam(context, personalVisual, config);
			}
		}
	}

	private static void renderBeam(WorldRenderContext context, ShieldVisual visual, ShieldProfileConfig config) {
		if (context.world() == null || !config.beamEnabled()) {
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
		int color = config.beamColor();
		int innerColor = (0x64000000 | (color & 0x00FFFFFF));
		BeaconBeamRenderer.render(
				matrices,
				consumers,
				BEAM_TEXTURE,
				config.beamInnerRadius(),
				config.beamOuterRadius(),
				remaining,
				worldTime,
				tickDelta,
				color,
				innerColor);
		matrices.pop();
	}

	private static void renderSphere(Entry entry, VertexConsumer consumer, ShieldVisual visual, ShieldProfileConfig config, float alpha) {
		Matrix4f matrix = entry.getPositionMatrix();
		Matrix3f normalMatrix = entry.getNormalMatrix();
		config.meshLayers().forEach((id, layer) ->
				renderLayer(matrix, normalMatrix, consumer, visual, config, id, layer, alpha * 0.92F));
	}

	private static void renderLayer(Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer consumer,
	                                ShieldVisual visual, ShieldProfileConfig profile, String layerId,
	                                ShieldMeshLayerConfig layer, float baseAlpha) {
		int latSteps = MathHelper.clamp(layer.latSteps(), 2, 512);
		int lonSteps = MathHelper.clamp(layer.lonSteps(), 4, 1024);
		float latStart = layer.latStart();
		float latRange = Math.max(0.001F, layer.latEnd() - latStart);
		float lonStart = layer.lonStart();
		float lonRange = Math.max(0.001F, layer.lonEnd() - layer.lonStart());
		float radiusScale = Math.max(0.01F, layer.radiusMultiplier());
		float basePhase = visual.phase + layer.phaseOffset();
		ShieldTriangleTypeConfig triangleType = ShieldTriangleTypeStore.getOrDefault(
				profile.layerTriangles().getOrDefault(layerId, "default"));

		for (int lat = 0; lat < latSteps; lat++) {
			float lat0Norm = latStart + (lat / (float) latSteps) * latRange;
			float lat1Norm = latStart + ((lat + 1) / (float) latSteps) * latRange;
			float relLat0 = (lat0Norm - latStart) / latRange;
			float relLat1 = (lat1Norm - latStart) / latRange;
			float relLatMid = (relLat0 + relLat1) * 0.5F;
			float theta0 = lat0Norm * MathHelper.PI;
			float theta1 = lat1Norm * MathHelper.PI;
			float layerAlpha = MathHelper.lerp(relLatMid, layer.alphaMin(), layer.alphaMax());
			float finalAlpha = MathHelper.clamp(baseAlpha * layerAlpha, 0.0F, 1.0F);
			if (finalAlpha <= 0.01F) {
				continue;
			}
			int color = withAlpha(lerpColor(layer.primaryColor(), layer.secondaryColor(), relLatMid), finalAlpha);
			for (int lon = 0; lon < lonSteps; lon++) {
				float lon0Norm = lonStart + (lon / (float) lonSteps) * lonRange;
				float lon1Norm = lonStart + ((lon + 1) / (float) lonSteps) * lonRange;
				float relLonMid = ((lon0Norm + lon1Norm) * 0.5F - lonStart) / lonRange;
				if (!layer.shouldRenderCell(lat, latSteps, lon, lonSteps, relLatMid, relLonMid)) {
					continue;
				}
				float swirl = layer.swirlStrength();
				float swirlOffset = swirl * (relLatMid - 0.5F);
				float phi0 = (lon0Norm * MathHelper.TAU) + basePhase + swirlOffset;
				float phi1 = (lon1Norm * MathHelper.TAU) + basePhase + swirlOffset;
				float u0 = lon0Norm;
				float u1 = lon1Norm;
				float v0 = lat0Norm;
				float v1 = lat1Norm;

				for (ShieldTriangleTypeConfig.Corner[] tri : triangleType.triangles()) {
					addCornerVertex(matrix, normalMatrix, consumer, tri[0], theta0, theta1, phi0, phi1, u0, u1, v0, v1, radiusScale, color);
					addCornerVertex(matrix, normalMatrix, consumer, tri[1], theta0, theta1, phi0, phi1, u0, u1, v0, v1, radiusScale, color);
					addCornerVertex(matrix, normalMatrix, consumer, tri[2], theta0, theta1, phi0, phi1, u0, u1, v0, v1, radiusScale, color);
				}
			}
		}
	}

	private static void addCornerVertex(Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer consumer,
	                                    ShieldTriangleTypeConfig.Corner corner,
	                                    float theta0, float theta1, float phi0, float phi1,
	                                    float u0, float u1, float v0, float v1,
	                                    float radiusScale, int color) {
		float theta = switch (corner) {
			case TOP_LEFT, TOP_RIGHT -> theta0;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> theta1;
		};
		float phi = switch (corner) {
			case TOP_LEFT, BOTTOM_LEFT -> phi0;
			case TOP_RIGHT, BOTTOM_RIGHT -> phi1;
		};
		float u = switch (corner) {
			case TOP_LEFT, BOTTOM_LEFT -> u0;
			case TOP_RIGHT, BOTTOM_RIGHT -> u1;
		};
		float v = switch (corner) {
			case TOP_LEFT, TOP_RIGHT -> v0;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> v1;
		};
		addVertex(matrix, normalMatrix, consumer, theta, phi, u, v, radiusScale, color);
	}

	private static void addVertex(Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer consumer,
	                              float theta, float phi, float u, float v, float radiusScale, int color) {
		float sinTheta = MathHelper.sin(theta);
		float x = sinTheta * MathHelper.cos(phi);
		float y = MathHelper.cos(theta);
		float z = sinTheta * MathHelper.sin(phi);
		Vector4f position = new Vector4f(x * radiusScale, y * radiusScale, z * radiusScale, 1.0F);
		position.mul(matrix);
		Vector3f normal = new Vector3f(x, y, z);
		normal.mul(normalMatrix);
		consumer.vertex(position.x(), position.y(), position.z(), color, u, v,
				OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE,
				normal.x(), normal.y(), normal.z());
	}

	private static int lerpColor(int start, int end, float progress) {
		float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
		int sa = (start >>> 24) & 0xFF;
		int sr = (start >>> 16) & 0xFF;
		int sg = (start >>> 8) & 0xFF;
		int sb = start & 0xFF;
		int ea = (end >>> 24) & 0xFF;
		int er = (end >>> 16) & 0xFF;
		int eg = (end >>> 8) & 0xFF;
		int eb = end & 0xFF;
		int a = MathHelper.floor(MathHelper.lerp(clamped, sa, ea));
		int r = MathHelper.floor(MathHelper.lerp(clamped, sr, er));
		int g = MathHelper.floor(MathHelper.lerp(clamped, sg, eg));
		int b = MathHelper.floor(MathHelper.lerp(clamped, sb, eb));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int withAlpha(int color, float alpha) {
		int a = MathHelper.floor(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F) & 0xFF;
		return (color & 0x00FFFFFF) | (a << 24);
	}

	private static void updatePersonalShield(MinecraftClient client) {
		if (!personalShieldEnabled) {
			return;
		}
		if (client.player == null || client.world == null) {
			personalVisual = null;
			return;
		}
		Vec3d targetPos = resolvePersonalAnchor(client.player);
		boolean autoScale = personalUsesAutoScale;
		float baseRadius = autoScale ? personalProfile.radius()
				: (personalOverrideRadius > 0.0F ? personalOverrideRadius : personalProfile.radius());
		if (personalVisual == null) {
			float phase = client.world.random.nextFloat() * MathHelper.TAU;
			ShieldProfileConfig config = createPersonalProfile(baseRadius, autoScale);
			personalVisual = new ShieldVisual(targetPos, config, true, -1, phase, personalProfileRevision);
			return;
		}
		if (personalVisual.profileRevision != personalProfileRevision) {
			float phase = personalVisual.phase;
			ShieldProfileConfig config = createPersonalProfile(baseRadius, autoScale);
			personalVisual = new ShieldVisual(targetPos, config, true, -1, phase, personalProfileRevision);
		}
		Vec3d currentPos = personalVisual.position;
		Vec3d nextPos = personalFollowMode.interpolate(currentPos, targetPos);
		personalVisual.setPosition(nextPos);
		personalVisual.tick();
	}

	private static Vec3d resolvePersonalAnchor(PlayerEntity player) {
		Vec3d center = player.getBoundingBox().getCenter().add(0.0D, PERSONAL_CENTER_SHIFT, 0.0D);
		if (!personalProfile.predictionEnabled()) {
			return center;
		}
		return applyPersonalPrediction(player, center);
	}

	private static Vec3d applyPersonalPrediction(PlayerEntity player, Vec3d base) {
		double lead = MathHelper.clamp(personalProfile.predictionLeadTicks(), 0, 60);
		if (lead <= 0.0D) {
			return base;
		}
		Vec3d predicted = base.add(player.getVelocity().multiply(lead));
		float lookAhead = personalProfile.predictionLookAhead();
		if (lookAhead != 0.0F) {
			Vec3d look = player.getRotationVec(1.0F).normalize();
			predicted = predicted.add(look.multiply(lookAhead));
		}
		float vertical = personalProfile.predictionVerticalBoost();
		if (vertical != 0.0F) {
			predicted = predicted.add(0.0D, vertical, 0.0D);
		}
		float maxDistance = personalProfile.predictionMaxDistance();
		if (maxDistance > 0.01F) {
			Vec3d delta = predicted.subtract(base);
			double maxSq = maxDistance * maxDistance;
			if (delta.lengthSquared() > maxSq) {
				predicted = base.add(delta.normalize().multiply(maxDistance));
			}
		}
		return predicted;
	}

	public enum PersonalFollowMode {
		SNAP("snap", 1.0F),
		SMOOTH("smooth", 0.35F),
		GLIDE("glide", 0.2F);

		private final String id;
		private final float lerpFactor;

		PersonalFollowMode(String id, float lerpFactor) {
			this.id = id;
			this.lerpFactor = lerpFactor;
		}

		public String id() {
			return id;
		}

		public float lerpFactor() {
			return lerpFactor;
		}

		public Vec3d interpolate(Vec3d current, Vec3d target) {
			if (current == null || lerpFactor >= 0.999F) {
				return target;
			}
			return current.lerp(target, lerpFactor);
		}

		public static PersonalFollowMode fromName(String name) {
			if (name == null) {
				return null;
			}
			String key = name.toLowerCase(Locale.ROOT);
			for (PersonalFollowMode mode : values()) {
				if (mode.id.equals(key)) {
					return mode;
				}
			}
			return null;
		}
	}

	private static final class ShieldVisual {
		Vec3d position;
		final ShieldProfileConfig config;
		final boolean local;
		final int maxLifeTicks;
		final float phase;
		final int profileRevision;
		private int age;

		ShieldVisual(Vec3d position, ShieldProfileConfig config, boolean local, int maxLifeTicks, float phase) {
			this(position, config, local, maxLifeTicks, phase, 0);
		}

		ShieldVisual(Vec3d position, ShieldProfileConfig config, boolean local, int maxLifeTicks, float phase, int profileRevision) {
			this.position = position;
			this.config = config;
			this.local = local;
			this.maxLifeTicks = maxLifeTicks;
			this.phase = phase;
			this.profileRevision = profileRevision;
		}

		boolean tick() {
			age++;
			return local && maxLifeTicks > 0 && age >= maxLifeTicks;
		}

		float radius() {
			return Math.max(0.5F, config.radius());
		}

		float alpha() {
			float sine = 0.5F + 0.5F * MathHelper.sin((age * 0.04F) + phase);
			return MathHelper.lerp(sine, config.minAlpha(), config.maxAlpha());
		}

		void setPosition(Vec3d position) {
			this.position = position;
		}
	}

	private static void applyProfile(String name, ShieldProfileConfig profile) {
		currentProfile = profile.copy();
		activeProfileName = normalizeName(name);
	}

	private static ShieldProfileConfig createPersonalProfile(float radius, boolean applyScale) {
		float scaled = applyScale ? Math.max(0.5F, radius * PERSONAL_RADIUS_SCALE) : Math.max(0.5F, radius);
		ShieldProfileConfig config = personalProfile.copy();
		config.setBeamEnabled(false);
		config.setBeamRadii(0.0F, 0.0F);
		config.setRadius(scaled);
		return config;
	}

	public static boolean applyPreset(String name) {
		String key = normalizeName(name);
		ShieldProfileConfig preset = BUILTIN_PROFILES.get(key);
		if (preset == null) {
			return false;
		}
		applyProfile(key, preset);
		return true;
	}

	public static String cyclePreset() {
		int index = BUILTIN_ORDER.indexOf(activeProfileName);
		if (index < 0) {
			index = 0;
		}
		int next = (index + 1) % BUILTIN_ORDER.size();
		String name = BUILTIN_ORDER.get(next);
		applyProfile(name, BUILTIN_PROFILES.get(name));
		return activeProfileName;
	}

	public static boolean setLatSteps(int value) {
		if (value < 8 || value > 640) {
			return false;
		}
		currentProfile.setLatSteps(value);
		markCustom();
		return true;
	}

	public static boolean setLonSteps(int value) {
		if (value < 16 || value > 960) {
			return false;
		}
		currentProfile.setLonSteps(value);
		markCustom();
		return true;
	}

	public static boolean setSwirlStrength(float value) {
		if (value < -8.0F || value > 8.0F) {
			return false;
		}
		currentProfile.setSwirlStrength(value);
		markCustom();
		return true;
	}

	public static Iterable<String> getPersonalLayerIds() {
		return personalProfile.meshLayers().keySet();
	}

	public static Iterable<String> getPersonalConfigKeys() {
		return personalProfile.knownKeys();
	}

	public static List<String> describePersonalLayers() {
		return describeLayersFor(personalProfile);
	}

	public static boolean addPersonalMeshLayer(String id) {
		boolean added = personalProfile.addLayer(id);
		if (added) {
			markPersonalCustom();
		}
		return added;
	}

	public static boolean removePersonalMeshLayer(String id) {
		boolean removed = personalProfile.removeLayer(id);
		if (removed) {
			markPersonalCustom();
		}
		return removed;
	}

	public static boolean setPersonalLayerValue(String id, String key, String value) {
		try {
			boolean updated = personalProfile.setLayerValue(id, key, value);
			if (updated) {
				markPersonalCustom();
				return true;
			}
			return false;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public static boolean setPersonalLatSteps(int value) {
		if (value < 8 || value > 640) {
			return false;
		}
		personalProfile.setLatSteps(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalLonSteps(int value) {
		if (value < 16 || value > 960) {
			return false;
		}
		personalProfile.setLonSteps(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalSwirlStrength(float value) {
		if (value < -8.0F || value > 8.0F) {
			return false;
		}
		personalProfile.setSwirlStrength(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalScale(float value) {
		if (value < 0.05F || value > 2.0F) {
			return false;
		}
		personalProfile.setVisualScale(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalSpinSpeed(float value) {
		if (value < -0.2F || value > 0.2F) {
			return false;
		}
		personalProfile.setSpinSpeed(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalTiltMultiplier(float value) {
		if (value < -1.0F || value > 1.0F) {
			return false;
		}
		personalProfile.setTiltMultiplier(value);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalConfigValue(String key, String rawValue) {
		boolean updated = personalProfile.setValue(key, rawValue);
		if (updated) {
			markPersonalCustom();
		}
		return updated;
	}

	public static void setPersonalPrimaryColor(int color) {
		personalProfile.setPrimaryColor(color);
		markPersonalCustom();
	}

	public static void setPersonalSecondaryColor(int color) {
		personalProfile.setSecondaryColor(color);
		markPersonalCustom();
	}

	public static void setPersonalBeamColor(int color) {
		personalProfile.setBeamColor(color);
		markPersonalCustom();
	}

	public static boolean setPersonalPredictionEnabled(boolean enabled) {
		personalProfile.setPredictionEnabled(enabled);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalPredictionLeadTicks(int ticks) {
		personalProfile.setPredictionLeadTicks(ticks);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalPredictionMaxDistance(float distance) {
		personalProfile.setPredictionMaxDistance(distance);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalPredictionLookAhead(float offset) {
		personalProfile.setPredictionLookAhead(offset);
		markPersonalCustom();
		return true;
	}

	public static boolean setPersonalPredictionVerticalBoost(float boost) {
		personalProfile.setPredictionVerticalBoost(boost);
		markPersonalCustom();
		return true;
	}

	public static boolean isPersonalPredictionEnabled() {
		return personalProfile.predictionEnabled();
	}

	public static int getPersonalPredictionLeadTicks() {
		return personalProfile.predictionLeadTicks();
	}

	public static float getPersonalPredictionMaxDistance() {
		return personalProfile.predictionMaxDistance();
	}

	public static float getPersonalPredictionLookAhead() {
		return personalProfile.predictionLookAhead();
	}

	public static float getPersonalPredictionVerticalBoost() {
		return personalProfile.predictionVerticalBoost();
	}

	public static String describePersonalPrediction() {
		return String.format(Locale.ROOT,
				"enabled=%s lead=%dt max=%.2f look=%.2f vertical=%.2f",
				personalProfile.predictionEnabled(),
				personalProfile.predictionLeadTicks(),
				personalProfile.predictionMaxDistance(),
				personalProfile.predictionLookAhead(),
				personalProfile.predictionVerticalBoost());
	}

	public static boolean savePersonalProfile(String name) {
		return PersonalShieldProfileStore.save(name, personalProfile);
	}

	public static boolean loadPersonalProfile(String name) {
		String key = normalizeName(name);
		return PersonalShieldProfileStore.load(name).map(config -> {
			personalProfile = config.copy();
			disablePersonalBeam(personalProfile);
			personalProfileName = key.isEmpty() ? "personal" : key;
			bumpPersonalRevision();
			return true;
		}).orElseGet(() -> applyPersonalPreset(key));
	}

	public static boolean applyPersonalPreset(String name) {
		String key = normalizeName(name);
		ShieldProfileConfig preset = PERSONAL_BUILTINS.get(key);
		if (preset == null) {
			return false;
		}
		personalProfile = preset.copy();
		disablePersonalBeam(personalProfile);
		personalProfileName = key;
		bumpPersonalRevision();
		return true;
	}

	public static void copyWorldProfileToPersonal() {
		personalProfile = currentProfile.copy();
		disablePersonalBeam(personalProfile);
		personalProfileName = normalizeName(activeProfileName + "-personal");
		bumpPersonalRevision();
	}

	private static void disablePersonalBeam(ShieldProfileConfig profile) {
		profile.setBeamEnabled(false);
		profile.setBeamRadii(0.0F, 0.0F);
	}

	private static void applyPersonalPredictionDefaults(ShieldProfileConfig profile) {
		profile.setPredictionEnabled(true);
		profile.setPredictionLeadTicks(2);
	}

	public static void enablePersonalShield(float radius) {
		personalShieldEnabled = true;
		personalUsesAutoScale = radius <= 0.0F;
		personalOverrideRadius = personalUsesAutoScale ? 0.0F : Math.max(0.5F, radius);
		float baseRadius = personalUsesAutoScale ? personalProfile.radius() : personalOverrideRadius;
		ShieldProfileConfig config = createPersonalProfile(baseRadius, personalUsesAutoScale);
		personalVisual = new ShieldVisual(Vec3d.ZERO, config, true, -1, 0.0F, personalProfileRevision);
	}

	public static void disablePersonalShield() {
		personalShieldEnabled = false;
		personalVisual = null;
		personalUsesAutoScale = false;
		personalOverrideRadius = 0.0F;
	}

	private static void markCustom() {
		activeProfileName = "custom";
	}

	private static void markPersonalCustom() {
		personalProfileName = "personal-custom";
		bumpPersonalRevision();
	}

	private static void bumpPersonalRevision() {
		personalProfileRevision++;
	}

	public static String getActivePresetName() {
		return activeProfileName;
	}

	public static Iterable<String> getLayerIds() {
		return currentProfile.meshLayers().keySet();
	}

	public static Iterable<String> getPersonalPresetNames() {
		return Collections.unmodifiableList(PERSONAL_BUILTIN_ORDER);
	}

	public static List<String> getSavedPersonalProfileNames() {
		return PersonalShieldProfileStore.listProfiles();
	}

	public static Iterable<String> getLayerKeySuggestions() {
		return ShieldMeshLayerConfig.knownKeys();
	}

	public static Map<String, ShieldMeshLayerConfig> getLayers() {
		return currentProfile.meshLayers();
	}

	public static boolean addMeshLayer(String id) {
		boolean added = currentProfile.addLayer(id);
		if (added) {
			markCustom();
		}
		return added;
	}

	public static boolean removeMeshLayer(String id) {
		boolean removed = currentProfile.removeLayer(id);
		if (removed) {
			markCustom();
		}
		return removed;
	}

	public static boolean setLayerConfigValue(String id, String key, String value) {
		boolean updated = currentProfile.setLayerValue(id, key, value);
		if (updated) {
			markCustom();
		}
		return updated;
	}

	public static List<String> describeLayers() {
		return describeLayersFor(currentProfile);
	}

	private static List<String> describeLayersFor(ShieldProfileConfig profile) {
		List<String> lines = new ArrayList<>();
		Map<String, String> styles = profile.layerStyles();
		Map<String, String> shapes = profile.layerShapes();
		Map<String, String> triangles = profile.layerTriangles();
		profile.meshLayers().forEach((id, layer) -> {
			String entry = String.format(Locale.ROOT,
					"%s -> style=%s shape=%s triangle=%s type=%s lat=%d lon=%d radius=%.2f",
					id,
					styles.getOrDefault(id, "custom"),
					shapes.getOrDefault(id, "custom"),
					triangles.getOrDefault(id, "default"),
					layer.meshType().name().toLowerCase(Locale.ROOT),
					layer.latSteps(),
					layer.lonSteps(),
					layer.radiusMultiplier());
			lines.add(entry);
		});
		return lines;
	}

	public static int getLatSteps() {
		return currentProfile.latSteps();
	}

	public static int getLonSteps() {
		return currentProfile.lonSteps();
	}

	public static float getSwirlStrength() {
		return currentProfile.swirlStrength();
	}

	public static float getVisualScale() {
		return currentProfile.visualScale();
	}

	public static float getSpinSpeed() {
		return currentProfile.spinSpeed();
	}

	public static float getTiltMultiplier() {
		return currentProfile.tiltMultiplier();
	}

	public static ShieldProfileConfig getCurrentProfile() {
		return currentProfile;
	}

	public static ShieldProfileConfig getPersonalProfile() {
		return personalProfile;
	}

	public static String getPersonalProfileName() {
		return personalProfileName;
	}

	public static Iterable<String> getPresetNames() {
		return Collections.unmodifiableList(BUILTIN_ORDER);
	}

	public static CompletableFuture<Suggestions> suggestPresetNames(SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(BUILTIN_ORDER.stream(), builder);
	}

	public static boolean setScale(float value) {
		if (value < 0.05F || value > 2.0F) {
			return false;
		}
		currentProfile.setVisualScale(value);
		markCustom();
		return true;
	}

	public static boolean setSpinSpeed(float value) {
		if (value < -0.2F || value > 0.2F) {
			return false;
		}
		currentProfile.setSpinSpeed(value);
		markCustom();
		return true;
	}

	public static boolean setTiltMultiplier(float value) {
		if (value < -1.0F || value > 1.0F) {
			return false;
		}
		currentProfile.setTiltMultiplier(value);
		markCustom();
		return true;
	}

	public static boolean setConfigValue(String key, String rawValue) {
		boolean updated = currentProfile.setValue(key, rawValue);
		if (updated) {
			markCustom();
		}
		return updated;
	}

	public static Iterable<String> getConfigKeys() {
		return currentProfile.knownKeys();
	}

	public static boolean saveCurrentProfile(String name) {
		return ShieldProfileStore.save(name, currentProfile);
	}

	public static boolean loadProfile(String name) {
		String key = normalizeName(name);
		return ShieldProfileStore.load(name).map(config -> {
			applyProfile(key, config);
			return true;
		}).orElseGet(() -> applyPreset(key));
	}

	public static Iterable<String> getSavedProfileNames() {
		return ShieldProfileStore.listProfiles();
	}

	public static void spawnLocal(Vec3d center, float radius, int durationTicks) {
		ShieldProfileConfig config = currentProfile.copy().setRadius(radius);
		long id = localVisualId++;
		float phase = MinecraftClient.getInstance().world == null ? 0.0F : MinecraftClient.getInstance().world.random.nextFloat() * MathHelper.TAU;
		ShieldVisual visual = new ShieldVisual(center, config, true, durationTicks, phase);
		ACTIVE.put(id, visual);
	}

	private static String normalizeName(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}

	public static void reloadActiveProfile() {
		if (activeProfileName == null || "custom".equals(activeProfileName)) {
			return;
		}
		String name = activeProfileName;
		if (!loadProfile(name)) {
			applyPreset(name);
		}
	}
}

