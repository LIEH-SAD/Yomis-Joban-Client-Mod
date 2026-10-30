package com.jsblock.audio;

import com.jsblock.Joban;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.openal.AL10.*;

public class NetworkAudioPlayer {

    public static void play(String url, float volume, BlockPos pos) {
        if (Minecraft.getInstance() == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            Path tempFile = null;
            try {
                trustAllCertificates();

                URL audioUrl = new URL(url);
                tempFile = Files.createTempFile("network_audio", ".tmp");
                try (InputStream in = audioUrl.openStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(tempFile.toFile());
                AudioFormat baseFormat = audioStream.getFormat();
                AudioFormat pcmFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false);
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, audioStream);
                byte[] pcmData = pcmStream.readAllBytes();

                int alFormat;
                if (pcmFormat.getChannels() == 1) {
                    alFormat = AL_FORMAT_MONO16;
                } else if (pcmFormat.getChannels() == 2) {
                    alFormat = AL_FORMAT_STEREO16;
                } else {
                    throw new UnsupportedAudioFileException("仅支持单声道或双声道音频");
                }

                int sampleRate = (int) pcmFormat.getSampleRate();
                float centerX = pos.getX() + 0.5f;
                float centerY = pos.getY() + 0.5f;
                float centerZ = pos.getZ() + 0.5f;

                Path finalTempFile = tempFile;
                Minecraft.getInstance().execute(() -> {
                    try {
                        ByteBuffer bufferData = ByteBuffer.wrap(pcmData);
                        playOpenAL(bufferData, alFormat, sampleRate, volume, centerX, centerY, centerZ, finalTempFile);
                    } catch (Exception e) {
                        Joban.LOGGER.error("OpenAL播放失败", e);
                        deleteTempFile(finalTempFile);
                    }
                });

            } catch (UnsupportedAudioFileException e) {
                Joban.LOGGER.warn("不支持的音频格式: {}", url);
                e.printStackTrace();
                deleteTempFile(tempFile);
            } catch (Exception e) {
                Joban.LOGGER.error("网络音频处理失败", e);
                e.printStackTrace();
                deleteTempFile(tempFile);
            }
        });
    }

    private static void playOpenAL(ByteBuffer pcmData, int alFormat, int sampleRate,
                                   float volume, float x, float y, float z,
                                   Path tempFile) {
        int buffer = alGenBuffers();
        int source = alGenSources();

        alBufferData(buffer, alFormat, pcmData, sampleRate);
        alSourcei(source, AL_BUFFER, buffer);

        alSourcef(source, AL_GAIN, volume);
        alSource3f(source, AL_POSITION, x, y, z);
        alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
        alSourcef(source, AL_ROLLOFF_FACTOR, 1.0f);
        alSourcef(source, AL_REFERENCE_DISTANCE, 5.0f);
        alSourcef(source, AL_MAX_DISTANCE, 50.0f);

        alSourcePlay(source);

        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new RuntimeException("OpenAL错误: " + error);
        }

        checkPlaybackCompletion(source, buffer, tempFile);
    }

    private static void checkPlaybackCompletion(int source, int buffer, Path tempFile) {
        if (alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING) {
            Minecraft.getInstance().execute(() -> checkPlaybackCompletion(source, buffer, tempFile));
        } else {
            alDeleteSources(source);
            alDeleteBuffers(buffer);
            deleteTempFile(tempFile);
        }
    }

    private static void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    private static void trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}