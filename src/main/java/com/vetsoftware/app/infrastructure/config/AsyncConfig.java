package com.vetsoftware.app.infrastructure.config;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita la ejecución asíncrona ({@code @Async}) y define un pool dedicado al envío de correo, para que
 * el correo (Resend) NO sume latencia al flujo principal (registro, facturación, etc.): el request responde
 * de inmediato y el envío corre en un hilo aparte (fire-and-forget).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    /**
     * Propaga la observación/traza activa y únicamente el MDC necesario para correlacionar
     * la tarea con el actor y la operación HTTP que la originaron. Se omiten deliberadamente
     * client.ip y user_agent.original para no copiar datos innecesarios a hilos de background.
     */
    @Bean
    public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new Slf4jThreadLocalAccessor(
                MdcKeys.ACTOR_TYPE,
                MdcKeys.ACTOR_EMPLOYEE_ID,
                MdcKeys.ACTOR_COMPANY_ID,
                MdcKeys.ACTOR_SYSTEM_USER_ID,
                MdcKeys.HTTP_METHOD,
                MdcKeys.HTTP_PATH));
        return new ContextPropagatingTaskDecorator();
    }

    @Bean(name = "emailTaskExecutor")
    public ThreadPoolTaskExecutor emailTaskExecutor(ContextPropagatingTaskDecorator contextDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setTaskDecorator(contextDecorator);
        // Al apagar la app, esperar a drenar los envíos en curso (poco tiempo) para no perder correos.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
