package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.port.in.DeletePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.delete")
@Service
public class DeletePrescriptionService implements DeletePrescriptionUseCase {
    private final PrescriptionRepository repository;

    public DeletePrescriptionService(PrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new PrescriptionNotFoundException(id));
        repository.delete(id);
    }
}
