package com.vetsoftware.app.taxreturn.domain;

/**
 * Se intento cambiar una declaracion que ya no admite ese cambio.
 *
 * <p>
 * Es un conflicto (409): el cuerpo es valido y lo que falla es el estado de la
 * declaracion en este instante.
 *
 * <p>
 * <strong>La base impide la fila incoherente, no la transicion
 * equivocada.</strong> {@code chk_tax_returns_filed} comprueba que una
 * {@code FILED} lleve fecha, firmante, radicado, copia y firmeza; no sabe de
 * donde venia. Reeditar los importes de una declaracion ya presentada produce
 * una fila que el {@code CHECK} acepta sin una queja — y con ella, unos numeros
 * que ya no coinciden con el formulario radicado ante la DIAN. La declaracion
 * presentada <b>no se edita: se sucede</b>, y para eso esta la correccion.
 */
public class TaxReturnNotEditableException extends RuntimeException {

    public TaxReturnNotEditableException(Long id, TaxReturnStatus status) {
        super("Tax return " + id + " cannot be modified while in status " + status);
    }
}
