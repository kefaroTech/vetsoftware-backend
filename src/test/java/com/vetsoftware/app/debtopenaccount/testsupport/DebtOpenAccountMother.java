package com.vetsoftware.app.debtopenaccount.testsupport;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo debtopenaccount (los abonos de una cuenta abierta).
 *
 * <p>
 * Los abonos se construyen con el constructor publico y no con
 * {@code DebtOpenAccount.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * El abono vale 30.000 sobre un saldo pendiente de 50.000, que deja sitio para
 * los casos de frontera del guard de sobrepago sin tener que recalcular nada a
 * mano.
 */
public final class DebtOpenAccountMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long OPEN_ACCOUNT_ID = 50L;
    public static final Long OTRA_CUENTA_ID = 51L;
    public static final Long PAYMENT_ID = 100L;

    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "Ana Ruiz");
    public static final EmployeeRef OTRO_EMPLEADO = new EmployeeRef(8L, "Luis Paz");
    public static final OpenAccountRef CUENTA = new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID);
    public static final OpenAccountRef OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA_ID, COMPANY_ID);
    public static final OpenAccountRef CUENTA_AJENA = new OpenAccountRef(OPEN_ACCOUNT_ID,
            OTRA_COMPANY_ID);

    public static final BigDecimal MONTO = new BigDecimal("30000");
    public static final String SALDO_PENDIENTE = "50000";

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime ANULADO = LocalDateTime.of(2026, 2, 1, 8, 0);

    private DebtOpenAccountMother() {
    }

    /** Abono activo en efectivo. */
    public static DebtOpenAccount abono() {
        return abono(PAYMENT_ID);
    }

    public static DebtOpenAccount abono(Long id) {
        return new DebtOpenAccount(id, MONTO, PaymentMethod.CASH, CUENTA, EMPLEADO, CREADO, null,
                true, false, null, null, null, null);
    }

    /** Abono con la idempotency key ya usada por un intento anterior. */
    public static DebtOpenAccount abonoConClave(String clientRequestId) {
        return new DebtOpenAccount(PAYMENT_ID, MONTO, PaymentMethod.CASH, CUENTA, EMPLEADO, CREADO,
                null, true, false, null, null, null, clientRequestId);
    }

    public static DebtOpenAccount abonoAnulado() {
        return new DebtOpenAccount(PAYMENT_ID, MONTO, PaymentMethod.CASH, CUENTA, EMPLEADO, CREADO,
                null, true, true, OTRO_EMPLEADO, ANULADO, "Cobrado por error", null);
    }

    /** Abono cuya cuenta pertenece a OTRA empresa: dispara el guard de tenancy. */
    public static DebtOpenAccount abonoDeOtraEmpresa() {
        return new DebtOpenAccount(PAYMENT_ID, MONTO, PaymentMethod.CASH, CUENTA_AJENA, EMPLEADO,
                CREADO, null, true, false, null, null, null, null);
    }

    public static CreateDebtOpenAccountCommand comandoCrear() {
        return comandoCrear(null);
    }

    public static CreateDebtOpenAccountCommand comandoCrear(String clientRequestId) {
        return new CreateDebtOpenAccountCommand(MONTO, "CASH", OPEN_ACCOUNT_ID, COMPANY_ID,
                EMPLEADO.id(), clientRequestId, null);
    }

    /** Alta por un monto arbitrario, para los casos de frontera del sobrepago. */
    public static CreateDebtOpenAccountCommand comandoCrearPor(String monto) {
        return new CreateDebtOpenAccountCommand(new BigDecimal(monto), "CASH", OPEN_ACCOUNT_ID,
                COMPANY_ID, EMPLEADO.id(), null, null);
    }

    public static UpdateDebtOpenAccountCommand comandoActualizar() {
        return new UpdateDebtOpenAccountCommand(PAYMENT_ID, new BigDecimal("45000"), "CARD",
                OPEN_ACCOUNT_ID, COMPANY_ID, null);
    }

    /** Actualizacion que traslada el abono a OTRA cuenta de la misma empresa. */
    public static UpdateDebtOpenAccountCommand comandoTrasladar() {
        return new UpdateDebtOpenAccountCommand(PAYMENT_ID, MONTO, "CASH", OTRA_CUENTA_ID,
                COMPANY_ID, null);
    }

    public static VoidDebtOpenAccountCommand comandoAnular() {
        return new VoidDebtOpenAccountCommand(PAYMENT_ID, COMPANY_ID, OTRO_EMPLEADO.id(),
                "Cobrado por error", null);
    }
}
