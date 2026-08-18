package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateDayCareService")
class CreateDayCareServiceTest {

    private static final Long EMPRESA = DayCareMother.CLINICA.id();

    @Mock
    private DayCareRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateDayCareService service;

    private static CreateDayCareCommand comandoValido() {
        return new CreateDayCareCommand(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa", "Sin novedades",
                DayCareMother.FIRULAIS.id(), DayCareMother.CLINICA.id());
    }

    @org.junit.jupiter.api.BeforeEach
    void crearServicio() {
        service = new CreateDayCareService(repository, animalQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el daycare con animal y empresa resueltos por los puertos")
        void persiste_el_daycare_con_animal_y_empresa_resueltos() {
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.FIRULAIS.id(), EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.FIRULAIS));
            when(companyQueryPort.findById(DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DayCareDto dto = service.execute(comandoValido());

            ArgumentCaptor<DayCare> guardado = ArgumentCaptor.forClass(DayCare.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getAnimal()).isEqualTo(DayCareMother.FIRULAIS);
            assertThat(guardado.getValue().getCompany()).isEqualTo(DayCareMother.CLINICA);
            assertThat(dto.type()).isEqualTo(DayCareType.DAYCARE);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio si el animal no existe")
        void no_toca_el_repositorio_si_el_animal_no_existe() {
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.FIRULAIS.id(), EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DayCareMother.FIRULAIS.id());

            verifyNoInteractions(repository, companyQueryPort);
        }

        @Test
        @DisplayName("no toca el repositorio si la empresa no existe")
        void no_toca_el_repositorio_si_la_empresa_no_existe() {
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.FIRULAIS.id(), EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.FIRULAIS));
            when(companyQueryPort.findById(DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + DayCareMother.CLINICA.id());

            verifyNoInteractions(repository);
        }
    }
}
