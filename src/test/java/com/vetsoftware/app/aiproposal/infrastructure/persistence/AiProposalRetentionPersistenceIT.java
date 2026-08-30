package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La politica de retencion contra MySQL real.
 *
 * <p>
 * <b>Nada de esto se puede comprobar sin una base de verdad.</b> Las seis
 * consultas son nativas y descansan en cuatro cosas que ningun test unitario
 * ve: el {@code ORDER BY ... LIMIT} sobre {@code UPDATE} y {@code DELETE} —que
 * MySQL solo admite en mutaciones de una sola tabla—, la columna
 * {@code contact_email_hash} que es {@code GENERATED ALWAYS ... STORED} y
 * desaparece sola al vaciar el correo, los {@code CHECK} que pueden rechazar la
 * fila resultante, y las FK {@code ON DELETE RESTRICT} que fijan el orden de la
 * purga. Un mock del repositorio habria dado verde con las seis rotas.
 *
 * <p>
 * <b>Y las tres tablas van versionadas</b>, asi que cada {@code UPDATE} tiene
 * que dejar la {@code version} un punto mas arriba. Sin eso, un {@code save}
 * concurrente que venga de una lectura anterior casa igual y deshace la
 * anonimizacion sin excepcion y sin log: aqui se afirma el incremento, no se da
 * por supuesto porque la cadena SQL lo mencione.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("AiProposalRetentionJpaRepository — la retencion contra MySQL real")
class AiProposalRetentionPersistenceIT extends AbstractDataJpaTest {

    private static final Long PRIVACY_NOTICE_ID = 9_502L;

    private static final String PRIVACY_NOTICE_CODE = "PRIVACY_NOTICE_RETENTION_IT";

    private static final String SNAPSHOT = "0123456789abcdef".repeat(4);

    /**
     * &#9940; El motivo cita al prospecto, porque el prompt lo obliga. Es
     * exactamente la frase que sobrevivia a la anonimizacion en el diseño anterior:
     * la cabecera quedaba marcada como limpia y estas palabras seguian escritas en
     * la tabla de al lado.
     */
    private static final String MOTIVO = "Le vendes a credito a una fundacion que paga a fin de"
            + " mes.";

    private static final String TEXTO_LIBRE = "Tengo una veterinaria en Chapinero y le vendo a"
            + " credito a la fundacion del barrio.";

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"),
            ZoneOffset.UTC);

    /** Posterior a la actividad del fixture: todo lo sembrado es elegible. */
    private static final LocalDateTime CORTE = LocalDateTime.of(2026, 12, 31, 0, 0);

    /** Anterior a todo: nada es elegible. */
    private static final LocalDateTime CORTE_QUE_NO_ALCANZA = LocalDateTime.of(2020, 1, 1, 0, 0);

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 31, 3, 55);

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private AiProposalRetentionJpaRepository retention;
    @Autowired
    private AiProposalJpaRepository proposalJpaRepository;
    @Autowired
    private AiProposalTurnJpaRepository turnJpaRepository;
    @Autowired
    private AiProposalLineJpaRepository lineJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaAiProposalRepository repository;
    private Long catalogItemCoreId;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        sembrarAvisoDePrivacidad();
        catalogItemCoreId = SchemaSeed.catalogItemId(entityManager, "CORE");
        repository = new JpaAiProposalRepository(proposalJpaRepository, turnJpaRepository,
                lineJpaRepository, new AiProposalJpaMapper());
    }

    private void sembrarAvisoDePrivacidad() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO legal_document_versions
                    (id, code, document_version, kind, title, content, content_hash,
                     published_at, published_by_system_user_id, effective_from, created_date,
                     version)
                VALUES (:id, :code, 1, 'PRIVACY_NOTICE', 'Aviso de privacidad',
                        'Texto del aviso para la rodaja de retencion.',
                        :hash, '2026-01-01 00:00:00', :autor, '2026-01-01', NOW(), 0)
                """).setParameter("id", PRIVACY_NOTICE_ID).setParameter("code", PRIVACY_NOTICE_CODE)
                .setParameter("hash", "abcdef9876543210".repeat(4))
                .setParameter("autor", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
    }

    private static String token() {
        String semilla = "RetenIT" + SECUENCIA.incrementAndGet();
        return (semilla + "_".repeat(43)).substring(0, 43);
    }

    /**
     * Cabecera + turno cerrado con texto y respuesta cruda + una linea con motivo.
     */
    private AiProposal propuestaCompleta(String correo) {
        AiProposal guardada = repository.save(AiProposal.create(token(), SchemaSeed.PRICE_LIST_ID,
                ProposalBillingCycle.MONTHLY, SNAPSHOT, PRIVACY_NOTICE_ID,
                UUID.randomUUID().toString(), correo, "es-CO", 14, RELOJ));
        ProposalTurn turno = repository.saveTurn(ProposalTurn.pendienteDeModelo(guardada.getId(), 1,
                TurnType.MODEL_INITIAL, TEXTO_LIBRE, "claude-sonnet-5", "p-2026-08",
                UUID.randomUUID().toString(), RELOJ));
        turno.cerrarConExito(820, 240, 3_400, "end_turn", "{\"necesarios\":[\"CORE\"]}", RELOJ);
        ProposalTurn cerrado = repository.saveTurn(turno);
        repository.saveLines(List.of(new ProposalLine(null, cerrado.getId(), "CORE",
                catalogItemCoreId, LineAction.ADDED, LineSource.MODEL, LineVerdict.ACCEPTED, 1,
                new BigDecimal("46000.00"), MOTIVO, null, 0, LocalDateTime.now(RELOJ), null)));
        entityManager.flush();
        entityManager.clear();
        return guardada;
    }

    private Object columna(String tabla, String columna, Long id) {
        return entityManager
                .createNativeQuery("SELECT " + columna + " FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult();
    }

    private Long unicoTurno(Long proposalId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM ai_proposal_turns WHERE proposal_id = :id")
                .setParameter("id", proposalId).getSingleResult()).longValue();
    }

    private Long unicaLinea(Long turnId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM ai_proposal_lines WHERE turn_id = :id")
                .setParameter("id", turnId).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("Anonimizacion")
    class Anonimizacion {

        @Test
        @DisplayName("vacia el correo y la clave de idempotencia, y mueve la version")
        void vacia_el_correo_y_mueve_la_version() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.anonymizeProposals(CORTE, AHORA, 100)).isEqualTo(1);
            entityManager.clear();

            assertThat(columna("ai_proposals", "contact_email", propuesta.getId())).isNull();
            assertThat(columna("ai_proposals", "idempotency_key", propuesta.getId())).isNull();
            assertThat(columna("ai_proposals", "anonymized_at", propuesta.getId())).isNotNull();
            assertThat(((Number) columna("ai_proposals", "version", propuesta.getId())).longValue())
                    .as("un UPDATE masivo que no mueve la version deja pasar el save concurrente"
                            + " que la deshace, sin excepcion y sin log")
                    .isEqualTo(1L);
        }

        /**
         * {@code contact_email_hash} es {@code GENERATED ALWAYS ... STORED}: se va sola
         * al vaciar el correo. Es la razon por la que la supresion dirigida tiene que
         * ordenar sus pasos al reves que el barrido.
         */
        @Test
        @DisplayName("la columna generada del hash se va sola con el correo")
        void el_hash_generado_se_va_con_el_correo() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");

            retention.anonymizeProposals(CORTE, AHORA, 100);
            entityManager.clear();

            assertThat(columna("ai_proposals", "contact_email_hash", propuesta.getId())).isNull();
        }

        @Test
        @DisplayName("no toca lo que todavia esta dentro del plazo")
        void no_toca_lo_que_esta_dentro_del_plazo() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.anonymizeProposals(CORTE_QUE_NO_ALCANZA, AHORA, 100)).isZero();
        }

        @Test
        @DisplayName("es idempotente: la segunda pasada no vuelve a marcar nada")
        void es_idempotente() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.anonymizeProposals(CORTE, AHORA, 100)).isEqualTo(1);
            assertThat(retention.anonymizeProposals(CORTE, AHORA, 100)).isZero();
        }

        /**
         * El {@code LIMIT} es lo que impide que la primera pasada despues de un pico
         * escriba cientos de miles de filas en una transaccion.
         */
        @Test
        @DisplayName("el lote acota de verdad: tres elegibles y un lote de dos deja una")
        void el_lote_acota() {
            propuestaCompleta("uno@vet.co");
            propuestaCompleta("dos@vet.co");
            propuestaCompleta("tres@vet.co");

            assertThat(retention.anonymizeProposals(CORTE, AHORA, 2)).isEqualTo(2);
            assertThat(retention.anonymizeProposals(CORTE, AHORA, 2)).isEqualTo(1);
        }

        @Test
        @DisplayName("el texto libre y la respuesta cruda del turno se borran, con su version")
        void redacta_el_turno() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");
            retention.anonymizeProposals(CORTE, AHORA, 100);
            entityManager.clear();
            Long turnoId = unicoTurno(propuesta.getId());

            assertThat(retention.redactTurns(100)).isEqualTo(1);
            entityManager.clear();

            assertThat(columna("ai_proposal_turns", "input_text", turnoId)).isNull();
            assertThat(columna("ai_proposal_turns", "raw_response", turnoId)).isNull();
            assertThat(columna("ai_proposal_turns", "input_text_chars", turnoId))
                    .as("el contador de caracteres no es dato personal y es la unica señal que"
                            + " queda de cuanto escribio el prospecto")
                    .isNotNull();
            assertThat(((Number) columna("ai_proposal_turns", "version", turnoId)).longValue())
                    .isEqualTo(2L);
        }

        /**
         * &#9940; La prueba del defecto que cierra esta fase. Antes de
         * {@code reason_redacted_at}, una fila con {@code anonymized_at} puesto seguia
         * llevando las palabras del prospecto dentro, y el informe de cumplimiento
         * decia que estaba limpia.
         */
        @Test
        @DisplayName("el motivo, que cita al prospecto, se borra y queda sellado")
        void redacta_el_motivo_que_cita_al_prospecto() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");
            retention.anonymizeProposals(CORTE, AHORA, 100);
            entityManager.clear();
            Long lineaId = unicaLinea(unicoTurno(propuesta.getId()));
            assertThat(columna("ai_proposal_lines", "reason", lineaId)).isEqualTo(MOTIVO);

            assertThat(retention.redactLineReasons(AHORA, 100)).isEqualTo(1);
            entityManager.clear();

            assertThat(columna("ai_proposal_lines", "reason", lineaId)).isNull();
            assertThat(columna("ai_proposal_lines", "reason_redacted_at", lineaId)).isNotNull();
            assertThat(columna("ai_proposal_lines", "source", lineaId))
                    .as("reescribir el source para saltarse el CHECK destruiria la unica señal"
                            + " que dice que propuso el modelo")
                    .isEqualTo("MODEL");
            assertThat(((Number) columna("ai_proposal_lines", "version", lineaId)).longValue())
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("no redacta motivos de propuestas que aun no estan anonimizadas")
        void no_redacta_lo_que_no_esta_anonimizado() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.redactTurns(100)).isZero();
            assertThat(retention.redactLineReasons(AHORA, 100)).isZero();
        }
    }

    @Nested
    @DisplayName("Supresion dirigida")
    class SupresionDirigida {

        @Test
        @DisplayName("borra el correo Y el motivo del titular, casando por hash")
        void borra_correo_y_motivo() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");
            Long turnoId = unicoTurno(propuesta.getId());
            Long lineaId = unicaLinea(turnoId);

            assertThat(retention.suppressLinesByEmail("laura@vetchapinero.co", AHORA)).isEqualTo(1);
            assertThat(retention.suppressTurnsByEmail("laura@vetchapinero.co")).isEqualTo(1);
            assertThat(retention.suppressProposalsByEmail("laura@vetchapinero.co", AHORA))
                    .isEqualTo(1);
            entityManager.clear();

            assertThat(columna("ai_proposals", "contact_email", propuesta.getId())).isNull();
            assertThat(columna("ai_proposal_turns", "input_text", turnoId)).isNull();
            assertThat(columna("ai_proposal_lines", "reason", lineaId))
                    .as("un borrado que deja la frase del titular en la tabla de al lado no es un"
                            + " borrado")
                    .isNull();
        }

        /**
         * La columna generada normaliza a minusculas, asi que cambiar una mayuscula no
         * esquiva la supresion. Sin esto, {@code Laura@...} se quedaria dentro.
         */
        @Test
        @DisplayName("casa sin distinguir mayusculas, porque la columna generada normaliza")
        void casa_sin_distinguir_mayusculas() {
            AiProposal propuesta = propuestaCompleta("Laura@VetChapinero.CO");

            assertThat(retention.suppressProposalsByEmail("laura@vetchapinero.co", AHORA))
                    .isEqualTo(1);
            entityManager.clear();

            assertThat(columna("ai_proposals", "contact_email", propuesta.getId())).isNull();
        }

        @Test
        @DisplayName("un correo que no esta no borra nada de nadie")
        void un_correo_que_no_esta_no_borra_nada() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.suppressProposalsByEmail("otra@vet.co", AHORA)).isZero();
            assertThat(retention.suppressLinesByEmail("otra@vet.co", AHORA)).isZero();
        }
    }

    @Nested
    @DisplayName("Purga")
    class Purga {

        /**
         * El orden lo imponen las FK {@code ON DELETE RESTRICT}: lineas, turnos,
         * cabecera. Cada paso deja al siguiente su condicion cumplida, y por eso el
         * {@code NOT EXISTS} de los dos ultimos no es defensivo sino estructural.
         */
        @Test
        @DisplayName("borra en el orden que exigen las FK y deja la tabla vacia")
        void borra_en_el_orden_de_las_fk() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.purgeLines(CORTE, 100)).isEqualTo(1);
            assertThat(retention.purgeTurns(CORTE, 100)).isEqualTo(1);
            assertThat(retention.purgeProposals(CORTE, 100)).isEqualTo(1);
            entityManager.clear();

            assertThat(entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM ai_proposals WHERE id = :id")
                    .setParameter("id", propuesta.getId()).getSingleResult())
                    .satisfies(cuantas -> assertThat(((Number) cuantas).intValue()).isZero());
        }

        /**
         * Sin haber vaciado las hijas, la cabecera no se va: el {@code NOT EXISTS} lo
         * impide antes de que la FK {@code RESTRICT} lo convierta en un error de
         * integridad a las cuatro de la mañana.
         */
        @Test
        @DisplayName("no borra la cabecera mientras le cuelguen turnos")
        void no_borra_la_cabecera_con_hijas() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.purgeProposals(CORTE, 100)).isZero();
        }

        @Test
        @DisplayName("no purga nada que siga dentro del plazo")
        void no_purga_dentro_del_plazo() {
            propuestaCompleta("laura@vetchapinero.co");

            assertThat(retention.purgeLines(CORTE_QUE_NO_ALCANZA, 100)).isZero();
            assertThat(retention.purgeTurns(CORTE_QUE_NO_ALCANZA, 100)).isZero();
        }

        /**
         * &#9940; La propuesta convertida no se purga nunca: su fila de conversion la
         * retiene con {@code ON DELETE RESTRICT}, y aqui se excluye en el {@code WHERE}
         * para que sea una decision escrita y no un error de integridad.
         */
        @Test
        @DisplayName("la propuesta convertida sobrevive a la purga, con lineas y turnos")
        void la_convertida_sobrevive() {
            AiProposal propuesta = propuestaCompleta("laura@vetchapinero.co");
            entityManager.createNativeQuery("""
                    INSERT INTO ai_proposal_conversions
                        (proposal_id, company_id, converted_at, created_date, enabled)
                    VALUES (:propuesta, :empresa, NOW(), NOW(), true)
                    """).setParameter("propuesta", propuesta.getId())
                    .setParameter("empresa", SchemaSeed.COMPANY_ID).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            assertThat(retention.purgeLines(CORTE, 100)).isZero();
            assertThat(retention.purgeTurns(CORTE, 100)).isZero();
            assertThat(retention.purgeProposals(CORTE, 100)).isZero();
        }
    }
}
