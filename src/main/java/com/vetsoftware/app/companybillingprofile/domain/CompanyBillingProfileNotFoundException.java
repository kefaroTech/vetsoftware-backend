package com.vetsoftware.app.companybillingprofile.domain;

/**
 * No hay ficha de facturacion que devolver.
 *
 * <p>
 * <strong>Las dos formas de faltar se cuentan igual, y esa es la
 * decision.</strong> Una empresa que nunca abrio su ficha y una que la tuviera
 * toda en el historico le presentan al cliente el mismo hecho: <em>no hay ficha
 * vigente</em>, y la misma accion siguiente, que es abrir una. Partirlo en dos
 * codigos obligaria al front a pintar dos pantallas para el mismo desenlace.
 */
public class CompanyBillingProfileNotFoundException extends RuntimeException {

    public CompanyBillingProfileNotFoundException(Long id) {
        super("Company billing profile not found: " + id);
    }

    private CompanyBillingProfileNotFoundException(String message) {
        super(message);
    }

    /**
     * La empresa no tiene ficha vigente. Lo contesta tanto la consulta del tenant
     * como el intento de suceder una ficha que no existe: en los dos casos lo que
     * falta es exactamente la misma fila.
     */
    public static CompanyBillingProfileNotFoundException withoutCurrentProfile(Long companyId) {
        return new CompanyBillingProfileNotFoundException(
                "Company " + companyId + " has no current billing profile");
    }
}
