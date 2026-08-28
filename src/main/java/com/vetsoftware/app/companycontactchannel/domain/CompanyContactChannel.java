package com.vetsoftware.app.companycontactchannel.domain;

import java.time.LocalDateTime;

/**
 * Por donde se le puede escribir a una empresa, y con que permiso.
 *
 * <p>
 * <strong>Es una bitacora probatoria, no una libreta de direcciones.</strong>
 * La ley solo permite contactar por los canales que el cliente autorizo; sin
 * esta fila, la cobranza no puede demostrar que uso un canal permitido, y ese
 * incumplimiento tiene sancion propia. Es la gemela de los eventos de mora:
 * alli queda que se aviso, aqui que se podia avisar.
 *
 * <h2>Las cuatro reglas que sostiene esta clase</h2>
 *
 * <p>
 * <strong>1. Un solo primario por empresa Y PROPOSITO, no uno global.</strong>
 * Es el error que cometeria quien copie el patron de al lado. El indice unico
 * del esquema lleva <em>dos</em> columnas —{@code (primary_marker, purpose)}— y
 * no solo la generada: con la generada sola habria un unico canal primario por
 * empresa <em>en total</em>, y un correo de facturacion y un movil de mora —que
 * son propositos distintos y conviven— fallarian el segundo. Aqui eso se
 * traduce en que {@link #designateAsPrimary()} no sabe nada de otros canales:
 * quien libera al incumbente es el caso de uso, y solo el del mismo proposito.
 *
 * <p>
 * <strong>2. Revocar cierra la fila, no la borra.</strong> Hay que poder
 * demostrar que el aviso de marzo iba a una direccion autorizada en marzo; una
 * fila borrada demuestra lo contrario de lo que hace falta. No hay
 * {@code delete}, no hay {@code enabled} y no hay borrado logico —una prueba
 * que se puede desactivar no prueba nada—. La revocacion escribe el cierre y
 * exige el motivo, y revocar dos veces se rechaza.
 *
 * <p>
 * <strong>3. Autorizar un proposito no autoriza los demas.</strong> El
 * {@code purpose} viaja en cada fila y la consulta caliente —por donde puedo
 * escribirle a esta empresa para cobrar— filtra por
 * {@code (company_id, purpose)} <em>y</em> {@code revoked_at IS NULL}.
 *
 * <p>
 * <strong>4. La escribe el cliente, la empresa la pone el servidor.</strong> El
 * {@code companyId} nunca viaja en el cuerpo de la peticion: lo inyecta el
 * controller desde quien firma el token.
 *
 * <h2>Dos decisiones que conviene no deshacer</h2>
 *
 * <p>
 * <strong>{@code authorizedAt} sale del reloj inyectado, no de la
 * peticion.</strong> Es la fecha desde la que el consentimiento vale, asi que
 * dejarla escribir al cliente seria dejarle antedatar su propio permiso: la
 * unica columna que decide si un aviso ya enviado estaba permitido pasaria a
 * ser un campo de formulario.
 *
 * <p>
 * <strong>Revocar NO baja {@code is_primary}, y es a proposito.</strong> El
 * hueco de primario lo libera la columna generada en cuanto hay
 * {@code revoked_at}, asi que no hace falta tocar el marcador; y conservarlo
 * deja escrito que <em>ese</em> era el canal principal mientras estuvo vivo,
 * que es justo lo que se le pregunta a una bitacora probatoria.
 */
public class CompanyContactChannel {

    /** {@code address VARCHAR(160)}. */
    private static final int MAX_ADDRESS_LENGTH = 160;

    /** {@code authorization_evidence VARCHAR(255)}. */
    private static final int MAX_EVIDENCE_LENGTH = 255;

    /** {@code revoked_reason VARCHAR(255)}. */
    private static final int MAX_REVOKED_REASON_LENGTH = 255;

    private final Long id;

    /** La empresa duena del canal. Nunca llega del cuerpo de la peticion. */
    private final Long companyId;

    private final ContactChannelType channelType;

    /** La direccion del canal: el correo, el movil o el identificador. */
    private final String address;

    /**
     * La finalidad para la que se autorizo. Autorizar una no autoriza el resto.
     */
    private final ContactPurpose purpose;

    /** Desde cuando vale el consentimiento. */
    private final LocalDateTime authorizedAt;

    /** Con que se demuestra: el formulario, el contrato, la grabacion. */
    private final String authorizationEvidence;

    /** Cuando dejo de valer. Nulo si y solo si el canal sigue vivo. */
    private LocalDateTime revokedAt;

    /** Por que dejo de valer. Nulo si y solo si el canal sigue vivo. */
    private String revokedReason;

    /** Si es el canal principal de su proposito. */
    private boolean primary;

    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyContactChannel(Long id, Long companyId, ContactChannelType channelType,
            String address, ContactPurpose purpose, LocalDateTime authorizedAt,
            String authorizationEvidence, LocalDateTime revokedAt, String revokedReason,
            boolean primary, LocalDateTime createdDate, Long version) {
        validate(companyId, channelType, address, purpose, authorizedAt, authorizationEvidence,
                revokedAt, revokedReason);
        this.id = id;
        this.companyId = companyId;
        this.channelType = channelType;
        this.address = address;
        this.purpose = purpose;
        this.authorizedAt = authorizedAt;
        this.authorizationEvidence = authorizationEvidence;
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
        this.primary = primary;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Canal recien autorizado: vivo, y <strong>no primario</strong>.
     *
     * <p>
     * Nace sin el marcador a proposito. Designar el primario es una decision
     * declarada y tiene su propio caso de uso; si el alta pudiera marcarlo, un alta
     * rutinaria desviaria en silencio la facturacion de la empresa a una direccion
     * nueva, y el rastro seria un {@code POST} que parecia inofensivo.
     *
     * @param authorizedAt
     *            del reloj inyectado del caso de uso, nunca de un
     *            {@code LocalDateTime.now()} pelado: es la fecha que decide si un
     *            aviso ya enviado estaba permitido
     */
    public static CompanyContactChannel authorize(Long companyId, ContactChannelType channelType,
            String address, ContactPurpose purpose, String authorizationEvidence,
            LocalDateTime authorizedAt) {
        return new CompanyContactChannel(null, companyId, channelType, address, purpose,
                authorizedAt, authorizationEvidence, null, null, false, authorizedAt, null);
    }

    /**
     * Cierra el canal dejando escrito cuando y por que.
     *
     * <p>
     * Las dos columnas van juntas o no van —es la forma de bicondicional de
     * {@code chk_company_contact_channels_revocation}— y el motivo no puede quedar
     * en blanco: una revocacion sin motivo obliga a quien audite el ano siguiente a
     * adivinar si el cliente se dio de baja o si fue un error de captura.
     *
     * @throws CompanyContactChannelAlreadyRevokedException
     *             si el canal ya estaba cerrado. Reescribir la fecha moveria la
     *             frontera entre los avisos permitidos y los que no
     */
    public void revoke(LocalDateTime revokedAt, String revokedReason) {
        if (isRevoked()) {
            throw new CompanyContactChannelAlreadyRevokedException(id, this.revokedAt);
        }
        validateRevocation(authorizedAt, revokedAt, revokedReason);
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
    }

    /**
     * Marca este canal como el principal de su proposito.
     *
     * <p>
     * <strong>No mira a los demas canales, y no es un olvido.</strong> Liberar al
     * incumbente es cosa del caso de uso, que es quien puede cargarlo; aqui solo
     * cabe la mitad de la regla que esta fila conoce. La otra mitad —que no haya
     * dos a la vez— la garantiza {@code uq_company_contact_channels_primary} en el
     * motor.
     *
     * @throws RevokedContactChannelCannotBePrimaryException
     *             si el canal esta revocado: el motor dejaria pasar el
     *             {@code UPDATE} y la empresa se quedaria sin primario creyendo que
     *             acaba de designarlo
     */
    public void designateAsPrimary() {
        if (isRevoked()) {
            throw new RevokedContactChannelCannotBePrimaryException(id);
        }
        this.primary = true;
    }

    /**
     * Deja de ser el principal, sin dejar de estar autorizado.
     *
     * <p>
     * Lo usa el caso de uso que designa a otro: el indice unico se comprueba
     * sentencia a sentencia, asi que el incumbente tiene que bajar el marcador
     * <em>antes</em> de que suba el sucesor.
     */
    public void releasePrimary() {
        this.primary = false;
    }

    /** Sigue autorizado: es el filtro de la consulta caliente. */
    public boolean isUsable() {
        return revokedAt == null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Espejo de las dos comprobaciones que el esquema hace sobre la revocacion.
     *
     * <p>
     * La primera es la bicondicional de
     * {@code chk_company_contact_channels_revocation}: las dos columnas nulas, o
     * las dos con valor. La segunda es
     * {@code chk_company_contact_channels_revoked_after}, que impide cerrar el
     * canal antes de abrirlo — sin ella cabria una fila que declara un permiso que
     * caduco antes de existir, y cualquier consulta de si estaba permitido el dia X
     * daria una respuesta distinta segun por que extremo la mire.
     */
    private static void validateRevocation(LocalDateTime authorizedAt, LocalDateTime revokedAt,
            String revokedReason) {
        if (revokedAt == null && revokedReason == null) {
            return;
        }
        if (revokedAt == null) {
            throw new IllegalArgumentException("revokedAt is required when revokedReason is set");
        }
        if (revokedReason == null || revokedReason.isBlank()) {
            throw new IllegalArgumentException("revokedReason is required when revokedAt is set");
        }
        if (revokedReason.length() > MAX_REVOKED_REASON_LENGTH) {
            throw new IllegalArgumentException("revokedReason must be 255 chars or less");
        }
        if (revokedAt.isBefore(authorizedAt)) {
            throw new IllegalArgumentException("revokedAt cannot be before authorizedAt");
        }
    }

    private static void validate(Long companyId, ContactChannelType channelType, String address,
            ContactPurpose purpose, LocalDateTime authorizedAt, String authorizationEvidence,
            LocalDateTime revokedAt, String revokedReason) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        if (channelType == null) {
            throw new IllegalArgumentException("channelType is required");
        }
        validateText("address", address, MAX_ADDRESS_LENGTH);
        if (purpose == null) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (authorizedAt == null) {
            throw new IllegalArgumentException("authorizedAt is required");
        }
        validateText("authorizationEvidence", authorizationEvidence, MAX_EVIDENCE_LENGTH);
        validateRevocation(authorizedAt, revokedAt, revokedReason);
    }

    /**
     * {@code NOT NULL} en la base solo prohibe el nulo: una cadena de espacios
     * entra igual. La evidencia en blanco es peor que la ausente, porque la fila
     * aparenta estar respaldada.
     */
    private static void validateText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be " + maxLength + " chars or less");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public ContactChannelType getChannelType() {
        return channelType;
    }

    public String getAddress() {
        return address;
    }

    public ContactPurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public String getAuthorizationEvidence() {
        return authorizationEvidence;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public boolean isPrimary() {
        return primary;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
