package com.vetsoftware.app.role.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<RoleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<RoleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "company.membership"})
    List<RoleJpaEntity> findAllByCode(String code);

    @EntityGraph(attributePaths = "company")
    List<RoleJpaEntity> findAllByCompanyId(Long companyId);
}
