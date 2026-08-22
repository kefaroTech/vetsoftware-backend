package com.vetsoftware.app.company.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    List<CompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    Optional<CompanyJpaEntity> findById(Long id);

    // Las dos asociaciones del grafo son to-one, así que el JOIN FETCH convive con
    // la paginación sin traerse la tabla a memoria. Con una colección habría que
    // separar la consulta o Hibernate paginaría en el heap (HHH000104).
    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    Page<CompanyJpaEntity> findAll(Pageable pageable);

    /**
     * La empresa propia servida como página de una fila. Existe para que el
     * adaptador tenga una sola forma de responder —{@code Page} siempre— y no tenga
     * que fabricar a mano los metadatos de la página del empleado: con
     * {@code page=1} la consulta devuelve contenido vacío y {@code totalElements=1}
     * sola, sin aritmética que equivocar.
     */
    @EntityGraph(attributePaths = {"city", "membership"})
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE c.id = :companyId
            """)
    Page<CompanyJpaEntity> findPageByCompanyId(@Param("companyId") Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"city", "membership"})
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.identifier) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<CompanyJpaEntity> searchByTerm(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "membership"})
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE c.id = :companyId
              AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.identifier) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CompanyJpaEntity> searchByCompanyAndTerm(@Param("companyId") Long companyId,
            @Param("query") String query, Pageable pageable);

    boolean existsByIdentifier(String identifier);

}
