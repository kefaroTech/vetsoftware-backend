package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
@DisplayName("FindEconomicActivityService")
class FindEconomicActivityServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private FindEconomicActivityService service;

    @BeforeEach
    void setUp() {
        service = new FindEconomicActivityService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el dto de la actividad encontrada")
        void devuelve_el_dto_de_la_actividad_encontrada() {
            when(repository.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .thenReturn(Optional.of(EconomicActivityMother.existente()));

            EconomicActivityDto dto = service.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);

            assertThat(dto.id()).isEqualTo(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
            assertThat(dto.code()).isEqualTo(EconomicActivityMother.CODIGO);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("lanza EconomicActivityNotFoundException si no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(EconomicActivityNotFoundException.class)
                    .hasMessageContaining("EconomicActivity not found: 99");

            verifyNoMoreInteractions(repository);
        }
    }
}
