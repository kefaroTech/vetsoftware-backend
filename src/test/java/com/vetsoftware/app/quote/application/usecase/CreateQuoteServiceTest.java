package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.NUMERO;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.PRICE_LIST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.borrador;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioConIncluidas;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.pregunta;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteAnswerCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteLineDto;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.ConfiguratorQuestionQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.Quote;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateQuoteService: idempotencia, congelacion y cuadre")
class CreateQuoteServiceTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

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
    @Mock
    private ConfiguratorQuestionQueryPort configuratorQuestionQueryPort;

    private CreateQuoteService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateQuoteService(repository, quoteNumberPort, companyQueryPort,
                priceListQueryPort, catalogItemQueryPort, catalogPriceQueryPort,
                configuratorQuestionQueryPort, RELOJ);
    }

    private static CreateQuoteCommand comando(List<QuoteLineCommand> lineas,
            List<QuoteAnswerCommand> respuestas) {
        return new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), null, null, null, null,
                PRICE_LIST_ID, "MONTHLY", VIGENTE_HASTA, 0, lineas, respuestas);
    }

    private static CreateQuoteCommand comandoDeUnModulo() {
        return comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)), List.of());
    }

    private void caminoFeliz() {
        when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                .thenReturn(Optional.empty());
        when(priceListQueryPort.findPublishedById(PRICE_LIST_ID)).thenReturn(Optional.of(tarifa()));
        when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
        when(quoteNumberPort.next(2026)).thenReturn(NUMERO);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Idempotencia (R13)")
    class Idempotencia {

        @Test
        @DisplayName("busca por la llave del cliente ANTES de insertar y devuelve la que ya nacio")
        void devuelve_la_cotizacion_que_ya_existe() {
            Quote yaCreada = borrador();
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.of(yaCreada));

            QuoteDto resultado = service.execute(comandoDeUnModulo());

            assertThat(resultado.quoteNumber()).isEqualTo(yaCreada.getQuoteNumber());
            assertThat(resultado.clientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
        }

        /**
         * La llave la elige quien llama, asi que la busqueda ancha servida a un
         * principal de empresa es una lectura cross-tenant disfrazada de idempotencia:
         * reutilizar el clientRequestId de otra clinica devolveria SU cotizacion entera
         * —razon social, prospecto, cada linea con su precio y su descuento, y la
         * prueba de aceptacion con su IP—. Con empresa se busca acotado y la ancha no
         * se toca.
         */
        @Test
        @DisplayName("con empresa busca la llave acotada y NUNCA la ancha (fuga cross-tenant)")
        void con_empresa_no_usa_la_busqueda_ancha() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.of(borrador()));

            service.execute(comandoDeUnModulo());

            verify(repository, never()).findByClientRequestId(any());
        }

        @Test
        @DisplayName("el reintento no escribe nada: ni guarda, ni consume un numero de cotizacion")
        void el_reintento_no_escribe_nada() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.of(borrador()));

            service.execute(comandoDeUnModulo());

            verify(repository, never()).save(any());
            verifyNoInteractions(quoteNumberPort);
        }

        @Test
        @DisplayName("el reintento no vuelve a leer el catalogo: devuelve el documento congelado")
        void el_reintento_no_relee_el_catalogo() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.of(borrador()));

            service.execute(comandoDeUnModulo());

            verifyNoInteractions(catalogItemQueryPort);
            verifyNoInteractions(catalogPriceQueryPort);
            verifyNoInteractions(priceListQueryPort);
        }

        @Test
        @DisplayName("cuando la llave es nueva si emite numero y guarda")
        void cuando_la_llave_es_nueva_si_guarda() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));

            service.execute(comandoDeUnModulo());

            verify(repository).save(any());
            verify(quoteNumberPort).next(2026);
        }
    }

    @Nested
    @DisplayName("Congelacion y cuadre de totales (R5)")
    class CongelacionYCuadre {

        @Test
        @DisplayName("copia del catalogo el codigo, el nombre, el precio y la tarifa de IVA")
        void congela_los_datos_del_catalogo() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));

            service.execute(comandoDeUnModulo());

            ArgumentCaptor<Quote> guardada = ArgumentCaptor.forClass(Quote.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getLines()).singleElement().satisfies(linea -> {
                assertThat(linea.getItemCode()).isEqualTo("CLINICAL_HISTORY");
                assertThat(linea.getItemName()).isEqualTo("Historia clinica");
                assertThat(linea.getUnitAmount()).isEqualByComparingTo("100000.00");
                assertThat(linea.getTaxRate()).isEqualByComparingTo("19.00");
            });
        }

        @Test
        @DisplayName("los totales guardados son los que suman las lineas, no los que llegan")
        void los_totales_guardados_suman_las_lineas() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY, 3)).thenReturn(Optional.of(precioGravado("12000.00")));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(modulo().id(), 1, new BigDecimal("10.00")),
                            new QuoteLineCommand(usuarioExtra().id(), 3, BigDecimal.ZERO)),
                    List.of()));

            assertThat(dto.subtotalAmount()).isEqualByComparingTo("136000.00");
            assertThat(dto.discountAmount()).isEqualByComparingTo("10000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("23940.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("149940.00");
        }

        @Test
        @DisplayName("numera las lineas desde 1 para que el orden impreso sea un contrato")
        void numera_las_lineas_desde_uno() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findApplicable(eq(PRICE_LIST_ID), eq(modulo().id()), any(),
                    anyInt())).thenReturn(Optional.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findApplicable(eq(PRICE_LIST_ID), eq(usuarioExtra().id()),
                    any(), anyInt())).thenReturn(Optional.of(precioGravado("12000.00")));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 2, BigDecimal.ZERO)),
                    List.of()));

            assertThat(dto.lines()).extracting(QuoteLineDto::lineNumber).containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("R15: lo incluido se resta antes de fijar la cantidad")
    class UnidadesIncluidas {

        @Test
        @DisplayName("cobra solo lo que excede lo incluido y guarda las tres cifras")
        void cobra_solo_lo_que_excede_lo_incluido() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY, 3))
                    .thenReturn(Optional.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(usuarioExtra().id(), 3, BigDecimal.ZERO)),
                            List.of()));

            assertThat(dto.lines()).singleElement().satisfies(linea -> {
                assertThat(linea.contractedQuantity()).isEqualTo(3);
                assertThat(linea.includedQuantity()).isEqualTo(2);
                assertThat(linea.quantity()).isEqualTo(1);
                assertThat(linea.lineTotal()).isEqualByComparingTo("14280.00");
            });
            assertThat(dto.totalAmount()).isEqualByComparingTo("14280.00");
        }

        @Test
        @DisplayName("si lo contratado no supera lo incluido no se emite linea y no se cobra nada")
        void lo_totalmente_incluido_no_genera_linea() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY, 1))
                    .thenReturn(Optional.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 1, BigDecimal.ZERO)),
                    List.of()));

            assertThat(dto.lines()).hasSize(1);
            assertThat(dto.lines().getFirst().itemCode()).isEqualTo("CLINICAL_HISTORY");
            assertThat(dto.totalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("la cantidad exactamente igual a la incluida tampoco genera cobro")
        void la_cantidad_igual_a_la_incluida_no_genera_cobro() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY, 2))
                    .thenReturn(Optional.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 2, BigDecimal.ZERO)),
                    List.of()));

            assertThat(dto.lines()).hasSize(1);
            assertThat(dto.totalAmount()).isEqualByComparingTo("119000.00");
        }
    }

    @Nested
    @DisplayName("Tenancy y referencias")
    class TenancyYReferencias {

        @Test
        @DisplayName("sin companyId cotiza a un prospecto y no consulta la tabla de empresas")
        void sin_company_id_cotiza_a_un_prospecto() {
            when(repository.findByClientRequestId(CLIENT_REQUEST_ID)).thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(quoteNumberPort.next(2026)).thenReturn(NUMERO);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));

            QuoteDto dto = service.execute(new CreateQuoteCommand(CLIENT_REQUEST_ID, null,
                    "Veterinaria del Sur", null, null, null, PRICE_LIST_ID, "MONTHLY",
                    VIGENTE_HASTA, 15,
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)), List.of()));

            assertThat(dto.company()).isNull();
            assertThat(dto.prospectName()).isEqualTo("Veterinaria del Sur");
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("registra la respuesta del configurador copiando el codigo de la pregunta")
        void registra_las_respuestas_del_configurador() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(configuratorQuestionQueryPort.findById(11L)).thenReturn(Optional.of(pregunta()));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)),
                            List.of(new QuoteAnswerCommand(11L, 99L, "SI"))));

            assertThat(dto.answers()).singleElement().satisfies(respuesta -> {
                assertThat(respuesta.questionCode()).isEqualTo("SELLS_PRODUCTS");
                assertThat(respuesta.optionId()).isEqualTo(99L);
            });
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("una tarifa que no esta publicada no sirve para cotizar")
        void exige_tarifa_publicada() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoDeUnModulo()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Published price list not found: 7");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un articulo que no esta activo no se puede congelar en la oferta")
        void exige_articulo_activo() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
            when(catalogItemQueryPort.findActiveById(modulo().id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoDeUnModulo()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Catalog item not found or not active: 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un articulo sin precio en esa tarifa y ese ciclo no se puede cotizar")
        void exige_precio_en_la_tarifa() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoDeUnModulo()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No price for catalog item 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un ciclo de facturacion desconocido se rechaza con su valor")
        void rechaza_un_ciclo_desconocido() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());

            CreateQuoteCommand comando = new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(),
                    null, null, null, null, PRICE_LIST_ID, "SEMANAL", VIGENTE_HASTA, 0,
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)), List.of());

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown billingCycle: SEMANAL");
        }

        @Test
        @DisplayName("una empresa inexistente no puede recibir una ampliacion")
        void exige_empresa_existente() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoDeUnModulo()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: 42");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una peticion sin lineas no es una oferta")
        void rechaza_una_peticion_sin_lineas() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));

            assertThatThrownBy(() -> service.execute(comando(List.of(), List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }

        @Test
        @DisplayName("una pregunta de configurador inexistente no se puede congelar")
        void exige_pregunta_existente() {
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(configuratorQuestionQueryPort.findById(11L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)),
                            List.of(new QuoteAnswerCommand(11L, 99L, "SI")))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Configurator question not found: 11");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Determinismo")
    class Determinismo {

        @Test
        @DisplayName("pide el numero del ano del reloj inyectado, no del reloj de la maquina")
        void usa_el_ano_del_reloj_inyectado() {
            Clock enDosMil = Clock.fixed(
                    LocalDate.of(2030, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());
            CreateQuoteService conOtroReloj = new CreateQuoteService(repository, quoteNumberPort,
                    companyQueryPort, priceListQueryPort, catalogItemQueryPort,
                    catalogPriceQueryPort, configuratorQuestionQueryPort, enDosMil);
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifa()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findApplicable(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY, 1)).thenReturn(Optional.of(precioGravado("100000.00")));
            when(quoteNumberPort.next(2030)).thenReturn("COT-2030-00001");
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            QuoteDto dto = conOtroReloj.execute(comandoDeUnModulo());

            assertThat(dto.quoteNumber()).isEqualTo("COT-2030-00001");
        }
    }
}
