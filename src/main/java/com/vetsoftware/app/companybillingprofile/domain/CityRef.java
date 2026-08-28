package com.vetsoftware.app.companybillingprofile.domain;

/**
 * El municipio de la direccion de facturacion, con lo unico que esta feature
 * necesita saber de el.
 *
 * <p>
 * <strong>Companion VO y no la entidad de {@code city}</strong>: el dominio de
 * una feature nunca importa el dominio de otra. Lo rellena
 * {@code CityQueryPort} y lo unico que sale de aqui hacia HTTP es
 * {@code CitySummary}, que es un record propio de esta feature.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no falta ninguna.</strong>
 * {@code cities} es catalogo maestro global —no tiene {@code company_id} ni lo
 * alcanza por ninguna asociacion—, asi que el {@code findById} ancho del puerto
 * <em>es</em> la consulta correcta. La regla
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} solo pide la variante
 * acotada cuando la entidad referida pertenece a una empresa; aqui no hay nada
 * que acotar y un {@code findByIdAndCompanyId} devolveria siempre vacio.
 */
public record CityRef(Long id, String name) {

    public CityRef {
        if (id == null)
            throw new IllegalArgumentException("city id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("city name is required");
    }
}
