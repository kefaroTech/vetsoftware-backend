package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import java.time.Duration;
import java.util.List;

/**
 * Reparte el trabajo de los jobs de DIAN entre las réplicas del backend.
 *
 * <p>
 * Los jobs son {@code @Scheduled}: cada tarea Fargate ejecuta su propia copia.
 * Sin nada que las coordine, todas leen la misma lista de documentos de un
 * estado y todas los retransmiten — el mismo documento sale N veces hacia la
 * DIAN. Con un proveedor que no deduplique, eso son documentos fiscales
 * repetidos.
 *
 * <p>
 * El reparto se hace reclamando lotes en exclusiva, no serializando el job
 * entero: cada réplica se lleva un subconjunto disjunto y todas avanzan. El
 * lease es temporal a propósito — si la réplica que lo tomó muere a mitad, el
 * lote vuelve a estar disponible al expirar.
 */
public interface DianJobLeasePort {

    /**
     * Reclama en exclusiva hasta {@code limit} documentos del estado indicado que
     * nadie más tenga arrendado, y los marca como propios durante {@code lease}.
     *
     * @param status
     *            estado DIAN de los documentos a procesar
     * @param limit
     *            tamaño máximo del lote
     * @param lease
     *            cuánto dura la exclusividad; debe cubrir el tiempo de proceso del
     *            lote
     * @return ids reclamados, vacío si no había trabajo disponible
     */
    List<Long> leaseByDianStatus(DianStatus status, int limit, Duration lease);
}
