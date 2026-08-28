package com.vetsoftware.app.documentwithholding.application.port.out;

/**
 * La FK {@code document_withholdings.municipality_code} contra
 * {@code cities.dane_code}.
 *
 * <p>
 * <strong>Apunta al codigo DIVIPOLA y no al {@code id} de la ciudad</strong>, y
 * eso es lo que hace falta saber para no equivocarse aqui: el municipio de una
 * retencion de ICA es el codigo oficial de cinco digitos, porque es el que
 * aparece en la declaracion y el que identifica la tarifa. El changeset 315
 * tuvo que alinear la colacion de esa columna antes de que esta tabla pudiera
 * nacer —cruzar dos colaciones distintas no solo desactiva el indice: MySQL ni
 * siquiera deja crear la clave foranea (errno 3780)—.
 *
 * <p>
 * <strong>Sin empresa, y no es un descuido.</strong> La geografia es un
 * catalogo global: los municipios no pertenecen a ninguna clinica, asi que no
 * hay tenant por el que acotar. La FK de la migracion tampoco es compuesta,
 * justamente por eso.
 */
public interface MunicipalityValidationPort {

    /** {@code true} si existe un municipio con ese codigo DIVIPOLA. */
    boolean existsByDaneCode(String daneCode);
}
