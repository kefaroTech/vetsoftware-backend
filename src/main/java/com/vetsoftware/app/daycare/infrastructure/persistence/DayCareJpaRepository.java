package com.vetsoftware.app.daycare.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DayCareJpaRepository extends JpaRepository<DayCareJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    List<DayCareJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<DayCareJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<DayCareJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"animal", "company"})
    @Query("""
            SELECT x
            FROM DayCareJpaEntity x
            WHERE x.animal.id = :animalId
              AND x.company.id = :companyId
              AND (:q IS NULL OR :q = '' OR LOWER(x.objects) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.observations) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<DayCareJpaEntity> findAllByAnimalIdAndCompanyId(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * Un UPDATE por id a secas reactivaba el registro borrado de cualquier tenant
     * para quien conociera el id, porque no hay ninguna lectura previa que valide
     * la propiedad — el servicio decide si existe mirando las filas afectadas.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: una consulta nativa va directa a la base de datos, asi que ni
     * comprueba ni incrementa la version, y el candado queda ciego ante este
     * camino. Sin el bump, un save cargado antes reescribe {@code enabled} con su
     * valor viejo (el mapper copia la fila entera desde el dominio) y su
     * {@code WHERE version = ?} casa igual, con lo que una edicion concurrente
     * deshace la reactivacion en silencio. Movida la version, ese save ya no
     * encuentra fila y salta {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}, para que el front recargue y reintente sobre
     * datos frescos. {@code version} NO va en el {@code WHERE}: reactivar es una
     * operacion deliberada y debe ejecutarse siempre, no competir con una edicion.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE daycares
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
