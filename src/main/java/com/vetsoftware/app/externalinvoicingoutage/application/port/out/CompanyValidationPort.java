package com.vetsoftware.app.externalinvoicingoutage.application.port.out;

/**
 * Comprueba que la clinica a la que se va a repartir una caida existe.
 *
 * <p>
 * <strong>Valida existencia y nada mas, y por eso es un {@code ValidationPort}
 * y no un {@code QueryPort}.</strong> El reparto guarda un
 * {@code Long companyId} y no necesita un solo campo de la empresa: ni el
 * nombre ni el NIT entran en la puente. Traerse un companion VO seria pagar una
 * consulta mas ancha para tirar el resultado, que es justo el caso que
 * {@code CLAUDE.md} deja fuera del patron de referencia cross-feature.
 *
 * <p>
 * Sin esta comprobacion la clave foranea {@code fk_eioc_company} rechazaria la
 * fila igualmente, pero como un error de integridad en vez de como el «esa
 * empresa no existe» que corresponde.
 */
public interface CompanyValidationPort {

    boolean existsById(Long companyId);
}
