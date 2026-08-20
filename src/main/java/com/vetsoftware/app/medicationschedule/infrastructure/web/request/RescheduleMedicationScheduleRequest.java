package com.vetsoftware.app.medicationschedule.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * {@code mode} era {@code String} sin restricciones: llegaba crudo al caso de
 * uso pese al {@code @Valid} y todo lo que no fuese {@code "cascade"} degradaba
 * a «solo esta toma» sin decirlo (#134). Como enum, un valor desconocido lo
 * rechaza el deserializador y sale un 400 {@code MALFORMED_REQUEST}.
 *
 * <p>
 * {@code ACCEPT_CASE_INSENSITIVE_VALUES} es transitorio: los fronts desplegados
 * envian {@code "one"}/{@code "cascade"} en minusculas —la comparacion vieja
 * era {@code equalsIgnoreCase}— y sin esto un backend nuevo delante de un front
 * viejo responderia 400 a cada arrastre de dosis. Se quita cuando los dos
 * fronts envien el valor del contrato.
 */
public record RescheduleMedicationScheduleRequest(@NotNull LocalDateTime newDateTime,
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) RescheduleMode mode) {
}
