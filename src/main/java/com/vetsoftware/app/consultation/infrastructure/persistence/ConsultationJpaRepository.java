package com.vetsoftware.app.consultation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationJpaRepository extends JpaRepository<ConsultationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    List<ConsultationJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"consultationType", "animal", "company"})
    Optional<ConsultationJpaEntity> findById(Long id);
}
