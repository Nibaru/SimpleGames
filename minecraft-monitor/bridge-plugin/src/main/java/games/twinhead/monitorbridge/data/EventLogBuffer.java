package games.twinhead.monitorbridge.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventLogBuffer {

    private static final Gson GSON = new GsonBuilder().create();

    private final int maxSize;
    private final LinkedList<Map<String, Object>> events = new LinkedList<>();
    private final CopyOnWriteArrayList<Consumer<Map<String, Object>>> subscribers = new CopyOnWriteArrayList<>();

    public EventLogBuffer(int maxSize) {
        this.maxSize = Math.max(100, maxSize);
    }

    public synchronized void log(String type, String level, String message, Map<String, Object> extra) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("type", type);
        event.put("level", level);
        event.put("message", message);
        event.put("time", System.currentTimeMillis());
        event.put("timeFormatted", formatTime());
        if (extra != null && !extra.isEmpty()) {
            event.put("data", extra);
        }

        events.addLast(event);
        while (events.size() > maxSize) {
            events.removeFirst();
        }

        for (Consumer<Map<String, Object>> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized List<Map<String, Object>> getRecent(int limit) {
        int size = Math.min(limit, events.size());
        if (size == 0) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>(size);
        int start = events.size() - size;
        int i = 0;
        for (Map<String, Object> event : events) {
            if (i++ >= start) result.add(event);
        }
        return result;
    }

    public synchronized List<String> toLogLines(int limit) {
        List<Map<String, Object>> recent = getRecent(limit);
        List<String> lines = new ArrayList<>(recent.size());
        for (Map<String, Object> event : recent) {
            lines.add(formatAsLogLine(event));
        }
        return lines;
    }

    public String formatAsLogLine(Map<String, Object> event) {
        String time = (String) event.getOrDefault("timeFormatted", "??:??:??");
        String level = String.valueOf(event.getOrDefault("level", "INFO")).toUpperCase();
        String type = String.valueOf(event.getOrDefault("type", "event"));
        String message = String.valueOf(event.getOrDefault("message", ""));
        return String.format("[%s] [MonitorBridge/%s] [%s] %s: %s", time, type, level, type, message);
    }

    public void subscribe(Consumer<Map<String, Object>> consumer) {
        subscribers.add(consumer);
    }

    public void unsubscribe(Consumer<Map<String, Object>> consumer) {
        subscribers.remove(consumer);
    }

    public String toJson(List<Map<String, Object>> items) {
        return GSON.toJson(items);
    }

    public String toJson(Map<String, Object> item) {
        return GSON.toJson(item);
    }

    private static String formatTime() {
        long now = System.currentTimeMillis();
        java.time.Instant instant = java.time.Instant.ofEpochMilli(now);
        java.time.LocalTime time = java.time.LocalTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
    }
}
