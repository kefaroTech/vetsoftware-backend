package com.vetsoftware.app.companyusageevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.application.port.out.EffectiveUsageLimitPort;
import com.vetsoftware.app.companyusageevent.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.companyusageevent.application.port.out.UsageLimitDenialPort;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageLimitExceededException;
import com.vetsoftware.app.companyusageevent.domain.EffectiveUsageLimit;
import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckUsageLimitService")
class CheckUsageLimitServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DIMENSION_ID = 5L;
    private static final LimitDimensionRef ANIMAL_DIMENSION = new LimitDimensionRef(DIMENSION_ID,
            "ANIMAL");

    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;
    @Mock
    private CompanyUsageEventRepository repository;
    @Mock
    private EffectiveUsageLimitPort effectiveUsageLimitPort;
    @Mock
    private UsageLimitDenialPort limitDenialPort;

    private CheckUsageLimitService service;

    private void crearServicio() {
        service = new CheckUsageLimitService(limitDimensionQueryPort, repository,
                effectiveUsageLimitPort, limitDenialPort);
    }

    @Nested
    @DisplayName("dentro del cupo")
    class DentroDelCupo {

        @Test
        @DisplayName("un eje sin techo pasa sin contar el consumo")
        void un_eje_sin_techo_pasa_sin_contar() {
            crearServicio();
            when(limitDimensionQueryPort.findByCode("ANIMAL"))
                    .thenReturn(Optional.of(ANIMAL_DIMENSION));
            when(effectiveUsageLimitPort.resolve(COMPANY_ID, DIMENSION_ID))
                    .thenReturn(new EffectiveUsageLimit(null, true));

            assertThatCode(() -> service
                    .execute(new CheckUsageLimitCommand(COMPANY_ID, "ANIMAL", "ALLTIME")))
                    .doesNotThrowAnyException();

            verifyNoInteractions(repository, limitDenialPort);
        }

        @Test
        @DisplayName("por debajo del techo pasa sin denegar")
        void por_debajo_del_techo_pasa() {
            crearServicio();
            when(limitDimensionQueryPort.findByCode("ANIMAL"))
                    .thenReturn(Optional.of(ANIMAL_DIMENSION));
            when(effectiveUsageLimitPort.resolve(COMPANY_ID, DIMENSION_ID))
                    .thenReturn(new EffectiveUsageLimit(100, false));
            when(repository.countCurrent(COMPANY_ID, DIMENSION_ID, "ALLTIME")).thenReturn(99L);

            assertThatCode(() -> service
                    .execute(new CheckUsageLimitCommand(COMPANY_ID, "ANIMAL", "ALLTIME")))
                    .doesNotThrowAnyException();

            verifyNoInteractions(limitDenialPort);
        }
    }

    @Nested
    @DisplayName("cupo agotado")
    class CupoAgotado {

        @Test
        @DisplayName("en el techo, deniega y registra el portazo antes de lanzar")
        void en_el_techo_deniega_y_registra() {
            crearServicio();
            when(limitDimensionQueryPort.findByCode("ANIMAL"))
                    .thenReturn(Optional.of(ANIMAL_DIMENSION));
            when(effectiveUsageLimitPort.resolve(COMPANY_ID, DIMENSION_ID))
                    .thenReturn(new EffectiveUsageLimit(100, false));
            when(repository.countCurrent(COMPANY_ID, DIMENSION_ID, "ALLTIME")).thenReturn(100L);

            assertThatThrownBy(() -> service
                    .execute(new CheckUsageLimitCommand(COMPANY_ID, "ANIMAL", "ALLTIME")))
                    .isInstanceOf(CompanyUsageLimitExceededException.class)
                    .hasMessageContaining("ANIMAL");

            verify(limitDenialPort).limitDenied(eq(COMPANY_ID), eq(DIMENSION_ID), eq(100), eq(100),
                    eq(1));
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("un eje que no esta en el catalogo no consulta nada mas")
        void un_eje_desconocido_no_consulta_nada_mas() {
            crearServicio();
            when(limitDimensionQueryPort.findByCode("DESCONOCIDO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new CheckUsageLimitCommand(COMPANY_ID, "DESCONOCIDO", "ALLTIME")))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).countCurrent(eq(COMPANY_ID), eq(DIMENSION_ID),
                    eq("ALLTIME"));
            verifyNoInteractions(effectiveUsageLimitPort, limitDenialPort);
        }
    }
}
