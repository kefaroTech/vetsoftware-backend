package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.AnswerType;
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
 * {@code parent_option_id} va como {@code Long} pelado y <strong>no</strong>
 * como {@code @ManyToOne} a {@code ConfiguratorOptionJpaEntity}, y es
 * deliberado.
 *
 * <p>
 * Las dos tablas se apuntan mutuamente: la migración rompe el ciclo creando
 * {@code configurator_questions} sin la columna y añadiéndola en el paso 9 con
 * un {@code ALTER}, porque MySQL exige que la tabla referenciada exista.
 * Mapearlo como asociación devolvería ese ciclo al lado de Hibernate —esta vez
 * en el orden de inserción y en la resolución de proxies— para no ganar nada:
 * el configurador nunca navega de una pregunta a su opción padre, sino que
 * carga el árbol entero de una vez. {@code ddl-auto: validate} solo mira
 * columnas y tipos, así que la FK sigue existiendo en la base y sigue siendo
 * {@code RESTRICT}.
 *
 * <p>
 * El {@code @SQLDelete} lleva {@code AND version = ?} porque la entidad es
 * versionada: Hibernate liga dos parámetros —id y versión— y el {@code UPDATE}
 * de un solo parámetro fallaría en ejecución, no al compilar.
 */
@Entity
@Table(name = "configurator_questions")
@SQLDelete(sql = "UPDATE configurator_questions SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class ConfiguratorQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "question_text", nullable = false, length = 255)
    private String questionText;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 20)
    private AnswerType answerType;

    @Column(name = "parent_option_id")
    private Long parentOptionId;

    @Column(name = "required", nullable = false)
    private boolean required = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ConfiguratorQuestionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public void setAnswerType(AnswerType answerType) {
        this.answerType = answerType;
    }

    public Long getParentOptionId() {
        return parentOptionId;
    }

    public void setParentOptionId(Long parentOptionId) {
        this.parentOptionId = parentOptionId;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
