package com.vetsoftware.app.promotion.domain;

public class PromotionNotFoundException extends RuntimeException {
    public PromotionNotFoundException(Long id) {
        super("Promotion not found: " + id);
    }
}
