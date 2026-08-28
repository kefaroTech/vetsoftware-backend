package com.vetsoftware.app.accountingaccount.testsupport;

import com.vetsoftware.app.accountingaccount.application.command.CloseAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.command.CreateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.command.UpdateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo accountingaccount.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code AccountingAccount.create(...)}: eso deja al test decidir el id y la
 * version, que es lo que necesita un caso que cierra o actualiza una cuenta ya
 * publicada.
 */
public final class AccountingAccountMother {

    public static final Long ACCOUNT_ID = 500L;
    public static final String CODE = "110506";
    public static final String PARENT_CODE = "1105";
    public static final String NAME = "Caja general";
    public static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 1, 9, 0);

    private AccountingAccountMother() {
    }

    /** Cuenta de nivel 6, postable, vigente. El caso por defecto. */
    public static AccountingAccount cuentaPostable() {
        return cuentaPostable(ACCOUNT_ID);
    }

    public static AccountingAccount cuentaPostable(Long id) {
        return new AccountingAccount(id, CODE, NAME, AccountClass.ASSET, PARENT_CODE, 6, true,
                false, VALID_FROM, null, CREADO, true, 3L);
    }

    public static AccountingAccount cuentaCerrada(LocalDate cierre) {
        return new AccountingAccount(ACCOUNT_ID, CODE, NAME, AccountClass.ASSET, PARENT_CODE, 6,
                true, false, VALID_FROM, cierre, CREADO, true, 3L);
    }

    /** La raiz del plan: unico nivel sin padre y que no admite asiento. */
    public static AccountingAccount cuentaRaiz() {
        return new AccountingAccount(1L, "1", "Activo", AccountClass.ASSET, null, 1, false, false,
                VALID_FROM, null, CREADO, true, 1L);
    }

    public static CreateAccountingAccountCommand comandoCrearHija() {
        return new CreateAccountingAccountCommand(CODE, NAME, AccountClass.ASSET, PARENT_CODE, 6,
                true, false, VALID_FROM, null);
    }

    public static CreateAccountingAccountCommand comandoCrearRaiz() {
        return new CreateAccountingAccountCommand("1", "Activo", AccountClass.ASSET, null, 1, false,
                false, VALID_FROM, null);
    }

    public static UpdateAccountingAccountCommand comandoActualizar(Long id) {
        return new UpdateAccountingAccountCommand(id, "Caja general - sede norte", true);
    }

    public static CloseAccountingAccountCommand comandoCerrar(Long id, LocalDate validTo) {
        return new CloseAccountingAccountCommand(id, validTo);
    }
}
