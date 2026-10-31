package com.jsblock.render;

import com.jsblock.block.FareSaver1;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.IBlock;
import mtr.client.Config;
import mtr.data.IGui;
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

/**
 * Render Fare saver's discount value
 * @author LX86
 * @since 1.1.4
 * @see FareSaver1
 */
public class RenderFaresaver1<T extends BlockEntityMapper> extends BlockEntityRendererMapper<T> implements IGui {

    public RenderFaresaver1(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(T entity, float delta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        final Level world = entity.getLevel();
        final BlockPos pos = entity.getBlockPos();
        if (world == null) {
            return;
        }

        /* This defines the font style. If MTR Font is enabled, use the font defined in Config. Otherwise, don't add any style */
        final Style style = Config.useMTRFont() ? Style.EMPTY.withFont(new ResourceLocation("mtr:mtr")) : Style.EMPTY;
        final Direction facing = IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);
        final IBlock.EnumThird third = IBlock.getStatePropertySafe(world, pos, IBlock.THIRD);
        final int discountDollar = ((FareSaver1.TileEntityFareSaver)entity).getDiscount();
        final Font textRenderer = Minecraft.getInstance().font;
        final float currentWidth = textRenderer.width(Text.translatable("gui.jsblock.faresaver.currency", discountDollar));
        final float maxWidth = textRenderer.width(Text.translatable("gui.jsblock.faresaver.currency", 2));

        if(third != IBlock.EnumThird.UPPER) {
            return;
        }

        matrices.pushPose();
        UtilitiesClient.rotateZDegrees(matrices, 180);
        UtilitiesClient.rotateYDegrees(matrices, facing.toYRot());

        if (facing == Direction.SOUTH) {
            matrices.translate(-0.465, -0.363, 0.43);
        }

        if (facing == Direction.NORTH) {
            matrices.translate(0.535, -0.363, -0.57);
        }

        if (facing == Direction.EAST) {
            matrices.translate(0.535, -0.363, 0.43);
        }

        if (facing == Direction.WEST) {
            matrices.translate(-0.465, -0.363, -0.57);
        }

        matrices.scale(0.012F, 0.012F, 0.012F);
        if(currentWidth > maxWidth) {
            matrices.scale(maxWidth / currentWidth, maxWidth / currentWidth, maxWidth / currentWidth);
            matrices.translate(0, (currentWidth / maxWidth), 0);
        }

        final Component formattedText = Text.translatable("gui.jsblock.faresaver.currency", discountDollar).setStyle(style);
        textRenderer.drawInBatch(formattedText, 0, 0, ARGB_WHITE, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 15728880);
        matrices.popPose();
    }
}
