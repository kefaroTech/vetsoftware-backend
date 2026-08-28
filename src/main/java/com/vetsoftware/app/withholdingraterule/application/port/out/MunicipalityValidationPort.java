package com.vetsoftware.app.withholdingraterule.application.port.out;

/**
 * La FK {@code withholding_rate_rules.municipality_code} contra
 * {@code cities.dane_code}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque de este slice
 * <strong>no se lee un solo campo</strong> del municipio: la tarifa se archiva
 * bajo su codigo DIVIPOLA y el nombre de la ciudad lo pinta quien muestre la
 * lista. Traer aqui un {@code CityRef} seria copiar un dato que nadie usa y
 * atar esta rodaja a la forma de otra.
 *
 * <p>
 * <strong>Apunta a {@code dane_code} y no a {@code cities.id}</strong>, que es
 * lo inusual: el codigo DIVIPOLA es el identificador con el que la DIAN nombra
 * el municipio, y guardar el id interno obligaria a traducir en cada consulta
 * fiscal. El changeset 315 alineo esa columna —{@code ascii_bin} y unica— justo
 * para que esta clave foranea sea posible.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no es un descuido.</strong>
 * {@code cities} es geografia compartida y no lleva {@code company_id}, asi que
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica: no hay
 * empresa por la que acotar.
 */
public interface MunicipalityValidationPort {

    /**
     * {@code true} si existe un municipio con ese codigo DIVIPOLA. Devuelve un
     * booleano en vez de lanzar: la excepcion de FK inexistente la decide el caso
     * de uso, nunca el adaptador.
     */
    boolean existsByDaneCode(String daneCode);
}
