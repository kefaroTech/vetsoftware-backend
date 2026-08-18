package com.vetsoftware.app.passwordreset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PasswordResetToken — invariantes y ciclo de vida de un token de un solo uso")
class PasswordResetTokenTest {

    private static final Long EMPLOYEE_ID = 500L;
    private static final Long COMPANY_ID = 9L;
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final LocalDateTime FUTURO = AHORA.plusHours(1);
    private static final LocalDateTime PASADO = AHORA.minusHours(1);

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, FUTURO, null);

            assertThat(token.getId()).isEqualTo(1L);
            assertThat(token.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(token.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(token.getExpiresAt()).isEqualTo(FUTURO);
            assertThat(token.getConsumedAt()).isNull();
        }

        @Test
        @DisplayName("issue() nace sin id y sin consumir")
        void issue_nace_sin_id_y_sin_consumir() {
            PasswordResetToken token = PasswordResetToken.issue(EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH,
                    FUTURO);

            assertThat(token.getId()).isNull();
            assertThat(token.getConsumedAt()).isNull();
            assertThat(token.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(token.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("employeeId nulo: un token siempre pertenece a alguien")
        void employee_id_nulo() {
            assertThatThrownBy(
                    () -> new PasswordResetToken(1L, null, COMPANY_ID, TOKEN_HASH, FUTURO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeeId is required");
        }

        @Test
        @DisplayName("companyId nulo")
        void company_id_nulo() {
            assertThatThrownBy(
                    () -> new PasswordResetToken(1L, EMPLOYEE_ID, null, TOKEN_HASH, FUTURO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("tokenHash nulo")
        void token_hash_nulo() {
            assertThatThrownBy(
                    () -> new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, null, FUTURO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tokenHash is required");
        }

        @ParameterizedTest(name = "tokenHash invalido: [{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("tokenHash vacio o en blanco")
        void token_hash_vacio_o_en_blanco(String hashInvalido) {
            assertThatThrownBy(() -> new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    hashInvalido, FUTURO, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tokenHash is required");
        }

        @Test
        @DisplayName("expiresAt nulo: un token sin vencimiento nunca caduca")
        void expires_at_nulo() {
            assertThatThrownBy(() -> new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH,
                    null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expiresAt is required");
        }
    }

    @Nested
    @DisplayName("isUsable — seguridad: consumido y expirado nunca vuelven a ser usables")
    class Usable {

        @Test
        @DisplayName("no consumido y no expirado: usable")
        void no_consumido_y_no_expirado_es_usable() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, FUTURO, null);

            assertThat(token.isUsable(AHORA)).isTrue();
        }

        @Test
        @DisplayName("el instante exacto de expiracion todavia es usable (isAfter es estricto)")
        void el_instante_exacto_de_expiracion_es_usable() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, AHORA, null);

            assertThat(token.isUsable(AHORA)).isTrue();
        }

        @Test
        @DisplayName("token expirado no es usable")
        void token_expirado_no_es_usable() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, PASADO, null);

            assertThat(token.isUsable(AHORA)).isFalse();
        }

        @Test
        @DisplayName("token ya consumido no es usable, aunque no haya expirado")
        void token_consumido_no_es_usable() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, FUTURO, AHORA);

            assertThat(token.isUsable(AHORA)).isFalse();
        }
    }

    @Nested
    @DisplayName("consume — de un solo uso")
    class Consume {

        @Test
        @DisplayName("consume un token vigente y fija consumedAt")
        void consume_un_token_vigente() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, FUTURO, null);

            token.consume(AHORA);

            assertThat(token.getConsumedAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("un token ya usado no se puede volver a consumir")
        void un_token_ya_usado_no_se_puede_reusar() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, FUTURO, PASADO);

            assertThatThrownBy(() -> token.consume(AHORA))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("already used");
        }

        @Test
        @DisplayName("un token expirado no se puede consumir")
        void un_token_expirado_no_se_puede_consumir() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, PASADO, null);

            assertThatThrownBy(() -> token.consume(AHORA))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("consumir en el instante exacto de expiracion todavia es valido")
        void consumir_en_el_instante_exacto_de_expiracion_es_valido() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, AHORA, null);

            assertThatCode(() -> token.consume(AHORA)).doesNotThrowAnyException();
            assertThat(token.getConsumedAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("un consume invalido no deja el token a medias: consumedAt no cambia")
        void un_consume_invalido_no_deja_el_token_a_medias() {
            PasswordResetToken token = new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, PASADO, null);

            assertThatThrownBy(() -> token.consume(AHORA))
                    .isInstanceOf(InvalidPasswordResetTokenException.class);

            assertThat(token.getConsumedAt()).isNull();
        }
    }
}
