package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;
import java.util.List;

/**
 * Un paquete con su precio y <strong>la lista de rotulos que trae
 * dentro</strong>.
 *
 * <p>
 * Los rotulos son lo que convierte esta respuesta en un comparador: con el
 * precio del paquete y el de cada pieza suelta —que viaja en la misma
 * respuesta—, el front puede decirle al cliente «lo que has elegido cuesta X y
 * este paquete lo incluye por Y», que es la conversacion que el modelo de
 * compra por necesidad pide y que hoy nadie puede tener, porque las piezas
 * sueltas no tienen precio publicado.
 *
 * <p>
 * <strong>Y sirven para no cobrar dos veces.</strong> Un paquete y una pieza
 * suya no se compran juntos; el servidor lo rechaza en
 * {@code SelfServeQuoteService}. Publicar la composicion es lo que permite que
 * el front lo evite antes de pedirlo, en vez de descubrirlo con un 400.
 */
public record PublicCatalogPackDto(String code, String name, String tagline,
        BigDecimal monthlyAmount, BigDecimal annualAmount, BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment, List<String> componentCodes) {

    /**
     * La cabecera sale de la misma fila plana que alimenta {@code GET /plans}
     * ({@link PublicPlanRowDto}); los componentes se le adjuntan ya agrupados por
     * el servicio.
     */
    public static PublicCatalogPackDto from(PublicPlanRowDto row, List<String> componentCodes) {
        return new PublicCatalogPackDto(row.code(), row.name(), row.tagline(),
                row.monthlyFromAmount(), row.annualFromAmount(), row.setupAmount(), row.taxRate(),
                row.taxTreatment(), List.copyOf(componentCodes));
    }
}
