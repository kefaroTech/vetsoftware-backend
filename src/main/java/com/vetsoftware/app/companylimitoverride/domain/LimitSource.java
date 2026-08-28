package com.vetsoftware.app.companylimitoverride.domain;

/**
 * De dónde salió el techo que rige, en orden de precedencia descendente.
 *
 * <p>
 * Se guarda <em>ya resuelto</em> junto al contador, con su origen dentro, para
 * que el servidor no tenga que resolver la precedencia en cada petición y para
 * que «¿por qué tengo 300 y no 100?» se responda mirando una fila en vez de
 * cruzando cuatro tablas.
 *
 * <p>
 * La contrapartida está escrita a propósito: la base guarda el resultado, no la
 * regla. Si el recálculo se equivoca, la fila mentirá con toda coherencia — por
 * eso la precedencia se resuelve en un solo sitio y con pruebas encima.
 */
public enum LimitSource {

    /** La excepción negociada. Manda sobre todo lo demás. */
    COMPANY_OVERRIDE,

    /** Lo que el cliente contrató, congelado al firmar. */
    SUBSCRIPTION,

    /** El escalón de fábrica del artículo. */
    CATALOG_DEFAULT,

    /**
     * No hay techo declarado por nadie. <strong>Significa techo cero, jamás
     * ilimitado</strong>, salvo el caso de D-74 que resuelve
     * {@link EffectiveLimitResolver}.
     */
    NONE
}
