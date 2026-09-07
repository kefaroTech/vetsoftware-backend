package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.application.command.UpdateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import java.time.LocalDate;
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
@DisplayName("UpdateDayCareService")
class UpdateDayCareServiceTest {

    @Mock
    private DayCareRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateDayCareService service;

    private static UpdateDayCareCommand comandoConEmpresa() {
        return new UpdateDayCareCommand(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), DayCareType.HOTEL, "Cama", "Alergico",
                DayCareMother.MICHI.id(), DayCareMother.CLINICA.id());
    }

    private static UpdateDayCareCommand comandoSinEmpresa() {
        return new UpdateDayCareCommand(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), DayCareType.HOTEL, "Cama", "Alergico",
                DayCareMother.MICHI.id(), null);
    }

    @BeforeEach
    void crearServicio() {
        service = new UpdateDayCareService(repository, animalQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("con companyId en el comando busca por empresa y resuelve las refs con esa empresa")
        void con_company_id_busca_por_empresa() {
            DayCare existente = DayCareMother.guarderiaValida();
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(existente));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(),
                    DayCareMother.CLINICA.id())).thenReturn(Optional.of(DayCareMother.MICHI));
            when(companyQueryPort.findById(DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DayCareDto dto = service.execute(comandoConEmpresa());

            ArgumentCaptor<DayCare> guardado = ArgumentCaptor.forClass(DayCare.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getAnimal()).isEqualTo(DayCareMother.MICHI);
            assertThat(guardado.getValue().getType()).isEqualTo(DayCareType.HOTEL);
            assertThat(dto.objects()).isEqualTo("Cama");
        }

        @Test
        @DisplayName("sin companyId busca por id global y usa la empresa del daycare existente")
        void sin_company_id_usa_la_empresa_del_daycare_existente() {
            DayCare existente = DayCareMother.guarderiaValida();
            when(repository.findById(5L)).thenReturn(Optional.of(existente));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(),
                    existente.getCompany().id())).thenReturn(Optional.of(DayCareMother.MICHI));
            when(companyQueryPort.findById(existente.getCompany().id()))
                    .thenReturn(Optional.of(existente.getCompany()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoSinEmpresa());

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(animalQueryPort).findByIdAndCompanyId(DayCareMother.MICHI.id(),
                    existente.getCompany().id());
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca los puertos de referencia si el daycare no existe")
        void no_toca_los_puertos_si_el_daycare_no_existe() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verifyNoInteractions(animalQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("no guarda si el animal no existe en esa empresa")
        void no_guarda_si_el_animal_no_existe() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(),
                    DayCareMother.CLINICA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DayCareMother.MICHI.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(),
                    DayCareMother.CLINICA.id())).thenReturn(Optional.of(DayCareMother.MICHI));
            when(companyQueryPort.findById(DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + DayCareMother.CLINICA.id());

            verify(repository, never()).save(any());
        }
    }
}
