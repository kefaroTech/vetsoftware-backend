package com.vetsoftware.app.companytrialgrant.domain;

import java.time.LocalDate;

/**
 * La ventana de la empresa vista desde la concesión. Companion VO: el dominio
 * de {@code companytrialwindow} no se importa.
 *
 * <p>
 * <strong>Lleva la empresa dentro a propósito.</strong> Sin ella, la prueba de
 * una clínica podría colgar de la ventana de otra y heredar un techo ajeno —el
 * mismo defecto que la primera auditoría encontró seis veces en el bloque del
 * dinero—. El motor lo impone con una clave foránea triple
 * {@code (company_id, trial_window_id, trial_window_end_date)}; esta copia es
 * lo que permite al dominio comprobarlo antes de llegar al motor.
 *
 * @param open
 *            si la ventana sigue abierta. El cierre vive en otra columna y una
 *            restricción de fila no puede mirarlo: por eso R-TRIAL-09 es código
 *            y no motor.
 */
public record TrialWindowRef(Long id, Long companyId, LocalDate startDate, LocalDate endDate,
        boolean open) {

    public TrialWindowRef {
        if (id == null)
            throw new IllegalArgumentException("trial window id is required");
        if (companyId == null)
            throw new IllegalArgumentException("trial window company id is required");
        if (startDate == null)
            throw new IllegalArgumentException("trial window start date is required");
        if (endDate == null)
            throw new IllegalArgumentException("trial window end date is required");
    }

    /** Si el día cae dentro de la ventana y la ventana sigue abierta. */
    public boolean admitsGrantOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        return open && !day.isBefore(startDate) && !day.isAfter(endDate);
    }
}
