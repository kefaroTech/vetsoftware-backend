package com.vetsoftware.app.appointment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"animal", "owner", "employee", "company", "branch"})
    Optional<AppointmentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"animal", "owner", "employee", "company", "branch"})
    @Query("""
            SELECT a
            FROM AppointmentJpaEntity a
            WHERE a.company.id = :companyId
              AND (:from IS NULL OR a.startAt >= :from)
              AND (:to IS NULL OR a.startAt <= :to)
              AND (:employeeId IS NULL OR a.employee.id = :employeeId)
              AND (:status IS NULL OR a.status = :status)
              AND (:branchId IS NULL OR a.branch.id = :branchId)
            ORDER BY a.startAt ASC
            """)
    List<AppointmentJpaEntity> findByFilters(@Param("companyId") Long companyId,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("employeeId") Long employeeId, @Param("status") String status,
            @Param("branchId") Long branchId);

    @Query("""
            SELECT a.id
            FROM AppointmentJpaEntity a
            WHERE a.company.id = :companyId
              AND a.employee.id = :employeeId
              AND a.startAt = :startAt
              AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    List<Long> findClashingIds(@Param("companyId") Long companyId,
            @Param("employeeId") Long employeeId, @Param("startAt") LocalDateTime startAt,
            @Param("excludeId") Long excludeId);

    // Soft-delete por query nativa (evita el conflicto @SQLDelete + @Version en
    // em.remove).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE appointments
            SET enabled = false
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);
}
