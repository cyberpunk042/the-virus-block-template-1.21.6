package net.cyberpunk042.log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static entry point for the logging system.
 * 
 * <p>Usage: {@code Logging.GROWTH.info("Message");}</p>
 * <p>With static import: {@code import static net.cyberpunk042.log.Logging.*;}</p>
 */
public final class Logging {
    
    private static final Map<String, Channel> CHANNELS = new LinkedHashMap<>();
    
    // ========== CHANNELS ==========
    
    public static final Channel SINGULARITY   = register(Channel.of("singularity",   "Singularity",   LogLevel.INFO));
    public static final Channel COLLAPSE      = register(Channel.of("collapse",      "Collapse",      LogLevel.INFO));
    public static final Channel FUSE          = register(Channel.of("fuse",          "Fuse",          LogLevel.INFO));
    public static final Channel CHUNKS        = register(Channel.of("chunks",        "Chunks",        LogLevel.WARN));
    public static final Channel GROWTH        = register(Channel.of("growth",        "Growth",        LogLevel.WARN));
    public static final Channel RENDER        = register(Channel.of("render",        "Render",        LogLevel.WARN));
    public static final Channel COLLISION     = register(Channel.of("collision",     "Collision",     LogLevel.OFF));
    public static final Channel PROFILER      = register(Channel.of("profiler",      "Profiler",      LogLevel.OFF));
    public static final Channel ORCHESTRATOR  = register(Channel.of("orchestrator",  "Orchestrator",  LogLevel.WARN));
    public static final Channel SCENARIO      = register(Channel.of("scenario",      "Scenario",      LogLevel.WARN));
    public static final Channel PHASE         = register(Channel.of("phase",         "Phase",         LogLevel.WARN));
    public static final Channel SCHEDULER     = register(Channel.of("scheduler",     "Scheduler",     LogLevel.OFF));
    public static final Channel CONFIG        = register(Channel.of("config",        "Config",        LogLevel.INFO));
    public static final Channel REGISTRY      = register(Channel.of("registry",      "Registry",      LogLevel.WARN));
    public static final Channel COMMANDS      = register(Channel.of("commands",      "Commands",      LogLevel.INFO));
    public static final Channel EFFECTS       = register(Channel.of("effects",       "Effects",       LogLevel.OFF));
    public static final Channel INFECTION     = register(Channel.of("infection",     "Infection",     LogLevel.OFF));
    public static final Channel CALLBACKS     = register(Channel.of("callbacks",     "Callbacks",     LogLevel.WARN));
    public static final Channel FIELD         = register(Channel.of("field",         "Field",         LogLevel.INFO));
    
    // ========== REGISTRY ==========
    
    private static Channel register(Channel channel) {
        CHANNELS.put(channel.id(), channel);
        return channel;
    }
    
    public static Channel channel(String id) {
        return CHANNELS.get(id);
    }
    
    public static Collection<Channel> channels() {
        return Collections.unmodifiableCollection(CHANNELS.values());
    }
    
    // ========== LIFECYCLE ==========
    
    public static void reload() {
        LogConfig.load();
    }
    
    public static void reset() {
        for (Channel ch : CHANNELS.values()) {
            ch.reset();
        }
        LogWatchdog.reset();
    }
    
    private Logging() {}
}
