package com.vetsoftware.app.accountingaccount.domain;

import java.time.LocalDate;

/**
 * Se intento cerrar la vigencia de una cuenta que ya estaba cerrada.
 *
 * <p>
 * Es un conflicto (409) y no una peticion mal formada: el cuerpo es valido y lo
 * que falla es el estado de la cuenta en este instante.
 *
 * <p>
 * <strong>La base no lo impide.</strong>
 * {@code chk_accounting_accounts_validity} solo comprueba que la fecha de fin
 * sea posterior a la de inicio, asi que un segundo cierre pasaria en silencio y
 * machacaria la fecha desde la que la cuenta dejo de admitir asiento — que es
 * el dato del que depende que un asiento viejo siga teniendo explicacion.
 */
public class AccountingAccountAlreadyClosedException extends RuntimeException {

    public AccountingAccountAlreadyClosedException(Long id, LocalDate validTo) {
        super("Accounting account " + id + " is already closed since " + validTo);
    }
}
