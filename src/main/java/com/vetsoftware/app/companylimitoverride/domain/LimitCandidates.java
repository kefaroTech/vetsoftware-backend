package com.vetsoftware.app.companylimitoverride.domain;

import java.util.List;

/**
 * Los candidatos a techo de una empresa sobre un eje, ya recogidos y sin
 * ordenar todavía por precedencia.
 *
 * <p>
 * Existe para que {@link EffectiveLimitResolver} siga siendo una función pura
 * sobre números: el resolutor no sabe consultar nada, así que alguien tiene que
 * traerle los candidatos. Esta es esa forma, y por eso es un {@code record} sin
 * comportamiento.
 *
 * <p>
 * <strong>Un elemento {@code null} en cualquiera de las dos listas significa
 * «sin techo», y gana a cualquier número.</strong> No es lo mismo que la lista
 * vacía —que significa «no hay candidato de ese origen»— ni que un cero, que es
 * un techo real que no deja crear nada. Confundir los tres es la diferencia
 * entre una clínica bloqueada, una clínica sin límite y una clínica cuyo techo
 * lo pone el escalón de abajo.
 *
 * @param contractedQuantities
 *            los techos congelados de las líneas <em>vivas</em> del contrato
 *            sobre este eje
 * @param catalogDefaultQuantities
 *            los escalones de fábrica de los artículos que la empresa usa
 *            gratis sobre este eje
 * @param axisPredatesContract
 *            si el eje ya existía cuando la empresa firmó. Solo importa cuando
 *            no hay ningún candidato, y entonces lo decide todo (D-74): con el
 *            eje anterior a la firma, no haber vendido nada significa techo
 *            cero; con el eje posterior, significa que a ese cliente todavía no
 *            se le ofreció y bloquearle sería castigarle por una decisión de
 *            producto que no existía cuando contrató
 */
public record LimitCandidates(List<Integer> contractedQuantities,
        List<Integer> catalogDefaultQuantities, boolean axisPredatesContract) {

    public LimitCandidates {
        contractedQuantities = contractedQuantities == null ? List.of() : contractedQuantities;
        catalogDefaultQuantities = catalogDefaultQuantities == null
                ? List.of()
                : catalogDefaultQuantities;
    }
}
