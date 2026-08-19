package com.vetsoftware.app.medicament.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MedicamentJpaRepository extends JpaRepository<MedicamentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<MedicamentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @org.springframework.data.jpa.repository.Query("""
            SELECT e
            FROM MedicamentJpaEntity e
            LEFT
            JOIN e.company c
            WHERE e.id = :id
              AND (e.general = true OR c.id = :companyId)
            """)
    Optional<MedicamentJpaEntity> findAvailableById(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    /**
     * Estrictamente el medicamento PROPIO de la empresa. Distinto de
     * {@link #findAvailableById}, que ademas devuelve los generales: para leer y
     * recetar sirve el catalogo disponible, pero escribir —editar, borrar,
     * reactivar— solo puede alcanzar lo que la empresa creo. Un general
     * ({@code company_id} NULL) es de la plataforma y no lo toca ningun tenant.
     */
    @EntityGraph(attributePaths = "company")
    Optional<MedicamentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    // Native: los pausados (enabled = false) NO pasan el @SQLRestriction; se listan
    // crudos para
    // reactivar.
    @Query(value = """
            SELECT *
            FROM medicaments
            WHERE enabled = false
              AND company_id = :companyId
            """, nativeQuery = true)
    List<MedicamentJpaEntity> findAllDisabledForCompany(@Param("companyId") Long companyId);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * En la reactivacion no hay lectura previa que valide la propiedad —el servicio
     * decide si existe mirando las filas afectadas—, asi que un UPDATE por id a
     * secas resucitaba el medicamento pausado de cualquier tenant para quien
     * conociera el id.
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
     * edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medicaments
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id, @Param("companyId") Long companyId);
}
