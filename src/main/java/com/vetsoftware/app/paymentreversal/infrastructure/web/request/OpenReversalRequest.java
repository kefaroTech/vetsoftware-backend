package com.vetsoftware.app.paymentreversal.infrastructure.web.request;

import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y no es un olvido.</strong> Ningun
 * {@code @RequestBody} del proyecto puede declararlo: la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} lo prohibe <em>sin excepcion</em>, y no
 * la hace ni siquiera para un endpoint de plataforma. Como este es un endpoint
 * SYSTEM y un principal SYSTEM no tiene empresa propia, la empresa que se elige
 * viaja como <em>query param</em> —el mismo patron que
 * {@code SystemSubscriptionPaymentController}—, nunca dentro del JSON.
 *
 * @param deadlineAt
 *            plazo de resolucion. Obligatorio y sin valor por defecto: el
 *            termino legal se cuenta en dias habiles y depende de la causal,
 *            asi que calcularlo aqui seria inventarse una fecha con
 *            consecuencias juridicas
 * @param causal
 *            una de las cinco tasadas. Obligatoria salvo cuando el origen es un
 *            contracargo de pasarela, que a veces llega sin causal legible
 */
public record OpenReversalRequest(
        @NotNull(message = "Debes indicar el pago que se reversa.") Long paymentId,
        @NotNull(message = "Debes indicar el origen de la reversion.") ReversalOrigin origin,
        ReversalCausal causal,
        @NotNull(message = "Debes indicar la calificacion del reclamante.") ConsumerDetermination consumerDetermination,
        LocalDateTime consumerBecameAwareAt,
        @NotNull(message = "Debes indicar cuando llego la queja.") LocalDateTime claimReceivedAt,
        LocalDateTime issuerNotifiedAt,
        @Size(max = 255, message = "La constancia de la queja no puede superar los 255 caracteres.") String claimEvidenceRef,
        @NotNull(message = "Debes indicar el plazo de resolucion.") LocalDateTime deadlineAt) {
}
