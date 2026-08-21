package com.vetsoftware.app.servicechargeopenaccount.testsupport;

import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.command.VoidServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo servicechargeopenaccount.
 *
 * <p>
 * Los cargos se construyen con el constructor publico y no con
 * {@code ServiceChargeOpenAccount.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * Los importes estan elegidos para que el desglose de IVA 19 % sea exacto:
 * 11.900 con IVA = 10.000 de base + 1.900 de impuesto. Un fallo de aritmetica
 * se lee de un vistazo.
 */
public final class ServiceChargeOpenAccountMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long OPEN_ACCOUNT_ID = 50L;
    public static final Long CHARGE_ID = 100L;

    public static final AnimalRef ANIMAL = new AnimalRef(1L, "Firulais", "A-001");
    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "Ana Ruiz");
    public static final EmployeeRef OTRO_EMPLEADO = new EmployeeRef(8L, "Luis Paz");
    public static final OpenAccountRef CUENTA = new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID);
    public static final OpenAccountRef CUENTA_AJENA = new OpenAccountRef(OPEN_ACCOUNT_ID,
            OTRA_COMPANY_ID);

    public static final TaxRef IVA_19 = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"), "IVA");
    public static final TaxRef IVA_0 = new TaxRef(5L, "IVA 0%", BigDecimal.ZERO, "IVA");

    public static final BigDecimal PRECIO = new BigDecimal("11900");
    public static final BigDecimal BASE = new BigDecimal("10000.00");
    public static final BigDecimal IMPUESTO = new BigDecimal("1900.00");
    public static final BigDecimal TOTAL = new BigDecimal("11900.00");

    public static final ServiceRef SERVICIO = new ServiceRef(2L, "Consulta general", PRECIO, true,
            IVA_19, "GRAVADO");
    public static final ServiceRef SERVICIO_SIN_IMPUESTO = new ServiceRef(3L, "Bano",
            new BigDecimal("5000"));

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime ANULADO = LocalDateTime.of(2026, 2, 1, 8, 0);

    private ServiceChargeOpenAccountMother() {
    }

    /** Cargo activo con IVA 19 % ya desglosado. */
    public static ServiceChargeOpenAccount cargo() {
        return cargo(CHARGE_ID);
    }

    public static ServiceChargeOpenAccount cargo(Long id) {
        return new ServiceChargeOpenAccount(id, ANIMAL, SERVICIO, PRECIO, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", BASE, IMPUESTO, TOTAL, CUENTA,
                EMPLEADO, CREADO, null, true, false, null, null, null, null);
    }

    /** Cargo con la idempotency key ya usada por un intento anterior. */
    public static ServiceChargeOpenAccount cargoConClave(String clientRequestId) {
        return new ServiceChargeOpenAccount(CHARGE_ID, ANIMAL, SERVICIO, PRECIO, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", BASE, IMPUESTO, TOTAL, CUENTA,
                EMPLEADO, CREADO, null, true, false, null, null, null, clientRequestId);
    }

    public static ServiceChargeOpenAccount cargoAnulado() {
        return new ServiceChargeOpenAccount(CHARGE_ID, ANIMAL, SERVICIO, PRECIO, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", BASE, IMPUESTO, TOTAL, CUENTA,
                EMPLEADO, CREADO, null, true, true, OTRO_EMPLEADO, ANULADO, "Cobrado por error",
                null);
    }

    /** Cargo cuya cuenta pertenece a OTRA empresa: dispara el guard de tenancy. */
    public static ServiceChargeOpenAccount cargoDeOtraEmpresa() {
        return new ServiceChargeOpenAccount(CHARGE_ID, ANIMAL, SERVICIO, PRECIO, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", BASE, IMPUESTO, TOTAL,
                CUENTA_AJENA, EMPLEADO, CREADO, null, true, false, null, null, null, null);
    }

    /** Cargo sin impuesto: base = total = precio, impuesto en cero. */
    public static ServiceChargeOpenAccount cargoSinImpuesto() {
        return new ServiceChargeOpenAccount(CHARGE_ID, ANIMAL, SERVICIO_SIN_IMPUESTO,
                new BigDecimal("5000"), CUENTA, EMPLEADO, CREADO, true);
    }

    public static CreateServiceChargeOpenAccountCommand comandoCrear() {
        return comandoCrear(null);
    }

    public static CreateServiceChargeOpenAccountCommand comandoCrear(String clientRequestId) {
        return new CreateServiceChargeOpenAccountCommand(ANIMAL.id(), SERVICIO.id(),
                OPEN_ACCOUNT_ID, COMPANY_ID, EMPLEADO.id(), clientRequestId, null);
    }

    public static VoidServiceChargeOpenAccountCommand comandoAnular() {
        return new VoidServiceChargeOpenAccountCommand(CHARGE_ID, COMPANY_ID, OTRO_EMPLEADO.id(),
                "Cobrado por error", null);
    }
}
