package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import com.vetsoftware.app.quote.domain.QuoteLineArithmeticException;
import com.vetsoftware.app.quote.domain.QuoteTotalsMismatchException;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.registration.domain.PlatformRoleCatalogNotConfiguredException;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.OverAppliedSourceException;
import io.micrometer.tracing.Tracer;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

/**
 * Las cuatro decisiones del handler que no se leen en el status: el dato
 * estructurado que el front necesita, el 503 que trae escritos sus propios
 * pasos de arreglo, y el 500 que a proposito NO es un 409.
 *
 * <p>
 * Lo que aqui se prueba no es "que el metodo devuelva algo", sino lo que se
 * pierde en silencio cuando alguien lo simplifica: un {@code cycle} degradado a
 * texto obliga al front a parsear "12 &gt; 44 &gt; 12"; unos importes
 * convertidos a {@code double} le quitan al operador el "quedaban 100.000" que
 * es lo unico accionable; un mensaje de 503 recortado deja un 503 opaco; y un
 * 500 rebajado a 409 invita al cliente a reintentar algo que va a fallar igual.
 *
 * <p>
 * <strong>Por que hay un test de resolucion y no solo llamadas
 * directas.</strong> Las dos excepciones de cotizacion heredan de
 * {@code IllegalStateException}, que ya tiene su propio handler de 409. Invocar
 * {@code handleQuoteIntegrity} a mano demuestra que ese metodo devuelve 500,
 * pero no demuestra que Spring lo elija: eso depende de que siga siendo el
 * mapeo mas especifico. Se comprueba con el mismo
 * {@code ExceptionHandlerMethodResolver} que usa Spring MVC.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — datos estructurados, 503 de plataforma y 500 de corrupcion")
class GlobalExceptionHandlerStructuredProblemTest {

    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Tracer tracer;

    @InjectMocks
    private GlobalExceptionHandler handler;

    /**
     * Una peticion con contexto de observacion colgado, que es lo que el filtro de
     * Spring deja puesto en produccion. Sin el, {@code markObservationError} es un
     * no-op y el test no podria distinguir "marca el error" de "no hace nada".
     */
    private static ServerRequestObservationContext observacionDe(MockHttpServletRequest request) {
        ServerRequestObservationContext context = new ServerRequestObservationContext(request,
                new MockHttpServletResponse());
        request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE,
                context);
        return context;
    }

    @Nested
    @DisplayName("ciclo de dependencias del catalogo")
    class CicloDeDependencias {

        @Test
        @DisplayName("el ciclo viaja como lista de ids, no solo dentro del mensaje")
        void el_ciclo_viaja_como_lista_de_ids() {
            List<Long> ciclo = List.of(12L, 44L, 12L);

            ProblemDetail pd = handler.handleCatalogItemDependencyCycle(
                    new CatalogItemDependencyCycleException(ciclo));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "CATALOG_ITEM_DEPENDENCY_CYCLE");
            assertThat(pd.getProperties()).containsEntry("cycle", ciclo);
        }

        @Test
        @DisplayName("el ciclo es una lista y no el texto del mensaje ya formateado")
        void el_ciclo_es_una_lista_y_no_texto() {
            ProblemDetail pd = handler.handleCatalogItemDependencyCycle(
                    new CatalogItemDependencyCycleException(List.of(7L, 9L, 7L)));

            // Degradarlo a String es el atajo que rompe al front sin romper ningun
            // status: el mensaje ya lleva "7 > 9 > 7" y parece redundante publicarlo
            // dos veces.
            assertThat(pd.getProperties().get("cycle")).isInstanceOf(List.class)
                    .isNotInstanceOf(String.class);
        }

        @Test
        @DisplayName("el orden del recorrido se conserva: es el camino, no un conjunto")
        void el_orden_del_recorrido_se_conserva() {
            ProblemDetail pd = handler.handleCatalogItemDependencyCycle(
                    new CatalogItemDependencyCycleException(List.of(3L, 1L, 2L, 3L)));

            // List.equals es sensible al orden, asi que containsEntry ya fija el camino
            // completo: 3 -> 1 -> 2 -> 3 no es lo mismo que 3 -> 2 -> 1 -> 3.
            assertThat(pd.getProperties()).containsEntry("cycle", List.of(3L, 1L, 2L, 3L));
        }
    }

    @Nested
    @DisplayName("pago sobre-aplicado")
    class SobreAplicado {

        @Test
        @DisplayName("el disponible y lo pedido salen como propiedades, no solo en el texto")
        void el_disponible_y_lo_pedido_salen_como_propiedades() {
            ProblemDetail pd = handler.handleOverAppliedSource(
                    new OverAppliedSourceException(ApplicationSourceKind.PAYMENT, 88L,
                            new BigDecimal("100000.00"), new BigDecimal("150000.00")));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "SOURCE_OVER_APPLIED");
            assertThat(pd.getProperties()).containsEntry("available", new BigDecimal("100000.00"));
            assertThat(pd.getProperties()).containsEntry("requested", new BigDecimal("150000.00"));
        }

        @Test
        @DisplayName("los importes conservan su escala: siguen siendo dinero, no un double")
        void los_importes_conservan_su_escala() {
            ProblemDetail pd = handler.handleOverAppliedSource(
                    new OverAppliedSourceException(ApplicationSourceKind.CREDIT_NOTE, 4L,
                            new BigDecimal("0.10"), new BigDecimal("0.20")));

            // equals() de BigDecimal es sensible a la escala: si alguien pasa por
            // double o por long de centavos, "0.10" deja de ser igual a 0.10 y esto
            // cae. Es justo el defecto que se quiere cazar.
            assertThat(pd.getProperties().get("available")).isEqualTo(new BigDecimal("0.10"));
            assertThat(pd.getProperties().get("requested")).isEqualTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("una nota credito sobre-aplicada usa el mismo codigo que un pago")
        void una_nota_credito_usa_el_mismo_codigo() {
            ProblemDetail pd = handler.handleOverAppliedSource(new OverAppliedSourceException(
                    ApplicationSourceKind.CREDIT_NOTE, 5L, BigDecimal.ONE, BigDecimal.TEN));

            assertThat(pd.getProperties()).containsEntry("code", "SOURCE_OVER_APPLIED");
        }
    }

    @Nested
    @DisplayName("plataforma sin configurar")
    class PlataformaSinConfigurar {

        @Test
        @DisplayName("la facturacion sin sembrar es 503 y trae el INSERT que la arregla")
        void la_facturacion_sin_sembrar_es_503_con_su_insert() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handlePlatformBillingConfigNotConfigured(
                    new PlatformBillingConfigNotConfiguredException(), request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "PLATFORM_BILLING_CONFIG_NOT_CONFIGURED");
            // El mensaje llega INTEGRO: el INSERT es lo unico que separa un 503 opaco
            // de uno que alguien puede cerrar sin abrir el codigo.
            assertThat(pd.getDetail()).contains("INSERT INTO platform_billing_config")
                    .contains("default_grace_days").contains("invoice_day_of_month");
        }

        @Test
        @DisplayName("el catalogo comercial sin sembrar es 503 y trae los cinco pasos")
        void el_catalogo_sin_sembrar_es_503_con_sus_cinco_pasos() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handlePlatformNotConfigured(
                    new PlatformCatalogNotConfiguredException("Clinica Norte"), request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(pd.getProperties()).containsEntry("code", "PLATFORM_CATALOG_NOT_CONFIGURED");
            assertThat(pd.getDetail()).contains("Clinica Norte").contains("(1)").contains("(5)")
                    .contains("platform_billing_config");
        }

        @Test
        @DisplayName("la facturacion sin sembrar cuenta como peticion fallida en las metricas")
        void la_facturacion_sin_sembrar_cuenta_como_peticion_fallida() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            ServerRequestObservationContext observacion = observacionDe(request);
            PlatformBillingConfigNotConfiguredException fallo = new PlatformBillingConfigNotConfiguredException();

            handler.handlePlatformBillingConfigNotConfigured(fallo, request);

            // Es un despliegue incompleto: tiene que contar como request fallido y no
            // diluirse entre los 4xx normales, que es lo que pasaria sin esta marca.
            assertThat(observacion.getError()).isSameAs(fallo);
        }

        @Test
        @DisplayName("el catalogo de roles sin sembrar es 503 con su propio errorCode")
        void el_catalogo_de_roles_sin_sembrar_es_503_con_codigo_propio() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handlePlatformRoleCatalogNotConfigured(
                    new PlatformRoleCatalogNotConfiguredException("Clinica Norte"), request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            // errorCode PROPIO y no el compartido de las dos de catalogo comercial: se
            // arregla en base_roles, que es otra tabla y otro changeset. Compartirlo
            // mandaria a sembrar un catalogo comercial que puede estar perfecto.
            assertThat(pd.getProperties()).containsEntry("code",
                    "PLATFORM_ROLE_CATALOG_NOT_CONFIGURED");
            assertThat(pd.getDetail()).contains("Clinica Norte").contains("base_roles")
                    .contains("ADMIN");
        }

        @Test
        @DisplayName("con roles base pero ninguno obligatorio el mensaje dice que la tabla no está vacía")
        void con_roles_pero_ninguno_obligatorio_el_mensaje_lo_distingue() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handlePlatformRoleCatalogNotConfigured(
                    new PlatformRoleCatalogNotConfiguredException("Clinica Sur",
                            java.util.List.of("VET", "RECEP")),
                    request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(pd.getDetail()).contains("Clinica Sur").contains("VET").contains("RECEP")
                    .contains("mandatory");
        }

        @Test
        @DisplayName("los roles sin sembrar cuentan como peticion fallida en las metricas")
        void los_roles_sin_sembrar_cuentan_como_peticion_fallida() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            ServerRequestObservationContext observacion = observacionDe(request);
            PlatformRoleCatalogNotConfiguredException fallo = new PlatformRoleCatalogNotConfiguredException(
                    "Clinica Sur");

            handler.handlePlatformRoleCatalogNotConfigured(fallo, request);

            assertThat(observacion.getError()).isSameAs(fallo);
        }

        @Test
        @DisplayName("el catalogo sin sembrar cuenta como peticion fallida en las metricas")
        void el_catalogo_sin_sembrar_cuenta_como_peticion_fallida() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            ServerRequestObservationContext observacion = observacionDe(request);
            PlatformCatalogNotConfiguredException fallo = new PlatformCatalogNotConfiguredException(
                    "Clinica Sur");

            handler.handlePlatformNotConfigured(fallo, request);

            assertThat(observacion.getError()).isSameAs(fallo);
        }

        @Test
        @DisplayName("un contrato que no encaja sobre un catalogo que si existe sigue siendo 409")
        void el_contrato_que_no_encaja_sigue_siendo_409() {
            ProblemDetail pd = handler.handlePlatformCatalogNotConfiguredForSubscription(
                    new PlatformCatalogNotConfiguredForSubscriptionException(42L));

            // Misma familia y MISMO errorCode que el 503 de arriba, por decision de
            // producto, pero distinto status: aqui el catalogo minimo si existe. Se fija
            // aqui porque las dos mitades se leen a 1.000 lineas de distancia y unificar
            // los status "por coherencia" es el error natural.
            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "PLATFORM_CATALOG_NOT_CONFIGURED");
        }
    }

    @Nested
    @DisplayName("autochequeos de corrupcion de una cotizacion")
    class CorrupcionDeCotizacion {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.infrastructure.web.GlobalExceptionHandlerStructuredProblemTest#corrupcionDeCotizacion")
        @DisplayName("es 500 y no 409: el cliente no puede hacer nada al respecto")
        void es_500_y_no_409(String caso, IllegalStateException fallo) {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handleQuoteIntegrity(fallo, request);

            // 500 y no 409 aunque las dos hereden de IllegalStateException: un 409 le
            // dice al cliente que reintente, y el reintento vuelve a fallar igual
            // porque no hay nada que el cliente pueda hacer con un documento corrupto.
            assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(pd.getProperties()).containsEntry("code", "QUOTE_DATA_CORRUPTED");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.infrastructure.web.GlobalExceptionHandlerStructuredProblemTest#corrupcionDeCotizacion")
        @DisplayName("cuenta como peticion fallida: pide que un humano mire la fila")
        void cuenta_como_peticion_fallida(String caso, IllegalStateException fallo) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            ServerRequestObservationContext observacion = observacionDe(request);

            handler.handleQuoteIntegrity(fallo, request);

            assertThat(observacion.getError()).isSameAs(fallo);
        }

        @Test
        @DisplayName("el detalle no le filtra al cliente los importes que no cuadran")
        void el_detalle_no_filtra_los_importes() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            ProblemDetail pd = handler.handleQuoteIntegrity(new QuoteTotalsMismatchException(
                    "grandTotal", new BigDecimal("999999.99"), new BigDecimal("11.11")), request);

            assertThat(pd.getDetail()).doesNotContain("999999.99").doesNotContain("11.11")
                    .doesNotContain("stored").contains("soporte");
        }

        @Test
        @DisplayName("Spring elige el handler de 500 aunque la excepcion sea un IllegalState")
        void spring_elige_el_handler_de_500() {
            ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(
                    GlobalExceptionHandler.class);

            Method paraTotales = resolver.resolveMethod(
                    new QuoteTotalsMismatchException("total", BigDecimal.ONE, BigDecimal.TEN));
            Method paraLinea = resolver.resolveMethod(
                    new QuoteLineArithmeticException("lineTotal", BigDecimal.ONE, BigDecimal.TEN));

            assertThat(paraTotales.getName()).isEqualTo("handleQuoteIntegrity");
            assertThat(paraLinea.getName()).isEqualTo("handleQuoteIntegrity");
        }

        @Test
        @DisplayName("un IllegalState cualquiera sigue cayendo en el 409 generico")
        void un_illegal_state_cualquiera_sigue_siendo_409() {
            ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(
                    GlobalExceptionHandler.class);

            Method generico = resolver.resolveMethod(new IllegalStateException("cualquier cosa"));

            // La otra mitad del test anterior: si esta cae, es que el mapeo especifico
            // se ensancho y ahora se traga estados de negocio legitimos como 500.
            assertThat(generico.getName()).isEqualTo("handleConflictState");
        }
    }

    static Stream<Arguments> faltaDeConfiguracion() {
        return Stream.of(
                Arguments.of("facturacion de plataforma",
                        new PlatformBillingConfigNotConfiguredException()),
                Arguments.of("catalogo comercial",
                        new PlatformCatalogNotConfiguredException("Clinica Sur")));
    }

    static Stream<Arguments> corrupcionDeCotizacion() {
        return Stream.of(
                Arguments.of("totales de cabecera contra la suma de lineas",
                        new QuoteTotalsMismatchException("subtotal", new BigDecimal("100.00"),
                                new BigDecimal("90.00"))),
                Arguments.of("aritmetica congelada de una linea", new QuoteLineArithmeticException(
                        "lineTotal", new BigDecimal("50.00"), new BigDecimal("49.00"))));
    }
}
