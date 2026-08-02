package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailField;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailTable;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventDetailQueryPort;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.HospitalizationObservationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence.HospitalizationProgressNoteJpaRepository;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el detalle clínico rico de cada evento desde la entidad fuente de su
 * feature. Cruce cross-feature permitido: solo importa los
 * {@code XxxJpaRepository} de otras features (adaptadores de persistencia),
 * nunca su dominio ni su application.
 */
@Component
public class JpaClinicalEventDetailQueryPort implements ClinicalEventDetailQueryPort {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConsultationJpaRepository consultationRepo;
    private final SurgeryJpaRepository surgeryRepo;
    private final VaccinationJpaRepository vaccinationRepo;
    private final DewormingJpaRepository dewormingRepo;
    private final HospitalizationJpaRepository hospitalizationRepo;
    private final LaboratoryTestJpaRepository laboratoryTestRepo;
    private final DiagnosticImagingJpaRepository diagnosticImagingRepo;
    private final PrescriptionJpaRepository prescriptionRepo;
    private final SpaJpaRepository spaRepo;
    private final MedicamentPrescriptionJpaRepository medicamentPrescriptionRepo;
    private final HospitalizationMedicationJpaRepository hospitalizationMedicationRepo;
    private final HospitalizationProcedureJpaRepository hospitalizationProcedureRepo;
    private final HospitalizationProgressNoteJpaRepository hospitalizationProgressNoteRepo;
    private final HospitalizationObservationJpaRepository hospitalizationObservationRepo;

    public JpaClinicalEventDetailQueryPort(ConsultationJpaRepository consultationRepo,
            SurgeryJpaRepository surgeryRepo, VaccinationJpaRepository vaccinationRepo,
            DewormingJpaRepository dewormingRepo, HospitalizationJpaRepository hospitalizationRepo,
            LaboratoryTestJpaRepository laboratoryTestRepo,
            DiagnosticImagingJpaRepository diagnosticImagingRepo,
            PrescriptionJpaRepository prescriptionRepo, SpaJpaRepository spaRepo,
            MedicamentPrescriptionJpaRepository medicamentPrescriptionRepo,
            HospitalizationMedicationJpaRepository hospitalizationMedicationRepo,
            HospitalizationProcedureJpaRepository hospitalizationProcedureRepo,
            HospitalizationProgressNoteJpaRepository hospitalizationProgressNoteRepo,
            HospitalizationObservationJpaRepository hospitalizationObservationRepo) {
        this.consultationRepo = consultationRepo;
        this.surgeryRepo = surgeryRepo;
        this.vaccinationRepo = vaccinationRepo;
        this.dewormingRepo = dewormingRepo;
        this.hospitalizationRepo = hospitalizationRepo;
        this.laboratoryTestRepo = laboratoryTestRepo;
        this.diagnosticImagingRepo = diagnosticImagingRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.spaRepo = spaRepo;
        this.medicamentPrescriptionRepo = medicamentPrescriptionRepo;
        this.hospitalizationMedicationRepo = hospitalizationMedicationRepo;
        this.hospitalizationProcedureRepo = hospitalizationProcedureRepo;
        this.hospitalizationProgressNoteRepo = hospitalizationProgressNoteRepo;
        this.hospitalizationObservationRepo = hospitalizationObservationRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClinicalEventDetail> load(ClinicalEventType type, Long sourceId,
            Long companyId) {
        return switch (type) {
            case CONSULTATION -> consultation(sourceId, companyId);
            case SURGERY -> surgery(sourceId, companyId);
            case VACCINATION -> vaccination(sourceId, companyId);
            case DEWORMING -> deworming(sourceId, companyId);
            case HOSPITALIZATION -> hospitalization(sourceId, companyId);
            case LABORATORY_TEST -> laboratoryTest(sourceId, companyId);
            case DIAGNOSTIC_IMAGING -> diagnosticImaging(sourceId, companyId);
            case PRESCRIPTION -> prescription(sourceId, companyId);
            case SPA -> spa(sourceId, companyId);
        };
    }

    private Optional<ClinicalEventDetail> consultation(Long id, Long companyId) {
        return consultationRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Motivo / Anamnesis", e.getAnamnesis());
                    // Examen físico / constantes vitales (Fase 3) — la "O" de SOAP.
                    add(f, "Temperatura", temperature(e.getTemperature()));
                    add(f, "Frec. cardíaca", withUnit(e.getHeartRate(), "lpm"));
                    add(f, "Frec. respiratoria", withUnit(e.getRespiratoryRate(), "rpm"));
                    add(f, "Mucosas", e.getMucousMembranes());
                    add(f, "Llenado capilar", e.getCapillaryRefill());
                    add(f, "Hidratación", e.getHydration());
                    add(f, "Condición corporal", score(e.getBodyConditionScore(), 9));
                    add(f, "Escala de dolor", score(e.getPainScore(), 10));
                    add(f, "Actitud", e.getAttitude());
                    add(f, "Hallazgos al examen", e.getExamFindings());
                    add(f, "Diagnóstico", e.getDiagnosis());
                    add(f, "Pronóstico", e.getPrognosis());
                    add(f, "Próximo control", date(e.getNextControl()));
                    return ClinicalEventDetail.of(e.getConsultationType().getName(), f);
                });
    }

    private Optional<ClinicalEventDetail> surgery(Long id, Long companyId) {
        return surgeryRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Descripción", e.getDescription());
                    add(f, "Anestesia / medicamento", e.getMedicament());
                    add(f, "Complicaciones", e.getComplications());
                    add(f, "Observaciones", e.getObservations());
                    add(f, "Estado", e.getStatus());
                    return ClinicalEventDetail.of(e.getSurgeryType().getName(), f);
                });
    }

    private Optional<ClinicalEventDetail> vaccination(Long id, Long companyId) {
        return vaccinationRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Lote", e.getLot());
                    add(f, "Vía", e.getRoute());
                    add(f, "Sitio de aplicación", e.getApplicationSite());
                    add(f, "Próxima dosis", date(e.getNextVaccination()));
                    add(f, "Notas", e.getNotes());
                    return ClinicalEventDetail.of(e.getVaccinationType().getName(), f);
                });
    }

    private Optional<ClinicalEventDetail> deworming(Long id, Long companyId) {
        return dewormingRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Vía", dewormingType(enumName(e.getType())));
                    add(f, "Dosis", e.getDosage());
                    add(f, "Última desparasitación", date(e.getLastDeworming()));
                    add(f, "Próximo control", date(e.getNextControl()));
                    add(f, "Observaciones", e.getObservations());
                    return ClinicalEventDetail.of(e.getProduct(), f);
                });
    }

    private Optional<ClinicalEventDetail> hospitalization(Long id, Long companyId) {
        return hospitalizationRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Motivo", e.getReason());
                    add(f, "Ingreso", date(e.getStartDate()));
                    add(f, "Egreso", date(e.getEndDate()));
                    add(f, "Motivo de egreso", reasonLeaving(enumName(e.getReasonLeaving())));
                    add(f, "Observaciones", e.getObservations());
                    List<DetailTable> tables = new ArrayList<>();
                    addTable(tables, medicationTable(id));
                    addTable(tables, procedureTable(id));
                    addTable(tables, progressNoteTable(id));
                    addTable(tables, observationTable(id));
                    return new ClinicalEventDetail(hospitalizationType(enumName(e.getType())), f,
                            tables);
                });
    }

    private Optional<ClinicalEventDetail> laboratoryTest(Long id, Long companyId) {
        return laboratoryTestRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Indicación", e.getDiagnosis());
                    add(f, "Cantidad",
                            e.getQuantity() == null ? null : String.valueOf(e.getQuantity()));
                    add(f, "Prioridad", e.getPrioridad());
                    add(f, "Estado", e.getStatus());
                    add(f, "Procesado",
                            e.getProcessedDate() == null
                                    ? null
                                    : e.getProcessedDate().toLocalDate().format(DATE));
                    return ClinicalEventDetail.of(e.getTestType().getName(), f);
                });
    }

    private Optional<ClinicalEventDetail> diagnosticImaging(Long id, Long companyId) {
        return diagnosticImagingRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Tipo de estudio", e.getStudyType());
                    add(f, "Signos clínicos", e.getClinicalSigns());
                    add(f, "Hallazgos / Diagnóstico", e.getDiagnosis());
                    add(f, "Observaciones", e.getObservations());
                    add(f, "Estado", e.getStatus());
                    return ClinicalEventDetail.of(e.getDiagnosticImagingType().getName(), f);
                });
    }

    private Optional<ClinicalEventDetail> prescription(Long id, Long companyId) {
        return prescriptionRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Diagnóstico", e.getDiagnosis());
                    add(f, "Observaciones", e.getObservations());
                    List<DetailTable> tables = new ArrayList<>();
                    addTable(tables, medicamentTable(id));
                    return new ClinicalEventDetail("Prescripción", f, tables);
                });
    }

    private Optional<ClinicalEventDetail> spa(Long id, Long companyId) {
        return spaRepo.findById(id).filter(
                e -> sameCompany(e.getCompany() == null ? null : e.getCompany().getId(), companyId))
                .map(e -> {
                    List<DetailField> f = new ArrayList<>();
                    add(f, "Motivo", e.getReason());
                    add(f, "Detalles", e.getDetails());
                    add(f, "Observaciones", e.getObservations());
                    add(f, "Estado", e.getStatus());
                    return ClinicalEventDetail.of(e.getSpaType().getName(), f);
                });
    }

    // --- Tablas hijas (Fase 2)
    // -------------------------------------------------------------

    private DetailTable medicamentTable(Long prescriptionId) {
        List<List<String>> rows = medicamentPrescriptionRepo.findByPrescriptionId(prescriptionId)
                .stream()
                .map(m -> List.of(nz(m.getName()), nz(m.getPresentation()),
                        formatDouble(m.getQuantity()), nz(m.getPosology()), nz(m.getObservation())))
                .toList();
        return new DetailTable("Medicamentos",
                List.of("Medicamento", "Presentación", "Cantidad", "Posología", "Observación"),
                rows);
    }

    private DetailTable medicationTable(Long hospitalizationId) {
        List<List<String>> rows = hospitalizationMedicationRepo
                .findByHospitalizationId(hospitalizationId).stream()
                .sorted(Comparator.comparing(m -> m.getCreatedDate()))
                .map(m -> List.of(nz(m.getName()), nz(m.getDose()), nz(m.getFrequency()),
                        duration(m.getDurationQuantity(), m.getDurationMeasure()),
                        startAt(m.getStartDate(), m.getStartTime()), status(m.getSuspensionDate())))
                .toList();
        return new DetailTable("Medicaciones",
                List.of("Medicamento", "Dosis", "Frecuencia", "Duración", "Inicio", "Estado"),
                rows);
    }

    private DetailTable procedureTable(Long hospitalizationId) {
        List<List<String>> rows = hospitalizationProcedureRepo
                .findByHospitalizationId(hospitalizationId).stream()
                .sorted(Comparator.comparing(p -> p.getCreatedDate()))
                .map(p -> List.of(nz(p.getName()), nz(p.getDose()), nz(p.getFrequency()),
                        duration(p.getDurationQuantity(), p.getDurationMeasure()),
                        startAt(p.getStartDate(), p.getStartTime()), status(p.getSuspensionDate())))
                .toList();
        return new DetailTable("Procedimientos",
                List.of("Procedimiento", "Dosis", "Frecuencia", "Duración", "Inicio", "Estado"),
                rows);
    }

    private DetailTable progressNoteTable(Long hospitalizationId) {
        List<List<String>> rows = hospitalizationProgressNoteRepo
                .findByHospitalizationId(hospitalizationId).stream()
                .sorted(Comparator.comparing(n -> n.getCreatedDate()))
                .map(n -> List.of(n.getCreatedDate().toLocalDate().format(DATE),
                        author(n.getCreatedBy() == null ? null : n.getCreatedBy().getName()),
                        nz(n.getDescription())))
                .toList();
        return new DetailTable("Notas de evolución", List.of("Fecha", "Registró", "Evolución"),
                rows);
    }

    private DetailTable observationTable(Long hospitalizationId) {
        List<List<String>> rows = hospitalizationObservationRepo
                .findByHospitalizationId(hospitalizationId).stream()
                .sorted(Comparator.comparing(o -> o.getCreatedDate()))
                .map(o -> List.of(o.getCreatedDate().toLocalDate().format(DATE),
                        author(o.getCreatedBy() == null ? null : o.getCreatedBy().getName()),
                        nz(o.getDescription())))
                .toList();
        return new DetailTable("Observaciones", List.of("Fecha", "Registró", "Observación"), rows);
    }

    // --- Helpers
    // ---------------------------------------------------------------------------

    private static boolean sameCompany(Long entityCompanyId, Long companyId) {
        return entityCompanyId != null && entityCompanyId.equals(companyId);
    }

    private static void add(List<DetailField> fields, String label, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(new DetailField(label, value.strip()));
        }
    }

    private static void addTable(List<DetailTable> tables, DetailTable table) {
        if (table != null && !table.rows().isEmpty()) {
            tables.add(table);
        }
    }

    private static String date(LocalDate date) {
        return date == null ? null : date.format(DATE);
    }

    private static String temperature(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString() + " °C";
    }

    private static String withUnit(Integer value, String unit) {
        return value == null ? null : value + " " + unit;
    }

    private static String score(Integer value, int max) {
        return value == null ? null : value + "/" + max;
    }

    private static String nz(String value) {
        return value == null ? "" : value.strip();
    }

    private static String author(String name) {
        return name == null || name.isBlank() ? "—" : name.strip();
    }

    private static String startAt(LocalDate startDate, LocalTime startTime) {
        if (startDate == null)
            return "";
        return startTime == null
                ? startDate.format(DATE)
                : startDate.format(DATE) + " " + startTime;
    }

    private static String status(LocalDateTime suspensionDate) {
        return suspensionDate == null
                ? "Activo"
                : "Suspendido (" + suspensionDate.toLocalDate().format(DATE) + ")";
    }

    private static String duration(Integer quantity, String measure) {
        if (quantity == null && (measure == null || measure.isBlank()))
            return "";
        if (quantity == null)
            return measure.strip();
        return measure == null || measure.isBlank()
                ? String.valueOf(quantity)
                : quantity + " " + measure.strip();
    }

    private static String formatDouble(Double value) {
        if (value == null)
            return "";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /**
     * Nombre del enum sin importar su tipo de dominio (respeta el vertical
     * slicing).
     */
    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String dewormingType(String type) {
        if (type == null)
            return null;
        return switch (type) {
            case "INTERNAL" -> "Interna";
            case "EXTERNAL" -> "Externa";
            case "MIX" -> "Mixta";
            case "OTHER" -> "Otra";
            default -> type;
        };
    }

    private static String hospitalizationType(String type) {
        if (type == null)
            return "Hospitalización";
        return switch (type) {
            case "OUTPATIENT" -> "Ambulatorio";
            case "HOSPITALIZATION" -> "Hospitalización";
            default -> type;
        };
    }

    private static String reasonLeaving(String reason) {
        if (reason == null)
            return null;
        return switch (reason) {
            case "MEDICAL_DISCHARGE" -> "Alta médica";
            case "HOME_TREATMENT" -> "Tratamiento en casa";
            case "TRANSFER" -> "Traslado";
            case "TUTOR_WISH" -> "Voluntad del tutor";
            case "ADMIN" -> "Administrativo";
            case "DEATH" -> "Fallecimiento";
            case "EUTHANASIA" -> "Eutanasia";
            default -> reason;
        };
    }
}
