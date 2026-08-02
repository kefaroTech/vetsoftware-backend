package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.CreateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.type.create")
@Service
public class CreateConsultationTypeService implements CreateConsultationTypeUseCase {
  private final ConsultationTypeRepository repository;

  public CreateConsultationTypeService(ConsultationTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public ConsultationTypeDto execute(CreateConsultationTypeCommand command) {
    return ConsultationTypeDto.from(
        repository.save(ConsultationType.create(command.name(), command.description())));
  }
}
