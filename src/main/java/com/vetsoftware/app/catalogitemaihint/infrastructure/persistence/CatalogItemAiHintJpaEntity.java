package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * Espejo de {@code catalog_item_ai_hints} (changeset 381).
 *
 * <p>
 * <strong>Con {@code @Version}, y por escrito.</strong> La tabla trae columna
 * {@code version} y la decision es versionar: la vigencia se cierra sobre la
 * propia fila ({@code superseded_at}) y dos correcciones simultaneas del mismo
 * articulo sucederian ambas a la misma revision vigente. Es exactamente el
 * mismo razonamiento —y la misma forma— que
 * {@code LegalDocumentVersionJpaEntity}. No hay linea en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} porque no hace falta ninguna.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: una pista
 * publicada no se desactiva ni se borra, se sucede. La columna no existe en la
 * tabla, asi que tampoco aplica {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * &#9940; <strong>{@code current_hint_marker} no se mapea a proposito</strong>:
 * es una columna generada ({@code STORED}) que la base calcula a partir de
 * {@code superseded_at} para sostener {@code uq_catalog_item_ai_hints_current}.
 * Mapearla haria que Hibernate intentara escribirla y MySQL abortaria el INSERT
 * con el error 3105.
 *
 * <p>
 * <strong>{@code hint_hash} si se mapea, pero de solo lectura</strong>
 * ({@code insertable = false, updatable = false}): tambien es generada
 * —{@code SHA2(hint_text,256)}—, asi que Hibernate no puede escribirla, pero
 * mapearla es lo que permite preguntarle a
 * {@code uq_catalog_item_ai_hints_text} <em>antes</em> de chocar contra el, con
 * un finder derivado en vez de con SQL a mano. Lleva
 * {@code columnDefinition = "char(64)"} por el mismo motivo que
 * {@code LegalDocumentVersionJpaEntity.contentHash}: sin el, Hibernate espera
 * {@code varchar(64)} y {@code ddl-auto: validate} rompe el arranque.
 *
 * <p>
 * <strong>{@code catalog_item_id} va como {@code Long} pelado y no como
 * {@code @ManyToOne}</strong>, aunque la clave foranea exista. El
 * {@code CLAUDE.md} permite esa asociacion como unico cruce entre features,
 * pero aqui costaria mas de lo que da: {@code CatalogItemJpaEntity} declara
 * {@code @SQLRestriction("enabled = true")}, asi que el {@code LEFT JOIN FETCH}
 * de un {@code @EntityGraph} devolveria {@code null} en la asociacion para la
 * pista de un articulo retirado —una fila legitima del historial— y el mapeador
 * moriria con un NPE justo en la pantalla que existe para revisar el historico.
 * El codigo y el nombre los trae {@code JpaAiHintCatalogItemQueryPort}, con una
 * consulta aparte y por lote.
 */
@Entity
@Table(name = "catalog_item_ai_hints")
public class CatalogItemAiHintJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "hint_revision", nullable = false)
    private int hintRevision;

    @Column(name = "hint_text", nullable = false, length = 1000)
    private String hintText;

    @Column(name = "hint_hash", insertable = false, updatable = false, columnDefinition = "char(64)")
    private String hintHash;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "published_by_system_user_id", nullable = false)
    private Long publishedBySystemUserId;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    /**
     * La firma de quien cerro la vigencia (changeset 393). <b>Nulable, y no es una
     * concesion</b>: una revision vigente no la ha retirado nadie, y las ya
     * sucedidas antes de 393 no tienen a quien atribuirse. Su coherencia con
     * {@code superseded_at} la impone
     * {@code chk_catalog_item_ai_hints_superseded_by} en la base y el constructor
     * de {@code CatalogItemAiHint} en el dominio.
     *
     * <p>
     * <b>Sin {@code @ManyToOne} a {@code SystemUserJpaEntity}</b>, por el mismo
     * criterio que {@code published_by_system_user_id} justo encima: aqui no se
     * necesita ni un campo del usuario, solo su id, y colgar la asociacion metaria
     * su grafo en cada carga de pista.
     */
    @Column(name = "superseded_by_system_user_id")
    private Long supersededBySystemUserId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CatalogItemAiHintJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public int getHintRevision() {
        return hintRevision;
    }

    public void setHintRevision(int hintRevision) {
        this.hintRevision = hintRevision;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    /** Lo calcula la base. No hay {@code setHintHash} y no debe haberlo. */
    public String getHintHash() {
        return hintHash;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public void setPublishedBySystemUserId(Long publishedBySystemUserId) {
        this.publishedBySystemUserId = publishedBySystemUserId;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    public void setSupersededAt(LocalDateTime supersededAt) {
        this.supersededAt = supersededAt;
    }

    public Long getSupersededBySystemUserId() {
        return supersededBySystemUserId;
    }

    public void setSupersededBySystemUserId(Long supersededBySystemUserId) {
        this.supersededBySystemUserId = supersededBySystemUserId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
