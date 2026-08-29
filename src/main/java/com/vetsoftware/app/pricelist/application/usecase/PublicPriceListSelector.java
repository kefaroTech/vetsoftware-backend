package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.shared.pricing.PriceListValidity;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * <strong>Cual de las tarifas publicadas manda hoy</strong>, en un solo sitio.
 *
 * <p>
 * Lo usan {@link GetPublicPlansService} y {@link GetPublicCatalogService}, y
 * esa es la razon de que exista: son dos endpoints publicos que ponen precio a
 * las mismas filas, y si eligieran la tarifa por su cuenta podrian elegir
 * distinto. El dia que eso pasara, la portada anunciaria el precio de una lista
 * y el configurador el de otra, sin que nada fallara — el peor desenlace
 * posible para un numero que el cliente compara.
 *
 * <p>
 * El criterio, que antes vivia duplicado en un metodo privado:
 *
 * <ul>
 * <li>De las {@code PUBLISHED}, las <em>vigentes</em> hoy, y lo decide
 * {@link PriceListValidity} —el unico predicado del arbol— sobre la fecha que
 * viene del reloj inyectado, no de un {@code CURRENT_DATE} del motor.</li>
 * <li>Si hay varias solapadas —el esquema no lo impide— gana la de
 * {@code validFrom} mas reciente, y a igualdad la de id mayor: la ultima que se
 * publico es la que manda. Determinista, en vez de «la primera que devuelva la
 * consulta».</li>
 * </ul>
 */
final class PublicPriceListSelector {

    private PublicPriceListSelector() {
    }

    /** La tarifa que rige en {@code today}, o vacio si hoy no rige ninguna. */
    static Optional<PublicPriceListDto> vigente(List<PublicPriceListDto> publicadas,
            LocalDate today) {
        if (publicadas == null) {
            return Optional.empty();
        }
        return publicadas.stream()
                .filter(lista -> new PriceListValidity(lista.validFrom(), lista.validTo())
                        .isEffectiveOn(today))
                .max(Comparator.comparing(PublicPriceListDto::validFrom)
                        .thenComparing(PublicPriceListDto::id));
    }
}
