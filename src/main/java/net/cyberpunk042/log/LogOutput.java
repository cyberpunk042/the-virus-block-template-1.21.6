package net.cyberpunk042.log;

import net.cyberpunk042.TheVirusBlock;
import org.slf4j.Logger;

/**
 * Central output pipeline. All log messages flow through here.
 * 
 * Pipeline:
 * 1. Watchdog check (spam detection)
 * 2. Format message
 * 3. Output to TheVirusBlock.LOGGER
 * 4. Forward to chat if enabled
 */
public final class LogOutput {
    
    private static final Logger LOGGER = TheVirusBlock.LOGGER;
    
    public static void emit(Channel channel, String topic, LogLevel level, 
                           String message, Throwable exception) {
        emit(channel, topic, level, message, exception, false);
    }
    
    public static void emit(Channel channel, String topic, LogLevel level, 
                           String message, Throwable exception, boolean forceChat) {
        
        // 1. Watchdog check
        WatchdogDecision decision = LogWatchdog.observe(channel.id(), topic, message);
        if (decision.suppress()) {
            if (decision.summary() != null) {
                // Log suppression summary
                doLog(LogLevel.WARN, "[Watchdog] " + decision.summary(), null);
            }
            return;
        }
        
        // 2. Log to console
        doLog(level, message, exception);
        
        // 3. Forward to chat if enabled OR forced
        if ((channel.chatForward() || forceChat) && LogConfig.chatEnabled()) {
            LogChatBridge.forward(channel, topic, level, message);
        }
    }
    
    private static void doLog(LogLevel level, String message, Throwable exception) {
        switch (level) {
            case ERROR -> {
                if (exception != null) LOGGER.error(message, exception);
                else LOGGER.error(message);
            }
            case WARN -> {
                if (exception != null) LOGGER.warn(message, exception);
                else LOGGER.warn(message);
            }
            case INFO -> LOGGER.info(message);
            case DEBUG -> LOGGER.debug(message);
            case TRACE -> LOGGER.trace(message);
            default -> {}
        }
    }
    
    private LogOutput() {}
}
