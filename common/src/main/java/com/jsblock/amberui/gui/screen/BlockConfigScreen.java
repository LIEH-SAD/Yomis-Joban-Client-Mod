package com.jsblock.amberui.gui.screen;

import com.jsblock.amberui.gui.GuiHelper;
import mtr.client.ClientData;
import mtr.data.IGui;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * GUI Screen for configuring block settings, you should extend this class for your own block config screen.<br>
 * Ported from JCM 2.3's {@code com.lx862.amberui.gui.screen.BlockConfigScreen}.
 */
public abstract class BlockConfigScreen extends SavableScreen implements GuiHelper {
	protected final BlockPos blockPos;

	public BlockConfigScreen(BlockPos blockPos) {
		super(Component.empty(), false);
		this.blockPos = blockPos;
	}

	protected int getContentWidth() {
		return (int) Math.min((width * 0.75), MAX_CONTENT_WIDTH);
	}

	@Override
	public Component getScreenSubtitle() {
		final Station atStation = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, blockPos);

		if(atStation != null) {
			return Text.translatable("gui.jsblock.block_config.subtitle_with_station",
					blockPos.getX(), blockPos.getY(), blockPos.getZ(),
					IGui.formatStationName(atStation.name)
			);
		} else {
			return Text.translatable("gui.jsblock.block_config.subtitle",
					blockPos.getX(), blockPos.getY(), blockPos.getZ()
			);
		}
	}
}
