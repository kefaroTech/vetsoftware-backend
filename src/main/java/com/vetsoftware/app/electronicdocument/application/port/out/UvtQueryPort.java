package com.vetsoftware.app.electronicdocument.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lee el valor del UVT vigente (COP) del almacén global de configuración
 * (system_configurations, fila 'uvt'). Lo usa el control del tope de 5 UVT del
 * documento equivalente POS (Res. DIAN 000165/2023). Vacío si la propiedad no
 * está configurada o su valor no es numérico.
 */
public interface UvtQueryPort {
    Optional<BigDecimal> currentUvt();
}
