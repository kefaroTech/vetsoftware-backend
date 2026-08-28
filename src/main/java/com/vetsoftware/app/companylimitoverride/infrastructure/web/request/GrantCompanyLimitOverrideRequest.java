package com.vetsoftware.app.companylimitoverride.infrastructure.web.request;

import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Negociar una excepción de techo para una empresa.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin firma.</strong> La empresa entra por la
 * ruta ({@code /system/company-limit-overrides/companies/{companyId}}) y quien
 * concede lo pone el servidor con {@code authz.currentSystemUserId()}. Dejar
 * que el cuerpo declarase al firmante sería peor que no firmar: el informe de
 * excepciones seguiría enseñando un nombre, y sería el que el llamador
 * escribió.
 *
 * <p>
 * <strong>El motivo es obligatorio y no tiene valor por defecto</strong>, y esa
 * es una decisión, no una restricción heredada: dentro de seis meses el informe
 * de a quién se le ha hecho una excepción tiene que poder leerse. La longitud
 * espeja el máximo del dominio (255).
 */
public record GrantCompanyLimitOverrideRequest(
        @NotNull(message = "Debes indicar el eje sobre el que se negocia la excepción.") Long limitDimensionId,
        @NotNull(message = "Debes indicar el techo negociado.") @PositiveOrZero(message = "El techo negociado no puede ser negativo.") Integer limitQuantity,
        @NotNull(message = "Debes indicar desde cuándo rige la excepción.") LocalDate validFrom,
        @NotNull(message = "Debes indicar el tipo de motivo.") OverrideReasonCode reasonCode,
        @NotBlank(message = "El motivo de la excepción es obligatorio.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
