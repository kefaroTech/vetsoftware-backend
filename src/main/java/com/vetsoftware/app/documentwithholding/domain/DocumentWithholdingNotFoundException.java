package com.vetsoftware.app.documentwithholding.domain;

public class DocumentWithholdingNotFoundException extends RuntimeException {

    public DocumentWithholdingNotFoundException(Long id) {
        super("Document withholding not found: " + id);
    }
}
