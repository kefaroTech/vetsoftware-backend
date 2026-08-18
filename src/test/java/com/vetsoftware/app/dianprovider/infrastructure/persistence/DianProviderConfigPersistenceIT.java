package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la config DIAN contra MySQL real.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: la unicidad de
 * {@code company_id} (0-o-1 config por empresa) la impone un indice de la base,
 * y el cifrado AES-GCM de las columnas de credenciales lo aplica Hibernate al
 * hacer flush a traves de {@code EncryptedStringConverter}. Con un doble del
 * repositorio las dos cosas darian verde siempre.
 */
@Import({JpaDianProviderConfigRepository.class, DianProviderConfigJpaMapper.class})
@DisplayName("JpaDianProviderConfigRepository — persistencia, unicidad y cifrado contra MySQL real")
class DianProviderConfigPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaDianProviderConfigRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private DianProviderConfig nuevaConfig() {
        CompanyRef company = new CompanyRef(SchemaSeed.COMPANY_ID, "placeholder", "placeholder");
        return DianProviderConfig.create(company, ProviderType.MATIAS, "https://api.matias.test",
                "client-id", "client-secret", "user@test.com", "secret-pass", null,
                "webhook-secret", "RES-001");
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y findByCompanyId recupera los campos persistidos")
        void guardar_asigna_id_y_find_by_company_id_recupera_los_campos() {
            DianProviderConfig guardado = repository.save(nuevaConfig());

            assertThat(guardado.getId()).isNotNull();

            DianProviderConfig leido = repository.findByCompanyId(SchemaSeed.COMPANY_ID)
                    .orElseThrow();
            assertThat(leido.getBaseUrl()).isEqualTo("https://api.matias.test");
            assertThat(leido.getProvider()).isEqualTo(ProviderType.MATIAS);
            assertThat(leido.getClientId()).isEqualTo("client-id");
            assertThat(leido.getClientSecret()).isEqualTo("client-secret");
            assertThat(leido.getUsername()).isEqualTo("user@test.com");
            assertThat(leido.getPassword()).isEqualTo("secret-pass");
            assertThat(leido.getWebhookSecret()).isEqualTo("webhook-secret");
            assertThat(leido.getNumberingProviderRef()).isEqualTo("RES-001");
            // el CompanyRef leido viene de la fila real de companies (el que se paso al
            // crear no se persiste: la FK es lo unico que se guarda).
            assertThat(leido.getCompany().id()).isEqualTo(SchemaSeed.COMPANY_ID);
            assertThat(leido.getCompany().name()).isEqualTo("Veterinaria de prueba");
        }

        @Test
        @DisplayName("una empresa sin config no devuelve nada")
        void una_empresa_sin_config_no_devuelve_nada() {
            assertThat(repository.findByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("las credenciales se guardan cifradas en la columna, no en claro")
        void las_credenciales_se_guardan_cifradas_en_la_columna() {
            repository.save(nuevaConfig());
            entityManager.flush();
            entityManager.clear();

            Object crudo = entityManager.createNativeQuery(
                    "select credential_password from dian_provider_configs where company_id = :cid")
                    .setParameter("cid", SchemaSeed.COMPANY_ID).getSingleResult();

            // Si el converter no aplicara, la columna tendria el texto plano.
            assertThat(crudo).isNotEqualTo("secret-pass");
        }
    }

    @Nested
    @DisplayName("0-o-1 config por empresa")
    class UnicidadPorEmpresa {

        @Test
        @DisplayName("una segunda config para la misma empresa la rechaza la base")
        void una_segunda_config_para_la_misma_empresa_la_rechaza_la_base() {
            repository.save(nuevaConfig());

            // Dos filas de config para la misma empresa dejarian ambiguo cual usa el
            // caso de uso de creacion (que ya valida en memoria, pero sin el indice
            // unico una carrera entre dos requests concurrentes coleria las dos).
            assertThatThrownBy(() -> {
                repository.save(nuevaConfig());
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("otra empresa si puede tener su propia config")
        void otra_empresa_si_puede_tener_su_propia_config() {
            repository.save(nuevaConfig());
            DianProviderConfig configOtraEmpresa = DianProviderConfig.create(
                    new CompanyRef(SchemaSeed.OTRA_COMPANY_ID, "placeholder", "placeholder"),
                    ProviderType.MATIAS, "https://api.matias.test/otra", "cid", "csec", "user2",
                    "pass2", null, "whs2", "RES-002");

            repository.save(configOtraEmpresa);

            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID).orElseThrow().getBaseUrl())
                    .isEqualTo("https://api.matias.test");
            assertThat(repository.findByCompanyId(SchemaSeed.OTRA_COMPANY_ID).orElseThrow()
                    .getBaseUrl()).isEqualTo("https://api.matias.test/otra");
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("guardar la fila leida tras un update no crea una fila nueva")
        void guardar_la_fila_leida_tras_un_update_no_crea_una_fila_nueva() {
            DianProviderConfig guardado = repository.save(nuevaConfig());

            DianProviderConfig recargado = repository.findByCompanyId(SchemaSeed.COMPANY_ID)
                    .orElseThrow();
            recargado.update(ProviderType.MATIAS, "https://api.matias.test/v2", "cid-2", "csec-2",
                    "user2@test.com", "pass-2", null, "whs-2", "RES-002");
            DianProviderConfig actualizado = repository.save(recargado);

            assertThat(actualizado.getId()).isEqualTo(guardado.getId());
            DianProviderConfig releido = repository.findByCompanyId(SchemaSeed.COMPANY_ID)
                    .orElseThrow();
            assertThat(releido.getId()).isEqualTo(guardado.getId());
            assertThat(releido.getBaseUrl()).isEqualTo("https://api.matias.test/v2");
            assertThat(releido.getNumberingProviderRef()).isEqualTo("RES-002");
        }
    }
}
