package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request;

import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y aqui no hace falta ni siquiera como
 * {@code @RequestParam}.</strong> Una caida no apunta a una clinica: apunta a
 * varias, y el reparto va por su propio endpoint con la empresa en la ruta.
 *
 * @param startedAt
 *            cuando empezo la interrupcion. <b>Es un dato observado, no el
 *            reloj del servidor</b>: la caida se detecta despues de haber
 *            empezado, y sellarla con la hora de la peticion acortaria la
 *            interrupcion medida —siempre en la direccion de parecer mejores—
 * @param causeParty
 *            quien la causo. Separa un incidente de un incumplimiento propio, y
 *            ademas decide la unicidad: <b>solo cabe una caida abierta por
 *            causante</b>
 * @param affectedCompanyCount
 *            primera estimacion del alcance, antes de que exista una sola fila
 *            de reparto. Cero es legitimo
 * @param externalIncidentRef
 *            el radicado del proveedor. Opcional al abrir —rara vez lo dan en
 *            caliente— y es lo que traslada la responsabilidad con nombre y
 *            numero
 */
public record OpenExternalInvoicingOutageRequest(
        @NotNull(message = "Debes indicar cuando empezo la caida.") @Schema(description = "Instante observado del inicio, no el reloj del servidor.") LocalDateTime startedAt,
        @NotNull(message = "Debes indicar quien causo la caida.") CauseParty causeParty,
        @NotBlank(message = "El resumen es obligatorio.") @Size(max = 255, message = "El resumen no puede superar los 255 caracteres.") String summary,
        @PositiveOrZero(message = "El numero de empresas alcanzadas no puede ser negativo.") int affectedCompanyCount,
        @Size(max = 100, message = "El radicado del proveedor no puede superar los 100 caracteres.") String externalIncidentRef) {
}
