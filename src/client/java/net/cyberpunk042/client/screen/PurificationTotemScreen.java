package net.cyberpunk042.client.screen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.item.PurificationOption;
import net.cyberpunk042.network.PurificationTotemSelectPayload;
import net.cyberpunk042.screen.handler.PurificationTotemScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class PurificationTotemScreen extends HandledScreen<PurificationTotemScreenHandler> {
	private final List<OptionButton> optionButtons = new ArrayList<>();
	@Nullable
	private Text pendingTooltip;

	public PurificationTotemScreen(PurificationTotemScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 196;
		this.backgroundHeight = 110;
	}

	@Override
	protected void init() {
		super.init();
		clearChildren();
		optionButtons.clear();

		int buttonWidth = 36;
		int spacing = 52;
		int totalWidth = buttonWidth + spacing * (PurificationOption.values().length - 1);
		int startX = x + (backgroundWidth - totalWidth) / 2;
		int yPos = y + backgroundHeight / 2 - buttonWidth / 2;

		for (int i = 0; i < PurificationOption.values().length; i++) {
			PurificationOption option = PurificationOption.values()[i];
			int buttonX = startX + i * spacing;
			OptionButton button = new OptionButton(buttonX, yPos, option);
			optionButtons.add(button);
			addDrawableChild(button);
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = x;
		int top = y;
		int right = left + backgroundWidth;
		int bottom = top + backgroundHeight;
		context.fill(left, top, right, bottom, 0xCC060606);
		context.drawBorder(left, top, backgroundWidth, backgroundHeight, 0xFF6E3FDB);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		pendingTooltip = null;
		super.render(context, mouseX, mouseY, delta);
		drawMouseoverTooltip(context, mouseX, mouseY);
		if (pendingTooltip != null) {
			context.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(textRenderer, title, 8, 6, 0x404040, false);
	}

	private void selectOption(PurificationOption option) {
		ClientPlayNetworking.send(new PurificationTotemSelectPayload(handler.syncId, option));
		optionButtons.forEach(button -> button.active = false);
	}

	private class OptionButton extends PressableWidget {
		private final PurificationOption option;
		private final ItemStack icon;

		OptionButton(int x, int y, PurificationOption option) {
			super(x, y, 36, 36, option.title());
			this.option = option;
			this.icon = option.createIcon();
		}

		@Override
		public void onPress() {
			if (!active) {
				return;
			}
			selectOption(option);
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			boolean hovered = isHovered();
			int borderColor = hovered ? 0xFFE6A90F : 0xFF6E3FDB;
			int fillColor = hovered ? 0x66FFD088 : 0x66202020;
			context.fill(getX(), getY(), getX() + width, getY() + height, fillColor);
			context.drawBorder(getX(), getY(), width, height, borderColor);
			context.drawCenteredTextWithShadow(PurificationTotemScreen.this.textRenderer, option.title(), getX() + width / 2, getY() - 12, 0xFFFFFF);
			context.drawItem(icon, getX() + width / 2 - 8, getY() + height / 2 - 8);
			if (hovered) {
				pendingTooltip = option.description();
			}
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			builder.put(NarrationPart.TITLE, getMessage());
		}
	}
}

