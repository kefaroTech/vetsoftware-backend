package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.PriceListRef;
import java.util.Optional;

/**
 * Resuelve la tarifa con la que se cotiza. Tabla global de plataforma, sin
 * tenant.
 */
public interface PriceListQueryPort {

    /**
     * Solo devuelve la lista si esta PUBLISHED. Cotizar contra un borrador
     * congelaria en un documento con valor legal unos precios que todavia se
     * estaban editando.
     *
     * <p>
     * <b>Devuelve la lista publicada CON su ventana de vigencia, y el vacio
     * significa exactamente una cosa</b>: no hay lista con ese id publicada y
     * habilitada. La vigencia por fecha (D-73) <i>no</i> se filtra aqui a
     * proposito: si el SQL la descartara, una tarifa caducada y una inexistente
     * volverian las dos como {@code Optional.empty()} y el llamador no podria
     * distinguirlas —son dos problemas con arreglos distintos, y uno de ellos
     * significa que el catalogo se quedo sin tarifa—. Quien cotiza compara la
     * ventana contra el dia que deriva de su reloj inyectado
     * ({@code PriceListRef#requireEffectiveOn}), que es donde vive la zona del
     * negocio (D-81) y donde la decision se puede probar sin base de datos.
     */
    Optional<PriceListRef> findPublishedById(Long priceListId);
}
