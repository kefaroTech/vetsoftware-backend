package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.port.in.DeleteConsultationUseCase;
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
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.delete")
@Service
public class DeleteConsultationService implements DeleteConsultationUseCase {
    private final ConsultationRepository repository;
    private final VaccinationChildrenQueryPort vaccinationChildrenQueryPort;
    private final SurgeryChildrenQueryPort surgeryChildrenQueryPort;
    private final HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort;
    private final DewormingChildrenQueryPort dewormingChildrenQueryPort;
    private final DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;
    private final LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;
    private final PrescriptionChildrenQueryPort prescriptionChildrenQueryPort;

    public DeleteConsultationService(
            ConsultationRepository repository,
            VaccinationChildrenQueryPort vaccinationChildrenQueryPort,
            SurgeryChildrenQueryPort surgeryChildrenQueryPort,
            HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort,
            DewormingChildrenQueryPort dewormingChildrenQueryPort,
            DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort,
            LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort,
            PrescriptionChildrenQueryPort prescriptionChildrenQueryPort) {
        this.repository = repository;
        this.vaccinationChildrenQueryPort = vaccinationChildrenQueryPort;
        this.surgeryChildrenQueryPort = surgeryChildrenQueryPort;
        this.hospitalizationChildrenQueryPort = hospitalizationChildrenQueryPort;
        this.dewormingChildrenQueryPort = dewormingChildrenQueryPort;
        this.diagnosticImagingChildrenQueryPort = diagnosticImagingChildrenQueryPort;
        this.laboratoryTestChildrenQueryPort = laboratoryTestChildrenQueryPort;
        this.prescriptionChildrenQueryPort = prescriptionChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConsultationNotFoundException(id));
        if (vaccinationChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "vaccination");
        }
        if (surgeryChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "surgery");
        }
        if (hospitalizationChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "hospitalization");
        }
        if (dewormingChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "deworming");
        }
        if (diagnosticImagingChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "diagnosticImaging");
        }
        if (laboratoryTestChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "laboratoryTest");
        }
        if (prescriptionChildrenQueryPort.existsActiveByConsultationId(id)) {
            throw new ConsultationHasActiveChildrenException(id, "prescription");
        }
        repository.delete(id);
    }
}
