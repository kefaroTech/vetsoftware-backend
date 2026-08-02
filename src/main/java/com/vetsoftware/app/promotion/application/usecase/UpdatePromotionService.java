package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.UpdatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "promotion.update")
@Service
public class UpdatePromotionService implements UpdatePromotionUseCase {
  private final PromotionRepository repository;
  private final CompanyQueryPort companyQueryPort;
  private final PromotionTargetQueryPort promotionTargetQueryPort;

  public UpdatePromotionService(
      PromotionRepository repository,
      CompanyQueryPort companyQueryPort,
      PromotionTargetQueryPort promotionTargetQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
    this.promotionTargetQueryPort = promotionTargetQueryPort;
  }

  @Override
  @Transactional
  public PromotionDto execute(UpdatePromotionCommand command) {
    Promotion promotion =
        repository
            .findById(command.id())
            .orElseThrow(() -> new PromotionNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    if (!promotionTargetQueryPort.exists(
        command.applicationType(), command.applicationItem(), command.companyId())) {
      throw new IllegalArgumentException("applicationItem not found: " + command.applicationItem());
    }
    promotion.update(
        command.name(),
        command.promotionType(),
        command.applicationType(),
        command.applicationItem(),
        command.valueType(),
        command.value(),
        command.startDate(),
        command.endDate(),
        command.promotionStatus(),
        company);
    return PromotionDto.from(repository.save(promotion));
  }
}
