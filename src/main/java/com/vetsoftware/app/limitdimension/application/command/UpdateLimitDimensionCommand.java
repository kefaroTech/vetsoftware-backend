package com.vetsoftware.app.limitdimension.application.command;

/**
 * Editar un eje limitable.
 *
 * <p>
 * <strong>No lleva ni el código ni el tipo de medida, y las dos ausencias son
 * la regla.</strong> El código es la clave con la que la línea del contrato
 * nombra el eje ({@code subscription_items.capacity_unit}) y se cruza vivo en
 * cada recálculo: cambiarlo dejaría a los contadores de todas las clínicas que
 * lo usan sin eje al que apuntar, en silencio y sin error. El tipo de medida va
 * copiado y atado por clave foránea compuesta contra
 * {@code limit_dimensions(id, measure_kind)}: cambiarlo con artículos vendidos
 * es un error del motor a mitad de transacción, y una operación que muere así
 * es peor que una que no existe. Cambiar cualquiera de los dos es retirar el
 * eje y declarar otro.
 *
 * <p>
 * Tampoco lleva {@code availableFrom}: es la fecha que decide D-74 —si la
 * ausencia de contador significa techo cero o «a este cliente todavía no se le
 * vendió»— y moverla hacia adelante desbloquearía cupos de contratos ya
 * firmados; hacia atrás, los bloquearía. No es un dato editable, es un hecho.
 *
 * <p>
 * No lleva {@code companyId} y no puede llevarlo: el catálogo de ejes es global
 * de plataforma.
 */
public record UpdateLimitDimensionCommand(Long id, String name, Long subModuleId,
        Integer releaseDelayDays) {
}
