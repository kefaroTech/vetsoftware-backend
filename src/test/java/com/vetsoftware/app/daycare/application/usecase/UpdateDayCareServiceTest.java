package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private static final Long EMPRESA = DayCareMother.CLINICA.id();

    @Mock
    private DayCareRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateDayCareService service;

    private static UpdateDayCareCommand comandoValido() {
        return new UpdateDayCareCommand(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 5), DayCareType.HOTEL, "Cama", "Alergico",
                DayCareMother.MICHI.id(), EMPRESA);
    }

    @BeforeEach
    void crearServicio() {
        service = new UpdateDayCareService(repository, animalQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el daycare existente con animal y empresa resueltos")
        void actualiza_el_daycare_existente() {
            DayCare existente = DayCareMother.guarderiaValida();
            when(repository.findByIdAndCompanyId(5L, EMPRESA)).thenReturn(Optional.of(existente));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(), EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.MICHI));
            when(companyQueryPort.findById(EMPRESA)).thenReturn(Optional.of(DayCareMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DayCareDto dto = service.execute(comandoValido());

            ArgumentCaptor<DayCare> guardado = ArgumentCaptor.forClass(DayCare.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getAnimal()).isEqualTo(DayCareMother.MICHI);
            assertThat(guardado.getValue().getType()).isEqualTo(DayCareType.HOTEL);
            assertThat(dto.objects()).isEqualTo("Cama");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca los puertos de referencia si el daycare no existe")
        void no_toca_los_puertos_si_el_daycare_no_existe() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verifyNoInteractions(animalQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("no guarda si el animal no existe")
        void no_guarda_si_el_animal_no_existe() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(), EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DayCareMother.MICHI.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("no guarda si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(), EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.MICHI));
            when(companyQueryPort.findById(EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + EMPRESA);

            verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El {@code @authz.isMyCompany(#command.companyId)} del puerto solo prueba que
         * el actor declara SU empresa. Sin acotar la carga, el {@code update} posterior
         * reescribiria el company de la estancia ajena: apropiacion.
         */
        @Test
        @DisplayName("una estancia de otra empresa es un 404 y no se guarda nada")
        void estancia_de_otra_empresa_no_se_apropia() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository, org.mockito.Mockito.never()).save(any());
            verify(repository, org.mockito.Mockito.never()).findById(any());
            verifyNoInteractions(animalQueryPort, companyQueryPort);
        }

        /**
         * La cuarta forma del defecto, y la que sobrevive a la anterior: aqui la
         * estancia <b>si es mia</b> y lo ajeno es la <b>referencia</b>. Con la carga
         * propia ya acotada nadie puede robarme la fila; lo que se podia era colgarla
         * del animal de otro tenant, porque la referencia se resolvia con
         * {@code findById(animalId)} sin empresa. El {@code verify} de la empresa es la
         * asercion que lo caza: antes esa llamada no la llevaba.
         */
        @Test
        @DisplayName("no puede reapuntar la estancia propia al animal de otra empresa")
        void no_puede_reapuntar_la_estancia_al_animal_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));
            when(animalQueryPort.findByIdAndCompanyId(DayCareMother.MICHI.id(), EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DayCareMother.MICHI.id());

            verify(animalQueryPort).findByIdAndCompanyId(DayCareMother.MICHI.id(), EMPRESA);
            verify(repository, org.mockito.Mockito.never()).save(any());
            verifyNoInteractions(companyQueryPort);
        }
    }
}
