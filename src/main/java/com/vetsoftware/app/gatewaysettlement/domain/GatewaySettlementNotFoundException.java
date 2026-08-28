package com.vetsoftware.app.gatewaysettlement.domain;

public class GatewaySettlementNotFoundException extends RuntimeException {

    public GatewaySettlementNotFoundException(Long id) {
        super("Gateway settlement not found: " + id);
    }
}
