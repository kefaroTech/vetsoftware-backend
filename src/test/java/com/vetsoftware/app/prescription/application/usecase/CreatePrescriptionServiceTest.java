package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.prescription.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.prescription.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePrescriptionService")
class CreatePrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreatePrescriptionService service;

    @Captor
    private ArgumentCaptor<Prescription> captor;

    private void todasLasReferenciasExisten() {
        when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(PrescriptionMother.MASCOTA));
        when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                PrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.of(PrescriptionMother.CONSULTA));
        when(companyQueryPort.findById(PrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.of(PrescriptionMother.CLINICA));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la receta con las referencias resueltas por los puertos")
        void persiste_la_receta_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(PrescriptionMother.persistida());

            service.execute(PrescriptionMother.comandoCrear());

            verify(repository).save(captor.capture());
            Prescription guardada = captor.getValue();
            assertThat(guardada.getAnimal()).isEqualTo(PrescriptionMother.MASCOTA);
            assertThat(guardada.getConsultation()).isEqualTo(PrescriptionMother.CONSULTA);
            assertThat(guardada.getCompany()).isEqualTo(PrescriptionMother.CLINICA);
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la receta ya persistida")
        void devuelve_el_dto_de_la_receta_persistida() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(PrescriptionMother.persistida());

            PrescriptionDto dto = service.execute(PrescriptionMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(PrescriptionMother.PRESCRIPTION_ID);
            assertThat(dto.diagnosis()).isEqualTo("Otitis externa");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("animal inexistente: no consulta los puertos siguientes ni persiste")
        void animal_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + PrescriptionMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("consulta inexistente")
        void consulta_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + PrescriptionMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CONSULTA));
            when(companyQueryPort.findById(PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PrescriptionMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una fecha nula no llega a persistirse")
        void una_fecha_nula_no_llega_a_persistirse() {
            todasLasReferenciasExisten();
            var comando = new com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand(
                    null, "Otitis externa", "Control en 7 dias", PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.CONSULTATION_ID, PrescriptionMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("date is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy — referencias de otra empresa")
    class Tenancy {

        /**
         * La fuga que sobrevive a tener la carga propia acotada: aqui no se carga
         * ninguna receta previa, asi que nada valida la empresa salvo el filtro de las
         * dos referencias. Con un {@code findById} pelado, un animalId de otro tenant
         * resolvia y la receta quedaba colgada de su historia clinica.
         */
        @Test
        @DisplayName("un animal de otra empresa no resuelve: no persiste ni sigue resolviendo")
        void un_animal_de_otra_empresa_no_resuelve() {
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + PrescriptionMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("una consulta de otra empresa no resuelve: no persiste nada")
        void una_consulta_de_otra_empresa_no_resuelve() {
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + PrescriptionMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }
    }
}
