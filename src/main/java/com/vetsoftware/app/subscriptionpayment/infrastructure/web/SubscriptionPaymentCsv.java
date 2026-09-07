package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Serializa el cierre de tesorería a CSV. */
final class SubscriptionPaymentCsv {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // BOM UTF-8: hace que Excel abra el archivo con la codificación correcta
    // (acentos/ñ).
    private static final String BOM = "﻿";

    private SubscriptionPaymentCsv() {
    }

    static byte[] rows(List<SubscriptionPaymentDto> payments) {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "Empresa", "Referencia pasarela", "Llave de idempotencia", "Estado", "Importe",
                "Moneda", "Comisión", "Neto", "Reembolsado", "Referencia de liquidación",
                "Fecha de recepción", "Fecha de abono");
        for (SubscriptionPaymentDto p : payments) {
            row(sb, String.valueOf(p.companyId()), blank(p.gatewayReference()),
                    blank(p.clientRequestId()), p.status().name(), plain(p.amount()), p.currency(),
                    plain(p.feeAmount()), plain(p.netAmount()), plain(p.refundedAmount()),
                    blank(p.settlementReference()),
                    p.receivedAt() == null ? "" : DT.format(p.receivedAt()),
                    p.settledOn() == null ? "" : D.format(p.settledOn()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    /** Escapa comillas/comas/saltos entre comillas dobles (RFC 4180). */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        String v = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }
}
