package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalEventJpaRepository
        extends
            JpaRepository<ClinicalEventViewJpaEntity, String> {

    @Query("""
            SELECT e
            FROM ClinicalEventViewJpaEntity e
            WHERE e.animalId = :animalId
              AND e.companyId = :companyId
              AND e.eventType IN :types
              AND (:from IS NULL OR e.eventDate >= :from)
              AND (:to IS NULL OR e.eventDate <= :to)
              AND (:q IS NULL OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.eventDate DESC, e.sourceId DESC
            """)
    List<ClinicalEventViewJpaEntity> findHistory(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("types") List<ClinicalEventType> types,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("q") String q);

    /**
     * BE-06: la misma consulta, paginada, para la pantalla. La vista no tiene
     * colecciones, asi que Hibernate pagina en SQL sin producto cartesiano. El
     * orden desempata por sourceId para que la paginacion sea determinista.
     */
    @Query("""
            SELECT e
            FROM ClinicalEventViewJpaEntity e
            WHERE e.animalId = :animalId
              AND e.companyId = :companyId
              AND e.eventType IN :types
              AND (:from IS NULL OR e.eventDate >= :from)
              AND (:to IS NULL OR e.eventDate <= :to)
              AND (:q IS NULL OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.eventDate DESC, e.sourceId DESC
            """)
    Page<ClinicalEventViewJpaEntity> findHistoryPage(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("types") List<ClinicalEventType> types,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM ClinicalEventViewJpaEntity e
            WHERE e.companyId = :companyId
              AND e.eventType IN :types
              AND (:from IS NULL OR e.eventDate >= :from)
              AND (:to IS NULL OR e.eventDate <= :to)
            ORDER BY e.eventDate ASC, e.sourceId ASC
            """)
    List<ClinicalEventViewJpaEntity> findByCompany(@Param("companyId") Long companyId,
            @Param("types") List<ClinicalEventType> types, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
