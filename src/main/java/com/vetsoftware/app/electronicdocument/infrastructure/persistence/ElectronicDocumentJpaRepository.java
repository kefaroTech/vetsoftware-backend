package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectronicDocumentJpaRepository extends JpaRepository<ElectronicDocumentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    List<ElectronicDocumentJpaEntity> findByCompanyId(Long companyId);
}
