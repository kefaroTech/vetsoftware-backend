package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.consultation.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
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
@DisplayName("UpdateConsultationService")
class UpdateConsultationServiceTest {

    @Mock
    private ConsultationRepository repository;
    @Mock
    private ConsultationTypeQueryPort consultationTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateConsultationService service;

    @Captor
    private ArgumentCaptor<Consultation> consultationCaptor;

    private void todasLasReferenciasExistenParaLaEmpresaDelComando() {
        when(consultationTypeQueryPort.findById(ConsultationMother.VACUNACION.id()))
                .thenReturn(Optional.of(ConsultationMother.VACUNACION));
        when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.MICHI.id(),
                ConsultationMother.COMPANY_ID)).thenReturn(Optional.of(ConsultationMother.MICHI));
        when(companyQueryPort.findById(ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.CLINICA));
    }

    @Nested
    @DisplayName("actualizacion con companyId en el comando")
    class ConCompanyId {

        @Test
        @DisplayName("busca por id y empresa y reemplaza cada referencia por la resuelta")
        void busca_por_id_y_empresa_y_reemplaza_cada_referencia() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            todasLasReferenciasExistenParaLaEmpresaDelComando();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(ConsultationMother.comandoActualizar());

            verify(repository).save(consultationCaptor.capture());
            Consultation actualizada = consultationCaptor.getValue();
            assertThat(actualizada.getConsultationType()).isEqualTo(ConsultationMother.VACUNACION);
            assertThat(actualizada.getAnimal()).isEqualTo(ConsultationMother.MICHI);
            assertThat(actualizada.getCompany()).isEqualTo(ConsultationMother.CLINICA);
            assertThat(actualizada.getAnamnesis()).isEqualTo("Anamnesis nueva");
            assertThat(actualizada.getPhysicalExam().temperature()).isEqualByComparingTo("39.0");
        }

        @Test
        @DisplayName("consulta inexistente en la empresa no toca ningun otro puerto")
        void consulta_inexistente_en_la_empresa() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoActualizar()))
                    .isInstanceOf(ConsultationNotFoundException.class).hasMessageContaining(
                            "Consultation not found: " + ConsultationMother.CONSULTATION_ID);

            verifyNoInteractions(consultationTypeQueryPort, animalQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("tipo de consulta inexistente no consulta animal ni empresa ni guarda")
        void tipo_de_consulta_inexistente() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            when(consultationTypeQueryPort.findById(ConsultationMother.VACUNACION.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "ConsultationType not found: " + ConsultationMother.VACUNACION.id());

            verifyNoInteractions(animalQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("animal inexistente en la empresa efectiva no consulta empresa ni guarda")
        void animal_inexistente() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            when(consultationTypeQueryPort.findById(ConsultationMother.VACUNACION.id()))
                    .thenReturn(Optional.of(ConsultationMother.VACUNACION));
            when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.MICHI.id(),
                    ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + ConsultationMother.MICHI.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente no guarda")
        void empresa_inexistente() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            todasLasReferenciasExistenParaLaEmpresaDelComando();
            when(companyQueryPort.findById(ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ConsultationMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizacion con companyId null en el comando")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas y usa la empresa persistida como empresa efectiva")
        void busca_por_id_a_secas_y_usa_la_empresa_persistida() {
            when(repository.findById(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            when(consultationTypeQueryPort.findById(ConsultationMother.VACUNACION.id()))
                    .thenReturn(Optional.of(ConsultationMother.VACUNACION));
            when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.MICHI.id(),
                    ConsultationMother.CLINICA.id()))
                    .thenReturn(Optional.of(ConsultationMother.MICHI));
            when(companyQueryPort.findById(ConsultationMother.CLINICA.id()))
                    .thenReturn(Optional.of(ConsultationMother.CLINICA));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var comandoSinEmpresa = new com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand(
                    ConsultationMother.CONSULTATION_ID, ConsultationMother.FECHA,
                    ConsultationMother.VACUNACION.id(), "Anamnesis nueva", null, null, null,
                    ConsultationMother.MICHI.id(), null, null, null, null, null, null, null, null,
                    null, null, null);

            service.execute(comandoSinEmpresa);

            // El animal se busca acotado por la empresa PERSISTIDA, no por un companyId
            // que nunca llego: esa es la defensa contra colgar el animal en otro tenant.
            verify(animalQueryPort).findByIdAndCompanyId(ConsultationMother.MICHI.id(),
                    ConsultationMother.CLINICA.id());
            verify(repository, never()).findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    null);
        }
    }
}
