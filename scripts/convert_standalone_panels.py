#!/usr/bin/env python3
"""
Properly convert standalone panels to extend AbstractPanel.

This script handles:
1. Add extends AbstractPanel
2. Add required imports
3. Add Screen parent parameter to constructor
4. Add super(parent, state) call
5. Remove local widgets field (use inherited)
6. Add proper init() method
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

def convert_beam_sub_panel():
    """Convert BeamSubPanel to AbstractPanel."""
    filepath = PANEL_DIR / "BeamSubPanel.java"
    content = filepath.read_text(encoding='utf-8')
    
    # Already converted?
    if "extends AbstractPanel" in content:
        print("BeamSubPanel: already converted")
        return
    
    # Add imports
    content = content.replace(
        "package net.cyberpunk042.client.gui.panel.sub;",
        """package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;"""
    )
    
    # Remove duplicate/unnecessary imports that we'll handle
    content = re.sub(r'import net\.minecraft\.client\.gui\.widget\.ClickableWidget;\n', '', content)
    
    # Remove local widgets field
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Change class declaration
    content = content.replace(
        "public class BeamSubPanel {",
        "public class BeamSubPanel extends AbstractPanel {"
    )
    
    # Update constructor to take Screen parent and call super
    content = content.replace(
        "public BeamSubPanel(FieldEditState state, int x, int y, int width) {",
        """public BeamSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;"""
    )
    
    # Remove old constructor body that's now in init
    content = re.sub(
        r'this\.state = state;\s*\n\s*this\.section = new ExpandableSection[^}]+build\(\);',
        """this.section = new ExpandableSection(x, y, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2, "Beam (Debug)", true);
        registerSection(section);
        this.layout = new GuiLayout(x + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, section.getContentY(), net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        build();""",
        content, flags=re.DOTALL
    )
    
    # Remove the state field declaration (inherited from AbstractPanel)
    content = re.sub(r'\s*private final FieldEditState state;\n', '\n', content)
    
    filepath.write_text(content, encoding='utf-8')
    print("BeamSubPanel: converted")

def convert_prediction_sub_panel():
    """Convert PredictionSubPanel to AbstractPanel."""
    filepath = PANEL_DIR / "PredictionSubPanel.java"
    content = filepath.read_text(encoding='utf-8')
    
    if "extends AbstractPanel" in content:
        print("PredictionSubPanel: already converted")
        return
    
    # Add imports
    content = content.replace(
        "package net.cyberpunk042.client.gui.panel.sub;",
        """package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;"""
    )
    
    # Remove local widgets field  
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Change class declaration
    content = content.replace(
        "public class PredictionSubPanel {",
        "public class PredictionSubPanel extends AbstractPanel {"
    )
    
    # Fix constructor signature and add super call
    old_constructor = r'public PredictionSubPanel\(FieldEditState state, int x, int y, int width\) \{'
    new_constructor = """public PredictionSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Continue with original build logic..."""
    content = re.sub(old_constructor, new_constructor, content)
    
    # Remove state field
    content = re.sub(r'\s*private final FieldEditState state;\n', '\n', content)
    
    filepath.write_text(content, encoding='utf-8')
    print("PredictionSubPanel: converted")

def convert_follow_mode_sub_panel():
    """Convert FollowModeSubPanel to AbstractPanel."""
    filepath = PANEL_DIR / "FollowModeSubPanel.java"
    content = filepath.read_text(encoding='utf-8')
    
    if "extends AbstractPanel" in content:
        print("FollowModeSubPanel: already converted")
        return
    
    # Add imports
    content = content.replace(
        "package net.cyberpunk042.client.gui.panel.sub;",
        """package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;"""
    )
    
    # Remove local widgets field
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Change class declaration
    content = content.replace(
        "public class FollowModeSubPanel {",
        "public class FollowModeSubPanel extends AbstractPanel {"
    )
    
    # Fix constructor
    old_constructor = r'public FollowModeSubPanel\(FieldEditState state, int x, int y, int width\) \{'
    new_constructor = """public FollowModeSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Original build logic..."""
    content = re.sub(old_constructor, new_constructor, content)
    
    # Remove state field
    content = re.sub(r'\s*private final FieldEditState state;\n', '\n', content)
    
    filepath.write_text(content, encoding='utf-8')
    print("FollowModeSubPanel: converted")

def convert_bindings_sub_panel():
    """Convert BindingsSubPanel to AbstractPanel."""
    filepath = PANEL_DIR / "BindingsSubPanel.java"
    content = filepath.read_text(encoding='utf-8')
    
    if "extends AbstractPanel" in content:
        print("BindingsSubPanel: already converted")
        return
    
    # Add imports
    content = content.replace(
        "package net.cyberpunk042.client.gui.panel.sub;",
        """package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;"""
    )
    
    # Remove local widgets field
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Change class declaration
    content = content.replace(
        "public class BindingsSubPanel {",
        "public class BindingsSubPanel extends AbstractPanel {"
    )
    
    # Fix constructor
    old_constructor = r'public BindingsSubPanel\(FieldEditState state, int x, int y, int width\) \{'
    new_constructor = """public BindingsSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Original build logic..."""
    content = re.sub(old_constructor, new_constructor, content)
    
    # Remove state field
    content = re.sub(r'\s*private final FieldEditState state;\n', '\n', content)
    
    filepath.write_text(content, encoding='utf-8')
    print("BindingsSubPanel: converted")

def convert_linking_sub_panel():
    """Convert LinkingSubPanel to AbstractPanel."""
    filepath = PANEL_DIR / "LinkingSubPanel.java"
    content = filepath.read_text(encoding='utf-8')
    
    if "extends AbstractPanel" in content:
        print("LinkingSubPanel: already converted")
        return
    
    # Add imports
    content = content.replace(
        "package net.cyberpunk042.client.gui.panel.sub;",
        """package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;"""
    )
    
    # Remove local widgets field
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Change class declaration
    content = content.replace(
        "public class LinkingSubPanel {",
        "public class LinkingSubPanel extends AbstractPanel {"
    )
    
    # Fix constructor
    old_constructor = r'public LinkingSubPanel\(FieldEditState state, int x, int y, int width\) \{'
    new_constructor = """public LinkingSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Original build logic..."""
    content = re.sub(old_constructor, new_constructor, content)
    
    # Remove state field
    content = re.sub(r'\s*private final FieldEditState state;\n', '\n', content)
    
    filepath.write_text(content, encoding='utf-8')
    print("LinkingSubPanel: converted")

def main():
    print("Converting standalone panels to extend AbstractPanel...")
    print("=" * 60)
    
    convert_beam_sub_panel()
    convert_prediction_sub_panel()
    convert_follow_mode_sub_panel()
    convert_bindings_sub_panel()
    convert_linking_sub_panel()
    
    print("=" * 60)
    print("Done! Manual review may be needed for constructor bodies.")

if __name__ == "__main__":
    main()

