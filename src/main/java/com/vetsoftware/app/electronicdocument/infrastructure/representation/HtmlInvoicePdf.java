package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoicePdfPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Representación gráfica con Thymeleaf y el motor PDF embebido. Arma un modelo de tipos planos
 * (Map/String/BigDecimal) para que Thymeleaf navegue sin getters de records.
 */
@Component
public class HtmlInvoicePdf implements InvoicePdfPort {

  private final HtmlPdfRenderer renderer;

  public HtmlInvoicePdf(HtmlPdfRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public byte[] render(ElectronicDocument doc, String qrPngBase64) {
    Map<String, Object> model = new LinkedHashMap<>();
    model.put(
        "number",
        (doc.getPrefix() == null ? "" : doc.getPrefix())
            + (doc.getConsecutive() == null ? "" : doc.getConsecutive()));
    model.put("documentType", doc.getDocumentType().name());
    model.put("issueDate", String.valueOf(doc.getIssueDate()));
    model.put("issueTime", doc.getIssueTime());
    model.put("dianStatus", doc.getDianStatus().name());
    model.put("cufe", doc.getCufe());
    model.put("cude", doc.getCude());

    model.put("issuerLegalName", doc.getIssuer().legalName());
    model.put("issuerDocumentId", doc.getIssuer().documentId());
    model.put("issuerTaxRegime", doc.getIssuer().taxRegime());
    model.put("issuerEmail", doc.getIssuer().email());

    model.put("customerName", doc.getCustomer().name());
    model.put("customerLegalName", doc.getCustomer().legalName());
    model.put("customerDocumentId", doc.getCustomer().documentId());
    model.put("customerEmail", doc.getCustomer().email());

    List<Map<String, Object>> lines = new ArrayList<>();
    Map<String, BigDecimal[]> byRate = new LinkedHashMap<>();
    for (ElectronicDocumentLine l : doc.getLines()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("description", l.getDescription());
      row.put("quantity", l.getQuantity());
      row.put("unitMeasureCode", l.getUnitMeasureCode());
      row.put("unitPrice", l.getUnitPrice());
      row.put("base", l.getLineExtensionAmount());
      row.put("taxCategory", l.getTaxCategory().name());
      row.put("taxScheme", l.getTaxScheme() == null ? "" : l.getTaxScheme().name());
      row.put("taxRate", l.getTaxRate());
      row.put("taxAmount", l.getTaxAmount());
      row.put("total", l.getTotalAmount());
      lines.add(row);
      if (l.getTaxScheme() != null) {
        String key = l.getTaxScheme().name() + " " + l.getTaxRate() + "%";
        BigDecimal[] acc =
            byRate.computeIfAbsent(key, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        acc[0] = acc[0].add(l.getLineExtensionAmount());
        acc[1] = acc[1].add(l.getTaxAmount());
      }
    }
    model.put("lines", lines);

    List<Map<String, Object>> taxTotals = new ArrayList<>();
    byRate.forEach(
        (key, acc) -> {
          Map<String, Object> t = new LinkedHashMap<>();
          t.put("label", key);
          t.put("taxable", acc[0]);
          t.put("tax", acc[1]);
          taxTotals.add(t);
        });
    model.put("taxTotals", taxTotals);

    model.put("lineExtension", doc.getLineExtensionAmount());
    model.put("taxExclusive", doc.getTaxExclusiveAmount());
    model.put("taxInclusive", doc.getTaxInclusiveAmount());
    model.put("payable", doc.getPayableAmount());

    model.put("qr", "data:image/png;base64," + qrPngBase64);

    return renderer.render("electronic-invoice", model);
  }
}
