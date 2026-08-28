package com.vetsoftware.app.publicholiday.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de un festivo.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>, y aqui ni siquiera por la regla del
 * tenant: la tabla es de plataforma y su gate es {@code hasRole('SYSTEM')}.
 *
 * <p>
 * {@code holidayDate} es la fecha <em>observada</em>. {@code nominalDate} es la
 * efemeride antes del traslado de la Ley 51 de 1983 y solo es obligatoria
 * cuando {@code moved} es cierto; el dominio comprueba las dos ramas, igual que
 * {@code chk_public_holidays_move}.
 */
public record CreatePublicHolidayRequest(@NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 120) String name, LocalDate nominalDate, @NotNull Boolean moved,
        @NotBlank @Size(max = 255) String legalReference) {
}
