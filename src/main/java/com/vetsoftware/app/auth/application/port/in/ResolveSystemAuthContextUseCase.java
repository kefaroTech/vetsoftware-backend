package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface ResolveSystemAuthContextUseCase {
  AuthContext execute(Long systemUserId, Long authVersion);
}
