package com.jsblock.render;

import com.jsblock.block.DepartureTimer;
import com.jsblock.block.FontBase;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.Config;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.ScheduleEntry;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class for rendering departure timer's text
 * @author LX86
 * @see DepartureTimer
 */
public class RenderDepartureTimer<T extends BlockEntityMapper> extends BlockEntityRendererMapper<T> {

    public RenderDepartureTimer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(T entity, float delta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        final Level world = entity.getLevel();
        final BlockPos pos = entity.getBlockPos();
        final String fontName = ((FontBase.TileEntityBlockFontBase)entity).getFont();
        if (world == null || !(entity instanceof DepartureTimer.TileEntityDepartureTimer)) {
            return;
        }

        final long platformId = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos, 5, 3, 3);
        if (platformId == 0) {
            return;
        }

        final Set<ScheduleEntry> schedules = ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
        if (schedules == null) {
            return;
        }

        String timeRemaining = "";

        final List<ScheduleEntry> scheduleList = new ArrayList<>(schedules);
        if (!scheduleList.isEmpty()) {
            Collections.sort(scheduleList);
            if (scheduleList.get(0).arrivalMillis - System.currentTimeMillis() > 0) {
                return;
            }

            /* remainingSecond = Train ETA */
            int remainingSecond = (int) (scheduleList.get(0).arrivalMillis - System.currentTimeMillis()) / 1000;
            /* seconds = dwell - ETA */
            final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
            int seconds = platform == null ? 0 : Math.abs((platform.getDwellTime() / 2) - Math.abs(remainingSecond));
            int minutes = seconds / 60;
            timeRemaining = String.format("%d:%02d", minutes % 60, seconds % 60);
        }

        /* This defines the font style. If MTR Font is enabled, use the font stored in the block entity. Otherwise, don't add any style */
        final Style style = Config.useMTRFont() ? Style.EMPTY.withFont(new ResourceLocation(fontName)) : Style.EMPTY;
        final Direction facing = IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);

        matrices.pushPose();
        if (facing == Direction.SOUTH) {
            matrices.translate(0.73, 0.52, 0.43);
        }

        if (facing == Direction.NORTH) {
            matrices.translate(0.28, 0.52, 0.57);
        }

        if (facing == Direction.EAST) {
            matrices.translate(0.43, 0.52, 0.28);
        }

        if (facing == Direction.WEST) {
            matrices.translate(0.57, 0.52, 0.73);
        }

        UtilitiesClient.rotateZDegrees(matrices, 180);
        UtilitiesClient.rotateYDegrees(matrices, facing.toYRot());
        matrices.scale(0.018F, 0.018F, 0.018F);

        final Font textRenderer = Minecraft.getInstance().font;
        final Component formattedText = Text.literal(timeRemaining).setStyle(style);
        textRenderer.drawInBatch(formattedText, 0, 0, 0xEE2233, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 15728880);
        matrices.popPose();
    }
}
