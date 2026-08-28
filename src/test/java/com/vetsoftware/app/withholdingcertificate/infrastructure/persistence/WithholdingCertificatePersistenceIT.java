package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
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
 * Rodaja de {@code JpaWithholdingCertificateRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las tres barandillas que solo
 * existen en el motor</b>: la unicidad del numero de certificado, el CHECK del
 * periodo fiscal y el aislamiento por empresa de la carga por id. Las tres se
 * pueden leer perfectamente en el changeset 328 y estar mal escritas en la
 * base; un test de mapper o de servicio pasaria igual.
 *
 * <p>
 * <b>El {@code @Import} lleva ademas el adaptador y el mapper.</b> Lo normal es
 * que {@code PersistenceSliceConfig} los traiga -asi todas las rodajas
 * comparten una sola clave de {@code MergedContextConfiguration} y un solo
 * contexto-, pero esta feature aun no esta anadida alli. El precio es un
 * arranque de contexto propio; la alternativa, no tener rodaja.
 *
 * <p>
 * <b>El seed no trae certificados.</b> {@code SchemaSeed} satisface claves
 * foraneas y se detiene antes del bloque fiscal, a proposito. Las escrituras
 * crudas de aqui usan ids del rango 8500, que ninguna otra rodaja toca.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaWithholdingCertificateRepository — certificados contra MySQL real")
class WithholdingCertificatePersistenceIT extends AbstractDataJpaTest {

    private static final String NIT = "830012345";

    /** Cuatro fechas distintas entre si: un cruce en el mapeo se ve. */
    private static final LocalDate EXPEDIDO_EL = LocalDate.of(2026, 2, 10);
    private static final LocalDate VENCE_EL = LocalDate.of(2026, 3, 31);
    private static final LocalDate RECIBIDO_EL = LocalDate.of(2026, 3, 18);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 2, 12, 9, 15, 30);

    /** 6,9 por mil expresado como porcentaje. La tarifa real de ICA. */
    private static final BigDecimal TARIFA_ICA_POR_MIL = new BigDecimal("0.690000");

    private static final BigDecimal IMPORTE = new BigDecimal("1847320.55");

    @Autowired
    private JpaWithholdingCertificateRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el certificado y lo recupera con cada fecha e importe en su sitio")
        void guarda_el_certificado_y_lo_recupera_campo_a_campo() {
            WithholdingCertificate guardado = repository.save(deIca("CERT-IDA-0001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperado.getIssuedByTaxId()).isEqualTo(NIT);
                        assertThat(recuperado.getCertificateNumber()).isEqualTo("CERT-IDA-0001");
                        assertThat(recuperado.getWithholdingType()).isEqualTo(WithholdingType.ICA);
                        assertThat(recuperado.getFiscalYear()).isEqualTo(2025);
                        assertThat(recuperado.getFiscalPeriodKey()).isEqualTo("2025-B03");
                        assertThat(recuperado.getCertifiedAmount()).isEqualByComparingTo(IMPORTE);
                        assertThat(recuperado.getIssuedOn()).isEqualTo(EXPEDIDO_EL);
                        assertThat(recuperado.getLegalDeadlineOn()).isEqualTo(VENCE_EL);
                        assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                        assertThat(recuperado.getReceivedOn()).isNull();
                        assertThat(recuperado.getFileRef()).isNull();
                    });
        }

        @Test
        @DisplayName("la tarifa por mil vuelve de la columna con sus seis decimales intactos")
        void la_tarifa_por_mil_vuelve_con_sus_seis_decimales() {
            // DECIMAL(9,6) contra una columna de cuatro decimales: 0,690000 sobrevive
            // en las dos, pero 0,004140 solo en esta. Este caso es lo que congela la
            // decision del changeset 328.
            WithholdingCertificate guardado = repository
                    .save(conTarifa("CERT-TARIFA-0001", new BigDecimal("0.004140")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get()
                    .satisfies(recuperado -> assertThat(recuperado.getRatePercent())
                            .isEqualByComparingTo("0.004140"));
        }

        @Test
        @DisplayName("la segunda escritura mueve la version que la tabla declara")
        void la_segunda_escritura_mueve_la_version() {
            // Es lo unico que prueba que el @Version esta vivo: sin el, dos ediciones
            // simultaneas se pisan sin excepcion y sin log.
            WithholdingCertificate guardado = repository.save(deRenta("CERT-VERSION-0001"));
            entityManager.flush();

            assertThat(versionEnLaBase(guardado.getId())).isZero();

            guardado.receive(RECIBIDO_EL, "s3://certificados/CERT-VERSION-0001.pdf");
            repository.save(guardado);
            entityManager.flush();

            assertThat(versionEnLaBase(guardado.getId())).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("dos certificados con el mismo numero y ano los para uq_withholding_certificates_number")
        void el_mismo_numero_y_ano_dos_veces_lo_para_la_unicidad() {
            // Nadie expide dos certificados del mismo ano con el mismo numero. Lo
            // unico repetido entre las dos filas es la llave, asi que no hay otra
            // constraint que pueda saltar antes.
            repository.save(deRenta("CERT-UNICO-0001"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_withholding_certificates_number", () -> {
                repository.save(deRenta("CERT-UNICO-0001"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("el mismo numero en otro ano gravable si entra: el ano es parte de la llave")
        void el_mismo_numero_en_otro_ano_si_entra() {
            repository.save(deRenta("CERT-UNICO-0002"));
            entityManager.flush();

            repository.save(
                    new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT, "CERT-UNICO-0002",
                            WithholdingType.INCOME_TAX, 2024, "2024-A", new BigDecimal("2.500000"),
                            IMPORTE, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("el mismo numero en otra empresa si entra: la empresa va delante de la llave")
        void el_mismo_numero_en_otra_empresa_si_entra() {
            repository.save(deRenta("CERT-UNICO-0003"));
            repository.save(new WithholdingCertificate(null, SchemaSeed.OTRA_COMPANY_ID, NIT,
                    "CERT-UNICO-0003", WithholdingType.INCOME_TAX, 2025, "2025-A",
                    new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("un periodo bimestral en renta lo para chk_withholding_certificates_period")
        void un_periodo_bimestral_en_renta_lo_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza -el cinturon bajo el tirante-
            // es escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_withholding_certificates_period",
                    () -> insertarCrudo(8500L, "INCOME_TAX", 2025, "2025-B01",
                            new BigDecimal("2.500000")));
        }

        @Test
        @DisplayName("un bimestre de otro ano lo para el mismo check")
        void un_bimestre_de_otro_ano_lo_para_el_mismo_check() {
            // La mitad del CHECK que parece redundante: 2024-B03 pasa el formato y
            // mandaria la retencion a la declaracion del ano equivocado.
            EngineConstraint.assertViolates("chk_withholding_certificates_period",
                    () -> insertarCrudo(8501L, "ICA", 2025, "2024-B03", TARIFA_ICA_POR_MIL));
        }

        @Test
        @DisplayName("un septimo bimestre no existe y lo para el mismo check")
        void un_septimo_bimestre_no_existe() {
            EngineConstraint.assertViolates("chk_withholding_certificates_period",
                    () -> insertarCrudo(8502L, "VAT", 2025, "2025-B07", TARIFA_ICA_POR_MIL));
        }

        @Test
        @DisplayName("una tarifa por encima de cien la para chk_withholding_certificates_rate")
        void una_tarifa_por_encima_de_cien_la_para_el_check() {
            EngineConstraint.assertViolates("chk_withholding_certificates_rate",
                    () -> insertarCrudo(8503L, "INCOME_TAX", 2025, "2025-A",
                            new BigDecimal("100.000001")));
        }

        @Test
        @DisplayName("un sustituto sobre un certificado ya recibido lo para su check")
        void un_sustituto_sobre_uno_recibido_lo_para_su_check() {
            EngineConstraint.assertViolates("chk_withholding_certificates_substitute",
                    () -> insertarCrudoConSustitutoYRecepcion(8504L));
        }

        @Test
        @DisplayName("un certificado de una empresa inexistente lo para fk_withholding_certificates_company")
        void un_certificado_de_una_empresa_inexistente_lo_para_la_fk() {
            EngineConstraint.assertViolates("fk_withholding_certificates_company", () -> {
                repository.save(new WithholdingCertificate(null, 8599L, NIT, "CERT-FK-0001",
                        WithholdingType.INCOME_TAX, 2025, "2025-A", new BigDecimal("2.500000"),
                        IMPORTE, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            WithholdingCertificate guardado = repository.save(deRenta("CERT-TENANCY-0001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("el listado de una empresa no trae los certificados de la otra")
        void el_listado_de_una_empresa_no_trae_los_de_la_otra() {
            repository.save(deRenta("CERT-TENANCY-0002"));
            repository.save(new WithholdingCertificate(null, SchemaSeed.OTRA_COMPANY_ID, NIT,
                    "CERT-TENANCY-0003", WithholdingType.INCOME_TAX, 2025, "2025-A",
                    new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .singleElement().satisfies(fila -> assertThat(fila.getCertificateNumber())
                            .isEqualTo("CERT-TENANCY-0002"));
            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("ordena por ano gravable y expedicion descendentes y desempata por id")
        void ordena_descendente_y_desempata_por_id() {
            WithholdingCertificate antiguo = repository.save(new WithholdingCertificate(null,
                    SchemaSeed.COMPANY_ID, NIT, "CERT-ORDEN-0001", WithholdingType.INCOME_TAX, 2024,
                    "2024-A", new BigDecimal("2.500000"), IMPORTE, LocalDate.of(2025, 2, 10),
                    LocalDate.of(2025, 3, 31), null, null, null, null, CREADO_EL));
            // Estos dos comparten ano y fecha de expedicion EXACTOS: sin el id de
            // desempate el orden entre ellos seria el que quiera el motor y la
            // paginacion podria repetir o perder uno.
            WithholdingCertificate empateA = repository.save(deRenta("CERT-ORDEN-0002"));
            WithholdingCertificate empateB = repository.save(deRenta("CERT-ORDEN-0003"));
            entityManager.flush();
            entityManager.clear();

            PageResult<WithholdingCertificate> pagina = repository
                    .findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(WithholdingCertificate::getId)
                    .containsExactly(empateB.getId(), empateA.getId(), antiguo.getId());
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("el barrido de vencimientos trae solo los que faltan y vencen antes del corte")
        void el_barrido_trae_solo_los_que_faltan_y_vencen_antes_del_corte() {
            WithholdingCertificate urgente = repository.save(deRenta("CERT-FALTA-0001"));
            repository.save(new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT,
                    "CERT-FALTA-0002", WithholdingType.INCOME_TAX, 2026, "2026-A",
                    new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL, LocalDate.of(2027, 3, 31),
                    null, null, null, null, CREADO_EL));
            repository.save(new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT,
                    "CERT-FALTA-0003", WithholdingType.INCOME_TAX, 2025, "2025-A",
                    new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL, VENCE_EL, RECIBIDO_EL,
                    "s3://certificados/llego.pdf", null, null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            PageResult<WithholdingCertificate> faltan = repository
                    .findAllMissing(LocalDate.of(2026, 12, 31), 0, 20);

            // Las dos mitades del filtro: el que vence en 2027 queda fuera por fecha y
            // el que ya llego queda fuera por received_on. Si faltara cualquiera de
            // las dos, este caso traeria dos filas o tres.
            assertThat(faltan.content()).extracting(WithholdingCertificate::getId)
                    .containsExactly(urgente.getId());
        }

        @Test
        @DisplayName("el barrido de vencimientos ordena por el mas urgente primero")
        void el_barrido_ordena_por_el_mas_urgente_primero() {
            WithholdingCertificate tarde = repository.save(new WithholdingCertificate(null,
                    SchemaSeed.COMPANY_ID, NIT, "CERT-ORDEN-FALTA-0001", WithholdingType.INCOME_TAX,
                    2025, "2025-A", new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL,
                    LocalDate.of(2026, 6, 30), null, null, null, null, CREADO_EL));
            WithholdingCertificate pronto = repository.save(deRenta("CERT-ORDEN-FALTA-0002"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllMissing(LocalDate.of(2026, 12, 31), 0, 20).content())
                    .extracting(WithholdingCertificate::getId)
                    .containsExactly(pronto.getId(), tarde.getId());
        }

        @Test
        @DisplayName("el barrido acotado no ve los vencimientos de la otra empresa")
        void el_barrido_acotado_no_ve_los_de_la_otra_empresa() {
            WithholdingCertificate propio = repository.save(deRenta("CERT-FALTA-PROPIA"));
            repository.save(new WithholdingCertificate(null, SchemaSeed.OTRA_COMPANY_ID, NIT,
                    "CERT-FALTA-AJENA", WithholdingType.INCOME_TAX, 2025, "2025-A",
                    new BigDecimal("2.500000"), IMPORTE, EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            LocalDate corte = LocalDate.of(2026, 12, 31);

            // Acotar por vencimiento no acota por tenant: el barrido ancho ve las dos.
            assertThat(repository.findAllMissing(corte, 0, 20).totalElements()).isEqualTo(2L);
            assertThat(repository.findAllMissingByCompanyId(SchemaSeed.COMPANY_ID, corte, 0, 20)
                    .content()).extracting(WithholdingCertificate::getId)
                    .containsExactly(propio.getId());
        }

        @Test
        @DisplayName("el corte es estricto: lo que vence justo ese dia queda fuera")
        void el_corte_es_estricto() {
            repository.save(deRenta("CERT-CORTE-0001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllMissing(VENCE_EL, 0, 20).content()).isEmpty();
            assertThat(repository.findAllMissing(VENCE_EL.plusDays(1), 0, 20).content()).hasSize(1);
        }
    }

    // --- andamio ------------------------------------------------------------

    private WithholdingCertificate deRenta(String numero) {
        return new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT, numero,
                WithholdingType.INCOME_TAX, 2025, "2025-A", new BigDecimal("2.500000"), IMPORTE,
                EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL);
    }

    private WithholdingCertificate deIca(String numero) {
        return new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT, numero,
                WithholdingType.ICA, 2025, "2025-B03", TARIFA_ICA_POR_MIL, IMPORTE, EXPEDIDO_EL,
                VENCE_EL, null, null, null, null, CREADO_EL);
    }

    private WithholdingCertificate conTarifa(String numero, BigDecimal tarifa) {
        return new WithholdingCertificate(null, SchemaSeed.COMPANY_ID, NIT, numero,
                WithholdingType.ICA, 2025, "2025-B03", tarifa, IMPORTE, EXPEDIDO_EL, VENCE_EL, null,
                null, null, null, CREADO_EL);
    }

    private Long versionEnLaBase(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT version FROM withholding_certificates WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los CHECK que el dominio
     * ya replica: sin ella no habria forma de comprobar que la base tambien los
     * cuida.
     */
    private void insertarCrudo(Long id, String tipo, int ano, String periodo, BigDecimal tarifa) {
        entityManager.createNativeQuery("""
                INSERT INTO withholding_certificates (id, company_id, issued_by_tax_id,
                                                      certificate_number, withholding_type,
                                                      fiscal_year, fiscal_period_key,
                                                      rate_percent, certified_amount, issued_on,
                                                      legal_deadline_on, created_date, version)
                VALUES (:id, :companyId, :nit, :numero, :tipo, :ano, :periodo, :tarifa, :importe,
                        :expedido, :vence, :creado, 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("nit", NIT).setParameter("numero", "CERT-CRUDO-" + id)
                .setParameter("tipo", tipo).setParameter("ano", ano)
                .setParameter("periodo", periodo).setParameter("tarifa", tarifa)
                .setParameter("importe", IMPORTE).setParameter("expedido", EXPEDIDO_EL)
                .setParameter("vence", VENCE_EL).setParameter("creado", CREADO_EL).executeUpdate();
    }

    /** La combinacion que el dominio nunca produce: sustituto y papel a la vez. */
    private void insertarCrudoConSustitutoYRecepcion(Long id) {
        entityManager.createNativeQuery("""
                INSERT INTO withholding_certificates (id, company_id, issued_by_tax_id,
                                                      certificate_number, withholding_type,
                                                      fiscal_year, fiscal_period_key,
                                                      rate_percent, certified_amount, issued_on,
                                                      legal_deadline_on, received_on, file_ref,
                                                      substitute_evidence_kind,
                                                      substitute_evidence_ref, created_date,
                                                      version)
                VALUES (:id, :companyId, :nit, :numero, 'INCOME_TAX', 2025, '2025-A', 2.500000,
                        :importe, :expedido, :vence, :recibido, 's3://certificados/llego.pdf',
                        :clase, 's3://pagos/REC-1.pdf', :creado, 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("nit", NIT).setParameter("numero", "CERT-CRUDO-" + id)
                .setParameter("importe", IMPORTE).setParameter("expedido", EXPEDIDO_EL)
                .setParameter("vence", VENCE_EL).setParameter("recibido", RECIBIDO_EL)
                .setParameter("clase", SubstituteEvidenceKind.PAYMENT_RECEIPT.name())
                .setParameter("creado", CREADO_EL).executeUpdate();
    }
}
