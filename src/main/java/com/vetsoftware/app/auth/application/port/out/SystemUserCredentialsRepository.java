package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface SystemUserCredentialsRepository {
    Optional<SystemUserCredentials> findByCode(String code);

    record SystemUserCredentials(Long id, String hashPassword) {
    }
}
