package com.vetsoftware.app.productchargeopenaccount.testsupport;

import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import com.vetsoftware.app.productchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo productchargeopenaccount.
 *
 * <p>
 * Los cargos se construyen con el constructor publico y no con
 * {@code ProductChargeOpenAccount.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * Los importes estan elegidos para que el desglose de IVA 19 % sea exacto:
 * 11.900 con IVA = 10.000 de base + 1.900 de impuesto.
 */
public final class ProductChargeOpenAccountMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long OPEN_ACCOUNT_ID = 50L;
    public static final Long OTRA_CUENTA_ID = 51L;
    public static final Long CHARGE_ID = 100L;
    public static final Long BRANCH_ID = 3L;

    public static final AnimalRef ANIMAL = new AnimalRef(1L, "Firulais", "A-001");
    public static final AnimalRef OTRO_ANIMAL = new AnimalRef(11L, "Michi", "A-002");
    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "Ana Ruiz");
    public static final EmployeeRef OTRO_EMPLEADO = new EmployeeRef(8L, "Luis Paz");
    public static final OpenAccountRef CUENTA = new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID);
    public static final OpenAccountRef OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA_ID, COMPANY_ID);
    public static final OpenAccountRef CUENTA_AJENA = new OpenAccountRef(OPEN_ACCOUNT_ID,
            OTRA_COMPANY_ID);

    public static final TaxRef IVA_19 = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"), "IVA");
    public static final TaxRef IVA_0 = new TaxRef(5L, "IVA 0%", BigDecimal.ZERO, "IVA");

    public static final BigDecimal PRECIO = new BigDecimal("11900");
    public static final ProductRef PRODUCTO = new ProductRef(2L, "Alimento", "P-001", PRECIO, true,
            IVA_19, "GRAVADO");
    public static final ProductRef PRODUCTO_SIN_IMPUESTO = new ProductRef(3L, "Collar", "P-002",
            new BigDecimal("5000"));
    public static final ProductRef OTRO_PRODUCTO = new ProductRef(12L, "Juguete", "P-003",
            new BigDecimal("2380"), true, IVA_19, "GRAVADO");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime ANULADO = LocalDateTime.of(2026, 2, 1, 8, 0);

    private ProductChargeOpenAccountMother() {
    }

    /** Cargo activo de una unidad con IVA 19 % ya desglosado. */
    public static ProductChargeOpenAccount cargo() {
        return cargo(CHARGE_ID);
    }

    public static ProductChargeOpenAccount cargo(Long id) {
        return new ProductChargeOpenAccount(id, ANIMAL, PRODUCTO, PRECIO, 1, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", new BigDecimal("10000.00"),
                new BigDecimal("1900.00"), new BigDecimal("11900.00"), CUENTA, EMPLEADO, CREADO,
                null, true, false, null, null, null, null);
    }

    /** Cargo con la idempotency key ya usada por un intento anterior. */
    public static ProductChargeOpenAccount cargoConClave(String clientRequestId) {
        return new ProductChargeOpenAccount(CHARGE_ID, ANIMAL, PRODUCTO, PRECIO, 1, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", new BigDecimal("10000.00"),
                new BigDecimal("1900.00"), new BigDecimal("11900.00"), CUENTA, EMPLEADO, CREADO,
                null, true, false, null, null, null, clientRequestId);
    }

    public static ProductChargeOpenAccount cargoAnulado() {
        return new ProductChargeOpenAccount(CHARGE_ID, ANIMAL, PRODUCTO, PRECIO, 1, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", new BigDecimal("10000.00"),
                new BigDecimal("1900.00"), new BigDecimal("11900.00"), CUENTA, EMPLEADO, CREADO,
                null, true, true, OTRO_EMPLEADO, ANULADO, "Cobrado por error", null);
    }

    /** Cargo cuya cuenta pertenece a OTRA empresa: dispara el guard de tenancy. */
    public static ProductChargeOpenAccount cargoDeOtraEmpresa() {
        return new ProductChargeOpenAccount(CHARGE_ID, ANIMAL, PRODUCTO, PRECIO, 1, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", new BigDecimal("10000.00"),
                new BigDecimal("1900.00"), new BigDecimal("11900.00"), CUENTA_AJENA, EMPLEADO,
                CREADO, null, true, false, null, null, null, null);
    }

    /**
     * Cargo colgado de otra cuenta de la MISMA empresa (para probar el traslado).
     */
    public static ProductChargeOpenAccount cargoEnOtraCuenta() {
        return new ProductChargeOpenAccount(CHARGE_ID, ANIMAL, PRODUCTO, PRECIO, 1, IVA_19, true,
                new BigDecimal("19.00"), "IVA 19%", "IVA", "GRAVADO", new BigDecimal("10000.00"),
                new BigDecimal("1900.00"), new BigDecimal("11900.00"), OTRA_CUENTA, EMPLEADO,
                CREADO, null, true, false, null, null, null, null);
    }

    public static CreateProductChargeOpenAccountCommand comandoCrear() {
        return comandoCrear(null);
    }

    public static CreateProductChargeOpenAccountCommand comandoCrear(String clientRequestId) {
        return new CreateProductChargeOpenAccountCommand(ANIMAL.id(), PRODUCTO.id(), 2,
                OPEN_ACCOUNT_ID, COMPANY_ID, EMPLEADO.id(), BRANCH_ID, clientRequestId, null);
    }

    public static UpdateProductChargeOpenAccountCommand comandoActualizar() {
        return new UpdateProductChargeOpenAccountCommand(CHARGE_ID, ANIMAL.id(), PRODUCTO.id(),
                OPEN_ACCOUNT_ID, COMPANY_ID, null);
    }

    public static VoidProductChargeOpenAccountCommand comandoAnular() {
        return new VoidProductChargeOpenAccountCommand(CHARGE_ID, COMPANY_ID, OTRO_EMPLEADO.id(),
                "Cobrado por error", null);
    }
}
