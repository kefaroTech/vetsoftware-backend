package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationProcedureJpaRepository
        extends
            JpaRepository<HospitalizationProcedureJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationProcedureJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationProcedureJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Page<HospitalizationProcedureJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);

    /**
     * El filtro por empresa no es defensa en profundidad: es LA defensa. Un UPDATE
     * por id a secas reactivaba la orden borrada de cualquier tenant para quien
     * conociera el id, porque en este caso de uso no hay ninguna lectura previa que
     * valide la propiedad — el servicio decide si existe mirando las filas
     * afectadas.
     *
     * <p>
     * La empresa no cuelga de {@code hospitalization_procedures}: cuelga de la
     * hospitalizacion padre, asi que el filtro viaja por un {@code EXISTS} contra
     * {@code hospitalizations}, que es la misma ruta que usa
     * {@code findByIdAndHospitalization_Company_Id}.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: una consulta nativa no la comprueba ni la incrementa, asi que un
     * save cargado antes de la reactivacion reescribia la fila entera desde el
     * dominio —el mapper la copia— y su {@code WHERE version = ?} casaba igual,
     * deshaciendo en silencio el {@code enabled = true}. Movida la version, ese
     * save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el {@code WHERE}:
     * reactivar es deliberado y debe ejecutarse siempre, no competir con una
     * edicion. La version que sube es la de la propia orden, no la de la
     * hospitalizacion padre, que el EXISTS solo lee.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE hospitalization_procedures p
            SET p.enabled = true, p.version = p.version + 1
            WHERE p.id = :id
              AND EXISTS (SELECT 1
                          FROM hospitalizations h
                          WHERE h.id = p.hospitalization_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
