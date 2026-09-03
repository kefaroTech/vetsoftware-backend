package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogAreaRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogRequirementRowDto;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.infrastructure.persistence.JpaCatalogQueryPorts;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Import;

/**
 * La rodaja que le faltaba a {@link JpaPublicCatalogQueryPort}, y que su propio
 * javadoc reclama: sus consultas nativas quedan <b>fuera</b> de
 * {@code ADAPTADOR_JPA_CON_RODAJA} —esa regla solo alcanza a los
 * {@code Jpa<Algo>Repository}—, asi que sin esta clase no las ejecutaria nadie
 * en el build. Es exactamente como sobrevivio meses la incidencia #196.
 *
 * <p>
 * <b>Aqui el SQL decide cuanto se le cobra a alguien</b>, asi que lo que se
 * fija es lo que un mock no puede comprobar:
 *
 * <ul>
 * <li><b>{@code structural_minimum} llega como {@code Byte}, no como
 * {@code Boolean}.</b> MySQL entrega {@code TINYINT} asi y nadie lo convierte
 * solo: es la clase de defecto de #472, que tumbo el alta de empresa entera. Un
 * mock devuelve el booleano que le pidas y no ve nada.</li>
 * <li><b>El {@code EXISTS} que decide {@code selfServiceEligible}</b> tiene que
 * dar el mismo veredicto que el gate de la autocontratacion. Se siembra un
 * modulo que cuelga de un paquete publicado y otro que no cuelga de
 * ninguno.</li>
 * <li><b>Las dos columnas {@code included_quantity}</b>, una por ciclo, con
 * valores <em>distintos</em>: si el adaptador leyera la misma dos veces, con
 * valores iguales el test pasaria igual.</li>
 * <li><b>{@code setup_amount}</b>, que en un {@code ONE_TIME} es todo su
 * precio.</li>
 * <li><b>El grafo {@code REQUIRES} que la portada anuncia</b>: que sean los
 * arcos DIRECTOS y no el cierre transitivo, que los cinco predicados de
 * {@code SQL_REQUIREMENTS} descarten cada arco muerto por su propio motivo, y
 * que un requisito sin tarifar se siga anunciando aunque no salga en ninguna de
 * las listas. Contra un mock todo eso es la palabra del test.</li>
 * </ul>
 *
 * <p>
 * <b>Todo lo que se afirma va acotado a la tarifa sembrada aqui o a los codigos
 * {@code TESTCAT_}.</b> El contenedor MySQL es unico para la suite y Liquibase
 * ya siembra el catalogo comercial real, asi que un conteo global seria un
 * fallo intermitente esperando. Los codigos llevan prefijo propio porque
 * {@code uq_catalog_items_code} es UNIQUE global y
 * {@code PublicPlanQueryPortIT} comparte contenedor con sus {@code TEST_}.
 *
 * <p>
 * <b>El adaptador no se declara como bean</b>, por lo mismo que
 * {@code PublicPlanQueryPortIT}: solo necesita un {@code EntityManager} y
 * anadirlo al {@code @Import} cambiaria la clave del
 * {@code MergedContextConfiguration} y costaria un arranque de contexto entero.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPublicCatalogQueryPort — el SQL del catalogo contratable contra MySQL real")
class PublicCatalogQueryPortIT extends AbstractDataJpaTest {

    private static final Long LISTA = 2700L;
    private static final Long LISTA_OTRA = 2701L;

    private static final Long PACK = 2710L;
    private static final Long MOD_EN_PACK = 2711L;
    private static final Long MOD_SUELTO = 2712L;
    private static final Long MOD_SOLO_MENSUAL = 2713L;
    private static final Long MOD_SIN_PRECIO = 2714L;
    private static final Long MOD_BORRADOR = 2715L;
    private static final Long MOD_RETIRADO = 2716L;
    private static final Long CAP_NUCLEO = 2717L;
    private static final Long ONE_TIME = 2718L;
    private static final Long MOD_DE_BAJA = 2719L;

    private static final String COD_PACK = "TESTCAT_PACK";
    private static final String COD_MOD_EN_PACK = "TESTCAT_EN_PACK";
    private static final String COD_MOD_SUELTO = "TESTCAT_SUELTO";
    private static final String COD_SOLO_MENSUAL = "TESTCAT_SOLO_MENSUAL";
    private static final String COD_CAP_NUCLEO = "TESTCAT_CAP_NUCLEO";
    private static final String COD_ONE_TIME = "TESTCAT_ONE_TIME";
    private static final String COD_SIN_PRECIO = "TESTCAT_SIN_PRECIO";
    private static final String COD_BORRADOR = "TESTCAT_BORRADOR";
    private static final String COD_RETIRADO = "TESTCAT_RETIRADO";
    private static final String COD_DE_BAJA = "TESTCAT_DE_BAJA";

    private static final Long AREA_CLINICA = 2780L;
    private static final Long AREA_MOSTRADOR = 2781L;
    private static final Long AREA_APAGADA = 2782L;
    private static final String COD_AREA_CLINICA = "TESTCAT_CLINICA";
    private static final String COD_AREA_MOSTRADOR = "TESTCAT_MOSTRADOR";
    private static final String COD_AREA_APAGADA = "TESTCAT_APAGADA";

    /**
     * La cadena de dos saltos que siembra el changeset 309 en TODOS los entornos,
     * contenedor de test incluido. No se siembra aqui a proposito: es el catalogo
     * comercial de verdad el que tiene que demostrar que se publican los arcos y no
     * el cierre.
     */
    private static final String COD_ALMACENAMIENTO = "EXTRA_STORAGE";
    private static final String COD_LABORATORIO = "LAB_IMAGING";
    private static final String COD_HISTORIA = "CLINICAL_HISTORY";

    @PersistenceContext
    private EntityManager entityManager;

    private JpaPublicCatalogQueryPort port;

    /**
     * El paso vinculante, instanciado aqui a proposito. {@code pricelist} y
     * {@code quote} son rodajas independientes y su codigo de produccion no se
     * importa entre si; este es el unico punto del build donde las <b>dos</b>
     * sentencias nativas se ejecutan sobre las mismas filas, que es lo que hace
     * falta para demostrar que aceptan el mismo conjunto. La alternativa —copiar el
     * predicado en una tercera consulta— probaria la copia y no el original.
     */
    private JpaCatalogQueryPorts.JpaPublishedCatalogItemQueryPort gate;

    @BeforeEach
    void sembrar() {
        port = new JpaPublicCatalogQueryPort(entityManager);
        gate = new JpaCatalogQueryPorts.JpaPublishedCatalogItemQueryPort(entityManager);
        SchemaSeed.seed(entityManager);

        tarifa(LISTA, "TESTCAT-LISTA", LocalDate.of(2026, 8, 1));
        tarifa(LISTA_OTRA, "TESTCAT-OTRA", LocalDate.of(2025, 1, 1));

        // El orden importa: fk_catalog_items_area exige el area antes que el modulo que
        // la referencia. Los sort_order van en el rango 9xxx porque
        // uq_catalog_areas_sort_order es UNIQUE global y la semilla comercial ocupa las
        // decenas bajas.
        area(AREA_MOSTRADOR, COD_AREA_MOSTRADOR, "Mostrador de prueba", 9200, true);
        area(AREA_CLINICA, COD_AREA_CLINICA, "Clinica de prueba", 9100, true);
        area(AREA_APAGADA, COD_AREA_APAGADA, "Area retirada", 9300, false);

        articulo(PACK, COD_PACK, "Pack de prueba", "BUNDLE", null, false, 1, 1, "ACTIVE",
                "NEVER_FREE", null, null);
        articulo(MOD_EN_PACK, COD_MOD_EN_PACK, "Modulo dentro del pack", "MODULE", null, false, 1,
                2, "ACTIVE", "ELIGIBLE", 30, "CONVERT_TO_PAID");
        articulo(MOD_SUELTO, COD_MOD_SUELTO, "Modulo de catalogo interno", "MODULE", null, false, 1,
                3, "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_SOLO_MENSUAL, COD_SOLO_MENSUAL, "Modulo solo mensual", "MODULE", null, false,
                1, 4, "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_SIN_PRECIO, COD_SIN_PRECIO, "Modulo sin tarifar", "MODULE", null, false, 1, 5,
                "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_BORRADOR, COD_BORRADOR, "Modulo en redaccion", "MODULE", null, false, 1, 6,
                "DRAFT", "NEVER_FREE", null, null);
        articulo(MOD_RETIRADO, COD_RETIRADO, "Modulo retirado", "MODULE", null, false, 1, 7,
                "DEPRECATED", "NEVER_FREE", null, null);
        articulo(CAP_NUCLEO, COD_CAP_NUCLEO, "Usuario incluido", "CAPACITY", "USER", true, 1, 8,
                "ACTIVE", "NEVER_FREE", null, null);
        articulo(ONE_TIME, COD_ONE_TIME, "Migracion de datos", "ONE_TIME", null, false, 1, 9,
                "ACTIVE", "NEVER_FREE", null, null);
        articuloDeBaja(MOD_DE_BAJA, COD_DE_BAJA, "Modulo dado de baja");

        linea(2730L, PACK, MOD_EN_PACK, 1, true);

        // Los cuatro que se publican. Ni un solo codigo repetido entre los dos
        // extremos de un mismo arco y ningun arco inverso sembrado: leer dos veces
        // la primera columna, intercambiarlas o devolver dos veces la misma fila
        // rompe el containsExactly en vez de coincidir por casualidad.
        requisito(2760L, MOD_EN_PACK, MOD_SUELTO, "REQUIRES", true);
        requisito(2761L, MOD_SOLO_MENSUAL, MOD_SIN_PRECIO, "REQUIRES", true);
        requisito(2762L, CAP_NUCLEO, MOD_EN_PACK, "REQUIRES", true);
        requisito(2763L, CAP_NUCLEO, ONE_TIME, "REQUIRES", true);

        // Uno por cada predicado del WHERE, para que cada filtro caiga por su
        // propio motivo y no arrastrado por el de al lado.
        requisito(2764L, ONE_TIME, MOD_SUELTO, "RECOMMENDS", true);
        requisito(2765L, MOD_SUELTO, MOD_SOLO_MENSUAL, "REQUIRES", false);
        requisito(2766L, MOD_BORRADOR, MOD_SUELTO, "REQUIRES", true);
        requisito(2767L, MOD_SUELTO, MOD_RETIRADO, "REQUIRES", true);
        requisito(2768L, MOD_DE_BAJA, MOD_SUELTO, "REQUIRES", true);
        requisito(2769L, MOD_SUELTO, MOD_DE_BAJA, "REQUIRES", true);

        precio(2740L, LISTA, PACK, "MONTHLY", 1, 10, 0, "89000.00", "150000.00", true);
        precio(2741L, LISTA, PACK, "MONTHLY", 11, null, 0, "79000.00", "0.00", true);
        precio(2742L, LISTA, PACK, "ANNUAL", 1, null, 0, "890000.00", "200000.00", true);

        precio(2743L, LISTA, MOD_EN_PACK, "MONTHLY", 1, null, 0, "38000.00", "0.00", true);
        precio(2744L, LISTA, MOD_EN_PACK, "ANNUAL", 1, null, 0, "350000.00", "0.00", true);

        precio(2745L, LISTA, MOD_SUELTO, "MONTHLY", 1, null, 0, "25000.00", "0.00", true);
        precio(2746L, LISTA, MOD_SOLO_MENSUAL, "MONTHLY", 1, null, 0, "29000.00", "0.00", true);

        precio(2747L, LISTA, CAP_NUCLEO, "MONTHLY", 1, null, 3, "15000.00", "0.00", true);
        precio(2748L, LISTA, CAP_NUCLEO, "ANNUAL", 1, null, 5, "145000.00", "0.00", true);

        precio(2749L, LISTA, ONE_TIME, "MONTHLY", 1, null, 0, "0.00", "450000.00", true);

        precio(2750L, LISTA, MOD_BORRADOR, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2751L, LISTA, MOD_RETIRADO, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2752L, LISTA, MOD_DE_BAJA, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2753L, LISTA_OTRA, MOD_SIN_PRECIO, "MONTHLY", 1, null, 0, "99000.00", "0.00", true);

        // uq_catalog_items_recommended_bundle es UNIQUE entre los paquetes vivos de
        // toda la tabla, y la semilla comercial ya trae uno marcado: sin apagarlo
        // primero, marcar el de prueba choca contra el indice. El rollback de
        // @DataJpaTest lo devuelve a su sitio al terminar cada caso.
        entityManager
                .createNativeQuery(
                        "UPDATE catalog_items SET recommended = FALSE WHERE recommended = TRUE")
                .executeUpdate();
        recomendar(PACK);
        rotular(MOD_EN_PACK, "En pack");
        entityManager.flush();
    }

    private List<PublicCatalogItemRowDto> articulosDePrueba() {
        return port.findContractableItems(LISTA).stream()
                .filter(fila -> fila.code().startsWith("TESTCAT_")).toList();
    }

    private List<PublicCatalogRequirementRowDto> requisitosDePrueba() {
        return port.findRequirements().stream()
                .filter(fila -> fila.itemCode().startsWith("TESTCAT_")).toList();
    }

    @Nested
    @DisplayName("que entra y que no")
    class Alcance {

        @Test
        @DisplayName("solo los articulos ACTIVE, habilitados y con precio en esa tarifa")
        void solo_los_activos_con_precio_en_esa_tarifa() {
            assertThat(articulosDePrueba()).extracting(PublicCatalogItemRowDto::code)
                    .containsExactlyInAnyOrder(COD_MOD_EN_PACK, COD_MOD_SUELTO, COD_SOLO_MENSUAL,
                            COD_CAP_NUCLEO, COD_ONE_TIME);
        }

        @Test
        @DisplayName("el paquete no sale entre los articulos sueltos: lo sirve findPacks")
        void el_paquete_no_sale_entre_los_sueltos() {
            assertThat(articulosDePrueba()).extracting(PublicCatalogItemRowDto::code)
                    .doesNotContain(COD_PACK);
        }
    }

    @Nested
    @DisplayName("lo que un mock no puede comprobar")
    class ColumnasDeVerdad {

        /**
         * MySQL entrega {@code TINYINT} como {@code Byte}. Si el adaptador hiciera un
         * cast directo a {@code Boolean}, esto revienta con {@code ClassCastException};
         * si comparase mal, {@code mandatory} saldria invertido.
         */
        @Test
        @DisplayName("structural_minimum llega como TINYINT y se convierte: el del nucleo sale obligatorio")
        void structural_minimum_se_convierte_desde_tinyint() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.mandatory()).isTrue());
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.mandatory()).isFalse());
        }

        /**
         * El mismo predicado que {@code JpaPublishedCatalogItemQueryPort}: colgar de un
         * paquete ACTIVE publicado. El modulo suelto tiene precio y no cuelga de
         * ninguno, asi que la contratacion lo rechazaria y aqui sale marcado.
         */
        @Test
        @DisplayName("selfServiceEligible es el EXISTS del gate, no una etiqueta")
        void self_service_eligible_es_el_exists_del_gate() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_EN_PACK.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isTrue());
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isFalse());
        }

        /**
         * &#9940; <b>El agujero que las tres copias del gate no tapaban, y el motivo de
         * que el {@code CASE} lleve ahora {@code item_type IN ('MODULE',
         * 'CAPACITY')}.</b>
         *
         * <p>
         * El {@code WHERE} exterior de {@code SQL_ITEMS} admite {@code ONE_TIME} —y
         * tiene que admitirlo: la portada publica el precio de lista de la implantacion
         * y de la migracion—, mientras que el paso vinculante,
         * {@code JpaPublishedCatalogItemQueryPort.SQL_PUBLISHED_ID_BY_CODE}, <b>no lo
         * admite</b>: su rama del componente exige {@code MODULE} o {@code CAPACITY}.
         * Sin este predicado bastaba con que alguien metiera un cargo unico dentro de
         * un pack para que el {@code EXISTS} lo marcara contratable.
         *
         * <p>
         * <b>La direccion del error es lo que lo hace caro.</b> Un
         * {@code selfServiceEligible} conservador de mas pierde una venta y el
         * prospecto se entera <em>antes</em>; uno optimista lo estrella
         * <em>despues</em>, en el paso 6, cuando ya se registro y verifico el correo,
         * con un texto que ni siquiera le dice que linea sobra. Y esta columna viaja al
         * contrato publico con una {@code @Schema} que promete por escrito que los
         * cargos unicos salen fuera «porque se negocian».
         *
         * <p>
         * <b>Este estado hay que sembrarlo a proposito.</b> Ninguna semilla mete hoy un
         * {@code ONE_TIME} dentro de un paquete —la 309 solo cuelga trece
         * {@code MODULE} y un {@code CAPACITY}—, que es justo lo que hacia que la
         * promesa la sostuviera el dato y no el SQL, y por lo que ninguna fila real
         * podia delatar el defecto.
         */
        @Test
        @DisplayName("un cargo unico dentro de un paquete NO es contratable por autoservicio")
        void un_one_time_dentro_de_un_paquete_no_es_contratable() {
            linea(2731L, PACK, ONE_TIME, 1, true);

            assertThat(articulosDePrueba()).filteredOn(f -> COD_ONE_TIME.equals(f.code()))
                    .singleElement()
                    .satisfies(f -> assertThat(f.selfServiceEligible())
                            .as("el paso vinculante rechaza los ONE_TIME: anunciarlo aqui como"
                                    + " contratable estrella al prospecto DESPUES de registrarse")
                            .isFalse());
        }

        /**
         * La otra mitad del mismo predicado: apretar el gate no puede haber dejado
         * fuera a nadie que la contratacion si acepta. Un {@code MODULE} y una
         * {@code CAPACITY} que cuelgan del mismo paquete siguen saliendo contratables,
         * que es lo que separa «alinear» de «romper».
         */
        @Test
        @DisplayName("apretar el gate no saca a los MODULE ni a las CAPACITY del paquete")
        void el_gate_apretado_no_excluye_modulos_ni_capacidades() {
            linea(2732L, PACK, CAP_NUCLEO, 1, true);

            assertThat(articulosDePrueba()).filteredOn(
                    f -> COD_MOD_EN_PACK.equals(f.code()) || COD_CAP_NUCLEO.equals(f.code()))
                    .hasSize(2).allSatisfy(f -> assertThat(f.selfServiceEligible()).isTrue());
        }

        @Test
        @DisplayName("las dos included_quantity son columnas distintas, una por ciclo")
        void las_dos_included_quantity_son_distintas() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyIncludedQuantity()).isEqualTo(3);
                        assertThat(f.annualIncludedQuantity()).isEqualTo(5);
                        assertThat(f.capacityUnit()).isEqualTo("USER");
                    });
        }

        /**
         * Sin esta columna el catalogo anunciaria la migracion de datos como gratuita:
         * su {@code unit_amount} es cero en los dos ciclos.
         */
        @Test
        @DisplayName("el cargo unico publica su precio real, que vive en setup_amount")
        void el_cargo_unico_publica_su_setup_amount() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_ONE_TIME.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyAmount()).isEqualByComparingTo("0.00");
                        assertThat(f.setupAmount()).isEqualByComparingTo("450000.00");
                        assertThat(f.selfServiceEligible()).isFalse();
                    });
        }

        @Test
        @DisplayName("un modulo sin tarifa anual publica null, no un cero ni el mensual")
        void un_modulo_sin_tarifa_anual_publica_null() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_SOLO_MENSUAL.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyAmount()).isEqualByComparingTo("29000.00");
                        assertThat(f.annualAmount()).isNull();
                    });
        }

        /**
         * <b>La segunda via del gate: la capacidad extra, que no cuelga de nada.</b>
         * {@link #CAP_NUCLEO} es una {@code CAPACITY} sin una sola fila en
         * {@code bundle_components} —la misma forma que los cuatro {@code EXTRA_*}— y
         * hasta ahora salia como no contratable. Colgarla de un pack para abrirle el
         * gate no era la salida: esa tabla significa «incluido en el precio del
         * paquete», asi que la anunciaria como regalada y el rechazo de cobro doble
         * prohibiria justo la cesta que se quiere vender.
         */
        @Test
        @DisplayName("una capacidad marcada self_service es contratable sin colgar de ningun pack")
        void una_capacidad_marcada_es_contratable_sin_colgar_de_ningun_pack() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isFalse());

            autoservicio(CAP_NUCLEO);

            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isTrue());
        }

        /**
         * Ampliar el gate es aditivo: la marca abre una via mas, no sustituye a la de
         * {@code bundle_components} ni contagia a los vecinos.
         */
        @Test
        @DisplayName("marcar uno no mueve al que colgaba del pack ni al que no colgaba de nada")
        void marcar_uno_no_mueve_a_los_demas() {
            autoservicio(CAP_NUCLEO);

            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_EN_PACK.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isTrue());
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isFalse());
        }
    }

    /**
     * <b>La invariante que ninguna constraint puede declarar</b>: el gate y la
     * bandera que lo anuncia son dos sentencias nativas distintas, en dos rodajas
     * distintas, y que acepten el mismo conjunto solo se sostiene por revision. Si
     * divergen, la portada ofrece un articulo que la contratacion rechaza —o
     * esconde uno que aceptaria— y el prospecto se estrella <em>despues</em> de
     * registrarse y verificar el correo, con un error deliberadamente mudo.
     *
     * <p>
     * La comparacion se acota a los articulos con importe mensual porque el paso
     * vinculante exige precio de entrada en el ciclo pedido con un {@code JOIN}
     * interno, mientras que {@code SQL_ITEMS} lo trae por {@code LEFT JOIN} y deja
     * al consumidor leerlo del importe. Fuera de esa condicion los dos no responden
     * la misma pregunta.
     */
    @Nested
    @DisplayName("el gate y selfServiceEligible aceptan el mismo conjunto")
    class ElGateYLaProyeccionCoinciden {

        @Test
        @DisplayName("cada articulo tarifado al mes: la bandera dice lo que el gate resuelve")
        void la_bandera_dice_lo_que_el_gate_resuelve() {
            assertThat(articulosDePrueba()).filteredOn(f -> f.monthlyAmount() != null).isNotEmpty()
                    .allSatisfy(f -> assertThat(f.selfServiceEligible())
                            .as("%s: la portada y la contratacion tienen que coincidir", f.code())
                            .isEqualTo(gate
                                    .findPublishedIdByCode(f.code(), LISTA, BillingCycle.MONTHLY)
                                    .isPresent()));
        }

        @Test
        @DisplayName("y siguen coincidiendo despues de marcar una capacidad como self_service")
        void siguen_coincidiendo_con_una_capacidad_marcada() {
            autoservicio(CAP_NUCLEO);

            assertThat(gate.findPublishedIdByCode(COD_CAP_NUCLEO, LISTA, BillingCycle.MONTHLY))
                    .contains(CAP_NUCLEO);
            assertThat(articulosDePrueba()).filteredOn(f -> f.monthlyAmount() != null).isNotEmpty()
                    .allSatisfy(f -> assertThat(f.selfServiceEligible())
                            .as("%s: la portada y la contratacion tienen que coincidir", f.code())
                            .isEqualTo(gate
                                    .findPublishedIdByCode(f.code(), LISTA, BillingCycle.MONTHLY)
                                    .isPresent()));
        }

        /**
         * La marca abre una via dentro del gate, no lo esquiva: el resto de predicados
         * —estado, baja logica y precio de entrada en el ciclo pedido— siguen mandando.
         * Sin este caso, la union podria haberse escrito como un {@code OR} por encima
         * del {@code WHERE} entero y nadie lo notaria.
         */
        @Test
        @DisplayName("la marca no salta el estado, la baja logica ni el precio del ciclo")
        void la_marca_no_salta_los_demas_predicados() {
            autoservicio(MOD_BORRADOR);
            autoservicio(MOD_DE_BAJA);
            autoservicio(MOD_SOLO_MENSUAL);

            assertThat(gate.findPublishedIdByCode(COD_BORRADOR, LISTA, BillingCycle.MONTHLY))
                    .isEmpty();
            assertThat(gate.findPublishedIdByCode(COD_DE_BAJA, LISTA, BillingCycle.MONTHLY))
                    .isEmpty();
            assertThat(gate.findPublishedIdByCode(COD_SOLO_MENSUAL, LISTA, BillingCycle.ANNUAL))
                    .isEmpty();
            assertThat(gate.findPublishedIdByCode(COD_SOLO_MENSUAL, LISTA, BillingCycle.MONTHLY))
                    .contains(MOD_SOLO_MENSUAL);
        }
    }

    @Nested
    @DisplayName("paquetes y composicion")
    class Paquetes {

        @Test
        @DisplayName("del paquete sale el tramo de entrada, no la escalera por volumen")
        void del_paquete_sale_el_tramo_de_entrada() {
            assertThat(port.findPacks(LISTA)).filteredOn(p -> COD_PACK.equals(p.code()))
                    .singleElement().satisfies(p -> {
                        assertThat(p.monthlyFromAmount()).isEqualByComparingTo("89000.00");
                        assertThat(p.annualFromAmount()).isEqualByComparingTo("890000.00");
                        assertThat(p.setupAmount()).isEqualByComparingTo("150000.00");
                        assertThat(p.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
                    });
        }

        @Test
        @DisplayName("la composicion sale por rotulos: es el grafo del rechazo de cobro doble")
        void la_composicion_sale_por_rotulos() {
            assertThat(port.findPackComponents(LISTA))
                    .filteredOn(c -> COD_PACK.equals(c.packCode()))
                    .extracting(PublicCatalogPackComponentRowDto::componentCode)
                    .containsExactly(COD_MOD_EN_PACK);
        }

        @Test
        @DisplayName("sin tarifa no hay nada que publicar")
        void sin_tarifa_no_hay_nada_que_publicar() {
            assertThat(port.findContractableItems(null)).isEmpty();
            assertThat(port.findPacks(null)).isEmpty();
            assertThat(port.findPackComponents(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("area, rotulo corto y combinacion recomendada")
    class Presentacion {

        /**
         * &#9888; Los bucles leen su {@code Object[]} por posicion, asi que intercalar
         * una columna desplaza los indices posteriores sin que el compilador diga nada.
         */
        @Test
        @DisplayName("el modulo publica su area y su rotulo corto sin mover las columnas de antes")
        void el_modulo_publica_su_area_y_su_rotulo() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_EN_PACK.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.areaCode()).isEqualTo(COD_AREA_CLINICA);
                        assertThat(f.shortLabel()).isEqualTo("En pack");
                        assertThat(f.name()).isEqualTo("Modulo dentro del pack");
                        assertThat(f.monthlyAmount()).isEqualByComparingTo("38000.00");
                        assertThat(f.trialDays()).isEqualTo(30);
                        assertThat(f.selfServiceEligible()).isTrue();
                    });
        }

        @Test
        @DisplayName("un rotulo corto sin escribir llega nulo")
        void un_rotulo_corto_sin_escribir_llega_nulo() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.shortLabel()).isNull());
        }

        /** El {@code CHECK} de la tabla prohibe el area fuera de los MODULE. */
        @Test
        @DisplayName("un contador y un cargo unico llegan sin area")
        void un_contador_y_un_cargo_unico_llegan_sin_area() {
            assertThat(articulosDePrueba())
                    .filteredOn(
                            f -> COD_CAP_NUCLEO.equals(f.code()) || COD_ONE_TIME.equals(f.code()))
                    .hasSize(2).allSatisfy(f -> assertThat(f.areaCode()).isNull());
        }

        /**
         * MySQL entrega el {@code BOOLEAN} como {@code TINYINT}, o sea {@code Byte}:
         * proyectarlo a un {@code Boolean} revienta con {@code ClassCastException} y un
         * literal booleano en el {@code SELECT} depende del dialecto. Es el defecto de
         * #196 y #472, y contra un mock no se ve nada.
         */
        @Test
        @DisplayName("recommended llega desde TINYINT y se convierte, no se castea")
        void recommended_llega_desde_tinyint() {
            assertThat(port.findPacks(LISTA)).filteredOn(p -> COD_PACK.equals(p.code()))
                    .singleElement().satisfies(p -> assertThat(p.recommended()).isTrue());
        }

        @Test
        @DisplayName("las areas salen en el orden de presentacion y las apagadas no salen")
        void las_areas_salen_ordenadas_y_sin_las_apagadas() {
            List<PublicCatalogAreaRowDto> areas = port.findAreas().stream()
                    .filter(a -> a.code().startsWith("TESTCAT_")).toList();

            assertThat(areas).containsExactly(
                    new PublicCatalogAreaRowDto(COD_AREA_CLINICA, "Clinica de prueba"),
                    new PublicCatalogAreaRowDto(COD_AREA_MOSTRADOR, "Mostrador de prueba"));
            assertThat(areas).extracting(PublicCatalogAreaRowDto::code)
                    .doesNotContain(COD_AREA_APAGADA);
        }

        /**
         * El area del modulo tiene que casar con alguna de las publicadas, o la
         * cabecera bajo la que el front lo pinta no existiria.
         */
        @Test
        @DisplayName("el areaCode de un modulo casa con un area publicada")
        void el_area_del_modulo_casa_con_un_area_publicada() {
            List<String> publicadas = port.findAreas().stream().map(PublicCatalogAreaRowDto::code)
                    .toList();

            assertThat(articulosDePrueba()).filteredOn(f -> f.areaCode() != null).isNotEmpty()
                    .allSatisfy(f -> assertThat(publicadas).contains(f.areaCode()));
        }
    }

    /**
     * El grafo {@code REQUIRES} tal y como sale al mundo.
     *
     * <p>
     * <b>Es la consulta que decide que se le anade al carrito a alguien</b>: el
     * front pinta con ella la explicacion de por que aparecio una linea que el
     * cliente no pidio, y {@code SelfServeCartGuard} rechaza contra el mismo grafo.
     * Hasta aqui no la ejecutaba nadie en el build
     * —{@code ADAPTADOR_JPA_CON_RODAJA} solo alcanza a los adaptadores con
     * {@code @Query} y esta usa {@code createNativeQuery}—, asi que su unico
     * contrato era el que le fijaban los mocks del caso de uso.
     */
    @Nested
    @DisplayName("los requisitos que la portada anuncia")
    class Requisitos {

        /**
         * El orden es parte de lo publicado: {@code ORDER BY ci.sort_order, ci.id,
         * req.sort_order, req.id} existe para que el diff de un contrato no baile entre
         * peticiones. {@code CAP_NUCLEO} aparece dos veces con requisitos distintos, y
         * ahi es donde se ve el desempate por el segundo extremo.
         */
        @Test
        @DisplayName("publica cada arco REQUIRES vivo, en el orden de presentacion del catalogo")
        void publica_cada_arco_requires_vivo() {
            assertThat(requisitosDePrueba()).containsExactly(
                    new PublicCatalogRequirementRowDto(COD_MOD_EN_PACK, COD_MOD_SUELTO),
                    new PublicCatalogRequirementRowDto(COD_SOLO_MENSUAL, COD_SIN_PRECIO),
                    new PublicCatalogRequirementRowDto(COD_CAP_NUCLEO, COD_MOD_EN_PACK),
                    new PublicCatalogRequirementRowDto(COD_CAP_NUCLEO, COD_ONE_TIME));
        }

        /**
         * Cada fila es un predicado distinto del {@code WHERE}. Un arco hacia algo
         * retirado le exigiria al prospecto anadir un articulo que ya no se vende y
         * dejaria el carrito imposible de completar; un {@code RECOMMENDS} colado seria
         * vender de mas sin que nadie lo pidiera.
         */
        @ParameterizedTest(name = "{2}")
        @CsvSource({"TESTCAT_ONE_TIME, TESTCAT_SUELTO, un RECOMMENDS no arrastra nada",
                "TESTCAT_SUELTO, TESTCAT_SOLO_MENSUAL, el arco esta desactivado",
                "TESTCAT_BORRADOR, TESTCAT_SUELTO, el articulo esta en redaccion",
                "TESTCAT_SUELTO, TESTCAT_RETIRADO, el requisito esta retirado",
                "TESTCAT_DE_BAJA, TESTCAT_SUELTO, el articulo esta dado de baja",
                "TESTCAT_SUELTO, TESTCAT_DE_BAJA, el requisito esta dado de baja"})
        @DisplayName("un arco muerto no se anuncia")
        void un_arco_muerto_no_se_anuncia(String articulo, String requisito, String motivo) {
            assertThat(requisitosDePrueba()).as(motivo)
                    .doesNotContain(new PublicCatalogRequirementRowDto(articulo, requisito));
        }

        /**
         * La semilla 309 encadena {@code EXTRA_STORAGE → LAB_IMAGING →
         * CLINICAL_HISTORY} en todos los entornos. Publicar el cierre haria que
         * {@code EXTRA_STORAGE} saliera exigiendo {@code CLINICAL_HISTORY} como si
         * fuera requisito suyo —no lo es, lo es de {@code LAB_IMAGING}— y el front
         * perderia el eslabon con el que le explica al cliente la cadena entera. Sin
         * este caso, aplanar la consulta a un cierre transitivo no lo notaria nadie.
         */
        @Test
        @DisplayName("son arcos directos: EXTRA_STORAGE no exige CLINICAL_HISTORY")
        void son_arcos_directos_y_no_el_cierre_transitivo() {
            List<PublicCatalogRequirementRowDto> arcos = port.findRequirements();
            assertThat(arcos).contains(
                    new PublicCatalogRequirementRowDto(COD_ALMACENAMIENTO, COD_LABORATORIO),
                    new PublicCatalogRequirementRowDto(COD_LABORATORIO, COD_HISTORIA));
            assertThat(arcos).doesNotContain(
                    new PublicCatalogRequirementRowDto(COD_ALMACENAMIENTO, COD_HISTORIA));
        }

        /**
         * <b>Esto es intencionado y esta aqui para que no se «arregle».</b>
         * {@code catalog_item_dependencies} no tiene columna de tarifa y la consulta no
         * filtra por precio, asi que un {@code requiredItemCode} puede apuntar a un
         * articulo que no sale en ninguna de las listas publicadas. El servidor lo
         * anadiria al carrito igual —{@code RequiredItemsClosure} recorre el mismo
         * grafo—, asi que el front tiene que poder anticiparlo. Filtrarlo aqui dejaria
         * una respuesta que anuncia una cosa y cobra otra.
         */
        @Test
        @DisplayName("un requisito sin tarifar se anuncia aunque no salga en ninguna lista")
        void un_requisito_sin_tarifar_se_anuncia_igual() {
            assertThat(requisitosDePrueba())
                    .contains(new PublicCatalogRequirementRowDto(COD_SOLO_MENSUAL, COD_SIN_PRECIO));
            assertThat(articulosDePrueba()).extracting(PublicCatalogItemRowDto::code)
                    .doesNotContain(COD_SIN_PRECIO);
            assertThat(port.findPacks(LISTA)).extracting(PublicCatalogPackRowDto::code)
                    .doesNotContain(COD_SIN_PRECIO);
        }
    }

    private void tarifa(Long id, String code, LocalDate desde) {
        entityManager.createNativeQuery("""
                INSERT INTO price_lists (id, code, name, currency, valid_from, valid_to, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, :code, :code, 'COP', :desde, NULL, 'PUBLISHED', :firmadoEl,
                        :firmante, '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("desde", desde)
                .setParameter("firmadoEl", LocalDateTime.of(2026, 1, 1, 0, 0))
                .setParameter("firmante", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
    }

    private void articulo(Long id, String code, String name, String itemType, String capacityUnit,
            boolean core, int minQuantity, int sortOrder, String status, String trialEligibility,
            Integer trialDays, String trialOutcome) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                                           capacity_unit, structural_minimum, min_quantity, max_quantity,
                                                           sort_order, status, trial_eligibility,
                                                           default_trial_days, trial_outcome, service_nature,
                                                           created_date, enabled, version,
                                                           area_code, short_label, recommended)
                                VALUES (:id, :code, :name, NULL, :itemType, :capacityUnit, :core, :minQuantity,
                                        NULL, :sortOrder, :status, :elegibilidad, :dias, :desenlace,
                                        'SOFTWARE_LICENSING', '2026-01-01 00:00:00', TRUE, 0,
                                        :area, NULL, FALSE)
                                """)
                .setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("area", "MODULE".equals(itemType) ? COD_AREA_CLINICA : null)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .setParameter("core", core).setParameter("minQuantity", minQuantity)
                .setParameter("sortOrder", sortOrder).setParameter("status", status)
                .setParameter("elegibilidad", trialEligibility).setParameter("dias", trialDays)
                .setParameter("desenlace", trialOutcome).executeUpdate();
    }

    private void articuloDeBaja(Long id, String code, String name) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                                           capacity_unit, structural_minimum, min_quantity, max_quantity,
                                                           sort_order, status, trial_eligibility,
                                                           default_trial_days, trial_outcome, service_nature,
                                                           created_date, enabled, version,
                                                           area_code, short_label, recommended)
                                VALUES (:id, :code, :name, NULL, 'MODULE', NULL, FALSE, 1, NULL, 20, 'ACTIVE',
                                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                                        '2026-01-01 00:00:00', FALSE, 0,
                                        :area, NULL, FALSE)
                                """)
                .setParameter("area", COD_AREA_CLINICA).setParameter("id", id)
                .setParameter("code", code).setParameter("name", name).executeUpdate();
    }

    private void area(Long id, String code, String name, int sortOrder, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_areas (id, code, name, sort_order, created_date, enabled,
                                           version)
                VALUES (:id, :code, :name, :orden, '2026-01-01 00:00:00', :enabled, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("orden", sortOrder).setParameter("enabled", enabled).executeUpdate();
    }

    private void recomendar(Long catalogItemId) {
        entityManager
                .createNativeQuery("UPDATE catalog_items SET recommended = TRUE WHERE id = :id")
                .setParameter("id", catalogItemId).executeUpdate();
    }

    /**
     * {@code chk_catalog_items_self_service} solo la admite en {@code MODULE} y
     * {@code CAPACITY}: marcar un {@code BUNDLE} o un {@code ONE_TIME} aqui muere
     * en la base, no en la asercion.
     */
    private void autoservicio(Long catalogItemId) {
        entityManager
                .createNativeQuery("UPDATE catalog_items SET self_service = TRUE WHERE id = :id")
                .setParameter("id", catalogItemId).executeUpdate();
        entityManager.flush();
    }

    private void rotular(Long catalogItemId, String shortLabel) {
        entityManager
                .createNativeQuery("UPDATE catalog_items SET short_label = :rotulo WHERE id = :id")
                .setParameter("rotulo", shortLabel).setParameter("id", catalogItemId)
                .executeUpdate();
    }

    private void linea(Long id, Long bundleId, Long componentId, int quantity, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO bundle_components (id, bundle_item_id, component_item_id, quantity,
                                               created_date, enabled)
                VALUES (:id, :paquete, :componente, :cantidad, '2026-01-01 00:00:00', :enabled)
                """).setParameter("id", id).setParameter("paquete", bundleId)
                .setParameter("componente", componentId).setParameter("cantidad", quantity)
                .setParameter("enabled", enabled).executeUpdate();
    }

    private void requisito(Long id, Long itemId, Long requiredId, String tipo, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_item_dependencies (id, catalog_item_id, related_item_id,
                                                       relation_type, note, created_date, enabled)
                VALUES (:id, :articulo, :requisito, :tipo, NULL, '2026-01-01 00:00:00', :enabled)
                """).setParameter("id", id).setParameter("articulo", itemId)
                .setParameter("requisito", requiredId).setParameter("tipo", tipo)
                .setParameter("enabled", enabled).executeUpdate();
    }

    private void precio(Long id, Long priceListId, Long catalogItemId, String cycle, int tierMin,
            Integer tierMax, int includedQuantity, String unitAmount, String setupAmount,
            boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, :ciclo, :tierMin, :tierMax, :incluidas, :importe,
                        :implantacion, 19.00, 'TAXED', '2026-01-01 00:00:00', :enabled, 0)
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("ciclo", cycle)
                .setParameter("tierMin", tierMin).setParameter("tierMax", tierMax)
                .setParameter("incluidas", includedQuantity)
                .setParameter("importe", new BigDecimal(unitAmount))
                .setParameter("implantacion", new BigDecimal(setupAmount))
                .setParameter("enabled", enabled).executeUpdate();
    }
}
