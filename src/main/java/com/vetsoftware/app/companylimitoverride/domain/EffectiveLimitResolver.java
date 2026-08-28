package com.vetsoftware.app.companylimitoverride.domain;

import java.util.List;

/**
 * <strong>Resuelve la precedencia del techo, en un solo sitio.</strong>
 *
 * <p>
 * Vive en esta rodaja porque la excepción negociada es la cima de la cadena, y
 * es una función pura sobre números: no importa el dominio de ninguna otra
 * feature —recibe candidatos ya resueltos— y por eso puede probarse entera sin
 * base de datos ni contexto.
 *
 * <h2>Las dos reglas, y por qué son dos</h2>
 *
 * <p>
 * <strong>Entre orígenes distintos manda el de más arriba</strong>:
 * {@code COMPANY_OVERRIDE > SUBSCRIPTION > CATALOG_DEFAULT > NONE}. Una empresa
 * con excepción de 300, techo contratado de 200 y fábrica de 100 lee 300, con
 * origen {@code COMPANY_OVERRIDE}.
 *
 * <p>
 * <strong>Dentro de un mismo origen se toma el máximo, nunca la suma</strong>
 * (D-14). El escalón gratuito es un <em>suelo</em>, no un sumando: quien compra
 * tres usuarios tiene tres, no cuatro por tener además un módulo gratuito con
 * cupo uno. Si se sumaran, bastaría con activarse varios módulos gratuitos para
 * inflar el mismo cupo sin pagar, y el escalón gratuito dejaría de ser una
 * puerta de entrada para convertirse en una vía de escape.
 *
 * <h2>Sin candidatos: cero, y la excepción de D-74</h2>
 *
 * <p>
 * Que no haya fila significa <strong>techo cero, jamás ilimitado</strong>: es
 * la decisión que el sistema ya toma hoy y se mantiene. La única excepción es
 * que el eje sea <em>posterior</em> a la firma del contrato — «sin fila porque
 * no se vendió» y «sin fila porque el eje no existía cuando se firmó» tienen
 * respuestas opuestas, y sin distinguirlas un eje de citas añadido en abril
 * dejaría bloqueadas todas las agendas firmadas en enero.
 */
public final class EffectiveLimitResolver {

    private EffectiveLimitResolver() {
    }

    /**
     * @param overrideQuantity
     *            el techo de la excepción viva, o {@code null} si no hay excepción.
     *            Una excepción siempre declara cantidad, así que aquí {@code null}
     *            significa «no hay», no «sin techo»
     * @param overrideId
     *            el id de esa excepción; obligatorio si hay cantidad
     * @param contractedQuantities
     *            los techos congelados de las líneas vivas sobre este eje. Un
     *            elemento {@code null} es una línea sin techo, y gana a cualquier
     *            número
     * @param catalogDefaultQuantities
     *            los escalones de fábrica de los artículos que la empresa usa
     *            gratis sobre este eje, con la misma convención
     * @param axisPredatesContract
     *            si el eje ya existía cuando se firmó el contrato. Solo importa
     *            cuando no hay ningún candidato
     */
    public static EffectiveLimit resolve(Integer overrideQuantity, Long overrideId,
            List<Integer> contractedQuantities, List<Integer> catalogDefaultQuantities,
            boolean axisPredatesContract) {
        if (overrideQuantity != null) {
            if (overrideId == null)
                throw new IllegalArgumentException(
                        "an override ceiling must name the override it came from");
            return new EffectiveLimit(overrideQuantity, LimitSource.COMPANY_OVERRIDE, overrideId);
        }
        if (contractedQuantities != null && !contractedQuantities.isEmpty())
            return new EffectiveLimit(highest(contractedQuantities), LimitSource.SUBSCRIPTION,
                    null);
        if (catalogDefaultQuantities != null && !catalogDefaultQuantities.isEmpty())
            return new EffectiveLimit(highest(catalogDefaultQuantities),
                    LimitSource.CATALOG_DEFAULT, null);
        // D-74: sin fila y con el eje posterior a la firma, sin techo; si el eje ya
        // existia, cero.
        return new EffectiveLimit(axisPredatesContract ? Integer.valueOf(0) : null,
                LimitSource.NONE, null);
    }

    /**
     * El mayor de los candidatos, con {@code null} —«sin techo»— ganando a
     * cualquier número. <strong>Nunca la suma.</strong>
     */
    private static Integer highest(List<Integer> quantities) {
        int best = 0;
        for (Integer quantity : quantities) {
            if (quantity == null)
                return null;
            if (quantity > best)
                best = quantity;
        }
        return best;
    }
}
