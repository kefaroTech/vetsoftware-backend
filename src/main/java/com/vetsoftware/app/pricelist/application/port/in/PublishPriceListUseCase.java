package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.command.PublishPriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Congela la tarifa. Es la operacion que hace inmutable a la lista y a todos
 * sus precios (regla R9).
 */
public interface PublishPriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PriceListDto execute(PublishPriceListCommand command);
}
