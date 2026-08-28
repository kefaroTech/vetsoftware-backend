package com.vetsoftware.app.publicholiday.application.command;

import java.time.LocalDate;

/**
 * Alta de un festivo. Solo la plataforma la ejecuta, asi que no lleva
 * {@code companyId}: la tabla no tiene empresa y el gate del puerto es
 * {@code hasRole('SYSTEM')} a secas.
 */
public record CreatePublicHolidayCommand(LocalDate holidayDate, String name, LocalDate nominalDate,
        boolean moved, String legalReference) {
}
