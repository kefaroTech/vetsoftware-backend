package com.vetsoftware.app.withholdingraterule.domain;

public class WithholdingRateRuleNotFoundException extends RuntimeException {

    public WithholdingRateRuleNotFoundException(Long id) {
        super("Withholding rate rule not found: " + id);
    }
}
