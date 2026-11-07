package com.jsblock.scripting.core.util;

import com.jsblock.Joban;
import com.jsblock.lib.org.mozilla.javascript.Context;
import com.jsblock.lib.org.mozilla.javascript.Function;
import com.jsblock.lib.org.mozilla.javascript.Scriptable;
import com.jsblock.lib.org.mozilla.javascript.ScriptableObject;
import com.jsblock.scripting.Scripting;
import com.jsblock.scripting.core.rhino.MTRRhinoClassShutter;
import com.jsblock.scripting.core.rhino.MTRRhinoContextFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code Networking}, {@code NetworkResponse} and {@code BackgroundWorker} globals.<br>
 * JCM 2.x's documentation wraps its HTTP examples in {@code BackgroundWorker.submit(function(){...})}
 * because {@code Networking} blocks; the pair is ported together so those examples keep working.
 * @author LX86 (JCM)
 */
public final class NetworkJS {
    private NetworkJS() {
    }

    /** {@code BackgroundWorker.submit(function)} — runs a script function off the render thread. */
    public static final class BackgroundWorker {
        private static final ExecutorService POOL = Executors.newFixedThreadPool(2, runnable -> {
            final Thread thread = new Thread(runnable, "YJCM-Script-Worker");
            thread.setDaemon(true);
            return thread;
        });

        private BackgroundWorker() {
        }

        public static void submit(Function task) {
            if (task == null) return;
            final Scriptable scope = task.getParentScope();
            POOL.submit(() -> {
                final Context cx = Context.enter();
                try {
                    MTRRhinoContextFactory.prepare(cx);
                    if (Scripting.getScriptManager() != null) {
                        cx.setClassShutter(new MTRRhinoClassShutter(Scripting.getScriptManager().getClassShutter()));
                    }
                    task.call(cx, scope, scope, new Object[0]);
                } catch (Throwable t) {
                    Joban.LOGGER.warn("[YJCM Scripting] Background script task failed: {}", t.toString());
                } finally {
                    Context.exit();
                }
            });
        }
    }

    /** {@code Networking.fetch(...)} — synchronous HTTP, meant to be called from a background worker. */
    public static final class Networking {
        private static final HttpClient CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        private Networking() {
        }

        public static NetworkResponse fetch(String url) {
            return fetch(url, null);
        }

        public static NetworkResponse fetch(String url, Scriptable options) {
            final Reply reply = request(url, options);
            final byte[] body = reply.body;
            return new NetworkResponse(reply.code, new DataReaderJS(() -> new ByteArrayInputStream(body)));
        }

        public static NetworkResponse fetchString(String url) {
            return fetchString(url, null);
        }

        public static NetworkResponse fetchString(String url, Scriptable options) {
            final Reply reply = request(url, options);
            return new NetworkResponse(reply.code, new String(reply.body, StandardCharsets.UTF_8));
        }

        public static NetworkResponse fetchImage(String url) {
            return fetchImage(url, null);
        }

        public static NetworkResponse fetchImage(String url, Scriptable options) {
            final Reply reply = request(url, options);
            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(reply.body));
            } catch (Exception e) {
                Joban.LOGGER.warn("[YJCM Scripting] Could not decode the image at {}", url);
            }
            return new NetworkResponse(reply.code, image);
        }

        private static Reply request(String url, Scriptable options) {
            try {
                final String method = optionString(options, "method", "GET").toUpperCase(java.util.Locale.ROOT);
                final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "Yomis-Joban-Client-Mod-PIDS");
                if ("POST".equals(method)) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(optionString(options, "body", "")));
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.noBody());
                }
                final HttpResponse<byte[]> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                return new Reply(response.statusCode(), response.body() == null ? new byte[0] : response.body());
            } catch (Exception e) {
                Joban.LOGGER.warn("[YJCM Scripting] HTTP request to {} failed: {}", url, e.toString());
                return new Reply(0, new byte[0]);
            }
        }

        private static String optionString(Scriptable options, String key, String fallback) {
            if (options == null) return fallback;
            final Object value = ScriptableObject.getProperty(options, key);
            if (value == Scriptable.NOT_FOUND || value == null) return fallback;
            final String text = value.toString();
            return text.isEmpty() ? fallback : text;
        }

        private static final class Reply {
            private final int code;
            private final byte[] body;

            private Reply(int code, byte[] body) {
                this.code = code;
                this.body = body;
            }
        }
    }

    /** {@code NetworkResponse} — the reply, as returned by {@code Networking.*}. */
    public static final class NetworkResponse {
        private final int code;
        private final Object data;

        NetworkResponse(int code, Object data) {
            this.code = code;
            this.data = data;
        }

        public boolean ok() {
            return code >= 200 && code < 300;
        }

        public int getResponseCode() {
            return code;
        }

        public Object getData() {
            return data;
        }

        @Override
        public String toString() {
            return "NetworkResponse[" + code + "]";
        }
    }
}
