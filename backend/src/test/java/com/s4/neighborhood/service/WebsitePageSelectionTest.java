package com.s4.neighborhood.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebsitePageSelectionTest {
    @Test
    void selectsBoundedSameSitePagesAndLeavesExternalBookingProviderAlone() {
        var page = Jsoup.parse("""
                <a href='/services'>Services</a>
                <a href='/appointments'>Book Appointment</a>
                <a href='/prices'>Prices</a>
                <a href='/contact'>Contact</a>
                <a href='https://external-booking.example/appointments'>External booking</a>
                <a href='/cart'>Cart</a>
                <a href='/downloads/menu.pdf'>Download</a>
                """, "https://salon.example/");
        var links = WebsiteResearchService.detailLinks(page, 4);
        assertEquals(4, links.size());
        assertEquals("SERVICES", links.get(0).type());
        assertEquals("BOOKING", links.get(1).type());
        assertEquals("PRICING", links.get(2).type());
        assertEquals("CONTACT", links.get(3).type());
        assertTrue(links.stream().noneMatch(item -> item.url().contains("external-booking")));
    }

    @Test
    void doesNotInventPagesWhenLinksAreMissing() {
        var page = Jsoup.parse("<a href='/'>Home</a>", "https://shop.example/");
        assertTrue(WebsiteResearchService.detailLinks(page, 4).isEmpty());
    }
}
