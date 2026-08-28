package com.vetsoftware.app.companylimitevent.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * El contador visto desde la bitacora: leer los que estan sin comprobar y
 * sellarlos cuando cuadran.
 *
 * <p>
 * Es un puerto y no una llamada directa porque el contador vive en otra rodaja.
 * El adaptador que lo implementa esta en {@code infrastructure/orchestration},
 * el unico sitio de esta feature autorizado a conocer la otra --el mismo camino
 * que ya usa {@link CompanyUsageAdjustmentPort}, y en la misma direccion: esta
 * rodaja depende de {@code entitlement}, nunca al reves--.
 *
 * <p>
 * <strong>No sabe corregir el contador, y es deliberado.</strong> El recuento
 * escribe un hecho compensatorio, no un {@code UPDATE} (R-LIMIT-19,
 * R-LIMIT-30); la correccion, cuando toca, la firma una persona de plataforma
 * por {@code AdjustCompanyUsageUseCase}. Ofrecer aqui un metodo para
 * sobrescribir el consumo seria ofrecer justo el camino que el modelo prohibe.
 */
public interface CapacityCounterPort {

    /**
     * Los contadores sin comprobar o comprobados hace demasiado, en lotes y por
     * <strong>cursor de id</strong>.
     *
     * <p>
     * El cursor no es una optimizacion: es lo que hace que el barrido termine. Un
     * contador con desvio no se sella --a proposito-- y por tanto sigue siendo
     * «pendiente» despues de examinarlo, asi que un lote que no avance por id
     * devolveria las mismas filas indefinidamente.
     *
     * @param staleBefore
     *            sello anterior a este instante; los de sello nulo entran siempre
     * @param afterId
     *            id del ultimo contador del lote anterior; {@code 0} para empezar
     */
    List<CapacityCounter> findUnreconciled(LocalDateTime staleBefore, long afterId, int limit);

    /**
     * Sella el consumo de un contador. Solo se invoca cuando el recuento cuadro:
     * sellar uno que se sabe desviado es peor que no sellar nada.
     */
    boolean markReconciled(Long companyId, Long limitDimensionId, String periodKey,
            LocalDateTime reconciledAt);

    /**
     * Un contador tal como lo necesita el recuento: a quien pertenece, que eje
     * mide, de que periodo habla y los dos numeros que el hecho tiene que copiar.
     *
     * <p>
     * Copia propia de esta rodaja: el DTO de {@code entitlement} no se importa.
     */
    record CapacityCounter(Long id, Long companyId, Long limitDimensionId, String dimensionCode,
            String measureKind, String periodKey, int limitQuantity, int usedQuantity) {
    }
}
