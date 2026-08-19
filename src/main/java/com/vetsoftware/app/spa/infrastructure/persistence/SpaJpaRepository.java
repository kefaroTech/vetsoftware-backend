package com.vetsoftware.app.spa.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpaJpaRepository extends JpaRepository<SpaJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    List<SpaJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    @Query("""
            SELECT x
            FROM SpaJpaEntity x
            WHERE x.animal.id = :animalId
              AND x.company.id = :companyId
              AND (:q IS NULL OR :q = '' OR LOWER(x.reason) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.details) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.observations) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<SpaJpaEntity> findAllByAnimalIdAndCompanyId(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * Un UPDATE por id a secas reactivaba el registro borrado de cualquier tenant
     * para quien conociera el id, porque no hay ninguna lectura previa que valide
     * la propiedad — el servicio decide si existe mirando las filas afectadas.
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
            UPDATE spas
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsBySpaType_Id(Long spaTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
