package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * G32: Reusable confirmation dialog overlay.
 */
public class ConfirmDialog extends Screen {
    
    private final Screen parent;
    private final String message;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    
    private ButtonWidget confirmButton;
    private ButtonWidget cancelButton;
    
    public ConfirmDialog(Screen parent, String title, String message, 
                         Runnable onConfirm, Runnable onCancel) {
        super(Text.literal(title));
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel != null ? onCancel : () -> {};
        
        Logging.GUI.topic("dialog").debug("ConfirmDialog created: {}", title);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int centerY = height / 2;
        int buttonWidth = 80;
        int buttonGap = 10;
        
        confirmButton = ButtonWidget.builder(Text.literal("Yes"), btn -> {
            Logging.GUI.topic("dialog").debug("ConfirmDialog confirmed");
            close();
            onConfirm.run();
        }).dimensions(centerX - buttonWidth - buttonGap/2, centerY + 20, buttonWidth, 20).build();
        
        cancelButton = ButtonWidget.builder(Text.literal("No"), btn -> {
            Logging.GUI.topic("dialog").debug("ConfirmDialog cancelled");
            close();
            onCancel.run();
        }).dimensions(centerX + buttonGap/2, centerY + 20, buttonWidth, 20).build();
        
        addDrawableChild(confirmButton);
        addDrawableChild(cancelButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent background
        context.fill(0, 0, width, height, 0xC0000000);
        
        // Dialog box
        int boxWidth = 250;
        int boxHeight = 100;
        int boxX = (width - boxWidth) / 2;
        int boxY = (height - boxHeight) / 2;
        
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, GuiConstants.BG_PANEL);
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, GuiConstants.ACCENT);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, boxY + 10, GuiConstants.TEXT_PRIMARY);
        
        // Message
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(message), width / 2, boxY + 35, GuiConstants.TEXT_SECONDARY);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    /**
     * Shows a confirmation dialog.
     */
    public static void show(Screen parent, String title, String message, 
                           Runnable onConfirm, Runnable onCancel) {
        MinecraftClient.getInstance().setScreen(
            new ConfirmDialog(parent, title, message, onConfirm, onCancel));
    }
    
    /**
     * Shows a confirmation dialog with no cancel action.
     */
    public static void show(Screen parent, String title, String message, Runnable onConfirm) {
        show(parent, title, message, onConfirm, null);
    }
}
