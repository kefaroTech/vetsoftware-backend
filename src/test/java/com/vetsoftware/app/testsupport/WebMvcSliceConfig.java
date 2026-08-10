package com.vetsoftware.app.testsupport;

import static org.mockito.Mockito.mock;

import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.web.GlobalExceptionHandler;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
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

    @Bean
    Authz authz() {
        Authz authz = mock(Authz.class);
        org.mockito.Mockito.lenient().when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
        org.mockito.Mockito.lenient().when(authz.currentEmployeeIdOrNull()).thenReturn(EMPLOYEE_ID);
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

    @SuppressWarnings("unchecked")
    @Bean
    LettuceBasedProxyManager<String> loginRateLimitProxyManager() {
        return mock(LettuceBasedProxyManager.class);
    }
}
