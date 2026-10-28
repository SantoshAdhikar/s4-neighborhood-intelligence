package com.s4.neighborhood.security;

import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessAccessGuardTest {
    @Mock BusinessProfileRepository businesses;
    @Mock AccountService accounts;
    @InjectMocks BusinessAccessGuard access;

    @Test
    void allowsOnlyBusinessOwner() {
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(accounts.currentUserId()).thenReturn(ownerId);
        when(businesses.existsByIdAndOwnerUserId(businessId, ownerId)).thenReturn(true);
        assertDoesNotThrow(() -> access.requireOwner(businessId));
    }

    @Test
    void returnsNotFoundForOtherOwnersAndUnclaimedRecords() {
        UUID businessId = UUID.randomUUID();
        UUID visitorId = UUID.randomUUID();
        when(accounts.currentUserId()).thenReturn(visitorId);
        when(businesses.existsByIdAndOwnerUserId(businessId, visitorId)).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class, () -> access.requireOwner(businessId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
