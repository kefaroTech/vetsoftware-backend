package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;

/**
 * Contra-prueba de la allowlist: una allowlist demasiado estrecha no rompe nada
 * visiblemente, solo <b>ciega la auditoría en silencio</b> — los eventos siguen
 * llegando a Loki, pero con {@code ***} en los campos con los que se investiga
 * un incidente.
 *
 * <p>
 * Ejercita cada evento de {@link AuditLogger} y el contexto de request que lo
 * acompaña, y afirma que ningún campo que la auditoría emite a propósito sale
 * enmascarado. Añadir un {@code
 * addKeyValue(...)} nuevo sin declararlo en {@link LogFieldPolicy} rompe esta
 * prueba.
 *
 * <p>
 * <b>Cómo se impide la recaída.</b> Durante meses esta prueba llevó una lista
 * escrita a mano de invocaciones, y el fallo que tenía era el propio de toda
 * lista a mano: <em>no se queja de lo que le falta</em>. Dos eventos —entre
 * ellos {@code refresh_token_reuse_detected}, el único que describe un ataque
 * en curso— nunca se ejercitaron, así que sus campos podían salir enmascarados
 * en producción con la suite entera en verde. Por eso los casos viven ahora en
 * {@link #auditCases()}, una única fuente de verdad donde cada entrada declara
 * explícitamente <b>qué método de {@link AuditLogger} ejercita</b>, y
 * {@link #everyAuditMethodIsExercisedByACase()} cruza ese conjunto de nombres
 * contra el que devuelve la reflexión sobre la clase de producción. Un evento
 * nuevo en {@code AuditLogger} sin su entrada aquí ya no pasa desapercibido: la
 * paridad falla nombrando el método huérfano.
 *
 * @see LogRedactionPipelineTest
 */
class AuditFieldsSurviveRedactionTest {

    /**
     * Un evento de auditoría a ejercitar. {@code method} es el nombre del método de
     * {@link AuditLogger} que invoca {@code emit} — no es decorativo: es la clave
     * con la que {@link #everyAuditMethodIsExercisedByACase()} compara contra la
     * reflexión, y un nombre mal escrito convierte la paridad en un falso verde (de
     * ahí que esa prueba compruebe también el sentido contrario).
     */
    private record AuditCase(String method, String event, Consumer<AuditLogger> emit) {

        @Override
        public String toString() {
            return method + " → " + event;
        }
    }

    /**
     * Única fuente de verdad de esta prueba: la batería de casos sobre la que itera
     * {@link #everyAuditFieldIsAllowlisted(AuditCase)} y contra la que se verifica
     * la paridad. Antes había dos listas —la de invocaciones y la mental del que
     * revisaba—; aquí solo hay una.
     *
     * <p>
     * {@code loginRateLimited} y {@code rateLimited} llevan entrada propia aunque
     * el primero no sea más que una delegación en el segundo: la paridad razona por
     * método declarado, y cubrir uno solo dejaría al otro sin ejercitar. El código
     * de {@code rateLimited} es uno real de {@code LoginRateLimitFilter}, no un
     * literal inventado, para que la prueba ejercite la forma que sale en
     * producción.
     */
    private static List<AuditCase> auditCases() {
        return List.of(
                new AuditCase("mutation", "http_mutation",
                        audit -> audit.mutation("PATCH", "/api/v1/animals/9", 200, "SUCCESS", 37)),
                new AuditCase("companyRegistered", "company_registered",
                        audit -> audit.companyRegistered(3L, "Clinica Vetrina", "900123456", 77L,
                                "OWNER01")),
                new AuditCase("employeeInvited", "employee_invited",
                        audit -> audit.employeeInvited(88L, "EMP0042", 3L)),
                new AuditCase("employeeInvitationResent", "employee_invitation_resent",
                        audit -> audit.employeeInvitationResent(88L, "EMP0042", 3L)),
                new AuditCase("invitationAccepted", "invitation_accepted",
                        audit -> audit.invitationAccepted(88L, 3L)),
                new AuditCase("loginSuccess", "login_success",
                        audit -> audit.loginSuccess("EMPLOYEE", "EMP0042")),
                new AuditCase("loginFailure", "login_failure",
                        audit -> audit.loginFailure("/auth/login/employee", "bad_credentials")),
                new AuditCase("loginBlockedEmailNotVerified", "login_blocked_email_not_verified",
                        audit -> audit.loginBlockedEmailNotVerified("EMP0042")),
                new AuditCase("refreshTokenReuseDetected", "refresh_token_reuse_detected",
                        audit -> audit.refreshTokenReuseDetected(77L, "EMPLOYEE", 12L)),
                new AuditCase("accessDenied", "access_denied",
                        audit -> audit.accessDenied("DELETE", "/api/v1/owners/5")),
                new AuditCase("unauthenticated", "unauthenticated",
                        audit -> audit.unauthenticated("GET", "/api/v1/owners", "invalid_token")),
                new AuditCase("loginRateLimited", "rate_limited", AuditLogger::loginRateLimited),
                new AuditCase("rateLimited", "rate_limited",
                        audit -> audit.rateLimited("REGISTER_RATE_LIMITED")));
    }

    /**
     * El correo de un dueño de veterinaria tal como llega hoy a la auditoría: en el
     * auto-registro el código de acceso se deriva del email, así que este mismo
     * valor viaja como {@code actor.identifier}. No es un literal decorativo — es
     * la forma exacta del dato que se filtraba.
     */
    private static final String CORREO_DEL_DUENO = "ana.perez@clinica.com";

    /**
     * Los tres emisores reales de {@code actor.identifier}, con el correo dentro.
     */
    private static Stream<Arguments> emisoresDelIdentificadorDeActor() {
        return Stream.of(
                Arguments.of(
                        (Consumer<AuditLogger>) audit -> audit.companyRegistered(3L,
                                "Clinica Vetrina", "900123456", 77L, CORREO_DEL_DUENO),
                        "company_registered"),
                Arguments.of((Consumer<AuditLogger>) audit -> audit.loginSuccess("EMPLOYEE",
                        CORREO_DEL_DUENO), "login_success"),
                Arguments.of(
                        (Consumer<AuditLogger>) audit -> audit
                                .loginBlockedEmailNotVerified(CORREO_DEL_DUENO),
                        "login_blocked_email_not_verified"));
    }

    private final AuditLogger auditLogger = new AuditLogger();

    private Logger auditChannel;
    private RedactingAppender redacting;
    private ListAppender<ILoggingEvent> sink;
    private Level previousLevel;

    @BeforeEach
    void wireAuditChannel() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        sink = new ListAppender<>();
        sink.setContext(context);
        sink.start();

        redacting = new RedactingAppender();
        redacting.setContext(context);
        redacting.setName("AUDIT_REDACTION_PROBE");
        redacting.addAppender(sink);
        redacting.start();

        auditChannel = context.getLogger("AUDIT");
        previousLevel = auditChannel.getLevel();
        auditChannel.setLevel(Level.INFO);
        auditChannel.addAppender(redacting);

        // Contexto de request que RequestLoggingContextFilter y AuthFilter ponen en el
        // MDC.
        MDC.put(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
        MDC.put(MdcKeys.ACTOR_EMPLOYEE_ID, "77");
        MDC.put(MdcKeys.ACTOR_COMPANY_ID, "3");
        MDC.put(MdcKeys.ACTOR_SYSTEM_USER_ID, "9");
        MDC.put(MdcKeys.CLIENT_IP, "192.0.2.10");
        MDC.put(MdcKeys.HTTP_METHOD, "POST");
        MDC.put(MdcKeys.HTTP_PATH, "/api/v1/owners");
        MDC.put(MdcKeys.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    }

    @AfterEach
    void tearDown() {
        auditChannel.detachAppender(redacting);
        auditChannel.setLevel(previousLevel);
        redacting.stop();
        MDC.clear();
    }

    private void assertNothingMasked(String event, Consumer<AuditLogger> emit) {
        sink.list.clear();
        emit.accept(auditLogger);

        assertThat(sink.list).as("el evento '%s' no se emitió", event).hasSize(1);
        ILoggingEvent emitted = sink.list.get(0);

        List<KeyValuePair> pairs = emitted.getKeyValuePairs();
        if (pairs != null) {
            assertThat(pairs).allSatisfy(pair -> assertThat(pair.value)
                    .as("el campo de auditoría '%s' del evento '%s' quedó enmascarado; "
                            + "declararlo en LogFieldPolicy", pair.key, event)
                    .isNotEqualTo(LogRedactor.MASK));
        }
        assertThat(emitted.getMDCPropertyMap())
                .as("un campo del MDC del evento '%s' quedó enmascarado; declararlo en "
                        + "LogFieldPolicy", event)
                .doesNotContainValue(LogRedactor.MASK);
        assertThat(emitted.getFormattedMessage()).doesNotContain(LogRedactor.MASK);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("auditCases")
    @DisplayName("ningún campo de los eventos AUDIT queda enmascarado por la allowlist")
    void everyAuditFieldIsAllowlisted(AuditCase testCase) {
        assertNothingMasked(testCase.event(), testCase.emit());
    }

    /**
     * El antirrecaída. Sin esta prueba, añadir un evento a {@link AuditLogger} y
     * olvidar su caso aquí deja la suite en verde y el campo enmascarado en Loki —
     * que es exactamente como {@code refresh_token_reuse_detected} y
     * {@code rateLimited} estuvieron sin red.
     */
    @Test
    @DisplayName("todo método público de AuditLogger queda ejercitado por algún caso")
    void everyAuditMethodIsExercisedByACase() {
        // isSynthetic() descarta el puente que mete la instrumentación de JaCoCo y
        // cualquier método generado por el compilador: solo interesa lo que un humano
        // escribió en AuditLogger.
        Set<String> declaredByProduction = Arrays.stream(AuditLogger.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.isSynthetic()).map(Method::getName)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> exercisedByCases = auditCases().stream().map(AuditCase::method)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exercisedByCases)
                .as("AuditLogger expone métodos públicos que ninguna entrada de auditCases() "
                        + "ejercita: sus campos nunca pasan por la allowlist, así que pueden "
                        + "salir como '%s' en producción sin que nada falle aquí. Añade una "
                        + "entrada por cada método listado abajo", LogRedactor.MASK)
                .containsAll(declaredByProduction);

        // El sentido contrario: el nombre de cada entrada es un String escrito a mano y
        // el compilador no lo valida. Una errata o un rename dejarían un método real
        // sin cubrir mientras la comprobación de arriba pasa por casualidad.
        assertThat(declaredByProduction)
                .as("auditCases() declara nombres de método que AuditLogger ya no expone: "
                        + "errata al escribirlos, o un rename/borrado en producción que dejó la "
                        + "entrada apuntando a la nada")
                .containsAll(exercisedByCases);
    }

    @Test
    @DisplayName("el NIT del tenant y los códigos de empleado salen íntegros, no como documentos")
    void keepsTenantTaxIdAndEmployeeCodesIntact() {
        sink.list.clear();
        auditLogger.companyRegistered(3L, "Clinica Vetrina", "900123456", 77L, "OWNER01");

        ILoggingEvent emitted = sink.list.get(0);
        assertThat(emitted.getKeyValuePairs()).extracting(pair -> pair.key + "=" + pair.value)
                .contains("company.identifier=900123456", "actor.identifier=OWNER01");
    }

    /**
     * La contra-prueba de la contra-prueba (#216).
     *
     * <p>
     * El resto de esta clase vigila que la allowlist no se pase de estrecha. Esto
     * vigila lo contrario, que es como nació el defecto: {@code actor.identifier}
     * estaba declarado {@code VERBATIM} bajo la premisa de que un código de acceso
     * de empleado no es un dato personal, y esa premisa la rompe el propio producto
     * — en el auto-registro el código de acceso <b>es</b> el correo
     * ({@code RegisterUserService.register}). Con la clave en {@code VERBATIM},
     * {@code POST /register} y cada {@code POST /auth/login/employee} publicaban el
     * correo del usuario en claro hasta Loki (Grafana Cloud, EE. UU.).
     *
     * <p>
     * Los tres casos son los tres emisores reales de la clave. El de
     * {@code loginBlockedEmailNotVerified} llega redactado desde
     * {@code GlobalExceptionHandler} (#180) y aquí se pasa <b>en claro</b> a
     * propósito: lo que se afirma es que el pipeline protege por sí solo, sin
     * depender de que el emisor se acuerde.
     */
    @ParameterizedTest(name = "{1}")
    @MethodSource("emisoresDelIdentificadorDeActor")
    @DisplayName("el correo usado como código de acceso no sale en claro en ningún campo")
    void el_correo_usado_como_codigo_de_acceso_no_sale_en_claro(Consumer<AuditLogger> emit,
            String event) {
        sink.list.clear();
        emit.accept(auditLogger);

        ILoggingEvent emitted = sink.list.get(0);

        assertThat(emitted.getKeyValuePairs())
                .as("el evento '%s' publicó el correo en claro en un campo estructurado", event)
                .noneMatch(pair -> CORREO_DEL_DUENO.equals(pair.value));
        assertThat(emitted.getFormattedMessage())
                .as("el evento '%s' publicó el correo en claro en el texto del mensaje", event)
                .doesNotContain(CORREO_DEL_DUENO);
    }

    /**
     * Y la mitad que impide que el arreglo se convierta en «borrar el campo»: se
     * conserva el dominio —diagnóstico de entregas, y no es dato personal— y, en el
     * alta, el id numérico del actor que va al lado. Quien investigue el incidente
     * sigue teniendo a quién señalar.
     */
    @Test
    @DisplayName("enmascarar el correo no ciega la auditoría: quedan el dominio y el id del actor")
    void enmascarar_el_correo_no_ciega_la_auditoria() {
        sink.list.clear();
        auditLogger.companyRegistered(3L, "Clinica Vetrina", "900123456", 77L, CORREO_DEL_DUENO);

        assertThat(sink.list.get(0).getKeyValuePairs())
                .extracting(pair -> pair.key + "=" + pair.value)
                .contains("actor.identifier=" + LogRedactor.MASK + "@clinica.com",
                        "actor.employeeId=77");
    }

    @Test
    @DisplayName("el contexto de request del MDC llega completo a cada evento de auditoría")
    void keepsRequestContextIntact() {
        sink.list.clear();
        auditLogger.loginSuccess("EMPLOYEE", "EMP0042");

        assertThat(sink.list.get(0).getMDCPropertyMap())
                .containsEntry(MdcKeys.CLIENT_IP, "192.0.2.10")
                .containsEntry(MdcKeys.HTTP_METHOD, "POST")
                .containsEntry(MdcKeys.HTTP_PATH, "/api/v1/owners")
                .containsEntry(MdcKeys.ACTOR_EMPLOYEE_ID, "77")
                .containsEntry(MdcKeys.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    }
}
