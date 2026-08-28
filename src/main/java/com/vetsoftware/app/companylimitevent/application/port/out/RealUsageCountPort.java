package com.vetsoftware.app.companylimitevent.application.port.out;

import java.util.OptionalInt;

/**
 * Cuenta las filas <strong>de verdad</strong> de un eje para una empresa. Es la
 * otra mitad de R-LIMIT-30: sin una fuente de verdad contra la que comparar, el
 * contador es una cifra que nadie puede contradecir.
 *
 * <h2>Por que devuelve un opcional y no un numero</h2>
 *
 * <p>
 * <strong>No todos los ejes se pueden contar hoy, y fingir que si es peor que
 * admitirlo.</strong> De los ocho sembrados por el changeset 313:
 *
 * <ul>
 * <li><strong>{@code USER}, {@code BRANCH} y {@code TERMINAL}</strong> se
 * cuentan: son de existencias, la fila viva es la verdad y su predicado es
 * exactamente el que mueven las altas y las bajas.
 * <li><strong>{@code ANIMAL} y {@code OWNER}</strong> <em>no</em>: son
 * acumulativos con enfriamiento (D-61), asi que la verdad es «las no borradas
 * mas las borradas dentro de la ventana de
 * {@code limit_dimensions.release_delay_days}», y <strong>ninguna tabla clinica
 * guarda la fecha de borrado</strong> --el borrado es logico y solo deja un
 * booleano--. Contarlas como «las vivas» daria un desvio falso por cada
 * registro borrado en los ultimos treinta dias.
 * <li><strong>{@code APPOINTMENT} e {@code INVOICE}</strong> tampoco: son de
 * flujo, y en facturas ademas solo cuentan tres de los caminos de emision
 * (D-16), asi que un {@code COUNT(*)} por periodo contaria notas credito y
 * conversiones que el contador nunca sumo.
 * <li><strong>{@code STORAGE_GB}</strong> tampoco: los ficheros no guardan su
 * tamaño todavia (R-LIMIT-24), asi que no hay nada que sumar.
 * </ul>
 *
 * <p>
 * Un eje sin fuente devuelve {@link OptionalInt#empty()} y el recuento lo
 * <strong>salta</strong>: ni escribe hecho ni sella. Tratarlo como cero
 * escribiria un desvio catastrofico contra un contador correcto y --peor-- le
 * pondria el sello, declarando comprobado lo que nadie comprobo.
 */
public interface RealUsageCountPort {

    /**
     * @return las filas reales de ese eje en esa empresa, o vacio si el eje no se
     *         puede contar hoy con la verdad que el modelo le exige
     */
    OptionalInt countFor(Long companyId, String dimensionCode);
}
