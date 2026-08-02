package com.vetsoftware.app.salesreport.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lee los documentos electrónicos de una empresa para los reportes contables (F6). Vista de solo
 * lectura en tipos propios de esta feature (String/BigDecimal): no acopla salesreport al dominio de
 * electronicdocument.
 */
public interface SalesDocumentQueryPort {

  /**
   * Documentos de la empresa en el rango, con filtro OPCIONAL por sede (branchId null = todas las
   * sedes).
   */
  List<SalesDocumentView> findByCompanyAndDateRange(
      Long companyId, LocalDate from, LocalDate to, Long branchId);

  record SalesDocumentView(
      Long id,
      String documentType,
      String prefix,
      Long consecutive,
      LocalDate issueDate,
      String customerDocumentId,
      String customerName,
      String dianStatus,
      String cufe,
      String cude,
      BigDecimal lineExtensionAmount,
      BigDecimal taxInclusiveAmount,
      BigDecimal payableAmount,
      BigDecimal reteFuente,
      BigDecimal reteIva,
      BigDecimal reteIca,
      List<TaxLineView> taxLines,
      List<PaymentLineView> paymentLines) {}

  /** Una línea tributada del documento (esquema + tarifa + base + impuesto). */
  record TaxLineView(
      String taxScheme, BigDecimal taxRate, BigDecimal taxableAmount, BigDecimal taxAmount) {}

  /** Un pago del documento (medio DIAN + monto). */
  record PaymentLineView(String paymentMeans, String dianCode, BigDecimal amount) {}
}
