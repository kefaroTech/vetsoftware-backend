package com.vetsoftware.app.platformbillingconfig.application.port.out;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import java.util.Optional;

/**
 * Puerto de salida de la tabla singleton.
 *
 * <p>
 * No declara {@code findAll}, ni {@code findById}, ni {@code delete}: la tabla
 * tiene exactamente una fila garantizada por el esquema, así que no hay lista
 * que paginar ni fila que señalar por id, y no hay borrado ni lógico ni físico.
 * Tampoco declara un {@code create}: la fila la siembra el changeset que crea
 * la tabla.
 */
public interface PlatformBillingConfigRepository {

    /**
     * La fila única de configuración, o vacío si no se sembró.
     *
     * <p>
     * Este es el <b>único</b> punto del slice donde el vacío es representable: los
     * dos casos de uso lo convierten inmediatamente en
     * {@code PlatformBillingConfigNotConfiguredException}. El {@code Optional} está
     * aquí para que el adaptador no tenga que conocer esa excepción de dominio, no
     * para que nadie sirva un valor por defecto.
     */
    Optional<PlatformBillingConfig> find();

    /**
     * Persiste los cambios. El {@code @Version} de la entidad JPA hace el control
     * de concurrencia.
     */
    PlatformBillingConfig save(PlatformBillingConfig config);
}
