package com.vetsoftware.app.registration.application.usecase;

import static com.vetsoftware.app.registration.testsupport.RegistrationMother.EMPLOYEE_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.comandoVerificar;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenExpirado;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenVigente;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenYaConsumido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeEmailVerifier;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verificación de correo (Opción B). La posesión del token de un solo uso es la
 * autorización, por lo que el camino de fallo importa tanto como el feliz: un
 * token ausente, inexistente, expirado o ya usado nunca debe tocar {@code save}
 * ni verificar al empleado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyEmailService")
class VerifyEmailServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private EmployeeEmailVerifier employeeEmailVerifier;

    private VerifyEmailService service;

    @BeforeEach
    void setUp() {
        service = new VerifyEmailService(tokenRepository, employeeEmailVerifier);
    }

    @Nested
    @DisplayName("Validación de entrada")
    class ValidacionDeEntrada {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("un token vacío o en blanco se rechaza sin tocar el repositorio")
        void un_token_vacio_se_rechaza_sin_tocar_el_repositorio(String tokenInvalido) {
            assertThatThrownBy(() -> service.execute(comandoVerificar(tokenInvalido)))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("required");

            verifyNoInteractions(tokenRepository, employeeEmailVerifier);
        }
    }

    @Nested
    @DisplayName("Resolución del token")
    class ResolucionDelToken {

        @Test
        @DisplayName("un token que no existe se rechaza")
        void un_token_inexistente_se_rechaza() {
            when(tokenRepository.findByTokenHash(VerificationTokens.hash("raw-token")))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoVerificar("raw-token")))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("Invalid verification token");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeeEmailVerifier);
        }

        @Test
        @DisplayName("un token expirado no se consume ni verifica al empleado")
        void un_token_expirado_no_se_consume() {
            when(tokenRepository.findByTokenHash(VerificationTokens.hash("raw-token")))
                    .thenReturn(Optional.of(tokenExpirado()));

            assertThatThrownBy(() -> service.execute(comandoVerificar("raw-token")))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("expired");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeeEmailVerifier);
        }

        @Test
        @DisplayName("un token ya usado no se puede volver a consumir")
        void un_token_ya_usado_no_se_puede_reconsumir() {
            when(tokenRepository.findByTokenHash(VerificationTokens.hash("raw-token")))
                    .thenReturn(Optional.of(tokenYaConsumido()));

            assertThatThrownBy(() -> service.execute(comandoVerificar("raw-token")))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("already used");

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(employeeEmailVerifier);
        }
    }

    @Nested
    @DisplayName("Verificación exitosa")
    class VerificacionExitosa {

        @Test
        @DisplayName("consume el token, lo persiste y verifica el correo del empleado dueño")
        void consume_persiste_y_verifica_al_empleado() {
            EmailVerificationToken vigente = tokenVigente(VerificationTokens.hash("raw-token"));
            when(tokenRepository.findByTokenHash(VerificationTokens.hash("raw-token")))
                    .thenReturn(Optional.of(vigente));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoVerificar("raw-token"));

            ArgumentCaptor<EmailVerificationToken> guardado = ArgumentCaptor
                    .forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(guardado.capture());
            assertThat(guardado.getValue().getConsumedAt()).isNotNull();
            verify(employeeEmailVerifier).verify(EMPLOYEE_ID);
        }
    }
}
