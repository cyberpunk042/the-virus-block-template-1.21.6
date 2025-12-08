# Field System Class Diagram

> **Last Updated:** December 6, 2024  
> **Version:** 1.0.0

---

## Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SHARED MODULE                                  │
│  (Used by both client and server)                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │   Color     │    │   Shape     │    │  Registry   │                     │
│  │   System    │    │   System    │    │   System    │                     │
│  └─────────────┘    └─────────────┘    └─────────────┘                     │
│         │                  │                  │                             │
│         ▼                  ▼                  ▼                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Field Definitions                            │   │
│  │  FieldDefinition, Primitive, EffectConfig, FieldType               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                    │                               │
                    ▼                               ▼
┌───────────────────────────────────┐ ┌───────────────────────────────────────┐
│          CLIENT MODULE            │ │           SERVER MODULE               │
├───────────────────────────────────┤ ├───────────────────────────────────────┤
│                                   │ │                                       │
│  ClientFieldManager               │ │  ServerFieldManager                   │
│  FieldRenderer                    │ │  EffectProcessor                      │
│  PrimitiveRenderers               │ │  ActiveEffect                         │
│  VertexEmitter                    │ │                                       │
│                                   │ │                                       │
└───────────────────────────────────┘ └───────────────────────────────────────┘
```

---

## Color System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              COLOR SYSTEM                                   │
│                        Package: visual.color                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────┐                                               │
│  │       ColorMath         │  (static utility)                             │
│  ├─────────────────────────┤                                               │
│  │ + lighten(argb, amount) │                                               │
│  │ + darken(argb, amount)  │                                               │
│  │ + saturate(argb, amt)   │                                               │
│  │ + blend(a, b, factor)   │                                               │
│  │ + withAlpha(argb, a)    │                                               │
│  │ + toHSL(argb)           │                                               │
│  │ + fromHSL(h, s, l, a)   │                                               │
│  └─────────────────────────┘                                               │
│                                                                             │
│  ┌─────────────────────────┐       ┌─────────────────────────────────────┐ │
│  │      ColorTheme         │       │       ColorThemeRegistry            │ │
│  ├─────────────────────────┤       ├─────────────────────────────────────┤ │
│  │ - id: Identifier        │◄──────│ - themes: Map<Identifier, Theme>    │ │
│  │ - base: Integer         │       ├─────────────────────────────────────┤ │
│  │ - autoDerive: boolean   │       │ + get(id): ColorTheme               │ │
│  │ - roles: Map<String,Int>│       │ + register(theme)                   │ │
│  ├─────────────────────────┤       │ + derive(base): ColorTheme          │ │
│  │ + resolve(role): int    │       │ + load(path)                        │ │
│  │ + getPrimary(): int     │       │ + reload()                          │ │
│  │ + getSecondary(): int   │       └─────────────────────────────────────┘ │
│  │ + getGlow(): int        │                                               │
│  │ + getBeam(): int        │       ┌─────────────────────────────────────┐ │
│  │ + getWire(): int        │       │        ColorResolver                │ │
│  └─────────────────────────┘       ├─────────────────────────────────────┤ │
│                                    │ + resolve(str): int                 │ │
│         Uses existing:             │   - "@primary" → theme lookup       │ │
│         ColorConfig                │   - "$slotName" → ColorConfig       │ │
│                                    │   - "cyan" → basic color            │ │
│                                    │   - "#FF00AA" → hex parse           │ │
│                                    └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Shape System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SHAPE SYSTEM                                   │
│                         Package: visual.shape                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                        ┌─────────────────────┐                             │
│                        │   «interface»       │                             │
│                        │      Shape          │                             │
│                        ├─────────────────────┤                             │
│                        │ + getType(): String │                             │
│                        │ + getBounds(): Box  │                             │
│                        └─────────────────────┘                             │
│                                  △                                         │
│                                  │                                         │
│          ┌───────────────────────┼───────────────────────┐                 │
│          │                       │                       │                 │
│  ┌───────┴───────┐      ┌───────┴───────┐      ┌───────┴───────┐          │
│  │  SphereShape  │      │   RingShape   │      │  PrismShape   │          │
│  ├───────────────┤      ├───────────────┤      ├───────────────┤          │
│  │ - radius      │      │ - y           │      │ - sides       │          │
│  │ - latSteps    │      │ - radius      │      │ - height      │          │
│  │ - lonSteps    │      │ - thickness   │      │ - radius      │          │
│  └───────────────┘      └───────────────┘      └───────────────┘          │
│                                                                             │
│  ┌───────────────┐      ┌───────────────┐      ┌───────────────┐          │
│  │PolyhedronShape│      │  DiscShape    │      │  BeamShape    │          │
│  ├───────────────┤      ├───────────────┤      ├───────────────┤          │
│  │ - type: enum  │      │ - y           │      │ - radius      │          │
│  │   CUBE        │      │ - radius      │      │ - height      │          │
│  │   OCTAHEDRON  │      └───────────────┘      └───────────────┘          │
│  │   ICOSAHEDRON │                                                         │
│  │ - size        │                                                         │
│  └───────────────┘                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Primitive System (Hierarchical)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PRIMITIVE SYSTEM                                 │
│                       Package: field.primitive                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                        ┌─────────────────────────┐                         │
│                        │     «interface»         │                         │
│                        │       Primitive         │                         │
│                        ├─────────────────────────┤                         │
│                        │ + getShape(): Shape     │                         │
│                        │ + getTransform()        │                         │
│                        │ + getAppearance()       │                         │
│                        │ + getAnimation()        │                         │
│                        └─────────────────────────┘                         │
│                                    △                                       │
│                                    │                                       │
│        ┌───────────────────────────┼───────────────────────────┐           │
│        │                           │                           │           │
│ ┌──────┴──────┐            ┌───────┴───────┐          ┌───────┴───────┐   │
│ │«abstract»   │            │  «abstract»   │          │  «abstract»   │   │
│ │SolidPrimitive│           │ BandPrimitive │          │StructuralPrim │   │
│ ├─────────────┤            ├───────────────┤          ├───────────────┤   │
│ │ - fill      │            │ - count       │          │ - wireThick   │   │
│ │ - glow      │            │ - thickness   │          │               │   │
│ └─────────────┘            │ - gap         │          └───────────────┘   │
│        △                   └───────────────┘                   △          │
│        │                          △                            │          │
│   ┌────┴────┐               ┌─────┴─────┐              ┌───────┴───────┐  │
│   │         │               │           │              │               │  │
│ ┌─┴───────┐ ┌┴────────┐  ┌──┴─────┐ ┌───┴────┐   ┌─────┴────┐ ┌───────┴─┐│
│ │ Sphere  │ │  Prism  │  │ Ring   │ │Stripes │   │   Cage   │ │  Beam   ││
│ │Primitive│ │Primitive│  │Primitive│ │Primitive│  │Primitive │ │Primitive││
│ └─────────┘ └─────────┘  └────────┘ └────────┘   └──────────┘ └─────────┘│
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                       Common Components                              │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │  │
│  │  │  Transform  │  │ Appearance  │  │  Animation  │  │   Alpha     │ │  │
│  │  ├─────────────┤  ├─────────────┤  ├─────────────┤  ├─────────────┤ │  │
│  │  │ - offset    │  │ - color     │  │ - spin      │  │ - base      │ │  │
│  │  │ - rotation  │  │ - alpha     │  │ - pulse     │  │ - pulse     │ │  │
│  │  │ - scale     │  │ - fill      │  │ - phase     │  │ - min/max   │ │  │
│  │  └─────────────┘  │ - glow      │  └─────────────┘  └─────────────┘ │  │
│  │                   │ - wireThick │                                    │  │
│  │                   └─────────────┘                                    │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Field Definition System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FIELD DEFINITION SYSTEM                             │
│                        Package: field.definition                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         FieldDefinition                               │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - id: Identifier                                                      │ │
│  │ - type: FieldType                                                     │ │
│  │ - theme: String                         (nullable)                    │ │
│  │ - primitives: List<Primitive>                                         │ │
│  │ - baseRadius: float                                                   │ │
│  │ - modifiers: Modifiers                                                │ │
│  │ - effects: List<EffectConfig>                                         │ │
│  │ - beam: BeamConfig                      (nullable)                    │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + getPrimitive(index): Primitive                                      │ │
│  │ + addPrimitive(Primitive)                                             │ │
│  │ + removePrimitive(index)                                              │ │
│  │ + setTheme(String)                                                    │ │
│  │ + setRadius(float)                                                    │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                         │                                                   │
│            ┌────────────┼────────────┐                                     │
│            ▼            ▼            ▼                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                          │
│  │  FieldType  │ │  Modifiers  │ │ EffectConfig│                          │
│  │   «enum»    │ ├─────────────┤ ├─────────────┤                          │
│  ├─────────────┤ │ - spin      │ │ - type      │                          │
│  │ SHIELD      │ │ - tilt      │ │ - strength  │                          │
│  │ PERSONAL    │ │ - swirl     │ │ - radius    │                          │
│  │ SINGULARITY │ │ - pulse     │ │ - params    │                          │
│  │ GROWTH      │ └─────────────┘ └─────────────┘                          │
│  │ FORCE       │                                                           │
│  │ AURA        │        ┌─────────────┐                                   │
│  └─────────────┘        │ BeamConfig  │                                   │
│                         ├─────────────┤                                   │
│                         │ - color     │                                   │
│                         │ - radius    │                                   │
│                         │ - height    │                                   │
│                         └─────────────┘                                   │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         FieldBuilder                                  │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + define(id): FieldBuilder                                            │ │
│  │ + type(FieldType): FieldBuilder                                       │ │
│  │ + theme(String): FieldBuilder                                         │ │
│  │ + add(Primitive): FieldBuilder                                        │ │
│  │ + radius(float): FieldBuilder                                         │ │
│  │ + spin(float): FieldBuilder                                           │ │
│  │ + push(float): FieldBuilder                                           │ │
│  │ + pull(float): FieldBuilder                                           │ │
│  │ + beam(BeamConfig): FieldBuilder                                      │ │
│  │ + build(): FieldDefinition                                            │ │
│  │ + register(): FieldDefinition                                         │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Field Instance System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FIELD INSTANCE SYSTEM                              │
│                         Package: field.instance                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         FieldInstance                                 │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - definition: FieldDefinition                                         │ │
│  │ - position: Vec3d                                                     │ │
│  │ - animationTime: float                                                │ │
│  │ - activeEffects: List<ActiveEffect>                                   │ │
│  │ - alive: boolean                                                      │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + tick()                                                              │ │
│  │ + getPosition(): Vec3d                                                │ │
│  │ + getDefinition(): FieldDefinition                                    │ │
│  │ + isAlive(): boolean                                                  │ │
│  │ + despawn()                                                           │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    △                                       │
│                                    │                                       │
│                    ┌───────────────┴───────────────┐                       │
│                    │                               │                       │
│  ┌─────────────────┴─────────────┐ ┌──────────────┴──────────────────────┐│
│  │      PersonalFieldInstance    │ │       AnchoredFieldInstance        ││
│  ├───────────────────────────────┤ ├─────────────────────────────────────┤│
│  │ - owner: PlayerEntity         │ │ - blockPos: BlockPos               ││
│  │ - followMode: FollowMode      │ │ - blockEntity: BlockEntity         ││
│  │ - predictionConfig            │ └─────────────────────────────────────┘│
│  ├───────────────────────────────┤                                        │
│  │ + getOwner(): PlayerEntity    │  ┌─────────────┐                      │
│  │ + updatePosition()            │  │ FollowMode  │                      │
│  │ + setPrediction(config)       │  │  «enum»     │                      │
│  └───────────────────────────────┘  ├─────────────┤                      │
│                                     │ FIXED       │                      │
│  ┌───────────────────────────────┐  │ SMOOTH      │                      │
│  │        ActiveEffect           │  │ PREDICTIVE  │                      │
│  ├───────────────────────────────┤  └─────────────┘                      │
│  │ - config: EffectConfig        │                                        │
│  │ - cooldown: int               │                                        │
│  ├───────────────────────────────┤                                        │
│  │ + tick(instance)              │                                        │
│  │ + apply(entity)               │                                        │
│  │ + isOnCooldown(): boolean     │                                        │
│  └───────────────────────────────┘                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Registry System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            REGISTRY SYSTEM                                  │
│                         Package: field.registry                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                   «abstract» ProfileRegistry<T>                       │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ # profiles: Map<Identifier, T>                                        │ │
│  │ # directory: Path                                                     │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + get(id): T                                                          │ │
│  │ + getOrDefault(id, fallback): T                                       │ │
│  │ + register(T)                                                         │ │
│  │ + unregister(id)                                                      │ │
│  │ + all(): Collection<T>                                                │ │
│  │ + ids(): Set<Identifier>                                              │ │
│  │ + load()                                                              │ │
│  │ + reload()                                                            │ │
│  │ + save(id)                                                            │ │
│  │ # parse(JsonObject): T        «abstract»                              │ │
│  │ # serialize(T): JsonObject    «abstract»                              │ │
│  │ # getDefaultPath(): Path      «abstract»                              │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    △                                       │
│                                    │                                       │
│          ┌─────────────────────────┼─────────────────────────┐             │
│          │                         │                         │             │
│  ┌───────┴─────────────┐  ┌────────┴────────────┐  ┌────────┴───────────┐ │
│  │    FieldRegistry    │  │   GrowthRegistry    │  │ ColorThemeRegistry │ │
│  │  extends Profile    │  │  extends Profile    │  │  extends Profile   │ │
│  │  Registry<Field     │  │  Registry<Growth    │  │  Registry<Color    │ │
│  │  Definition>        │  │  BlockDefinition>   │  │  Theme>            │ │
│  ├─────────────────────┤  ├─────────────────────┤  ├────────────────────┤ │
│  │ + getByType(type)   │  │ + glowProfile(id)   │  │ + derive(base)     │ │
│  │ + getPresets(type)  │  │ + particleProfile() │  │                    │ │
│  │                     │  │ + forceProfile()    │  │                    │ │
│  └─────────────────────┘  └─────────────────────┘  └────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Manager System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            MANAGER SYSTEM                                   │
│                         Package: field.manager                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                     «abstract» FieldManager                           │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ # instances: Map<UUID, FieldInstance>                                 │ │
│  │ # registry: FieldRegistry                                             │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + spawn(definition, position): FieldInstance                          │ │
│  │ + despawn(uuid)                                                       │ │
│  │ + get(uuid): FieldInstance                                            │ │
│  │ + getAll(): Collection<FieldInstance>                                 │ │
│  │ + tick()                                                              │ │
│  │ + clear()                                                             │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    △                                       │
│                                    │                                       │
│                    ┌───────────────┴───────────────┐                       │
│                    │                               │                       │
│  ┌─────────────────┴─────────────┐ ┌──────────────┴──────────────────────┐│
│  │      ClientFieldManager       │ │       ServerFieldManager            ││
│  ├───────────────────────────────┤ ├─────────────────────────────────────┤│
│  │ - renderer: FieldRenderer     │ │ - effectProcessor: EffectProcessor ││
│  │ - personalShield              │ │                                     ││
│  ├───────────────────────────────┤ ├─────────────────────────────────────┤│
│  │ + render(context)             │ │ + processEffects()                  ││
│  │ + setPersonalShield(instance) │ │ + applyForce(entity)               ││
│  │ + getPersonalShield()         │ │ + checkShieldBlock(pos)            ││
│  └───────────────────────────────┘ └─────────────────────────────────────┘│
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                       EffectProcessor                                 │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + process(instance, world)                                            │ │
│  │ + applyPush(entity, strength, center)                                 │ │
│  │ + applyPull(entity, strength, center)                                 │ │
│  │ + applyDamage(entity, amount)                                         │ │
│  │ + checkShield(pos, infectionSource): boolean                          │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Render System (Client Only)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            RENDER SYSTEM                                    │
│                  Package: client.field.render (CLIENT)                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         FieldRenderer                                 │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - primitiveRenderers: Map<Class, PrimitiveRenderer>                   │ │
│  │ - colorResolver: ColorResolver                                        │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + render(instance, context)                                           │ │
│  │ + render(definition, position, context)                               │ │
│  │ + registerRenderer(class, renderer)                                   │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    │                                       │
│                                    ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                  «interface» PrimitiveRenderer                        │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + render(primitive, matrices, consumer, light, time, colorResolver)   │ │
│  │ + supports(primitiveClass): boolean                                   │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    △                                       │
│                                    │                                       │
│     ┌──────────────┬───────────────┼───────────────┬──────────────┐        │
│     │              │               │               │              │        │
│ ┌───┴────┐   ┌─────┴────┐   ┌──────┴─────┐  ┌─────┴────┐  ┌──────┴────┐  │
│ │ Sphere │   │   Ring   │   │  Stripes   │  │   Cage   │  │   Beam    │  │
│ │Renderer│   │ Renderer │   │  Renderer  │  │ Renderer │  │ Renderer  │  │
│ └────────┘   └──────────┘   └────────────┘  └──────────┘  └───────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
│                                                                             │
│                         SHARED RENDER UTILITIES                             │
│                      Package: visual.render (SHARED)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │
│  │  VertexEmitter  │  │   Tessellator   │  │   MeshBuilder   │            │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤            │
│  │ + emitQuad()    │  │ + sphere()      │  │ + vertex()      │            │
│  │ + emitVertex()  │  │ + ring()        │  │ + quad()        │            │
│  │ + emitMesh()    │  │ + prism()       │  │ + build(): Mesh │            │
│  │ + emitWireframe │  │ + polyhedron()  │  └─────────────────┘            │
│  └─────────────────┘  └─────────────────┘                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Command System (Using CommandKnob)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         COMMAND KNOB SYSTEM                                 │
│                     Package: command.util                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         CommandKnob                                   │ │
│  │                   «static factory + builders»                         │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + toggle(path, displayName): ToggleBuilder                            │ │
│  │ + value(path, displayName): ValueBuilder                              │ │
│  │ + floatValue(path, displayName): FloatBuilder                         │ │
│  │ + enumValue(path, displayName, class): EnumBuilder<E>                 │ │
│  │ + action(path, displayName): ActionBuilder                            │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│       │                                                                     │
│       ├── ToggleBuilder                                                     │
│       │   ├── defaultValue(boolean)          → registers in Defaults       │
│       │   ├── handler((source, enabled) → bool)                            │
│       │   ├── scenarioRequired(predicate)                                  │
│       │   ├── build() → LiteralArgumentBuilder                             │
│       │   └── attach(parent)                 → adds "enable/disable" subs  │
│       │                                                                     │
│       ├── ValueBuilder                                                      │
│       │   ├── range(min, max)                                              │
│       │   ├── unit(string)                   → for display (e.g. "blocks") │
│       │   ├── defaultValue(int)              → registers in Defaults       │
│       │   ├── handler((source, value) → bool)                              │
│       │   ├── scenarioRequired(predicate)                                  │
│       │   ├── build() → LiteralArgumentBuilder                             │
│       │   └── attach(parent)                                               │
│       │                                                                     │
│       ├── FloatBuilder                                                      │
│       │   ├── range(min, max)                                              │
│       │   ├── unit(string)                                                 │
│       │   ├── defaultValue(float)            → registers in Defaults       │
│       │   ├── handler((source, value) → bool)                              │
│       │   ├── scenarioRequired(predicate)                                  │
│       │   └── attach(parent)                                               │
│       │                                                                     │
│       ├── EnumBuilder<E>                                                    │
│       │   ├── idMapper(E → String)           → for suggestions             │
│       │   ├── parser(String → E)             → for parsing                 │
│       │   ├── defaultValue(E)                → registers in Defaults       │
│       │   ├── handler((source, E) → bool)                                  │
│       │   ├── scenarioRequired(predicate)                                  │
│       │   └── attach(parent)                                               │
│       │                                                                     │
│       └── ActionBuilder                                                     │
│           ├── handler(source → bool)                                       │
│           ├── scenarioRequired(predicate)                                  │
│           ├── successMessage(string)                                       │
│           └── attach(parent)                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       COMMAND PROTECTION                                    │
│                     Package: command.util                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                      CommandProtection                                │ │
│  │              Config: command_protection.json                          │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - untouchable: Set<String>         (completely blocked)               │ │
│  │ - blacklisted: Map<String, String> (path → reason, warns but runs)    │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + load(configRoot)                                                    │ │
│  │ + reload()                                                            │ │
│  │ + isUntouchable(path): boolean                                        │ │
│  │ + isBlacklisted(path): boolean                                        │ │
│  │ + getBlacklistReason(path): Optional<String>                          │ │
│  │ + checkAndWarn(source, path): boolean  ← called by every knob!       │ │
│  │ + auditDeviations()                    ← server start log            │ │
│  │ + auditDeviations(currentValueProvider)                               │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  command_protection.json:                                                   │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │ {                                                                     │ │
│  │   "untouchable": [                                                    │ │
│  │     "field.shield.max_radius"       // completely locked              │ │
│  │   ],                                                                  │ │
│  │   "blacklisted": [                                                    │ │
│  │     { "path": "erosion.native_fill",                                  │ │
│  │       "reason": "May cause lag on large collapses" }                  │ │
│  │   ]                                                                   │ │
│  │ }                                                                     │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       COMMAND FEEDBACK                                      │
│                     Package: command.util                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                       CommandFeedback                                 │ │
│  │                   Consistent message formatting                       │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + success(source, message)           → green                          │ │
│  │ + successBroadcast(source, message)  → green, broadcasts to ops       │ │
│  │ + error(source, message)             → red                            │ │
│  │ + info(source, message)              → gray                           │ │
│  │ + warn(source, message)              → yellow                         │ │
│  │ + highlight(source, message)         → aqua                           │ │
│  │                                                                       │ │
│  │ // Common patterns                                                    │ │
│  │ + toggle(source, name, enabled)      → "Name enabled/disabled"        │ │
│  │ + valueSet(source, name, value)      → "Name set to value"            │ │
│  │ + valueGet(source, name, value)      → "Current Name: value"          │ │
│  │ + notFound(source, type, id)         → "Type not found: id"           │ │
│  │ + invalidValue(source, name, value)  → "Invalid Name: value"          │ │
│  │ + requires(source, requirement)      → "Requires requirement"         │ │
│  │                                                                       │ │
│  │ // Formatting helpers                                                 │ │
│  │ + header(text): MutableText          → "═══ text ═══" gold bold       │ │
│  │ + subheader(text): MutableText       → "── text ──" yellow            │ │
│  │ + bullet(text): MutableText          → "  • text" gray                │ │
│  │ + keyValue(key, value, color)        → "  key: value"                 │ │
│  │ + labeled(label, value): MutableText → "label: value"                 │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       COMMAND KNOB DEFAULTS                                 │
│                     Package: command.util                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                     CommandKnobDefaults                               │ │
│  │               Auto-populated by knob builders                         │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - defaults: Map<String, Object>                                       │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + register(path, value)        ← called by builders                  │ │
│  │ + get(path): Object                                                   │ │
│  │ + format(path): String         ← for protection warnings             │ │
│  │ + paths(): Collection<String>                                         │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## CommandKnob Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    KNOB EXECUTION CHAIN                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User runs: /singularity collapse enable                                    │
│       │                                                                     │
│       ▼                                                                     │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │ CommandKnob.ToggleBuilder.execute(source, enabled=true)            │    │
│  │                                                                    │    │
│  │   1. PROTECTION CHECK                                              │    │
│  │      ┌──────────────────────────────────────────────────────────┐  │    │
│  │      │ CommandProtection.checkAndWarn(source, "singularity.     │  │    │
│  │      │                                         collapse")       │  │    │
│  │      │                                                          │  │    │
│  │      │ if untouchable("singularity.collapse"):                  │  │    │
│  │      │   → "🔒 This setting is locked. Default: true" (RED)     │  │    │
│  │      │   → return 0 (BLOCKED)                                   │  │    │
│  │      │                                                          │  │    │
│  │      │ if blacklisted("singularity.collapse"):                  │  │    │
│  │      │   → "⚠ reason (default: true)" (YELLOW)                  │  │    │
│  │      │   → continue (WARNED)                                    │  │    │
│  │      └──────────────────────────────────────────────────────────┘  │    │
│  │                                                                    │    │
│  │   2. SCENARIO CHECK (optional)                                     │    │
│  │      ┌──────────────────────────────────────────────────────────┐  │    │
│  │      │ if scenarioCheck != null:                                │  │    │
│  │      │   if !scenarioCheck.test(source, null):                  │  │    │
│  │      │     → return 0 (e.g., "Requires active scenario")        │  │    │
│  │      └──────────────────────────────────────────────────────────┘  │    │
│  │                                                                    │    │
│  │   3. EXECUTE HANDLER                                               │    │
│  │      ┌──────────────────────────────────────────────────────────┐  │    │
│  │      │ success = handler.apply(source, enabled)                 │  │    │
│  │      │   → calls: getFacade(source).setCollapseEnabled(true)    │  │    │
│  │      │                                                          │  │    │
│  │      │ if !success:                                             │  │    │
│  │      │   → CommandFeedback.error("Failed to update settings")   │  │    │
│  │      │   → return 0                                             │  │    │
│  │      └──────────────────────────────────────────────────────────┘  │    │
│  │                                                                    │    │
│  │   4. SUCCESS FEEDBACK                                              │    │
│  │      ┌──────────────────────────────────────────────────────────┐  │    │
│  │      │ CommandFeedback.toggle(source, "Singularity collapse",   │  │    │
│  │      │                        enabled=true)                     │  │    │
│  │      │   → "Singularity collapse enabled" (GREEN)               │  │    │
│  │      └──────────────────────────────────────────────────────────┘  │    │
│  │                                                                    │    │
│  │   → return 1                                                       │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Field Commands Using CommandKnob

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    FIELD COMMAND REGISTRATION                               │
│                  Package: client.command (CLIENT)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  FieldCommand.register(dispatcher):                                         │
│                                                                             │
│  dispatcher.register(literal("field")                                       │
│      │                                                                      │
│      │  // Global actions                                                   │
│      ├── CommandKnob.action("field.list", "List fields")                    │
│      │       .handler(source -> { listFields(source); return true; })       │
│      │       .attach(parent)                                                │
│      │                                                                      │
│      ├── CommandKnob.action("field.reload", "Reload fields")                │
│      │       .handler(source -> { reloadAll(source); return true; })        │
│      │       .successMessage("Field definitions reloaded")                  │
│      │       .attach(parent)                                                │
│      │                                                                      │
│      │  // Shield settings                                                  │
│      ├── literal("shield")                                                  │
│      │       │                                                              │
│      │       ├── CommandKnob.floatValue("field.shield.radius", "Radius")    │
│      │       │       .range(1.0f, 64.0f)                                    │
│      │       │       .unit("blocks")                                        │
│      │       │       .defaultValue(8.0f)                                    │
│      │       │       .handler((src, r) -> setRadius(src, r))                │
│      │       │       .attach(parent)                                        │
│      │       │                                                              │
│      │       ├── CommandKnob.value("field.shield.lat_steps", "Lat steps")   │
│      │       │       .range(4, 64)                                          │
│      │       │       .defaultValue(16)                                      │
│      │       │       .handler((src, v) -> setLat(src, v))                   │
│      │       │       .attach(parent)                                        │
│      │       │                                                              │
│      │       ├── CommandKnob.toggle("field.shield.spin", "Auto-spin")       │
│      │       │       .defaultValue(true)                                    │
│      │       │       .handler((src, e) -> setSpin(src, e))                  │
│      │       │       .attach(parent)                                        │
│      │       │                                                              │
│      │       └── CommandKnob.enumValue("field.shield.style",                │
│      │               "Mesh style", MeshStyle.class)                         │
│      │               .defaultValue(MeshStyle.WIREFRAME)                     │
│      │               .handler((src, s) -> setStyle(src, s))                 │
│      │               .attach(parent)                                        │
│      │                                                                      │
│      │  // Personal field settings                                          │
│      └── literal("personal")                                                │
│              │                                                              │
│              ├── CommandKnob.toggle("field.personal.enabled", "Personal")   │
│              │       .defaultValue(false)                                   │
│              │       .handler((src, e) -> togglePersonal(src, e))           │
│              │       .attach(parent)                                        │
│              │                                                              │
│              └── ... etc                                                    │
│  )                                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Command Registration Chain

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      COMMAND REGISTRATION                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ClientModInitializer.onInitializeClient()                                  │
│       │                                                                     │
│       └── ClientCommandRegistrationCallback.EVENT.register(...)            │
│               │                                                             │
│               └── FieldCommand.register(dispatcher)                         │
│                       │                                                     │
│                       ├── dispatcher.register(literal("field")              │
│                       │       │                                             │
│                       │       ├── .then(literal("list")...)                 │
│                       │       ├── .then(literal("reload")...)               │
│                       │       ├── .then(literal("types")...)                │
│                       │       │                                             │
│                       │       ├── .then(ThemeSubcommand.build())            │
│                       │       │       └── literal("theme")                  │
│                       │       │           ├── .then(literal("list")...)     │
│                       │       │           ├── .then(literal("show")...)     │
│                       │       │           ├── .then(literal("apply")...)    │
│                       │       │           └── .then(literal("create")...)   │
│                       │       │                                             │
│                       │       └── for each FieldTypeProvider:               │
│                       │               .then(provider.buildSubcommands())    │
│                       │                                                     │
│                       └── // Each provider builds its own subtree           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Command Tree Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      FULL COMMAND TREE                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  /field                                                                     │
│  ├── list                           → FieldCommand.handleList()             │
│  ├── reload                         → FieldCommand.handleReload()           │
│  ├── types                          → FieldCommand.handleTypes()            │
│  │                                                                          │
│  ├── theme                          → ThemeSubcommand                       │
│  │   ├── list                       → .handleList()                         │
│  │   ├── show <name>                → .handleShow(name)                     │
│  │   ├── apply <name>               → .handleApply(name)                    │
│  │   ├── create <name> <color>      → .handleCreate(name, color)            │
│  │   └── save                       → .handleSave()                         │
│  │                                                                          │
│  ├── shield                         → ShieldProvider                        │
│  │   ├── list                       → .handleList()                         │
│  │   ├── info <id>                  → .handleInfo(id)                       │
│  │   ├── preset <name>              → .handlePreset(name)                   │
│  │   ├── cycle                      → .handleCycle()                        │
│  │   ├── profiles                                                           │
│  │   │   ├── list                   → .handleProfileList()                  │
│  │   │   ├── save <name>            → .handleProfileSave(name)              │
│  │   │   └── load <name>            → .handleProfileLoad(name)              │
│  │   ├── config                                                             │
│  │   │   ├── show                   → .handleConfigShow()                   │
│  │   │   └── set <key> <value>      → .handleConfigSet(key, value)          │
│  │   ├── layer                                                              │
│  │   │   ├── add <type>             → .handleLayerAdd(type)                 │
│  │   │   ├── remove <id>            → .handleLayerRemove(id)                │
│  │   │   └── set <id> <key> <val>   → .handleLayerSet(id, key, value)       │
│  │   ├── set                                                                │
│  │   │   ├── lat <value>            → .handleSetLat(value)                  │
│  │   │   ├── lon <value>            → .handleSetLon(value)                  │
│  │   │   ├── swirl <value>          → .handleSetSwirl(value)                │
│  │   │   ├── scale <value>          → .handleSetScale(value)                │
│  │   │   ├── spin <value>           → .handleSetSpin(value)                 │
│  │   │   └── tilt <value>           → .handleSetTilt(value)                 │
│  │   ├── spawn [radius] [seconds]   → .handleSpawn(radius, seconds)         │
│  │   └── target <world|personal>    → .handleTarget(target)                 │
│  │                                                                          │
│  ├── personal                       → PersonalProvider                      │
│  │   ├── on [radius]                → .handleOn(radius)                     │
│  │   ├── off                        → .handleOff()                          │
│  │   ├── sync                       → .handleSync()                         │
│  │   ├── visual <on|off>            → .handleVisual(state)                  │
│  │   ├── follow <mode>              → .handleFollow(mode)                   │
│  │   ├── preset <name>              → .handlePreset(name)                   │
│  │   ├── profile                                                            │
│  │   │   ├── list                   → .handleProfileList()                  │
│  │   │   ├── save <name>            → .handleProfileSave(name)              │
│  │   │   └── load <name>            → .handleProfileLoad(name)              │
│  │   ├── color                                                              │
│  │   │   ├── simple <name>          → .handleColorSimple(name)              │
│  │   │   ├── primary <value>        → .handleColorPrimary(value)            │
│  │   │   ├── secondary <value>      → .handleColorSecondary(value)          │
│  │   │   └── beam <value>           → .handleColorBeam(value)               │
│  │   └── prediction                                                         │
│  │       ├── show                   → .handlePredictionShow()               │
│  │       ├── enable <bool>          → .handlePredictionEnable(bool)         │
│  │       ├── lead <ticks>           → .handlePredictionLead(ticks)          │
│  │       ├── max <blocks>           → .handlePredictionMax(blocks)          │
│  │       ├── look <offset>          → .handlePredictionLook(offset)         │
│  │       └── vertical <boost>       → .handlePredictionVertical(boost)      │
│  │                                                                          │
│  ├── singularity                    → SingularityProvider                   │
│  │   ├── show                       → .handleShow()                         │
│  │   ├── set <key> <value>          → .handleSet(key, value)                │
│  │   ├── save                       → .handleSave()                         │
│  │   └── reload                     → .handleReload()                       │
│  │                                                                          │
│  ├── growth                         → GrowthProvider                        │
│  │   ├── list                       → .handleList()                         │
│  │   ├── info <id>                  → .handleInfo(id)                       │
│  │   ├── set <id> <key> <value>     → .handleSet(id, key, value)            │
│  │   └── reload                     → .handleReload()                       │
│  │                                                                          │
│  ├── force                          → ForceProvider                         │
│  │   ├── list                       → .handleList()                         │
│  │   ├── info <id>                  → .handleInfo(id)                       │
│  │   ├── set <id> <key> <value>     → .handleSet(id, key, value)            │
│  │   └── reload                     → .handleReload()                       │
│  │                                                                          │
│  └── aura                           → AuraProvider                          │
│      ├── list                       → .handleList()                         │
│      ├── info <id>                  → .handleInfo(id)                       │
│      ├── set <id> <key> <value>     → .handleSet(id, key, value)            │
│      └── reload                     → .handleReload()                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Logging System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          LOGGING SYSTEM                                     │
│                        Package: log                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                          Logging                                      │ │
│  │                    Static channel registry                            │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + SINGULARITY : Channel                                               │ │
│  │ + COLLAPSE    : Channel                                               │ │
│  │ + FUSE        : Channel                                               │ │
│  │ + CHUNKS      : Channel                                               │ │
│  │ + GROWTH      : Channel                                               │ │
│  │ + RENDER      : Channel       ← use for field rendering               │ │
│  │ + COLLISION   : Channel                                               │ │
│  │ + PROFILER    : Channel                                               │ │
│  │ + ORCHESTRATOR: Channel                                               │ │
│  │ + SCENARIO    : Channel                                               │ │
│  │ + PHASE       : Channel                                               │ │
│  │ + SCHEDULER   : Channel                                               │ │
│  │ + CONFIG      : Channel                                               │ │
│  │ + REGISTRY    : Channel                                               │ │
│  │ + COMMANDS    : Channel       ← use for command logging               │ │
│  │ + EFFECTS     : Channel                                               │ │
│  │ + INFECTION   : Channel                                               │ │
│  │ + CALLBACKS   : Channel                                               │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + channel(id): Channel                                                │ │
│  │ + channels(): Collection<Channel>                                     │ │
│  │ + reload()                                                            │ │
│  │ + reset()                                                             │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    │                                       │
│                                    ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                          Channel                                      │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - id: String                                                          │ │
│  │ - displayName: String                                                 │ │
│  │ - level: LogLevel                                                     │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + info(format, args...)                                               │ │
│  │ + warn(format, args...)                                               │ │
│  │ + error(format, args...)                                              │ │
│  │ + debug(format, args...)                                              │ │
│  │ + topic(name): TopicLogger    ← for sub-categorization                │ │
│  │ + reset()                                                             │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    │                                       │
│                                    ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                        TopicLogger                                    │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ - channel: Channel                                                    │ │
│  │ - topic: String                                                       │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ + info(format, args...)                                               │ │
│  │ + warn(format, args...)                                               │ │
│  │ + error(format, args...)                                              │ │
│  │ + at(BlockPos): PositionalLogger  ← adds position context             │ │
│  │ + kv(...): KeyValueLogger         ← structured logging                │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         LogLevel «enum»                               │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │ OFF                                                                   │ │
│  │ ERROR                                                                 │ │
│  │ WARN                                                                  │ │
│  │ INFO                                                                  │ │
│  │ DEBUG                                                                 │ │
│  │ TRACE                                                                 │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Logging Usage Examples

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       LOGGING PATTERNS                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  // Simple channel logging                                                  │
│  Logging.COMMANDS.info("Loaded {} field definitions", count);               │
│  Logging.RENDER.warn("Field {} has no primitives", fieldId);                │
│  Logging.CONFIG.error("Failed to parse {}: {}", file, e.getMessage());      │
│                                                                             │
│  // Topic-based logging (for sub-categories)                                │
│  Logging.SINGULARITY.topic("chunk").info(                                   │
│      "tick={} colsProcessed={} blocksCleared={}",                           │
│      tick, columns, blocks);                                                │
│                                                                             │
│  Logging.INFECTION.topic("boobytrap")                                       │
│      .at(pos)                                                               │
│      .kv("type", type)                                                      │
│      .kv("affected", players)                                               │
│      .info("triggered");                                                    │
│                                                                             │
│  // Field system logging                                                    │
│  Logging.RENDER.topic("field").info(                                        │
│      "Rendering {} primitives for field {}",                                │
│      primitiveCount, definition.getId());                                   │
│                                                                             │
│  Logging.REGISTRY.topic("field").info(                                      │
│      "Registered {} field definitions from {}",                             │
│      count, source);                                                        │
│                                                                             │
│  Logging.COMMANDS.topic("field").warn(                                      │
│      "Unknown field preset: {}", presetName);                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Provider Implementation Pattern

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PROVIDER IMPLEMENTATION                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  class ShieldProvider implements FieldTypeProvider {                        │
│                                                                             │
│      private final ClientFieldManager manager;                              │
│      private final FieldRegistry registry;                                  │
│                                                                             │
│      @Override                                                              │
│      public FieldType getType() {                                           │
│          return FieldType.SHIELD;                                           │
│      }                                                                      │
│                                                                             │
│      @Override                                                              │
│      public LiteralArgumentBuilder<FabricClientCommandSource>               │
│              buildSubcommands() {                                           │
│                                                                             │
│          return literal("shield")                                           │
│              .then(literal("list")                                          │
│                  .executes(ctx -> handleList(ctx.getSource())))             │
│              .then(literal("info")                                          │
│                  .then(argument("id", StringArgumentType.word())            │
│                      .suggests(this::suggestIds)                            │
│                      .executes(ctx -> handleInfo(                           │
│                          ctx.getSource(),                                   │
│                          StringArgumentType.getString(ctx, "id")))))        │
│              .then(literal("preset")                                        │
│                  .then(argument("name", StringArgumentType.word())          │
│                      .suggests(this::suggestPresets)                        │
│                      .executes(ctx -> handlePreset(                         │
│                          ctx.getSource(),                                   │
│                          StringArgumentType.getString(ctx, "name")))))      │
│              // ... more subcommands                                        │
│              ;                                                              │
│      }                                                                      │
│                                                                             │
│      @Override                                                              │
│      public Collection<String> suggestIds() {                               │
│          return registry.getByType(FieldType.SHIELD)                        │
│              .stream()                                                      │
│              .map(def -> def.getId().toString())                            │
│              .toList();                                                     │
│      }                                                                      │
│                                                                             │
│      private int handleList(FabricClientCommandSource source) {             │
│          List<FieldDefinition> shields = registry.getByType(SHIELD);        │
│          source.sendFeedback(Text.literal("Shields: " +                     │
│              shields.stream().map(d -> d.getId().getPath())                 │
│                  .collect(Collectors.joining(", "))));                      │
│          return 1;                                                          │
│      }                                                                      │
│                                                                             │
│      private int handlePreset(FabricClientCommandSource source,             │
│                               String name) {                                │
│          FieldDefinition def = registry.get(                                │
│              Identifier.of("the-virus-block", name));                       │
│          if (def == null) {                                                 │
│              source.sendError(Text.literal("Unknown preset: " + name));     │
│              return 0;                                                      │
│          }                                                                  │
│          manager.setActiveShield(def);                                      │
│          source.sendFeedback(Text.literal("Preset → " + name));             │
│          return 1;                                                          │
│      }                                                                      │
│  }                                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Class Relationships Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KEY RELATIONSHIPS                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  INHERITANCE (extends)                                                      │
│  ─────────────────────                                                      │
│  ProfileRegistry<T>                                                         │
│    ├── FieldRegistry                                                        │
│    ├── GrowthRegistry                                                       │
│    └── ColorThemeRegistry                                                   │
│                                                                             │
│  FieldManager                                                               │
│    ├── ClientFieldManager                                                   │
│    └── ServerFieldManager                                                   │
│                                                                             │
│  FieldInstance                                                              │
│    ├── PersonalFieldInstance                                                │
│    └── AnchoredFieldInstance                                                │
│                                                                             │
│  Primitive                                                                  │
│    ├── SolidPrimitive                                                       │
│    │     ├── SpherePrimitive                                                │
│    │     └── PrismPrimitive                                                 │
│    ├── BandPrimitive                                                        │
│    │     ├── RingPrimitive                                                  │
│    │     └── StripesPrimitive                                               │
│    └── StructuralPrimitive                                                  │
│          ├── CagePrimitive                                                  │
│          └── BeamPrimitive                                                  │
│                                                                             │
│  COMPOSITION (has-a)                                                        │
│  ───────────────────                                                        │
│  FieldDefinition ──────────► List<Primitive>                                │
│  FieldDefinition ──────────► List<EffectConfig>                             │
│  FieldDefinition ──────────► Modifiers                                      │
│  FieldDefinition ──────────► BeamConfig                                     │
│                                                                             │
│  FieldInstance ────────────► FieldDefinition                                │
│  FieldInstance ────────────► List<ActiveEffect>                             │
│                                                                             │
│  Primitive ────────────────► Shape                                          │
│  Primitive ────────────────► Transform                                      │
│  Primitive ────────────────► Appearance                                     │
│  Primitive ────────────────► Animation                                      │
│                                                                             │
│  USES (depends on)                                                          │
│  ─────────────────                                                          │
│  FieldRenderer ────────────► PrimitiveRenderer                              │
│  FieldRenderer ────────────► ColorResolver                                  │
│  PrimitiveRenderer ────────► VertexEmitter                                  │
│  PrimitiveRenderer ────────► Tessellator                                    │
│  ColorResolver ────────────► ColorThemeRegistry                             │
│  ColorResolver ────────────► ColorConfig (existing)                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Mesh & Triangle Building Chain

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MESH GENERATION PIPELINE                               │
│                      Package: visual.mesh                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Shape                    Tessellator                   Mesh              │
│  (geometry)    ────────►   (generates)    ────────►    (data)              │
│                                                                             │
│  ┌───────────┐            ┌─────────────────────────────────────────────┐  │
│  │SphereShape│            │              Tessellator                    │  │
│  │ - radius  │            ├─────────────────────────────────────────────┤  │
│  │ - latSteps│──────────► │ + tessellate(shape, detail): Mesh           │  │
│  │ - lonSteps│            │ + sphere(radius, lat, lon): Mesh            │  │
│  └───────────┘            │ + ring(y, radius, thickness, segments): Mesh│  │
│                           │ + prism(sides, height, radius): Mesh        │  │
│                           │ + polyhedron(type, size): Mesh              │  │
│                           └─────────────────────────────────────────────┘  │
│                                          │                                  │
│                                          ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                             Mesh                                     │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │ - vertices: List<Vertex>                                             │  │
│  │ - indices: int[]              (triangle indices into vertices)       │  │
│  │ - primitiveType: PrimitiveType  (TRIANGLES, QUADS, LINES)           │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │ + getVertexCount(): int                                              │  │
│  │ + getTriangleCount(): int                                            │  │
│  │ + getVertex(index): Vertex                                           │  │
│  │ + forEachTriangle(consumer)                                          │  │
│  │ + forEachQuad(consumer)                                              │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                          │                                  │
│                           ┌──────────────┴──────────────┐                  │
│                           ▼                              ▼                  │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │           Vertex                │  │        PrimitiveType            │  │
│  ├─────────────────────────────────┤  │          «enum»                 │  │
│  │ - x, y, z: float    (position)  │  ├─────────────────────────────────┤  │
│  │ - nx, ny, nz: float (normal)    │  │ TRIANGLES                       │  │
│  │ - u, v: float       (texture)   │  │ QUADS                           │  │
│  └─────────────────────────────────┘  │ LINES                           │  │
│                                       └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                      VERTEX EMISSION PIPELINE                               │
│                      Package: visual.render                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Mesh              VertexEmitter           VertexConsumer                 │
│  (data)   ────────►  (emits)     ────────►  (Minecraft)                    │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        VertexEmitter                                 │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  + emitMesh(consumer, mesh, matrix, argb, light)                     │  │
│  │      └── for each triangle in mesh:                                  │  │
│  │              emitTriangle(...)                                       │  │
│  │                                                                      │  │
│  │  + emitTriangle(consumer, matrix, v1, v2, v3, argb, light)          │  │
│  │      └── emitVertex(v1)                                              │  │
│  │          emitVertex(v2)                                              │  │
│  │          emitVertex(v3)                                              │  │
│  │                                                                      │  │
│  │  + emitQuad(consumer, matrix, v1, v2, v3, v4, argb, light)          │  │
│  │      └── emitVertex(v1)                                              │  │
│  │          emitVertex(v2)                                              │  │
│  │          emitVertex(v3)                                              │  │
│  │          emitVertex(v4)                                              │  │
│  │                                                                      │  │
│  │  + emitVertex(consumer, matrix, vertex, argb, light)                 │  │
│  │      └── consumer.vertex(matrix, x, y, z)                            │  │
│  │              .color(r, g, b, a)                                      │  │
│  │              .texture(u, v)                                          │  │
│  │              .light(light)                                           │  │
│  │              .normal(nx, ny, nz)                                     │  │
│  │                                                                      │  │
│  │  + emitWireframe(consumer, mesh, matrix, argb, thickness, light)     │  │
│  │      └── for each edge in mesh:                                      │  │
│  │              emitLine(...)                                           │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                      FULL RENDER CHAIN                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  FieldRenderer.render(instance, context)                                    │
│       │                                                                     │
│       ├── resolve theme → colors                                            │
│       │                                                                     │
│       └── for each Primitive in definition:                                 │
│               │                                                             │
│               ├── get PrimitiveRenderer for type                            │
│               │                                                             │
│               └── primitiveRenderer.render(primitive, ...)                  │
│                       │                                                     │
│                       ├── shape = primitive.getShape()                      │
│                       │                                                     │
│                       ├── mesh = Tessellator.tessellate(shape, detail)     │
│                       │       │                                             │
│                       │       └── generates vertices + indices              │
│                       │           based on shape type:                      │
│                       │                                                     │
│                       │           SphereTessellator:                        │
│                       │             for lat in 0..latSteps:                 │
│                       │               for lon in 0..lonSteps:               │
│                       │                 add vertex at spherical coords      │
│                       │                 add triangle indices                │
│                       │                                                     │
│                       │           RingTessellator:                          │
│                       │             for segment in 0..segments:             │
│                       │               add inner vertex                      │
│                       │               add outer vertex                      │
│                       │               add quad indices                      │
│                       │                                                     │
│                       ├── apply transform (offset, rotation, scale)         │
│                       │                                                     │
│                       ├── apply animation (spin, pulse) based on time       │
│                       │                                                     │
│                       ├── resolve color from theme or direct                │
│                       │                                                     │
│                       └── VertexEmitter.emitMesh(consumer, mesh, ...)       │
│                               │                                             │
│                               └── pushes vertices to Minecraft's            │
│                                   VertexConsumer → GPU                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Sphere Tessellation Detail

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SPHERE TESSELLATION (LAT/LON)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  SphereTessellator.tessellate(radius, latSteps, lonSteps)                   │
│                                                                             │
│  1. GENERATE VERTICES                                                       │
│     ──────────────────                                                      │
│     for lat = 0 to latSteps:                                                │
│         θ = π * lat / latSteps           (0 at top, π at bottom)           │
│         for lon = 0 to lonSteps:                                            │
│             φ = 2π * lon / lonSteps      (around equator)                  │
│                                                                             │
│             x = radius * sin(θ) * cos(φ)                                   │
│             y = radius * cos(θ)                                            │
│             z = radius * sin(θ) * sin(φ)                                   │
│                                                                             │
│             normal = normalize(x, y, z)                                     │
│             u = lon / lonSteps                                              │
│             v = lat / latSteps                                              │
│                                                                             │
│             vertices.add(Vertex(x, y, z, normal, u, v))                     │
│                                                                             │
│  2. GENERATE TRIANGLES (indices)                                            │
│     ────────────────────────────                                            │
│     for lat = 0 to latSteps - 1:                                            │
│         for lon = 0 to lonSteps - 1:                                        │
│                                                                             │
│             topLeft     = lat * (lonSteps+1) + lon                          │
│             topRight    = topLeft + 1                                       │
│             bottomLeft  = (lat+1) * (lonSteps+1) + lon                      │
│             bottomRight = bottomLeft + 1                                    │
│                                                                             │
│             // Two triangles per grid cell (quad)                           │
│             indices.add(topLeft, bottomLeft, topRight)                      │
│             indices.add(topRight, bottomLeft, bottomRight)                  │
│                                                                             │
│                     topLeft ────── topRight                                 │
│                        │  \          │                                      │
│                        │    \   T2   │                                      │
│                        │ T1   \      │                                      │
│                        │        \    │                                      │
│                     bottomLeft ── bottomRight                               │
│                                                                             │
│  3. RETURN MESH                                                             │
│     ───────────                                                             │
│     return Mesh(vertices, indices, TRIANGLES)                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Ring Tessellation Detail

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      RING TESSELLATION                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  RingTessellator.tessellate(y, innerRadius, outerRadius, segments)          │
│                                                                             │
│  1. GENERATE VERTICES                                                       │
│     ──────────────────                                                      │
│     for i = 0 to segments:                                                  │
│         angle = 2π * i / segments                                           │
│                                                                             │
│         // Inner vertex                                                     │
│         innerX = innerRadius * cos(angle)                                   │
│         innerZ = innerRadius * sin(angle)                                   │
│         vertices.add(Vertex(innerX, y, innerZ, UP, ...))                    │
│                                                                             │
│         // Outer vertex                                                     │
│         outerX = outerRadius * cos(angle)                                   │
│         outerZ = outerRadius * sin(angle)                                   │
│         vertices.add(Vertex(outerX, y, outerZ, UP, ...))                    │
│                                                                             │
│  2. GENERATE QUADS (indices)                                                │
│     ────────────────────────                                                │
│     for i = 0 to segments - 1:                                              │
│         inner1 = i * 2                                                      │
│         outer1 = i * 2 + 1                                                  │
│         inner2 = (i + 1) * 2                                                │
│         outer2 = (i + 1) * 2 + 1                                            │
│                                                                             │
│         // Quad per segment                                                 │
│         indices.add(inner1, inner2, outer2, outer1)                         │
│                                                                             │
│              outer1 ──────── outer2                                         │
│                │               │                                            │
│                │     QUAD      │                                            │
│                │               │                                            │
│              inner1 ──────── inner2                                         │
│                                                                             │
│  3. RETURN MESH                                                             │
│     ───────────                                                             │
│     return Mesh(vertices, indices, QUADS)                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
net.cyberpunk042
├── visual/                          # SHARED UTILITIES
│   ├── color/
│   │   ├── ColorMath.java
│   │   ├── ColorTheme.java
│   │   ├── ColorThemeRegistry.java
│   │   └── ColorResolver.java
│   ├── shape/
│   │   ├── Shape.java
│   │   ├── SphereShape.java
│   │   ├── RingShape.java
│   │   ├── PrismShape.java
│   │   ├── PolyhedronShape.java
│   │   ├── DiscShape.java
│   │   └── BeamShape.java
│   ├── mesh/
│   │   ├── Mesh.java
│   │   ├── MeshBuilder.java
│   │   └── Tessellator.java
│   ├── transform/
│   │   ├── Transform.java
│   │   └── AnimatedTransform.java
│   ├── appearance/
│   │   ├── Appearance.java
│   │   ├── Alpha.java
│   │   └── FillMode.java
│   ├── animation/
│   │   ├── Animation.java
│   │   ├── Spin.java
│   │   └── Pulse.java
│   └── render/
│       ├── VertexEmitter.java
│       └── MeshRenderer.java
│
├── field/                           # SHARED FIELD SYSTEM
│   ├── primitive/
│   │   ├── Primitive.java
│   │   ├── SolidPrimitive.java
│   │   ├── BandPrimitive.java
│   │   ├── StructuralPrimitive.java
│   │   ├── SpherePrimitive.java
│   │   ├── RingPrimitive.java
│   │   ├── StripesPrimitive.java
│   │   ├── CagePrimitive.java
│   │   ├── PrismPrimitive.java
│   │   └── BeamPrimitive.java
│   ├── definition/
│   │   ├── FieldDefinition.java
│   │   ├── FieldType.java
│   │   ├── FieldBuilder.java
│   │   ├── Modifiers.java
│   │   ├── EffectConfig.java
│   │   └── BeamConfig.java
│   ├── instance/
│   │   ├── FieldInstance.java
│   │   ├── PersonalFieldInstance.java
│   │   ├── AnchoredFieldInstance.java
│   │   ├── ActiveEffect.java
│   │   └── FollowMode.java
│   ├── registry/
│   │   ├── ProfileRegistry.java
│   │   ├── FieldRegistry.java
│   │   └── FieldLoader.java
│   └── manager/
│       ├── FieldManager.java
│       └── EffectProcessor.java
│
├── client/field/                    # CLIENT ONLY
│   ├── manager/
│   │   └── ClientFieldManager.java
│   ├── render/
│   │   ├── FieldRenderer.java
│   │   ├── PrimitiveRenderer.java
│   │   ├── SphereRenderer.java
│   │   ├── RingRenderer.java
│   │   ├── StripesRenderer.java
│   │   ├── CageRenderer.java
│   │   └── BeamRenderer.java
│   └── command/
│       ├── FieldCommand.java
│       ├── FieldTypeProvider.java
│       ├── ShieldProvider.java
│       ├── PersonalProvider.java
│       └── ThemeSubcommand.java
│
└── server/field/                    # SERVER ONLY
    └── manager/
        └── ServerFieldManager.java
```

---

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2024-12-06 | 1.0.0 | Initial class diagram |

