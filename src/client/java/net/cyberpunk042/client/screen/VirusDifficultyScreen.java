package net.cyberpunk042.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.network.VirusDifficultySelectPayload;
import net.cyberpunk042.screen.handler.VirusDifficultyScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class VirusDifficultyScreen extends HandledScreen<VirusDifficultyScreenHandler> {
	private final List<DifficultyButton> buttons = new ArrayList<>();

	private final Text heading = Text.translatable("screen.the-virus-block.difficulty.heading");
	private final Text warningLine1 = Text.translatable("screen.the-virus-block.difficulty.warning.line1");
	private final Text warningLine2 = Text.translatable("screen.the-virus-block.difficulty.warning.line2");

	public VirusDifficultyScreen(VirusDifficultyScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 280;
		this.backgroundHeight = 160;
	}

	@Override
	protected void init() {
		super.init();
		buttons.clear();
		clearChildren();
		int buttonSize = 52;
		int gap = 14;
		int count = VirusDifficulty.values().length;
		int visibleWidth = buttonSize * count + gap * Math.max(0, count - 1);
		int startX = x + (backgroundWidth - visibleWidth) / 2;
		int yPos = y + backgroundHeight / 2 - buttonSize / 2;

		for (int i = 0; i < VirusDifficulty.values().length; i++) {
			VirusDifficulty difficulty = VirusDifficulty.values()[i];
			int buttonX = startX + i * (buttonSize + gap);
			DifficultyButton button = new DifficultyButton(buttonX, yPos, buttonSize, difficulty);
			buttons.add(button);
			addDrawableChild(button);
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xAA000000);
		context.drawBorder(x, y, backgroundWidth, backgroundHeight, 0xFF552266);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		int titleWidth = textRenderer.getWidth(heading);
		context.drawTextWithShadow(textRenderer, heading, (backgroundWidth - titleWidth) / 2, 8, 0xFFFFFF);
		int warningY = backgroundHeight - 32;
		int warningWidth1 = textRenderer.getWidth(warningLine1);
		int warningWidth2 = textRenderer.getWidth(warningLine2);
		context.drawText(textRenderer, warningLine1, (backgroundWidth - warningWidth1) / 2, warningY, 0xFFCC5555, false);
		context.drawText(textRenderer, warningLine2, (backgroundWidth - warningWidth2) / 2, warningY + 12, 0xFFCC5555, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		for (DifficultyButton button : buttons) {
			if (button.isHovered()) {
				context.drawTooltip(textRenderer, List.of(button.difficulty.getDisplayName(), button.difficulty.getDescription()), mouseX, mouseY);
				break;
			}
		}
	}

	private class DifficultyButton extends PressableWidget {
		private final VirusDifficulty difficulty;

		DifficultyButton(int x, int y, int size, VirusDifficulty difficulty) {
			super(x, y, size, size, difficulty.getDisplayName());
			this.difficulty = difficulty;
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			boolean selected = VirusDifficultyClientState.get() == difficulty;
			boolean hovered = isHovered();
			int borderColor = selected ? 0xFF66FFAA : (hovered ? 0xFF8888EE : 0xFF444444);
			int fillColor = hovered ? 0x88333366 : 0x66000000;
			context.fill(getX(), getY(), getX() + width, getY() + height, fillColor);
			context.drawBorder(getX(), getY(), width, height, borderColor);
			int iconSize = 32;
			int iconX = getX() + width / 2 - iconSize / 2;
			int iconY = getY() + height / 2 - iconSize / 2;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, difficulty.getIconTexture(), iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
			context.drawText(VirusDifficultyScreen.this.textRenderer, difficulty.getDisplayName(),
					getX() + width / 2 - VirusDifficultyScreen.this.textRenderer.getWidth(difficulty.getDisplayName()) / 2,
					getY() + height + 4, 0xFFFFFF, false);
		}

		@Override
		public void onPress() {
			ClientPlayNetworking.send(new VirusDifficultySelectPayload(handler.syncId, difficulty));
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			builder.put(NarrationPart.TITLE, difficulty.getDisplayName());
		}
	}
}

