package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListEconomicActivitiesService")
class ListEconomicActivitiesServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private ListEconomicActivitiesService service;

    @BeforeEach
    void setUp() {
        service = new ListEconomicActivitiesService(repository);
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada actividad del repositorio a su dto")
        void mapea_cada_actividad_a_su_dto() {
            EconomicActivity uno = EconomicActivityMother.existente(1L);
            EconomicActivity dos = EconomicActivityMother.deshabilitada();
            when(repository.findAll()).thenReturn(List.of(uno, dos));

            List<EconomicActivityDto> dtos = service.listAll();

            assertThat(dtos).extracting(EconomicActivityDto::id).containsExactly(1L,
                    EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
        }

        @Test
        @DisplayName("devuelve una lista vacia si el repositorio no tiene actividades")
        void devuelve_lista_vacia_si_no_hay_actividades() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
