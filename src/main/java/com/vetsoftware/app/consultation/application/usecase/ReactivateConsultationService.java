package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.ReactivateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.reactivate")
@Service
public class ReactivateConsultationService implements ReactivateConsultationUseCase {
    private final ConsultationRepository repository;

    public ReactivateConsultationService(ConsultationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ConsultationDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0) throw new ConsultationNotFoundException(id);
        return ConsultationDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ConsultationNotFoundException(id)));
    }
}
