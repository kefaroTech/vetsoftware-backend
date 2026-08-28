package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * El hecho externo: la factura que emitio el tercero.
 *
 * <p>
 * <strong>No lleva {@code status} ni {@code difference}, y eso no es una
 * omision.</strong> Los dos los calcula el dominio a partir de los importes:
 * dejar que el cliente enviara el estado convertiria la regla de los dos pesos
 * en una sugerencia, y dejarle enviar la diferencia permitiria escribir una
 * resta que no cuadra con sus propios sumandos —lo que
 * {@code chk_eir_difference} rechazaria mas tarde y como un error de integridad
 * sin explicacion—.
 *
 * <p>
 * Los cuatro campos de la resolucion de numeracion son opcionales pero van
 * juntos: la regla completa la valida el dominio, que es donde vive.
 */
public record MatchExternalInvoiceRequest(
        @NotBlank(message = "Debes indicar el numero de la factura externa.") @Size(max = 60, message = "El numero de la factura externa no puede superar los 60 caracteres.") String externalInvoiceId,
        @Size(max = 100, message = "El CUFE no puede superar los 100 caracteres.") String externalCufe,
        @NotNull(message = "El total de la factura externa es obligatorio.") @PositiveOrZero(message = "El total de la factura externa no puede ser negativo.") BigDecimal externalTotal,
        @NotNull(message = "El impuesto de la factura externa es obligatorio.") @PositiveOrZero(message = "El impuesto de la factura externa no puede ser negativo.") BigDecimal externalTax,
        @Size(max = 60, message = "El numero de resolucion no puede superar los 60 caracteres.") String externalResolutionNumber,
        @Positive(message = "El inicio del rango debe ser mayor que cero.") Integer externalRangeFrom,
        @Positive(message = "El fin del rango debe ser mayor que cero.") Integer externalRangeTo,
        LocalDate resolutionValidUntil) {
}
