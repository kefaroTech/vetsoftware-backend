package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La comparacion de paquete: el cambio de decision de S1.5.
 *
 * <p>
 * La v1 sustituia el carrito por el paquete cuando salia mas barato. Con los
 * numeros reales eso ahorraba 35.000 al mes y le quitaba al cliente ~164.500
 * del primer mes, mientras la landing prometia "prueba gratis, sin tarjeta". Lo
 * que se comprueba aqui es que el paquete se <em>ofrece</em> con sus dos
 * dimensiones y que el carrito no se toca nunca.
 */
@DisplayName("PackComparison — compara, no sustituye")
class PackComparisonTest {

    private static final SellableCatalog CATALOGO = SellableCatalogMother.completo();

    private static final String MOTIVO = "Porque lo dijiste tu.";

    private static CartResult carrito(SellableCatalog catalogo, String... necesarios) {
        List<String> codigos = List.of(necesarios);
        Map<String, String> motivos = Stream.of(necesarios).distinct()
                .collect(Collectors.toMap(c -> c, c -> MOTIVO));
        return ProposalCart.build(codigos, List.of(), motivos, catalogo);
    }

    @Nested
    @DisplayName("Cuando hay oferta")
    class CuandoHayOferta {

        /**
         * <b>El ejemplo literal del plan, con los numeros de la semilla.</b> Los cinco
         * modulos sueltos suman 224.000 y el {@code PACK_CLINIC} cuesta 189.000: la
         * oferta ahorra 35.000 al mes <em>y</em> cuesta 30 dias de prueba, porque los
         * tres paquetes son {@code NEVER_FREE}. Una oferta que solo dijera el precio
         * esconderia justo la mitad que le cuesta dinero al cliente.
         */
        @Test
        @DisplayName("ofrece el paquete contenido y mas barato, con las dos dimensiones")
        void ofrece_el_paquete_con_precio_y_prueba() {
            CartResult cesta = carrito(CATALOGO, "CLINICAL_HISTORY", "VACCINATION",
                    "CASH_REGISTER");

            Optional<PackComparisonResult> oferta = PackComparison.mejorOferta(cesta, CATALOGO);

            assertThat(oferta).get().satisfies(o -> {
                assertThat(o.packCode()).isEqualTo("PACK_CLINIC");
                assertThat(o.sumaSuelta()).isEqualByComparingTo("224000.00");
                assertThat(o.packAmount()).isEqualByComparingTo("189000.00");
                assertThat(o.ahorroMensual()).isEqualByComparingTo("35000.00");
                assertThat(o.diasDePruebaPerdidos()).isEqualTo(30);
                assertThat(o.sinCosteEnPrueba()).isFalse();
                assertThat(o.currency()).isEqualTo("COP");
                assertThat(o.modulosQuePierdenPrueba()).contains("Caja y punto de venta",
                        "Historia clinica y consultas");
            });
        }

        /**
         * <b>Compara, no sustituye.</b> El carrito por defecto siguen siendo los
         * modulos sueltos -los que conservan la prueba-; el paquete es una oferta que
         * el cliente acepta con un clic. Si esta afirmacion dejara de ser cierta, el
         * carrito de abajo cambiaria.
         */
        @Test
        @DisplayName("comparar no toca el carrito ni su total")
        void comparar_no_modifica_el_carrito() {
            CartResult cesta = carrito(CATALOGO, "CLINICAL_HISTORY", "VACCINATION",
                    "CASH_REGISTER");
            List<String> antes = cesta.aceptadas().stream().map(CartLine::code).toList();

            PackComparison.mejorOferta(cesta, CATALOGO);

            assertThat(cesta.aceptadas().stream().map(CartLine::code).toList()).isEqualTo(antes);
            assertThat(cesta.subtotal()).isEqualByComparingTo("224000.00");
        }
    }

    @Nested
    @DisplayName("Cuando no hay oferta")
    class CuandoNoHayOferta {

        /**
         * <b>El caso que la v1 ofrecia y no debia:</b> 155.000 = 155.000. La
         * comparacion es estricta, asi que empatar no es una oferta -cambiar de
         * producto para pagar lo mismo y perder la prueba es una perdida neta-.
         */
        @Test
        @DisplayName("no ofrece el paquete que cuesta exactamente lo mismo")
        void el_empate_no_es_una_oferta() {
            SellableCatalog soloEmpate = new SellableCatalog(CATALOGO.items(), CATALOGO.requires(),
                    List.of(SellableCatalogMother.packAlMismoPrecio()));
            CartResult cesta = carrito(soloEmpate, "CLINICAL_HISTORY", "VACCINATION",
                    "CASH_REGISTER");

            assertThat(PackComparison.mejorOferta(cesta, soloEmpate)).isEmpty();
        }

        /**
         * <b>La contencion se evalua sobre los modulos del paquete, y tienen que estar
         * TODOS.</b> Sin {@code CASH_REGISTER} en el carrito, el {@code PACK_CLINIC} no
         * esta contenido: ofrecerlo seria venderle al cliente algo que no pidio.
         */
        @Test
        @DisplayName("no ofrece el paquete que el carrito no contiene entero")
        void el_paquete_no_contenido_no_se_ofrece() {
            CartResult cesta = carrito(CATALOGO, "CLINICAL_HISTORY", "VACCINATION");

            assertThat(PackComparison.mejorOferta(cesta, CATALOGO)).isEmpty();
        }

        /**
         * <b>La correccion que convirtio esta funcion en codigo vivo.</b>
         * {@code CAPACITY_TERMINAL} es componente de los tres paquetes y no entra al
         * carrito por si solo, asi que evaluar la contencion sobre <em>todos</em> los
         * componentes hacia que no se cumpliera nunca: la funcion estrella de la v1 era
         * codigo muerto. Aqui el carrito lleva la caja -y por tanto su terminal-, y la
         * oferta aparece porque las capacidades no participan en la contencion.
         */
        @Test
        @DisplayName("las capacidades del carrito no cuentan en la contencion ni inflan el ahorro")
        void las_capacidades_no_participan() {
            CartResult cesta = carrito(CATALOGO, "CLINICAL_HISTORY", "VACCINATION",
                    "CASH_REGISTER");

            assertThat(cesta.aceptadas()).extracting(CartLine::code).contains("CAPACITY_TERMINAL");
            assertThat(PackComparison.mejorOferta(cesta, CATALOGO)).get()
                    .satisfies(o -> assertThat(o.sumaSuelta()).isEqualByComparingTo("224000.00"));
        }

        /**
         * <b>El otro lado del mismo error.</b> La contencion de un conjunto vacio es
         * cierta por vacuidad: sin la guarda, un paquete que solo trae capacidades se
         * ofreceria con cualquier carrito, incluso con el minimo.
         */
        @Test
        @DisplayName("un paquete sin modulos no se ofrece con ningun carrito")
        void el_paquete_sin_modulos_nunca_se_ofrece() {
            SellableCatalog soloCapacidades = new SellableCatalog(CATALOGO.items(),
                    CATALOGO.requires(), List.of(SellableCatalogMother.packSinModulos()));

            assertThat(PackComparison.mejorOferta(carrito(soloCapacidades, "CLINICAL_HISTORY"),
                    soloCapacidades)).isEmpty();
        }

        @Test
        @DisplayName("sin paquetes en el catalogo no hay nada que ofrecer")
        void sin_paquetes_no_hay_oferta() {
            SellableCatalog sinPaquetes = SellableCatalogMother.sinPaquetes();

            assertThat(PackComparison.mejorOferta(carrito(sinPaquetes, "CLINICAL_HISTORY"),
                    sinPaquetes)).isEmpty();
        }

        @Test
        @DisplayName("sin carrito ni catalogo no explota, devuelve vacio")
        void los_nulos_devuelven_vacio() {
            assertThat(PackComparison.mejorOferta(null, CATALOGO)).isEmpty();
            assertThat(PackComparison.mejorOferta(carrito(CATALOGO, "CLINICAL_HISTORY"), null))
                    .isEmpty();
        }
    }
}
