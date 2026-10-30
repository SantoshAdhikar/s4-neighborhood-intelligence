package com.s4.neighborhood.repository;

import com.s4.neighborhood.entity.BusinessClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessClaimRepository extends JpaRepository<BusinessClaim, UUID> {
    Optional<BusinessClaim> findByBusinessId(UUID businessId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BusinessClaim> findByIdentitySha256(String identityHash);
}
