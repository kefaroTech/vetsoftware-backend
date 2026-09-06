package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence.SubscriptionPaymentMethodJpaEntity;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence.SubscriptionPaymentMethodJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * {@code SubscriptionPaymentMethodJpaEntity} se mockea porque su constructor
 * sin argumentos es {@code protected}, igual que
 * {@code JpaBaseRoleQueryPortTest} con {@code BaseRoleJpaEntity}.
 *
 * <p>
 * <strong>La entidad siempre se construye en una variable propia, nunca como
 * argumento inline de {@code when(...).thenReturn(...)}.</strong>
 * {@code medio(...)} hace sus propios {@code when/thenReturn} sobre la entidad,
 * y evaluarla como argumento de un {@code thenReturn} todavía abierto dejaba a
 * Mockito con un stubbing sin terminar ({@code UnfinishedStubbingException}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDefaultCardPaymentMethodQueryPort")
class JpaDefaultCardPaymentMethodQueryPortTest {

    private static final Long EMPRESA_A = 42L;
    private static final Long EMPRESA_B = 99L;
    private static final String WOMPI = "WOMPI";

    @Mock
    private SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository;
    @InjectMocks
    private JpaDefaultCardPaymentMethodQueryPort port;

    private static SubscriptionPaymentMethodJpaEntity medio(long id, String token,
            boolean defaultMethod, String gateway, MandateStatus mandateStatus) {
        SubscriptionPaymentMethodJpaEntity entity = mock(SubscriptionPaymentMethodJpaEntity.class);
        // lenient(): getId/getToken solo se leen si el filtro deja pasar la entidad; en
        // los escenarios de descarte (no default, otro gateway, mandato no ACTIVE) esta
        // fabrica sigue montando la entidad completa y esos dos stubs quedan sin usar.
        org.mockito.Mockito.lenient().when(entity.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(entity.getToken()).thenReturn(token);
        when(entity.isDefaultMethod()).thenReturn(defaultMethod);
        // lenient(): con defaultMethod = false el filtro corta antes de leer el gateway
        // (cortocircuito de &&), y esta fabrica no distingue ese caso al construir.
        org.mockito.Mockito.lenient().when(entity.getGateway()).thenReturn(gateway);
        org.mockito.Mockito.lenient().when(entity.getMandateStatus()).thenReturn(mandateStatus);
        return entity;
    }

    private static Page<SubscriptionPaymentMethodJpaEntity> pageOf(
            SubscriptionPaymentMethodJpaEntity... entities) {
        return new PageImpl<>(List.of(entities));
    }

    @Nested
    @DisplayName("seleccion del medio predeterminado")
    class SeleccionDelMedioPredeterminado {

        @Test
        @DisplayName("un medio ACTIVE, predeterminado y del gateway pedido se devuelve")
        void un_medio_activo_predeterminado_del_gateway_pedido_se_devuelve() {
            Page<SubscriptionPaymentMethodJpaEntity> pagina = pageOf(
                    medio(10L, "tok_abc", true, WOMPI, MandateStatus.ACTIVE));
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pagina);

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI))
                    .contains(new PaymentMethodRef(10L, "tok_abc"));
        }

        @Test
        @DisplayName("un medio que no es el predeterminado se descarta")
        void un_medio_no_predeterminado_se_descarta() {
            Page<SubscriptionPaymentMethodJpaEntity> pagina = pageOf(
                    medio(10L, "tok_abc", false, WOMPI, MandateStatus.ACTIVE));
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pagina);

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI)).isEmpty();
        }

        @Test
        @DisplayName("un medio predeterminado de otro gateway se descarta")
        void un_medio_predeterminado_de_otro_gateway_se_descarta() {
            Page<SubscriptionPaymentMethodJpaEntity> pagina = pageOf(
                    medio(10L, "tok_abc", true, "OTRA_PASARELA", MandateStatus.ACTIVE));
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pagina);

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI)).isEmpty();
        }

        @Test
        @DisplayName("un medio predeterminado con mandato revocado se descarta")
        void un_medio_predeterminado_con_mandato_revocado_se_descarta() {
            Page<SubscriptionPaymentMethodJpaEntity> pagina = pageOf(
                    medio(10L, "tok_abc", true, WOMPI, MandateStatus.REVOKED));
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pagina);

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI)).isEmpty();
        }

        @Test
        @DisplayName("una empresa sin medios de pago devuelve vacio")
        void una_empresa_sin_medios_devuelve_vacio() {
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pageOf());

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("consulta el repositorio con el companyId exacto que recibe")
        void consulta_el_repositorio_con_el_company_id_exacto() {
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(pageOf());

            port.findDefaultActiveCard(EMPRESA_A, WOMPI);

            verify(paymentMethodJpaRepository).findAllByCompanyId(eq(EMPRESA_A), any());
        }

        @Test
        @DisplayName("el medio predeterminado de otra empresa no se devuelve")
        void el_medio_predeterminado_de_otra_empresa_no_se_devuelve() {
            Page<SubscriptionPaymentMethodJpaEntity> paginaEmpresaA = pageOf(
                    medio(10L, "tok_abc", true, WOMPI, MandateStatus.ACTIVE));
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_A), any()))
                    .thenReturn(paginaEmpresaA);
            when(paymentMethodJpaRepository.findAllByCompanyId(eq(EMPRESA_B), any()))
                    .thenReturn(pageOf());

            assertThat(port.findDefaultActiveCard(EMPRESA_A, WOMPI))
                    .contains(new PaymentMethodRef(10L, "tok_abc"));
            assertThat(port.findDefaultActiveCard(EMPRESA_B, WOMPI)).isEmpty();
        }
    }
}
