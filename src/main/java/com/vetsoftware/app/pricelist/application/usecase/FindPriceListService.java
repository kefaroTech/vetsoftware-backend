package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.FindPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "pricelist.find")
@Service
public class FindPriceListService implements FindPriceListUseCase {

    private final PriceListRepository repository;

    public FindPriceListService(PriceListRepository repository) {
        this.repository = repository;
    }

    @Override
    public PriceListDto findById(Long id) {
        return repository.findById(id).map(PriceListDto::from)
                .orElseThrow(() -> new PriceListNotFoundException(id));
    }
}
