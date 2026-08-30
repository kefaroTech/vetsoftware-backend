package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.NUMERO;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.PRICE_LIST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaCaducada;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaFutura;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaQueVenceEl;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaSinCierre;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.shared.pricing.PriceListNotEffectiveException;
import com.vetsoftware.app.quote.domain.PriceListRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-73 en el caso de uso que cotiza: <em>el precio sale de la tarifa vigente
 * por fecha</em> (COT-020 y COT-021).
 *
 * <p>
 * El defecto que cierra esta clase: {@code JpaPriceListQueryPort} elegia la
 * tarifa filtrando <b>solo</b> por {@code status = 'PUBLISHED'}. Bastaba con
 * que una lista vieja siguiera publicada para que <i>hoy se cotizara con la
 * tarifa de 2025</i>, sin error y sin alarma. El camino del contrato
 * -{@code JpaSubscriptionCommercialSnapshotPort#isApplicable}- si comprobaba la
 * vigencia; el de la cotizacion, que es por donde entra el dinero nuevo, no.
 * Aqui se fija que las dos vias deciden con el mismo predicado.
 *
 * <p>
 * {@link ZonaDelReloj} es ademas el chivato de D-81: el dia contra el que se
 * compara la ventana se deriva del reloj <b>inyectado y con zona</b>, y los dos
 * casos parten del mismo instante para que la unica variable sea la zona.
 *
 * @see ClockConfig
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateQuoteService: cotizar exige la tarifa VIGENTE por fecha (D-73)")
class CreateQuotePriceListValidityTest {

    /** Fecha de validez de la oferta; no es lo que se prueba aqui. */
    private static final LocalDate OFERTA_VALIDA_HASTA = LocalDate.of(2026, 12, 31);

    /** El reloj del caso normal: el 2026-08-22 a las 10:30 en Bogota. */
    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    /**
     * Las 19:30 del ultimo dia de vigencia de la tarifa, hora de Bogota. Se
     * construye desde {@link ClockConfig#BUSINESS_ZONE} y no escribiendo
     * {@code -05:00} a mano: escribir el desplazamiento duplicaria la decision del
     * bean en vez de comprobarla.
     */
    private static final Instant LAS_19_30_DEL_ULTIMO_DIA = ZonedDateTime
            .of(VIGENTE_HASTA, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant();

    @Mock
    private QuoteRepository repository;
    @Mock
    private QuoteNumberPort quoteNumberPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;
    @Mock
    private CatalogPriceQueryPort catalogPriceQueryPort;

    private CreateQuoteService servicio(Clock reloj) {
        return new CreateQuoteService(repository, quoteNumberPort, companyQueryPort,
                priceListQueryPort, catalogItemQueryPort, catalogPriceQueryPort, reloj);
    }

    private static CreateQuoteCommand comando() {
        return new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), null, null, null, null,
                PRICE_LIST_ID, "MONTHLY", OFERTA_VALIDA_HASTA, 0,
                List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)));
    }

    /** Lo minimo que se lee antes de mirar la tarifa: la llave de idempotencia. */
    private void llaveNueva(PriceListRef devuelta) {
        when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                .thenReturn(Optional.empty());
        when(priceListQueryPort.findPublishedById(PRICE_LIST_ID)).thenReturn(Optional.of(devuelta));
    }

    /** Lo que solo se llega a leer si la tarifa paso el filtro de vigencia. */
    private void restoDelCatalogo() {
        when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
        when(quoteNumberPort.next(2026)).thenReturn(NUMERO);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogItemQueryPort.findActiveById(modulo().id())).thenReturn(Optional.of(modulo()));
        when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(), BillingCycle.MONTHLY))
                .thenReturn(List.of(precioGravado("100000.00")));
    }

    @Nested
    @DisplayName("Vigencia por fecha")
    class Vigencia {

        @Test
        @DisplayName("COT-020: la tarifa publicada y vigente hoy si pone precio")
        void la_tarifa_vigente_pone_precio() {
            llaveNueva(tarifa());
            restoDelCatalogo();

            QuoteDto dto = servicio(RELOJ).execute(comando());

            assertThat(dto.quoteNumber()).isEqualTo(NUMERO);
            assertThat(dto.lines()).hasSize(1);
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("100000.00");
        }

        /**
         * El caso que da nombre al defecto: la lista de 2025 seguia PUBLISHED y ponia
         * precio a la cotizacion de hoy.
         */
        @Test
        @DisplayName("COT-021: una tarifa publicada pero caducada no cotiza")
        void una_tarifa_caducada_no_cotiza() {
            llaveNueva(tarifaCaducada());

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOfSatisfying(PriceListNotEffectiveException.class, fallo -> {
                        assertThat(fallo.getPriceListId()).isEqualTo(PRICE_LIST_ID);
                        assertThat(fallo.getValidTo()).isEqualTo(LocalDate.of(2025, 12, 31));
                        assertThat(fallo.getQuotedOn()).isEqualTo(AHORA.toLocalDate());
                    });
        }

        @Test
        @DisplayName("COT-021: y al rechazarla no lee precios ni escribe: no queda documento con"
                + " la tarifa vieja")
        void al_rechazar_la_caducada_no_escribe() {
            llaveNueva(tarifaCaducada());

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOf(PriceListNotEffectiveException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(catalogPriceQueryPort);
            verifyNoInteractions(quoteNumberPort);
        }

        @Test
        @DisplayName("COT-021: una tarifa publicada que aun no empieza tampoco cotiza")
        void una_tarifa_futura_no_cotiza() {
            llaveNueva(tarifaFutura());

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando())).isInstanceOfSatisfying(
                    PriceListNotEffectiveException.class,
                    fallo -> assertThat(fallo.getValidFrom()).isEqualTo(LocalDate.of(2027, 1, 1)));
        }

        /**
         * La comprobacion que, mal escrita, tumba el alta de empresas entera: la lista
         * viva del catalogo se publica con {@code valid_to = NULL} y un
         * {@code hoy <= validTo} contra el nulo la descartaria, dejando la plataforma
         * sin ninguna tarifa con la que cotizar.
         */
        @Test
        @DisplayName("COT-020: una tarifa sin fecha de fin (valid_to nulo) si cotiza")
        void una_tarifa_sin_fecha_de_fin_si_cotiza() {
            llaveNueva(tarifaSinCierre());
            restoDelCatalogo();

            QuoteDto dto = servicio(RELOJ).execute(comando());

            assertThat(dto.quoteNumber()).isEqualTo(NUMERO);
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("100000.00");
        }
    }

    @Nested
    @DisplayName("La zona del reloj decide cual es el ultimo dia de vigencia (D-81)")
    class ZonaDelReloj {

        private final Clock relojDeNegocio = Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA,
                ClockConfig.BUSINESS_ZONE);

        private final Clock relojSinZonaDeNegocio = Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA,
                ZoneOffset.UTC);

        @Test
        @DisplayName("COT-020: a las 19:30 del ultimo dia de vigencia la tarifa todavia rige y la"
                + " cotizacion sale")
        void a_las_19_30_del_ultimo_dia_la_tarifa_todavia_rige() {
            llaveNueva(tarifaQueVenceEl(VIGENTE_HASTA));
            restoDelCatalogo();

            QuoteDto dto = servicio(relojDeNegocio).execute(comando());

            assertThat(dto.quoteNumber()).isEqualTo(NUMERO);
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("100000.00");
        }

        /**
         * El chivato de la regresion: vuelve a rojo el dia que alguien devuelva el bean
         * del reloj a {@code systemDefaultZone()} —la imagen no declara zona, asi que
         * produccion decidiria en UTC—. Mismo instante, unica variable la zona.
         */
        @Test
        @DisplayName("el mismo instante con el reloj en UTC deriva manana y rechaza una tarifa"
                + " que si estaba vigente")
        void el_mismo_instante_en_utc_rechaza_una_tarifa_vigente() {
            llaveNueva(tarifaQueVenceEl(VIGENTE_HASTA));

            assertThatThrownBy(() -> servicio(relojSinZonaDeNegocio).execute(comando()))
                    .isInstanceOfSatisfying(PriceListNotEffectiveException.class, fallo -> {
                        assertThat(fallo.getQuotedOn()).isEqualTo(VIGENTE_HASTA.plusDays(1));
                        assertThat(fallo.getValidTo()).isEqualTo(VIGENTE_HASTA);
                    });
            verify(repository, never()).save(any());
        }
    }
}
