package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * {@code CompanyJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete; no tiene logica
 * propia, es un portador de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DianProviderConfigJpaMapper")
class DianProviderConfigJpaMapperTest {

    private final DianProviderConfigJpaMapper mapper = new DianProviderConfigJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private DianProviderConfigJpaEntity entidadCompleta() {
        DianProviderConfigJpaEntity entity = new DianProviderConfigJpaEntity();
        entity.setId(DianProviderConfigMother.CONFIG_ID);
        entity.setProvider(ProviderType.MATIAS);
        entity.setBaseUrl("https://api.matias.test");
        entity.setClientId("client-id");
        entity.setClientSecret("client-secret");
        entity.setUsername("user@test.com");
        entity.setPassword("secret-pass");
        entity.setApiToken(null);
        entity.setWebhookSecret("webhook-secret");
        entity.setAccessToken(null);
        entity.setTokenExpiresAt(null);
        entity.setNumberingProviderRef("RES-001");
        entity.setCreatedDate(DianProviderConfigMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            DianProviderConfig config = DianProviderConfigMother.configValida();

            DianProviderConfigJpaEntity entity = mapper.toJpa(config, companyEntity);

            assertThat(entity.getId()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(entity.getProvider()).isEqualTo(ProviderType.MATIAS);
            assertThat(entity.getBaseUrl()).isEqualTo("https://api.matias.test");
            assertThat(entity.getClientId()).isEqualTo("client-id");
            assertThat(entity.getClientSecret()).isEqualTo("client-secret");
            assertThat(entity.getUsername()).isEqualTo("user@test.com");
            assertThat(entity.getPassword()).isEqualTo("secret-pass");
            assertThat(entity.getApiToken()).isNull();
            assertThat(entity.getWebhookSecret()).isEqualTo("webhook-secret");
            assertThat(entity.getAccessToken()).isNull();
            assertThat(entity.getTokenExpiresAt()).isNull();
            assertThat(entity.getNumberingProviderRef()).isEqualTo("RES-001");
            assertThat(entity.getCreatedDate()).isEqualTo(DianProviderConfigMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha la asociacion de empresa en su slot")
        void engancha_la_asociacion_de_empresa_en_su_slot() {
            DianProviderConfigJpaEntity entity = mapper
                    .toJpa(DianProviderConfigMother.configValida(), companyEntity);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("arrastra el token de acceso y su expiracion cuando estan cacheados")
        void arrastra_el_token_de_acceso_cuando_esta_cacheado() {
            DianProviderConfig config = DianProviderConfigMother.configConTokenCacheado();

            DianProviderConfigJpaEntity entity = mapper.toJpa(config, companyEntity);

            assertThat(entity.getAccessToken()).isEqualTo("cached-access-token");
            assertThat(entity.getTokenExpiresAt()).isEqualTo(DianProviderConfigMother.TOKEN_EXPIRA);
        }
    }

    @Nested
    @DisplayName("toDomain con CompanyRef precargado — camino de escritura")
    class ToDomainConRef {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getCompany(), Hibernate lanzaria un SELECT extra por save.
            DianProviderConfig config = mapper.toDomain(entidadCompleta(),
                    DianProviderConfigMother.CLINICA);

            assertThat(config.getId()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(config.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
            assertThat(config.getProvider()).isEqualTo(ProviderType.MATIAS);
            assertThat(config.getBaseUrl()).isEqualTo("https://api.matias.test");
            assertThat(config.getCreatedDate()).isEqualTo(DianProviderConfigMother.CREADO);
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio a entidad a dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            DianProviderConfig original = DianProviderConfigMother.configValida();

            DianProviderConfigJpaEntity entity = mapper.toJpa(original, companyEntity);
            DianProviderConfig vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el CompanyRef desde la asociacion cargada")
        void construye_el_company_ref_desde_la_asociacion() {
            when(companyEntity.getId()).thenReturn(DianProviderConfigMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(DianProviderConfigMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(DianProviderConfigMother.CLINICA.identifier());
            DianProviderConfigJpaEntity entity = entidadCompleta();
            entity.setCompany(companyEntity);

            DianProviderConfig config = mapper.toDomain(entity);

            assertThat(config.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
        }

        @Test
        @DisplayName("una asociacion de empresa ausente propaga la invariante del dominio")
        void una_asociacion_de_empresa_ausente_propaga_la_invariante() {
            // company_id es NOT NULL en el schema real: este camino no ocurre en
            // produccion, pero fija que el mapper no oculta el problema devolviendo un
            // agregado a medias — deja que el dominio lo rechace.
            DianProviderConfigJpaEntity entity = entidadCompleta();
            entity.setCompany(null);

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }
    }
}
