package com.vetsoftware.app.registration.application.port.out;

/**
 * Crea el perfil fiscal de la empresa recién registrada. El tipo de documento es NIT, el DV se autocalcula
 * y no se asignan responsabilidades: el alta solo necesita el identificador (NIT), la razón social, el
 * régimen tributario y el correo fiscal. El régimen viaja como String para no acoplar la feature de
 * registro al dominio de companytaxprofile (la conversión vive en el adaptador de orquestación).
 */
public interface CompanyTaxProfileCreator {
    void create(Long companyId, String documentId, String legalName, String taxRegime, String fiscalEmail);
}
