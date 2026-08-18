package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
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
@DisplayName("UpdateHospitalizationService")
class UpdateHospitalizationServiceTest {

    @Mock
    private HospitalizationRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateHospitalizationService service;

    @Captor
    private ArgumentCaptor<Hospitalization> guardada;

    private void existeLaHospitalizacion() {
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.internado()));
    }

    private void todasLasReferenciasExisten() {
        when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
        when(consultationQueryPort.findByIdAndCompanyId(HospitalizationMother.CONSULTATION_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.CONSULTA));
        when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.CLINICA));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica los campos del comando sobre la entidad cargada")
        void aplica_los_campos_del_comando() {
            existeLaHospitalizacion();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(HospitalizationMother.comandoActualizar());

            verify(repository).save(guardada.capture());
            Hospitalization actualizada = guardada.getValue();
            assertThat(actualizada.getId()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(actualizada.getReason()).isEqualTo("Gastroenteritis controlada");
            assertThat(actualizada.getObservations()).isEqualTo("Alta con dieta");
            assertThat(actualizada.getReasonLeaving()).isEqualTo(ReasonLeaving.HOME_TREATMENT);
            assertThat(actualizada.getType()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(actualizada.getAnimal()).isEqualTo(HospitalizationMother.FIRULAIS);
            assertThat(actualizada.getConsultation()).isEqualTo(HospitalizationMother.CONSULTA);
            assertThat(actualizada.getCompany()).isEqualTo(HospitalizationMother.CLINICA);
            // createdDate es final: la actualizacion no puede reescribir el alta.
            assertThat(actualizada.getCreatedDate()).isEqualTo(HospitalizationMother.CREADO);
        }

        @Test
        @DisplayName("devuelve el DTO de lo devuelto por save, no de la entidad de entrada")
        void devuelve_el_dto_de_lo_guardado() {
            existeLaHospitalizacion();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            HospitalizationDto dto = service.execute(HospitalizationMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(dto.reason()).isEqualTo("Gastroenteritis aguda");
        }

        @Test
        @DisplayName("quitar la consulta no consulta el puerto y deja la referencia nula")
        void quitar_la_consulta_deja_la_referencia_nula() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.CLINICA));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(HospitalizationMother.comandoActualizarSinConsulta());

            verifyNoInteractions(consultationQueryPort);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("errores")
    class Errores {

        @Test
        @DisplayName("hospitalizacion inexistente: 404 de dominio y cero escrituras")
        void hospitalizacion_inexistente() {
            when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                    HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(HospitalizationNotFoundException.class)
                    .hasMessageContaining("Hospitalization not found: "
                            + HospitalizationMother.HOSPITALIZATION_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("animal inexistente: no persiste ni resuelve las referencias siguientes")
        void animal_inexistente() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + HospitalizationMother.ANIMAL_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort);
        }

        @Test
        @DisplayName("consulta inexistente: no persiste")
        void consulta_inexistente() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(HospitalizationMother.CONSULTATION_ID,
                    HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + HospitalizationMother.CONSULTATION_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(HospitalizationMother.CONSULTATION_ID,
                    HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.CONSULTA));
            when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + HospitalizationMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una invariante rota en el comando aborta antes del save")
        void invariante_rota_aborta_antes_del_save() {
            existeLaHospitalizacion();
            todasLasReferenciasExisten();
            UpdateHospitalizationCommand comando = new UpdateHospitalizationCommand(
                    HospitalizationMother.HOSPITALIZATION_ID, HospitalizationMother.FECHA,
                    HospitalizationMother.INICIO, HospitalizationMother.FIN,
                    HospitalizationType.HOSPITALIZATION, null, "x".repeat(501), null,
                    HospitalizationMother.ANIMAL_ID, HospitalizationMother.CONSULTATION_ID,
                    HospitalizationMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason must be 500 chars or less");

            verify(repository, never()).save(any());
        }
    }

    /**
     * El {@code @PreAuthorize} de {@code UpdateHospitalizationUseCase} solo prueba
     * que el atacante declara SU propia empresa: {@code command.companyId()} lo
     * pone el controller con {@code authz.currentCompanyId()}, nunca es la empresa
     * de la entidad. Mientras la carga fue {@code findById(command.id())} el gate
     * era vacuo y el efecto no era un rechazo sino una APROPIACION — la fila de la
     * empresa B pasaba a ser de A al reescribir {@code company}. La carga acotada
     * es lo que cierra esa via.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una hospitalizacion de otra empresa no se actualiza: 404 y no persiste nada")
        void una_hospitalizacion_de_otra_empresa_no_se_actualiza() {
            when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                    HospitalizationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    HospitalizationMother.comandoActualizar(HospitalizationMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(HospitalizationNotFoundException.class)
                    .hasMessageContaining("Hospitalization not found: "
                            + HospitalizationMother.HOSPITALIZATION_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        }

        /**
         * La cuarta forma del defecto, y la que sobrevive a la de arriba: aqui la
         * hospitalizacion <b>si es mia</b> y lo ajeno es la <b>referencia</b>. Nadie
         * puede robarme la fila —eso ya lo cierra la carga acotada—, pero si se podia
         * colgarla del animal de otro tenant, porque la referencia se resolvia con
         * {@code findById(animalId)} y ese metodo no llevaba empresa. El {@code verify}
         * de la empresa es la asercion que lo caza.
         */
        @Test
        @DisplayName("no puede reapuntar la hospitalizacion propia al animal de otra empresa")
        void no_puede_reapuntar_al_animal_de_otra_empresa() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + HospitalizationMother.ANIMAL_ID);

            verify(animalQueryPort).findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort);
        }

        /** La misma fuga con el otro padre: la consulta. */
        @Test
        @DisplayName("no puede reapuntar la hospitalizacion propia a la consulta de otra empresa")
        void no_puede_reapuntar_a_la_consulta_de_otra_empresa() {
            existeLaHospitalizacion();
            when(animalQueryPort.findByIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(HospitalizationMother.CONSULTATION_ID,
                    HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + HospitalizationMother.CONSULTATION_ID);

            verify(consultationQueryPort).findByIdAndCompanyId(
                    HospitalizationMother.CONSULTATION_ID, HospitalizationMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(companyQueryPort);
        }
    }
}
