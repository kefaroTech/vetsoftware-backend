package com.vetsoftware.app.animalalert.application.usecase;

import com.vetsoftware.app.animalalert.application.port.in.DeleteAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.alert.delete")
@Service
public class DeleteAnimalAlertService implements DeleteAnimalAlertUseCase {
    private final AnimalAlertRepository repository;

    public DeleteAnimalAlertService(AnimalAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new AnimalAlertNotFoundException(id));
        repository.delete(id, companyId);
    }
}
