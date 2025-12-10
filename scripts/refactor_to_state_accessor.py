#!/usr/bin/env python3
"""
Transform FieldEditState to use @StateField + StateAccessor pattern.
From 1644 lines to ~200 lines!
"""

import sys

TARGET_FILE = "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"

# The new minimal FieldEditState
NEW_CONTENT = r'''package net.cyberpunk042.client.gui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.field.instance.FollowModeConfig;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.SpinConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.animation.ColorCycleConfig;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.OrbitConfig;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.cyberpunk042.field.BeamConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * G02: Manages the shared editing state for both GUI and /field commands.
 * 
 * <p>Uses @StateField annotation + StateAccessor for reflection-based access,
 * eliminating hundreds of lines of repetitive getter/setter boilerplate.</p>
 * 
 * @see StateField
 * @see StateAccessor
 */
public class FieldEditState {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private SphereShape sphere = SphereShape.DEFAULT;
    @StateField private RingShape ring = RingShape.DEFAULT;
    @StateField private DiscShape disc = DiscShape.DEFAULT;
    @StateField private PrismShape prism = PrismShape.builder().build();
    @StateField private CylinderShape cylinder = CylinderShape.builder().build();
    @StateField private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    
    @StateField private String shapeType = "sphere";
    @StateField private float radius = 3.0f;
    
    public Shape currentShape() {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            case "disc" -> disc;
            case "prism" -> prism;
            case "cylinder" -> cylinder;
            case "polyhedron" -> polyhedron;
            default -> sphere;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM & ORBIT
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private Transform transform = Transform.IDENTITY;
    @StateField private OrbitConfig orbit = OrbitConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APPEARANCE & FILL
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private FillConfig fill = FillConfig.SOLID;
    @StateField private VisibilityMask mask = VisibilityMask.FULL;
    @StateField private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    
    @StateField private int color = 0xFF00FFFF;
    @StateField private float alpha = 0.8f;
    @StateField private float glow = 0.5f;
    @StateField private float emissive = 0f;
    @StateField private int primaryColor = 0xFF00FFFF;
    @StateField private int secondaryColor = 0xFFFF00FF;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private SpinConfig spin = SpinConfig.NONE;
    @StateField private PulseConfig pulse = PulseConfig.NONE;
    @StateField private WobbleConfig wobble = WobbleConfig.builder().build();
    @StateField private WaveConfig wave = WaveConfig.builder().build();
    @StateField private ColorCycleConfig colorCycle = ColorCycleConfig.builder().build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW & PREDICTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private FollowModeConfig followConfig = FollowModeConfig.DEFAULT;
    @StateField private PredictionConfig prediction = PredictionConfig.DEFAULT;
    @StateField private FollowMode followMode = FollowMode.SMOOTH;
    @StateField private boolean predictionEnabled = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING & BEAM
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private PrimitiveLink link = null;
    @StateField private BeamConfig beam = BeamConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE STATE (dirty tracking, profiles)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean isDirty = false;
    private String currentProfileName = "default";
    private boolean currentProfileServer = false;
    private String snapshotJson = null;
    
    public FieldEditState() {
        Logging.GUI.topic("state").debug("FieldEditState created");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRTY TRACKING
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isDirty() { return isDirty; }
    public void markDirty() { isDirty = true; }
    public void clearDirty() { isDirty = false; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GENERIC ACCESSORS (via StateAccessor reflection)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Get a state record by type. */
    public <T> T get(Class<T> type) { 
        return StateAccessor.get(this, type); 
    }
    
    /** Get a state record by name. */
    public <T> T get(String name, Class<T> type) { 
        return StateAccessor.get(this, name, type); 
    }
    
    /** Set a state record (auto-detects field by type). */
    public <T> void set(T value) { 
        StateAccessor.set(this, value); 
        markDirty(); 
    }
    
    /** Set a state record by name. */
    public void set(String name, Object value) { 
        StateAccessor.set(this, name, value); 
        markDirty(); 
    }
    
    /** Update a record using a modifier function (for immutable records with toBuilder). */
    public <T> void update(String name, java.util.function.Function<T, T> modifier) {
        StateAccessor.update(this, name, modifier);
        markDirty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED CONVENIENCE ACCESSORS (for common operations)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Shapes
    public SphereShape sphere() { return sphere; }
    public RingShape ring() { return ring; }
    public DiscShape disc() { return disc; }
    public PrismShape prism() { return prism; }
    public CylinderShape cylinder() { return cylinder; }
    public PolyhedronShape polyhedron() { return polyhedron; }
    public String getShapeType() { return shapeType; }
    public float getRadius() { return radius; }
    
    // Transform & Orbit
    public Transform transform() { return transform; }
    public OrbitConfig orbit() { return orbit; }
    
    // Appearance
    public FillConfig fill() { return fill; }
    public VisibilityMask mask() { return mask; }
    public int getColor() { return color; }
    public float getAlpha() { return alpha; }
    public float getGlow() { return glow; }
    
    // Animation
    public SpinConfig spin() { return spin; }
    public PulseConfig pulse() { return pulse; }
    public WobbleConfig wobble() { return wobble; }
    
    // Follow/Prediction
    public FollowMode getFollowMode() { return followMode; }
    public boolean isPredictionEnabled() { return predictionEnabled; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON SERIALIZATION (via StateAccessor)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public JsonObject toJson() {
        return StateAccessor.toJson(this);
    }
    
    public void fromJson(JsonObject json) {
        StateAccessor.fromJson(this, json);
        clearDirty();
    }
    
    public String toStateJson() {
        return toJson().toString();
    }
    
    public void fromStateJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        fromJson(json);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SNAPSHOT (for revert functionality)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void saveSnapshot() {
        snapshotJson = toStateJson();
        Logging.GUI.topic("state").debug("Snapshot saved");
    }
    
    public void restoreSnapshot() {
        if (snapshotJson != null) {
            fromStateJson(snapshotJson);
            Logging.GUI.topic("state").debug("Snapshot restored");
        }
    }
    
    public boolean hasSnapshot() { return snapshotJson != null; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String getCurrentProfileName() { return currentProfileName; }
    public void setCurrentProfileName(String name) { currentProfileName = name; }
    public boolean isCurrentProfileServer() { return currentProfileServer; }
    public void setCurrentProfile(String name, boolean server) {
        currentProfileName = name;
        currentProfileServer = server;
    }
    
    public void fromProfileJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        if (json.has("state")) {
            fromJson(json.getAsJsonObject("state"));
        }
    }
    
    public String toProfileJson(String profileName) {
        JsonObject json = new JsonObject();
        json.addProperty("name", profileName);
        json.addProperty("version", "1.0");
        json.add("state", toJson());
        return json.toString();
    }
}
'''


def main():
    dry_run = '--dry-run' in sys.argv
    
    print("=" * 70)
    print("Transforming FieldEditState to @StateField pattern")
    if dry_run:
        print("🔍 DRY RUN MODE")
    print("=" * 70)
    
    # Read current file
    with open(TARGET_FILE, 'r', encoding='utf-8') as f:
        old_content = f.read()
    
    old_lines = old_content.count('\n')
    new_lines = NEW_CONTENT.count('\n')
    
    print(f"\n📊 Line count: {old_lines} → {new_lines} ({new_lines - old_lines:+d})")
    print(f"   Reduction: {((old_lines - new_lines) / old_lines * 100):.1f}%")
    
    print("\n📦 What's changing:")
    print("   ❌ Removed: ShapeState inner class (372 lines)")
    print("   ❌ Removed: Individual field getters/setters (500+ lines)")
    print("   ❌ Removed: Manual JSON serialization (200 lines)")
    print("   ❌ Removed: Forwarding methods (100+ lines)")
    print("   ❌ Removed: Layer management (can add back if needed)")
    print("   ✅ Added: @StateField annotations on records")
    print("   ✅ Added: Generic accessors via StateAccessor")
    print("   ✅ Added: Typed convenience getters (sphere(), transform(), etc.)")
    
    if dry_run:
        print("\n🔍 DRY RUN - No files modified")
    else:
        # Backup
        with open(TARGET_FILE + '.bak.original', 'w', encoding='utf-8') as f:
            f.write(old_content)
        print(f"\n✅ Backup: {TARGET_FILE}.bak.original")
        
        # Write new file
        with open(TARGET_FILE, 'w', encoding='utf-8') as f:
            f.write(NEW_CONTENT)
        print(f"✅ Written: {TARGET_FILE}")
        print("\n🚀 Run ./gradlew build to verify!")


if __name__ == "__main__":
    main()
