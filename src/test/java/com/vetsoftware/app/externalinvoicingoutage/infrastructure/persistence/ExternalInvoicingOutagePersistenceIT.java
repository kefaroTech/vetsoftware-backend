package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
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
 * Rodaja de {@code JpaExternalInvoicingOutageRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es {@code uq_eio_open}</b>, que es
 * la unica invariante de la feature que Java no puede cuidar y que ninguna
 * prueba de servicio puede ver. Se apoya en la columna generada
 * {@code open_outage_marker} —el causante mientras {@code ended_at} es nulo,
 * {@code NULL} en cuanto se cierra— y de ella dependen las tres mitades que
 * aqui se congelan: que dos caidas abiertas del mismo causante <em>chocan</em>,
 * que dos causantes distintos <em>coexisten</em>, y que cerrar una <em>libera
 * el hueco</em>. Sin la tercera, «cerrar» no significaria nada.
 *
 * <p>
 * El escenario que la justifica esta escrito en el changeset 358: sin el
 * marcador, el proceso de deteccion abre una caida nueva en cada sondeo y deja
 * un rastro de caidas vivas que nunca se cierran. Y el defecto solo aparece con
 * el motor delante — un {@code exists} previo en el service lo pasarian las dos
 * peticiones concurrentes.
 *
 * <p>
 * <b>No se siembra nada.</b> {@code external_invoicing_outages} no tiene
 * {@code company_id} ni ninguna clave foranea, asi que no hace falta
 * {@code SchemaSeed}: la tabla nace vacia en cada caso y el
 * {@code @DataJpaTest} revierte al terminar.
 *
 * <p>
 * <b>Por que no hay {@code @Import} propio.</b> {@code PersistenceSliceConfig}
 * reune los adaptadores de todas las rodajas para que compartan una unica clave
 * de {@code MergedContextConfiguration} y, con ella, un unico contexto
 * cacheado. Declarar aqui un {@code @Import} con este adaptador volveria a
 * darle a esta clase una clave unica y un arranque de contexto entero para ella
 * sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaExternalInvoicingOutageRepository — las caidas de la emision contra MySQL real")
class ExternalInvoicingOutagePersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja, para las escrituras crudas. */
    private static final Long CAIDA_CRUDA = 8700L;

    private static final LocalDateTime EMPEZO = LocalDateTime.of(2026, 3, 10, 8, 15, 0);
    private static final LocalDateTime TERMINO = LocalDateTime.of(2026, 3, 10, 14, 42, 30);
    private static final LocalDateTime AVISADO = LocalDateTime.of(2026, 3, 10, 9, 0, 0);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 10, 8, 16, 0);

    @Autowired
    private ExternalInvoicingOutageJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaExternalInvoicingOutageRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaExternalInvoicingOutageRepository(springDataRepository,
                new ExternalInvoicingOutageJpaMapper());
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("guarda la caida y la recupera campo a campo, viva y sin aviso")
        void guarda_la_caida_y_la_recupera_campo_a_campo() {
            ExternalInvoicingOutage guardada = repository
                    .save(abierta(CauseParty.EXTERNAL_ISSUER, "El proveedor no responde"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getStartedAt()).isEqualTo(EMPEZO);
                assertThat(recuperada.getEndedAt()).isNull();
                assertThat(recuperada.getCauseParty()).isEqualTo(CauseParty.EXTERNAL_ISSUER);
                assertThat(recuperada.getSummary()).isEqualTo("El proveedor no responde");
                assertThat(recuperada.getAffectedCompanyCount()).isEqualTo(40);
                assertThat(recuperada.getNotifiedCompaniesAt()).isNull();
                assertThat(recuperada.getExternalIncidentRef()).isEqualTo("INC-2026-0310");
                assertThat(recuperada.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperada.getVersion()).isZero();
                assertThat(recuperada.isOpen()).isTrue();
            });
        }

        @Test
        @DisplayName("el aviso a las clinicas se anota sobre la caida viva y mueve la version")
        void el_aviso_se_anota_sobre_la_caida_viva() {
            ExternalInvoicingOutage guardada = repository
                    .save(abierta(CauseParty.NETWORK, "Corte de transporte"));
            entityManager.flush();
            entityManager.clear();

            ExternalInvoicingOutage cargada = repository.findById(guardada.getId()).orElseThrow();
            repository.save(cargada.notifyCompanies(AVISADO, 52));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(avisada -> {
                assertThat(avisada.getNotifiedCompaniesAt()).isEqualTo(AVISADO);
                // El contador se corrige al avisar: la primera estimacion en caliente
                // eran 40, y al mandar el correo ya se sabia que eran 52.
                assertThat(avisada.getAffectedCompanyCount()).isEqualTo(52);
                assertThat(avisada.isOpen()).isTrue();
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no
                // por una escritura masiva, que la dejaria intacta.
                assertThat(avisada.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("cerrar escribe la hora de vuelta y deja la caida fuera de las abiertas")
        void cerrar_escribe_la_hora_de_vuelta() {
            ExternalInvoicingOutage guardada = repository
                    .save(abierta(CauseParty.AUTHORITY, "La DIAN no valida"));
            entityManager.flush();
            entityManager.clear();

            ExternalInvoicingOutage cargada = repository.findById(guardada.getId()).orElseThrow();
            repository.save(cargada.end(TERMINO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(cerrada -> {
                assertThat(cerrada.getEndedAt()).isEqualTo(TERMINO);
                assertThat(cerrada.isOpen()).isFalse();
                assertThat(cerrada.getVersion()).isEqualTo(1L);
            });
            assertThat(repository.findAllOpen()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("dos caidas abiertas del MISMO causante las para uq_eio_open")
        void dos_caidas_abiertas_del_mismo_causante_chocan() {
            // La invariante central del changeset 358. Sin ella, el proceso de
            // deteccion abre una caida nueva en cada sondeo y deja un rastro de
            // caidas vivas que nunca se cierran.
            repository.save(abierta(CauseParty.EXTERNAL_ISSUER, "Primera deteccion"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_eio_open", () -> {
                repository.save(abierta(CauseParty.EXTERNAL_ISSUER, "Segundo sondeo"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos caidas abiertas de causantes DISTINTOS si coexisten")
        void dos_caidas_de_causantes_distintos_coexisten() {
            // La otra mitad, y la que impide que alguien «arregle» la unicidad
            // poniendo una constante en el marcador: el emisor y la red pueden estar
            // caidos a la vez y son dos hechos distintos, con dos responsables.
            repository.save(abierta(CauseParty.EXTERNAL_ISSUER, "El proveedor no responde"));
            repository.save(abierta(CauseParty.NETWORK, "Corte de transporte"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllOpen()).extracting(ExternalInvoicingOutage::getCauseParty)
                    .containsExactlyInAnyOrder(CauseParty.EXTERNAL_ISSUER, CauseParty.NETWORK);
        }

        @Test
        @DisplayName("cerrar una LIBERA el hueco: el mismo causante puede volver a caerse")
        void cerrar_libera_el_hueco_del_causante() {
            // Esto es lo que hace que «cerrar» signifique algo. Al escribir ended_at
            // la columna generada pasa a NULL, y MySQL admite multiples NULL en un
            // indice unico: el causante queda libre para la caida siguiente.
            ExternalInvoicingOutage primera = repository
                    .save(abierta(CauseParty.EXTERNAL_ISSUER, "Caida de la manana"));
            entityManager.flush();
            entityManager.clear();

            ExternalInvoicingOutage cargada = repository.findById(primera.getId()).orElseThrow();
            repository.save(cargada.end(TERMINO));
            entityManager.flush();
            entityManager.clear();

            ExternalInvoicingOutage segunda = repository.save(
                    ExternalInvoicingOutage.open(TERMINO.plusHours(2), CauseParty.EXTERNAL_ISSUER,
                            "Caida de la tarde", 12, "INC-2026-0310-B", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(segunda.getId())).get()
                    .satisfies(viva -> assertThat(viva.isOpen()).isTrue());
            assertThat(repository.findAllOpen()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una caida que termina antes de empezar la para chk_eio_ended")
        void una_caida_que_termina_antes_de_empezar_la_para_el_check() {
            // El dominio ya lo rechaza, asi que la unica forma de comprobar que la
            // base tambien lo hace —el cinturon bajo el tirante— es escribir la fila
            // por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_eio_ended", () -> insertarCruda(CAIDA_CRUDA,
                    EMPEZO, EMPEZO.minusMinutes(1), "NETWORK", 0, null));
        }

        @Test
        @DisplayName("un aviso anterior al inicio lo para chk_eio_notified")
        void un_aviso_anterior_al_inicio_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_eio_notified", () -> insertarCruda(CAIDA_CRUDA + 1,
                    EMPEZO, null, "NETWORK", 0, EMPEZO.minusMinutes(1)));
        }

        @Test
        @DisplayName("un contador de alcanzadas negativo lo para chk_eio_count")
        void un_contador_negativo_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_eio_count",
                    () -> insertarCruda(CAIDA_CRUDA + 2, EMPEZO, null, "NETWORK", -1, null));
        }

        @Test
        @DisplayName("un causante fuera de los cuatro lo para chk_eio_cause")
        void un_causante_desconocido_lo_para_el_check() {
            // La red del enum: si alguien anade un valor a CauseParty sin el changeset
            // que lo admita, el INSERT muere en vez de guardar un causante que la
            // consulta de la autoridad no sabra clasificar.
            EngineConstraint.assertViolates("chk_eio_cause",
                    () -> insertarCruda(CAIDA_CRUDA + 3, EMPEZO, null, "PROVIDER", 0, null));
        }

        @Test
        @DisplayName("escribir open_outage_marker a mano lo rechaza el motor: es generada")
        void escribir_el_marcador_a_mano_lo_rechaza_el_motor() {
            // Por esto la columna NO esta mapeada en la entidad. Mapearla obligaria a
            // insertable=false/updatable=false y, peor, invitaria a escribirla desde
            // Java: el primer INSERT que llevara un valor propio moriria aqui.
            EngineConstraint.assertViolates("open_outage_marker",
                    () -> entityManager.createNativeQuery("""
                            INSERT INTO external_invoicing_outages
                                    (id, started_at, ended_at, cause_party, summary,
                                     affected_company_count, notified_companies_at,
                                     external_incident_ref, open_outage_marker, created_date,
                                     version)
                            VALUES (:id, :empezo, NULL, 'NETWORK', 'Marcador escrito a mano',
                                    0, NULL, NULL, 'NETWORK', :creado, 0)
                            """).setParameter("id", CAIDA_CRUDA + 4).setParameter("empezo", EMPEZO)
                            .setParameter("creado", CREADO_EL).executeUpdate());
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el historico va de la caida mas reciente hacia atras")
        void el_historico_va_de_la_mas_reciente_hacia_atras() {
            // Orden total con desempate por id: sin el, dos paginas consecutivas
            // pueden repetir u omitir filas. Los causantes son distintos porque
            // uq_eio_open no admite dos abiertas del mismo.
            repository.save(ExternalInvoicingOutage.open(EMPEZO, CauseParty.EXTERNAL_ISSUER,
                    "La del medio", 1, null, CREADO_EL));
            repository.save(ExternalInvoicingOutage.open(EMPEZO.plusDays(1), CauseParty.NETWORK,
                    "La mas reciente", 1, null, CREADO_EL));
            repository.save(ExternalInvoicingOutage.open(EMPEZO.minusDays(1), CauseParty.AUTHORITY,
                    "La mas antigua", 1, null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).content())
                    .extracting(ExternalInvoicingOutage::getSummary)
                    .containsExactly("La mas reciente", "La del medio", "La mas antigua");
        }

        @Test
        @DisplayName("las abiertas excluyen las cerradas y van de la mas antigua a la mas nueva")
        void las_abiertas_excluyen_las_cerradas() {
            // El orden es el inverso del historico a proposito: en la bandeja de
            // incidencias vivas lo urgente es lo que lleva mas tiempo caido.
            ExternalInvoicingOutage cerrada = repository.save(ExternalInvoicingOutage.open(
                    EMPEZO.minusDays(1), CauseParty.AUTHORITY, "Ya resuelta", 1, null, CREADO_EL));
            repository.save(ExternalInvoicingOutage.open(EMPEZO, CauseParty.EXTERNAL_ISSUER,
                    "Viva desde antes", 1, null, CREADO_EL));
            repository.save(ExternalInvoicingOutage.open(EMPEZO.plusHours(3), CauseParty.NETWORK,
                    "Viva desde hace poco", 1, null, CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(cerrada.getId()).orElseThrow().end(TERMINO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllOpen()).extracting(ExternalInvoicingOutage::getSummary)
                    .containsExactly("Viva desde antes", "Viva desde hace poco");
        }

        @Test
        @DisplayName("la pagina respeta el tope del kernel de paginacion")
        void la_pagina_respeta_el_tope_del_kernel() {
            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAll(0, 100000).pageSize()).isEqualTo(200);
        }
    }

    private static ExternalInvoicingOutage abierta(CauseParty causante, String resumen) {
        return ExternalInvoicingOutage.open(EMPEZO, causante, resumen, 40, "INC-2026-0310",
                CREADO_EL);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para las comprobaciones que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * las cuida.
     *
     * <p>
     * {@code open_outage_marker} no se nombra: es generada, y listarla haria que el
     * motor rechazara el {@code INSERT} por un motivo que no es el del caso. Los
     * {@code NULL} constantes van como literales porque una consulta nativa sin
     * metadatos de tipo no puede inferir el tipo de un {@code null}.
     */
    private void insertarCruda(Long id, LocalDateTime empezo, LocalDateTime termino,
            String causante, int alcanzadas, LocalDateTime avisado) {
        entityManager.createNativeQuery("""
                INSERT INTO external_invoicing_outages
                        (id, started_at, ended_at, cause_party, summary,
                         affected_company_count, notified_companies_at, external_incident_ref,
                         created_date, version)
                VALUES (:id, :empezo, :termino, :causante, 'Escritura cruda de la rodaja',
                        :alcanzadas, :avisado, NULL, :creado, 0)
                """).setParameter("id", id).setParameter("empezo", empezo)
                .setParameter("termino", termino).setParameter("causante", causante)
                .setParameter("alcanzadas", alcanzadas).setParameter("avisado", avisado)
                .setParameter("creado", CREADO_EL).executeUpdate();
    }
}
