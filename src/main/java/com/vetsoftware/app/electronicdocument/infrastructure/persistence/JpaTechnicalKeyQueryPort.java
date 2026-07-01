package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.TechnicalKeyQueryPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaEntity;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link TechnicalKeyQueryPort}: lee la clave técnica de la resolución activa (misma que asigna la
 * numeración). Único cruce permitido de vertical slicing (persistence → persistence de numberingresolution).
 */
@Component
public class JpaTechnicalKeyQueryPort implements TechnicalKeyQueryPort {
    private final NumberingResolutionJpaRepository repository;

    public JpaTechnicalKeyQueryPort(NumberingResolutionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> findActiveTechnicalKey(Long companyId, ElectronicDocumentType documentType) {
        return repository.findActive(companyId, documentType.name())
                .map(NumberingResolutionJpaEntity::getTechnicalKey)
                .filter(key -> key != null && !key.isBlank());
    }
}
