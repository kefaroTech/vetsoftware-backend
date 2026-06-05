package com.vetsoftware.app.promotion.application.usecase;

import com.vetsoftware.app.promotion.application.port.in.DeletePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "promotion.delete")
@Service
public class DeletePromotionService implements DeletePromotionUseCase {
    private final PromotionRepository repository;

    public DeletePromotionService(PromotionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new PromotionNotFoundException(id));
        repository.delete(id);
    }
}
