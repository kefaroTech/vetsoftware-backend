package com.vetsoftware.app.companytrialwindow.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Abrir el reloj de prueba de una empresa.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: la empresa entra por la ruta
 * ({@code /system/company-trial-windows/companies/{companyId}}) y no por el
 * cuerpo, que es lo que exige {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}. Tampoco se
 * deriva del principal, porque quien abre ventanas es plataforma y un usuario
 * de plataforma no tiene empresa.
 *
 * <p>
 * <strong>Y sin fecha de fin</strong>, que es la restricción que sostiene toda
 * la capa: el fin se deriva de la duración con el último día incluido
 * ({@code start + windowDays - 1}). Aceptarlo de fuera abriría la puerta a la
 * ventana de 45 días escrita a mano que D-54 prohíbe, y además ese desfase
 * muere después contra la clave foránea triple de las concesiones.
 */
public record OpenTrialWindowRequest(
        @NotNull(message = "Debes indicar el día en que empieza la prueba.") LocalDate startDate,
        @NotNull(message = "Debes indicar cuántos días dura la prueba.") @Positive(message = "La prueba tiene que durar al menos un día.") Integer windowDays,
        @NotNull(message = "Debes indicar la cotización que originó la prueba.") Long sourceQuoteId) {
}
