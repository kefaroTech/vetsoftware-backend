package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.ReactivatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "promotion.reactivate")
@Service
public class ReactivatePromotionService implements ReactivatePromotionUseCase {
    private final PromotionRepository repository;

    public ReactivatePromotionService(PromotionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PromotionDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new PromotionNotFoundException(id);
        return PromotionDto.from(repository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException(id)));
    }
}
