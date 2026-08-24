package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.command.UpdatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdatePriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PriceListDto execute(UpdatePriceListCommand command);
}
