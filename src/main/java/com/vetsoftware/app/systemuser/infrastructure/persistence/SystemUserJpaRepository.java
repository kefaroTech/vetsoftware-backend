package com.vetsoftware.app.systemuser.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SystemUserJpaRepository extends JpaRepository<SystemUserJpaEntity, Long> {

    Optional<SystemUserJpaEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM SystemUserJpaEntity u
            WHERE u.id = :id
            """)
    Optional<SystemUserJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // Invalida los access tokens vivos del usuario de sistema:
    // el filtro rechaza todo JWT cuyo claim authVersion no
    // coincida con esta columna.
    //
    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // del bump reescribe auth_version con su valor viejo
    // —el mapper la copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente revalida en silencio una sesion ya revocada.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // revocar es deliberado y debe ejecutarse siempre.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE system_users
            SET auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int bumpAuthVersion(@Param("id") Long id);

    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // del bump reescribe auth_version con su valor viejo
    // —el mapper la copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente revalida en silencio las sesiones que la
    // reactivacion acababa de tumbar. Movida la version, ese
    // save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // dar de baja o reactivar es una operacion administrativa
    // deliberada y debe ejecutarse siempre.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE system_users
            SET enabled = true, auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
