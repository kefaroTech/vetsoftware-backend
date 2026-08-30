package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalStatus;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.TurnStatus;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
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
 * La rodaja de persistencia de {@code aiproposal} contra MySQL real.
 *
 * <p>
 * <b>Por que esta rodaja importa mas de lo normal.</b> La regla de ArchUnit que
 * exige rodaja a cada adaptador solo alcanza a las clases que se llaman
 * {@code *Repository}; un inventario de este mismo arbol encontro 20
 * adaptadores JPA con {@code createNativeQuery} y cinco <b>sin ninguna
 * cobertura de integracion</b>, porque lo que se llama {@code *Port} o
 * {@code *Adapter} se escapa de la regla en silencio. Aqui no se confia en la
 * regla: las tres tablas de la feature nacen con esta clase.
 *
 * <p>
 * <b>Lo que solo se puede comprobar aqui.</b> Tres entidades nuevas bajo
 * {@code ddl-auto: validate}, dos columnas {@code GENERATED ... STORED} que
 * Java no puede escribir, una {@code JSON}, dos {@code CHAR} de ancho fijo y
 * trece {@code CHECK} que el compilador no ve. Un desajuste de tipo en
 * cualquiera de ellas no rompe un test: <b>impide que arranque cualquier
 * contexto de Spring</b> del repositorio entero.
 *
 * <p>
 * <b>Fixture.</b> {@link SchemaSeed#seed} es obligatorio -sin el,
 * {@code fk_price_lists_published_by} tumba la insercion-, y la FK
 * {@code fk_ai_proposals_privacy_notice} obliga a sembrar ademas una version de
 * documento legal, que el seed compartido no trae. Se siembra con SQL nativo y
 * con un {@code code} propio: {@code legal_document_versions} tiene tres
 * indices unicos globales y el contenedor MySQL se comparte entre rodajas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAiProposalRepository — la propuesta anonima contra MySQL real")
class AiProposalPersistenceIT extends AbstractDataJpaTest {

    /** Id propio, fuera del rango 9xx del seed compartido. */
    private static final Long PRIVACY_NOTICE_ID = 9_501L;

    private static final String PRIVACY_NOTICE_CODE = "PRIVACY_NOTICE_AIPROP_IT";

    /** 64 caracteres hex: {@code chk_ai_proposals_snapshot_hash} lo exige asi. */
    private static final String SNAPSHOT = "0123456789abcdef".repeat(4);

    private static final String MOTIVO = "Porque vendes concentrado y accesorios.";

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"),
            ZoneOffset.UTC);

    /**
     * {@code uq_ai_proposals_token} es global. Cada test rueda atras su
     * transaccion, pero el contador mantiene los tokens distintos tambien dentro de
     * un mismo metodo, que es donde de verdad chocarian.
     */
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

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

    /**
     * {@code current_version_marker} es {@code GENERATED ALWAYS}: MySQL devuelve
     * {@code ERROR 3105} si se nombra en el {@code INSERT}, aunque el valor sea
     * {@code NULL}. Esta deliberadamente ausente de la lista de columnas.
     */
    private void sembrarAvisoDePrivacidad() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO legal_document_versions
                    (id, code, document_version, kind, title, content, content_hash,
                     published_at, published_by_system_user_id, effective_from, created_date,
                     version)
                VALUES (:id, :code, 1, 'PRIVACY_NOTICE', 'Aviso de privacidad',
                        'Texto del aviso de privacidad para la rodaja de propuestas.',
                        :hash, '2026-01-01 00:00:00', :autor, '2026-01-01', NOW(), 0)
                """).setParameter("id", PRIVACY_NOTICE_ID).setParameter("code", PRIVACY_NOTICE_CODE)
                .setParameter("hash", "fedcba9876543210".repeat(4))
                .setParameter("autor", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
    }

    private static String token() {
        String semilla = "AiPropIT" + SECUENCIA.incrementAndGet();
        return (semilla + "_".repeat(43)).substring(0, 43);
    }

    private AiProposal nuevaPropuesta(String correo) {
        return AiProposal.create(token(), SchemaSeed.PRICE_LIST_ID, ProposalBillingCycle.MONTHLY,
                SNAPSHOT, PRIVACY_NOTICE_ID, UUID.randomUUID().toString(), correo, "es-CO", 14,
                RELOJ);
    }

    private AiProposal guardar(AiProposal proposal) {
        AiProposal guardada = repository.save(proposal);
        entityManager.flush();
        entityManager.clear();
        return guardada;
    }

    private ProposalTurn turnoInicialCerrado(Long proposalId) {
        ProposalTurn turno = ProposalTurn.pendienteDeModelo(proposalId, 1, TurnType.MODEL_INITIAL,
                "Tengo una veterinaria en Chapinero y vendo concentrado.", "claude-sonnet-5",
                "p-2026-08", UUID.randomUUID().toString(), RELOJ);
        ProposalTurn persistido = repository.saveTurn(turno);
        persistido.cerrarConExito(820, 240, 3_400, "end_turn", "{\"necesarios\":[\"CORE\"]}",
                RELOJ);
        ProposalTurn cerrado = repository.saveTurn(persistido);
        entityManager.flush();
        entityManager.clear();
        return cerrado;
    }

    @Nested
    @DisplayName("Cabecera")
    class Cabecera {

        @Test
        @DisplayName("guarda la propuesta y la devuelve por su token, que es como se lee")
        void guarda_y_lee_por_token() {
            AiProposal guardada = guardar(nuevaPropuesta("laura@vetchapinero.co"));

            assertThat(repository.findByPublicToken(guardada.getPublicToken())).get()
                    .satisfies(leida -> {
                        assertThat(leida.getId()).isEqualTo(guardada.getId());
                        assertThat(leida.getStatus()).isEqualTo(ProposalStatus.DRAFT);
                        assertThat(leida.getBillingCycle()).isEqualTo(ProposalBillingCycle.MONTHLY);
                        assertThat(leida.getCatalogSnapshotHash()).isEqualTo(SNAPSHOT);
                        assertThat(leida.getPrivacyNoticeVersionId()).isEqualTo(PRIVACY_NOTICE_ID);
                        assertThat(leida.getContactEmail()).isEqualTo("laura@vetchapinero.co");
                        assertThat(leida.getVersion()).isZero();
                    });
        }

        /**
         * <b>Un token que no existe devuelve vacio, no la primera fila.</b> Es la unica
         * frontera de autorizacion de la feature: si esta consulta fallara abierta,
         * cualquier anonimo leeria la propuesta de otro prospecto -su correo, su texto
         * libre y sus lineas-.
         */
        @Test
        @DisplayName("un token que no existe no devuelve ninguna propuesta")
        void token_desconocido_no_devuelve_nada() {
            guardar(nuevaPropuesta("laura@vetchapinero.co"));

            assertThat(repository.findByPublicToken("N".repeat(43))).isEmpty();
            assertThat(repository.findByPublicToken(null)).isEmpty();
            assertThat(repository.findByPublicToken("  ")).isEmpty();
        }

        /**
         * <b>La columna generada, que es la mitad del arreglo de idempotencia de
         * S4.2.2.</b> {@code contact_email_hash} no se mapea en Java -MySQL rechaza el
         * {@code INSERT} que la nombre- y aun asi tiene que existir y valer
         * {@code UNHEX(SHA2(LOWER(correo),256))}, porque el unico
         * {@code (contact_email_hash, idempotency_key)} cuelga de ella. Si un dia
         * alguien la mapeara desde Java, este caso se pone rojo antes de que la feature
         * llegue a produccion con la clave de idempotencia sin acotar.
         */
        @Test
        @DisplayName("contact_email_hash lo calcula la base y sigue el correo en minusculas")
        void el_hash_del_correo_lo_calcula_la_base() {
            AiProposal guardada = guardar(nuevaPropuesta("Laura@VetChapinero.CO"));

            Object hash = entityManager.createNativeQuery("""
                    SELECT HEX(contact_email_hash) FROM ai_proposals WHERE id = :id
                    """).setParameter("id", guardada.getId()).getSingleResult();
            Object esperado = entityManager
                    .createNativeQuery("SELECT UPPER(SHA2('laura@vetchapinero.co', 256))")
                    .getSingleResult();

            assertThat(hash).isEqualTo(esperado);
        }

        /**
         * El {@code @SQLDelete} de {@code ai_proposals} lleva {@code AND version = ?}
         * porque la entidad esta versionada -la trampa de BE-26: Hibernate liga
         * <em>dos</em> parametros y un {@code WHERE id = ?} suelto actualizaria cero
         * filas sin decir nada-. Aqui se comprueba que el borrado logico de verdad
         * esconde la fila, por las dos vias de lectura.
         */
        @Test
        @DisplayName("el borrado logico esconde la propuesta por id y por token")
        void el_borrado_logico_esconde_la_propuesta() {
            AiProposal guardada = guardar(nuevaPropuesta("laura@vetchapinero.co"));

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();
            assertThat(repository.findByPublicToken(guardada.getPublicToken())).isEmpty();
        }

        @Test
        @DisplayName("dos propuestas no pueden compartir el token")
        void el_token_es_unico() {
            AiProposal primera = guardar(nuevaPropuesta("laura@vetchapinero.co"));

            EngineConstraint.assertViolates("uq_ai_proposals_token", () -> {
                repository
                        .save(AiProposal.create(primera.getPublicToken(), SchemaSeed.PRICE_LIST_ID,
                                ProposalBillingCycle.MONTHLY, SNAPSHOT, PRIVACY_NOTICE_ID,
                                UUID.randomUUID().toString(), "otro@vet.co", "es-CO", 14, RELOJ));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Turnos")
    class Turnos {

        /**
         * <b>La secuencia obligada de la feature, escrita como test.</b>
         * {@code SIN_IO_EXTERNO_EN_TRANSACCION} prohibe llamar al modelo dentro de una
         * transaccion, asi que el turno se escribe {@code PENDING} y se cierra despues.
         * Con {@code NOT NULL} en las columnas de resultado esto no seria posible, y la
         * feature no arrancaria.
         */
        @Test
        @DisplayName("el turno se escribe PENDING sin resultado y se cierra despues")
        void el_turno_nace_pendiente_y_se_cierra() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));

            ProposalTurn pendiente = repository
                    .saveTurn(ProposalTurn.pendienteDeModelo(propuesta.getId(), 1,
                            TurnType.MODEL_INITIAL, "Somos dos veterinarios y una auxiliar.",
                            "claude-sonnet-5", "p-2026-08", UUID.randomUUID().toString(), RELOJ));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findTurnById(pendiente.getId())).get().satisfies(leido -> {
                assertThat(leido.getStatus()).isEqualTo(TurnStatus.PENDING);
                assertThat(leido.getCompletedAt()).isNull();
                assertThat(leido.getOutputTokens()).isNull();
                assertThat(leido.getInputTextChars()).isEqualTo(38);
            });

            ProposalTurn recuperado = repository.findTurnById(pendiente.getId()).orElseThrow();
            recuperado.cerrarConExito(820, 240, 3_400, "end_turn", "{\"necesarios\":[\"CORE\"]}",
                    RELOJ);
            repository.saveTurn(recuperado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findTurnById(pendiente.getId())).get().satisfies(cerrado -> {
                assertThat(cerrado.getStatus()).isEqualTo(TurnStatus.SUCCEEDED);
                assertThat(cerrado.getCompletedAt()).isNotNull();
                assertThat(cerrado.getInputTokens()).isEqualTo(820);
                assertThat(cerrado.getRawResponse()).contains("CORE");
                assertThat(cerrado.getVersion()).isEqualTo(1L);
            });
        }

        /**
         * <b>{@code FAILED} es un estado normal, no una anomalia.</b> Un turno que
         * nunca recibio respuesta -Bedrock caido, tope de gasto, tiempo agotado- se
         * cierra igual, y el endpoint responde 200 con el modo degradado.
         */
        @Test
        @DisplayName("un turno que no recibio respuesta se cierra FAILED con su codigo")
        void el_turno_fallido_guarda_su_codigo() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            ProposalTurn pendiente = repository.saveTurn(ProposalTurn.pendienteDeModelo(
                    propuesta.getId(), 1, TurnType.MODEL_INITIAL, "Texto del prospecto.",
                    "claude-sonnet-5", "p-2026-08", UUID.randomUUID().toString(), RELOJ));
            pendiente.cerrarConFallo("MODEL_TIMEOUT", 25_000, RELOJ);
            repository.saveTurn(pendiente);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findTurnById(pendiente.getId())).get().satisfies(leido -> {
                assertThat(leido.getStatus()).isEqualTo(TurnStatus.FAILED);
                assertThat(leido.getFailureCode()).isEqualTo("MODEL_TIMEOUT");
                assertThat(leido.getCompletedAt()).isNotNull();
                assertThat(leido.getOutputTokens()).isNull();
            });
        }

        /**
         * Los refinamientos son acumulativos y el orden es contenido, no presentacion:
         * leerlos desordenados cambia lo que se reconstruye del prospecto.
         */
        @Test
        @DisplayName("los turnos de una propuesta salen en el orden en que ocurrieron")
        void los_turnos_salen_ordenados() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            turnoInicialCerrado(propuesta.getId());
            repository.saveTurn(ProposalTurn.pendienteDeModelo(propuesta.getId(), 3,
                    TurnType.MODEL_REFINEMENT, "Tenemos dos sedes.", "claude-sonnet-5", "p-2026-08",
                    UUID.randomUUID().toString(), RELOJ));
            repository.saveTurn(ProposalTurn.edicionDelCliente(propuesta.getId(), 2,
                    UUID.randomUUID().toString(), RELOJ));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findTurnsByProposalId(propuesta.getId()))
                    .extracting(ProposalTurn::getTurnNumber).containsExactly(1, 2, 3);
        }

        /**
         * {@code uq_ai_proposal_turns_seq} + {@code chk_..._initial_is_first}
         * garantizan juntas "como mucho un {@code MODEL_INITIAL} por propuesta": el
         * {@code CHECK} ancla el tipo al numero 1 y el {@code UNIQUE} impide que haya
         * dos numeros 1. Es una invariante entre filas, sin trigger, y la mitad que se
         * prueba aqui es la unica que Java no puede comprobar solo.
         */
        @Test
        @DisplayName("dos turnos no pueden compartir numero dentro de la misma propuesta")
        void el_numero_de_turno_es_unico_por_propuesta() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            turnoInicialCerrado(propuesta.getId());

            EngineConstraint.assertViolates("uq_ai_proposal_turns_seq", () -> {
                repository.saveTurn(ProposalTurn.pendienteDeModelo(propuesta.getId(), 1,
                        TurnType.MODEL_REFINEMENT, "Otro texto.", "claude-sonnet-5", "p-2026-08",
                        UUID.randomUUID().toString(), RELOJ));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Lineas")
    class Lineas {

        private ProposalLine aceptada(Long turnId, String code, int orden) {
            return new ProposalLine(null, turnId, code, catalogItemCoreId, LineAction.ADDED,
                    LineSource.MODEL, LineVerdict.ACCEPTED, 1, new BigDecimal("49000.00"), MOTIVO,
                    null, orden, LocalDateTime.now(RELOJ), null);
        }

        /**
         * <b>Las rechazadas se persisten, y por eso {@code catalog_item_id} es
         * nulable.</b> No se puede poner una FK a una fila que no existe, y la
         * alucinacion del modelo es precisamente el dato que mide su calidad. Lo que
         * este test fija es que la tabla las <em>admite</em>: el que no salgan por HTTP
         * lo fija el contrato de {@code CartResult}, no el esquema.
         */
        @Test
        @DisplayName("guarda la aceptada con su FK y la alucinada sin ella, verbatim")
        void guarda_aceptadas_y_rechazadas() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            ProposalTurn turno = turnoInicialCerrado(propuesta.getId());

            repository.saveLines(List.of(aceptada(turno.getId(), "CORE", 0),
                    new ProposalLine(null, turno.getId(), "PACK_ENTERPRISE_2027", null,
                            LineAction.ADDED, LineSource.MODEL, LineVerdict.UNKNOWN_CODE, 1, null,
                            MOTIVO, null, 1, LocalDateTime.now(RELOJ), null)));
            entityManager.flush();
            entityManager.clear();

            List<ProposalLine> leidas = repository.findLinesByTurnId(turno.getId());
            assertThat(leidas).extracting(ProposalLine::getItemCode).containsExactly("CORE",
                    "PACK_ENTERPRISE_2027");
            assertThat(leidas.get(0).getCatalogItemId()).isEqualTo(catalogItemCoreId);
            assertThat(leidas.get(0).getUnitAmount()).isEqualByComparingTo("49000.00");
            assertThat(leidas.get(1).getCatalogItemId()).isNull();
            assertThat(leidas.get(1).getVerdict()).isEqualTo(LineVerdict.UNKNOWN_CODE);
        }

        /**
         * {@code uq_ai_proposal_lines_code} va sobre {@code (turn_id, item_code)} y no
         * sobre {@code (turn_id, catalog_item_id)}: con el id la restriccion no valdria
         * nada para las alucinaciones, porque MySQL admite multiples {@code NULL} en un
         * indice unico y el mismo codigo inventado podria repetirse veinte veces en el
         * mismo turno.
         */
        @Test
        @DisplayName("un codigo no puede aparecer dos veces en el mismo turno")
        void el_codigo_es_unico_dentro_del_turno() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            ProposalTurn turno = turnoInicialCerrado(propuesta.getId());
            repository.saveLines(List.of(aceptada(turno.getId(), "CORE", 0)));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_ai_proposal_lines_code", () -> {
                repository.saveLines(List.of(aceptada(turno.getId(), "CORE", 1)));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("guardar una lista vacia no toca la base ni falla")
        void guardar_lista_vacia_no_hace_nada() {
            assertThat(repository.saveLines(List.of())).isEmpty();
            assertThat(repository.saveLines(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retencion")
    class Retencion {

        /**
         * <b>El defecto que esta feature estuvo a punto de llevarse a produccion.</b>
         * El prompt obliga al modelo a <em>citar</em> al cliente, asi que
         * {@code reason} guarda las palabras del prospecto -su modelo de negocio, su
         * convenio con la fundacion, a veces la ciudad-. La politica de retencion
         * borraba el correo y el texto libre y conservaba "todas las lineas": una fila
         * marcada {@code anonymized_at} seguia llevando la frase del titular dentro, y
         * el informe de cumplimiento la daba por limpia.
         *
         * <p>
         * Aqui se comprueba que las dos escrituras -cabecera y linea- caben en el
         * esquema a la vez: {@code chk_ai_proposals_anonimizado} sobre la cabecera, y
         * sobre la linea las dos ramas nuevas -{@code ..._model_reason} acepta una
         * linea de modelo sin motivo <em>si consta que se borro</em>, y
         * {@code ..._redaccion} impide que la marca conviva con el motivo-.
         */
        @Test
        @DisplayName("anonimizar borra correo, clave y motivo, y conserva el aviso mostrado")
        void la_anonimizacion_borra_tambien_el_motivo() {
            AiProposal propuesta = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            ProposalTurn turno = turnoInicialCerrado(propuesta.getId());
            ProposalLine linea = repository.saveLines(List.of(new ProposalLine(null, turno.getId(),
                    "CORE", catalogItemCoreId, LineAction.ADDED, LineSource.MODEL,
                    LineVerdict.ACCEPTED, 1, new BigDecimal("69000.00"), MOTIVO, null, 0,
                    LocalDateTime.now(RELOJ), null))).get(0);
            entityManager.flush();
            entityManager.clear();

            AiProposal recuperada = repository.findById(propuesta.getId()).orElseThrow();
            recuperada.anonimizar(RELOJ);
            repository.save(recuperada);

            ProposalLine leida = repository.findLinesByTurnId(turno.getId()).get(0);
            leida.redactarMotivo(RELOJ);
            repository.saveLines(List.of(leida));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(propuesta.getId())).get().satisfies(anonima -> {
                assertThat(anonima.getContactEmail()).isNull();
                assertThat(anonima.getIdempotencyKey()).isNull();
                assertThat(anonima.getAnonymizedAt()).isNotNull();
                assertThat(anonima.estaAnonimizada()).isTrue();
                assertThat(anonima.getPrivacyNoticeVersionId()).isEqualTo(PRIVACY_NOTICE_ID);
                assertThat(anonima.getVersion()).isEqualTo(1L);
            });
            assertThat(repository.findLinesByTurnId(turno.getId())).singleElement()
                    .satisfies(sinMotivo -> {
                        assertThat(sinMotivo.getId()).isEqualTo(linea.getId());
                        assertThat(sinMotivo.getReason()).isNull();
                        assertThat(sinMotivo.getReasonRedactedAt()).isNotNull();
                        assertThat(sinMotivo.tieneMotivoBorrado()).isTrue();
                        assertThat(sinMotivo.getSource()).isEqualTo(LineSource.MODEL);
                        assertThat(sinMotivo.getVerdict()).isEqualTo(LineVerdict.ACCEPTED);
                        assertThat(sinMotivo.getVersion()).isEqualTo(1L);
                    });
        }

        /**
         * <b>Y la clave de idempotencia anonimizada no rompe su unico.</b>
         * {@code uq_ai_proposals_idempotency} va sobre
         * {@code (contact_email_hash, idempotency_key)} y las dos columnas quedan a
         * {@code NULL} tras la anonimizacion: MySQL admite multiples {@code NULL} en un
         * indice unico, asi que dos propuestas anonimizadas conviven. Si el unico
         * hubiera quedado sobre la columna sola -como en la v2 del diseno- el segundo
         * barrido nocturno moriria contra su propio indice.
         */
        @Test
        @DisplayName("dos propuestas anonimizadas conviven bajo el unico de idempotencia")
        void dos_anonimizadas_conviven() {
            AiProposal primera = guardar(nuevaPropuesta("laura@vetchapinero.co"));
            AiProposal segunda = guardar(nuevaPropuesta("carlos@vetnorte.co"));

            AiProposal a = repository.findById(primera.getId()).orElseThrow();
            AiProposal b = repository.findById(segunda.getId()).orElseThrow();
            a.anonimizar(RELOJ);
            b.anonimizar(RELOJ);
            repository.save(a);
            repository.save(b);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(primera.getId())).get()
                    .extracting(AiProposal::getIdempotencyKey).isNull();
            assertThat(repository.findById(segunda.getId())).get()
                    .extracting(AiProposal::getIdempotencyKey).isNull();
        }
    }
}
