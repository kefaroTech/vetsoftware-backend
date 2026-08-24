package com.vetsoftware.app.dunning.application.usecase;

import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.AHORA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.EMPRESA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.contrato;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.factura;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dunning.application.command.RecordDunningEventCommand;
import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
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

@ExtendWith(MockitoExtension.class)
class RecordDunningEventServiceTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private DunningEventRepository repository;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private BillingDocumentQueryPort billingDocumentQueryPort;

    private RecordDunningEventService service;

    @BeforeEach
    void setUp() {
        service = new RecordDunningEventService(repository, subscriptionQueryPort,
                billingDocumentQueryPort, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("anota el recordatorio con el contrato y la factura resueltos")
        void anota_el_recordatorio() {
            resuelveContrato();
            when(billingDocumentQueryPort.findByIdAndCompanyId(100L, EMPRESA))
                    .thenReturn(Optional.of(factura()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DunningEventDto dto = service.execute(comando(100L, DunningEventType.REMINDER_SENT,
                    DunningChannel.EMAIL, AHORA.minusHours(2)));

            assertThat(dto.subscription().subscriptionNumber()).isEqualTo("SUS-2026-00184");
            assertThat(dto.billingDocument().documentNumber()).isEqualTo("FAC-2026-0001");
            assertThat(dto.occurredAt()).isEqualTo(AHORA.minusHours(2));
            assertThat(dto.createdDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("un evento de contrato no resuelve ninguna factura")
        void evento_de_contrato_sin_factura() {
            resuelveContrato();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DunningEventDto dto = service
                    .execute(comando(null, DunningEventType.READ_ONLY_APPLIED, null, AHORA));

            assertThat(dto.billingDocument()).isNull();
            verifyNoInteractions(billingDocumentQueryPort);
        }

        @Test
        @DisplayName("sin fecha de ocurrencia usa el reloj inyectado, no el del sistema")
        void sin_fecha_usa_el_reloj() {
            resuelveContrato();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null, DunningEventType.GRACE_STARTED, null, null));

            ArgumentCaptor<DunningEvent> guardado = ArgumentCaptor.forClass(DunningEvent.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getOccurredAt()).isEqualTo(AHORA);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un recordatorio sin canal se rechaza y no se escribe nada")
        void recordatorio_sin_canal_no_se_escribe() {
            resuelveContrato();

            assertThatThrownBy(() -> service
                    .execute(comando(null, DunningEventType.REMINDER_SENT, null, AHORA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("channel is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el contrato de otra clinica no se resuelve y aborta el registro")
        void contrato_de_otra_empresa_no_se_resuelve() {
            when(subscriptionQueryPort.findByIdAndCompanyId(11L, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando(null, DunningEventType.GRACE_STARTED, null, AHORA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found: 11");

            verify(repository, never()).save(any());
            verifyNoInteractions(billingDocumentQueryPort);
        }

        @Test
        @DisplayName("la factura de otra clinica no se resuelve y aborta el registro")
        void factura_de_otra_empresa_no_se_resuelve() {
            resuelveContrato();
            when(billingDocumentQueryPort.findByIdAndCompanyId(100L, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    comando(100L, DunningEventType.REMINDER_SENT, DunningChannel.SMS, AHORA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BillingDocument not found: 100");

            verify(repository, never()).save(any());
        }
    }

    private void resuelveContrato() {
        when(subscriptionQueryPort.findByIdAndCompanyId(11L, EMPRESA))
                .thenReturn(Optional.of(contrato()));
    }

    private static RecordDunningEventCommand comando(Long billingDocumentId,
            DunningEventType eventType, DunningChannel channel,
            java.time.LocalDateTime occurredAt) {
        return new RecordDunningEventCommand(EMPRESA, 11L, billingDocumentId, eventType, 5, channel,
                "Aviso", occurredAt);
    }
}
