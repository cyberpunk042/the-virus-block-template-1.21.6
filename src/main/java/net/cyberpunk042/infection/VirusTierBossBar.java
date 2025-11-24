package net.cyberpunk042.infection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.cyberpunk042.network.SkyTintPayload;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class VirusTierBossBar {
	private static final Map<RegistryKey<World>, ServerBossBar> BARS = new HashMap<>();
	private static final Object2ByteMap<UUID> SKY_TINT = new Object2ByteOpenHashMap<>();

	private VirusTierBossBar() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SKY_TINT.removeByte(handler.player.getUuid()));
	}

	public static void update(ServerWorld world, VirusWorldState state) {
		syncSkyTint(world, state);
		RegistryKey<World> key = world.getRegistryKey();
		ServerBossBar bar = BARS.get(key);
		if (!state.isInfected()) {
			if (bar != null) {
				clearBar(bar);
				BARS.remove(key);
			}
			return;
		}

		if (bar == null) {
			bar = createBar();
			BARS.put(key, bar);
		}

		syncPlayers(world, bar);
		bar.setVisible(true);
		updateBarSegments(bar, state);
	}

	private static ServerBossBar createBar() {
		ServerBossBar bar = new ServerBossBar(
				Text.literal("Virus Tier"),
				BossBar.Color.PURPLE,
				BossBar.Style.NOTCHED_10);
		bar.setVisible(false);
		return bar;
	}

	private static void clearBar(ServerBossBar bar) {
		bar.setVisible(false);
		bar.setPercent(0.0F);
		for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
			bar.removePlayer(player);
		}
	}

	private static void updateBarSegments(ServerBossBar bar, VirusWorldState state) {
		float progressRatio = getProgressRatio(state);
		float healthRatio = (float) state.getHealthPercent();
		int displayTier = state.getCurrentTier().getIndex() + 1;
		String key = state.isCalm() ? "bossbar.the-virus-block.state.calm" : "bossbar.the-virus-block.state.active";

		if (state.isApocalypseMode()) {
			MutableText vulnerableTitle = Text.translatable("bossbar.the-virus-block.vulnerable")
					.formatted(Formatting.RED)
					.append(Text.literal(String.format(" [%d%%]", Math.round(healthRatio * 100))).formatted(Formatting.DARK_RED));
			bar.setName(vulnerableTitle);
			bar.setPercent(MathHelper.clamp(healthRatio, 0.0F, 1.0F));
			bar.setColor(BossBar.Color.RED);
			return;
		}

		MutableText title = Text.translatable("bossbar.the-virus-block.tier", displayTier, Text.translatable(key))
				.formatted(state.isCalm() ? Formatting.AQUA : Formatting.DARK_PURPLE)
				.append(Text.literal(String.format("  [P:%d%%] ", Math.round(progressRatio * 100))).formatted(Formatting.AQUA))
				.append(Text.literal(String.format("[H:%d%%]", Math.round(healthRatio * 100))).formatted(Formatting.DARK_RED));
		bar.setName(title);

		bar.setPercent(progressRatio);
		bar.setColor(progressRatio >= 1.0F ? BossBar.Color.RED : BossBar.Color.PURPLE);
	}

	private static float getProgressRatio(VirusWorldState state) {
		int duration = state.getCurrentTierDuration();
		if (duration <= 0) {
			return 1.0F;
		}
		return MathHelper.clamp((float) state.getTicksInTier() / (float) duration, 0.0F, 1.0F);
	}

	private static void syncPlayers(ServerWorld world, ServerBossBar bar) {
		for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
			if (player.getWorld() != world) {
				bar.removePlayer(player);
			}
		}

		for (ServerPlayerEntity player : world.getPlayers()) {
			if (!bar.getPlayers().contains(player)) {
				bar.addPlayer(player);
			}
		}
	}

	private static void syncSkyTint(ServerWorld world, VirusWorldState state) {
		boolean skyCorrupted = state.isInfected() && (state.getCurrentTier().getIndex() >= 0 || state.isApocalypseMode());
		boolean fluidsCorrupted = state.areLiquidsCorrupted(world);
		byte encoded = encodeSkyState(skyCorrupted, fluidsCorrupted);
		for (ServerPlayerEntity player : world.getPlayers()) {
			byte cached = SKY_TINT.getOrDefault(player.getUuid(), (byte) 0);
			if (cached == encoded) {
				continue;
			}
			SKY_TINT.put(player.getUuid(), encoded);
			ServerPlayNetworking.send(player, new SkyTintPayload(skyCorrupted, fluidsCorrupted));
		}
	}

	private static byte encodeSkyState(boolean sky, boolean fluids) {
		byte value = 0;
		if (sky) {
			value |= 1;
		}
		if (fluids) {
			value |= 2;
		}
		return value;
	}
}

