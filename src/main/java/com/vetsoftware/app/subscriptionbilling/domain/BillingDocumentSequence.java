package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * El consecutivo de las cuentas de cobro: una serie por prefijo.
 *
 * <p>
 * <b>Por qué existe como tabla y no se saca de un «máximo más uno».</b> Dos
 * procesos simultáneos que consulten el máximo leen el mismo número y se lo dan
 * a dos documentos distintos. La serie se lee y se incrementa <b>en la misma
 * operación, con la fila bloqueada</b>
 * ({@code SELECT next_value ... FOR UPDATE} seguido de
 * {@code UPDATE ... SET next_value = next_value + 1}), y las dos sentencias van
 * <b>dentro de la misma transacción de negocio</b>, nunca en un
 * {@code REQUIRES_NEW}: así un fallo posterior deshace también el incremento y
 * no deja huecos en la numeración.
 *
 * <p>
 * <b>Es la diferencia deliberada con {@code numbering_resolutions}.</b> El
 * consecutivo fiscal de la DIAN no puede tener huecos aunque la emisión falle
 * —por eso aquel se reserva aparte—; el consecutivo <b>interno</b> sí debe
 * deshacerse si el documento no llega a existir.
 *
 * <p>
 * <b>Contador global de plataforma: no lleva {@code company_id}</b>, y por eso
 * todos sus puertos están cerrados a {@code hasRole("SYSTEM")} a secas. Tampoco
 * lleva {@code version} ({@code E6_YA_PROTEGIDO}: el bloqueo pesimista ya lo
 * serializa, y un 409 en mitad de una emisión sería peor) ni {@code enabled}:
 * una serie desactivada dejaría de verse para {@code @SQLRestriction} y el
 * siguiente documento arrancaría desde cero, que es un modo de fallo sin vuelta
 * atrás.
 */
public final class BillingDocumentSequence {

    private final Long id;
    private final String prefix;
    private final long nextValue;
    private final LocalDateTime createdDate;

    public BillingDocumentSequence(Long id, String prefix, long nextValue,
            LocalDateTime createdDate) {
        if (prefix == null || prefix.isBlank())
            throw new IllegalArgumentException("prefix is required");
        if (prefix.length() > 10)
            throw new IllegalArgumentException("prefix must be 10 chars or less");
        if (nextValue < 1)
            throw new IllegalArgumentException("nextValue must be at least 1");
        this.id = id;
        this.prefix = prefix;
        this.nextValue = nextValue;
        this.createdDate = createdDate;
    }

    /** Serie nueva, arrancando en 1. Espejo del default de la columna. */
    public static BillingDocumentSequence create(String prefix, Clock clock) {
        return new BillingDocumentSequence(null, prefix, 1L, LocalDateTime.now(clock));
    }

    public Long getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getNextValue() {
        return nextValue;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
