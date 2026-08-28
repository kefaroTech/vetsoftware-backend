package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja de {@code JpaExternalInvoiceReconciliationRepository} contra MySQL
 * real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las tres parejas del
 * esquema</b>, y lo importante es que el dominio y la base las cuidan las dos:
 * el dominio para que el error llegue con un mensaje que se entiende, la base
 * para que no haya forma de escribir la fila mala por ningun otro camino. Un
 * test que solo pruebe el dominio deja la mitad sin cubrir, y el dia que
 * alguien escriba por SQL nativo —una importacion, un job— la fila entra sin
 * protestar. Por eso los casos de {@link RestriccionesDelMotor} <b>se saltan el
 * agregado</b> y escriben crudo: es la unica forma de comprobar que
 * {@code chk_eir_external_pair} y {@code chk_eir_resolved} existen de verdad.
 *
 * <p>
 * <b>Y la bandeja de {@code MISSING_EXTERNAL} tiene su propio bloque</b> porque
 * es la consulta que de verdad importa: documentos de cobro devengados que
 * nadie facturo. No produce ninguna diferencia que llame la atencion, asi que
 * si esta consulta se rompe nadie se entera —el listado sale vacio y se lee
 * como «no hay nada pendiente»—.
 *
 * <p>
 * <b>El {@code @Import} lleva el adaptador y el mapper propios</b> porque
 * {@code PersistenceSliceConfig} todavia no los conoce, y este agente tiene
 * prohibido editarlo. Eso cuesta un arranque de contexto extra: en cuanto las
 * dos clases entren en esa configuracion, aqui debe quedar
 * {@code @Import(PersistenceSliceConfig.class)} pelado.
 *
 * <p>
 * <b>Las filas de apoyo van con ids del rango 8600</b>, que ninguna otra rodaja
 * usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaExternalInvoiceReconciliationRepository — conciliaciones contra MySQL real")
class ExternalInvoiceReconciliationPersistenceIT extends AbstractDataJpaTest {

    private static final Long DOCUMENTO_PROPIO = 8600L;
    private static final Long OTRO_DOCUMENTO_PROPIO = 8601L;
    private static final Long TERCER_DOCUMENTO_PROPIO = 8602L;
    private static final Long DOCUMENTO_AJENO = 8603L;

    private static final BigDecimal TOTAL_PROPIO = new BigDecimal("119000.00");
    private static final BigDecimal IMPUESTO_PROPIO = new BigDecimal("19000.00");

    /**
     * Cuatro instantes deliberadamente distintos entre si. Si el mapper cruza
     * {@code createdDate} con {@code resolvedAt}, o {@code resolutionValidUntil}
     * con cualquiera de los dos, la asercion cae; con la misma fecha en todos, no.
     */
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 14, 30, 15);
    private static final LocalDateTime CREADO_ANTES = LocalDateTime.of(2026, 1, 9, 8, 0, 0);
    private static final LocalDateTime CREADO_DESPUES = LocalDateTime.of(2026, 5, 21, 17, 45, 30);
    private static final LocalDateTime RESUELTO_EL = LocalDateTime.of(2026, 4, 11, 9, 20, 45);
    private static final LocalDate VIGENTE_HASTA = LocalDate.of(2027, 1, 31);

    @Autowired
    private JpaExternalInvoiceReconciliationRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * El periodo que usa {@code IdaYVuelta} al resolver la conciliacion. El
     * changeset 346 crea disparadores {@code BEFORE INSERT/UPDATE} sobre esta tabla
     * que rechazan escribir contra un {@code posting_period} que no exista en
     * {@code accounting_periods} o que no este {@code OPEN}, y
     * {@code fk_eir_posting_period} (331) exige ademas que la fila exista.
     * "2026-03" no colisiona con el resto de la suite: 2026-08 es la semilla de
     * Liquibase, 2027-xx es de {@code AccountingPeriodPersistenceIT}, 2028-xx de
     * {@code RevenueRecognitionLinePersistenceIT} y 2031-xx de
     * {@code AccountingPeriodTriggerIT}.
     */
    private static final Long PERIODO_ID = 8600L;
    private static final String PERIODO_MARZO = "2026-03";

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        periodo(PERIODO_ID, PERIODO_MARZO);
        documento(DOCUMENTO_PROPIO, "DC-TEST-8600", SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID);
        documento(OTRO_DOCUMENTO_PROPIO, "DC-TEST-8601", SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID);
        documento(TERCER_DOCUMENTO_PROPIO, "DC-TEST-8602", SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID);
        documento(DOCUMENTO_AJENO, "DC-TEST-8603", SchemaSeed.OTRA_COMPANY_ID,
                SchemaSeed.OTRA_SUBSCRIPTION_ID);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la conciliacion resuelta y la recupera con cada numero y cada fecha en su sitio")
        void guarda_la_conciliacion_resuelta_y_la_recupera_campo_a_campo() {
            ExternalInvoiceReconciliation abierta = repository.save(abierta(DOCUMENTO_PROPIO));
            abierta.match("FE-1043", "CUFE-0011", new BigDecimal("118998.00"),
                    new BigDecimal("18998.31"), "18764000000123", 1000, 5000, VIGENTE_HASTA);
            ExternalInvoiceReconciliation conciliada = repository.save(abierta);
            conciliada.resolve(SchemaSeed.SYSTEM_USER_ID, "Ajuste por redondeo del impuesto",
                    "2026-03", RESUELTO_EL);
            ExternalInvoiceReconciliation resuelta = repository.save(conciliada);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(resuelta.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(recuperada.getBillingDocumentId()).isEqualTo(DOCUMENTO_PROPIO);
                assertThat(recuperada.getComputedTotal()).isEqualByComparingTo("119000.00");
                assertThat(recuperada.getComputedTax()).isEqualByComparingTo("19000.00");
                assertThat(recuperada.getExternalTotal()).isEqualByComparingTo("118998.00");
                assertThat(recuperada.getExternalTax()).isEqualByComparingTo("18998.31");
                assertThat(recuperada.getDifference()).isEqualByComparingTo("2.00");
                assertThat(recuperada.getStatus())
                        .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
                assertThat(recuperada.getExternalInvoiceId()).isEqualTo("FE-1043");
                assertThat(recuperada.getExternalCufe()).isEqualTo("CUFE-0011");
                assertThat(recuperada.getExternalResolutionNumber()).isEqualTo("18764000000123");
                assertThat(recuperada.getExternalRangeFrom()).isEqualTo(1000);
                assertThat(recuperada.getExternalRangeTo()).isEqualTo(5000);
                assertThat(recuperada.getResolutionValidUntil()).isEqualTo(VIGENTE_HASTA);
                assertThat(recuperada.getResolvedBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                assertThat(recuperada.getResolvedAt()).isEqualTo(RESUELTO_EL);
                assertThat(recuperada.getResolutionNote())
                        .isEqualTo("Ajuste por redondeo del impuesto");
                assertThat(recuperada.getPostingPeriod()).isEqualTo("2026-03");
                assertThat(recuperada.getCreatedDate()).isEqualTo(CREADO_EL);
            });
        }

        @Test
        @DisplayName("el CHECK de la diferencia acepta la resta que calculo el dominio")
        void el_check_de_la_diferencia_acepta_la_resta_del_dominio() {
            // chk_eir_difference exige difference = computed_total - external_total. Si
            // el dominio invirtiera la resta, esta escritura moriria con un error de
            // integridad; que pase es la prueba de que los dos lados calculan igual.
            ExternalInvoiceReconciliation abierta = repository.save(abierta(DOCUMENTO_PROPIO));
            abierta.match("FE-2000", null, new BigDecimal("100000.00"), new BigDecimal("15966.39"),
                    null, null, null, null);

            ExternalInvoiceReconciliation guardada = repository.save(abierta);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getDifference()).isEqualByComparingTo("19000.00");
                assertThat(recuperada.getStatus())
                        .isEqualTo(ExternalInvoiceReconciliationStatus.MISMATCH);
            });
        }

        @Test
        @DisplayName("la version sube en cada escritura: el bloqueo optimista esta vivo")
        void la_version_sube_en_cada_escritura() {
            // Si el mapper no arrastrara la version, cada save haria merge con version
            // nula y el candado dejaria de existir sin que nada lo dijera.
            ExternalInvoiceReconciliation abierta = repository.save(abierta(DOCUMENTO_PROPIO));
            entityManager.flush();
            assertThat(abierta.getVersion()).isZero();

            abierta.match("FE-1043", null, TOTAL_PROPIO, IMPUESTO_PROPIO, null, null, null, null);
            ExternalInvoiceReconciliation conciliada = repository.save(abierta);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(conciliada.getId())).get()
                    .satisfies(recuperada -> assertThat(recuperada.getVersion()).isEqualTo(1L));
        }
    }

    @Nested
    @DisplayName("Unicidad por documento")
    class UnicidadPorDocumento {

        @Test
        @DisplayName("un documento de cobro no puede tener dos conciliaciones: uq_eir_document")
        void un_documento_no_puede_tener_dos_conciliaciones() {
            repository.save(abierta(DOCUMENTO_PROPIO));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_eir_document", () -> {
                repository.save(abierta(DOCUMENTO_PROPIO));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la consulta previa ve el duplicado y no lo ve donde no lo hay")
        void la_consulta_previa_ve_el_duplicado() {
            // Es lo que convierte el 500 de la unicidad en un 409 que se entiende.
            repository.save(abierta(DOCUMENTO_PROPIO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID,
                    DOCUMENTO_PROPIO)).isTrue();
            assertThat(repository.existsByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID,
                    OTRO_DOCUMENTO_PROPIO)).isFalse();
            // Y no cruza de empresa: la unicidad es por el par, no por el documento solo.
            assertThat(repository.existsByCompanyIdAndBillingDocumentId(SchemaSeed.OTRA_COMPANY_ID,
                    DOCUMENTO_PROPIO)).isFalse();
        }

        @Test
        @DisplayName("una conciliacion no puede colgar del documento de otra empresa: fk_eir_document")
        void una_conciliacion_no_cuelga_del_documento_de_otra_empresa() {
            // DOCUMENTO_AJENO existe de verdad, solo que bajo OTRA_COMPANY_ID. La unica
            // barandilla que puede pararlo es la FK COMPUESTA (company_id,
            // billing_document_id). Si manana alguien la degradara a una FK simple sobre
            // billing_document_id, esta fila entraria y el caso se pondria rojo.
            EngineConstraint.assertViolates("fk_eir_document", () -> {
                repository.save(ExternalInvoiceReconciliation.open(SchemaSeed.COMPANY_ID,
                        DOCUMENTO_AJENO, TOTAL_PROPIO, IMPUESTO_PROPIO, CREADO_EL));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una MISSING_EXTERNAL con factura externa la para chk_eir_external_pair")
        void una_missing_external_con_factura_externa_la_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza —el cinturon bajo el tirante— es
            // escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_eir_external_pair",
                    () -> insertarCruda("MISSING_EXTERNAL", "FE-9999", null, null, null, null));
        }

        @Test
        @DisplayName("una conciliada sin total externo la para el mismo check")
        void una_conciliada_sin_total_externo_la_para_el_mismo_check() {
            // La otra mitad del CHECK. Sin este caso, uno que solo mirara la rama de
            // MISSING_EXTERNAL pasaria por bueno y una fila MATCHED podria quedarse sin
            // los numeros contra los que dice haber cuadrado.
            EngineConstraint.assertViolates("chk_eir_external_pair",
                    () -> insertarCruda("MATCHED", "FE-9999", null, null, null, null));
        }

        @Test
        @DisplayName("una resolucion a medias la para chk_eir_resolved")
        void una_resolucion_a_medias_la_para_el_check() {
            // Firma y fecha, pero sin periodo contable: el ajuste no quedaria imputado a
            // ningun cierre y nadie lo notaria hasta cuadrar el mes.
            EngineConstraint.assertViolates("chk_eir_resolved",
                    () -> insertarCruda("MISSING_EXTERNAL", null, SchemaSeed.SYSTEM_USER_ID,
                            RESUELTO_EL, "Nota sin periodo", null));
        }

        @Test
        @DisplayName("un periodo contable con mes 13 lo para el REGEXP del mismo check")
        void un_periodo_con_mes_13_lo_para_el_regexp() {
            // No hay FK contra accounting_periods que pueda salvarlo: esa tabla no
            // existe en el arbol de changesets. El REGEXP es toda la comprobacion que
            // hay en la base.
            EngineConstraint.assertViolates("chk_eir_resolved",
                    () -> insertarCruda("MISSING_EXTERNAL", null, SchemaSeed.SYSTEM_USER_ID,
                            RESUELTO_EL, "Nota con periodo imposible", "2026-13"));
        }

        @Test
        @DisplayName("un total propio negativo lo para chk_eir_amounts")
        void un_total_propio_negativo_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_eir_amounts", () -> {
                entityManager.createNativeQuery("""
                        INSERT INTO external_invoice_reconciliations
                            (company_id, billing_document_id, computed_total, computed_tax,
                             status, created_date, version)
                        VALUES (:companyId, :documento, -1.00, 0.00, 'MISSING_EXTERNAL',
                                :creadoEl, 0)
                        """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                        .setParameter("documento", OTRO_DOCUMENTO_PROPIO)
                        .setParameter("creadoEl", CREADO_EL).executeUpdate();
            });
        }
    }

    @Nested
    @DisplayName("Bandeja de MISSING_EXTERNAL")
    class BandejaDeMissingExternal {

        @Test
        @DisplayName("trae solo lo que nadie facturo, y lo mas antiguo primero")
        void trae_solo_lo_que_nadie_facturo_y_lo_mas_antiguo_primero() {
            // Lo que lleva mas dias devengado sin factura externa es lo primero que hay
            // que mirar; ponerlo al final es como se pierde de vista. Ademas es el orden
            // natural de ix_eir_pending (status, created_date).
            ExternalInvoiceReconciliation antigua = repository
                    .save(abiertaEn(DOCUMENTO_PROPIO, CREADO_ANTES));
            ExternalInvoiceReconciliation reciente = repository
                    .save(abiertaEn(OTRO_DOCUMENTO_PROPIO, CREADO_DESPUES));

            // Esta tercera SI recibio su factura: no puede aparecer en la bandeja.
            ExternalInvoiceReconciliation conciliada = repository
                    .save(abiertaEn(TERCER_DOCUMENTO_PROPIO, CREADO_EL));
            conciliada.match("FE-1043", null, TOTAL_PROPIO, IMPUESTO_PROPIO, null, null, null,
                    null);
            repository.save(conciliada);
            entityManager.flush();
            entityManager.clear();

            PageResult<ExternalInvoiceReconciliation> bandeja = repository
                    .findAllByStatus(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, 0, 20);

            assertThat(bandeja.content()).extracting(ExternalInvoiceReconciliation::getId)
                    .containsExactly(antigua.getId(), reciente.getId());
            assertThat(bandeja.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("la bandeja cruza empresas a proposito: la pregunta es que se quedo sin facturar")
        void la_bandeja_cruza_empresas_a_proposito() {
            // No es una fuga: el puerto que la sirve esta cerrado a hasRole('SYSTEM') a
            // secas, y la pregunta no es «que se le quedo sin facturar a esta clinica»
            // sino «que se me quedo sin facturar».
            repository.save(abiertaEn(DOCUMENTO_PROPIO, CREADO_ANTES));
            repository.save(ExternalInvoiceReconciliation.open(SchemaSeed.OTRA_COMPANY_ID,
                    DOCUMENTO_AJENO, TOTAL_PROPIO, IMPUESTO_PROPIO, CREADO_DESPUES));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository
                    .findAllByStatus(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, 0, 20)
                    .content()).extracting(ExternalInvoiceReconciliation::getCompanyId)
                    .containsExactly(SchemaSeed.COMPANY_ID, SchemaSeed.OTRA_COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el barrido ordena por fecha de apertura descendente y desempata por id")
        void el_barrido_ordena_por_fecha_descendente_y_desempata_por_id() {
            ExternalInvoiceReconciliation antigua = repository
                    .save(abiertaEn(DOCUMENTO_PROPIO, CREADO_ANTES));
            // Estas dos comparten instante EXACTO: sin el id de desempate el orden entre
            // ellas seria el que quiera el motor y la paginacion podria repetir o perder
            // una.
            ExternalInvoiceReconciliation empateA = repository
                    .save(abiertaEn(OTRO_DOCUMENTO_PROPIO, CREADO_DESPUES));
            ExternalInvoiceReconciliation empateB = repository
                    .save(abiertaEn(TERCER_DOCUMENTO_PROPIO, CREADO_DESPUES));
            entityManager.flush();
            entityManager.clear();

            PageResult<ExternalInvoiceReconciliation> pagina = repository
                    .findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(ExternalInvoiceReconciliation::getId)
                    .containsExactly(empateB.getId(), empateA.getId(), antigua.getId());
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("el barrido acotado no trae las de otra empresa")
        void el_barrido_acotado_no_trae_las_de_otra_empresa() {
            repository.save(abiertaEn(DOCUMENTO_PROPIO, CREADO_EL));
            repository.save(ExternalInvoiceReconciliation.open(SchemaSeed.OTRA_COMPANY_ID,
                    DOCUMENTO_AJENO, TOTAL_PROPIO, IMPUESTO_PROPIO, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .singleElement().satisfies(fila -> assertThat(fila.getBillingDocumentId())
                            .isEqualTo(DOCUMENTO_PROPIO));
            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }
    }

    // --- andamio ------------------------------------------------------------

    private static ExternalInvoiceReconciliation abierta(Long documentoId) {
        return abiertaEn(documentoId, CREADO_EL);
    }

    private static ExternalInvoiceReconciliation abiertaEn(Long documentoId,
            LocalDateTime creadoEl) {
        return ExternalInvoiceReconciliation.open(SchemaSeed.COMPANY_ID, documentoId, TOTAL_PROPIO,
                IMPUESTO_PROPIO, creadoEl);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los CHECK que el dominio
     * ya replica: sin ella no habria forma de comprobar que la base tambien los
     * cuida. Va contra {@code OTRO_DOCUMENTO_PROPIO} para que la unicidad por
     * documento no pueda saltar antes que el CHECK que el caso dice probar.
     */
    private void insertarCruda(String estado, String facturaExterna, Long firmante,
            LocalDateTime resueltoEl, String nota, String periodo) {
        entityManager.createNativeQuery("""
                INSERT INTO external_invoice_reconciliations
                    (company_id, billing_document_id, external_invoice_id, computed_total,
                     computed_tax, status, resolved_by_system_user_id, resolved_at,
                     resolution_note, posting_period, created_date, version)
                VALUES (:companyId, :documento, :facturaExterna, :total, :impuesto, :estado,
                        :firmante, :resueltoEl, :nota, :periodo, :creadoEl, 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("documento", OTRO_DOCUMENTO_PROPIO)
                .setParameter("facturaExterna", facturaExterna).setParameter("total", TOTAL_PROPIO)
                .setParameter("impuesto", IMPUESTO_PROPIO).setParameter("estado", estado)
                .setParameter("firmante", firmante).setParameter("resueltoEl", resueltoEl)
                .setParameter("nota", nota).setParameter("periodo", periodo)
                .setParameter("creadoEl", CREADO_EL).executeUpdate();
    }

    /**
     * Periodo contable OPEN. Sin el, el disparador {@code trg_eir_bi_period_open}
     * del changeset 346 rechaza cualquier fila resuelta con este
     * {@code posting_period} antes de que {@code fk_eir_posting_period} llegue a
     * mirarla siquiera.
     */
    private void periodo(Long id, String claveDelPeriodo) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, created_date, version)
                VALUES (:id, :clave, 'OPEN', NOW(6), 0)
                """).setParameter("id", id).setParameter("clave", claveDelPeriodo).executeUpdate();
    }

    /**
     * Documento de cobro minimo. {@code chk_sbd_total} exige
     * {@code total = subtotal + impuesto}; {@code balance_amount},
     * {@code recurring_cycle_marker} y {@code overdue_marker} son
     * {@code GENERATED ALWAYS} y no se nombran.
     */
    private void documento(Long id, String numero, Long companyId, Long subscriptionId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents
                    (id, document_number, company_id, subscription_id, document_kind,
                     billing_reason, period_start, period_end, issue_status, subtotal_amount,
                     tax_amount, total_amount, settled_amount, created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', 'ONE_TIME',
                        :inicio, :fin, 'DRAFT', 100000.00, 19000.00, 119000.00, 0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("inicio", LocalDate.of(2026, 2, 1))
                .setParameter("fin", LocalDate.of(2026, 2, 28)).executeUpdate();
    }
}
