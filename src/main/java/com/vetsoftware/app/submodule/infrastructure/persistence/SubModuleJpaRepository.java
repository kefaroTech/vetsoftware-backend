package com.vetsoftware.app.submodule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubModuleJpaRepository extends JpaRepository<SubModuleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "module")
    List<SubModuleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "module")
    Optional<SubModuleJpaEntity> findById(Long id);
}
