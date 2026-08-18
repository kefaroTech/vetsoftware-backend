package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
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
@DisplayName("UpdateEconomicActivityService")
class UpdateEconomicActivityServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private UpdateEconomicActivityService service;

    @BeforeEach
    void setUp() {
        service = new UpdateEconomicActivityService(repository);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza codigo y nombre de la actividad existente")
        void actualiza_codigo_y_nombre() {
            EconomicActivity existente = EconomicActivityMother.existente();
            when(repository.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EconomicActivityDto dto = service.execute(EconomicActivityMother.comandoActualizar());

            ArgumentCaptor<EconomicActivity> guardada = ArgumentCaptor
                    .forClass(EconomicActivity.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCode()).isEqualTo("0112");
            assertThat(guardada.getValue().getName()).isEqualTo("Cultivo de hortalizas");
            assertThat(dto.code()).isEqualTo("0112");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no guarda si la actividad no existe")
        void no_guarda_si_la_actividad_no_existe() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EconomicActivityMother.comandoActualizar(99L)))
                    .isInstanceOf(
                            com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException.class)
                    .hasMessageContaining("EconomicActivity not found: 99");

            verify(repository, never()).save(any());
            verifyNoMoreInteractions(repository);
        }
    }
}
