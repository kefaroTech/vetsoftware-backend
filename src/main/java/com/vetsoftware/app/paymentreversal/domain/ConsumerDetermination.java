package com.vetsoftware.app.paymentreversal.domain;

/**
 * Si quien reclama es consumidor a efectos de la norma. Espejo de
 * {@code chk_prr_consumer_determination}.
 *
 * <p>
 * No es un detalle administrativo: el derecho de reversion protege al
 * consumidor, y una clinica veterinaria contratando software de gestion no
 * siempre lo es. {@link #UNDETERMINED} existe porque la calificacion es un
 * juicio que puede no estar hecho el dia que se abre el expediente, y forzar
 * una respuesta antes de tiempo produciria una calificacion inventada.
 */
public enum ConsumerDetermination {
    CONSUMER, NOT_CONSUMER, UNDETERMINED
}
