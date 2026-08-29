package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;
import java.util.List;

/**
 * Un plan tal como lo puede leer un prospecto sin cuenta.
 *
 * <p>
 * <strong>Un plan es un paquete del catalogo</strong>
 * ({@code ItemType.BUNDLE}), no una agrupacion que invente este endpoint.
 * Componer «Esencial / Clinica / Cadena» a partir de modulos sueltos es una
 * decision editorial, y tomarla aqui la escondaria en el codigo en vez de
 * dejarla donde se puede cambiar sin desplegar: sembrando un paquete. Si la
 * lista sale vacia es que comercial todavia no compuso ninguno.
 *
 * <p>
 * <strong>Es mas pobre que {@code CatalogItemDto} a proposito.</strong> No
 * lleva id, ni {@code status}, ni {@code core}, ni {@code minQuantity} /
 * {@code maxQuantity}, ni {@code sortOrder}, ni {@code version}, ni la escalera
 * de tramos: mezclar «lo que puede ver el mundo» con «lo que puede editar
 * SYSTEM» convierte cualquier campo nuevo del lado de administracion en una
 * fuga silenciosa hacia la respuesta publica.
 *
 * @param tagline
 *            el {@code short_description} del paquete. Es el unico texto
 *            comercial que el modelo tiene; no se inventa ninguno, y en
 *            particular no hay ningun campo «recomendado»: eso es una decision
 *            editorial que el catalogo no guarda.
 * @param monthlyFromAmount
 *            precio del tramo de entrada, y por eso se rotula «desde». Puede
 *            ser nulo si el paquete solo esta tarifado en anual.
 */
public record PublicPlanDto(String code, String name, String tagline, BigDecimal monthlyFromAmount,
        BigDecimal annualFromAmount, BigDecimal setupAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, List<PublicPlanIncludedDto> includes,
        List<PublicPlanCapacityDto> capacities) {
}
