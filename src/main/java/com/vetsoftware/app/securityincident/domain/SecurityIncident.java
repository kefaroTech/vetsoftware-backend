package com.vetsoftware.app.securityincident.domain;

import java.time.LocalDateTime;

/**
 * Un incidente de seguridad sobre datos personales: cuando se detecto, cuando
 * se escalo, cuando vence el reporte a la autoridad y como acabo.
 *
 * <h2>El plazo se cuenta desde el escalamiento, no desde la deteccion</h2>
 *
 * <p>
 * La Circular Unica de la Superintendencia de Industria y Comercio, Titulo V,
 * numeral 2.1, literal f), romanillo (ii), fija el reporte de incidentes de
 * seguridad en <strong>quince dias habiles</strong>, y el <em>dies a quo</em>
 * no es el instante en que alguien se entera sino el momento en que el
 * incidente <strong>llega al area que lo atiende</strong>: el escalamiento
 * interno. De ahi que {@code escalated_at} sea una columna obligatoria y no un
 * detalle de bitacora.
 *
 * <p>
 * <strong>Contarlo desde {@code detectedAt} da un vencimiento mas
 * largo</strong> que el real —el error cae siempre del lado de incumplir, nunca
 * del de sobrar—, y ese es exactamente el defecto que las dos restricciones del
 * changeset 356 impiden:
 * {@code chk_security_incidents_escalated (escalated_at >= detected_at)} y
 * {@code chk_security_incidents_deadline (deadline_at > escalated_at)}. Este
 * constructor las reproduce las dos.
 *
 * <p>
 * Segunda precision de la misma fuente: el reporte <b>no</b> se hace por el
 * registro nacional de bases de datos, sino por el micrositio de la Delegatura,
 * y no estar inscrito en ese registro no exime de reportar. Por eso aqui no hay
 * ninguna columna ni ninguna invariante que lo mencione.
 *
 * <h2>El vencimiento es un dato, no una formula</h2>
 *
 * <p>
 * {@code deadlineAt} se calcula <em>una vez</em>, al registrar, contra el
 * calendario laboral vigente, y se guarda. No se recalcula al consultar: el
 * listado de lo que esta a punto de incumplirse tiene que poder resolverse por
 * indice ({@code ix_security_incidents_unreported}) sin cargar el calendario, y
 * una fecha que cambia sola cuando alguien corrige un festivo dejaria de ser la
 * prueba de que se aviso a tiempo.
 *
 * <h2>Sin marca de activo, y a proposito</h2>
 *
 * <p>
 * La tabla no lleva {@code enabled}: una prueba que se puede desactivar no
 * prueba nada. Lo que si lleva es {@code version}, porque la fila se reescribe
 * dos veces —al reportar y al cerrar— y dos operadores simultaneos se pisarian
 * sin excepcion y sin log.
 */
public class SecurityIncident {

    /**
     * Quince dias habiles: Circular Unica de la SIC, Titulo V, numeral 2.1, literal
     * f), romanillo (ii).
     *
     * <p>
     * <strong>Habiles, no corridos</strong>, y contados desde el escalamiento. El
     * calculo no vive aqui: lo resuelve el calendario laboral colombiano a traves
     * de {@code BusinessDayDeadlinePort}, para que la correccion de un festivo
     * llegue a la vez a todos los plazos del producto.
     */
    public static final int PLAZO_REPORTE_SIC_DIAS_HABILES = 15;

    private static final int MAX_SUMMARY_LENGTH = 255;
    private static final int MAX_REPORT_REFERENCE_LENGTH = 100;

    private final Long id;
    private final LocalDateTime detectedAt;

    /** Cuando ocurrio de verdad, si se supo. Nunca despues de la deteccion. */
    private final LocalDateTime occurredAt;

    /**
     * El escalamiento interno: cuando el incidente llego al area que lo atiende.
     * <strong>Es el punto de partida del plazo legal.</strong>
     */
    private final LocalDateTime escalatedAt;

    private final SecurityIncidentKind kind;
    private final IncidentSeverity severity;
    private final String summary;

    /**
     * Contador de conveniencia. <strong>La verdad esta en la puente</strong>
     * ({@code security_incident_companies}): aqui vive el total que se declara, no
     * la suma de lo que se registro.
     */
    private final int affectedSubjectCount;

    private final LocalDateTime deadlineAt;
    private final LocalDateTime reportedToAuthorityAt;
    private final String reportReference;

    /**
     * Cuando se informo a los titulares. <strong>Sin plazo legal asociado</strong>:
     * en Colombia la obligacion es informar a la autoridad, no a los titulares. La
     * columna se conserva porque el hecho importa, no porque venza.
     */
    private final LocalDateTime notifiedSubjectsAt;

    private final String containment;
    private final String rootCause;
    private final LocalDateTime closedAt;
    private final LocalDateTime createdDate;
    private final Long version;

    public SecurityIncident(Long id, LocalDateTime detectedAt, LocalDateTime occurredAt,
            LocalDateTime escalatedAt, SecurityIncidentKind kind, IncidentSeverity severity,
            String summary, int affectedSubjectCount, LocalDateTime deadlineAt,
            LocalDateTime reportedToAuthorityAt, String reportReference,
            LocalDateTime notifiedSubjectsAt, String containment, String rootCause,
            LocalDateTime closedAt, LocalDateTime createdDate, Long version) {
        validate(detectedAt, occurredAt, escalatedAt, kind, severity, summary, affectedSubjectCount,
                deadlineAt, reportedToAuthorityAt, reportReference, containment, rootCause,
                closedAt, createdDate);
        this.id = id;
        this.detectedAt = detectedAt;
        this.occurredAt = occurredAt;
        this.escalatedAt = escalatedAt;
        this.kind = kind;
        this.severity = severity;
        this.summary = summary;
        this.affectedSubjectCount = affectedSubjectCount;
        this.deadlineAt = deadlineAt;
        this.reportedToAuthorityAt = reportedToAuthorityAt;
        this.reportReference = reportReference;
        this.notifiedSubjectsAt = notifiedSubjectsAt;
        this.containment = containment;
        this.rootCause = rootCause;
        this.closedAt = closedAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Da de alta el incidente. Nace <b>sin reportar y sin cerrar</b>: las dos cosas
     * ocurren despues y cada una tiene su caso de uso.
     *
     * <p>
     * {@code deadlineAt} llega ya resuelto —el calendario laboral es un hecho
     * externo y no se consulta desde el dominio—, pero se comprueba aqui contra
     * {@code escalatedAt}, que es la invariante que importa.
     */
    public static SecurityIncident register(LocalDateTime detectedAt, LocalDateTime occurredAt,
            LocalDateTime escalatedAt, SecurityIncidentKind kind, IncidentSeverity severity,
            String summary, int affectedSubjectCount, LocalDateTime deadlineAt,
            LocalDateTime createdDate) {
        return new SecurityIncident(null, detectedAt, occurredAt, escalatedAt, kind, severity,
                summary, affectedSubjectCount, deadlineAt, null, null, null, null, null, null,
                createdDate, null);
    }

    /**
     * Anota el reporte a la autoridad con su radicado.
     *
     * <p>
     * <strong>Se niega a reportar dos veces</strong>, y esa negativa es toda la
     * barandilla que hay: {@code chk_security_incidents_report} exige que la fecha
     * y el radicado vayan juntos, pero no impide sobrescribirlos. Sin esta
     * comprobacion el segundo reporte machacaria en silencio la fecha que prueba
     * que se aviso dentro del plazo, que es justamente lo que hay que enseñar si
     * alguien lo discute.
     *
     * <p>
     * Devuelve una instancia nueva <b>conservando la version</b>: es lo que hace
     * que el {@code save} posterior siga siendo un ciclo leer-modificar-guardar con
     * bloqueo optimista y no un insert.
     */
    public SecurityIncident report(LocalDateTime reportedAt, String reference) {
        if (reportedToAuthorityAt != null)
            throw new SecurityIncidentAlreadyReportedException(id, reportedToAuthorityAt);
        return new SecurityIncident(id, detectedAt, occurredAt, escalatedAt, kind, severity,
                summary, affectedSubjectCount, deadlineAt, reportedAt, reference,
                notifiedSubjectsAt, containment, rootCause, closedAt, createdDate, version);
    }

    /**
     * Cierra el incidente escribiendo contencion y causa raiz.
     *
     * <p>
     * Espejo de {@code chk_security_incidents_close}: sin las dos narraciones no se
     * cierra. Un incidente que no se documento en su momento es indistinguible de
     * uno que se oculto, y esa es la unica diferencia que un tercero puede
     * comprobar despues.
     *
     * <p>
     * {@code notifiedSubjectsAt} se escribe aqui porque el cierre es el momento en
     * que el expediente se completa —igual que la puente de afectados, que se
     * escribe una sola vez al cerrar—. Es opcional: la obligacion legal es con la
     * autoridad.
     */
    public SecurityIncident close(LocalDateTime closingAt, String containmentNarrative,
            String rootCauseNarrative, LocalDateTime subjectsNotifiedAt) {
        if (closedAt != null)
            throw new SecurityIncidentAlreadyClosedException(id, closedAt);
        return new SecurityIncident(id, detectedAt, occurredAt, escalatedAt, kind, severity,
                summary, affectedSubjectCount, deadlineAt, reportedToAuthorityAt, reportReference,
                subjectsNotifiedAt, containmentNarrative, rootCauseNarrative, closingAt,
                createdDate, version);
    }

    /** Ya se reporto a la autoridad. */
    public boolean isReported() {
        return reportedToAuthorityAt != null;
    }

    /** Ya se cerro. */
    public boolean isClosed() {
        return closedAt != null;
    }

    /**
     * El plazo vencio sin reporte a esa fecha.
     *
     * <p>
     * Recibe el instante por parametro y no lo consulta: el dominio no tiene reloj
     * —{@code LocalDateTime.now()} aqui dentro haria que el mismo incidente
     * estuviera vencido o no segun el momento del test— y quien pregunta ya sabe
     * contra que momento compara.
     */
    public boolean isOverdue(LocalDateTime at) {
        return !isReported() && at != null && at.isAfter(deadlineAt);
    }

    private static void validate(LocalDateTime detectedAt, LocalDateTime occurredAt,
            LocalDateTime escalatedAt, SecurityIncidentKind kind, IncidentSeverity severity,
            String summary, int affectedSubjectCount, LocalDateTime deadlineAt,
            LocalDateTime reportedToAuthorityAt, String reportReference, String containment,
            String rootCause, LocalDateTime closedAt, LocalDateTime createdDate) {
        if (detectedAt == null)
            throw new IllegalArgumentException("detectedAt is required");
        if (kind == null)
            throw new IllegalArgumentException("kind is required");
        if (severity == null)
            throw new IllegalArgumentException("severity is required");
        validateSummary(summary);
        if (affectedSubjectCount < 0)
            throw new IllegalArgumentException("affectedSubjectCount must not be negative");
        validateTimeline(detectedAt, occurredAt, escalatedAt, deadlineAt);
        validateReport(detectedAt, reportedToAuthorityAt, reportReference);
        validateClosure(detectedAt, containment, rootCause, closedAt);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /** Espejo de la nulabilidad de {@code summary} y de su {@code VARCHAR(255)}. */
    private static void validateSummary(String summary) {
        if (summary == null || summary.isBlank())
            throw new IllegalArgumentException("summary is required");
        if (summary.length() > MAX_SUMMARY_LENGTH)
            throw new IllegalArgumentException("summary must be 255 chars or less");
    }

    /**
     * Espejo de {@code chk_security_incidents_occurred},
     * {@code chk_security_incidents_escalated} y
     * {@code chk_security_incidents_deadline}.
     *
     * <p>
     * <strong>El vencimiento se compara contra el escalamiento y no contra la
     * deteccion.</strong> Es la diferencia entre cumplir y no cumplir: entre que
     * alguien se entere y que el incidente llegue al area que lo atiende pueden
     * pasar dias, y contar el plazo desde el primer instante lo alarga.
     */
    private static void validateTimeline(LocalDateTime detectedAt, LocalDateTime occurredAt,
            LocalDateTime escalatedAt, LocalDateTime deadlineAt) {
        if (occurredAt != null && occurredAt.isAfter(detectedAt))
            throw new IllegalArgumentException("occurredAt must not be after detectedAt");
        if (escalatedAt == null)
            throw new IllegalArgumentException("escalatedAt is required");
        if (escalatedAt.isBefore(detectedAt))
            throw new IllegalArgumentException("escalatedAt must not be before detectedAt");
        if (deadlineAt == null)
            throw new IllegalArgumentException("deadlineAt is required");
        if (!deadlineAt.isAfter(escalatedAt))
            throw new IllegalArgumentException("deadlineAt must be after escalatedAt");
    }

    /**
     * Espejo de {@code chk_security_incidents_report}, con las dos ramas escritas.
     * Sin la segunda, un reporte sin radicado entraria en silencio: un reporte que
     * no se puede rastrear no consta.
     */
    private static void validateReport(LocalDateTime detectedAt,
            LocalDateTime reportedToAuthorityAt, String reportReference) {
        if (reportedToAuthorityAt == null) {
            if (reportReference != null)
                throw new IllegalArgumentException(
                        "reportReference must be absent while the incident is not reported");
            return;
        }
        if (reportReference == null || reportReference.isBlank())
            throw new IllegalArgumentException("reportReference is required once reported");
        if (reportReference.length() > MAX_REPORT_REFERENCE_LENGTH)
            throw new IllegalArgumentException("reportReference must be 100 chars or less");
        if (reportedToAuthorityAt.isBefore(detectedAt))
            throw new IllegalArgumentException(
                    "reportedToAuthorityAt must not be before detectedAt");
    }

    /** Espejo de {@code chk_security_incidents_close}. */
    private static void validateClosure(LocalDateTime detectedAt, String containment,
            String rootCause, LocalDateTime closedAt) {
        if (closedAt == null)
            return;
        if (containment == null || containment.isBlank())
            throw new IllegalArgumentException("containment is required to close an incident");
        if (rootCause == null || rootCause.isBlank())
            throw new IllegalArgumentException("rootCause is required to close an incident");
        if (closedAt.isBefore(detectedAt))
            throw new IllegalArgumentException("closedAt must not be before detectedAt");
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public SecurityIncidentKind getKind() {
        return kind;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public String getSummary() {
        return summary;
    }

    public int getAffectedSubjectCount() {
        return affectedSubjectCount;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public LocalDateTime getReportedToAuthorityAt() {
        return reportedToAuthorityAt;
    }

    public String getReportReference() {
        return reportReference;
    }

    public LocalDateTime getNotifiedSubjectsAt() {
        return notifiedSubjectsAt;
    }

    public String getContainment() {
        return containment;
    }

    public String getRootCause() {
        return rootCause;
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
