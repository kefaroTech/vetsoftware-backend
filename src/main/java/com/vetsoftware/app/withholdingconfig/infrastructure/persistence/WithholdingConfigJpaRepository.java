package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithholdingConfigJpaRepository
    extends JpaRepository<WithholdingConfigJpaEntity, Long> {

  @EntityGraph(attributePaths = "company")
  Optional<WithholdingConfigJpaEntity> findByCompany_Id(Long companyId);
}
