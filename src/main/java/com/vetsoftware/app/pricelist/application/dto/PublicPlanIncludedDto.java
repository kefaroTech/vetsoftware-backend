package com.vetsoftware.app.pricelist.application.dto;

/**
 * Un modulo que el paquete enciende, con los dias de prueba que concede.
 *
 * <p>
 * <strong>La prueba vence por linea, no por contrato</strong>
 * ({@code ModuleGrantLine.trialEndDate} lleva su propia fecha, y
 * {@code default_trial_days} es por articulo), asi que los dias van aqui y no
 * en la cabecera del plan: un plan que dijera «30 dias» a secas mentiria el dia
 * 14 al cliente cuyo modulo de caja tiene una prueba mas corta. Un paquete,
 * ademas, nunca puede ser elegible por si mismo
 * —{@code chk_catalog_items_bundle_trial} obliga a que todo {@code BUNDLE} sea
 * {@code NEVER_FREE}—, asi que este es el unico sitio del contrato publico
 * donde una prueba puede aparecer.
 *
 * <p>
 * {@code trialDays} nulo significa «este modulo no tiene prueba», no «no lo
 * sabemos».
 */
public record PublicPlanIncludedDto(String code, String name, Integer trialDays) {
}
