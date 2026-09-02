package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * La rodaja que le faltaba a {@link JpaPublicPlanQueryPort}, y que su propio
 * javadoc reclama: sus tres consultas nativas quedan <b>fuera</b> de
 * {@code ADAPTADOR_JPA_CON_RODAJA} —esa regla solo alcanza a los
 * {@code Jpa<Algo>Repository}—, asi que hasta hoy no las ejecutaba nadie en el
 * build. Es exactamente como sobrevivio meses la incidencia #196.
 *
 * <p>
 * <b>Aqui duele mas que de costumbre.</b> Es la unica consulta publica del
 * sistema: lo que devuelva sale a internet sin sesion. Por eso esta clase no
 * comprueba «que devuelva planes» —eso pasaria igual con el SELECT equivocado—,
 * sino <b>lo que NO debe salir</b>, que es donde vive el riesgo:
 *
 * <ul>
 * <li><b>Ningun id.</b> Un id es una llave de escritura; un {@code code} es un
 * rotulo. Lo sujeta {@link Recorte}, que fija por reflexion los componentes
 * exactos de los dos records de proyeccion: el adaptador no puede publicar lo
 * que el DTO no declara, asi que anadir un campo al {@code SELECT} solo llega a
 * la respuesta si alguien anade tambien el componente — y ese dia esta clase se
 * pone roja por dos sitios (compilacion de los esperados y la lista de
 * componentes).</li>
 * <li><b>Ni {@code validTo}.</b> Con la caducidad publicada, quien compara
 * espera al ultimo dia. El puerto de tarifas si lo transporta —lo necesita
 * {@code PriceListValidity}— y donde se corta es en
 * {@code PublicPlanCatalogDto}; eso lo fija
 * {@code GetPublicPlansServiceTest}.</li>
 * <li><b>Ni la escalera de tramos.</b> Es la politica de descuento por volumen:
 * sale solo el tramo de entrada, rotulado «desde». Se siembra un segundo tramo
 * para el paquete y otro para el contador; si alguien quita {@code tier_min =
 * 1} aparecen dos filas y {@link Planes} y {@link Lineas} caen.</li>
 * <li><b>Ni articulos {@code DRAFT} / {@code DEPRECATED}, ni {@code ONE_TIME},
 * ni paquetes anidados, ni tarifas que no esten {@code PUBLISHED} y
 * habilitadas, ni {@code published_by_system_user_id}.</b> Cada exclusion tiene
 * su fila sembrada: sin ella el filtro se cumpliria por ausencia de datos y el
 * test seguiria verde el dia que se rompiera.</li>
 * </ul>
 *
 * <p>
 * <b>El adaptador no se declara como bean</b>, por lo mismo que
 * {@code QuoteCatalogQueryPortsIT}: solo necesita un {@code EntityManager} y
 * anadirlo al {@code @Import} cambiaria la clave del
 * {@code MergedContextConfiguration} y costaria un arranque de contexto entero.
 *
 * <p>
 * <b>Todo lo que se afirma va acotado a la tarifa sembrada aqui o a los codigos
 * {@code TEST_*}.</b> El contenedor MySQL es unico para la suite y Liquibase ya
 * siembra el catalogo comercial real (changesets 308-310: {@code PACK_SPA},
 * {@code PACK_CLINIC}, {@code PACK_FULL}), asi que un conteo global seria un
 * fallo intermitente esperando. En particular {@code findPlanComponents} <b>no
 * filtra los paquetes por tarifa</b> —la lista solo entra en el {@code LEFT
 * JOIN} del precio—, de modo que devuelve tambien las lineas de los paquetes
 * del catalogo real; quien las descarta es el {@code getOrDefault} del
 * servicio.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPublicPlanQueryPort — el SQL del catalogo publico contra MySQL real")
class PublicPlanQueryPortIT extends AbstractDataJpaTest {

    /** La tarifa del escenario: publicada, habilitada y sin fecha de cierre. */
    private static final Long LISTA_VIGENTE = 2600L;
    /** Publicada y habilitada, pero su ventana acabo: el SQL SI la devuelve. */
    private static final Long LISTA_CADUCADA = 2601L;
    /** En borrador: sus precios todavia se mueven. */
    private static final Long LISTA_BORRADOR = 2602L;
    /** Publicada pero dada de baja logica. */
    private static final Long LISTA_DESHABILITADA = 2603L;
    /** Retirada del catalogo. */
    private static final Long LISTA_ARCHIVADA = 2604L;

    private static final Long PACK = 2610L;
    private static final Long PACK_SOLO_ANUAL = 2611L;
    private static final Long PACK_SIN_PRECIO = 2612L;
    private static final Long PACK_BORRADOR = 2613L;
    private static final Long PACK_DE_BAJA = 2614L;

    private static final Long MOD_AGENDA = 2620L;
    private static final Long MOD_CAJA = 2621L;
    private static final Long CAP_USUARIO = 2622L;
    private static final Long MOD_BORRADOR = 2623L;
    private static final Long MOD_RETIRADO = 2624L;
    private static final Long ONE_TIME_IMPLANTACION = 2625L;
    private static final Long MOD_DE_BAJA = 2626L;
    private static final Long PACK_ANIDADO = 2627L;
    private static final Long MOD_CON_ENLACE_DE_BAJA = 2628L;

    private static final Long NEVER_FREE_CON_DIAS = 2629L;

    private static final String CODIGO_PACK = "TEST_PACK_ESENCIAL";
    private static final String CODIGO_PACK_ANUAL = "TEST_PACK_SOLO_ANUAL";

    private static final LocalDate DESDE_VIGENTE = LocalDate.of(2026, 8, 1);

    /**
     * Las tres tarifas publicadas y habilitadas que hay en el contenedor durante
     * esta rodaja: las dos de aqui y la que siembra {@link SchemaSeed}. Se nombran
     * porque el contenedor MySQL es unico para la suite y un aserto de orden sin
     * acotar dependeria de lo que hubiera sembrado cualquier otra clase.
     */
    private static final List<Long> ORDEN_ESPERADO = List.of(LISTA_VIGENTE,
            SchemaSeed.PRICE_LIST_ID, LISTA_CADUCADA);

    /** Las dos extrapolaciones que el importe anual NO puede ser. */
    private static final BigDecimal DOCE = new BigDecimal("12");
    private static final BigDecimal DIEZ = new BigDecimal("10");

    @PersistenceContext
    private EntityManager entityManager;

    private JpaPublicPlanQueryPort port;

    @BeforeEach
    void sembrarElCatalogoPublico() {
        SchemaSeed.seed(entityManager);
        port = new JpaPublicPlanQueryPort(entityManager);

        tarifa(LISTA_VIGENTE, "TEST-LISTA-VIGENTE", DESDE_VIGENTE, null, "PUBLISHED", true);
        tarifa(LISTA_CADUCADA, "TEST-LISTA-2025", LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31), "PUBLISHED", true);
        tarifa(LISTA_BORRADOR, "TEST-LISTA-BORRADOR", DESDE_VIGENTE, null, "DRAFT", true);
        tarifa(LISTA_DESHABILITADA, "TEST-LISTA-BAJA", DESDE_VIGENTE, null, "PUBLISHED", false);
        tarifa(LISTA_ARCHIVADA, "TEST-LISTA-ARCHIVADA", DESDE_VIGENTE, null, "ARCHIVED", true);

        paquete(PACK, CODIGO_PACK, "Pack esencial de laboratorio", "Para una clinica que empieza",
                5, "ACTIVE", true);
        paquete(PACK_SOLO_ANUAL, CODIGO_PACK_ANUAL, "Pack solo anual", null, 6, "ACTIVE", true);
        paquete(PACK_SIN_PRECIO, "TEST_PACK_SIN_PRECIO", "Pack sin tarifar", null, 7, "ACTIVE",
                true);
        paquete(PACK_BORRADOR, "TEST_PACK_BORRADOR", "Pack en redaccion", null, 8, "DRAFT", true);
        paquete(PACK_DE_BAJA, "TEST_PACK_DE_BAJA", "Pack dado de baja", null, 9, "ACTIVE", false);
        paquete(PACK_ANIDADO, "TEST_PACK_ANIDADO", "Pack anidado", null, 8, "ACTIVE", true);

        modulo(MOD_AGENDA, "TEST_MOD_AGENDA", "Agenda", 1, "ACTIVE", true, "ELIGIBLE", 30,
                "CONVERT_TO_PAID");
        modulo(MOD_CAJA, "TEST_MOD_CAJA", "Caja", 2, "ACTIVE", true, "NEVER_FREE", null, null);
        contador(CAP_USUARIO, "TEST_CAP_USUARIO", "Usuario adicional", 3, "ACTIVE", true);
        modulo(MOD_BORRADOR, "TEST_MOD_BORRADOR", "Modulo en redaccion", 4, "DRAFT", true,
                "NEVER_FREE", null, null);
        modulo(MOD_RETIRADO, "TEST_MOD_RETIRADO", "Modulo retirado", 5, "DEPRECATED", true,
                "NEVER_FREE", null, null);
        articuloUnico(ONE_TIME_IMPLANTACION, "TEST_ONE_TIME_IMPL", "Implantacion", 6);
        modulo(MOD_DE_BAJA, "TEST_MOD_DE_BAJA", "Modulo de baja", 7, "ACTIVE", false, "NEVER_FREE",
                null, null);
        modulo(MOD_CON_ENLACE_DE_BAJA, "TEST_MOD_ENLACE_BAJA", "Modulo desenganchado", 9, "ACTIVE",
                true, "NEVER_FREE", null, null);

        linea(2640L, PACK, MOD_AGENDA, 1, true);
        linea(2641L, PACK, MOD_CAJA, 1, true);
        linea(2642L, PACK, CAP_USUARIO, 3, true);
        linea(2643L, PACK, MOD_BORRADOR, 1, true);
        linea(2644L, PACK, MOD_RETIRADO, 1, true);
        linea(2645L, PACK, ONE_TIME_IMPLANTACION, 1, true);
        linea(2646L, PACK, MOD_DE_BAJA, 1, true);
        linea(2647L, PACK, PACK_ANIDADO, 1, true);
        linea(2648L, PACK, MOD_CON_ENLACE_DE_BAJA, 1, false);

        // El paquete: tramo de entrada, escalera por volumen y precio anual.
        precio(2660L, LISTA_VIGENTE, PACK, "MONTHLY", 1, 10, "89000.00", "150000.00", "19.00",
                "TAXED", true);
        precio(2661L, LISTA_VIGENTE, PACK, "MONTHLY", 11, null, "79000.00", "0.00", "19.00",
                "TAXED", true);
        precio(2662L, LISTA_VIGENTE, PACK, "ANNUAL", 1, null, "890000.00", "200000.00", "19.00",
                "TAXED", true);
        // Solo anual: obliga al COALESCE a caer en la segunda rama.
        precio(2663L, LISTA_VIGENTE, PACK_SOLO_ANUAL, "ANNUAL", 1, null, "990000.00", "300000.00",
                "0.00", "EXEMPT", true);
        precio(2664L, LISTA_VIGENTE, PACK_BORRADOR, "MONTHLY", 1, null, "50000.00", "0.00", "19.00",
                "TAXED", true);
        precio(2665L, LISTA_VIGENTE, PACK_DE_BAJA, "MONTHLY", 1, null, "60000.00", "0.00", "19.00",
                "TAXED", true);
        // El contador: tramo de entrada y escalera, en los DOS ciclos. El anual NO
        // es ningun multiplo del mensual: 145.000 no es 15.000 por doce (180.000) ni
        // por diez (150.000), asi que un adaptador que extrapolara en vez de leer la
        // fila ANNUAL falla aqui. Elegir 150.000 seria elegir justo el resultado de
        // la extrapolacion que este caso existe para descartar.
        precio(2666L, LISTA_VIGENTE, CAP_USUARIO, "MONTHLY", 1, 10, "15000.00", "0.00", "19.00",
                "TAXED", true);
        precio(2667L, LISTA_VIGENTE, CAP_USUARIO, "MONTHLY", 11, null, "12000.00", "0.00", "19.00",
                "TAXED", true);
        precio(2671L, LISTA_VIGENTE, CAP_USUARIO, "ANNUAL", 1, 10, "145000.00", "0.00", "19.00",
                "TAXED", true);
        precio(2672L, LISTA_VIGENTE, CAP_USUARIO, "ANNUAL", 11, null, "120000.00", "0.00", "19.00",
                "TAXED", true);
        // Caja: tiene precio suelto, pero dado de baja.
        precio(2668L, LISTA_VIGENTE, MOD_CAJA, "MONTHLY", 1, null, "25000.00", "0.00", "19.00",
                "TAXED", false);
        // El mismo contador, en OTRA tarifa: no puede filtrarse a la vigente.
        precio(2669L, LISTA_CADUCADA, CAP_USUARIO, "MONTHLY", 1, null, "99000.00", "0.00", "19.00",
                "TAXED", true);
        // Agenda solo esta tarifada en anual: el SQL de lineas solo mira MONTHLY.
        precio(2670L, LISTA_VIGENTE, MOD_AGENDA, "ANNUAL", 1, null, "300000.00", "0.00", "19.00",
                "TAXED", true);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findPublishedPriceLists — que tarifas puede llegar a ver la portada")
    class Tarifas {

        @Test
        @DisplayName("cada columna cae en su campo: id, moneda y las DOS fechas de la ventana")
        void cada_columna_cae_en_su_campo() {
            assertThat(port.findPublishedPriceLists())
                    .filteredOn(lista -> LISTA_VIGENTE.equals(lista.id())).containsExactly(
                            new PublicPriceListDto(LISTA_VIGENTE, "COP", DESDE_VIGENTE, null));
        }

        @Test
        @DisplayName("una tarifa con cierre trae validTo con valor, no un nulo")
        void una_tarifa_con_cierre_trae_valid_to() {
            assertThat(port.findPublishedPriceLists())
                    .filteredOn(lista -> LISTA_CADUCADA.equals(lista.id()))
                    .containsExactly(new PublicPriceListDto(LISTA_CADUCADA, "COP",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        }

        /**
         * El chivato de D-81. Si alguien mete un {@code CURRENT_DATE} en el SQL «para
         * ahorrar una vuelta», esta tarifa deja de salir y este caso cae — que es lo
         * que hay que evitar: entre las 19:00 y la medianoche el motor ya contesta
         * manana y la portada se quedaria sin precios el ultimo dia de la tarifa.
         */
        @Test
        @DisplayName("la vigencia por fecha NO se filtra en el SQL: la caducada tambien sale")
        void la_vigencia_por_fecha_no_se_filtra_en_el_sql() {
            assertThat(port.findPublishedPriceLists()).extracting(PublicPriceListDto::id)
                    .contains(LISTA_CADUCADA);
        }

        @Test
        @DisplayName("ni un borrador, ni una archivada, ni una dada de baja llegan a la portada")
        void solo_las_publicadas_y_habilitadas() {
            assertThat(port.findPublishedPriceLists()).extracting(PublicPriceListDto::id)
                    .doesNotContain(LISTA_BORRADOR, LISTA_ARCHIVADA, LISTA_DESHABILITADA);
        }

        @Test
        @DisplayName("ordena por valid_from descendente: la ultima publicada manda")
        void ordena_por_valid_from_descendente() {
            assertThat(port.findPublishedPriceLists())
                    .filteredOn(lista -> ORDEN_ESPERADO.contains(lista.id()))
                    .extracting(PublicPriceListDto::id)
                    .containsExactly(LISTA_VIGENTE, SchemaSeed.PRICE_LIST_ID, LISTA_CADUCADA);
        }
    }

    @Nested
    @DisplayName("findPlans — los paquetes vendibles, con el precio DE ENTRADA")
    class Planes {

        @Test
        @DisplayName("cada columna cae en su campo, y el importe es el del tramo de entrada")
        void cada_columna_cae_en_su_campo() {
            assertThat(port.findPlans(LISTA_VIGENTE)).first().usingRecursiveComparison()
                    .withEqualsForType((a, b) -> a.compareTo(b) == 0, BigDecimal.class)
                    .isEqualTo(new PublicPlanRowDto(CODIGO_PACK, "Pack esencial de laboratorio",
                            "Para una clinica que empieza", new BigDecimal("89000.00"),
                            new BigDecimal("890000.00"), new BigDecimal("150000.00"),
                            new BigDecimal("19.00"), TaxTreatment.TAXED));
        }

        /**
         * La escalera de tramos es la politica de descuento por volumen y publicarla
         * entera es publicarla. El paquete tiene DOS tramos sembrados; si alguien quita
         * {@code tier_min = 1} salen dos filas del mismo plan y esto cae.
         */
        @Test
        @DisplayName("la escalera de tramos no se publica: un solo renglon por plan, el de entrada")
        void la_escalera_de_tramos_no_se_publica() {
            assertThat(port.findPlans(LISTA_VIGENTE))
                    .filteredOn(plan -> CODIGO_PACK.equals(plan.code())).singleElement()
                    .extracting(PublicPlanRowDto::monthlyFromAmount)
                    .isEqualTo(new BigDecimal("89000.00"));
        }

        @Test
        @DisplayName("un paquete tarifado solo en anual sale, con el mensual nulo y el COALESCE"
                + " cayendo al anual")
        void un_paquete_solo_anual_sale_con_el_mensual_nulo() {
            assertThat(port.findPlans(LISTA_VIGENTE))
                    .filteredOn(plan -> CODIGO_PACK_ANUAL.equals(plan.code())).singleElement()
                    .usingRecursiveComparison()
                    .withEqualsForType((a, b) -> a.compareTo(b) == 0, BigDecimal.class)
                    .isEqualTo(new PublicPlanRowDto(CODIGO_PACK_ANUAL, "Pack solo anual", null,
                            null, new BigDecimal("990000.00"), new BigDecimal("300000.00"),
                            new BigDecimal("0.00"), TaxTreatment.EXEMPT));
        }

        /**
         * Acotado a la tarifa sembrada aqui, que es una lista propia: ningun changeset
         * pone precios en ella, asi que el conteo exacto es estable pese al catalogo
         * comercial que Liquibase ya siembra en el contenedor.
         */
        @Test
        @DisplayName("sin precio en esa tarifa no hay plan; un DRAFT o un dado de baja tampoco")
        void solo_los_paquetes_activos_con_precio_en_esa_tarifa() {
            assertThat(port.findPlans(LISTA_VIGENTE)).extracting(PublicPlanRowDto::code)
                    .containsExactly(CODIGO_PACK, CODIGO_PACK_ANUAL);
        }

        @Test
        @DisplayName("los precios de otra tarifa no se cuelan: la caducada no publica ningun plan")
        void los_precios_de_otra_tarifa_no_se_cuelan() {
            assertThat(port.findPlans(LISTA_CADUCADA)).isEmpty();
        }

        /**
         * El caso vacio es correcto y no un fallo: la portada tiene que seguir
         * cargando.
         */
        @Test
        @DisplayName("sin tarifa —priceListId nulo— devuelve lista vacia, no un fallo")
        void sin_tarifa_devuelve_lista_vacia() {
            assertThat(port.findPlans(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPlanComponents — que trae el paquete, linea a linea")
    class Lineas {

        @Test
        @DisplayName("publica el modulo con su prueba, el modulo sin ella y el contador con su"
                + " unidad, en el orden del catalogo")
        void publica_modulos_y_contadores_en_orden() {
            assertThat(port.findPlanComponents(LISTA_VIGENTE))
                    .filteredOn(linea -> CODIGO_PACK.equals(linea.planCode()))
                    .usingRecursiveComparison()
                    .withEqualsForType((a, b) -> a.compareTo(b) == 0, BigDecimal.class)
                    .isEqualTo(List.of(
                            // Tarifado solo en anual: el mensual viene nulo y el anual con su
                            // importe. Antes los dos JOIN eran uno solo clavado en MONTHLY y
                            // esta linea salia sin ningun precio.
                            new PublicPlanComponentRowDto(CODIGO_PACK, "TEST_MOD_AGENDA", "Agenda",
                                    null, 1, 30, null, new BigDecimal("300000.00")),
                            // Su unico precio suelto esta dado de baja: nulo en los dos.
                            new PublicPlanComponentRowDto(CODIGO_PACK, "TEST_MOD_CAJA", "Caja",
                                    null, 1, null, null, null),
                            new PublicPlanComponentRowDto(CODIGO_PACK, "TEST_CAP_USUARIO",
                                    "Usuario adicional", "USER", 3, null,
                                    new BigDecimal("15000.00"), new BigDecimal("145000.00"))));
        }

        /**
         * El mismo recorte que en el plan, un nivel mas abajo: el contador tiene dos
         * tramos y solo sale el de entrada. Quitar {@code tier_min = 1} duplica la
         * linea y el caso de arriba cae; este lo nombra.
         */
        @Test
        @DisplayName("del contador sale el precio del tramo de entrada, no el de volumen, en los"
                + " dos ciclos")
        void del_contador_sale_el_tramo_de_entrada() {
            assertThat(contadorDelPack())
                    .extracting(PublicPlanComponentRowDto::monthlyExtraUnitAmount,
                            PublicPlanComponentRowDto::annualExtraUnitAmount)
                    .containsExactly(new BigDecimal("15000.00"), new BigDecimal("145000.00"));
        }

        /**
         * <b>El importe anual se LEE de la fila {@code ANNUAL}; no se calcula.</b> Es
         * el defecto entero de esta consulta: con un solo {@code LEFT JOIN} clavado en
         * {@code MONTHLY}, el unico precio publicado era el mensual y quien pintaba un
         * plan anual no tenia mas remedio que extrapolarlo —el front lo multiplica por
         * diez—, mientras {@code CreateQuoteService} cotiza contra la escalera
         * {@code ANNUAL} del articulo. La fixture usa 145.000 justamente porque no es
         * 15.000 por doce (180.000) ni por diez (150.000): cualquiera de las dos
         * extrapolaciones falla. La primera version de este caso usaba 150.000 y el
         * propio aserto la tumbo, que es exactamente para lo que esta.
         */
        @Test
        @DisplayName("el importe anual sale de su propia fila, no de multiplicar el mensual")
        void el_importe_anual_sale_de_su_propia_fila() {
            PublicPlanComponentRowDto contador = contadorDelPack();

            assertThat(contador.annualExtraUnitAmount()).isEqualByComparingTo("145000.00");
            assertThat(contador.annualExtraUnitAmount())
                    .isNotEqualByComparingTo(contador.monthlyExtraUnitAmount().multiply(DOCE));
            assertThat(contador.annualExtraUnitAmount())
                    .isNotEqualByComparingTo(contador.monthlyExtraUnitAmount().multiply(DIEZ));
        }

        private PublicPlanComponentRowDto contadorDelPack() {
            return port.findPlanComponents(LISTA_VIGENTE).stream()
                    .filter(linea -> "TEST_CAP_USUARIO".equals(linea.code())
                            && CODIGO_PACK.equals(linea.planCode()))
                    .findFirst().orElseThrow();
        }

        @Test
        @DisplayName("ni un DRAFT, ni un DEPRECATED, ni un ONE_TIME, ni un paquete anidado, ni un"
                + " articulo de baja, ni un enlace desactivado se anuncian")
        void lo_que_no_se_anuncia_en_una_landing() {
            assertThat(port.findPlanComponents(LISTA_VIGENTE))
                    .filteredOn(linea -> CODIGO_PACK.equals(linea.planCode()))
                    .extracting(PublicPlanComponentRowDto::code).doesNotContain("TEST_MOD_BORRADOR",
                            "TEST_MOD_RETIRADO", "TEST_ONE_TIME_IMPL", "TEST_PACK_ANIDADO",
                            "TEST_MOD_DE_BAJA", "TEST_MOD_ENLACE_BAJA");
        }

        @Test
        @DisplayName("el precio suelto de otra tarifa no se cuela por ninguno de los dos LEFT JOIN")
        void el_precio_suelto_de_otra_tarifa_no_se_cuela() {
            assertThat(port.findPlanComponents(LISTA_BORRADOR))
                    .filteredOn(linea -> "TEST_CAP_USUARIO".equals(linea.code())
                            && CODIGO_PACK.equals(linea.planCode()))
                    .singleElement()
                    .extracting(PublicPlanComponentRowDto::monthlyExtraUnitAmount,
                            PublicPlanComponentRowDto::annualExtraUnitAmount)
                    .containsExactly(null, null);
        }

        @Test
        @DisplayName("capacityUnit es el discriminante: solo el contador lo trae")
        void capacity_unit_es_el_discriminante() {
            assertThat(port.findPlanComponents(LISTA_VIGENTE))
                    .filteredOn(linea -> CODIGO_PACK.equals(linea.planCode()))
                    .filteredOn(PublicPlanComponentRowDto::esCapacidad)
                    .extracting(PublicPlanComponentRowDto::code)
                    .containsExactly("TEST_CAP_USUARIO");
        }

        @Test
        @DisplayName("sin tarifa —priceListId nulo— devuelve lista vacia, no un fallo")
        void sin_tarifa_devuelve_lista_vacia() {
            assertThat(port.findPlanComponents(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("La politica de prueba: el CASE, no la columna a pelo (#196)")
    class PoliticaDePrueba {

        @Test
        @DisplayName("un articulo ELIGIBLE publica sus dias de prueba")
        void un_articulo_elegible_publica_sus_dias() {
            assertThat(port.findPlanComponents(LISTA_VIGENTE))
                    .filteredOn(linea -> "TEST_MOD_AGENDA".equals(linea.code())
                            && CODIGO_PACK.equals(linea.planCode()))
                    .singleElement().extracting(PublicPlanComponentRowDto::trialDays).isEqualTo(30);
        }

        @Test
        @DisplayName("un articulo NEVER_FREE publica un nulo, que significa «no hay prueba»")
        void un_articulo_never_free_publica_un_nulo() {
            assertThat(port.findPlanComponents(LISTA_VIGENTE))
                    .filteredOn(linea -> "TEST_MOD_CAJA".equals(linea.code())
                            && CODIGO_PACK.equals(linea.planCode()))
                    .singleElement().extracting(PublicPlanComponentRowDto::trialDays).isNull();
        }

        /**
         * <b>El caso que hay que sujetar y que hoy NO se puede sembrar.</b> El
         * {@code CASE WHEN trial_eligibility = 'ELIGIBLE' THEN default_trial_days END}
         * existe para que la landing no prometa una prueba que nadie concedio el dia
         * que la columna lleve dias sin elegibilidad. Ese estado es imposible mientras
         * {@code chk_catalog_items_trial_policy} lo prohiba —es un arco exclusivo—, asi
         * que un fixture {@code NEVER_FREE} con dias en la columna <b>no entra en la
         * base</b>: MySQL lo rechaza, y desactivar el CHECK exigiria un {@code ALTER
         * TABLE} cuyo commit implicito rompe el rollback de la rodaja y ensucia el
         * contenedor compartido para el resto de la suite.
         *
         * <p>
         * Lo que si se puede sujetar es la <b>otra mitad del argumento</b>: que la
         * unica razon por la que leer la columna a pelo seria equivalente al
         * {@code CASE} sigue en pie. Este caso se pone rojo <b>el mismo dia</b> en que
         * alguien relaje el CHECK — que es exactamente el dia en que el {@code CASE}
         * pasa de cinturon a unica defensa — y obliga a mirar esta clase antes de tocar
         * el SQL.
         */
        @Test
        @DisplayName("el esquema sigue prohibiendo un NEVER_FREE con dias en la columna: el dia"
                + " que deje de hacerlo, el CASE es la unica defensa")
        void el_esquema_sigue_prohibiendo_un_never_free_con_dias() {
            assertThatThrownBy(() -> modulo(NEVER_FREE_CON_DIAS, "TEST_MOD_REGALADO",
                    "Modulo con dias sin elegibilidad", 10, "ACTIVE", true, "NEVER_FREE", 30, null))
                    .hasStackTraceContaining("chk_catalog_items_trial_policy");
        }
    }

    @Nested
    @DisplayName("El recorte del contrato publico")
    class Recorte {

        /**
         * El adaptador no puede publicar lo que el record no declara: el {@code SELECT}
         * se lee por posicion y cada columna se copia a un componente. Si manana
         * alguien anade un campo a la proyeccion, este caso lo nombra —y los esperados
         * de {@link Planes} dejan de compilar—, que es lo que impide que un campo nuevo
         * del lado de administracion se cuele en la respuesta anonima.
         */
        @Test
        @DisplayName("el plan proyecta ocho campos y ninguno es un id")
        void el_plan_proyecta_ocho_campos_y_ninguno_es_un_id() {
            assertThat(PublicPlanRowDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("code", "name", "tagline",
                            "monthlyFromAmount", "annualFromAmount", "setupAmount", "taxRate",
                            "taxTreatment");
        }

        @Test
        @DisplayName("la linea proyecta ocho campos —un importe por ciclo—: ni id, ni tierMax, ni"
                + " el resto de la escalera")
        void la_linea_proyecta_ocho_campos() {
            assertThat(PublicPlanComponentRowDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("planCode", "code",
                            "name", "capacityUnit", "includedQuantity", "trialDays",
                            "monthlyExtraUnitAmount", "annualExtraUnitAmount");
        }

        /**
         * El id y {@code validTo} de la tarifa llegan hasta aqui a proposito —el
         * servicio los necesita para pedir las filas y para decidir la vigencia con
         * {@code PriceListValidity}— y se cortan un piso mas arriba. Lo que NO llega
         * nunca es la firma: {@code published_at} y {@code published_by_system_user_id}
         * no estan en el {@code SELECT} ni en el record.
         */
        @Test
        @DisplayName("la tarifa no proyecta quien la publico ni cuando")
        void la_tarifa_no_proyecta_la_firma() {
            assertThat(PublicPriceListDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("id", "currency", "validFrom", "validTo");
        }
    }

    /**
     * {@code chk_price_lists_published} es un arco exclusivo: un borrador no puede
     * llevar firma y todo lo demas la exige. Los dos valores se derivan del estado
     * aqui, en el andamio, en vez de dejarlos a un {@code CASE} del motor: la
     * sentencia queda con un solo tipo por parametro y el fallo, si lo hay, habla
     * del CHECK y no del driver.
     */
    private void tarifa(Long id, String code, LocalDate desde, LocalDate hasta, String status,
            boolean enabled) {
        boolean borrador = "DRAFT".equals(status);
        entityManager.createNativeQuery("""
                INSERT INTO price_lists (id, code, name, currency, valid_from, valid_to, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, :code, :code, 'COP', :desde, :hasta, :status, :firmadoEl, :firmante,
                        '2026-01-01 00:00:00', :enabled, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("desde", desde)
                .setParameter("hasta", hasta).setParameter("status", status)
                .setParameter("firmadoEl", borrador ? null : LocalDateTime.of(2026, 1, 1, 0, 0))
                .setParameter("firmante", borrador ? null : SchemaSeed.SYSTEM_USER_ID)
                .setParameter("enabled", enabled).executeUpdate();
    }

    private void paquete(Long id, String code, String name, String shortDescription, int sortOrder,
            String status, boolean enabled) {
        articulo(id, code, name, shortDescription, "BUNDLE", null, sortOrder, status, enabled,
                "NEVER_FREE", null, null);
    }

    private void modulo(Long id, String code, String name, int sortOrder, String status,
            boolean enabled, String trialEligibility, Integer trialDays, String trialOutcome) {
        articulo(id, code, name, null, "MODULE", null, sortOrder, status, enabled, trialEligibility,
                trialDays, trialOutcome);
    }

    private void contador(Long id, String code, String name, int sortOrder, String status,
            boolean enabled) {
        articulo(id, code, name, null, "CAPACITY", "USER", sortOrder, status, enabled, "NEVER_FREE",
                null, null);
    }

    private void articuloUnico(Long id, String code, String name, int sortOrder) {
        articulo(id, code, name, null, "ONE_TIME", null, sortOrder, "ACTIVE", true, "NEVER_FREE",
                null, null);
    }

    private void articulo(Long id, String code, String name, String shortDescription,
            String itemType, String capacityUnit, int sortOrder, String status, boolean enabled,
            String trialEligibility, Integer trialDays, String trialOutcome) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                                           capacity_unit, structural_minimum, min_quantity, max_quantity,
                                                           sort_order, status, trial_eligibility,
                                                           default_trial_days, trial_outcome, service_nature,
                                                           created_date, enabled, version)
                                VALUES (:id, :code, :name, :descripcion, :itemType, :capacityUnit, false, 1, NULL,
                                        :sortOrder, :status, :elegibilidad, :dias, :desenlace,
                                        'SOFTWARE_LICENSING', '2026-01-01 00:00:00', :enabled, 0)
                                """)
                .setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("descripcion", shortDescription).setParameter("itemType", itemType)
                .setParameter("capacityUnit", capacityUnit).setParameter("sortOrder", sortOrder)
                .setParameter("status", status).setParameter("elegibilidad", trialEligibility)
                .setParameter("dias", trialDays).setParameter("desenlace", trialOutcome)
                .setParameter("enabled", enabled).executeUpdate();
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

    private void precio(Long id, Long priceListId, Long catalogItemId, String cycle, int tierMin,
            Integer tierMax, String unitAmount, String setupAmount, String taxRate,
            String taxTreatment, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, :ciclo, :tierMin, :tierMax, 0, :importe,
                        :implantacion, :tasa, :tratamiento, '2026-01-01 00:00:00', :enabled, 0)
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("ciclo", cycle)
                .setParameter("tierMin", tierMin).setParameter("tierMax", tierMax)
                .setParameter("importe", new BigDecimal(unitAmount))
                .setParameter("implantacion", new BigDecimal(setupAmount))
                .setParameter("tasa", new BigDecimal(taxRate))
                .setParameter("tratamiento", taxTreatment).setParameter("enabled", enabled)
                .executeUpdate();
    }
}
