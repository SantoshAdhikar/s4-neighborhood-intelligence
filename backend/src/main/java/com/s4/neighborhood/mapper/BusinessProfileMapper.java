
package com.s4.neighborhood.mapper;

import com.s4.neighborhood.dto.BusinessProfileRequest;
import com.s4.neighborhood.dto.BusinessProfileResponse;
import com.s4.neighborhood.entity.BusinessProfile;
import org.springframework.stereotype.Component;

@Component
public class BusinessProfileMapper {

    public void updateEntity(
            BusinessProfile entity,
            BusinessProfileRequest request
    ) {
        entity.setName(request.name().trim());
        entity.setIndustry(request.industry().trim());
        entity.setStreetAddress(request.streetAddress().trim());
        entity.setCity(request.city().trim());
        entity.setState(request.state().trim());
        entity.setPostalCode(request.postalCode().trim());
        entity.setWebsite(
                request.website() == null || request.website().isBlank()
                        ? null : request.website().trim()
        );
        entity.setServices(request.services().trim());
        entity.setPrimaryGoal(request.primaryGoal().trim());
        entity.setResearchRadiusMiles(request.researchRadiusMiles());
    }

    public BusinessProfileResponse toResponse(BusinessProfile entity) {
        return new BusinessProfileResponse(
                entity.getId(),
                entity.getName(),
                entity.getIndustry(),
                entity.getStreetAddress(),
                entity.getCity(),
                entity.getState(),
                entity.getPostalCode(),
                entity.getWebsite(),
                entity.getServices(),
                entity.getPrimaryGoal(),
                entity.getResearchRadiusMiles(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
