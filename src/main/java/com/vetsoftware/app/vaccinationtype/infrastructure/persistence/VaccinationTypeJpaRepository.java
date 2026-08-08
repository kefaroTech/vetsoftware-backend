package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationTypeJpaRepository
        extends
            JpaRepository<VaccinationTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<VaccinationTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<VaccinationTypeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @org.springframework.data.jpa.repository.Query("""
            SELECT e
            FROM VaccinationTypeJpaEntity e
            LEFT
            JOIN e.company c
            WHERE e.id = :id
              AND (e.general = true OR c.id = :companyId)
            """)
    Optional<VaccinationTypeJpaEntity> findAvailableById(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @EntityGraph(attributePaths = "company")
    List<VaccinationTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE vaccination_types
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
