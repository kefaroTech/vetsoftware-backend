package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginSystemUserServiceTest {

    @Mock private SystemUserCredentialsRepository credentialsRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private RefreshTokenIssuer refreshTokenIssuer;
    @Mock private PasswordHasher passwordHasher;
    @Mock private AuthSystemUserRepository authSystemUserRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private LoginSystemUserService service;

    @Test
    void login_rota_la_version_y_revoca_la_sesion_anterior() {
        var credentials = new SystemUserCredentialsRepository.SystemUserCredentials(2L, "hash");
        var activeSession = new AuthSystemUserRepository.AuthSystemUser(2L, 9L);
        when(credentialsRepository.findByCode("ADMIN")).thenReturn(Optional.of(credentials));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        when(authSystemUserRepository.rotateAuthVersion(2L)).thenReturn(Optional.of(activeSession));
        when(tokenGenerator.generate(2L, "SYSTEM_USER", null, 9L)).thenReturn("access");
        when(refreshTokenIssuer.issue(2L, "SYSTEM_USER", 9L)).thenReturn("refresh");

        TokenDto result = service.execute(new LoginSystemUserCommand("ADMIN", "secret"));

        assertThat(result.token()).isEqualTo("access");
        InOrder order = inOrder(authSystemUserRepository, refreshTokenRepository,
                tokenGenerator, refreshTokenIssuer);
        order.verify(authSystemUserRepository).rotateAuthVersion(2L);
        order.verify(refreshTokenRepository).revokeAllForSubject(2L, "SYSTEM_USER");
        order.verify(tokenGenerator).generate(2L, "SYSTEM_USER", null, 9L);
        order.verify(refreshTokenIssuer).issue(2L, "SYSTEM_USER", 9L);
    }
}
