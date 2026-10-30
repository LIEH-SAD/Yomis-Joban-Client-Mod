package com.jsblock.scripting.core.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Wraps the raw bytes of a resource so scripts can read it more than once.
 * @author LX86 (JCM)
 */
public class DataReaderJS {
    private final Supplier<InputStream> streamSupplier;
    private byte[] cachedBytes;

    public DataReaderJS(Supplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    private byte[] bytes() {
        if (cachedBytes == null) {
            try (final InputStream stream = streamSupplier.get()) {
                cachedBytes = stream == null ? new byte[0] : stream.readAllBytes();
            } catch (IOException e) {
                cachedBytes = new byte[0];
            }
        }
        return cachedBytes;
    }

    public InputStream asInputStream() {
        return new ByteArrayInputStream(bytes());
    }

    public String asString() {
        return new String(bytes(), StandardCharsets.UTF_8);
    }

    public BufferedImage asBufferedImage() {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes()));
        } catch (IOException e) {
            return null;
        }
    }

    public Font asFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes()));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return asString();
    }
}
