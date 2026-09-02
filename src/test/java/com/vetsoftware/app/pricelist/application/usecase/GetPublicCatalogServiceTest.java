package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.CARGO_UNICO;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.CONTADOR;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.CORE;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.MODULO;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.PAQUETE;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.cargoUnico;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.componente;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.contador;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.modulo;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.moduloSoloMensual;
import static com.vetsoftware.app.pricelist.testsupport.PublicCatalogMother.nucleo;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.HOY;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.TARIFA_VIGENTE_ID;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.plan;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaCaducada;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaFutura;
import static com.vetsoftware.app.pricelist.testsupport.PublicPlanMother.tarifaVigente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemDto;
import com.vetsoftware.app.pricelist.application.port.out.PublicCatalogQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El catalogo contratable: <b>lo que el servidor publica y lo que no llega a
 * salir</b>, porque esta respuesta la lee cualquiera sin autenticarse.
 *
 * <p>
 * Lo que este endpoint anade sobre {@code GET /plans} es el precio de cada
 * pieza <em>por si misma</em>, que es lo que hace falta para que un cliente
 * componga lo que necesita en vez de elegir un paquete cerrado. Por eso lo que
 * se fija aqui es el reparto por naturaleza, el nulo que significa «no se vende
 * en ese ciclo», y que el nucleo salga marcado como obligatorio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPublicCatalogService — todo lo contratable, y nada mas")
class GetPublicCatalogServiceTest {

    /** Las 10:30 del 2026-08-28 en la zona del negocio. */
    private static final Clock RELOJ = Clock.fixed(
            ZonedDateTime.of(HOY, LocalTime.of(10, 30), ClockConfig.BUSINESS_ZONE).toInstant(),
            ClockConfig.BUSINESS_ZONE);

    @Mock
    private PublicPlanQueryPort priceListQueryPort;
    @Mock
    private PublicCatalogQueryPort queryPort;

    private GetPublicCatalogService servicio() {
        return new GetPublicCatalogService(priceListQueryPort, queryPort, RELOJ);
    }

    /** La tarifa vigente existe y devuelve los cuatro grupos sembrados. */
    private void elCatalogoDeLaTarifaVigente() {
        when(priceListQueryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
        when(queryPort.findContractableItems(TARIFA_VIGENTE_ID))
                .thenReturn(List.of(nucleo(), modulo(), contador(), cargoUnico()));
        when(queryPort.findPacks(TARIFA_VIGENTE_ID)).thenReturn(List.of(plan(PAQUETE)));
        when(queryPort.findPackComponents(TARIFA_VIGENTE_ID))
                .thenReturn(List.of(componente(CORE), componente(MODULO)));
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("reparte cada articulo en su grupo por el tipo, no por el nombre")
        void reparte_cada_articulo_en_su_grupo() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.modules()).extracting(PublicCatalogItemDto::code)
                    .containsExactly(CORE, MODULO);
            assertThat(catalogo.capacities()).extracting(PublicCatalogCapacityDto::code)
                    .containsExactly(CONTADOR);
            assertThat(catalogo.oneTimeItems()).extracting(PublicCatalogItemDto::code)
                    .containsExactly(CARGO_UNICO);
            assertThat(catalogo.packs()).singleElement()
                    .satisfies(pack -> assertThat(pack.code()).isEqualTo(PAQUETE));
        }

        /**
         * Es lo que {@code GET /plans} no puede dar: alli un modulo sale sin precio
         * porque el precio es el del paquete que lo contiene.
         */
        @Test
        @DisplayName("publica el precio propio del modulo en los DOS ciclos")
        void publica_el_precio_propio_del_modulo_en_los_dos_ciclos() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.modules()).filteredOn(m -> MODULO.equals(m.code())).singleElement()
                    .satisfies(m -> {
                        assertThat(m.monthlyAmount()).isEqualByComparingTo("38000.00");
                        assertThat(m.annualAmount()).isEqualByComparingTo("350000.00");
                        assertThat(m.trialDays()).isEqualTo(30);
                    });
        }

        /**
         * Las unidades incluidas son columna de la <em>fila de precio</em>, y hay una
         * por ciclo. Publicar una sola obligaria a suponer que coinciden, y nada lo
         * obliga.
         */
        @Test
        @DisplayName("el contador lleva sus unidades incluidas por ciclo, no una sola cifra")
        void el_contador_lleva_sus_unidades_incluidas_por_ciclo() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.capacities()).singleElement().satisfies(c -> {
                assertThat(c.unit()).isEqualTo("USER");
                assertThat(c.monthlyIncludedQuantity()).isEqualTo(3);
                assertThat(c.annualIncludedQuantity()).isEqualTo(5);
                assertThat(c.monthlyUnitAmount()).isEqualByComparingTo("15000.00");
                assertThat(c.annualUnitAmount()).isEqualByComparingTo("145000.00");
            });
        }

        /**
         * Los rotulos y no objetos anidados: el detalle de cada pieza ya viaja una vez
         * en {@code modules}. Es lo que permite al front avisar «no anadas esto, ya
         * viene en el paquete» antes de que el servidor lo rechace.
         */
        @Test
        @DisplayName("el paquete publica los rotulos de lo que trae dentro")
        void el_paquete_publica_los_rotulos_de_lo_que_trae_dentro() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.packs()).singleElement().satisfies(
                    pack -> assertThat(pack.componentCodes()).containsExactly(CORE, MODULO));
        }

        @Test
        @DisplayName("un paquete sin componentes sembrados sale con la lista vacia, no nula")
        void un_paquete_sin_componentes_sale_con_la_lista_vacia() {
            when(priceListQueryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            when(queryPort.findContractableItems(TARIFA_VIGENTE_ID)).thenReturn(List.of());
            when(queryPort.findPacks(TARIFA_VIGENTE_ID)).thenReturn(List.of(plan("PACK_HUERFANO")));
            when(queryPort.findPackComponents(TARIFA_VIGENTE_ID)).thenReturn(List.of());

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.packs()).singleElement()
                    .satisfies(pack -> assertThat(pack.componentCodes()).isEmpty());
        }
    }

    @Nested
    @DisplayName("El nulo es una respuesta, no un hueco")
    class PrecioAusente {

        /**
         * El gate de la contratacion exige precio de entrada <b>en el ciclo pedido</b>
         * con un {@code JOIN} interno. Publicar un cero, o extrapolar el mensual,
         * pondria en la portada un numero que la contratacion rechaza despues — que es
         * el defecto que ya costo un arreglo en {@code SQL_COMPONENTS}.
         */
        @Test
        @DisplayName("un modulo sin tarifa anual publica null, nunca un cero ni el mensual por doce")
        void un_modulo_sin_tarifa_anual_publica_null() {
            when(priceListQueryPort.findPublishedPriceLists()).thenReturn(List.of(tarifaVigente()));
            when(queryPort.findContractableItems(TARIFA_VIGENTE_ID))
                    .thenReturn(List.of(moduloSoloMensual()));
            when(queryPort.findPacks(TARIFA_VIGENTE_ID)).thenReturn(List.of());
            when(queryPort.findPackComponents(TARIFA_VIGENTE_ID)).thenReturn(List.of());

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.modules()).singleElement().satisfies(m -> {
                assertThat(m.monthlyAmount()).isEqualByComparingTo("29000.00");
                assertThat(m.annualAmount()).isNull();
            });
        }

        @Test
        @DisplayName("sin tarifa vigente devuelve el catalogo vacio y no pregunta por articulos")
        void sin_tarifa_vigente_devuelve_el_catalogo_vacio() {
            when(priceListQueryPort.findPublishedPriceLists())
                    .thenReturn(List.of(tarifaCaducada(), tarifaFutura()));

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.currency()).isNull();
            assertThat(catalogo.priceValidFrom()).isNull();
            assertThat(catalogo.modules()).isEmpty();
            assertThat(catalogo.capacities()).isEmpty();
            assertThat(catalogo.oneTimeItems()).isEmpty();
            assertThat(catalogo.packs()).isEmpty();
            verify(queryPort, never()).findContractableItems(any());
        }
    }

    @Nested
    @DisplayName("El minimo estructural")
    class NucleoObligatorio {

        /**
         * {@code mandatory} sale de {@code catalog_items.structural_minimum} y esa
         * columna no es una opinion comercial:
         * {@code PlatformCatalogTemplateJpaRepository.findInitialContractTemplate}
         * monta el contrato inicial de toda empresa con un {@code JOIN} interno sobre
         * {@code code = 'CORE' AND structural_minimum = TRUE}, y sin esa fila el alta
         * falla entera. No existe empresa sin nucleo, asi que el front no puede
         * pintarlo como una casilla que se desmarque.
         */
        @Test
        @DisplayName("el nucleo sale marcado obligatorio y el resto no")
        void el_nucleo_sale_marcado_obligatorio() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.modules()).filteredOn(m -> CORE.equals(m.code())).singleElement()
                    .satisfies(m -> assertThat(m.mandatory()).isTrue());
            assertThat(catalogo.modules()).filteredOn(m -> MODULO.equals(m.code())).singleElement()
                    .satisfies(m -> assertThat(m.mandatory()).isFalse());
        }

        /**
         * Los contadores del minimo estructural tambien lo llevan: son
         * {@code structural_minimum = TRUE} en la semilla y
         * {@code findInitialCapacityTemplates} los usa como predicado de conjunto.
         * Confundir «el articulo CORE» con «el conjunto del nucleo» es lo que hizo
         * nacer empresas sin una sola capacidad (#490).
         */
        @Test
        @DisplayName("un contador del nucleo tambien sale obligatorio")
        void un_contador_del_nucleo_tambien_sale_obligatorio() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.capacities()).singleElement()
                    .satisfies(c -> assertThat(c.mandatory()).isTrue());
        }
    }

    @Nested
    @DisplayName("Lo contratable frente a lo que solo se anuncia")
    class GateYContrato {

        /**
         * <b>La forma publicada y el gate tienen que decir lo mismo.</b> El cargo unico
         * tiene precio de lista y se publica —el cliente quiere saber cuanto cuesta la
         * migracion— pero la autocontratacion lo rechaza, porque no cuelga de ningun
         * paquete. Publicarlo sin la marca seria anunciar lo que la contratacion niega.
         *
         * <p>
         * <b>Y su precio vive en {@code setupAmount}, no en los importes por ciclo.</b>
         * {@code DATA_MIGRATION} va a {@code 0.00} mensual y anual con
         * {@code setup_amount = 450000.00} (semilla 310): sin publicar esa columna, el
         * catalogo anunciaria la migracion como gratuita. Esta asercion es la que
         * sujeta esa columna.
         */
        @Test
        @DisplayName("el cargo unico publica su precio real, que vive en setupAmount y no en el ciclo")
        void el_cargo_unico_no_es_linea_de_autoservicio() {
            elCatalogoDeLaTarifaVigente();

            PublicCatalogDto catalogo = servicio().get();

            assertThat(catalogo.oneTimeItems()).singleElement().satisfies(item -> {
                assertThat(item.setupAmount()).isEqualByComparingTo("450000.00");
                assertThat(item.selfServiceEligible()).isFalse();
            });
            assertThat(catalogo.modules())
                    .allSatisfy(m -> assertThat(m.selfServiceEligible()).isTrue());
        }
    }

    @Nested
    @DisplayName("Recorte")
    class Recorte {

        /**
         * El {@code id} de la tarifa llega hasta este servicio —lo necesita para pedir
         * las filas— y <b>muere aqui</b>. Igual que {@code validTo}: publicarlo hace
         * que quien compara espere al ultimo dia de la oferta. Por reflexion y no a
         * ojo, que es lo que se pone rojo el dia que alguien anada un campo.
         */
        @Test
        @DisplayName("no publica ningun id ni la fecha de caducidad de la tarifa")
        void no_publica_ningun_id_ni_la_fecha_de_caducidad() {
            assertThat(PublicCatalogDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("currency",
                            "priceValidFrom", "modules", "capacities", "oneTimeItems", "packs",
                            "requirements");
        }

        @Test
        @DisplayName("ninguna pieza publicada lleva id: solo rotulos")
        void ninguna_pieza_publicada_lleva_id() {
            assertThat(PublicCatalogItemDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("id");
            assertThat(PublicCatalogCapacityDto.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("id");
        }
    }
}
