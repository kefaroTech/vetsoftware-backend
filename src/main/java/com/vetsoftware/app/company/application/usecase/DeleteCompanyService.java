package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.EmployeeChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.PermissionChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.PrescriptionChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.RoleChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.delete")
@Service
public class DeleteCompanyService implements DeleteCompanyUseCase {
    private final CompanyRepository repository;
    private final AnimalChildrenQueryPort animalChildrenQueryPort;
    private final OwnerChildrenQueryPort ownerChildrenQueryPort;
    private final EmployeeChildrenQueryPort employeeChildrenQueryPort;
    private final VaccinationChildrenQueryPort vaccinationChildrenQueryPort;
    private final SurgeryChildrenQueryPort surgeryChildrenQueryPort;
    private final HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort;
    private final DewormingChildrenQueryPort dewormingChildrenQueryPort;
    private final DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;
    private final LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;
    private final PrescriptionChildrenQueryPort prescriptionChildrenQueryPort;
    private final SpaChildrenQueryPort spaChildrenQueryPort;
    private final DayCareChildrenQueryPort dayCareChildrenQueryPort;
    private final ConsultationChildrenQueryPort consultationChildrenQueryPort;
    private final PermissionChildrenQueryPort permissionChildrenQueryPort;
    private final RoleChildrenQueryPort roleChildrenQueryPort;

    public DeleteCompanyService(
            CompanyRepository repository,
            AnimalChildrenQueryPort animalChildrenQueryPort,
            OwnerChildrenQueryPort ownerChildrenQueryPort,
            EmployeeChildrenQueryPort employeeChildrenQueryPort,
            VaccinationChildrenQueryPort vaccinationChildrenQueryPort,
            SurgeryChildrenQueryPort surgeryChildrenQueryPort,
            HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort,
            DewormingChildrenQueryPort dewormingChildrenQueryPort,
            DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort,
            LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort,
            PrescriptionChildrenQueryPort prescriptionChildrenQueryPort,
            SpaChildrenQueryPort spaChildrenQueryPort,
            DayCareChildrenQueryPort dayCareChildrenQueryPort,
            ConsultationChildrenQueryPort consultationChildrenQueryPort,
            PermissionChildrenQueryPort permissionChildrenQueryPort,
            RoleChildrenQueryPort roleChildrenQueryPort) {
        this.repository = repository;
        this.animalChildrenQueryPort = animalChildrenQueryPort;
        this.ownerChildrenQueryPort = ownerChildrenQueryPort;
        this.employeeChildrenQueryPort = employeeChildrenQueryPort;
        this.vaccinationChildrenQueryPort = vaccinationChildrenQueryPort;
        this.surgeryChildrenQueryPort = surgeryChildrenQueryPort;
        this.hospitalizationChildrenQueryPort = hospitalizationChildrenQueryPort;
        this.dewormingChildrenQueryPort = dewormingChildrenQueryPort;
        this.diagnosticImagingChildrenQueryPort = diagnosticImagingChildrenQueryPort;
        this.laboratoryTestChildrenQueryPort = laboratoryTestChildrenQueryPort;
        this.prescriptionChildrenQueryPort = prescriptionChildrenQueryPort;
        this.spaChildrenQueryPort = spaChildrenQueryPort;
        this.dayCareChildrenQueryPort = dayCareChildrenQueryPort;
        this.consultationChildrenQueryPort = consultationChildrenQueryPort;
        this.permissionChildrenQueryPort = permissionChildrenQueryPort;
        this.roleChildrenQueryPort = roleChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        if (animalChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "animal");
        }
        if (ownerChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "owner");
        }
        if (employeeChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "employee");
        }
        if (vaccinationChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "vaccination");
        }
        if (surgeryChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "surgery");
        }
        if (hospitalizationChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "hospitalization");
        }
        if (dewormingChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "deworming");
        }
        if (diagnosticImagingChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "diagnosticImaging");
        }
        if (laboratoryTestChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "laboratoryTest");
        }
        if (prescriptionChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "prescription");
        }
        if (spaChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "spa");
        }
        if (dayCareChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "dayCare");
        }
        if (consultationChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "consultation");
        }
        if (permissionChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "permission");
        }
        if (roleChildrenQueryPort.existsActiveByCompanyId(id)) {
            throw new CompanyHasActiveChildrenException(id, "role");
        }
        repository.delete(id);
    }
}
