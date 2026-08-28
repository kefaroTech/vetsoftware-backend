package com.vetsoftware.app.entitlement.application.command;

/**
 * Mover el consumo de una capacidad. {@code delta} es con signo: +1 al dar de
 * alta un usuario, -1 al darlo de baja.
 *
 * <p>
 * <strong>El eje se nombra por su codigo del catalogo</strong>
 * ({@code limit_dimensions.code}), no por un enumerado: es lo que hace que
 * empezar a contar un eje nuevo sea sembrar una fila. El caso de uso lo
 * resuelve contra el catalogo y falla en voz alta si no existe --un codigo mal
 * escrito es un error de programacion, no un cupo de cero--.
 *
 * <p>
 * {@code periodKey} solo lo llevan los ejes de flujo, y es obligatorio en
 * ellos: un eje que se mide por periodo tiene un contador por periodo, y dejar
 * que el servidor invente cual seria repartir el consumo entre dos filas segun
 * quien lo escribiera. Para los demas va {@code null} y el caso de uso pone el
 * centinela (R-LIMIT-05).
 *
 * <p>
 * El movimiento se aplica con un {@code UPDATE ... SET used_quantity =
 * used_quantity + ?} atomico en el motor. <strong>Nunca</strong> leyendo,
 * modificando y guardando desde Java: dos altas simultaneas perderian una.
 */
public record AdjustCompanyCapacityUsageCommand(Long companyId, String dimensionCode,
        String periodKey, int delta) {

    public AdjustCompanyCapacityUsageCommand {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (dimensionCode == null || dimensionCode.isBlank())
            throw new IllegalArgumentException("limit dimension code is required");
        if (delta == 0)
            throw new IllegalArgumentException("delta must not be zero");
    }

    /** El caso corriente: un eje que no es de flujo, sin periodo que declarar. */
    public AdjustCompanyCapacityUsageCommand(Long companyId, String dimensionCode, int delta) {
        this(companyId, dimensionCode, null, delta);
    }
}
