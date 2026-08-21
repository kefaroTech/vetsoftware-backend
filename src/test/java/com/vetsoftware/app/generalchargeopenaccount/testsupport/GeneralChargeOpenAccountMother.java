package com.vetsoftware.app.generalchargeopenaccount.testsupport;

import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo generalchargeopenaccount.
 *
 * <p>
 * Los cargos se construyen con el constructor publico y no con
 * {@code GeneralChargeOpenAccount.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * A diferencia del cargo de servicio, aqui el importe lo teclea el usuario:
 * precio unitario por cantidad. 5.950 x 2 = 11.900 con IVA 19 % incluido =
 * 10.000 de base + 1.900 de impuesto, asi que un fallo de aritmetica se lee de
 * un vistazo.
 */
public final class GeneralChargeOpenAccountMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long OPEN_ACCOUNT_ID = 50L;
    public static final Long OTRA_CUENTA_ID = 51L;
    public static final Long CHARGE_ID = 100L;

    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "Ana Ruiz");
    public static final EmployeeRef OTRO_EMPLEADO = new EmployeeRef(8L, "Luis Paz");
    public static final OpenAccountRef CUENTA = new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID);
    public static final OpenAccountRef OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA_ID, COMPANY_ID);
    public static final OpenAccountRef CUENTA_AJENA = new OpenAccountRef(OPEN_ACCOUNT_ID,
            OTRA_COMPANY_ID);

    public static final TaxRef IVA_19 = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"), "IVA");
    public static final TaxRef IVA_0 = new TaxRef(5L, "IVA 0%", BigDecimal.ZERO, "IVA");

    public static final String NOMBRE = "Traslado en ambulancia";
    public static final BigDecimal UNITARIO = new BigDecimal("5950");
    public static final BigDecimal CANTIDAD = new BigDecimal("2");
    public static final BigDecimal BASE = new BigDecimal("10000.00");
    public static final BigDecimal IMPUESTO = new BigDecimal("1900.00");
    public static final BigDecimal TOTAL = new BigDecimal("11900.00");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime ANULADO = LocalDateTime.of(2026, 2, 1, 8, 0);

    private GeneralChargeOpenAccountMother() {
    }

    /** Cargo activo con IVA 19 % ya desglosado. */
    public static GeneralChargeOpenAccount cargo() {
        return cargo(CHARGE_ID);
    }

    public static GeneralChargeOpenAccount cargo(Long id) {
        return new GeneralChargeOpenAccount(id, NOMBRE, UNITARIO, CANTIDAD, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", BASE, IMPUESTO, TOTAL, CUENTA, EMPLEADO,
                CREADO, null, true, false, null, null, null, null);
    }

    /** Cargo con la idempotency key ya usada por un intento anterior. */
    public static GeneralChargeOpenAccount cargoConClave(String clientRequestId) {
        return new GeneralChargeOpenAccount(CHARGE_ID, NOMBRE, UNITARIO, CANTIDAD, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", BASE, IMPUESTO, TOTAL, CUENTA, EMPLEADO,
                CREADO, null, true, false, null, null, null, clientRequestId);
    }

    public static GeneralChargeOpenAccount cargoAnulado() {
        return new GeneralChargeOpenAccount(CHARGE_ID, NOMBRE, UNITARIO, CANTIDAD, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", BASE, IMPUESTO, TOTAL, CUENTA, EMPLEADO,
                CREADO, null, true, true, OTRO_EMPLEADO, ANULADO, "Cobrado por error", null);
    }

    /** Cargo cuya cuenta pertenece a OTRA empresa: dispara el guard de tenancy. */
    public static GeneralChargeOpenAccount cargoDeOtraEmpresa() {
        return new GeneralChargeOpenAccount(CHARGE_ID, NOMBRE, UNITARIO, CANTIDAD, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", BASE, IMPUESTO, TOTAL, CUENTA_AJENA,
                EMPLEADO, CREADO, null, true, false, null, null, null, null);
    }

    /** Cargo sin impuesto: base = total = importe, impuesto en cero. */
    public static GeneralChargeOpenAccount cargoSinImpuesto() {
        return new GeneralChargeOpenAccount(CHARGE_ID, NOMBRE, UNITARIO, CANTIDAD, null, false,
                null, null, null, TOTAL, new BigDecimal("0.00"), TOTAL, CUENTA, EMPLEADO, CREADO,
                null, true, false, null, null, null, null);
    }

    public static CreateGeneralChargeOpenAccountCommand comandoCrear() {
        return comandoCrear(null);
    }

    public static CreateGeneralChargeOpenAccountCommand comandoCrear(String clientRequestId) {
        return new CreateGeneralChargeOpenAccountCommand(NOMBRE, UNITARIO, CANTIDAD, IVA_19.id(),
                OPEN_ACCOUNT_ID, COMPANY_ID, EMPLEADO.id(), clientRequestId, null);
    }

    /** Alta sin impuesto: el comando no trae taxId. */
    public static CreateGeneralChargeOpenAccountCommand comandoCrearSinImpuesto() {
        return new CreateGeneralChargeOpenAccountCommand(NOMBRE, UNITARIO, CANTIDAD, null,
                OPEN_ACCOUNT_ID, COMPANY_ID, EMPLEADO.id(), null, null);
    }

    public static VoidGeneralChargeOpenAccountCommand comandoAnular() {
        return new VoidGeneralChargeOpenAccountCommand(CHARGE_ID, COMPANY_ID, OTRO_EMPLEADO.id(),
                "Cobrado por error", null);
    }
}
