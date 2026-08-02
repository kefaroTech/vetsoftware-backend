package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DianProviderConfigJpaRepository
    extends JpaRepository<DianProviderConfigJpaEntity, Long> {

  @EntityGraph(attributePaths = "company")
  Optional<DianProviderConfigJpaEntity> findByCompany_Id(Long companyId);
}
