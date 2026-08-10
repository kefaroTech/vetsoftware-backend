package com.vetsoftware.app.goodsreceipt.testsupport;

import com.vetsoftware.app.goodsreceipt.application.command.CreateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.command.UpdateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures del modulo goodsreceipt.
 *
 * <p>
 * Se usa el constructor publico del agregado y no {@code GoodsReceipt.create}:
 * el factory sella {@code LocalDateTime.now()} en {@code createdDate} y haria
 * no deterministas las aserciones sobre las fechas de auditoria.
 */
public final class GoodsReceiptMother {

    public static final Long RECEIPT_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    public static final Long PURCHASE_ORDER_ID = 88L;
    public static final Long ACTOR_ID = 77L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900123");
    public static final BranchRef SEDE = new BranchRef(4L, "Sede Norte");
    public static final SupplierRef PROVEEDOR = new SupplierRef(7L, "Distribuidora Vet");

    public static final BranchRef OTRA_SEDE = new BranchRef(5L, "Sede Sur");
    public static final SupplierRef OTRO_PROVEEDOR = new SupplierRef(8L, "Insumos Andinos");

    public static final ProductRef VACUNA = new ProductRef(21L, "Vacuna triple", "P-021");
    public static final ProductRef JERINGA = new ProductRef(22L, "Jeringa 5 ml", "P-022");

    public static final LocalDate FECHA_RECEPCION = LocalDate.of(2026, 3, 12);
    public static final LocalDate VENCIMIENTO = LocalDate.of(2027, 1, 31);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 12, 9, 15);
    public static final LocalDateTime ACTUALIZADO = LocalDateTime.of(2026, 3, 13, 8, 0);

    public static final BigDecimal COSTO = new BigDecimal("12.50");

    private GoodsReceiptMother() {
    }

    /** Linea sin enlace a orden de compra. El caso por defecto. */
    public static GoodsReceiptLine linea() {
        return new GoodsReceiptLine(300L, VACUNA, null, "LOTE-A", VENCIMIENTO, 10, COSTO);
    }

    /** Linea enlazada a una linea de orden de compra (recepcion parcial). */
    public static GoodsReceiptLine lineaDeOrden(Long purchaseOrderLineId) {
        return new GoodsReceiptLine(301L, JERINGA, purchaseOrderLineId, "LOTE-B", VENCIMIENTO, 4,
                new BigDecimal("3.00"));
    }

    /** Recepcion en DRAFT, sin orden de compra, con una sola linea. */
    public static GoodsReceipt enBorrador() {
        return conEstado(GoodsReceiptStatus.DRAFT);
    }

    /** Recepcion ya CONFIRMED, sin orden de compra. */
    public static GoodsReceipt confirmada() {
        return conEstado(GoodsReceiptStatus.CONFIRMED);
    }

    /** Recepcion ya CANCELLED, sin orden de compra. */
    public static GoodsReceipt cancelada() {
        return conEstado(GoodsReceiptStatus.CANCELLED);
    }

    public static GoodsReceipt conEstado(GoodsReceiptStatus status) {
        return new GoodsReceipt(RECEIPT_ID, CLINICA, SEDE, PROVEEDOR, null, FECHA_RECEPCION,
                "FV-1001", "Entrega parcial", status, List.of(linea()), CREADO, ACTOR_ID, null,
                null, 0L, true);
    }

    /**
     * Recepcion originada en una orden de compra: la primera linea enlaza a la
     * linea 900 de la orden y la segunda es una linea suelta sin enlace.
     */
    public static GoodsReceipt conOrdenDeCompra(GoodsReceiptStatus status) {
        return new GoodsReceipt(RECEIPT_ID, CLINICA, SEDE, PROVEEDOR, PURCHASE_ORDER_ID,
                FECHA_RECEPCION, "FV-1001", "Entrega parcial", status,
                List.of(lineaDeOrden(900L), linea()), CREADO, ACTOR_ID, null, null, 0L, true);
    }

    /** Recepcion con dos lineas y sin orden de compra. */
    public static GoodsReceipt conDosLineas(GoodsReceiptStatus status) {
        return new GoodsReceipt(RECEIPT_ID, CLINICA, SEDE, PROVEEDOR, null, FECHA_RECEPCION,
                "FV-1001", "Entrega parcial", status, List.of(linea(), lineaDeOrden(null)), CREADO,
                ACTOR_ID, ACTUALIZADO, ACTOR_ID, 3L, true);
    }

    public static GoodsReceiptLineCommand comandoLinea() {
        return new GoodsReceiptLineCommand(VACUNA.id(), null, "LOTE-A", VENCIMIENTO, 10, COSTO);
    }

    public static GoodsReceiptLineCommand comandoLineaDeOrden(Long purchaseOrderLineId) {
        return new GoodsReceiptLineCommand(JERINGA.id(), purchaseOrderLineId, "LOTE-B", VENCIMIENTO,
                4, new BigDecimal("3.00"));
    }

    /**
     * Comando de creacion coherente con las refs de arriba. Sin orden de compra.
     */
    public static CreateGoodsReceiptCommand comandoCrear() {
        return comandoCrear(List.of(comandoLinea()));
    }

    public static CreateGoodsReceiptCommand comandoCrear(List<GoodsReceiptLineCommand> lineas) {
        return new CreateGoodsReceiptCommand(SEDE.id(), PROVEEDOR.id(), null, FECHA_RECEPCION,
                "FV-1001", "Entrega parcial", lineas, COMPANY_ID, ACTOR_ID);
    }

    /** Comando de actualizacion que mueve sede, proveedor y linea. */
    public static UpdateGoodsReceiptCommand comandoActualizar() {
        return comandoActualizar(List.of(comandoLineaDeOrden(900L)));
    }

    public static UpdateGoodsReceiptCommand comandoActualizar(
            List<GoodsReceiptLineCommand> lineas) {
        return new UpdateGoodsReceiptCommand(RECEIPT_ID, OTRA_SEDE.id(), OTRO_PROVEEDOR.id(),
                PURCHASE_ORDER_ID, LocalDate.of(2026, 4, 1), "FV-2002", "Corregida", lineas,
                COMPANY_ID, ACTOR_ID, 3L);
    }

    public static SearchGoodsReceiptsCommand comandoBuscar() {
        return new SearchGoodsReceiptsCommand(COMPANY_ID, PROVEEDOR.id(), SEDE.id(),
                GoodsReceiptStatus.DRAFT, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 0,
                20);
    }
}
