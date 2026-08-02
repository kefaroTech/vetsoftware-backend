package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.FindPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "prescription.find")
@Service
public class FindPrescriptionService implements FindPrescriptionUseCase {
    private final PrescriptionRepository repository;
    private final MedicamentQueryPort medicamentQueryPort;

    public FindPrescriptionService(PrescriptionRepository repository,
                                   MedicamentQueryPort medicamentQueryPort) {
        this.repository = repository;
        this.medicamentQueryPort = medicamentQueryPort;
    }

    @Override
    public PrescriptionDto findById(Long id, Long companyId) {
        Prescription prescription = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new PrescriptionNotFoundException(id));
        List<MedicamentRef> medicaments = medicamentQueryPort.findByPrescriptionId(id);
        return PrescriptionDto.from(prescription, medicaments);
    }
}
