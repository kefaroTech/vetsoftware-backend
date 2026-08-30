package com.vetsoftware.app.infrastructure.observability;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catálogo único de los barridos programados, con su cadencia declarada.
 *
 * <p>
 * <b>Por qué existe.</b> Hasta el issue #609 la hora a la que corría cada
 * barrido era la del último despliegue: todos se declaraban con
 * {@code fixedDelay}, así que un despliegue a las 11:00 dejaba el barrido de
 * facturación corriendo a las 11:03 todos los días, sobre el mismo servidor de
 * dos núcleos que atiende clínicas. Peor que el coste: una hora que se mueve
 * sola no permite fijar el umbral de «este job no corrió», que es la única
 * forma canónica de detectar un barrido que dejó de programarse — y un barrido
 * que no corre no produce ninguna señal, ni un log, ni un contador.
 *
 * <p>
 * <b>Qué gobierna esta clase.</b> Tres cosas que antes vivían separadas y se
 * desincronizaban en silencio:
 *
 * <ol>
 * <li>El <b>nombre</b> del job, que es el valor de la etiqueta {@code job.name}
 * de {@code tasks.scheduled.execution}. Estaba escrito como literal en cada
 * clase, así que un typo creaba una serie nueva en Prometheus sin romper nada:
 * la alerta seguía verde porque miraba el nombre viejo, que ya no existía.
 * {@link ScheduledJobTelemetry#observe} exige ahora una constante de este enum.
 * <li>La <b>expresión cron</b> por defecto y la clave de propiedad que la
 * sobrescribe. Es un contrato: a partir de aquí, los umbrales de la alerta de
 * retraso dependen de estos valores, así que cambiarlos es cambiar el
 * alertamiento (docs/ALERTAS_STACK_LOCAL.md).
 * <li>Si el barrido exige <b>una sola réplica</b>. Los tres barridos DIAN
 * reclaman su lote en exclusiva con {@code DianJobLeasePort} y toleran N
 * réplicas; los otros cinco recorren la tabla con un cursor y no arbitran nada,
 * así que dos réplicas procesarían el mismo lote. Ver
 * {@link #requiresSingleWriter()}.
 * </ol>
 *
 * <p>
 * <b>Qué cambia el paso a cron respecto a la concurrencia, dicho sin
 * adornos.</b> Nada estructural: con {@code fixedDelay} dos réplicas ya
 * procesaban los mismos lotes, solo que a horas distintas y por accidente. Cron
 * sube la probabilidad de solape de «tarde o temprano» a «siempre», y por eso
 * el hueco deja de ser latente. La solución correcta es un candado distribuido
 * con su tabla; no se implementa aquí a propósito, por dos motivos que conviene
 * tener escritos: el candado consultivo de MySQL ({@code GET_LOCK}) obliga a
 * retener una conexión del pool durante todo el barrido y
 * {@code leak-detection-threshold: 20000} emitiría una falsa alarma de fuga
 * cada noche, y una tabla de candados exige un changeset de Liquibase.
 *
 * <p>
 * Mientras tanto la precondición es explícita y <b>vigilada</b>: el servicio
 * corre con una sola tarea, y la alerta
 * {@code VetSoftwareScheduledJobMultipleReplicas} —que cuenta cuántos procesos
 * publican el heartbeat de un mismo {@code job.name}— dispara en cuanto alguien
 * escale a dos, antes de que el primer cierre de mes duplique los cargos. Una
 * precondición no vigilada es una precondición que se rompe en silencio.
 *
 * <p>
 * <b>Los dos que no están aquí, y por qué.</b> {@code database.availability} y
 * {@code business.metrics.snapshot} son muestreo continuo, no calendario: su
 * pregunta es «¿cuál es el estado ahora?», no «¿corrió hoy?». Siguen con
 * {@code fixedDelay} y su retraso ya lo vigilan sus propias señales
 * ({@code vetsoftware_business_metrics_snapshot_age_seconds} y la sonda de base
 * de datos). Meterlos aquí duplicaría esa vigilancia y añadiría cuatro series
 * que no responden ninguna pregunta nueva.
 */
public enum ScheduledJobCatalog {

    /**
     * Cancelaciones efectivas, fin de prueba y vigencias de línea. 03:10 de Bogotá:
     * después de medianoche, porque el modelo razona con «vence a medianoche», y
     * antes de que abra la primera clínica.
     */
    SUBSCRIPTION_LIFECYCLE("subscription.lifecycle", "subscription.lifecycle.cron", "0 10 3 * * *",
            true),

    /**
     * Cobranza: facturas vencidas con saldo. 03:40, media hora después del
     * lifecycle, para que un contrato que acaba de expirar ya esté en su estado
     * final cuando se evalúe su mora.
     */
    SUBSCRIPTION_DUNNING("subscription.dunning", "subscription.dunning.cron", "0 40 3 * * *", true),

    /**
     * <b>Facturación recurrente</b>: devenga el periodo que toca y emite su cuenta
     * de cobro, de todas las clínicas. 04:40, la última de la cadena — después del
     * lifecycle (03:10), de la cobranza (03:40) y del recuento de consumo (04:10).
     * El orden no es estético: facturar antes de que el lifecycle cierre las líneas
     * vencidas cobraría servicios que dejaron de prestarse esa misma noche, y
     * hacerlo antes del recuento facturaría contra contadores que el recálculo
     * estaba a punto de mover.
     *
     * <p>
     * Exige una sola réplica: recorre {@code subscriptions} con un cursor sobre el
     * id y no arbitra nada, así que dos copias examinarían el mismo lote. Lo que
     * impide que eso <em>duplique un cobro</em> no es el cursor sino la llave
     * calculada de {@code RecurringChargeKey} y {@code uq_sbd_recurring_cycle}; aun
     * así duplicarían el trabajo y competirían por la fila del consecutivo.
     */
    SUBSCRIPTION_BILLING("subscription.billing", "subscription.billing.cron", "0 40 4 * * *", true),

    /** Caducidad de cotizaciones. 03:25, entre los dos anteriores. */
    QUOTE_EXPIRATION("quote.expiration", "quote.expiration.cron", "0 25 3 * * *", true),

    /**
     * Recuento del consumo contra las filas reales (R-LIMIT-30). 04:10, después de
     * que el lifecycle y la cobranza hayan dejado los contratos en su estado final:
     * recontar antes daría desvíos sobre contadores que el recálculo de las 03:10
     * estaba a punto de tocar.
     *
     * <p>
     * Exige una sola réplica: recorre {@code company_capacities} con un cursor
     * sobre el índice de no reconciliados y no arbitra nada, así que dos copias
     * examinarían el mismo lote y escribirían el hecho de desvío por duplicado.
     */
    USAGE_RECONCILIATION("usage.reconciliation", "usage.reconciliation.cron", "0 10 4 * * *", true),

    /**
     * Purga de tokens de seguridad. Cada hora al minuto 20 en vez de «cada hora
     * desde el arranque»: la alerta de crecimiento de la tabla
     * ({@code VetSoftwareSecurityTokenTableGrowth}) razona sobre «después de varias
     * ejecuciones de la purga», y eso exige saber cuándo son.
     */
    SECURITY_TOKENS_CLEANUP("security.tokens.cleanup", "vetsoftware.token-cleanup.cron",
            "0 20 * * * *", true),

    /**
     * Reintento de documentos en contingencia DIAN. Dos pasadas diarias, 02:15 y
     * 14:15. La de la tarde se mantiene a propósito: la DIAN se cae por horas y
     * esperar a la madrugada siguiente alargaría la contingencia un día entero.
     */
    DIAN_CONTINGENCY_RETRY("dian.contingency.retry", "dian.contingency.cron", "0 15 2,14 * * *",
            false),

    /** Reintento de entrega de la representación gráfica. 02:45 y 14:45. */
    DIAN_DELIVERY_RETRY("dian.delivery.retry", "dian.delivery.cron", "0 45 2,14 * * *", false),

    /** Conciliación de documentos pendientes en la DIAN. 02:30 y 14:30. */
    DIAN_PENDING_RECONCILIATION("dian.pending.reconciliation", "dian.reconciliation.cron",
            "0 30 2,14 * * *", false),

    /**
     * Retención de las propuestas del asistente comercial: anonimización a los 90
     * días, purga a los 24 meses. 03:55, en el hueco que dejan la caducidad de
     * cotizaciones (03:25) y la cobranza (03:40), y antes de que la conciliación de
     * consumo abra las 04:00.
     *
     * <p>
     * <b>Escritor único.</b> Dos réplicas a la vez no corromperían nada —los seis
     * pasos son idempotentes y van por lotes acotados— pero duplicarían el trabajo
     * y se pisarían los bloqueos de fila sobre las mismas tres tablas.
     *
     * <p>
     * <b>Los dos plazos son configuración</b>, no parte de esta entrada: aquí solo
     * vive la cadencia. Ver {@code AiProposalRetentionProperties}.
     */
    AI_PROPOSAL_RETENTION("aiproposal.retention", "vetsoftware.ai.proposal.retention.cron",
            "0 55 3 * * *", true);

    /**
     * Zona horaria de todas las expresiones. Explícita y no la del contenedor: ECS
     * corre en UTC, así que un cron sin zona pondría el barrido de las 03:10 a las
     * 22:10 de Bogotá, dentro del horario de atención de la costa.
     */
    public static final String ZONE = "America/Bogota";

    private final String jobName;
    private final String cronProperty;
    private final String defaultCron;
    private final boolean singleWriter;

    ScheduledJobCatalog(String jobName, String cronProperty, String defaultCron,
            boolean singleWriter) {
        this.jobName = jobName;
        this.cronProperty = cronProperty;
        this.defaultCron = defaultCron;
        this.singleWriter = singleWriter;
    }

    /** Valor de la etiqueta {@code job.name}; {@code lowercase.dot.notation}. */
    public String jobName() {
        return jobName;
    }

    /** Clave de propiedad que sobrescribe la cadencia. */
    public String cronProperty() {
        return cronProperty;
    }

    /** Expresión cron por defecto, en {@link #ZONE}. */
    public String defaultCron() {
        return defaultCron;
    }

    /**
     * Placeholder listo para la anotación: {@code ${clave:por-defecto}}. La prueba
     * {@code ScheduledJobCatalogParityTest} comprueba que la cadena que aparece en
     * cada {@code @Scheduled} es exactamente esta — sin ella, cambiar la cadencia
     * aquí dejaría el umbral de la alerta apuntando a una hora que ya no es.
     */
    public String cronPlaceholder() {
        return "${" + cronProperty + ":" + defaultCron + "}";
    }

    /**
     * {@code true} si el barrido no tolera dos réplicas simultáneas. Los DIAN
     * devuelven {@code false} porque reclaman su lote en exclusiva con
     * {@code DianJobLeasePort}: ahí el arbitraje es por lote, más fino que por job,
     * y todas las réplicas avanzan.
     *
     * <p>
     * Los cinco que devuelven {@code true} son la razón de ser de
     * {@code VetSoftwareScheduledJobMultipleReplicas}: hoy la única cosa que impide
     * que dupliquen trabajo es que el servicio corre con una sola tarea, y eso es
     * una propiedad del despliegue, no del código.
     */
    public boolean requiresSingleWriter() {
        return singleWriter;
    }

    public static Optional<ScheduledJobCatalog> byJobName(String jobName) {
        return Arrays.stream(values()).filter(job -> job.jobName.equals(jobName)).findFirst();
    }
}
