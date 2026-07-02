package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface ResolveAuthContextUseCase {
    AuthContext execute(Long employeeId, Long authVersion);
}
