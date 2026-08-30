package com.vetsoftware.app.aiproposal.infrastructure.retention;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * &#9940; <strong>Los dos plazos son configuracion, no constantes.</strong> Los
 * 90 dias y los 24 meses son la propuesta de ingenieria y <strong>nadie con
 * criterio legal los ha validado todavia</strong> (R-5, abierto): van a
 * cambiar, y cuando cambien tienen que cambiar con una variable de entorno y un
 * reinicio, no con un PR, una revision y un despliegue. Un plazo de retencion
 * escrito como {@code static final} es una decision juridica compilada dentro
 * de un jar.
 *
 * <p>
 * <strong>{@link Duration} y no meses.</strong> El binder de Spring no sabe
 * construir un {@code Period} desde {@code P24M}, asi que los 24 meses se
 * expresan como 730 dias. La diferencia con dos años naturales es de un dia
 * cada cuatro años y no significa nada frente a un plazo que todavia no esta
 * validado; escribirlo aqui evita la alternativa peor, que es un
 * {@code int meses} que cada llamante interpreta a su manera.
 */
@ConfigurationProperties("vetsoftware.ai.proposal.retention")
public class AiProposalRetentionProperties {

    private boolean enabled = true;

    /** Inactividad tras la cual la propuesta se anonimiza. Propuesta: 90 dias. */
    private Duration anonymizeAfter = Duration.ofDays(90);

    /** Inactividad tras la cual la propuesta se purga. Propuesta: 24 meses. */
    private Duration purgeAfter = Duration.ofDays(730);

    private int batchSize = 500;

    private int maxBatchesPerRun = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getAnonymizeAfter() {
        return anonymizeAfter;
    }

    public void setAnonymizeAfter(Duration anonymizeAfter) {
        this.anonymizeAfter = anonymizeAfter;
    }

    public Duration getPurgeAfter() {
        return purgeAfter;
    }

    public void setPurgeAfter(Duration purgeAfter) {
        this.purgeAfter = purgeAfter;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxBatchesPerRun() {
        return maxBatchesPerRun;
    }

    public void setMaxBatchesPerRun(int maxBatchesPerRun) {
        this.maxBatchesPerRun = maxBatchesPerRun;
    }

    /**
     * Se valida al construir el job, no al usarse. Un plazo mal escrito en el
     * entorno tiene que impedir el arranque: descubrirlo a las 03:55 significa que
     * ya se borro lo que no tocaba, y de un borrado no se vuelve.
     *
     * <p>
     * &#9940; <strong>La comprobacion que de verdad importa es la ultima.</strong>
     * Con {@code purgeAfter} por debajo de {@code anonymizeAfter}, el barrido
     * purgaria filas que todavia no ha anonimizado -es decir, borraria propuestas
     * frescas- y lo haria en silencio, informando exito. Es el unico error de
     * configuracion de esta clase que destruye datos.
     */
    void validate() {
        if (anonymizeAfter == null || anonymizeAfter.isNegative() || anonymizeAfter.isZero()) {
            throw new IllegalStateException(
                    "vetsoftware.ai.proposal.retention.anonymize-after debe ser positiva");
        }
        if (purgeAfter == null || purgeAfter.isNegative() || purgeAfter.isZero()) {
            throw new IllegalStateException(
                    "vetsoftware.ai.proposal.retention.purge-after debe ser positiva");
        }
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalStateException(
                    "vetsoftware.ai.proposal.retention.batch-size debe estar entre 1 y 10000");
        }
        if (maxBatchesPerRun < 1 || maxBatchesPerRun > 200) {
            throw new IllegalStateException(
                    "vetsoftware.ai.proposal.retention.max-batches-per-run debe estar entre 1"
                            + " y 200");
        }
        if (purgeAfter.compareTo(anonymizeAfter) < 0) {
            throw new IllegalStateException(
                    "vetsoftware.ai.proposal.retention.purge-after no puede ser menor que"
                            + " anonymize-after: se purgarian propuestas todavia sin anonimizar");
        }
    }
}
