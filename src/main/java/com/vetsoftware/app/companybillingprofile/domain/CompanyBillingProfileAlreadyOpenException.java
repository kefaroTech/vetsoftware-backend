package com.vetsoftware.app.companybillingprofile.domain;

/**
 * La empresa ya tiene ficha de facturacion vigente y se pidio abrir otra.
 *
 * <p>
 * <strong>Es un conflicto, no un cuerpo mal formado</strong>: los datos que
 * llegan pueden ser perfectos y lo que choca es el estado de la empresa en este
 * instante. Quien se equivoco no tiene que corregir un campo, tiene que usar la
 * <em>sucesion</em>: cerrar la vigente y abrir la nueva en la misma operacion.
 *
 * <p>
 * <strong>La comprobacion que lanza esto no sustituye a
 * {@code uq_company_billing_profiles_current}: la traduce.</strong> Entre la
 * lectura y el {@code INSERT} cabe otra transaccion, asi que lo unico que de
 * verdad garantiza que no haya dos fichas vigentes es la columna generada y su
 * indice unico. Lo que aporta esta excepcion es que el caso comun —dos pestañas
 * abiertas, el boton pulsado dos veces— conteste un 409 que dice que hacer, en
 * lugar de un 500 con un {@code Duplicate entry} del driver sobre una columna
 * que el cliente ni sabe que existe.
 */
public class CompanyBillingProfileAlreadyOpenException extends RuntimeException {

    public CompanyBillingProfileAlreadyOpenException(Long companyId) {
        super("Company " + companyId + " already has a current billing profile");
    }
}
