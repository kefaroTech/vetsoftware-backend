package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Un contador del plan: cuantas unidades trae y a como sale la siguiente.
 *
 * <p>
 * Los dos importes son el precio del <strong>tramo de entrada</strong>, no la
 * escalera entera: los tramos son acumulativos y publicarlos completos es
 * publicar la politica de descuento por volumen.
 *
 * <p>
 * <strong>Un importe por ciclo, igual que {@code monthlyFromAmount} y
 * {@code annualFromAmount} en {@link PublicPlanResponse}.</strong> Habia uno
 * solo, {@code extraUnitAmount}, y era siempre el mensual aunque el nombre no
 * lo dijera. Quien pintaba un plan anual no tenia de donde sacar el precio
 * anual de la unidad adicional y lo extrapolaba del mensual, mientras el
 * servidor cotizaba contra la fila {@code ANNUAL} del articulo: la cifra que
 * veia el cliente no era la que se le iba a cobrar.
 *
 * <p>
 * <strong>Nulo es una respuesta, no un hueco.</strong> Significa que ese
 * contador no se vende suelto en ese ciclo, y es exactamente lo que la
 * contratacion va a decidir —exige precio de entrada en el ciclo pedido—. Quien
 * pinta el plan no debe ofrecer la unidad adicional para ese ciclo; el contador
 * sigue saliendo en la lista, porque {@code included} —lo que el plan trae
 * dentro— es cierto en los dos ciclos.
 */
public record PublicPlanCapacityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Codigo del eje: USER, BRANCH...") String unit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int included,
        @Schema(description = "Precio de la unidad adicional al mes; nulo si no se vende suelta en ese ciclo") BigDecimal monthlyExtraUnitAmount,
        @Schema(description = "Precio de la unidad adicional al ano; nulo si no se vende suelta en ese ciclo") BigDecimal annualExtraUnitAmount) {
}
