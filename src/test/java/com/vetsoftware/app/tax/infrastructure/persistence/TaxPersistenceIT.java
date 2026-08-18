package com.vetsoftware.app.tax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxScheme;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del impuesto contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> El
 * {@code @SQLDelete}/{@code @SQLRestriction} que convierte el borrado en un
 * {@code enabled = false} invisible, la reactivacion por UPDATE nativo con su
 * condicion {@code company_id}, y la unicidad de nombre por empresa (columna
 * {@code name} + constraint) los verifica el motor, no el mapper. Con un doble
 * el contrato lo definiria el propio test.
 */
@Import({JpaTaxRepository.class, TaxJpaMapper.class})
@DisplayName("JpaTaxRepository — impuestos contra MySQL real")
class TaxPersistenceIT extends AbstractDataJpaTest {

    private static final CompanyRef EMPRESA = new CompanyRef(SchemaSeed.COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(SchemaSeed.OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    @Autowired
    private JpaTaxRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private Tax guardar(CompanyRef empresa, String name, BigDecimal percentage, TaxScheme scheme) {
        return repository.save(Tax.create(name, percentage, scheme, empresa));
    }

    private Tax guardar(String name) {
        return guardar(EMPRESA, name, new BigDecimal("19.00"), TaxScheme.IVA);
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo, incluida la empresa")
        void guardar_y_releer_conserva_cada_campo() {
            Tax guardado = guardar(EMPRESA, "IVA General", new BigDecimal("19.00"), TaxScheme.IVA);
            releerDesdeLaBase();

            Tax leido = repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID)
                    .orElseThrow();

            assertThat(leido.getName()).isEqualTo("IVA General");
            assertThat(leido.getPercentage()).isEqualByComparingTo("19.00");
            assertThat(leido.getTaxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el save devuelve el id generado")
        void el_save_devuelve_el_id_generado() {
            Tax guardado = guardar("IVA General");

            assertThat(guardado.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("esquema tributario")
    class EsquemaTributario {

        @ParameterizedTest
        @EnumSource(TaxScheme.class)
        @DisplayName("cada esquema se relee tal cual desde la columna VARCHAR")
        void cada_esquema_se_relee_tal_cual(TaxScheme scheme) {
            Tax guardado = guardar(EMPRESA, "Impuesto " + scheme, new BigDecimal("10.00"), scheme);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .map(Tax::getTaxScheme).contains(scheme);
        }
    }

    @Nested
    @DisplayName("unicidad de nombre por empresa")
    class UnicidadDeNombre {

        @Test
        @DisplayName("existsByCompanyIdAndName detecta un nombre activo repetido en la empresa")
        void detecta_un_nombre_repetido_en_la_empresa() {
            guardar("IVA General");
            releerDesdeLaBase();

            assertThat(repository.existsByCompanyIdAndName(SchemaSeed.COMPANY_ID, "IVA General"))
                    .isTrue();
        }

        @Test
        @DisplayName("el mismo nombre en otra empresa no cuenta como repetido")
        void el_mismo_nombre_en_otra_empresa_no_cuenta() {
            guardar(OTRA_EMPRESA, "IVA General", new BigDecimal("19.00"), TaxScheme.IVA);
            releerDesdeLaBase();

            assertThat(repository.existsByCompanyIdAndName(SchemaSeed.COMPANY_ID, "IVA General"))
                    .isFalse();
        }

        @Test
        @DisplayName("existsByCompanyIdAndNameExcludingId no cuenta el propio impuesto")
        void existsByCompanyIdAndNameExcludingId_no_cuenta_el_propio_impuesto() {
            Tax propio = guardar("IVA General");
            releerDesdeLaBase();

            assertThat(repository.existsByCompanyIdAndNameExcludingId(SchemaSeed.COMPANY_ID,
                    "IVA General", propio.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("el listado por empresa no mezcla los del tenant ajeno")
        void el_listado_no_mezcla_los_del_tenant_ajeno() {
            Tax propio = guardar("IVA General");
            guardar(OTRA_EMPRESA, "IVA Ajeno", new BigDecimal("19.00"), TaxScheme.IVA);
            releerDesdeLaBase();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).extracting(Tax::getId)
                    .containsExactly(propio.getId());
        }
    }

    @Nested
    @DisplayName("borrado y reactivacion")
    class BorradoYReactivacion {

        @Test
        @DisplayName("el borrado es logico: la fila deja de verse por id y en el listado")
        void el_borrado_es_logico() {
            Tax borrado = guardar("IVA General");
            Tax vivo = guardar("IVA Reducido");
            releerDesdeLaBase();

            repository.delete(borrado.getId());
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(borrado.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).extracting(Tax::getId)
                    .containsExactly(vivo.getId());
        }

        @Test
        @DisplayName("un impuesto dado de baja aparece en el listado de pausados")
        void un_impuesto_dado_de_baja_aparece_en_pausados() {
            Tax pausado = guardar("IVA General");
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            assertThat(repository.findAllDisabledByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(Tax::getId).containsExactly(pausado.getId());
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar la fila y devuelve 1 fila afectada")
        void reactivate_vuelve_a_habilitar_la_fila() {
            Tax pausado = guardar("IVA General");
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(pausado.getId(), SchemaSeed.COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(pausado.getId(), SchemaSeed.COMPANY_ID))
                    .map(Tax::isEnabled).contains(true);
        }

        @Test
        @DisplayName("reactivate de un impuesto de otra empresa no afecta ninguna fila")
        void reactivate_de_otra_empresa_no_afecta_filas() {
            Tax pausado = guardar("IVA General");
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(pausado.getId(), SchemaSeed.OTRA_COMPANY_ID);

            assertThat(filas).isZero();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("un impuesto de otra empresa no se encuentra por id")
        void un_impuesto_de_otra_empresa_no_se_encuentra_por_id() {
            Tax ajeno = guardar(OTRA_EMPRESA, "IVA Ajeno", new BigDecimal("19.00"), TaxScheme.IVA);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("findById (sin acotar empresa) si encuentra un impuesto ajeno")
        void find_by_id_sin_acotar_empresa_encuentra_el_ajeno() {
            // Usa este metodo solo infraestructura interna (p.ej. joins). Los casos de
            // uso siempre acotan por empresa via findByIdAndCompanyId.
            Tax ajeno = guardar(OTRA_EMPRESA, "IVA Ajeno", new BigDecimal("19.00"), TaxScheme.IVA);
            releerDesdeLaBase();

            assertThat(repository.findById(ajeno.getId())).map(Tax::getId).contains(ajeno.getId());
        }
    }
}
