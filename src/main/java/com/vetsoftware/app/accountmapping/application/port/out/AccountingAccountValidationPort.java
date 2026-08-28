package com.vetsoftware.app.accountmapping.application.port.out;

/**
 * Las tres claves foraneas de {@code account_mappings} contra
 * {@code accounting_accounts(code)}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque de este slice
 * <strong>no se lee un solo campo</strong> de la cuenta: el mapeo se archiva
 * bajo el codigo y el nombre de la cuenta lo pinta quien muestre la lista.
 * Traer aqui un {@code AccountingAccountRef} seria copiar un dato que nadie usa
 * y atar esta rodaja a la forma de otra.
 *
 * <p>
 * <strong>Apunta a {@code code} y no a {@code id}</strong>, que es lo inusual:
 * el codigo es el identificador con el que el contador nombra la cuenta, y
 * guardar el id interno obligaria a traducir en cada consulta contable. El
 * changeset 342 alineo esa columna —{@code ascii_bin} y unica— justo para que
 * estas tres claves foraneas sean posibles.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no es un descuido.</strong>
 * {@code accounting_accounts} es el plan de cuentas propio y no lleva
 * {@code company_id}, asi que
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica: no hay
 * empresa por la que acotar.
 */
public interface AccountingAccountValidationPort {

    /**
     * {@code true} si existe una cuenta con ese codigo. Devuelve un booleano en vez
     * de lanzar: la excepcion de clave foranea inexistente la decide el caso de
     * uso, nunca el adaptador.
     */
    boolean existsByCode(String code);

    /**
     * {@code true} si la cuenta existe <b>y</b> admite asiento
     * ({@code postable = true}, que por {@code chk_accounting_accounts_postable}
     * implica nivel 6).
     *
     * <p>
     * <strong>Sin esto, la unica invariante que ninguna clave foranea puede cuidar
     * se cae.</strong> {@code fk_account_mappings_debit} garantiza que la cuenta
     * <em>existe</em>, no que sea asentable: un mapeo contra un grupo pasa la clave
     * foranea, genera asientos y descuadra el balance de prueba por arrastre sin un
     * solo error.
     */
    boolean existsPostableByCode(String code);
}
