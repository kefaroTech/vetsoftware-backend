package com.vetsoftware.app.companylimitevent.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corregir el consumo de un contador desde plataforma.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin firma.</strong> La empresa entra por la
 * ruta
 * ({@code /system/company-limit-events/companies/{companyId}/usage-adjustments})
 * y quien firma lo pone el servidor con {@code authz.currentSystemUserId()}.
 * Dejar que el cuerpo declarase al firmante sería peor que no firmar: el
 * informe de correcciones seguiría enseñando un nombre, y sería el que el
 * llamador escribió.
 *
 * <p>
 * <strong>{@code delta} puede ser negativo, y ese es su caso principal</strong>
 * — la migración que cargó quinientas mascotas duplicadas se compensa restando.
 * Por eso no lleva {@code @Positive}: acotarlo a números positivos convertiría
 * la válvula de escape de D-12 en una palanca que solo sabe inflar contadores.
 *
 * <p>
 * <strong>El motivo es obligatorio y no tiene valor por defecto.</strong> Es lo
 * único que separa una corrección auditable de un {@code UPDATE} a mano en
 * producción: la cifra tiene que seguir siendo demostrable dentro de un año.
 * Las longitudes espejan los máximos del dominio (30 y 255).
 */
public record AdjustCompanyUsageRequest(
        @NotNull(message = "Debes indicar el eje cuyo consumo se corrige.") Long limitDimensionId,
        @NotBlank(message = "Debes indicar la unidad de capacidad del contador.") @Size(max = 50, message = "La unidad de capacidad no puede superar los 50 caracteres.") String capacityUnit,
        @NotNull(message = "Debes indicar en cuánto se corrige el consumo.") Integer delta,
        @NotBlank(message = "El tipo de motivo es obligatorio.") @Size(max = 30, message = "El tipo de motivo no puede superar los 30 caracteres.") String reasonCode,
        @NotBlank(message = "El motivo de la corrección es obligatorio.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
