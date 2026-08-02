package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private RefreshTokenSecret refreshTokenSecret;
  @Mock private RefreshTokenIssuer refreshTokenIssuer;
  @Mock private TokenGenerator tokenGenerator;
  @Mock private AuthEmployeeRepository authEmployeeRepository;
  @Mock private AuthSystemUserRepository authSystemUserRepository;
  @InjectMocks private RefreshAccessTokenService service;

  @Test
  void rechaza_refresh_de_una_sesion_reemplazada() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("old-refresh")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(stored));
    when(authEmployeeRepository.findActiveById(7L))
        .thenReturn(Optional.of(new AuthEmployeeRepository.AuthEmployee(7L, 2L, 4L)));

    assertThatThrownBy(() -> service.execute("old-refresh"))
        .isInstanceOf(SessionReplacedException.class);

    verify(refreshTokenRepository, never()).revokeById(11L);
    verify(refreshTokenIssuer, never()).issue(7L, "EMPLOYEE", 3L);
  }

  @Test
  void rota_refresh_de_la_sesion_activa_del_usuario_de_sistema() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            12L, 2L, "SYSTEM_USER", 9L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("refresh")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(stored));
    when(authSystemUserRepository.findActiveById(2L))
        .thenReturn(Optional.of(new AuthSystemUserRepository.AuthSystemUser(2L, 9L)));
    when(tokenGenerator.generate(2L, "SYSTEM_USER", null, 9L)).thenReturn("access-2");
    when(refreshTokenIssuer.issue(2L, "SYSTEM_USER", 9L)).thenReturn("refresh-2");

    var result = service.execute("refresh");

    assertThat(result.token()).isEqualTo("access-2");
    assertThat(result.refreshToken()).isEqualTo("refresh-2");
    verify(refreshTokenRepository).revokeById(12L);
  }

  @Test
  void rota_el_refresh_de_un_empleado_y_revoca_el_presentado() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("refresh")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(stored));
    when(authEmployeeRepository.findActiveById(7L))
        .thenReturn(Optional.of(new AuthEmployeeRepository.AuthEmployee(7L, 2L, 3L)));
    when(tokenGenerator.generate(7L, "EMPLOYEE", 2L, 3L)).thenReturn("access-1");
    when(refreshTokenIssuer.issue(7L, "EMPLOYEE", 3L)).thenReturn("refresh-1");

    var result = service.execute("refresh");

    assertThat(result.token()).isEqualTo("access-1");
    assertThat(result.type()).isEqualTo("EMPLOYEE");
    verify(refreshTokenRepository).revokeById(11L);
  }

  @Test
  void un_refresh_vacio_o_nulo_se_rechaza_sin_tocar_la_base() {
    assertThatThrownBy(() -> service.execute(null)).isInstanceOf(InvalidCredentialsException.class);
    assertThatThrownBy(() -> service.execute("   "))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(refreshTokenRepository, never()).findByHash(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void un_refresh_desconocido_se_rechaza() {
    when(refreshTokenSecret.hash("desconocido")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute("desconocido"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void un_refresh_ya_revocado_no_emite_tokens_nuevos() {
    var revoked =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().plusHours(1), true);
    when(refreshTokenSecret.hash("reusado")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> service.execute("reusado"))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(refreshTokenIssuer, never())
        .issue(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void un_refresh_expirado_se_rechaza() {
    var expired =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().minusSeconds(1), false);
    when(refreshTokenSecret.hash("vencido")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.execute("vencido"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void el_refresh_de_un_empleado_desactivado_deja_de_servir() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("refresh")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(stored));
    when(authEmployeeRepository.findActiveById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute("refresh"))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(refreshTokenRepository, never()).revokeById(11L);
  }

  @Test
  void el_token_se_busca_por_hash_nunca_por_su_valor_plano() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "EMPLOYEE", 3L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("secreto-en-claro")).thenReturn("hash-derivado");
    when(refreshTokenRepository.findByHash("hash-derivado")).thenReturn(Optional.of(stored));
    when(authEmployeeRepository.findActiveById(7L))
        .thenReturn(Optional.of(new AuthEmployeeRepository.AuthEmployee(7L, 2L, 3L)));

    service.execute("secreto-en-claro");

    verify(refreshTokenSecret).hash("secreto-en-claro");
    verify(refreshTokenRepository).findByHash("hash-derivado");
    verify(refreshTokenRepository, never()).findByHash("secreto-en-claro");
  }

  @Test
  void un_tipo_de_sujeto_desconocido_se_rechaza() {
    var stored =
        new RefreshTokenRepository.StoredRefreshToken(
            11L, 7L, "OTRO", 3L, LocalDateTime.now().plusHours(1), false);
    when(refreshTokenSecret.hash("refresh")).thenReturn("hash");
    when(refreshTokenRepository.findByHash("hash")).thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> service.execute("refresh"))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
