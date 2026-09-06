package com.vetsoftware.app.paymentgateway.infrastructure.web.response;

import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import java.time.LocalDate;

public record WompiPaymentMethodResponse(Long paymentMethodId, String brand, String lastFour,
        LocalDate expiresOn, boolean defaultMethod) {

    public static WompiPaymentMethodResponse from(WompiPaymentMethodDto dto) {
        return new WompiPaymentMethodResponse(dto.paymentMethodId(), dto.brand(), dto.lastFour(),
                dto.expiresOn(), dto.defaultMethod());
    }
}
