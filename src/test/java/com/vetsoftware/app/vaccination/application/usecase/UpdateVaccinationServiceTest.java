package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationTypeQueryPort;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
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
@DisplayName("UpdateVaccinationService")
class UpdateVaccinationServiceTest {

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

    private UpdateVaccinationService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateVaccinationService(repository, vaccinationTypeQueryPort,
                animalQueryPort, consultationQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("actualiza la vacuna existente con las cuatro referencias resueltas")
        void actualiza_la_vacuna_existente_con_las_referencias_resueltas() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.of(VaccinationMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.OTRA_CONSULTA.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.OTRA_CONSULTA));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationDto dto = service.execute(VaccinationMother.comandoActualizar());

            ArgumentCaptor<Vaccination> guardada = ArgumentCaptor.forClass(Vaccination.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getVaccinationType())
                    .isEqualTo(VaccinationMother.MOQUILLO);
            assertThat(guardada.getValue().getAnimal()).isEqualTo(VaccinationMother.MICHI);
            assertThat(guardada.getValue().getConsultation())
                    .isEqualTo(VaccinationMother.OTRA_CONSULTA);
            assertThat(guardada.getValue().getLot()).isEqualTo("L-2026-B");
            assertThat(dto.lot()).isEqualTo("L-2026-B");
        }

        @Test
        @DisplayName("sin consultationId no consulta el puerto de consultas")
        void sin_consultation_id_no_consulta_el_puerto_de_consultas() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.of(VaccinationMother.MICHI));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(VaccinationMother.comandoActualizarSinConsulta());

            verifyNoInteractions(consultationQueryPort);
        }
    }

    @Nested
    @DisplayName("validaciones que fallan")
    class Validaciones {

        @Test
        @DisplayName("vacuna inexistente no consulta ningun otro puerto")
        void vacuna_inexistente_no_consulta_ningun_otro_puerto() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(VaccinationNotFoundException.class).hasMessageContaining(
                            "Vaccination not found: " + VaccinationMother.VACCINATION_ID);

            verifyNoInteractions(vaccinationTypeQueryPort, animalQueryPort, consultationQueryPort,
                    companyQueryPort);
        }

        @Test
        @DisplayName("tipo de vacuna inexistente no guarda cambios")
        void tipo_de_vacuna_inexistente_no_guarda_cambios() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "VaccinationType not found: " + VaccinationMother.MOQUILLO.id());

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("animal inexistente no guarda cambios")
        void animal_inexistente_no_guarda_cambios() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + VaccinationMother.MICHI.id());

            verifyNoInteractions(consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("consulta inexistente no guarda cambios")
        void consulta_inexistente_no_guarda_cambios() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.of(VaccinationMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.OTRA_CONSULTA.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + VaccinationMother.OTRA_CONSULTA.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente no guarda cambios")
        void empresa_inexistente_no_guarda_cambios() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.of(VaccinationMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(VaccinationMother.OTRA_CONSULTA.id(),
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.OTRA_CONSULTA));
            when(companyQueryPort.findById(VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + VaccinationMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        private static final Long OTRA_EMPRESA_ID = 77L;

        /**
         * BE-fix3: la carga previa a la edicion pasa de {@code findById(command.id())}
         * a {@code findByIdAndCompanyId(command.id(), command.companyId())} — con un
         * findById a secas, la vacuna de otro tenant se actualizaba igual porque el
         * 
         * @PreAuthorize valida el companyId del comando, no el de la entidad cargada.
         */
        @Test
        @DisplayName("una vacuna de otra empresa no se actualiza: 404 y no toca ningun otro puerto")
        void vacuna_de_otra_empresa_no_se_actualiza() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID, OTRA_EMPRESA_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateVaccinationCommand(VaccinationMother.VACCINATION_ID,
                            VaccinationMother.FECHA, VaccinationMother.MOQUILLO.id(), "L-2026-B",
                            "Reaccion leve", "Intramuscular", "Muslo", VaccinationMother.PROXIMA,
                            VaccinationMother.MICHI.id(), null, OTRA_EMPRESA_ID)))
                    .isInstanceOf(VaccinationNotFoundException.class).hasMessageContaining(
                            "Vaccination not found: " + VaccinationMother.VACCINATION_ID);

            verifyNoInteractions(vaccinationTypeQueryPort, animalQueryPort, consultationQueryPort,
                    companyQueryPort);
            verify(repository, never()).save(any());
        }

        /**
         * La cuarta forma del defecto, y la que sobrevive a la de arriba: aqui la
         * vacuna <b>si es mia</b> y lo ajeno es la <b>referencia</b>. Nadie puede
         * robarme la fila, pero si se podia colgarla del animal de otro tenant —una
         * dosis puesta por mi empresa en el carne de la vecina—, porque la referencia
         * se resolvia con {@code findById(animalId)} sin empresa. El {@code verify} de
         * la empresa es la asercion que lo caza.
         */
        @Test
        @DisplayName("no puede reapuntar la vacuna propia al animal de otra empresa")
        void no_puede_reapuntar_al_animal_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.MOQUILLO));
            when(animalQueryPort.findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + VaccinationMother.MICHI.id());

            verify(animalQueryPort).findByIdAndCompanyId(VaccinationMother.MICHI.id(),
                    VaccinationMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort);
        }

        /**
         * El catalogo de tipos mezcla filas generales con las privadas de cada empresa,
         * asi que la variante acotada es «general O mia»: un tipo general sigue
         * sirviendo, y el privado del vecino deja de servir.
         */
        @Test
        @DisplayName("no puede reapuntar la vacuna propia a un tipo privado de otra empresa")
        void no_puede_reapuntar_a_un_tipo_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));
            when(vaccinationTypeQueryPort.findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "VaccinationType not found: " + VaccinationMother.MOQUILLO.id());

            verify(vaccinationTypeQueryPort).findAvailableByIdAndCompanyId(
                    VaccinationMother.MOQUILLO.id(), VaccinationMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        }
    }
}
