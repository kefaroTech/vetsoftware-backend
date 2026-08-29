package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.PreviewQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuotePreviewDto;
import com.vetsoftware.app.quote.application.dto.QuotePreviewLineDto;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>La calculadora publica devuelve el precio que se va a cobrar.</b>
 *
 * <p>
 * El caso que justifica el endpoint entero es el primero: quince usuarios con
 * la escalera de la semilla. Un front que solo tiene el tramo de entrada —lo
 * unico que el catalogo publica— multiplica y saca <b>156.000</b>; el servidor
 * reparte por tramos y cobra <b>141.000</b>. Esa diferencia es la que este
 * servicio existe para borrar, y lo hace usando el mismo codigo que congela una
 * oferta real en vez de una segunda implementacion que podria discrepar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PreviewQuoteService — el precio que se muestra es el que se cobra")
class PreviewQuoteServiceTest {

    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    private static final Long TARIFA = 70L;
    private static final String COD = "EXTRA_USER";

    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;
    @Mock
    private CatalogPriceQueryPort catalogPriceQueryPort;

    private PreviewQuoteService servicio() {
        return new PreviewQuoteService(priceListQueryPort, publishedCatalogItemQueryPort,
                catalogItemQueryPort, catalogPriceQueryPort, RELOJ);
    }

    private static PreviewQuoteCommand comando(int cantidad) {
        return new PreviewQuoteCommand("MONTHLY",
                List.of(new SelfServeQuoteLineCommand(COD, cantidad)));
    }

    private void hayTarifaVigente() {
        when(priceListQueryPort.findAllPublished()).thenReturn(List.of(
                new PriceListRef(TARIFA, "LISTA-2026-08", "COP", LocalDate.of(2026, 8, 1), null)));
    }

    private void cestaLimpia() {
        when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                .thenReturn(List.of());
        when(publishedCatalogItemQueryPort.findMissingRequirements(any())).thenReturn(List.of());
    }

    /** Escalera de la semilla: 1-8 a 12.000 y de la 9 en adelante a 9.000. */
    private void laEscaleraDeLaSemilla() {
        when(publishedCatalogItemQueryPort.findPublishedIdByCode(COD, TARIFA, BillingCycle.MONTHLY))
                .thenReturn(Optional.of(usuarioExtra().id()));
        when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                .thenReturn(Optional.of(usuarioExtra()));
        when(catalogPriceQueryPort.findAllTiers(TARIFA, usuarioExtra().id(), BillingCycle.MONTHLY))
                .thenReturn(List.of(
                        new CatalogPriceRef(new BigDecimal("12000.00"), new BigDecimal("19.00"),
                                TaxTreatment.TAXED, 0, 1, 8),
                        new CatalogPriceRef(new BigDecimal("9000.00"), new BigDecimal("19.00"),
                                TaxTreatment.TAXED, 0, 9, null)));
    }

    @Nested
    @DisplayName("el reparto por tramos")
    class Tramos {

        /**
         * <b>El caso D-66.</b> 8 x 12.000 + 5 x 9.000 = 141.000. La cifra que un front
         * sacaria multiplicando el tramo de entrada es 13 x 12.000 = 156.000, y es
         * exactamente la que este endpoint existe para no dejar que nadie calcule.
         */
        @Test
        @DisplayName("trece unidades salen 141.000, no 156.000 de multiplicar el tramo")
        void trece_unidades_salen_ciento_cuarenta_y_un_mil() {
            hayTarifaVigente();
            cestaLimpia();
            laEscaleraDeLaSemilla();

            QuotePreviewDto vista = servicio().preview(comando(13));

            assertThat(vista.subtotalAmount()).isEqualByComparingTo("141000.00");
        }

        /**
         * Un renglon por tramo y no uno agregado: es el mismo desglose con el que se
         * facturara, y lo que permite al cliente comprobar la cuenta.
         */
        @Test
        @DisplayName("devuelve un renglon por tramo, con el mismo desglose de la oferta")
        void devuelve_un_renglon_por_tramo() {
            hayTarifaVigente();
            cestaLimpia();
            laEscaleraDeLaSemilla();

            QuotePreviewDto vista = servicio().preview(comando(13));

            assertThat(vista.lines()).hasSize(2);
            assertThat(vista.lines()).extracting(QuotePreviewLineDto::quantity).containsExactly(8,
                    5);
            assertThat(vista.lines()).extracting(QuotePreviewLineDto::unitAmount)
                    .containsExactly(new BigDecimal("12000.00"), new BigDecimal("9000.00"));
        }

        @Test
        @DisplayName("los totales cuadran con la suma de las lineas")
        void los_totales_cuadran_con_las_lineas() {
            hayTarifaVigente();
            cestaLimpia();
            laEscaleraDeLaSemilla();

            QuotePreviewDto vista = servicio().preview(comando(13));

            BigDecimal sumaLineas = vista.lines().stream().map(QuotePreviewLineDto::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(vista.totalAmount()).isEqualByComparingTo(sumaLineas);
            assertThat(vista.totalAmount()).isEqualByComparingTo(
                    vista.subtotalAmount().subtract(vista.discountAmount()).add(vista.taxAmount()));
        }

        @Test
        @DisplayName("publica la divisa de la tarifa vigente, no una supuesta")
        void publica_la_divisa_de_la_tarifa() {
            hayTarifaVigente();
            cestaLimpia();
            laEscaleraDeLaSemilla();

            assertThat(servicio().preview(comando(13)).currency()).isEqualTo("COP");
        }
    }

    @Nested
    @DisplayName("el mismo gate que la contratacion")
    class MismoGate {

        /**
         * Si la vista previa tarifara lo que la contratacion rechaza, volveria a haber
         * un numero que la portada promete y el contrato niega.
         */
        @Test
        @DisplayName("una cesta con cobro doble se rechaza igual que al contratar")
        void una_cesta_con_cobro_doble_se_rechaza() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of("SCHEDULING"));

            assertThatThrownBy(() -> servicio().preview(new PreviewQuoteCommand("MONTHLY",
                    List.of(new SelfServeQuoteLineCommand("PACK_CLINIC", 1),
                            new SelfServeQuoteLineCommand("SCHEDULING", 1)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already included in a bundle");
        }

        @Test
        @DisplayName("una cesta a la que le falta un requisito se rechaza igual que al contratar")
        void una_cesta_incoherente_se_rechaza() {
            hayTarifaVigente();
            when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                    .thenReturn(List.of());
            when(publishedCatalogItemQueryPort.findMissingRequirements(any()))
                    .thenReturn(List.of("CASH_REGISTER"));

            assertThatThrownBy(() -> servicio().preview(new PreviewQuoteCommand("MONTHLY",
                    List.of(new SelfServeQuoteLineCommand("ELECTRONIC_INVOICING", 1)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing catalog items required");
        }

        @Test
        @DisplayName("un rotulo no publicado se rechaza sin decir por que")
        void un_rotulo_no_publicado_se_rechaza() {
            hayTarifaVigente();
            cestaLimpia();
            when(publishedCatalogItemQueryPort.findPublishedIdByCode("INTERNO", TARIFA,
                    BillingCycle.MONTHLY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio().preview(new PreviewQuoteCommand("MONTHLY",
                    List.of(new SelfServeQuoteLineCommand("INTERNO", 1)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown or unavailable catalog item code");
        }

        @Test
        @DisplayName("una cesta vacia no se tarifa")
        void una_cesta_vacia_no_se_tarifa() {
            hayTarifaVigente();

            assertThatThrownBy(
                    () -> servicio().preview(new PreviewQuoteCommand("MONTHLY", List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }

        @Test
        @DisplayName("sin tarifa vigente no se inventa un precio")
        void sin_tarifa_vigente_no_se_inventa_un_precio() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of());

            assertThatThrownBy(() -> servicio().preview(comando(1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No published price list is effective");
        }
    }
}
