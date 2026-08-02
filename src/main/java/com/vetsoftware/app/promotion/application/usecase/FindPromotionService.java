package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.FindPromotionUseCase;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "promotion.find")
@Service
public class FindPromotionService implements FindPromotionUseCase {
  private final PromotionRepository repository;

  public FindPromotionService(PromotionRepository repository) {
    this.repository = repository;
  }

  @Override
  public PromotionDto findById(Long id, Long companyId) {
    return PromotionDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new PromotionNotFoundException(id)));
  }
}
