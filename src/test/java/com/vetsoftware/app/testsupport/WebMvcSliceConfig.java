package com.vetsoftware.app.testsupport;

import static org.mockito.Mockito.mock;

import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.web.GlobalExceptionHandler;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Piezas comunes de las rodajas {@code @WebMvcTest}. Se importa una vez por
 * test de controller y evita repetir el andamiaje en 92 clases.
 *
 * <p>
 * <b>Por que hay dobles de cosas que el test no usa.</b> {@code @WebMvcTest}
 * instancia TODOS los beans de tipo {@code Filter} del classpath, no solo el
 * controller bajo prueba. Los seis filtros de esta aplicacion arrastran JWT,
 * auditoria, tracing y el proxy de Redis del rate limit. Sin estos dobles el
 * contexto no arranca y ninguna rodaja web es posible.
 *
 * <p>
 * <b>Que se prueba y que no.</b> Aqui se prueba el mapeo HTTP —rutas, binding,
 * validacion del request, codigos de estado y forma del JSON—, no la
 * autorizacion: la cadena de seguridad se sustituye por una permisiva porque la
 * real necesita Redis y base de datos. La autorizacion tiene su propia red: el
 * {@code @PreAuthorize} de cada puerto lo verifica ArchUnit y las expresiones
 * SpEL se prueban aparte.
 *
 * <p>
 * Importa el {@link GlobalExceptionHandler} real porque es medio contrato de la
 * API: sin el, una excepcion de dominio saldria como 500 y el test dejaria de
 * comprobar el 404/400 que ve el cliente.
 */
@TestConfiguration
@Import(GlobalExceptionHandler.class)
public class WebMvcSliceConfig {

    /** Company del contexto en las rodajas. Los tests pueden re-stubearlo. */
    public static final Long COMPANY_ID = 9L;

    /**
     * Empleado del contexto en las rodajas. Se stubea explicitamente porque los
     * controllers que sellan autoria (`createdBy`/`updatedBy`) leen
     * {@code currentEmployeeIdOrNull()}: sin stub, Mockito devuelve 0L para un
     * {@code Long} —no null—, y el command llegaria firmado por un empleado que no
     * existe. Un valor propio deja ver el sello en la asercion.
     */
    public static final Long EMPLOYEE_ID = 4L;

    /**
     * Cuenta de plataforma del contexto en las rodajas. Se stubea por la misma
     * razon que {@link #EMPLOYEE_ID} y con una consecuencia peor: el modelo de
     * suscripciones guarda quien tomo cada decision comercial
     * —{@code price_lists.published_by_system_user_id},
     * {@code subscription_amendments.requested_by_system_user_id},
     * {@code subscription_billing_documents.external_registered_by_system_user_id}—
     * y esos controllers leen {@code currentSystemUserId()}.
     *
     * <p>
     * Sin stub, Mockito devuelve 0L para un {@code Long} —no null— y una rodaja que
     * afirma la firma pasa en VERDE con una tarifa publicada por un usuario de
     * sistema inexistente. El test no falla: miente. Un valor propio y distinto de
     * {@code EMPLOYEE_ID} deja ver en la asercion cual de los dos actores firmo.
     */
    public static final Long SYSTEM_USER_ID = 6L;

    @Bean
    Authz authz() {
        Authz authz = mock(Authz.class);
        org.mockito.Mockito.lenient().when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
        org.mockito.Mockito.lenient().when(authz.currentEmployeeIdOrNull()).thenReturn(EMPLOYEE_ID);
        org.mockito.Mockito.lenient().when(authz.currentSystemUserId()).thenReturn(SYSTEM_USER_ID);
        org.mockito.Mockito.lenient().when(authz.currentSystemUserIdOrNull())
                .thenReturn(SYSTEM_USER_ID);
        org.mockito.Mockito.lenient().when(authz.isMyCompany(org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        return authz;
    }

    @Bean
    ResolveAuthContextUseCase resolveAuthContextUseCase() {
        return mock(ResolveAuthContextUseCase.class);
    }

    @Bean
    ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase() {
        return mock(ResolveSystemAuthContextUseCase.class);
    }

    @Bean
    JwtProvider jwtProvider() {
        return mock(JwtProvider.class);
    }

    @Bean
    AuditLogger auditLogger() {
        return mock(AuditLogger.class);
    }

    @Bean
    Tracer tracer() {
        return mock(Tracer.class);
    }

    /**
     * Registro de metricas real y en memoria, no un doble: {@code AuthFilter}
     * construye su contador en el constructor
     * ({@code Counter.builder(...).withRegistry(...)}), asi que un mock devolveria
     * null donde el filtro espera un registro y la rodaja fallaria al arrancar.
     * {@code SimpleMeterRegistry} no exporta a ningun sitio y no tiene estado
     * compartido entre contextos.
     */
    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @SuppressWarnings("unchecked")
    @Bean
    LettuceBasedProxyManager<String> loginRateLimitProxyManager() {
        return mock(LettuceBasedProxyManager.class);
    }
}
