package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/**
 * Alta de linea. Genera su otrosi {@code ADD_ITEM} y comprueba el solape antes
 * de abrir. {@code clientRequestId} es la llave antiduplicados: se busca antes
 * de insertar, dentro de la transaccion.
 *
 * <p>
 * <strong>No trae importes NI SNAPSHOTS.</strong> El prorrateo y el cambio de
 * la cuota recurrente los calcula el caso de uso, y desde R-QUOTE-02 tampoco
 * viajan el precio, el nombre, el tipo, la unidad, el IVA ni lo incluido: la
 * linea es una {@link RequestedSubscriptionItemCommand}, es decir SELECCION
 * COMERCIAL —que articulo, cuantos y entre que fechas— y nada mas. Que no esten
 * aqui es lo que garantiza que ningun caller —ni el controller, ni otro
 * servicio futuro— pueda dictarlos: mientras el tipo de este campo admitiera
 * importes, abrir una linea a cero pesos era un campo de formulario.
 */
public record AddSubscriptionItemCommand(Long id, Long companyId, String clientRequestId,
        LocalDate effectiveDate, String reason, Long requestedByEmployeeId,
        Long requestedBySystemUserId, Long quoteId, RequestedSubscriptionItemCommand line) {
}
