package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.command.UpdateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.spa.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
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
@DisplayName("UpdateSpaService")
class UpdateSpaServiceTest {

    @Mock
    private SpaRepository repository;
    @Mock
    private SpaTypeQueryPort spaTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateSpaService service;

    private static UpdateSpaCommand comandoConEmpresa() {
        return new UpdateSpaCommand(5L, LocalDate.of(2026, 3, 1), SpaMother.CORTE_DE_PELO.id(),
                "Corte de verano", "Tijera", "Nervioso", SpaMother.MICHI.id(),
                SpaMother.CLINICA.id());
    }

    private static UpdateSpaCommand comandoSinEmpresa() {
        return new UpdateSpaCommand(5L, LocalDate.of(2026, 3, 1), SpaMother.CORTE_DE_PELO.id(),
                "Corte de verano", "Tijera", "Nervioso", SpaMother.MICHI.id(), null);
    }

    @BeforeEach
    void crearServicio() {
        service = new UpdateSpaService(repository, spaTypeQueryPort, animalQueryPort,
                companyQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("con companyId en el comando busca por empresa y resuelve las refs con esa empresa")
        void con_company_id_busca_por_empresa() {
            Spa existente = SpaMother.spaValido();
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(existente));
            when(spaTypeQueryPort.findById(SpaMother.CORTE_DE_PELO.id()))
                    .thenReturn(Optional.of(SpaMother.CORTE_DE_PELO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.MICHI.id(), SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.MICHI));
            when(companyQueryPort.findById(SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SpaDto dto = service.execute(comandoConEmpresa());

            ArgumentCaptor<Spa> guardado = ArgumentCaptor.forClass(Spa.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getAnimal()).isEqualTo(SpaMother.MICHI);
            assertThat(guardado.getValue().getSpaType()).isEqualTo(SpaMother.CORTE_DE_PELO);
            assertThat(dto.details()).isEqualTo("Tijera");
        }

        @Test
        @DisplayName("sin companyId busca por id global y usa la empresa del spa existente")
        void sin_company_id_usa_la_empresa_del_spa_existente() {
            Spa existente = SpaMother.spaValido();
            when(repository.findById(5L)).thenReturn(Optional.of(existente));
            when(spaTypeQueryPort.findById(SpaMother.CORTE_DE_PELO.id()))
                    .thenReturn(Optional.of(SpaMother.CORTE_DE_PELO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.MICHI.id(),
                    existente.getCompany().id())).thenReturn(Optional.of(SpaMother.MICHI));
            when(companyQueryPort.findById(existente.getCompany().id()))
                    .thenReturn(Optional.of(existente.getCompany()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoSinEmpresa());

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(animalQueryPort).findByIdAndCompanyId(SpaMother.MICHI.id(),
                    existente.getCompany().id());
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca los puertos de referencia si el spa no existe")
        void no_toca_los_puertos_si_el_spa_no_existe() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(SpaNotFoundException.class).hasMessageContaining("5");

            verifyNoInteractions(spaTypeQueryPort, animalQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("no guarda si el tipo de spa no existe")
        void no_guarda_si_el_tipo_no_existe() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.spaValido()));
            when(spaTypeQueryPort.findById(SpaMother.CORTE_DE_PELO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SpaType not found: " + SpaMother.CORTE_DE_PELO.id());

            verifyNoInteractions(animalQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si el animal no existe en esa empresa")
        void no_guarda_si_el_animal_no_existe() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.spaValido()));
            when(spaTypeQueryPort.findById(SpaMother.CORTE_DE_PELO.id()))
                    .thenReturn(Optional.of(SpaMother.CORTE_DE_PELO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.MICHI.id(), SpaMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + SpaMother.MICHI.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.spaValido()));
            when(spaTypeQueryPort.findById(SpaMother.CORTE_DE_PELO.id()))
                    .thenReturn(Optional.of(SpaMother.CORTE_DE_PELO));
            when(animalQueryPort.findByIdAndCompanyId(SpaMother.MICHI.id(), SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.MICHI));
            when(companyQueryPort.findById(SpaMother.CLINICA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + SpaMother.CLINICA.id());

            verify(repository, never()).save(any());
        }
    }
}
