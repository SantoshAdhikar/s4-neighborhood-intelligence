
package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.mapper.BusinessProfileMapper;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessProfileServiceTest {

    @Mock
    private BusinessProfileRepository repository;

    @Spy
    private BusinessProfileMapper mapper = new BusinessProfileMapper();

    @InjectMocks
    private BusinessProfileService service;

    private BusinessProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new BusinessProfileRequest(
                "Golden Brows",
                "Beauty & Personal Care",
                "123 Main Street",
                "Paramount",
                "CA",
                "90723",
                "https://example.com",
                "Eyebrow threading, waxing",
                "Reach more nearby customers",
                4
        );
    }

    @Test
    void createsBusinessWithValidRadius() {
        when(repository.saveAndFlush(any(BusinessProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(validRequest, UUID.randomUUID());

        assertEquals("Golden Brows", response.name());
        assertEquals(4, response.researchRadiusMiles());
        verify(repository).saveAndFlush(any(BusinessProfile.class));
    }

    @Test
    void rejectsUnsupportedRadius() {
        var request = new BusinessProfileRequest(
                "Golden Brows",
                "Beauty",
                "123 Main Street",
                "Paramount",
                "CA",
                "90723",
                "",
                "Threading",
                "Grow locally",
                7
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, UUID.randomUUID())
        );

        verifyNoInteractions(repository);
    }

    @Test
    void returnsNotFoundForUnknownBusiness() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.get(id)
        );
    }

    @Test
    void retrievesExistingBusiness() {
        UUID id = UUID.randomUUID();
        BusinessProfile business = new BusinessProfile();
        mapper.updateEntity(business, validRequest);

        when(repository.findById(id)).thenReturn(Optional.of(business));

        var response = service.get(id);

        assertEquals("Golden Brows", response.name());
        assertEquals("Paramount", response.city());
    }

    @Test
    void updatesExistingBusiness() {
        UUID id = UUID.randomUUID();
        BusinessProfile business = new BusinessProfile();

        when(repository.findById(id)).thenReturn(Optional.of(business));
        when(repository.saveAndFlush(business)).thenReturn(business);

        var response = service.update(id, validRequest);

        assertEquals("Golden Brows", response.name());
        verify(repository).saveAndFlush(business);
    }
}
