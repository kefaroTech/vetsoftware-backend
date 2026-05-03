package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.port.in.DeleteConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.delete")
@Service
public class DeleteConsultationService implements DeleteConsultationUseCase {
    private final ConsultationRepository repository;

    public DeleteConsultationService(ConsultationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConsultationNotFoundException(id));
        repository.delete(id);
    }
}
