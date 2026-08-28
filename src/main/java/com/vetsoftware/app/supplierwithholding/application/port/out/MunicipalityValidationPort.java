package com.vetsoftware.app.supplierwithholding.application.port.out;

/**
 * La clave foranea {@code fk_sw_municipality} contra {@code cities.dane_code},
 * que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort} porque de este slice <b>no se
 * lee un solo campo</b> del municipio: la retencion se archiva bajo su codigo
 * DIVIPOLA, que es como lo nombra la declaracion de ICA.
 *
 * <p>
 * <strong>Sin variante acotada por empresa</strong>: {@code cities} es
 * geografia compartida y no lleva {@code company_id}.
 */
public interface MunicipalityValidationPort {

    boolean existsByDaneCode(String daneCode);
}
