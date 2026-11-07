package com.jsblock.block;

import com.jsblock.BlockEntityTypes;
import com.jsblock.packet.PacketServer;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * PIDS Projector — a single invisible block that projects a PIDS panel into the air.
 *
 * <p>Ported from JCM 2.x's {@code PIDSProjectorBlock} / {@code PIDSProjectorBlockEntity}. What it
 * draws is an ordinary preset, so every scripted preset this mod supports works on it; what makes
 * it different is that the panel is not attached to a block shape. The block itself has no model
 * at all, and the projector carries an offset, a rotation and a scale that place its panel
 * anywhere nearby — which is what the guide lines drawn while holding a brush are for.</p>
 *
 * <p>It is deliberately not a two-block PIDS: {@link BlockPIDSBaseHorizontal} places a companion
 * half behind every panel and refuses to place at all without room for two, whereas a projector
 * is one block standing on its own.</p>
 */
public class PIDSProjector extends JobanPIDSBase {

    /** The panel is drawn 1.785 blocks wide rather than the 1.417 a normal RV panel occupies. */
    public static final float PROJECTOR_PANEL_SCALE = 1.26315F;

    /**
     * Invisible, and it has to say so.
     *
     * <p>{@code noOcclusion} is not decoration here: the projector's model is empty, and without
     * it the game treats the block as a solid opaque cube and culls the face of every block placed
     * against it, so a projector buried in a wall opens a window onto whatever is behind. It also
     * must not suffocate or block sight, for the same reason.</p>
     */
    public PIDSProjector() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops()
                .strength(2)
                .lightLevel(state -> 5)
                .noOcclusion()
                .isViewBlocking((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false));
    }

    @Override
    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlockPIDSProjector(pos, state);
    }

    /**
     * One block, placed at a fixed facing.
     *
     * <p>The panel's orientation does not come from the block at all — the projector transform is
     * a constant {@code rotateY(90)}, and the aiming is done with the projector's own rotation
     * settings — so the facing only has one job here, and it is not a cosmetic one: the render
     * path decides which half of a PIDS is the key half from {@code FACING == NORTH || FACING ==
     * EAST}, and a single-block PIDS placed facing south would otherwise be told it is the non-key
     * half. Pinning it to north keeps the projector its own key block whatever way the player was
     * looking.</p>
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, Direction.NORTH);
    }

    /** One block, so no companion to place and no neighbour state to copy. */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
    }

    /** One block, so a neighbour changing must not remove this one. */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        return state;
    }

    /**
     * Opens the projector's own screen: its placement on top of everything an ordinary PIDS offers.
     *
     * <p>Not the ordinary PIDS screen, which is built around a two-block panel and has no room for
     * an offset and a rotation. The projector screen shares that screen's entries -- the preset, the
     * platform filter, one custom message and one hidden row per arrival, the hide-platform-number
     * option -- and adds its own placement entries above them.</p>
     */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TileEntityBlockPIDSProjector) {
                final TileEntityBlockPIDSProjector projector = (TileEntityBlockPIDSProjector) entity;
                projector.syncData();
                final String[] messages = new String[projector.getMaxArrivals()];
                final boolean[] rowHidden = new boolean[projector.getMaxArrivals()];
                for (int i = 0; i < messages.length; i++) {
                    messages[i] = projector.getMessage(i);
                    rowHidden[i] = projector.getHideArrival(i);
                }
                PacketServer.sendPIDSProjectorScreenS2C((ServerPlayer) player, pos, projector.getPresetID(),
                        messages, rowHidden, projector.getHidePlatformNumber(), projector.getPlatformIds(),
                        projector.getOffsetX(), projector.getOffsetY(), projector.getOffsetZ(),
                        projector.getRotateX(), projector.getRotateY(), projector.getRotateZ(),
                        projector.getScale());
            }
        });
    }

    /** Full block to aim at and click, nothing to collide with: the projector is invisible. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Empty, as JCM 2.x has it: a projector is a ghost you can walk through. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** A single-block PIDS that draws a floating panel is always its own key block. */
    public static class TileEntityBlockPIDSProjector extends JobanPIDSBase.TileEntityBlockJobanPIDS {

        public static final int MAX_ARRIVALS = 4;

        /** Where the panel sits relative to the block, in blocks. */
        private double offsetX = 0;
        private double offsetY = 0;
        private double offsetZ = 0;
        /** Its rotation, in degrees about each axis. */
        private double rotateX = 0;
        private double rotateY = 0;
        private double rotateZ = 0;
        /** And its size multiplier. */
        private double scale = 1;
        /** Whether the panel hides its platform numbers. */
        private boolean hidePlatformNumber = false;

        private static final String KEY_OFFSET_X = "offset_x";
        private static final String KEY_OFFSET_Y = "offset_y";
        private static final String KEY_OFFSET_Z = "offset_z";
        private static final String KEY_ROTATE_X = "rotate_x";
        private static final String KEY_ROTATE_Y = "rotate_y";
        private static final String KEY_ROTATE_Z = "rotate_z";
        private static final String KEY_SCALE = "scale";
        private static final String KEY_HIDE_PLATFORM_NUMBER = "hide_platform_number";

        /* JCM 2.x's own keys, read as a fallback so a projector built with it keeps its placement */
        private static final String KEY_JCM_OFFSET_X = "x1";
        private static final String KEY_JCM_OFFSET_Y = "y1";
        private static final String KEY_JCM_OFFSET_Z = "z1";
        private static final String KEY_JCM_ROTATE_X = "rotateX";
        private static final String KEY_JCM_ROTATE_Y = "rotateY";
        private static final String KEY_JCM_ROTATE_Z = "rotateZ";

        public TileEntityBlockPIDSProjector(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.PIDS_PROJECTOR_TILE_ENTITY.get(), pos, state);
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            this.offsetX = readDouble(compoundTag, KEY_OFFSET_X, KEY_JCM_OFFSET_X);
            this.offsetY = readDouble(compoundTag, KEY_OFFSET_Y, KEY_JCM_OFFSET_Y);
            this.offsetZ = readDouble(compoundTag, KEY_OFFSET_Z, KEY_JCM_OFFSET_Z);
            this.rotateX = readDouble(compoundTag, KEY_ROTATE_X, KEY_JCM_ROTATE_X);
            this.rotateY = readDouble(compoundTag, KEY_ROTATE_Y, KEY_JCM_ROTATE_Y);
            this.rotateZ = readDouble(compoundTag, KEY_ROTATE_Z, KEY_JCM_ROTATE_Z);
            this.scale = compoundTag.contains(KEY_SCALE) ? compoundTag.getDouble(KEY_SCALE) : 1;
            if (this.scale <= 0) {
                this.scale = 1;
            }
            this.hidePlatformNumber = compoundTag.getBoolean(KEY_HIDE_PLATFORM_NUMBER);
        }

        private static double readDouble(CompoundTag compoundTag, String key, String fallbackKey) {
            return compoundTag.contains(key) ? compoundTag.getDouble(key) : compoundTag.getDouble(fallbackKey);
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putDouble(KEY_OFFSET_X, this.offsetX);
            compoundTag.putDouble(KEY_OFFSET_Y, this.offsetY);
            compoundTag.putDouble(KEY_OFFSET_Z, this.offsetZ);
            compoundTag.putDouble(KEY_ROTATE_X, this.rotateX);
            compoundTag.putDouble(KEY_ROTATE_Y, this.rotateY);
            compoundTag.putDouble(KEY_ROTATE_Z, this.rotateZ);
            compoundTag.putDouble(KEY_SCALE, this.scale);
            compoundTag.putBoolean(KEY_HIDE_PLATFORM_NUMBER, this.hidePlatformNumber);
        }

        @Override
        public int getMaxArrivals() {
            return MAX_ARRIVALS;
        }

        public double getOffsetX() {
            return offsetX;
        }

        public double getOffsetY() {
            return offsetY;
        }

        public double getOffsetZ() {
            return offsetZ;
        }

        public double getRotateX() {
            return rotateX;
        }

        public double getRotateY() {
            return rotateY;
        }

        public double getRotateZ() {
            return rotateZ;
        }

        public double getScale() {
            return scale;
        }

        public boolean getHidePlatformNumber() {
            return hidePlatformNumber;
        }

        public void setHidePlatformNumber(boolean hidePlatformNumber) {
            this.hidePlatformNumber = hidePlatformNumber;
            this.setChanged();
            this.syncData();
        }

        public void setProjectorTransform(double offsetX, double offsetY, double offsetZ,
                                          double rotateX, double rotateY, double rotateZ, double scale) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.rotateX = rotateX;
            this.rotateY = rotateY;
            this.rotateZ = rotateZ;
            this.scale = scale <= 0 ? 1 : scale;
            this.setChanged();
            this.syncData();
        }
    }
}
