package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleSubscription;
import com.vetsoftware.app.subscriptionbilling.domain.BillingPeriodicity;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La consulta que decide <b>a quien se le cobra hoy</b>, contra MySQL real.
 *
 * <p>
 * <b>Nadie la ejecutaba.</b> {@code JpaDueSubscriptionQueryPort} no es un
 * {@code Jpa<Algo>Repository}, asi que {@code ADAPTADOR_JPA_CON_RODAJA} no lo
 * alcanza —la regla lo dice por escrito—, y no habia ni un test que lo
 * nombrara. Es la misma via por la que sobrevivio meses el defecto #196: SQL
 * nativo, fuera del predicado de la regla, sin rodaja escrita a mano.
 *
 * <p>
 * <b>Y aqui se paga en pesos.</b> Un contrato que esta consulta se deja fuera
 * no se factura ese mes y nadie lo nota hasta que alguien cuadre la cartera;
 * uno que devuelve de mas se factura dos veces. Los dos errores son
 * silenciosos.
 *
 * <h2>Como se acota, y por que hace falta acotar</h2>
 *
 * <p>
 * El contenedor MySQL es <b>uno solo para toda la suite</b> y
 * {@link SchemaSeed} ya siembra dos contratos vigentes (970 y 971) con
 * {@code next_billing_date} en febrero de 2026. Un aserto de conteo global aqui
 * seria un fallo intermitente esperando. Todo lo de esta clase se acota con el
 * <b>cursor</b>: los ids propios empiezan en {@link #PRIMER_ID} y las llamadas
 * pasan {@link #ANTES_DE_LOS_MIOS} como {@code afterId}, asi que el
 * {@code WHERE s.id > :afterId} deja fuera al andamio y a cualquier fila que
 * siembre una migracion.
 *
 * <h2>Una empresa por contrato, y no es un capricho</h2>
 *
 * <p>
 * {@code uq_subscriptions_active_company} sobre {@code active_marker} impone
 * <b>un solo contrato vigente por empresa</b>. Sembrar seis contratos vigentes
 * exige por tanto seis empresas; hacerlo con una sola habria hecho que el
 * segundo {@code INSERT} reventara y que este test probara la restriccion en
 * vez de la consulta —«el verde por el motivo equivocado» en su version roja—.
 *
 * <p>
 * <b>Efecto colateral que si vale como asercion</b>: como hay varias empresas,
 * {@link SinFiltroDeEmpresa} puede afirmar que el barrido devuelve contratos de
 * <em>mas de una</em>. Esa consulta no lleva filtro de empresa a proposito —es
 * un barrido de plataforma— y sin dos empresas en el resultado esa propiedad se
 * cumpliria de forma vacia.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDueSubscriptionQueryPort — a quien le toca cobro hoy, contra MySQL real")
class DueSubscriptionQueryPortIT extends AbstractDataJpaTest {

    /** El dia del barrido. Cae en el primero de mes a proposito. */
    private static final LocalDate DIA_DEL_BARRIDO = LocalDate.of(2026, 9, 1);

    private static final long PRIMER_ID = 9000L;
    private static final long ANTES_DE_LOS_MIOS = PRIMER_ID - 1;
    private static final int LOTE_AMPLIO = 100;

    /** Vence justo hoy: la frontera del {@code <=}. */
    private static final long VENCE_HOY = 9000L;
    /** Vencio ayer: el rezagado que el barrido anterior no cogio. */
    private static final long VENCIO_AYER = 9001L;
    /** Vence mañana: la frontera del otro lado. */
    private static final long VENCE_MANANA = 9002L;
    /** Sin fecha de cobro y con la prueba acabada ayer: le toca hoy. */
    private static final long PRUEBA_ACABADA_AYER = 9003L;
    /** Sin fecha de cobro y con la prueba acabando hoy: todavia NO le toca. */
    private static final long PRUEBA_ACABA_HOY = 9004L;
    /** Sin fecha de cobro y sin prueba: manda {@code start_date}. */
    private static final long SIN_PRUEBA_NI_FECHA = 9005L;
    /** Cancelado y con fecha vencida: no se cobra a quien ya se fue. */
    private static final long CANCELADO = 9006L;
    /** Dado de baja logica y con fecha vencida: tampoco. */
    private static final long DE_BAJA = 9007L;
    /** En solo lectura por mora, y con fecha vencida: <b>si</b> se le cobra. */
    private static final long EN_SOLO_LECTURA = 9008L;

    @Autowired
    private JpaDueSubscriptionQueryPort port;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLosContratosDelBarrido() {
        SchemaSeed.seed(entityManager);

        contrato(VENCE_HOY, "MONTHLY", "ACTIVE", "2026-01-01", null, DIA_DEL_BARRIDO.toString());
        contrato(VENCIO_AYER, "ANNUAL", "ACTIVE", "2026-01-01", null, "2026-08-31");
        contrato(VENCE_MANANA, "MONTHLY", "ACTIVE", "2026-01-01", null, "2026-09-02");
        contrato(PRUEBA_ACABADA_AYER, "MONTHLY", "TRIALING", "2026-08-01", "2026-08-31", null);
        contrato(PRUEBA_ACABA_HOY, "MONTHLY", "TRIALING", "2026-08-01", "2026-09-01", null);
        contrato(SIN_PRUEBA_NI_FECHA, "MONTHLY", "ACTIVE", "2026-08-01", null, null);
        contrato(CANCELADO, "MONTHLY", "CANCELLED", "2026-01-01", null, "2026-08-01");
        contratoDeBaja(DE_BAJA, "2026-08-01");
        contrato(EN_SOLO_LECTURA, "MONTHLY", "READ_ONLY", "2026-01-01", null, "2026-08-15");

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("La frontera del dia")
    class FronteraDelDia {

        /**
         * <b>El paso de mes es donde se rompe todo en facturacion.</b> El contrato que
         * vence justo el dia del barrido tiene que entrar ({@code <=}); el que vence al
         * dia siguiente, no. Un {@code <} en vez de {@code <=} deja sin cobrar a todos
         * los contratos anclados al dia 1, que son la mayoria.
         */
        @Test
        @DisplayName("entra el que vence hoy y el que ya vencio, no el que vence mañana")
        void entra_el_que_vence_hoy_y_el_que_ya_vencio() {
            assertThat(idsDelBarrido()).contains(VENCE_HOY, VENCIO_AYER)
                    .doesNotContain(VENCE_MANANA);
        }

        /**
         * <b>La prueba que acaba hoy todavia no se cobra.</b> El primer periodo
         * cobrable empieza al dia siguiente del ultimo dia de prueba —de ahi el
         * {@code DATE_ADD(trial_end_date, INTERVAL 1 DAY)}—, asi que una prueba que
         * termina el mismo dia del barrido produce fecha de cobro <em>mañana</em>.
         * Quitar el {@code INTERVAL 1 DAY} le cobraria al cliente un dia antes de que
         * su prueba terminara: la queja llega el mismo dia.
         */
        @Test
        @DisplayName("la prueba que acabo ayer se cobra; la que acaba hoy, todavia no")
        void la_prueba_que_acabo_ayer_se_cobra_y_la_que_acaba_hoy_no() {
            assertThat(idsDelBarrido()).contains(PRUEBA_ACABADA_AYER)
                    .doesNotContain(PRUEBA_ACABA_HOY);
        }

        /**
         * La rama del {@code COALESCE} que solo se ejecuta cuando <b>no hay prueba</b>:
         * {@code DATE_ADD(NULL, …)} es {@code NULL} y manda {@code start_date}. Es el
         * contrato que nunca tuvo prueba y al que aun no se le ha escrito la primera
         * fecha de cobro; sin esta rama no se le factura nunca.
         */
        @Test
        @DisplayName("sin fecha de cobro y sin prueba manda la fecha de firma")
        void sin_fecha_de_cobro_y_sin_prueba_manda_la_fecha_de_firma() {
            assertThat(idsDelBarrido()).contains(SIN_PRUEBA_NI_FECHA);
        }

        /**
         * El dia anterior al barrido no debe traer al que vence hoy. Es la misma
         * frontera mirada desde el otro lado, y lo que impide que el test anterior pase
         * por casualidad con un {@code >=} mal puesto en cualquier sitio.
         */
        @Test
        @DisplayName("el dia de antes todavia no arrastra al que vence hoy")
        void el_dia_de_antes_todavia_no_arrastra_al_que_vence_hoy() {
            List<Long> ids = idsDe(port.dueForBillingAfter(DIA_DEL_BARRIDO.minusDays(1),
                    ANTES_DE_LOS_MIOS, LOTE_AMPLIO));

            assertThat(ids).contains(VENCIO_AYER).doesNotContain(VENCE_HOY, PRUEBA_ACABADA_AYER);
        }
    }

    @Nested
    @DisplayName("Que estados se cobran")
    class Estados {

        /**
         * <b>{@code READ_ONLY} se cobra</b>, y es contraintuitivo: la clinica esta en
         * solo lectura por mora, pero sigue siendo cliente y sus lineas de pago
         * obligatorio —facturacion electronica DIAN— se devengan igual (R-TRIAL-13).
         * Dejarlo fuera es dejar de cobrar a quien mas debe.
         */
        @Test
        @DisplayName("un contrato en solo lectura por mora sigue entrando al cobro")
        void un_contrato_en_solo_lectura_por_mora_sigue_entrando_al_cobro() {
            assertThat(idsDelBarrido()).contains(EN_SOLO_LECTURA);
        }

        /**
         * Cobrarle a quien cancelo es la reclamacion mas cara que existe. El estado
         * {@code CANCELLED} no esta en la lista del {@code IN}.
         */
        @Test
        @DisplayName("a un contrato cancelado no se le cobra aunque tenga fecha vencida")
        void a_un_contrato_cancelado_no_se_le_cobra() {
            assertThat(idsDelBarrido()).doesNotContain(CANCELADO);
        }

        /**
         * La baja logica es la otra mitad: {@code enabled = false} con fecha vencida y
         * estado vigente. Sin el {@code s.enabled = true} del {@code WHERE} este
         * contrato entraria, porque su estado si esta en la lista.
         */
        @Test
        @DisplayName("un contrato dado de baja no entra aunque su estado sea vigente")
        void un_contrato_dado_de_baja_no_entra() {
            assertThat(idsDelBarrido()).doesNotContain(DE_BAJA);
        }
    }

    @Nested
    @DisplayName("El cursor y el tamaño del lote")
    class Cursor {

        /**
         * <b>Sin orden total el cierre mensual factura dos veces o no factura.</b> El
         * barrido avanza por cursor, asi que el {@code ORDER BY s.id ASC} es lo unico
         * que garantiza que la pagina siguiente empiece donde acabo la anterior.
         */
        @Test
        @DisplayName("devuelve los contratos ordenados por id ascendente")
        void devuelve_los_contratos_ordenados_por_id_ascendente() {
            assertThat(idsDelBarrido()).isSorted();
        }

        @Test
        @DisplayName("el cursor no repite lo que ya devolvio")
        void el_cursor_no_repite_lo_que_ya_devolvio() {
            List<Long> primeraPasada = idsDelBarrido();

            List<Long> segundaPasada = idsDe(
                    port.dueForBillingAfter(DIA_DEL_BARRIDO, primeraPasada.get(0), LOTE_AMPLIO));

            assertThat(segundaPasada).doesNotContain(primeraPasada.get(0))
                    .containsExactlyElementsOf(primeraPasada.subList(1, primeraPasada.size()));
        }

        /**
         * El lote acota, y el que se devuelve es el <b>principio</b> del orden: si
         * {@code setMaxResults} se aplicara antes del {@code ORDER BY}, el cursor
         * avanzaria saltandose contratos.
         */
        @Test
        @DisplayName("el tamaño del lote corta por el principio del orden")
        void el_tamano_del_lote_corta_por_el_principio_del_orden() {
            List<Long> completo = idsDelBarrido();

            List<Long> primerosDos = idsDe(
                    port.dueForBillingAfter(DIA_DEL_BARRIDO, ANTES_DE_LOS_MIOS, 2));

            assertThat(primerosDos).containsExactlyElementsOf(completo.subList(0, 2));
        }

        @Test
        @DisplayName("un lote sin tamaño positivo se rechaza antes de tocar la base")
        void un_lote_sin_tamano_positivo_se_rechaza() {
            assertThatThrownBy(() -> port.dueForBillingAfter(DIA_DEL_BARRIDO, ANTES_DE_LOS_MIOS, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("batchSize must be positive");
        }

        @Test
        @DisplayName("un barrido sin dia se rechaza antes de tocar la base")
        void un_barrido_sin_dia_se_rechaza() {
            assertThatThrownBy(() -> port.dueForBillingAfter(null, ANTES_DE_LOS_MIOS, LOTE_AMPLIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("runDate is required");
        }
    }

    @Nested
    @DisplayName("Los ocho escalares que viajan")
    class Proyeccion {

        /**
         * Las cinco columnas de fecha se leen por posicion desde un {@code Object[]}, y
         * cruzar dos de ellas compila y no avisa. Por eso el contrato de este caso
         * lleva <b>cinco fechas distintas</b>: firma, fin de prueba, inicio y fin del
         * periodo y proxima fecha de cobro no coinciden en ningun par.
         */
        @Test
        @DisplayName("cada columna llega a su componente sin cruzarse con la de al lado")
        void cada_columna_llega_a_su_componente_sin_cruzarse() {
            BillingCycleSubscription contrato = unoDelBarrido(PRUEBA_ACABADA_AYER);

            assertThat(contrato.id()).isEqualTo(PRUEBA_ACABADA_AYER);
            assertThat(contrato.companyId()).isEqualTo(PRUEBA_ACABADA_AYER);
            assertThat(contrato.periodicity()).isEqualTo(BillingPeriodicity.MONTHLY);
            assertThat(contrato.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(contrato.trialEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
            assertThat(contrato.currentPeriodStart()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(contrato.currentPeriodEnd()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(contrato.nextBillingDate()).isNull();
        }

        /**
         * El ciclo llega como cadena de la base y se convierte con
         * {@code BillingPeriodicity.de}. El anual se siembra a proposito: con solo
         * mensuales, esa conversion se cumpliria por tener un unico valor posible en
         * los datos.
         */
        @Test
        @DisplayName("el ciclo anual se traduce a su periodicidad, no solo el mensual")
        void el_ciclo_anual_se_traduce_a_su_periodicidad() {
            assertThat(unoDelBarrido(VENCIO_AYER).periodicity())
                    .isEqualTo(BillingPeriodicity.ANNUAL);
        }
    }

    @Nested
    @DisplayName("Un barrido de plataforma, sin filtro de empresa")
    class SinFiltroDeEmpresa {

        /**
         * <b>Esta propiedad no puede probarse con una sola empresa.</b> La consulta no
         * lleva {@code company_id} porque su consumidor es un puerto cerrado a
         * {@code hasRole('SYSTEM')} y su pregunta es «a quien le toca cobro en toda la
         * plataforma». Con un unico tenant sembrado, «devuelve contratos de varias
         * empresas» seria cierto por ausencia de datos y seguiria verde el dia que
         * alguien le añadiera un filtro de empresa por error.
         */
        @Test
        @DisplayName("el barrido cruza la frontera de empresa y trae contratos de varias")
        void el_barrido_cruza_la_frontera_de_empresa() {
            List<BillingCycleSubscription> lote = port.dueForBillingAfter(DIA_DEL_BARRIDO,
                    ANTES_DE_LOS_MIOS, LOTE_AMPLIO);

            assertThat(lote).extracting(BillingCycleSubscription::companyId).doesNotHaveDuplicates()
                    .hasSizeGreaterThan(1);
        }
    }

    private List<Long> idsDelBarrido() {
        return idsDe(port.dueForBillingAfter(DIA_DEL_BARRIDO, ANTES_DE_LOS_MIOS, LOTE_AMPLIO));
    }

    private BillingCycleSubscription unoDelBarrido(long id) {
        return port.dueForBillingAfter(DIA_DEL_BARRIDO, id - 1, 1).get(0);
    }

    private static List<Long> idsDe(List<BillingCycleSubscription> lote) {
        return lote.stream().map(BillingCycleSubscription::id).toList();
    }

    /**
     * Cada contrato con <b>su propia empresa</b>, con el mismo id, por
     * {@code uq_subscriptions_active_company}. {@code active_marker} es
     * {@code GENERATED ALWAYS} y no se nombra en el {@code INSERT}: MySQL devuelve
     * ERROR 3105 si se hace, aunque el valor sea nulo.
     */
    private void contrato(long id, String ciclo, String estado, String firma, String finDePrueba,
            String proximoCobro) {
        empresa(id);
        entityManager.createNativeQuery("""
                INSERT INTO subscriptions (id, subscription_number, company_id, quote_id,
                                           price_list_id, billing_cycle, status, start_date,
                                           trial_end_date, current_period_start,
                                           current_period_end, next_billing_date,
                                           commitment_end_date, grace_days, past_due_since,
                                           auto_renew, created_date, enabled, version)
                VALUES (:id, :numero, :empresa, NULL, :lista, :ciclo, :estado, :firma,
                        %s, '2026-03-01', '2026-03-31', %s,
                        NULL, 5, NULL, true, NOW(6), true, 0)
                """.formatted(fecha(finDePrueba), fecha(proximoCobro))).setParameter("id", id)
                .setParameter("numero", "SUS-TEST-" + id).setParameter("empresa", id)
                .setParameter("lista", SchemaSeed.PRICE_LIST_ID).setParameter("ciclo", ciclo)
                .setParameter("estado", estado).setParameter("firma", firma).executeUpdate();
    }

    /**
     * El de baja logica: {@code enabled = false} con estado vigente y fecha
     * vencida.
     */
    private void contratoDeBaja(long id, String proximoCobro) {
        empresa(id);
        entityManager.createNativeQuery("""
                INSERT INTO subscriptions (id, subscription_number, company_id, quote_id,
                                           price_list_id, billing_cycle, status, start_date,
                                           trial_end_date, current_period_start,
                                           current_period_end, next_billing_date,
                                           commitment_end_date, grace_days, past_due_since,
                                           auto_renew, created_date, enabled, version)
                VALUES (:id, :numero, :empresa, NULL, :lista, 'MONTHLY', 'ACTIVE', '2026-01-01',
                        NULL, '2026-03-01', '2026-03-31', %s,
                        NULL, 5, NULL, true, NOW(6), false, 0)
                """.formatted(fecha(proximoCobro))).setParameter("id", id)
                .setParameter("numero", "SUS-TEST-" + id).setParameter("empresa", id)
                .setParameter("lista", SchemaSeed.PRICE_LIST_ID).executeUpdate();
    }

    /**
     * Una fecha nulable va <b>al texto</b> del {@code INSERT}, no como parametro.
     * Hibernate no puede inferir el tipo de un parametro nulo en una consulta
     * nativa, y este andamio necesita insertar {@code trial_end_date} y
     * {@code next_billing_date} vacias: son justamente las dos columnas cuyo nulo
     * dispara la rama del {@code COALESCE} que se quiere probar. Es el mismo
     * criterio que usa {@link SchemaSeed}, y aqui los valores son constantes del
     * test, no entrada de nadie.
     */
    private static String fecha(String iso) {
        return iso == null ? "NULL" : "'" + iso + "'";
    }

    private void empresa(long id) {
        entityManager.createNativeQuery("""
                INSERT INTO companies (id, name, identifier, city_id)
                VALUES (:id, :nombre, :nit, :ciudad)
                """).setParameter("id", id).setParameter("nombre", "Clinica de barrido " + id)
                .setParameter("nit", "800" + id).setParameter("ciudad", SchemaSeed.CITY_ID)
                .executeUpdate();
    }
}
