package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateEconomicActivityService")
class CreateEconomicActivityServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private CreateEconomicActivityService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new CreateEconomicActivityService(repository);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la actividad con el codigo y el nombre del command")
        void persiste_la_actividad_con_el_codigo_y_el_nombre() {
            when(repository.existsByCode("0111")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EconomicActivityDto dto = service.execute(EconomicActivityMother.comandoCrear());

            ArgumentCaptor<EconomicActivity> guardada = ArgumentCaptor
                    .forClass(EconomicActivity.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCode()).isEqualTo(EconomicActivityMother.CODIGO);
            assertThat(guardada.getValue().getName()).isEqualTo(EconomicActivityMother.NOMBRE);
            assertThat(guardada.getValue().isEnabled()).isTrue();
            assertThat(dto.code()).isEqualTo(EconomicActivityMother.CODIGO);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no guarda si el codigo ya existe")
        void no_guarda_si_el_codigo_ya_existe() {
            when(repository.existsByCode("0111")).thenReturn(true);

            assertThatThrownBy(() -> service.execute(EconomicActivityMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EconomicActivity code already exists: 0111");

            verify(repository, never()).save(any());
        }
    }
}
