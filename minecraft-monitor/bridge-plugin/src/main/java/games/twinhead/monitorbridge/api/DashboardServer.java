package games.twinhead.monitorbridge.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import games.twinhead.monitorbridge.MonitorBridgePlugin;
import games.twinhead.monitorbridge.data.CollectingCommandSender;
import games.twinhead.monitorbridge.data.EventLogBuffer;
import games.twinhead.monitorbridge.data.LiveStatsService;
import games.twinhead.monitorbridge.data.ServerFilesService;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DashboardServer {

    private static final Gson GSON = new GsonBuilder().create();

    private final MonitorBridgePlugin plugin;
    private final EventLogBuffer buffer;
    private final LiveStatsService stats;
    private final ServerFilesService files;
    private final AuthService auth;
    private HttpServer server;

    public DashboardServer(MonitorBridgePlugin plugin, EventLogBuffer buffer,
                           LiveStatsService stats, ServerFilesService files, AuthService auth) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.stats = stats;
        this.files = files;
        this.auth = auth;
    }

    public void start() throws IOException {
        String host = plugin.getConfig().getString("http.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("http.port", 8765);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/") || path.equals("/index.html")) {
                serveStatic(exchange, "/web/index.html", "text/html; charset=utf-8", false);
                return;
            }
            if (path.equals("/app.js")) {
                serveStatic(exchange, "/web/app.js", "application/javascript; charset=utf-8", false);
                return;
            }
            if (path.equals("/styles.css")) {
                serveStatic(exchange, "/web/styles.css", "text/css; charset=utf-8", false);
                return;
            }

            if (path.equals("/health")) {
                sendJson(exchange, 200, Map.of("ok", true, "plugin", "Schellmonitor"));
                return;
            }

            if (path.equals("/api/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
                return;
            }

            if (path.equals("/api/config")) {
                handleConfig(exchange);
                return;
            }

            if (!authorize(exchange)) return;

            switch (path) {
                case "/api/status" -> handleStatus(exchange);
                case "/api/players" -> handlePlayers(exchange);
                case "/api/plugins" -> handlePlugins(exchange);
                case "/api/datapacks" -> handleDatapacks(exchange);
                case "/api/whitelist" -> handleWhitelist(exchange);
                case "/api/ops" -> handleOps(exchange);
                case "/api/banned" -> handleBanned(exchange);
                case "/api/logs" -> handleLogs(exchange);
                case "/api/logs/history" -> handleLogs(exchange);
                case "/api/logs/download" -> handleLogDownload(exchange);
                case "/api/events" -> handleEvents(exchange);
                case "/api/command" -> handleCommand(exchange, method);
                case "/api/rcon" -> handleCommand(exchange, method);
                default -> sendJson(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("embedded", true);
        body.put("authRequired", auth.isAuthRequired());
        body.put("bridge", Map.of("connected", true, "embedded", true));
        body.put("quickCommands", loadQuickCommands());
        body.put("logFile", files.getLogFile().getAbsolutePath());
        sendJson(exchange, 200, body);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<?, ?> json = GSON.fromJson(body, Map.class);
        String password = json != null && json.get("password") != null ? String.valueOf(json.get("password")) : "";
        String token = auth.login(password);
        if (token == null) {
            sendJson(exchange, 401, Map.of("error", "Invalid password"));
            return;
        }
        exchange.getResponseHeaders().add("Set-Cookie", "monitor_session=" + token + "; Path=/; HttpOnly; SameSite=Strict");
        sendJson(exchange, 200, Map.of("token", token, "authRequired", true));
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> data = runOnMainThread(stats::getStatus);
        if (data == null) {
            sendJson(exchange, 503, Map.of("error", "Server not ready"));
            return;
        }
        data.put("bridgeConnected", true);
        data.put("embedded", true);
        Map<String, String> props = files.readServerProperties();
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        Object motd = data.get("motd");
        serverInfo.put("motd", props.getOrDefault("motd", motd != null ? String.valueOf(motd) : ""));
        serverInfo.put("difficulty", props.get("difficulty"));
        serverInfo.put("gamemode", props.get("gamemode"));
        data.put("server", serverInfo);
        sendJson(exchange, 200, data);
    }

    private void handlePlayers(HttpExchange exchange) throws IOException {
        Map<String, Object> data = runOnMainThread(stats::getPlayers);
        sendJson(exchange, 200, data != null ? data : Map.of());
    }

    private void handlePlugins(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> runtime = runOnMainThread(stats::getPlugins);
        List<Map<String, Object>> filePlugins = files.listFilePlugins();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", runtime != null ? runtime.size() : 0);
        body.put("plugins", runtime);
        body.put("files", filePlugins);
        body.put("source", "bridge");
        sendJson(exchange, 200, body);
    }

    private void handleDatapacks(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, files.listDatapacks());
    }

    private void handleWhitelist(HttpExchange exchange) throws IOException {
        List<String> names = files.readWhitelist();
        if (names == null) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }
        sendJson(exchange, 200, Map.of("count", names.size(), "names", names));
    }

    private void handleOps(HttpExchange exchange) throws IOException {
        List<String> names = files.readOps();
        if (names == null) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }
        sendJson(exchange, 200, Map.of("count", names.size(), "names", names));
    }

    private void handleBanned(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> players = files.readBanned();
        if (players == null) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }
        sendJson(exchange, 200, Map.of("count", players.size(), "players", players));
    }

    private void handleLogs(HttpExchange exchange) throws IOException {
        int limit = 500;
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
        body.put("source", "bridge");
        sendJson(exchange, 200, body);
    }

    private void handleLogDownload(HttpExchange exchange) throws IOException {
        java.io.File log = files.getLogFile();
        if (!log.exists()) {
            sendJson(exchange, 404, Map.of("error", "Log not found"));
            return;
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(log.toPath());
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"latest.log\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleCommand(HttpExchange exchange, String method) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<?, ?> json = GSON.fromJson(body, Map.class);
        String command = json != null && json.get("command") != null ? String.valueOf(json.get("command")).trim() : "";
        if (command.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "Command required"));
            return;
        }

        String response = runOnMainThread(() -> {
            CollectingCommandSender sender = new CollectingCommandSender();
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            Bukkit.dispatchCommand(sender, cmd);
            return sender.getOutput();
        });

        sendJson(exchange, 200, Map.of("command", command, "response", response != null ? response : "(no output)"));
    }

    private void handleEvents(HttpExchange exchange) throws IOException {
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
                String line = buffer.formatAsLogLine(event);
                writeSseRaw(out, Map.of("type", "log", "line", line, "event", event));
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
                // keep alive
            }
        } finally {
            buffer.unsubscribe(holder[0]);
            heartbeat.shutdown();
            out.close();
        }
    }

    private void writeSseRaw(OutputStream out, Map<String, Object> payload) throws IOException {
        out.write(("data: " + GSON.toJson(payload) + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void writeSse(OutputStream out, Map<String, Object> event) throws IOException {
        writeSseRaw(out, event);
    }

    private List<Map<String, Object>> loadQuickCommands() {
        try (InputStream in = plugin.getResource("/web/quick-commands.json")) {
            if (in == null) return List.of();
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            return GSON.fromJson(arr, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private void serveStatic(HttpExchange exchange, String resource, String contentType, boolean auth) throws IOException {
        if (auth && !authorize(exchange)) return;
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String bearer = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            bearer = authHeader.substring(7);
        }
        String apiKeyHeader = exchange.getRequestHeaders().getFirst("X-Api-Key");
        if (apiKeyHeader != null) bearer = apiKeyHeader;

        String query = exchange.getRequestURI().getRawQuery();
        String queryKey = null;
        if (query != null) {
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && (kv[0].equals("apiKey") || kv[0].equals("token"))) {
                    queryKey = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        String session = null;
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("monitor_session=")) {
                    session = trimmed.substring("monitor_session=".length());
                }
            }
        }

        if (auth.authorize(bearer, queryKey, session)) return true;
        sendJson(exchange, 401, Map.of("error", "Unauthorized"));
        return false;
    }

    private <T> T runOnMainThread(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                return null;
            }
        }
        final Object[] result = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);
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

    private void sendJson(HttpExchange exchange, int code, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
