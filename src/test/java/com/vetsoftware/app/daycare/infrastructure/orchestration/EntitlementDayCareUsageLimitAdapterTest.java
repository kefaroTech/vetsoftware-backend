package com.vetsoftware.app.daycare.infrastructure.orchestration;

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
@DisplayName("EntitlementDayCareUsageLimitAdapter")
class EntitlementDayCareUsageLimitAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DAYCARE_ID = 500L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 3, 10, 9, 14);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T09:14:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CheckUsageLimitUseCase checkUsageLimit;
    @Mock
    private RecordCompanyUsageEventUseCase recordUsageEvent;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    private EntitlementDayCareUsageLimitAdapter adapter;

    private void crearAdaptador() {
        adapter = new EntitlementDayCareUsageLimitAdapter(checkUsageLimit, recordUsageEvent,
                systemAuthRunner, CLOCK);
    }

    @Nested
    @DisplayName("checkNotExceeded")
    class Comprobacion {

        @Test
        @DisplayName("consulta el eje GROOMING_SERVICE con la clave del mes en curso")
        void consulta_el_eje_grooming_service_del_mes_en_curso() {
            crearAdaptador();

            adapter.checkNotExceeded(COMPANY_ID);

            ArgumentCaptor<CheckUsageLimitCommand> captor = ArgumentCaptor
                    .forClass(CheckUsageLimitCommand.class);
            verify(checkUsageLimit).execute(captor.capture());
            assertThat(captor.getValue()).isEqualTo(
                    new CheckUsageLimitCommand(COMPANY_ID, "GROOMING_SERVICE", "2026-03"));
            verifyNoInteractions(systemAuthRunner, recordUsageEvent);
        }
    }

    @Nested
    @DisplayName("record")
    class Registro {

        @Test
        @DisplayName("anota el hecho con origen DAYCARE, bajo SystemAuthRunner")
        void anota_el_hecho_con_origen_daycare() {
            crearAdaptador();
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(systemAuthRunner).run(any());

            adapter.record(COMPANY_ID, DAYCARE_ID, OCCURRED_AT);

            ArgumentCaptor<RecordCompanyUsageEventCommand> captor = ArgumentCaptor
                    .forClass(RecordCompanyUsageEventCommand.class);
            verify(recordUsageEvent).execute(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RecordCompanyUsageEventCommand(COMPANY_ID,
                    "GROOMING_SERVICE", DAYCARE_ID, OCCURRED_AT, "2026-03", false, "DAYCARE"));
        }
    }
}
