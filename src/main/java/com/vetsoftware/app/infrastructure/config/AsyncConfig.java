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
     */
    @Bean
    public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor(MdcKeys.ACTOR_TYPE,
                        MdcKeys.ACTOR_EMPLOYEE_ID, MdcKeys.ACTOR_COMPANY_ID,
                        MdcKeys.ACTOR_SYSTEM_USER_ID, MdcKeys.HTTP_METHOD, MdcKeys.HTTP_PATH));
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
