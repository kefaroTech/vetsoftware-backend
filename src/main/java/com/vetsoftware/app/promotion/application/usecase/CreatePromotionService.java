package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.CreatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "promotion.create")
@Service
public class CreatePromotionService implements CreatePromotionUseCase {
  private final PromotionRepository repository;
  private final CompanyQueryPort companyQueryPort;
  private final PromotionTargetQueryPort promotionTargetQueryPort;

  public CreatePromotionService(
      PromotionRepository repository,
      CompanyQueryPort companyQueryPort,
      PromotionTargetQueryPort promotionTargetQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
    this.promotionTargetQueryPort = promotionTargetQueryPort;
  }

  @Override
  public PromotionDto execute(CreatePromotionCommand command) {
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    if (!promotionTargetQueryPort.exists(
        command.applicationType(), command.applicationItem(), command.companyId())) {
      throw new IllegalArgumentException("applicationItem not found: " + command.applicationItem());
    }
    return PromotionDto.from(
        repository.save(
            Promotion.create(
                command.name(),
                command.promotionType(),
                command.applicationType(),
                command.applicationItem(),
                command.valueType(),
                command.value(),
                command.startDate(),
                command.endDate(),
                command.promotionStatus(),
                company)));
  }
}
