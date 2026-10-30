package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AppUser;
import com.s4.neighborhood.entity.BusinessClaim;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.repository.AppUserRepository;
import com.s4.neighborhood.repository.BusinessClaimRepository;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessClaimServiceTest {
    @Mock BusinessClaimRepository claims;
    @Mock BusinessProfileRepository businesses;
    @Mock AppUserRepository users;
    @InjectMocks BusinessClaimService service;

    @Test
    void emailVerifiedIsRequiredAndCannotBeSubstitutedByHavingBusinessId() {
        UUID userId = UUID.randomUUID(), businessId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(new AppUser()));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requestReview(businessId, userId));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(claims);
    }

    @Test
    void registeredFirstNeverGrantsVerifiedOwnership() {
        UUID userId = UUID.randomUUID(), businessId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setEmailVerified(true);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(businesses.findById(businessId)).thenReturn(Optional.of(business(businessId, userId)));
        when(claims.findByBusinessId(businessId)).thenReturn(Optional.empty());
        when(claims.findByIdentitySha256(any())).thenReturn(List.of());
        when(claims.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("PENDING_REVIEW", service.requestReview(businessId, userId).status());
    }

    @Test
    void sameBusinessClaimFromAnotherWorkspaceCreatesConflictButNoTransfer() {
        UUID ram = UUID.randomUUID(), shyam = UUID.randomUUID(), businessId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setEmailVerified(true);
        when(users.findById(ram)).thenReturn(Optional.of(user));
        when(businesses.findById(businessId)).thenReturn(Optional.of(business(businessId, ram)));
        when(claims.findByBusinessId(businessId)).thenReturn(Optional.empty());
        BusinessClaim existing = new BusinessClaim();
        existing.setBusinessId(UUID.randomUUID());
        existing.setClaimantUserId(shyam);
        existing.setStatus("PENDING_REVIEW");
        when(claims.findByIdentitySha256(any())).thenReturn(List.of(existing));
        when(claims.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("CONFLICT_REVIEW", service.requestReview(businessId, ram).status());
        assertEquals("CONFLICT_REVIEW", existing.getStatus());
        verify(businesses, never()).save(any()); // No business ownership changed.
    }

    @Test
    void similarNameAtDifferentAddressDoesNotProduceSameIdentityKey() {
        var left = business(UUID.randomUUID(), UUID.randomUUID());
        var right = business(UUID.randomUUID(), UUID.randomUUID());
        right.setStreetAddress("999 Elsewhere Lane");
        assertNotEquals(BusinessClaimService.identity(left), BusinessClaimService.identity(right));
    }

    private static BusinessProfile business(UUID id, UUID owner) {
        BusinessProfile business = new BusinessProfile();
        business.setOwnerUserId(owner);
        business.setName("ABC Business");
        business.setStreetAddress("123 Main St");
        business.setCity("Paramount");
        business.setState("CA");
        business.setPostalCode("90723");
        return business;
    }
}
