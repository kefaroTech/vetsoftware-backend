package com.vetsoftware.app.pricelist.infrastructure.web;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicCatalogUseCase;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicCatalogCapacityResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicCatalogItemResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicCatalogPackResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicCatalogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catalogo contratable completo, para el configurador de la landing.
 *
 * <p>
 * <strong>Por que un recurso nuevo y no un campo mas en
 * {@link PublicPlanController}.</strong> Tres razones, y ninguna es estetica:
 *
 * <ul>
 * <li><b>Son dos preguntas distintas.</b> {@code /plans} responde «que paquetes
 * cerrados vendo»; esto responde «que puede comprar quien no quiere un
 * paquete». Un cliente que arma su propia combinacion nunca lee la
 * primera.</li>
 * <li><b>Ampliar la otra respuesta rompe los dos fronts.</b>
 * {@code PublicPlanCatalogResponse} lo consume la landing del tenant con
 * {@code MatchesContract}, y su comprobacion {@code UndeclaredFields} falla en
 * cuanto llega un campo que el front no declara. Un recurso nuevo es
 * estrictamente aditivo: no toca una sola linea de lo que ya funciona.</li>
 * <li><b>La forma seria incoherente.</b> En {@code /plans} un modulo sale sin
 * precio <em>a proposito</em>, porque alli el precio es el del paquete que lo
 * contiene. Meterle el precio suelto obligaria a que el mismo campo significara
 * dos cosas segun donde se lea.</li>
 * </ul>
 *
 * <p>
 * <strong>Es publico</strong> y esta declarado como tal en
 * {@code PublicRoutes.BUSINESS} con patron literal y sin comodines: un
 * {@code /catalog/**} habria abierto de paso cualquier administracion que acabe
 * colgando del mismo prefijo. Es el razonamiento que ya dejaron
 * {@code /configurator} y {@code /plans} con sus rutas exactas. Son las DOS
 * cosas —la ruta y el {@code @NoAuthorizationRequired} del puerto—: con una
 * sola el prospecto se lleva un 401, o el puerto queda abierto y nadie lo
 * alcanza.
 *
 * <p>
 * Un {@code GET} anonimo no necesita limite de tasa por la invariante de
 * {@code LoginRateLimitFilterTest} —esa exige solo de los {@code POST}
 * publicos—, pero <strong>si conviene cachearlo en el borde</strong>: la
 * respuesta es identica para todo el mundo.
 */
@RestController
@RequestMapping("/catalog")
public class PublicCatalogController {

    private final GetPublicCatalogUseCase useCase;

    public PublicCatalogController(GetPublicCatalogUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Sin parametros y sin cabecera de empresa: la respuesta es la misma para
     * cualquiera. Devuelve 200 con las cuatro listas vacias cuando no hay tarifa
     * vigente, y no un 404: la portada tiene que seguir cargando.
     */
    @GetMapping
    public PublicCatalogResponse catalog() {
        PublicCatalogDto catalog = useCase.get();
        return new PublicCatalogResponse(catalog.currency(), catalog.priceValidFrom(),
                catalog.modules().stream().map(PublicCatalogController::toItemResponse).toList(),
                catalog.capacities().stream().map(PublicCatalogController::toCapacityResponse)
                        .toList(),
                catalog.oneTimeItems().stream().map(PublicCatalogController::toItemResponse)
                        .toList(),
                catalog.packs().stream().map(PublicCatalogController::toPackResponse).toList());
    }

    private static PublicCatalogItemResponse toItemResponse(PublicCatalogItemDto dto) {
        return new PublicCatalogItemResponse(dto.code(), dto.name(), dto.description(),
                dto.mandatory(), dto.trialDays(), dto.monthlyAmount(), dto.annualAmount(),
                dto.setupAmount(), dto.taxRate(), dto.taxTreatment(), dto.selfServiceEligible());
    }

    private static PublicCatalogCapacityResponse toCapacityResponse(PublicCatalogCapacityDto dto) {
        return new PublicCatalogCapacityResponse(dto.code(), dto.name(), dto.description(),
                dto.mandatory(), dto.unit(), dto.monthlyIncludedQuantity(),
                dto.annualIncludedQuantity(), dto.monthlyUnitAmount(), dto.annualUnitAmount(),
                dto.taxRate(), dto.taxTreatment(), dto.selfServiceEligible());
    }

    private static PublicCatalogPackResponse toPackResponse(PublicCatalogPackDto dto) {
        return new PublicCatalogPackResponse(dto.code(), dto.name(), dto.tagline(),
                dto.monthlyAmount(), dto.annualAmount(), dto.setupAmount(), dto.taxRate(),
                dto.taxTreatment(), dto.componentCodes());
    }
}
