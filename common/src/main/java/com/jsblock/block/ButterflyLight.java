package com.jsblock.block;

import com.jsblock.BlockEntityTypes;
import com.jsblock.packet.PacketServer;
import mtr.MTR;
import mtr.block.IBlock;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.ScheduleEntry;
import mtr.mappings.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Butterfly Light
 * @author LX86
 * @since 1.0.0
 */
public class ButterflyLight extends BlockDirectionalMapper implements EntityBlockMapper {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public ButterflyLight(Properties blockProperties) {
        super(blockProperties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(LIT, false);
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity entity = world.getBlockEntity(pos);

            if(!(entity instanceof ButterflyLight.TileEntityButterFlyLight)) {
                return InteractionResult.FAIL;
            }

            IBlock.checkHoldingBrush(world, player, () -> {
                /* Have Brush */
                PacketServer.sendButterflyConfigScreenS2C((ServerPlayer) player, pos, ((ButterflyLight.TileEntityButterFlyLight) entity).getSecondsToBlink());
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 5.8, 10, facing);
    }

    @Override
    public BlockEntityType<? extends BlockEntityMapper> getType() {
        return BlockEntityTypes.BUTTERFLY_LIGHT_TILE_ENTITY.get();
    }

    @Override
    public <T extends BlockEntityMapper> void tick(Level world, BlockPos pos, T blockEntity) {
        ((TileEntityButterFlyLight)blockEntity).tick(world, pos);
    }

    /* Set the blockstates properties of this block */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /* Link the block to the Block Entity class below */
    @Override
    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityButterFlyLight(pos, state);
    }

    public static class TileEntityButterFlyLight extends BlockEntityClientSerializableMapper implements TickableMapper {
        private int secondsToBlink = 10;
        private static final String KEY_SECONDS_TO_BLINK = "seconds_to_blink";

        public TileEntityButterFlyLight(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.BUTTERFLY_LIGHT_TILE_ENTITY.get(), pos, state);
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            this.secondsToBlink = compoundTag.getInt(KEY_SECONDS_TO_BLINK);
            super.readCompoundTag(compoundTag);
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            compoundTag.putInt(KEY_SECONDS_TO_BLINK, this.secondsToBlink);
            super.writeCompoundTag(compoundTag);
        }

        public void setData(int secondsToBlink) {
            this.secondsToBlink = secondsToBlink;
            this.setChanged();
            this.syncData();
        }

        public int getSecondsToBlink() {
            return this.secondsToBlink;
        }

        @Override
        public void tick() {
            /* Passed to the tick method below */
            tick(level, worldPosition);
        }

        public <T extends BlockEntityMapper> void tick(Level world, BlockPos pos) {
            if (MTR.isGameTickInterval(20) && world != null && !world.isClientSide) {
                final BlockState state = world.getBlockState(pos);
                final RailwayData railwayData = RailwayData.getInstance(world);
                final boolean lastBlockLit = IBlock.getStatePropertySafe(state, LIT);
                if (railwayData != null) {
                    /* Get the closest platform */
                    final long platformId = RailwayData.getClosePlatformId(railwayData.platforms, railwayData.dataCache, pos, 5, 3, 3);
                    if (platformId == 0) {
                        if(lastBlockLit) world.setBlockAndUpdate(pos, state.setValue(LIT, false));
                        return;
                    }

                    /* Get the arrivals on that platform */
                    final List<ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platformId);
                    if (schedules == null || schedules.isEmpty()) {
                        if(lastBlockLit) world.setBlockAndUpdate(pos, state.setValue(LIT, false));
                        return;
                    }

                    final List<ScheduleEntry> scheduleList = new ArrayList<>(schedules);
                    Collections.sort(scheduleList);

                    /* If the train has not yet arrived (Should be negative when train arrived) */
                    if (scheduleList.get(0).arrivalMillis - System.currentTimeMillis() > 0) {
                        if(lastBlockLit) world.setBlockAndUpdate(pos, state.setValue(LIT, false));
                        return;
                    }

                    int remainingSecond = (int) (scheduleList.get(0).arrivalMillis - System.currentTimeMillis()) / 1000;
                    final Platform platform = railwayData.dataCache.platformIdMap.get(platformId);
                    /* platform.getDwellTime() returns the dwell second x2, so we have to divide it by 2 to get the second */
                    int seconds = platform == null ? 0 : (platform.getDwellTime() / 2) - Math.abs(remainingSecond);
                    /* Start blinking if the departure time is less than configured second */
                    if (!lastBlockLit && seconds < secondsToBlink) {
                        world.setBlockAndUpdate(pos, state.setValue(LIT, true));
                    }
                }
            }
        }
    }
}