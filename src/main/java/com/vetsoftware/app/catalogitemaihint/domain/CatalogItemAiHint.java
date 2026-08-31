package com.vetsoftware.app.catalogitemaihint.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Una revision publicada de la pista que el prompt le ensena al modelo sobre un
 * articulo del catalogo.
 *
 * <p>
 * <strong>No se edita: se sucede.</strong> La tabla nace como historial
 * append-only —{@code hint_revision}, {@code superseded_at} y el indice
 * {@code uq_catalog_item_ai_hints_current}— por el mismo motivo que
 * {@code legal_document_versions}: una propuesta pasada se genero con un texto
 * concreto, y sobrescribir {@code hint_text} destruiria la unica evidencia de
 * que se le estaba diciendo al modelo cuando la genero. Por eso <b>no hay
 * setters de texto y no hay {@code update}</b>: la unica mutacion es
 * {@link #supersede(LocalDateTime, Long)}, que mueve la fecha de cierre y la
 * firma de quien lo decidio.
 *
 * <p>
 * <strong>La huella se deriva aqui y la base la calcula sola.</strong>
 * {@code hint_hash} es una columna
 * {@code GENERATED ALWAYS AS (SHA2(hint_text,256)) STORED} y sostiene
 * {@code uq_catalog_item_ai_hints_text} —no se puede republicar el mismo texto
 * bajo el mismo articulo—. {@link #hashOf(String)} reproduce ese calculo en
 * Java para que el adaptador pueda <em>preguntar</em> por el indice antes de
 * chocar contra el: sin eso, un texto repetido sale como un 500 de integridad
 * en vez de como el 409 que el front sabe pintar. Que las dos huellas coincidan
 * lo comprueba la rodaja de persistencia contra MySQL real, que es el unico
 * sitio donde esa igualdad se puede afirmar de verdad.
 *
 * <p>
 * &#9940; <strong>Sin {@code companyId}, y no es un olvido.</strong> La tabla
 * no lo tiene ni alcanza {@code companies} por ningun camino: la pista describe
 * un articulo del catalogo global de plataforma, y quien la lee es el prompt
 * que atiende a un prospecto anonimo. No hay tenant que acotar, y por eso todos
 * sus puertos van cerrados a rol de sistema a secas.
 */
public class CatalogItemAiHint {

    /** El ancho de {@code hint_text} en el changeset 381. */
    public static final int MAX_HINT_TEXT = 1000;

    /**
     * Las tres partes de la convencion: que es el modulo en palabras del negocio,
     * las senales literales en el texto del prospecto y —la que mas trabaja— cuando
     * NO aplica.
     */
    private static final int PARTES_DE_LA_PISTA = 3;

    private static final String ALGORITMO_DE_HUELLA = "SHA-256";

    private final Long id;
    private final Long catalogItemId;
    private final int hintRevision;
    private final String hintText;
    private final LocalDateTime publishedAt;
    private final Long publishedBySystemUserId;
    private LocalDateTime supersededAt;
    private Long supersededBySystemUserId;
    private final LocalDateTime createdDate;
    private final Long version;

    /**
     * &#9940; <strong>La coherencia de la firma de retirada se comprueba aqui en un
     * solo sentido, y esa asimetria es la decision.</strong> Este constructor lo
     * ejecuta tambien el mapeador al <em>leer</em> una fila, exactamente por el
     * mismo motivo que {@link #publish} deja fuera de aqui la regla de las tres
     * partes.
     *
     * <ul>
     * <li><b>Firmante sin fecha de retirada: prohibido.</b> Es el espejo exacto de
     * {@code chk_catalog_item_ai_hints_superseded_by} (changeset 393). No se puede
     * saber quien retiro algo que sigue vigente, y ninguna fila de la tabla puede
     * estar en ese estado —ni una vieja ni una nueva—, asi que la invariante vale
     * tanto leyendo como escribiendo.</li>
     * <li><b>Fecha de retirada sin firmante: permitido, y a proposito.</b> La
     * columna nacio nulable porque las revisiones ya sucedidas <em>antes</em> del
     * changeset 393 no tienen a quien atribuirse: el actor real nunca se escribio y
     * no hay forma de reconstruirlo. Exigir el firmante aqui convertiria cada una
     * de esas filas historicas en una excepcion al abrir el historial —la pantalla
     * que existe justamente para leerlas—. Donde esa exigencia si muerde es en
     * {@link #supersede(LocalDateTime, Long)}, que es el unico camino por el que se
     * cierra una vigencia <em>nueva</em>.</li>
     * </ul>
     */
    public CatalogItemAiHint(Long id, Long catalogItemId, int hintRevision, String hintText,
            LocalDateTime publishedAt, Long publishedBySystemUserId, LocalDateTime supersededAt,
            Long supersededBySystemUserId, LocalDateTime createdDate, Long version) {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId is required");
        }
        if (hintRevision < 1) {
            throw new IllegalArgumentException(
                    "hintRevision must be at least 1 (chk_catalog_item_ai_hints_revision)");
        }
        if (hintText == null || hintText.isBlank()) {
            throw new IllegalArgumentException("hintText is required");
        }
        if (hintText.length() > MAX_HINT_TEXT) {
            throw new IllegalArgumentException(
                    "hintText must be " + MAX_HINT_TEXT + " chars or less");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("publishedAt is required");
        }
        if (publishedBySystemUserId == null) {
            throw new IllegalArgumentException("publishedBySystemUserId is required");
        }
        if (supersededAt != null && supersededAt.isBefore(publishedAt)) {
            throw new IllegalArgumentException("supersededAt cannot be earlier than publishedAt"
                    + " (chk_catalog_item_ai_hints_supersede)");
        }
        if (supersededBySystemUserId != null && supersededAt == null) {
            throw new IllegalArgumentException(
                    "supersededBySystemUserId requires supersededAt: nobody can have retired a"
                            + " hint that is still current"
                            + " (chk_catalog_item_ai_hints_superseded_by)");
        }
        this.id = id;
        this.catalogItemId = catalogItemId;
        this.hintRevision = hintRevision;
        this.hintText = hintText;
        this.publishedAt = publishedAt;
        this.publishedBySystemUserId = publishedBySystemUserId;
        this.supersededAt = supersededAt;
        this.supersededBySystemUserId = supersededBySystemUserId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Publica una revision nueva.
     *
     * <p>
     * &#9940; <strong>La regla de las tres partes se comprueba AQUI y no en el
     * constructor, y esa asimetria es la decision.</strong> El constructor lo
     * ejecuta tambien el mapeador al <em>leer</em> una fila, asi que una
     * comprobacion editorial ahi arriba convertiria cualquier pista historica que
     * no la cumpla en una excepcion al abrir la pantalla que existe para
     * corregirla: el caso de uso moriria justo sobre el dato que viene a arreglar.
     * En {@code publish} solo pasa el texto que alguien esta escribiendo ahora, que
     * es exactamente donde la convencion tiene que morder.
     *
     * <p>
     * Lo que exige es <b>estructura y no vocabulario</b>: al menos tres bloques
     * separados por linea en blanco. Las catorce pistas que siembra el changeset
     * 382 tienen exactamente tres, pero trece cierran con «NO se necesita si…» y
     * {@code CORE} con «NUNCA lo devuelvas…». Exigir un literal habria rechazado la
     * decimocuarta —y cualquier forma legitima de decir lo mismo—; exigir el bloque
     * obliga a escribir el contraejemplo sin dictar como se redacta. Sin el, el
     * modelo mete de todo.
     */
    public static CatalogItemAiHint publish(Long catalogItemId, int hintRevision, String hintText,
            Long publishedBySystemUserId, LocalDateTime publishedAt, LocalDateTime createdDate) {
        exigirLasTresPartes(hintText);
        return new CatalogItemAiHint(null, catalogItemId, hintRevision, hintText, publishedAt,
                publishedBySystemUserId, null, null, createdDate, null);
    }

    private static void exigirLasTresPartes(String hintText) {
        if (hintText == null || hintText.isBlank()) {
            throw new IllegalArgumentException("hintText is required");
        }
        long bloques = Arrays.stream(hintText.split("\\R\\s*\\R"))
                .filter(bloque -> !bloque.isBlank()).count();
        if (bloques < PARTES_DE_LA_PISTA) {
            throw new IllegalArgumentException("hintText must have at least " + PARTES_DE_LA_PISTA
                    + " blocks separated by a blank line: what the module is, the literal signals"
                    + " in the prospect text, and when it does NOT apply");
        }
    }

    /**
     * SHA-256 del texto en UTF-8, en hexadecimal minuscula: el mismo valor que
     * MySQL guarda en la columna generada {@code hint_hash}.
     */
    public static String hashOf(String hintText) {
        if (hintText == null) {
            throw new IllegalArgumentException("hintText is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_DE_HUELLA);
            return HexFormat.of()
                    .formatHex(digest.digest(hintText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 es obligatorio en toda JVM; si falta, el entorno esta roto.
            throw new IllegalStateException("SHA-256 is not available in this JVM", exception);
        }
    }

    /**
     * Cierra la vigencia de esta revision: pone la fecha y <b>la firma de quien lo
     * decidio</b>. Es la unica mutacion que existe.
     *
     * <p>
     * Sirve a los dos casos de negocio, y esa es la razon de que no se llame
     * {@code retire} ni {@code replace}: corregir es suceder <em>y</em> publicar la
     * siguiente; retirar es suceder <em>sin</em> sucesora, y el articulo pasa a no
     * tener pista. Desde aqui son la misma escritura.
     *
     * <p>
     * &#9940; <strong>El firmante es obligatorio AQUI aunque la columna sea
     * nulable, y no es una contradiccion.</strong> {@code 381} audita quien publica
     * ({@code published_by_system_user_id}, {@code NOT NULL}) y hasta el changeset
     * 393 nadie auditaba quien apaga: la fila retirada conservaba el firmante de
     * quien la <em>publico</em>, que es precisamente el actor equivocado. La
     * columna nace nulable por las filas anteriores a 393 —ver el constructor—,
     * pero toda vigencia que se cierre <b>a partir de ahora</b> pasa por este
     * metodo, asi que exigirlo aqui es lo que impide que la laguna siga creciendo
     * sin convertir el historico en un error.
     *
     * <p>
     * <strong>El firmante NO es {@code publishedBySystemUserId} y no se puede
     * derivar de el.</strong> Al corregir, quien escribe la revision nueva puede no
     * ser quien escribio la anterior; al retirar, casi nunca lo es. Por eso lo pone
     * el llamador desde la sesion y no este metodo desde la propia fila.
     */
    public void supersede(LocalDateTime supersededAt, Long supersededBySystemUserId) {
        if (this.supersededAt != null) {
            throw new CatalogItemAiHintAlreadySupersededException(catalogItemId, hintRevision);
        }
        if (supersededAt == null) {
            throw new IllegalArgumentException("supersededAt is required");
        }
        if (supersededBySystemUserId == null) {
            throw new IllegalArgumentException("supersededBySystemUserId is required: closing a"
                    + " hint without recording who did it is what changeset 393 exists to fix");
        }
        if (supersededAt.isBefore(publishedAt)) {
            throw new IllegalArgumentException("supersededAt cannot be earlier than publishedAt"
                    + " (chk_catalog_item_ai_hints_supersede)");
        }
        this.supersededAt = supersededAt;
        this.supersededBySystemUserId = supersededBySystemUserId;
    }

    /**
     * {@code true} mientras nadie la haya sucedido. Espejo de la columna generada
     * {@code current_hint_marker} y del indice que sostiene.
     */
    public boolean isCurrent() {
        return supersededAt == null;
    }

    public Long getId() {
        return id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    /** La revision <em>de negocio</em>, no la de concurrencia. */
    public int getHintRevision() {
        return hintRevision;
    }

    public String getHintText() {
        return hintText;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    /**
     * Quien cerro la vigencia. <b>Nulo en dos casos legitimos</b>: la revision
     * vigente —que nadie ha retirado— y toda revision sucedida antes del changeset
     * 393, cuyo actor real no quedo escrito en ninguna parte. Un nulo aqui es una
     * laguna conocida, nunca un firmante inventado.
     */
    public Long getSupersededBySystemUserId() {
        return supersededBySystemUserId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
