package com.vetsoftware.app.accountingexport.application.port.out;

/**
 * La clave foranea {@code fk_accounting_exports_period} contra
 * {@code accounting_periods(period_key)}, que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort}: de aqui no se lee ni el estado
 * ni la fecha de cierre del periodo. Solo hace falta saber que el mes existe,
 * porque la clave es {@code RESTRICT} y una clave inventada saldria como error
 * de integridad en vez de como «ese periodo contable no existe».
 *
 * <p>
 * <strong>Que el periodo este ABIERTO no se comprueba aqui, y no es un
 * olvido.</strong> Lo impone el disparador
 * {@code trg_accounting_exports_bi_period_open} del changeset 346, que es donde
 * tiene que estar: una comprobacion previa desde Java la pasarian dos
 * peticiones concurrentes y el cierre del mes podria ocurrir entre la pregunta
 * y el {@code INSERT}.
 *
 * <p>
 * <strong>Sin variante acotada por empresa</strong>: el calendario contable es
 * de plataforma y no lleva {@code company_id}.
 */
public interface AccountingPeriodValidationPort {

    boolean existsByPeriodKey(String periodKey);
}
