package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationTypeQueryPort;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
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
@DisplayName("CreateVaccinationService")
class CreateVaccinationServiceTest {

    @Mock
    private VaccinationRepository repository;
    @Mock
    private VaccinationTypeQueryPort vaccinationTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateVaccinationService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateVaccinationService(repository, vaccinationTypeQueryPort,
                animalQueryPort, consultationQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("resuelve las cuatro referencias y persiste la vacuna")
        void resuelve_las_cuatro_referencias_y_persiste_la_vacuna() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.RABIA));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.CONSULTA.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CONSULTA));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationDto dto = service.execute(VaccinationMother.comandoCrear());

            ArgumentCaptor<Vaccination> guardada = ArgumentCaptor.forClass(Vaccination.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getVaccinationType()).isEqualTo(VaccinationMother.RABIA);
            assertThat(guardada.getValue().getAnimal()).isEqualTo(VaccinationMother.FIRULAIS);
            assertThat(guardada.getValue().getConsultation()).isEqualTo(VaccinationMother.CONSULTA);
            assertThat(guardada.getValue().getCompany()).isEqualTo(VaccinationMother.CLINICA);
            assertThat(dto.lot()).isEqualTo("L-2026-A");
        }

        @Test
        @DisplayName("sin consultationId no consulta el puerto de consultas")
        void sin_consultation_id_no_consulta_el_puerto_de_consultas() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.RABIA));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.FIRULAIS));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(VaccinationMother.comandoCrearSinConsulta());

            verifyNoInteractions(consultationQueryPort);
        }
    }

    @Nested
    @DisplayName("validaciones que fallan")
    class Validaciones {

        @Test
        @DisplayName("tipo de vacuna inexistente no toca el repositorio")
        void tipo_de_vacuna_inexistente_no_toca_el_repositorio() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "VaccinationType not found: " + VaccinationMother.RABIA.id());

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort,
                    repository);
        }

        @Test
        @DisplayName("animal inexistente no toca el repositorio")
        void animal_inexistente_no_toca_el_repositorio() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.RABIA));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + VaccinationMother.FIRULAIS.id());

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("consulta inexistente no toca el repositorio")
        void consulta_inexistente_no_toca_el_repositorio() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.RABIA));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.CONSULTA.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + VaccinationMother.CONSULTA.id());

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente no toca el repositorio")
        void empresa_inexistente_no_toca_el_repositorio() {
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.RABIA.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.RABIA));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.CONSULTA.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CONSULTA));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + VaccinationMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
