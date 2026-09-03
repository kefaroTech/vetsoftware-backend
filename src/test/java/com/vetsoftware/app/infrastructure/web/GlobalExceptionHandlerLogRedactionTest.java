package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Tracer;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * El gate de la incidencia #171: {@code GlobalExceptionHandler} no puede
 * escribir en el log el <b>dato de la fila</b> que provocó el error.
 *
 * <p>
 * <b>Qué se rompió y por qué volvería.</b> La incidencia #81 encontró
 * {@code log.warn("Data integrity violation: {}", ex.getMessage())}: el mensaje
 * de Hibernate arrastra la sentencia entera y el valor duplicado —el nombre de
 * un propietario, su documento o su correo—, así que una carrera de unicidad
 * publicaba dato personal en Grafana. Se arregló dejando solo
 * {@code constraint=} y {@code type=}, pero <b>nada impedía reintroducirlo</b>:
 * cero aserciones de log en este paquete, ninguna regla de arquitectura y
 * ningún Checkstyle. Volver a escribir {@code ex.getMessage()} en cualquiera de
 * estas dos ramas era un cambio de una línea que pasa la revisión.
 *
 * <p>
 * <b>Por qué no una regexp.</b> {@code GlobalExceptionHandler} tiene decenas de
 * llamadas {@code log.*} con {@code getMessage()} y casi todas son legítimas:
 * el mensaje de una excepción <em>de dominio</em> es una constante que
 * escribimos nosotros. Una regla por texto nacería en rojo y se desactivaría el
 * primer día. Lo que distingue las dos ramas peligrosas no es la forma de la
 * llamada sino el origen del mensaje —el driver de la base de datos, o el
 * binder con el payload del cliente dentro—, y eso solo se ve ejecutándolas.
 *
 * <p>
 * <b>La aserción es sobre el evento crudo, a propósito.</b> El
 * {@code ListAppender} cuelga directamente del logger de la clase, así que ve
 * lo que el handler emite <em>antes</em> de que {@code RedactingAppender} lo
 * toque. Es defensa en profundidad y no duplicidad: la redacción por patrones
 * no reconoce nombres propios ni prosa (ASVS V7.1.1), de modo que un
 * {@code Duplicate entry 'Mafalda Pérez'} la atraviesa entero. La única
 * garantía real es que el valor no entre en el mensaje.
 */
@DisplayName("GlobalExceptionHandler: lo que NO puede acabar en el log")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerLogRedactionTest {

    /** Valor de fila que jamás puede salir por el log. */
    private static final String VALOR_DE_LA_FILA = "MAFALDA-PEREZ-CENUELA";

    private static final String CONSTRAINT_MAPEADA = "uq_products_company_active_code";

    private static final String CONSTRAINT_SIN_MAPEO = "uq_ficticia_sin_mapeo_de_negocio";

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Tracer tracer;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private Logger canal;
    private ListAppender<ILoggingEvent> sumidero;
    private Level nivelPrevio;

    @BeforeEach
    void engancharElCanal() {
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        sumidero = new ListAppender<>();
        sumidero.setContext(context);
        sumidero.start();

        canal = context.getLogger(GlobalExceptionHandler.class);
        nivelPrevio = canal.getLevel();
        canal.setLevel(Level.INFO);
        canal.addAppender(sumidero);
    }

    @AfterEach
    void soltarElCanal() {
        canal.detachAppender(sumidero);
        canal.setLevel(nivelPrevio);
        sumidero.stop();
    }

    /**
     * El evento que emite el handler. Es el primero a propósito y no «el único»:
     * {@code ResponseEntityExceptionHandler} hereda su {@code Log} de
     * {@code getClass()}, así que comparte categoría con esta clase y podría añadir
     * los suyos por detrás. El del handler siempre va delante —se escribe antes de
     * delegar en {@code super}—, y donde la cuenta importa se afirma aparte.
     */
    private ILoggingEvent emitido() {
        assertThat(sumidero.list).as("el handler no emitio ningun evento de log").isNotEmpty();
        return sumidero.list.getFirst();
    }

    /**
     * Todo lo que el evento lleva escrito: el mensaje ya formateado con sus
     * argumentos. Es lo que acaba en Loki como cuerpo de la línea.
     */
    private String loEscrito() {
        return emitido().getFormattedMessage();
    }

    /**
     * El error del driver tal como llega: {@code DataIntegrityViolationException}
     * envolviendo la excepción de MySQL con el {@code Duplicate entry} dentro.
     */
    private static DataIntegrityViolationException duplicado(String constraint) {
        SQLIntegrityConstraintViolationException delDriver = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '" + VALOR_DE_LA_FILA + "' for key '" + constraint + "'", "23000",
                1062);
        return new DataIntegrityViolationException("could not execute statement [Duplicate entry '"
                + VALOR_DE_LA_FILA + "' for key '" + constraint + "'] [insert into products ...]",
                delDriver);
    }

    @Nested
    @DisplayName("violacion de integridad")
    class ViolacionDeIntegridad {

        @Test
        @DisplayName("la constraint mapeada se registra por su nombre y sin el valor duplicado")
        void la_constraint_mapeada_no_registra_el_valor_duplicado() {
            ProblemDetail respuesta = handler.handleDataIntegrity(duplicado(CONSTRAINT_MAPEADA));

            assertThat(respuesta.getProperties()).containsEntry("code",
                    "PRODUCT_CODE_ALREADY_EXISTS");
            assertThat(loEscrito()).contains("constraint=" + CONSTRAINT_MAPEADA)
                    .contains("type=DataIntegrityViolationException")
                    .doesNotContain(VALOR_DE_LA_FILA).doesNotContain("Duplicate entry")
                    .doesNotContain("insert into products");
            // INFO y no WARN: la rama mapeada devuelve un 409 atribuible al cliente, y
            // desde #89 los 4xx de dominio se emiten en INFO (el criterio esta escrito en
            // handleExceptionInternal). El WARN queda reservado a la rama sin mapeo, que
            // es la que si exige que alguien mire. Lo que este caso protege no es el
            // nivel, sino que el valor duplicado no entre en el mensaje.
            assertThat(emitido().getLevel()).isEqualTo(Level.INFO);
            assertThat(sumidero.list).as("una sola linea por violacion de integridad").hasSize(1);
        }

        /**
         * La rama sin mapeo es la que más tienta a loguear el mensaje entero —la
         * respuesta es genérica y este evento es el único rastro para poder mapear la
         * constraint después—, así que es la que más falta hacía cerrar.
         */
        @Test
        @DisplayName("la constraint sin mapeo tampoco registra el valor duplicado en el mensaje")
        void la_constraint_sin_mapeo_no_registra_el_valor_duplicado() {
            ProblemDetail respuesta = handler.handleDataIntegrity(duplicado(CONSTRAINT_SIN_MAPEO));

            assertThat(respuesta.getProperties()).containsEntry("code", "DATA_INTEGRITY_VIOLATION");
            assertThat(loEscrito()).contains("constraint=" + CONSTRAINT_SIN_MAPEO)
                    .doesNotContain(VALOR_DE_LA_FILA).doesNotContain("Duplicate entry");
        }

        /**
         * El throwable <b>sí</b> viaja en la rama sin mapeo, y es deliberado: sin él la
         * constraint nueva no se puede diagnosticar. Esa es exactamente la ruta que
         * {@code RedactedThrowable} redacta —{@code RedactingAppender} envuelve a todos
         * los appenders de la raíz en {@code logback-spring.xml}—, así que el contrato
         * que este caso fija es «la excepción sigue adjunta»; que su mensaje salga
         * enmascarado lo cubre {@code LogRedactionPipelineTest}. La diferencia con la
         * rama mapeada, que no adjunta nada, es la mitad del valor de la pareja.
         */
        @Test
        @DisplayName("la rama sin mapeo conserva la excepcion adjunta para el appender")
        void la_rama_sin_mapeo_conserva_la_excepcion_adjunta() {
            handler.handleDataIntegrity(duplicado(CONSTRAINT_SIN_MAPEO));

            assertThat(emitido().getThrowableProxy()).isNotNull();
        }

        @Test
        @DisplayName("la rama mapeada no adjunta la excepcion: basta la constraint")
        void la_rama_mapeada_no_adjunta_la_excepcion() {
            handler.handleDataIntegrity(duplicado(CONSTRAINT_MAPEADA));

            assertThat(emitido().getThrowableProxy()).isNull();
        }
    }

    @Nested
    @DisplayName("validacion de cuerpo")
    class ValidacionDeCuerpo {

        private MethodArgumentNotValidException conValorRechazado() {
            MethodParameter parametro = new MethodParameter(objetivoDeBinding(), 0);
            BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(),
                    "command");
            binding.addError(new FieldError("command", "reason", VALOR_DE_LA_FILA, false, null,
                    null, "size must be between 0 and 300"));
            return new MethodArgumentNotValidException(parametro, binding);
        }

        /**
         * {@code MethodArgumentNotValidException.getMessage()} incluye el
         * {@code rejected value}. El handler tiene que sacar el detalle por
         * {@code clientErrorDetail(...)} —que de un {@code BindException} publica solo
         * los nombres de campo, que son esquema— y nunca del mensaje.
         */
        @Test
        @DisplayName("el 4xx registra los campos con error y no el valor que el cliente tecleo")
        void el_4xx_no_registra_el_valor_rechazado() {
            ServletWebRequest peticion = new ServletWebRequest(new MockHttpServletRequest());

            handler.handleMethodArgumentNotValid(conValorRechazado(), new HttpHeaders(),
                    HttpStatus.BAD_REQUEST, peticion);

            assertThat(loEscrito()).contains("MethodArgumentNotValidException")
                    .contains("fields=[reason]").doesNotContain(VALOR_DE_LA_FILA)
                    .doesNotContain("rejected value");
            assertThat(emitido().getLevel()).isEqualTo(Level.INFO);
        }

        /**
         * La otra mitad de la fuga: el valor rechazado tampoco puede volver al cliente
         * dentro del {@code ProblemDetail}. Del error de campo se publican el nombre y
         * el mensaje de la restricción, nunca lo que se tecleó.
         */
        @Test
        @DisplayName("el cuerpo de la respuesta lleva campo y mensaje, no el valor rechazado")
        void el_cuerpo_de_la_respuesta_no_lleva_el_valor_rechazado() {
            ServletWebRequest peticion = new ServletWebRequest(new MockHttpServletRequest());

            ResponseEntity<Object> respuesta = handler.handleMethodArgumentNotValid(
                    conValorRechazado(), new HttpHeaders(), HttpStatus.BAD_REQUEST, peticion);

            ProblemDetail cuerpo = (ProblemDetail) respuesta.getBody();
            assertThat(String.valueOf(cuerpo.getProperties().get("errors"))).contains("reason")
                    .contains("size must be between 0 and 300").doesNotContain(VALOR_DE_LA_FILA);
        }
    }

    /**
     * Firma de la que colgar el {@link MethodParameter} que exige el constructor de
     * {@code MethodArgumentNotValidException}. No se invoca nunca.
     */
    private static java.lang.reflect.Method objetivoDeBinding() {
        return java.util.Arrays
                .stream(GlobalExceptionHandlerLogRedactionTest.class.getDeclaredMethods())
                .filter(m -> "objetivo".equals(m.getName())).findFirst().orElseThrow();
    }

    @SuppressWarnings("unused")
    private static void objetivo(String reason) {
    }
}
