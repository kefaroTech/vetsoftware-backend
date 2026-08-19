package com.vetsoftware.app.owner.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OwnerJpaRepository extends JpaRepository<OwnerJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    List<OwnerJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    Optional<OwnerJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"city", "company"})
    @Query("""
            SELECT o
            FROM OwnerJpaEntity o
            WHERE o.id = :id
              AND o.company.id = :companyId
            """)
    Optional<OwnerJpaEntity> findByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    // Las asociaciones del grafo son to-one, así que el JOIN FETCH convive con la
    // paginación sin traerse la tabla a memoria. Con una colección habría que
    // separar la consulta o Hibernate paginaría en el heap (HHH000104).
    @EntityGraph(attributePaths = {"city", "company"})
    @Query("""
            SELECT o
            FROM OwnerJpaEntity o
            WHERE o.company.id = :companyId
            """)
    Page<OwnerJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "company"})
    @Query("""
            SELECT o
            FROM OwnerJpaEntity o
            WHERE o.company.id = :companyId
              AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.document) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<OwnerJpaEntity> searchByCompanyAndTerm(@Param("companyId") Long companyId,
            @Param("query") String query, Pageable pageable);

    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // de la reactivacion reescribe `enabled` con su valor
    // viejo —el mapper lo copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente vuelve a apagar en silencio lo que la
    // reactivacion acababa de encender. Movida la version,
    // ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // reactivar es una operacion deliberada y debe
    // ejecutarse siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE owners
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByCity_Id(Long cityId);

    boolean existsByCompany_Id(Long companyId);
}
