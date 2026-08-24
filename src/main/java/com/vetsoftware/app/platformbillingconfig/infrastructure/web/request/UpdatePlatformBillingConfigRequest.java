package com.vetsoftware.app.platformbillingconfig.infrastructure.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario de políticas de facturación de la plataforma.
 *
 * <p>
 * No lleva {@code companyId} ni podría llevarlo: la configuración es global de
 * plataforma y el endpoint está cerrado a {@code SYSTEM}. Tampoco lleva
 * {@code enabled} (la tabla no lo tiene) ni ningún interruptor de corte de
 * acceso (el máximo del producto es solo lectura).
 *
 * <p>
 * Las restricciones de aquí son la primera barrera y devuelven un 400 con el
 * campo marcado; las mismas reglas viven además en el constructor del dominio y
 * en los {@code CHECK} de la tabla, que son las que de verdad mandan.
 */
public record UpdatePlatformBillingConfigRequest(

        Long defaultPriceListId,

        @NotNull(message = "Los días de gracia son obligatorios.") @Min(value = 0, message = "Los días de gracia no pueden ser negativos.") Integer defaultGraceDays,

        @NotNull(message = "Los días de prueba son obligatorios.") @Min(value = 0, message = "Los días de prueba no pueden ser negativos.") Integer defaultTrialDays,

        @NotNull(message = "El día de emisión es obligatorio.") @Min(value = 1, message = "El día de emisión debe estar entre 1 y 28.") @Max(value = 28, message = "El día de emisión debe estar entre 1 y 28: los días 29, 30 y 31"
                + " no existen en todos los meses.") Integer invoiceDayOfMonth,

        @NotNull(message = "El plazo de pago es obligatorio.") @Min(value = 0, message = "El plazo de pago no puede ser negativo; cero significa pago"
                + " inmediato.") Integer defaultPaymentTermDays,

        @Size(max = 40, message = "El proveedor de facturación externa no puede superar los 40"
                + " caracteres.") String externalBillingProvider) {
}
