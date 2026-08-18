package com.vetsoftware.app.animalalert.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.animalalert.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAnimalAlertService")
class CreateAnimalAlertServiceTest {

    @Mock
    private AnimalAlertRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateAnimalAlertService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve animal y empresa por sus puertos y persiste la alerta")
        void resuelve_animal_y_empresa_y_persiste_la_alerta() {
            when(animalQueryPort.findByIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalAlertMother.FIRULAIS));
            when(companyQueryPort.findById(AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalAlertMother.CLINICA));
            when(repository.save(any(AnimalAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            AnimalAlertDto dto = service.execute(AnimalAlertMother.comandoCrear());

            assertThat(dto.animalId()).isEqualTo(AnimalAlertMother.ANIMAL_ID);
            assertThat(dto.description()).isEqualTo("Alergia a la penicilina");

            ArgumentCaptor<AnimalAlert> captor = ArgumentCaptor.forClass(AnimalAlert.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(captor.getValue().getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
            assertThat(captor.getValue().getId()).isNull();
        }
    }

    @Nested
    @DisplayName("animal ajeno o inexistente")
    class AnimalNoEncontrado {

        @Test
        @DisplayName("un animal que no existe en la empresa no llega a resolver la empresa ni a guardar")
        void un_animal_inexistente_no_toca_el_resto() {
            when(animalQueryPort.findByIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalAlertMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + AnimalAlertMother.ANIMAL_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaNoEncontrada {

        @Test
        @DisplayName("una empresa que no existe no llega a guardar")
        void una_empresa_inexistente_no_guarda() {
            when(animalQueryPort.findByIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalAlertMother.FIRULAIS));
            when(companyQueryPort.findById(AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalAlertMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + AnimalAlertMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
