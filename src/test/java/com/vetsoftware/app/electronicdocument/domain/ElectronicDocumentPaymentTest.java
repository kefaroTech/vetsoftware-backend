package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pago congelado del documento: medio DIAN + monto. Es lo que cuadra la caja
 * contra el total, asi que ninguno de los dos campos admite null.
 */
@DisplayName("ElectronicDocumentPayment — medio de pago congelado")
class ElectronicDocumentPaymentTest {

    @Test
    @DisplayName("rechaza un pago sin medio de pago")
    void rechaza_un_pago_sin_medio_de_pago() {
        assertThatThrownBy(
                () -> new ElectronicDocumentPayment(null, null, new BigDecimal("1190.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMeans is required");
    }

    @Test
    @DisplayName("rechaza un pago sin monto")
    void rechaza_un_pago_sin_monto() {
        assertThatThrownBy(() -> new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payment amount is required");
    }

    @ParameterizedTest
    @EnumSource(PaymentMeans.class)
    @DisplayName("acepta cualquier medio del catalogo DIAN vigente")
    void acepta_cualquier_medio_del_catalogo(PaymentMeans means) {
        ElectronicDocumentPayment pago = new ElectronicDocumentPayment(null, means,
                new BigDecimal("1190.00"));

        assertThat(pago.getPaymentMeans()).isEqualTo(means);
        assertThat(pago.getPaymentMeans().dianCode()).isNotBlank();
    }

    @Test
    @DisplayName("acepta un monto cero: un pago compensado sigue siendo un pago")
    void acepta_un_monto_cero() {
        assertThatCode(
                () -> new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, BigDecimal.ZERO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("expone id, medio y monto tal como se construyeron")
    void expone_id_medio_y_monto() {
        ElectronicDocumentPayment pago = new ElectronicDocumentPayment(12L,
                PaymentMeans.TARJETA_CREDITO, new BigDecimal("1190.00"));

        assertThat(pago.getId()).isEqualTo(12L);
        assertThat(pago.getPaymentMeans()).isEqualTo(PaymentMeans.TARJETA_CREDITO);
        assertThat(pago.getAmount()).isEqualByComparingTo("1190.00");
    }

    @Test
    @DisplayName("un pago nuevo no tiene id hasta persistirse")
    void un_pago_nuevo_no_tiene_id() {
        assertThat(
                new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, BigDecimal.TEN).getId())
                .isNull();
    }
}
