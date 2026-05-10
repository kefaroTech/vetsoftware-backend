package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryTypeJpaRepository extends JpaRepository<SurgeryTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<SurgeryTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<SurgeryTypeJpaEntity> findById(Long id);
}
