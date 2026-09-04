package com.vetsoftware.app.platformtaxprofile.domain;

/**
 * La actividad economica (CIIU) de Lumbre, con lo unico que esta feature
 * necesita saber de ella.
 *
 * <p>
 * <strong>Companion VO y no la entidad de {@code economicactivity}</strong>: el
 * dominio de una feature nunca importa el dominio de otra. Lo rellena
 * {@code EconomicActivityQueryPort} y lo unico que sale de aqui hacia HTTP es
 * {@code PlatformEconomicActivitySummary}, que es un record propio.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no falta ninguna.</strong>
 * {@code economic_activities} es catalogo maestro global —no tiene
 * {@code company_id} ni lo alcanza por ninguna asociacion—, asi que el
 * {@code findById} ancho del puerto <em>es</em> la consulta correcta.
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} solo exige la acotada
 * cuando la entidad referida pertenece a una empresa; y ademas esta feature no
 * tiene empresa desde la que acotar.
 *
 * <p>
 * Mismo record —{@code (Long id, String code, String name)}— que el
 * {@code EconomicActivityRef} de {@code companytaxprofile}. Son dos clases
 * distintas en dos paquetes distintos y esa duplicacion es deliberada: es el
 * precio del vertical slicing.
 */
public record EconomicActivityRef(Long id, String code, String name) {

    public EconomicActivityRef {
        if (id == null)
            throw new IllegalArgumentException("economic activity id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("economic activity code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("economic activity name is required");
    }
}
