package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.port.in.DeleteConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation_type.delete")
@Service
public class DeleteConsultationTypeService implements DeleteConsultationTypeUseCase {
    private final ConsultationTypeRepository repository;

    public DeleteConsultationTypeService(ConsultationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConsultationTypeNotFoundException(id));
        repository.delete(id);
    }
}
