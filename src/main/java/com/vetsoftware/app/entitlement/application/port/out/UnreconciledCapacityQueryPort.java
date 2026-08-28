package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Los contadores cuyo consumo <strong>nadie ha comprobado nunca</strong>
 * --sello nulo-- o hace demasiado. Es la consulta que alimenta el recuento
 * periodico de R-LIMIT-30.
 *
 * <p>
 * <strong>Es el unico puerto de esta rodaja que no se acota por
 * empresa</strong>, y esa excepcion esta justificada: un barrido de plataforma
 * que preguntara empresa por empresa tendria que conocer antes la lista de
 * empresas, que es exactamente la misma consulta sin acotar con un paso mas y
 * un N+1 encima. Por eso el unico caso de uso que lo consume va cerrado a
 * {@code hasRole('SYSTEM')} a secas --regla dura
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}--: acotar por una clave foranea
 * ajena no cuenta como acotar por empresa.
 *
 * <p>
 * {@code ix_company_capacities_unreconciled} existe justo para esto.
 */
public interface UnreconciledCapacityQueryPort {

    /**
     * <strong>Avanza por cursor de id, no por prioridad</strong>, y esa eleccion es
     * lo que hace que el barrido termine.
     *
     * <p>
     * Lo natural seria servir primero los nunca comprobados. No se puede: un
     * contador con desvio <em>no se sella</em> --a proposito, para volver a
     * mirarlo--, asi que sigue en este conjunto despues de examinarlo. Ordenando
     * por urgencia, los atascados ocuparian la cabeza de todos los lotes y el
     * barrido giraria sobre las mismas filas sin avanzar nunca, o directamente para
     * siempre. Con el cursor, cada lote empieza donde acabo el anterior y una
     * pasada recorre la plataforma entera pase lo que pase con cada fila.
     *
     * @param staleBefore
     *            sello anterior a este instante. Los de sello nulo entran siempre
     * @param afterId
     *            id del ultimo contador del lote anterior; {@code 0} para empezar
     * @param limit
     *            tope del lote. Acota la transaccion, no el trabajo: el barrido
     *            repite mientras el lote salga lleno
     */
    List<CompanyCapacity> findUnreconciled(LocalDateTime staleBefore, long afterId, int limit);
}
