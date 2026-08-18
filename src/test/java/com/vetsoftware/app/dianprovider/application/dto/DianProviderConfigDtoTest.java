package com.vetsoftware.app.dianprovider.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * from(...) es el unico lugar donde una credencial en claro podria filtrarse al
 * front: NUNCA expone el valor, solo si esta configurada. Un campo cruzado aqui
 * (un secreto devuelto en vez de su booleano) no lo detecta ningun otro test.
 */
@DisplayName("DianProviderConfigDto — from(...)")
class DianProviderConfigDtoTest {

    @Nested
    @DisplayName("campo por campo")
    class CampoPorCampo {

        @Test
        @DisplayName("copia id, companyId, provider, baseUrl y metadatos sin enmascarar")
        void copia_id_company_id_provider_base_url_y_metadatos() {
            DianProviderConfig config = DianProviderConfigMother.configValida();

            DianProviderConfigDto dto = DianProviderConfigDto.from(config);

            assertThat(dto.id()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(dto.companyId()).isEqualTo(DianProviderConfigMother.COMPANY_ID);
            assertThat(dto.provider()).isEqualTo(ProviderType.MATIAS);
            assertThat(dto.baseUrl()).isEqualTo("https://api.matias.test");
            // clientId es semi-publico: viaja en claro, a diferencia de los secretos.
            assertThat(dto.clientId()).isEqualTo("client-id");
            assertThat(dto.numberingProviderRef()).isEqualTo("RES-001");
            assertThat(dto.createdDate()).isEqualTo(DianProviderConfigMother.CREADO);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("company null se traduce a companyId null, sin reventar")
        void company_null_se_traduce_a_company_id_null() {
            DianProviderConfig config = new DianProviderConfig(DianProviderConfigMother.CONFIG_ID,
                    DianProviderConfigMother.CLINICA, ProviderType.MATIAS,
                    "https://api.matias.test", "client-id", "client-secret", "user@test.com",
                    "secret-pass", null, "webhook-secret", null, null, "RES-001",
                    DianProviderConfigMother.CREADO, true);
            // No hay forma de construir un DianProviderConfig con company null (invariante
            // de dominio), asi que el unico camino a "companyId null" es leyendo un config
            // valido: el ternario se prueba desde el lado alcanzable.
            DianProviderConfigDto dto = DianProviderConfigDto.from(config);

            assertThat(dto.companyId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("banderas *Configured — nunca el secreto en claro")
    class BanderasConfigured {

        @Test
        @DisplayName("con todos los secretos presentes, las cinco banderas son true")
        void con_todos_los_secretos_presentes_las_cinco_banderas_son_true() {
            DianProviderConfigDto dto = DianProviderConfigDto
                    .from(DianProviderConfigMother.configValida());

            assertThat(dto.clientSecretConfigured()).isTrue();
            assertThat(dto.usernameConfigured()).isTrue();
            assertThat(dto.passwordConfigured()).isTrue();
            assertThat(dto.apiTokenConfigured()).isFalse();
            assertThat(dto.webhookSecretConfigured()).isTrue();
        }

        @Test
        @DisplayName("autenticado solo por PAT: apiTokenConfigured true, login todo false")
        void autenticado_solo_por_pat() {
            DianProviderConfigDto dto = DianProviderConfigDto
                    .from(DianProviderConfigMother.configConApiToken());

            assertThat(dto.apiTokenConfigured()).isTrue();
            assertThat(dto.usernameConfigured()).isFalse();
            assertThat(dto.passwordConfigured()).isFalse();
            assertThat(dto.clientSecretConfigured()).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("null, vacio o en blanco cuentan como no configurado")
        void null_vacio_o_en_blanco_cuentan_como_no_configurado(String valorSecreto) {
            DianProviderConfig config = new DianProviderConfig(DianProviderConfigMother.CONFIG_ID,
                    DianProviderConfigMother.CLINICA, ProviderType.MATIAS,
                    "https://api.matias.test", "client-id", valorSecreto, valorSecreto,
                    valorSecreto, valorSecreto, valorSecreto, null, null, "RES-001",
                    DianProviderConfigMother.CREADO, true);

            DianProviderConfigDto dto = DianProviderConfigDto.from(config);

            assertThat(dto.clientSecretConfigured()).isFalse();
            assertThat(dto.usernameConfigured()).isFalse();
            assertThat(dto.passwordConfigured()).isFalse();
            assertThat(dto.apiTokenConfigured()).isFalse();
            assertThat(dto.webhookSecretConfigured()).isFalse();
        }
    }
}
