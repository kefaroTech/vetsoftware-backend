package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailField;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailTable;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaEntity;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaEntity;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.HospitalizationObservationJpaEntity;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.HospitalizationObservationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence.HospitalizationProgressNoteJpaEntity;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence.HospitalizationProgressNoteJpaRepository;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaEntity;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaEntity;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaEntity;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaEntity;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaEntity;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaEntity;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Las entidades JPA de otras features se mockean (nunca {@code new}): sus
 * constructores son {@code protected} en el paquete de su propia feature, no
 * accesible desde aquí. Mismo patrón que
 * {@code appointment/infrastructure/persistence/JpaBranchQueryPortTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaClinicalEventDetailQueryPort — detalle rico por tipo de evento")
class JpaClinicalEventDetailQueryPortTest {

    private static final Long COMPANY = 9L;
    private static final Long OTRA_COMPANY = 77L;
    private static final Long SOURCE_ID = 500L;

    @Mock
    private ConsultationJpaRepository consultationRepo;
    @Mock
    private SurgeryJpaRepository surgeryRepo;
    @Mock
    private VaccinationJpaRepository vaccinationRepo;
    @Mock
    private DewormingJpaRepository dewormingRepo;
    @Mock
    private HospitalizationJpaRepository hospitalizationRepo;
    @Mock
    private LaboratoryTestJpaRepository laboratoryTestRepo;
    @Mock
    private DiagnosticImagingJpaRepository diagnosticImagingRepo;
    @Mock
    private PrescriptionJpaRepository prescriptionRepo;
    @Mock
    private SpaJpaRepository spaRepo;
    @Mock
    private MedicamentPrescriptionJpaRepository medicamentPrescriptionRepo;
    @Mock
    private HospitalizationMedicationJpaRepository hospitalizationMedicationRepo;
    @Mock
    private HospitalizationProcedureJpaRepository hospitalizationProcedureRepo;
    @Mock
    private HospitalizationProgressNoteJpaRepository hospitalizationProgressNoteRepo;
    @Mock
    private HospitalizationObservationJpaRepository hospitalizationObservationRepo;

    private JpaClinicalEventDetailQueryPort port;

    @org.junit.jupiter.api.BeforeEach
    void construirPuerto() {
        port = new JpaClinicalEventDetailQueryPort(consultationRepo, surgeryRepo, vaccinationRepo,
                dewormingRepo, hospitalizationRepo, laboratoryTestRepo, diagnosticImagingRepo,
                prescriptionRepo, spaRepo, medicamentPrescriptionRepo,
                hospitalizationMedicationRepo, hospitalizationProcedureRepo,
                hospitalizationProgressNoteRepo, hospitalizationObservationRepo);
    }

    private static CompanyJpaEntity companyDe(Long id) {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        when(company.getId()).thenReturn(id);
        return company;
    }

    private void otrosRepositoriosSinTocar(Object usado) {
        List<Object> todos = List.of(consultationRepo, surgeryRepo, vaccinationRepo, dewormingRepo,
                hospitalizationRepo, laboratoryTestRepo, diagnosticImagingRepo, prescriptionRepo,
                spaRepo);
        Object[] otros = todos.stream().filter(r -> r != usado).toArray();
        verifyNoInteractions(otros);
    }

    @Nested
    @DisplayName("CONSULTATION")
    class Consultation {

        private ConsultationJpaEntity entidadCompleta() {
            ConsultationJpaEntity e = mock(ConsultationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getAnamnesis()).thenReturn("Vómito y decaimiento");
            when(e.getTemperature()).thenReturn(new BigDecimal("38.50"));
            when(e.getHeartRate()).thenReturn(80);
            when(e.getRespiratoryRate()).thenReturn(20);
            when(e.getMucousMembranes()).thenReturn("Rosadas");
            when(e.getCapillaryRefill()).thenReturn("<2s");
            when(e.getHydration()).thenReturn("Normal");
            when(e.getBodyConditionScore()).thenReturn(5);
            when(e.getPainScore()).thenReturn(2);
            when(e.getAttitude()).thenReturn("Alerta");
            when(e.getExamFindings()).thenReturn("Sin hallazgos relevantes");
            when(e.getDiagnosis()).thenReturn("Gastritis");
            when(e.getPrognosis()).thenReturn("Bueno");
            when(e.getNextControl()).thenReturn(LocalDate.of(2026, 9, 1));
            ConsultationTypeJpaEntity tipo = mock(ConsultationTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Consulta general");
            when(e.getConsultationType()).thenReturn(tipo);
            return e;
        }

        @Test
        @DisplayName("con todos los campos llenos arma los 14 campos SOAP")
        void con_todos_los_campos_llenos() {
            ConsultationJpaEntity entidad = entidadCompleta();
            when(consultationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(entidad));

            Optional<ClinicalEventDetail> result = port.load(ClinicalEventType.CONSULTATION,
                    SOURCE_ID, COMPANY);

            assertThat(result).isPresent();
            ClinicalEventDetail detail = result.get();
            assertThat(detail.title()).isEqualTo("Consulta general");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Motivo / Anamnesis", "Vómito y decaimiento"),
                    new DetailField("Temperatura", "38.5 °C"),
                    new DetailField("Frec. cardíaca", "80 lpm"),
                    new DetailField("Frec. respiratoria", "20 rpm"),
                    new DetailField("Mucosas", "Rosadas"),
                    new DetailField("Llenado capilar", "<2s"),
                    new DetailField("Hidratación", "Normal"),
                    new DetailField("Condición corporal", "5/9"),
                    new DetailField("Escala de dolor", "2/10"),
                    new DetailField("Actitud", "Alerta"),
                    new DetailField("Hallazgos al examen", "Sin hallazgos relevantes"),
                    new DetailField("Diagnóstico", "Gastritis"),
                    new DetailField("Pronóstico", "Bueno"),
                    new DetailField("Próximo control", "01/09/2026"));
            assertThat(detail.tables()).isEmpty();
        }

        @Test
        @DisplayName("los campos opcionales nulos o en blanco no aparecen en el detalle")
        void campos_opcionales_nulos_o_en_blanco_se_omiten() {
            ConsultationJpaEntity e = mock(ConsultationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getCapillaryRefill()).thenReturn("   ");
            // Un mock sin stub en un getter de tipo Integer devuelve 0, no null (a
            // diferencia de String/objeto): sin este stub explícito, "0 lpm"/"0/9" se
            // colarían como si el dato existiera.
            when(e.getHeartRate()).thenReturn(null);
            when(e.getRespiratoryRate()).thenReturn(null);
            when(e.getBodyConditionScore()).thenReturn(null);
            when(e.getPainScore()).thenReturn(null);
            ConsultationTypeJpaEntity tipo = mock(ConsultationTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Consulta general");
            when(e.getConsultationType()).thenReturn(tipo);
            when(consultationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            Optional<ClinicalEventDetail> result = port.load(ClinicalEventType.CONSULTATION,
                    SOURCE_ID, COMPANY);

            assertThat(result.get().fields()).isEmpty();
        }

        @Test
        @DisplayName("de otra empresa no se expone: Optional vacío")
        void de_otra_empresa_no_se_expone() {
            ConsultationJpaEntity e = mock(ConsultationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(OTRA_COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(consultationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            assertThat(port.load(ClinicalEventType.CONSULTATION, SOURCE_ID, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("sin empresa asociada tampoco se expone")
        void sin_empresa_asociada_no_se_expone() {
            ConsultationJpaEntity e = mock(ConsultationJpaEntity.class);
            when(e.getCompany()).thenReturn(null);
            when(consultationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            assertThat(port.load(ClinicalEventType.CONSULTATION, SOURCE_ID, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("inexistente devuelve vacío y solo toca el repositorio de consultas")
        void inexistente_devuelve_vacio() {
            when(consultationRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.CONSULTATION, SOURCE_ID, COMPANY)).isEmpty();
            verify(consultationRepo).findById(SOURCE_ID);
            otrosRepositoriosSinTocar(consultationRepo);
        }
    }

    @Nested
    @DisplayName("SURGERY")
    class Surgery {

        @Test
        @DisplayName("arma los 5 campos de la cirugía")
        void arma_los_campos() {
            SurgeryJpaEntity e = mock(SurgeryJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getDescription()).thenReturn("Esterilización");
            when(e.getMedicament()).thenReturn("Isoflurano");
            when(e.getComplications()).thenReturn("Ninguna");
            when(e.getObservations()).thenReturn("Post-op estable");
            when(e.getStatus()).thenReturn("Completada");
            SurgeryTypeJpaEntity tipo = mock(SurgeryTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Cirugía electiva");
            when(e.getSurgeryType()).thenReturn(tipo);
            when(surgeryRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port.load(ClinicalEventType.SURGERY, SOURCE_ID, COMPANY)
                    .orElseThrow();

            assertThat(detail.title()).isEqualTo("Cirugía electiva");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Descripción", "Esterilización"),
                    new DetailField("Anestesia / medicamento", "Isoflurano"),
                    new DetailField("Complicaciones", "Ninguna"),
                    new DetailField("Observaciones", "Post-op estable"),
                    new DetailField("Estado", "Completada"));
        }

        @Test
        @DisplayName("de otra empresa no se expone")
        void de_otra_empresa_no_se_expone() {
            SurgeryJpaEntity e = mock(SurgeryJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(OTRA_COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(surgeryRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            assertThat(port.load(ClinicalEventType.SURGERY, SOURCE_ID, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(surgeryRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.SURGERY, SOURCE_ID, COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("VACCINATION")
    class Vaccination {

        @Test
        @DisplayName("arma los 5 campos de la vacunación")
        void arma_los_campos() {
            VaccinationJpaEntity e = mock(VaccinationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getLot()).thenReturn("L-2026-08");
            when(e.getRoute()).thenReturn("Subcutánea");
            when(e.getApplicationSite()).thenReturn("Interescapular");
            when(e.getNextVaccination()).thenReturn(LocalDate.of(2027, 8, 1));
            when(e.getNotes()).thenReturn("Sin reacción");
            VaccinationTypeJpaEntity tipo = mock(VaccinationTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Antirrábica");
            when(e.getVaccinationType()).thenReturn(tipo);
            when(vaccinationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.VACCINATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Antirrábica");
            assertThat(detail.fields()).containsExactly(new DetailField("Lote", "L-2026-08"),
                    new DetailField("Vía", "Subcutánea"),
                    new DetailField("Sitio de aplicación", "Interescapular"),
                    new DetailField("Próxima dosis", "01/08/2027"),
                    new DetailField("Notas", "Sin reacción"));
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(vaccinationRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.VACCINATION, SOURCE_ID, COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("DEWORMING")
    class Deworming {

        private DewormingJpaEntity entidad(DewormingType tipo) {
            DewormingJpaEntity e = mock(DewormingJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getType()).thenReturn(tipo);
            when(e.getDosage()).thenReturn("5 mg/kg");
            when(e.getLastDeworming()).thenReturn(LocalDate.of(2026, 5, 1));
            when(e.getNextControl()).thenReturn(LocalDate.of(2026, 11, 1));
            when(e.getObservations()).thenReturn("Tolerado bien");
            when(e.getProduct()).thenReturn("Drontal Plus");
            return e;
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"INTERNAL,Interna", "EXTERNAL,Externa", "MIX,Mixta", "OTHER,Otra"})
        @DisplayName("la vía se traduce según el tipo")
        void la_via_se_traduce_segun_el_tipo(String tipoStr, String esperada) {
            DewormingType tipo = DewormingType.valueOf(tipoStr);
            DewormingJpaEntity entidadDeworming = entidad(tipo);
            when(dewormingRepo.findById(SOURCE_ID)).thenReturn(Optional.of(entidadDeworming));

            ClinicalEventDetail detail = port.load(ClinicalEventType.DEWORMING, SOURCE_ID, COMPANY)
                    .orElseThrow();

            assertThat(detail.title()).isEqualTo("Drontal Plus");
            assertThat(detail.fields()).contains(new DetailField("Vía", esperada));
        }

        @Test
        @DisplayName("sin tipo la vía no aparece en el detalle")
        void sin_tipo_la_via_no_aparece() {
            DewormingJpaEntity e = entidad(null);
            when(dewormingRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port.load(ClinicalEventType.DEWORMING, SOURCE_ID, COMPANY)
                    .orElseThrow();

            assertThat(detail.fields()).extracting(DetailField::label).doesNotContain("Vía");
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(dewormingRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.DEWORMING, SOURCE_ID, COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("LABORATORY_TEST")
    class LaboratoryTest {

        @Test
        @DisplayName("arma los 5 campos, incluida la fecha de procesado")
        void arma_los_campos() {
            LaboratoryTestJpaEntity e = mock(LaboratoryTestJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getDiagnosis()).thenReturn("Sospecha de anemia");
            when(e.getQuantity()).thenReturn(3);
            when(e.getPrioridad()).thenReturn("Urgente");
            when(e.getStatus()).thenReturn("Procesado");
            when(e.getProcessedDate()).thenReturn(LocalDateTime.of(2026, 8, 10, 14, 30));
            LaboratoryTestTypeJpaEntity tipo = mock(LaboratoryTestTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Hemograma completo");
            when(e.getTestType()).thenReturn(tipo);
            when(laboratoryTestRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.LABORATORY_TEST, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Hemograma completo");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Indicación", "Sospecha de anemia"),
                    new DetailField("Cantidad", "3"), new DetailField("Prioridad", "Urgente"),
                    new DetailField("Estado", "Procesado"),
                    new DetailField("Procesado", "10/08/2026"));
        }

        @Test
        @DisplayName("sin cantidad ni fecha de procesado esos campos no aparecen")
        void sin_cantidad_ni_procesado() {
            LaboratoryTestJpaEntity e = mock(LaboratoryTestJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            // Igual que con Integer en Consultation: un mock sin stub en getQuantity()
            // devuelve 0, no null, así que sin este stub "Cantidad" aparecería con "0".
            when(e.getQuantity()).thenReturn(null);
            LaboratoryTestTypeJpaEntity tipo = mock(LaboratoryTestTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Hemograma completo");
            when(e.getTestType()).thenReturn(tipo);
            when(laboratoryTestRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.LABORATORY_TEST, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.fields()).extracting(DetailField::label).doesNotContain("Cantidad",
                    "Procesado");
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(laboratoryTestRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.LABORATORY_TEST, SOURCE_ID, COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("DIAGNOSTIC_IMAGING")
    class DiagnosticImaging {

        @Test
        @DisplayName("arma los 5 campos del estudio")
        void arma_los_campos() {
            DiagnosticImagingJpaEntity e = mock(DiagnosticImagingJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getStudyType()).thenReturn("Radiografía torácica");
            when(e.getClinicalSigns()).thenReturn("Tos persistente");
            when(e.getDiagnosis()).thenReturn("Sin hallazgos");
            when(e.getObservations()).thenReturn("Control en 1 mes");
            when(e.getStatus()).thenReturn("Completado");
            DiagnosticImagingTypeJpaEntity tipo = mock(DiagnosticImagingTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Rayos X");
            when(e.getDiagnosticImagingType()).thenReturn(tipo);
            when(diagnosticImagingRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.DIAGNOSTIC_IMAGING, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Rayos X");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Tipo de estudio", "Radiografía torácica"),
                    new DetailField("Signos clínicos", "Tos persistente"),
                    new DetailField("Hallazgos / Diagnóstico", "Sin hallazgos"),
                    new DetailField("Observaciones", "Control en 1 mes"),
                    new DetailField("Estado", "Completado"));
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(diagnosticImagingRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.DIAGNOSTIC_IMAGING, SOURCE_ID, COMPANY))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("PRESCRIPTION")
    class Prescription {

        private PrescriptionJpaEntity entidad() {
            PrescriptionJpaEntity e = mock(PrescriptionJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getDiagnosis()).thenReturn("Otitis externa");
            when(e.getObservations()).thenReturn("Revisar en 10 días");
            return e;
        }

        @Test
        @DisplayName("sin medicamentos la tabla no aparece")
        void sin_medicamentos_la_tabla_no_aparece() {
            PrescriptionJpaEntity entidadPrescripcion = entidad();
            when(prescriptionRepo.findById(SOURCE_ID)).thenReturn(Optional.of(entidadPrescripcion));
            when(medicamentPrescriptionRepo.findByPrescriptionId(SOURCE_ID)).thenReturn(List.of());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.PRESCRIPTION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Prescripción");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Diagnóstico", "Otitis externa"),
                    new DetailField("Observaciones", "Revisar en 10 días"));
            assertThat(detail.tables()).isEmpty();
        }

        @Test
        @DisplayName("con un medicamento la tabla aparece con la cantidad sin decimales de sobra")
        void con_un_medicamento_la_tabla_aparece() {
            PrescriptionJpaEntity entidadPrescripcion = entidad();
            when(prescriptionRepo.findById(SOURCE_ID)).thenReturn(Optional.of(entidadPrescripcion));
            MedicamentPrescriptionJpaEntity m = mock(MedicamentPrescriptionJpaEntity.class);
            when(m.getName()).thenReturn("Amoxicilina");
            when(m.getPresentation()).thenReturn("Tabletas 250mg");
            when(m.getQuantity()).thenReturn(12.50);
            when(m.getPosology()).thenReturn("1 tableta c/12h");
            when(m.getObservation()).thenReturn("Con alimento");
            when(medicamentPrescriptionRepo.findByPrescriptionId(SOURCE_ID)).thenReturn(List.of(m));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.PRESCRIPTION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).containsExactly(new DetailTable("Medicamentos",
                    List.of("Medicamento", "Presentación", "Cantidad", "Posología", "Observación"),
                    List.of(List.of("Amoxicilina", "Tabletas 250mg", "12.5", "1 tableta c/12h",
                            "Con alimento"))));
        }

        @Test
        @DisplayName("de otra empresa no se expone")
        void de_otra_empresa_no_se_expone() {
            PrescriptionJpaEntity e = mock(PrescriptionJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(OTRA_COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(prescriptionRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            assertThat(port.load(ClinicalEventType.PRESCRIPTION, SOURCE_ID, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("un medicamento sin cantidad ni campos de texto se normaliza a cadenas vacías")
        void medicamento_con_campos_nulos_se_normaliza() {
            PrescriptionJpaEntity entidadPrescripcion = entidad();
            when(prescriptionRepo.findById(SOURCE_ID)).thenReturn(Optional.of(entidadPrescripcion));
            MedicamentPrescriptionJpaEntity m = mock(MedicamentPrescriptionJpaEntity.class);
            when(m.getName()).thenReturn(null);
            when(m.getPresentation()).thenReturn(null);
            when(m.getQuantity()).thenReturn(null);
            when(m.getPosology()).thenReturn(null);
            when(m.getObservation()).thenReturn(null);
            when(medicamentPrescriptionRepo.findByPrescriptionId(SOURCE_ID)).thenReturn(List.of(m));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.PRESCRIPTION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).containsExactly(new DetailTable("Medicamentos",
                    List.of("Medicamento", "Presentación", "Cantidad", "Posología", "Observación"),
                    List.of(List.of("", "", "", "", ""))));
        }

        @Test
        @DisplayName("inexistente devuelve vacío")
        void inexistente_devuelve_vacio() {
            when(prescriptionRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.PRESCRIPTION, SOURCE_ID, COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("SPA")
    class Spa {

        @Test
        @DisplayName("arma los 4 campos del servicio")
        void arma_los_campos() {
            SpaJpaEntity e = mock(SpaJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getReason()).thenReturn("Baño y corte");
            when(e.getDetails()).thenReturn("Pelo enredado");
            when(e.getObservations()).thenReturn("Sin novedad");
            when(e.getStatus()).thenReturn("Completado");
            SpaTypeJpaEntity tipo = mock(SpaTypeJpaEntity.class);
            when(tipo.getName()).thenReturn("Baño medicado");
            when(e.getSpaType()).thenReturn(tipo);
            when(spaRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            ClinicalEventDetail detail = port.load(ClinicalEventType.SPA, SOURCE_ID, COMPANY)
                    .orElseThrow();

            assertThat(detail.title()).isEqualTo("Baño medicado");
            assertThat(detail.fields()).containsExactly(new DetailField("Motivo", "Baño y corte"),
                    new DetailField("Detalles", "Pelo enredado"),
                    new DetailField("Observaciones", "Sin novedad"),
                    new DetailField("Estado", "Completado"));
        }

        @Test
        @DisplayName("inexistente devuelve vacío y solo toca el repositorio de spa")
        void inexistente_devuelve_vacio() {
            when(spaRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.SPA, SOURCE_ID, COMPANY)).isEmpty();
            verify(spaRepo).findById(SOURCE_ID);
            otrosRepositoriosSinTocar(spaRepo);
        }
    }

    @Nested
    @DisplayName("HOSPITALIZATION")
    class Hospitalization {

        private HospitalizationJpaEntity entidadBase() {
            HospitalizationJpaEntity e = mock(HospitalizationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(e.getReason()).thenReturn("Deshidratación severa");
            when(e.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
            when(e.getEndDate()).thenReturn(LocalDate.of(2026, 8, 5));
            when(e.getObservations()).thenReturn("Evolución favorable");
            return e;
        }

        private void sinFilasEnLasCuatroTablas() {
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
        }

        @Test
        @DisplayName("con las 4 tablas vacías el detalle no trae ninguna tabla")
        void con_las_cuatro_tablas_vacias_no_hay_tablas() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(ReasonLeaving.MEDICAL_DISCHARGE);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            sinFilasEnLasCuatroTablas();

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Hospitalización");
            assertThat(detail.fields()).containsExactly(
                    new DetailField("Motivo", "Deshidratación severa"),
                    new DetailField("Ingreso", "01/08/2026"),
                    new DetailField("Egreso", "05/08/2026"),
                    new DetailField("Motivo de egreso", "Alta médica"),
                    new DetailField("Observaciones", "Evolución favorable"));
            assertThat(detail.tables()).isEmpty();
        }

        @Test
        @DisplayName("con una medicación activa la tabla de medicaciones aparece")
        void con_una_medicacion_activa() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            HospitalizationMedicationJpaEntity m = mock(HospitalizationMedicationJpaEntity.class);
            when(m.getName()).thenReturn("Amoxicilina");
            when(m.getDose()).thenReturn("250mg");
            when(m.getFrequency()).thenReturn("c/12h");
            when(m.getDurationQuantity()).thenReturn(5);
            when(m.getDurationMeasure()).thenReturn("días");
            when(m.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
            when(m.getStartTime()).thenReturn(LocalTime.of(8, 0));
            when(m.getSuspensionDate()).thenReturn(null);
            // lenient: con una sola fila, Comparator.comparing nunca invoca el
            // extractor (no hay nada que comparar), así que el stub queda sin usar.
            org.mockito.Mockito.lenient().when(m.getCreatedDate())
                    .thenReturn(LocalDateTime.of(2026, 8, 1, 8, 0));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.fields()).extracting(DetailField::label)
                    .doesNotContain("Motivo de egreso");
            assertThat(detail.tables()).containsExactly(new DetailTable("Medicaciones",
                    List.of("Medicamento", "Dosis", "Frecuencia", "Duración", "Inicio", "Estado"),
                    List.of(List.of("Amoxicilina", "250mg", "c/12h", "5 días", "01/08/2026 08:00",
                            "Activo"))));
        }

        @Test
        @DisplayName("con una medicacion sin duracion ni medida la columna Duración queda vacía")
        void medicacion_sin_duracion_ni_medida() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            HospitalizationMedicationJpaEntity m = mock(HospitalizationMedicationJpaEntity.class);
            when(m.getName()).thenReturn("Amoxicilina");
            when(m.getDose()).thenReturn("250mg");
            when(m.getFrequency()).thenReturn("c/12h");
            when(m.getDurationQuantity()).thenReturn(null);
            when(m.getDurationMeasure()).thenReturn(null);
            when(m.getStartDate()).thenReturn(null);
            when(m.getSuspensionDate()).thenReturn(LocalDateTime.of(2026, 8, 3, 10, 0));
            org.mockito.Mockito.lenient().when(m.getCreatedDate())
                    .thenReturn(LocalDateTime.of(2026, 8, 1, 8, 0));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).singleElement().satisfies(t -> {
                List<String> fila = t.rows().get(0);
                assertThat(fila.get(3)).isEqualTo(""); // Duración: sin cantidad ni medida
                assertThat(fila.get(4)).isEqualTo(""); // Inicio: sin fecha
                assertThat(fila.get(5)).isEqualTo("Suspendido (03/08/2026)"); // Estado: suspendida
            });
        }

        @Test
        @DisplayName("con solo la medida la Duración muestra la medida sin cantidad")
        void medicacion_solo_con_medida() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            HospitalizationMedicationJpaEntity m = mock(HospitalizationMedicationJpaEntity.class);
            when(m.getName()).thenReturn("Amoxicilina");
            when(m.getDose()).thenReturn("250mg");
            when(m.getFrequency()).thenReturn("c/12h");
            when(m.getDurationQuantity()).thenReturn(null);
            when(m.getDurationMeasure()).thenReturn("hasta alta");
            when(m.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
            when(m.getStartTime()).thenReturn(null);
            when(m.getSuspensionDate()).thenReturn(null);
            org.mockito.Mockito.lenient().when(m.getCreatedDate())
                    .thenReturn(LocalDateTime.of(2026, 8, 1, 8, 0));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).singleElement().satisfies(t -> {
                List<String> fila = t.rows().get(0);
                assertThat(fila.get(3)).isEqualTo("hasta alta"); // Duración: solo medida
                assertThat(fila.get(4)).isEqualTo("01/08/2026"); // Inicio: sin hora
                assertThat(fila.get(5)).isEqualTo("Activo");
            });
        }

        @Test
        @DisplayName("con solo la cantidad la Duración muestra el número sin unidad")
        void medicacion_solo_con_cantidad() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            HospitalizationMedicationJpaEntity m = mock(HospitalizationMedicationJpaEntity.class);
            when(m.getName()).thenReturn("Amoxicilina");
            when(m.getDose()).thenReturn("250mg");
            when(m.getFrequency()).thenReturn("c/12h");
            when(m.getDurationQuantity()).thenReturn(3);
            when(m.getDurationMeasure()).thenReturn("   ");
            when(m.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
            when(m.getStartTime()).thenReturn(LocalTime.of(8, 0));
            when(m.getSuspensionDate()).thenReturn(null);
            org.mockito.Mockito.lenient().when(m.getCreatedDate())
                    .thenReturn(LocalDateTime.of(2026, 8, 1, 8, 0));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).singleElement()
                    .satisfies(t -> assertThat(t.rows().get(0).get(3)).isEqualTo("3"));
        }

        @Test
        @DisplayName("con una nota de evolución la tabla aparece con quien la registró")
        void con_una_nota_de_evolucion() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());

            HospitalizationProgressNoteJpaEntity n = mock(
                    HospitalizationProgressNoteJpaEntity.class);
            when(n.getCreatedDate()).thenReturn(LocalDateTime.of(2026, 8, 2, 9, 30));
            EmployeeJpaEntity registro = mock(EmployeeJpaEntity.class);
            when(registro.getName()).thenReturn("Dra. Ana Ruiz");
            when(n.getCreatedBy()).thenReturn(registro);
            when(n.getDescription()).thenReturn("Paciente estable, buena diuresis");
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(n)));
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).containsExactly(new DetailTable("Notas de evolución",
                    List.of("Fecha", "Registró", "Evolución"), List.of(List.of("02/08/2026",
                            "Dra. Ana Ruiz", "Paciente estable, buena diuresis"))));
        }

        @Test
        @DisplayName("una observación sin empleado registrado usa el guión como autor")
        void observacion_sin_empleado_usa_guion() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            when(hospitalizationMedicationRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProcedureRepo.findByHospitalizationIdAndHospitalization_Company_Id(
                    SOURCE_ID, COMPANY, Pageable.ofSize(200))).thenReturn(Page.empty());
            when(hospitalizationProgressNoteRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(Page.empty());

            HospitalizationObservationJpaEntity o = mock(HospitalizationObservationJpaEntity.class);
            when(o.getCreatedDate()).thenReturn(LocalDateTime.of(2026, 8, 3, 7, 0));
            when(o.getCreatedBy()).thenReturn(null);
            when(o.getDescription()).thenReturn("Sin novedad en la noche");
            when(hospitalizationObservationRepo
                    .findByHospitalizationIdAndHospitalization_Company_Id(SOURCE_ID, COMPANY,
                            Pageable.ofSize(200)))
                    .thenReturn(new PageImpl<>(List.of(o)));

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.tables()).containsExactly(
                    new DetailTable("Observaciones", List.of("Fecha", "Registró", "Observación"),
                            List.of(List.of("03/08/2026", "—", "Sin novedad en la noche"))));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"OUTPATIENT,Ambulatorio", "HOSPITALIZATION,Hospitalización"})
        @DisplayName("el título se traduce según el tipo de estancia")
        void el_titulo_se_traduce_segun_el_tipo(String tipoStr, String esperado) {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.valueOf(tipoStr));
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            sinFilasEnLasCuatroTablas();

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("sin tipo el título cae al default 'Hospitalización'")
        void sin_tipo_cae_al_default() {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(null);
            when(e.getReasonLeaving()).thenReturn(null);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            sinFilasEnLasCuatroTablas();

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.title()).isEqualTo("Hospitalización");
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"MEDICAL_DISCHARGE,Alta médica", "HOME_TREATMENT,Tratamiento en casa",
                "TRANSFER,Traslado", "TUTOR_WISH,Voluntad del tutor", "ADMIN,Administrativo",
                "DEATH,Fallecimiento", "EUTHANASIA,Eutanasia"})
        @DisplayName("el motivo de egreso se traduce por cada valor del enum")
        void el_motivo_de_egreso_se_traduce(String razonStr, String esperado) {
            HospitalizationJpaEntity e = entidadBase();
            when(e.getType()).thenReturn(HospitalizationType.HOSPITALIZATION);
            when(e.getReasonLeaving()).thenReturn(ReasonLeaving.valueOf(razonStr));
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));
            sinFilasEnLasCuatroTablas();

            ClinicalEventDetail detail = port
                    .load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY).orElseThrow();

            assertThat(detail.fields()).contains(new DetailField("Motivo de egreso", esperado));
        }

        @Test
        @DisplayName("de otra empresa no se expone")
        void de_otra_empresa_no_se_expone() {
            HospitalizationJpaEntity e = mock(HospitalizationJpaEntity.class);
            CompanyJpaEntity companyDeE = companyDe(OTRA_COMPANY);
            when(e.getCompany()).thenReturn(companyDeE);
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.of(e));

            assertThat(port.load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("inexistente devuelve vacío y solo toca el repositorio de hospitalización")
        void inexistente_devuelve_vacio() {
            when(hospitalizationRepo.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThat(port.load(ClinicalEventType.HOSPITALIZATION, SOURCE_ID, COMPANY)).isEmpty();
            verify(hospitalizationRepo).findById(SOURCE_ID);
            otrosRepositoriosSinTocar(hospitalizationRepo);
        }
    }

    @Nested
    @DisplayName("el switch de load() despacha al tipo correcto")
    class Despacho {

        @ParameterizedTest
        @EnumSource(ClinicalEventType.class)
        @DisplayName("cada tipo devuelve vacío cuando su repositorio no encuentra el id")
        void cada_tipo_devuelve_vacio_si_no_existe(ClinicalEventType tipo) {
            // lenient: cada valor del enum solo dispara UNO de los 9 repos; los otros 8
            // stubs quedan sin usar en esa vuelta del @ParameterizedTest.
            org.mockito.Mockito.lenient().when(consultationRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(surgeryRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(vaccinationRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(dewormingRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(hospitalizationRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(laboratoryTestRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(diagnosticImagingRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(prescriptionRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.lenient().when(spaRepo.findById(SOURCE_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.load(tipo, SOURCE_ID, COMPANY)).isEmpty();
        }
    }
}
