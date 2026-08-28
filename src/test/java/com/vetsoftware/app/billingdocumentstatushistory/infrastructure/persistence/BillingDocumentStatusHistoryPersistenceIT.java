package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaBillingDocumentStatusHistoryRepository} contra MySQL
 * real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las barandillas que solo el
 * motor tiene</b>, y la principal es {@code chk_bdsh_transition}. El dominio ya
 * rechaza una transicion al mismo estado, asi que la unica forma de comprobar
 * que la base tambien la cuida —el cinturon bajo el tirante— es escribir la
 * fila por SQL nativo, saltandose el agregado. Si manana alguien quitara ese
 * {@code CHECK} de la migracion, los tests de dominio seguirian verdes y solo
 * este se pondria rojo.
 *
 * <p>
 * La otra es la <b>FK compuesta</b> {@code (company_id, billing_document_id)}.
 * Con una FK simple contra el {@code id} del documento, un fotograma podria
 * colgar de la factura de otra empresa y solo la aplicacion lo impediria;
 * {@link Tenancy#un_fotograma_no_cuelga_de_la_factura_de_otra_empresa()}
 * congela que el motor tambien.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola. Instanciarlo con el
 * {@code BillingDocumentStatusHistoryJpaRepository} que la rodaja ya expone
 * cuesta una linea y no ejercita menos SQL: la consulta que se ejecuta contra
 * MySQL es exactamente la misma. Es el mismo criterio que
 * {@code BankReceiptPersistenceIT} y {@code DocumentWithholdingPersistenceIT}.
 *
 * <p>
 * <b>El seed no trae tablas de facturacion.</b> {@code SchemaSeed} satisface
 * claves foraneas y se detiene antes; los documentos de cobro se insertan con
 * SQL nativo y con ids del rango 8500, que ninguna otra rodaja usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBillingDocumentStatusHistoryRepository — la pelicula contra MySQL real")
class BillingDocumentStatusHistoryPersistenceIT extends AbstractDataJpaTest {

    private static final Long DOCUMENTO_PROPIO = 8500L;
    private static final Long OTRO_DOCUMENTO_PROPIO = 8501L;
    private static final Long DOCUMENTO_AJENO = 8502L;

    private static final LocalDateTime OCURRIO_EL = LocalDateTime.of(2026, 3, 5, 9, 30, 0);

    /**
     * Distinto de {@link #OCURRIO_EL} a proposito: si el mapper cruzara
     * {@code occurred_at} con {@code created_date}, con la misma fecha en las dos
     * no se veria.
     */
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 14, 15, 0);

    private static final String ACTOR = "Laura Restrepo";
    private static final String MOTIVO = "Factura externa FE-1043 registrada";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private BillingDocumentStatusHistoryJpaRepository jpaRepository;

    private JpaBillingDocumentStatusHistoryRepository repository;

    @BeforeEach
    void seed() {
        repository = new JpaBillingDocumentStatusHistoryRepository(jpaRepository,
                new BillingDocumentStatusHistoryJpaMapper());
        SchemaSeed.seed(entityManager);
        // Cada documento recurrente de la MISMA suscripcion necesita su propio
        // periodo: uq_sbd_recurring_cycle los separa por (marcador, desde, hasta).
        documento(DOCUMENTO_PROPIO, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "FAC-8500",
                "2026-02-01", "2026-02-28");
        documento(OTRO_DOCUMENTO_PROPIO, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                "FAC-8501", "2026-03-01", "2026-03-31");
        documento(DOCUMENTO_AJENO, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID,
                "FAC-8502", "2026-02-01", "2026-02-28");
        entityManager.flush();
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el fotograma y lo recupera con cada campo y cada instante en su sitio")
        void guarda_el_fotograma_y_lo_recupera_campo_a_campo() {
            BillingDocumentStatusHistory guardado = repository
                    .save(fotograma(BillingDocumentStatus.DRAFT,
                            BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperado.getBillingDocumentId()).isEqualTo(DOCUMENTO_PROPIO);
                        assertThat(recuperado.getFromStatus())
                                .isEqualTo(BillingDocumentStatus.DRAFT);
                        assertThat(recuperado.getToStatus())
                                .isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
                        assertThat(recuperado.getOccurredAt()).isEqualTo(OCURRIO_EL);
                        assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                        assertThat(recuperado.getActor()).isEqualTo(ACTOR);
                        assertThat(recuperado.getReason()).isEqualTo(MOTIVO);
                    });
        }

        @Test
        @DisplayName("dos fotogramas del mismo documento y el mismo par de estados SI caben")
        void dos_fotogramas_del_mismo_par_de_estados_si_caben() {
            // No hay unicidad y no debe haberla: un documento puede ir y volver del
            // mismo par de estados varias veces —se emite, se anula, se vuelve a
            // emitir— y una clave unica por (documento, from, to) rechazaria historia
            // legitima. Lo que los distingue es occurred_at.
            repository.save(fotograma(BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            repository.save(fotograma(BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL.plusDays(10)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID,
                    DOCUMENTO_PROPIO, 0, 20).totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("la precision de microsegundos del instante sobrevive al viaje")
        void la_precision_de_microsegundos_sobrevive_al_viaje() {
            // La columna es DATETIME(6). Si alguien la degradara a DATETIME, varios
            // movimientos del mismo segundo colapsarian al mismo instante y el orden
            // entre ellos lo decidiria solo el desempate por id.
            LocalDateTime conMicros = OCURRIO_EL.withNano(123_456_000);
            BillingDocumentStatusHistory guardado = repository.save(fotograma(
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, conMicros));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> assertThat(recuperado.getOccurredAt())
                            .isEqualTo(conMicros));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una transicion al mismo estado la para chk_bdsh_transition en la base")
        void una_transicion_al_mismo_estado_la_para_el_check() {
            // El dominio ya la rechaza, asi que la unica forma de comprobar que la base
            // tambien la cuida es escribir la fila por SQL nativo. Si el CHECK
            // desapareciera de la migracion, este caso —y solo este— se pone rojo.
            EngineConstraint.assertViolates("chk_bdsh_transition",
                    () -> insertaCrudo("DRAFT", "DRAFT"));
        }

        @Test
        @DisplayName("un estado que no esta en la lista cerrada lo para chk_bdsh_statuses")
        void un_estado_fuera_de_la_lista_lo_para_el_check() {
            // El enum de esta feature es una copia del de subscriptionbilling y el
            // CHECK es lo que impide que la copia derive: un quinto valor que la tabla
            // de documentos no admite no entra aqui tampoco.
            EngineConstraint.assertViolates("chk_bdsh_statuses",
                    () -> insertaCrudo("DRAFT", "PARTIALLY_PAID"));
        }

        @Test
        @DisplayName("un estado de origen fuera de la lista lo para el mismo check")
        void un_estado_de_origen_fuera_de_la_lista_lo_para_el_mismo_check() {
            // La otra mitad del CHECK: un control que solo mirara to_status pasaria por
            // bueno un fotograma que viene de un estado imposible, y el tramo anterior
            // de la pelicula no empalmaria con nada.
            EngineConstraint.assertViolates("chk_bdsh_statuses",
                    () -> insertaCrudo("PARTIALLY_PAID", "VOIDED"));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            BillingDocumentStatusHistory guardado = repository
                    .save(fotograma(BillingDocumentStatus.DRAFT,
                            BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("un fotograma no cuelga de la factura de otra empresa: fk_bdsh_document")
        void un_fotograma_no_cuelga_de_la_factura_de_otra_empresa() {
            // DOCUMENTO_AJENO existe de verdad, solo que bajo OTRA_COMPANY_ID. La
            // empresa y el resto de datos son validos, asi que la unica barandilla que
            // puede pararlo es la FK COMPUESTA (company_id, billing_document_id). Si
            // manana alguien la degradara a una FK simple contra el id del documento,
            // esta fila entraria y el caso se pondria rojo.
            EngineConstraint.assertViolates("fk_bdsh_document", () -> {
                repository.save(new BillingDocumentStatusHistory(null, SchemaSeed.COMPANY_ID,
                        DOCUMENTO_AJENO, BillingDocumentStatus.DRAFT,
                        BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL, ACTOR, MOTIVO,
                        CREADO_EL));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la pelicula de una empresa no ve la de la vecina")
        void la_pelicula_de_una_empresa_no_ve_la_de_la_vecina() {
            repository.save(fotograma(BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            repository.save(new BillingDocumentStatusHistory(null, SchemaSeed.OTRA_COMPANY_ID,
                    DOCUMENTO_AJENO, BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL, ACTOR, MOTIVO, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyIdAndToStatus(SchemaSeed.COMPANY_ID,
                    BillingDocumentStatus.AWAITING_EXTERNAL, 0, 20).content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getBillingDocumentId())
                            .isEqualTo(DOCUMENTO_PROPIO));
            // Y el barrido de plataforma si las ve las dos: es la diferencia entre los
            // dos casos de uso, y la razon de que sean dos puertos y no uno.
            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("la pelicula sale en orden de proyeccion y desempata por id")
        void la_pelicula_sale_en_orden_de_proyeccion() {
            BillingDocumentStatusHistory primero = repository
                    .save(fotograma(BillingDocumentStatus.DRAFT,
                            BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            // Los dos siguientes comparten instante EXACTO, que es lo que pasa cuando
            // los escribe el proceso automatico en la misma transaccion. Sin el id de
            // desempate el orden entre ellos seria el que quiera el motor y la
            // paginacion podria repetir o perder uno — y una bitacora que pierde
            // fotogramas al paginar es peor que no tenerla: nadie sabe que falta.
            BillingDocumentStatusHistory empateA = repository
                    .save(fotograma(BillingDocumentStatus.AWAITING_EXTERNAL,
                            BillingDocumentStatus.EXTERNAL_REGISTERED, OCURRIO_EL.plusDays(2)));
            BillingDocumentStatusHistory empateB = repository
                    .save(fotograma(BillingDocumentStatus.EXTERNAL_REGISTERED,
                            BillingDocumentStatus.VOIDED, OCURRIO_EL.plusDays(2)));
            entityManager.flush();
            entityManager.clear();

            PageResult<BillingDocumentStatusHistory> pelicula = repository
                    .findAllByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID, DOCUMENTO_PROPIO,
                            0, 20);

            assertThat(pelicula.content()).extracting(BillingDocumentStatusHistory::getId)
                    .containsExactly(primero.getId(), empateA.getId(), empateB.getId());
            assertThat(pelicula.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("la pelicula de un documento no arrastra la de otro de la misma empresa")
        void la_pelicula_de_un_documento_no_arrastra_la_de_otro() {
            repository.save(fotograma(BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            repository.save(new BillingDocumentStatusHistory(null, SchemaSeed.COMPANY_ID,
                    OTRO_DOCUMENTO_PROPIO, BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.VOIDED, OCURRIO_EL, ACTOR, MOTIVO, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyIdAndBillingDocumentId(SchemaSeed.COMPANY_ID,
                    DOCUMENTO_PROPIO, 0, 20).content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getToStatus())
                            .isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL));
        }

        @Test
        @DisplayName("la bandeja por estado solo trae los que quedaron en ese estado")
        void la_bandeja_por_estado_solo_trae_los_de_ese_estado() {
            // Es la consulta que justifica la ficha: los que estan esperando factura
            // externa. Mezclarla con los ya registrados daria una cifra a reclamar que
            // no corresponde a ningun corte.
            repository.save(fotograma(BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            repository.save(fotograma(BillingDocumentStatus.AWAITING_EXTERNAL,
                    BillingDocumentStatus.EXTERNAL_REGISTERED, OCURRIO_EL.plusDays(2)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyIdAndToStatus(SchemaSeed.COMPANY_ID,
                    BillingDocumentStatus.AWAITING_EXTERNAL, 0, 20).content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getFromStatus())
                            .isEqualTo(BillingDocumentStatus.DRAFT));
        }

        @Test
        @DisplayName("la bandeja de la empresa ordena lo mas reciente primero")
        void la_bandeja_de_la_empresa_ordena_lo_mas_reciente_primero() {
            BillingDocumentStatusHistory antiguo = repository
                    .save(fotograma(BillingDocumentStatus.DRAFT,
                            BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL));
            BillingDocumentStatusHistory reciente = repository
                    .save(fotograma(BillingDocumentStatus.AWAITING_EXTERNAL,
                            BillingDocumentStatus.EXTERNAL_REGISTERED, OCURRIO_EL.plusDays(9)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(BillingDocumentStatusHistory::getId)
                    .containsExactly(reciente.getId(), antiguo.getId());
        }
    }

    // --- andamio ------------------------------------------------------------

    private BillingDocumentStatusHistory fotograma(BillingDocumentStatus desde,
            BillingDocumentStatus hasta, LocalDateTime ocurrioEl) {
        return new BillingDocumentStatusHistory(null, SchemaSeed.COMPANY_ID, DOCUMENTO_PROPIO,
                desde, hasta, ocurrioEl, ACTOR, MOTIVO, CREADO_EL);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los {@code CHECK} que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * los cuida.
     */
    private void insertaCrudo(String desde, String hasta) {
        entityManager.createNativeQuery("""
                INSERT INTO billing_document_status_history (company_id, billing_document_id,
                                                             from_status, to_status, occurred_at,
                                                             actor, reason, created_date)
                VALUES (:companyId, :documentoId, :desde, :hasta, :ocurrioEl, :actor, :motivo,
                        :creadoEl)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("documentoId", DOCUMENTO_PROPIO).setParameter("desde", desde)
                .setParameter("hasta", hasta).setParameter("ocurrioEl", OCURRIO_EL)
                .setParameter("actor", ACTOR).setParameter("motivo", MOTIVO)
                .setParameter("creadoEl", CREADO_EL).executeUpdate();
    }

    /**
     * Un documento de cobro del que colgar historia.
     *
     * <p>
     * <strong>El periodo es parametro y no una constante</strong>: los documentos
     * recurrentes de una misma suscripcion los separa
     * {@code uq_sbd_recurring_cycle}, que es
     * {@code (recurring_cycle_marker, period_start, period_end)} — y el marcador se
     * genera a partir de {@code subscription_id} cuando la razon es
     * {@code RECURRING_CYCLE}. Con el periodo fijo, el segundo documento de la
     * misma suscripcion choca con el primero y la siembra entera muere con un
     * {@code Duplicate entry} que no menciona esta bitacora.
     */
    private void documento(Long id, Long companyId, Long subscriptionId, String numero,
            String periodoDesde, String periodoHasta) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', 'RECURRING_CYCLE',
                        :desde, :hasta, 'DRAFT', 1234567.89, 0.00, 1234567.89,
                        0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("desde", periodoDesde).setParameter("hasta", periodoHasta)
                .executeUpdate();
    }
}
