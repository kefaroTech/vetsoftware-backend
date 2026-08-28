package com.vetsoftware.app.accountingperiod.application.command;

/**
 * Abrir un mes contable.
 *
 * <p>
 * <strong>No lleva {@code companyId} y no hay ninguno que pudiera
 * llevar.</strong> La tabla no tiene empresa: el calendario contable es de la
 * plataforma. No es la omision defensiva de un recurso scoped —aqui no hay nada
 * que suplantar—, es que el dato no existe.
 *
 * <p>
 * <strong>Ni estado ni fechas.</strong> Un periodo nace {@code OPEN} y sin
 * cierre, que es la unica combinacion que la base admite; dejar que quien llama
 * eligiera el estado inicial permitiria crear un mes ya cerrado sin que conste
 * quien lo cerro.
 *
 * @param periodKey
 *            la clave del mes en texto, {@code yyyy-MM}. Viaja como
 *            {@code String} y no como {@code AccountingPeriodKey} porque el
 *            command es la frontera con el mundo exterior: quien valida el
 *            formato es el value object del dominio al construirse, que es
 *            donde el CLAUDE.md pide las invariantes
 */
public record OpenAccountingPeriodCommand(String periodKey) {
}
