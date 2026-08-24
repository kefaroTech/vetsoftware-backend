package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoDePasarela;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoDePasarelaDeOtraEmpresa;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoPendiente;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pesos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * R13: toda peticion que mueve dinero lleva llave de idempotencia y <strong>se
 * busca antes de insertar</strong>. Lo que estos tests protegen es que un doble
 * clic del operador o un reintento de la pasarela no cobren dos veces, y que el
 * rechazo no sea un 500 con una violacion de indice unico.
 */
@ExtendWith(MockitoExtension.class)
class RegisterSubscriptionPaymentServiceTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private SubscriptionPaymentRepository repository;

    private RegisterSubscriptionPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RegisterSubscriptionPaymentService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("registra el pago con la fecha de entrada que declaro el operador")
        void registra_el_pago() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionPaymentDto dto = service.execute(comando("req-1", null, null));

            assertThat(dto.status()).isEqualTo(SubscriptionPaymentStatus.PENDING);
            assertThat(dto.amount()).isEqualByComparingTo("500000.00");
            ArgumentCaptor<SubscriptionPayment> guardado = ArgumentCaptor
                    .forClass(SubscriptionPayment.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getReceivedAt()).isEqualTo(AHORA.minusDays(1));
            assertThat(guardado.getValue().getCreatedDate()).isEqualTo(AHORA);
        }
    }

    @Nested
    @DisplayName("R13 - idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("el doble clic devuelve el pago que ya existe y no crea otro")
        void doble_clic_devuelve_el_mismo_pago() {
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-1"))
                    .thenReturn(Optional.of(pagoPendiente()));

            SubscriptionPaymentDto dto = service.execute(comando("req-1", null, null));

            assertThat(dto.id()).isEqualTo(7L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el mismo aviso de la pasarela recibido dos veces no crea dos pagos")
        void el_aviso_repetido_no_duplica() {
            when(repository.findByGatewayAndGatewayReference("wompi", "TX-2026-0001"))
                    .thenReturn(Optional.of(pagoDePasarela()));

            SubscriptionPaymentDto dto = service.execute(comando(null, "wompi", "TX-2026-0001"));

            assertThat(dto.id()).isEqualTo(8L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una llave en blanco no deduplica: se registra el pago")
        void llave_en_blanco_no_deduplica() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando("   ", null, null));

            verify(repository).save(any());
            verify(repository, never()).findByCompanyIdAndClientRequestId(any(), any());
        }

        @Test
        @DisplayName("un pago manual sin pasarela no consulta la barandilla del webhook")
        void sin_pasarela_no_consulta_el_par() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null, null, null));

            verify(repository, never()).findByGatewayAndGatewayReference(any(), any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("una referencia de pasarela tomada por otra clinica se rechaza sin filtrar sus datos")
        void referencia_de_otra_empresa_se_rechaza() {
            when(repository.findByGatewayAndGatewayReference("wompi", "TX-2026-0001"))
                    .thenReturn(Optional.of(pagoDePasarelaDeOtraEmpresa()));

            assertThatThrownBy(() -> service.execute(comando(null, "wompi", "TX-2026-0001")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already registered by another company");

            verify(repository, never()).save(any());
        }
    }

    private static RegisterSubscriptionPaymentCommand comando(String clientRequestId,
            String gateway, String gatewayReference) {
        return new RegisterSubscriptionPaymentCommand(EMPRESA, pesos("500000.00"), "COP",
                PaymentMethod.TRANSFER, gateway, gatewayReference, AHORA.minusDays(1),
                clientRequestId);
    }
}
