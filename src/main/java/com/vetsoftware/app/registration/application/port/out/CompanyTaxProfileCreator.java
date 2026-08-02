package com.vetsoftware.app.registration.application.port.out;

/**
 * Crea el perfil fiscal de la empresa recién registrada. El DV se autocalcula
 * para NIT y no se asignan responsabilidades: el alta solo necesita el tipo y
 * número de documento, la razón social, el régimen tributario y el correo
 * fiscal. El tipo de documento y el régimen viajan como String para no acoplar
 * la feature de registro al dominio de companytaxprofile (la conversión vive en
 * el adaptador de orquestación).
 */
public interface CompanyTaxProfileCreator {
    void create(Long companyId, String documentType, String documentId, String legalName,
            String taxRegime, String fiscalEmail);
}
