package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PlatformAccessRequestJpaRepository
        extends
            JpaRepository<PlatformAccessRequestJpaEntity, Long> {

    Optional<PlatformAccessRequestJpaEntity> findByApprovalTokenHash(String approvalTokenHash);

    /**
     * Solicitud viva para ese correo: sin decidir, sin caducar y con margen de
     * intentos.
     *
     * <p>
     * «Una sola solicitud viva por correo» no es expresable como constraint —
     * dependería de {@code NOW()} y MySQL prohíbe funciones no deterministas en la
     * expresión de una columna generada—, y un único parcial sobre «sin decidir»
     * bloquearía para siempre a quien pidió acceso, caducó y vuelve a pedirlo. Se
     * resuelve aquí, y el abuso del buzón lo acota el limitador por correo.
     */
    @Query("""
            SELECT r
            FROM PlatformAccessRequestJpaEntity r
            WHERE r.email = :email
              AND r.decision IS NULL
              AND r.expiresAt > :now
              AND r.verificationAttempts < r.maxAttempts
            ORDER BY r.id DESC
            """)
    List<PlatformAccessRequestJpaEntity> findLivePendingByEmail(@Param("email") String email,
            @Param("now") LocalDateTime now, Limit limit);

    /**
     * Gasta un intento, y solo si queda margen. {@code rowcount = 0} significa que
     * la solicitud ya estaba bloqueada.
     *
     * <p>
     * <b>{@code REQUIRES_NEW} no es decorativo.</b> El servicio lanza la excepción
     * del 422 o del 429 justo después de llamar aquí; sin transacción propia, ese
     * rollback desharía el incremento y el contador de intentos no contaría nada —
     * es decir, la fuerza bruta sobre el millón de códigos quedaría sin freno,
     * silenciosamente y con el test de servicio en verde—.
     *
     * <p>
     * El {@code SET} mueve también {@code version}: {@code @Version} solo protege
     * el ciclo leer-modificar-guardar de una entidad gestionada, y un
     * {@code UPDATE} masivo va directo a la base sin comprobar ni incrementar nada,
     * así que un {@code save} concurrente con la versión vieja casaría igual y
     * pisaría el cambio. La versión va en el {@code SET} y nunca en el
     * {@code WHERE}: condicionarlo por versión haría que actualizara cero filas y
     * el servicio lo leería como «ya estaba bloqueada».
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
            UPDATE platform_access_requests
            SET verification_attempts = verification_attempts + 1, version = version + 1
            WHERE id = :id
              AND verification_attempts < max_attempts
            """, nativeQuery = true)
    int registerFailedAttempt(@Param("id") Long id);

    /**
     * Aplica la decisión, y solo si sigue siendo aplicable. {@code rowcount = 0}
     * significa ya decidida, caducada o bloqueada por una petición concurrente: es
     * lo que separa «aprobar» de «aprobar dos veces» sin depender de una lectura
     * previa, que la concurrencia se come.
     *
     * <p>
     * Mueve {@code version} en el {@code SET} por el mismo motivo que el
     * {@code UPDATE} de arriba.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE platform_access_requests
            SET decision = :decision, decided_at = :now, version = version + 1
            WHERE id = :id
              AND decision IS NULL
              AND expires_at > :now
              AND verification_attempts < max_attempts
            """, nativeQuery = true)
    int applyDecision(@Param("id") Long id, @Param("decision") String decision,
            @Param("now") LocalDateTime now);
}
