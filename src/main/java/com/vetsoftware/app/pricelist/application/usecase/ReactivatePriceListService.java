package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.ReactivatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deshace una baja logica. Solo puede alcanzar borradores: una lista publicada
 * nunca llega a estar deshabilitada, porque el borrado la rechaza.
 */
@Observed(name = "pricelist.reactivate")
@Service
public class ReactivatePriceListService implements ReactivatePriceListUseCase {

    private final PriceListRepository repository;

    public ReactivatePriceListService(PriceListRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PriceListDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new PriceListNotFoundException(id);
        return PriceListDto.from(
                repository.findById(id).orElseThrow(() -> new PriceListNotFoundException(id)));
    }
}
