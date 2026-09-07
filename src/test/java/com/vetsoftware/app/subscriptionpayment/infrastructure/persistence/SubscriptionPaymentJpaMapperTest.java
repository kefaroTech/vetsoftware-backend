package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionPaymentJpaMapper — ida y vuelta de los cinco campos de conciliacion")
class SubscriptionPaymentJpaMapperTest {

    private final SubscriptionPaymentJpaMapper mapper = new SubscriptionPaymentJpaMapper();

    @Nested
    @DisplayName("Un pago conciliado")
    class PagoConciliado {

        @Test
        @DisplayName("toJpa conserva comision, neto, lote, fecha de liquidacion y lo devuelto")
        void toJpa_conserva_los_cinco_campos() {
            SubscriptionPaymentJpaEntity entity = mapper.toJpa(pagoConciliado());

            assertThat(entity.getFeeAmount()).isEqualByComparingTo("4500.00");
            assertThat(entity.getNetAmount()).isEqualByComparingTo("495500.00");
            assertThat(entity.getSettlementReference()).isEqualTo("LOTE-2026-0042");
            assertThat(entity.getSettledOn()).isEqualTo(LocalDate.of(2026, 8, 25));
            assertThat(entity.getRefundedAmount()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("toDomain reconstruye los mismos cinco campos desde la entidad")
        void toDomain_reconstruye_los_cinco_campos() {
            SubscriptionPayment payment = mapper.toDomain(entidadConciliada());

            assertThat(payment.getFeeAmount()).isEqualByComparingTo("4500.00");
            assertThat(payment.getNetAmount()).isEqualByComparingTo("495500.00");
            assertThat(payment.getSettlementReference()).isEqualTo("LOTE-2026-0042");
            assertThat(payment.getSettledOn()).isEqualTo(LocalDate.of(2026, 8, 25));
            assertThat(payment.getRefundedAmount()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("un viaje completo ida y vuelta conserva los cinco campos tal cual")
        void ida_y_vuelta_conserva_los_cinco_campos() {
            SubscriptionPayment original = pagoConciliado();

            SubscriptionPayment rehidratado = mapper.toDomain(mapper.toJpa(original));

            assertThat(rehidratado.getFeeAmount()).isEqualByComparingTo(original.getFeeAmount());
            assertThat(rehidratado.getNetAmount()).isEqualByComparingTo(original.getNetAmount());
            assertThat(rehidratado.getSettlementReference())
                    .isEqualTo(original.getSettlementReference());
            assertThat(rehidratado.getSettledOn()).isEqualTo(original.getSettledOn());
            assertThat(rehidratado.getRefundedAmount())
                    .isEqualByComparingTo(original.getRefundedAmount());
        }
    }

    @Test
    @DisplayName("un pago sin conciliar viaja con los cinco campos en null o cero, no inventados")
    void un_pago_sin_conciliar_no_inventa_conciliacion() {
        SubscriptionPayment payment = SubscriptionPayment.register(42L, new BigDecimal("500000.00"),
                "COP", PaymentMethod.TRANSFER, null, null, LocalDateTime.of(2026, 8, 22, 10, 30),
                "req-sin-conciliar", LocalDateTime.of(2026, 8, 22, 10, 30));

        SubscriptionPaymentJpaEntity entity = mapper.toJpa(payment);

        assertThat(entity.getFeeAmount()).isNull();
        assertThat(entity.getNetAmount()).isNull();
        assertThat(entity.getSettlementReference()).isNull();
        assertThat(entity.getSettledOn()).isNull();
        assertThat(entity.getRefundedAmount()).isEqualByComparingTo("0.00");
    }

    private static SubscriptionPayment pagoConciliado() {
        return new SubscriptionPayment(7L, 42L, new BigDecimal("500000.00"), "COP",
                PaymentMethod.PSE, "WOMPI", "TX-2026-0001", LocalDateTime.of(2026, 8, 22, 10, 30),
                SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 23, 9, 0),
                new BigDecimal("4500.00"), new BigDecimal("495500.00"), "LOTE-2026-0042",
                LocalDate.of(2026, 8, 25), new BigDecimal("50000.00"), "req-conciliado",
                LocalDateTime.of(2026, 8, 22, 10, 30), 3L);
    }

    private static SubscriptionPaymentJpaEntity entidadConciliada() {
        SubscriptionPaymentJpaEntity entity = new SubscriptionPaymentJpaEntity();
        entity.setId(7L);
        entity.setCompanyId(42L);
        entity.setAmount(new BigDecimal("500000.00"));
        entity.setCurrency("COP");
        entity.setPaymentMethod(PaymentMethod.PSE);
        entity.setGateway("WOMPI");
        entity.setGatewayReference("TX-2026-0001");
        entity.setReceivedAt(LocalDateTime.of(2026, 8, 22, 10, 30));
        entity.setStatus(SubscriptionPaymentStatus.CONFIRMED);
        entity.setReconciledAt(LocalDateTime.of(2026, 8, 23, 9, 0));
        entity.setFeeAmount(new BigDecimal("4500.00"));
        entity.setNetAmount(new BigDecimal("495500.00"));
        entity.setSettlementReference("LOTE-2026-0042");
        entity.setSettledOn(LocalDate.of(2026, 8, 25));
        entity.setRefundedAmount(new BigDecimal("50000.00"));
        entity.setClientRequestId("req-conciliado");
        entity.setCreatedDate(LocalDateTime.of(2026, 8, 22, 10, 30));
        entity.setVersion(3L);
        return entity;
    }
}
