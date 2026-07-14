package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.EmployeeActivationPort;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
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
class LoginEmployeeServiceTest {

    @Mock private EmployeeCredentialsRepository credentialsRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private RefreshTokenIssuer refreshTokenIssuer;
    @Mock private PasswordHasher passwordHasher;
    @Mock private EmployeeActivationPort employeeActivationPort;
    @Mock private AuthEmployeeRepository authEmployeeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private LoginEmployeeService service;

    @Test
    void login_rota_la_version_y_revoca_la_sesion_anterior_antes_de_emitir_tokens() {
        var credentials = new EmployeeCredentialsRepository.EmployeeCredentials(
                7L, 3L, 4L, "hash", true);
        var activeSession = new AuthEmployeeRepository.AuthEmployee(7L, 3L, 5L);
        when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
        when(passwordHasher.matches("secret", "hash")).thenReturn(true);
        when(authEmployeeRepository.rotateAuthVersion(7L)).thenReturn(Optional.of(activeSession));
        when(tokenGenerator.generate(7L, "EMPLOYEE", 3L, 5L)).thenReturn("access");
        when(refreshTokenIssuer.issue(7L, "EMPLOYEE", 5L)).thenReturn("refresh");

        TokenDto result = service.execute(new LoginEmployeeCommand("EMP-1", "secret"));

        assertThat(result.token()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        InOrder order = inOrder(authEmployeeRepository, refreshTokenRepository,
                tokenGenerator, refreshTokenIssuer);
        order.verify(authEmployeeRepository).rotateAuthVersion(7L);
        order.verify(refreshTokenRepository).revokeAllForSubject(7L, "EMPLOYEE");
        order.verify(tokenGenerator).generate(7L, "EMPLOYEE", 3L, 5L);
        order.verify(refreshTokenIssuer).issue(7L, "EMPLOYEE", 5L);
    }

    @Test
    void credenciales_invalidas_no_rotan_la_sesion() {
        var credentials = new EmployeeCredentialsRepository.EmployeeCredentials(
                7L, 3L, 4L, "hash", true);
        when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.execute(new LoginEmployeeCommand("EMP-1", "wrong")))
            .isInstanceOf(com.vetsoftware.app.auth.application.exception.InvalidCredentialsException.class);

        verify(authEmployeeRepository, never()).rotateAuthVersion(7L);
        verify(refreshTokenRepository, never()).revokeAllForSubject(7L, "EMPLOYEE");
    }
}
