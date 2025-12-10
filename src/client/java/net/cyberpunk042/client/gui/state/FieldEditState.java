package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.profile.Profile;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.FillMode;

import java.util.ArrayList;
import java.util.List;

/**
 * G02: Manages the shared editing state for both GUI and /field commands.
 * 
 * <p>This is the single source of truth for field configuration being edited.
 * Used by:</p>
 * <ul>
 *   <li>FieldCustomizerScreen (GUI panels)</li>
 *   <li>/field commands (via S2C packets)</li>
 *   <li>TestFieldRenderer (client-side preview)</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.gui.screen.FieldCustomizerScreen
 */
public class FieldEditState {
    
    public static final class ProfileEntry {
        private final String name;
        private final boolean server;
        
        public ProfileEntry(String name, boolean server) {
            this.name = name;
            this.server = server;
        }
        
        public String name() { return name; }
        public boolean isServer() { return server; }
    }
    
    private FieldDefinition originalDefinition;
    private FieldDefinition workingDefinition;
    private boolean isDirty = false;
    private boolean debugUnlocked = false;
    private String currentProfileName = "default";
    private boolean currentProfileServer = false;
    private final List<ProfileEntry> profiles = new ArrayList<>();
    
    // Profile snapshot - stores original values when profile was loaded
    // Snapshot stored as JSON string - captures ALL state, not just a few fields
    private String snapshotJson = null;
    
    /**
     * Saves current state as JSON snapshot.
     * Call this when loading a profile to enable revert functionality.
     */
    public void saveProfileSnapshot() {
        snapshotJson = toStateJson();
        Logging.GUI.topic("state").debug("Profile snapshot saved ({} chars)", snapshotJson.length());
    }
    
    /**
     * Restore state from JSON snapshot (for revert).
     */
    public void restoreFromSnapshot() {
        if (snapshotJson == null) {
            Logging.GUI.topic("state").warn("No snapshot to restore");
            return;
        }
        fromStateJson(snapshotJson);
        Logging.GUI.topic("state").debug("State restored from snapshot");
    }
    
    /**
     * Check if snapshot exists.
     */
    public boolean hasSnapshot() {
        return snapshotJson != null;
    }
    
    // Undo/Redo (simple stubs for now)
    private java.util.Deque<String> undoStack = new java.util.ArrayDeque<>();
    private java.util.Deque<String> redoStack = new java.util.ArrayDeque<>();
    
    // Quick Panel state (G41-G50)
    private String shapeType = "sphere";
    private float radius = 3.0f;
    private int color = 0xFF00FFFF;
    private float alpha = 0.8f;
    private FillMode fillMode = FillMode.SOLID;
    private float spinSpeed = 45.0f; // degrees per second
    private FollowMode followMode = FollowMode.SMOOTH;
    private boolean predictionEnabled = true;
    private int predictionTicks = 2;
    
    // Appearance state (G71-G75)
    private float glow = 0.5f;
    private float emissive = 0f;
    private float saturation = 0f;
    private int primaryColor = 0xFF00FFFF;   // Cyan
    private int secondaryColor = 0xFFFF00FF; // Magenta
    
    public float getGlow() { return glow; }
    public void setGlow(float v) { glow = v; markDirty(); }
    public float getEmissive() { return emissive; }
    public void setEmissive(float v) { emissive = v; markDirty(); }
    public float getSaturation() { return saturation; }
    public void setSaturation(float v) { saturation = v; markDirty(); }
    public int getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(int c) { primaryColor = c; markDirty(); }
    public int getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(int c) { secondaryColor = c; markDirty(); }
    
    // Animation state (G76-G80)
    private boolean spinEnabled = false;
    private String spinAxis = "Y";
    // spinSpeed is defined in Quick Panel section above
    private boolean pulseEnabled = false;
    private String pulseMode = "SCALE";
    private float pulseFrequency = 1f;
    private float pulseAmplitude = 0.1f;
    private boolean alphaFadeEnabled = false;
    private float alphaMin = 0.3f;
    private float alphaMax = 1f;
    
    public boolean isSpinEnabled() { return spinEnabled; }
    public void setSpinEnabled(boolean v) { spinEnabled = v; markDirty(); }
    public String getSpinAxis() { return spinAxis; }
    public void setSpinAxis(String v) { spinAxis = v; markDirty(); }
    // getSpinSpeed/setSpinSpeed defined in Quick Panel section
    public boolean isPulseEnabled() { return pulseEnabled; }
    public void setPulseEnabled(boolean v) { pulseEnabled = v; markDirty(); }
    public String getPulseMode() { return pulseMode; }
    public void setPulseMode(String v) { pulseMode = v; markDirty(); }
    public float getPulseFrequency() { return pulseFrequency; }
    public void setPulseFrequency(float v) { pulseFrequency = v; markDirty(); }
    public float getPulseAmplitude() { return pulseAmplitude; }
    public void setPulseAmplitude(float v) { pulseAmplitude = v; markDirty(); }
    public boolean isAlphaFadeEnabled() { return alphaFadeEnabled; }
    public void setAlphaFadeEnabled(boolean v) { alphaFadeEnabled = v; markDirty(); }
    public float getAlphaMin() { return alphaMin; }
    public void setAlphaMin(float v) { alphaMin = v; markDirty(); }
    public float getAlphaMax() { return alphaMax; }
    public void setAlphaMax(float v) { alphaMax = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD MODIFIERS (bobbing, breathing)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private float modifierBobbing = 0f;     // vertical oscillation strength
    private float modifierBreathing = 0f;   // scale breathing strength
    
    public float getModifierBobbing() { return modifierBobbing; }
    public void setModifierBobbing(float v) { modifierBobbing = v; markDirty(); }
    public float getModifierBreathing() { return modifierBreathing; }
    public void setModifierBreathing(float v) { modifierBreathing = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION EXTRAS (colorCycle, wobble, wave)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Color Cycle
    private boolean colorCycleEnabled = false;
    private float colorCycleSpeed = 1f;
    private boolean colorCycleBlend = true; // smooth blend vs instant
    
    public boolean isColorCycleEnabled() { return colorCycleEnabled; }
    public void setColorCycleEnabled(boolean v) { colorCycleEnabled = v; markDirty(); }
    public float getColorCycleSpeed() { return colorCycleSpeed; }
    public void setColorCycleSpeed(float v) { colorCycleSpeed = v; markDirty(); }
    public boolean isColorCycleBlend() { return colorCycleBlend; }
    public void setColorCycleBlend(boolean v) { colorCycleBlend = v; markDirty(); }
    
    // Wobble (random movement)
    private boolean wobbleEnabled = false;
    private float wobbleAmplitude = 0.1f;
    private float wobbleSpeed = 1f;
    
    public boolean isWobbleEnabled() { return wobbleEnabled; }
    public void setWobbleEnabled(boolean v) { wobbleEnabled = v; markDirty(); }
    public float getWobbleAmplitude() { return wobbleAmplitude; }
    public void setWobbleAmplitude(float v) { wobbleAmplitude = v; markDirty(); }
    public float getWobbleSpeed() { return wobbleSpeed; }
    public void setWobbleSpeed(float v) { wobbleSpeed = v; markDirty(); }
    
    // Wave (surface deformation)
    private boolean waveEnabled = false;
    private float waveAmplitude = 0.1f;
    private float waveFrequency = 2f;
    private String waveDirection = "Y"; // X, Y, Z, or RADIAL
    
    public boolean isWaveEnabled() { return waveEnabled; }
    public void setWaveEnabled(boolean v) { waveEnabled = v; markDirty(); }
    public float getWaveAmplitude() { return waveAmplitude; }
    public void setWaveAmplitude(float v) { waveAmplitude = v; markDirty(); }
    public float getWaveFrequency() { return waveFrequency; }
    public void setWaveFrequency(float v) { waveFrequency = v; markDirty(); }
    public String getWaveDirection() { return waveDirection; }
    public void setWaveDirection(String v) { waveDirection = v; markDirty(); }
    
    // Debug Panel - Lifecycle state (G82-G85)
    private String lifecycleState = "ACTIVE";
    private int fadeInTicks = 20;
    private int fadeOutTicks = 20;
    
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String s) { lifecycleState = s; markDirty(); }
    public int getFadeInTicks() { return fadeInTicks; }
    public void setFadeInTicks(int t) { fadeInTicks = t; markDirty(); }
    public int getFadeOutTicks() { return fadeOutTicks; }
    public void setFadeOutTicks(int t) { fadeOutTicks = t; markDirty(); }
    
    // Debug Panel - Trigger state (G86-G90)
    private String triggerType = "DAMAGE";
    private String triggerEffect = "FLASH";
    private float triggerIntensity = 1f;
    private int triggerDuration = 10;
    
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String t) { triggerType = t; markDirty(); }
    public String getTriggerEffect() { return triggerEffect; }
    public void setTriggerEffect(String e) { triggerEffect = e; markDirty(); }
    public float getTriggerIntensity() { return triggerIntensity; }
    public void setTriggerIntensity(float i) { triggerIntensity = i; markDirty(); }
    public int getTriggerDuration() { return triggerDuration; }
    public void setTriggerDuration(int d) { triggerDuration = d; markDirty(); }
    
    // Action Panel state (G57-G60)
    private boolean livePreviewEnabled = true;
    private boolean autoSaveEnabled = false;
    
    public boolean isLivePreviewEnabled() { return livePreviewEnabled; }
    public void setLivePreviewEnabled(boolean enabled) { this.livePreviewEnabled = enabled; }
    public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
    public void setAutoSaveEnabled(boolean enabled) { this.autoSaveEnabled = enabled; }
    
    public FieldEditState() {
        Logging.GUI.topic("state").debug("GuiState created");
        seedProfiles();
    }
    
    public FieldEditState(FieldDefinition definition) {
        this.originalDefinition = definition;
        this.workingDefinition = definition;
        Logging.GUI.topic("state").debug("GuiState created with definition");
        seedProfiles();
    }
    
    // Quick Panel getters
    public String getShapeType() { return shapeType; }
    public float getRadius() { return radius; }
    public int getColor() { return color; }
    public float getAlpha() { return alpha; }
    public FillMode getFillMode() { return fillMode; }
    public float getSpinSpeed() { return spinSpeed; }
    public FollowMode getFollowMode() { return followMode; }
    public boolean isPredictionEnabled() { return predictionEnabled; }
    public int getPredictionTicks() { return predictionTicks; }
    
    // Quick Panel setters
    public void setShapeType(String type) { this.shapeType = type; markDirty(); }
    public void setRadius(float r) { this.radius = r; markDirty(); }
    public void setColor(int c) { this.color = c; markDirty(); }
    public void setAlpha(float a) { this.alpha = a; markDirty(); }
    public void setFillMode(FillMode m) { this.fillMode = m; markDirty(); }
    public void setSpinSpeed(float s) { this.spinSpeed = s; markDirty(); }
    public void setFollowMode(FollowMode m) { this.followMode = m; markDirty(); }
    public void setPredictionEnabled(boolean e) { this.predictionEnabled = e; markDirty(); }
    public void setPredictionTicks(int t) { this.predictionTicks = t; markDirty(); }
    
    // Core state
    public FieldDefinition getOriginal() { return originalDefinition; }
    public FieldDefinition getWorking() { return workingDefinition; }
    public boolean isDirty() { return isDirty; }
    public boolean isDebugUnlocked() { return debugUnlocked; }
    public String getCurrentProfileName() { return currentProfileName; }
    public boolean isCurrentProfileServer() { return currentProfileServer; }
    public boolean isCurrentProfileServerSourced() { return currentProfileServer; } // Alias
    public List<ProfileEntry> getProfiles() { return profiles; }
    
    public void setDebugUnlocked(boolean unlocked) { this.debugUnlocked = unlocked; }
    public void setCurrentProfileName(String name) { this.currentProfileName = name; }
    public void setCurrentProfile(String name, boolean server) {
        this.currentProfileName = name;
        this.currentProfileServer = server;
    }
    
    public void markDirty() {
        if (!isDirty) {
            isDirty = true;
            Logging.GUI.topic("state").trace("State marked dirty");
        }
    }
    
    public void markClean() { isDirty = false; }
    public void clearDirty() { isDirty = false; }
    
    // Undo/Redo
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    
    public void undo() {
        if (canUndo()) {
            String state = undoStack.pop();
            redoStack.push(state);
            Logging.GUI.topic("state").debug("Undo performed");
        }
    }
    
    public void redo() {
        if (canRedo()) {
            String state = redoStack.pop();
            undoStack.push(state);
            Logging.GUI.topic("state").debug("Redo performed");
        }
    }
    
    public void pushUndo() {
        undoStack.push("state_" + System.currentTimeMillis());
        redoStack.clear();
    }

    private void seedProfiles() {
        if (!profiles.isEmpty()) {
            return;
        }
        profiles.add(new ProfileEntry("my_shield_v2", false));
        profiles.add(new ProfileEntry("radar_pulse", false));
        profiles.add(new ProfileEntry("cage_wire", false));
        profiles.add(new ProfileEntry("shield_default", true));
        profiles.add(new ProfileEntry("aura_heal", true));
        
        ProfileEntry first = profiles.get(0);
        this.currentProfileName = first.name();
        this.currentProfileServer = first.isServer();
    }

    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String anchor = "CENTER";
    private float offsetX = 0f, offsetY = 0f, offsetZ = 0f;
    private float rotationX = 0f, rotationY = 0f, rotationZ = 0f;
    private float scale = 1f;
    private String facing = "FIXED";
    private String billboard = "NONE";
    
    public String getAnchor() { return anchor; }
    public void setAnchor(String v) { anchor = v; markDirty(); }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public void setOffset(float x, float y, float z) { offsetX = x; offsetY = y; offsetZ = z; markDirty(); }
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    public void setRotation(float x, float y, float z) { rotationX = x; rotationY = y; rotationZ = z; markDirty(); }
    public float getScale() { return scale; }
    public void setScale(float v) { scale = v; markDirty(); }
    public String getFacing() { return facing; }
    public void setFacing(String v) { facing = v; markDirty(); }
    public String getBillboard() { return billboard; }
    public void setBillboard(String v) { billboard = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORBIT STATE (G-ORBIT)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean orbitEnabled = false;
    private float orbitRadius = 2.0f;
    private float orbitSpeed = 1.0f;
    private String orbitAxis = "Y";  // X, Y, Z
    private float orbitPhase = 0f;
    
    public boolean isOrbitEnabled() { return orbitEnabled; }
    public void setOrbitEnabled(boolean v) { orbitEnabled = v; markDirty(); }
    public float getOrbitRadius() { return orbitRadius; }
    public void setOrbitRadius(float v) { orbitRadius = v; markDirty(); }
    public float getOrbitSpeed() { return orbitSpeed; }
    public void setOrbitSpeed(float v) { orbitSpeed = v; markDirty(); }
    public String getOrbitAxis() { return orbitAxis; }
    public void setOrbitAxis(String v) { orbitAxis = v; markDirty(); }
    public float getOrbitPhase() { return orbitPhase; }
    public void setOrbitPhase(float v) { orbitPhase = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY MASK STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String maskType = "FULL";
    private int maskCount = 8;
    private float maskThickness = 0.5f;
    private float maskOffset = 0f;
    private float maskFeather = 0f;
    private boolean maskInverted = false;
    private boolean maskAnimated = false;
    private float maskAnimateSpeed = 1f;
    
    public String getMaskType() { return maskType; }
    public void setMaskType(String v) { maskType = v; markDirty(); }
    public int getMaskCount() { return maskCount; }
    public void setMaskCount(int v) { maskCount = v; markDirty(); }
    public float getMaskThickness() { return maskThickness; }
    public void setMaskThickness(float v) { maskThickness = v; markDirty(); }
    public float getMaskOffset() { return maskOffset; }
    public void setMaskOffset(float v) { maskOffset = v; markDirty(); }
    public float getMaskFeather() { return maskFeather; }
    public void setMaskFeather(float v) { maskFeather = v; markDirty(); }
    public boolean isMaskInverted() { return maskInverted; }
    public void setMaskInverted(boolean v) { maskInverted = v; markDirty(); }
    public boolean isMaskAnimated() { return maskAnimated; }
    public void setMaskAnimated(boolean v) { maskAnimated = v; markDirty(); }
    public float getMaskAnimateSpeed() { return maskAnimateSpeed; }
    public void setMaskAnimateSpeed(float v) { maskAnimateSpeed = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ARRANGEMENT STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String quadPattern = "FILLED";
    private String segmentPattern = "FULL";
    private String sectorPattern = "FULL";
    private boolean multiPartArrangement = false;
    
    public String getQuadPattern() { return quadPattern; }
    public void setQuadPattern(String v) { quadPattern = v; markDirty(); }
    public String getSegmentPattern() { return segmentPattern; }
    public void setSegmentPattern(String v) { segmentPattern = v; markDirty(); }
    public String getSectorPattern() { return sectorPattern; }
    public void setSectorPattern(String v) { sectorPattern = v; markDirty(); }
    public boolean isMultiPartArrangement() { return multiPartArrangement; }
    public void setMultiPartArrangement(boolean v) { multiPartArrangement = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILL STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private float wireThickness = 1f;
    private boolean doubleSided = true;
    private boolean depthTest = true;
    private boolean depthWrite = false;
    private int cageLatCount = 8;
    private int cageLonCount = 16;
    private float pointSize = 2f;
    
    public float getWireThickness() { return wireThickness; }
    public void setWireThickness(float v) { wireThickness = v; markDirty(); }
    public boolean isDoubleSided() { return doubleSided; }
    public void setDoubleSided(boolean v) { doubleSided = v; markDirty(); }
    public boolean isDepthTest() { return depthTest; }
    public void setDepthTest(boolean v) { depthTest = v; markDirty(); }
    public boolean isDepthWrite() { return depthWrite; }
    public void setDepthWrite(boolean v) { depthWrite = v; markDirty(); }
    public int getCageLatCount() { return cageLatCount; }
    public void setCageLatCount(int v) { cageLatCount = v; markDirty(); }
    public int getCageLonCount() { return cageLonCount; }
    public void setCageLonCount(int v) { cageLonCount = v; markDirty(); }
    public float getPointSize() { return pointSize; }
    public void setPointSize(float v) { pointSize = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String primitiveId = "";
    private float radiusOffset = 0f;
    private float phaseOffset = 0f;
    private String mirrorAxis = "NONE";
    private boolean followLinked = false;
    private boolean scaleWithLinked = false;
    
    public String getPrimitiveId() { return primitiveId; }
    public void setPrimitiveId(String v) { primitiveId = v; markDirty(); }
    public float getRadiusOffset() { return radiusOffset; }
    public void setRadiusOffset(float v) { radiusOffset = v; markDirty(); }
    public float getPhaseOffset() { return phaseOffset; }
    public void setPhaseOffset(float v) { phaseOffset = v; markDirty(); }
    public String getMirrorAxis() { return mirrorAxis; }
    public void setMirrorAxis(String v) { mirrorAxis = v; markDirty(); }
    public boolean isFollowLinked() { return followLinked; }
    public void setFollowLinked(boolean v) { followLinked = v; markDirty(); }
    public boolean isScaleWithLinked() { return scaleWithLinked; }
    public void setScaleWithLinked(boolean v) { scaleWithLinked = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXTENDED PREDICTION STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int predictionLeadTicks = 2;
    private float predictionMaxDistance = 8f;
    private float predictionLookAhead = 0.5f;
    private float predictionVerticalBoost = 0f;
    
    public int getPredictionLeadTicks() { return predictionLeadTicks; }
    public void setPredictionLeadTicks(int v) { predictionLeadTicks = v; markDirty(); }
    public float getPredictionMaxDistance() { return predictionMaxDistance; }
    public void setPredictionMaxDistance(float v) { predictionMaxDistance = v; markDirty(); }
    public float getPredictionLookAhead() { return predictionLookAhead; }
    public void setPredictionLookAhead(float v) { predictionLookAhead = v; markDirty(); }
    public float getPredictionVerticalBoost() { return predictionVerticalBoost; }
    public void setPredictionVerticalBoost(float v) { predictionVerticalBoost = v; markDirty(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW MODE STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean followEnabled = true;
    
    public boolean isFollowEnabled() { return followEnabled; }
    public void setFollowEnabled(boolean v) { followEnabled = v; markDirty(); }

// ═══════════════════════════════════════════════════════════════════════════
    // LAYER MANAGEMENT (G51-G56)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int selectedLayerIndex = 0;
    private java.util.List<LayerState> layers = new java.util.ArrayList<>();
    private java.util.List<java.util.List<PrimitiveState>> primitivesPerLayer = new java.util.ArrayList<>();
    private java.util.List<Integer> selectedPrimitivePerLayer = new java.util.ArrayList<>();
    
    /** Simple layer state holder. */
    public static class LayerState {
        public boolean visible = true;
        public String name = "Layer";
        public String blendMode = "NORMAL";  // NORMAL, ADD, MULTIPLY, SCREEN
        public int order = 0;
        public float alpha = 1.0f;
        
        public LayerState(String name) { this.name = name; }
    }
    
    /** Simple primitive state holder (placeholder for future per-primitive data). */
    public static class PrimitiveState {
        public String id;
        public PrimitiveState(String id) { this.id = id; }
    }
    
    {
        // Initialize with one default layer
        layers.add(new LayerState("Layer 1"));
        java.util.List<PrimitiveState> primitives = new java.util.ArrayList<>();
        primitives.add(new PrimitiveState("primitive_1"));
        primitivesPerLayer.add(primitives);
        selectedPrimitivePerLayer.add(0);
    }
    
    public int getSelectedLayerIndex() { return selectedLayerIndex; }
    public void setSelectedLayerIndex(int index) { 
        this.selectedLayerIndex = Math.max(0, Math.min(index, layers.size() - 1));
        // Clamp primitive selection to new layer
        clampPrimitiveSelection(selectedLayerIndex);
    }
    
    public int getLayerCount() { return layers.size(); }
    
    public int addLayer() {
        if (layers.size() >= 10) return -1; // Max 10 layers
        layers.add(new LayerState("Layer " + (layers.size() + 1)));
        java.util.List<PrimitiveState> primitives = new java.util.ArrayList<>();
        primitives.add(new PrimitiveState("primitive_1"));
        primitivesPerLayer.add(primitives);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return layers.size() - 1;
    }
    
    public boolean removeLayer(int index) {
        if (layers.size() <= 1 || index < 0 || index >= layers.size()) return false;
        layers.remove(index);
        primitivesPerLayer.remove(index);
        selectedPrimitivePerLayer.remove(index);
        markDirty();
        return true;
    }
    
    public boolean isLayerVisible(int index) {
        if (index < 0 || index >= layers.size()) return false;
        return layers.get(index).visible;
    }
    
    public boolean toggleLayerVisibility(int index) {
        if (index < 0 || index >= layers.size()) return false;
        layers.get(index).visible = !layers.get(index).visible;
        markDirty();
        return layers.get(index).visible;
    }
    
    public boolean swapLayers(int a, int b) {
        if (a < 0 || a >= layers.size() || b < 0 || b >= layers.size()) return false;
        java.util.Collections.swap(layers, a, b);
        java.util.Collections.swap(primitivesPerLayer, a, b);
        java.util.Collections.swap(selectedPrimitivePerLayer, a, b);
        markDirty();
        return true;
    }
    
    public String getLayerBlendMode(int index) {
        if (index < 0 || index >= layers.size()) return "NORMAL";
        return layers.get(index).blendMode;
    }
    
    public void setLayerBlendMode(int index, String mode) {
        if (index < 0 || index >= layers.size()) return;
        layers.get(index).blendMode = mode;
        markDirty();
    }
    
    public int getLayerOrder(int index) {
        if (index < 0 || index >= layers.size()) return 0;
        return layers.get(index).order;
    }
    
    public void setLayerOrder(int index, int order) {
        if (index < 0 || index >= layers.size()) return;
        layers.get(index).order = order;
        markDirty();
    }
    
    public float getLayerAlpha(int index) {
        if (index < 0 || index >= layers.size()) return 1.0f;
        return layers.get(index).alpha;
    }
    
    public void setLayerAlpha(int index, float alpha) {
        if (index < 0 || index >= layers.size()) return;
        layers.get(index).alpha = Math.max(0f, Math.min(1f, alpha));
        markDirty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE MANAGEMENT (per layer)
    // ═══════════════════════════════════════════════════════════════════════════

    public int getPrimitiveCount(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return 0;
        return primitivesPerLayer.get(layerIndex).size();
    }

    public int getSelectedPrimitiveIndex() {
        if (selectedPrimitivePerLayer.isEmpty()) return 0;
        return selectedPrimitivePerLayer.get(selectedLayerIndex);
    }

    public void setSelectedPrimitiveIndex(int index) {
        if (selectedPrimitivePerLayer.isEmpty()) return;
        int clamped = clampPrimitiveIndex(selectedLayerIndex, index);
        selectedPrimitivePerLayer.set(selectedLayerIndex, clamped);
    }

    public String getPrimitiveName(int layerIndex, int primitiveIndex) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return "";
        return primitivesPerLayer.get(layerIndex).get(primitiveIndex).id;
    }

    public void renamePrimitive(int layerIndex, int primitiveIndex, String newId) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return;
        primitivesPerLayer.get(layerIndex).get(primitiveIndex).id = newId;
        markDirty();
    }

    public int addPrimitive(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return -1;
        var list = primitivesPerLayer.get(layerIndex);
        String id = "primitive_" + (list.size() + 1);
        list.add(new PrimitiveState(id));
        selectedPrimitivePerLayer.set(layerIndex, list.size() - 1);
        markDirty();
        return list.size() - 1;
    }

    public boolean removePrimitive(int layerIndex, int primitiveIndex) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return false;
        var list = primitivesPerLayer.get(layerIndex);
        if (list.size() <= 1) return false; // keep at least one primitive
        list.remove(primitiveIndex);
        clampPrimitiveSelection(layerIndex);
        markDirty();
        return true;
    }

    public boolean swapPrimitives(int layerIndex, int a, int b) {
        if (!isValidPrimitiveIndex(layerIndex, a) || !isValidPrimitiveIndex(layerIndex, b)) return false;
        java.util.Collections.swap(primitivesPerLayer.get(layerIndex), a, b);
        int selected = getSelectedPrimitiveIndex();
        if (selected == a) {
            selectedPrimitivePerLayer.set(layerIndex, b);
        } else if (selected == b) {
            selectedPrimitivePerLayer.set(layerIndex, a);
        }
        markDirty();
        return true;
    }

    private boolean isValidPrimitiveIndex(int layerIndex, int primitiveIndex) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return false;
        var list = primitivesPerLayer.get(layerIndex);
        return primitiveIndex >= 0 && primitiveIndex < list.size();
    }

    private int clampPrimitiveIndex(int layerIndex, int index) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return 0;
        var list = primitivesPerLayer.get(layerIndex);
        return Math.max(0, Math.min(index, Math.max(0, list.size() - 1)));
    }

    private void clampPrimitiveSelection(int layerIndex) {
        if (selectedPrimitivePerLayer.isEmpty()) return;
        int clamped = clampPrimitiveIndex(layerIndex, getSelectedPrimitiveIndex());
        selectedPrimitivePerLayer.set(layerIndex, clamped);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER/PRIMITIVE NAME-BASED OPERATIONS (for preset merge)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find layer index by name.
     * @return index or -1 if not found
     */
    public int findLayerByName(String name) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get layer name at index.
     */
    public String getLayerName(int index) {
        if (index < 0 || index >= layers.size()) return "";
        return layers.get(index).name;
    }

    /**
     * Set layer name at index.
     */
    public void setLayerName(int index, String name) {
        if (index < 0 || index >= layers.size()) return;
        layers.get(index).name = name;
        markDirty();
    }

    /**
     * Add a layer with specific name. If name conflicts, auto-increment.
     * @param baseName desired name (e.g., "Ring Layer")
     * @return index of the new layer, or -1 if max layers reached
     */
    public int addLayerWithName(String baseName) {
        if (layers.size() >= 10) return -1;
        
        String finalName = resolveLayerName(baseName);
        layers.add(new LayerState(finalName));
        java.util.List<PrimitiveState> primitives = new java.util.ArrayList<>();
        primitives.add(new PrimitiveState("primitive_1"));
        primitivesPerLayer.add(primitives);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return layers.size() - 1;
    }

    /**
     * Resolve layer name conflict by adding increment.
     * "Ring" -> "Ring", "Ring 2", "Ring 3", etc.
     */
    private String resolveLayerName(String baseName) {
        if (findLayerByName(baseName) == -1) {
            return baseName;
        }
        int counter = 2;
        while (findLayerByName(baseName + " " + counter) != -1) {
            counter++;
        }
        return baseName + " " + counter;
    }

    /**
     * Find primitive index by id in a layer.
     * @return index or -1 if not found
     */
    public int findPrimitiveById(int layerIndex, String id) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return -1;
        var list = primitivesPerLayer.get(layerIndex);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Add primitive to layer with specific id. If id conflicts, auto-increment.
     * @return index of the new primitive, or -1 if failed
     */
    public int addPrimitiveWithId(int layerIndex, String baseId) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return -1;
        var list = primitivesPerLayer.get(layerIndex);
        
        String finalId = resolvePrimitiveId(layerIndex, baseId);
        list.add(new PrimitiveState(finalId));
        selectedPrimitivePerLayer.set(layerIndex, list.size() - 1);
        markDirty();
        return list.size() - 1;
    }

    /**
     * Resolve primitive id conflict by adding increment.
     */
    private String resolvePrimitiveId(int layerIndex, String baseId) {
        if (findPrimitiveById(layerIndex, baseId) == -1) {
            return baseId;
        }
        int counter = 2;
        while (findPrimitiveById(layerIndex, baseId + "_" + counter) != -1) {
            counter++;
        }
        return baseId + "_" + counter;
    }

    /**
     * Get all layer names.
     */
    public java.util.List<String> getLayerNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (LayerState layer : layers) {
            names.add(layer.name);
        }
        return names;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE PARAMETERS (used by ShapeSubPanel) - currently global, future per-primitive
    // ═══════════════════════════════════════════════════════════════════════════

    // Sphere
    private int sphereLatSteps = 32;
    private int sphereLonSteps = 64;
    private float sphereLatStart = 0f;
    private float sphereLatEnd = 1f;
    private String sphereAlgorithm = "LAT_LON";

    public int getSphereLatSteps() { return sphereLatSteps; }
    public void setSphereLatSteps(int v) { sphereLatSteps = v; markDirty(); }
    public int getSphereLonSteps() { return sphereLonSteps; }
    public void setSphereLonSteps(int v) { sphereLonSteps = v; markDirty(); }
    public float getSphereLatStart() { return sphereLatStart; }
    public void setSphereLatStart(float v) { sphereLatStart = v; markDirty(); }
    public float getSphereLatEnd() { return sphereLatEnd; }
    public void setSphereLatEnd(float v) { sphereLatEnd = v; markDirty(); }
    public String getSphereAlgorithm() { return sphereAlgorithm; }
    public void setSphereAlgorithm(String v) { sphereAlgorithm = v; markDirty(); }

    // Ring
    private float ringInnerRadius = 0.5f;
    private float ringOuterRadius = 1.5f;
    private int ringSegments = 64;
    private float ringHeight = 0f;
    private float ringY = 0f;
    private float ringArcStart = 0f;    // degrees 0-360
    private float ringArcEnd = 360f;    // degrees 0-360
    private float ringTwist = 0f;       // degrees -360 to 360

    public float getRingInnerRadius() { return ringInnerRadius; }
    public void setRingInnerRadius(float v) { ringInnerRadius = v; markDirty(); }
    public float getRingOuterRadius() { return ringOuterRadius; }
    public void setRingOuterRadius(float v) { ringOuterRadius = v; markDirty(); }
    public int getRingSegments() { return ringSegments; }
    public void setRingSegments(int v) { ringSegments = v; markDirty(); }
    public float getRingHeight() { return ringHeight; }
    public void setRingHeight(float v) { ringHeight = v; markDirty(); }
    public float getRingY() { return ringY; }
    public void setRingY(float v) { ringY = v; markDirty(); }
    public float getRingArcStart() { return ringArcStart; }
    public void setRingArcStart(float v) { ringArcStart = v; markDirty(); }
    public float getRingArcEnd() { return ringArcEnd; }
    public void setRingArcEnd(float v) { ringArcEnd = v; markDirty(); }
    public float getRingTwist() { return ringTwist; }
    public void setRingTwist(float v) { ringTwist = v; markDirty(); }

    // Disc
    private float discRadius = 1f;
    private int discSegments = 64;
    private float discY = 0f;
    private float discInnerRadius = 0f;
    private float discArcStart = 0f;    // degrees 0-360 (pac-man effect)
    private float discArcEnd = 360f;    // degrees 0-360
    private int discRings = 1;          // concentric ring divisions

    public float getDiscRadius() { return discRadius; }
    public void setDiscRadius(float v) { discRadius = v; markDirty(); }
    public int getDiscSegments() { return discSegments; }
    public void setDiscSegments(int v) { discSegments = v; markDirty(); }
    public float getDiscY() { return discY; }
    public void setDiscY(float v) { discY = v; markDirty(); }
    public float getDiscInnerRadius() { return discInnerRadius; }
    public void setDiscInnerRadius(float v) { discInnerRadius = v; markDirty(); }
    public float getDiscArcStart() { return discArcStart; }
    public void setDiscArcStart(float v) { discArcStart = v; markDirty(); }
    public float getDiscArcEnd() { return discArcEnd; }
    public void setDiscArcEnd(float v) { discArcEnd = v; markDirty(); }
    public int getDiscRings() { return discRings; }
    public void setDiscRings(int v) { discRings = v; markDirty(); }

    // Prism
    private int prismSides = 6;
    private float prismRadius = 1f;
    private float prismHeight = 1f;
    private float prismTopRadius = 1f;
    private float prismTwist = 0f;          // degrees -360 to 360
    private int prismHeightSegments = 1;    // vertical divisions
    private boolean prismCapTop = true;
    private boolean prismCapBottom = true;

    public int getPrismSides() { return prismSides; }
    public void setPrismSides(int v) { prismSides = v; markDirty(); }
    public float getPrismRadius() { return prismRadius; }
    public void setPrismRadius(float v) { prismRadius = v; markDirty(); }
    public float getPrismHeight() { return prismHeight; }
    public void setPrismHeight(float v) { prismHeight = v; markDirty(); }
    public float getPrismTopRadius() { return prismTopRadius; }
    public void setPrismTopRadius(float v) { prismTopRadius = v; markDirty(); }
    public float getPrismTwist() { return prismTwist; }
    public void setPrismTwist(float v) { prismTwist = v; markDirty(); }
    public int getPrismHeightSegments() { return prismHeightSegments; }
    public void setPrismHeightSegments(int v) { prismHeightSegments = v; markDirty(); }
    public boolean isPrismCapTop() { return prismCapTop; }
    public void setPrismCapTop(boolean v) { prismCapTop = v; markDirty(); }
    public boolean isPrismCapBottom() { return prismCapBottom; }
    public void setPrismCapBottom(boolean v) { prismCapBottom = v; markDirty(); }

    // Cylinder
    private float cylinderRadius = 1f;
    private float cylinderHeight = 1f;
    private int cylinderSegments = 32;
    private float cylinderTopRadius = 1f;
    private float cylinderArc = 360f;       // degrees 0-360 (partial cylinder)
    private int cylinderHeightSegments = 1; // vertical divisions
    private boolean cylinderCapTop = true;
    private boolean cylinderCapBottom = false;
    private boolean cylinderOpenEnded = true; // no caps = tube

    public float getCylinderRadius() { return cylinderRadius; }
    public void setCylinderRadius(float v) { cylinderRadius = v; markDirty(); }
    public float getCylinderHeight() { return cylinderHeight; }
    public void setCylinderHeight(float v) { cylinderHeight = v; markDirty(); }
    public int getCylinderSegments() { return cylinderSegments; }
    public void setCylinderSegments(int v) { cylinderSegments = v; markDirty(); }
    public float getCylinderTopRadius() { return cylinderTopRadius; }
    public void setCylinderTopRadius(float v) { cylinderTopRadius = v; markDirty(); }
    public float getCylinderArc() { return cylinderArc; }
    public void setCylinderArc(float v) { cylinderArc = v; markDirty(); }
    public int getCylinderHeightSegments() { return cylinderHeightSegments; }
    public void setCylinderHeightSegments(int v) { cylinderHeightSegments = v; markDirty(); }
    public boolean isCylinderCapTop() { return cylinderCapTop; }
    public void setCylinderCapTop(boolean v) { cylinderCapTop = v; markDirty(); }
    public boolean isCylinderCapBottom() { return cylinderCapBottom; }
    public void setCylinderCapBottom(boolean v) { cylinderCapBottom = v; markDirty(); }
    public boolean isCylinderOpenEnded() { return cylinderOpenEnded; }
    public void setCylinderOpenEnded(boolean v) { cylinderOpenEnded = v; markDirty(); }

    // Polyhedron
    private String polyType = "CUBE";
    private float polyRadius = 1f;
    private int polySubdivisions = 0;

    public String getPolyType() { return polyType; }
    public void setPolyType(String v) { polyType = v; markDirty(); }
    public float getPolyRadius() { return polyRadius; }
    public void setPolyRadius(float v) { polyRadius = v; markDirty(); }
    public int getPolySubdivisions() { return polySubdivisions; }
    public void setPolySubdivisions(int v) { polySubdivisions = v; markDirty(); }

    // ═══════════════════════════════════════════════════════════════════════════
    // BEAM CONFIG (Debug tab)
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean beamEnabled = false;
    private float beamInnerRadius = 0.05f;
    private float beamOuterRadius = 0.1f;
    private String beamColor = "@beam";
    private float beamHeight = 10f;
    private float beamGlow = 0.5f;

    // Beam pulse
    private boolean beamPulseEnabled = false;
    private float beamPulseScale = 0.1f;
    private float beamPulseSpeed = 1.0f;
    private String beamPulseWaveform = "SINE";
    private float beamPulseMin = 0.9f;
    private float beamPulseMax = 1.1f;

    public boolean isBeamEnabled() { return beamEnabled; }
    public void setBeamEnabled(boolean v) { beamEnabled = v; markDirty(); }
    public float getBeamInnerRadius() { return beamInnerRadius; }
    public void setBeamInnerRadius(float v) { beamInnerRadius = v; markDirty(); }
    public float getBeamOuterRadius() { return beamOuterRadius; }
    public void setBeamOuterRadius(float v) { beamOuterRadius = v; markDirty(); }
    public String getBeamColor() { return beamColor; }
    public void setBeamColor(String v) { beamColor = v; markDirty(); }
    public float getBeamHeight() { return beamHeight; }
    public void setBeamHeight(float v) { beamHeight = v; markDirty(); }
    public float getBeamGlow() { return beamGlow; }
    public void setBeamGlow(float v) { beamGlow = v; markDirty(); }

    public boolean isBeamPulseEnabled() { return beamPulseEnabled; }
    public void setBeamPulseEnabled(boolean v) { beamPulseEnabled = v; markDirty(); }
    public float getBeamPulseScale() { return beamPulseScale; }
    public void setBeamPulseScale(float v) { beamPulseScale = v; markDirty(); }
    public float getBeamPulseSpeed() { return beamPulseSpeed; }
    public void setBeamPulseSpeed(float v) { beamPulseSpeed = v; markDirty(); }
    public String getBeamPulseWaveform() { return beamPulseWaveform; }
    public void setBeamPulseWaveform(String v) { beamPulseWaveform = v; markDirty(); }
    public float getBeamPulseMin() { return beamPulseMin; }
    public void setBeamPulseMin(float v) { beamPulseMin = v; markDirty(); }
    public float getBeamPulseMax() { return beamPulseMax; }
    public void setBeamPulseMax(float v) { beamPulseMax = v; markDirty(); }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROPERTY BINDINGS (Debug tab - dynamic property bindings)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Represents a single property binding configuration. */
    public static class BindingEntry {
        public final String property;
        public final String source;
        public final float inputMin, inputMax;
        public final float outputMin, outputMax;
        public final String curve;
        
        public BindingEntry(String property, String source, 
                           float inputMin, float inputMax,
                           float outputMin, float outputMax, String curve) {
            this.property = property;
            this.source = source;
            this.inputMin = inputMin;
            this.inputMax = inputMax;
            this.outputMin = outputMin;
            this.outputMax = outputMax;
            this.curve = curve;
        }
    }
    
    private final java.util.List<BindingEntry> bindings = new java.util.ArrayList<>();
    
    public java.util.List<BindingEntry> getBindings() { 
        return java.util.Collections.unmodifiableList(bindings); 
    }
    
    public void addBinding(String property, String source, 
                          float inputMin, float inputMax,
                          float outputMin, float outputMax, String curve) {
        // Remove existing binding for same property
        bindings.removeIf(b -> b.property.equals(property));
        bindings.add(new BindingEntry(property, source, inputMin, inputMax, outputMin, outputMax, curve));
        markDirty();
    }
    
    public void removeBinding(String property) {
        bindings.removeIf(b -> b.property.equals(property));
        markDirty();
    }
    
    public void clearBindings() {
        bindings.clear();
        markDirty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET NAME GETTERS (for ProfilesPanel category summary)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Returns current shape preset name or "Custom" if no match. */
    public String getCurrentShapeFragmentName() {
        // Simplified: check if default values match
        if ("sphere".equalsIgnoreCase(shapeType) && sphereLatSteps == 32 && sphereLonSteps == 64) {
            return "Default";
        }
        return "Custom";
    }

    /** Returns current fill preset name or "Custom" if no match. */
    public String getCurrentFillFragmentName() {
        if (wireThickness == 1.0f && !doubleSided && depthTest && depthWrite) {
            return "Default";
        } else if (wireThickness < 0.5f) {
            return "Thin Wire";
        } else if (wireThickness > 1.5f) {
            return "Thick Wire";
        }
        return "Custom";
    }

    /** Returns current visibility preset name or "Custom" if no match. */
    public String getCurrentVisibilityFragmentName() {
        if ("FULL".equals(maskType)) return "Full";
        if ("BANDS".equals(maskType)) return "Bands";
        if ("STRIPES".equals(maskType)) return "Stripes";
        if ("CHECKER".equals(maskType)) return "Checker";
        if ("RADIAL".equals(maskType)) return "Radial";
        if ("GRADIENT".equals(maskType)) return "Gradient";
        return "Custom";
    }

    /** Returns current arrangement preset name or "Custom" if no match. */
    public String getCurrentArrangementFragmentName() {
        if ("FILLED".equals(quadPattern) && "FULL".equals(segmentPattern) && "FULL".equals(sectorPattern)) {
            return "Default";
        } else if ("WAVE".equals(quadPattern)) {
            return "Wavey";
        } else if (multiPartArrangement) {
            return "Multi-Part";
        }
        return "Custom";
    }

    /** Returns current animation preset name or "Custom" if no match. */
    public String getCurrentAnimationFragmentName() {
        if (!spinEnabled && !pulseEnabled && !alphaFadeEnabled) {
            return "Static";
        } else if (spinEnabled && spinSpeed <= 60f && !pulseEnabled) {
            return "Spin Slow";
        } else if (spinEnabled && spinSpeed > 100f && !pulseEnabled) {
            return "Spin Fast";
        } else if (!spinEnabled && pulseEnabled) {
            return "Pulse Soft";
        } else if (!spinEnabled && alphaFadeEnabled) {
            return "Alpha Breathe";
        }
        return "Custom";
    }

    /** Returns current beam preset name or "Custom" if no match. */
    public String getCurrentBeamFragmentName() {
        if (!beamEnabled) return "None";
        if (beamInnerRadius <= 0.06f && beamOuterRadius <= 0.12f && !beamPulseEnabled) {
            return "Default";
        } else if (beamInnerRadius < 0.04f) {
            return "Thin";
        } else if (beamInnerRadius > 0.08f) {
            return "Thick";
        } else if (beamPulseEnabled) {
            return "Tall Pulse";
        }
        return "Custom";
    }

    /** Returns current follow preset name or "Custom" if no match. */
    public String getCurrentFollowFragmentName() {
        if (!followEnabled) return "Static";
        return switch (followMode) {
            case SNAP -> "Snap";
            case SMOOTH -> "Smooth";
            case GLIDE -> "Glide";
        };
    }

    /** Returns current prediction preset name or "Custom" if no match. */
    public String getCurrentPredictionPresetName() {
        if (!predictionEnabled) return "OFF";
        if (predictionTicks == 1) return "LOW";
        if (predictionTicks == 2) return "MEDIUM";
        if (predictionTicks >= 3) return "HIGH";
        return "Custom";
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON SERIALIZATION - Complete state snapshot
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Serialize ALL editable state to JSON (for snapshot/revert).
     */
    public String toStateJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        
        // Shape & basic
        json.addProperty("shapeType", shapeType);
        json.addProperty("radius", radius);
        json.addProperty("color", color);
        json.addProperty("alpha", alpha);
        json.addProperty("fillMode", fillMode.name());
        json.addProperty("sphereLatSteps", sphereLatSteps);
        json.addProperty("sphereLonSteps", sphereLonSteps);
        json.addProperty("maskCount", maskCount);
        
        // Follow & prediction
        json.addProperty("followMode", followMode.name());
        json.addProperty("predictionEnabled", predictionEnabled);
        json.addProperty("predictionTicks", predictionTicks);
        
        // Effects
        json.addProperty("glow", glow);
        json.addProperty("emissive", emissive);
        json.addProperty("saturation", saturation);
        json.addProperty("primaryColor", primaryColor);
        json.addProperty("secondaryColor", secondaryColor);
        
        // Spin
        json.addProperty("spinEnabled", spinEnabled);
        json.addProperty("spinAxis", spinAxis);
        json.addProperty("spinSpeed", spinSpeed);
        
        // Pulse
        json.addProperty("pulseEnabled", pulseEnabled);
        json.addProperty("pulseMode", pulseMode);
        json.addProperty("pulseFrequency", pulseFrequency);
        json.addProperty("pulseAmplitude", pulseAmplitude);
        
        // Alpha fade
        json.addProperty("alphaFadeEnabled", alphaFadeEnabled);
        json.addProperty("alphaMin", alphaMin);
        json.addProperty("alphaMax", alphaMax);
        
        // Modifiers
        json.addProperty("modifierBobbing", modifierBobbing);
        json.addProperty("modifierBreathing", modifierBreathing);
        
        // Color cycle
        json.addProperty("colorCycleEnabled", colorCycleEnabled);
        json.addProperty("colorCycleSpeed", colorCycleSpeed);
        json.addProperty("colorCycleBlend", colorCycleBlend);
        
        // Wobble
        json.addProperty("wobbleEnabled", wobbleEnabled);
        json.addProperty("wobbleAmplitude", wobbleAmplitude);
        json.addProperty("wobbleSpeed", wobbleSpeed);
        
        // Wave
        json.addProperty("waveEnabled", waveEnabled);
        json.addProperty("waveAmplitude", waveAmplitude);
        json.addProperty("waveFrequency", waveFrequency);
        json.addProperty("waveDirection", waveDirection);
        
        // Lifecycle
        json.addProperty("lifecycleState", lifecycleState);
        json.addProperty("fadeInTicks", fadeInTicks);
        json.addProperty("fadeOutTicks", fadeOutTicks);
        
        // Trigger
        json.addProperty("triggerType", triggerType);
        json.addProperty("triggerEffect", triggerEffect);
        json.addProperty("triggerIntensity", triggerIntensity);
        json.addProperty("triggerDuration", triggerDuration);
        
        // Transform
        json.addProperty("anchor", anchor);
        json.addProperty("offsetX", offsetX);
        json.addProperty("offsetY", offsetY);
        json.addProperty("offsetZ", offsetZ);
        json.addProperty("rotationX", rotationX);
        json.addProperty("rotationY", rotationY);
        json.addProperty("rotationZ", rotationZ);
        json.addProperty("scale", scale);
        json.addProperty("facing", facing);
        
        return new com.google.gson.Gson().toJson(json);
    }
    
    /**
     * Restore ALL editable state from JSON.
     */
    public void fromStateJson(String jsonStr) {
        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonStr).getAsJsonObject();
        
        // Shape & basic
        if (json.has("shapeType")) shapeType = json.get("shapeType").getAsString();
        if (json.has("radius")) radius = json.get("radius").getAsFloat();
        if (json.has("color")) color = json.get("color").getAsInt();
        if (json.has("alpha")) alpha = json.get("alpha").getAsFloat();
        if (json.has("fillMode")) fillMode = FillMode.valueOf(json.get("fillMode").getAsString());
        if (json.has("sphereLatSteps")) sphereLatSteps = json.get("sphereLatSteps").getAsInt();
        if (json.has("sphereLonSteps")) sphereLonSteps = json.get("sphereLonSteps").getAsInt();
        if (json.has("maskCount")) maskCount = json.get("maskCount").getAsInt();
        
        // Follow & prediction
        if (json.has("followMode")) followMode = FollowMode.valueOf(json.get("followMode").getAsString());
        if (json.has("predictionEnabled")) predictionEnabled = json.get("predictionEnabled").getAsBoolean();
        if (json.has("predictionTicks")) predictionTicks = json.get("predictionTicks").getAsInt();
        
        // Effects
        if (json.has("glow")) glow = json.get("glow").getAsFloat();
        if (json.has("emissive")) emissive = json.get("emissive").getAsFloat();
        if (json.has("saturation")) saturation = json.get("saturation").getAsFloat();
        if (json.has("primaryColor")) primaryColor = json.get("primaryColor").getAsInt();
        if (json.has("secondaryColor")) secondaryColor = json.get("secondaryColor").getAsInt();
        
        // Spin
        if (json.has("spinEnabled")) spinEnabled = json.get("spinEnabled").getAsBoolean();
        if (json.has("spinAxis")) spinAxis = json.get("spinAxis").getAsString();
        if (json.has("spinSpeed")) spinSpeed = json.get("spinSpeed").getAsFloat();
        
        // Pulse
        if (json.has("pulseEnabled")) pulseEnabled = json.get("pulseEnabled").getAsBoolean();
        if (json.has("pulseMode")) pulseMode = json.get("pulseMode").getAsString();
        if (json.has("pulseFrequency")) pulseFrequency = json.get("pulseFrequency").getAsFloat();
        if (json.has("pulseAmplitude")) pulseAmplitude = json.get("pulseAmplitude").getAsFloat();
        
        // Alpha fade
        if (json.has("alphaFadeEnabled")) alphaFadeEnabled = json.get("alphaFadeEnabled").getAsBoolean();
        if (json.has("alphaMin")) alphaMin = json.get("alphaMin").getAsFloat();
        if (json.has("alphaMax")) alphaMax = json.get("alphaMax").getAsFloat();
        
        // Modifiers
        if (json.has("modifierBobbing")) modifierBobbing = json.get("modifierBobbing").getAsFloat();
        if (json.has("modifierBreathing")) modifierBreathing = json.get("modifierBreathing").getAsFloat();
        
        // Color cycle
        if (json.has("colorCycleEnabled")) colorCycleEnabled = json.get("colorCycleEnabled").getAsBoolean();
        if (json.has("colorCycleSpeed")) colorCycleSpeed = json.get("colorCycleSpeed").getAsFloat();
        if (json.has("colorCycleBlend")) colorCycleBlend = json.get("colorCycleBlend").getAsBoolean();
        
        // Wobble
        if (json.has("wobbleEnabled")) wobbleEnabled = json.get("wobbleEnabled").getAsBoolean();
        if (json.has("wobbleAmplitude")) wobbleAmplitude = json.get("wobbleAmplitude").getAsFloat();
        if (json.has("wobbleSpeed")) wobbleSpeed = json.get("wobbleSpeed").getAsFloat();
        
        // Wave
        if (json.has("waveEnabled")) waveEnabled = json.get("waveEnabled").getAsBoolean();
        if (json.has("waveAmplitude")) waveAmplitude = json.get("waveAmplitude").getAsFloat();
        if (json.has("waveFrequency")) waveFrequency = json.get("waveFrequency").getAsFloat();
        if (json.has("waveDirection")) waveDirection = json.get("waveDirection").getAsString();
        
        // Lifecycle
        if (json.has("lifecycleState")) lifecycleState = json.get("lifecycleState").getAsString();
        if (json.has("fadeInTicks")) fadeInTicks = json.get("fadeInTicks").getAsInt();
        if (json.has("fadeOutTicks")) fadeOutTicks = json.get("fadeOutTicks").getAsInt();
        
        // Trigger
        if (json.has("triggerType")) triggerType = json.get("triggerType").getAsString();
        if (json.has("triggerEffect")) triggerEffect = json.get("triggerEffect").getAsString();
        if (json.has("triggerIntensity")) triggerIntensity = json.get("triggerIntensity").getAsFloat();
        if (json.has("triggerDuration")) triggerDuration = json.get("triggerDuration").getAsInt();
        
        // Transform
        if (json.has("anchor")) anchor = json.get("anchor").getAsString();
        if (json.has("offsetX")) offsetX = json.get("offsetX").getAsFloat();
        if (json.has("offsetY")) offsetY = json.get("offsetY").getAsFloat();
        if (json.has("offsetZ")) offsetZ = json.get("offsetZ").getAsFloat();
        if (json.has("rotationX")) rotationX = json.get("rotationX").getAsFloat();
        if (json.has("rotationY")) rotationY = json.get("rotationY").getAsFloat();
        if (json.has("rotationZ")) rotationZ = json.get("rotationZ").getAsFloat();
        if (json.has("scale")) scale = json.get("scale").getAsFloat();
        if (json.has("facing")) facing = json.get("facing").getAsString();
    }
    
    /**
     * Export current state as profile JSON (for saving to file).
     */
    public String toProfileJson(String profileName) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("name", profileName);
        json.addProperty("version", "1.0");
        json.addProperty("description", "Saved from GUI");
        json.addProperty("category", "general");
        
        // Embed full state
        com.google.gson.JsonObject state = com.google.gson.JsonParser.parseString(toStateJson()).getAsJsonObject();
        json.add("state", state);
        
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
    }
    
    /**
     * Load state from profile JSON.
     */
    public void fromProfileJson(String jsonStr) {
        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonStr).getAsJsonObject();
        if (json.has("state")) {
            fromStateJson(json.get("state").toString());
        }
    }

}
