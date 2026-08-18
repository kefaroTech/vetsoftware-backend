package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests del VO {@link SupplierInvoicePayment}: abono append-only con sus
 * invariantes de monto positivo, fecha, medio de pago y longitudes de texto.
 */
class SupplierInvoicePaymentTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static SupplierInvoicePayment construir(BigDecimal amount, LocalDate paymentDate,
            SupplierInvoicePaymentMethod method, String reference, String note) {
        return new SupplierInvoicePayment(null, amount, paymentDate, method, reference, note,
                CREADA, 7L);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("create() fija el id en null y conserva cada campo")
        void create_fija_el_id_en_null_y_conserva_cada_campo() {
            SupplierInvoicePayment abono = SupplierInvoicePayment.create(bd("165000"),
                    LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER, "TRF-9",
                    "nota", 7L);

            assertThat(abono.getId()).isNull();
            assertThat(abono.getAmount()).isEqualByComparingTo("165000");
            assertThat(abono.getPaymentDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(abono.getMethod()).isEqualTo(SupplierInvoicePaymentMethod.TRANSFER);
            assertThat(abono.getReference()).isEqualTo("TRF-9");
            assertThat(abono.getNote()).isEqualTo("nota");
            assertThat(abono.getCreatedBy()).isEqualTo(7L);
            assertThat(abono.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("el constructor conserva un id ya persistido")
        void el_constructor_conserva_un_id_ya_persistido() {
            SupplierInvoicePayment abono = new SupplierInvoicePayment(88L, bd("165000"),
                    LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.CASH, null, null, CREADA,
                    7L);

            assertThat(abono.getId()).isEqualTo(88L);
            assertThat(abono.getReference()).isNull();
            assertThat(abono.getNote()).isNull();
        }
    }

    static Stream<Arguments> invariantesInvalidas() {
        LocalDate fecha = LocalDate.of(2026, 2, 1);
        return Stream.of(
                Arguments.of("amount nulo",
                        (ThrowingCallable) () -> construir(null, fecha,
                                SupplierInvoicePaymentMethod.CASH, null, null),
                        "payment amount must be positive"),
                Arguments.of("amount cero",
                        (ThrowingCallable) () -> construir(bd("0"), fecha,
                                SupplierInvoicePaymentMethod.CASH, null, null),
                        "payment amount must be positive"),
                Arguments.of("amount negativo",
                        (ThrowingCallable) () -> construir(bd("-1"), fecha,
                                SupplierInvoicePaymentMethod.CASH, null, null),
                        "payment amount must be positive"),
                Arguments.of("paymentDate nula",
                        (ThrowingCallable) () -> construir(bd("100"), null,
                                SupplierInvoicePaymentMethod.CASH, null, null),
                        "paymentDate is required"),
                Arguments.of("method nulo",
                        (ThrowingCallable) () -> construir(bd("100"), fecha, null, null, null),
                        "payment method is required"),
                Arguments.of("reference excede 80 caracteres",
                        (ThrowingCallable) () -> construir(bd("100"), fecha,
                                SupplierInvoicePaymentMethod.CASH, "R".repeat(81), null),
                        "reference must be 80 chars or less"),
                Arguments.of("note excede 300 caracteres",
                        (ThrowingCallable) () -> construir(bd("100"), fecha,
                                SupplierInvoicePaymentMethod.CASH, null, "N".repeat(301)),
                        "note must be 300 chars or less"));
    }

    @Nested
    @DisplayName("validaciones del constructor")
    class Validaciones {

        @ParameterizedTest(name = "{0}")
        @DisplayName("cada invariante del abono se rechaza con su mensaje")
        @MethodSource("com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentTest#invariantesInvalidas")
        void invariante_de_constructor_es_rechazada(String descripcion, ThrowingCallable invocacion,
                String mensajeEsperado) {
            assertThatThrownBy(invocacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }
    }
}
