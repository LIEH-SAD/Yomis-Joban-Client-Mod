package com.jsblock.render;

import com.jsblock.block.BlockPIDSBaseHorizontal;
import com.jsblock.block.JobanPIDSBase;
import com.jsblock.block.PIDSProjector;
import com.jsblock.block.PIDSRVBase;
import com.jsblock.client.JobanCustomResources;
import com.jsblock.data.PIDSPreset;
import com.jsblock.scripting.pids.PIDSScriptRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.ScheduleEntry;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Abstract class to handle all the preprocessing of Joban PIDS.<br>
 * (Variables, PIDS Preset etc.)
 * @author LX86
 */
public abstract class RenderPIDSBase<T extends BlockEntityMapper> extends BlockEntityRendererMapper<T> implements IGui {
    private final int maxArrivals;

    public static final int SWITCH_LANGUAGE_TICKS = 80;
    public static final int MAX_VIEW_DISTANCE = 16;
    public static final boolean[] SHOW_ALL_ROWS = new boolean[]{false, false, false, false};

    public RenderPIDSBase(BlockEntityRenderDispatcher dispatcher, int maxArrivals) {
        super(dispatcher);
        this.maxArrivals = maxArrivals;
    }

    @Override
    public void render(T entity, float delta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        final Level world = entity.getLevel();
        if (world == null) {
            return;
        }

        if(!(entity instanceof JobanPIDSBase.TileEntityBlockJobanPIDS)) {
            return;
        }

        final String[] customMessages = new String[maxArrivals];
        final List<Long> platformIds = new ArrayList<>(((BlockPIDSBaseHorizontal.TileEntityBlockPIDSBaseHorizontal) entity).getPlatformIds());
        final boolean[] hideArrivals = new boolean[maxArrivals];
        final boolean hidePlatforms;
        final String presetID = ((JobanPIDSBase.TileEntityBlockJobanPIDS)entity).getPresetID();
        PIDSPreset preset = JobanCustomResources.PIDSPresets.getOrDefault(presetID, null);

        /* Auto-switch presets based on train arrival/departure time */
        final JobanPIDSBase.TileEntityBlockJobanPIDS jobanEntity = (JobanPIDSBase.TileEntityBlockJobanPIDS) entity;
        if (jobanEntity.getDepAutoSwitchEnabled() || jobanEntity.getAutoSwitchEnabled()) {
            final BlockPos pos = entity.getBlockPos();
            final List<ScheduleEntry> scheduleList = new ArrayList<>();
            long primaryPlatformId = 0;

            if (!platformIds.isEmpty()) {
                for (long platformId : platformIds) {
                    final Set<ScheduleEntry> schedules = ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
                    if (schedules != null) {
                        scheduleList.addAll(schedules);
                    }
                }
                primaryPlatformId = platformIds.get(0);
            } else {
                final long closestPlatformId = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos);
                primaryPlatformId = closestPlatformId;
                final Set<ScheduleEntry> schedules = ClientData.SCHEDULES_FOR_PLATFORM.get(closestPlatformId);
                if (schedules != null) {
                    scheduleList.addAll(schedules);
                }
            }

            if (!scheduleList.isEmpty()) {
                Collections.sort(scheduleList);
                final ScheduleEntry firstEntry = scheduleList.get(0);
                final long diff = firstEntry.arrivalMillis - System.currentTimeMillis();

                /* Priority 1: Departure auto-switch (only when train is at platform) */
                if (jobanEntity.getDepAutoSwitchEnabled() && diff <= 0) {
                    final int timeSinceArrivalSec = (int)(-diff / 1000);
                    final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(primaryPlatformId);
                    final int depCountdownSec = platform != null ? (platform.getDwellTime() / 2) - timeSinceArrivalSec : 0;
                    final boolean inWindow;
                    if (jobanEntity.getDepAutoSwitchUntilClose()) {
                        inWindow = depCountdownSec > 0 && depCountdownSec <= jobanEntity.getDepAutoSwitchCountdown();
                    } else {
                        inWindow = (depCountdownSec > 0 && depCountdownSec <= jobanEntity.getDepAutoSwitchCountdown()) ||
                                   (depCountdownSec <= 0 && depCountdownSec > -jobanEntity.getDepAutoSwitchDuration());
                    }
                    if (inWindow) {
                        final PIDSPreset depPreset = JobanCustomResources.PIDSPresets.getOrDefault(jobanEntity.getDepAutoSwitchPreset(), null);
                        if (depPreset != null) {
                            preset = depPreset;
                        }
                    }
                }
                /* Priority 2: Arrival auto-switch (lower priority than departure) */
                /* Check !(departure switch active) to avoid overriding departure preset */
                if (preset == JobanCustomResources.PIDSPresets.getOrDefault(presetID, null) && jobanEntity.getAutoSwitchEnabled()) {
                    if ((diff > 0 && diff <= jobanEntity.getAutoSwitchCountdown() * 1000L) ||
                        (diff <= 0 && diff > -jobanEntity.getAutoSwitchDuration() * 1000L)) {
                        final PIDSPreset arrPreset = JobanCustomResources.PIDSPresets.getOrDefault(jobanEntity.getAutoSwitchPreset(), null);
                        if (arrPreset != null) {
                            preset = arrPreset;
                        }
                    }
                }
            }
        }

        if(preset != null && preset.visibility != null) {
            System.arraycopy(preset.visibility, 0, hideArrivals, 0, Math.min(preset.visibility.length, hideArrivals.length));
        }

        for (int i = 0; i < maxArrivals; i++) {
            customMessages[i] = parseVariable(((BlockPIDSBaseHorizontal.TileEntityBlockPIDSBaseHorizontal) entity).getMessage(i), world);
            boolean hideArrival = ((BlockPIDSBaseHorizontal.TileEntityBlockPIDSBaseHorizontal) entity).getHideArrival(i);
            if(hideArrival) {
                hideArrivals[i] = true;
            }
        }

        /* Hide Platform Circles (RV PIDS and the Projector, which has the same switch but is not an RV block) */
        final boolean presetHidePlatform = preset != null && preset.hidePlatform;
        if (entity instanceof PIDSRVBase.TileEntityBlockRVPIDS) {
            hidePlatforms = presetHidePlatform || ((PIDSRVBase.TileEntityBlockRVPIDS) entity).getHidePlatformNumber();
        } else if (entity instanceof PIDSProjector.TileEntityBlockPIDSProjector) {
            hidePlatforms = presetHidePlatform || ((PIDSProjector.TileEntityBlockPIDSProjector) entity).getHidePlatformNumber();
        } else {
            hidePlatforms = presetHidePlatform;
        }

        try {
            render(entity, world, customMessages, hideArrivals, hidePlatforms, preset, platformIds, delta, matrices, vertexConsumers, light, overlay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void render(T entity, Level world, String[] customMessages, boolean[] hideArrivals, boolean hidePlatforms, PIDSPreset preset, List<Long> platformId, float delta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);

    public static String parseVariable(String str, Level world) {
        long time = world.getDayTime() + 6000;
        long hours = time / 1000;
        long minutes = Math.round((time - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        String weatherString = world.isRaining() ? "Raining" : world.isThundering() ? "Thundering" : "Sunny";
        String weatherChinString = world.isRaining() ? "下雨" : world.isThundering() ? "雷暴" : "晴天";
        int worldDay = (int) (world.getDayTime() / 24000L);
        int worldPlayer = world.players().size();
        String timeGreetings;

        if (time >= 6000 & time <= 12000) {
            timeGreetings = "Morning";
        } else if (time >= 12000 & time <= 18000) {
            timeGreetings = "Afternoon";
        } else {
            timeGreetings = "Night";
        }

        return str.replace("{time}", timeString)
                .replace("{day}", String.valueOf(worldDay))
                .replace("{weather}", weatherString)
                .replace("{time_period}", timeGreetings)
                .replace("{weatherChin}", weatherChinString)
                .replace("{worldPlayer}", String.valueOf(worldPlayer));
    }

    static void drawTexture(PoseStack matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int color, int light) {
        IDrawing.drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, color, light);
    }

    /**
     * Whether this renderer draws a PIDS Projector, whose panel is not attached to its block at all.
     *
     * <p>A projector's panel is placed by the block's own offset, rotation and scale, so it replaces
     * the transform the other shapes use instead of adjusting it. It is a flag on the shared RV
     * renderer rather than a subclass because a subclass referenced from the client registration made
     * that class impossible to load on Forge, even though every reference resolved.</p>
     */
    protected boolean projectorMode = false;

    /** @see #projectorMode */
    public RenderPIDSBase<T> setProjectorMode(boolean projector) {
        this.projectorMode = projector;
        return this;
    }

    /** @return whether {@code entity} is a projector drawn by this renderer. */
    protected boolean isProjector(T entity) {
        return projectorMode && entity instanceof PIDSProjector.TileEntityBlockPIDSProjector;
    }

    /** How far the script panel sits in front of the block centre, in blocks, after its translate. */
    protected static final float SCRIPT_PANEL_OUTWARD = 0.02F;

    /* JCM 2.x's script panel transform, one set of literals per PIDS shape. A script lays itself
       out against pids.width/pids.height and is then placed by these numbers, so deriving them
       from the built-in background's geometry -- which is what this used to do -- put the panel a
       fraction of a block off its screen and drew its content at the wrong size. The defaults are
       RVPIDSRenderer's. */
    private float panelTranslateX = -0.21F;
    private float panelTranslateY = -0.14F;
    private float panelTranslateZ = -0.114F;
    private int scriptCanvasWidth = 136;
    private int scriptCanvasHeight = 76;

    /**
     * Lean of the panel about its local X axis, in degrees, applied after the facing rotations and
     * before the translate, so the translate runs in the leaning frame.
     *
     * <p>YJCM's own renderers carry the same term -- the constructor's {@code rotation}, applied
     * straight after the two facing rotations -- and it is what puts a panel on the slanted signs.
     * The two SIL shapes are a V: their halves face opposite ways, so one 22.5 degree lean comes
     * out mirrored and forms it.</p>
     */
    private float panelRotateXDegrees = 0F;

    /**
     * Adopts another PIDS shape's JCM 2.x panel transform and canvas.
     *
     * <p>JCM 2.x gives each renderer its own literals: RV {@code (-0.21, -0.14, -0.128)} with a
     * 136x76 canvas, 1A {@code (-0.47, -0.155, -0.130)} with 186x60, LCD {@code (-0.19, -0.125,
     * -0.130)} with 133x72. Borrowing another shape's set puts the panel roughly a quarter block
     * off its screen.</p>
     */
    public RenderPIDSBase<T> setScriptPanelProfile(float x, float y, float z, int canvasWidth, int canvasHeight) {
        this.panelTranslateX = x;
        this.panelTranslateY = y;
        this.panelTranslateZ = z;
        this.scriptCanvasWidth = canvasWidth;
        this.scriptCanvasHeight = canvasHeight;
        return this;
    }

    /** Adopts a shape's lean as well; see {@link #panelRotateXDegrees}. */
    public RenderPIDSBase<T> setScriptPanelRotation(float degreesX) {
        this.panelRotateXDegrees = degreesX;
        return this;
    }

    /**
     * The canvas size JCM 2.x hands this PIDS type's scripts, in script units.
     *
     * <p>Also a literal there -- 136x76 for RV, 133x72 for LCD, 186x60 for 1A -- rather than
     * something derived, so a script that lays out against {@code pids.width} sees exactly the
     * numbers its author wrote against.</p>
     */
    protected int scriptCanvasWidth() {
        return scriptCanvasWidth;
    }

    /** @see #scriptCanvasWidth() */
    protected int scriptCanvasHeight() {
        return scriptCanvasHeight;
    }

    /**
     * A scripted preset's panel placement, from the block centre.
     *
     * <p>A preset with a script paints its own background, clock and text against the canvas JCM
     * 2.x gives its PIDS type, so it takes no part in the built-in transform: that transform scales
     * the stack by {@code 1/scale} to match a texture's pixels, and mapping the canvas onto those
     * pixels at the same time shrank everything on it. Here the chain ends in {@code 1/96}, i.e.
     * one script unit is one ninety-sixth of a block, and the caller must not scale again.</p>
     */
    protected void applyScriptPanelTransform(PoseStack matrices, T entity, Level world, Direction facing, boolean rotate90) {
        if (isProjector(entity)) {
            applyProjectorScriptTransform(matrices, entity, world, facing, rotate90);
            return;
        }

        matrices.translate(0.5, 0.5, 0.5);
        UtilitiesClient.rotateYDegrees(matrices, (rotate90 ? 90 : 0) - facing.toYRot());
        UtilitiesClient.rotateZDegrees(matrices, 180);
        matrices.mulPose(Axis.XP.rotationDegrees(panelRotateXDegrees));
        matrices.translate(panelTranslateX, panelTranslateY, panelTranslateZ);
        matrices.translate(0F, 0F, -SCRIPT_PANEL_OUTWARD);
        matrices.scale(1F / 96F, 1F / 96F, 1F / 96F);
    }

    /**
     * Draws a scripted preset: the one path every PIDS shape takes for them, so that the canvas
     * placement lives in one place instead of in each renderer's shared matrix stack.
     *
     * @return {@code true} when the preset's script ran and painted the panel, {@code false} when
     * there was nothing to run -- no script, scripting switched off, or a script that could not be
     * resolved. A pack whose script is missing or broken still shows its built-in background rather
     * than a blank panel, which is what the concrete renderers fall back to.
     */
    protected boolean renderScriptPanel(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, T entity, Level world, Direction facing, PIDSPreset preset, String pidsType, String[] customMessages, boolean[] hideArrivals, boolean hidePlatforms, List<Long> targetPlatformIds, boolean rotate90) {
        /* A pack may still declare a canvas of its own; JCM 2.x's per-type literal is the default. */
        final float canvasWidth = preset.scriptWidth == null ? scriptCanvasWidth() : preset.scriptWidth;
        final float canvasHeight = preset.scriptHeight == null ? scriptCanvasHeight() : preset.scriptHeight;

        matrices.pushPose();
        applyScriptPanelTransform(matrices, entity, world, facing, rotate90);
        drawProjectorFrameIfAiming(world, facing, matrices, vertexConsumers, (int) canvasWidth, (int) canvasHeight);
        /* The chain ends in 1/96, so a script unit is one canvas unit and the draw calls must not
           scale again. A z-order step is given in blocks, hence 96 canvas units per block.
           A panel is drawn fully lit, as JCM 2.x does: its script render context is given
           MAX_LIGHT_GLOWING rather than the block's own light. */
        final boolean rendered = PIDSScriptRenderer.render(matrices, vertexConsumers, immediate, font, facing, MAX_LIGHT_GLOWING, entity, preset, pidsType, customMessages, hideArrivals, targetPlatformIds, canvasWidth, canvasHeight, hidePlatforms, 96F);
        matrices.popPose();
        return rendered;
    }

    /**
     * Whether the player is holding a brush, which is when a projector shows where its panel is.
     *
     * <p>MTR's own test, with a flag standing in for the answer: it is a held-item check, and running
     * it is how the mod asks the same question elsewhere.</p>
     */
    protected boolean aimingWithBrush(Level world) {
        final Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        final boolean[] aiming = {false};
        IBlock.checkHoldingBrush(world, player, () -> aiming[0] = true);
        return aiming[0];
    }

    /**
     * A projector's panel placement.
     *
     * <p>JCM 2.x's chain, run on top of the block centre: the facing rotation, the 180 degree Z flip
     * that puts the panel the right way up (without it the panel is mirrored and its text upside
     * down), the projector's own 90 degree turn, then its offset, then its rotation. The panel scale
     * comes next, and {@code unitsPerBlock} at the end so the caller's own element positions keep the
     * numbers they were written as.</p>
     *
     * @param unitsPerBlock   how many units make a block in the caller's element space
     * @param rowOffsetBlocks how far down the panel to start, in blocks
     */
    protected void applyProjectorTransform(PoseStack matrices, T entity, Level world, Direction facing, boolean rotate90, float unitsPerBlock, float rowOffsetBlocks) {        final PIDSProjector.TileEntityBlockPIDSProjector projector = (PIDSProjector.TileEntityBlockPIDSProjector) entity;
        final float panelScale = (float) projector.getScale();

        matrices.translate(0.5, 0.5, 0.5);
        UtilitiesClient.rotateYDegrees(matrices, (rotate90 ? 90 : 0) - facing.toYRot());
        UtilitiesClient.rotateZDegrees(matrices, 180);
        UtilitiesClient.rotateYDegrees(matrices, 90);
        matrices.translate(-0.5F + (float) projector.getOffsetX(), -0.5F - (float) projector.getOffsetY(), 0.5F + (float) projector.getOffsetZ());
        matrices.mulPose(Axis.XP.rotationDegrees((float) projector.getRotateX()));
        matrices.mulPose(Axis.YP.rotationDegrees((float) projector.getRotateY()));
        matrices.mulPose(Axis.ZP.rotationDegrees((float) projector.getRotateZ()));

        /* JCM 2.x's projection rectangle: four red lines around the area the panel will cover, drawn
           only while the projector is unrotated, because a turned panel makes an axis-aligned
           rectangle say something untrue. */
        if (aimingWithBrush(world) && projector.getRotateX() == 0 && projector.getRotateY() == 0 && projector.getRotateZ() == 0) {
            drawProjectorRangeLines(matrices, panelScale);
        }

        if (rowOffsetBlocks != 0) {
            /* Local +Y runs down the panel after the Z flip, and the panel scale is applied below, so
               dividing here makes a row advance exactly rowOffsetBlocks in the world. */
            matrices.translate(0, rowOffsetBlocks / (PIDSProjector.PROJECTOR_PANEL_SCALE * panelScale), 0);
        }

        matrices.scale(PIDSProjector.PROJECTOR_PANEL_SCALE * panelScale, PIDSProjector.PROJECTOR_PANEL_SCALE * panelScale, 1F);
        matrices.scale(1F / unitsPerBlock, 1F / unitsPerBlock, 1F / unitsPerBlock);
    }

    /** @see #applyProjectorTransform */
    protected void applyProjectorBuiltInTransform(PoseStack matrices, T entity, Level world, Direction facing, boolean rotate90, float scale, float rowOffsetBlocks) {
        applyProjectorTransform(matrices, entity, world, facing, rotate90, scale, rowOffsetBlocks);
    }

    /** The same placement for a scripted preset, whose canvas is 1/96 block per unit. */
    protected void applyProjectorScriptTransform(PoseStack matrices, T entity, Level world, Direction facing, boolean rotate90) {
        applyProjectorTransform(matrices, entity, world, facing, rotate90, 96F, 0F);
    }

    /**
     * Draws the projector's frame while the player holds a brush, in the caller's units.
     *
     * <p>Four thin strips rather than one stretched glow texture: JCM 2.x stretches
     * {@code jsblock:textures/block/light_1.png} across the whole panel and relies on it being a hollow
     * ring, but it is a flat 16x16, so the same call paints an opaque slab over the panel. Four edges
     * say the same thing and cannot cover anything.</p>
     */
    protected void drawProjectorFrameIfAiming(Level world, Direction facing, PoseStack matrices, MultiBufferSource vertexConsumers, int canvasWidth, int canvasHeight) {
        if (!projectorMode || !aimingWithBrush(world)) {
            return;
        }
        final VertexConsumer consumer = vertexConsumers.getBuffer(MoreRenderLayers.getLight(new ResourceLocation("jsblock:textures/block/light_1.png"), false));
        final float thickness = 1.5F;
        final float width = canvasWidth;
        final float height = canvasHeight;
        drawFrameStrip(matrices, consumer, facing, 0F, 0F, width, thickness);
        drawFrameStrip(matrices, consumer, facing, 0F, height - thickness, width, thickness);
        drawFrameStrip(matrices, consumer, facing, 0F, thickness, thickness, height - thickness * 2F);
        drawFrameStrip(matrices, consumer, facing, width - thickness, thickness, thickness, height - thickness * 2F);
    }

    private void drawFrameStrip(PoseStack matrices, VertexConsumer consumer, Direction facing, float x, float y, float stripWidth, float stripHeight) {
        IDrawing.drawTexture(matrices, consumer, x, y, 0.1F, x + stripWidth, y + stripHeight, 0.1F, 0, 0, 1, 1, facing, 0xFFFF0000, MAX_LIGHT_GLOWING);
    }

    /**
     * The four red lines JCM 2.x draws from a projector while a brush is held: the area the panel will
     * cover, 1.785 by 1 blocks per unit of scale, in the projector's own space.
     */
    private void drawProjectorRangeLines(PoseStack matrices, float panelScale) {
        final MultiBufferSource.BufferSource lines = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        final VertexConsumer consumer = lines.getBuffer(RenderType.lines());
        final Matrix4f pose = matrices.last().pose();
        final float width = 1.785F * panelScale;
        final float height = panelScale;
        rangeLine(consumer, pose, 0F, 0F, width, 0F);
        rangeLine(consumer, pose, 0F, 0F, 0F, height);
        rangeLine(consumer, pose, width, 0F, width, height);
        rangeLine(consumer, pose, 0F, height, width, height);
        lines.endBatch();
    }

    /** One edge of {@link #drawProjectorRangeLines}, in the projector's own space. */
    private static void rangeLine(VertexConsumer consumer, Matrix4f pose, float x1, float y1, float x2, float y2) {
        consumer.vertex(pose, x1, y1, 0F).color(1F, 0F, 0F, 1F).normal(0F, 0F, 1F).endVertex();
        consumer.vertex(pose, x2, y2, 0F).color(1F, 0F, 0F, 1F).normal(0F, 0F, 1F).endVertex();
    }
}