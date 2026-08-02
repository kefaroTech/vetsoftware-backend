package com.vetsoftware.app.purchasereport.infrastructure.web;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/** Serializa el libro de compras a CSV (UTF-8 con BOM para que Excel respete los acentos). */
final class PurchaseBookCsv {

  private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  // BOM UTF-8: hace que Excel abra el archivo con la codificación correcta (acentos/ñ).
  private static final String BOM = "﻿";

  private PurchaseBookCsv() {}

  static byte[] purchaseBook(PurchaseBookDto b) {
    StringBuilder sb = new StringBuilder(BOM);
    row(sb, "Libro de compras");
    row(
        sb,
        "Desde",
        b.dateFrom() == null ? "" : D.format(b.dateFrom()),
        "Hasta",
        b.dateTo() == null ? "" : D.format(b.dateTo()));
    row(sb);
    row(
        sb,
        "Proveedor",
        "NIT",
        "Factura",
        "Fecha",
        "Vence",
        "Base",
        "Impuesto",
        "Retención",
        "Total",
        "Pagado",
        "Saldo",
        "Estado");
    for (PurchaseBookDto.EntryDto e : b.entries()) {
      row(
          sb,
          e.supplierName(),
          nn(e.supplierTaxId()),
          e.invoiceNumber(),
          e.issueDate() == null ? "" : D.format(e.issueDate()),
          e.dueDate() == null ? "" : D.format(e.dueDate()),
          plain(e.subtotal()),
          plain(e.taxAmount()),
          plain(e.withholdingAmount()),
          plain(e.total()),
          plain(e.paidAmount()),
          plain(e.balance()),
          e.status());
    }
    PurchaseBookDto.TotalsDto t = b.totals();
    row(sb);
    row(
        sb,
        "TOTAL (" + t.invoiceCount() + ")",
        "",
        "",
        "",
        "",
        plain(t.subtotal()),
        plain(t.taxAmount()),
        plain(t.withholdingAmount()),
        plain(t.total()),
        plain(t.paidAmount()),
        plain(t.balance()),
        "");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void row(StringBuilder sb, String... cells) {
    for (int i = 0; i < cells.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(escape(cells[i]));
    }
    sb.append("\r\n");
  }

  /** Escapa comillas/comas/saltos entre comillas dobles (RFC 4180). */
  private static String escape(String value) {
    if (value == null) return "";
    boolean needsQuotes =
        value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
    String v = value.replace("\"", "\"\"");
    return needsQuotes ? "\"" + v + "\"" : v;
  }

  private static String plain(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  private static String nn(String value) {
    return value == null ? "" : value;
  }
}
