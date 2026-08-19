package com.vetsoftware.app.company.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    List<CompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    Optional<CompanyJpaEntity> findById(Long id);

    boolean existsByIdentifier(String identifier);

    /**
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
            UPDATE companies
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
