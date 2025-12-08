# Logging System

A fluent, channel-based logging system with topic-level control,
formatted output, watchdog throttling, and in-game chat forwarding.

All output goes to `TheVirusBlock.LOGGER` - channels are filters and formatters on top.

---

## Quick Start

```java
import static net.cyberpunk042.log.Logging.*;

// Simple log
GROWTH.info("Block placed");

// With topic
PROFILER.topic("chunk").info("Corrupted {} blocks", count);

// With context
GROWTH.at(pos).kv("scale", s).info("Placed");

// Formatted output
SINGULARITY.formatted()
    .heading("Report")
    .table("Rings").headers("Ring", "Status").rows(...).done()
    .info();
```

---

## Architecture

```
Logging.CHANNEL → [.topic()] → [.context()] → .level("msg")
       │              │              │              │
    Channel         Topic         Context       Terminal
```

### Flow
1. **Logging** - Static entry point with channel constants
2. **Channel** - A log channel (GROWTH, PROFILER, etc.)
3. **Topic** - Optional sub-category within a channel
4. **Context** - Fluent builder for adding fields
5. **Terminal** - Log level method (info, warn, error, debug, trace)

### Output Pipeline
1. Check effective level (channel or topic override)
2. Watchdog spam detection
3. Format: `[Label:topic] fields message`
4. Output to `TheVirusBlock.LOGGER`
5. Forward to chat if enabled

---

## Channels

| Channel | Default | Description |
|---------|---------|-------------|
| `SINGULARITY` | INFO | Collapse orchestration |
| `COLLAPSE` | INFO | Collapse wave processing |
| `FUSE` | INFO | Fuse entity lifecycle |
| `CHUNKS` | WARN | Chunk operations |
| `GROWTH` | WARN | Growth block lifecycle |
| `RENDER` | WARN | Client-side rendering |
| `COLLISION` | OFF | Collision detection (spammy!) |
| `PROFILER` | OFF | CorruptionProfiler (spammy!) |
| `ORCHESTRATOR` | WARN | World orchestrator |
| `SCENARIO` | WARN | Scenario management |
| `PHASE` | WARN | Phase transitions |
| `SCHEDULER` | OFF | Task scheduling |
| `CONFIG` | INFO | Configuration loading |
| `REGISTRY` | WARN | Profile registry |
| `COMMANDS` | INFO | Command execution |
| `EFFECTS` | OFF | Visual effects |
| `INFECTION` | OFF | Infection spread |
| `CALLBACKS` | WARN | World state callbacks |

---

## Usage Patterns

### 1. Quick Log (No Context)
```java
Logging.GROWTH.info("Block placed at {}", pos);
Logging.CONFIG.error("Failed to load profile", exception);
```

### 2. With Topic
```java
Logging.PROFILER.topic("chunk").info("Corrupted {} blocks", count);
Logging.PROFILER.topic("boobytrap").at(pos).warn("Armed");
```

### 3. With Context (Fluent)
```java
Logging.GROWTH
    .at(pos)
    .kv("scale", scale)
    .entity(player)
    .info("Block placed");
```

### 4. With Exception (Chained)
```java
Logging.REGISTRY
    .at(path)
    .exception(ex)
    .error("Failed to load profile");
```

### 5. Lazy Evaluation (Expensive Messages)
```java
if (Logging.COLLISION.is(LogLevel.DEBUG)) {
    Logging.COLLISION.debug("Expensive: {}", computeExpensive());
}

// Or with Supplier
Logging.COLLISION.debug(() -> "Expensive: " + computeExpensive());
```

### 6. Pre-Cached Topic in Domain Class
```java
class CorruptionProfiler {
    private static final Topic LOG = Logging.PROFILER.topic("chunk");
    
    void process() {
        LOG.at(pos).count("blocks", n).info("Corrupted");
    }
}
```

### 7. Formatted Output
```java
Logging.SINGULARITY.topic("report").formatted()
    .heading("Collapse Report")
    .pairs()
        .add("World", world)
        .add("Radius", radius)
        .add("Duration", duration)
        .done()
    .table("Rings")
        .headers("Ring", "Chunks", "Status")
        .rows(rings, r -> new Object[] { r.index(), r.chunks(), r.status() })
        .done()
    .list("Players", players, Player::getName)
    .info();
```

### 8. Static Formatters
```java
Logging.PROFILER.info("Chunks:\n{}", 
    LogFormat.list(chunks, ChunkPos::toString));

Logging.SINGULARITY.info("Stats:\n{}",
    LogFormat.table()
        .headers("Metric", "Value")
        .row("Chunks", 42)
        .row("Blocks", 1000)
        .build());
```

### 9. Custom Injection
```java
// Custom section
Logging.PROFILER.formatted()
    .section(new MyCustomReportSection(data))
    .info();

// Raw string
Logging.PROFILER.formatted()
    .raw(myPreformattedString)
    .info();

// Lambda block
Logging.PROFILER.formatted()
    .block(() -> buildCustomOutput())
    .info();
```

---

## Configuration (logs.json)

Located at: `config/the-virus-block/logs.json`

```json
{
  "channels": {
    "profiler": {
      "level": "INFO",
      "chat": false,
      "topics": {
        "chunk": "DEBUG",
        "boobytrap": "OFF"
      }
    },
    "collision": {
      "level": "OFF",
      "chat": false
    },
    "growth": {
      "level": "WARN",
      "chat": true
    }
  },
  "chat": {
    "enabled": true,
    "recipients": "ops",
    "rateLimit": 10
  },
  "watchdog": {
    "enabled": true,
    "perSecond": 50,
    "perMinute": 500,
    "suppress": true
  }
}
```

### Level Values
- `OFF` - Channel disabled
- `ERROR` - Errors only
- `WARN` - Warnings and above
- `INFO` - Normal operation (default for most)
- `DEBUG` - Developer details
- `TRACE` - Everything (very verbose)

### Topic Inheritance
Topics without explicit configuration inherit from their parent channel.

---

## Commands

```
/virus logs                              → Dashboard overview
/virus logs list                         → All channels with levels
/virus logs set <channel> <level>        → Set channel level
/virus logs set <channel:topic> <level>  → Set topic level
/virus logs clear <channel:topic>        → Clear topic (inherit from channel)
/virus logs chat <channel> on|off        → Toggle chat forwarding
/virus logs chat list                    → Show forwarded channels
/virus logs watchdog on|off              → Toggle spam watchdog
/virus logs watchdog <perSec> <perMin>   → Set thresholds
/virus logs reload                       → Reload from config file
/virus logs reset                        → Reset all to defaults
```

---

## Extension Points

### Custom Type Formatters
Register global formatters for your types:
```java
LogFormat.register(ChunkPos.class, cp -> "[" + cp.x + "," + cp.z + "]");
LogFormat.register(BlockPos.class, bp -> "(" + bp.getX() + "," + bp.getY() + "," + bp.getZ() + ")");
```

### Custom Sections
Implement `LogSection` for complex output:
```java
public class CollapseReportSection implements LogSection {
    @Override
    public String render() {
        return buildComplexReport();
    }
}
```

### Custom Table Formatters
Per-column or per-row formatting:
```java
LogFormat.table()
    .headers("Ring", "Progress")
    .columnFormatter(1, v -> progressBar((int) v))
    .rows(...)
    .build();
```

---

## Design Decisions

### Why Channels Are Static
- Fast access without lookup
- Static import support: `import static Logging.*`
- Compile-time safety

### Why Topics Are Cached
- Same topic object returned for same name
- Thread-safe via ConcurrentHashMap
- Enables pre-caching in domain classes

### Why Context Is Single-Use
- Follows standard builder pattern
- Avoids state accumulation bugs
- Clear mental model

### Why Everything Uses TheVirusBlock.LOGGER
- Consistent log format
- Single output configuration
- Familiar SLF4J patterns

---

## Thread Safety

- `Channel.level` - volatile
- `Channel.topicLevels` - ConcurrentHashMap
- `Channel.topicCache` - ConcurrentHashMap
- `LogWatchdog.stats` - ConcurrentHashMap
- `Context` - single-threaded (one thread builds and logs)

---

## Console Output Examples

```
[the-virus-block] [Growth] Block placed at [100,64,-50]
[the-virus-block] [Profiler:chunk] pos=[0,0] blocks=42 Corrupted
[the-virus-block] [Singularity:collapse] radius=64 duration=5m Starting
[the-virus-block] [Collision:shape] entity=Zombie box=[...] Testing collision
```

---

## Class Reference

| Class | Purpose |
|-------|---------|
| `Logging` | Static entry point with channel constants |
| `Channel` | A log channel with level control |
| `Topic` | A topic within a channel |
| `Context` | Fluent message builder |
| `FormattedContext` | Rich formatted output builder |
| `LogFormat` | Static formatting utilities |
| `LogLevel` | Level enum (OFF → TRACE) |
| `LogConfig` | Configuration management |
| `LogOutput` | Output pipeline |
| `LogWatchdog` | Spam detection |
| `LogChatBridge` | In-game chat forwarding |
| `LogCommands` | Command integration |

