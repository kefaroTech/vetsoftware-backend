package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del adaptador de retenciones contra MySQL real.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: el {@code unique} de
 * {@code company_id} lo impone un indice de la base (0-o-1 fila por company) y
 * el {@code @EntityGraph} de {@code findByCompany_Id} es lo que evita el N+1 al
 * hidratar el {@code CompanyRef}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaWithholdingConfigRepository — upsert 0-o-1 por company contra MySQL real")
class WithholdingConfigPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaWithholdingConfigRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private static WithholdingConfig configPara(Long companyId, String rf, String ri, String ria) {
        CompanyRef company = new CompanyRef(companyId, "Veterinaria de prueba", "900123456");
        return WithholdingConfig.create(company, new BigDecimal(rf), new BigDecimal(ri),
                new BigDecimal(ria));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer conserva las tres tasas")
        void guardar_asigna_id_y_releer_conserva_las_tasas() {
            WithholdingConfig guardado = repository.save(configPara(COMPANY, "2.5", "15.0", "1.0"));

            assertThat(guardado.getId()).isNotNull();

            WithholdingConfig leido = repository.findByCompanyId(COMPANY).orElseThrow();
            assertThat(leido.getReteFuenteRate()).isEqualByComparingTo("2.5");
            assertThat(leido.getReteIvaRate()).isEqualByComparingTo("15.0");
            assertThat(leido.getReteIcaRate()).isEqualByComparingTo("1.0");
            assertThat(leido.getCompany().id()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("una company sin configuracion no devuelve nada")
        void una_company_sin_configuracion_no_devuelve_nada() {
            assertThat(repository.findByCompanyId(OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("una configuracion por company")
    class UnaPorCompany {

        @Test
        @DisplayName("guardar sobre la fila leida actualiza en vez de duplicar")
        void guardar_sobre_la_fila_leida_actualiza_en_vez_de_duplicar() {
            WithholdingConfig original = repository.save(configPara(COMPANY, "2.5", "15.0", "1.0"));

            WithholdingConfig releido = repository.findByCompanyId(COMPANY).orElseThrow();
            releido.update(new BigDecimal("3.0"), new BigDecimal("16.0"), new BigDecimal("2.0"));
            WithholdingConfig actualizado = repository.save(releido);

            assertThat(actualizado.getId()).isEqualTo(original.getId());
            assertThat(repository.findByCompanyId(COMPANY).orElseThrow().getReteFuenteRate())
                    .isEqualByComparingTo("3.0");
        }

        @Test
        @DisplayName("cada company tiene su propia fila")
        void cada_company_tiene_su_propia_fila() {
            repository.save(configPara(COMPANY, "2.5", "15.0", "1.0"));
            repository.save(configPara(OTRA_COMPANY, "4.0", "18.0", "3.0"));

            assertThat(repository.findByCompanyId(COMPANY).orElseThrow().getReteFuenteRate())
                    .isEqualByComparingTo("2.5");
            assertThat(repository.findByCompanyId(OTRA_COMPANY).orElseThrow().getReteFuenteRate())
                    .isEqualByComparingTo("4.0");
        }

        @Test
        @DisplayName("insertar dos configuraciones nuevas para la misma company lo rechaza la base")
        void insertar_dos_nuevas_para_la_misma_company_lo_rechaza_la_base() {
            repository.save(configPara(COMPANY, "2.5", "15.0", "1.0"));

            assertThatThrownBy(() -> {
                repository.save(configPara(COMPANY, "9.0", "9.0", "9.0"));
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
