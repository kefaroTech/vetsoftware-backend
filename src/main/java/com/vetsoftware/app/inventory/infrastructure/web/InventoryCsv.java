package com.vetsoftware.app.inventory.infrastructure.web;

import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.KardexReportLine;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Serializa los reportes de inventario a CSV (UTF-8 con BOM para que Excel
 * respete los acentos).
 */
final class InventoryCsv {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // BOM UTF-8: hace que Excel abra el archivo con la codificación correcta
    // (acentos/ñ).
    private static final String BOM = "﻿";

    private InventoryCsv() {
    }

    static byte[] kardex(KardexReport r) {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "Fecha", "Tipo", "Referencia", "Lote", "Cantidad", "Costo unitario", "Saldo");
        // Saldo inicial como primera fila de contexto (útil cuando hay rango de
        // fechas).
        row(sb, "", "Saldo inicial", "", "", "", "", String.valueOf(r.openingBalance()));
        for (KardexReportLine l : r.lines()) {
            row(sb, DT.format(l.createdDate()), l.typeLabel(), l.referenceLabel(),
                    l.lotId() == null ? "" : String.valueOf(l.lotId()),
                    String.valueOf(l.quantity()), plain(l.unitCost()),
                    String.valueOf(l.runningBalance()));
        }
        row(sb, "", "Saldo final", "", "", "", "", String.valueOf(r.closingBalance()));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] purchases(PurchasesReport r) {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "Fecha", "Producto", "SKU", "Sede", "Lote", "Cantidad", "Costo unitario", "Total");
        for (PurchaseView p : r.lines()) {
            row(sb, DT.format(p.createdDate()), p.productName(), p.productCode(), p.branchName(),
                    p.lotId() == null ? "" : String.valueOf(p.lotId()),
                    String.valueOf(Math.abs(p.quantity())), plain(p.unitCost()), plain(p.total()));
        }
        row(sb, "", "TOTAL", "", "", "", String.valueOf(r.totalQuantity()), "",
                plain(r.totalValue()));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    /** Escapa comillas/comas/saltos entre comillas dobles (RFC 4180). */
    private static String escape(String value) {
        if (value == null)
            return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        String v = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
