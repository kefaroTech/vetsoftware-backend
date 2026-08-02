package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.FindConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.find")
@Service
public class FindConsultationService implements FindConsultationUseCase {
  private final ConsultationRepository repository;

  public FindConsultationService(ConsultationRepository repository) {
    this.repository = repository;
  }

  @Override
  public ConsultationDto findById(Long id, Long companyId) {
    return ConsultationDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ConsultationNotFoundException(id)));
  }
}
