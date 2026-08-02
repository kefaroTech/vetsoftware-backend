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
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", true);
    var activeSession = new AuthEmployeeRepository.AuthEmployee(7L, 3L, 5L);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("secret", "hash")).thenReturn(true);
    when(authEmployeeRepository.rotateAuthVersion(7L)).thenReturn(Optional.of(activeSession));
    when(tokenGenerator.generate(7L, "EMPLOYEE", 3L, 5L)).thenReturn("access");
    when(refreshTokenIssuer.issue(7L, "EMPLOYEE", 5L)).thenReturn("refresh");

    TokenDto result = service.execute(new LoginEmployeeCommand("EMP-1", "secret"));

    assertThat(result.token()).isEqualTo("access");
    assertThat(result.refreshToken()).isEqualTo("refresh");
    InOrder order =
        inOrder(authEmployeeRepository, refreshTokenRepository, tokenGenerator, refreshTokenIssuer);
    order.verify(authEmployeeRepository).rotateAuthVersion(7L);
    order.verify(refreshTokenRepository).revokeAllForSubject(7L, "EMPLOYEE");
    order.verify(tokenGenerator).generate(7L, "EMPLOYEE", 3L, 5L);
    order.verify(refreshTokenIssuer).issue(7L, "EMPLOYEE", 5L);
  }

  @Test
  void credenciales_invalidas_no_rotan_la_sesion() {
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", true);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.execute(new LoginEmployeeCommand("EMP-1", "wrong")))
        .isInstanceOf(
            com.vetsoftware.app.auth.application.exception.InvalidCredentialsException.class);

    verify(authEmployeeRepository, never()).rotateAuthVersion(7L);
    verify(refreshTokenRepository, never()).revokeAllForSubject(7L, "EMPLOYEE");
  }

  @Test
  void un_codigo_inexistente_falla_igual_que_una_clave_incorrecta() {
    // Mismo tipo de excepción para no revelar si el usuario existe.
    when(credentialsRepository.findByCode("NO-EXISTE")).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.execute(new LoginEmployeeCommand("NO-EXISTE", "loquesea")))
        .isInstanceOf(
            com.vetsoftware.app.auth.application.exception.InvalidCredentialsException.class);

    verify(passwordHasher, never())
        .matches(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void un_correo_sin_verificar_no_puede_iniciar_sesion_aunque_la_clave_sea_correcta() {
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", false);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("secret", "hash")).thenReturn(true);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.execute(new LoginEmployeeCommand("EMP-1", "secret")))
        .isInstanceOf(
            com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException.class);

    verify(employeeActivationPort, never()).activateOnLogin(7L);
    verify(authEmployeeRepository, never()).rotateAuthVersion(7L);
  }

  @Test
  void la_verificacion_del_correo_se_evalua_despues_de_la_clave() {
    // Si se evaluara antes, un atacante sabría qué cuentas existen sin conocer la contraseña.
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", false);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.execute(new LoginEmployeeCommand("EMP-1", "wrong")))
        .isInstanceOf(
            com.vetsoftware.app.auth.application.exception.InvalidCredentialsException.class);
  }

  @Test
  void el_primer_login_del_staff_invitado_lo_activa() {
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", true);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("secret", "hash")).thenReturn(true);
    when(authEmployeeRepository.rotateAuthVersion(7L))
        .thenReturn(Optional.of(new AuthEmployeeRepository.AuthEmployee(7L, 3L, 5L)));

    service.execute(new LoginEmployeeCommand("EMP-1", "secret"));

    verify(employeeActivationPort).activateOnLogin(7L);
  }

  @Test
  void un_empleado_desactivado_no_obtiene_sesion() {
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", true);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("secret", "hash")).thenReturn(true);
    when(authEmployeeRepository.rotateAuthVersion(7L)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.execute(new LoginEmployeeCommand("EMP-1", "secret")))
        .isInstanceOf(
            com.vetsoftware.app.auth.application.exception.InvalidCredentialsException.class);

    verify(tokenGenerator, never())
        .generate(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void el_token_lleva_la_empresa_y_la_version_recien_rotadas_no_las_del_lookup() {
    // credentials trae authVersion 4; la rotación devuelve 5: el token debe firmar la 5,
    // o una sesión anterior seguiría siendo válida.
    var credentials =
        new EmployeeCredentialsRepository.EmployeeCredentials(7L, 3L, 4L, "hash", true);
    when(credentialsRepository.findByCode("EMP-1")).thenReturn(Optional.of(credentials));
    when(passwordHasher.matches("secret", "hash")).thenReturn(true);
    when(authEmployeeRepository.rotateAuthVersion(7L))
        .thenReturn(Optional.of(new AuthEmployeeRepository.AuthEmployee(7L, 3L, 5L)));

    service.execute(new LoginEmployeeCommand("EMP-1", "secret"));

    verify(tokenGenerator).generate(7L, "EMPLOYEE", 3L, 5L);
    verify(tokenGenerator, never()).generate(7L, "EMPLOYEE", 3L, 4L);
  }
}
