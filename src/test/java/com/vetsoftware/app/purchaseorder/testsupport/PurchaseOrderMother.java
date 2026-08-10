package com.vetsoftware.app.purchaseorder.testsupport;

import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.domain.SupplierRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Object mother de la feature {@code purchaseorder}: valores validos por
 * defecto y una variante por escenario. Los agregados se construyen de verdad
 * —nunca se mockean— para que cada test pase por las invariantes reales.
 */
public final class PurchaseOrderMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long ACTOR_ID = 77L;

    public static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Veterinaria", "900123456");
    public static final BranchRef SEDE_NORTE = new BranchRef(4L, "Sede Norte");
    public static final BranchRef SEDE_SUR = new BranchRef(5L, "Sede Sur");
    public static final SupplierRef PROVEEDOR = new SupplierRef(7L, "Distribuidora Animal");
    public static final SupplierRef OTRO_PROVEEDOR = new SupplierRef(8L, "Insumos Vet");
    public static final ProductRef VACUNA = new ProductRef(11L, "Vacuna Triple", "VAC-001");
    public static final ProductRef JERINGA = new ProductRef(12L, "Jeringa 5 ml", "JER-005");

    public static final LocalDate FECHA_ORDEN = LocalDate.of(2026, 3, 10);
    public static final LocalDate FECHA_ESPERADA = LocalDate.of(2026, 3, 20);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 10, 8, 0);
    public static final LocalDateTime ACTUALIZADO = LocalDateTime.of(2026, 3, 11, 9, 30);

    private PurchaseOrderMother() {
    }

    /** Linea nueva (sin id) de 10 unidades de vacuna a 15000. */
    public static PurchaseOrderLine lineaNueva() {
        return PurchaseOrderLine.create(VACUNA, 10, new BigDecimal("15000.00"));
    }

    /** Linea persistida con la cantidad recibida que se le indique. */
    public static PurchaseOrderLine linea(Long id, ProductRef producto, int pedida, int recibida) {
        return new PurchaseOrderLine(id, producto, pedida, new BigDecimal("15000.00"), recibida);
    }

    /** Orden en DRAFT con una sola linea de 10 vacunas sin recibir. */
    public static PurchaseOrder borrador() {
        return enEstado(PurchaseOrderStatus.DRAFT, List.of(linea(100L, VACUNA, 10, 0)));
    }

    /** Orden emitida (PLACED) con una sola linea de 10 vacunas sin recibir. */
    public static PurchaseOrder emitida() {
        return enEstado(PurchaseOrderStatus.PLACED, List.of(linea(100L, VACUNA, 10, 0)));
    }

    /** Orden emitida con dos lineas (vacuna y jeringa), nada recibido. */
    public static PurchaseOrder emitidaConDosLineas() {
        return enEstado(PurchaseOrderStatus.PLACED,
                List.of(linea(100L, VACUNA, 10, 0), linea(200L, JERINGA, 4, 0)));
    }

    /** Orden en el estado pedido con las lineas que se le pasen. */
    public static PurchaseOrder enEstado(PurchaseOrderStatus status,
            List<PurchaseOrderLine> lineas) {
        return new PurchaseOrder(1L, CLINICA, SEDE_NORTE, PROVEEDOR, status, FECHA_ORDEN,
                FECHA_ESPERADA, "Pedido mensual", lineas, CREADO, ACTOR_ID, null, null, 0L, true);
    }

    /** Orden pausada (soft-delete): {@code enabled=false}. */
    public static PurchaseOrder pausada() {
        return new PurchaseOrder(1L, CLINICA, SEDE_NORTE, PROVEEDOR, PurchaseOrderStatus.DRAFT,
                FECHA_ORDEN, FECHA_ESPERADA, null, List.of(linea(100L, VACUNA, 10, 0)), CREADO,
                ACTOR_ID, ACTUALIZADO, ACTOR_ID, 3L, false);
    }
}
