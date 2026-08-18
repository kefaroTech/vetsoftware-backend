package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.dto.PrescriptionReportModel;
import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import com.vetsoftware.app.prescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriberQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionPdfPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionReportQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
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
@DisplayName("ExportPrescriptionService")
class ExportPrescriptionServiceTest {

    private static final Long EMPLOYEE_ID = 700L;
    private static final byte[] PDF_BYTES = {1, 2, 3};

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private PrescriptionReportQueryPort reportQueryPort;
    @Mock
    private MedicamentQueryPort medicamentQueryPort;
    @Mock
    private PrescriberQueryPort prescriberQueryPort;
    @Mock
    private PrescriptionPdfPort pdfPort;

    @InjectMocks
    private ExportPrescriptionService service;

    @Captor
    private ArgumentCaptor<PrescriptionReportModel> modelCaptor;

    private static PrescriptionSignalment signalment() {
        return new PrescriptionSignalment("Veterinaria Test", "900123456", "Calle 1", "3000000000",
                "Medellin", "Firulais", "A-001", "Perro", "Labrador", "Macho", "Negro", "3 años",
                "12 kg", "Juan Perez", "123456", "3111111111", "juan@test.local", "Calle 2");
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("arma el modelo con signalment, prescriptor y medicamentos, y renderiza")
        void arma_el_modelo_y_renderiza() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(reportQueryPort.loadByAnimal(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(signalment()));
            when(medicamentQueryPort.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(PrescriptionMother.unMedicamento());
            when(prescriberQueryPort.findName(EMPLOYEE_ID)).thenReturn(Optional.of("Dra. Ana"));
            when(pdfPort.render(any())).thenReturn(PDF_BYTES);

            byte[] result = service.execute(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID, EMPLOYEE_ID);

            assertThat(result).isEqualTo(PDF_BYTES);
            verify(pdfPort).render(modelCaptor.capture());
            assertThat(modelCaptor.getValue().prescriberName()).isEqualTo("Dra. Ana");
            assertThat(modelCaptor.getValue().medicaments())
                    .containsExactly(PrescriptionMother.MEDICAMENTO);
        }

        @Test
        @DisplayName("sin employeeId no resuelve el nombre del prescriptor")
        void sin_employee_id_no_resuelve_prescriptor() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(reportQueryPort.loadByAnimal(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(signalment()));
            when(medicamentQueryPort.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(PrescriptionMother.unMedicamento());
            when(pdfPort.render(any())).thenReturn(PDF_BYTES);

            service.execute(PrescriptionMother.PRESCRIPTION_ID, PrescriptionMother.COMPANY_ID,
                    null);

            verifyNoInteractions(prescriberQueryPort);
            verify(pdfPort).render(modelCaptor.capture());
            assertThat(modelCaptor.getValue().prescriberName()).isNull();
        }
    }

    @Nested
    @DisplayName("no encontrada o de otra empresa")
    class NoEncontrada {

        @Test
        @DisplayName("receta inexistente")
        void receta_inexistente() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID, EMPLOYEE_ID))
                    .isInstanceOf(PrescriptionNotFoundException.class);

            verifyNoInteractions(reportQueryPort, medicamentQueryPort, prescriberQueryPort,
                    pdfPort);
        }

        @Test
        @DisplayName("receta de otra empresa se trata como inexistente")
        void receta_de_otra_empresa_se_trata_como_inexistente() {
            // El filtro va en la consulta: la receta ajena ni siquiera se carga, asi que
            // no hay forma de que sus datos lleguen al PDF.
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(PrescriptionMother.PRESCRIPTION_ID, 999L, EMPLOYEE_ID))
                    .isInstanceOf(PrescriptionNotFoundException.class);

            verifyNoInteractions(reportQueryPort, medicamentQueryPort, prescriberQueryPort,
                    pdfPort);
        }

        @Test
        @DisplayName("animal no encontrado para la empresa actual")
        void animal_no_encontrado() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(reportQueryPort.loadByAnimal(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID, EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal " + PrescriptionMother.ANIMAL_ID);

            verifyNoInteractions(medicamentQueryPort, prescriberQueryPort, pdfPort);
        }
    }
}
