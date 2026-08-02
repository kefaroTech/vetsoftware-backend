package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * @see LogRedactionPipelineTest
 */
class AuditFieldsSurviveRedactionTest {

    private final AuditLogger auditLogger = new AuditLogger(mock(AuditEventStore.class));

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

    @Test
    @DisplayName("ningún campo de los eventos AUDIT queda enmascarado por la allowlist")
    void everyAuditFieldIsAllowlisted() {
        assertNothingMasked("http_mutation",
                audit -> audit.mutation("PATCH", "/api/v1/animals/9", 200, "SUCCESS", 37));
        assertNothingMasked("company_registered", audit -> audit.companyRegistered(3L,
                "Clinica Vetrina", "900123456", 77L, "OWNER01"));
        assertNothingMasked("employee_invited", audit -> audit.employeeInvited(88L, "EMP0042", 3L));
        assertNothingMasked("employee_invitation_resent",
                audit -> audit.employeeInvitationResent(88L, "EMP0042", 3L));
        assertNothingMasked("invitation_accepted", audit -> audit.invitationAccepted(88L, 3L));
        assertNothingMasked("login_success", audit -> audit.loginSuccess("EMPLOYEE", "EMP0042"));
        assertNothingMasked("login_failure",
                audit -> audit.loginFailure("/auth/login/employee", "bad_credentials"));
        assertNothingMasked("login_blocked_email_not_verified",
                audit -> audit.loginBlockedEmailNotVerified("EMP0042"));
        assertNothingMasked("access_denied",
                audit -> audit.accessDenied("DELETE", "/api/v1/owners/5"));
        assertNothingMasked("unauthenticated",
                audit -> audit.unauthenticated("GET", "/api/v1/owners", "invalid_token"));
        assertNothingMasked("rate_limited", audit -> audit.loginRateLimited());
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
