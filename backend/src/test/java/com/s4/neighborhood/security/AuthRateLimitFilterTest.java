package com.s4.neighborhood.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitFilterTest {
    @Test
    void throttlesLoginAfterTwelveSubmissionsFromSameClient() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(fixedClock());
        for (int i = 0; i < 12; i++) {
            MockHttpServletResponse response = request(filter, "/api/auth/login", "192.0.2.1", "POST");
            assertEquals(200, response.getStatus());
        }
        MockHttpServletResponse blocked = request(filter, "/api/auth/login", "192.0.2.1", "POST");
        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        assertEquals(200, request(filter, "/api/auth/login", "192.0.2.2", "POST").getStatus());
    }

    @Test
    void registrationHasSeparateLimitAndReadsDoNotCount() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(fixedClock());
        for (int i = 0; i < 5; i++) {
            assertEquals(200, request(filter, "/api/auth/register", "192.0.2.3", "POST").getStatus());
        }
        assertEquals(429, request(filter, "/api/auth/register", "192.0.2.3", "POST").getStatus());
        assertEquals(200, request(filter, "/api/auth/login", "192.0.2.3", "POST").getStatus());
        assertEquals(200, request(filter, "/api/auth/register", "192.0.2.3", "GET").getStatus());
    }

    @Test
    void passwordRecoveryRequestsAreRateLimitedPerClient() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(fixedClock());
        for (int i = 0; i < 5; i++) {
            assertEquals(200, request(filter, "/api/auth/forgot-password", "192.0.2.30", "POST").getStatus());
        }
        assertEquals(429, request(filter, "/api/auth/forgot-password", "192.0.2.30", "POST").getStatus());
        assertEquals(200, request(filter, "/api/auth/reset-password", "192.0.2.30", "POST").getStatus());
    }

    private static MockHttpServletResponse request(AuthRateLimitFilter filter, String path,
                                                   String address, String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-10-29T20:00:00Z"), ZoneOffset.UTC);
    }
}
