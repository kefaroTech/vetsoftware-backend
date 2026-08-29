package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.domain.CustomerCreditLotRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * El lote de saldo a favor del que se puede gastar, contra MySQL real.
 *
 * <p>
 * <b>Nadie ejecutaba este SQL.</b> {@code JpaCustomerCreditQueryPort} no es un
 * {@code Jpa<Algo>Repository}, asi que queda fuera de
 * {@code ADAPTADOR_JPA_CON_RODAJA}, y ningun test del repositorio lo nombraba.
 * Su consulta lleva <b>tres</b> condiciones en el {@code WHERE} y un
 * {@code FOR UPDATE}, y cada una de las tres sostiene algo distinto:
 *
 * <ul>
 * <li>{@code e.id} — el lote que se pide.</li>
 * <li>{@code e.company_id} — <b>el aislamiento entre clinicas</b>. Sin el, una
 * empresa aplica el saldo a favor de otra contra su propia factura: dinero de
 * la clinica A pagando la deuda de la B, sin traza y sin error.</li>
 * <li>{@code e.entry_kind = 'GRANT'} — <b>lo que impide gastar dos veces el
 * mismo dinero</b>. Un {@code CONSUMPTION} o una {@code EXPIRATION} son
 * asientos que <em>restan</em>; devolverlos como origen del que aplicar seria
 * volver a gastar lo ya gastado.</li>
 * </ul>
 *
 * <p>
 * <b>Cada condicion tiene su fila sembrada.</b> Sin una fila que la ejercite,
 * un filtro se cumple por ausencia de datos y el test sigue verde el dia que
 * alguien lo borra — la prueba vacia. Aqui hay un lote de la empresa ajena y un
 * asiento de consumo <em>precisamente</em> para que quitar cualquiera de los
 * dos filtros ponga esta clase en rojo.
 *
 * <p>
 * <b>Lo que este IT NO cubre, declarado.</b> El {@code FOR UPDATE} toma candado
 * de verdad —la sentencia se ejecuta y MySQL lo concede—, pero que ese candado
 * <em>bloquee</em> a una segunda transaccion no se comprueba aqui: haria falta
 * un segundo hilo con su propia conexion y
 * {@code @Transactional(NOT_SUPPORTED)}, que es el andamio de
 * {@code ApplyBillingDocumentConcurrencyIT}. Queda como hueco consciente y no
 * se disfraza de cobertura.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCustomerCreditQueryPort — el lote de saldo a favor contra MySQL real")
class CustomerCreditQueryPortIT extends AbstractDataJpaTest {

    private static final long LOTE_PROPIO = 9100L;
    private static final long LOTE_SIN_CADUCIDAD = 9101L;
    private static final long LOTE_AJENO = 9102L;
    private static final long CONSUMO = 9103L;

    private static final BigDecimal IMPORTE_PROPIO = new BigDecimal("150000.00");
    private static final LocalDate CADUCA_EL = LocalDate.of(2026, 12, 31);

    @Autowired
    private JpaCustomerCreditQueryPort port;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLosAsientosDeSaldo() {
        SchemaSeed.seed(entityManager);

        concesion(LOTE_PROPIO, SchemaSeed.COMPANY_ID, IMPORTE_PROPIO, CADUCA_EL);
        concesion(LOTE_SIN_CADUCIDAD, SchemaSeed.COMPANY_ID, new BigDecimal("40000.00"), null);
        concesion(LOTE_AJENO, SchemaSeed.OTRA_COMPANY_ID, new BigDecimal("999999.00"), null);
        consumo(CONSUMO, SchemaSeed.COMPANY_ID, LOTE_PROPIO, new BigDecimal("-25000.00"));

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("El lote que si se puede gastar")
    class Concesion {

        /**
         * Los cuatro escalares se leen por posicion de un {@code Object[]}. Van con
         * valores distintos entre si —dos {@code Long} que no coinciden, un importe con
         * decimales y una fecha— para que un cruce entre el id del asiento y el de la
         * empresa no pueda pasar desapercibido.
         */
        @Test
        @DisplayName("devuelve el lote con su importe y su caducidad, sin cruzar columnas")
        void devuelve_el_lote_con_su_importe_y_su_caducidad() {
            CustomerCreditLotRef lote = port
                    .lockLotByIdAndCompanyId(LOTE_PROPIO, SchemaSeed.COMPANY_ID).orElseThrow();

            assertThat(lote.id()).isEqualTo(LOTE_PROPIO);
            assertThat(lote.companyId()).isEqualTo(SchemaSeed.COMPANY_ID);
            assertThat(lote.grantedAmount()).isEqualByComparingTo(IMPORTE_PROPIO);
            assertThat(lote.expiresOn()).isEqualTo(CADUCA_EL);
        }

        /**
         * <b>La caducidad nula es un dato, no un error.</b> Un saldo sin fecha de
         * caducidad no vence nunca, y la conversion de la columna {@code DATE} tiene
         * una rama propia para el nulo: si se rompiera —un {@code NullPointerException}
         * o un {@code IllegalStateException} del conversor— la aplicacion de ese saldo
         * fallaria entera y el cliente pagaria una factura que ya tenia cubierta.
         */
        @Test
        @DisplayName("un lote sin caducidad se lee sin romper la conversion de la fecha")
        void un_lote_sin_caducidad_se_lee_sin_romper_la_conversion() {
            CustomerCreditLotRef lote = port
                    .lockLotByIdAndCompanyId(LOTE_SIN_CADUCIDAD, SchemaSeed.COMPANY_ID)
                    .orElseThrow();

            assertThat(lote.expiresOn()).isNull();
            assertThat(lote.haCaducado(LocalDate.of(2099, 1, 1))).isFalse();
        }

        /**
         * El asiento de consumo existe en la tabla, es de la misma empresa y cuelga de
         * este mismo lote; lo unico que lo deja fuera es {@code entry_kind = 'GRANT'}.
         * Quitar esa condicion del {@code WHERE} devuelve un importe <b>negativo</b>,
         * que el {@code record} de destino rechaza —«a credit lot must have been
         * granted a positive amount»—: el fallo seria ruidoso, pero solo porque este
         * caso existe para provocarlo.
         */
        @Test
        @DisplayName("un asiento de consumo no es un lote del que se pueda gastar")
        void un_asiento_de_consumo_no_es_un_lote_del_que_gastar() {
            assertThat(port.lockLotByIdAndCompanyId(CONSUMO, SchemaSeed.COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Aislamiento entre clinicas")
    class Tenancy {

        /**
         * <b>El lote ajeno existe y tiene saldo de sobra</b> — casi un millon—, asi que
         * este caso no puede pasar por ausencia de datos: lo unico que lo deja fuera es
         * el {@code company_id} del {@code WHERE}.
         */
        @Test
        @DisplayName("el saldo de otra clinica no se entrega ni con el id correcto")
        void el_saldo_de_otra_clinica_no_se_entrega() {
            assertThat(port.lockLotByIdAndCompanyId(LOTE_AJENO, SchemaSeed.COMPANY_ID)).isEmpty();
        }

        /**
         * Y la otra mitad: con su empresa correcta si aparece. Sin este caso, el
         * anterior seria igual de verde con una consulta que no devuelve nunca nada.
         */
        @Test
        @DisplayName("el mismo lote si aparece cuando lo pide su propia clinica")
        void el_mismo_lote_aparece_para_su_propia_clinica() {
            assertThat(port.lockLotByIdAndCompanyId(LOTE_AJENO, SchemaSeed.OTRA_COMPANY_ID))
                    .isPresent();
        }

        /**
         * <b>El candado ni siquiera se pide si falta un dato.</b> Un {@code company_id}
         * nulo llegando al {@code WHERE} devolveria cero filas de todas formas, pero la
         * guarda existe para que el caso de uso no dependa de esa casualidad.
         */
        @Test
        @DisplayName("sin lote o sin empresa no se pide candado a la base")
        void sin_lote_o_sin_empresa_no_se_pide_candado() {
            assertThat(port.lockLotByIdAndCompanyId(null, SchemaSeed.COMPANY_ID)).isEmpty();
            assertThat(port.lockLotByIdAndCompanyId(LOTE_PROPIO, null)).isEmpty();
        }

        @Test
        @DisplayName("un lote que no existe no devuelve nada")
        void un_lote_que_no_existe_no_devuelve_nada() {
            assertThat(port.lockLotByIdAndCompanyId(999_999L, SchemaSeed.COMPANY_ID)).isEmpty();
        }
    }

    /**
     * Una concesion de saldo. {@code origin_kind = 'MANUAL'} es la unica rama de
     * {@code chk_cce_origin_branch} que no exige un pago, un documento ni un
     * contrato de origen, asi que evita arrastrar aqui la cadena entera de cobro
     * para probar una consulta que no la mira. {@code origin_marker} es
     * {@code GENERATED} y solo se calcula para los otros cuatro origenes, asi que
     * {@code uq_cce_origin} no entra en juego.
     */
    private void concesion(long id, Long empresa, BigDecimal importe, LocalDate caduca) {
        entityManager.createNativeQuery("""
                INSERT INTO customer_credit_entries (id, company_id, entry_kind, amount,
                                                     lot_entry_id, origin_kind, origin_payment_id,
                                                     origin_document_id, origin_subscription_id,
                                                     occurred_at, value_date, expires_on,
                                                     client_request_id, created_date)
                VALUES (:id, :empresa, 'GRANT', :importe, NULL, 'MANUAL', NULL, NULL, NULL,
                        '2026-06-01 10:00:00.000000', '2026-06-01', %s, :peticion, NOW(6))
                """.formatted(caduca == null ? "NULL" : "'" + caduca + "'")).setParameter("id", id)
                .setParameter("empresa", empresa).setParameter("importe", importe)
                .setParameter("peticion", "req-credit-" + id).executeUpdate();
    }

    /**
     * Un asiento que <b>resta</b>. {@code chk_cce_sign} exige importe negativo y
     * {@code chk_cce_lot} exige que nombre el lote del que sale: es una fila
     * legitima de la tabla, que es justo lo que la hace util aqui.
     */
    private void consumo(long id, Long empresa, long lote, BigDecimal importe) {
        entityManager.createNativeQuery("""
                INSERT INTO customer_credit_entries (id, company_id, entry_kind, amount,
                                                     lot_entry_id, origin_kind, origin_payment_id,
                                                     origin_document_id, origin_subscription_id,
                                                     occurred_at, value_date, expires_on,
                                                     client_request_id, created_date)
                VALUES (:id, :empresa, 'CONSUMPTION', :importe, :lote, 'MANUAL', NULL, NULL, NULL,
                        '2026-07-01 10:00:00.000000', '2026-07-01', NULL, :peticion, NOW(6))
                """).setParameter("id", id).setParameter("empresa", empresa)
                .setParameter("importe", importe).setParameter("lote", lote)
                .setParameter("peticion", "req-credit-" + id).executeUpdate();
    }
}
