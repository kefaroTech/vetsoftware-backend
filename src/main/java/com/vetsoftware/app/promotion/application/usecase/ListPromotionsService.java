package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.ListPromotionsUseCase;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "promotion.list")
@Service
public class ListPromotionsService implements ListPromotionsUseCase {
  private final PromotionRepository repository;

  public ListPromotionsService(PromotionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PromotionDto> listByCompany(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(PromotionDto::from).toList();
  }
}
