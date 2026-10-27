
package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.dto.BusinessProfileResponse;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.mapper.BusinessProfileMapper;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class BusinessProfileService {

    private final BusinessProfileRepository repository;
    private final BusinessProfileMapper mapper;

    public BusinessProfileService(
            BusinessProfileRepository repository,
            BusinessProfileMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public BusinessProfileResponse create(BusinessProfileRequest request) {
        validateRadius(request.researchRadiusMiles());

        BusinessProfile business = new BusinessProfile();
        mapper.updateEntity(business, request);

        return mapper.toResponse(repository.saveAndFlush(business));
    }

    @Transactional(readOnly = true)
    public BusinessProfileResponse get(UUID id) {
        return mapper.toResponse(findBusiness(id));
    }

    @Transactional
    public BusinessProfileResponse update(
            UUID id,
            BusinessProfileRequest request
    ) {
        validateRadius(request.researchRadiusMiles());

        BusinessProfile business = findBusiness(id);
        mapper.updateEntity(business, request);

        return mapper.toResponse(repository.saveAndFlush(business));
    }

    private BusinessProfile findBusiness(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Business profile not found: " + id
                ));
    }

    private void validateRadius(Integer radius) {
        if (radius == null || (radius != 4 && radius != 10)) {
            throw new IllegalArgumentException(
                    "Research radius must be 4 or 10 miles"
            );
        }
    }
}
