package com.jsblock.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.block.BlockSignalLightBase;
import mtr.block.BlockSignalSemaphoreBase;
import mtr.block.IBlock;
import mtr.data.IGui;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.MoreRenderLayers;
import mtr.render.RenderTrains;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render Signal Base but doesn't need anything for it to be lit, code copied from MTR.
 * @author LX86
 * @since 1.0.0
 * @see mtr.render.RenderSignalBase
 */
public abstract class RenderConstantSignalBase<T extends BlockEntityMapper> extends BlockEntityRendererMapper<T> implements IBlock, IGui {

    protected final boolean isSingleSided;

    public RenderConstantSignalBase(BlockEntityRenderDispatcher dispatcher, boolean isSingleSided) {
        super(dispatcher);
        this.isSingleSided = isSingleSided;
    }

    @Override
    public final void render(T entity, float delta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        final Level world = entity.getLevel();
        if (world == null) {
            return;
        }

        final BlockPos pos = entity.getBlockPos();
        final BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockSignalLightBase || state.getBlock() instanceof BlockSignalSemaphoreBase)) {
            return;
        }
        final Direction facing = IBlock.getStatePropertySafe(state, HorizontalDirectionalBlock.FACING);
        if (RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance, null)) {
            return;
        }
        matrices.pushPose();
        matrices.translate(0.5, 0, 0.5);

        for (int i = 0; i < 2; i++) {
            final Direction newFacing = (i == 1 ? facing.getOpposite() : facing);
            matrices.pushPose();
            UtilitiesClient.rotateYDegrees(matrices, -newFacing.toYRot());
            final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getLight(new ResourceLocation("mtr:textures/block/white.png"), false));
            render(matrices, vertexConsumers, vertexConsumer, entity, delta, newFacing, false, i == 1);
            matrices.popPose();

            if (isSingleSided) {
                break;
            }
        }
        matrices.popPose();
    }

    protected abstract void render(PoseStack matrices, MultiBufferSource vertexConsumers, VertexConsumer vertexConsumer, T entity, float delta, Direction facing, boolean isOccupied, boolean isBackSide);
}
