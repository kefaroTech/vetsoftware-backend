package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.PrescriptionChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.consultation.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.consultation.domain.ConsultationHasActiveChildrenException;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los siete tipos de hijo se prueban uno a uno, en el orden exacto en que
 * {@code DeleteConsultationService.execute} los comprueba: vaccination,
 * surgery, hospitalization, deworming, diagnosticImaging, laboratoryTest,
 * prescription. Un bucle no distingue un copiar-pegar que compruebe un puerto y
 * reporte el nombre de otro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConsultationService")
class DeleteConsultationServiceTest {

    @Mock
    private ConsultationRepository repository;
    @Mock
    private VaccinationChildrenQueryPort vaccination;
    @Mock
    private SurgeryChildrenQueryPort surgery;
    @Mock
    private HospitalizationChildrenQueryPort hospitalization;
    @Mock
    private DewormingChildrenQueryPort deworming;
    @Mock
    private DiagnosticImagingChildrenQueryPort diagnosticImaging;
    @Mock
    private LaboratoryTestChildrenQueryPort laboratoryTest;
    @Mock
    private PrescriptionChildrenQueryPort prescription;

    @InjectMocks
    private DeleteConsultationService service;

    private void consultaExistePorEmpresa() {
        when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.consultaValida()));
    }

    private void sinHijosActivos() {
        when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(deworming.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(diagnosticImaging.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(laboratoryTest.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
        when(prescription.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                .thenReturn(false);
    }

    private void borrar() {
        service.execute(ConsultationMother.CONSULTATION_ID, ConsultationMother.COMPANY_ID);
    }

    private void assertBloqueadoPor(String tipoHijo) {
        assertThatThrownBy(DeleteConsultationServiceTest.this::borrar)
                .isInstanceOf(ConsultationHasActiveChildrenException.class)
                .hasMessageContaining(
                        "Cannot delete consultation " + ConsultationMother.CONSULTATION_ID)
                .hasMessageContaining("has active " + tipoHijo + " children");
        verify(repository, never()).delete(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin hijos activos borra acotando por la empresa recibida")
        void sin_hijos_activos_borra_acotando_por_la_empresa_recibida() {
            consultaExistePorEmpresa();
            sinHijosActivos();

            borrar();

            verify(repository).delete(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID);
        }

        @Test
        @DisplayName("con companyId null busca por id y borra con la empresa persistida")
        void con_company_id_null_borra_con_la_empresa_persistida() {
            when(repository.findById(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(Optional.of(ConsultationMother.consultaValida()));
            sinHijosActivos();

            service.execute(ConsultationMother.CONSULTATION_ID, null);

            verify(repository).delete(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.CLINICA.id());
            verify(repository, never()).findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    null);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("una consulta de otra empresa no existe y no se consulta ningun hijo")
        void una_consulta_de_otra_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(DeleteConsultationServiceTest.this::borrar)
                    .isInstanceOf(ConsultationNotFoundException.class).hasMessageContaining(
                            "Consultation not found: " + ConsultationMother.CONSULTATION_ID);

            verifyNoInteractions(vaccination, surgery, hospitalization, deworming,
                    diagnosticImaging, laboratoryTest, prescription);
            verify(repository, never()).delete(ConsultationMother.CONSULTATION_ID,
                    ConsultationMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("bloqueo por hijos activos — un tipo por test, en el orden en que se comprueban")
    class BloqueoPorHijos {

        @Test
        @DisplayName("vacunaciones activas")
        void vacunaciones_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("vaccination");
            verifyNoInteractions(surgery, hospitalization, deworming, diagnosticImaging,
                    laboratoryTest, prescription);
        }

        @Test
        @DisplayName("cirugias activas")
        void cirugias_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("surgery");
            verifyNoInteractions(hospitalization, deworming, diagnosticImaging, laboratoryTest,
                    prescription);
        }

        @Test
        @DisplayName("hospitalizaciones activas")
        void hospitalizaciones_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("hospitalization");
            verifyNoInteractions(deworming, diagnosticImaging, laboratoryTest, prescription);
        }

        @Test
        @DisplayName("desparasitaciones activas")
        void desparasitaciones_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(deworming.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("deworming");
            verifyNoInteractions(diagnosticImaging, laboratoryTest, prescription);
        }

        @Test
        @DisplayName("imagenes diagnosticas activas")
        void imagenes_diagnosticas_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(deworming.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(diagnosticImaging.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("diagnosticImaging");
            verifyNoInteractions(laboratoryTest, prescription);
        }

        @Test
        @DisplayName("examenes de laboratorio activos")
        void examenes_de_laboratorio_activos() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(deworming.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(diagnosticImaging.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("laboratoryTest");
            verifyNoInteractions(prescription);
        }

        @Test
        @DisplayName("recetas activas — el ultimo puerto que se comprueba")
        void recetas_activas() {
            consultaExistePorEmpresa();
            when(vaccination.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(surgery.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(hospitalization.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(deworming.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(diagnosticImaging.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(false);
            when(prescription.existsActiveByConsultationId(ConsultationMother.CONSULTATION_ID))
                    .thenReturn(true);

            assertBloqueadoPor("prescription");
        }
    }
}
