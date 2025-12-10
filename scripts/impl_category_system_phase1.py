#!/usr/bin/env python3
"""
Phase 1: Create category system enums and update Profile.
Creates: PresetCategory.java, ProfileCategory.java, ProfileSource.java
Updates: Profile.java
"""

from pathlib import Path

# Paths
MAIN_PACKAGE = Path("src/main/java/net/cyberpunk042")
FIELD_PACKAGE = MAIN_PACKAGE / "field"
CATEGORY_PACKAGE = FIELD_PACKAGE / "category"
PROFILE_PACKAGE = FIELD_PACKAGE / "profile"

# =============================================================================
# PresetCategory.java
# =============================================================================

PRESET_CATEGORY = '''package net.cyberpunk042.field.category;

/**
 * Categories for organizing presets in the GUI.
 * Used for two-tier dropdown: [Category ▼] [Preset ▼]
 */
public enum PresetCategory {
    ADDITIVE("additive", "Additive", "Add elements to your field"),
    STYLE("style", "Style", "Change visual appearance"),
    ANIMATION("animation", "Animation", "Motion and effects"),
    EFFECT("effect", "Effect", "Composite transformations"),
    PERFORMANCE("performance", "Performance", "Adjust detail level");

    private final String id;
    private final String displayName;
    private final String description;

    PresetCategory(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get category from folder name/id.
     */
    public static PresetCategory fromId(String id) {
        for (PresetCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return STYLE; // Default fallback
    }

    /**
     * Get the folder name for this category.
     */
    public String getFolderName() {
        return id;
    }
}
'''

# =============================================================================
# ProfileCategory.java
# =============================================================================

PROFILE_CATEGORY = '''package net.cyberpunk042.field.category;

/**
 * Categories for organizing profiles in the GUI.
 * Used for filtering in Profiles tab.
 */
public enum ProfileCategory {
    COMBAT("combat", "Combat", "Battle-focused configurations"),
    UTILITY("utility", "Utility", "Functional and practical"),
    DECORATIVE("decorative", "Decorative", "Pure visual aesthetics"),
    EXPERIMENTAL("experimental", "Experimental", "Testing and WIP");

    private final String id;
    private final String displayName;
    private final String description;

    ProfileCategory(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get category from JSON value.
     */
    public static ProfileCategory fromId(String id) {
        if (id == null) return UTILITY; // Default
        for (ProfileCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return UTILITY; // Default fallback
    }
}
'''

# =============================================================================
# ProfileSource.java
# =============================================================================

PROFILE_SOURCE = '''package net.cyberpunk042.field.category;

/**
 * Source/origin of a profile.
 * Determines editability and storage location.
 */
public enum ProfileSource {
    BUNDLED("Bundled", false),   // Shipped with mod, read-only
    LOCAL("Local", true),        // User-created, editable
    SERVER("Server", false);     // From server, read-only

    private final String displayName;
    private final boolean editable;

    ProfileSource(String displayName, boolean editable) {
        this.displayName = displayName;
        this.editable = editable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return editable;
    }

    /**
     * Get icon for UI display.
     * ✎ = editable, 🔒 = read-only
     */
    public String getIcon() {
        return editable ? "✎" : "🔒";
    }
}
'''

# =============================================================================
# Profile.java (NEW - complete rewrite with categories)
# =============================================================================

PROFILE = '''package net.cyberpunk042.field.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved field profile with metadata.
 * Profiles can come from bundled assets, local files, or server.
 */
public record Profile(
    int version,
    String id,
    String name,
    String description,
    FieldType type,
    ProfileCategory category,
    List<String> tags,
    ProfileSource source,
    Instant created,
    Instant modified,
    FieldDefinition definition
) {
    
    public static final int CURRENT_VERSION = 1;
    
    /**
     * Get display name with category: "Profile Name (category)"
     */
    public String getDisplayName() {
        return name + " (" + category.getId() + ")";
    }
    
    /**
     * Check if this profile can be edited/saved.
     */
    public boolean isEditable() {
        return source.isEditable();
    }
    
    /**
     * Get UI icon based on source.
     */
    public String getSourceIcon() {
        return source.getIcon();
    }
    
    /**
     * Check if profile has a specific tag.
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.stream()
            .anyMatch(t -> t.equalsIgnoreCase(tag));
    }
    
    /**
     * Create from JSON.
     */
    public static Profile fromJson(JsonObject json, ProfileSource source) {
        int version = json.has("version") ? json.get("version").getAsInt() : 1;
        String id = json.has("id") ? json.get("id").getAsString() : "unknown";
        String name = json.has("name") ? json.get("name").getAsString() : id;
        String description = json.has("description") ? json.get("description").getAsString() : "";
        
        // Type (functional)
        FieldType type = FieldType.SHIELD;
        if (json.has("type")) {
            try {
                type = FieldType.valueOf(json.get("type").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Category (organizational)
        ProfileCategory category = ProfileCategory.UTILITY;
        if (json.has("category")) {
            category = ProfileCategory.fromId(json.get("category").getAsString());
        }
        
        // Tags
        List<String> tags = new ArrayList<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            for (var elem : json.getAsJsonArray("tags")) {
                tags.add(elem.getAsString());
            }
        }
        
        // Timestamps
        Instant created = Instant.now();
        Instant modified = Instant.now();
        if (json.has("created")) {
            try {
                created = Instant.parse(json.get("created").getAsString());
            } catch (Exception ignored) {}
        }
        if (json.has("modified")) {
            try {
                modified = Instant.parse(json.get("modified").getAsString());
            } catch (Exception ignored) {}
        }
        
        // Definition - the actual field data
        FieldDefinition definition = null;
        if (json.has("definition")) {
            definition = FieldDefinition.fromJson(json.getAsJsonObject("definition"));
        }
        
        return new Profile(version, id, name, description, type, category, tags, source, created, modified, definition);
    }
    
    /**
     * Convert to JSON for saving.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("version", version);
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("type", type.name());
        json.addProperty("category", category.getId());
        
        JsonArray tagsArray = new JsonArray();
        if (tags != null) {
            tags.forEach(tagsArray::add);
        }
        json.add("tags", tagsArray);
        
        json.addProperty("created", created.toString());
        json.addProperty("modified", Instant.now().toString());
        
        if (definition != null) {
            json.add("definition", definition.toJson());
        }
        
        return json;
    }
    
    /**
     * Create a copy with updated definition and modified timestamp.
     */
    public Profile withDefinition(FieldDefinition newDefinition) {
        return new Profile(
            version, id, name, description, type, category, tags, source,
            created, Instant.now(), newDefinition
        );
    }
    
    /**
     * Create a copy with new name (for Save As).
     */
    public Profile withName(String newName, String newId) {
        return new Profile(
            version, newId, newName, description, type, category, tags,
            ProfileSource.LOCAL, // Save As always creates local
            Instant.now(), Instant.now(), definition
        );
    }
    
    /**
     * Create a copy with different category.
     */
    public Profile withCategory(ProfileCategory newCategory) {
        return new Profile(
            version, id, name, description, type, newCategory, tags, source,
            created, Instant.now(), definition
        );
    }
    
    /**
     * Builder for creating new profiles.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id = "new_profile";
        private String name = "New Profile";
        private String description = "";
        private FieldType type = FieldType.SHIELD;
        private ProfileCategory category = ProfileCategory.UTILITY;
        private List<String> tags = new ArrayList<>();
        private FieldDefinition definition;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(FieldType type) { this.type = type; return this; }
        public Builder category(ProfileCategory category) { this.category = category; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder definition(FieldDefinition definition) { this.definition = definition; return this; }
        
        public Profile build() {
            return new Profile(
                CURRENT_VERSION, id, name, description, type, category, tags,
                ProfileSource.LOCAL, Instant.now(), Instant.now(), definition
            );
        }
    }
}
'''

# =============================================================================
# Main
# =============================================================================

def main():
    print("=" * 60)
    print("PHASE 1: Category System - Enums & Profile")
    print("=" * 60)
    
    # Create category package
    CATEGORY_PACKAGE.mkdir(parents=True, exist_ok=True)
    
    # Create profile package (may already exist)
    PROFILE_PACKAGE.mkdir(parents=True, exist_ok=True)
    
    # Write PresetCategory.java
    preset_cat_file = CATEGORY_PACKAGE / "PresetCategory.java"
    preset_cat_file.write_text(PRESET_CATEGORY, encoding='utf-8')
    print(f"✅ Created: {preset_cat_file}")
    
    # Write ProfileCategory.java
    profile_cat_file = CATEGORY_PACKAGE / "ProfileCategory.java"
    profile_cat_file.write_text(PROFILE_CATEGORY, encoding='utf-8')
    print(f"✅ Created: {profile_cat_file}")
    
    # Write ProfileSource.java
    source_file = CATEGORY_PACKAGE / "ProfileSource.java"
    source_file.write_text(PROFILE_SOURCE, encoding='utf-8')
    print(f"✅ Created: {source_file}")
    
    # Write Profile.java
    profile_file = PROFILE_PACKAGE / "Profile.java"
    profile_file.write_text(PROFILE, encoding='utf-8')
    print(f"✅ Created: {profile_file}")
    
    print()
    print("=" * 60)
    print("Phase 1 Complete!")
    print("=" * 60)
    print()
    print("Created files:")
    print(f"  - {CATEGORY_PACKAGE}/PresetCategory.java")
    print(f"  - {CATEGORY_PACKAGE}/ProfileCategory.java")
    print(f"  - {CATEGORY_PACKAGE}/ProfileSource.java")
    print(f"  - {PROFILE_PACKAGE}/Profile.java")
    print()
    print("Next: Run Phase 2 script for registry updates")


if __name__ == "__main__":
    main()

