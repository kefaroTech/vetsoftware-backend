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

    /**
     * {@code companyId} null = caller sin empresa (SYSTEM); con empresa, la lectura
     * previa va acotada para que borrar la promoción de otro tenant sea un 404 y no
     * un borrado.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new PromotionNotFoundException(id));
        repository.delete(id);
    }
}
