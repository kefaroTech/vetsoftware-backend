package com.vetsoftware.app.paymentgateway.infrastructure.web.response;

import com.vetsoftware.app.paymentgateway.application.dto.WompiCheckoutConfigDto;

public record WompiCheckoutConfigResponse(String environment, String apiBaseUrl, String publicKey,
        Acceptance acceptance, Acceptance personalDataAuthorization) {

    public record Acceptance(String token, String permalink) {
        static Acceptance from(WompiCheckoutConfigDto.Acceptance dto) {
            return new Acceptance(dto.token(), dto.permalink());
        }
    }

    public static WompiCheckoutConfigResponse from(WompiCheckoutConfigDto dto) {
        return new WompiCheckoutConfigResponse(dto.environment(), dto.apiBaseUrl(), dto.publicKey(),
                Acceptance.from(dto.acceptance()),
                Acceptance.from(dto.personalDataAuthorization()));
    }
}
