package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.command.UpdatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.UpdatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La puerta unica por la que se edita una lista, y por tanto el sitio donde R9
 * se cumple o se rompe: {@code PriceList.update} empieza llamando a
 * {@code requireDraft()}.
 */
@Observed(name = "pricelist.update")
@Service
public class UpdatePriceListService implements UpdatePriceListUseCase {

    private final PriceListRepository repository;

    public UpdatePriceListService(PriceListRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PriceListDto execute(UpdatePriceListCommand command) {
        PriceList priceList = repository.findById(command.id())
                .orElseThrow(() -> new PriceListNotFoundException(command.id()));
        priceList.update(command.name(), command.currency(), command.validFrom(),
                command.validTo());
        return PriceListDto.from(repository.save(priceList));
    }
}
