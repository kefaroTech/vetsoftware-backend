package com.vetsoftware.app.publicholiday.domain;

import java.time.LocalDate;

/**
 * Ya hay un festivo con esa fecha observada. Mapea a 409.
 *
 * <p>
 * Es el espejo de {@code uq_public_holidays_date}. Dejar que lo cazara la base
 * daria un 500 con un mensaje del driver que no nombra ni la columna ni el ano.
 */
public class PublicHolidayAlreadyExistsException extends RuntimeException {

    public PublicHolidayAlreadyExistsException(LocalDate holidayDate) {
        super("Public holiday already exists for date: " + holidayDate);
    }
}
