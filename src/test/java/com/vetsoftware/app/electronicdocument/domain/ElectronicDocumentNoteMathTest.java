package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Aritmética de las notas crédito/débito: la única corrección fiscal válida sobre una factura
 * validada. Una nota mal escalada devuelve dinero de más o reversa cartera equivocada, así que se
 * fijan el clon total, el prorrateo parcial (líneas, IVA, pagos y retenciones) y los topes que
 * impiden abusarla.
 */
class ElectronicDocumentNoteMathTest {

  private static final BigDecimal UVT = new BigDecimal("49799");

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private static IssuerSnapshot issuer() {
    return new IssuerSnapshot(
        "NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co", List.of("O-13"));
  }

  private static ElectronicDocumentLine ivaLine(int number, String base, String tax) {
    return new ElectronicDocumentLine(
        null,
        number,
        "Gravado " + number,
        BigDecimal.ONE,
        "94",
        bd(base),
        bd(base),
        TaxCategory.GRAVADO,
        TaxScheme.IVA,
        bd("19"),
        bd(tax),
        bd(base).add(bd(tax)));
  }

  /** Factura VALIDADA de 1.190.000 (base 1.000.000 + IVA 190.000) con retenciones practicadas. */
  private static ElectronicDocument validatedInvoice() {
    ElectronicDocument doc =
        ElectronicDocument.createPending(
            9L,
            100L,
            ElectronicDocumentType.FE_VENTA,
            issuer(),
            CustomerSnapshot.finalConsumer(),
            List.of(ivaLine(1, "1000000", "190000")),
            List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, bd("1190000"))),
            PaymentForm.CONTADO,
            true,
            bd("4"),
            bd("15"),
            bd("9.66"),
            UVT,
            "req-1",
            4L,
            7L);
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

  @Nested
  class NotaTotal {

    @Test
    void clona_exactamente_los_totales_del_original() {
      ElectronicDocument original = validatedInvoice();

      ElectronicDocument note =
          ElectronicDocument.createCreditNote(original, "2", "Anulación", 4L, null);

      assertThat(note.getLineExtensionAmount())
          .isEqualByComparingTo(original.getLineExtensionAmount());
      assertThat(note.getTaxInclusiveAmount())
          .isEqualByComparingTo(original.getTaxInclusiveAmount());
      assertThat(note.getPayableAmount()).isEqualByComparingTo(original.getPayableAmount());
    }

    @Test
    void clona_las_retenciones_para_reversarlas_completas() {
      ElectronicDocument original = validatedInvoice();

      ElectronicDocument note =
          ElectronicDocument.createCreditNote(original, "2", "Anulación", 4L, null);

      assertThat(note.getReteFuenteAmount()).isEqualByComparingTo(original.getReteFuenteAmount());
      assertThat(note.getReteIvaAmount()).isEqualByComparingTo(original.getReteIvaAmount());
      assertThat(note.getReteIcaAmount()).isEqualByComparingTo(original.getReteIcaAmount());
    }

    @Test
    void referencia_el_cufe_prefijo_y_consecutivo_del_original() {
      ElectronicDocument original = validatedInvoice();

      ElectronicDocument note =
          ElectronicDocument.createCreditNote(original, "2", "Anulación", 4L, null);

      assertThat(note.getReference().cufe()).isEqualTo("CUFE-ABC123");
      assertThat(note.getReference().prefix()).isEqualTo("SETP");
      assertThat(note.getReference().number()).isEqualTo(990L);
    }

    @Test
    void la_nota_nace_pendiente_y_sin_numeracion_propia() {
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(validatedInvoice(), "2", "Anulación", 4L, null);

      assertThat(note.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
      assertThat(note.getConsecutive()).isNull();
      assertThat(note.getCufe()).isNull();
      assertThat(note.isNote()).isTrue();
    }
  }

  @Nested
  class NotaParcial {

    @Test
    void escala_totales_por_el_ratio_monto_sobre_total() {
      ElectronicDocument original = validatedInvoice();

      // 25 % de 1.190.000
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(
              original, "2", "Devolución parcial", 4L, bd("297500"));

      assertThat(note.getPayableAmount()).isEqualByComparingTo("297500.00");
      assertThat(note.getLineExtensionAmount()).isEqualByComparingTo("250000.00");
    }

    @Test
    void escala_tambien_las_lineas_y_su_iva() {
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(
              validatedInvoice(), "2", "Devolución parcial", 4L, bd("297500"));

      ElectronicDocumentLine line = note.getLines().getFirst();
      assertThat(line.getLineExtensionAmount()).isEqualByComparingTo("250000.00");
      assertThat(line.getTaxAmount()).isEqualByComparingTo("47500.00");
      assertThat(line.getTaxRate())
          .as("la tarifa no se escala, solo los montos")
          .isEqualByComparingTo("19");
    }

    @Test
    void escala_las_retenciones_proporcionalmente() {
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(
              validatedInvoice(), "2", "Devolución parcial", 4L, bd("297500"));

      assertThat(note.getReteFuenteAmount()).isEqualByComparingTo("10000.00");
      assertThat(note.getReteIvaAmount()).isEqualByComparingTo("7125.00");
      assertThat(note.getReteIcaAmount()).isEqualByComparingTo("2415.00");
    }

    @Test
    void escala_los_pagos_del_original() {
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(
              validatedInvoice(), "2", "Devolución parcial", 4L, bd("297500"));

      assertThat(note.getPayments()).hasSize(1);
      assertThat(note.getPayments().getFirst().getAmount()).isEqualByComparingTo("297500.00");
    }

    @Test
    void una_nota_credito_no_puede_exceder_el_total_de_la_factura() {
      assertThatThrownBy(
              () ->
                  ElectronicDocument.createCreditNote(
                      validatedInvoice(), "2", "Exceso", 4L, bd("1190001")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("no puede exceder el total");
    }

    @Test
    void una_nota_credito_por_el_total_exacto_si_es_valida() {
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(
              validatedInvoice(), "2", "Anulación exacta", 4L, bd("1190000"));

      assertThat(note.getPayableAmount()).isEqualByComparingTo("1190000.00");
    }

    @Test
    void el_monto_de_la_nota_debe_ser_mayor_que_cero() {
      assertThatThrownBy(
              () ->
                  ElectronicDocument.createCreditNote(
                      validatedInvoice(), "2", "Cero", 4L, BigDecimal.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("mayor que cero");

      assertThatThrownBy(
              () ->
                  ElectronicDocument.createCreditNote(
                      validatedInvoice(), "2", "Negativo", 4L, bd("-1")))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class NotaDebito {

    @Test
    void admite_un_incremento_superior_al_total_de_la_factura() {
      // La ND representa un aumento: a diferencia de la NC no tiene tope en el total original.
      ElectronicDocument note =
          ElectronicDocument.createDebitNote(
              validatedInvoice(), "1", "Ajuste al alza", 4L, bd("2380000"));

      assertThat(note.getDocumentType()).isEqualTo(ElectronicDocumentType.NOTA_DEBITO);
      assertThat(note.getPayableAmount()).isEqualByComparingTo("2380000.00");
    }

    @Test
    void sin_monto_hereda_el_total_del_original() {
      ElectronicDocument original = validatedInvoice();

      ElectronicDocument note =
          ElectronicDocument.createDebitNote(original, "1", "Ajuste heredado", 4L, null);

      assertThat(note.getPayableAmount()).isEqualByComparingTo(original.getPayableAmount());
    }
  }

  @Nested
  class Precondiciones {

    @Test
    void no_se_puede_emitir_una_nota_sobre_un_documento_sin_cufe() {
      ElectronicDocument sinValidar =
          ElectronicDocument.createPending(
              9L,
              100L,
              ElectronicDocumentType.FE_VENTA,
              issuer(),
              CustomerSnapshot.finalConsumer(),
              List.of(ivaLine(1, "1000", "190")),
              List.of(),
              PaymentForm.CONTADO,
              false,
              null,
              null,
              null,
              UVT,
              null,
              4L,
              7L);

      assertThatThrownBy(
              () -> ElectronicDocument.createCreditNote(sinValidar, "2", "Anulación", 4L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("no CUFE");
    }

    @Test
    void no_se_puede_emitir_una_nota_sin_documento_original() {
      assertThatThrownBy(
              () -> ElectronicDocument.createCreditNote(null, "2", "Anulación", 4L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("original document is required");
    }

    @Test
    void una_nota_exige_concepto_dian() {
      assertThatThrownBy(
              () ->
                  ElectronicDocument.createCreditNote(
                      validatedInvoice(), " ", "Sin código", 4L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("reason code");
    }

    @Test
    void la_nota_no_arrastra_el_clientRequestId_del_original() {
      // Idempotencia del POS: reutilizar la clave del original haría colisionar la nota con la
      // venta.
      ElectronicDocument note =
          ElectronicDocument.createCreditNote(validatedInvoice(), "2", "Anulación", 4L, null);

      assertThat(note.getClientRequestId()).isNull();
    }
  }

  @Nested
  class MaquinaDeEstados {

    @Test
    void un_documento_validado_no_puede_volver_a_transicionar() {
      ElectronicDocument doc = validatedInvoice();

      assertThatThrownBy(doc::markRejected)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("estado terminal");
      assertThatThrownBy(doc::markContingency).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void un_documento_validado_no_puede_renumerarse() {
      ElectronicDocument doc = validatedInvoice();

      assertThatThrownBy(() -> doc.assignNumber("RES-1", "SETP", 1L))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void solo_se_libera_la_numeracion_de_un_documento_rechazado() {
      ElectronicDocument doc =
          ElectronicDocument.createPending(
              9L,
              100L,
              ElectronicDocumentType.FE_VENTA,
              issuer(),
              CustomerSnapshot.finalConsumer(),
              List.of(ivaLine(1, "1000", "190")),
              List.of(),
              PaymentForm.CONTADO,
              false,
              null,
              null,
              null,
              UVT,
              null,
              4L,
              7L);
      doc.assignNumber("RES-1", "SETP", 5L);

      assertThatThrownBy(doc::releaseFiscalNumber).isInstanceOf(IllegalStateException.class);

      doc.markRejected();
      doc.releaseFiscalNumber();

      assertThat(doc.getConsecutive()).isNull();
      assertThat(doc.getPrefix()).isNull();
      assertThat(doc.getResolutionNumber()).isNull();
    }

    @Test
    void el_consecutivo_no_se_reasigna() {
      ElectronicDocument doc =
          ElectronicDocument.createPending(
              9L,
              100L,
              ElectronicDocumentType.FE_VENTA,
              issuer(),
              CustomerSnapshot.finalConsumer(),
              List.of(ivaLine(1, "1000", "190")),
              List.of(),
              PaymentForm.CONTADO,
              false,
              null,
              null,
              null,
              UVT,
              null,
              4L,
              7L);
      doc.assignNumber("RES-1", "SETP", 5L);

      assertThatThrownBy(() -> doc.assignNumber("RES-1", "SETP", 6L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("ya tiene consecutivo");
    }

    @Test
    void el_tiquete_pos_usa_consecutivo_del_proveedor() {
      ElectronicDocument pos =
          ElectronicDocument.createPending(
              9L,
              null,
              ElectronicDocumentType.DOC_EQUIV_POS,
              issuer(),
              CustomerSnapshot.finalConsumer(),
              List.of(ivaLine(1, "1000", "190")),
              List.of(),
              PaymentForm.CONTADO,
              false,
              null,
              null,
              null,
              UVT,
              null,
              4L,
              7L);

      assertThat(pos.usesProviderAssignedConsecutive()).isTrue();

      pos.assignResolutionOnly("RES-POS", "POS");
      assertThat(pos.getResolutionNumber()).isEqualTo("RES-POS");
      assertThat(pos.getConsecutive()).as("el consecutivo lo sella el proveedor").isNull();
    }

    @Test
    void un_documento_en_contingencia_sin_sellos_es_provisional() {
      ElectronicDocument doc =
          ElectronicDocument.createPending(
              9L,
              100L,
              ElectronicDocumentType.FE_VENTA,
              issuer(),
              CustomerSnapshot.finalConsumer(),
              List.of(ivaLine(1, "1000", "190")),
              List.of(),
              PaymentForm.CONTADO,
              false,
              null,
              null,
              null,
              UVT,
              null,
              4L,
              7L);

      doc.markContingency();

      assertThat(doc.isProvisional()).isTrue();
    }

    @Test
    void marcar_reversada_es_idempotente() {
      ElectronicDocument doc = validatedInvoice();

      doc.markReversed();
      doc.markReversed();

      assertThat(doc.isReversed()).isTrue();
    }

    @Test
    void solo_un_pendiente_puede_marcarse_no_electronico() {
      ElectronicDocument doc = validatedInvoice();

      assertThatThrownBy(doc::markLocal).isInstanceOf(IllegalStateException.class);
    }
  }
}
