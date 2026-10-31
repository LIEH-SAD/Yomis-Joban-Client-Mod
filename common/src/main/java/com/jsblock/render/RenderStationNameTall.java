package com.jsblock.render;

import com.jsblock.block.StationNameStanding;
import mtr.block.BlockStationNameTallBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderStationNameBase;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render Station Name Tall
 * @author LX86
 * @see StationNameStanding
 */
public class RenderStationNameTall<T extends BlockStationNameTallBase.TileEntityStationNameTallBase> extends RenderStationNameBase<T> {

    private static final float WIDTH = 0.6875F;
    private static final float HEIGHT = 1;
    private static final float OFFSET_Y = 0.125F;

    public RenderStationNameTall(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void drawStationName(BlockGetter world, BlockPos pos, BlockState state, Direction facing, StoredMatrixTransformations storedMatrixTransformations, MultiBufferSource vertexConsumers, String stationName, int stationColor, int color, int light) {
        if (IBlock.getStatePropertySafe(state, BlockStationNameTallBase.THIRD) == IBlock.EnumThird.MIDDLE) {
            RenderTrains.scheduleRender(ClientData.DATA_CACHE.getTallStationName(color, stationName, stationColor, WIDTH / HEIGHT).resourceLocation, false, MoreRenderLayers::getExterior, (matrices, vertexConsumer) -> {
                storedMatrixTransformations.transform(matrices);
                IDrawing.drawTexture(matrices, vertexConsumer,  -WIDTH / 2, -HEIGHT / 2 - OFFSET_Y, WIDTH, HEIGHT, 0, 0, 1, 1, facing, ARGB_WHITE, light);
                matrices.popPose();
            });
        }
    }
}