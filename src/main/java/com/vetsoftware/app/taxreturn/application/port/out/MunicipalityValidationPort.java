package com.vetsoftware.app.taxreturn.application.port.out;

/**
 * La clave foranea {@code fk_tax_returns_municipality} contra
 * {@code cities.dane_code}, que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort} porque de este slice <b>no se
 * lee un solo campo</b> del municipio: la declaracion se archiva bajo su codigo
 * DIVIPOLA y el nombre lo pinta quien muestre la lista.
 *
 * <p>
 * <strong>Apunta a {@code dane_code} y no a {@code cities.id}</strong>: el
 * codigo DIVIPOLA es el identificador con el que la DIAN y los municipios
 * nombran el territorio, y guardar el id interno obligaria a traducir en cada
 * consulta fiscal. El changeset 315 alineo esa columna justo para que esta
 * clase de clave sea posible.
 *
 * <p>
 * <strong>Sin variante acotada por empresa</strong>: {@code cities} es
 * geografia compartida y no lleva {@code company_id}.
 */
public interface MunicipalityValidationPort {

    boolean existsByDaneCode(String daneCode);
}
