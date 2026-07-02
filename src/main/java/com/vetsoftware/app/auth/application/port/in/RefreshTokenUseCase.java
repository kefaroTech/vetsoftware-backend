package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.TokenDto;

/** Rota un refresh token válido: revoca el usado y emite un nuevo par access + refresh. */
public interface RefreshTokenUseCase {
    TokenDto execute(String rawRefreshToken);
}
