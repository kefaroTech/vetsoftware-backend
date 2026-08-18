package com.vetsoftware.app.passwordreset.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePasswordResetTokenService — chequeo sin consumir")
class ValidatePasswordResetTokenServiceTest {

    private static final Long EMPLOYEE_ID = 500L;
    private static final Long COMPANY_ID = 9L;
    private static final String RAW_TOKEN = "raw-token-de-prueba";
    private static final String HASH = PasswordResetTokens.hash(RAW_TOKEN);
    private static final LocalDateTime FUTURO = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime PASADO = LocalDateTime.now().minusHours(1);

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private ValidatePasswordResetTokenService service;

    @Nested
    @DisplayName("token en blanco: no consulta el repositorio")
    class TokenEnBlanco {

        @ParameterizedTest(name = "[{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("token vacio o en blanco es invalido sin tocar el repositorio")
        void token_vacio_o_en_blanco_es_invalido(String tokenInvalido) {
            assertThat(service.isValid(tokenInvalido)).isFalse();

            verifyNoInteractions(tokenRepository);
        }

        @Test
        @DisplayName("token nulo es invalido sin tocar el repositorio")
        void token_nulo_es_invalido() {
            assertThat(service.isValid(null)).isFalse();

            verifyNoInteractions(tokenRepository);
        }
    }

    @Nested
    @DisplayName("token presente en base")
    class TokenPresente {

        @Test
        @DisplayName("token que no existe es invalido")
        void token_que_no_existe_es_invalido() {
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

            assertThat(service.isValid(RAW_TOKEN)).isFalse();
        }

        @Test
        @DisplayName("token expirado es invalido")
        void token_expirado_es_invalido() {
            PasswordResetToken expirado = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    PASADO, null);
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(expirado));

            assertThat(service.isValid(RAW_TOKEN)).isFalse();
        }

        @Test
        @DisplayName("token ya usado es invalido")
        void token_ya_usado_es_invalido() {
            PasswordResetToken consumido = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    FUTURO, LocalDateTime.now());
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(consumido));

            assertThat(service.isValid(RAW_TOKEN)).isFalse();
        }

        @Test
        @DisplayName("token vigente es valido")
        void token_vigente_es_valido() {
            PasswordResetToken vigente = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, HASH,
                    FUTURO, null);
            when(tokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(vigente));

            assertThat(service.isValid(RAW_TOKEN)).isTrue();
        }
    }
}
