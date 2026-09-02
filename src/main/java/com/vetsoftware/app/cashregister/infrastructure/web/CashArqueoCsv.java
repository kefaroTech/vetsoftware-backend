package com.vetsoftware.app.cashregister.infrastructure.web;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.dto.CashMovementView;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Serializa el arqueo a CSV (UTF-8 con BOM para que Excel respete los acentos).
 */
final class CashArqueoCsv {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // BOM UTF-8: hace que Excel abra el archivo con la codificación correcta
    // (acentos/ñ).
    private static final String BOM = "﻿";

    private CashArqueoCsv() {
    }

    static byte[] arqueo(CashArqueoReport r) {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "Arqueo de caja");
        row(sb, "Sesión", r.sessionId() == null ? "" : String.valueOf(r.sessionId()));
        row(sb, "Sede", r.branchId() == null ? "" : String.valueOf(r.branchId()));
        row(sb, "Terminal", r.terminal());
        row(sb, "Estado", r.status() == null ? "" : r.status().name());
        row(sb, "Apertura", r.openedAt() == null ? "" : DT.format(r.openedAt()));
        row(sb, "Cierre", r.closedAt() == null ? "" : DT.format(r.closedAt()));
        row(sb, "Base inicial", plain(r.openingFloat()));
        row(sb);

        row(sb, "Método", "Base", "Ventas", "Abonos", "Ingresos", "Retiros", "Gastos", "Reversas",
                "Esperado", "Contado", "Diferencia");
        for (CashArqueoReport.MethodRow m : r.methods()) {
            row(sb, methodLabel(m.method()), plain(m.opening()), plain(m.salesIn()),
                    plain(m.accountIn()), plain(m.manualIn()), plain(m.withdrawals()),
                    plain(m.expenses()), plain(m.voidOut()), plain(m.expected()),
                    plain(m.counted()), plain(m.difference()));
        }
        row(sb, "TOTAL", "", "", "", "", "", "", "", plain(r.totalExpected()),
                plain(r.totalCounted()), plain(r.totalDifference()));
        row(sb);

        row(sb, "Fecha", "Tipo", "Método", "Monto", "Referencia", "Nota");
        for (CashMovementView mv : r.movements()) {
            row(sb, DT.format(mv.createdAt()), typeLabel(mv.type()), methodLabel(mv.method()),
                    signed(mv.type(), mv.amount()),
                    mv.referenceId() == null ? "" : (mv.referenceType() + " #" + mv.referenceId()),
                    mv.note() == null ? "" : mv.note());
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String methodLabel(CashPaymentMethod m) {
        if (m == null)
            return "";
        return switch (m) {
            case CASH -> "Efectivo";
            case CARD -> "Tarjeta";
            case TRANSFER -> "Transferencia";
        };
    }

    static String typeLabel(CashMovementType t) {
        if (t == null)
            return "";
        return switch (t) {
            case SALE_IN -> "Venta";
            case OPEN_ACCOUNT_IN -> "Abono";
            case MANUAL_IN -> "Ingreso";
            case WITHDRAWAL -> "Retiro";
            case EXPENSE -> "Gasto";
            case VOID_OUT -> "Reversa";
        };
    }

    /**
     * Monto con signo según el tipo (entra + / sale −), para el listado de
     * movimientos.
     */
    private static String signed(CashMovementType type, BigDecimal amount) {
        if (amount == null)
            return "0";
        BigDecimal v = (type != null && !type.isInflow()) ? amount.negate() : amount;
        return v.stripTrailingZeros().toPlainString();
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
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
