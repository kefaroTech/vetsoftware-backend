package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Nadie paga dos veces por la misma funcionalidad.</b>
 *
 * <p>
 * El motor de precios no expande paquetes: {@code CreateQuoteService} pone
 * precio a cada linea que recibe, una por una. Eso deja dos de los tres casos
 * correctos <em>por construccion</em> —un modulo suelto se cobra una vez, un
 * paquete se cobra una vez y sus piezas no se cotizan— y deja abierto el
 * tercero, que es el que esta clase cierra: una cesta con el paquete <em>y</em>
 * una pieza suya produce dos cobros por lo mismo.
 *
 * <p>
 * <b>Por que ahora y no antes.</b> Hasta hoy no pasaba porque el front no
 * mandaba lineas de modulo — la proteccion era una convencion del llamador, que
 * no es una proteccion. Desde que {@code GET /catalog} publica el precio de
 * cada modulo suelto, componer esa cesta a mano es trivial y el gate tiene que
 * estar en el servidor.
 *
 * <p>
 * La otra mitad del argumento —que un paquete no arrastre sus componentes al
 * congelar las lineas— se prueba contra el motor de precios en
 * {@link CreateQuoteBundlePricingTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelfServeQuoteService — un paquete y una pieza suya no se cobran juntos")
class SelfServeQuoteDoubleChargeTest {

    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    private static final Long TARIFA = 70L;

    private static final String PAQUETE = "PACK_CLINIC";
    private static final String PIEZA_DEL_PAQUETE = "SCHEDULING";
    private static final String MODULO_SUELTO = "SURGERY";

    @Mock
    private PlatformQuoteIssuerPort issuer;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;
    @Captor
    private ArgumentCaptor<CreateQuoteCommand> emitido;

    private SelfServeQuoteService servicio() {
        return new SelfServeQuoteService(issuer, priceListQueryPort, publishedCatalogItemQueryPort,
                RELOJ);
    }

    private static PriceListRef vigente() {
        return new PriceListRef(TARIFA, "LISTA-2026-08", "COP", LocalDate.of(2026, 8, 1), null);
    }

    private static SelfServeQuoteCommand comando(String... codigos) {
        return new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), "MONTHLY",
                List.of(codigos).stream().map(c -> new SelfServeQuoteLineCommand(c, 1)).toList());
    }

    private void hayTarifaVigente() {
        when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
    }

    private void elIssuerEmite() {
        when(issuer.issue(any()))
                .thenReturn(QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT)));
    }

    private void elCatalogoResuelve(String codigo, Long id) {
        when(publishedCatalogItemQueryPort.findPublishedIdByCode(codigo, TARIFA,
                BillingCycle.MONTHLY)).thenReturn(Optional.of(id));
    }

    @Nested
    @DisplayName("Comprar suelto se cobra una vez")
    class CompraSuelta {

        /**
         * Primera direccion del problema. {@code SURGERY} no es un paquete, asi que el
         * grafo de componentes no devuelve nada y la cesta pasa: una linea, un cobro.
         */
        @Test
        @DisplayName("un modulo suelto produce exactamente una linea")
        void un_modulo_suelto_produce_exactamente_una_linea() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of());
            elCatalogoResuelve(MODULO_SUELTO, 31L);
            elIssuerEmite();

            servicio().execute(comando(MODULO_SUELTO));

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                    .containsExactly(31L);
        }

        /**
         * Segunda direccion. Pedir el paquete no arrastra ninguna de sus piezas a la
         * cesta: se emite <b>una</b> linea, la del paquete, y sus componentes no se
         * traducen ni se cotizan. Lo que el paquete trae dentro lo cobra su propio
         * precio.
         */
        @Test
        @DisplayName("un paquete produce una sola linea: sus componentes no se cotizan aparte")
        void un_paquete_produce_una_sola_linea() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of(PIEZA_DEL_PAQUETE, "CORE", "CASH_REGISTER"));
            elCatalogoResuelve(PAQUETE, 90L);
            elIssuerEmite();

            servicio().execute(comando(PAQUETE));

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                    .containsExactly(90L);
            verify(publishedCatalogItemQueryPort, never()).findPublishedIdByCode(PIEZA_DEL_PAQUETE,
                    TARIFA, BillingCycle.MONTHLY);
        }

        /**
         * El paquete <b>si</b> se combina con lo que no trae dentro: esa es la venta
         * cruzada legitima y el rechazo tiene que ser quirurgico, no una prohibicion de
         * mezclar paquete y modulo.
         */
        @Test
        @DisplayName("un paquete mas un modulo que NO incluye se acepta: son dos cosas distintas")
        void un_paquete_mas_un_modulo_ajeno_se_acepta() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of(PIEZA_DEL_PAQUETE, "CORE"));
            elCatalogoResuelve(PAQUETE, 90L);
            elCatalogoResuelve(MODULO_SUELTO, 31L);
            elIssuerEmite();

            servicio().execute(comando(PAQUETE, MODULO_SUELTO));

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                    .containsExactly(90L, 31L);
        }
    }

    @Nested
    @DisplayName("El cobro doble se rechaza")
    class CobroDoble {

        /**
         * El caso que el front evitaba por convencion. {@code SCHEDULING} ya viene
         * dentro de {@code PACK_CLINIC}: cotizarlo ademas es cobrar dos veces la misma
         * agenda.
         */
        @Test
        @DisplayName("un paquete junto a una pieza suya se rechaza y no se emite nada")
        void un_paquete_junto_a_una_pieza_suya_se_rechaza() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of(PIEZA_DEL_PAQUETE, "CORE"));

            assertThatThrownBy(() -> servicio().execute(comando(PAQUETE, PIEZA_DEL_PAQUETE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(PIEZA_DEL_PAQUETE)
                    .hasMessageContaining("already included in a bundle");

            verifyNoInteractions(issuer);
        }

        /**
         * Se rechaza <b>antes</b> de traducir un solo rotulo: una cesta invalida no
         * gasta consultas ni deja a medias una traduccion que nadie va a usar.
         */
        @Test
        @DisplayName("no traduce ningun codigo cuando la cesta ya es invalida")
        void no_traduce_ningun_codigo_cuando_la_cesta_es_invalida() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of(PIEZA_DEL_PAQUETE));

            assertThatThrownBy(() -> servicio().execute(comando(PAQUETE, PIEZA_DEL_PAQUETE)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(publishedCatalogItemQueryPort, never()).findPublishedIdByCode(anyString(), any(),
                    any());
        }

        /**
         * La cantidad de un modulo es una casilla encendida, no una unidad que se
         * acumule: dos lineas del mismo rotulo son dos cobros por lo mismo con la misma
         * cara.
         */
        @Test
        @DisplayName("el mismo rotulo dos veces en la misma cesta se rechaza")
        void el_mismo_rotulo_dos_veces_se_rechaza() {
            hayTarifaVigente();

            assertThatThrownBy(() -> servicio().execute(comando(MODULO_SUELTO, MODULO_SUELTO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicated catalog item code")
                    .hasMessageContaining(MODULO_SUELTO);

            verifyNoInteractions(issuer);
        }
    }
}
