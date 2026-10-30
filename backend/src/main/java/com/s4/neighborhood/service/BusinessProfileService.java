
package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.dto.BusinessProfileResponse;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.mapper.BusinessProfileMapper;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessProfileService {

    private final BusinessProfileRepository repository;
    private final BusinessProfileMapper mapper;
    private final AppUserRepository users;

    public BusinessProfileService(
            BusinessProfileRepository repository,
            BusinessProfileMapper mapper,
            AppUserRepository users
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.users = users;
    }

    @Transactional
    public BusinessProfileResponse create(BusinessProfileRequest request, UUID ownerId) {
        validateRadius(request.researchRadiusMiles());
        // Lock the owning user so simultaneous requests cannot create two workspaces.
        users.lockForWorkspaceCreation(ownerId)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));
        if (repository.existsByOwnerUserId(ownerId)) {
            throw new WorkspaceAlreadyExistsException();
        }

        BusinessProfile business = new BusinessProfile();
        business.setOwnerUserId(ownerId);
        mapper.updateEntity(business, request);

        return mapper.toResponse(repository.saveAndFlush(business));
    }

    @Transactional(readOnly = true)
    public List<BusinessProfileResponse> list(UUID ownerId) {
        return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerId)
                .stream().map(mapper::toResponse).toList();
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
        if (radius == null || (radius != 2 && radius != 3 && radius != 4 && radius != 10)) {
            throw new IllegalArgumentException(
                    "Research radius must be 2, 3, 4, or 10 miles"
            );
        }
    }
}
