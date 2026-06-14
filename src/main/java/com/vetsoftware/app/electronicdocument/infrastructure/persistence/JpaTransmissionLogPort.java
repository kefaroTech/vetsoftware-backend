package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaTransmissionLogPort implements TransmissionLogPort {
    private final ElectronicDocumentTransmissionJpaRepository jpaRepository;

    public JpaTransmissionLogPort(ElectronicDocumentTransmissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void record(Long electronicDocumentId, String provider, Integer httpStatus,
                       String providerDocumentKey, TransmissionResult result, String errorMessage) {
        int attempt = jpaRepository.countByElectronicDocumentId(electronicDocumentId) + 1;
        ElectronicDocumentTransmissionJpaEntity entity = new ElectronicDocumentTransmissionJpaEntity();
        entity.setElectronicDocumentId(electronicDocumentId);
        entity.setProvider(provider);
        entity.setAttempt(attempt);
        entity.setHttpStatus(httpStatus);
        entity.setProviderDocumentKey(providerDocumentKey);
        entity.setResult(result);
        entity.setErrorMessage(errorMessage == null ? null
                : errorMessage.substring(0, Math.min(errorMessage.length(), 2000)));
        entity.setCreatedDate(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Long> findDocumentIdByProviderKey(String providerDocumentKey) {
        return jpaRepository.findFirstByProviderDocumentKeyOrderByIdDesc(providerDocumentKey)
                .map(ElectronicDocumentTransmissionJpaEntity::getElectronicDocumentId);
    }
}
