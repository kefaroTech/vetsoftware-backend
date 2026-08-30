package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Espejo de {@code ai_proposal_suppression_requests} (changeset 392) — la
 * constancia de que una peticion de supresion se atendio.
 *
 * <p>
 * &#9940; <strong>Guarda el HECHO sin guardar el DATO.</strong> El aviso de
 * privacidad promete el derecho de supresion del articulo 8, literal e, de la
 * Ley 1581, el endpoint borra, y hasta hoy no quedaba constancia de que se
 * hubiera atendido: el propio borrado vacia {@code contact_email} y con el
 * desaparece la columna generada {@code contact_email_hash}, asi que despues
 * del exito no queda ni rastro por el que reconocer que hubo peticion. Ante la
 * SIC no habia nada que ensenar. Esta tabla es ese rastro.
 *
 * <p>
 * &#9940; <strong>{@code subject_email_hash} es {@code BINARY(32)} y lleva los
 * 32 bytes crudos de SHA-256 del correo en minusculas</strong>, byte a byte lo
 * mismo que la columna generada {@code ai_proposals.contact_email_hash}
 * ({@code UNHEX(SHA2(LOWER(contact_email), 256))}). No es un HMAC con pimienta,
 * y es deliberado: una pimienta perdida o rotada destruiria en silencio la
 * unica propiedad por la que esta tabla existe, que es poder emparejar una
 * peticion repetida con su predecesora. El correo en claro no se guarda —seria
 * absurdo que la prueba de haber borrado un dato personal consistiera en
 * conservarlo— y el hash es irreversible.
 *
 * <p>
 * <strong>La fecha de la peticion anterior no es una columna: se
 * deriva.</strong> El changeset declara el indice compuesto
 * {@code (subject_email_hash, executed_at)} precisamente para responder "¿ya se
 * atendio antes, y cuando?" con un {@code max(executed_at)} sobre el hash. Una
 * columna que copiara ese valor seria un segundo sitio donde vive el mismo dato
 * y podria contradecir al primero.
 *
 * <p>
 * &#9940; <strong>Ningun {@code company_id} y ninguna asociacion a
 * {@code CompanyJpaEntity}</strong>, igual que el resto de la rodaja: una sola
 * entidad de {@code aiproposal} con empresa encenderia las cuatro reglas duras
 * de BE-COV sobre la rodaja entera. {@code executed_by_system_user_id} tiene su
 * FK en el esquema pero aqui es una columna suelta, mismo criterio que
 * {@code ai_proposals.privacy_notice_version_id}.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong> —exenta
 * {@code E1_APPEND_ONLY} en {@code ENTIDADES_EXENTAS_DE_VERSION}, misma forma
 * que {@code legal_document_acceptances}—: una prueba de cumplimiento que se
 * puede reescribir o desactivar no prueba nada. Por eso tampoco lleva
 * {@code @SQLDelete} ni {@code @SQLRestriction}, y su repositorio no expone
 * ninguna escritura sobre fila existente.
 */
@Entity
@Table(name = "ai_proposal_suppression_requests")
public class AiProposalSuppressionRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * {@code columnDefinition} explicito porque es {@code BINARY(32)}: Hibernate
     * mapea {@code byte[]} a {@code varbinary} por defecto y
     * {@code ddl-auto: validate} tumbaria el arranque de la aplicacion entera. Es
     * la misma leccion del {@code char(64)} de {@code AiProposalJpaEntity}.
     */
    @Column(name = "subject_email_hash", nullable = false, columnDefinition = "binary(32)")
    private byte[] subjectEmailHash;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    /**
     * Quien la atendio, tomado de la sesion por el controller y jamas del cuerpo de
     * la peticion: un rastro de auditoria que escribe el auditado no es un rastro
     * de auditoria.
     */
    @Column(name = "executed_by_system_user_id", nullable = false)
    private Long executedBySystemUserId;

    @Column(name = "proposals_suppressed", nullable = false)
    private int proposalsSuppressed;

    @Column(name = "turns_suppressed", nullable = false)
    private int turnsSuppressed;

    @Column(name = "lines_suppressed", nullable = false)
    private int linesSuppressed;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected AiProposalSuppressionRequestJpaEntity() {
    }

    /**
     * &#9940; <strong>Sin mutadores y con todo por constructor.</strong> La fila se
     * arma entera y se inserta una vez; poder cambiarle un contador despues seria
     * poder corregir la prueba a posteriori.
     */
    @SuppressWarnings("java:S107")
    public AiProposalSuppressionRequestJpaEntity(byte[] subjectEmailHash, LocalDateTime executedAt,
            Long executedBySystemUserId, int proposalsSuppressed, int turnsSuppressed,
            int linesSuppressed, LocalDateTime createdDate) {
        this.subjectEmailHash = subjectEmailHash == null ? null : subjectEmailHash.clone();
        this.executedAt = executedAt;
        this.executedBySystemUserId = executedBySystemUserId;
        this.proposalsSuppressed = proposalsSuppressed;
        this.turnsSuppressed = turnsSuppressed;
        this.linesSuppressed = linesSuppressed;
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public byte[] getSubjectEmailHash() {
        return subjectEmailHash == null ? null : subjectEmailHash.clone();
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public Long getExecutedBySystemUserId() {
        return executedBySystemUserId;
    }

    public int getProposalsSuppressed() {
        return proposalsSuppressed;
    }

    public int getTurnsSuppressed() {
        return turnsSuppressed;
    }

    public int getLinesSuppressed() {
        return linesSuppressed;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
