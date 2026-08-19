package com.vetsoftware.app.specie.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecieJpaRepository extends JpaRepository<SpecieJpaEntity, Long> {

    // Sube version porque este UPDATE nativo va directo a la base: no comprueba
    // ni incrementa la version, que @Version solo protege en el ciclo
    // leer-modificar-guardar. Un save concurrente cargado antes reescribe la
    // fila entera desde el dominio, con su enabled = false, y su
    // WHERE version = ? casa igual, deshaciendo la reactivacion en silencio.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409 CONCURRENT_MODIFICATION.
    // La version NO va en el WHERE: reactivar es deliberado y debe ejecutarse
    // siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE species
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
