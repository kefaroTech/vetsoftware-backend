package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationProgressNoteJpaRepository
        extends
            JpaRepository<HospitalizationProgressNoteJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationProgressNoteJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationProgressNoteJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Page<HospitalizationProgressNoteJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);

    /**
     * El filtro por empresa no es defensa en profundidad: es LA defensa. Un UPDATE
     * por id a secas resucitaba la nota de evolucion borrada de cualquier tenant
     * para quien conociera el id, porque en este caso de uso no hay ninguna lectura
     * previa que valide la propiedad — el servicio decide si existe mirando las
     * filas afectadas.
     *
     * <p>
     * La empresa no cuelga de {@code hospitalization_progress_notes}: cuelga de la
     * hospitalizacion padre, asi que el filtro viaja por un {@code EXISTS} contra
     * {@code hospitalizations}, que es la misma ruta que usa
     * {@code findByIdAndHospitalization_Company_Id}.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE hospitalization_progress_notes n
            SET n.enabled = true
            WHERE n.id = :id
              AND EXISTS (SELECT 1
                          FROM hospitalizations h
                          WHERE h.id = n.hospitalization_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
