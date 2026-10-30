package com.s4.neighborhood.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Limits authentication submissions on this application instance. */
public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_KEYS = 10_000;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthRateLimitFilter() {
        this(Clock.systemUTC());
    }

    AuthRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (!"POST".equals(request.getMethod()) ||
                (!"/api/auth/login".equals(path) && !"/api/auth/register".equals(path)
                        && !"/api/auth/verify-email".equals(path) && !"/api/auth/resend-verification".equals(path)
                        && !"/api/auth/forgot-password".equals(path) && !"/api/auth/reset-password".equals(path))) {
            chain.doFilter(request, response);
            return;
        }

        int max = "/api/auth/login".equals(path) || "/api/auth/verify-email".equals(path) ? 12 : 5;
        String key = path + ":" + request.getRemoteAddr();
        if (!allow(key, max)) {
            response.setHeader("Retry-After", "60");
            response.sendError(429, "Too many authentication requests. Retry in one minute.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key, int max) {
        long now = clock.millis();
        if (attempts.size() >= MAX_KEYS) {
            attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
            if (attempts.size() >= MAX_KEYS && !attempts.containsKey(key)) return false;
        }
        AtomicBoolean allowed = new AtomicBoolean();
        attempts.compute(key, (ignored, prior) -> {
            int count = prior == null || prior.expiresAt <= now ? 1 : prior.count + 1;
            allowed.set(count <= max);
            return new Window(count, prior == null || prior.expiresAt <= now
                    ? now + WINDOW_MILLIS : prior.expiresAt);
        });
        return allowed.get();
    }

    private record Window(int count, long expiresAt) {}
}
