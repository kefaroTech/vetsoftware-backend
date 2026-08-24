package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PriceListDto(Long id, String code, String name, String currency, LocalDate validFrom,
        LocalDate validTo, PriceListStatus status, LocalDateTime publishedAt,
        Long publishedBySystemUserId, LocalDateTime createdDate, boolean enabled) {

    public static PriceListDto from(PriceList priceList) {
        return new PriceListDto(priceList.getId(), priceList.getCode(), priceList.getName(),
                priceList.getCurrency(), priceList.getValidFrom(), priceList.getValidTo(),
                priceList.getStatus(), priceList.getPublishedAt(),
                priceList.getPublishedBySystemUserId(), priceList.getCreatedDate(),
                priceList.isEnabled());
    }
}
