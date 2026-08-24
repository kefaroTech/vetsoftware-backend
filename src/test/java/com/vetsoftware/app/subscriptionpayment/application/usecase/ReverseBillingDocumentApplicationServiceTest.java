package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.aplicacionDePago;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplicationNotFoundException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReverseBillingDocumentApplicationServiceTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final ReverseBillingDocumentApplicationCommand COMANDO = new ReverseBillingDocumentApplicationCommand(
            500L, EMPRESA);

    @Mock
    private BillingDocumentApplicationRepository repository;
    @Mock
    private BillingDocumentQueryPort billingDocumentQueryPort;
    @Mock
    private BillingDocumentSettlementPort settlementPort;
    @Mock
    private DunningReevaluationPort dunningReevaluationPort;

    private ReverseBillingDocumentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ReverseBillingDocumentApplicationService(repository, billingDocumentQueryPort,
                settlementPort, dunningReevaluationPort, RELOJ);
    }

    @Nested
    @DisplayName("Contra-aplicacion")
    class ContraAplicacion {

        @Test
        @DisplayName("crea una fila negativa que apunta a la original en vez de borrarla")
        void crea_la_contra_aplicacion() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(aplicacionDePago()));
            when(repository.findByReversalOfIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentApplicationDto dto = service.execute(COMANDO);

            assertThat(dto.appliedAmount()).isEqualByComparingTo("-200000.00");
            assertThat(dto.reversalOfId()).isEqualTo(500L);
            ArgumentCaptor<BillingDocumentApplication> guardada = ArgumentCaptor
                    .forClass(BillingDocumentApplication.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getTargetDocument().id()).isEqualTo(100L);
            assertThat(guardada.getValue().getPaymentId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("recalcula el saldo del destino despues de guardar la reversa")
        void recalcula_el_saldo() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(aplicacionDePago()));
            when(repository.findByReversalOfIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(COMANDO);

            InOrder orden = inOrder(repository, settlementPort, dunningReevaluationPort);
            orden.verify(repository).save(any());
            orden.verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            orden.verify(dunningReevaluationPort).reevaluate(100L, EMPRESA);
        }

        @Test
        @DisplayName("bloquea el documento destino antes de buscar la reversa existente")
        void bloquea_antes_de_buscar() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(aplicacionDePago()));
            when(repository.findByReversalOfIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(COMANDO);

            InOrder orden = inOrder(billingDocumentQueryPort, repository);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(100L, EMPRESA);
            orden.verify(repository).findByReversalOfIdAndCompanyId(500L, EMPRESA);
        }
    }

    @Nested
    @DisplayName("R13 - idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("revertir dos veces devuelve la misma reversa y no inserta otra")
        void revertir_dos_veces_no_duplica() {
            BillingDocumentApplication yaRevertida = BillingDocumentApplication
                    .reversalOf(aplicacionDePago(), AHORA);
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(aplicacionDePago()));
            when(repository.findByReversalOfIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(yaRevertida));

            BillingDocumentApplicationDto dto = service.execute(COMANDO);

            assertThat(dto.appliedAmount()).isEqualByComparingTo("-200000.00");
            verify(repository, never()).save(any());
            verifyNoInteractions(settlementPort, dunningReevaluationPort);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la aplicacion de otra clinica no se resuelve y no se escribe nada")
        void aplicacion_de_otra_empresa_no_se_resuelve() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(COMANDO))
                    .isInstanceOf(BillingDocumentApplicationNotFoundException.class)
                    .hasMessageContaining("500");

            verify(repository, never()).save(any());
            verifyNoInteractions(billingDocumentQueryPort, settlementPort, dunningReevaluationPort);
        }
    }
}
