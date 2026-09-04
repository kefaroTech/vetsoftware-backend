package com.vetsoftware.app.taxreturn.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * La presentacion de la declaracion.
 *
 * <p>
 * <strong>Sin fecha de presentacion y sin firmante</strong>: la primera la pone
 * el caso de uso con su {@code Clock} inyectado —aceptarla por HTTP dejaria
 * antedatar una presentacion— y el segundo sale de
 * {@code authz.currentSystemUserId()}.
 *
 * @param firmezaUntil
 *            hasta cuando pueden revisarla. <strong>Llega como dato y no se
 *            calcula</strong> porque depende de si Lumbre compensa perdidas
 *            fiscales —tres años (art. 714 ET) o cinco—, y esa pregunta sigue
 *            abierta para un contador. De esta fecha cuelga la ventana de
 *            conservacion de todos los soportes que la declaracion sostiene
 */
public record FileTaxReturnRequest(
        @NotBlank(message = "Debes indicar el radicado.") @Size(max = 100, message = "El radicado no puede superar los 100 caracteres.") String receiptRef,
        @NotBlank(message = "Debes indicar donde esta la copia de lo presentado.") @Size(max = 255, message = "La referencia del fichero no puede superar los 255 caracteres.") String fileRef,
        @NotNull(message = "Debes indicar hasta cuando queda en firme.") @Schema(description = "Estrictamente posterior a la fecha de presentacion.") LocalDate firmezaUntil) {
}
