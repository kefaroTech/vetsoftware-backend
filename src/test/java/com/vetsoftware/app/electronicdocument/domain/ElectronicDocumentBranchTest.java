package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Threading de la sede (branchId) en el agregado fiscal INMUTABLE {@link ElectronicDocument}. La
 * sede es un metadato fiscal obligatorio (qué sede emitió). Verifica: (1) createPending congela el
 * branchId; (2) el branchId es una invariante dura (no se puede construir sin él); (3) las notas
 * crédito/débito HEREDAN la sede del documento original — nunca inventan otra.
 */
class ElectronicDocumentBranchTest {

  private static final BigDecimal UVT = new BigDecimal("47065");

  private static ElectronicDocumentLine gravadoLine() {
    return new ElectronicDocumentLine(
        null,
        1,
        "Consulta general",
        BigDecimal.ONE,
        "94",
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        TaxCategory.GRAVADO,
        TaxScheme.IVA,
        new BigDecimal("19"),
        new BigDecimal("190"),
        new BigDecimal("1190"));
  }

  private static IssuerSnapshot issuer() {
    return new IssuerSnapshot(
        "NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co", List.of("O-13"));
  }

  private static List<ElectronicDocumentPayment> cashPaymentMatchingTotal() {
    return List.of(
        new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, new BigDecimal("1190")));
  }

  private static ElectronicDocument pending(Long branchId) {
    return ElectronicDocument.createPending(
        9L,
        100L,
        ElectronicDocumentType.FE_VENTA,
        issuer(),
        CustomerSnapshot.finalConsumer(),
        List.of(gravadoLine()),
        cashPaymentMatchingTotal(),
        PaymentForm.CONTADO,
        false,
        null,
        null,
        null,
        UVT,
        "req-1",
        4L,
        branchId);
  }

  /** Original VALIDADO (con CUFE) para poder emitir notas que lo referencien. */
  private static ElectronicDocument validatedOriginal(Long branchId) {
    ElectronicDocument doc = pending(branchId);
    doc.markValidated(
        "SETP",
        990L,
        "CUFE-ABC123",
        "cude",
        "uuid",
        "<xml/>",
        "qr",
        "https://qr",
        "pdf",
        LocalDateTime.of(2026, 8, 1, 10, 0));
    return doc;
  }

  @Test
  void createPending_congela_el_branchId_en_el_documento() {
    ElectronicDocument doc = pending(77L);

    assertThat(doc.getBranchId()).isEqualTo(77L);
    assertThat(doc.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
    // El documento es válido (pagos cuadran con el total), no un stub que "siempre pasa".
    assertThat(doc.getPayableAmount()).isEqualByComparingTo("1190");
  }

  @Test
  void createPending_exige_branchId() {
    assertThatThrownBy(() -> pending(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("branchId is required");
  }

  @Test
  void nota_credito_hereda_la_sede_del_original() {
    ElectronicDocument original = validatedOriginal(77L);

    ElectronicDocument note =
        ElectronicDocument.createCreditNote(original, "2", "Anulación de la venta", 4L, null);

    assertThat(note.getDocumentType()).isEqualTo(ElectronicDocumentType.NOTA_CREDITO);
    assertThat(note.getBranchId())
        .as("la nota debe emitirse desde la misma sede que la factura")
        .isEqualTo(77L);
  }

  @Test
  void nota_debito_hereda_la_sede_del_original() {
    ElectronicDocument original = validatedOriginal(88L);

    ElectronicDocument note =
        ElectronicDocument.createDebitNote(original, "1", "Ajuste al alza", 4L, null);

    assertThat(note.getDocumentType()).isEqualTo(ElectronicDocumentType.NOTA_DEBITO);
    assertThat(note.getBranchId()).isEqualTo(88L);
  }

  @Test
  void nota_credito_parcial_conserva_la_sede_del_original() {
    ElectronicDocument original = validatedOriginal(77L);

    ElectronicDocument note =
        ElectronicDocument.createCreditNote(
            original, "2", "Devolución parcial", 4L, new BigDecimal("595"));

    assertThat(note.getBranchId()).isEqualTo(77L);
    // La NC parcial escala montos pero NO la sede.
    assertThat(note.getPayableAmount()).isEqualByComparingTo("595");
  }
}
