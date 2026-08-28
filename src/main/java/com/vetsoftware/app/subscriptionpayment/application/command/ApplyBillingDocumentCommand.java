package com.vetsoftware.app.subscriptionpayment.application.command;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aplicar un origen contra una factura.
 *
 * <p>
 * <strong>Los cuatro campos de referencia son excluyentes entre si</strong>:
 * segun {@code sourceKind} se rellena uno y solo uno, y {@code ROUNDING} y
 * {@code WRITE_OFF} no rellenan ninguno. Lo valida la entidad de dominio y lo
 * vuelve a validar {@code chk_bda_source_exclusive} en la base.
 *
 * @param withholdingId
 *            la retencion practicada, cuando el origen es {@code WITHHOLDING}
 * @param creditEntryId
 *            el lote de saldo a favor del que sale, cuando el origen es
 *            {@code CUSTOMER_CREDIT}
 * @param writeOffAuthorizedBySystemUserId
 *            la firma nominal de plataforma del castigo. <strong>Lo pone el
 *            controller desde el principal y nunca el cuerpo de la
 *            peticion</strong>: si viajara en el JSON, quien castiga la deuda
 *            elegiria a quien atribuirsela
 * @param writeOffReason
 *            el motivo escrito del castigo. Obligatorio en {@code WRITE_OFF} y
 *            prohibido en el resto
 * @param valueDate
 *            cuando el asiento CUENTA, que no es cuando se registra. Opcional:
 *            si no llega, el caso de uso usa el dia de la aplicacion, que es lo
 *            correcto en un pago y en una nota credito. En una retencion no
 *            —practicada el 30 de octubre y registrada el 3 de noviembre
 *            pertenece a octubre—
 * @param clientRequestId
 *            llave de idempotencia (R13). Sin ella, un doble clic o un
 *            reintento del navegador salda la factura dos veces, y R3 no lo
 *            detiene porque mide el total aplicado desde el origen, no cuantas
 *            veces se aplico
 */
public record ApplyBillingDocumentCommand(Long companyId, Long targetDocumentId,
        ApplicationSourceKind sourceKind, Long paymentId, Long sourceDocumentId, Long withholdingId,
        Long creditEntryId, BigDecimal appliedAmount, Long writeOffAuthorizedBySystemUserId,
        String writeOffReason, LocalDate valueDate, String clientRequestId) {

    /**
     * Forma corta para los dos origenes de siempre. Se conserva para que abrir los
     * cuatro nuevos no obligara a propagar cinco nulos por cada llamador de un pago
     * o una nota credito, que son la inmensa mayoria.
     */
    public ApplyBillingDocumentCommand(Long companyId, Long targetDocumentId,
            ApplicationSourceKind sourceKind, Long paymentId, Long sourceDocumentId,
            BigDecimal appliedAmount, String clientRequestId) {
        this(companyId, targetDocumentId, sourceKind, paymentId, sourceDocumentId, null, null,
                appliedAmount, null, null, null, clientRequestId);
    }
}
