# Questions Log

> **Purpose:** Track questions that arise during implementation and their answers  
> **Status:** Living document - update as questions arise and get answered  
> **Created:** December 8, 2024

---

## How to Use This Document

1. **When you have a question** → Add a new row
2. **When answered** → Fill in the Answer(s) column, update Status
3. **Reference observations** → Link to OBSERVATIONS.md if related

---

## 📋 Active Questions

| # | Date | Category | Question | Context | Answer(s) | Status |
|---|------|----------|----------|---------|-----------|--------|
| - | - | - | - | - | - | - |

---

## ✅ Answered Questions

| # | Date | Category | Question | Context | Answer(s) | Answered By |
|---|------|----------|----------|---------|-----------|-------------|
| Q1 | 2024-12-08 | Logging | How does visibility mask apply to EDGE cells? | EDGE = lines, not areas | EdgePattern handles visibility, not VisibilityMask | Architecture review |
| Q2 | 2024-12-08 | Primitives | What's Primitive.arrangement() return type? | String or ArrangementConfig? | ArrangementConfig - parser converts string | User decision |
| Q3 | 2024-12-08 | Patterns | Pattern mismatch handling? | Wrong CellType pattern on shape | B: Log error, render nothing, chat message (see OBS-C2) | User decision |
| Q4 | 2024-12-08 | Shapes | How does CAGE work for non-spheres? | Cage mode for prism/poly/cylinder | Shape-specific CageOptions (see OBS-C3) | User decision |
| Q5 | 2024-12-08 | Naming | Waveform.TRIANGLE conflicts with TrianglePattern? | Same name different concepts | Renamed to TRIANGLE_WAVE | User decision |

---

## Question Categories

| Category | Description |
|----------|-------------|
| Architecture | Overall system design |
| Primitives | Primitive types, hierarchy |
| Shapes | Shape parameters, tessellation |
| Patterns | Pattern system, arrangements |
| Rendering | Render pipeline, shaders |
| Logging | Logging system, channels |
| Commands | Command system, knobs |
| JSON | Profile format, parsing |

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ❓ Open | Waiting for answer |
| 🔄 Discussing | Under discussion |
| ✅ Answered | Has definitive answer |
| 🚫 Deferred | Postponed to later phase |

---

*Link to related observations: [OBSERVATIONS.md](./OBSERVATIONS.md)*

