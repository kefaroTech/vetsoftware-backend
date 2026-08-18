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

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aquí no hay un
     * findById previo que valide la propiedad, así que sin filtrar por empresa se
     * revivía la promoción de otro tenant y con ella los precios que cobra. Cero
     * filas afectadas significa «no existe en TU empresa» → 404.
     */
    @Override
    @Transactional
    public PromotionDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new PromotionNotFoundException(id);
        return PromotionDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new PromotionNotFoundException(id)));
    }
}
