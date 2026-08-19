package com.vetsoftware.app.dianprovider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DianProviderConfig — invariantes y ciclo de vida del agregado")
class DianProviderConfigTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir quince
     * argumentos en cada escenario invalido.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = DianProviderConfigMother.CONFIG_ID;
        private CompanyRef company = DianProviderConfigMother.CLINICA;
        private ProviderType provider = ProviderType.MATIAS;
        private String baseUrl = "https://api.matias.test";
        private String clientId = "client-id";
        private String clientSecret = "client-secret";
        private String username = "user@test.com";
        private String password = "secret-pass";
        private String apiToken;
        private String webhookSecret = "webhook-secret";
        private String accessToken;
        private LocalDateTime tokenExpiresAt;
        private String numberingProviderRef = "RES-001";
        private LocalDateTime createdDate = DianProviderConfigMother.CREADO;
        private boolean enabled = true;

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder provider(ProviderType v) {
            this.provider = v;
            return this;
        }

        private Builder baseUrl(String v) {
            this.baseUrl = v;
            return this;
        }

        private DianProviderConfig build() {
            return new DianProviderConfig(id, company, provider, baseUrl, clientId, clientSecret,
                    username, password, apiToken, webhookSecret, accessToken, tokenExpiresAt,
                    numberingProviderRef, createdDate, null, enabled);
        }

        private void applyUpdateTo(DianProviderConfig config) {
            config.update(provider, baseUrl, clientId, clientSecret, username, password, apiToken,
                    webhookSecret, numberingProviderRef);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            DianProviderConfig config = valido().build();

            assertThat(config.getId()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(config.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
            assertThat(config.getProvider()).isEqualTo(ProviderType.MATIAS);
            assertThat(config.getBaseUrl()).isEqualTo("https://api.matias.test");
            assertThat(config.getClientId()).isEqualTo("client-id");
            assertThat(config.getClientSecret()).isEqualTo("client-secret");
            assertThat(config.getUsername()).isEqualTo("user@test.com");
            assertThat(config.getPassword()).isEqualTo("secret-pass");
            assertThat(config.getApiToken()).isNull();
            assertThat(config.getWebhookSecret()).isEqualTo("webhook-secret");
            assertThat(config.getAccessToken()).isNull();
            assertThat(config.getTokenExpiresAt()).isNull();
            assertThat(config.getNumberingProviderRef()).isEqualTo("RES-001");
            assertThat(config.getCreatedDate()).isEqualTo(DianProviderConfigMother.CREADO);
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            DianProviderConfig config = DianProviderConfig.create(DianProviderConfigMother.CLINICA,
                    ProviderType.MATIAS, "https://api.matias.test", "client-id", "client-secret",
                    "user@test.com", "secret-pass", null, "webhook-secret", "RES-001");

            assertThat(config.getId()).isNull();
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getAccessToken()).isNull();
            assertThat(config.getTokenExpiresAt()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(config.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("una config autenticada solo por PAT no exige credenciales de login")
        void una_config_autenticada_solo_por_pat_no_exige_credenciales_de_login() {
            DianProviderConfig config = DianProviderConfigMother.configConApiToken();

            assertThat(config.getApiToken()).isEqualTo("static-pat-token");
            assertThat(config.getUsername()).isNull();
            assertThat(config.getPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(arguments("company null",
                    (ThrowingCallable) () -> valido().company(null).build(), "company is required"),
                    arguments("provider null",
                            (ThrowingCallable) () -> valido().provider(null).build(),
                            "provider is required"),
                    arguments("baseUrl null",
                            (ThrowingCallable) () -> valido().baseUrl(null).build(),
                            "baseUrl is required"),
                    arguments("baseUrl vacio",
                            (ThrowingCallable) () -> valido().baseUrl("").build(),
                            "baseUrl is required"),
                    arguments("baseUrl en blanco",
                            (ThrowingCallable) () -> valido().baseUrl("   ").build(),
                            "baseUrl is required"),
                    arguments("baseUrl de 256 chars",
                            (ThrowingCallable) () -> valido().baseUrl("x".repeat(256)).build(),
                            "baseUrl must be 255 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("baseUrl de 255 chars, el limite exacto, se acepta")
        void base_url_en_el_limite_exacto_se_acepta() {
            assertThatCode(() -> valido().baseUrl("x".repeat(255)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id, company y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_company_y_created_date() {
            DianProviderConfig config = valido().build();

            config.update(ProviderType.MATIAS, "https://api.matias.test/v2", "otro-client-id",
                    "otro-client-secret", "otro@test.com", "otro-pass", "pat-nuevo",
                    "otro-webhook-secret", "RES-999");

            assertThat(config.getBaseUrl()).isEqualTo("https://api.matias.test/v2");
            assertThat(config.getClientId()).isEqualTo("otro-client-id");
            assertThat(config.getClientSecret()).isEqualTo("otro-client-secret");
            assertThat(config.getUsername()).isEqualTo("otro@test.com");
            assertThat(config.getPassword()).isEqualTo("otro-pass");
            assertThat(config.getApiToken()).isEqualTo("pat-nuevo");
            assertThat(config.getWebhookSecret()).isEqualTo("otro-webhook-secret");
            assertThat(config.getNumberingProviderRef()).isEqualTo("RES-999");
            assertThat(config.getId()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(config.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
            assertThat(config.getCreatedDate()).isEqualTo(DianProviderConfigMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            DianProviderConfig config = valido().build();

            // La url es invalida: si validate() no corriera ANTES de asignar, el
            // clientId nuevo quedaria pegado a una baseUrl vacia.
            assertThatThrownBy(() -> config.update(ProviderType.MATIAS, "  ", "otro-client-id",
                    "otro-secret", "otro@test.com", "otro-pass", null, "otro-webhook", "RES-999"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(config.getClientId()).isEqualTo("client-id");
            assertThat(config.getBaseUrl()).isEqualTo("https://api.matias.test");
        }

        @Test
        @DisplayName("no puede cambiar la empresa duena de la config")
        void no_puede_cambiar_la_empresa_duena_de_la_config() {
            DianProviderConfig config = valido().build();

            config.update(ProviderType.MATIAS, "https://api.matias.test/v2", "client-id",
                    "client-secret", "user@test.com", "secret-pass", null, "webhook-secret",
                    "RES-001");

            assertThat(config.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
        }
    }

    @Nested
    @DisplayName("cache del token")
    class CacheDelToken {

        @Test
        @DisplayName("cacheToken guarda el token vigente y su expiracion")
        void cache_token_guarda_el_token_vigente_y_su_expiracion() {
            DianProviderConfig config = valido().build();
            LocalDateTime expira = LocalDateTime.of(2026, 6, 1, 12, 0);

            config.cacheToken("nuevo-access-token", expira);

            assertThat(config.getAccessToken()).isEqualTo("nuevo-access-token");
            assertThat(config.getTokenExpiresAt()).isEqualTo(expira);
        }

        @Test
        @DisplayName("un cacheToken nuevo sobrescribe el anterior")
        void un_cache_token_nuevo_sobrescribe_el_anterior() {
            DianProviderConfig config = DianProviderConfigMother.configConTokenCacheado();
            LocalDateTime nuevaExpiracion = LocalDateTime.of(2026, 7, 1, 8, 0);

            config.cacheToken("token-rotado", nuevaExpiracion);

            assertThat(config.getAccessToken()).isEqualTo("token-rotado");
            assertThat(config.getTokenExpiresAt()).isEqualTo(nuevaExpiracion);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("enable y disable alternan el estado y son idempotentes")
        void enable_y_disable_alternan_el_estado_y_son_idempotentes() {
            DianProviderConfig config = valido().build();

            config.disable();
            assertThat(config.isEnabled()).isFalse();
            config.disable();
            assertThat(config.isEnabled()).isFalse();

            config.enable();
            assertThat(config.isEnabled()).isTrue();
            config.enable();
            assertThat(config.isEnabled()).isTrue();
        }
    }
}
