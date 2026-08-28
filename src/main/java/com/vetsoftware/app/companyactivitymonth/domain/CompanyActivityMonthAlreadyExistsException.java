package com.vetsoftware.app.companyactivitymonth.domain;

/**
 * Ya hay una fila de actividad para ese par empresa-mes:
 * {@code uq_cam_month (company_id, period_key)}.
 *
 * <p>
 * <strong>Este choque llega desde la base, no de una comprobacion
 * previa.</strong> Un {@code exists} antes del {@code insert} lo pasarian
 * <em>las dos</em> peticiones concurrentes —o los dos reintentos del proceso
 * nocturno— y la segunda escribiria igual; la unicidad del motor es lo unico
 * que serializa. Por eso el alta no pregunta: intenta escribir y traduce la
 * violacion de integridad a esta excepcion, que es la unica respuesta que no
 * miente.
 *
 * <p>
 * Y la salida correcta no es reintentar el alta: el mes ya existe, asi que lo
 * que toca es <b>recalcularlo</b> ({@code UpdateCompanyActivityMonthUseCase}),
 * que es el camino que respeta el bloqueo optimista.
 */
public class CompanyActivityMonthAlreadyExistsException extends RuntimeException {

    public CompanyActivityMonthAlreadyExistsException(Long companyId, String periodKey) {
        super("Company " + companyId + " already has an activity row for period " + periodKey
                + ": the month is recalculated over itself, not inserted twice");
    }

    public CompanyActivityMonthAlreadyExistsException(Long companyId, String periodKey,
            Throwable cause) {
        super("Company " + companyId + " already has an activity row for period " + periodKey
                + ": the month is recalculated over itself, not inserted twice", cause);
    }
}
