package com.vetsoftware.app.auth.application.port.out;

public interface TokenGenerator {
    String generate(Long id, String type, Long companyId, Long authVersion);
}
