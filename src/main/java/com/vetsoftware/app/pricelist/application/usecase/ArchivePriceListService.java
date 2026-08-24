package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.ArchivePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "pricelist.archive")
@Service
public class ArchivePriceListService implements ArchivePriceListUseCase {

    private final PriceListRepository repository;

    public ArchivePriceListService(PriceListRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PriceListDto execute(Long id) {
        PriceList priceList = repository.findById(id)
                .orElseThrow(() -> new PriceListNotFoundException(id));
        priceList.archive();
        return PriceListDto.from(repository.save(priceList));
    }
}
