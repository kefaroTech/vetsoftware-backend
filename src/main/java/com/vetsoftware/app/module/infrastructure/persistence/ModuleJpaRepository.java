package com.vetsoftware.app.module.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleJpaRepository extends JpaRepository<ModuleJpaEntity, Long> {

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
            UPDATE modules
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
