package com.vetsoftware.app.platformaccess.application.port.out;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persistencia de la solicitud. Ningun metodo lleva empresa en el nombre ni en
 * la firma: este flujo es global de plataforma y sus tablas no tienen
 * {@code company_id}.
 *
 * <p>
 * Los dos metodos de escritura condicional NO son azucar sobre un
 * {@code save()}: son {@code UPDATE} atomicos cuyo {@code WHERE} <b>es</b> la
 * invariante. Contar intentos con leer-comprobar-guardar es un
 * <i>read-then-write</i>: dos peticiones simultaneas con el codigo equivocado
 * leen 4, escriben 5 las dos, y se han gastado 6 intentos.
 */
public interface PlatformAccessRequestRepository {

    PlatformAccessRequest save(PlatformAccessRequest request);

    Optional<PlatformAccessRequest> findById(Long id);

    Optional<PlatformAccessRequest> findByApprovalTokenHash(String approvalTokenHash);

    /**
     * Solicitud viva (sin decidir, sin caducar y sin bloquear) para ese correo. Es
     * lo que hace idempotente el formulario sin necesitar un indice unico parcial,
     * que MySQL no puede expresar aqui porque dependeria de {@code NOW()}.
     */
    Optional<PlatformAccessRequest> findLivePendingByEmail(String email, LocalDateTime now);

    /**
     * Incrementa el contador SOLO si queda margen.
     *
     * @return filas afectadas; {@code 0} significa que ya estaba bloqueada.
     */
    int registerFailedAttempt(Long id);

    /**
     * Aplica la decision SOLO si sigue siendo aplicable.
     *
     * @return filas afectadas; {@code 0} significa ya decidida, caducada o
     *         bloqueada por una peticion concurrente.
     */
    int applyDecision(Long id, PlatformAccessDecision decision, LocalDateTime now);
}
