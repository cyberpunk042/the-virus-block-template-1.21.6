package net.cyberpunk042.infection.service;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.config.InfectionLogConfig.LogChannel;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SimpleEffectBus;
import net.minecraft.util.Identifier;

final class EffectBusTelemetry implements SimpleEffectBus.BusTelemetry {
	private final LoggingService logging;
	@Nullable
	private final VirusWorldState state;

	EffectBusTelemetry(LoggingService logging, @Nullable VirusWorldState state) {
		this.logging = logging;
		this.state = state;
	}

	@Override
	public void onRegister(Class<?> eventType, Consumer<?> handler) {
		log("register", eventType, handler);
	}

	@Override
	public void onUnregister(Class<?> eventType, Consumer<?> handler) {
		log("unregister", eventType, handler);
	}

	private void log(String action, Class<?> eventType, Consumer<?> handler) {
		String scenario = "unbound";
		if (state != null) {
			scenario = state.orchestrator().scenarios().activeId()
					.map(Identifier::toString)
					.orElse("unbound");
		}
		String eventName = eventType != null ? eventType.getSimpleName() : "unknown";
		String handlerName = handler != null ? handler.getClass().getSimpleName() : "anonymous";
		logging.info(LogChannel.EFFECTS, "[effectBus] {} scenario={} event={} handler={}",
				action, scenario, eventName, handlerName);
	}
}

