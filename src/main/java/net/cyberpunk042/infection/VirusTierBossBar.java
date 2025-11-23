package net.cyberpunk042.infection;

import java.util.List;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.network.SkyTintPayload;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class VirusTierBossBar {
	private static final ServerBossBar BAR = new ServerBossBar(
			Text.literal("Virus Tier"),
			BossBar.Color.PURPLE,
			BossBar.Style.NOTCHED_10);
	private static final Object2ByteMap<UUID> SKY_TINT = new Object2ByteOpenHashMap<>();

	private VirusTierBossBar() {
	}

	public static void init() {
		BAR.setVisible(false);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SKY_TINT.removeByte(handler.player.getUuid()));
	}

	public static void update(ServerWorld world, VirusWorldState state) {
		syncPlayers(world);
		syncSkyTint(world, state);

		if (!state.isInfected()) {
			BAR.setVisible(false);
			BAR.setPercent(0.0F);
			return;
		}

		BAR.setVisible(true);
		setTitle(state);
		setProgress(state);
		setColor(state);
	}

	private static void setTitle(VirusWorldState state) {
		int displayTier = state.getCurrentTier().getIndex() + 1;
		String key = state.isCalm() ? "bossbar.the-virus-block.state.calm" : "bossbar.the-virus-block.state.active";
		MutableText text = Text.translatable("bossbar.the-virus-block.tier", displayTier, Text.translatable(key));
		if (state.isCalm()) {
			text.formatted(Formatting.AQUA);
		} else {
			text.formatted(Formatting.DARK_PURPLE);
		}
		BAR.setName(text);
	}

	private static void setProgress(VirusWorldState state) {
		int duration = state.getCurrentTier().getDurationTicks();
		if (duration <= 0) {
			BAR.setPercent(1.0F);
			return;
		}

		float percent = Math.min(1.0F, (float) state.getTicksInTier() / (float) duration);
		BAR.setPercent(percent);
	}

	private static void setColor(VirusWorldState state) {
		switch (state.getCurrentTier().getIndex()) {
			case 0 -> BAR.setColor(BossBar.Color.BLUE);
			case 1 -> BAR.setColor(BossBar.Color.GREEN);
			case 2 -> BAR.setColor(BossBar.Color.YELLOW);
			case 3 -> BAR.setColor(BossBar.Color.PINK);
			default -> BAR.setColor(BossBar.Color.RED);
		}
	}

	private static void syncPlayers(ServerWorld world) {
		for (ServerPlayerEntity player : List.copyOf(BAR.getPlayers())) {
			if (player.getWorld() != world) {
				BAR.removePlayer(player);
			}
		}

		for (ServerPlayerEntity player : world.getPlayers()) {
			if (!BAR.getPlayers().contains(player)) {
				BAR.addPlayer(player);
			}
		}
	}

	private static void syncSkyTint(ServerWorld world, VirusWorldState state) {
		boolean skyCorrupted = state.isInfected() && (state.getCurrentTier().getIndex() >= 0 || state.isApocalypseMode());
		boolean fluidsCorrupted = state.isInfected()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_LIQUID_MUTATION_ENABLED)
				&& (state.getCurrentTier().getIndex() >= 2 || state.isApocalypseMode());
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

