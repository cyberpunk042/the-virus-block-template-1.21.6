package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory for creating modal dialogs used in the Field Customizer.
 * Centralizes modal creation logic to reduce FieldCustomizerScreen complexity.
 */
public final class ModalFactory {
    
    private ModalFactory() {} // Static factory only
    
    /**
     * Creates a modal for renaming/deleting a layer.
     */
    public static ModalDialog createLayerModal(
            FieldEditState state,
            String layerName,
            int layerIndex,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        int count = state.getFieldLayers().size();
        return createRenameModal(
            "Rename Layer", layerName, count > 1,
            textRenderer, screenWidth, screenHeight,
            onRename, onDelete, onClose
        );
    }
    
    /**
     * Creates a modal for renaming/deleting a primitive.
     */
    public static ModalDialog createPrimitiveModal(
            FieldEditState state,
            String primName,
            int layerIndex, int primIndex,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        int count = state.getPrimitivesForLayer(layerIndex).size();
        return createRenameModal(
            "Rename Primitive", primName, count > 1,
            textRenderer, screenWidth, screenHeight,
            onRename, onDelete, onClose
        );
    }
    
    /**
     * Creates a generic rename/delete modal dialog.
     */
    public static ModalDialog createRenameModal(
            String title,
            String currentName,
            boolean canDelete,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        final TextFieldWidget[] fieldHolder = new TextFieldWidget[1];
        
        ModalDialog modal = new ModalDialog(title, textRenderer, screenWidth, screenHeight)
            .size(300, canDelete ? 150 : 130)
            .content((bounds, tr) -> {
                List<ClickableWidget> widgets = new ArrayList<>();
                TextFieldWidget field = new TextFieldWidget(tr, 
                    bounds.x(), bounds.y() + 10, bounds.width(), 20, Text.literal(""));
                field.setText(currentName);
                field.setMaxLength(64);
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                fieldHolder[0] = field;
                widgets.add(field);
                return widgets;
            })
            .addAction("Cancel", () -> {});
        
        if (canDelete) {
            modal.addAction("§cDelete", onDelete);
        }
        
        modal.addAction("OK", () -> {
            if (fieldHolder[0] != null) onRename.accept(fieldHolder[0].getText());
        }, true);
        
        modal.onClose(onClose);
        
        return modal;
    }
    
    /**
     * Focuses the text field within a modal and selects all text.
     * Call this after showing the modal and registering widgets.
     * 
     * @param modal The modal dialog
     * @param screen The screen to set focus on
     */
    public static void focusTextField(ModalDialog modal, net.minecraft.client.gui.screen.Screen screen) {
        if (modal == null) return;
        for (ClickableWidget w : modal.getWidgets()) {
            if (w instanceof TextFieldWidget tf) {
                tf.visible = true;
                tf.active = true;
                tf.setEditable(true);
                tf.setFocused(true);
                screen.setFocused(tf);
                tf.setCursorToStart(false);
                tf.setCursorToEnd(true);
                break;
            }
        }
    }
}
