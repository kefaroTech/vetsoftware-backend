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
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaSinCierre;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteLineDto;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.TaxTreatment;
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

    private CreateQuoteService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateQuoteService(repository, quoteNumberPort, companyQueryPort,
                priceListQueryPort, catalogItemQueryPort, catalogPriceQueryPort, RELOJ);
    }

    private static CreateQuoteCommand comando(List<QuoteLineCommand> lineas) {
        return new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), null, null, null, null,
                PRICE_LIST_ID, "MONTHLY", VIGENTE_HASTA, 0, lineas);
    }

    private static CreateQuoteCommand comandoDeUnModulo() {
        return comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)));
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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));

            service.execute(comandoDeUnModulo());

            verify(repository).save(any());
            verify(quoteNumberPort).next(2026);
        }
    }

    @Nested
    @DisplayName("D-66 — los tramos son acumulativos (R-PRICE-04, R-QUOTE-09)")
    class TramosAcumulativos {

        /**
         * «Unidades extra 1 a 8 a 12.000, de la 9 en adelante a 9.000», dos incluidas.
         */
        private static final CatalogPriceRef TRAMO_BAJO = new CatalogPriceRef(
                new BigDecimal("12000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, 2, 1, 8);
        private static final CatalogPriceRef TRAMO_ALTO = new CatalogPriceRef(
                new BigDecimal("9000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, 0, 9, null);

        @Test
        @DisplayName("cotizar 15 usuarios produce dos renglones de tramo y un subtotal de 141000")
        void cotizar_15_usuarios_produce_dos_renglones_de_tramo_y_un_total_de_141000() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(TRAMO_BAJO, TRAMO_ALTO));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(usuarioExtra().id(), 15, BigDecimal.ZERO))));

            assertThat(dto.lines()).hasSize(2);
            assertThat(dto.lines().get(0).tierMin()).isEqualTo(1);
            assertThat(dto.lines().get(0).tierMax()).isEqualTo(8);
            assertThat(dto.lines().get(0).quantity()).isEqualTo(8);
            assertThat(dto.lines().get(0).unitAmount()).isEqualByComparingTo("12000.00");
            assertThat(dto.lines().get(1).tierMin()).isEqualTo(9);
            assertThat(dto.lines().get(1).tierMax()).isNull();
            assertThat(dto.lines().get(1).quantity()).isEqualTo(5);
            assertThat(dto.lines().get(1).unitAmount()).isEqualByComparingTo("9000.00");

            // El subtotal de la cotizacion es la suma de las lineas, y es 141.000 exactos.
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("141000.00");
            assertThat(dto.subtotalAmount()).isNotEqualByComparingTo("117000.00");
            assertThat(dto.subtotalAmount()).isNotEqualByComparingTo("135000.00");
        }

        @Test
        @DisplayName("los dos renglones se numeran seguidos: el cliente ve el desglose ordenado")
        void los_dos_renglones_se_numeran_seguidos() {
            caminoFeliz();
            when(catalogItemQueryPort.findActiveById(usuarioExtra().id()))
                    .thenReturn(Optional.of(usuarioExtra()));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(TRAMO_BAJO, TRAMO_ALTO));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(usuarioExtra().id(), 15, BigDecimal.ZERO))));

            assertThat(dto.lines()).extracting(QuoteLineDto::lineNumber).containsExactly(1, 2);
            assertThat(dto.lines()).extracting(QuoteLineDto::contractedQuantity).containsExactly(15,
                    15);
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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));

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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("12000.00")));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, new BigDecimal("10.00")),
                            new QuoteLineCommand(usuarioExtra().id(), 3, BigDecimal.ZERO))));

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
            when(catalogPriceQueryPort.findAllTiers(eq(PRICE_LIST_ID), eq(modulo().id()), any()))
                    .thenReturn(List.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findAllTiers(eq(PRICE_LIST_ID), eq(usuarioExtra().id()),
                    any())).thenReturn(List.of(precioGravado("12000.00")));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 2, BigDecimal.ZERO))));

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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(comando(
                    List.of(new QuoteLineCommand(usuarioExtra().id(), 3, BigDecimal.ZERO))));

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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 1, BigDecimal.ZERO))));

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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, usuarioExtra().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioConIncluidas("12000.00", 2)));

            QuoteDto dto = service.execute(
                    comando(List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO),
                            new QuoteLineCommand(usuarioExtra().id(), 2, BigDecimal.ZERO))));

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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));

            QuoteDto dto = service
                    .execute(new CreateQuoteCommand(CLIENT_REQUEST_ID, null, "Veterinaria del Sur",
                            null, null, null, PRICE_LIST_ID, "MONTHLY", VIGENTE_HASTA, 15,
                            List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO))));

            assertThat(dto.company()).isNull();
            assertThat(dto.prospectName()).isEqualTo("Veterinaria del Sur");
            verifyNoInteractions(companyQueryPort);
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
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of());

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
                    List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)));

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

            assertThatThrownBy(() -> service.execute(comando(List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
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
                    catalogPriceQueryPort, enDosMil);
            when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                    .thenReturn(Optional.empty());
            // Tarifa SIN fecha de fin: lo que se prueba aqui es el ano del numero, no la
            // vigencia, y con el reloj puesto en 2030 una tarifa del ejercicio 2026 ya no
            // regiria (D-73). La abierta deja el foco donde estaba.
            when(priceListQueryPort.findPublishedById(PRICE_LIST_ID))
                    .thenReturn(Optional.of(tarifaSinCierre()));
            when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
            when(catalogItemQueryPort.findActiveById(modulo().id()))
                    .thenReturn(Optional.of(modulo()));
            when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, modulo().id(),
                    BillingCycle.MONTHLY)).thenReturn(List.of(precioGravado("100000.00")));
            when(quoteNumberPort.next(2030)).thenReturn("COT-2030-00001");
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            QuoteDto dto = conOtroReloj.execute(comandoDeUnModulo());

            assertThat(dto.quoteNumber()).isEqualTo("COT-2030-00001");
        }
    }
}
