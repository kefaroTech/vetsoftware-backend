package com.vetsoftware.app.promotion.application.port.out;

import com.vetsoftware.app.promotion.domain.Promotion;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository {
    Promotion save(Promotion promotion);
    Optional<Promotion> findById(Long id);
    Optional<Promotion> findByIdAndCompanyId(Long id, Long companyId);
    List<Promotion> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
