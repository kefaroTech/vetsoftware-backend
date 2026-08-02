package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests del agregado {@link SupplierInvoice}: matemática de dinero (total/neto/saldo con
 * retención), transición de estado por abonos (PENDING → PARTIAL → PAID), rechazo de sobrepago /
 * pago sobre anulada, y anulación solo sin abonos.
 */
class SupplierInvoiceTest {

  private static final CompanyRef CO = new CompanyRef(1L, "Vet SAS", "900123456-7");
  private static final BranchRef BR = new BranchRef(10L, "Principal");
  private static final SupplierRef SUP = new SupplierRef(5L, "Distribuidora", "800111222-3");

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private SupplierInvoice invoice(String subtotal, String tax, String withholding) {
    return SupplierInvoice.create(
        CO,
        BR,
        SUP,
        null,
        null,
        "FV-1",
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 31),
        bd(subtotal),
        bd(tax),
        bd(withholding),
        null,
        7L);
  }

  private SupplierInvoicePayment payment(String amount) {
    return SupplierInvoicePayment.create(
        bd(amount), LocalDate.of(2026, 7, 10), SupplierInvoicePaymentMethod.CASH, null, null, 7L);
  }

  @Test
  void nace_pending_con_totales_derivados_y_saldo_neto() {
    SupplierInvoice inv = invoice("1000", "190", "10");
    assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PENDING);
    assertThat(inv.getTotal()).isEqualByComparingTo("1190"); // base + impuesto
    assertThat(inv.payableAmount()).isEqualByComparingTo("1180"); // total - retención
    assertThat(inv.paidAmount()).isEqualByComparingTo("0");
    assertThat(inv.balance()).isEqualByComparingTo("1180");
  }

  @Test
  void abono_parcial_pasa_a_partial_y_reduce_saldo() {
    SupplierInvoice inv = invoice("1000", "190", "10");
    inv.registerPayment(payment("180"), 7L, null);
    assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PARTIAL);
    assertThat(inv.paidAmount()).isEqualByComparingTo("180");
    assertThat(inv.balance()).isEqualByComparingTo("1000");
  }

  @Test
  void abono_total_pasa_a_paid_y_saldo_cero() {
    SupplierInvoice inv = invoice("1000", "190", "10");
    inv.registerPayment(payment("1180"), 7L, null);
    assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
    assertThat(inv.balance()).isEqualByComparingTo("0");
  }

  @Test
  void sobrepago_es_rechazado() {
    SupplierInvoice inv = invoice("1000", "190", "10");
    assertThatThrownBy(() -> inv.registerPayment(payment("1181"), 7L, null))
        .isInstanceOf(InvalidSupplierInvoiceStateException.class);
  }

  @Test
  void no_se_puede_abonar_una_anulada() {
    SupplierInvoice inv = invoice("1000", "0", "0");
    inv.cancel(7L, null);
    assertThatThrownBy(() -> inv.registerPayment(payment("10"), 7L, null))
        .isInstanceOf(InvalidSupplierInvoiceStateException.class);
  }

  @Test
  void anular_con_abonos_es_rechazado() {
    SupplierInvoice inv = invoice("1000", "0", "0");
    inv.registerPayment(payment("100"), 7L, null);
    assertThatThrownBy(() -> inv.cancel(7L, null))
        .isInstanceOf(InvalidSupplierInvoiceStateException.class);
  }

  @Test
  void anular_pending_deja_saldo_cero() {
    SupplierInvoice inv = invoice("1000", "0", "0");
    inv.cancel(7L, null);
    assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.CANCELLED);
    assertThat(inv.balance()).isEqualByComparingTo("0");
  }

  @Test
  void vencimiento_anterior_a_emision_es_invalido() {
    assertThatThrownBy(
            () ->
                SupplierInvoice.create(
                    CO,
                    BR,
                    SUP,
                    null,
                    null,
                    "FV-2",
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 7, 1),
                    bd("100"),
                    bd("0"),
                    bd("0"),
                    null,
                    7L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void retencion_mayor_al_total_es_invalida() {
    assertThatThrownBy(() -> invoice("100", "19", "200"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
