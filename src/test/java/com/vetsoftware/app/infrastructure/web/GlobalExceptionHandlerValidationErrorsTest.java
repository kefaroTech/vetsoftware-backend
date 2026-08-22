package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableValidator;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los <b>cuatro</b> caminos que hoy desembocan en {@code validationProblem} —el
 * constructor unico del 400 de validacion— y la forma que los cuatro deben
 * emitir: {@code code} {@code VALIDATION_FAILED}, el mismo {@code detail}, el
 * {@code traceId} del span vivo y una lista {@code errors} de pares
 * {@code {field, message}} con <b>una sola entrada por campo</b>.
 *
 * <p>
 * Por que en un fichero propio y no dentro de
 * {@code GlobalExceptionHandlerTest} (@WebMvcTest): tres de los cuatro caminos
 * no se pueden provocar hoy con una peticion real. El de
 * {@code HandlerMethodValidationException} no tiene ni un solo
 * {@code @RequestParam} con restricciones en todo el repositorio que lo
 * dispare, y el de {@code ConstraintViolationException} exige un controller
 * anotado con {@code @Validated} que tampoco existe. Invocando los
 * {@code handleX} directamente —son de este mismo paquete— se ejercitan de
 * verdad en vez de quedar comprobados solo por compilacion.
 *
 * <p>
 * El criterio transversal que sujeta este fichero, y que ninguna revision
 * humana puede garantizar de forma duradera: <b>ningun mensaje que sale por
 * aqui nombra una clase Java, una tabla, una columna ni una expresion
 * regular</b>. El unico dato interno que si se publica a proposito son los
 * valores admitidos de un enum, que son contrato publicado en
 * {@code api/openapi.json} y lo unico accionable que se le puede decir al
 * usuario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — los cuatro caminos del 400 de validacion")
class GlobalExceptionHandlerValidationErrorsTest {

    /** El detalle unico de los cuatro caminos. */
    private static final String DETALLE_UNICO = "La información enviada no es válida. Revisa los campos marcados.";

    /**
     * El mensaje cuando el tipo destino no es un enum, o tiene demasiados valores.
     */
    private static final String MENSAJE_GENERICO = "El valor enviado no es válido para este campo.";

    /** El mensaje de relleno cuando el validador no dio ninguno. */
    private static final String MENSAJE_DE_RELLENO = "El valor enviado no es válido.";

    /**
     * Lo que un mensaje publicado no puede contener nunca. Tres familias, y cada
     * una tuvo su fuga real: nombres de tipo Java (el {@code detail} de serie de
     * Spring para un type mismatch dice
     * {@code Failed to convert value of type 'java.lang.String'…}), identificadores
     * de esquema en snake_case (el {@code Duplicate entry … for key 'uk_owner_doc'}
     * del driver) y metacaracteres de expresion regular (el mensaje por defecto de
     * {@code @Pattern} es {@code must match "{regexp}"}, que publica la regex
     * entera).
     */
    private static final Pattern FUGA_INTERNA = Pattern
            .compile("\\b(java|jakarta|com|org|tools)\\.[a-z]"
                    + "|\\b[A-Z][A-Za-z]*(Exception|Request|Response|Entity|Dto|Command|Repository|Service|Controller|Json)\\b"
                    + "|\\b(BigDecimal|LocalDate|LocalDateTime|Long|Integer|Boolean|String|Class)\\b"
                    + "|\\b[a-z]{2,}_[a-z_]{2,}\\b"
                    + "|\\\\[dwsDWSbB]|\\[A-Za-z|\\[0-9|\\(\\?[:=!]|\\^|\\$|\\{\\d+,");

    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;

    @InjectMocks
    private GlobalExceptionHandler handler;

    // -----------------------------------------------------------------------------------------
    // 1. Enum en el cuerpo (#326): muere en el deserializador, antes de Bean
    // Validation.
    // -----------------------------------------------------------------------------------------

    @Nested
    @DisplayName("EnumEnElCuerpo — un valor de enum que Jackson no supo convertir (#326)")
    class EnumEnElCuerpo {

        @Test
        @DisplayName("el field es el nombre del componente del record, nunca la clase Java")
        void el_field_es_el_componente_del_record() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"gender\":\"NINGUNO\"}", MascotaJson.class));

            List<Map<String, String>> errores = erroresDe(cuerpoDe(respuesta));
            assertThat(errores).singleElement().extracting(error -> error.get("field"))
                    .isEqualTo("gender");
        }

        @Test
        @DisplayName("el mensaje enumera los valores admitidos: es contrato publicado y accionable")
        void el_mensaje_enumera_los_valores_admitidos() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"gender\":\"NINGUNO\"}", MascotaJson.class));

            assertThat(mensajeUnico(cuerpoDe(respuesta)))
                    .isEqualTo("El valor enviado no es válido. Valores admitidos: MACHO, HEMBRA.");
        }

        @Test
        @DisplayName("ni el field ni el mensaje nombran la clase del record ni la del enum")
        void ni_el_field_ni_el_mensaje_nombran_clases_java() {
            ProblemDetail pd = cuerpoDe(
                    manejar(cuerpoIlegible("{\"gender\":\"NINGUNO\"}", MascotaJson.class)));

            assertThat(erroresDe(pd))
                    .allSatisfy(GlobalExceptionHandlerValidationErrorsTest::sinFugaInterna);
            assertThat(pd.getDetail()).isEqualTo(DETALLE_UNICO);
        }

        @Test
        @DisplayName("un anidado usa la sintaxis de Bean Validation: lines[0].quantity")
        void un_anidado_usa_la_sintaxis_de_bean_validation() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"lines\":[{\"quantity\":\"muchos\"}]}", PedidoJson.class));

            assertThat(erroresDe(cuerpoDe(respuesta))).singleElement()
                    .extracting(error -> error.get("field")).isEqualTo("lines[0].quantity");
        }

        @Test
        @DisplayName("un tipo que no es enum cae al mensaje generico, sin nombrar el tipo")
        void un_tipo_que_no_es_enum_cae_al_mensaje_generico() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"lines\":[{\"quantity\":\"muchos\"}]}", PedidoJson.class));

            assertThat(mensajeUnico(cuerpoDe(respuesta))).isEqualTo(MENSAJE_GENERICO);
        }

        @Test
        @DisplayName("un elemento de una lista raiz no arrastra el indice sin nombre delante")
        void un_elemento_de_lista_raiz_no_arrastra_el_indice() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("[{\"quantity\":\"muchos\"}]", LineaJson[].class));

            assertThat(erroresDe(cuerpoDe(respuesta))).singleElement()
                    .extracting(error -> error.get("field")).isEqualTo("quantity");
        }

        @Test
        @DisplayName("un enum con mas de 12 valores cae al generico: el volcado deja de ser ayuda")
        void un_enum_con_mas_de_doce_valores_cae_al_generico() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"tipo\":\"NINGUNO\"}", DocumentoJson.class));

            assertThat(mensajeUnico(cuerpoDe(respuesta))).isEqualTo(MENSAJE_GENERICO);
            assertThat(mensajeUnico(cuerpoDe(respuesta))).doesNotContain("FACTURA_VENTA");
        }

        @Test
        @DisplayName("el enum del tope justo (12 valores) todavia se enumera")
        void el_enum_del_tope_justo_todavia_se_enumera() {
            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"mes\":\"NINGUNO\"}", PeriodoJson.class));

            assertThat(mensajeUnico(cuerpoDe(respuesta))).contains("Valores admitidos: ENE",
                    "DIC.");
        }

        @Test
        @DisplayName("JSON sintacticamente roto conserva MALFORMED_REQUEST y NO lleva errors")
        void json_roto_conserva_malformed_request_sin_errors() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "no se pudo leer", parseRoto("{\"gender\":"), new CuerpoVacio());

            ProblemDetail pd = cuerpoDe(manejar(ex));

            assertThat(pd.getProperties()).containsEntry("code", "MALFORMED_REQUEST")
                    .doesNotContainKey("errors");
        }

        @Test
        @DisplayName("un cuerpo ilegible sin causa tampoco inventa un campo que marcar")
        void un_cuerpo_ilegible_sin_causa_no_inventa_campo() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "cuerpo ausente", new CuerpoVacio());

            ProblemDetail pd = cuerpoDe(manejar(ex));

            assertThat(pd.getProperties()).containsEntry("code", "MALFORMED_REQUEST")
                    .doesNotContainKey("errors");
        }

        @Test
        @DisplayName("responde 400 con VALIDATION_FAILED y el traceId del span vivo")
        void responde_400_con_validation_failed_y_trace_id() {
            darSpanVivo("4f1d2c3b4a59687776655443322110ff");

            ResponseEntity<Object> respuesta = manejar(
                    cuerpoIlegible("{\"gender\":\"NINGUNO\"}", MascotaJson.class));

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(cuerpoDe(respuesta).getProperties())
                    .containsEntry("code", "VALIDATION_FAILED")
                    .containsEntry("traceId", "4f1d2c3b4a59687776655443322110ff");
        }

        private ResponseEntity<Object> manejar(HttpMessageNotReadableException ex) {
            return handler.handleHttpMessageNotReadable(ex, new HttpHeaders(),
                    HttpStatus.BAD_REQUEST, peticion());
        }
    }

    // -----------------------------------------------------------------------------------------
    // 2. Parametros validados (#327): HandlerMethodValidationException, sin
    // BindingResult.
    // -----------------------------------------------------------------------------------------

    @Nested
    @DisplayName("ParametrosValidados — HandlerMethodValidationException (#327)")
    class ParametrosValidados {

        @Test
        @DisplayName("con ParameterErrors el campo es la propiedad del objeto de formulario")
        void con_parameter_errors_el_campo_es_la_propiedad() throws Exception {
            BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(),
                    "filtro");
            binding.addError(new FieldError("filtro", "desde", "debe ser una fecha pasada"));
            ParameterErrors errores = new ParameterErrors(
                    parametro("objetoDeFormulario", Object.class), new Object(), binding, null,
                    null, null);

            ProblemDetail pd = cuerpoDe(manejar(errores));

            assertThat(erroresDe(pd)).singleElement()
                    .isEqualTo(Map.of("field", "desde", "message", "debe ser una fecha pasada"));
        }

        @ParameterizedTest(name = "{0} → field {1}")
        @DisplayName("el nombre lo pone la anotacion de binding, nunca el metodo ni la clase")
        @CsvSource({"conNombreEnLaAnotacion, estado", "conNombreSoloEnElAtributo, desde",
                "conVariableDeRuta, animalId", "conCabecera, X-Sede"})
        void el_nombre_lo_pone_la_anotacion(String metodo, String campoEsperado) throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro(metodo, String.class), "no es válido")));

            assertThat(erroresDe(pd)).singleElement().extracting(error -> error.get("field"))
                    .isEqualTo(campoEsperado);
        }

        @Test
        @DisplayName("sin anotacion, el nombre sale del codigo fuente gracias a -parameters")
        void sin_anotacion_el_nombre_sale_de_parameters() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro("sinAnotacion", String.class), "no es válido")));

            assertThat(erroresDe(pd)).singleElement().extracting(error -> error.get("field"))
                    .isEqualTo("pageSize");
        }

        @Test
        @DisplayName("un error del formulario sin propiedad a la que anclar no viaja en la lista")
        void un_error_sin_propiedad_no_viaja() throws Exception {
            BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(),
                    "filtro");
            binding.addError(new FieldError("filtro", "", "el filtro completo no es coherente"));
            ParameterErrors errores = new ParameterErrors(
                    parametro("objetoDeFormulario", Object.class), new Object(), binding, null,
                    null, null);

            ProblemDetail pd = cuerpoDe(manejar(errores));

            assertThat(erroresDe(pd)).isEmpty();
            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        }

        @Test
        @DisplayName("dos violaciones del mismo parametro se funden en UNA entrada: el front indexa por field")
        void dos_violaciones_del_mismo_parametro_se_funden_en_una() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro("conNombreEnLaAnotacion", String.class),
                            "no debe estar vacío", "debe tener al menos 8 caracteres")));

            assertThat(erroresDe(pd)).hasSize(1);
            assertThat(mensajeUnico(pd)).contains("no debe estar vacío")
                    .contains("debe tener al menos 8 caracteres");
        }

        @Test
        @DisplayName("un mensaje repetido no se concatena consigo mismo")
        void un_mensaje_repetido_no_se_concatena() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro("conNombreEnLaAnotacion", String.class),
                            "no es válido", "no es válido")));

            assertThat(mensajeUnico(pd)).isEqualTo("no es válido");
        }

        @Test
        @DisplayName("un validador que no dio mensaje cae al texto de relleno, no a un hueco")
        void un_validador_sin_mensaje_cae_al_relleno() throws Exception {
            ParameterValidationResult sinMensaje = new ParameterValidationResult(
                    parametro("conNombreEnLaAnotacion", String.class), "valor",
                    List.of(new DefaultMessageSourceResolvable("codigo.sin.mensaje")), null, null,
                    null, (error, tipo) -> error);

            assertThat(mensajeUnico(cuerpoDe(manejar(sinMensaje)))).isEqualTo(MENSAJE_DE_RELLENO);
        }

        @Test
        @DisplayName("responde 400 con VALIDATION_FAILED, el detalle unico y el traceId del span vivo")
        void responde_400_con_la_forma_unica() throws Exception {
            darSpanVivo("aa11bb22cc33dd44ee55ff6677889900");

            ResponseEntity<Object> respuesta = manejar(
                    resultado(parametro("conNombreEnLaAnotacion", String.class), "no es válido"));

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ProblemDetail pd = cuerpoDe(respuesta);
            assertThat(pd.getDetail()).isEqualTo(DETALLE_UNICO);
            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED")
                    .containsEntry("traceId", "aa11bb22cc33dd44ee55ff6677889900");
        }

        @Test
        @DisplayName("varios parametros invalidos producen una entrada por cada uno, en orden")
        void varios_parametros_producen_una_entrada_cada_uno() throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(
                    resultado(parametro("conNombreEnLaAnotacion", String.class), "no es válido"),
                    resultado(parametro("conVariableDeRuta", String.class), "no es válido")));

            assertThat(erroresDe(pd)).extracting(error -> error.get("field"))
                    .containsExactly("estado", "animalId");
        }

        private ResponseEntity<Object> manejar(ParameterValidationResult... resultados) {
            return handler.handleHandlerMethodValidationException(excepcion(resultados),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, peticion());
        }
    }

    // -----------------------------------------------------------------------------------------
    // 2 bis. La MISMA validacion de parametros, con el proxy adaptando las
    // violaciones (#330): con spring.validation.method.adapt-constraint-violations
    // en true, Spring lanza su MethodValidationException en vez de la de Jakarta.
    // Estos casos existen para que el VALOR DE ESA PROPIEDAD NO CAMBIE LA
    // RESPUESTA:
    // si alguien la activa dentro de seis meses por un motivo ajeno, no puede
    // reabrir el 500 de #327 sin que el build se entere.
    // -----------------------------------------------------------------------------------------

    @Nested
    @DisplayName("ParametrosValidadosAdaptados — MethodValidationException (#330)")
    class ParametrosValidadosAdaptados {

        @Test
        @DisplayName("da exactamente la misma respuesta que sin adaptar: el flag deja de importar")
        void da_exactamente_la_misma_respuesta_que_sin_adaptar() throws Exception {
            ParameterValidationResult resultado = resultado(
                    parametro("conNombreEnLaAnotacion", String.class),
                    "El valor mínimo permitido es 1.");

            ProblemDetail adaptado = cuerpoDe(manejar(resultado));
            ProblemDetail sinAdaptar = cuerpoDe(handler.handleHandlerMethodValidationException(
                    excepcion(resultado), new HttpHeaders(), HttpStatus.BAD_REQUEST, peticion()));

            assertThat(adaptado.getStatus()).isEqualTo(sinAdaptar.getStatus());
            assertThat(adaptado.getDetail()).isEqualTo(sinAdaptar.getDetail());
            assertThat(adaptado.getProperties()).isEqualTo(sinAdaptar.getProperties());
        }

        @Test
        @DisplayName("responde 400 aunque Spring proponga el 500 que era el defecto de #327")
        void responde_400_aunque_spring_proponga_500() throws Exception {
            ResponseEntity<Object> respuesta = handler.handleMethodValidationException(
                    adaptada(resultado(parametro("conNombreEnLaAnotacion", String.class),
                            "no es válido")),
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, peticion());

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(cuerpoDe(respuesta).getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("el field sigue siendo el parametro, y el mensaje no publica interioridades")
        void el_field_sigue_siendo_el_parametro() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro("conVariableDeRuta", String.class),
                            "El valor mínimo permitido es 1.")));

            assertThat(erroresDe(pd)).singleElement().isEqualTo(
                    Map.of("field", "animalId", "message", "El valor mínimo permitido es 1."));
            assertThat(erroresDe(pd))
                    .allSatisfy(GlobalExceptionHandlerValidationErrorsTest::sinFugaInterna);
        }

        @Test
        @DisplayName("dos violaciones del mismo parametro se funden en UNA entrada, como en el resto")
        void dos_violaciones_del_mismo_parametro_dan_una_entrada() throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(resultado(parametro("sinAnotacion", String.class),
                    "El valor mínimo permitido es 1.", "Debe tener entre 1 y 200 caracteres.")));

            assertThat(erroresDe(pd)).singleElement()
                    .satisfies(error -> assertThat(error.get("field")).isEqualTo("pageSize"));
        }

        @Test
        @DisplayName("lleva el code, el detalle unico y el traceId que el 500 anterior no daba")
        void lleva_el_code_el_detalle_y_el_trace_id() throws Exception {
            darSpanVivo("aabbccddeeff00112233445566778899");

            ProblemDetail pd = cuerpoDe(
                    manejar(resultado(parametro("sinAnotacion", String.class), "no es válido")));

            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED")
                    .containsEntry("traceId", "aabbccddeeff00112233445566778899");
            assertThat(pd.getDetail()).isEqualTo(DETALLE_UNICO);
        }

        private ResponseEntity<Object> manejar(ParameterValidationResult... resultados) {
            return handler.handleMethodValidationException(adaptada(resultados), new HttpHeaders(),
                    HttpStatus.BAD_REQUEST, peticion());
        }
    }

    // -----------------------------------------------------------------------------------------
    // 3. ConstraintViolation (#327): el 500 que era un 400.
    // -----------------------------------------------------------------------------------------

    @Nested
    @DisplayName("ConstraintViolation — @Validated en la clase del controller (#327)")
    class ConstraintViolationDelProxy {

        @Test
        @DisplayName("responde 400 y no 500: es un error del cliente, no una caida del servidor")
        void responde_400_y_no_500() throws Exception {
            ProblemDetail pd = handler
                    .handleConstraintViolation(violacionesDe("listAll", int.class, -1));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED");
            assertThat(pd.getDetail()).isEqualTo(DETALLE_UNICO);
        }

        @Test
        @DisplayName("el path listAll.page se reduce a page: el nombre del metodo Java no se publica")
        void el_path_se_reduce_al_ultimo_tramo_nombrado() throws Exception {
            ProblemDetail pd = handler
                    .handleConstraintViolation(violacionesDe("listAll", int.class, -1));

            assertThat(erroresDe(pd)).singleElement().extracting(error -> error.get("field"))
                    .isEqualTo("page");
        }

        @Test
        @DisplayName("dos restricciones sobre el mismo parametro dan UNA sola entrada")
        void dos_restricciones_sobre_el_mismo_parametro_dan_una_entrada() throws Exception {
            ProblemDetail pd = handler
                    .handleConstraintViolation(violacionesDe("buscar", String.class, ""));

            assertThat(erroresDe(pd)).hasSize(1);
            assertThat(erroresDe(pd)).singleElement().extracting(error -> error.get("field"))
                    .isEqualTo("texto");
        }

        @Test
        @DisplayName("una excepcion sin violaciones sigue siendo un 400 con lista vacia")
        void sin_violaciones_sigue_siendo_400_con_lista_vacia() {
            ProblemDetail pd = handler
                    .handleConstraintViolation(new ConstraintViolationException(Set.of()));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(erroresDe(pd)).isEmpty();
        }

        @Test
        @DisplayName("ningun mensaje del validador se publica nombrando clase, tabla, columna ni regex")
        void ningun_mensaje_publica_interioridades() throws Exception {
            ProblemDetail pd = handler
                    .handleConstraintViolation(violacionesDe("buscar", String.class, ""));

            assertThat(erroresDe(pd))
                    .allSatisfy(GlobalExceptionHandlerValidationErrorsTest::sinFugaInterna);
        }

        @Test
        @DisplayName("lleva el traceId del span vivo, que es lo que el 500 anterior no daba")
        void lleva_el_trace_id_del_span_vivo() throws Exception {
            darSpanVivo("0011223344556677889900aabbccddee");

            ProblemDetail pd = handler
                    .handleConstraintViolation(violacionesDe("listAll", int.class, -1));

            assertThat(pd.getProperties()).containsEntry("traceId",
                    "0011223344556677889900aabbccddee");
        }
    }

    // -----------------------------------------------------------------------------------------
    // 4. Conversion imposible: enum en la query string, /animals/abc.
    // -----------------------------------------------------------------------------------------

    @Nested
    @DisplayName("ConversionImposible — el binding de un parametro o una variable de ruta")
    class ConversionImposible {

        @Test
        @DisplayName("un enum en la query string nombra el parametro y enumera los valores")
        void un_enum_en_la_query_string_enumera_los_valores() throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(mismatch("NINGUNO", Genero.class, "status")));

            assertThat(erroresDe(pd)).singleElement().isEqualTo(Map.of("field", "status", "message",
                    "El valor enviado no es válido. Valores admitidos: MACHO, HEMBRA."));
        }

        @Test
        @DisplayName("/animals/abc nombra el id y NO dice a que tipo Java se intentaba convertir")
        void animals_abc_no_dice_el_tipo_java() throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(mismatch("abc", Long.class, "id")));

            assertThat(erroresDe(pd)).singleElement()
                    .isEqualTo(Map.of("field", "id", "message", MENSAJE_GENERICO));
        }

        @ParameterizedTest(name = "requiredType {0}")
        @DisplayName("ningun tipo que no sea enum se nombra en el mensaje: siempre el generico")
        @MethodSource("com.vetsoftware.app.infrastructure.web.GlobalExceptionHandlerValidationErrorsTest#tiposQueNoSonEnum")
        void ningun_tipo_no_enum_se_nombra(Class<?> tipo) throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(mismatch("abc", tipo, "campo")));

            assertThat(mensajeUnico(pd)).isEqualTo(MENSAJE_GENERICO);
            sinFugaInterna(erroresDe(pd).getFirst());
        }

        @Test
        @DisplayName("sin tipo destino conocido tampoco se inventa nada")
        void sin_tipo_destino_conocido_no_se_inventa_nada() throws Exception {
            ProblemDetail pd = cuerpoDe(manejar(mismatch("abc", null, "campo")));

            assertThat(mensajeUnico(pd)).isEqualTo(MENSAJE_GENERICO);
        }

        @Test
        @DisplayName("un enum con mas de 12 valores en la query string cae al generico")
        void un_enum_grande_en_la_query_string_cae_al_generico() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(mismatch("NINGUNO", TipoDeDocumento.class, "tipo")));

            assertThat(mensajeUnico(pd)).isEqualTo(MENSAJE_GENERICO);
        }

        @Test
        @DisplayName("una constante de enum con cuerpo propio sigue enumerando los valores del enum")
        void una_constante_con_cuerpo_propio_sigue_enumerando() throws Exception {
            ProblemDetail pd = cuerpoDe(
                    manejar(mismatch("NINGUNO", Talla.PEQUENA.getClass(), "talla")));

            assertThat(mensajeUnico(pd)).isEqualTo(
                    "El valor enviado no es válido. Valores admitidos: PEQUENA, GRANDE.");
        }

        @Test
        @DisplayName("un TypeMismatch sin nombre de propiedad conserva MALFORMED_REQUEST sin errors")
        void sin_nombre_de_propiedad_conserva_malformed_request() {
            ProblemDetail pd = cuerpoDe(manejar(
                    new org.springframework.beans.TypeMismatchException("abc", Long.class)));

            assertThat(pd.getProperties()).containsEntry("code", "MALFORMED_REQUEST")
                    .doesNotContainKey("errors");
        }

        @Test
        @DisplayName("responde 400 con VALIDATION_FAILED, el detalle unico y el traceId del span vivo")
        void responde_400_con_la_forma_unica() throws Exception {
            darSpanVivo("ffeeddccbbaa99887766554433221100");

            ResponseEntity<Object> respuesta = manejar(mismatch("NINGUNO", Genero.class, "status"));

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ProblemDetail pd = cuerpoDe(respuesta);
            assertThat(pd.getDetail()).isEqualTo(DETALLE_UNICO);
            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED")
                    .containsEntry("traceId", "ffeeddccbbaa99887766554433221100");
        }

        private ResponseEntity<Object> manejar(org.springframework.beans.TypeMismatchException ex) {
            return handler.handleTypeMismatch(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                    peticion());
        }

        private MethodArgumentTypeMismatchException mismatch(Object valor, Class<?> tipo,
                String nombre) throws Exception {
            return new MethodArgumentTypeMismatchException(valor, tipo, nombre,
                    parametro("conNombreEnLaAnotacion", String.class), null);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Fixtures y ayudas. Nada de esto lleva logica de decision: solo construye lo
    // que
    // en produccion construyen Jackson, Spring MVC y Hibernate Validator.
    // -----------------------------------------------------------------------------------------

    static Stream<Class<?>> tiposQueNoSonEnum() {
        return Stream.of(Long.class, Integer.class, int.class, BigDecimal.class, LocalDate.class,
                LocalDateTime.class, Boolean.class);
    }

    private void darSpanVivo(String traceId) {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);
    }

    private static ServletWebRequest peticion() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private static ProblemDetail cuerpoDe(ResponseEntity<Object> respuesta) {
        return (ProblemDetail) respuesta.getBody();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> erroresDe(ProblemDetail pd) {
        return (List<Map<String, String>>) pd.getProperties().get("errors");
    }

    private static String mensajeUnico(ProblemDetail pd) {
        return erroresDe(pd).getFirst().get("message");
    }

    private static void sinFugaInterna(Map<String, String> error) {
        assertThat(error.get("message")).isNotBlank();
        assertThat(FUGA_INTERNA.matcher(error.get("field") + " " + error.get("message")).find())
                .as("lo publicado no puede nombrar clase Java, tabla, columna ni regex: %s", error)
                .isFalse();
    }

    /**
     * Un cuerpo ilegible construido con la excepcion REAL que produce Jackson 3.
     */
    private static HttpMessageNotReadableException cuerpoIlegible(String json, Class<?> tipo) {
        MismatchedInputException causa = catchThrowableOfType(MismatchedInputException.class,
                () -> new JsonMapper().readValue(json, tipo));
        return new HttpMessageNotReadableException("no se pudo leer", causa, new CuerpoVacio());
    }

    /**
     * El fallo de un JSON sintacticamente roto, que NO es un
     * MismatchedInputException.
     */
    private static Throwable parseRoto(String json) {
        return catchThrowableOfType(tools.jackson.core.JacksonException.class,
                () -> new JsonMapper().readValue(json, MascotaJson.class));
    }

    private static MethodParameter parametro(String metodo, Class<?> tipo) throws Exception {
        Method firma = FirmasDeControlador.class.getDeclaredMethod(metodo, tipo);
        MethodParameter parametro = new MethodParameter(firma, 0);
        parametro.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        return parametro;
    }

    private static ParameterValidationResult resultado(MethodParameter parametro,
            String... mensajes) {
        List<MessageSourceResolvable> errores = Arrays.stream(mensajes)
                .map(mensaje -> (MessageSourceResolvable) new DefaultMessageSourceResolvable(
                        new String[]{"codigo"}, null, mensaje))
                .toList();
        return new ParameterValidationResult(parametro, "valor", errores, null, null, null,
                (error, tipo) -> error);
    }

    private static HandlerMethodValidationException excepcion(
            ParameterValidationResult... resultados) {
        Method firma = firmaCualquiera();
        return new HandlerMethodValidationException(MethodValidationResult
                .create(new FirmasDeControlador(), firma, List.of(resultados)));
    }

    /**
     * El mismo resultado de validacion dentro del envoltorio que usa Spring cuando
     * el proxy adapta las violaciones. Que se construya con el mismo
     * {@code MethodValidationResult} que {@code excepcion(...)} no es comodidad del
     * test: es exactamente lo que hace que las dos formas puedan compartir
     * manejador.
     */
    private static MethodValidationException adaptada(ParameterValidationResult... resultados) {
        return new MethodValidationException(MethodValidationResult
                .create(new FirmasDeControlador(), firmaCualquiera(), List.of(resultados)));
    }

    private static Method firmaCualquiera() {
        return Arrays.stream(FirmasDeControlador.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().equals("sinAnotacion")).findFirst()
                .orElseThrow();
    }

    /**
     * Violaciones reales de Hibernate Validator sobre los parametros de un metodo:
     * es la unica forma de que el {@code Path} traiga el nodo METHOD delante del
     * PARAMETER, que es justo lo que {@code violatedPropertyName} tiene que saltar.
     */
    private static ConstraintViolationException violacionesDe(String metodo, Class<?> tipo,
            Object argumento) throws Exception {
        ListadoValidado destino = new ListadoValidado();
        Method firma = ListadoValidado.class.getDeclaredMethod(metodo, tipo);
        Set<ConstraintViolation<ListadoValidado>> violaciones = VALIDADOR
                .validateParameters(destino, firma, new Object[]{argumento});
        return new ConstraintViolationException(violaciones);
    }

    /**
     * Inmutable y thread-safe por contrato de Jakarta Validation; no es estado
     * compartido.
     */
    private static final ExecutableValidator VALIDADOR = Validation.buildDefaultValidatorFactory()
            .getValidator().forExecutables();

    /**
     * Firmas de las que colgar un {@code MethodParameter} real; no se invocan
     * nunca.
     */
    @SuppressWarnings("unused")
    static final class FirmasDeControlador {
        void conNombreEnLaAnotacion(@RequestParam("estado") String estado) {
        }

        void conNombreSoloEnElAtributo(@RequestParam(name = "desde") String argumento) {
        }

        void conVariableDeRuta(@PathVariable("animalId") String animalId) {
        }

        void conCabecera(@RequestHeader("X-Sede") String sede) {
        }

        void sinAnotacion(String pageSize) {
        }

        void objetoDeFormulario(Object filtro) {
        }
    }

    /**
     * El controller anotado con {@code @Validated} que el repositorio todavia no
     * tiene.
     */
    @SuppressWarnings("unused")
    static final class ListadoValidado {
        void listAll(@Min(0) int page) {
        }

        void buscar(@NotBlank @Size(min = 3) String texto) {
        }
    }

    /**
     * Cuerpo vacio: a {@code handleHttpMessageNotReadable} no le hace falta leerlo.
     */
    private static final class CuerpoVacio implements HttpInputMessage {
        @Override
        public InputStream getBody() {
            return InputStream.nullInputStream();
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }

    enum Genero {
        MACHO, HEMBRA
    }

    /** Doce valores: el tope exacto que todavia se enumera. */
    enum Mes {
        ENE, FEB, MAR, ABR, MAY, JUN, JUL, AGO, SEP, OCT, NOV, DIC
    }

    /** Trece valores: uno por encima del tope, cae al mensaje generico. */
    enum TipoDeDocumento {
        FACTURA_VENTA, NOTA_CREDITO, NOTA_DEBITO, FACTURA_COMPRA, RECIBO_CAJA, COMPROBANTE_EGRESO, NOTA_AJUSTE, DOCUMENTO_SOPORTE, NOMINA_ELECTRONICA, FACTURA_EXPORTACION, FACTURA_CONTINGENCIA, NOTA_CREDITO_SOPORTE, NOTA_DEBITO_SOPORTE
    }

    /**
     * Constantes con cuerpo propio: cada una es una subclase anonima que NO es
     * enum.
     */
    enum Talla {
        PEQUENA {
            @Override
            String etiqueta() {
                return "S";
            }
        },
        GRANDE {
            @Override
            String etiqueta() {
                return "L";
            }
        };

        abstract String etiqueta();
    }

    record MascotaJson(Genero gender) {
    }

    record LineaJson(int quantity) {
    }

    record PedidoJson(List<LineaJson> lines) {
    }

    record DocumentoJson(TipoDeDocumento tipo) {
    }

    record PeriodoJson(Mes mes) {
    }
}
