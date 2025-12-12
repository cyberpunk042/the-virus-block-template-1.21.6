# LogScope Migration Tool - Strategy

## The Transformation

A loop with logging:
```java
for (Item item : items) {
    doSomething();
    Logging.CHAN.topic("x").info("name={} val={}", item.name(), item.val());
    doMore();
}
```

Becomes:
```java
try (LogScope scope = Logging.CHAN.topic("x").scope("process-items", LogLevel.INFO)) {
    for (Item item : items) {
        doSomething();
        scope.branch("item").kv("name", item.name()).kv("val", item.val());
        doMore();
    }
}
```

## Rules

1. **Unit of transformation = LOOP** (not individual log lines)
2. **Wrapper added AROUND the loop** with try-with-resources
3. **Each batchable log call REPLACED** with scope.branch()
4. **WARN/ERROR calls stay as-is** (immediate logging)
5. **Scope level = highest level among batchable calls** (trace < debug < info)

## Data Model

```
LoopWithLogs:
  - file_path
  - loop_start_line, loop_end_line
  - loop_type (for-each, for-i, while)
  - loop_var (the iteration variable)
  - loop_collection (what's being iterated)
  - indent (whitespace before loop)
  - original_lines[] (exact text)
  - log_calls[]:
      - line_num (relative to loop start)
      - level (trace/debug/info/warn/error)
      - channel, topic
      - format_string
      - arguments[]
      - is_batchable (trace/debug/info = yes)
```

## Phases

### Phase 1: Scan
- Walk all .java files
- Find loops using brace-counting
- For each loop, find log calls inside
- Build LoopWithLogs objects

### Phase 2: Transform (generate, don't apply yet)
- For each LoopWithLogs:
  - Generate scope wrapper line
  - For each batchable log call, generate replacement
  - Build complete transformed_lines[]

### Phase 3: Display
- Show file:line, loop info
- Show BEFORE (original_lines with markers on log calls)
- Show AFTER (transformed_lines with +/~ markers)
- Show which calls are BATCH vs KEEP

### Phase 4: Apply (on user confirmation)
- Backup file
- Replace original_lines with transformed_lines
- Add imports if missing

## KV Extraction

Format string: `"name={} status={} count={}"`
Arguments: `[item.name(), "ok", count]`
Result: `.kv("name", item.name()).kv("status", "ok").kv("count", count)`

If no named placeholders, infer from argument:
- `item.getName()` → `.kv("name", item.getName())`
- `pos` → `.kv("pos", pos)`
- `x + y` → `.kv("val", x + y)`

## Implementation Files

```
scripts/
  migrate_loop_logging.py   # Main tool
  lib/
    scanner.py              # Phase 1: Find loops with logs
    transformer.py          # Phase 2: Generate transformed code
    display.py              # Phase 3: Show diff
    applier.py              # Phase 4: Apply changes
```

## Iteration Plan

1. First: Get scanner working correctly (find ALL cases)
2. Then: Get transformer working (correct output)
3. Then: Get display working (clear diff)
4. Finally: Get applier working (safe file modification)

Each phase is a DELTA on top of the previous - no rewrites.

