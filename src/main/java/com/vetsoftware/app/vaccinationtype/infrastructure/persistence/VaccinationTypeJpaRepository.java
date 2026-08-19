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

    /**
     * Lectura ESTRICTA por propiedad, para los caminos de ESCRITURA. A diferencia
     * de {@link #findAvailableById}, que incluye a propósito las filas generales
     * porque sirve a los {@code find}/{@code list}, esta excluye lo que la empresa
     * solo puede consultar: editar, borrar o reactivar una fila general la
     * cambiaría para todos los tenants, y una fila general ajena la reasignaría.
     */
    @EntityGraph(attributePaths = "company")
    Optional<VaccinationTypeJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * En reactivación no hay lectura previa que valide la propiedad — el servicio
     * decide si existe mirando las filas afectadas —, así que un UPDATE por id a
     * secas revivía el tipo retirado de cualquier tenant. Cero filas = «no existe
     * en TU empresa» → 404.
     * <p>
     * Y sube {@code version} porque este UPDATE nativo va directo a la base: no
     * comprueba ni incrementa la version, que {@code @Version} solo protege en el
     * ciclo leer-modificar-guardar. Un {@code save} concurrente cargado antes
     * reescribe la fila entera desde el dominio, con su {@code enabled = false}, y
     * su {@code WHERE version = ?} casa igual, deshaciendo la reactivacion en
     * silencio. Movida la version, ese save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. La {@code version} NO va en el
     * {@code WHERE}: reactivar es deliberado y debe ejecutarse siempre, no competir
     * con una edicion.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE vaccination_types
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
