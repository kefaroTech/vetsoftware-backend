package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de los contadores contratados, siempre acotado por empresa.
 */
public interface CompanyCapacityRepository {

    List<CompanyCapacity> findAllByCompanyId(Long companyId);

    Optional<CompanyCapacity> findByCompanyIdAndDimension(Long companyId, Long limitDimensionId,
            String periodKey);

    /**
     * Escribe el techo derivado del contrato <strong>sin tocar el consumo</strong>.
     *
     * <p>
     * Es el arreglo de #648 y la razon de que este metodo exista en vez de un
     * {@code saveAll}. Guardar la fila entera obliga a nombrar
     * {@code used_quantity} en el {@code UPDATE}, con el valor que se leyo al
     * empezar el recalculo; una baja de empleado que ocurra entre esa lectura y
     * esta escritura se pierde sin excepcion y sin log, y el cliente queda con un
     * techo que no puede llenar --el contador dice que tiene ocupadas plazas que ya
     * libero--.
     *
     * <p>
     * La implementacion inserta o actualiza en una sola sentencia, de modo que
     * tampoco hay carrera con el nacimiento de la fila.
     *
     * @return numero de contadores escritos
     */
    int upsertCeilings(List<CompanyCapacity> capacities);

    /**
     * Suma {@code delta} al consumo, en el motor y en una sola sentencia.
     *
     * @return filas afectadas: 0 si la empresa no tiene contador de ese eje y
     *         periodo, si el movimiento dejaria el consumo en negativo, o si una
     *         reserva positiva pasaria del techo
     */
    int addUsage(Long companyId, Long limitDimensionId, String periodKey, int delta);

    /**
     * Suma {@code delta} al consumo <strong>sin comprobar el techo</strong>, en el
     * motor y en una sola sentencia.
     *
     * <p>
     * <strong>Solo lo llama el camino del excedente</strong>, y solo despues de que
     * {@link OverageAllowancePort} haya devuelto un permiso escrito: la clinica
     * declaro {@code enforcement = OVERAGE} y su precio por unidad, es decir compro
     * el derecho a pasarse. Sin ese permiso se sigue llamando a {@link #addUsage},
     * que es el que niega.
     *
     * <p>
     * <strong>Lo unico que no perdona es el signo.</strong> El
     * {@code used_quantity + :delta >= 0} se conserva por el mismo motivo que en
     * {@link #addUsage}: con {@code chk_company_capacities_quantities} detras, un
     * delta pasado del reves seria un error de motor a mitad de transaccion en vez
     * de cero filas afectadas, que es lo que el caso de uso sabe interpretar.
     *
     * <p>
     * <strong>Por que es una sentencia aparte y no un parametro de la
     * primera.</strong> Un {@code addUsage(..., boolean allowOverage)} deja el
     * salto del techo a merced de un booleano que se puede pasar mal desde
     * cualquier llamador nuevo, y en una revision se lee igual de bien en los dos
     * sentidos. Con dos metodos, saltarse el techo <b>tiene nombre</b> y sale en el
     * diff.
     *
     * @return filas afectadas: 0 si la empresa no tiene contador de ese eje y
     *         periodo, o si el movimiento dejaria el consumo en negativo.
     *         <b>Nunca</b> 0 por techo alcanzado, que es el punto entero
     */
    int addUsageAllowingOverage(Long companyId, Long limitDimensionId, String periodKey, int delta);

    /**
     * Hace <strong>nacer</strong> la fila de un periodo de flujo, con consumo cero
     * y el techo <em>ya resuelto</em> heredado del periodo anterior de la misma
     * serie (R-LIMIT-04).
     *
     * <p>
     * <strong>No cuenta nada.</strong> El consumo lo sigue moviendo
     * {@link #addUsage} y solo el: aqui se escribe un cero. Esa separacion es lo
     * que deja intacta la instruccion atomica de R-LIMIT-01 --sube y comprueba el
     * techo en una sola sentencia-- en lugar de reimplementarla.
     *
     * <p>
     * <strong>Y no resuelve el techo, lo hereda.</strong> Cruzar
     * {@code subscription_item_limits}, {@code catalog_item_limits} y
     * {@code limit_dimensions} para calcularlo aqui pondria tres tablas en el
     * camino mas caliente del sistema, que es justo lo que la fila con el techo ya
     * resuelto existe para evitar. Un cupo de flujo <em>se reinicia</em>: el techo
     * del periodo nuevo es el mismo que el del anterior, y ese ya esta resuelto en
     * su fila. Es una sola tabla.
     *
     * @return 1 si la fila nacio; 0 si no habia ningun periodo anterior del que
     *         heredar --la serie no existe todavia y quien la abre es el
     *         recalculo-- o si otra peticion simultanea gano la carrera y la fila
     *         ya estaba
     */
    int openPeriod(Long companyId, Long limitDimensionId, String periodKey, LocalDateTime at);

    /**
     * Sella el consumo: deja escrito cuando se comprobo por ultima vez que el
     * contador cuadra con las filas reales (R-ENT-13).
     *
     * <p>
     * <strong>No toca ni el techo ni el consumo</strong>, igual que
     * {@link #upsertCeilings} no toca el sello del consumo y por el mismo motivo:
     * son dos hechos distintos, y refrescar un sello sin haber mirado lo que sella
     * deja un indicador de salud diciendo «sano» justo cuando el dato puede estar
     * mal --peor que no tener indicador--.
     *
     * @return filas selladas
     */
    int markUsageReconciled(Long companyId, Long limitDimensionId, String periodKey,
            LocalDateTime at);
}
