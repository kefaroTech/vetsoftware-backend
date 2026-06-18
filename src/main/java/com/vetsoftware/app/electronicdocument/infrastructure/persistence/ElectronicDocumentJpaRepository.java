package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
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

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByCufeAndCompany_Id(String cufe, Long companyId);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByOpenAccountIdAndCompany_Id(Long openAccountId, Long companyId);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    List<ElectronicDocumentJpaEntity> findByDianStatus(DianStatus dianStatus);

    boolean existsByOpenAccountId(Long openAccountId);
}
