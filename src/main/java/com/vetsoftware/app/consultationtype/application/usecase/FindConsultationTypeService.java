package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.FindConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.type.find")
@Service
public class FindConsultationTypeService implements FindConsultationTypeUseCase {
  private final ConsultationTypeRepository repository;

  public FindConsultationTypeService(ConsultationTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public ConsultationTypeDto findById(Long id) {
    return ConsultationTypeDto.from(
        repository.findById(id).orElseThrow(() -> new ConsultationTypeNotFoundException(id)));
  }
}
