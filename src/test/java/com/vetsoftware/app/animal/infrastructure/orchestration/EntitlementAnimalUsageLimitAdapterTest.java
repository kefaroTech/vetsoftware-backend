package com.vetsoftware.app.animal.infrastructure.orchestration;

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
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntitlementAnimalUsageLimitAdapter")
class EntitlementAnimalUsageLimitAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long ANIMAL_ID = 100L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 3, 10, 9, 14);

    @Mock
    private CheckUsageLimitUseCase checkUsageLimit;
    @Mock
    private RecordCompanyUsageEventUseCase recordUsageEvent;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    private EntitlementAnimalUsageLimitAdapter adapter;

    private void crearAdaptador() {
        adapter = new EntitlementAnimalUsageLimitAdapter(checkUsageLimit, recordUsageEvent,
                systemAuthRunner);
    }

    @Nested
    @DisplayName("checkNotExceeded")
    class Comprobacion {

        @Test
        @DisplayName("consulta el eje ANIMAL sin reinicio de periodo (ALLTIME)")
        void consulta_el_eje_animal_sin_reinicio() {
            crearAdaptador();

            adapter.checkNotExceeded(COMPANY_ID);

            ArgumentCaptor<CheckUsageLimitCommand> captor = ArgumentCaptor
                    .forClass(CheckUsageLimitCommand.class);
            verify(checkUsageLimit).execute(captor.capture());
            assertThat(captor.getValue())
                    .isEqualTo(new CheckUsageLimitCommand(COMPANY_ID, "ANIMAL", "ALLTIME"));
            verifyNoInteractions(systemAuthRunner, recordUsageEvent);
        }
    }

    @Nested
    @DisplayName("record")
    class Registro {

        @Test
        @DisplayName("anota el hecho bajo SystemAuthRunner, con el id del animal ya creado")
        void anota_el_hecho_bajo_system_auth_runner() {
            crearAdaptador();
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(systemAuthRunner).run(any());

            adapter.record(COMPANY_ID, ANIMAL_ID, OCCURRED_AT);

            ArgumentCaptor<RecordCompanyUsageEventCommand> captor = ArgumentCaptor
                    .forClass(RecordCompanyUsageEventCommand.class);
            verify(recordUsageEvent).execute(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RecordCompanyUsageEventCommand(COMPANY_ID,
                    "ANIMAL", ANIMAL_ID, OCCURRED_AT, "ALLTIME", false, null));
        }

        @Test
        @DisplayName("si SystemAuthRunner no ejecuta la accion, no se anota nada")
        void sin_ejecutar_la_accion_no_se_anota_nada() {
            crearAdaptador();

            adapter.record(COMPANY_ID, ANIMAL_ID, OCCURRED_AT);

            verifyNoInteractions(recordUsageEvent);
        }
    }
}
