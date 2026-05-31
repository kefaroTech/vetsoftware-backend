package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureScheduleJpaRepository
        extends JpaRepository<ProcedureScheduleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    Optional<ProcedureScheduleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    List<ProcedureScheduleJpaEntity> findByHospitalizationProcedureId(Long hospitalizationProcedureId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE procedure_schedules SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
