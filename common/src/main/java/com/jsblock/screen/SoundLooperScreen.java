package com.jsblock.screen;

import com.jsblock.block.SoundLooper;
import com.jsblock.packet.PacketClient;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Sound Looper Configuration Screen.
 * @author LX86
 * @since 1.0.7
 */
public class SoundLooperScreen extends ScreenMapper implements IGui {

    private final WidgetBetterTextField textBoxSoundId;
    private final WidgetIntegerTextField textBoxSoundVolume;
    private final WidgetIntegerTextField textBoxRepeatTick;
    private final WidgetIntegerTextField textBoxx1;
    private final WidgetIntegerTextField textBoxx2;
    private final WidgetIntegerTextField textBoxy1;
    private final WidgetIntegerTextField textBoxy2;
    private final WidgetIntegerTextField textBoxz1;
    private final WidgetIntegerTextField textBoxz2;
    private final WidgetBetterCheckbox checkBoxLimitRange;
    private final WidgetBetterCheckbox checkBoxNeedRedstone;

    private final WidgetBetterCheckbox checkBoxUseNetworkAudio;
    private final WidgetBetterTextField textBoxNetworkAudioUrl;

    private final Button buttonCategory;
    private final BlockPos pos;
    private int selectedCategory;

    private static final int VOLUME_SCALE = 100;
    private static final int TEXT_PADDING = 16;
    private static final int TEXT_FIELD_WIDTH = 100;
    private static final int POS_FIELD_WIDTH = 50;
    private static final int FINAL_TEXT_HEIGHT = TEXT_HEIGHT + TEXT_PADDING;
    private static final int MAX_TEXT_LENGTH = 128;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = TEXT_HEIGHT + 10;
    private static final int DEFAULT_REPEAT_TICK = 20;
    private static final int DEFAULT_VOLUME = 100;
    private static final SoundSource[] SOURCE_LIST = {SoundSource.MASTER, SoundSource.MUSIC, SoundSource.WEATHER, SoundSource.AMBIENT, SoundSource.PLAYERS, SoundSource.BLOCKS, SoundSource.VOICE};

    public SoundLooperScreen(BlockPos pos) {
        super(Text.literal(""));
        this.pos = pos;

        buttonCategory = UtilitiesClient.newButton(Text.literal(""), button -> {
            selectedCategory++;
            if (selectedCategory > SOURCE_LIST.length - 1) {
                selectedCategory = 0;
            }

            button.setMessage(Text.literal(SOURCE_LIST[selectedCategory].getName()));
        });

        textBoxSoundId = TextFields.setValue(new WidgetBetterTextField("", MAX_TEXT_LENGTH), "mtr:ticket_barrier");
        textBoxRepeatTick = new WidgetIntegerTextField(DEFAULT_REPEAT_TICK, true, MAX_TEXT_LENGTH);
        textBoxSoundVolume = new WidgetIntegerTextField(DEFAULT_VOLUME, true, MAX_TEXT_LENGTH);
        textBoxx1 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);
        textBoxx2 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);
        textBoxy1 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);
        textBoxy2 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);
        textBoxz1 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);
        textBoxz2 = new WidgetIntegerTextField(0, false, MAX_TEXT_LENGTH);

        textBoxNetworkAudioUrl = TextFields.setValue(new WidgetBetterTextField("", MAX_TEXT_LENGTH), "https://example.com/audio.ogg");

        checkBoxUseNetworkAudio = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE,
                Text.translatable("gui.jsblock.looper.use_network_audio"), checked -> {
            textBoxSoundId.setVisible(!checked);
            textBoxNetworkAudioUrl.setVisible(checked);
        });

        checkBoxLimitRange = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.literal(""), checked -> {
        });
        checkBoxNeedRedstone = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.literal(""), checked -> {
        });
    }

    @Override
    protected void init() {
        super.init();
        int i = 1;

        IDrawing.setPositionAndWidth(buttonCategory, width - SQUARE_SIZE - BUTTON_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, BUTTON_WIDTH);

        int soundRow = i;
        IDrawing.setPositionAndWidth(textBoxSoundId, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * soundRow + SQUARE_SIZE, TEXT_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxNetworkAudioUrl, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * soundRow + SQUARE_SIZE, TEXT_FIELD_WIDTH);
        i++;

        IDrawing.setPositionAndWidth(textBoxSoundVolume, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, TEXT_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxRepeatTick, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, TEXT_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(checkBoxLimitRange, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, TEXT_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(checkBoxNeedRedstone, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, TEXT_FIELD_WIDTH);

        IDrawing.setPositionAndWidth(textBoxx1, width - SQUARE_SIZE - (POS_FIELD_WIDTH * 3), FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxy1, width - SQUARE_SIZE - (POS_FIELD_WIDTH * 2), FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxz1, width - SQUARE_SIZE - POS_FIELD_WIDTH, FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        i++;

        IDrawing.setPositionAndWidth(textBoxx2, width - SQUARE_SIZE - (POS_FIELD_WIDTH * 3), FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxy2, width - SQUARE_SIZE - (POS_FIELD_WIDTH * 2), FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        IDrawing.setPositionAndWidth(textBoxz2, width - SQUARE_SIZE - POS_FIELD_WIDTH, FINAL_TEXT_HEIGHT * i + SQUARE_SIZE, POS_FIELD_WIDTH);
        i++;

        IDrawing.setPositionAndWidth(checkBoxUseNetworkAudio, width - SQUARE_SIZE - TEXT_FIELD_WIDTH, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, TEXT_FIELD_WIDTH);

        final Level world = Minecraft.getInstance().level;
        if (world != null) {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof SoundLooper.TileEntitySoundLooper) {
                BlockPos pos1 = ((SoundLooper.TileEntitySoundLooper) entity).getPos1();
                BlockPos pos2 = ((SoundLooper.TileEntitySoundLooper) entity).getPos2();
                textBoxx1.setValue(String.valueOf(pos1.getX()));
                textBoxx2.setValue(String.valueOf(pos2.getX()));
                textBoxy1.setValue(String.valueOf(pos1.getY()));
                textBoxy2.setValue(String.valueOf(pos2.getY()));
                textBoxz1.setValue(String.valueOf(pos1.getZ()));
                textBoxz2.setValue(String.valueOf(pos2.getZ()));
                textBoxSoundId.setValue(((SoundLooper.TileEntitySoundLooper) entity).getSoundId());
                textBoxRepeatTick.setValue(String.valueOf(((SoundLooper.TileEntitySoundLooper) entity).getLoopInterval()));
                textBoxSoundVolume.setValue(String.valueOf(Math.round(((SoundLooper.TileEntitySoundLooper) entity).getSoundVolume() * VOLUME_SCALE)));
                checkBoxNeedRedstone.setChecked(((SoundLooper.TileEntitySoundLooper) entity).getNeedRedstone());
                checkBoxLimitRange.setChecked(((SoundLooper.TileEntitySoundLooper) entity).getLimitRange());
                selectedCategory = ((SoundLooper.TileEntitySoundLooper) entity).getSoundCategory();
                buttonCategory.setMessage(Text.literal(SOURCE_LIST[selectedCategory].getName()));

                checkBoxUseNetworkAudio.setChecked(((SoundLooper.TileEntitySoundLooper) entity).getUseNetworkAudio());
                textBoxNetworkAudioUrl.setValue(((SoundLooper.TileEntitySoundLooper) entity).getNetworkAudioUrl());
            }
        }

        addDrawableChild(buttonCategory);
        addDrawableChild(textBoxSoundId);
        addDrawableChild(textBoxSoundVolume);
        addDrawableChild(textBoxRepeatTick);
        addDrawableChild(checkBoxLimitRange);
        addDrawableChild(checkBoxNeedRedstone);
        addDrawableChild(textBoxx1);
        addDrawableChild(textBoxy1);
        addDrawableChild(textBoxz1);
        addDrawableChild(textBoxx2);
        addDrawableChild(textBoxy2);
        addDrawableChild(textBoxz2);
        addDrawableChild(checkBoxUseNetworkAudio);
        addDrawableChild(textBoxNetworkAudioUrl);

        boolean useNetwork = checkBoxUseNetworkAudio.selected();
        textBoxSoundId.setVisible(!useNetwork);
        textBoxNetworkAudioUrl.setVisible(useNetwork);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(guiGraphics);
            guiGraphics.drawCenteredString(font, Text.translatable("gui.jsblock.looper"), width / 2, TEXT_PADDING, ARGB_WHITE);

            int i = 1;
            boolean limitedRange = checkBoxLimitRange.selected();

            boolean useNetwork = checkBoxUseNetworkAudio.selected();

            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.sound_source"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            if (useNetwork) {
                guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.network_audio_url"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            } else {
                guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.sound_id"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            }
            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.sound_vol"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.repeat_tick"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.limit_range"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.need_redstone"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.use_network_audio"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);

            if (limitedRange) {
                guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.pos1"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
                guiGraphics.drawString(font, Text.translatable("gui.jsblock.looper.pos2"), SQUARE_SIZE, FINAL_TEXT_HEIGHT * (i++) + SQUARE_SIZE, ARGB_WHITE);
            }

            textBoxx1.setVisible(limitedRange);
            textBoxy1.setVisible(limitedRange);
            textBoxz1.setVisible(limitedRange);
            textBoxx2.setVisible(limitedRange);
            textBoxy2.setVisible(limitedRange);
            textBoxz2.setVisible(limitedRange);

            textBoxSoundId.setVisible(!useNetwork);
            textBoxNetworkAudioUrl.setVisible(useNetwork);

            super.render(guiGraphics, mouseX, mouseY, delta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose() {
        int repeatTick = textBoxRepeatTick.getIntegerValue(1);
        float volume = (float)textBoxSoundVolume.getIntegerValue(1) / VOLUME_SCALE;
        final BlockPos pos1 = new BlockPos(textBoxx1.getIntegerValue(), textBoxy1.getIntegerValue(), textBoxz1.getIntegerValue());
        final BlockPos pos2 = new BlockPos(textBoxx2.getIntegerValue(), textBoxy2.getIntegerValue(), textBoxz2.getIntegerValue());

        boolean useNetwork = checkBoxUseNetworkAudio.selected();
        String networkUrl = useNetwork ? textBoxNetworkAudioUrl.getValue() : "";

        PacketClient.sendSoundLooperC2S(pos, selectedCategory, textBoxSoundId.getValue(),
                repeatTick, volume, checkBoxNeedRedstone.selected(), checkBoxLimitRange.selected(),
                pos1, pos2, useNetwork, networkUrl);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
