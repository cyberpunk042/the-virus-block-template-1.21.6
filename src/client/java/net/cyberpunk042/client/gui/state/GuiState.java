package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.field.instance.FollowMode;

/**
 * G02: Manages the state of the Field Customizer GUI.
 */
public class GuiState {
    
    private FieldDefinition originalDefinition;
    private FieldDefinition workingDefinition;
    private boolean isDirty = false;
    private boolean debugUnlocked = false;
    private String currentProfileName = "default";
    
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
    
    public GuiState() {
        Logging.GUI.topic("state").debug("GuiState created");
    }
    
    public GuiState(FieldDefinition definition) {
        this.originalDefinition = definition;
        this.workingDefinition = definition;
        Logging.GUI.topic("state").debug("GuiState created with definition");
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
    
    public void setDebugUnlocked(boolean unlocked) { this.debugUnlocked = unlocked; }
    public void setCurrentProfileName(String name) { this.currentProfileName = name; }
    
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
    // LAYER MANAGEMENT (G51-G56)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int selectedLayerIndex = 0;
    private java.util.List<LayerState> layers = new java.util.ArrayList<>();
    
    /** Simple layer state holder. */
    public static class LayerState {
        public boolean visible = true;
        public String name = "Layer";
        public LayerState(String name) { this.name = name; }
    }
    
    {
        // Initialize with one default layer
        layers.add(new LayerState("Layer 1"));
    }
    
    public int getSelectedLayerIndex() { return selectedLayerIndex; }
    public void setSelectedLayerIndex(int index) { 
        this.selectedLayerIndex = Math.max(0, Math.min(index, layers.size() - 1)); 
    }
    
    public int getLayerCount() { return layers.size(); }
    
    public int addLayer() {
        if (layers.size() >= 10) return -1; // Max 10 layers
        layers.add(new LayerState("Layer " + (layers.size() + 1)));
        markDirty();
        return layers.size() - 1;
    }
    
    public boolean removeLayer(int index) {
        if (layers.size() <= 1 || index < 0 || index >= layers.size()) return false;
        layers.remove(index);
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
        markDirty();
        return true;
    }

}
