package com.vetsoftware.app.withholdingcertificate.domain;

public class WithholdingCertificateNotFoundException extends RuntimeException {

    public WithholdingCertificateNotFoundException(Long id) {
        super("Withholding certificate not found: " + id);
    }
}
