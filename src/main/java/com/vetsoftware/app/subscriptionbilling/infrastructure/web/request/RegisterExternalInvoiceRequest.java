package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * La referencia de la factura emitida <b>fuera</b> de Lumbre.
 *
 * <p>
 * {@code issuedAt} es la <b>fecha fiscal</b> y es obligatoria: es la única
 * desde la que se cuenta el vencimiento. El {@code cufe} es opcional a
 * propósito —a veces llega en un segundo paso—, y exigirlo aquí bloquearía el
 * registro legítimo de una factura recién emitida.
 *
 * <p>
 * <b>Quién registra NO viaja aquí, y es el mismo motivo por el que no viaja la
 * empresa.</b> {@code external_registered_by_system_user_id} es el rastro del
 * paso manual: si el id llegara en el cuerpo, quien captura la referencia
 * elegiría a quién atribuírsela, y reclamar el paso manual que no se hizo sería
 * imposible. Un rastro de auditoría que escribe el propio auditado no es un
 * rastro de auditoría. Lo pone el controller con
 * {@code authz.currentSystemUserId()}.
 *
 * <p>
 * Nada de esto tiene que ver con la facturación electrónica DIAN que la clínica
 * le emite a los dueños de las mascotas: eso sigue siendo parte del producto,
 * vive en {@code electronicdocument} y tiene su propia numeración.
 */
public record RegisterExternalInvoiceRequest(@NotBlank @Size(max = 60) String invoiceNumber,
        @Size(max = 100) String cufe, @NotNull LocalDate issuedAt,
        @NotBlank @Size(max = 40) String provider) {
}
