package com.vetsoftware.app.infrastructure.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vetsoftware.pdf.gotenberg")
public record PdfProperties(String url, int timeoutSeconds) {
}
