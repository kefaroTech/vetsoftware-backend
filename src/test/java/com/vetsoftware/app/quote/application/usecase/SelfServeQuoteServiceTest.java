package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.HOY;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
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
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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
 * La autocontratacion, mirada por donde puede doler: <b>el dinero</b>.
 *
 * <p>
 * <b>La garantia de fondo no es una validacion, es el tipo.</b>
 * {@link SelfServeQuoteCommand} no declara ni un campo economico —ni tarifa, ni
 * vigencia, ni descuento, ni dias de prueba— y
 * {@link SelfServeQuoteLineCommand} tampoco. No estan validados a cero: <b>no
 * se pueden escribir</b>. Eso lo fija {@link LaGarantiaEsElTipo}, que ademas
 * contrasta contra {@link CreateQuoteCommand}, donde esos campos SI viajan: sin
 * el contraste, la ausencia podria ser una casualidad de como quedo el record.
 *
 * <p>
 * Lo que queda por probar es lo que el servidor escribe <em>en su lugar</em>, y
 * cada punto cierra un abuso concreto:
 *
 * <ul>
 * <li><b>La tarifa</b> es la {@code PUBLISHED} vigente HOY segun el reloj
 * inyectado. Si viniera del cuerpo bastaria apuntar a una lista vieja que nadie
 * archivo para contratarse al precio del ano pasado — D-73 visto desde el otro
 * lado.</li>
 * <li><b>El descuento</b> se escribe en cero en el unico sitio del camino donde
 * ese campo llega a existir.</li>
 * <li><b>Que articulos existen</b> lo decide el servidor y no el cuerpo: ver
 * {@link ElCatalogoQueElTenantPuedeNombrar}.</li>
 * <li><b>La vigencia</b> son 15 dias desde hoy; del cuerpo saldria una oferta
 * perpetua.</li>
 * <li><b>{@code trialDays = 0} en la cabecera es deliberado</b>: la prueba
 * vence por linea, no por contrato, y rellenar el entero con «el maximo»
 * regalaria de mas y con «el minimo» de menos.</li>
 * </ul>
 *
 * <p>
 * Que la oferta quede {@code SENT} y no {@code DRAFT} —sin lo cual
 * {@code Quote.accept} rechazaria el ultimo clic, ver
 * {@code QuoteTest.un_borrador_no_se_acepta}— lo decide
 * {@code PlatformQuoteIssuerAdapter} y se prueba en su propia clase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelfServeQuoteService — nadie se cotiza a si mismo el precio")
class SelfServeQuoteServiceTest {

    /** Las 10:30 del 2026-08-22 en la zona del negocio. */
    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    /** Lo que dura una oferta de autoservicio: 15 dias desde hoy. */
    private static final LocalDate VIGENTE_HASTA_ESPERADO = HOY.plusDays(15);

    private static final Long TARIFA_VIGENTE = 70L;
    private static final Long TARIFA_CADUCADA = 69L;
    private static final Long TARIFA_GEMELA = 99L;

    @Mock
    private PlatformQuoteIssuerPort issuer;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;
    @Captor
    private ArgumentCaptor<CreateQuoteCommand> emitido;

    private SelfServeQuoteService servicio(Clock reloj) {
        return new SelfServeQuoteService(issuer, priceListQueryPort, publishedCatalogItemQueryPort,
                reloj);
    }

    private static PriceListRef vigente() {
        return new PriceListRef(TARIFA_VIGENTE, "LISTA-2026-08", "COP", LocalDate.of(2026, 8, 1),
                null);
    }

    private static PriceListRef caducada() {
        return new PriceListRef(TARIFA_CADUCADA, "LISTA-2025-01", "COP", LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31));
    }

    private static SelfServeQuoteCommand comando(List<SelfServeQuoteLineCommand> lineas) {
        return new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), "MONTHLY", lineas);
    }

    private static SelfServeQuoteCommand comando() {
        return comando(List.of(new SelfServeQuoteLineCommand(modulo().code(), 1),
                new SelfServeQuoteLineCommand(usuarioExtra().code(), 15)));
    }

    /** El embudo devuelve la oferta ya emitida; aqui solo hace de espejo. */
    private void elIssuerEmite() {
        when(issuer.issue(any()))
                .thenReturn(QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT)));
    }

    /**
     * Los dos articulos de {@link #comando()} estan publicados en esa tarifa y en
     * el ciclo mensual, asi que el traductor devuelve sus ids.
     */
    private void elCatalogoPublicaLasDosLineas(Long tarifa) {
        when(publishedCatalogItemQueryPort.findPublishedIdByCode(modulo().code(), tarifa,
                BillingCycle.MONTHLY)).thenReturn(Optional.of(modulo().id()));
        when(publishedCatalogItemQueryPort.findPublishedIdByCode(usuarioExtra().code(), tarifa,
                BillingCycle.MONTHLY)).thenReturn(Optional.of(usuarioExtra().id()));
    }

    @Nested
    @DisplayName("La tarifa la resuelve el servidor")
    class TarifaResueltaEnServidor {

        @Test
        @DisplayName("de las publicadas cotiza con la vigente HOY, no con la del ano pasado")
        void cotiza_con_la_vigente_hoy() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(caducada(), vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }

        /**
         * Dos ventanas solapadas son legales en el esquema. Que gane «la primera que
         * devuelva la consulta» significaria que el precio de una contratacion depende
         * del plan de ejecucion de MySQL.
         */
        @Test
        @DisplayName("con dos vigentes solapadas gana la de validFrom mas reciente")
        void con_dos_vigentes_gana_la_mas_reciente() {
            when(priceListQueryPort.findAllPublished())
                    .thenReturn(List.of(vigente(), new PriceListRef(TARIFA_GEMELA, "LISTA-2026-01",
                            "COP", LocalDate.of(2026, 1, 1), null)));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }

        @Test
        @DisplayName("a igualdad de validFrom gana el id mayor: la ultima publicada")
        void a_igualdad_de_valid_from_gana_el_id_mayor() {
            when(priceListQueryPort.findAllPublished())
                    .thenReturn(List.of(new PriceListRef(TARIFA_GEMELA, "LISTA-GEMELA", "COP",
                            LocalDate.of(2026, 8, 1), null), vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_GEMELA);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_GEMELA);
        }

        @Test
        @DisplayName("sin tarifa vigente no se emite nada: la contratacion se para aqui")
        void sin_tarifa_vigente_no_se_emite_nada() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(caducada()));

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No published price list is effective on");

            verifyNoInteractions(issuer, publishedCatalogItemQueryPort);
        }

        @Test
        @DisplayName("sin ninguna tarifa publicada tampoco se emite nada")
        void sin_ninguna_tarifa_publicada_tampoco() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of());

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No published price list is effective on");

            verifyNoInteractions(issuer, publishedCatalogItemQueryPort);
        }

        /**
         * D-81. A las 19:30 en Bogota del ultimo dia de la tarifa, «hoy» en horario
         * universal ya es manana: un {@code LocalDate.now()} pelado —o un
         * {@code CURRENT_DATE} del motor— rechazaria una contratacion legitima.
         */
        @Test
        @DisplayName("a las 19:30 del ultimo dia de la tarifa la contratacion sigue siendo posible")
        void a_las_19_30_del_ultimo_dia_todavia_se_contrata() {
            Clock alFilo = Clock.fixed(ZonedDateTime
                    .of(HOY, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant(),
                    ClockConfig.BUSINESS_ZONE);
            when(priceListQueryPort.findAllPublished())
                    .thenReturn(List.of(new PriceListRef(TARIFA_VIGENTE, "LISTA-CIERRE", "COP",
                            LocalDate.of(2026, 8, 1), HOY)));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(alFilo).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }
    }

    /**
     * <b>El endpoint era inalcanzable, y lo arregla este traductor.</b> La linea
     * pedia {@code catalogItemId}; {@code GET /plans} no publica ningun id a
     * proposito y {@code GET /catalog-items} es {@code hasRole('SYSTEM')}, asi que
     * no habia ninguna cadena por la que un empleado del tenant obtuviera esos
     * numeros: permiso sembrado, ruta publicada, cero llamadores posibles. Ahora la
     * linea nombra el articulo por {@code code} y el servidor lo resuelve.
     *
     * <p>
     * <b>Lo que estos tests SI garantizan y lo que NO — leelo antes de confiar en
     * ellos.</b> El puerto esta mockeado, asi que aqui no se comprueba <em>que
     * conjunto</em> de articulos es resoluble: eso lo decide el {@code WHERE} de
     * {@code JpaPublishedCatalogItemQueryPort} y solo se puede probar contra MySQL
     * real, en la rodaja del adaptador. Lo que si se comprueba —y no es poco— es
     * que <b>el servicio no deshace la indistinguibilidad</b>: dado un puerto que
     * ya responde {@code empty()} sin decir por que, el caso de uso no puede
     * inventarse la diferencia en el mensaje. Sin este test, el dia que alguien
     * escriba un «Catalog item not found: X» mas util para depurar, habra
     * convertido este endpoint en un enumerador del catalogo interno y nada lo
     * dira.
     */
    @Nested
    @DisplayName("El catalogo que el tenant puede nombrar lo decide el servidor")
    class ElCatalogoQueElTenantPuedeNombrar {

        private static final String CODE_INEXISTENTE = "NO_EXISTE_ESTE_ROTULO";
        private static final String CODE_INTERNO = "IMPLANTACION_ONBOARDING";

        @Test
        @DisplayName("el rotulo publicado se traduce al id del catalogo, y es el que se cotiza")
        void el_rotulo_publicado_se_traduce_al_id() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                    .containsExactly(modulo().id(), usuarioExtra().id());
        }

        /**
         * <b>Capacidad extra en ciclo anual, de punta a punta.</b> Era el camino que
         * nadie ejercitaba: {@code usuarioExtra()} es un {@code CAPACITY} y el ciclo es
         * {@code ANNUAL}, asi que el traductor tiene que pedirle al catalogo el
         * articulo <em>en ese ciclo</em> —no en el mensual— y la oferta tiene que salir
         * con el ciclo anual en la cabecera.
         *
         * <p>
         * Que el precio unitario viaje en cero no es un descuido: en autoservicio el
         * importe lo pone {@code CreateQuoteService} leyendo la escalera del ciclo, y
         * el cero es justamente lo que impide que el cliente proponga un precio. Que
         * esa escalera anual exista y sea la que manda lo prueba
         * {@code QuoteCatalogQueryPortsIT} contra MySQL real.
         */
        @Test
        @DisplayName("una linea de capacidad extra en ciclo anual se resuelve y se emite con el"
                + " ciclo anual")
        void la_capacidad_extra_anual_se_resuelve_y_se_emite() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(usuarioExtra().code(),
                    TARIFA_VIGENTE, BillingCycle.ANNUAL))
                    .thenReturn(Optional.of(usuarioExtra().id()));
            elIssuerEmite();

            servicio(RELOJ).execute(new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(),
                    "ANNUAL", List.of(new SelfServeQuoteLineCommand(usuarioExtra().code(), 15))));

            verify(publishedCatalogItemQueryPort).findPublishedIdByCode(usuarioExtra().code(),
                    TARIFA_VIGENTE, BillingCycle.ANNUAL);
            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().billingCycle()).isEqualTo("ANNUAL");
            assertThat(emitido.getValue().lines()).singleElement().satisfies(linea -> {
                assertThat(linea.catalogItemId()).isEqualTo(usuarioExtra().id());
                assertThat(linea.quantity()).isEqualTo(15);
                assertThat(linea.discountPercent()).isEqualByComparingTo(BigDecimal.ZERO);
            });
        }

        /**
         * <b>La prueba del oraculo.</b> Un rotulo que no existe y uno que existe pero
         * es catalogo interno —un {@code ONE_TIME} de implantacion, que
         * {@code GET /plans} no anuncia— tienen que salir por el mismo sitio y con el
         * mismo texto. La comparacion es {@code isEqualTo} sobre el mensaje entero, no
         * {@code hasMessageContaining}: aqui el punto es justamente que no sobre ni
         * falte un byte, porque cualquier diferencia —incluido el eco del codigo
         * recibido— es la que responde «ese existe, ese no».
         */
        @Test
        @DisplayName("un rotulo interno se rechaza igual que uno inexistente, y con el mismo texto")
        void un_rotulo_interno_es_indistinguible_de_uno_inexistente() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(CODE_INEXISTENTE,
                    TARIFA_VIGENTE, BillingCycle.MONTHLY)).thenReturn(Optional.empty());
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(CODE_INTERNO, TARIFA_VIGENTE,
                    BillingCycle.MONTHLY)).thenReturn(Optional.empty());

            Throwable porInexistente = catchThrowable(() -> servicio(RELOJ)
                    .execute(comando(List.of(new SelfServeQuoteLineCommand(CODE_INEXISTENTE, 1)))));
            Throwable porInterno = catchThrowable(() -> servicio(RELOJ)
                    .execute(comando(List.of(new SelfServeQuoteLineCommand(CODE_INTERNO, 1)))));

            assertThat(porInexistente).isInstanceOf(IllegalArgumentException.class);
            assertThat(porInterno).isInstanceOf(IllegalArgumentException.class)
                    .hasSameClassAs(porInexistente);
            assertThat(porInterno.getMessage()).isEqualTo(porInexistente.getMessage());
            assertThat(porInterno.getMessage()).doesNotContain(CODE_INTERNO)
                    .doesNotContain(CODE_INEXISTENTE);
        }

        /**
         * Con una sola linea mala no se emite media oferta: el embudo no llega a
         * enterarse. Un fallo parcial dejaria al cliente con una cotizacion que no es
         * la que pidio, firmada igualmente.
         */
        @Test
        @DisplayName("basta que una linea no sea contratable para que no se emita nada")
        void una_linea_no_contratable_frena_la_oferta_entera() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(modulo().code(),
                    TARIFA_VIGENTE, BillingCycle.MONTHLY)).thenReturn(Optional.of(modulo().id()));
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(CODE_INTERNO, TARIFA_VIGENTE,
                    BillingCycle.MONTHLY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio(RELOJ)
                    .execute(comando(List.of(new SelfServeQuoteLineCommand(modulo().code(), 1),
                            new SelfServeQuoteLineCommand(CODE_INTERNO, 1)))))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(issuer);
        }

        /**
         * Las tres coordenadas importan. Con el ciclo cableado a {@code MONTHLY} —o con
         * la tarifa cogida de cualquier otro sitio— un articulo tarifado solo en
         * mensual se colaria en una cotizacion anual y el precio saldria de una columna
         * que nadie publico para ese ciclo.
         */
        @Test
        @DisplayName("el traductor recibe la tarifa vigente y el ciclo pedido, no otros")
        void el_traductor_recibe_la_tarifa_y_el_ciclo_de_la_cotizacion() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(caducada(), vigente()));
            when(publishedCatalogItemQueryPort.findPublishedIdByCode(modulo().code(),
                    TARIFA_VIGENTE, BillingCycle.ANNUAL)).thenReturn(Optional.of(modulo().id()));
            elIssuerEmite();

            servicio(RELOJ).execute(new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(),
                    "ANNUAL", List.of(new SelfServeQuoteLineCommand(modulo().code(), 1))));

            verify(publishedCatalogItemQueryPort).findPublishedIdByCode(modulo().code(),
                    TARIFA_VIGENTE, BillingCycle.ANNUAL);
        }

        /**
         * El {@code @Pattern} del request ya acota el ciclo en el borde REST, pero
         * {@code SYSTEM} tambien puede llamar al puerto directamente y ahi no hay
         * binder que valide. Sale 400 y no un 500 desde el {@code valueOf}.
         */
        @Test
        @DisplayName("un ciclo que no existe se rechaza antes de tocar el catalogo")
        void un_ciclo_desconocido_se_rechaza() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));

            assertThatThrownBy(() -> servicio(RELOJ).execute(
                    new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), "SEMESTRAL",
                            List.of(new SelfServeQuoteLineCommand(modulo().code(), 1)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown billingCycle");

            verifyNoInteractions(issuer, publishedCatalogItemQueryPort);
        }
    }

    @Nested
    @DisplayName("Los terminos los escribe el servidor")
    class TerminosQueEscribeElServidor {

        @Test
        @DisplayName("cada linea sale con descuento CERO, en el unico sitio donde ese campo existe")
        void cada_linea_sale_con_descuento_cero() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).containsExactly(
                    new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO, false),
                    new QuoteLineCommand(usuarioExtra().id(), 15, BigDecimal.ZERO, false));
        }

        @Test
        @DisplayName("la oferta vale 15 dias desde hoy, no lo que diga el cuerpo")
        void la_oferta_vale_quince_dias() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().validUntil()).isEqualTo(VIGENTE_HASTA_ESPERADO);
        }

        /**
         * Cero significa «esta cabecera no promete prueba». La prueba real se concede
         * articulo a articulo en el camino de contrato, y {@code GET /plans} la publica
         * por linea; un numero aqui seria una promesa plana que el modelo no tiene.
         */
        @Test
        @DisplayName("la cabecera no promete prueba: trialDays cero, a proposito")
        void la_cabecera_no_promete_prueba() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().trialDays()).isZero();
        }

        @Test
        @DisplayName("la empresa, la llave de idempotencia y el ciclo viajan tal cual")
        void la_empresa_y_la_llave_viajan_tal_cual() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().companyId()).isEqualTo(empresa().id());
            assertThat(emitido.getValue().clientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
            assertThat(emitido.getValue().billingCycle()).isEqualTo("MONTHLY");
        }

        /**
         * Quien se autocontrata es una empresa, no un prospecto: los cuatro campos del
         * prospecto van nulos. Rellenarlos desde el cuerpo permitiria emitir una oferta
         * a nombre de cualquiera.
         */
        @Test
        @DisplayName("no hay prospecto que rellenar: los cuatro campos van nulos")
        void no_hay_prospecto_que_rellenar() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().prospectName()).isNull();
            assertThat(emitido.getValue().prospectEmail()).isNull();
            assertThat(emitido.getValue().prospectDocument()).isNull();
            assertThat(emitido.getValue().prospectPhone()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin lineas produce una oferta sin lineas, no un NullPointer")
        void un_cuerpo_sin_lineas_no_revienta() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando(null));

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).isEmpty();
            verifyNoInteractions(publishedCatalogItemQueryPort);
        }

        @Test
        @DisplayName("devuelve tal cual la oferta que emitio el embudo")
        void devuelve_tal_cual_la_oferta_emitida() {
            QuoteDto emitida = QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT));
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elCatalogoPublicaLasDosLineas(TARIFA_VIGENTE);
            when(issuer.issue(any())).thenReturn(emitida);

            assertThat(servicio(RELOJ).execute(comando())).isSameAs(emitida);
        }
    }

    @Nested
    @DisplayName("La garantia es el tipo, no una validacion")
    class LaGarantiaEsElTipo {

        /**
         * El contraste es la mitad del argumento: si {@link CreateQuoteCommand} tampoco
         * los declarase, la ausencia de arriba no probaria nada.
         */
        @Test
        @DisplayName("el comando de autoservicio no declara ni tarifa, ni vigencia, ni dias de"
                + " prueba; el de plataforma si")
        void el_comando_de_autoservicio_no_declara_terminos() {
            assertThat(SelfServeQuoteCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("clientRequestId", "companyId", "billingCycle", "lines");

            assertThat(CreateQuoteCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .contains("priceListId", "validUntil", "trialDays");
        }

        /**
         * Dos ausencias, no una. Sigue sin declarar descuento —lo de siempre— y
         * <b>tampoco declara ya el id del catalogo</b>: nombra el articulo por el mismo
         * rotulo que publica {@code GET /plans}. El contraste con
         * {@link QuoteLineCommand}, que si lleva las dos cosas, es lo que separa el
         * camino del tenant del camino de plataforma.
         */
        @Test
        @DisplayName("la linea de autoservicio nombra por code y no declara descuento;"
                + " la de plataforma lleva id y descuento")
        void la_linea_de_autoservicio_no_declara_descuento() {
            assertThat(SelfServeQuoteLineCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName).containsExactly("code", "quantity");

            assertThat(QuoteLineCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .contains("catalogItemId", "discountPercent", "discountIsConditional");
        }
    }
}
