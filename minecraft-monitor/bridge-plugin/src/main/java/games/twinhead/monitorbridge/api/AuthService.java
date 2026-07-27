package games.twinhead.monitorbridge.api;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final String apiKey;
    private final String dashboardPassword;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    public AuthService(String apiKey, String dashboardPassword) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.dashboardPassword = dashboardPassword == null ? "" : dashboardPassword;
    }

    public boolean isAuthRequired() {
        return !dashboardPassword.isEmpty() || !apiKey.isEmpty();
    }

    public boolean isDashboardPasswordRequired() {
        return !dashboardPassword.isEmpty();
    }

    public String login(String password) {
        if (!dashboardPassword.isEmpty()) {
            if (!dashboardPassword.equals(password)) return null;
        } else if (!apiKey.isEmpty()) {
            if (!apiKey.equals(password)) return null;
        } else {
            return null;
        }

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, System.currentTimeMillis());
        return token;
    }

    public boolean authorize(String bearerToken, String queryApiKey, String sessionToken) {
        if (!isAuthRequired()) return true;

        if (apiKey != null && !apiKey.isEmpty()) {
            if (apiKey.equals(bearerToken) || apiKey.equals(queryApiKey)) return true;
        }

        if (sessionToken != null && isValidSession(sessionToken)) return true;
        if (bearerToken != null && isValidSession(bearerToken)) return true;

        return false;
    }

    private boolean isValidSession(String token) {
        Long created = sessions.get(token);
        if (created == null) return false;
        if (System.currentTimeMillis() - created > SESSION_TTL_MS) {
            sessions.remove(token);
            return false;
        }
        return true;
    }
}
