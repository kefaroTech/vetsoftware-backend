package com.vetsoftware.app.companytrialwindow.application.command;

import java.time.LocalDate;

/**
 * Abrir la ventana de prueba de una empresa.
 *
 * <p>
 * <strong>No lleva fecha de fin</strong>, y no es un descuido: el fin se deriva
 * de la duración con el último día incluido. Aceptarlo de fuera sería abrir la
 * puerta a la ventana de 45 días escrita a mano que D-54 prohíbe.
 */
public record OpenTrialWindowCommand(Long companyId, LocalDate startDate, int windowDays,
        Long sourceQuoteId) {
}
