package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.ReactivateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.type.reactivate")
@Service
public class ReactivateConsultationTypeService implements ReactivateConsultationTypeUseCase {
    private final ConsultationTypeRepository repository;

    public ReactivateConsultationTypeService(ConsultationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ConsultationTypeDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new ConsultationTypeNotFoundException(id);
        return ConsultationTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new ConsultationTypeNotFoundException(id)));
    }
}
