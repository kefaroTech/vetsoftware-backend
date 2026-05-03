package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.FindPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "prescription.find")
@Service
public class FindPrescriptionService implements FindPrescriptionUseCase {
    private final PrescriptionRepository repository;

    public FindPrescriptionService(PrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PrescriptionDto findById(Long id) {
        return PrescriptionDto.from(repository.findById(id)
            .orElseThrow(() -> new PrescriptionNotFoundException(id)));
    }
}
