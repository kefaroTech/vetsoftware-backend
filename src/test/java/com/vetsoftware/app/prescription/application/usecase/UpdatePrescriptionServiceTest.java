package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.command.UpdatePrescriptionCommand;
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
@DisplayName("UpdatePrescriptionService")
class UpdatePrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdatePrescriptionService service;

    @Captor
    private ArgumentCaptor<Prescription> captor;

    @Nested
    @DisplayName("companyId presente en el comando")
    class ConCompanyId {

        @Test
        @DisplayName("busca por id y empresa, y actualiza con las referencias resueltas")
        void busca_por_id_y_empresa_y_actualiza() {
            Prescription existente = PrescriptionMother.persistida();
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CONSULTA));
            when(companyQueryPort.findById(PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CLINICA));
            when(repository.save(any())).thenReturn(existente);

            PrescriptionDto dto = service.execute(PrescriptionMother.comandoActualizar());

            verify(repository, org.mockito.Mockito.never()).findById(any());
            assertThat(dto.id()).isEqualTo(PrescriptionMother.PRESCRIPTION_ID);
        }

        @Test
        @DisplayName("receta inexistente para esa empresa")
        void receta_inexistente_para_esa_empresa() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoActualizar()))
                    .isInstanceOf(
                            com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PrescriptionMother.PRESCRIPTION_ID));

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }

    @Nested
    @DisplayName("companyId ausente en el comando")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas y usa la empresa de la receta ya persistida")
        void busca_por_id_a_secas_y_usa_la_empresa_persistida() {
            Prescription existente = PrescriptionMother.persistida();
            UpdatePrescriptionCommand comando = new UpdatePrescriptionCommand(
                    PrescriptionMother.PRESCRIPTION_ID, PrescriptionMother.FECHA, "Dermatitis",
                    "Revisar en 15 dias", PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.CONSULTATION_ID, null);
            when(repository.findById(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.of(existente));
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CONSULTA));
            when(companyQueryPort.findById(PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CLINICA));
            when(repository.save(any())).thenReturn(existente);

            service.execute(comando);

            verify(repository, org.mockito.Mockito.never()).findByIdAndCompanyId(any(), any());
            verify(animalQueryPort).findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("animal inexistente en la empresa: no toca consultation ni company")
        void animal_inexistente() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + PrescriptionMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("consulta inexistente en la empresa: no toca company")
        void consulta_inexistente() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + PrescriptionMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(animalQueryPort.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.CONSULTA));
            when(companyQueryPort.findById(PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PrescriptionMother.COMPANY_ID);

            verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }
}
