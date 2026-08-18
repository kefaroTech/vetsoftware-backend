package com.vetsoftware.app.medicamentprescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMedicamentPrescriptionService")
class UpdateMedicamentPrescriptionServiceTest {

    @Mock
    private MedicamentPrescriptionRepository repository;
    @Mock
    private PrescriptionQueryPort prescriptionQueryPort;
    @Mock
    private MedicamentQueryPort medicamentQueryPort;

    @InjectMocks
    private UpdateMedicamentPrescriptionService service;

    @Nested
    @DisplayName("companyId presente en el comando")
    class ConCompanyId {

        @Test
        @DisplayName("busca por id y empresa, y resuelve el medicamento con findAvailableById")
        void busca_por_id_y_empresa_y_resuelve_disponibilidad() {
            MedicamentPrescription existente = MedicamentPrescriptionMother.persistida();
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.MEDICAMENTO));
            when(repository.save(any())).thenReturn(existente);

            MedicamentPrescriptionDto dto = service.execute(MedicamentPrescriptionMother
                    .comandoActualizar(MedicamentPrescriptionMother.COMPANY_ID));

            verify(repository, never()).findById(any());
            verify(medicamentQueryPort, never()).findById(any());
            assertThat(dto.id()).isEqualTo(MedicamentPrescriptionMother.ID);
        }

        @Test
        @DisplayName("linea inexistente para esa empresa")
        void linea_inexistente_para_esa_empresa() {
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother
                    .comandoActualizar(MedicamentPrescriptionMother.COMPANY_ID)))
                    .isInstanceOf(MedicamentPrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(MedicamentPrescriptionMother.ID));

            verifyNoInteractions(prescriptionQueryPort, medicamentQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("companyId ausente en el comando")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas y resuelve el medicamento sin acotar por empresa")
        void busca_por_id_a_secas() {
            MedicamentPrescription existente = MedicamentPrescriptionMother.persistida();
            UpdateMedicamentPrescriptionCommand comando = MedicamentPrescriptionMother
                    .comandoActualizar(null);
            when(repository.findById(MedicamentPrescriptionMother.ID))
                    .thenReturn(Optional.of(existente));
            when(prescriptionQueryPort.findById(MedicamentPrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findById(MedicamentPrescriptionMother.MEDICAMENT_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.MEDICAMENTO));
            when(repository.save(any())).thenReturn(existente);

            service.execute(comando);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(prescriptionQueryPort).findById(MedicamentPrescriptionMother.PRESCRIPTION_ID);
            verify(medicamentQueryPort).findById(MedicamentPrescriptionMother.MEDICAMENT_ID);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("receta inexistente en la empresa: no consulta el medicamento ni persiste")
        void receta_inexistente() {
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother
                    .comandoActualizar(MedicamentPrescriptionMother.COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prescription not found: "
                            + MedicamentPrescriptionMother.PRESCRIPTION_ID);

            verifyNoInteractions(medicamentQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("medicamento no disponible para la empresa")
        void medicamento_no_disponible() {
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother
                    .comandoActualizar(MedicamentPrescriptionMother.COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Medicament not found: " + MedicamentPrescriptionMother.MEDICAMENT_ID);

            verify(repository, never()).save(any());
        }
    }
}
