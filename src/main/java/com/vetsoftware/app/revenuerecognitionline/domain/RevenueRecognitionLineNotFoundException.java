package com.vetsoftware.app.revenuerecognitionline.domain;

public class RevenueRecognitionLineNotFoundException extends RuntimeException {

    public RevenueRecognitionLineNotFoundException(Long id) {
        super("Revenue recognition line not found: " + id);
    }
}
