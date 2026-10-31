package com.jsblock.scripting.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Client state helpers exposed to scripts as {@code MinecraftClient}.<br>
 * Mirrors the class of the same name in JCM, so scripts written for it keep working
 * (MTR 4.x / MC 1.20.4 APIs are mapped onto what MTR 3.x and MC 1.20 provide).
 * @author Zbx1425 / LX86 (JCM)
 */
public class MinecraftClientJS {
    private MinecraftClientJS() {
    }

    private static Level level() {
        return Minecraft.getInstance().level;
    }

    public static boolean worldIsRaining() {
        final Level level = level();
        return level != null && level.isRaining();
    }

    public static boolean worldIsThundering() {
        final Level level = level();
        return level != null && level.isThundering();
    }

    /** @return the in-game time of day, between 0 and 23999 */
    public static int worldDayTime() {
        final Level level = level();
        return level == null ? 0 : (int) (level.getDayTime() % 24000L);
    }

    /** @return the total world age in ticks */
    public static long worldTime() {
        final Level level = level();
        return level == null ? 0 : level.getGameTime();
    }

    /** @return the dimension id, e.g. {@code minecraft:overworld} */
    public static String getCurrentWorldId() {
        final Level level = level();
        return level == null ? "minecraft:overworld" : level.dimension().location().toString();
    }

    public static boolean gamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    public static int renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    /** @return the camera perspective ({@code FIRST_PERSON}, {@code THIRD_PERSON_BACK} or {@code THIRD_PERSON_FRONT}) */
    public static String getCameraMode() {
        return Minecraft.getInstance().options.getCameraType().name();
    }

    public static String playerName() {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "" : player.getGameProfile().getName();
    }

    /** Send a chat ({@code actionBar == false}) or action bar message to the local player. */
    public static void displayMessage(String message, boolean actionBar) {
        if (message == null) return;
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            final LocalPlayer player = minecraft.player;
            if (player != null) {
                player.displayClientMessage(Component.literal(message), actionBar);
            }
        });
    }
}
