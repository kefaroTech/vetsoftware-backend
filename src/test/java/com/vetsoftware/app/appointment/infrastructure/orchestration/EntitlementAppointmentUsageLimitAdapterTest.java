package com.vetsoftware.app.appointment.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntitlementAppointmentUsageLimitAdapter")
class EntitlementAppointmentUsageLimitAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long APPOINTMENT_ID = 300L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 3, 10, 9, 14);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T09:14:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CheckUsageLimitUseCase checkUsageLimit;
    @Mock
    private RecordCompanyUsageEventUseCase recordUsageEvent;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    private EntitlementAppointmentUsageLimitAdapter adapter;

    private void crearAdaptador() {
        adapter = new EntitlementAppointmentUsageLimitAdapter(checkUsageLimit, recordUsageEvent,
                systemAuthRunner, CLOCK);
    }

    @Nested
    @DisplayName("checkNotExceeded")
    class Comprobacion {

        @Test
        @DisplayName("consulta el eje APPOINTMENT con la clave del mes en curso")
        void consulta_el_eje_appointment_del_mes_en_curso() {
            crearAdaptador();

            adapter.checkNotExceeded(COMPANY_ID);

            ArgumentCaptor<CheckUsageLimitCommand> captor = ArgumentCaptor
                    .forClass(CheckUsageLimitCommand.class);
            verify(checkUsageLimit).execute(captor.capture());
            assertThat(captor.getValue())
                    .isEqualTo(new CheckUsageLimitCommand(COMPANY_ID, "APPOINTMENT", "2026-03"));
            verifyNoInteractions(systemAuthRunner, recordUsageEvent);
        }
    }

    @Nested
    @DisplayName("record")
    class Registro {

        @Test
        @DisplayName("anota el hecho bajo SystemAuthRunner, con el mes en curso y sin origen")
        void anota_el_hecho_bajo_system_auth_runner() {
            crearAdaptador();
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(systemAuthRunner).run(any());

            adapter.record(COMPANY_ID, APPOINTMENT_ID, OCCURRED_AT);

            ArgumentCaptor<RecordCompanyUsageEventCommand> captor = ArgumentCaptor
                    .forClass(RecordCompanyUsageEventCommand.class);
            verify(recordUsageEvent).execute(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RecordCompanyUsageEventCommand(COMPANY_ID,
                    "APPOINTMENT", APPOINTMENT_ID, OCCURRED_AT, "2026-03", false, null));
        }
    }
}
