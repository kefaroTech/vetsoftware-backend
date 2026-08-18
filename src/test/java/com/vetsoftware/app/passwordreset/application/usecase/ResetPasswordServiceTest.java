package com.vetsoftware.app.passwordreset.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeePasswordResetter;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.domain.InvalidPasswordResetTokenException;
import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * "La posesion del token es la autorizacion" (javadoc de la clase bajo prueba):
 * no hay ningun parametro de identidad que comparar contra el empleado del
 * token, asi que no existe un escenario de "token de otro usuario" distinto de
 * los ya cubiertos aqui — cualquiera con el raw token correcto (recibido solo
 * por correo) puede resetear la cuenta a la que pertenece. Ver hueco anotado en
 * el informe final.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordService")
class ResetPasswordServiceTest {

    private static final Long EMPLOYEE_ID = 500L;
    private static final Long COMPANY_ID = 9L;
    private static final String RAW_TOKEN = "raw-token-de-prueba";
    private static final String HASH = PasswordResetTokens.hash(RAW_TOKEN);
    // Fijos y bien alejados de "ahora" en ambas direcciones (deuda anotada: el
    // service llama a LocalDateTime.now() directamente, sin Clock inyectable).
    private static final LocalDateTime FUTURO = LocalDateTime.of(2035, 6, 1, 10, 0);
    private static final LocalDateTime PASADO = LocalDateTime.of(2020, 1, 1, 10, 0);

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmployeePasswordResetter employeePasswordResetter;

    @InjectMocks
    private ResetPasswordService service;

    @Nested
    @DisplayName("token en blanco: no consulta el repositorio")
    class TokenEnBlanco {

        @ParameterizedTest(name = "[{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("token vacio o en blanco se rechaza sin tocar ningun puerto")
        void token_vacio_o_en_blanco_se_rechaza(String tokenInvalido) {
            assertThatThrownBy(
                    () -> service.execute(new ResetPasswordCommand(tokenInvalido, "nuevaClave123")))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("Password reset token is required");

            verifyNoInteractions(tokenRepository, employeePasswordResetter);
        }

        @Test
        @DisplayName("token nulo se rechaza sin tocar ningun puerto")
        void token_nulo_se_rechaza() {
            assertThatThrownBy(
                    () -> service.execute(new ResetPasswordCommand(null, "nuevaClave123")))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("Password reset token is required");

            verifyNoInteractions(tokenRepository, employeePasswordResetter);
        }
    }

    @Nested
    @DisplayName("token invalido: nunca llega a resetear la contrasena")
    class TokenInvalido {

        @Test
        @DisplayName("token que no existe")
        void token_que_no_existe() {
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new ResetPasswordCommand(RAW_TOKEN, "nuevaClave123")))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("Invalid password reset token");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeePasswordResetter);
        }

        @Test
        @DisplayName("token expirado: consume() lo rechaza")
        void token_expirado() {
            PasswordResetToken expirado = new PasswordResetToken(2L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    PASADO, null);
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(expirado));

            assertThatThrownBy(
                    () -> service.execute(new ResetPasswordCommand(RAW_TOKEN, "nuevaClave123")))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("expired");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeePasswordResetter);
        }

        @Test
        @DisplayName("token ya usado: consume() lo rechaza")
        void token_ya_usado() {
            PasswordResetToken consumido = new PasswordResetToken(3L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    FUTURO, LocalDateTime.of(2026, 1, 1, 10, 0));
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(consumido));

            assertThatThrownBy(
                    () -> service.execute(new ResetPasswordCommand(RAW_TOKEN, "nuevaClave123")))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("already used");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeePasswordResetter);
        }
    }

    @Nested
    @DisplayName("token vigente: consume y aplica la nueva contrasena")
    class TokenVigente {

        @Test
        @DisplayName("consume el token, lo guarda consumido y resetea al empleado dueno del token")
        void consume_guarda_y_aplica_la_contrasena() {
            PasswordResetToken vigente = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    FUTURO, null);
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(vigente));

            service.execute(new ResetPasswordCommand(RAW_TOKEN, "nuevaClave123"));

            verify(tokenRepository).save(vigente);
            assertThat(vigente.getConsumedAt()).isNotNull();
            verify(employeePasswordResetter).reset(EMPLOYEE_ID, "nuevaClave123");
        }
    }
}
