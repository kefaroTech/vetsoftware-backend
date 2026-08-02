package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Emisión y verificación del access token. Es la pieza que decide quién es el
 * llamante en cada request: si acepta una firma ajena, un token manipulado o
 * uno vencido, toda la autorización posterior (@PreAuthorize, multi-tenant)
 * queda anulada.
 */
class JwtProviderTest {

    private static final String SECRET = "clave-de-pruebas-vetsoftware-de-al-menos-32-bytes-para-hmac-sha256";
    private static final String OTHER_SECRET = "otra-clave-distinta-de-pruebas-vetsoftware-con-32-bytes-o-mas-aqui";

    private final JwtProvider provider = new JwtProvider(SECRET, 15);

    @Nested
    class Emision {

        @Test
        void el_token_emitido_se_lee_de_vuelta_con_sus_claims() {
            String token = provider.generate(7L, "EMPLOYEE", 3L, 5L);

            assertThat(provider.extractId(token)).isEqualTo(7L);
            assertThat(provider.extractType(token)).isEqualTo("EMPLOYEE");
            assertThat(provider.extractAuthVersion(token)).isEqualTo(5L);
        }

        @Test
        void el_token_de_system_user_no_lleva_empresa() {
            String token = provider.generate(1L, "SYSTEM_USER", null, 2L);

            assertThat(provider.extractType(token)).isEqualTo("SYSTEM_USER");
            assertThat(provider.extractId(token)).isEqualTo(1L);
        }

        @Test
        void sin_authVersion_el_claim_queda_ausente() {
            String token = provider.generate(7L, "EMPLOYEE", 3L, null);

            assertThat(provider.extractAuthVersion(token)).isNull();
        }

        @Test
        void es_un_jwt_compacto_de_tres_segmentos() {
            String token = provider.generate(7L, "EMPLOYEE", 3L, 5L);

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        void dos_tokens_del_mismo_sujeto_comparten_identidad() {
            String a = provider.generate(7L, "EMPLOYEE", 3L, 5L);
            String b = provider.generate(7L, "EMPLOYEE", 3L, 5L);

            assertThat(provider.extractId(a)).isEqualTo(provider.extractId(b));
        }
    }

    @Nested
    class Verificacion {

        @Test
        void rechaza_un_token_firmado_con_otra_clave() {
            String foreign = new JwtProvider(OTHER_SECRET, 15).generate(7L, "EMPLOYEE", 3L, 5L);

            assertThatThrownBy(() -> provider.extractId(foreign)).isInstanceOf(JwtException.class);
        }

        @Test
        void rechaza_un_token_con_el_payload_manipulado() {
            String token = provider.generate(7L, "EMPLOYEE", 3L, 5L);
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            String tampered = payload.replace("\"companyId\":3", "\"companyId\":999");
            String forged = parts[0] + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(tampered.getBytes())
                    + "." + parts[2];

            assertThatThrownBy(() -> provider.extractId(forged)).isInstanceOf(JwtException.class);
        }

        @Test
        void rechaza_un_token_sin_firma_alg_none() {
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"none\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"sub\":\"7\",\"type\":\"EMPLOYEE\"}".getBytes());

            assertThatThrownBy(() -> provider.extractId(header + "." + payload + "."))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void rechaza_basura_que_no_es_un_jwt() {
            assertThatThrownBy(() -> provider.extractType("no-es-un-token"))
                    .isInstanceOf(Exception.class);
            assertThatThrownBy(() -> provider.extractType("")).isInstanceOf(Exception.class);
        }

        @Test
        void un_token_vencido_se_distingue_de_uno_invalido() {
            // El borde usa esta diferencia para decidir entre refrescar (TOKEN_EXPIRED) y
            // desloguear.
            JwtProvider expiring = new JwtProvider(SECRET, -1);
            String token = expiring.generate(7L, "EMPLOYEE", 3L, 5L);

            assertThatThrownBy(() -> provider.extractId(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    class FortalezaDeLaClave {

        @Test
        void una_clave_corta_no_permite_arrancar_el_proveedor() {
            // Fail-fast en el arranque: HMAC-SHA256 exige al menos 256 bits de secreto.
            assertThatThrownBy(() -> new JwtProvider("corta", 15))
                    .isInstanceOf(WeakKeyException.class);
        }

        @Test
        void una_clave_de_exactamente_32_bytes_si_es_aceptada() {
            String key32 = "k".repeat(32);

            assertThat(key32).hasSize(32);
            assertThat(new JwtProvider(key32, 15).generate(1L, "EMPLOYEE", 1L, 1L)).isNotBlank();
        }
    }

    @Nested
    class VigenciaConfigurable {

        @Test
        void la_expiracion_sale_de_la_configuracion_no_de_una_constante() {
            String corto = new JwtProvider(SECRET, -1).generate(7L, "EMPLOYEE", 3L, 5L);
            String largo = new JwtProvider(SECRET, 60).generate(7L, "EMPLOYEE", 3L, 5L);

            assertThatThrownBy(() -> provider.extractId(corto))
                    .isInstanceOf(ExpiredJwtException.class);
            assertThat(provider.extractId(largo)).isEqualTo(7L);
        }
    }
}
