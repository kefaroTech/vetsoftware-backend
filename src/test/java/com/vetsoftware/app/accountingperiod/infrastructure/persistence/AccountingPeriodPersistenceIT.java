package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
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
 * Rodaja de {@code JpaAccountingPeriodRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es la resolucion del periodo de
 * imputacion</b>, que es lo unico de la feature que no se puede comprobar sin
 * el motor: {@code findFirstOpenFrom} depende de que el {@code >=} sobre un
 * {@code CHAR(7) ascii_bin} ordene como el calendario, y de que el {@code Sort}
 * ascendente del adaptador llegue intacto a la consulta. Con el orden invertido
 * —o con un formato de clave sin cero a la izquierda— un hecho de marzo acaba
 * imputado al mes equivocado <b>sin error y sin log</b>.
 *
 * <p>
 * <b>Y lo segundo que congela es una contradiccion del esquema.</b> Las dos
 * {@code CHECK} de la migracion 331 hicieron imposible persistir un periodo
 * reabierto hasta que el changeset <b>365</b> lo corrigio: hoy
 * {@code chk_accounting_periods_closure} admite la rama
 * {@code status = 'OPEN' AND reopened_at IS NOT NULL AND closed_at IS NOT NULL},
 * asi que la reapertura de un {@code SOFT_CLOSED} se guarda. Lo prueba
 * {@link Reapertura#el_motor_admite_un_periodo_reabierto()}.
 *
 * <p>
 * <b>Reabrir un {@code LOCKED} sigue prohibido</b>, y eso NO lo sostiene
 * ninguna constraint —el 365 lo dejo escrito— sino el disparador
 * {@code trg_accounting_periods_bu_guard} del changeset 346. Su prueba vive en
 * {@code AccountingPeriodTriggerIT}, no aqui.
 *
 * <p>
 * <b>Se siembra la cadena completa por las dos claves foraneas a
 * {@code system_users}</b> —{@code closed_by} y {@code reopened_by}—, que son
 * {@code RESTRICT}: sin un usuario de sistema real no se puede cerrar ni un
 * mes. Las claves de periodo van todas en el ano <b>2027</b> a proposito:
 * 2026-03 es la que usa la rodaja de conciliacion externa y la que tendra que
 * sembrar {@code SchemaSeed} en cuanto la clave foranea
 * {@code fk_eir_posting_period} este viva, y una colision con
 * {@code uq_accounting_periods_period} aqui seria un fallo que no habla de esta
 * feature.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAccountingPeriodRepository — el calendario contable contra MySQL real")
class AccountingPeriodPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja. */
    private static final Long PERIODO_CRUDO = 8900L;

    private static final AccountingPeriodKey ENERO = AccountingPeriodKey.of("2027-01");
    private static final AccountingPeriodKey FEBRERO = AccountingPeriodKey.of("2027-02");
    private static final AccountingPeriodKey MARZO = AccountingPeriodKey.of("2027-03");
    private static final AccountingPeriodKey SEPTIEMBRE = AccountingPeriodKey.of("2027-09");
    private static final AccountingPeriodKey OCTUBRE = AccountingPeriodKey.of("2027-10");

    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2027, 1, 1, 0, 5, 0);
    private static final LocalDateTime CERRADO_EL = LocalDateTime.of(2027, 2, 5, 17, 30, 15);
    private static final LocalDateTime REABIERTO_EL = LocalDateTime.of(2027, 2, 9, 9, 12, 45);

    @Autowired
    private AccountingPeriodJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaAccountingPeriodRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaAccountingPeriodRepository(springDataRepository,
                new AccountingPeriodJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el mes y lo recupera con la clave, el estado y la fecha en su sitio")
        void guarda_el_mes_y_lo_recupera_campo_a_campo() {
            AccountingPeriod guardado = repository.save(abierto(ENERO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getPeriodKey()).isEqualTo(ENERO);
                assertThat(recuperado.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
                assertThat(recuperado.getClosedAt()).isNull();
                assertThat(recuperado.getClosedBySystemUserId()).isNull();
                assertThat(recuperado.getReopenedAt()).isNull();
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.getVersion()).isZero();
                assertThat(recuperado.acceptsPostings()).isTrue();
            });
        }

        @Test
        @DisplayName("el ciclo del cierre: el mes queda sellado y la version se mueve")
        void el_ciclo_del_cierre_deja_el_mes_sellado() {
            AccountingPeriod guardado = repository.save(abierto(ENERO));
            // El changeset 346 pone un disparador que prohibe cerrar el ULTIMO mes
            // abierto —la misma regla que el dominio—, asi que hace falta dejar otro
            // abierto detras o el UPDATE no llega a ejecutarse.
            repository.save(abierto(FEBRERO));
            entityManager.flush();
            entityManager.clear();

            AccountingPeriod cargado = repository.findById(guardado.getId()).orElseThrow();
            cargado.softClose(SchemaSeed.SYSTEM_USER_ID, CERRADO_EL);
            repository.save(cargado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(cerrado -> {
                assertThat(cerrado.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
                assertThat(cerrado.getClosedAt()).isEqualTo(CERRADO_EL);
                assertThat(cerrado.getClosedBySystemUserId()).isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no por
                // una escritura masiva que la dejaria intacta.
                assertThat(cerrado.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("declarar el mes lo deja LOCKED conservando la firma del cierre")
        void declarar_el_mes_lo_deja_bloqueado() {
            AccountingPeriod guardado = repository.save(abierto(ENERO));
            // El changeset 346 pone un disparador que prohibe cerrar el ULTIMO mes
            // abierto —la misma regla que el dominio—, asi que hace falta dejar otro
            // abierto detras o el UPDATE no llega a ejecutarse.
            repository.save(abierto(FEBRERO));
            entityManager.flush();

            AccountingPeriod cargado = repository.findById(guardado.getId()).orElseThrow();
            cargado.lock(SchemaSeed.SYSTEM_USER_ID, CERRADO_EL);
            repository.save(cargado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(declarado -> {
                assertThat(declarado.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
                assertThat(declarado.getClosedBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                assertThat(declarado.acceptsPostings()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("Unicidad de la clave del mes")
    class UnicidadDeLaClave {

        @Test
        @DisplayName("el mismo mes dos veces lo para uq_accounting_periods_period")
        void el_mismo_mes_dos_veces_lo_para_la_unicidad() {
            repository.save(abierto(ENERO));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_accounting_periods_period", () -> {
                repository.save(abierto(ENERO));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la comprobacion previa distingue el mes que existe del que no")
        void la_comprobacion_previa_distingue_el_mes_que_existe() {
            // Es el metodo que el service consulta ANTES de insertar, para convertir el
            // duplicado en un 409 legible en vez de un Duplicate entry del driver.
            repository.save(abierto(ENERO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByPeriodKey(ENERO)).isTrue();
            assertThat(repository.existsByPeriodKey(FEBRERO)).isFalse();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un mes 13 lo para chk_accounting_periods_key")
        void un_mes_13_lo_para_el_check_de_la_clave() {
            // El dominio ya rechaza esta clave, asi que la unica forma de comprobar que
            // la base tambien la rechaza —el cinturon bajo el tirante— es escribir la
            // fila por SQL nativo, saltandose el value object.
            EngineConstraint.assertViolates("chk_accounting_periods_key",
                    () -> insertarCrudaSinCierre(PERIODO_CRUDO, "2027-13", "OPEN"));
        }

        @Test
        @DisplayName("un estado fuera de los tres lo para el motor, y lo para chk_..._closure")
        void un_estado_desconocido_lo_para_el_motor() {
            // La red del enum sigue existiendo: si alguien anade un valor a
            // AccountingPeriodStatus sin el changeset que lo admita, el INSERT muere.
            //
            // PERO NO LO MATA LA CONSTRAINT QUE PARECE. chk_accounting_periods_status
            // nunca llega a evaluarse para un valor desconocido, porque
            // chk_accounting_periods_closure ya lo excluye: sus dos ramas son
            // "OPEN con cierre vacio" o "SOFT_CLOSED/LOCKED con cierre relleno", y un
            // 'PENDING' no cae en ninguna, tenga el cierre relleno o vacio. Es decir,
            // el vocabulario esta vigilado DOS veces y la que dispara es la del cierre.
            //
            // Nombrarla mal era el defecto que EngineConstraint existe para cazar:
            // afirmar "lanzo alguna excepcion" habria dado verde por el motivo
            // equivocado, y el caso habria seguido pasando el dia que alguien borrara
            // chk_accounting_periods_status del esquema.
            EngineConstraint.assertViolates("chk_accounting_periods_closure",
                    () -> insertarCrudaSinCierre(PERIODO_CRUDO + 1, "2027-04", "PENDING"));
        }

        @Test
        @DisplayName("un mes cerrado sin fecha de cierre lo para chk_accounting_periods_closure")
        void un_mes_cerrado_sin_fecha_lo_para_el_check_de_cierre() {
            // Sin este caso, un mes cerrado podria quedarse sin la hora y sin la firma
            // del cierre, que es exactamente el dato por el que se pregunta despues.
            EngineConstraint.assertViolates("chk_accounting_periods_closure",
                    () -> insertarCrudaSinCierre(PERIODO_CRUDO + 2, "2027-05", "SOFT_CLOSED"));
        }

        @Test
        @DisplayName("una firma que no existe la para fk_accounting_periods_closed_by")
        void una_firma_inexistente_la_para_la_clave_foranea() {
            // La firma del cierre apunta a system_users con RESTRICT. Sin la clave, un
            // mes podria quedar cerrado por un usuario borrado y la auditoria no tendria
            // a quien preguntar.
            EngineConstraint.assertViolates("fk_accounting_periods_closed_by",
                    () -> insertarCrudaConCierre(PERIODO_CRUDO + 3, "2027-06", "SOFT_CLOSED",
                            999999L));
        }
    }

    @Nested
    @DisplayName("Reapertura")
    class Reapertura {

        @Test
        @DisplayName("el motor YA admite un periodo reabierto: OPEN conservando su cierre")
        void el_motor_admite_un_periodo_reabierto() {
            // ESTE CASO ESTUVO INVERTIDO Y AFIRMABA UN DEFECTO COMO CONTRATO.
            //
            // Cuando se escribio, chk_accounting_periods_closure exigia que un OPEN
            // tuviera closed_at NULL mientras chk_accounting_periods_reopening exigia
            // closed_at NOT NULL para admitir la reapertura: no habia fila que
            // satisfaciera las dos, y el caso documentaba esa contradiccion esperando la
            // violacion. Su propio javadoc dejaba escrito «el dia que el changeset se
            // corrija, este caso se invierte».
            //
            // El changeset 365 lo corrigio -anadio la rama (status = 'OPEN' AND
            // reopened_at IS NOT NULL AND closed_at IS NOT NULL)- y el caso se quedo sin
            // invertir, asi que llevaba rojo desde entonces. Aqui esta invertido: la fila
            // de la reapertura SE GUARDA, que es la regla de negocio de verdad.
            //
            // Reabrir un SOFT_CLOSED es legitimo. Reabrir un LOCKED no, y eso NO lo
            // sujeta ninguna constraint sino el disparador del changeset 346; su prueba
            // vive en AccountingPeriodTriggerIT.
            AccountingPeriod guardado = repository.save(abierto(ENERO));
            // El disparador del changeset 346 prohibe cerrar el ULTIMO mes abierto.
            repository.save(abierto(FEBRERO));
            entityManager.flush();
            entityManager.clear();

            AccountingPeriod cerrado = repository.findById(guardado.getId()).orElseThrow();
            cerrado.softClose(SchemaSeed.SYSTEM_USER_ID, CERRADO_EL);
            repository.save(cerrado);
            entityManager.flush();
            entityManager.clear();

            AccountingPeriod paraReabrir = repository.findById(guardado.getId()).orElseThrow();
            paraReabrir.reopen(SchemaSeed.SYSTEM_USER_ID, REABIERTO_EL,
                    "Ajuste recibido fuera de plazo");
            repository.save(paraReabrir);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(reabierto -> {
                assertThat(reabierto.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
                // El cierre previo se conserva: es el registro de que ocurrio.
                assertThat(reabierto.getClosedAt()).isEqualTo(CERRADO_EL);
                assertThat(reabierto.getClosedBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                assertThat(reabierto.getReopenedAt()).isEqualTo(REABIERTO_EL);
                assertThat(reabierto.getReopenedBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                assertThat(reabierto.getReopenedReason())
                        .isEqualTo("Ajuste recibido fuera de plazo");
                assertThat(reabierto.isReopened()).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("El periodo de imputacion")
    class PeriodoDeImputacion {

        @Test
        @DisplayName("si el mes del hecho esta abierto, devuelve ese mismo")
        void si_el_mes_del_hecho_esta_abierto_devuelve_ese() {
            repository.save(abierto(ENERO));
            repository.save(abierto(FEBRERO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findFirstOpenFrom(ENERO)).get()
                    .satisfies(periodo -> assertThat(periodo.getPeriodKey()).isEqualTo(ENERO));
        }

        @Test
        @DisplayName("si el mes del hecho esta cerrado, devuelve el primer abierto POSTERIOR")
        void si_el_mes_del_hecho_esta_cerrado_devuelve_el_siguiente_abierto() {
            // La regla entera de la ficha, ejecutada contra el motor: enero cerrado,
            // febrero cerrado, marzo abierto. Un hecho de enero se reconoce en marzo, no
            // en enero y no en febrero.
            cerrar(repository.save(abierto(ENERO)));
            cerrar(repository.save(abierto(FEBRERO)));
            repository.save(abierto(MARZO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findFirstOpenFrom(ENERO)).get()
                    .satisfies(periodo -> assertThat(periodo.getPeriodKey()).isEqualTo(MARZO));
        }

        @Test
        @DisplayName("NUNCA hacia atras: con el mes del hecho cerrado y solo meses abiertos antes")
        void nunca_hacia_atras() {
            // Enero abierto, febrero cerrado. Un hecho de febrero NO puede caer en
            // enero: eso reescribiria un informe ya declarado. Sin ningun mes abierto
            // posterior, la consulta no devuelve nada y el caso de uso rechaza.
            repository.save(abierto(ENERO));
            cerrar(repository.save(abierto(FEBRERO)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findFirstOpenFrom(FEBRERO)).isEmpty();
        }

        @Test
        @DisplayName("el orden es cronologico en el salto de septiembre a octubre")
        void el_orden_es_cronologico_en_el_salto_de_decena() {
            // El unico salto donde un formato de clave descuidado se rompe. Con
            // "2027-9" en vez de "2027-09", octubre ordenaria ANTES que septiembre y un
            // hecho de septiembre acabaria imputado a octubre.
            repository.save(abierto(SEPTIEMBRE));
            repository.save(abierto(OCTUBRE));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findFirstOpenFrom(SEPTIEMBRE)).get()
                    .satisfies(periodo -> assertThat(periodo.getPeriodKey()).isEqualTo(SEPTIEMBRE));
        }

        @Test
        @DisplayName("sin ningun mes abierto, no devuelve nada en vez de devolver uno cerrado")
        void sin_ningun_mes_abierto_no_devuelve_nada() {
            // El calendario se siembra CERRADO por SQL nativo en vez de abrirlo y
            // cerrarlo despues, y no es un atajo: el disparador del changeset 346
            // prohibe cerrar el ultimo mes abierto, asi que "no queda ningun mes
            // abierto" ya NO es un estado alcanzable por UPDATE. Sigue siendo
            // alcanzable por INSERT —el disparador es BEFORE UPDATE— y sigue siendo el
            // estado que esta consulta tiene que saber contestar: es lo que ve un
            // arranque en frio, antes de que nadie haya abierto el primer periodo.
            insertarCrudaConCierre(PERIODO_CRUDO + 7, ENERO.value(), "SOFT_CLOSED",
                    SchemaSeed.SYSTEM_USER_ID);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findFirstOpenFrom(ENERO)).isEmpty();
        }
    }

    @Nested
    @DisplayName("El calendario")
    class Calendario {

        @Test
        @DisplayName("el listado va del mes mas reciente hacia atras")
        void el_listado_va_del_mes_mas_reciente_hacia_atras() {
            AccountingPeriod enero = repository.save(abierto(ENERO));
            AccountingPeriod marzo = repository.save(abierto(MARZO));
            AccountingPeriod febrero = repository.save(abierto(FEBRERO));
            entityManager.flush();
            entityManager.clear();

            // containsSubsequence y no containsExactly: la tabla NO esta vacia. El
            // changeset 362 siembra el primer periodo abierto de la plataforma, y esa
            // fila aparece detras de las tres de este caso. Lo que aqui se prueba es el
            // ORDEN —del mes mas reciente hacia atras—, no cuantas filas hay; exigir la
            // lista exacta ataria la rodaja al contenido de una siembra que no es suya.
            assertThat(repository.findAll(0, 20).content())
                    .extracting(periodo -> periodo.getPeriodKey().value())
                    .containsSubsequence(marzo.getPeriodKey().value(),
                            febrero.getPeriodKey().value(), enero.getPeriodKey().value());
        }

        @Test
        @DisplayName("la cuenta de meses abiertos excluye el que se le indica")
        void la_cuenta_de_meses_abiertos_excluye_el_indicado() {
            // Es la consulta que sostiene «siempre al menos un periodo abierto»: el mes
            // que se esta cerrando todavia figura abierto cuando se pregunta.
            // La cuenta se mide contra la linea base y no contra un literal: el
            // changeset 362 ya deja un periodo abierto sembrado, y clavar un 1L ataria
            // la rodaja a que esa siembra no crezca nunca.
            long base = repository.countOpenExcluding(-1L);
            AccountingPeriod enero = repository.save(abierto(ENERO));
            repository.save(abierto(FEBRERO));
            cerrar(repository.save(abierto(MARZO)));
            entityManager.flush();
            entityManager.clear();

            // De los tres que abre este caso, marzo queda cerrado y enero se excluye
            // por id: suma exactamente febrero sobre la linea base.
            assertThat(repository.countOpenExcluding(enero.getId())).isEqualTo(base + 1);
        }

        @Test
        @DisplayName("la pagina acotada respeta el tope del kernel de paginacion")
        void la_pagina_acotada_respeta_el_tope() {
            repository.save(abierto(ENERO));
            entityManager.flush();
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAll(0, 100000).pageSize()).isEqualTo(200);
        }
    }

    private static AccountingPeriod abierto(AccountingPeriodKey clave) {
        return AccountingPeriod.open(clave, CREADO_EL);
    }

    /**
     * Cierra en blando un periodo ya guardado. Vive aqui, en el andamio, y no en el
     * cuerpo de ningun caso: lo que el caso quiere afirmar es el resultado de la
     * consulta, no el camino para llegar a ese estado.
     */
    private void cerrar(AccountingPeriod periodo) {
        periodo.softClose(SchemaSeed.SYSTEM_USER_ID, CERRADO_EL);
        repository.save(periodo);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para las comprobaciones que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * las cuida.
     *
     * <p>
     * Los {@code NULL} van como literales y no como parametros: una consulta nativa
     * sin metadatos de tipo no puede inferir el tipo de un {@code null}, y el fallo
     * saldria como un error de binding que no tiene nada que ver con lo que el caso
     * quiere probar.
     */
    private void insertarCrudaSinCierre(Long id, String clave, String estado) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, closed_at,
                                                closed_by_system_user_id, reopened_at,
                                                reopened_by_system_user_id, reopened_reason,
                                                created_date, version)
                VALUES (:id, :clave, :estado, NULL, NULL, NULL, NULL, NULL, :creado, 0)
                """).setParameter("id", id).setParameter("clave", clave)
                .setParameter("estado", estado).setParameter("creado", CREADO_EL).executeUpdate();
    }

    private void insertarCrudaConCierre(Long id, String clave, String estado, Long firma) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, closed_at,
                                                closed_by_system_user_id, reopened_at,
                                                reopened_by_system_user_id, reopened_reason,
                                                created_date, version)
                VALUES (:id, :clave, :estado, :cerrado, :firma, NULL, NULL, NULL, :creado, 0)
                """).setParameter("id", id).setParameter("clave", clave)
                .setParameter("estado", estado).setParameter("cerrado", CERRADO_EL)
                .setParameter("firma", firma).setParameter("creado", CREADO_EL).executeUpdate();
    }
}
