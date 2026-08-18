package com.vetsoftware.app.animalalert.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAnimalAlertsByAnimalService")
class ListAnimalAlertsByAnimalServiceTest {

    @Mock
    private AnimalAlertRepository repository;

    @InjectMocks
    private ListAnimalAlertsByAnimalService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada alerta encontrada a su dto, acotando por animal y empresa")
        void mapea_cada_alerta_encontrada_a_su_dto() {
            when(repository.findByAnimalIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(List.of(AnimalAlertMother.alergia()));

            List<AnimalAlertDto> dtos = service.execute(AnimalAlertMother.consultaPorAnimal());

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).id()).isEqualTo(AnimalAlertMother.ALERT_ID);
            assertThat(dtos.get(0).animalId()).isEqualTo(AnimalAlertMother.ANIMAL_ID);
        }

        @Test
        @DisplayName("un animal sin alertas devuelve una lista vacia, no null")
        void un_animal_sin_alertas_devuelve_lista_vacia() {
            when(repository.findByAnimalIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(List.of());

            assertThat(service.execute(AnimalAlertMother.consultaPorAnimal())).isEmpty();
        }
    }
}
