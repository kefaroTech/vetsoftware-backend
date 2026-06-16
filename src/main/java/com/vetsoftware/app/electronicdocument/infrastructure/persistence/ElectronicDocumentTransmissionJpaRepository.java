package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectronicDocumentTransmissionJpaRepository
        extends JpaRepository<ElectronicDocumentTransmissionJpaEntity, Long> {

    int countByElectronicDocumentId(Long electronicDocumentId);

    Optional<ElectronicDocumentTransmissionJpaEntity>
            findFirstByProviderDocumentKeyOrderByIdDesc(String providerDocumentKey);

    Optional<ElectronicDocumentTransmissionJpaEntity>
            findFirstByElectronicDocumentIdAndProviderDocumentKeyNotNullOrderByIdDesc(Long electronicDocumentId);
}
