package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.spa.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.testsupport.SpaMother;
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
@DisplayName("CreateSpaService")
class CreateSpaServiceTest {

    @Mock
    private SpaRepository repository;
    @Mock
    private SpaTypeQueryPort spaTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateSpaService service;

    private static CreateSpaCommand comandoValido() {
        return new CreateSpaCommand(LocalDate.of(2026, 2, 1), SpaMother.BANO_BASICO.id(),
                "Baño mensual", "Shampoo hipoalergenico", "Sin novedades", SpaMother.FIRULAIS.id(),
                SpaMother.CLINICA.id());
    }

    @BeforeEach
    void crearServicio() {
        service = new CreateSpaService(repository, spaTypeQueryPort, animalQueryPort,
                companyQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el spa con tipo, animal y empresa resueltos por los puertos")
        void persiste_el_spa_con_las_referencias_resueltas() {
            when(spaTypeQueryPort.findById(SpaMother.BANO_BASICO.id()))
                    .thenReturn(Optional.of(SpaMother.BANO_BASICO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.FIRULAIS.id(),
                    SpaMother.CLINICA.id())).thenReturn(Optional.of(SpaMother.FIRULAIS));
            when(companyQueryPort.findById(SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SpaDto dto = service.execute(comandoValido());

            ArgumentCaptor<Spa> guardado = ArgumentCaptor.forClass(Spa.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getSpaType()).isEqualTo(SpaMother.BANO_BASICO);
            assertThat(guardado.getValue().getAnimal()).isEqualTo(SpaMother.FIRULAIS);
            assertThat(guardado.getValue().getCompany()).isEqualTo(SpaMother.CLINICA);
            assertThat(dto.reason()).isEqualTo("Baño mensual");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca nada mas si el tipo de spa no existe")
        void no_toca_nada_mas_si_el_tipo_no_existe() {
            when(spaTypeQueryPort.findById(SpaMother.BANO_BASICO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SpaType not found: " + SpaMother.BANO_BASICO.id());

            verifyNoInteractions(repository, animalQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("no toca el repositorio si el animal no existe")
        void no_toca_el_repositorio_si_el_animal_no_existe() {
            when(spaTypeQueryPort.findById(SpaMother.BANO_BASICO.id()))
                    .thenReturn(Optional.of(SpaMother.BANO_BASICO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.FIRULAIS.id(),
                    SpaMother.CLINICA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + SpaMother.FIRULAIS.id());

            verifyNoInteractions(repository, companyQueryPort);
        }

        @Test
        @DisplayName("no toca el repositorio si la empresa no existe")
        void no_toca_el_repositorio_si_la_empresa_no_existe() {
            when(spaTypeQueryPort.findById(SpaMother.BANO_BASICO.id()))
                    .thenReturn(Optional.of(SpaMother.BANO_BASICO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.FIRULAIS.id(),
                    SpaMother.CLINICA.id())).thenReturn(Optional.of(SpaMother.FIRULAIS));
            when(companyQueryPort.findById(SpaMother.CLINICA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + SpaMother.CLINICA.id());

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Tenancy — referencia de otra empresa")
    class Tenancy {

        /**
         * Aqui no hay carga previa de ninguna estancia que valide la empresa: el filtro
         * del {@code AnimalQueryPort} es lo unico que impide colgar un spa de esta
         * empresa del animal de la vecina. {@code UpdateSpaService} ya lo hacia asi.
         */
        @Test
        @DisplayName("un animal de otra empresa no resuelve: no persiste nada")
        void un_animal_de_otra_empresa_no_resuelve() {
            when(spaTypeQueryPort.findById(SpaMother.BANO_BASICO.id()))
                    .thenReturn(Optional.of(SpaMother.BANO_BASICO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.FIRULAIS.id(),
                    SpaMother.CLINICA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + SpaMother.FIRULAIS.id());

            verifyNoInteractions(repository, companyQueryPort);
        }
    }
}
