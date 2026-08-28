package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
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
 * Rodaja de {@code JpaTaxReturnRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las tres columnas GENERATED
 * STORED del changeset 351</b>, invisibles desde Java y responsables de tres
 * cosas distintas:
 *
 * <ul>
 * <li>{@code municipality_key} pone el centinela {@code '-'} en las nacionales.
 * Sin el, dos declaraciones nacionales del mismo periodo <b>no chocarian</b>
 * —en SQL dos {@code NULL} no son iguales— y habria dos vigentes.</li>
 * <li>{@code current_return_marker} garantiza <b>una sola vigente</b> por
 * impuesto, periodo y municipio, y es lo que hace escribible «las declaraciones
 * no se editan: se suceden». {@link Correccion} demuestra las dos mitades: con
 * la anterior en {@code FILED} la correccion no cabe, y en cuanto pasa a
 * {@code CORRECTED} si.</li>
 * <li>{@code vat_frequency_year} vale {@code NULL} cuando el impuesto no es
 * IVA, y por eso {@code fk_tax_returns_vat_frequency} no se comprueba en los
 * otros tres. Es lo que permite que una declaracion de renta exista sin que
 * haya periodicidad de IVA publicada para su año.</li>
 * </ul>
 *
 * <p>
 * <b>Esta rodaja no siembra NADA, y esa es la parte que costo dos vueltas.</b>
 * Bogota con {@code dane_code = '11001'} la traen los changesets de geografia
 * colombiana, y la periodicidad de IVA de <b>2026 bimestral</b> la trae la
 * siembra minima del bloque contable (§9 de la especificacion, changeset 360).
 * Insertar cualquiera de las dos choca contra su indice unico y tumba la rodaja
 * entera en el {@code @BeforeEach}, antes de ejecutar un solo caso — que es
 * exactamente el anti-patron que el {@code CLAUDE.md} documenta para
 * {@code SchemaSeed}: <em>lo que ya siembra Liquibase no se resiembra, se
 * resuelve</em>.
 *
 * <p>
 * Que la periodicidad venga de la siembra y no de este fichero tiene ademas un
 * efecto util: si algun dia alguien cambiara la de 2026 a cuatrimestral, el
 * caso {@link IdaYVuelta#ica_guarda_municipio_y_iva_guarda_periodicidad()} se
 * pondria rojo contra {@code fk_tax_returns_vat_frequency}, que es justo lo que
 * esa clave foranea existe para vigilar.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaTaxReturnRepository — las declaraciones contra MySQL real")
class TaxReturnPersistenceIT extends AbstractDataJpaTest {

    private static final Long DECLARACION_CRUDA = 8442L;

    private static final String BOGOTA = "11001";
    private static final int ANO = 2026;

    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 1, 9, 0, 0);
    private static final LocalDateTime PRESENTADA_EL = LocalDateTime.of(2026, 4, 10, 11, 0, 0);
    private static final LocalDate FIRMEZA = LocalDate.of(2029, 4, 10);

    @Autowired
    private TaxReturnJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaTaxReturnRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
        repository = new JpaTaxReturnRepository(springDataRepository, new TaxReturnJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el borrador de renta y lo recupera con cada campo en su sitio")
        void guarda_el_borrador_de_renta_y_lo_recupera_campo_a_campo() {
            TaxReturn guardada = repository.save(borradorDeRenta());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getTaxKind()).isEqualTo(TaxKind.INCOME_TAX);
                // El año viaja por una columna SMALLINT y vuelve intacto.
                assertThat(recuperada.getFiscalYear()).isEqualTo(ANO);
                assertThat(recuperada.getFiscalPeriodKey()).isEqualTo("2026-A");
                assertThat(recuperada.getSequenceNumber()).isEqualTo(1);
                assertThat(recuperada.getMunicipalityCode()).isNull();
                assertThat(recuperada.getVatFrequency()).isNull();
                assertThat(recuperada.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
                assertThat(recuperada.getFirmezaUntil()).isNull();
                assertThat(recuperada.getCorrectsReturnId()).isNull();
                assertThat(recuperada.getTotalGenerated()).isEqualByComparingTo("4500000.00");
                assertThat(recuperada.getBalancePayable()).isEqualByComparingTo("1200000.00");
                assertThat(recuperada.getBalanceCredit()).isEqualByComparingTo("0.00");
                assertThat(recuperada.municipalityKey())
                        .isEqualTo(TaxReturn.NATIONAL_MUNICIPALITY_KEY);
                assertThat(recuperada.isCurrent()).isTrue();
                assertThat(recuperada.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("la declaracion de ICA guarda su municipio y la de IVA su periodicidad")
        void ica_guarda_municipio_y_iva_guarda_periodicidad() {
            TaxReturn ica = repository.save(borradorDeIca());
            TaxReturn iva = repository.save(borradorDeIva());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(ica.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getMunicipalityCode()).isEqualTo(BOGOTA);
                assertThat(recuperada.municipalityKey()).isEqualTo(BOGOTA);
            });
            assertThat(repository.findById(iva.getId())).get()
                    .satisfies(recuperada -> assertThat(recuperada.getVatFrequency())
                            .isEqualTo(VatFrequency.BIMONTHLY));
        }

        @Test
        @DisplayName("presentar escribe la firmeza y mueve la version")
        void presentar_escribe_la_firmeza_y_mueve_la_version() {
            TaxReturn guardada = repository.save(borradorDeRenta());
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(guardada.getId()).orElseThrow().file(PRESENTADA_EL,
                    SchemaSeed.SYSTEM_USER_ID, "RAD-0001", "s3://dian/2026-A.pdf", FIRMEZA));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(presentada -> {
                assertThat(presentada.getStatus()).isEqualTo(TaxReturnStatus.FILED);
                // De esta fecha cuelga toda la ventana de conservacion de soportes.
                assertThat(presentada.getFirmezaUntil()).isEqualTo(FIRMEZA);
                assertThat(presentada.getReceiptRef()).isEqualTo("RAD-0001");
                assertThat(presentada.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el barrido de firmeza solo trae las presentadas")
        void el_barrido_de_firmeza_solo_trae_las_presentadas() {
            // Un borrador tiene firmeza_until NULL y en SQL una comparacion contra NULL
            // no es cierta: queda fuera por construccion, que es lo correcto — una
            // declaracion sin presentar no sostiene ninguna ventana de conservacion.
            TaxReturn guardada = repository.save(borradorDeRenta());
            repository.save(borradorDeIca());
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(guardada.getId()).orElseThrow().file(PRESENTADA_EL,
                    SchemaSeed.SYSTEM_USER_ID, "RAD-0001", "s3://dian/2026-A.pdf", FIRMEZA));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByFirmezaUntilBefore(FIRMEZA.plusDays(1), 0, 20).content())
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Correccion")
    class Correccion {

        @Test
        @DisplayName("la correccion no cabe mientras la anterior siga vigente")
        void la_correccion_no_cabe_mientras_la_anterior_siga_vigente() {
            TaxReturn original = repository.save(borradorDeRenta());
            entityManager.flush();
            entityManager.clear();

            TaxReturn presentada = repository.save(repository.findById(original.getId())
                    .orElseThrow().file(PRESENTADA_EL, SchemaSeed.SYSTEM_USER_ID, "RAD-0001",
                            "s3://dian/2026-A.pdf", FIRMEZA));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_tax_returns_current", () -> {
                repository.save(presentada.correctionDraft(new BigDecimal("4600000.00"),
                        new BigDecimal("3300000.00"), new BigDecimal("1300000.00"), BigDecimal.ZERO,
                        CREADA_EL));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("marcar la anterior como corregida libera el hueco")
        void marcar_la_anterior_como_corregida_libera_el_hueco() {
            TaxReturn original = repository.save(borradorDeRenta());
            entityManager.flush();
            entityManager.clear();

            TaxReturn presentada = repository.save(repository.findById(original.getId())
                    .orElseThrow().file(PRESENTADA_EL, SchemaSeed.SYSTEM_USER_ID, "RAD-0001",
                            "s3://dian/2026-A.pdf", FIRMEZA));
            entityManager.flush();

            repository.save(presentada.markCorrected());
            entityManager.flush();

            TaxReturn correccion = repository.save(presentada.correctionDraft(
                    new BigDecimal("4600000.00"), new BigDecimal("3300000.00"),
                    new BigDecimal("1300000.00"), BigDecimal.ZERO, CREADA_EL));
            entityManager.flush();

            assertThat(correccion.getId()).isNotNull();
            assertThat(correccion.getSequenceNumber()).isEqualTo(2);
            assertThat(correccion.getCorrectsReturnId()).isEqualTo(presentada.getId());
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una retencion con clave bimestral la para chk_tax_returns_period")
        void una_retencion_con_clave_bimestral_la_para_el_check_del_periodo() {
            // EL check que impide que una retencion de diciembre acabe declarada en el
            // bimestre de enero. La retencion en la fuente es MENSUAL.
            EngineConstraint.assertViolates("chk_tax_returns_period",
                    () -> insertarCruda(DECLARACION_CRUDA, "WITHHOLDING", "2026-B01", null, null));
        }

        @Test
        @DisplayName("una declaracion nacional con municipio la para chk_tax_returns_municipality")
        void una_declaracion_nacional_con_municipio_la_para_el_check() {
            EngineConstraint.assertViolates("chk_tax_returns_municipality",
                    () -> insertarCruda(DECLARACION_CRUDA + 1, "WITHHOLDING", "2026-M03", BOGOTA,
                            null));
        }

        @Test
        @DisplayName("una declaracion de IVA sin periodicidad la para chk_tax_returns_vat_freq")
        void una_declaracion_de_iva_sin_periodicidad_la_para_el_check() {
            EngineConstraint.assertViolates("chk_tax_returns_vat_freq",
                    () -> insertarCruda(DECLARACION_CRUDA + 2, "VAT", "2026-B01", null, null));
        }
    }

    private static TaxReturn borradorDeRenta() {
        return TaxReturn.draft(TaxKind.INCOME_TAX, ANO, "2026-A", null, null,
                new BigDecimal("4500000.00"), new BigDecimal("3300000.00"),
                new BigDecimal("1200000.00"), BigDecimal.ZERO, CREADA_EL);
    }

    private static TaxReturn borradorDeIca() {
        return TaxReturn.draft(TaxKind.ICA, ANO, "2026-B01", BOGOTA, null,
                new BigDecimal("900000.00"), BigDecimal.ZERO, new BigDecimal("62100.00"),
                BigDecimal.ZERO, CREADA_EL);
    }

    private static TaxReturn borradorDeIva() {
        return TaxReturn.draft(TaxKind.VAT, ANO, "2026-B02", null, VatFrequency.BIMONTHLY,
                new BigDecimal("1900000.00"), new BigDecimal("400000.00"),
                new BigDecimal("1500000.00"), BigDecimal.ZERO, CREADA_EL);
    }

    private void insertarCruda(Long id, String impuesto, String periodo, String municipio,
            String periodicidad) {
        entityManager.createNativeQuery("""
                INSERT INTO tax_returns (id, tax_kind, fiscal_year, fiscal_period_key,
                        sequence_number, municipality_code, vat_frequency, status,
                        total_generated, total_deductible, balance_payable, balance_credit,
                        created_date, version)
                VALUES (:id, :impuesto, :ano, :periodo, 1, :municipio, :periodicidad, 'DRAFT',
                        0.00, 0.00, 0.00, 0.00, NOW(6), 0)
                """).setParameter("id", id).setParameter("impuesto", impuesto)
                .setParameter("ano", (short) ANO).setParameter("periodo", periodo)
                .setParameter("municipio", municipio).setParameter("periodicidad", periodicidad)
                .executeUpdate();
        entityManager.flush();
    }
}
