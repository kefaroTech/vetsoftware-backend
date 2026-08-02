package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.port.in.DeleteAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.delete")
@Service
public class DeleteAnimalService implements DeleteAnimalUseCase {
    private final AnimalRepository repository;
    private final VaccinationChildrenQueryPort vaccinationChildrenQueryPort;
    private final DewormingChildrenQueryPort dewormingChildrenQueryPort;
    private final SurgeryChildrenQueryPort surgeryChildrenQueryPort;
    private final HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort;
    private final DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;
    private final LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;
    private final SpaChildrenQueryPort spaChildrenQueryPort;
    private final DayCareChildrenQueryPort dayCareChildrenQueryPort;
    private final ConsultationChildrenQueryPort consultationChildrenQueryPort;

    public DeleteAnimalService(AnimalRepository repository,
            VaccinationChildrenQueryPort vaccinationChildrenQueryPort,
            DewormingChildrenQueryPort dewormingChildrenQueryPort,
            SurgeryChildrenQueryPort surgeryChildrenQueryPort,
            HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort,
            DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort,
            LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort,
            SpaChildrenQueryPort spaChildrenQueryPort,
            DayCareChildrenQueryPort dayCareChildrenQueryPort,
            ConsultationChildrenQueryPort consultationChildrenQueryPort) {
        this.repository = repository;
        this.vaccinationChildrenQueryPort = vaccinationChildrenQueryPort;
        this.dewormingChildrenQueryPort = dewormingChildrenQueryPort;
        this.surgeryChildrenQueryPort = surgeryChildrenQueryPort;
        this.hospitalizationChildrenQueryPort = hospitalizationChildrenQueryPort;
        this.diagnosticImagingChildrenQueryPort = diagnosticImagingChildrenQueryPort;
        this.laboratoryTestChildrenQueryPort = laboratoryTestChildrenQueryPort;
        this.spaChildrenQueryPort = spaChildrenQueryPort;
        this.dayCareChildrenQueryPort = dayCareChildrenQueryPort;
        this.consultationChildrenQueryPort = consultationChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new AnimalNotFoundException(id));
        if (vaccinationChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "vaccination");
        }
        if (dewormingChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "deworming");
        }
        if (surgeryChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "surgery");
        }
        if (hospitalizationChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "hospitalization");
        }
        if (diagnosticImagingChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "diagnosticImaging");
        }
        if (laboratoryTestChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "laboratoryTest");
        }
        if (spaChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "spa");
        }
        if (dayCareChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "dayCare");
        }
        if (consultationChildrenQueryPort.existsActiveByAnimalId(id)) {
            throw new AnimalHasActiveChildrenException(id, "consultation");
        }
        repository.delete(id, companyId);
    }
}
