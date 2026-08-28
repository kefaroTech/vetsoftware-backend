package com.vetsoftware.app.companyusageevent.application.port.out;

import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje de limite contra el catalogo.
 *
 * <p>
 * <strong>El eje es un catalogo global y no pertenece a ninguna
 * empresa</strong>, asi que aqui no hay variante acotada que declarar: la regla
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} (BE-COV) solo aplica
 * cuando la entidad referida pertenece a un tenant, y {@code limit_dimensions}
 * no lo hace. La referencia que <em>si</em> es de una empresa —la mascota, la
 * cita, el documento— no se resuelve por puerto: la vigila la base con cuatro
 * claves foraneas compuestas {@code (company_id, x_id)}, que es una barandilla
 * mas fuerte que una consulta previa, porque no se puede olvidar llamarla.
 */
public interface LimitDimensionQueryPort {

    /**
     * El eje por su codigo, con su identificador. Los dos viajan juntos porque
     * {@code fk_cue_dimension} es compuesta.
     */
    Optional<LimitDimensionRef> findByCode(String code);
}
