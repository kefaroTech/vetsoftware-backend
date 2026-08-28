package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaSupplierWithholdingRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para demostrar es que dos facturas distintas del
 * mismo proveedor en el mismo mes CABEN</b>, que es el caso normal y lo que la
 * clave del documento maestro prohibia. {@code uq_supplier_withholdings_case}
 * lleva {@code supplier_invoice_ref} dentro justo por eso, y
 * {@link Unicidad#el_mismo_soporte_dos_veces_choca()} es la otra mitad:
 * declarar dos veces la misma retencion por el mismo soporte duplicaria el
 * reporte anual de terceros.
 *
 * <p>
 * <b>Lo segundo es la tarifa.</b> El 4,14 por mil se escribe {@code 0.414000} y
 * tiene que sobrevivir entero al viaje por {@code DECIMAL(9,6)}: con dos
 * decimales se corta a {@code 0.41} y se retiene casi un uno por ciento de
 * menos, calculado en silencio y sin un solo error.
 *
 * <p>
 * <b>Y lo tercero es la granularidad del periodo</b>, que es donde esta gemela
 * se separa de {@code document_withholdings}: aqui {@code INCOME_TAX} es
 * <b>mensual</b> —la retencion que practicamos se declara en la retencion en la
 * fuente— y alli es anual. {@code chk_sw_period} lo impone, y el caso de
 * {@link RestriccionesDelMotor} es lo que impide que alguien lo «corrija».
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSupplierWithholdingRepository — lo que retenemos contra MySQL real")
class SupplierWithholdingPersistenceIT extends AbstractDataJpaTest {

    private static final Long RETENCION_CRUDA = 8451L;

    private static final String BOGOTA = "11001";
    private static final String NIT = "900123456-7";
    private static final int ANO = 2026;

    private static final LocalDate PRACTICADA_EL = LocalDate.of(2026, 3, 15);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 15, 10, 0, 0);

    @Autowired
    private SupplierWithholdingJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaSupplierWithholdingRepository repository;

    /**
     * <strong>No se siembra ningun municipio, y esa ausencia es el
     * arreglo.</strong> Bogota con {@code dane_code = '11001'} <b>ya la trae
     * Liquibase</b> —los changesets de geografia colombiana rellenan la DIVIPOLA
     * completa— asi que un {@code INSERT} propio choca contra
     * {@code uq_cities_dane_code} y tumba la rodaja entera en el
     * {@code @BeforeEach}, antes de ejecutar un solo caso. Es la misma trampa que
     * el {@code CLAUDE.md} documenta para {@code SchemaSeed}: no se resiembra lo
     * que la migracion ya siembra, se usa.
     */
    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
        repository = new JpaSupplierWithholdingRepository(springDataRepository,
                new SupplierWithholdingJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la retencion y la recupera con cada campo en su sitio")
        void guarda_la_retencion_y_la_recupera_campo_a_campo() {
            SupplierWithholding guardada = repository.save(renta("FV-001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getSupplierTaxId()).isEqualTo(NIT);
                assertThat(recuperada.getSupplierDocType()).isEqualTo(SupplierDocumentKind.NIT);
                assertThat(recuperada.getSupplierInvoiceRef()).isEqualTo("FV-001");
                assertThat(recuperada.getWithholdingType())
                        .isEqualTo(SupplierWithholdingType.INCOME_TAX);
                assertThat(recuperada.getTaxableBase()).isEqualByComparingTo("1000000.00");
                assertThat(recuperada.getAmount()).isEqualByComparingTo("40000.00");
                assertThat(recuperada.getMunicipalityCode()).isNull();
                // El año viaja por una columna SMALLINT y vuelve intacto.
                assertThat(recuperada.getFiscalYear()).isEqualTo(ANO);
                assertThat(recuperada.getFiscalPeriodKey()).isEqualTo("2026-M03");
                assertThat(recuperada.getPracticedOn()).isEqualTo(PRACTICADA_EL);
                assertThat(recuperada.isCertified()).isFalse();
                assertThat(recuperada.municipalityKey())
                        .isEqualTo(SupplierWithholding.NATIONAL_MUNICIPALITY_KEY);
                assertThat(recuperada.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("el 4,14 por mil sobrevive entero al viaje por DECIMAL(9,6)")
        void el_414_por_mil_sobrevive_entero() {
            // Con cuatro decimales, 0.414000 se corta a 0.4140 y con dos a 0.41: se
            // retendria casi un uno por ciento de menos en cada factura, en silencio.
            SupplierWithholding guardada = repository.save(ica(new BigDecimal("0.414000")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getRatePercent()).isEqualByComparingTo("0.414000");
                assertThat(recuperada.getRatePercent().scale()).isEqualTo(6);
                assertThat(recuperada.getMunicipalityCode()).isEqualTo(BOGOTA);
            });
        }

        @Test
        @DisplayName("emitir el certificado mueve la version: es una edicion, no un insert")
        void emitir_el_certificado_mueve_la_version() {
            SupplierWithholding guardada = repository.save(renta("FV-002"));
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(guardada.getId()).orElseThrow()
                    .issueCertificate(CREADA_EL.plusDays(20), "CERT-2026-0001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(certificada -> {
                assertThat(certificada.isCertified()).isTrue();
                assertThat(certificada.getCertificateRef()).isEqualTo("CERT-2026-0001");
                assertThat(certificada.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el certificado anual del proveedor trae sus dos retenciones")
        void el_certificado_anual_trae_sus_dos_retenciones() {
            repository.save(renta("FV-003"));
            repository.save(renta("FV-004"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllBySupplierTaxIdAndFiscalYear(NIT, ANO, 0, 20).content())
                    .hasSize(2);
            assertThat(
                    repository.findAllBySupplierTaxIdAndFiscalYear(NIT, ANO + 1, 0, 20).content())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("dos facturas distintas del mismo proveedor y mes SI caben")
        void dos_facturas_distintas_del_mismo_proveedor_y_mes_si_caben() {
            // EL caso de la correccion de la clave. Con la del documento maestro —sin la
            // factura dentro— esta segunda fila era inescribible, y ese es el caso
            // normal de cualquier proveedor recurrente.
            repository.save(renta("FV-005"));
            entityManager.flush();

            SupplierWithholding segunda = repository.save(renta("FV-006"));
            entityManager.flush();

            assertThat(segunda.getId()).isNotNull();
        }

        @Test
        @DisplayName("el mismo soporte dos veces choca: duplicaria el reporte de terceros")
        void el_mismo_soporte_dos_veces_choca() {
            repository.save(renta("FV-007"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_supplier_withholdings_case", () -> {
                repository.save(renta("FV-007"));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una retencion en la fuente con clave bimestral la para chk_sw_period")
        void una_retencion_en_la_fuente_con_clave_bimestral_la_para_el_check() {
            // Aqui INCOME_TAX es MENSUAL, al contrario que en document_withholdings.
            // Este caso es lo que impide que el primer lector lo «corrija».
            EngineConstraint.assertViolates("chk_sw_period", () -> insertarCruda(RETENCION_CRUDA,
                    "INCOME_TAX", "2026-B01", null, "1000000.00", "40000.00"));
        }

        @Test
        @DisplayName("retener mas que la base lo para chk_sw_amounts")
        void retener_mas_que_la_base_lo_para_el_check_de_importes() {
            // Es el sintoma de un calculo invertido —tarifa aplicada como fraccion, o
            // base y retenido cruzados— y sin esta linea entraria y se declararia.
            EngineConstraint.assertViolates("chk_sw_amounts",
                    () -> insertarCruda(RETENCION_CRUDA + 1, "INCOME_TAX", "2026-M03", null,
                            "40000.00", "1000000.00"));
        }

        @Test
        @DisplayName("una retencion nacional con municipio la para chk_sw_municipality")
        void una_retencion_nacional_con_municipio_la_para_el_check() {
            EngineConstraint.assertViolates("chk_sw_municipality",
                    () -> insertarCruda(RETENCION_CRUDA + 2, "INCOME_TAX", "2026-M03", BOGOTA,
                            "1000000.00", "40000.00"));
        }
    }

    private static SupplierWithholding renta(String factura) {
        return SupplierWithholding.practice(NIT, "Proveedor de andamio", SupplierDocumentKind.NIT,
                factura, SupplierWithholdingType.INCOME_TAX, "Servicios profesionales",
                new BigDecimal("1000000.00"), new BigDecimal("4.000000"),
                new BigDecimal("40000.00"), null, ANO, "2026-M03", PRACTICADA_EL, CREADA_EL);
    }

    private static SupplierWithholding ica(BigDecimal tarifa) {
        return SupplierWithholding.practice(NIT, "Proveedor de andamio", SupplierDocumentKind.NIT,
                "FV-ICA-001", SupplierWithholdingType.ICA, "Servicios profesionales",
                new BigDecimal("1000000.00"), tarifa, new BigDecimal("4140.00"), BOGOTA, ANO,
                "2026-B02", PRACTICADA_EL, CREADA_EL);
    }

    private void insertarCruda(Long id, String tipo, String periodo, String municipio, String base,
            String retenido) {
        entityManager.createNativeQuery("""
                INSERT INTO supplier_withholdings (id, supplier_tax_id, supplier_name,
                        supplier_doc_type, supplier_invoice_ref, withholding_type, concept,
                        taxable_base, rate_percent, amount, municipality_code, fiscal_year,
                        fiscal_period_key, practiced_on, created_date, version)
                VALUES (:id, :nit, 'Proveedor crudo', 'NIT', :factura, :tipo, 'Concepto crudo',
                        :base, 4.000000, :retenido, :municipio, :ano, :periodo, '2026-03-15',
                        NOW(6), 0)
                """).setParameter("id", id).setParameter("nit", NIT)
                .setParameter("factura", "FV-CRUDA-" + id).setParameter("tipo", tipo)
                .setParameter("base", new BigDecimal(base))
                .setParameter("retenido", new BigDecimal(retenido))
                .setParameter("municipio", municipio).setParameter("ano", (short) ANO)
                .setParameter("periodo", periodo).executeUpdate();
        entityManager.flush();
    }
}
