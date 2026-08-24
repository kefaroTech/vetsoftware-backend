package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * El consecutivo interno: una fila por serie.
 *
 * <p>
 * <b>Sin {@code company_id}</b>: es un contador global de plataforma, la única
 * tabla de este slice sin tenant. Eso tiene una consecuencia que conviene tener
 * escrita: como esta entidad no alcanza la empresa por ningún camino, las
 * cuatro reglas duras de BE-COV <b>ni la miran</b>, y su {@code @Query} de
 * incremento puede acotar solo por prefijo sin que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} tenga nada que decir. No es un
 * hueco: no hay ninguna empresa que nombrar.
 *
 * <p>
 * <b>Sin {@code @Version}</b> ({@code E6_YA_PROTEGIDO}): el
 * {@code SELECT ... FOR UPDATE} sobre la fila del prefijo ya serializa el
 * incremento, y un 409 en mitad de una emisión no protegería nada — solo
 * rompería la emisión.
 *
 * <p>
 * <b>Sin {@code enabled}</b>, y este es el motivo menos evidente y el más caro:
 * una serie desactivada dejaría de verse para
 * {@code @SQLRestriction("enabled = true")} y el siguiente documento arrancaría
 * la numeración desde cero. Es un modo de fallo sin vuelta atrás.
 */
@Entity
@Table(name = "billing_document_sequences")
public class BillingDocumentSequenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected BillingDocumentSequenceJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getNextValue() {
        return nextValue;
    }

    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
