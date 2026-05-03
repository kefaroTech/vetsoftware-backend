package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalizationJpaRepository extends JpaRepository<HospitalizationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    List<HospitalizationJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<HospitalizationJpaEntity> findById(Long id);
}
