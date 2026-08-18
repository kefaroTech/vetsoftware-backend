package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateEconomicActivityService")
class ReactivateEconomicActivityServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private ReactivateEconomicActivityService service;

    @BeforeEach
    void setUp() {
        service = new ReactivateEconomicActivityService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el dto releido")
        void reactiva_y_devuelve_el_dto_releido() {
            when(repository.reactivate(EconomicActivityMother.ECONOMIC_ACTIVITY_ID)).thenReturn(1);
            when(repository.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .thenReturn(Optional.of(EconomicActivityMother.existente()));

            EconomicActivityDto dto = service.execute(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);

            assertThat(dto.id()).isEqualTo(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no busca la actividad si el UPDATE no toco ninguna fila")
        void no_busca_la_actividad_si_el_update_no_toco_filas() {
            when(repository.reactivate(99L)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(EconomicActivityNotFoundException.class)
                    .hasMessageContaining("EconomicActivity not found: 99");

            verify(repository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("si reactivo una fila que luego no aparece, tambien falla con not found")
        void falla_si_la_fila_reactivada_no_se_encuentra_despues() {
            when(repository.reactivate(EconomicActivityMother.ECONOMIC_ACTIVITY_ID)).thenReturn(1);
            when(repository.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .isInstanceOf(EconomicActivityNotFoundException.class)
                    .hasMessageContaining("EconomicActivity not found: "
                            + EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
        }
    }
}
