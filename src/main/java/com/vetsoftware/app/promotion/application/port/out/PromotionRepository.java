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

    /**
     * Reactiva la promoción SOLO si pertenece a {@code companyId}. Devuelve las
     * filas afectadas: 0 = no existe en esa empresa.
     */
    int reactivate(Long id, Long companyId);
}
