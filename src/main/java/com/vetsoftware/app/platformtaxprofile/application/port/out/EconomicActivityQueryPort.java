package com.vetsoftware.app.platformtaxprofile.application.port.out;

import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import java.util.Optional;

/**
 * La actividad economica (CIIU) de VetSoftware, resuelta contra el catalogo
 * maestro.
 *
 * <p>
 * <strong>Solo {@code findById}, y no falta la variante acotada por
 * empresa.</strong> {@code economic_activities} es catalogo global: no tiene
 * {@code company_id} ni lo alcanza por ninguna asociacion, asi que no existe la
 * fila «de otra empresa» que un {@code findByIdAndCompanyId} protegeria.
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} solo exige la acotada
 * cuando la entidad referida pertenece a una empresa —el caso de
 * {@code animalQueryPort}, donde el animal <em>es</em> de alguien—; y esta
 * feature ni siquiera tiene una empresa desde la que acotar.
 *
 * <p>
 * Es un {@code QueryPort} y no un {@code ValidationPort} porque hacen falta los
 * datos y no solo la respuesta: el codigo y el nombre de la actividad salen en
 * la ficha.
 */
public interface EconomicActivityQueryPort {

    Optional<EconomicActivityRef> findById(Long economicActivityId);
}
