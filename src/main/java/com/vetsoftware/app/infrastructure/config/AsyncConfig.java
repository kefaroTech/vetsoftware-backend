package com.vetsoftware.app.infrastructure.config;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita la ejecución asíncrona ({@code @Async}) y define un pool dedicado al
 * envío de correo, para que el correo (Resend) NO sume latencia al flujo
 * principal (registro, facturación, etc.): el request responde de inmediato y
 * el envío corre en un hilo aparte (fire-and-forget).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    /**
     * Envíos simultáneos hacia Resend. No protege a la JVM —los hilos virtuales son
     * baratos— sino a la API del proveedor, que sí limita por tasa.
     */
    private static final int EMAIL_CONCURRENCY_LIMIT = 20;

    /**
     * Propaga la observación/traza activa y únicamente el MDC necesario para
     * correlacionar la tarea con el actor y la operación HTTP que la originaron. Se
     * omiten deliberadamente client.ip y user_agent.original para no copiar datos
     * innecesarios a hilos de background.
     *
     * <p>
     * <b>La lista de claves es explícita, y lo que no esté en ella se queda en el
     * hilo de la request.</b> {@code ContextPropagatingTaskDecorator} propaga por
     * su cuenta la observación y la traza; el MDC viaja por este
     * {@code Slf4jThreadLocalAccessor} y por nada más. Es la trampa que documenta
     * {@code docs/TELEMETRIA_ALTA_SUPERADMIN.md} §3.2: añadir una clave a
     * {@link MdcKeys} y a su política de redacción y olvidarla aquí no rompe nada
     * visible —el log de la request sale completo— pero el trabajo diferido a este
     * pool sale sin ella.
     *
     * <p>
     * <b>{@code system.user.request.id} está en la lista y hoy no propaga nada, y
     * eso hay que decirlo aquí porque el código no lo dice.</b> Los servicios de
     * {@code platformaccess} desatan la clave en el {@code finally} de un método
     * {@code @Transactional}, es decir <b>antes</b> del commit y por tanto antes
     * del {@code afterCommit} donde se encola el correo: cuando el decorador
     * captura el MDC, la clave ya no está. El ERROR del correo de invitación que no
     * salió no sabe de qué solicitud habla por el MDC — lo sabe porque
     * {@code PlatformAccessEmailSender} lleva el {@code requestId} en todas sus
     * firmas, que es precisamente el motivo por el que lo lleva.
     *
     * <p>
     * Se conserva en la lista de todas formas: cuesta cero, y cubre a cualquier
     * {@code @Async} que se despache algún día con el marco todavía abierto.
     * Quitarla sería igual de correcto hoy y peor mañana.
     */
    @Bean
    public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor(MdcKeys.ACTOR_TYPE,
                        MdcKeys.ACTOR_EMPLOYEE_ID, MdcKeys.ACTOR_COMPANY_ID,
                        MdcKeys.ACTOR_SYSTEM_USER_ID, MdcKeys.HTTP_METHOD, MdcKeys.HTTP_PATH,
                        MdcKeys.SYSTEM_USER_REQUEST_ID));
        return new ContextPropagatingTaskDecorator();
    }

    /**
     * Un hilo virtual por envío. El trabajo es I/O puro contra la API de Resend:
     * con hilos de plataforma, dos fijos y cinco como techo, un pico de facturación
     * de fin de mes encolaba trescientos correos detrás de cinco hilos que solo
     * esperaban red — y la cola tenía capacidad 100, así que a partir de ahí se
     * rechazaban envíos.
     *
     * <p>
     * El límite de concurrencia se conserva, pero cambia de significado: ya no
     * protege a la JVM de quedarse sin hilos —un hilo virtual cuesta kilobytes—
     * sino que acota cuántas peticiones simultáneas se le hacen a Resend, que sí
     * tiene límites de tasa.
     */
    @Bean(name = "emailTaskExecutor")
    public AsyncTaskExecutor emailTaskExecutor(ContextPropagatingTaskDecorator contextDecorator) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("email-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(EMAIL_CONCURRENCY_LIMIT);
        executor.setTaskDecorator(contextDecorator);
        // Al apagar la app, esperar a drenar los envíos en curso (poco tiempo) para no
        // perder correos.
        executor.setTaskTerminationTimeout(Duration.ofSeconds(20).toMillis());
        return executor;
    }
}
