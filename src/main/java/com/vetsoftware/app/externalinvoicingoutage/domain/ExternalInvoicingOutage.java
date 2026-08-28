package com.vetsoftware.app.externalinvoicingoutage.domain;

import java.time.LocalDateTime;

/**
 * Una caida de la emision fiscal electronica: cuando empezo, cuando termino,
 * quien la causo y a cuantas clinicas alcanzo.
 *
 * <p>
 * Existe para responder dos preguntas que hoy no tienen donde mirarse. La del
 * cliente —«¿por que no me salio la factura el martes?»— y la de la autoridad
 * —«¿por que hay un hueco en la numeracion?»—. Sin la ficha las dos se
 * contestan de memoria, y la segunda de memoria no se contesta.
 *
 * <h2>Catalogo global: aqui no hay empresa, y es a proposito</h2>
 *
 * <p>
 * Una caida es un hecho de la plataforma, no de una clinica: la sufren varias a
 * la vez y su causante es el mismo para todas. El reparto por clinica vive en
 * la puente {@link ExternalInvoicingOutageCompany}. Por eso no hay
 * {@code companyId} en esta clase ni columna {@code company_id} en la tabla, y
 * por eso {@code ExternalInvoicingOutageJpaEntity} no alcanza
 * {@code CompanyJpaEntity} por ninguna asociacion: si la alcanzara, las cuatro
 * reglas duras de aislamiento de BE-COV caerian sobre la feature entera.
 *
 * <h2>La caida nace abierta, y eso es lo que la hace util</h2>
 *
 * <p>
 * {@code endedAt} es nulo mientras dura —que es <em>cuando alguien va a
 * preguntar</em>—, y estrictamente posterior a {@code startedAt} en cuanto se
 * cierra ({@code chk_eio_ended}). Una caida que solo se pudiera registrar ya
 * terminada llegaria siempre tarde para lo unico que sirve: contarle al cliente
 * que lo sabemos antes de que llame.
 *
 * <h2>La unicidad que Java no puede cuidar</h2>
 *
 * <p>
 * <strong>Una sola caida abierta por causante.</strong> Lo impone la columna
 * generada {@code open_outage_marker} —que vale {@code cause_party} mientras
 * {@code ended_at} es nulo y {@code NULL} en cuanto se cierra— con
 * {@code uq_eio_open} encima. <b>Esa columna no se mapea</b> y no se comprueba
 * aqui, y las dos cosas son deliberadas: un {@code exists} previo en el service
 * seria una comprobacion que dos peticiones concurrentes pasarian las dos, y el
 * proceso de deteccion abriria una caida nueva en cada sondeo dejando un rastro
 * de caidas vivas que nunca se cierran. El duplicado llega como violacion de
 * integridad, que es la unica respuesta que no miente.
 *
 * <p>
 * Dos causantes distintos <em>si</em> pueden solaparse —el emisor y la red—, y
 * por eso el marcador lleva {@link CauseParty} y no una constante.
 */
public class ExternalInvoicingOutage {

    private static final int MAX_SUMMARY_LENGTH = 255;
    private static final int MAX_EXTERNAL_INCIDENT_REF_LENGTH = 100;

    private final Long id;
    private final LocalDateTime startedAt;

    /** Nulo mientras la caida sigue viva. Es lo que alimenta la generada. */
    private final LocalDateTime endedAt;

    private final CauseParty causeParty;
    private final String summary;
    private final int affectedCompanyCount;
    private final LocalDateTime notifiedCompaniesAt;

    /**
     * El radicado del proveedor: traslada la responsabilidad con nombre y numero.
     */
    private final String externalIncidentRef;

    private final LocalDateTime createdDate;
    private final Long version;

    public ExternalInvoicingOutage(Long id, LocalDateTime startedAt, LocalDateTime endedAt,
            CauseParty causeParty, String summary, int affectedCompanyCount,
            LocalDateTime notifiedCompaniesAt, String externalIncidentRef,
            LocalDateTime createdDate, Long version) {
        validate(startedAt, endedAt, causeParty, summary, affectedCompanyCount, notifiedCompaniesAt,
                externalIncidentRef, createdDate);
        this.id = id;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.causeParty = causeParty;
        this.summary = summary;
        this.affectedCompanyCount = affectedCompanyCount;
        this.notifiedCompaniesAt = notifiedCompaniesAt;
        this.externalIncidentRef = externalIncidentRef;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Abre una caida. Nace <b>viva</b> —sin fecha de fin—, sin aviso cursado y sin
     * version: la asigna Hibernate al insertar.
     *
     * <p>
     * El contador de alcanzadas entra por parametro y no se deduce de la puente: en
     * el momento de abrir todavia no hay ninguna fila hija, y la primera estimacion
     * —«esto le esta pegando a las cuarenta»— es justo el dato que hace util la
     * ficha antes de que nadie haya repartido nada.
     */
    public static ExternalInvoicingOutage open(LocalDateTime startedAt, CauseParty causeParty,
            String summary, int affectedCompanyCount, String externalIncidentRef,
            LocalDateTime createdDate) {
        return new ExternalInvoicingOutage(null, startedAt, null, causeParty, summary,
                affectedCompanyCount, null, externalIncidentRef, createdDate, null);
    }

    /**
     * Cierra la caida poniendo la hora de fin.
     *
     * <p>
     * <strong>Se niega a cerrar lo que ya estaba cerrado</strong>, y esa negativa
     * es toda la barandilla que hay: la base no la pone, porque
     * {@code open_outage_marker} vale {@code NULL} en una caida cerrada y una
     * unicidad sobre columna nula no restringe nada. Sin esta comprobacion, el
     * segundo cierre machacaria en silencio la hora en que el servicio volvio —que
     * es la que mide la duracion de la interrupcion, o sea el numero entero de la
     * reclamacion—.
     *
     * <p>
     * Devuelve una instancia nueva —la clase no tiene mutadores— <b>conservando la
     * version</b>: es lo que permite que el {@code save} posterior siga siendo un
     * ciclo leer-modificar-guardar con bloqueo optimista y no un insert.
     */
    public ExternalInvoicingOutage end(LocalDateTime endedOn) {
        if (endedAt != null)
            throw new ExternalInvoicingOutageAlreadyEndedException(id, endedAt);
        return new ExternalInvoicingOutage(id, startedAt, endedOn, causeParty, summary,
                affectedCompanyCount, notifiedCompaniesAt, externalIncidentRef, createdDate,
                version);
    }

    /**
     * Anota que ya se aviso a las clinicas alcanzadas, y con cuantas se conto al
     * hacerlo.
     *
     * <p>
     * <strong>Es idempotente a proposito y sobrescribe la marca anterior.</strong>
     * Avisar dos veces durante una caida larga es lo normal —el segundo correo
     * llega con el contador ya corregido—, y lo que hay que conservar es <em>la
     * ultima vez que se informo</em>, no la primera. Lo que si impide
     * {@code chk_eio_notified} es informar antes de que la caida empezara.
     */
    public ExternalInvoicingOutage notifyCompanies(LocalDateTime notifiedAt,
            int affectedCompanies) {
        return new ExternalInvoicingOutage(id, startedAt, endedAt, causeParty, summary,
                affectedCompanies, notifiedAt, externalIncidentRef, createdDate, version);
    }

    /** La caida sigue viva: no tiene hora de fin. */
    public boolean isOpen() {
        return endedAt == null;
    }

    /**
     * El mismo valor que la base calcula en {@code open_outage_marker}: el causante
     * mientras la caida esta abierta, y nada cuando ya se cerro. Vive aqui para que
     * quien lea el modelo entienda de donde sale {@code uq_eio_open} sin tener que
     * abrir el changeset.
     */
    public String openOutageMarker() {
        return isOpen() ? causeParty.name() : null;
    }

    private static void validate(LocalDateTime startedAt, LocalDateTime endedAt,
            CauseParty causeParty, String summary, int affectedCompanyCount,
            LocalDateTime notifiedCompaniesAt, String externalIncidentRef,
            LocalDateTime createdDate) {
        if (startedAt == null)
            throw new IllegalArgumentException("startedAt is required");
        if (causeParty == null)
            throw new IllegalArgumentException("causeParty is required");
        validateSummary(summary);
        validateEnded(startedAt, endedAt);
        if (affectedCompanyCount < 0)
            throw new IllegalArgumentException("affectedCompanyCount must not be negative");
        validateNotified(startedAt, notifiedCompaniesAt);
        if (externalIncidentRef != null
                && externalIncidentRef.length() > MAX_EXTERNAL_INCIDENT_REF_LENGTH)
            throw new IllegalArgumentException("externalIncidentRef must be 100 chars or less");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * El resumen es obligatorio: una lista de caidas sin titulo es ilegible, y
     * quien la abre en caliente es el unico que sabe en una linea que esta pasando.
     */
    private static void validateSummary(String summary) {
        if (summary == null || summary.isBlank())
            throw new IllegalArgumentException("summary is required");
        if (summary.length() > MAX_SUMMARY_LENGTH)
            throw new IllegalArgumentException("summary must be 255 chars or less");
    }

    /**
     * Espejo de {@code chk_eio_ended}. El limite es <b>estricto</b>: una caida que
     * empieza y termina en el mismo instante no es una caida, es un registro
     * escrito con un solo reloj y sin nada medido.
     */
    private static void validateEnded(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (endedAt != null && !endedAt.isAfter(startedAt))
            throw new IllegalArgumentException("endedAt must be after startedAt");
    }

    /**
     * Espejo de {@code chk_eio_notified}. Aqui el limite <b>no</b> es estricto:
     * avisar en el mismo instante en que se detecta el corte es el mejor caso
     * posible, no un error.
     */
    private static void validateNotified(LocalDateTime startedAt,
            LocalDateTime notifiedCompaniesAt) {
        if (notifiedCompaniesAt != null && notifiedCompaniesAt.isBefore(startedAt))
            throw new IllegalArgumentException("notifiedCompaniesAt must not precede startedAt");
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public CauseParty getCauseParty() {
        return causeParty;
    }

    public String getSummary() {
        return summary;
    }

    public int getAffectedCompanyCount() {
        return affectedCompanyCount;
    }

    public LocalDateTime getNotifiedCompaniesAt() {
        return notifiedCompaniesAt;
    }

    public String getExternalIncidentRef() {
        return externalIncidentRef;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
