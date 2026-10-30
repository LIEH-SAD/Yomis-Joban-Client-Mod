package com.jsblock.amberui.gui.screen;

import com.jsblock.amberui.gui.GuiHelper;
import com.jsblock.amberui.gui.widget.ListViewWidget;
import net.minecraft.core.BlockPos;

/**
 * Block Config Screen with a GUI list, override {@link BlockConfigListScreen#addConfigEntries(ListViewWidget)} to add items to the list.<br>
 * Ported from JCM 2.3's {@code com.lx862.amberui.gui.screen.BlockConfigListScreen}.
 */
public abstract class BlockConfigListScreen extends BlockConfigScreen implements GuiHelper {
	public BlockConfigListScreen(BlockPos blockPos) {
		super(blockPos);
	}

	@Override
	protected void init2() {
		super.init2();
		final int contentWidth = getContentWidth();
		final int listViewHeight = Math.max(160, (int) ((height - 60) * 0.75));
		final int startX = (width - contentWidth) / 2;
		final int startY = TEXT_PADDING * 5;
		final int bottomEntryHeight = (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2));

		final ListViewWidget listViewWidget = new ListViewWidget();
		listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
		addConfigEntries(listViewWidget);
		addBottomRowEntry(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
		listViewWidget.positionWidgets();
		addRenderableWidget(listViewWidget);
	}

	public abstract void addConfigEntries(ListViewWidget listViewWidget);
}
