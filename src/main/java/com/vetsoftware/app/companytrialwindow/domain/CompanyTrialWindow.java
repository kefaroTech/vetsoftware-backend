package com.vetsoftware.app.companytrialwindow.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * El reloj de la empresa: una ventana de prueba concedida, con su principio y
 * su fin.
 *
 * <p>
 * <strong>Vive fuera del contrato a propósito.</strong> Si viviera dentro,
 * cancelar el contrato y firmar otro abriría ventana nueva: el mismo abuso con
 * un rodeo de dos clics.
 *
 * <p>
 * <strong>El fin es inclusivo y no se elige: se calcula.</strong>
 * {@code end = start + días − 1}. El documento de diseño cayó en su propia
 * trampa aquí —decía que catorce días desde el día cero vencen el catorce, que
 * son quince— y el código anterior sumaba los días sin restar uno. Con la clave
 * foránea triple que cuelga de las concesiones, ese desfase de un día deja de
 * ser un informe raro y pasa a ser un error del motor a mitad de un alta
 * comercial. Por eso esta clase <strong>no acepta</strong> un {@code endDate}
 * de fuera en su factory: lo deriva.
 *
 * <p>
 * <strong>La ventana no se estira jamás</strong> (R-TRIAL-10, D-54). Esta clase
 * no expone ningún {@code extend(...)} ni ningún mutador de {@code windowDays},
 * y esa ausencia es la regla: no hay operación que amplíe una ventana viva. La
 * segunda mitad la impone el motor, con {@code ON UPDATE RESTRICT} en la clave
 * de las concesiones —mover el fin con pruebas colgando muere en la base—. Si
 * comercial quiere dar otra oportunidad, cierra esta y abre otra, que queda
 * registrada.
 *
 * <p>
 * <strong>Una abierta, no una en la vida.</strong> Lo impone el índice único
 * sobre la columna generada {@code open_window_marker}, que vale la empresa
 * mientras {@code closed_at} esté vacío. Una campaña de recuperación puede
 * abrir otra años después, y seguirá sin poder regalar dos veces el mismo
 * artículo: de eso se ocupa la unicidad de las concesiones, no esta tabla.
 */
public class CompanyTrialWindow {

    private final Long id;
    private final Long companyId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int windowDays;
    private final Long sourceQuoteId;
    private final LocalDateTime closedAt;
    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyTrialWindow(Long id, Long companyId, LocalDate startDate, LocalDate endDate,
            int windowDays, Long sourceQuoteId, LocalDateTime closedAt, LocalDateTime createdDate,
            Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (startDate == null)
            throw new IllegalArgumentException("start date is required");
        if (endDate == null)
            throw new IllegalArgumentException("end date is required");
        if (sourceQuoteId == null)
            throw new IllegalArgumentException("source quote id is required");
        // chk_company_trial_windows_days
        if (windowDays <= 0)
            throw new IllegalArgumentException("window days must be greater than zero");
        // chk_company_trial_windows_end: el fin es inclusivo.
        LocalDate expectedEnd = lastDayOf(startDate, windowDays);
        if (!expectedEnd.equals(endDate))
            throw new IllegalArgumentException("end date must be " + expectedEnd
                    + " (start + windowDays - 1, last day included) but was " + endDate);
        // chk_company_trial_windows_closed
        if (closedAt != null && closedAt.toLocalDate().isBefore(startDate))
            throw new IllegalArgumentException("closed at cannot precede the start date");
        this.id = id;
        this.companyId = companyId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.windowDays = windowDays;
        this.sourceQuoteId = sourceQuoteId;
        this.closedAt = closedAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Abre la ventana. El fin no es un parámetro: se deriva de la duración
     * concedida, con el último día incluido.
     */
    public static CompanyTrialWindow open(Long companyId, LocalDate startDate, int windowDays,
            Long sourceQuoteId, LocalDateTime createdDate) {
        return new CompanyTrialWindow(null, companyId, startDate, lastDayOf(startDate, windowDays),
                windowDays, sourceQuoteId, null, createdDate, null);
    }

    /**
     * El último día de una ventana, incluido. Es la misma cuenta que usan las
     * concesiones, y por eso está escrita una vez: si las dos convenciones se
     * separan, el desfase se multiplica por módulo y por cliente.
     */
    public static LocalDate lastDayOf(LocalDate startDate, int days) {
        if (startDate == null)
            throw new IllegalArgumentException("start date is required");
        if (days <= 0)
            throw new IllegalArgumentException("days must be greater than zero");
        return startDate.plusDays(days - 1L);
    }

    /**
     * Cierra la ventana. No la acorta ni la borra: escribe la fecha en que dejó de
     * estar abierta, que es lo que libera el marcador de «una abierta por empresa».
     */
    public CompanyTrialWindow close(LocalDateTime at) {
        if (at == null)
            throw new IllegalArgumentException("closed at is required");
        if (closedAt != null)
            throw new TrialWindowAlreadyClosedException(companyId, closedAt);
        return new CompanyTrialWindow(id, companyId, startDate, endDate, windowDays, sourceQuoteId,
                at, createdDate, version);
    }

    /** Vacío en {@code closedAt} = ventana viva. */
    public boolean isOpen() {
        return closedAt == null;
    }

    /**
     * Si el día cae dentro de la ventana <em>y</em> la ventana sigue abierta.
     *
     * <p>
     * R-TRIAL-09: el cierre vive en otra columna y una restricción de fila no puede
     * mirarlo, así que esta comprobación es código y no motor. Añadir un módulo el
     * día 35 de una ventana de 30 entra pagando, no en prueba.
     */
    public boolean admitsGrantOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        return isOpen() && !day.isBefore(startDate) && !day.isAfter(endDate);
    }

    /**
     * Los días que quedan desde ese día, contando el propio día y el último.
     *
     * <p>
     * Es la mitad de la regla que impide encadenar años de software gratis: un
     * módulo añadido el día 15 de una ventana de 30 recibe 15, no 30.
     *
     * @return cero si el día ya pasó del fin de la ventana
     */
    public int remainingDaysFrom(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        if (day.isAfter(endDate))
            return 0;
        LocalDate from = day.isBefore(startDate) ? startDate : day;
        return (int) ChronoUnit.DAYS.between(from, endDate) + 1;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    /** Último día en prueba, incluido. */
    public LocalDate getEndDate() {
        return endDate;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public Long getSourceQuoteId() {
        return sourceQuoteId;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
