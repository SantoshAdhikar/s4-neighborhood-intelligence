package com.s4.neighborhood.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebsiteResearchServiceTest {
    @Test
    void refusesNonHttpsOrLocalhostUrls() {
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("http://example.com"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://localhost/"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://127.0.0.1/"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://example.com:8443/"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://user@example.com"));
    }

    @Test
    void refusesIpv6LoopbackOrPrivateAddress() {
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://[::1]/"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://192.168.1.1/"));
        assertThrows(Exception.class, () -> WebsiteResearchService.checkedUri("https://10.0.0.4/"));
    }
}
