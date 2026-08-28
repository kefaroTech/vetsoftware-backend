package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.EffectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * {@code catalog_item_id} es una FK a {@code catalog_items}, de la feature
 * {@code catalogitem}, y va como {@code Long} pelado en vez de como
 * {@code @ManyToOne}: el configurador no lee ni un campo del artículo, así que
 * una asociación solo añadiría un proxy que hidratar y un {@code @EntityGraph}
 * que mantener. La existencia se valida en el caso de uso vía
 * {@code CatalogItemValidationPort}, y la integridad la impone la FK
 * {@code RESTRICT} de la base.
 */
@Entity
@Table(name = "configurator_effects")
@SQLDelete(sql = "UPDATE configurator_effects SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class ConfiguratorEffectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false, length = 25)
    private EffectType effect;

    @Column(name = "quantity")
    private Integer quantity;

    /**
     * Orden de aplicación. {@code INT NOT NULL DEFAULT 0} en el esquema, con
     * {@code chk_configurator_effects_priority} acotándolo a 0..9999 y el índice
     * {@code ix_configurator_effects_priority (priority, id)} sirviendo la única
     * lectura caliente de la tabla.
     *
     * <p>
     * <strong>Estuvo sin mapear desde que nació la tabla</strong>, y ese es el
     * defecto que este campo cierra: {@code ddl-auto: validate} comprueba que lo
     * mapeado exista en el esquema, no al revés, así que una columna que Java
     * ignora no rompe nada al arrancar — simplemente no ordena nada, y el
     * configurador aplicaba los efectos por {@code id}.
     *
     * <p>
     * {@code int} y no {@code Integer}: la columna es {@code NOT NULL} y un
     * envoltorio invitaría a mandar {@code null} desde el mapper, que Hibernate
     * traduciría a un {@code INSERT} rechazado por la base en vez de a un error del
     * dominio.
     */
    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ConfiguratorEffectJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public EffectType getEffect() {
        return effect;
    }

    public void setEffect(EffectType effect) {
        this.effect = effect;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
