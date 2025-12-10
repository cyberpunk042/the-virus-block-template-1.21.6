#!/usr/bin/env python3
"""
Update all callers of FieldEditState to use the new @StateField pattern.

This script handles:
1. Getter transformations (state.getXxx() -> state.record().field())
2. Setter transformations (state.setXxx(v) -> state.set("record", ...toBuilder()...))
3. Adding required imports
"""

import os
import re
import sys

CLIENT_DIR = "src/client/java/net/cyberpunk042/client"

# ═══════════════════════════════════════════════════════════════════════════════
# GETTER MAPPINGS: old method -> new expression
# ═══════════════════════════════════════════════════════════════════════════════

GETTER_MAPPINGS = {
    # ─────────────────────────────────────────────────────────────────────────
    # SPHERE
    # ─────────────────────────────────────────────────────────────────────────
    "getSphereLatSteps": "sphere().latSteps()",
    "getSphereLonSteps": "sphere().lonSteps()",
    "getSphereLatStart": "sphere().latStart()",
    "getSphereLatEnd": "sphere().latEnd()",
    "getSphereAlgorithm": "sphere().algorithm().name()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # RING
    # ─────────────────────────────────────────────────────────────────────────
    "getRingInnerRadius": "ring().innerRadius()",
    "getRingOuterRadius": "ring().outerRadius()",
    "getRingSegments": "ring().segments()",
    "getRingHeight": "ring().height()",
    "getRingY": "ring().y()",
    "getRingArcStart": "ring().arcStart()",
    "getRingArcEnd": "ring().arcEnd()",
    "getRingTwist": "ring().twist()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # DISC
    # ─────────────────────────────────────────────────────────────────────────
    "getDiscRadius": "disc().radius()",
    "getDiscSegments": "disc().segments()",
    "getDiscY": "disc().y()",
    "getDiscInnerRadius": "disc().innerRadius()",
    "getDiscArcStart": "disc().arcStart()",
    "getDiscArcEnd": "disc().arcEnd()",
    "getDiscRings": "disc().rings()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # PRISM
    # ─────────────────────────────────────────────────────────────────────────
    "getPrismSides": "prism().sides()",
    "getPrismRadius": "prism().radius()",
    "getPrismHeight": "prism().height()",
    "getPrismTopRadius": "prism().topRadius()",
    "getPrismTwist": "prism().twist()",
    "getPrismHeightSegments": "prism().heightSegments()",
    "isPrismCapTop": "prism().capTop()",
    "isPrismCapBottom": "prism().capBottom()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # CYLINDER
    # ─────────────────────────────────────────────────────────────────────────
    "getCylinderRadius": "cylinder().radius()",
    "getCylinderHeight": "cylinder().height()",
    "getCylinderSegments": "cylinder().segments()",
    "getCylinderTopRadius": "cylinder().topRadius()",
    "getCylinderArc": "cylinder().arc()",
    "getCylinderHeightSegments": "cylinder().heightSegments()",
    "isCylinderCapTop": "cylinder().capTop()",
    "isCylinderCapBottom": "cylinder().capBottom()",
    "isCylinderOpenEnded": "cylinder().isTube()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # POLYHEDRON
    # ─────────────────────────────────────────────────────────────────────────
    "getPolyType": "polyhedron().polyType().name()",
    "getPolyRadius": "polyhedron().radius()",
    "getPolySubdivisions": "polyhedron().subdivisions()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # TRANSFORM
    # ─────────────────────────────────────────────────────────────────────────
    "getAnchor": "transform().anchor().name()",
    "getScale": "transform().scale()",
    "getFacing": "transform().facing().name()",
    "getBillboard": "transform().billboard().name()",
    "getRotationX": "state.transform().rotation() != null ? state.transform().rotation().x : 0f",
    "getRotationY": "state.transform().rotation() != null ? state.transform().rotation().y : 0f",
    "getRotationZ": "state.transform().rotation() != null ? state.transform().rotation().z : 0f",
    "getOffsetX": "state.transform().offset() != null ? state.transform().offset().x : 0f",
    "getOffsetY": "state.transform().offset() != null ? state.transform().offset().y : 0f",
    "getOffsetZ": "state.transform().offset() != null ? state.transform().offset().z : 0f",
    
    # ─────────────────────────────────────────────────────────────────────────
    # ORBIT
    # ─────────────────────────────────────────────────────────────────────────
    "isOrbitEnabled": "orbit().enabled()",
    "getOrbitRadius": "orbit().radius()",
    "getOrbitSpeed": "orbit().speed()",
    "getOrbitAxis": "orbit().axis().name()",
    "getOrbitPhase": "orbit().phase()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # SPIN (SpinConfig fields: axis, speed, oscillate, range, customAxis)
    # ─────────────────────────────────────────────────────────────────────────
    "isSpinEnabled": "spin().isActive()",
    "getSpinAxis": "spin().axis().name()",
    "getSpinSpeed": "spin().speed()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # PULSE (PulseConfig fields: scale, speed, waveform, min, max)
    # ─────────────────────────────────────────────────────────────────────────
    "isPulseEnabled": "pulse().isActive()",
    "getPulseFrequency": "pulse().speed()",  # frequency -> speed
    "getPulseAmplitude": "pulse().scale()",  # amplitude -> scale
    # Note: getPulseMode removed - PulseMode is controlled by waveform field
    
    # ─────────────────────────────────────────────────────────────────────────
    # ALPHA FADE (AlphaPulseConfig fields: scale, speed, waveform, min, max)
    # ─────────────────────────────────────────────────────────────────────────
    "isAlphaFadeEnabled": "alphaPulse().isActive()",
    "getAlphaMin": "alphaPulse().min()",
    "getAlphaMax": "alphaPulse().max()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # VISIBILITY MASK (fields: mask, count, thickness, offset, invert, feather, animate, animSpeed)
    # ─────────────────────────────────────────────────────────────────────────
    "getMaskType": "mask().mask().name()",  # mask field is MaskType enum
    "getMaskCount": "mask().count()",
    "getMaskThickness": "mask().thickness()",
    "getMaskOffset": "mask().offset()",
    "getMaskFeather": "mask().feather()",
    "isMaskInverted": "mask().invert()",
    "isMaskAnimated": "mask().animate()",
    "getMaskAnimateSpeed": "mask().animSpeed()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # FILL
    # ─────────────────────────────────────────────────────────────────────────
    "getWireThickness": "fill().wireThickness()",
    "isDoubleSided": "fill().doubleSided()",
    "isDepthTest": "fill().depthTest()",
    "isDepthWrite": "fill().depthWrite()",
    
    # ─────────────────────────────────────────────────────────────────────────
    # APPEARANCE (direct fields on FieldEditState - no transformation needed)
    # These methods already exist on FieldEditState, so no mapping needed
    # ─────────────────────────────────────────────────────────────────────────
    
    # ─────────────────────────────────────────────────────────────────────────
    # MODIFIERS (WobbleConfig, WaveConfig, ColorCycleConfig)
    # ─────────────────────────────────────────────────────────────────────────
    "getModifierBobbing": "state.wobble().amplitude() != null ? state.wobble().amplitude().x : 0f",
    "getModifierBreathing": "pulse().scale()",
    "isColorCycleEnabled": "colorCycle().isActive()",
    "getColorCycleSpeed": "colorCycle().speed()",
    "isColorCycleBlend": "colorCycle().blend()",
    "isWobbleEnabled": "wobble().isActive()",
    "getWobbleAmplitude": "state.wobble().amplitude() != null ? state.wobble().amplitude().x : 0f",
    "getWobbleSpeed": "wobble().speed()",
    "isWaveEnabled": "wave().isActive()",
    "getWaveAmplitude": "wave().amplitude()",
    "getWaveFrequency": "wave().frequency()",
    "getWaveDirection": "wave().direction().name()",  # Axis enum
    
    # ─────────────────────────────────────────────────────────────────────────
    # ACTION PANEL (direct methods on FieldEditState - no transformation needed)
    # These methods already exist on FieldEditState, so no mapping needed
    # ─────────────────────────────────────────────────────────────────────────
}

# Getters that produce ternary expressions - already include state. in mapping
# These will be wrapped in parentheses but not have state. added
TERNARY_GETTERS = {
    "getRotationX", "getRotationY", "getRotationZ",
    "getOffsetX", "getOffsetY", "getOffsetZ",
    "getModifierBobbing", "getWobbleAmplitude",
}

# ═══════════════════════════════════════════════════════════════════════════════
# SETTER DEFINITIONS
# ═══════════════════════════════════════════════════════════════════════════════

# Format: (pattern_prefix, new_template with ARG placeholder, import_needed)
SETTER_DEFS = [
    # SPHERE
    (r'state\.setSphereLatSteps', 'state.set("sphere", state.sphere().toBuilder().latSteps((int)ARG).build())', None),
    (r'state\.setSphereLonSteps', 'state.set("sphere", state.sphere().toBuilder().lonSteps((int)ARG).build())', None),
    (r'state\.setSphereLatStart', 'state.set("sphere", state.sphere().toBuilder().latStart(ARG).build())', None),
    (r'state\.setSphereLatEnd', 'state.set("sphere", state.sphere().toBuilder().latEnd(ARG).build())', None),
    (r'state\.setSphereAlgorithm', 'state.set("sphere", state.sphere().toBuilder().algorithm(SphereAlgorithm.valueOf(ARG)).build())', 'SphereAlgorithm'),
    
    # RING
    (r'state\.setRingInnerRadius', 'state.set("ring", state.ring().toBuilder().innerRadius(ARG).build())', None),
    (r'state\.setRingOuterRadius', 'state.set("ring", state.ring().toBuilder().outerRadius(ARG).build())', None),
    (r'state\.setRingSegments', 'state.set("ring", state.ring().toBuilder().segments((int)ARG).build())', None),
    (r'state\.setRingHeight', 'state.set("ring", state.ring().toBuilder().height(ARG).build())', None),
    (r'state\.setRingY', 'state.set("ring", state.ring().toBuilder().y(ARG).build())', None),
    (r'state\.setRingArcStart', 'state.set("ring", state.ring().toBuilder().arcStart(ARG).build())', None),
    (r'state\.setRingArcEnd', 'state.set("ring", state.ring().toBuilder().arcEnd(ARG).build())', None),
    (r'state\.setRingTwist', 'state.set("ring", state.ring().toBuilder().twist(ARG).build())', None),
    
    # DISC
    (r'state\.setDiscRadius', 'state.set("disc", state.disc().toBuilder().radius(ARG).build())', None),
    (r'state\.setDiscSegments', 'state.set("disc", state.disc().toBuilder().segments((int)ARG).build())', None),
    (r'state\.setDiscY', 'state.set("disc", state.disc().toBuilder().y(ARG).build())', None),
    (r'state\.setDiscInnerRadius', 'state.set("disc", state.disc().toBuilder().innerRadius(ARG).build())', None),
    (r'state\.setDiscArcStart', 'state.set("disc", state.disc().toBuilder().arcStart(ARG).build())', None),
    (r'state\.setDiscArcEnd', 'state.set("disc", state.disc().toBuilder().arcEnd(ARG).build())', None),
    (r'state\.setDiscRings', 'state.set("disc", state.disc().toBuilder().rings((int)ARG).build())', None),
    
    # PRISM
    (r'state\.setPrismSides', 'state.set("prism", state.prism().toBuilder().sides((int)ARG).build())', None),
    (r'state\.setPrismRadius', 'state.set("prism", state.prism().toBuilder().radius(ARG).build())', None),
    (r'state\.setPrismHeight', 'state.set("prism", state.prism().toBuilder().height(ARG).build())', None),
    (r'state\.setPrismTopRadius', 'state.set("prism", state.prism().toBuilder().topRadius(ARG).build())', None),
    (r'state\.setPrismTwist', 'state.set("prism", state.prism().toBuilder().twist(ARG).build())', None),
    (r'state\.setPrismHeightSegments', 'state.set("prism", state.prism().toBuilder().heightSegments((int)ARG).build())', None),
    (r'state\.setPrismCapTop', 'state.set("prism", state.prism().toBuilder().capTop(ARG).build())', None),
    (r'state\.setPrismCapBottom', 'state.set("prism", state.prism().toBuilder().capBottom(ARG).build())', None),
    
    # CYLINDER
    (r'state\.setCylinderRadius', 'state.set("cylinder", state.cylinder().toBuilder().radius(ARG).build())', None),
    (r'state\.setCylinderHeight', 'state.set("cylinder", state.cylinder().toBuilder().height(ARG).build())', None),
    (r'state\.setCylinderSegments', 'state.set("cylinder", state.cylinder().toBuilder().segments((int)ARG).build())', None),
    (r'state\.setCylinderTopRadius', 'state.set("cylinder", state.cylinder().toBuilder().topRadius(ARG).build())', None),
    (r'state\.setCylinderArc', 'state.set("cylinder", state.cylinder().toBuilder().arc(ARG).build())', None),
    (r'state\.setCylinderHeightSegments', 'state.set("cylinder", state.cylinder().toBuilder().heightSegments((int)ARG).build())', None),
    (r'state\.setCylinderCapTop', 'state.set("cylinder", state.cylinder().toBuilder().capTop(ARG).build())', None),
    (r'state\.setCylinderCapBottom', 'state.set("cylinder", state.cylinder().toBuilder().capBottom(ARG).build())', None),
    (r'state\.setCylinderOpenEnded', 'state.set("cylinder", state.cylinder().toBuilder().isTube(ARG).build())', None),
    
    # POLYHEDRON
    (r'state\.setPolyType', 'state.set("polyhedron", state.polyhedron().toBuilder().polyType(PolyType.valueOf(ARG)).build())', 'PolyType'),
    (r'state\.setPolyRadius', 'state.set("polyhedron", state.polyhedron().toBuilder().radius(ARG).build())', None),
    (r'state\.setPolySubdivisions', 'state.set("polyhedron", state.polyhedron().toBuilder().subdivisions((int)ARG).build())', None),
    
    # TRANSFORM
    (r'state\.setAnchor', 'state.set("transform", state.transform().toBuilder().anchor(Anchor.valueOf(ARG)).build())', 'Anchor'),
    (r'state\.setScale', 'state.set("transform", state.transform().toBuilder().scale(ARG).build())', None),
    (r'state\.setFacing', 'state.set("transform", state.transform().toBuilder().facing(Facing.valueOf(ARG)).build())', 'Facing'),
    (r'state\.setBillboard', 'state.set("transform", state.transform().toBuilder().billboard(Billboard.valueOf(ARG)).build())', 'Billboard'),
    
    # ORBIT
    (r'state\.setOrbitEnabled', 'state.set("orbit", state.orbit().toBuilder().enabled(ARG).build())', None),
    (r'state\.setOrbitRadius', 'state.set("orbit", state.orbit().toBuilder().radius(ARG).build())', None),
    (r'state\.setOrbitSpeed', 'state.set("orbit", state.orbit().toBuilder().speed(ARG).build())', None),
    (r'state\.setOrbitAxis', 'state.set("orbit", state.orbit().toBuilder().axis(Axis.valueOf(ARG)).build())', 'Axis'),
    (r'state\.setOrbitPhase', 'state.set("orbit", state.orbit().toBuilder().phase(ARG).build())', None),
    
    # SPIN
    (r'state\.setSpinEnabled', 'state.set("spin", (ARG) ? SpinConfig.DEFAULT : SpinConfig.NONE)', 'SpinConfig'),
    (r'state\.setSpinAxis', 'state.set("spin", state.spin().toBuilder().axis(Axis.valueOf(ARG)).build())', 'Axis'),
    (r'state\.setSpinSpeed', 'state.set("spin", state.spin().toBuilder().speed(ARG).build())', None),
    
    # PULSE
    (r'state\.setPulseEnabled', 'state.set("pulse", (ARG) ? PulseConfig.DEFAULT : PulseConfig.NONE)', 'PulseConfig'),
    (r'state\.setPulseFrequency', 'state.set("pulse", state.pulse().toBuilder().speed(ARG).build())', None),
    (r'state\.setPulseAmplitude', 'state.set("pulse", state.pulse().toBuilder().scale(ARG).build())', None),
    # Note: setPulseMode removed - no longer needed, waveform controls mode
    
    # ALPHA FADE (AlphaPulseConfig)
    (r'state\.setAlphaFadeEnabled', 'state.set("alphaPulse", (ARG) ? AlphaPulseConfig.DEFAULT : AlphaPulseConfig.NONE)', 'AlphaPulseConfig'),
    (r'state\.setAlphaMin', 'state.set("alphaPulse", state.alphaPulse().toBuilder().min(ARG).build())', None),
    (r'state\.setAlphaMax', 'state.set("alphaPulse", state.alphaPulse().toBuilder().max(ARG).build())', None),
    
    # MASK
    (r'state\.setMaskType', 'state.set("mask", state.mask().toBuilder().mask(MaskType.valueOf(ARG)).build())', 'MaskType'),
    (r'state\.setMaskCount', 'state.set("mask", state.mask().toBuilder().count((int)ARG).build())', None),
    (r'state\.setMaskThickness', 'state.set("mask", state.mask().toBuilder().thickness(ARG).build())', None),
    (r'state\.setMaskOffset', 'state.set("mask", state.mask().toBuilder().offset(ARG).build())', None),
    (r'state\.setMaskFeather', 'state.set("mask", state.mask().toBuilder().feather(ARG).build())', None),
    (r'state\.setMaskInverted', 'state.set("mask", state.mask().toBuilder().invert(ARG).build())', None),
    (r'state\.setMaskAnimated', 'state.set("mask", state.mask().toBuilder().animate(ARG).build())', None),
    (r'state\.setMaskAnimateSpeed', 'state.set("mask", state.mask().toBuilder().animSpeed(ARG).build())', None),
    
    # APPEARANCE (direct fields)
    (r'state\.setGlow', 'state.set("glow", ARG)', None),
    (r'state\.setEmissive', 'state.set("emissive", ARG)', None),
    (r'state\.setPrimaryColor', 'state.set("primaryColor", ARG)', None),
    (r'state\.setSecondaryColor', 'state.set("secondaryColor", ARG)', None),
    (r'state\.setSaturation', 'state.set("saturation", ARG)', None),
    
    # MODIFIERS
    (r'state\.setWaveDirection', 'state.set("wave", state.wave().toBuilder().direction(Axis.valueOf(ARG)).build())', 'Axis'),
    (r'state\.setWobbleAmplitude', 'state.set("wobble", state.wobble().toBuilder().amplitude(new Vector3f(ARG, ARG, ARG)).build())', 'Vector3f'),
    (r'state\.setWobbleSpeed', 'state.set("wobble", state.wobble().toBuilder().speed(ARG).build())', None),
    (r'state\.setColorCycleEnabled', 'state.set("colorCycle", (ARG) ? ColorCycleConfig.builder().speed(1f).build() : ColorCycleConfig.builder().build())', None),
    (r'state\.setColorCycleSpeed', 'state.set("colorCycle", state.colorCycle().toBuilder().speed(ARG).build())', None),
    
    # ACTION PANEL - these keep the same API (methods added to FieldEditState)
    # No transformation needed for setLivePreviewEnabled, setAutoSaveEnabled
    (r'state\.restoreFromSnapshot', 'state.restoreSnapshot()', None),
    
    # TRANSFORM complex setters
    (r'state\.setOffset', 'state.set("transform", state.transform().toBuilder().offset(new Vector3f(ARG)).build())', 'Vector3f'),
    (r'state\.setRotation', 'state.set("transform", state.transform().toBuilder().rotation(new Vector3f(ARG)).build())', 'Vector3f'),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # FILL CONFIG
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setFillMode', 'state.set("fill", state.fill().toBuilder().mode(FillMode.valueOf(ARG)).build())', 'FillMode'),
    (r'state\.setWireThickness', 'state.set("fill", state.fill().toBuilder().wireThickness(ARG).build())', None),
    (r'state\.setDoubleSided', 'state.set("fill", state.fill().toBuilder().doubleSided(ARG).build())', None),
    (r'state\.setDepthTest', 'state.set("fill", state.fill().toBuilder().depthTest(ARG).build())', None),
    (r'state\.setDepthWrite', 'state.set("fill", state.fill().toBuilder().depthWrite(ARG).build())', None),
    (r'state\.setPointSize', 'state.set("pointSize", ARG)', None),  # Direct field
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # PRIMITIVE FIELDS (direct values)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setAlpha', 'state.set("alpha", ARG)', None),
    (r'state\.setColor', 'state.set("color", ARG)', None),
    (r'state\.setRadius', 'state.set("radius", ARG)', None),
    (r'state\.setShapeType', 'state.set("shapeType", ARG)', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # WAVE CONFIG
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setWaveEnabled', 'state.set("wave", (ARG) ? WaveConfig.builder().amplitude(0.1f).frequency(2f).direction(Axis.Y).build() : WaveConfig.NONE)', 'WaveConfig'),
    (r'state\.setWaveAmplitude', 'state.set("wave", state.wave().toBuilder().amplitude(ARG).build())', None),
    (r'state\.setWaveFrequency', 'state.set("wave", state.wave().toBuilder().frequency(ARG).build())', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # WOBBLE CONFIG
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setWobbleEnabled', 'state.set("wobble", (ARG) ? WobbleConfig.builder().amplitude(new Vector3f(0.1f, 0.1f, 0.1f)).speed(1f).build() : WobbleConfig.builder().build())', 'WobbleConfig'),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # COLOR CYCLE
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setColorCycleBlend', 'state.set("colorCycle", state.colorCycle().toBuilder().blend(ARG).build())', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # MODIFIERS (legacy mappings)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setModifierBobbing', 'state.set("wobble", state.wobble().toBuilder().amplitude(new Vector3f(ARG, ARG, ARG)).build())', 'Vector3f'),
    (r'state\.setModifierBreathing', 'state.set("pulse", state.pulse().toBuilder().scale(ARG).build())', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # FOLLOW & PREDICTION
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setFollowMode', 'state.set("followMode", FollowMode.valueOf(ARG))', 'FollowMode'),
    (r'state\.setFollowEnabled', 'state.set("followConfig", (ARG) ? FollowModeConfig.DEFAULT : FollowModeConfig.NONE)', 'FollowModeConfig'),
    (r'state\.setPredictionEnabled', 'state.set("predictionEnabled", ARG)', None),
    (r'state\.setPredictionLeadTicks', 'state.set("prediction", state.prediction().toBuilder().leadTicks((int)ARG).build())', None),
    (r'state\.setPredictionLookAhead', 'state.set("prediction", state.prediction().toBuilder().lookAhead(ARG).build())', None),
    (r'state\.setPredictionMaxDistance', 'state.set("prediction", state.prediction().toBuilder().maxDistance(ARG).build())', None),
    (r'state\.setPredictionVerticalBoost', 'state.set("prediction", state.prediction().toBuilder().verticalBoost(ARG).build())', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # BEAM CONFIG
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setBeamEnabled', 'state.set("beam", (ARG) ? BeamConfig.DEFAULT : BeamConfig.NONE)', 'BeamConfig'),
    (r'state\.setBeamColor', 'state.set("beam", state.beam().toBuilder().color(ARG).build())', None),
    (r'state\.setBeamGlow', 'state.set("beam", state.beam().toBuilder().glow(ARG).build())', None),
    (r'state\.setBeamHeight', 'state.set("beam", state.beam().toBuilder().height(ARG).build())', None),
    (r'state\.setBeamInnerRadius', 'state.set("beam", state.beam().toBuilder().innerRadius(ARG).build())', None),
    (r'state\.setBeamOuterRadius', 'state.set("beam", state.beam().toBuilder().outerRadius(ARG).build())', None),
    # Beam pulse setters - need nested PulseConfig in BeamConfig, skip for now
    # These will be left as-is and cause compile errors to fix manually
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # ARRANGEMENT (pattern setters)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setQuadPattern', 'state.set("arrangement", state.arrangement().toBuilder().quadPattern(ARG).build())', None),
    (r'state\.setSegmentPattern', 'state.set("arrangement", state.arrangement().toBuilder().segmentPattern(ARG).build())', None),
    (r'state\.setSectorPattern', 'state.set("arrangement", state.arrangement().toBuilder().sectorPattern(ARG).build())', None),
    (r'state\.setMultiPartArrangement', 'state.set("arrangement", state.arrangement().toBuilder().multiPart(ARG).build())', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # CAGE OPTIONS (direct fields in FieldEditState)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setCageLatCount', 'state.set("cageLatCount", ARG)', None),
    (r'state\.setCageLonCount', 'state.set("cageLonCount", ARG)', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # LIFECYCLE STATE (direct fields)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setFadeInTicks', 'state.set("fadeInTicks", ARG)', None),
    (r'state\.setFadeOutTicks', 'state.set("fadeOutTicks", ARG)', None),
    (r'state\.setLifecycleState', 'state.set("lifecycleState", ARG)', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # TRIGGER STATE (direct fields)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setTriggerType', 'state.set("triggerType", ARG)', None),
    (r'state\.setTriggerEffect', 'state.set("triggerEffect", ARG)', None),
    (r'state\.setTriggerIntensity', 'state.set("triggerIntensity", ARG)', None),
    (r'state\.setTriggerDuration', 'state.set("triggerDuration", ARG)', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # LINKING STATE (direct fields)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setPrimitiveId', 'state.set("primitiveId", ARG)', None),
    (r'state\.setRadiusOffset', 'state.set("radiusOffset", ARG)', None),
    (r'state\.setPhaseOffset', 'state.set("phaseOffset", ARG)', None),
    (r'state\.setMirrorAxis', 'state.set("mirrorAxis", ARG)', None),
    (r'state\.setFollowLinked', 'state.set("followLinked", ARG)', None),
    (r'state\.setScaleWithLinked', 'state.set("scaleWithLinked", ARG)', None),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # BEAM PULSE (nested pulse in BeamConfig)
    # ═══════════════════════════════════════════════════════════════════════════════
    (r'state\.setBeamPulseEnabled', 'state.set("beam", state.beam().toBuilder().pulse((ARG) ? PulseConfig.DEFAULT : null).build())', 'PulseConfig'),
    (r'state\.setBeamPulseScale', 'state.set("beam", state.beam().toBuilder().pulse(state.beam().pulse() != null ? state.beam().pulse().toBuilder().scale(ARG).build() : PulseConfig.sine(ARG, 1f)).build())', 'PulseConfig'),
    (r'state\.setBeamPulseSpeed', 'state.set("beam", state.beam().toBuilder().pulse(state.beam().pulse() != null ? state.beam().pulse().toBuilder().speed(ARG).build() : PulseConfig.sine(0.1f, ARG)).build())', 'PulseConfig'),
    (r'state\.setBeamPulseWaveform', 'state.set("beam", state.beam().toBuilder().pulse(state.beam().pulse() != null ? state.beam().pulse().toBuilder().waveform(Waveform.valueOf(ARG)).build() : PulseConfig.DEFAULT).build())', 'Waveform'),
    (r'state\.setBeamPulseMin', 'state.set("beam", state.beam().toBuilder().pulse(state.beam().pulse() != null ? state.beam().pulse().toBuilder().min(ARG).build() : PulseConfig.DEFAULT).build())', 'PulseConfig'),
    (r'state\.setBeamPulseMax', 'state.set("beam", state.beam().toBuilder().pulse(state.beam().pulse() != null ? state.beam().pulse().toBuilder().max(ARG).build() : PulseConfig.DEFAULT).build())', 'PulseConfig'),
    
    # ═══════════════════════════════════════════════════════════════════════════════
    # LAYER MANAGEMENT (these methods stay on FieldEditState - no transformation)
    # setLayerBlendMode(index, mode), setLayerAlpha(index, alpha), setLayerOrder(index, order)
    # setSelectedLayerIndex(index), setSelectedPrimitiveIndex(index)
    # These have multiple parameters so regex won't transform them cleanly.
    # They'll stay as wrapper methods on FieldEditState.
    # ═══════════════════════════════════════════════════════════════════════════════
]

# ═══════════════════════════════════════════════════════════════════════════════
# IMPORTS TO ADD
# ═══════════════════════════════════════════════════════════════════════════════

IMPORT_MAP = {
    'SpinConfig': 'import net.cyberpunk042.visual.animation.SpinConfig;',
    'PulseConfig': 'import net.cyberpunk042.visual.animation.PulseConfig;',
    'AlphaPulseConfig': 'import net.cyberpunk042.visual.animation.AlphaPulseConfig;',
    'Axis': 'import net.cyberpunk042.visual.animation.Axis;',
    'WaveConfig': 'import net.cyberpunk042.visual.animation.WaveConfig;',
    'WobbleConfig': 'import net.cyberpunk042.visual.animation.WobbleConfig;',
    'SphereAlgorithm': 'import net.cyberpunk042.visual.shape.SphereAlgorithm;',
    'PolyType': 'import net.cyberpunk042.visual.shape.PolyType;',
    'Anchor': 'import net.cyberpunk042.visual.transform.Anchor;',
    'Facing': 'import net.cyberpunk042.visual.transform.Facing;',
    'Billboard': 'import net.cyberpunk042.visual.transform.Billboard;',
    'MaskType': 'import net.cyberpunk042.visual.visibility.MaskType;',
    'FillMode': 'import net.cyberpunk042.visual.fill.FillMode;',
    'FollowMode': 'import net.cyberpunk042.field.instance.FollowMode;',
    'FollowModeConfig': 'import net.cyberpunk042.field.instance.FollowModeConfig;',
    'PredictionConfig': 'import net.cyberpunk042.field.instance.PredictionConfig;',
    'BeamConfig': 'import net.cyberpunk042.field.BeamConfig;',
    'ArrangementConfig': 'import net.cyberpunk042.visual.pattern.ArrangementConfig;',
    'Waveform': 'import net.cyberpunk042.visual.animation.Waveform;',
    'Vector3f': 'import org.joml.Vector3f;',
}


def find_balanced_call(content, start_pattern):
    """Find a method call with balanced parentheses starting from pattern match."""
    match = re.search(start_pattern + r'\(', content)
    if not match:
        return None, None, None
    
    start = match.start()
    paren_start = match.end() - 1
    depth = 1
    i = paren_start + 1
    
    while i < len(content) and depth > 0:
        if content[i] == '(':
            depth += 1
        elif content[i] == ')':
            depth -= 1
        i += 1
    
    if depth == 0:
        full_match = content[start:i]
        arg = content[paren_start + 1:i - 1]
        return start, full_match, arg
    return None, None, None


def replace_setter(content, old_pattern, new_template):
    """Replace setter calls handling nested parentheses."""
    changes = []
    result = content
    offset = 0
    
    while True:
        # Search only from current offset to avoid infinite loops
        start, full_match, arg = find_balanced_call(result[offset:], old_pattern)
        if start is None:
            break
        
        # Adjust start to absolute position
        abs_start = offset + start
        
        new_call = new_template.replace('ARG', arg)
        
        # Skip if replacement is same as original (no-op)
        if full_match == new_call:
            offset = abs_start + len(full_match)
            continue
            
        changes.append((full_match, new_call))
        result = result[:abs_start] + new_call + result[abs_start + len(full_match):]
        
        # Move offset past the replacement
        offset = abs_start + len(new_call)
    
    return result, changes


def add_imports(content, imports_needed):
    """Add required imports to the file."""
    if not imports_needed:
        return content
    
    # Find the package line
    pkg_match = re.search(r'^package\s+[^;]+;', content, re.MULTILINE)
    if not pkg_match:
        return content
    
    # Find existing imports
    import_section_end = pkg_match.end()
    last_import = None
    for m in re.finditer(r'^import\s+[^;]+;', content, re.MULTILINE):
        last_import = m
        import_section_end = m.end()
    
    # Filter out already present imports
    existing_imports = set(re.findall(r'^import\s+([^;]+);', content, re.MULTILINE))
    new_imports = []
    for imp_name in imports_needed:
        if imp_name in IMPORT_MAP:
            import_line = IMPORT_MAP[imp_name]
            # Extract the full class path from import line
            class_path = import_line.replace('import ', '').replace(';', '').strip()
            if class_path not in existing_imports:
                new_imports.append(import_line)
    
    if new_imports:
        insert_pos = import_section_end
        import_block = '\n' + '\n'.join(sorted(set(new_imports)))
        content = content[:insert_pos] + import_block + content[insert_pos:]
    
    return content


def update_file(filepath, dry_run=True):
    """Update a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    imports_needed = set()
    
    # Replace getters
    for old_method, new_expr in GETTER_MAPPINGS.items():
        pattern = r'state\.' + old_method + r'\(\)'
        
        # Check if this is a ternary getter (already includes state. prefix in mapping)
        if old_method in TERNARY_GETTERS:
            replacement = f'({new_expr})'
        elif '?' in new_expr or new_expr.startswith('"') or (new_expr and new_expr[0].isdigit()):
            # Ternary or literal - wrap in parens
            replacement = f'({new_expr})'
        else:
            replacement = f'state.{new_expr}'
        
        matches = re.findall(pattern, content)
        if matches:
            for m in matches:
                changes.append((m, replacement))
            content = re.sub(pattern, replacement, content)
    
    # Replace setters
    for pattern_prefix, new_template, import_needed in SETTER_DEFS:
        new_content, setter_changes = replace_setter(content, pattern_prefix, new_template)
        if setter_changes:
            changes.extend(setter_changes)
            content = new_content
            if import_needed:
                imports_needed.add(import_needed)
    
    # Add imports if needed
    if imports_needed and content != original:
        content = add_imports(content, imports_needed)
    
    if len(changes) > 0 and not dry_run:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    
    return changes, imports_needed


def find_java_files(directory):
    """Find all Java files (excluding .bak)."""
    java_files = []
    for root, dirs, files in os.walk(directory):
        for f in files:
            if f.endswith('.java') and not f.endswith('.bak'):
                java_files.append(os.path.join(root, f))
    return java_files


def main():
    dry_run = '--dry-run' in sys.argv
    
    print("=" * 70)
    print("Updating FieldEditState callers to new API")
    if dry_run:
        print("🔍 DRY RUN MODE")
    print("=" * 70)
    
    files = find_java_files(CLIENT_DIR)
    print(f"\nScanning {len(files)} Java files...")
    
    total_changes = 0
    files_changed = []
    
    for filepath in files:
        changes, imports = update_file(filepath, dry_run)
        if len(changes) > 0:
            rel_path = os.path.relpath(filepath)
            print(f"\n📁 {rel_path}: {len(changes)} changes")
            files_changed.append(rel_path)
            total_changes += len(changes)
            
            # Show preview of changes (deduplicated)
            if dry_run:
                seen = set()
                for old, new in changes:
                    key = (old, new)
                    if key not in seen:
                        seen.add(key)
                        print(f"    - {old}")
                        print(f"    + {new}")
                
                if imports:
                    print(f"    📦 Imports needed: {', '.join(imports)}")
    
    print(f"\n{'=' * 70}")
    print(f"📊 Total: {total_changes} changes in {len(files_changed)} files")
    
    if dry_run:
        print("\n🔍 DRY RUN - No files modified")
    else:
        print("\n✅ Files updated!")
        print("   Run ./gradlew build to check for remaining issues.")


if __name__ == "__main__":
    main()
