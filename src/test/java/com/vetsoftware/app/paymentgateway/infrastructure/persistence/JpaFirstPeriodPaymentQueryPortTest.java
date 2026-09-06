package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaEntity;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code SubscriptionPaymentJpaEntity} se mockea porque su constructor sin
 * argumentos es {@code protected}, igual que {@code JpaBaseRoleQueryPortTest}
 * con {@code BaseRoleJpaEntity}.
 *
 * <p>
 * <strong>La entidad siempre se construye en una variable propia, nunca como
 * argumento inline de {@code when(...).thenReturn(...)}.</strong>
 * {@code pago(...)} hace sus propios {@code when/thenReturn} sobre la entidad,
 * y evaluarla como argumento de un {@code thenReturn} todavía abierto dejaba a
 * Mockito con un stubbing sin terminar ({@code UnfinishedStubbingException}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaFirstPeriodPaymentQueryPort")
class JpaFirstPeriodPaymentQueryPortTest {

    private static final Long EMPRESA_A = 42L;
    private static final Long EMPRESA_B = 99L;
    private static final String REFERENCIA = "VS-SUS-2026-00184-P1";
    private static final LocalDateTime RECIBIDO_EN = LocalDateTime.of(2026, 1, 1, 8, 0);

    @Mock
    private SubscriptionPaymentJpaRepository subscriptionPaymentJpaRepository;
    @InjectMocks
    private JpaFirstPeriodPaymentQueryPort port;

    private static SubscriptionPaymentJpaEntity pago(long id, long companyId,
            SubscriptionPaymentStatus status, String gatewayReference) {
        SubscriptionPaymentJpaEntity entity = mock(SubscriptionPaymentJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getCompanyId()).thenReturn(companyId);
        when(entity.getStatus()).thenReturn(status);
        when(entity.getAmount()).thenReturn(new BigDecimal("149000.00"));
        when(entity.getCurrency()).thenReturn("COP");
        when(entity.getGatewayReference()).thenReturn(gatewayReference);
        when(entity.getReceivedAt()).thenReturn(RECIBIDO_EN);
        return entity;
    }

    @Nested
    @DisplayName("findByCompanyIdAndReference (idempotencia del primer cobro)")
    class FindByCompanyIdAndReference {

        @Test
        @DisplayName("mapea el pago encontrado a FirstPeriodPaymentSnapshot")
        void mapea_el_pago_encontrado_a_snapshot() {
            SubscriptionPaymentJpaEntity entity = pago(5L, EMPRESA_A,
                    SubscriptionPaymentStatus.CONFIRMED, "tx-wompi-1");
            when(subscriptionPaymentJpaRepository.findByCompanyIdAndClientRequestId(EMPRESA_A,
                    REFERENCIA)).thenReturn(Optional.of(entity));

            Optional<FirstPeriodPaymentSnapshot> snapshot = port
                    .findByCompanyIdAndReference(EMPRESA_A, REFERENCIA);

            assertThat(snapshot).contains(new FirstPeriodPaymentSnapshot(5L, EMPRESA_A, "CONFIRMED",
                    new BigDecimal("149000.00"), "COP", "tx-wompi-1", RECIBIDO_EN));
        }

        @Test
        @DisplayName("devuelve vacio si no hay pago con esa referencia")
        void devuelve_vacio_si_no_hay_pago_con_esa_referencia() {
            when(subscriptionPaymentJpaRepository.findByCompanyIdAndClientRequestId(EMPRESA_A,
                    REFERENCIA)).thenReturn(Optional.empty());

            assertThat(port.findByCompanyIdAndReference(EMPRESA_A, REFERENCIA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByGatewayAndReference (busqueda global, sin empresa: la usa el webhook)")
    class FindByGatewayAndReference {

        @Test
        @DisplayName("mapea el pago encontrado por (gateway, gatewayReference)")
        void mapea_el_pago_encontrado_por_gateway_y_referencia() {
            SubscriptionPaymentJpaEntity entity = pago(5L, EMPRESA_A,
                    SubscriptionPaymentStatus.PENDING, "tx-wompi-1");
            when(subscriptionPaymentJpaRepository.findByGatewayAndGatewayReference("WOMPI",
                    "tx-wompi-1")).thenReturn(Optional.of(entity));

            Optional<FirstPeriodPaymentSnapshot> snapshot = port.findByGatewayAndReference("WOMPI",
                    "tx-wompi-1");

            assertThat(snapshot).contains(new FirstPeriodPaymentSnapshot(5L, EMPRESA_A, "PENDING",
                    new BigDecimal("149000.00"), "COP", "tx-wompi-1", RECIBIDO_EN));
        }

        @Test
        @DisplayName("devuelve vacio si no hay pago con esa referencia de pasarela")
        void devuelve_vacio_si_no_hay_pago_con_esa_referencia_de_pasarela() {
            when(subscriptionPaymentJpaRepository.findByGatewayAndGatewayReference("WOMPI",
                    "tx-inexistente")).thenReturn(Optional.empty());

            assertThat(port.findByGatewayAndReference("WOMPI", "tx-inexistente")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("consulta el repositorio con el companyId exacto que recibe")
        void consulta_el_repositorio_con_el_company_id_exacto() {
            when(subscriptionPaymentJpaRepository.findByCompanyIdAndClientRequestId(EMPRESA_A,
                    REFERENCIA)).thenReturn(Optional.empty());

            port.findByCompanyIdAndReference(EMPRESA_A, REFERENCIA);

            verify(subscriptionPaymentJpaRepository)
                    .findByCompanyIdAndClientRequestId(eq(EMPRESA_A), eq(REFERENCIA));
        }

        @Test
        @DisplayName("el pago del primer periodo de otra empresa no se devuelve con esta misma referencia")
        void el_pago_de_otra_empresa_no_se_devuelve() {
            SubscriptionPaymentJpaEntity entityEmpresaA = pago(5L, EMPRESA_A,
                    SubscriptionPaymentStatus.CONFIRMED, "tx-wompi-1");
            when(subscriptionPaymentJpaRepository.findByCompanyIdAndClientRequestId(EMPRESA_A,
                    REFERENCIA)).thenReturn(Optional.of(entityEmpresaA));
            when(subscriptionPaymentJpaRepository.findByCompanyIdAndClientRequestId(EMPRESA_B,
                    REFERENCIA)).thenReturn(Optional.empty());

            assertThat(port.findByCompanyIdAndReference(EMPRESA_A, REFERENCIA)).isPresent();
            assertThat(port.findByCompanyIdAndReference(EMPRESA_B, REFERENCIA)).isEmpty();
        }
    }
}
