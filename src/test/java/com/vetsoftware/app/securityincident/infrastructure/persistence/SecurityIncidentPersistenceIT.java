package com.vetsoftware.app.securityincident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
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
 * Rodaja de {@code JpaSecurityIncidentRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es de que fecha cuelga el plazo.</b>
 * La Circular Unica de la SIC cuenta los quince dias habiles del reporte desde
 * el <em>escalamiento interno</em>, no desde la deteccion, y entre las dos
 * pueden pasar dias. Contarlo desde la deteccion da un vencimiento <em>mas
 * largo</em> que el real: el error cae siempre del lado de incumplir. Las dos
 * restricciones que lo sostienen —{@code chk_security_incidents_escalated} y
 * {@code chk_security_incidents_deadline}— solo se pueden comprobar contra el
 * motor, y el caso
 * {@link Plazo#un_vencimiento_posterior_a_la_deteccion_pero_anterior_al_escalamiento_no_entra()}
 * es el que de verdad distingue las dos lecturas: escribe una fila que seria
 * valida si el plazo colgara de {@code detected_at} y que el motor rechaza
 * porque cuelga de {@code escalated_at}.
 *
 * <p>
 * <b>Lo segundo que congela es que el expediente no se pueda vaciar.</b>
 * {@code chk_security_incidents_close} impide cerrar sin contencion ni causa
 * raiz, y {@code chk_security_incidents_report} impide un reporte sin radicado:
 * un incidente que no se documento en su momento es indistinguible de uno que
 * se oculto, y esa es la unica diferencia que un tercero puede comprobar
 * despues.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola.
 *
 * <p>
 * <b>Lo que esta clase NO cubre, y donde vive.</b> El calculo de los quince
 * dias habiles no pasa por el adaptador: {@code deadlineAt} le llega ya
 * resuelto. Lo hace {@code BusinessDayDeadlineAdapter} contra el calendario de
 * {@code publicholiday}, y la aritmetica —dia de partida excluido, festivos
 * observados— vive en {@code HolidayCalendar}. Aqui solo se comprueba la
 * invariante que el motor si puede imponer: <em>de que fecha cuelga</em> el
 * vencimiento.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSecurityIncidentRepository — el expediente de incidentes contra MySQL real")
class SecurityIncidentPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja, para las escrituras crudas. */
    private static final Long INCIDENTE_CRUDO = 8600L;

    private static final LocalDateTime OCURRIO = LocalDateTime.of(2026, 3, 2, 22, 15, 0);
    private static final LocalDateTime DETECTADO = LocalDateTime.of(2026, 3, 3, 8, 30, 0);
    private static final LocalDateTime ESCALADO = LocalDateTime.of(2026, 3, 5, 9, 0, 0);

    /**
     * Quince dias habiles desde el escalamiento, con el fin de dia que escribe el
     * caso de uso. Aqui entra como dato: quien lo calcula es otra rodaja.
     */
    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 3, 26, 23, 59, 59,
            999_999_000);

    private static final LocalDateTime REPORTADO = LocalDateTime.of(2026, 3, 20, 16, 40, 0);
    private static final LocalDateTime CERRADO = LocalDateTime.of(2026, 4, 10, 11, 0, 0);
    private static final LocalDateTime NOTIFICADO = LocalDateTime.of(2026, 4, 9, 10, 0, 0);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 9, 5, 0);

    @Autowired
    private SecurityIncidentJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaSecurityIncidentRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaSecurityIncidentRepository(springDataRepository,
                new SecurityIncidentJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el incidente y lo recupera con las cuatro fechas en su sitio")
        void guarda_el_incidente_y_lo_recupera_campo_a_campo() {
            SecurityIncident guardado = repository.save(registrado());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getDetectedAt()).isEqualTo(DETECTADO);
                assertThat(recuperado.getOccurredAt()).isEqualTo(OCURRIO);
                // Las cuatro fechas son distintas a proposito: si el mapper cruzara
                // deteccion con escalamiento, el plazo colgaria del sitio equivocado
                // y ninguna otra prueba de esta rodaja lo veria.
                assertThat(recuperado.getEscalatedAt()).isEqualTo(ESCALADO);
                assertThat(recuperado.getDeadlineAt()).isEqualTo(VENCE);
                assertThat(recuperado.getKind()).isEqualTo(SecurityIncidentKind.DATA_LEAK);
                assertThat(recuperado.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
                assertThat(recuperado.getAffectedSubjectCount()).isEqualTo(1200);
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.getVersion()).isZero();
                assertThat(recuperado.isReported()).isFalse();
                assertThat(recuperado.isClosed()).isFalse();
            });
        }

        @Test
        @DisplayName("el reporte deja fecha y radicado juntos, y mueve la version")
        void el_reporte_deja_fecha_y_radicado_y_mueve_la_version() {
            SecurityIncident guardado = repository.save(registrado());
            entityManager.flush();
            entityManager.clear();

            SecurityIncident cargado = repository.findById(guardado.getId()).orElseThrow();
            repository.save(cargado.report(REPORTADO, "SIC-2026-004512"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(reportado -> {
                assertThat(reportado.getReportedToAuthorityAt()).isEqualTo(REPORTADO);
                assertThat(reportado.getReportReference()).isEqualTo("SIC-2026-004512");
                assertThat(reportado.isReported()).isTrue();
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no
                // por una escritura masiva, que la dejaria intacta.
                assertThat(reportado.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el cierre escribe contencion y causa raiz sobre la fila que ya existia")
        void el_cierre_escribe_contencion_y_causa_raiz() {
            SecurityIncident guardado = repository.save(registrado());
            entityManager.flush();
            entityManager.clear();

            SecurityIncident cargado = repository.findById(guardado.getId()).orElseThrow();
            repository.save(cargado.close(CERRADO, "Se revocaron las credenciales expuestas",
                    "Una llave de API quedo en un repositorio publico", NOTIFICADO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(cerrado -> {
                assertThat(cerrado.getClosedAt()).isEqualTo(CERRADO);
                assertThat(cerrado.getContainment())
                        .isEqualTo("Se revocaron las credenciales expuestas");
                assertThat(cerrado.getRootCause())
                        .isEqualTo("Una llave de API quedo en un repositorio publico");
                assertThat(cerrado.getNotifiedSubjectsAt()).isEqualTo(NOTIFICADO);
                assertThat(cerrado.isClosed()).isTrue();
            });
        }

        @Test
        @DisplayName("el incidente sin reportar con el plazo pasado consta como vencido")
        void el_incidente_sin_reportar_con_el_plazo_pasado_consta_como_vencido() {
            SecurityIncident guardado = repository.save(registrado());
            entityManager.flush();
            entityManager.clear();

            SecurityIncident recuperado = repository.findById(guardado.getId()).orElseThrow();

            // Es la pregunta que sostiene ix_security_incidents_unreported. El instante
            // entra por parametro: el dominio no tiene reloj, asi que la respuesta no
            // depende de cuando se ejecute el test.
            assertThat(recuperado.isOverdue(VENCE.plusDays(1))).isTrue();
            assertThat(recuperado.isOverdue(VENCE.minusDays(1))).isFalse();
        }
    }

    @Nested
    @DisplayName("Plazo")
    class Plazo {

        @Test
        @DisplayName("un vencimiento posterior a la deteccion pero anterior al escalamiento no "
                + "entra")
        void un_vencimiento_posterior_a_la_deteccion_pero_anterior_al_escalamiento_no_entra() {
            // ESTE ES EL CASO QUE DISTINGUE LAS DOS LECTURAS DE LA NORMA, y el unico
            // que se pondria verde si alguien reescribiera la constraint contra
            // detected_at: la fila lleva deadline_at DESPUES de la deteccion —asi que
            // pasaria un CHECK escrito contra detected_at— y ANTES del escalamiento,
            // que es de donde la SIC hace colgar el plazo. El motor la rechaza.
            //
            // Se escribe por SQL nativo a proposito: el dominio ya replica la regla, y
            // saltarselo es la unica forma de comprobar que la base tambien la cuida.
            EngineConstraint.assertViolates("chk_security_incidents_deadline",
                    () -> insertarCruda(INCIDENTE_CRUDO, DETECTADO, ESCALADO,
                            DETECTADO.plusHours(1)));
        }

        @Test
        @DisplayName("un escalamiento anterior a la deteccion lo para chk_..._escalated")
        void un_escalamiento_anterior_a_la_deteccion_lo_para_el_motor() {
            // Escalar antes de detectar es imposible en el mundo, y si entrara daria un
            // vencimiento anterior al real sin que nada avisara.
            EngineConstraint.assertViolates("chk_security_incidents_escalated",
                    () -> insertarCruda(INCIDENTE_CRUDO + 1, DETECTADO, DETECTADO.minusHours(2),
                            VENCE));
        }

        @Test
        @DisplayName("un vencimiento igual al escalamiento lo para el motor: el plazo es estricto")
        void un_vencimiento_igual_al_escalamiento_lo_para_el_motor() {
            // El CHECK es > y no >=: un plazo de cero dias no es un plazo.
            EngineConstraint.assertViolates("chk_security_incidents_deadline",
                    () -> insertarCruda(INCIDENTE_CRUDO + 2, DETECTADO, ESCALADO, ESCALADO));
        }

        @Test
        @DisplayName("un hecho ocurrido despues de detectarse lo para chk_..._occurred")
        void un_hecho_ocurrido_despues_de_detectarse_lo_para_el_motor() {
            EngineConstraint.assertViolates("chk_security_incidents_occurred",
                    () -> insertarCrudaConOcurrencia(INCIDENTE_CRUDO + 3, DETECTADO.plusDays(1)));
        }
    }

    @Nested
    @DisplayName("El expediente no se puede vaciar")
    class ExpedienteCompleto {

        @Test
        @DisplayName("cerrar sin contencion ni causa raiz lo para chk_security_incidents_close")
        void cerrar_sin_contencion_ni_causa_raiz_lo_para_el_motor() {
            // Un incidente cerrado y sin documentar es indistinguible de uno ocultado.
            EngineConstraint.assertViolates("chk_security_incidents_close",
                    () -> insertarCrudaCerradaSinNarrativa(INCIDENTE_CRUDO + 4));
        }

        @Test
        @DisplayName("reportar sin radicado lo para chk_security_incidents_report")
        void reportar_sin_radicado_lo_para_el_motor() {
            // Un reporte que no se puede rastrear no consta: el radicado es la prueba.
            EngineConstraint.assertViolates("chk_security_incidents_report",
                    () -> insertarCrudaReportadaSinRadicado(INCIDENTE_CRUDO + 5));
        }

        @Test
        @DisplayName("un contador de afectados negativo lo para chk_security_incidents_count")
        void un_contador_negativo_lo_para_el_motor() {
            EngineConstraint.assertViolates("chk_security_incidents_count",
                    () -> insertarCrudaConContador(INCIDENTE_CRUDO + 6, -1));
        }
    }

    @Nested
    @DisplayName("El barrido de plataforma")
    class Barrido {

        @Test
        @DisplayName("el listado va del incidente mas reciente hacia atras")
        void el_listado_va_del_mas_reciente_hacia_atras() {
            SecurityIncident viejo = repository.save(registradoEl(DETECTADO.minusDays(10)));
            SecurityIncident nuevo = repository.save(registradoEl(DETECTADO.plusDays(4)));
            SecurityIncident medio = repository.save(registradoEl(DETECTADO));
            entityManager.flush();
            entityManager.clear();

            // containsSubsequence y no containsExactly: lo que se prueba es el ORDEN,
            // no cuantas filas hay. Exigir la lista exacta ataria la rodaja a que
            // ninguna otra prueba escriba jamas en esta tabla.
            assertThat(repository.findAll(0, 20).content()).extracting(SecurityIncident::getId)
                    .containsSubsequence(nuevo.getId(), medio.getId(), viejo.getId());
        }

        @Test
        @DisplayName("la pagina acotada respeta el tope del kernel de paginacion")
        void la_pagina_acotada_respeta_el_tope() {
            repository.save(registrado());
            entityManager.flush();
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAll(0, 100000).pageSize()).isEqualTo(200);
        }
    }

    private static SecurityIncident registrado() {
        return SecurityIncident.register(DETECTADO, OCURRIO, ESCALADO,
                SecurityIncidentKind.DATA_LEAK, IncidentSeverity.HIGH,
                "Llave de API expuesta en un repositorio publico", 1200, VENCE, CREADO_EL);
    }

    private static SecurityIncident registradoEl(LocalDateTime detectado) {
        return SecurityIncident.register(detectado, null, detectado.plusDays(2),
                SecurityIncidentKind.DATA_LEAK, IncidentSeverity.HIGH,
                "Llave de API expuesta en un repositorio publico", 1200, detectado.plusDays(23),
                CREADO_EL);
    }

    private void insertarCruda(Long id, LocalDateTime detectado, LocalDateTime escalado,
            LocalDateTime vence) {
        insertar(id, detectado, null, escalado, vence, 10, null, null, null, null, null);
    }

    private void insertarCrudaConOcurrencia(Long id, LocalDateTime ocurrio) {
        insertar(id, DETECTADO, ocurrio, ESCALADO, VENCE, 10, null, null, null, null, null);
    }

    private void insertarCrudaConContador(Long id, int contador) {
        insertar(id, DETECTADO, null, ESCALADO, VENCE, contador, null, null, null, null, null);
    }

    private void insertarCrudaCerradaSinNarrativa(Long id) {
        insertar(id, DETECTADO, null, ESCALADO, VENCE, 10, null, null, null, null, CERRADO);
    }

    private void insertarCrudaReportadaSinRadicado(Long id) {
        insertar(id, DETECTADO, null, ESCALADO, VENCE, 10, REPORTADO, null, null, null, null);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para las comprobaciones que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * las cuida.
     */
    private void insertar(Long id, LocalDateTime detectado, LocalDateTime ocurrio,
            LocalDateTime escalado, LocalDateTime vence, int contador, LocalDateTime reportado,
            String radicado, String contencion, String causaRaiz, LocalDateTime cerrado) {
        entityManager.createNativeQuery("""
                INSERT INTO security_incidents (id, detected_at, occurred_at, escalated_at, kind,
                                                severity, summary, affected_subject_count,
                                                deadline_at, reported_to_authority_at,
                                                report_reference, notified_subjects_at,
                                                containment, root_cause, closed_at, created_date,
                                                version)
                VALUES (:id, :detectado, :ocurrio, :escalado, 'DATA_LEAK', 'HIGH', :resumen,
                        :contador, :vence, :reportado, :radicado, NULL, :contencion, :causaRaiz,
                        :cerrado, :creado, 0)
                """).setParameter("id", id).setParameter("detectado", detectado)
                .setParameter("ocurrio", ocurrio).setParameter("escalado", escalado)
                .setParameter("resumen", "Fila cruda de la rodaja de persistencia")
                .setParameter("contador", contador).setParameter("vence", vence)
                .setParameter("reportado", reportado).setParameter("radicado", radicado)
                .setParameter("contencion", contencion).setParameter("causaRaiz", causaRaiz)
                .setParameter("cerrado", cerrado).setParameter("creado", CREADO_EL).executeUpdate();
    }
}
