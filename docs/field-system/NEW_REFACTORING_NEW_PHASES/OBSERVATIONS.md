# Observations & Potential Flaws Discovery Log

> **Purpose:** Track discoveries, potential issues, and their resolutions as we implement  
> **Status:** Living document - update as we go  
> **Created:** December 8, 2024

---

## How to Use This Document

1. **When you discover something** → Add a new row to the appropriate table
2. **When you resolve it** → Fill in the Response column
3. **If it needs a TODO** → Create one and reference it here
4. **If you have a question** → Add to [QUESTIONS.md](./QUESTIONS.md)

---

## 🔴 Critical Observations (Must Fix)

| # | Date | Category | Observation | Impact | Response | Status |
|---|------|----------|-------------|--------|----------|--------|
| C1 | 2024-12-08 | Logging | No direct `.alwaysChat()` on logging builder | Can't force specific errors to chat | **IMPLEMENTED:** Added `Context.alwaysChat()` and `FormattedContext.alwaysChat()` | ✅ Fixed |
| C2 | 2024-12-08 | Patterns | Pattern mismatch handling undefined - what if SECTOR pattern on QUAD shape? | Silent failure or crash? | **Decision:** Log error, render nothing, send chat message (see Q3) | ✅ Documented |
| C3 | 2024-12-08 | CageOptions | Cage mode only has sphere-specific options (lat/lon count) | Prism/Poly cage won't work | **Decision:** Shape-specific CageOptions fields | ✅ Documented |
| C4 | 2024-12-08 | Logging | **No FIELD channel!** Available: RENDER, REGISTRY, COMMANDS but no dedicated FIELD channel | Field logs mixed with other systems | **IMPLEMENTED:** Added `Logging.FIELD` channel | ✅ Fixed |

---

## 🟡 Medium Observations (Should Address)

| # | Date | Category | Observation | Impact | Response | Status |
|---|------|----------|-------------|--------|----------|--------|
| M1 | 2024-12-08 | Documentation | Inconsistent defaults: latSteps=16 vs 32 in different docs | Confusion | Fixed to 32 everywhere | ✅ Fixed |
| M2 | 2024-12-08 | Documentation | lonStart/lonEnd marked ❌ in 03 but ✅ in 04 - actually implemented | Misleading status | Verified in code, updated 03 | ✅ Fixed |
| M3 | 2024-12-08 | Naming | Waveform.TRIANGLE conflicts with TrianglePattern | Confusion | Renamed to TRIANGLE_WAVE | ✅ Fixed |
| M4 | 2024-12-08 | Missing | AlphaPulseConfig record not defined | Build will fail | Added to CLASS_DIAGRAM | ✅ Fixed |
| M5 | 2024-12-08 | Missing | DynamicTrianglePattern not in dynamic patterns list | Incomplete shuffle | Added to CLASS_DIAGRAM | ✅ Fixed |

---

## 🟢 Minor Observations (Nice to Have)

| # | Date | Category | Observation | Impact | Response | Status |
|---|------|----------|-------------|--------|----------|--------|
| m1 | 2024-12-08 | Docs | Different status symbols: ❌ vs 📋 for same meaning | Minor confusion | Acceptable - different docs | ⏳ Later |
| m2 | 2024-12-08 | Abbreviations | Primitive interface used Vis, Arr, App, Anim | Less readable | Expanded to full names | ✅ Fixed |

---

## 🔧 Technical Debt Discovered

| # | Date | Component | Issue | Priority | Status |
|---|------|-----------|-------|----------|--------|
| TD1 | 2024-12-08 | Logging | Add `.alwaysChat()` to Context builder | Medium | ✅ Done |
| TD2 | 2024-12-08 | Documentation | Document all CommandKnob utilities | Low | ✅ Done |
| TD3 | 2024-12-08 | Logging | Add `Logging.FIELD` channel | High | ✅ Done |
| TD4 | 2024-12-08 | Logging | Consider `startTimer()`/`stopTimer()` for perf | Low | ⏳ Future |
| TD5 | 2024-12-08 | Documentation | Document all Logging utilities | Low | ✅ Done |

---

## ✅ Available Utilities (Documented)

### Command Utilities (`net.cyberpunk042.command.util`)

| Utility | Purpose | Status |
|---------|---------|--------|
| `CommandKnob` | Fluent command builder with protection | ✅ Documented |
| `CommandKnobConfig` | Global limiter settings | ✅ Documented |
| `CommandKnobDefaults` | Auto-registered defaults | ✅ Documented |
| `CommandProtection` | Blacklist/untouchable system | ✅ Documented |
| `CommandFeedback` | Colored feedback messages | ✅ Documented |
| `EnumSuggester` | Tab completion for enums | ✅ Documented |
| `RegistrySuggester` | Tab completion from registries | ✅ Documented |
| `ReportBuilder` | Multi-line status output | ✅ Documented |
| `ListFormatter` | List output with tags | ✅ Documented |
| `CommandFormatters` | Value formatting utilities | ✅ Documented |
| `FieldCommandBuilder` | Dynamic command trees | ✅ Documented |

### Logging Utilities (`net.cyberpunk042.log`)

| Utility | Purpose | Status |
|---------|---------|--------|
| `Logging` | Channel definitions (18 channels now!) | ✅ Documented |
| `Channel` | Log channel with level control, chat forward | ✅ Documented |
| `Topic` | Subtopic within channel | ✅ Documented |
| `Context` | Fluent log message builder + `alwaysChat()` | ✅ Documented |
| `ContextBuilder` | Interface for context methods | ✅ Documented |
| `FormattedContext` | Rich formatted output builder + `alwaysChat()` | ✅ Documented |
| `FormattedPairs` | Key-value pair builder for Context | ✅ Documented |
| `FormattedTable` | Table builder for Context | ✅ Documented |
| `FormattedTree` | Tree structure builder for Context | ✅ Documented |
| `LogFormat` | Static formatters + standalone builders | ✅ Documented |
| `LogFormatter` | Interface for custom type formatting | ✅ Documented |
| `LogSection` | Interface for custom sections | ✅ Documented |
| `LogChatBridge` | Forward logs to in-game chat | ✅ Documented |
| `LogConfig` | Logging configuration (JSON) | ✅ Documented |
| `LogOutput` | Central output pipeline (now with forceChat) | ✅ Documented |
| `LogLevel` | Log levels (ERROR→TRACE) | ✅ Documented |
| `LogWatchdog` | Spam detection (50/s, 500/m) | ✅ Documented |
| `WatchdogDecision` | Watchdog result | ✅ Documented |
| `TableStyle` | Table styles (ASCII, UNICODE, etc.) | ✅ Documented |
| `ChatRecipients` | Who receives chat (ALL, OPS) | ✅ Documented |

See: `_design/SYSTEM_UTILITIES.md` for full documentation.

---

## 🆕 Implemented Additions

### 1. FIELD Channel ✅

```java
// Added to Logging.java:
public static final Channel FIELD = register(Channel.of("field", "Field", LogLevel.INFO));
```

**Usage:**
```java
Logging.FIELD.topic("spawn").player(player).id(fieldId).info("Spawning field");
Logging.FIELD.topic("update").kv("radius", radius).debug("Field updated");
```

### 2. alwaysChat() Method ✅

```java
// Added to Context.java and FormattedContext.java:
public Context alwaysChat() {
    this.forceChat = true;
    return this;
}
```

**Usage:**
```java
Logging.FIELD.topic("error")
    .alwaysChat()  // Forces chat regardless of channel setting
    .kv("expected", "QUAD")
    .kv("got", "SECTOR")
    .error("Pattern CellType mismatch - primitive will not render");
```

---

## 📋 Future Considerations

### Performance Timer (Low Priority)

```java
// Proposed: Performance timing helper
Logging.RENDER.topic("tessellate")
    .startTimer("sphere_tessellation")  // Starts timer
    .kv("vertices", count)
    .stopTimer()  // Auto-adds duration
    .debug("Tessellation complete");
```

**Why:** Field tessellation can be expensive. Quick timing helps identify bottlenecks.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Fixed | Issue resolved |
| ✅ Done | Task completed |
| 🔧 TODO | Needs implementation work |
| ⏳ Later | Deferred to later phase |
| ⏳ Future | Future consideration |
| ❓ Open | Still investigating |
| 🚫 Won't Fix | Intentionally not addressing |

---

*Link to questions: [QUESTIONS.md](./QUESTIONS.md)*  
*Last updated: December 8, 2024*
