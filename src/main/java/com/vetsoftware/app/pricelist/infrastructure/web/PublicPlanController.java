package com.vetsoftware.app.pricelist.infrastructure.web;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanIncludedDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicPlansUseCase;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicPlanCapacityResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicPlanCatalogResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicPlanIncludedResponse;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PublicPlanResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara publica del catalogo comercial. Separada de
 * {@link PriceListController} y de {@link CatalogPriceController} porque las
 * dos mitades tienen publico distinto: aqui entra gente que todavia no es
 * cliente.
 *
 * <p>
 * <strong>Es publico</strong> y esta declarado como tal en
 * {@code PublicRoutes.BUSINESS} con patron literal y sin comodines: un
 * {@code /plans/**} habria abierto de paso la administracion de planes que
 * acabara colgando del mismo prefijo. Es el razonamiento que ya dejo
 * {@code /configurator} con sus dos rutas exactas.
 *
 * <p>
 * <strong>Por que aqui y no en {@code catalogitem}.</strong> Lo que a la
 * landing le falta hoy no es el articulo —{@code /catalog-items} existe— sino
 * su <em>precio</em>, y el precio es de este slice. Ponerlo al lado del
 * catalogo obligaria a {@code catalogitem} a leer {@code catalog_prices}, que
 * es exactamente el cruce que el companion VO existe para evitar.
 *
 * <p>
 * Un {@code GET} anonimo no necesita limite de tasa por la invariante de
 * {@code LoginRateLimitFilterTest} —esa exige solo de los {@code POST}
 * publicos—, pero <strong>si conviene cachearlo en el borde</strong>: la
 * respuesta es identica para todo el mundo y esta es la ruta que mas trafico
 * anonimo va a ver.
 */
@RestController
@RequestMapping("/plans")
public class PublicPlanController {

    private final GetPublicPlansUseCase useCase;

    public PublicPlanController(GetPublicPlansUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Sin parametros y sin cabecera de empresa: la respuesta es la misma para
     * cualquiera. Devuelve 200 con la lista vacia cuando no hay tarifa vigente, y
     * no un 404: la portada tiene que seguir cargando.
     */
    @GetMapping
    public PublicPlanCatalogResponse plans() {
        PublicPlanCatalogDto catalog = useCase.get();
        return new PublicPlanCatalogResponse(catalog.currency(), catalog.priceValidFrom(),
                catalog.plans().stream().map(PublicPlanController::toResponse).toList());
    }

    private static PublicPlanResponse toResponse(PublicPlanDto dto) {
        return new PublicPlanResponse(dto.code(), dto.name(), dto.tagline(),
                dto.monthlyFromAmount(), dto.annualFromAmount(), dto.setupAmount(), dto.taxRate(),
                dto.taxTreatment(),
                dto.includes().stream().map(PublicPlanController::toIncludedResponse).toList(),
                dto.capacities().stream().map(PublicPlanController::toCapacityResponse).toList());
    }

    private static PublicPlanIncludedResponse toIncludedResponse(PublicPlanIncludedDto dto) {
        return new PublicPlanIncludedResponse(dto.code(), dto.name(), dto.trialDays());
    }

    private static PublicPlanCapacityResponse toCapacityResponse(PublicPlanCapacityDto dto) {
        return new PublicPlanCapacityResponse(dto.code(), dto.name(), dto.unit(), dto.included(),
                dto.extraUnitAmount());
    }
}
