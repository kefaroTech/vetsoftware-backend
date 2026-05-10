package com.vetsoftware.app.surgery.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryJpaRepository extends JpaRepository<SurgeryJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"surgeryType", "animal", "consultation", "company"})
    List<SurgeryJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"surgeryType", "animal", "consultation", "company"})
    Optional<SurgeryJpaEntity> findById(Long id);
}
