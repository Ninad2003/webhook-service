package com.project.webhook_service.repository;

import com.project.webhook_service.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    Optional<Partner> findByPartnerId(String partnerId);

    boolean existsByPartnerId(String partnerId);
}
