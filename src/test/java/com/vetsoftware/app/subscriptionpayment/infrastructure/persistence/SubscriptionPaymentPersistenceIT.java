package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionPaymentRepository — pagos recibidos contra MySQL real")
class SubscriptionPaymentPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionPaymentRepository repository;
    @Autowired
    private SubscriptionPaymentJpaRepository jpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda un pago pendiente y lo deduplica por llave del cliente")
    void guarda_pago_y_lo_encuentra_por_llave() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 23, 10, 0);
        SubscriptionPayment saved = repository.save(
                SubscriptionPayment.register(SchemaSeed.COMPANY_ID, new BigDecimal("250000.00"),
                        "COP", PaymentMethod.TRANSFER, null, null, at, "payment-request-1", at));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCompanyIdAndClientRequestId(SchemaSeed.COMPANY_ID,
                "payment-request-1")).get().satisfies(payment -> {
                    assertThat(payment.getId()).isEqualTo(saved.getId());
                    assertThat(payment.getStatus()).isEqualTo(SubscriptionPaymentStatus.PENDING);
                    assertThat(payment.getAmount()).isEqualByComparingTo("250000.00");
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }

    @Test
    @DisplayName("cuenta solo los PENDING de pasarela mas viejos que el umbral (#765)")
    void cuenta_pendientes_de_pasarela_envejecidos() {
        LocalDateTime hace2Horas = LocalDateTime.of(2026, 8, 23, 8, 0);
        LocalDateTime hace10Minutos = LocalDateTime.of(2026, 8, 23, 9, 50);
        LocalDateTime umbral = LocalDateTime.of(2026, 8, 23, 9, 0);

        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-viejo",
                hace2Horas, "req-viejo", hace2Horas));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-reciente",
                hace10Minutos, "req-reciente", hace10Minutos));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.TRANSFER, null, null, hace2Horas,
                "req-manual", hace2Horas));
        entityManager.flush();

        assertThat(jpaRepository.countByStatusAndGatewayIsNotNullAndReceivedAtBefore(
                SubscriptionPaymentStatus.PENDING, umbral)).isEqualTo(1);
    }

    @Test
    @DisplayName("findStalePendingGatewayPayments devuelve los PENDING de pasarela mas viejos primero (#777)")
    void encuentra_pendientes_de_pasarela_envejecidos_en_orden() {
        LocalDateTime hace2Horas = LocalDateTime.of(2026, 8, 23, 8, 0);
        LocalDateTime hace90Minutos = LocalDateTime.of(2026, 8, 23, 8, 30);
        LocalDateTime hace10Minutos = LocalDateTime.of(2026, 8, 23, 9, 50);
        LocalDateTime umbral = LocalDateTime.of(2026, 8, 23, 9, 0);

        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-mas-viejo",
                hace2Horas, "req-mas-viejo", hace2Horas));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-viejo",
                hace90Minutos, "req-viejo", hace90Minutos));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-reciente",
                hace10Minutos, "req-reciente", hace10Minutos));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.TRANSFER, null, null, hace2Horas,
                "req-manual", hace2Horas));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findStalePendingGatewayPayments(umbral, 50))
                .extracting(SubscriptionPayment::getClientRequestId)
                .containsExactly("req-mas-viejo", "req-viejo");
    }

    @Test
    @DisplayName("persiste la reserva sin referencia de pasarela y la asigna despues (#776)")
    void persiste_la_reserva_sin_referencia_y_la_asigna_despues() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 23, 10, 0);
        SubscriptionPayment reserva = repository.save(
                SubscriptionPayment.register(SchemaSeed.COMPANY_ID, new BigDecimal("100000.00"),
                        "COP", PaymentMethod.CARD, "WOMPI", null, at, "req-reserva-1", at));
        entityManager.flush();
        entityManager.clear();

        SubscriptionPayment recargada = repository
                .lockByIdAndCompanyId(reserva.getId(), SchemaSeed.COMPANY_ID).orElseThrow();
        assertThat(recargada.getGatewayReference()).isNull();

        recargada.assignGatewayReference("TX-2026-0001", null);
        repository.save(recargada);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdAndCompanyId(reserva.getId(), SchemaSeed.COMPANY_ID)).get()
                .extracting(SubscriptionPayment::getGatewayReference).isEqualTo("TX-2026-0001");
    }

    @Test
    @DisplayName("findAllFiltered con pendingThreshold ordena ascendente y acota a PENDING de pasarela")
    void filtra_pendientes_envejecidos_en_orden_ascendente() {
        LocalDateTime hace2Horas = LocalDateTime.of(2026, 8, 23, 8, 0);
        LocalDateTime hace90Minutos = LocalDateTime.of(2026, 8, 23, 8, 30);
        LocalDateTime umbral = LocalDateTime.of(2026, 8, 23, 9, 0);

        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-mas-viejo",
                hace2Horas, "req-mas-viejo", hace2Horas));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.CARD, "WOMPI", "ref-viejo",
                hace90Minutos, "req-viejo", hace90Minutos));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.TRANSFER, null, null, hace2Horas,
                "req-manual", hace2Horas));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllFiltered(null, null, null, null, umbral, 0, 20).content())
                .extracting(SubscriptionPayment::getClientRequestId)
                .containsExactly("req-mas-viejo", "req-viejo");
    }

    @Test
    @DisplayName("findAllFiltered sin pendingThreshold ordena descendente por fecha de recepcion")
    void filtra_por_rango_de_fechas_en_orden_descendente() {
        LocalDateTime dentro1 = LocalDateTime.of(2026, 8, 1, 8, 0);
        LocalDateTime dentro2 = LocalDateTime.of(2026, 8, 2, 8, 0);
        LocalDateTime fuera = LocalDateTime.of(2026, 7, 1, 8, 0);

        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.TRANSFER, null, null, dentro1,
                "req-dentro-1", dentro1));
        repository.save(SubscriptionPayment.register(SchemaSeed.COMPANY_ID,
                new BigDecimal("100000.00"), "COP", PaymentMethod.TRANSFER, null, null, dentro2,
                "req-dentro-2", dentro2));
        repository.save(
                SubscriptionPayment.register(SchemaSeed.COMPANY_ID, new BigDecimal("100000.00"),
                        "COP", PaymentMethod.TRANSFER, null, null, fuera, "req-fuera", fuera));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository
                .findAllFiltered(SchemaSeed.COMPANY_ID, null, LocalDateTime.of(2026, 7, 15, 0, 0),
                        LocalDateTime.of(2026, 8, 31, 23, 59), null, 0, 20)
                .content()).extracting(SubscriptionPayment::getClientRequestId)
                .containsExactly("req-dentro-2", "req-dentro-1");
    }
}
