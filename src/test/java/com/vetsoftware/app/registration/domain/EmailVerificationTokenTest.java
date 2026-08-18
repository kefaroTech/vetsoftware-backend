package com.vetsoftware.app.registration.domain;

import static com.vetsoftware.app.registration.testsupport.RegistrationMother.COMPANY_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.EMITIDO;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.EMPLOYEE_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.TOKEN_HASH;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.VIGENTE_HASTA;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenExpirado;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenVigente;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenYaConsumido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmailVerificationToken")
class EmailVerificationTokenTest {

    @Nested
    @DisplayName("Invariantes de construcción")
    class Invariantes {

        @Test
        @DisplayName("sin employeeId no se puede construir")
        void sin_employee_id_no_se_puede_construir() {
            assertThatThrownBy(() -> new EmailVerificationToken(null, null, COMPANY_ID, TOKEN_HASH,
                    VIGENTE_HASTA, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeeId is required");
        }

        @Test
        @DisplayName("sin companyId no se puede construir")
        void sin_company_id_no_se_puede_construir() {
            assertThatThrownBy(() -> new EmailVerificationToken(null, EMPLOYEE_ID, null, TOKEN_HASH,
                    VIGENTE_HASTA, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("con el hash en blanco no se puede construir")
        void con_hash_en_blanco_no_se_puede_construir() {
            assertThatThrownBy(() -> new EmailVerificationToken(null, EMPLOYEE_ID, COMPANY_ID, "  ",
                    VIGENTE_HASTA, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tokenHash is required");
        }

        @Test
        @DisplayName("sin fecha de expiración no se puede construir")
        void sin_expires_at_no_se_puede_construir() {
            assertThatThrownBy(() -> new EmailVerificationToken(null, EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expiresAt is required");
        }
    }

    @Nested
    @DisplayName("Emisión")
    class Emision {

        @Test
        @DisplayName("issue emite un token nuevo, sin id y sin consumir")
        void issue_emite_un_token_nuevo_sin_id_ni_consumo() {
            EmailVerificationToken emitido = EmailVerificationToken.issue(EMPLOYEE_ID, COMPANY_ID,
                    TOKEN_HASH, VIGENTE_HASTA);

            assertThat(emitido.getId()).isNull();
            assertThat(emitido.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(emitido.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(emitido.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(emitido.getExpiresAt()).isEqualTo(VIGENTE_HASTA);
            assertThat(emitido.getConsumedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Consumo")
    class Consumo {

        @Test
        @DisplayName("consume marca el token como usado en el instante recibido")
        void consume_marca_el_token_como_usado() {
            EmailVerificationToken vigente = tokenVigente();

            vigente.consume(EMITIDO);

            assertThat(vigente.getConsumedAt()).isEqualTo(EMITIDO);
        }

        @Test
        @DisplayName("consumir justo en el instante de expiración todavía es válido")
        void consumir_en_el_instante_exacto_de_expiracion_es_valido() {
            EmailVerificationToken vigente = tokenVigente();

            vigente.consume(VIGENTE_HASTA);

            assertThat(vigente.getConsumedAt()).isEqualTo(VIGENTE_HASTA);
        }

        @Test
        @DisplayName("un token ya consumido no se puede volver a consumir")
        void un_token_ya_consumido_no_se_puede_reconsumir() {
            EmailVerificationToken usado = tokenYaConsumido();

            assertThatThrownBy(() -> usado.consume(EMITIDO))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("already used");
        }

        @Test
        @DisplayName("un token expirado no se puede consumir")
        void un_token_expirado_no_se_puede_consumir() {
            EmailVerificationToken expirado = tokenExpirado();

            assertThatThrownBy(() -> expirado.consume(EMITIDO))
                    .isInstanceOf(InvalidVerificationTokenException.class)
                    .hasMessageContaining("expired");
        }
    }
}
