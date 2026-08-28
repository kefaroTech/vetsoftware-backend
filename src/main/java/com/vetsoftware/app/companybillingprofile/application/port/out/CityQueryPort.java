package com.vetsoftware.app.companybillingprofile.application.port.out;

import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import java.util.Optional;

/**
 * El municipio de la direccion de facturacion, resuelto contra el catalogo
 * maestro.
 *
 * <p>
 * <strong>Solo {@code findById}, y no falta la variante acotada por
 * empresa.</strong> {@code cities} es catalogo global: no tiene
 * {@code company_id} ni lo alcanza por ninguna asociacion, asi que no existe la
 * fila «de otra empresa» que un {@code findByIdAndCompanyId} protegeria.
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} solo exige la acotada
 * cuando la entidad referida pertenece a una empresa —el caso de
 * {@code animalQueryPort}, donde el animal <em>es</em> de alguien—; añadirla
 * aqui devolveria siempre vacio y ningun municipio se podria elegir.
 */
public interface CityQueryPort {

    Optional<CityRef> findById(Long cityId);
}
