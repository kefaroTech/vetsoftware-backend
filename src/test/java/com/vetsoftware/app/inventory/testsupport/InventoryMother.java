package com.vetsoftware.app.inventory.testsupport;

import com.vetsoftware.app.inventory.application.command.RecordCountCommand;
import com.vetsoftware.app.inventory.application.dto.KardexExportRow;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import com.vetsoftware.app.inventory.domain.InventoryCountLine;
import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.inventory.domain.StockLot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures del modulo inventory.
 *
 * <p>
 * Los agregados se construyen con el constructor publico y no con los factory
 * {@code create(...)}: esos ponen {@code LocalDateTime.now()} y harian no
 * deterministas las aserciones sobre fechas.
 *
 * <p>
 * El conteo de referencia tiene una linea que sobra (+3), una que falta (−2) y
 * una que cuadra: los tres caminos del ajuste en una sola sesion.
 */
public final class InventoryMother {

    public static final Long COMPANY_ID = 1L;
    public static final Long OTRA_COMPANY_ID = 2L;
    public static final Long BRANCH_ID = 10L;
    public static final Long OTRA_BRANCH_ID = 20L;
    public static final Long PRODUCT_ID = 100L;
    public static final Long OTRO_PRODUCT_ID = 200L;
    public static final Long TERCER_PRODUCT_ID = 300L;
    public static final Long COUNT_ID = 500L;
    public static final Long LOT_ID = 700L;
    public static final Long EMPLEADO_ID = 7L;

    public static final BigDecimal COSTO = new BigDecimal("1500");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDate VENCE = LocalDate.of(2027, 6, 30);

    private InventoryMother() {
    }

    // ── Conteo fisico ───────────────────────────────────────────────────────

    /** Conteo con una linea que sobra (+3), una que falta (−2) y una que cuadra. */
    public static InventoryCount conteo() {
        return conteo(COUNT_ID);
    }

    public static InventoryCount conteo(Long id) {
        return new InventoryCount(id, COMPANY_ID, BRANCH_ID, "Conteo ciclico de enero", EMPLEADO_ID,
                CREADO, true, List.of(linea(PRODUCT_ID, 10, 13), linea(OTRO_PRODUCT_ID, 8, 6),
                        linea(TERCER_PRODUCT_ID, 5, 5)));
    }

    /** Conteo en el que todas las lineas cuadran: no genera ningun ajuste. */
    public static InventoryCount conteoQueCuadra() {
        return new InventoryCount(COUNT_ID, COMPANY_ID, BRANCH_ID, null, EMPLEADO_ID, CREADO, true,
                List.of(linea(PRODUCT_ID, 10, 10), linea(OTRO_PRODUCT_ID, 8, 8)));
    }

    public static InventoryCountLine linea(Long productId, int sistema, int contado) {
        return new InventoryCountLine(null, productId, sistema, contado);
    }

    public static RecordCountCommand comandoContar() {
        return new RecordCountCommand(COMPANY_ID, BRANCH_ID, "Conteo ciclico de enero", EMPLEADO_ID,
                List.of(new RecordCountCommand.Line(PRODUCT_ID, 13),
                        new RecordCountCommand.Line(OTRO_PRODUCT_ID, 6),
                        new RecordCountCommand.Line(TERCER_PRODUCT_ID, 5)));
    }

    // ── Saldo y lotes ───────────────────────────────────────────────────────

    public static StockBalance saldo(int cantidad) {
        return new StockBalance(1L, COMPANY_ID, BRANCH_ID, PRODUCT_ID, cantidad, 5, 3L);
    }

    public static StockLot lote(int disponible) {
        return new StockLot(LOT_ID, COMPANY_ID, BRANCH_ID, PRODUCT_ID, "L-2026-01", VENCE,
                disponible, COSTO, CREADO, CREADO, true);
    }

    // ── Filas de reporte ────────────────────────────────────────────────────

    /**
     * Fila cruda del kardex, con la cantidad ya con signo (entrada +, salida −).
     */
    public static KardexExportRow filaKardex(String tipo, String referenceType, Long referenceId,
            int cantidadConSigno) {
        return new KardexExportRow("Amoxicilina 500mg", "SKU-100", "Sede Centro", CREADO, tipo,
                referenceType, referenceId, LOT_ID, cantidadConSigno, COSTO);
    }

    /** Renglon del libro de compras: la cantidad de una entrada es positiva. */
    public static PurchaseView compra(Long id, int cantidad, String total) {
        return new PurchaseView(id, PRODUCT_ID, "Amoxicilina 500mg", "SKU-100", LOT_ID, BRANCH_ID,
                "Sede Centro", cantidad, COSTO, new BigDecimal(total), 900L, CREADO);
    }
}
