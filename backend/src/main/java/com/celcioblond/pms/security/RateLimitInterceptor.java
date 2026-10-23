package com.celcioblond.pms.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-user rate limit for the paid AI endpoints.
 * State is in memory, so the limit applies per application instance.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int maxRequestsPerWindow;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int maxRequestsPerWindow, Clock clock) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Principal principal = request.getUserPrincipal();
        String key = principal != null ? principal.getName() : request.getRemoteAddr();
        long now = clock.millis();

        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.start >= WINDOW.toMillis()
                        ? new Window(now, 1)
                        : new Window(current.start, current.count + 1));

        if (window.count > maxRequestsPerWindow) {
            long retryAfterSeconds = Math.max(1, (window.start + WINDOW.toMillis() - now) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            return false;
        }
        return true;
    }

    private record Window(long start, int count) {
    }
}
