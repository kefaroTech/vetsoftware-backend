package com.vetsoftware.app.revenuerecognitionline.application.port.out;

import java.util.Optional;

/**
 * El calendario contable, visto desde este slice: en que periodo abierto se
 * puede registrar un reconocimiento.
 *
 * <p>
 * <strong>Es la mitad de codigo de una regla que la base no puede imponer
 * sola.</strong> La especificacion (§6.4) lo dice con nombre: «un hecho tardio
 * se reconoce en el primer periodo abierto», y cual es exactamente el primero
 * abierto <b>no lo sabe el motor</b> en el momento del {@code INSERT} sin una
 * consulta de conjunto que ademas cambiaria el resultado segun el orden de las
 * filas. Lo que si pone la base son las dos redes: el disparador
 * {@code trg_rrl_bi_period_open} rechaza escribir en un periodo cerrado y
 * {@code chk_rrl_not_backwards} impide ir hacia atras.
 *
 * <p>
 * <strong>Sin variante acotada por empresa</strong>, y no es un descuido:
 * {@code accounting_periods} es el calendario contable de la plataforma y no
 * lleva {@code company_id}. No hay empresa por la que acotar.
 *
 * <p>
 * Devuelve la clave y no un {@code AccountingPeriodRef}: de la otra feature no
 * se lee nada mas que el {@code period_key}, que es ademas la columna a la que
 * apunta {@code fk_rrl_posting_period}. Traer aqui un companion VO seria copiar
 * un dato que nadie usa.
 */
public interface AccountingPeriodQueryPort {

    /**
     * La clave del primer periodo <b>abierto</b> con clave mayor o igual que la
     * dada, si lo hay.
     *
     * <p>
     * El {@code >=} sobre {@code CHAR(7) ascii_bin} ordena como el calendario
     * porque el formato es {@code AAAA-MM}: el orden lexicografico <b>es</b> el
     * cronologico.
     */
    Optional<String> findFirstOpenPostingPeriodFrom(String periodKey);
}
