package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Los dos números sin los cuales un prorrateo no se puede reconstruir: cuántos
 * días se cobraron y sobre cuántos días del periodo.
 *
 * <p>
 * <b>Por qué es un tipo y no dos campos sueltos.</b> El {@code CHECK}
 * {@code chk_subscription_charges_proration} dice «o están los dos o no está
 * ninguno», y esa es exactamente la garantía que un record da gratis: o hay
 * {@code ProrationBasis} o hay {@code null}, y no existe el estado intermedio
 * en el que alguien guardó los días cobrados y se dejó el denominador.
 *
 * <p>
 * Sin estos dos números <b>se ve el importe pero no de dónde salió</b>, y
 * explicarle un prorrateo a un cliente que reclama pasa a ser arqueología: hay
 * que adivinar si se prorrateó sobre 30 días comerciales o sobre los 31 del
 * mes, y las dos respuestas dan importes distintos que ya nadie puede
 * contrastar.
 */
public record ProrationBasis(int prorationDays, int periodDays) {

    public ProrationBasis {
        if (periodDays <= 0)
            throw new IllegalArgumentException("periodDays must be greater than zero");
        if (prorationDays < 0)
            throw new IllegalArgumentException("prorationDays cannot be negative");
        if (prorationDays > periodDays)
            throw new IllegalArgumentException("prorationDays cannot exceed periodDays: "
                    + prorationDays + " > " + periodDays);
    }

    /**
     * Reconstruye la base desde las dos columnas nulables de la fila. Vacío cuando
     * ninguna está; error cuando está una sola, que es justo el estado que el
     * {@code CHECK} impide y que este método no deja entrar por el mapper.
     */
    public static ProrationBasis of(Integer prorationDays, Integer periodDays) {
        if (prorationDays == null && periodDays == null)
            return null;
        if (prorationDays == null || periodDays == null)
            throw new IllegalArgumentException(
                    "prorationDays and periodDays go together or not at all");
        return new ProrationBasis(prorationDays, periodDays);
    }
}
