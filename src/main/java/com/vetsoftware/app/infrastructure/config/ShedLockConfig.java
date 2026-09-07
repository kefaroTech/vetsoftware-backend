package com.vetsoftware.app.infrastructure.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Candado distribuido para los {@code @Scheduled} que exigen una sola réplica.
 * La tabla {@code shedlock} la trae el changeset 412; el nombre de cada candado
 * es el {@code job.name} de {@code ScheduledJobCatalog}, así que la fila de
 * {@code shedlock} y la etiqueta de la métrica identifican el mismo barrido.
 *
 * <p>
 * {@code defaultLockAtMostFor} es la red de seguridad si una tarea muere sin
 * liberar el candado (caída del contenedor a media ejecución): pasado ese
 * tiempo, otra réplica puede tomarlo aunque la fila siga marcada. Cada
 * {@code @SchedulerLock} declara el suyo propio acorde a su barrido; este es
 * solo el valor por defecto si alguno lo omitiera.
 *
 * <p>
 * <strong>Ninguna réplica que pierde el candado deja rastro, y es una decisión
 * documentada, no un hueco.</strong> {@code JdbcTemplateLockProvider} decide
 * con un {@code UPDATE ... WHERE lock_until <= NOW()} sobre la fila de
 * {@code shedlock}: la réplica ganadora es la que actualiza una fila, la
 * perdedora recibe cero filas afectadas y su método {@code @Scheduled}
 * simplemente no se invoca —no hay excepción, ni log, ni métrica que distinguir
 * esa réplica de una que nunca intentó ejecutar el barrido. Instrumentar el
 * intento fallido exigiría envolver cada {@code @Scheduled} con lógica propia
 * antes de que ShedLock decida, lo que duplicaría en cada job el candado que la
 * librería ya resuelve. Con una sola instancia por entorno hoy, el coste de esa
 * instrumentación no se justifica; si se escala a varias réplicas, el primer
 * indicio a vigilar es que un barrido lockeado no emita su métrica de
 * finalización en su franja esperada.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
