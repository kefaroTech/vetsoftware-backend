package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web.request;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <strong>Sin {@code companyId}.</strong> Lo prohibe la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que mira todo {@code @RequestBody} sin
 * mirar la ruta ni el rol. El unico endpoint que lo usa vive en
 * {@code SystemBillingDocumentStatusHistoryController} y recibe la empresa como
 * {@code @RequestParam}: quien escribe es tesoreria, que no tiene empresa
 * propia y la elige. La proteccion no es que el servidor la inyecte —no puede—
 * sino que el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Sin {@code occurredAt} tampoco</strong>, y esta ausencia es la que se
 * discute menos y cuesta mas: es la columna por la que se ordena la pelicula y
 * por la que se corta a una fecha. Aceptarla del cliente permitiria antedatar
 * un movimiento y con ello reescribir cuantos documentos estaban esperando
 * factura externa a 31 de marzo. La pone el reloj del servidor.
 *
 * <p>
 * <strong>Las restricciones de aqui son un filtro, no la regla.</strong> La de
 * verdad —que el estado de origen sea distinto del de destino— vive en el
 * constructor de {@code BillingDocumentStatusHistory}, porque es una verdad de
 * la transicion y vale aunque nadie llame a este endpoint; y ademas es una
 * condicion cruzada entre dos campos, que no cabe en una anotacion de campo. Lo
 * que Bean Validation aporta es rechazar el disparate evidente antes de tocar
 * la base y devolverlo con el nombre del campo, que es lo que el formulario del
 * front necesita para señalar donde.
 *
 * @param actor
 *            quien movio el documento: el nombre de una persona o el del
 *            proceso automatico. Es texto y no un id a proposito — un proceso
 *            no tiene fila en {@code system_users}
 * @param reason
 *            por que se movio, en lenguaje que se entienda seis meses despues:
 *            «Factura externa FE-1043 registrada», «anulado por nota credito
 *            NC-77»
 */
public record RecordBillingDocumentStatusChangeRequest(
        @NotNull(message = "Debes indicar el documento de cobro que cambio de estado.") Long billingDocumentId,
        @NotNull(message = "Debes indicar el estado del que venia el documento.") BillingDocumentStatus fromStatus,
        @NotNull(message = "Debes indicar el estado al que paso el documento.") BillingDocumentStatus toStatus,
        @NotBlank(message = "Debes indicar quien movio el documento.") @Size(max = 120, message = "El actor no puede superar 120 caracteres.") String actor,
        @NotBlank(message = "El motivo del cambio es obligatorio.") @Size(max = 255, message = "El motivo no puede superar 255 caracteres.") String reason) {
}
