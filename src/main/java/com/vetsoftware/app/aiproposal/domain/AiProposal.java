package com.vetsoftware.app.aiproposal.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * La cabecera de la propuesta de un prospecto anonimo (tabla
 * {@code ai_proposals}, changeset 383).
 *
 * <p>
 * ⛔ <strong>NINGUNA columna de empresa, y no puede tenerla.</strong> No es una
 * omision: un prospecto no es una empresa. Una sola entidad de esta rodaja que
 * alcance {@code CompanyJpaEntity} encenderia las cuatro reglas duras de BE-COV
 * sobre todos sus puertos y casos de uso, y
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} no tendria salida limpia -el
 * {@code UPDATE} de anonimizacion por fecha no tiene ninguna empresa que
 * nombrar en su {@code WHERE}-. El puente hacia {@code companies} vive en la
 * rodaja {@code company}, y la direccion importa: {@code aiproposal} no
 * referencia la conversion, solo al reves.
 *
 * <p>
 * <strong>{@code publicToken} es la unica frontera de seguridad de esta
 * tabla</strong>: sin empresa, sin JWT y sin principal, lo unico que separa la
 * propuesta de un prospecto de la de otro es que la URL sea imposible de
 * adivinar. 32 bytes de {@code SecureRandom} en base64url son 43 caracteres. La
 * propuesta se direcciona por el token, nunca por el id, y el token viaja en
 * {@code ?token=} o en el cuerpo -jamas en un segmento de ruta, que
 * {@code RequestLoggingContextFilter} escribiria en claro en CloudWatch-.
 *
 * <p>
 * <strong>{@code privacyNoticeVersionId} es "que aviso exacto se le mostro", no
 * "que lo acepto"</strong>. La aceptacion en si vive en
 * {@code legal_document_acceptances} (387), tiene vida propia y es revocable.
 * Esta columna queda fuera de la anonimizacion: sin ella la recogida del dato
 * no tiene respaldo.
 */
public class AiProposal {

    /** 32 bytes de entropia en base64url, sin relleno. */
    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    private Long id;
    private final String publicToken;
    private ProposalStatus status;
    private final Long priceListId;
    private final ProposalBillingCycle billingCycle;
    private final String catalogSnapshotHash;
    private final Long privacyNoticeVersionId;
    private String idempotencyKey;
    private String contactEmail;
    private final String locale;
    private int turnCount;
    private int totalInputTokens;
    private int totalOutputTokens;
    private final LocalDateTime firstSeenAt;
    private LocalDateTime lastActivityAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime anonymizedAt;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    @SuppressWarnings("java:S107")
    public AiProposal(Long id, String publicToken, ProposalStatus status, Long priceListId,
            ProposalBillingCycle billingCycle, String catalogSnapshotHash,
            Long privacyNoticeVersionId, String idempotencyKey, String contactEmail, String locale,
            int turnCount, int totalInputTokens, int totalOutputTokens, LocalDateTime firstSeenAt,
            LocalDateTime lastActivityAt, LocalDateTime expiresAt, LocalDateTime anonymizedAt,
            LocalDateTime createdDate, Long version, boolean enabled) {
        validarIdentidad(publicToken, catalogSnapshotHash, priceListId, privacyNoticeVersionId);
        validarContacto(idempotencyKey, contactEmail, locale, anonymizedAt);
        validarContadores(turnCount, totalInputTokens, totalOutputTokens);
        validarLinea(firstSeenAt, lastActivityAt, expiresAt);
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (billingCycle == null)
            throw new IllegalArgumentException("billingCycle is required");
        this.id = id;
        this.publicToken = publicToken;
        this.status = status;
        this.priceListId = priceListId;
        this.billingCycle = billingCycle;
        this.catalogSnapshotHash = catalogSnapshotHash;
        this.privacyNoticeVersionId = privacyNoticeVersionId;
        this.idempotencyKey = idempotencyKey;
        this.contactEmail = contactEmail;
        this.locale = locale;
        this.turnCount = turnCount;
        this.totalInputTokens = totalInputTokens;
        this.totalOutputTokens = totalOutputTokens;
        this.firstSeenAt = firstSeenAt;
        this.lastActivityAt = lastActivityAt;
        this.expiresAt = expiresAt;
        this.anonymizedAt = anonymizedAt;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    /**
     * Propuesta nueva. El {@link Clock} entra por parametro y no se llama a
     * {@code LocalDateTime.now()}: {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} esta
     * congelada sobre el arbol entero y una violacion nueva rompe el build.
     */
    @SuppressWarnings("java:S107")
    public static AiProposal create(String publicToken, Long priceListId,
            ProposalBillingCycle billingCycle, String catalogSnapshotHash,
            Long privacyNoticeVersionId, String idempotencyKey, String contactEmail, String locale,
            int diasDeVigencia, Clock clock) {
        if (diasDeVigencia <= 0)
            throw new IllegalArgumentException("diasDeVigencia must be greater than zero");
        LocalDateTime ahora = LocalDateTime.now(clock);
        return new AiProposal(null, publicToken, ProposalStatus.DRAFT, priceListId, billingCycle,
                catalogSnapshotHash, privacyNoticeVersionId, idempotencyKey, contactEmail, locale,
                0, 0, 0, ahora, ahora, ahora.plusDays(diasDeVigencia), null, ahora, null, true);
    }

    /**
     * Suma el consumo de un turno cerrado y mueve la ultima actividad. Los totales
     * de la cabecera son la vista barata del gasto: sumarlos a mano recorriendo los
     * turnos costaria un escaneo en la tabla que mas crece.
     */
    public void registrarTurno(int inputTokens, int outputTokens, Clock clock) {
        if (inputTokens < 0 || outputTokens < 0)
            throw new IllegalArgumentException("token counters cannot be negative");
        this.turnCount = this.turnCount + 1;
        this.totalInputTokens = this.totalInputTokens + inputTokens;
        this.totalOutputTokens = this.totalOutputTokens + outputTokens;
        tocar(clock);
    }

    /** La propuesta ya tiene lineas que ensenar. */
    public void marcarPropuesta(Clock clock) {
        this.status = ProposalStatus.PROPOSED;
        tocar(clock);
    }

    /**
     * <strong>{@code CONVERTED} lo escribe la rodaja {@code company}</strong>, que
     * es donde vive el puente; aqui solo se expone la transicion para que el estado
     * no se escriba desde fuera del agregado.
     */
    public void marcarConvertida(Clock clock) {
        this.status = ProposalStatus.CONVERTED;
        tocar(clock);
    }

    public void marcarAbandonada(Clock clock) {
        this.status = ProposalStatus.ABANDONED;
        tocar(clock);
    }

    public void marcarExpirada(Clock clock) {
        this.status = ProposalStatus.EXPIRED;
        tocar(clock);
    }

    public void tocar(Clock clock) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (ahora.isAfter(this.lastActivityAt))
            this.lastActivityAt = ahora;
    }

    /**
     * Borra el correo y la clave de idempotencia, y deja la marca. Lo que
     * <strong>no</strong> se toca es {@code privacyNoticeVersionId}: es la
     * evidencia de que hubo aviso, y borrarla dejaria la recogida sin respaldo.
     *
     * <p>
     * El motivo de cada linea se borra aparte, en
     * {@code ProposalLine.redactarMotivo}: MySQL no permite que un {@code CHECK}
     * referencie otra tabla, asi que la marca va en la propia linea.
     */
    public void anonimizar(Clock clock) {
        this.contactEmail = null;
        this.idempotencyKey = null;
        this.anonymizedAt = LocalDateTime.now(clock);
    }

    public boolean estaAnonimizada() {
        return anonymizedAt != null;
    }

    public boolean haCaducado(Clock clock) {
        return LocalDateTime.now(clock).isAfter(expiresAt);
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    private static void validarIdentidad(String publicToken, String catalogSnapshotHash,
            Long priceListId, Long privacyNoticeVersionId) {
        if (publicToken == null || !TOKEN.matcher(publicToken).matches())
            throw new IllegalArgumentException("publicToken must be 43 base64url chars");
        if (catalogSnapshotHash == null || !SHA256_HEX.matcher(catalogSnapshotHash).matches())
            throw new IllegalArgumentException(
                    "catalogSnapshotHash must be 64 lowercase hex chars");
        if (priceListId == null)
            throw new IllegalArgumentException("priceListId is required");
        if (privacyNoticeVersionId == null)
            throw new IllegalArgumentException("privacyNoticeVersionId is required");
    }

    /** Espejo de {@code chk_ai_proposals_anonimizado}. */
    private static void validarContacto(String idempotencyKey, String contactEmail, String locale,
            LocalDateTime anonymizedAt) {
        if (idempotencyKey != null && idempotencyKey.length() != 36)
            throw new IllegalArgumentException("idempotencyKey must be a 36-char UUID");
        if (contactEmail != null && (contactEmail.isBlank() || contactEmail.length() > 320))
            throw new IllegalArgumentException("contactEmail must be 1..320 chars when present");
        if (locale == null || locale.isBlank() || locale.length() > 10)
            throw new IllegalArgumentException("locale is required and must be 10 chars or less");
        if (anonymizedAt != null && contactEmail != null)
            throw new IllegalArgumentException(
                    "an anonymized proposal cannot keep its contact email");
    }

    /** Espejo de {@code chk_ai_proposals_counters}. */
    private static void validarContadores(int turnCount, int totalInputTokens,
            int totalOutputTokens) {
        if (turnCount < 0 || totalInputTokens < 0 || totalOutputTokens < 0)
            throw new IllegalArgumentException("proposal counters cannot be negative");
    }

    /** Espejo de {@code chk_ai_proposals_timeline}. */
    private static void validarLinea(LocalDateTime firstSeenAt, LocalDateTime lastActivityAt,
            LocalDateTime expiresAt) {
        if (firstSeenAt == null || lastActivityAt == null || expiresAt == null)
            throw new IllegalArgumentException("proposal timestamps are required");
        if (lastActivityAt.isBefore(firstSeenAt))
            throw new IllegalArgumentException("lastActivityAt cannot precede firstSeenAt");
        if (!expiresAt.isAfter(firstSeenAt))
            throw new IllegalArgumentException("expiresAt must be after firstSeenAt");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public ProposalBillingCycle getBillingCycle() {
        return billingCycle;
    }

    public String getCatalogSnapshotHash() {
        return catalogSnapshotHash;
    }

    public Long getPrivacyNoticeVersionId() {
        return privacyNoticeVersionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getLocale() {
        return locale;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public int getTotalInputTokens() {
        return totalInputTokens;
    }

    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getAnonymizedAt() {
        return anonymizedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
