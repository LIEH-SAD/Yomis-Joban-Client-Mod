package com.jsblock.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;

import java.util.function.Consumer;

/**
 * A minimal 20x20 checkbox widget, used by the configuration screen.
 * @since 1.2.12
 */
public class WidgetCheckbox extends AbstractWidget {
	private static final int SIZE = 20;
	private boolean checked;
	private final Consumer<Boolean> onChanged;

	public WidgetCheckbox(int x, int y, boolean checked, Consumer<Boolean> onChanged) {
		super(x, y, SIZE, SIZE, CommonComponents.EMPTY);
		this.checked = checked;
		this.onChanged = onChanged;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		checked = !checked;

		if(onChanged != null) {
			onChanged.accept(checked);
		}
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		final boolean hovered = mouseX >= getX() && mouseX < getX() + SIZE && mouseY >= getY() && mouseY < getY() + SIZE;

		guiGraphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, hovered ? 0xFFFFFFFF : 0xFFA0A0A0);
		guiGraphics.fill(getX() + 1, getY() + 1, getX() + SIZE - 1, getY() + SIZE - 1, 0xFF101010);

		if(checked) {
			guiGraphics.fill(getX() + 4, getY() + 9, getX() + 7, getY() + 14, 0xFF66FF66);
			guiGraphics.fill(getX() + 7, getY() + 5, getX() + 10, getY() + 14, 0xFF66FF66);
			guiGraphics.fill(getX() + 10, getY() + 5, getX() + 16, getY() + 10, 0xFF66FF66);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}
}
