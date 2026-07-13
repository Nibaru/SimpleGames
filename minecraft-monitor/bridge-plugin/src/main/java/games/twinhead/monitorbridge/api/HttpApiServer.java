package games.twinhead.monitorbridge.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import games.twinhead.monitorbridge.MonitorBridgePlugin;
import games.twinhead.monitorbridge.data.EventLogBuffer;
import games.twinhead.monitorbridge.data.LiveStatsService;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HttpApiServer {

    private static final Gson GSON = new GsonBuilder().create();

    private final MonitorBridgePlugin plugin;
    private final EventLogBuffer buffer;
    private final LiveStatsService stats;
    private final String apiKey;
    private HttpServer server;

    public HttpApiServer(MonitorBridgePlugin plugin, EventLogBuffer buffer, LiveStatsService stats) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.stats = stats;
        this.apiKey = plugin.getConfig().getString("http.api-key", "");
    }

    public void start() throws IOException {
        String host = plugin.getConfig().getString("http.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("http.port", 8765);

        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/players", this::handlePlayers);
        server.createContext("/api/plugins", this::handlePlugins);
        server.createContext("/api/logs", this::handleLogs);
        server.createContext("/api/events", this::handleEvents);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("plugin", "MonitorBridge");
        body.put("version", plugin.getPluginMeta().getVersion());
        sendJson(exchange, 200, body);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;
        Map<String, Object> data = runOnMainThread(stats::getStatus);
        if (data == null) {
            sendJson(exchange, 503, Map.of("error", "Server not ready"));
            return;
        }
        sendJson(exchange, 200, data);
    }

    private void handlePlayers(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;
        Map<String, Object> data = runOnMainThread(stats::getPlayers);
        if (data == null) {
            sendJson(exchange, 503, Map.of("error", "Server not ready"));
            return;
        }
        sendJson(exchange, 200, data);
    }

    private void handlePlugins(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;
        List<Map<String, Object>> plugins = runOnMainThread(stats::getPlugins);
        if (plugins == null) {
            sendJson(exchange, 503, Map.of("error", "Server not ready"));
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", plugins.size());
        body.put("plugins", plugins);
        sendJson(exchange, 200, body);
    }

    private void handleLogs(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;
        int limit = 200;
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("limit=")) {
            try {
                limit = Integer.parseInt(query.replaceAll(".*limit=(\\d+).*", "$1"));
            } catch (NumberFormatException ignored) {
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> events = buffer.getRecent(limit);
        body.put("count", events.size());
        body.put("events", events);
        body.put("lines", buffer.toLogLines(limit));
        sendJson(exchange, 200, body);
    }

    private void handleEvents(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) return;

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();

        for (Map<String, Object> event : buffer.getRecent(50)) {
            writeSse(out, event);
        }

        final Consumer<Map<String, Object>>[] holder = new Consumer[1];
        holder[0] = event -> {
            try {
                writeSse(out, event);
            } catch (IOException e) {
                buffer.unsubscribe(holder[0]);
            }
        };
        buffer.subscribe(holder[0]);

        var heartbeat = Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            try {
                out.write(": ping\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                buffer.unsubscribe(holder[0]);
                heartbeat.shutdown();
            }
        }, 15, 15, TimeUnit.SECONDS);

        try {
            byte[] buf = new byte[256];
            while (exchange.getRequestBody().read(buf) != -1) {
                // keep open until client disconnects
            }
        } finally {
            buffer.unsubscribe(holder[0]);
            heartbeat.shutdown();
            out.close();
        }
    }

    private <T> T runOnMainThread(java.util.concurrent.Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                return null;
            }
        }

        final Object[] result = new Object[1];
        var latch = new java.util.concurrent.CountDownLatch(1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                result[0] = task.call();
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(3, TimeUnit.SECONDS)) return null;
            @SuppressWarnings("unchecked")
            T typed = (T) result[0];
            return typed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void writeSse(OutputStream out, Map<String, Object> event) throws IOException {
        String payload = "data: " + GSON.toJson(event) + "\n\n";
        out.write(payload.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return true;
        }

        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            header = exchange.getRequestHeaders().getFirst("X-Api-Key");
        }

        String token = null;
        if (header != null) {
            token = header.startsWith("Bearer ") ? header.substring(7) : header;
        }

        if (apiKey.equals(token)) {
            return true;
        }

        sendJson(exchange, 401, Map.of("error", "Unauthorized"));
        return false;
    }

    private void sendJson(HttpExchange exchange, int code, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
