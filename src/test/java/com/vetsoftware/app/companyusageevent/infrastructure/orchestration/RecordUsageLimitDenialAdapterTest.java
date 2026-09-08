package com.vetsoftware.app.companyusageevent.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordUsageLimitDenialAdapter")
class RecordUsageLimitDenialAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DIMENSION_ID = 5L;

    @Mock
    private RecordLimitEventUseCase recordLimitEvent;
    @Mock
    private Authz authz;

    @InjectMocks
    private RecordUsageLimitDenialAdapter adapter;

    @Nested
    @DisplayName("actor")
    class Actor {

        @Test
        @DisplayName("firma el hecho con el empleado actual, cuando hay contexto de empleado")
        void firma_con_el_empleado_actual() {
            when(authz.currentEmployeeIdOrNull()).thenReturn(3L);

            adapter.limitDenied(COMPANY_ID, DIMENSION_ID, 30, 30, 1);

            ArgumentCaptor<RecordLimitEventCommand> captor = ArgumentCaptor
                    .forClass(RecordLimitEventCommand.class);
            verify(recordLimitEvent).execute(captor.capture());
            RecordLimitEventCommand command = captor.getValue();
            assertThat(command.actor().employeeId()).isEqualTo(3L);
            assertThat(command.actor().process()).isFalse();
        }

        @Test
        @DisplayName("firma como proceso automatico cuando no hay contexto de empleado")
        void firma_como_proceso_automatico_sin_contexto() {
            when(authz.currentEmployeeIdOrNull()).thenReturn(null);

            adapter.limitDenied(COMPANY_ID, DIMENSION_ID, 30, 30, 1);

            ArgumentCaptor<RecordLimitEventCommand> captor = ArgumentCaptor
                    .forClass(RecordLimitEventCommand.class);
            verify(recordLimitEvent).execute(captor.capture());
            assertThat(captor.getValue().actor().process()).isTrue();
        }
    }

    @Test
    @DisplayName("registra el portazo con las cinco cifras y el origen SUBSCRIPTION")
    void registra_el_portazo_con_las_cifras_del_momento() {
        when(authz.currentEmployeeIdOrNull()).thenReturn(3L);

        adapter.limitDenied(COMPANY_ID, DIMENSION_ID, 30, 29, 1);

        ArgumentCaptor<RecordLimitEventCommand> captor = ArgumentCaptor
                .forClass(RecordLimitEventCommand.class);
        verify(recordLimitEvent).execute(captor.capture());
        RecordLimitEventCommand command = captor.getValue();
        assertThat(command.companyId()).isEqualTo(COMPANY_ID);
        assertThat(command.limitDimensionId()).isEqualTo(DIMENSION_ID);
        assertThat(command.eventType()).isEqualTo(LimitEventType.LIMIT_BLOCKED);
        assertThat(command.limitQuantity()).isEqualTo(30);
        assertThat(command.usedQuantity()).isEqualTo(29);
        assertThat(command.requestedDelta()).isEqualTo(1);
        assertThat(command.limitSource()).isEqualTo(LimitSource.SUBSCRIPTION);
        assertThat(command.overrideId()).isNull();
    }

    @Test
    @DisplayName("un fallo al escribir el hecho no sale del adaptador: la negacion ya esta decidida")
    void un_fallo_al_escribir_no_sale_del_adaptador() {
        when(authz.currentEmployeeIdOrNull()).thenReturn(3L);
        when(recordLimitEvent.execute(any())).thenThrow(new RuntimeException("bitacora caida"));

        assertThatCode(() -> adapter.limitDenied(COMPANY_ID, DIMENSION_ID, 30, 30, 1))
                .doesNotThrowAnyException();
    }
}
